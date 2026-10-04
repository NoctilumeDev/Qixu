package dev.noctilume.qixu.spaces;

import dev.noctilume.qixu.common.*;
import dev.noctilume.qixu.preparation.PreparationValidity;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

/** A temporal projection of independent facts, never a second owner/state table. */
@Service
public class SpatialFacts {
  private final Business b;
  private final JsonMapper json;

  public SpatialFacts(Business b, JsonMapper json) {
    this.b = b;
    this.json = json;
  }

  public record Root(
      String kind,
      long id,
      long user,
      long space,
      long floor,
      long batch,
      long version,
      long secondaryVersion,
      LocalDateTime start,
      LocalDateTime end,
      String status,
      Map<String, Object> fact) {}

  public record Limit(
      long id,
      long space,
      long floor,
      String kind,
      Long venue,
      LocalDateTime start,
      LocalDateTime end,
      Map<String, Object> fact) {}

  public record Arrangement(
      long block,
      long offer,
      long from,
      Long target,
      LocalDateTime start,
      LocalDateTime end,
      Map<String, Object> fact) {}

  public record Piece(
      Root root,
      Long location,
      LocalDateTime start,
      LocalDateTime end,
      List<Long> sources,
      List<Long> unresolved,
      List<Long> locations) {}

  public record Snapshot(
      Map<Long, Map<String, Object>> spaces,
      Map<Long, Long> parents,
      List<Root> roots,
      List<Limit> limits,
      List<Arrangement> arrangements) {}

  private List<Map<String, Object>> bounded(String sql, Object... args) {
    var rows = b.jdbc.queryForList(sql + " LIMIT 5001", args);
    if (rows.size() > 5000)
      throw Business.conflict("SPATIAL_FACT_LIMIT", "相关空间事实超过首版处理上限，请缩小范围或分段处理。");
    return rows;
  }

  public Set<Long> connected(Collection<Long> initial) {
    var floors = new TreeSet<>(initial);
    var edges =
        bounded(
            "SELECT DISTINCT from_floor_id,target_floor_id FROM long_temporary_arrangement WHERE target_floor_id IS NOT NULL ORDER BY from_floor_id,target_floor_id");
    boolean changed = true;
    while (changed) {
      changed = false;
      for (var e : edges) {
        long a = Business.number(e, "from_floor_id"), z = Business.number(e, "target_floor_id");
        if (floors.contains(a) || floors.contains(z)) {
          changed |= floors.add(a);
          changed |= floors.add(z);
        }
      }
      if (floors.size() > 64)
        throw Business.conflict("SPATIAL_COORDINATION_LIMIT", "临时安排关联楼层超过首版协调上限，不能截断处理。");
    }
    return floors;
  }

  public Set<Long> lockFloors(Collection<Long> initial) {
    var expected = connected(initial);
    b.floors(expected);
    if (!expected.containsAll(connected(initial)))
      throw Business.conflict("SPATIAL_COORDINATES_CHANGED", "等待期间空间关联变化，请保留原方案重新预览。");
    return expected;
  }

