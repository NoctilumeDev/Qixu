package dev.noctilume.qixu.preparation;

import dev.noctilume.qixu.common.*;
import dev.noctilume.qixu.identity.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
public class BatchFreezer {
  private final PreparationBatches p;

  public BatchFreezer(PreparationBatches p) {
    this.p = p;
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public Map<String, Object> freeze(
      AuthService.Session session, long id, String key, PreparationBatches.Action body) {
    var batch = p.batch(id, true);
    var applications = applications(id);
    p.rights().lockFloors(p.floors(id));
    var users =
        new ArrayList<>(applications.stream().map(a -> Business.number(a, "user_id")).toList());
    users.add(session.actor().id());
    p.business().users(users);
    var actor = p.business().current(session);
    p.scope(actor, id);
    return p.business()
        .once(
            actor,
            key,
            "preparation.freeze:" + id,
            body,
            () -> {
              Business.version(Business.number(batch, "version"), body.version());
              if (!"FREEZE".equals(body.action())) throw DomainException.invalid("冻结操作不正确。");
              PreparationBatches.state(batch, "OPEN");
              if (p.business().now().isBefore(Business.date(batch, "closes_at")))
                throw Business.conflict("FREEZE_NOT_DUE", "申请窗口尚未结束，不能提前冻结。");
              freezeLocked(batch, applications, actor.id());
              return p.detail(id);
            });
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public void freezeDue(long id) {
    var batch = p.batch(id, true);
    if (!"OPEN".equals(batch.get("status"))
        || p.business().now().isBefore(Business.date(batch, "closes_at"))) return;
    var applications = applications(id);
    p.rights().lockFloors(p.floors(id));
    p.business().users(applications.stream().map(a -> Business.number(a, "user_id")).toList());
    freezeLocked(batch, applications, null);
  }

  private List<Map<String, Object>> applications(long id) {
    return p.business()
        .jdbc
        .queryForList(
            "SELECT a.*,v.preferences_json,v.keep_waitlist,v.accepted_at FROM preparation_application a JOIN application_version v ON a.id=v.application_id AND a.current_version=v.revision WHERE a.batch_id=? AND a.status='SUBMITTED' ORDER BY a.anonymous_id",
            id);
  }

  static LinkedHashMap<String, Object> ordered(Object... pairs) {
    var result = new LinkedHashMap<String, Object>();
    for (int i = 0; i < pairs.length; i += 2) result.put(pairs[i].toString(), pairs[i + 1]);
    return result;
  }

  private void freezeLocked(
      Map<String, Object> batch, List<Map<String, Object>> applications, Long actor) {
    long id = Business.number(batch, "id");
    var now = p.business().now();
    if (!now.isBefore(Business.date(batch, "freeze_deadline"))
        || !now.isBefore(Business.date(batch, "random_at"))) {
      p.failLocked(batch, "FREEZE_DEADLINE_MISSED", actor);
      return;
    }
    var pool = p.pool(id);
    if (pool.isEmpty() || pool.size() > 400 || applications.size() > 200) {
      p.failLocked(batch, "INPUT_LIMIT_OR_MISSING_POOL", actor);
      return;
    }
    for (var seat : pool) {
      if (!Boolean.TRUE.equals(seat.get("active"))
          || Business.number(seat, "space_version")
              != Business.number(seat, "current_space_version")) {
        p.failLocked(batch, "INPUT_RESOURCE_CHANGED", actor);
        return;
      }
      if (!p.rights()
          .conflicts(
              Business.number(seat, "space_id"),
              Business.number(seat, "floor_id"),
              Business.date(batch, "cycle_starts_at"),
              Business.date(batch, "cycle_ends_at"),
              -1,
              id)
          .isEmpty()) {
        p.failLocked(batch, "INPUT_RESOURCE_CONFLICT", actor);
        return;
      }
    }
    var candidates = new ArrayList<AllocationGraph.Candidate>();
    var included = new ArrayList<Map<String, Object>>();
    var versions = new ArrayList<Map<String, Object>>();
    var seatIds = new HashSet<>(pool.stream().map(s -> Business.number(s, "space_id")).toList());
    for (var application : applications) {
      long user = Business.number(application, "user_id"),
          applicationId = Business.number(application, "id");
      String reason = null;
      if (p.eligibility().reason(user) != null) reason = "ELIGIBILITY_INVALID_AT_FREEZE";
      else
        try {
          p.checkOtherParticipation(
              user,
              id,
              Business.date(batch, "cycle_starts_at"),
              Business.date(batch, "cycle_ends_at"));
        } catch (DomainException e) {
          reason = e.code();
        }
      if (reason != null) {
        p.business()
            .jdbc
            .update(
                "UPDATE preparation_application SET status='EXCLUDED',reason=? WHERE id=?",
                reason,
                applicationId);
        p.event(id, user, actor, "EXCLUDED_AT_FREEZE", Map.of("reason", reason));
        p.business()
            .notify(
                user,
                "application:" + applicationId + ":excluded",
                "本轮申请未进入分配",
                "资格或周期条件在冻结时不成立，可在我的申请查看明确原因及处置入口。",
                "BATCH",
                id);
        continue;
      }
      var preferences =
          p.json()
              .readValue(
                  application.get("preferences_json").toString(),
                  PreparationBatches.Preference[].class);
      var edges = new ArrayList<AllocationGraph.Edge>();
      for (var preference : preferences) {
        if (!seatIds.contains(preference.seatId())) {
          p.failLocked(batch, "INPUT_PREFERENCE_CHANGED", actor);
          return;
        }
        edges.add(new AllocationGraph.Edge("s" + preference.seatId(), preference.rank()));
      }
      edges.sort(Comparator.comparing(AllocationGraph.Edge::seat));
      String anonymous = application.get("anonymous_id").toString();
      candidates.add(new AllocationGraph.Candidate(anonymous, List.copyOf(edges)));
      included.add(application);
      versions.add(
          ordered(
              "candidate",
              anonymous,
              "revision",
              Business.number(application, "current_version"),
              "keepWaitlist",
              application.get("keep_waitlist")));
    }
    var graph =
        new AllocationGraph.Input(
            AllocationGraph.VERSION,
            pool.stream().map(s -> "s" + Business.number(s, "space_id")).sorted().toList(),
            List.copyOf(candidates));
    var snapshot =
        ordered(
            "schema",
            "qixu-allocation-input/2",
            "batch",
            batch.get("public_id"),
            "algorithm",
            AllocationGraph.VERSION,
            "cycle",
            ordered(
                "start",
                Business.iso(Business.date(batch, "cycle_starts_at")),
                "end",
                Business.iso(Business.date(batch, "cycle_ends_at"))),
            "schedule",
            ordered(
                "close",
                Business.iso(Business.date(batch, "closes_at")),
                "freezeDeadline",
                Business.iso(Business.date(batch, "freeze_deadline")),
                "randomAt",
                Business.iso(Business.date(batch, "random_at")),
                "resultDeadline",
                Business.iso(Business.date(batch, "result_deadline")),
                "confirmationDeadline",
                Business.iso(Business.date(batch, "confirmation_deadline")),
                "promotionUntil",
                Business.iso(Business.date(batch, "promotion_until")),
                "promotionSeconds",
                batch.get("promotion_seconds")),
            "source",
            ordered("chain", RandomnessSource.CHAIN, "round", batch.get("source_round")),
            "seats",
            pool.stream()
                .map(
                    s ->
                        ordered(
                            "id",
                            "s" + Business.number(s, "space_id"),
                            "floor",
                            s.get("floor_id"),
                            "code",
                            s.get("code"),
                            "name",
                            s.get("name"),
                            "version",
                            s.get("space_version")))
                .toList(),
            "versions",
            versions,
            "graph",
            graph);
    String bytes = p.frozenJson().encode(snapshot), hash = Digests.sha256(bytes);
    if (bytes.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 4_194_304) {
      p.failLocked(batch, "INPUT_BYTES_LIMIT", actor);
      return;
    }
    p.business()
        .jdbc
        .update(
            "INSERT INTO frozen_input(batch_id,input_hash,input_bytes,frozen_at) VALUES(?,?,?,?)",
            id,
            hash,
            bytes,
            now);
    for (var application : included) {
      long applicationId = Business.number(application, "id"),
          revision = Business.number(application, "current_version");
      p.business()
          .jdbc
          .update(
              "UPDATE preparation_application SET frozen_version=? WHERE id=?",
              revision,
              applicationId);
      p.business()
          .jdbc
          .update(
              "INSERT INTO frozen_person(batch_id,anonymous_id,application_id,user_id,revision) VALUES(?,?,?,?,?)",
              id,
              application.get("anonymous_id"),
              applicationId,
              Business.number(application, "user_id"),
              revision);
    }
    var acceptedAt = p.business().now();
    if (!acceptedAt.isBefore(Business.date(batch, "freeze_deadline"))
        || !acceptedAt.isBefore(Business.date(batch, "random_at")))
      throw Business.conflict("FREEZE_DEADLINE_PASSED", "冻结写入跨过公开期限，整次冻结未生效。");
    p.business()
        .jdbc
        .update("UPDATE preparation_batch SET status='FROZEN',version=version+1 WHERE id=?", id);
    p.event(
        id,
        null,
        actor,
        "INPUT_FROZEN",
        ordered(
            "inputHash",
            hash,
            "included",
            included.size(),
            "pool",
            pool.size(),
            "sourceRound",
            batch.get("source_round")));
  }

  @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
  public Map<String, Object> verification(String publicId) {
    if (publicId == null
        || !publicId.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"))
      throw DomainException.missing();
    var batches =
        p.business()
            .jdbc
            .queryForList("SELECT id FROM preparation_batch WHERE public_id=?", publicId);
    if (batches.isEmpty()) throw DomainException.missing();
    long id = Business.number(batches.get(0), "id");
    var inputs =
        p.business()
            .jdbc
            .queryForList(
                "SELECT input_hash,input_bytes,frozen_at FROM frozen_input WHERE batch_id=?", id);
    if (inputs.isEmpty()) throw DomainException.missing();
    var result = p.business().view(inputs.get(0));
    result.put("batch", publicId);
    result.put("phase", p.view(p.batch(id, false)).get("effectivePhase"));
    var formal =
        p.business()
            .jdbc
            .queryForList(
                "SELECT r.input_hash,r.output_hash,r.maximum_count,r.candidate_count,r.published_at,u.proof_json,u.output_json FROM allocation_result r JOIN allocation_run u ON r.batch_id=u.batch_id WHERE r.batch_id=?",
                id);
    // Candidate runs remain private until the entire official result has committed.
    if (formal.isEmpty()) result.put("result", Map.of());
    else {
      var published = p.business().view(formal.get(0));
      published.put(
          "outputBytes", p.frozenJson().normalize(published.remove("output_json").toString()));
      published.put(
          "proof", p.json().readValue(published.remove("proof_json").toString(), Map.class));
      result.put("result", published);
    }
    return result;
  }
}
