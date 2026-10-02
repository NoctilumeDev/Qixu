package dev.noctilume.qixu.preparation;

import dev.noctilume.qixu.common.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.availability.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PreparationTasks {
    private static final org.slf4j.Logger LOG=org.slf4j.LoggerFactory.getLogger(PreparationTasks.class);
    private final Business b;private final BatchFreezer freezer;private final AllocationWorker worker;private final AllocationPublisher publisher;private final LongSeats seats;private final ApplicationAvailability availability;private final boolean enabled;
    public PreparationTasks(Business b,BatchFreezer freezer,AllocationWorker worker,AllocationPublisher publisher,LongSeats seats,ApplicationAvailability availability,@Value("${qixu.tasks-enabled:true}")boolean enabled) {this.b=b;this.freezer=freezer;this.worker=worker;this.publisher=publisher;this.seats=seats;this.availability=availability;this.enabled=enabled;}
    @Scheduled(fixedDelayString="${qixu.task-delay-ms:5000}",initialDelay=10000)
    public void run() {
        if(!enabled || availability.getReadinessState()!=ReadinessState.ACCEPTING_TRAFFIC)return;
        try {
            var now=b.now();
            for(var row:b.jdbc.queryForList("SELECT id FROM preparation_batch WHERE status='OPEN' AND closes_at<=? ORDER BY freeze_deadline,id LIMIT 2",now))attempt("freeze",Business.number(row,"id"),()->freezer.freezeDue(Business.number(row,"id")));
            for(var row:b.jdbc.queryForList("SELECT id FROM preparation_batch WHERE status='FROZEN' AND result_deadline<=? ORDER BY result_deadline,id LIMIT 10",now))attempt("deadline",Business.number(row,"id"),()->publisher.failDue(Business.number(row,"id")));
            for(var row:b.jdbc.queryForList("SELECT id FROM preparation_batch WHERE status='FROZEN' AND random_at<=? AND result_deadline>? ORDER BY result_deadline,id LIMIT 2",now,now))attempt("allocate",Business.number(row,"id"),()->worker.allocateDue(Business.number(row,"id")));
            for(var row:b.jdbc.queryForList("SELECT id FROM preparation_batch WHERE status='RESULT_PUBLISHED' AND (cycle_ends_at<=? OR EXISTS(SELECT 1 FROM long_offer o WHERE o.batch_id=preparation_batch.id AND o.status='OPEN') OR EXISTS(SELECT 1 FROM waitlist_entry w WHERE w.batch_id=preparation_batch.id AND w.status='ACTIVE')) ORDER BY id LIMIT 10",now))attempt("maintain",Business.number(row,"id"),()->seats.maintain(Business.number(row,"id")));
        }catch(org.springframework.dao.DataAccessException e){LOG.warn("Preparation tasks deferred: {}",e.getClass().getSimpleName());}
    }
    private void attempt(String phase,long id,Runnable work) {
        try{work.run();}
        catch(DomainException e){LOG.warn("Preparation {} batch {} deferred: {}",phase,id,e.code());}
        catch(org.springframework.dao.DataAccessException e){LOG.warn("Preparation {} batch {} database deferred: {}",phase,id,e.getClass().getSimpleName());}
        catch(IllegalStateException e){LOG.error("Preparation {} batch {} stopped: {}",phase,id,e.getMessage());}
    }
}
