package dev.noctilume.qixu.governance;

import dev.noctilume.qixu.common.*;
import dev.noctilume.qixu.identity.*;
import dev.noctilume.qixu.preparation.LongEligibility;
import dev.noctilume.qixu.spaces.SpaceRights;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import tools.jackson.databind.json.JsonMapper;

/** A notice is not revocation; an overturned decision is not authority to steal another holder's seat. */
@Service
public class GovernanceCases {
  private final Business b;
  private final SpaceRights rights;
  private final LongEligibility eligibility;
  private final JsonMapper json;

  public record Create(
      long entitlementId,
      long entitlementVersion,
      String reasonCode,
      String evidence,
      OffsetDateTime statementUntil) {}

  public record Statement(long version, String message) {}

  public record Decision(long version, String action, String reason, OffsetDateTime penaltyUntil) {}

  public record Review(long version, String action, String reason) {}

  public GovernanceCases(
      Business b, SpaceRights rights, LongEligibility eligibility, JsonMapper json) {
    this.b = b;
    this.rights = rights;
    this.eligibility = eligibility;
    this.json = json;
  }

  private Map<String, Object> row(long id) {
    var rows = b.jdbc.queryForList("SELECT * FROM governance_case WHERE id=?", id);
    if (rows.isEmpty()) throw DomainException.missing();
    return rows.get(0);
  }

  private Map<String, Object> entitlement(long id) {
    var rows = b.jdbc.queryForList("SELECT * FROM seat_entitlement WHERE id=?", id);
    if (rows.isEmpty()) throw DomainException.missing();
    return rows.get(0);
  }

  private void history(long id, long actor, String action, String message) {
    b.jdbc.update(
        "INSERT INTO governance_history(case_id,actor_id,action,message,created_at) VALUES(?,?,?,?,?)",
        id,
        actor,
        action,
        message,
        b.now());
  }

  private void allocationHistory(
      Map<String, Object> record, long actor, String action, Object detail) {
    b.jdbc.update(
        "INSERT INTO allocation_event(batch_id,user_id,actor_id,action,detail_json,created_at) VALUES(?,?,?,?,?,?)",
        record.get("batch_id"),
        record.get("user_id"),
        actor,
        action,
        json.writeValueAsString(detail),
        b.now());
  }

  private void notify(Map<String, Object> record, String action, String message) {
    b.notify(
        Business.number(record, "user_id"),
        "governance:" + record.get("id") + ":" + action,
        "长期席位治理进展",
        message,
        "GOVERNANCE",
        Business.number(record, "id"));
  }

  private Actor lock(AuthService.Session session, Map<String, Object> target, boolean admin) {
    long batch = Business.number(target, "batch_id"), floor = Business.number(target, "floor_id");
    if (b.jdbc
        .queryForList("SELECT id FROM preparation_batch WHERE id=? FOR UPDATE", batch)
        .isEmpty()) throw DomainException.missing();
    var floors = new ArrayList<Long>(List.of(floor));
    b.jdbc
        .queryForList("SELECT floor_id FROM preparation_pool WHERE batch_id=?", batch)
        .forEach(r -> floors.add(Business.number(r, "floor_id")));
    rights.lockFloors(floors);
    b.users(List.of(session.actor().id(), Business.number(target, "user_id")));
    var actor = b.current(session);
    if (admin) b.admin(actor, floor);
    else if (actor.id() != Business.number(target, "user_id")) throw DomainException.missing();
    return actor;
  }

  private Map<String, Object> detail(Actor actor, long id) {
    var record = row(id);
    if (actor.id() != Business.number(record, "user_id")) {
      if (!actor.role().equals("ADMIN")) throw DomainException.missing();
      b.admin(actor, Business.number(record, "floor_id"));
    }
    var value = b.view(record);
    value.remove("open_entitlement_id");
    value.put(
        "history",
        b
            .jdbc
            .queryForList(
                "SELECT actor_id,action,message,created_at FROM governance_history WHERE case_id=? ORDER BY id",
                id)
            .stream()
            .map(b::view)
            .toList());
    value.put("entitlement", b.view(entitlement(Business.number(record, "entitlement_id"))));
    value.put(
        "penalty",
        b
            .jdbc
            .queryForList(
                "SELECT starts_at,ends_at,status,revoked_at FROM long_application_penalty WHERE case_id=?",
                id)
            .stream()
            .map(
                r -> {
                  var v = b.view(r);
                  v.put(
                      "effective",
                      r.get("status").equals("ACTIVE")
                          && !b.now().isBefore(Business.date(r, "starts_at"))
                          && b.now().isBefore(Business.date(r, "ends_at")));
                  return v;
                })
            .toList());
    return value;
  }

