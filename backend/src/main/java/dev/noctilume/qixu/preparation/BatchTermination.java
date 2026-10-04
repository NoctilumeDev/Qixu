package dev.noctilume.qixu.preparation;

import dev.noctilume.qixu.common.*;
import dev.noctilume.qixu.identity.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** Explicitly close an unused round; never a bulk entitlement revocation or a new draw. */
@Service
public class BatchTermination {
  private final PreparationBatches p;

  public record Cancel(long version, String action, String reason) {}

  public BatchTermination(PreparationBatches p) {
    this.p = p;
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public Map<String, Object> cancel(
      AuthService.Session session, long id, String key, Cancel body, String requestId) {
    var batch = p.batch(id, true);
    p.rights().lockFloors(p.floors(id));
    var b = p.business();
    var users = new TreeSet<Long>(List.of(session.actor().id()));
    for (var person :
        b.jdbc.queryForList("SELECT user_id FROM preparation_application WHERE batch_id=?", id))
      users.add(Business.number(person, "user_id"));
    b.users(users);
    var actor = b.current(session);
    p.scope(actor, id);
    return b.once(
        actor,
        key,
        "preparation.cancel:" + id,
        body,
        () -> {
          Business.version(Business.number(batch, "version"), body.version());
          Business.text(body.reason(), 500, true);
          String state = batch.get("status").toString();
          if (body.action() != null && body.action().equals("CANCEL")) {
            if (!Set.of("OPEN", "FROZEN").contains(state))
              throw Business.conflict("BATCH_STATE_CONFLICT", "该入口只结束未发布批次。");
          } else if (body.action() != null && body.action().equals("CANCEL_UNUSED")) {
            PreparationBatches.state(batch, "RESULT_PUBLISHED");
            if (b.jdbc.queryForObject(
                        "SELECT COUNT(*) FROM seat_entitlement WHERE batch_id=?", Integer.class, id)
                    > 0
                || b.jdbc.queryForObject(
                        "SELECT COUNT(*) FROM long_offer WHERE batch_id=? AND status='OPEN' AND deadline>?",
                        Integer.class,
                        id,
                        b.now())
                    > 0)
              throw Business.conflict("BATCH_HAS_GRANTED_RIGHTS", "已有确认历史或有效要约，不能把已授予轮次当废弃轮次取消。");
          } else throw DomainException.invalid("请选择明确取消未发布或无人持权轮次。");
          b.jdbc.update(
              "UPDATE long_offer SET status='CANCELED',version=version+1 WHERE batch_id=? AND status='OPEN'",
              id);
          b.jdbc.update(
              "UPDATE waitlist_entry SET status='FINISHED',version=version+1 WHERE batch_id=? AND status='ACTIVE'",
              id);
          b.jdbc.update(
              "UPDATE preparation_batch SET status='CANCELED',failure_reason='ADMINISTRATIVE_UNUSED_CANCELLATION',version=version+1 WHERE id=?",
              id);
          p.event(
              id,
              null,
              actor.id(),
              "BATCH_CANCELED",
              Map.of("reason", body.reason(), "previousStatus", state));
          b.audit(actor.id(), "BATCH_CANCELED", "BATCH", id, requestId);
          for (var person :
              b.jdbc.queryForList(
                  "SELECT user_id FROM preparation_application WHERE batch_id=? ORDER BY user_id",
                  id))
            b.notify(
                Business.number(person, "user_id"),
                "batch:" + id + ":canceled",
                "长期席位轮次已明确取消",
                body.reason() + "；没有隐式收回已确认权，原申请和分配历史仍可查询。",
                "BATCH",
                id);
          return p.detail(id);
        });
  }
}