  public Snapshot snapshot(LocalDateTime start, LocalDateTime end) {
    if (!start.isBefore(end)) throw DomainException.invalid("结束时间须晚于开始时间。");
    var now = b.now();
    var spaces = new TreeMap<Long, Map<String, Object>>();
    var parents = new HashMap<Long, Long>();
    for (var s : bounded("SELECT * FROM space ORDER BY id")) {
      long id = Business.number(s, "id");
      spaces.put(id, s);
      parents.put(id, s.get("parent_id") == null ? null : Business.number(s, "parent_id"));
    }
    var roots = new ArrayList<Root>();
    for (var r :
        bounded(
            "SELECT * FROM short_reservation WHERE starts_at<? AND ends_at>? AND ends_at>? AND (status='CHECKED_IN' OR (status='PENDING' AND check_in_deadline>?)) ORDER BY id",
            end,
            start,
            now,
            now))
      roots.add(root("SHORT", Business.number(r, "id"), 0, r, Business.number(r, "version"), 0));
    for (var r :
        bounded(
            "SELECT e.*,v.version,v.status AS request_status FROM venue_entitlement e JOIN venue_request v ON e.request_id=v.id WHERE e.status='ACTIVE' AND e.starts_at<? AND e.ends_at>? AND e.ends_at>? ORDER BY e.id",
            end,
            start,
            now))
      roots.add(
          root("VENUE", Business.number(r, "request_id"), 0, r, Business.number(r, "version"), 0));
    for (var r :
        bounded(
            "SELECT o.*,t.cycle_starts_at AS starts_at,t.cycle_ends_at AS ends_at,e.starts_at AS entitlement_starts_at,e.id AS entitlement_id,e.version AS entitlement_version,e.status AS entitlement_status FROM long_offer o JOIN preparation_batch t ON o.batch_id=t.id LEFT JOIN seat_entitlement e ON e.offer_id=o.id WHERE t.status='RESULT_PUBLISHED' AND t.cycle_starts_at<? AND t.cycle_ends_at>? AND t.cycle_ends_at>? AND ((o.status='OPEN' AND o.deadline>?) OR (o.status='ACCEPTED' AND e.status='ACTIVE' AND e.ends_at>?)) ORDER BY o.id",
            end,
            start,
            now,
            now,
            now)) {
      if (r.get("entitlement_starts_at") != null)
        r.put("starts_at", r.get("entitlement_starts_at"));
      roots.add(
          root(
              "LONG",
              Business.number(r, "id"),
              Business.number(r, "batch_id"),
              r,
              Business.number(r, "version"),
              r.get("entitlement_version") == null
                  ? 0
                  : Business.number(r, "entitlement_version")));
    }
    for (var r :
        bounded(
            "SELECT p.*,t.cycle_starts_at AS starts_at,t.cycle_ends_at AS ends_at,t.status,t.version,0 AS user_id FROM preparation_pool p JOIN preparation_batch t ON p.batch_id=t.id WHERE "
                + PreparationValidity.ACTIVE_SQL
                + " AND t.cycle_starts_at<? AND t.cycle_ends_at>? AND t.cycle_ends_at>? ORDER BY p.batch_id,p.space_id",
            now,
            now,
            end,
            start,
            now))
      roots.add(
          root(
              "POOL",
              Business.number(r, "space_id"),
              Business.number(r, "batch_id"),
              r,
              Business.number(r, "version"),
              Business.number(r, "space_version")));
    var limits = new ArrayList<Limit>();
    for (var r :
        bounded(
            "SELECT * FROM space_block WHERE status='ACTIVE' AND starts_at<? AND ends_at>? AND ends_at>? ORDER BY id",
            end,
            start,
            now))
      limits.add(
          new Limit(
              Business.number(r, "id"),
              Business.number(r, "space_id"),
              Business.number(r, "floor_id"),
              r.get("kind").toString(),
              r.get("venue_request_id") == null ? null : Business.number(r, "venue_request_id"),
              Business.date(r, "starts_at"),
              Business.date(r, "ends_at"),
              r));
    var arrangements = new ArrayList<Arrangement>();
    for (var r :
        bounded(
            "SELECT a.* FROM long_temporary_arrangement a JOIN space_block k ON a.block_id=k.id WHERE k.status='ACTIVE' AND k.ends_at>? AND a.starts_at<? AND a.ends_at>? ORDER BY a.id",
            now,
            end,
            start))
      arrangements.add(
          new Arrangement(
              Business.number(r, "block_id"),
              Business.number(r, "offer_id"),
              Business.number(r, "from_space_id"),
              r.get("target_space_id") == null ? null : Business.number(r, "target_space_id"),
              Business.date(r, "starts_at"),
              Business.date(r, "ends_at"),
              r));
    if (roots.size() + limits.size() + arrangements.size() > 5000)
      throw Business.conflict("SPATIAL_FACT_LIMIT", "有效权与限制超过首版处理上限，未截断结果。");
    return new Snapshot(
        spaces, parents, List.copyOf(roots), List.copyOf(limits), List.copyOf(arrangements));
  }

  private Root root(
      String kind, long id, long batch, Map<String, Object> r, long version, long secondary) {
    return new Root(
        kind,
        id,
        Business.number(r, "user_id"),
        Business.number(r, "space_id"),
        Business.number(r, "floor_id"),
        batch,
        version,
        secondary,
        Business.date(r, "starts_at"),
        Business.date(r, "ends_at"),
        r.get("status").toString(),
        r);
  }

  public static boolean related(long a, long c, Map<Long, Long> parents) {
    return ancestor(a, c, parents) || ancestor(c, a, parents);
  }