  @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
  public Map<String, Object> get(Actor actor, long id) {
    return detail(actor, id);
  }

  @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
  public Map<String, Object> list(Actor actor, boolean admin, int page) {
    if (page < 1 || page > 10000) throw DomainException.invalid("页码超出范围。");
    if (admin && !actor.role().equals("ADMIN")) throw DomainException.forbidden();
    String where =
        admin
            ? "EXISTS(SELECT 1 FROM admin_scope s WHERE s.floor_id=g.floor_id AND s.user_id=?)"
            : "g.user_id=?";
    long total =
        b.jdbc.queryForObject(
            "SELECT COUNT(*) FROM governance_case g WHERE " + where, Long.class, actor.id());
    return Map.of(
        "items",
        b
            .jdbc
            .queryForList(
                "SELECT g.id,g.space_id,g.batch_id,g.status,g.reason_code,g.statement_until,g.appeal_until,g.version FROM governance_case g WHERE "
                    + where
                    + " ORDER BY g.id DESC LIMIT 50 OFFSET ?",
                actor.id(),
                (page - 1) * 50)
            .stream()
            .map(b::view)
            .toList(),
        "total",
        total,
        "page",
        page,
        "size",
        50);
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public Map<String, Object> create(
      AuthService.Session session, String key, Create body, String requestId) {
    var original = entitlement(body.entitlementId());
    var actor = lock(session, original, true);
    return b.once(
        actor,
        key,
        "governance.create",
        body,
        () -> {
          var current = entitlement(body.entitlementId());
          Business.version(Business.number(current, "version"), body.entitlementVersion());
          if (!current.get("status").equals("ACTIVE")
              || !b.now().isBefore(Business.date(current, "ends_at")))
            throw Business.conflict("ENTITLEMENT_STATE_CONFLICT", "只有当前有效原使用权可以提出收回通知。");
          if (body.reasonCode() == null
              || !Set.of(
                      "FALSE_DECLARATION",
                      "UNAUTHORIZED_TRANSFER",
                      "PERSISTENT_OBSTRUCTION",
                      "EXPLICIT_RULE_VIOLATION")
                  .contains(body.reasonCode()))
            throw DomainException.invalid("必须选择明确规则问题，不支持勤奋、签到或时长指标。");
          Business.text(body.evidence(), 3000, true);
          var now = b.now();
          var until = Business.time(body.statementUntil());
          if (until.isBefore(now.plusHours(24)) || until.isAfter(now.plusDays(7)))
            throw DomainException.invalid("陈述期限须在当前时刻24小时至7天之后。");
          if (b.jdbc.queryForObject(
                  "SELECT COUNT(*) FROM governance_case WHERE entitlement_id=? AND status IN ('NOTICE','APPEALED')",
                  Integer.class,
                  body.entitlementId())
              > 0) throw Business.conflict("GOVERNANCE_ALREADY_OPEN", "该项使用权已有待处理案件。");
          long id =
              b.insert(
                  "INSERT INTO governance_case(entitlement_id,batch_id,user_id,space_id,floor_id,entitlement_version,reason_code,evidence,opened_by,opened_at,statement_until) VALUES(?,?,?,?,?,?,?,?,?,?,?)",
                  body.entitlementId(),
                  current.get("batch_id"),
                  current.get("user_id"),
                  current.get("space_id"),
                  current.get("floor_id"),
                  body.entitlementVersion(),
                  body.reasonCode(),
                  body.evidence(),
                  actor.id(),
                  now,
                  until);
          history(id, actor.id(), "NOTICE", body.evidence());
          b.audit(actor.id(), "GOVERNANCE_NOTICE", "GOVERNANCE", id, requestId);
          notify(row(id), "notice", "已提出治理通知；当前使用权保持。请在 " + Business.iso(until) + " 前查看私有依据并提交陈述。");
          if (until.isBefore(b.now().plusHours(24)))
            throw Business.conflict("STATEMENT_WINDOW_SHORTENED", "通知写入后不足24小时，未生效，请重新设置期限。");
          return detail(actor, id);
        });
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public Map<String, Object> statement(
      AuthService.Session session, long id, String key, Statement body, String requestId) {
    var actor = lock(session, row(id), false);
    return b.once(
        actor,
        key,
        "governance.statement:" + id,
        body,
        () -> {
          var record = row(id);
          Business.version(Business.number(record, "version"), body.version());
          Business.text(body.message(), 3000, true);
          if (!record.get("status").equals("NOTICE")
              || !b.now().isBefore(Business.date(record, "statement_until")))
            throw Business.conflict("STATEMENT_WINDOW_CLOSED", "陈述窗口已经结束，已提交历史仍可查看。");
          history(id, actor.id(), "STATEMENT", body.message());
          b.jdbc.update("UPDATE governance_case SET version=version+1 WHERE id=?", id);
          b.audit(actor.id(), "GOVERNANCE_STATEMENT", "GOVERNANCE", id, requestId);
          if (!b.now().isBefore(Business.date(record, "statement_until")))
            throw Business.conflict("STATEMENT_WINDOW_CLOSED", "陈述写入跨过期限，整次操作未生效。");
          return detail(actor, id);
        });
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public Map<String, Object> decide(
      AuthService.Session session, long id, String key, Decision body, String requestId) {
    var actor = lock(session, row(id), true);
    return b.once(
        actor,
        key,
        "governance.decision:" + id,
        body,
        () -> {
          var record = row(id);
          Business.version(Business.number(record, "version"), body.version());
          Business.text(body.reason(), 1500, true);
          if (!record.get("status").equals("NOTICE"))
            throw Business.conflict("GOVERNANCE_STATE_CONFLICT", "案件已进入其他阶段。");
          if (!Set.of("DISMISS", "REVOKE").contains(Objects.toString(body.action(), "")))
            throw DomainException.invalid("请选择撤销通知或正式收回。");
          var current = entitlement(Business.number(record, "entitlement_id"));
          var now = b.now();
          Long revoked = null;
          if (body.action().equals("REVOKE")) {
            if (now.isBefore(Business.date(record, "statement_until")))
              throw Business.conflict("STATEMENT_WINDOW_OPEN", "尚在陈述期，不能提前收回。");
            Business.version(
                Business.number(current, "version"),
                Business.number(record, "entitlement_version"));
            if (!current.get("status").equals("ACTIVE")
                || !now.isBefore(Business.date(current, "ends_at")))
              throw Business.conflict("ENTITLEMENT_STATE_CONFLICT", "原使用权已退出或结束，不能假称由本案收回。");
            if (body.penaltyUntil() != null) {
              var until = Business.time(body.penaltyUntil());
              if (!until.isAfter(now) || until.isAfter(now.plusDays(30)))
                throw DomainException.invalid("长期申请处罚期限须晚于当前时刻且不超过30天。");
              b.jdbc.update(
                  "INSERT INTO long_application_penalty(case_id,user_id,starts_at,ends_at) VALUES(?,?,?,?)",
                  id,
                  record.get("user_id"),
                  now,
                  until);
            }
            b.jdbc.update(
                "UPDATE seat_entitlement SET status='REVOKED',closed_at=?,version=version+1 WHERE id=?",
                now,
                current.get("id"));
            revoked = Business.number(current, "version") + 1;
            b.jdbc.update(
                "UPDATE long_offer SET status='CANCELED',version=version+1 WHERE batch_id=? AND user_id=? AND status='OPEN'",
                record.get("batch_id"),
                record.get("user_id"));
            b.jdbc.update(
                "UPDATE waitlist_entry SET status='EXITED',version=version+1 WHERE batch_id=? AND user_id=? AND status='ACTIVE'",
                record.get("batch_id"),
                record.get("user_id"));
            b.jdbc.update(
                "UPDATE preparation_application SET status='EXCLUDED',reason=? WHERE batch_id=? AND user_id=? AND status='SUBMITTED'",
                "GOVERNANCE_CASE:" + id,
                record.get("batch_id"),
                record.get("user_id"));
            allocationHistory(
                record,
                actor.id(),
                "GOVERNANCE_REVOKED",
                Map.of("case", id, "entitlement", current.get("id"), "reason", body.reason()));
          } else if (body.penaltyUntil() != null) throw DomainException.invalid("撤销通知不能附带处罚。");
          String state = body.action().equals("REVOKE") ? "REVOKED" : "DISMISSED";
          b.jdbc.update(
              "UPDATE governance_case SET status=?,decided_by=?,decided_at=?,decision_reason=?,revoked_version=?,appeal_until=?,version=version+1 WHERE id=?",
              state,
              actor.id(),
              now,
              body.reason(),
              revoked,
              revoked == null ? null : now.plusDays(7),
              id);
          history(id, actor.id(), state, body.reason());
          b.audit(actor.id(), "GOVERNANCE_" + state, "GOVERNANCE", id, requestId);
          notify(record, state, "处理结果：" + state + "。请查看私有理由与申诉期限；正式抽签历史保留。");
          if (revoked != null && !b.now().isBefore(Business.date(current, "ends_at")))
            throw Business.conflict("CYCLE_ENDED", "裁决写入跨过原权周期，整次收回未生效。");
          return detail(actor, id);
        });
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public Map<String, Object> appeal(
      AuthService.Session session, long id, String key, Statement body, String requestId) {
    var actor = lock(session, row(id), false);
    return b.once(
        actor,
        key,
        "governance.appeal:" + id,
        body,
        () -> {
          var record = row(id);
          Business.version(Business.number(record, "version"), body.version());
          Business.text(body.message(), 3000, true);
          if (!record.get("status").equals("REVOKED")
              || !b.now().isBefore(Business.date(record, "appeal_until")))
            throw Business.conflict("APPEAL_WINDOW_CLOSED", "当前不在本案的7天申诉窗口。");
          b.jdbc.update(
              "UPDATE governance_case SET status='APPEALED',appealed_at=?,appeal_reason=?,version=version+1 WHERE id=?",
              b.now(),
              body.message(),
              id);
          history(id, actor.id(), "APPEALED", body.message());
          b.audit(actor.id(), "GOVERNANCE_APPEALED", "GOVERNANCE", id, requestId);
          notify(record, "appeal", "申诉已保存，等待另一名范围管理员复核。申诉不自动恢复使用权。");
          if (!b.now().isBefore(Business.date(record, "appeal_until")))
            throw Business.conflict("APPEAL_WINDOW_CLOSED", "申诉写入跨过期限，整次操作未生效。");
          return detail(actor, id);
        });
  }

  private String cannotRestore(Map<String, Object> record) {
    long user = Business.number(record, "user_id"),
        batchId = Business.number(record, "batch_id"),
        space = Business.number(record, "space_id");
    var current = entitlement(Business.number(record, "entitlement_id"));
    var batch = b.jdbc.queryForMap("SELECT * FROM preparation_batch WHERE id=?", batchId);
    var now = b.now();
    if (!batch.get("status").equals("RESULT_PUBLISHED")
        || !now.isBefore(Business.date(current, "ends_at"))) return "CYCLE_NOT_ACTIVE";
    if (!current.get("status").equals("REVOKED")
        || Business.number(current, "version") != Business.number(record, "revoked_version"))
      return "ORIGINAL_RIGHT_CHANGED";
    String eligible = eligibility.reason(user);
    if (eligible != null) return eligible;
    if (b.jdbc.queryForObject(
            "SELECT COUNT(*) FROM preparation_application WHERE batch_id=? AND user_id=? AND status='WITHDRAWN'",
            Integer.class,
            batchId,
            user)
        > 0) return "USER_EXITED";
    var start = Business.date(current, "starts_at");
    if (start.isBefore(now)) start = now;
    var end = Business.date(current, "ends_at");
    if (b.jdbc.queryForObject(
            "SELECT COUNT(*) FROM preparation_application a JOIN preparation_batch t ON a.batch_id=t.id WHERE a.user_id=? AND a.batch_id<>? AND a.status='SUBMITTED' AND t.status IN ('OPEN','FROZEN','RESULT_PUBLISHED') AND t.cycle_starts_at<? AND t.cycle_ends_at>?",
            Integer.class,
            user,
            batchId,
            end,
            start)
        > 0) return "OTHER_LONG_APPLICATION";
    if (b.jdbc.queryForObject(
            "SELECT COUNT(*) FROM seat_entitlement WHERE user_id=? AND status='ACTIVE' AND starts_at<? AND ends_at>?",
            Integer.class,
            user,
            end,
            start)
        > 0) return "OTHER_LONG_RIGHT";
    if (b.jdbc.queryForObject(
            "SELECT COUNT(*) FROM long_offer o JOIN preparation_batch t ON o.batch_id=t.id WHERE o.user_id=? AND o.status='OPEN' AND o.deadline>? AND t.status='RESULT_PUBLISHED' AND t.cycle_starts_at<? AND t.cycle_ends_at>?",
            Integer.class,
            user,
            now,
            end,
            start)
        > 0) return "OTHER_LONG_OFFER";
    var seats =
        b.jdbc.queryForList(
            "SELECT s.*,p.space_version FROM space s JOIN preparation_pool p ON p.space_id=s.id WHERE p.batch_id=? AND s.id=?",
            batchId,
            space);
    if (seats.isEmpty()
        || !Boolean.TRUE.equals(seats.get(0).get("active"))
        || !seats.get(0).get("kind").equals("SEAT")
        || !Set.of("BOOKABLE", "PREPARATION").contains(seats.get(0).get("use_mode"))
        || Business.number(seats.get(0), "version")
            != Business.number(seats.get(0), "space_version")) return "ORIGINAL_SPACE_CHANGED";
    var conditions = json.readTree(seats.get(0).get("profile_json").toString()).path("conditions");
    for (String key :
        List.of("outletCondition", "deskCondition", "lightCondition", "environmentCondition"))
      if (Set.of("BROKEN", "REPAIRING").contains(conditions.path(key).asString()))
        return "ORIGINAL_FACILITY_UNSAFE";
    try {
      rights.personalFree(user, start, end);
      rights.freeForRestoration(space, start, end, batchId, Business.number(current, "id"));
    } catch (DomainException e) {
      if (e.status() != 409) throw e;
      return e.code();
    }
    return null;
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public Map<String, Object> review(
      AuthService.Session session, long id, String key, Review body, String requestId) {
    var actor = lock(session, row(id), true);
    return b.once(
        actor,
        key,
        "governance.review:" + id,
        body,
        () -> {
          var record = row(id);
          Business.version(Business.number(record, "version"), body.version());
          Business.text(body.reason(), 1500, true);
          if (!record.get("status").equals("APPEALED"))
            throw Business.conflict("GOVERNANCE_STATE_CONFLICT", "案件当前没有待处理申诉。");
          if (actor.id() == Business.number(record, "decided_by"))
            throw Business.conflict("INDEPENDENT_REVIEW_REQUIRED", "原裁决人不能处理自己的申诉结论。");
          if (!Set.of("UPHOLD", "OVERTURN").contains(Objects.toString(body.action(), "")))
            throw DomainException.invalid("请选择维持或纠正裁决。");
          String state = "UPHELD", reason = null;
          if (body.action().equals("OVERTURN")) {
            b.jdbc.update(
                "UPDATE long_application_penalty SET status='OVERTURNED',revoked_at=? WHERE case_id=? AND status='ACTIVE'",
                b.now(),
                id);
            reason = cannotRestore(record);
            state = reason == null ? "RESTORED" : "CORRECTED_UNAVAILABLE";
            if (reason == null) {
              b.jdbc.update(
                  "UPDATE seat_entitlement SET status='ACTIVE',closed_at=NULL,version=version+1 WHERE id=?",
                  record.get("entitlement_id"));
              b.jdbc.update(
                  "UPDATE preparation_application SET status='SUBMITTED',reason=NULL WHERE batch_id=? AND user_id=? AND status='EXCLUDED' AND reason=?",
                  record.get("batch_id"),
                  record.get("user_id"),
                  "GOVERNANCE_CASE:" + id);
            } else
              b.jdbc.update(
                  "UPDATE preparation_application SET reason=? WHERE batch_id=? AND user_id=? AND status='EXCLUDED' AND reason=?",
                  "GOVERNANCE_CORRECTED_UNAVAILABLE",
                  record.get("batch_id"),
                  record.get("user_id"),
                  "GOVERNANCE_CASE:" + id);
            allocationHistory(
                record,
                actor.id(),
                "GOVERNANCE_" + state,
                Map.of(
                    "case",
                    id,
                    "entitlement",
                    record.get("entitlement_id"),
                    "reason",
                    reason == null ? "AVAILABLE" : reason));
          }
          b.jdbc.update(
              "UPDATE governance_case SET status=?,reviewed_by=?,reviewed_at=?,review_reason=?,restoration_reason=?,version=version+1 WHERE id=?",
              state,
              actor.id(),
              b.now(),
              body.reason(),
              reason,
              id);
          history(
              id, actor.id(), state, body.reason() + (reason == null ? "" : "；未恢复原因：" + reason));
          b.audit(actor.id(), "GOVERNANCE_" + state, "GOVERNANCE", id, requestId);
          notify(
              record,
              "review",
              "申诉结果："
                  + state
                  + "。"
                  + (state.equals("CORRECTED_UNAVAILABLE")
                      ? "本案资格已纠正，当前原位不能恢复，可选择真实其他资源。"
                      : "请查看原使用权和私有处理依据。"));
          if (state.equals("RESTORED")
              && !b.now()
                  .isBefore(
                      Business.date(
                          entitlement(Business.number(record, "entitlement_id")), "ends_at")))
            throw Business.conflict("CYCLE_ENDED", "恢复写入跨过原周期，整次纠错未生效。");
          return detail(actor, id);
        });
  }
}
