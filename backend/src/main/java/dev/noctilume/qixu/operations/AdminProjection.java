package dev.noctilume.qixu.operations;

import dev.noctilume.qixu.common.*;
import dev.noctilume.qixu.identity.*;
import java.util.*;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** Read-only, whitelisted projections; unknown entity types never imply global scope. */
@Service
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
public class AdminProjection {
  private final Business b;
  private final NamedParameterJdbcTemplate sql;

  public AdminProjection(Business b) {
    this.b = b;
    this.sql = new NamedParameterJdbcTemplate(b.jdbc);
  }

  private Actor admin(AuthService.Session session) {
    var actor = b.current(session);
    if (!actor.role().equals("ADMIN")) throw DomainException.forbidden();
    return actor;
  }

  private Map<String, Object> parameters(Actor actor, Long floor, int page, int size) {
    if (page < 1 || page > 10000 || size < 1 || size > 50)
      throw DomainException.invalid("分页范围为1–10000页，每页1–50项。");
    if (floor != null) b.admin(actor, floor);
    var p = new HashMap<String, Object>();
    p.put("actor", actor.id());
    p.put("floor", floor);
    p.put("offset", (page - 1) * size);
    p.put("size", size);
    return p;
  }

  private static String floor(String expression) {
    return "EXISTS(SELECT 1 FROM admin_scope sx WHERE sx.user_id=:actor AND sx.floor_id="
        + expression
        + ")";
  }

  private static String batch(String expression) {
    return "EXISTS(SELECT 1 FROM preparation_pool pp WHERE pp.batch_id="
        + expression
        + ") AND NOT EXISTS(SELECT 1 FROM preparation_pool pp WHERE pp.batch_id="
        + expression
        + " AND NOT "
        + floor("pp.floor_id")
        + ")";
  }

  private static String scopedEntity(String a) {
    var parts = new ArrayList<String>();
    for (var entry :
        Map.of(
                "SHORT",
                "short_reservation",
                "VENUE",
                "venue_request",
                "SPACE",
                "space",
                "FEEDBACK",
                "feedback_report",
                "REPAIR",
                "repair_ticket",
                "BLOCK",
                "space_block",
                "GOVERNANCE",
                "governance_case")
            .entrySet())
      parts.add(
          "("
              + a
              + ".entity_type='"
              + entry.getKey()
              + "' AND EXISTS(SELECT 1 FROM "
              + entry.getValue()
              + " x WHERE CAST(x.id AS BINARY)=CAST("
              + a
              + ".entity_id AS BINARY) AND "
              + floor("x.floor_id")
              + "))");
    parts.add(
        "("
            + a
            + ".entity_type='EVENT' AND EXISTS(SELECT 1 FROM campus_event x JOIN venue_request v ON v.id=x.venue_request_id WHERE CAST(x.id AS BINARY)=CAST("
            + a
            + ".entity_id AS BINARY) AND "
            + floor("v.floor_id")
            + "))");
    parts.add(
        "("
            + a
            + ".entity_type='BATCH' AND EXISTS(SELECT 1 FROM preparation_batch x WHERE CAST(x.id AS BINARY)=CAST("
            + a
            + ".entity_id AS BINARY) AND "
            + batch("x.id")
            + "))");
    parts.add(
        "("
            + a
            + ".entity_type='APPLICATION' AND EXISTS(SELECT 1 FROM preparation_application x WHERE CAST(x.id AS CHAR)="
            + a
            + ".entity_id AND "
            + batch("x.batch_id")
            + "))");
    return "(" + String.join(" OR ", parts) + ")";
  }

  public Map<String, Object> blocks(AuthService.Session session, Long floor, int page, int size) {
    var actor = admin(session);
    var p = parameters(actor, floor, page, size);
    String where =
        " FROM space_block k JOIN space s ON s.id=k.space_id WHERE "
            + floor("k.floor_id")
            + " AND (:floor IS NULL OR k.floor_id=:floor)";
    long total = sql.queryForObject("SELECT COUNT(*)" + where, p, Long.class);
    var items =
        sql
            .queryForList(
                "SELECT k.id,k.space_id,k.floor_id,k.kind,k.starts_at,k.ends_at,k.reason,k.venue_request_id,k.status,k.version,k.created_at,k.revoked_at,s.code,s.name"
                    + where
                    + " ORDER BY k.id DESC LIMIT :size OFFSET :offset",
                p)
            .stream()
            .map(b::view)
            .toList();
    return Map.of(
        "items", items, "total", total, "page", page, "size", size, "asOf", Business.iso(b.now()));
  }

  public Map<String, Object> operations(AuthService.Session session) {
    var actor = admin(session);
    var p = Map.<String, Object>of("actor", actor.id());
    String where = " FROM notification_outbox n WHERE " + scopedEntity("n");
    var row =
        sql.queryForMap(
            "SELECT COUNT(*) AS total,COALESCE(SUM(n.delivered_at IS NULL),0) AS pending,COALESCE(SUM(n.delivered_at IS NOT NULL),0) AS delivered,MIN(CASE WHEN n.delivered_at IS NULL THEN n.created_at ELSE NULL END) AS oldest_pending_at"
                + where,
            p);
    return Map.of(
        "notifications",
        b.view(row),
        "asOf",
        Business.iso(b.now()),
        "scope",
        "CURRENT_AUTHORIZED_ENTITIES",
        "deliveryMeaning",
        "PERSISTED_INBOX_NOT_READ_OR_EXTERNAL_DELIVERY");
  }

  public Map<String, Object> entitlements(
      AuthService.Session session, Long floor, int page, int size) {
    var actor = admin(session);
    var p = parameters(actor, floor, page, size);
    String where =
        " FROM seat_entitlement e JOIN space s ON s.id=e.space_id WHERE "
            + floor("e.floor_id")
            + " AND (:floor IS NULL OR e.floor_id=:floor)";
    long total = sql.queryForObject("SELECT COUNT(*)" + where, p, Long.class);
    var items =
        sql
            .queryForList(
                "SELECT e.id,e.batch_id,e.user_id,e.space_id,e.floor_id,e.version,e.status,e.starts_at,e.ends_at,s.code,s.name"
                    + where
                    + " ORDER BY e.id DESC LIMIT :size OFFSET :offset",
                p)
            .stream()
            .map(b::view)
            .toList();
    return Map.of(
        "items", items, "total", total, "page", page, "size", size, "asOf", Business.iso(b.now()));
  }

  public Map<String, Object> audit(AuthService.Session session, int page, int size) {
    var actor = admin(session);
    var p = parameters(actor, null, page, size);
    String where = " FROM audit_entry a WHERE (a.actor_id=:actor OR " + scopedEntity("a") + ")";
    long total = sql.queryForObject("SELECT COUNT(*)" + where, p, Long.class);
    var items =
        sql
            .queryForList(
                "SELECT a.id,a.actor_id,a.action,a.entity_type,a.entity_id,a.request_id,a.created_at"
                    + where
                    + " ORDER BY a.id DESC LIMIT :size OFFSET :offset",
                p)
            .stream()
            .map(b::view)
            .toList();
    return Map.of(
        "items", items, "total", total, "page", page, "size", size, "asOf", Business.iso(b.now()));
  }
}