  private static boolean ancestor(long a, long c, Map<Long, Long> parents) {
    var seen = new HashSet<Long>();
    Long at = c;
    while (at != null && seen.add(at)) {
      if (at == a) return true;
      at = parents.get(at);
    }
    return false;
  }

  public static boolean overlap(
      LocalDateTime a, LocalDateTime z, LocalDateTime c, LocalDateTime d) {
    return a.isBefore(d) && c.isBefore(z);
  }

  private static LocalDateTime max(LocalDateTime a, LocalDateTime z) {
    return a.isAfter(z) ? a : z;
  }

  private static LocalDateTime min(LocalDateTime a, LocalDateTime z) {
    return a.isBefore(z) ? a : z;
  }

  public List<Piece> pieces(Snapshot s, Root root, LocalDateTime start, LocalDateTime end) {
    var a = max(start, root.start());
    var z = min(end, root.end());
    if (!a.isBefore(z)) return List.of();
    var cuts = new TreeSet<LocalDateTime>(List.of(a, z));
    var possible = new HashSet<Long>(List.of(root.space()));
    if (root.kind().equals("LONG"))
      for (var m : s.arrangements())
        if (m.offer() == root.id()) {
          possible.add(m.from());
          if (m.target() != null) possible.add(m.target());
        }
    for (var k : s.limits())
      if (possible.stream().anyMatch(at -> related(at, k.space(), s.parents()))) {
        if (k.start().isAfter(a) && k.start().isBefore(z)) cuts.add(k.start());
        if (k.end().isAfter(a) && k.end().isBefore(z)) cuts.add(k.end());
      }
    for (var m : s.arrangements())
      if (m.offer() == root.id() && root.kind().equals("LONG")) {
        if (m.start().isAfter(a) && m.start().isBefore(z)) cuts.add(m.start());
        if (m.end().isAfter(a) && m.end().isBefore(z)) cuts.add(m.end());
      }
    if (cuts.size() > 1002) throw Business.conflict("SPATIAL_SEGMENT_LIMIT", "时间片段过多，请分段处理。");
    var times = new ArrayList<>(cuts);
    var result = new ArrayList<Piece>();
    for (int i = 1; i < times.size(); i++) {
      var from = times.get(i - 1);
      var to = times.get(i);
      Long at = root.space();
      var path = new ArrayList<Long>();
      var unresolved = new ArrayList<Long>();
      var visited = new LinkedHashSet<Long>();
      for (int depth = 0; at != null; depth++) {
        if (depth >= 8 || !visited.add(at))
          throw Business.conflict("TEMPORARY_CYCLE", "临时安排形成循环或超过层数，不能推定可用。");
        long location = at;
        var blockers =
            s.limits().stream()
                .filter(
                    k ->
                        related(k.space(), location, s.parents())
                            && overlap(k.start(), k.end(), from, to)
                            && !(root.kind().equals("VENUE")
                                && Objects.equals(k.venue(), root.id())))
                .toList();
        if (blockers.isEmpty()) break;
        path.addAll(blockers.stream().map(Limit::id).toList());
        if (!root.kind().equals("LONG")) {
          unresolved.addAll(blockers.stream().map(Limit::id).toList());
          break;
        }
        Long target = null;
        boolean found = false;
        boolean missing = false;
        for (var k : blockers) {
          var maps =
              s.arrangements().stream()
                  .filter(
                      m ->
                          m.block() == k.id()
                              && m.offer() == root.id()
                              && m.from() == location
                              && !m.start().isAfter(from)
                              && !m.end().isBefore(to))
                  .toList();
          if (maps.size() != 1) {
            unresolved.add(k.id());
            missing = true;
            continue;
          }
          Long next = maps.get(0).target();
          if (found && !Objects.equals(target, next))
            throw Business.conflict("TEMPORARY_DISAGREEMENT", "重叠限制对同一使用权给出不一致安排，请重新完整处置。");
          target = next;
          found = true;
        }
        if (missing) break;
        at = target;
      }
      result.add(
          new Piece(
              root,
              at,
              from,
              to,
              List.copyOf(new TreeSet<>(path)),
              List.copyOf(unresolved),
              List.copyOf(visited)));
    }
    return result;
  }

