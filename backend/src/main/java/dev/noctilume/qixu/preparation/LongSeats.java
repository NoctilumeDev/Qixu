package dev.noctilume.qixu.preparation;

import dev.noctilume.qixu.common.*;
import dev.noctilume.qixu.identity.*;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** All transitions coordinate batch -> sorted floors -> sorted people. An offer never releases an old right. */
@Service
public class LongSeats {
  private final PreparationBatches p;

  public record Response(long version, String action, String impactHash) {}

  public LongSeats(PreparationBatches p) {
    this.p = p;
  }

  private Map<String, Object> locked(long id, Long actor) {
    var batch = p.batch(id, true);
    p.rights().lockFloors(p.floors(id));
    var users =
        new ArrayList<>(
            p
                .business()
                .jdbc
                .queryForList("SELECT user_id FROM preparation_application WHERE batch_id=?", id)
                .stream()
                .map(r -> Business.number(r, "user_id"))
                .toList());
    if (actor != null) users.add(actor);
    p.business().users(users);
    return batch;
  }

  private List<Map<String, Object>> rights(long batch, long user) {
    return p.business()
        .jdbc
        .queryForList(
            "SELECT e.*,o.preference_rank FROM seat_entitlement e JOIN long_offer o ON e.offer_id=o.id WHERE e.batch_id=? AND e.user_id=? AND e.status='ACTIVE' AND e.ends_at>?",
            batch,
            user,
            p.business().now());
  }

  private Map<String, Object> offer(long id) {
    var rows = p.business().jdbc.queryForList("SELECT * FROM long_offer WHERE id=?", id);
    if (rows.isEmpty()) throw DomainException.missing();
    return rows.get(0);
  }

  private long offerBatch(long offer, long user) {
    var row = offer(offer);
    if (Business.number(row, "user_id") != user) throw DomainException.missing();
    return Business.number(row, "batch_id");
  }

  @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
  public Map<String, Object> mine(long user, long batch) {
    return p.mine(user, batch);
  }

  @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
  public Map<String, Object> minePage(long user, int page) {
    if (page < 1 || page > 10000) throw DomainException.invalid("页码须在1至10000之间。");
    var jdbc = p.business().jdbc;
    long total =
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM preparation_application WHERE user_id=?", Long.class, user);
    var items =
        jdbc
            .queryForList(
                "SELECT batch_id FROM preparation_application WHERE user_id=? ORDER BY id DESC LIMIT 20 OFFSET ?",
                user,
                (page - 1) * 20)
            .stream()
            .map(row -> p.mine(user, Business.number(row, "batch_id")))
            .toList();
    return Map.of("items", items, "total", total, "page", page, "size", 20);
  }

  private void published(Map<String, Object> batch) {
    PreparationBatches.state(batch, "RESULT_PUBLISHED");
    if (!p.business().now().isBefore(Business.date(batch, "cycle_ends_at")))
      throw Business.conflict("CYCLE_ENDED", "本周期已结束，旧要约不能恢复使用权。");
  }

  private void checkSeat(Map<String, Object> batch, Map<String, Object> offer) {
    long id = Business.number(batch, "id"),
        space = Business.number(offer, "space_id"),
        floor = Business.number(offer, "floor_id");
    var current = p.rights().space(space);
    var frozen =
        p.business()
            .jdbc
            .queryForMap(
                "SELECT space_version FROM preparation_pool WHERE batch_id=? AND space_id=?",
                id,
                space);
    if (Business.number(current, "version") != Business.number(frozen, "space_version"))
      throw Business.conflict("RESOURCE_CHANGED", "该席位事实已变化，请等待管理员核实，原席位不会因此被释放。");
    var start = Business.date(batch, "cycle_starts_at");
    if (start.isBefore(p.business().now())) start = p.business().now();
    p.rights()
        .freeForOffer(
            Business.number(offer, "id"),
            Business.number(offer, "user_id"),
            start,
            Business.date(batch, "cycle_ends_at"),
            id,
            offer.get("upgrade_from_id") == null
                ? null
                : Business.number(offer, "upgrade_from_id"));
    if (p.business()
            .jdbc
            .queryForObject(
                "SELECT COUNT(*) FROM seat_entitlement WHERE batch_id=? AND space_id=? AND status='ACTIVE' AND ends_at>?",
                Integer.class,
                id,
                space,
                p.business().now())
        > 0) throw Business.conflict("LONG_SEAT_TAKEN", "该席位已有当前使用权，不能重复确认。");
  }

