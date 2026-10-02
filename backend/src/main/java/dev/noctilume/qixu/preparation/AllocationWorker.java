package dev.noctilume.qixu.preparation;

import dev.noctilume.qixu.common.*;
import dev.noctilume.qixu.identity.*;
import java.util.Map;
import org.springframework.stereotype.Service;

/** No transaction here: slow network, crypto and matching do not hold database guards. */
@Service
public class AllocationWorker {
    private final PreparationBatches p;private final AllocationPublisher publisher;private final RandomnessSource source;
    public AllocationWorker(PreparationBatches p,AllocationPublisher publisher,RandomnessSource source) {this.p=p;this.publisher=publisher;this.source=source;}
    private void prepare(long id) {
        if(p.b.jdbc.queryForObject("SELECT COUNT(*) FROM allocation_run WHERE batch_id=?",Integer.class,id)>0)return;
        var work=publisher.work(id);var proof=source.fetch(work.round());
        var candidate=AllocationGraph.allocate(work.graph(),work.inputHash(),proof.randomness());publisher.retainCandidate(work,proof,candidate);
    }
    public Map<String,Object> allocate(AuthService.Session session,long id,String key,PreparationBatches.Action body) {
        var actor=p.b.current(session);p.scope(actor,id);
        var replay=p.b.replay(actor,key,"preparation.allocate:"+id,body);if(replay.isPresent())return replay.get();
        if(!"ALLOCATE".equals(body.action()))throw DomainException.invalid("分配操作不正确。");
        var batch=p.batch(id,false);Business.version(Business.number(batch,"version"),body.version());PreparationBatches.state(batch,"FROZEN");
        prepare(id);
        return publisher.publish(session,id,key,body);
    }
    public void allocateDue(long id) {
        if(!"FROZEN".equals(p.batch(id,false).get("status")))return;
        prepare(id);publisher.publishDue(id);
    }
}