  public List<Map<String, Object>> conflicts(
      Snapshot s,
      long space,
      LocalDateTime start,
      LocalDateTime end,
      long excludeVenue,
      long excludeBatch,
      Long excludeOffer,
      Long excludeEntitlement) {
    var result = new ArrayList<Map<String, Object>>();
    for (var k : s.limits())
      if (!Objects.equals(k.venue(), excludeVenue)
          && related(space, k.space(), s.parents())
          && overlap(start, end, k.start(), k.end()))
        result.add(
            Map.of(
                "space_id",
                k.space(),
                "starts_at",
                Business.iso(k.start()),
                "ends_at",
                Business.iso(k.end()),
                "conflict_type",
                "BLOCK",
                "kind",
                k.kind()));
    for (var root : s.roots()) {
      if (root.kind().equals("VENUE") && root.id() == excludeVenue) continue;
      if ((root.kind().equals("POOL") || root.kind().equals("LONG"))
          && root.batch() == excludeBatch) continue;
      if (root.kind().equals("LONG")
          && (excludeOffer != null && Objects.equals(root.id(), excludeOffer)
              || excludeEntitlement != null
                  && Objects.equals(root.fact().get("entitlement_id"), excludeEntitlement)))
        continue;
      for (var piece : pieces(s, root, start, end))
        if (piece.location() != null && related(space, piece.location(), s.parents())) {
          if (root.kind().equals("POOL")
              && excludeVenue > 0
              && s.limits().stream()
                  .anyMatch(
                      k ->
                          Objects.equals(k.venue(), excludeVenue)
                              && related(root.space(), k.space(), s.parents())
                              && !k.start().isAfter(piece.start())
                              && !k.end().isBefore(piece.end()))) continue;
          result.add(
              Map.of(
                  "space_id",
                  piece.location(),
                  "starts_at",
                  Business.iso(piece.start()),
                  "ends_at",
                  Business.iso(piece.end()),
                  "conflict_type",
                  root.kind().equals("POOL")
                      ? "BATCH_PROTECTION"
                      : root.kind().equals("LONG") && root.status().equals("OPEN")
                          ? "LONG_OFFER"
                          : root.kind()));
        }
    }
    return result;
  }

  public Map<String, Object> offerImpact(long offer, long owner) {
    var rows =
        b.jdbc.queryForList(
            "SELECT o.*,t.cycle_starts_at,t.cycle_ends_at FROM long_offer o JOIN preparation_batch t ON o.batch_id=t.id WHERE o.id=? AND o.user_id=?",
            offer,
            owner);
    if (rows.isEmpty()) throw DomainException.missing();
    var row = rows.get(0);
    var start = Business.date(row, "cycle_starts_at");
    var end = Business.date(row, "cycle_ends_at");
    var s = snapshot(start, end);
    var root =
        s.roots().stream().filter(r -> r.kind().equals("LONG") && r.id() == offer).findFirst();
    var view = new ArrayList<Map<String, Object>>();
    var evidence = new ArrayList<Map<String, Object>>();
    if (root.isPresent())
      for (var p : pieces(s, root.get(), start, end))
        if (!p.sources().isEmpty()) {
          var value = new LinkedHashMap<String, Object>();
          value.put("startsAt", Business.iso(p.start()));
          value.put("endsAt", Business.iso(p.end()));
          value.put("originalSpaceId", root.get().space());
          value.put("spaceId", p.location());
          value.put(
              "state",
              !p.unresolved().isEmpty()
                  ? "ARRANGEMENT_REQUIRED"
                  : p.location() == null ? "TEMPORARILY_UNAVAILABLE" : "TEMPORARY");
          value.put("sourceIds", p.sources());
          view.add(value);
          p.sources()
              .forEach(
                  id ->
                      s.limits().stream()
                          .filter(k -> k.id() == id)
                          .forEach(k -> evidence.add(b.view(k.fact()))));
        }
    var resourceIds = new TreeSet<Long>(List.of(Business.number(row, "space_id")));
    var arrangements = s.arrangements().stream().filter(a -> a.offer() == offer).toList();
    for (var a : arrangements) {
      resourceIds.add(a.from());
      if (a.target() != null) resourceIds.add(a.target());
    }
    var source = new TreeMap<String, Object>();
    source.put("offerId", offer);
    source.put("deadline", Business.iso(Business.date(row, "deadline")));
    source.put("segments", view);
    source.put("limits", evidence);
    source.put("resources", resourceIds.stream().map(s.spaces()::get).map(b::view).toList());
    source.put("arrangements", arrangements.stream().map(a -> b.view(a.fact())).toList());
    return Map.of(
        "segments",
        view,
        "impactHash",
        Digests.sha256(
            new dev.noctilume.qixu.preparation.FrozenJson(
                    tools.jackson.databind.json.JsonMapper.builder().build())
                .encode(source)),
        "requiresAcknowledgement",
        !view.isEmpty());
  }

