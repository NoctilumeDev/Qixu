package dev.noctilume.qixu.preparation;

import dev.noctilume.qixu.common.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.availability.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PreparationTasks {
  private static final org.slf4j.Logger LOG =
      org.slf4j.LoggerFactory.getLogger(PreparationTasks.class);
  private final dev.noctilume.qixu.recovery.RecoveryFence recovery;
  private final Business b;
  private final BatchFreezer freezer;
  private final AllocationWorker worker;
  private final AllocationPublisher publisher;
  private final LongSeats seats;
  private final ApplicationAvailability availability;
  private final boolean enabled;
  private long maintenanceCursor;

  public PreparationTasks(
      Business b,
      BatchFreezer freezer,
      AllocationWorker worker,
      AllocationPublisher publisher,
      LongSeats seats,
      ApplicationAvailability availability,
      dev.noctilume.qixu.recovery.RecoveryFence recovery,
      @Value("${qixu.tasks-enabled:true}") boolean enabled) {
    this.b = b;
    this.freezer = freezer;
    this.worker = worker;
    this.publisher = publisher;
    this.seats = seats;
    this.availability = availability;
    this.recovery = recovery;
    this.enabled = enabled;
  }

  @Scheduled(fixedDelayString = "${qixu.task-delay-ms:5000}", initialDelay = 10000)
  public synchronized void run() {
    if (!enabled
        || !recovery.ready()
        || availability.getReadinessState() != ReadinessState.ACCEPTING_TRAFFIC) return;
    try {
      var now = b.now();
      for (var row :
          b.jdbc.queryForList(
              "SELECT id FROM preparation_batch WHERE status='OPEN' AND closes_at<=? ORDER BY freeze_deadline,id LIMIT 2",
              now))
        attempt(
            "freeze",
            Business.number(row, "id"),
            () -> freezer.freezeDue(Business.number(row, "id")));
      for (var row :
          b.jdbc.queryForList(
              "SELECT id FROM preparation_batch WHERE status='FROZEN' AND result_deadline<=? ORDER BY result_deadline,id LIMIT 10",
              now))
        attempt(
            "deadline",
            Business.number(row, "id"),
            () -> publisher.failDue(Business.number(row, "id")));
      for (var row :
          b.jdbc.queryForList(
              "SELECT id FROM preparation_batch WHERE status='FROZEN' AND random_at<=? AND result_deadline>? ORDER BY result_deadline,id LIMIT 2",
              now,
              now))
        attempt(
            "allocate",
            Business.number(row, "id"),
            () -> worker.allocateDue(Business.number(row, "id")));
      var maintenance = maintenancePage(now, maintenanceCursor);
      if (maintenance.isEmpty() && maintenanceCursor != 0) {
        maintenanceCursor = 0;
        maintenance = maintenancePage(now, 0);
      }
      for (var row : maintenance) {
        long id = Business.number(row, "id");
        // Advance even when one batch is deferred; it cannot pin later batches.
        maintenanceCursor = id;
        attempt("maintain", id, () -> seats.maintain(id));
      }
    } catch (org.springframework.dao.DataAccessException e) {
      LOG.warn("Preparation tasks deferred: {}", e.getClass().getSimpleName());
    }
  }

  private java.util.List<java.util.Map<String, Object>> maintenancePage(
      java.time.LocalDateTime now, long after) {
    return b.jdbc.queryForList(
        "SELECT id FROM preparation_batch WHERE id>? AND status='RESULT_PUBLISHED' AND (cycle_ends_at<=? OR EXISTS(SELECT 1 FROM long_offer o WHERE o.batch_id=preparation_batch.id AND o.status='OPEN') OR EXISTS(SELECT 1 FROM waitlist_entry w WHERE w.batch_id=preparation_batch.id AND w.status='ACTIVE')) ORDER BY id LIMIT 10",
        after,
        now);
  }

  private void attempt(String phase, long id, Runnable work) {
    try {
      work.run();
    } catch (DomainException e) {
      LOG.warn("Preparation {} batch {} deferred: {}", phase, id, e.code());
    } catch (org.springframework.dao.DataAccessException e) {
      LOG.warn(
          "Preparation {} batch {} database deferred: {}", phase, id, e.getClass().getSimpleName());
    } catch (IllegalStateException e) {
      LOG.error("Preparation {} batch {} stopped: {}", phase, id, e.getMessage());
    }
  }
}