  private void personalShortFree(long user, LocalDateTime start, LocalDateTime end) {
    if (p.business()
            .jdbc
            .queryForObject(
                "SELECT COUNT(*) FROM short_reservation WHERE user_id=? AND starts_at<? AND ends_at>? AND ends_at>? AND (status='CHECKED_IN' OR (status='PENDING' AND check_in_deadline>?))",
                Integer.class,
                user,
                end,
                start,
                p.business().now(),
                p.business().now())
        > 0) throw Business.conflict("PERSONAL_CONFLICT", "该周期与你的普通短约冲突，请先明确调整短约；原要约和已有长期位仍保留。");
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public Map<String, Object> respond(
      AuthService.Session session, long offerId, String key, PreparationBatches.Action body) {
    return respondInside(session, offerId, key, new Response(body.version(), body.action(), null));
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public Map<String, Object> respond(
      AuthService.Session session, long offerId, String key, Response body) {
    return respondInside(session, offerId, key, body);
  }

  private Map<String, Object> respondInside(
      AuthService.Session session, long offerId, String key, Response body) {
    long batchId = offerBatch(offerId, session.actor().id());
    var batch = locked(batchId, session.actor().id());
    var actor = p.business().current(session);
    Object fingerprint =
        body.impactHash() == null
            ? new PreparationBatches.Action(body.version(), body.action())
            : body;
    return p.business()
        .once(
            actor,
            key,
            "preparation.offer:" + offerId,
            fingerprint,
            () -> {
              var row = offer(offerId);
              if (Business.number(row, "user_id") != actor.id()) throw DomainException.missing();
              Business.version(Business.number(row, "version"), body.version());
              published(batch);
              if (!Set.of("ACCEPT", "DECLINE").contains(body.action() == null ? "" : body.action()))
                throw DomainException.invalid("请选择接受或拒绝本次要约。");
              if (!"OPEN".equals(row.get("status")))
                throw Business.conflict("OFFER_STATE_CONFLICT", "要约已经处理，请查看原回执。");
              if (!p.business().now().isBefore(Business.date(row, "deadline")))
                throw Business.conflict("OFFER_EXPIRED", "要约已过公布的期限，不能按旧页面确认。");
              var application =
                  p.business()
                      .jdbc
                      .queryForMap(
                          "SELECT status FROM preparation_application WHERE batch_id=? AND user_id=?",
                          batchId,
                          actor.id());
              if (!"SUBMITTED".equals(application.get("status")))
                throw Business.conflict("APPLICATION_STATE_CONFLICT", "申请已退出或资格不成立。");
              Long entitlement = null;
              if (body.action().equals("ACCEPT")) {
                var impact = p.rights().offerImpact(offerId, actor.id());
                if (Boolean.TRUE.equals(impact.get("requiresAcknowledgement"))
                    && !Objects.equals(body.impactHash(), impact.get("impactHash")))
                  throw Business.conflict(
                      "STALE_OFFER_IMPACT", "请查看当前每段限制和临时安排后确认；摘要变化时旧确认不生效，原期限保持。");
                Business.student(actor);
                p.eligibility().require(actor.id());
                LocalDateTime start = Business.date(batch, "cycle_starts_at"),
                    end = Business.date(batch, "cycle_ends_at");
                var now = p.business().now();
                if (start.isBefore(now)) start = now;
                p.checkOtherParticipation(actor.id(), batchId, start, end);
                personalShortFree(actor.id(), start, end);
                checkSeat(batch, row);
                var old = rights(batchId, actor.id());
                Object upgrade = row.get("upgrade_from_id");
                if (upgrade == null && !old.isEmpty()
                    || upgrade != null
                        && (old.size() != 1
                            || Business.number(old.get(0), "id") != ((Number) upgrade).longValue()))
                  throw Business.conflict("UPGRADE_CHANGED", "原使用权已经变化，升级必须重新核实；不会释放任何旧席位。");
                if (upgrade != null)
                  p.business()
                      .jdbc
                      .update(
                          "UPDATE seat_entitlement SET status='REPLACED',closed_at=?,version=version+1 WHERE id=?",
                          now,
                          upgrade);
                p.business()
                    .jdbc
                    .update(
                        "UPDATE long_offer SET status='ACCEPTED',version=version+1 WHERE id=?",
                        offerId);
                entitlement =
                    p.business()
                        .insert(
                            "INSERT INTO seat_entitlement(offer_id,batch_id,user_id,space_id,floor_id,starts_at,ends_at) VALUES(?,?,?,?,?,?,?)",
                            offerId,
                            batchId,
                            actor.id(),
                            row.get("space_id"),
                            row.get("floor_id"),
                            start,
                            end);
                if (Business.number(row, "preference_rank") == 1)
                  p.business()
                      .jdbc
                      .update(
                          "UPDATE waitlist_entry SET status='FINISHED',version=version+1 WHERE batch_id=? AND user_id=? AND status='ACTIVE'",
                          batchId,
                          actor.id());
              } else
                p.business()
                    .jdbc
                    .update(
                        "UPDATE long_offer SET status='DECLINED',version=version+1 WHERE id=?",
                        offerId);
              p.event(
                  batchId,
                  actor.id(),
                  actor.id(),
                  body.action().equals("ACCEPT") ? "OFFER_ACCEPTED" : "OFFER_DECLINED",
                  Map.of(
                      "offer",
                      offerId,
                      "seat",
                      row.get("space_id"),
                      "upgrade",
                      row.get("upgrade_from_id") == null ? 0 : row.get("upgrade_from_id")));
              p.business()
                  .notify(
                      actor.id(),
                      "offer:" + offerId + ":" + body.action(),
                      body.action().equals("ACCEPT") ? "长期使用权已确认" : "本次要约已拒绝",
                      body.action().equals("ACCEPT")
                          ? "已取得明确使用权。若是升级，原席位在同一事务中释放；暂离不丢失使用权。"
                          : "仅拒绝这个席位，已有长期使用权保持；候补资格按你原先选择保留。",
                      "BATCH",
                      batchId);
              // A write that crossed the published cutoff cannot become a successful confirmation.
              var acceptedAt = p.business().now();
              if (!acceptedAt.isBefore(Business.date(row, "deadline"))
                  || !acceptedAt.isBefore(Business.date(batch, "cycle_ends_at")))
                throw Business.conflict("OFFER_EXPIRED", "确认写入跨过要约期限，整次操作未生效，原席位保持。");
              var result = p.mine(actor.id(), batchId);
              if (entitlement != null) result.put("confirmedEntitlementId", entitlement);
              return result;
            });
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public Map<String, Object> exit(
      AuthService.Session session, long id, String key, PreparationBatches.Action body) {
    var batch = locked(id, session.actor().id());
    var actor = p.business().current(session);
    return p.business()
        .once(
            actor,
            key,
            "preparation.exit:" + id,
            body,
            () -> {
              if (!"EXIT".equals(body.action())) throw DomainException.invalid("请选择明确退出本轮。");
              var rows =
                  p.business()
                      .jdbc
                      .queryForList(
                          "SELECT * FROM preparation_application WHERE batch_id=? AND user_id=?",
                          id,
                          actor.id());
              if (rows.isEmpty()) throw DomainException.missing();
              var application = rows.get(0);
              Business.version(Business.number(application, "current_version"), body.version());
              if (!Set.of("RESULT_PUBLISHED", "FAILED_NO_RESULT", "CANCELED", "CLOSED")
                      .contains(batch.get("status"))
                  && PreparationValidity.failure(batch, p.business().now()) == null)
                throw Business.conflict("BATCH_STATE_CONFLICT", "申请期请用撤回，冻结后需等明确结果，不能改冻结集合。");
              if ("WITHDRAWN".equals(application.get("status")))
                throw Business.conflict("APPLICATION_STATE_CONFLICT", "你已退出本轮。");
              p.business()
                  .jdbc
                  .update(
                      "UPDATE long_offer SET status='CANCELED',version=version+1 WHERE batch_id=? AND user_id=? AND status='OPEN'",
                      id,
                      actor.id());
              p.business()
                  .jdbc
                  .update(
                      "UPDATE seat_entitlement SET status='RELINQUISHED',closed_at=?,version=version+1 WHERE batch_id=? AND user_id=? AND status='ACTIVE'",
                      p.business().now(),
                      id,
                      actor.id());
              p.business()
                  .jdbc
                  .update(
                      "UPDATE waitlist_entry SET status='EXITED',version=version+1 WHERE batch_id=? AND user_id=? AND status='ACTIVE'",
                      id,
                      actor.id());
              p.business()
                  .jdbc
                  .update(
                      "UPDATE preparation_application SET status='WITHDRAWN' WHERE id=?",
                      application.get("id"));
              p.event(
                  id,
                  actor.id(),
                  actor.id(),
                  "CYCLE_EXITED",
                  Map.of("application", application.get("id")));
              p.business()
                  .notify(
                      actor.id(),
                      "batch:" + id + ":exit",
                      "已退出本轮长期席位",
                      "要约、候补及当前席位一并退出；原分配历史继续可复核。",
                      "BATCH",
                      id);
              return p.mine(actor.id(), id);
            });
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public Map<String, Object> exitWaitlist(
      AuthService.Session session, long id, String key, PreparationBatches.Action body) {
    locked(id, session.actor().id());
    var actor = p.business().current(session);
    return p.business()
        .once(
            actor,
            key,
            "preparation.waitlist-exit:" + id,
            body,
            () -> {
              if (!"EXIT_WAITLIST".equals(body.action()))
                throw DomainException.invalid("请选择只退出候补。");
              var rows =
                  p.business()
                      .jdbc
                      .queryForList(
                          "SELECT * FROM waitlist_entry WHERE batch_id=? AND user_id=?",
                          id,
                          actor.id());
              if (rows.isEmpty()) throw DomainException.missing();
              var row = rows.get(0);
              Business.version(Business.number(row, "version"), body.version());
              if (!"ACTIVE".equals(row.get("status")))
                throw Business.conflict("WAITLIST_STATE_CONFLICT", "候补已经结束。");
              // A pending upgrade may be canceled, but an initial offer and an existing seat remain
              // owned by the student.
              p.business()
                  .jdbc
                  .update(
                      "UPDATE long_offer SET status='CANCELED',version=version+1 WHERE batch_id=? AND user_id=? AND status='OPEN' AND upgrade_from_id IS NOT NULL",
                      id,
                      actor.id());
              p.business()
                  .jdbc
                  .update(
                      "UPDATE waitlist_entry SET status='EXITED',version=version+1 WHERE batch_id=? AND user_id=?",
                      id,
                      actor.id());
              p.event(id, actor.id(), actor.id(), "WAITLIST_EXITED", Map.of());
              p.business()
                  .notify(
                      actor.id(),
                      "batch:" + id + ":waitlist-exit",
                      "已退出候补",
                      "已确认的当前长期席位及初始要约不受影响。",
                      "BATCH",
                      id);
              return p.mine(actor.id(), id);
            });
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public void maintain(long id) {
    var batch = locked(id, null);
    if (!"RESULT_PUBLISHED".equals(batch.get("status"))) return;
    var now = p.business().now();
    for (var row :
        p.business()
            .jdbc
            .queryForList(
                "SELECT * FROM long_offer WHERE batch_id=? AND status='OPEN' ORDER BY id", id)) {
      long user = Business.number(row, "user_id"), offer = Business.number(row, "id");
      String state = null;
      if (p.eligibility().reason(user) != null) state = "INVALID";
      else if (!now.isBefore(Business.date(row, "deadline"))
          || !now.isBefore(Business.date(batch, "cycle_ends_at"))) state = "EXPIRED";
      if (state != null) {
        p.business()
            .jdbc
            .update("UPDATE long_offer SET status=?,version=version+1 WHERE id=?", state, offer);
        p.event(id, user, null, "OFFER_" + state, Map.of("offer", offer));
        p.business()
            .notify(
                user,
                "offer:" + offer + ":" + state,
                "长期席位要约已结束",
                "本次要约因到期或资格变化结束；升级前的旧席位保持，不因通知延迟延长期限。",
                "BATCH",
                id);
      }
    }
    if (!now.isBefore(Business.date(batch, "cycle_ends_at"))) {
      for (var right :
          p.business()
              .jdbc
              .queryForList(
                  "SELECT * FROM seat_entitlement WHERE batch_id=? AND status='ACTIVE' ORDER BY id",
                  id)) {
        p.business()
            .jdbc
            .update(
                "UPDATE seat_entitlement SET status='EXPIRED',closed_at=ends_at,version=version+1 WHERE id=?",
                right.get("id"));
        p.business()
            .notify(
                Business.number(right, "user_id"),
                "entitlement:" + right.get("id") + ":end",
                "长期使用周期已结束",
                "使用权已按原公布周期结束，历史与回执继续可查。",
                "BATCH",
                id);
      }
      p.business()
          .jdbc
          .update(
              "UPDATE waitlist_entry SET status='FINISHED',version=version+1 WHERE batch_id=? AND status='ACTIVE'",
              id);
      p.business()
          .jdbc
          .update("UPDATE preparation_batch SET status='CLOSED',version=version+1 WHERE id=?", id);
      p.event(id, null, null, "CYCLE_CLOSED", Map.of());
      return;
    }
    if (!now.isBefore(Business.date(batch, "promotion_until"))) {
      for (var wait :
          p.business()
              .jdbc
              .queryForList(
                  "SELECT user_id FROM waitlist_entry WHERE batch_id=? AND status='ACTIVE'", id))
        p.business()
            .notify(
                Business.number(wait, "user_id"),
                "batch:" + id + ":waitlist-end",
                "本轮候补窗口已结束",
                "本轮不再发放新要约，已有长期使用权仍按原周期有效。可选择真实开放的其他空间。",
                "BATCH",
                id);
      p.business()
          .jdbc
          .update(
              "UPDATE waitlist_entry SET status='FINISHED',version=version+1 WHERE batch_id=? AND status='ACTIVE'",
              id);
      return;
    }
    var queue =
        p.business()
            .jdbc
            .queryForList(
                "SELECT w.*,f.revision FROM waitlist_entry w JOIN frozen_person f ON w.batch_id=f.batch_id AND w.user_id=f.user_id JOIN preparation_application a ON w.application_id=a.id WHERE w.batch_id=? AND w.status='ACTIVE' AND a.status='SUBMITTED' ORDER BY w.lottery_position",
                id);
    if (queue.isEmpty()) return;
    var available = new TreeMap<String, Map<String, Object>>();
    for (var seat : p.pool(id)) {
      long space = Business.number(seat, "space_id");
      if (!Boolean.TRUE.equals(seat.get("active"))
          || Business.number(seat, "space_version")
              != Business.number(seat, "current_space_version")) continue;
      var at = p.business().now();
      if (!at.isBefore(Business.date(batch, "promotion_until"))) break;
      var start = Business.date(batch, "cycle_starts_at");
      if (start.isBefore(at)) start = at;
      if (!p.rights()
          .conflicts(
              space,
              Business.number(seat, "floor_id"),
              start,
              Business.date(batch, "cycle_ends_at"),
              -1,
              id)
          .isEmpty()) continue;
      if (p.business()
              .jdbc
              .queryForObject(
                  "SELECT (SELECT COUNT(*) FROM long_offer WHERE batch_id=? AND space_id=? AND status='OPEN')+(SELECT COUNT(*) FROM seat_entitlement WHERE batch_id=? AND space_id=? AND status='ACTIVE' AND ends_at>?)",
                  Integer.class,
                  id,
                  space,
                  id,
                  space,
                  at)
          > 0) continue;
      available.put("s" + space, seat);
    }
    if (available.isEmpty() || queue.isEmpty()) return;
    var graphPeople = new ArrayList<AllocationGraph.Candidate>();
    var mapping = new HashMap<String, Map<String, Object>>();
    var oldRights = new HashMap<String, Map<String, Object>>();
    for (var person : queue) {
      long user = Business.number(person, "user_id"),
          application = Business.number(person, "application_id");
      String candidate = "c" + user;
      if (p.eligibility().reason(user) != null) continue;
      if (p.business()
              .jdbc
              .queryForObject(
                  "SELECT COUNT(*) FROM long_offer WHERE batch_id=? AND user_id=? AND status='OPEN'",
                  Integer.class,
                  id,
                  user)
          > 0) continue;
      var previous = rights(id, user);
      if (previous.size() > 1) throw new IllegalStateException("Multiple long rights");
      int best =
          previous.isEmpty()
              ? Integer.MAX_VALUE
              : Math.toIntExact(Business.number(previous.get(0), "preference_rank"));
      var rejected =
          new HashSet<>(
              p
                  .business()
                  .jdbc
                  .queryForList(
                      "SELECT space_id FROM long_offer WHERE batch_id=? AND user_id=? AND status IN ('DECLINED','EXPIRED','INVALID','CANCELED')",
                      id,
                      user)
                  .stream()
                  .map(r -> Business.number(r, "space_id"))
                  .toList());
      var v =
          p.business()
              .jdbc
              .queryForMap(
                  "SELECT preferences_json FROM application_version WHERE application_id=? AND revision=?",
                  application,
                  person.get("revision"));
      var edges = new ArrayList<AllocationGraph.Edge>();
      for (var pref :
          p.json()
              .readValue(
                  v.get("preferences_json").toString(), PreparationBatches.Preference[].class))
        if (available.containsKey("s" + pref.seatId())
            && pref.rank() < best
            && !rejected.contains(pref.seatId()))
          edges.add(new AllocationGraph.Edge("s" + pref.seatId(), pref.rank()));
      var start = Business.date(batch, "cycle_starts_at");
      if (start.isBefore(p.business().now())) start = p.business().now();
      try {
        p.checkOtherParticipation(user, id, start, Business.date(batch, "cycle_ends_at"));
      } catch (DomainException e) {
        continue;
      }
      // Use the original anonymous ID for stable same-rank ties, never an internal identity number.
      candidate =
          p.business()
              .jdbc
              .queryForObject(
                  "SELECT anonymous_id FROM frozen_person WHERE batch_id=? AND user_id=?",
                  String.class,
                  id,
                  user);
      graphPeople.add(new AllocationGraph.Candidate(candidate, List.copyOf(edges)));
      mapping.put(candidate, person);
      if (!previous.isEmpty()) oldRights.put(candidate, previous.get(0));
    }
    if (graphPeople.isEmpty()) return;
    var graph =
        new AllocationGraph.Input(
            AllocationGraph.VERSION, List.copyOf(available.keySet()), List.copyOf(graphPeople));
    var original =
        p.business()
            .jdbc
            .queryForMap("SELECT output_json FROM allocation_run WHERE batch_id=?", id);
    var seed =
        p.json()
            .readValue(original.get("output_json").toString(), AllocationGraph.Result.class)
            .seedDigest();
    var result =
        AllocationGraph.match(
            graph,
            graphPeople.stream().map(AllocationGraph.Candidate::id).toList(),
            seed,
            System.nanoTime() + 2_000_000_000L);
    var accepted = p.business().now();
    var deadline = accepted.plusSeconds(Business.number(batch, "promotion_seconds"));
    if (deadline.isAfter(Business.date(batch, "promotion_until")))
      deadline = Business.date(batch, "promotion_until");
    if (!accepted.isBefore(deadline)) return;
    p.event(
        id,
        null,
        null,
        "PROMOTION_ROUND",
        Map.of(
            "inputHash",
            Digests.sha256(p.frozenJson().encode(graph)),
            "outputHash",
            Digests.sha256(p.frozenJson().encode(result)),
            "count",
            result.maximum(),
            "graph",
            graph,
            "result",
            result));
    for (var assignment : result.assignments()) {
      if (assignment.seat() == null) continue;
      var person = mapping.get(assignment.candidate());
      var seat = available.get(assignment.seat());
      var previous = oldRights.get(assignment.candidate());
      long user = Business.number(person, "user_id");
      long offer =
          p.business()
              .insert(
                  "INSERT INTO long_offer(batch_id,application_id,user_id,space_id,floor_id,preference_rank,deadline,upgrade_from_id,created_at) VALUES(?,?,?,?,?,?,?,?,?)",
                  id,
                  person.get("application_id"),
                  user,
                  seat.get("space_id"),
                  seat.get("floor_id"),
                  assignment.rank(),
                  deadline,
                  previous == null ? null : previous.get("id"),
                  accepted);
      p.event(
          id,
          user,
          null,
          "PROMOTION_OFFERED",
          Map.of(
              "offer",
              offer,
              "seat",
              seat.get("space_id"),
              "position",
              person.get("lottery_position")));
      p.business()
          .notify(
              user,
              "offer:" + offer + ":promotion",
              "长期席位候补已轮到你",
              "请在我的申请中查看具体席位和确认期限。若已有兜底位，只有确认成功才会原子换位。",
              "BATCH",
              id);
    }
    if (!p.business().now().isBefore(deadline))
      throw Business.conflict("PROMOTION_WINDOW_PASSED", "递补写入跨过期限，未产生新的要约。");
  }
}