  public void freeForOffer(
      long offer,
      long user,
      LocalDateTime start,
      LocalDateTime end,
      long batch,
      Long upgradedEntitlement) {
    var s = snapshot(start, end);
    var root =
        s.roots().stream()
            .filter(r -> r.kind().equals("LONG") && r.id() == offer && r.user() == user)
            .findFirst()
            .orElseThrow(DomainException::missing);
    for (var p : pieces(s, root, start, end)) {
      if (!p.unresolved().isEmpty())
        throw Business.conflict("ARRANGEMENT_REQUIRED", "席位受限且尚无明确安排，不能猜测可用。");
      for (var a : s.arrangements())
        if (a.offer() == offer
            && p.sources().contains(a.block())
            && overlap(a.start(), a.end(), p.start(), p.end())) {
          if (a.fact().get("required_features_json") == null)
            throw Business.conflict(
                "TEMPORARY_FACTS_UNPROVEN", "旧临时安排没有创建时设施要求证据，请管理员重新完整核实，未把当前画像当作历史事实。");
          if (a.target() != null)
            temporaryFacilities(
                json.readValue(a.fact().get("required_features_json").toString(), Map.class),
                s.spaces().get(a.target()));
        }
      if (p.location() == null) continue;
      var check =
          conflicts(s, p.location(), p.start(), p.end(), -1, -1, offer, upgradedEntitlement);
      // Only the original pool protects this exact original position; target pools remain
      // exclusions.
      check =
          check.stream()
              .filter(
                  c ->
                      !("BATCH_PROTECTION".equals(c.get("conflict_type"))
                          && p.location() == root.space()
                          && s.roots().stream()
                              .anyMatch(
                                  r ->
                                      r.kind().equals("POOL")
                                          && r.batch() == batch
                                          && r.space() == Business.number(c, "space_id"))))
              .toList();
      if (!check.isEmpty())
        throw Business.conflict("SPACE_CONFLICT", "当前实际或临时位置已有冲突，确认未生效，旧使用权保持。");
    }
  }

  public Map<String, Boolean> requiredFeatures(Map<String, Object> original) {
    var result = new TreeMap<String, Boolean>();
    var features = json.readTree(original.get("profile_json").toString()).path("features");
    for (String f : List.of("window", "outlet", "quiet", "accessible"))
      if (features.path(f).asBoolean()) result.put(f, true);
    return result;
  }

  public void temporaryFacilities(Map<String, Boolean> required, Map<String, Object> target) {
    if (target == null
        || !Boolean.TRUE.equals(target.get("active"))
        || !"SEAT".equals(target.get("kind"))
        || !Set.of("BOOKABLE", "PREPARATION").contains(target.get("use_mode")))
      throw Business.conflict("TEMPORARY_RESOURCE_CHANGED", "临时位置当前不再是有效可用席位，请重新核实安排。");
    var profile = json.readTree(target.get("profile_json").toString());
    for (var entry : required.entrySet())
      if (Boolean.TRUE.equals(entry.getValue())) {
        if (!profile.path("features").path(entry.getKey()).asBoolean())
          throw Business.conflict("TEMPORARY_RESOURCE_CHANGED", "临时位置已不满足承诺的设施要求，旧归属保持，请重新核实。");
        String condition =
            switch (entry.getKey()) {
              case "outlet" -> "outletCondition";
              case "quiet" -> "environmentCondition";
              default -> "";
            };
        if (!condition.isEmpty()
            && Set.of("BROKEN", "REPAIRING")
                .contains(profile.path("conditions").path(condition).asString()))
          throw Business.conflict("TEMPORARY_RESOURCE_CHANGED", "临时位置必需设施已核实损坏或维修中，不能冒充可用。");
      }
  }
}
