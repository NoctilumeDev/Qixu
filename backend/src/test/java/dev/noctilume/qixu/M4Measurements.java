package dev.noctilume.qixu;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;

/** Raw SQL measurements, independent of the domain services and test verdicts. */
final class M4Measurements {
    private M4Measurements() {}

    static void collect(JdbcTemplate jdbc, Map<String,Object> target) {
        var checks = new LinkedHashMap<String,String>();
        checks.put("earlyRevocationCases", "SELECT COUNT(*) FROM governance_case WHERE revoked_version IS NOT NULL AND decided_at < statement_until");
        checks.put("selfReviewedCases", "SELECT COUNT(*) FROM governance_case WHERE reviewed_by IS NOT NULL AND reviewed_by=decided_by");
        checks.put("overlappingLongUserPairs", "SELECT COUNT(*) FROM seat_entitlement a JOIN seat_entitlement z ON a.id<z.id AND a.user_id=z.user_id WHERE a.status='ACTIVE' AND z.status='ACTIVE' AND a.starts_at<z.ends_at AND z.starts_at<a.ends_at");
        checks.put("canceledBatchWithRightsHistory", "SELECT COUNT(*) FROM preparation_batch b WHERE b.status='CANCELED' AND EXISTS(SELECT 1 FROM seat_entitlement e WHERE e.batch_id=b.id)");
        checks.put("originalEntitlementCoordinateDrift", "SELECT COUNT(*) FROM seat_entitlement e LEFT JOIN long_offer o ON o.id=e.offer_id WHERE o.id IS NULL OR e.batch_id<>o.batch_id OR e.user_id<>o.user_id OR e.space_id<>o.space_id OR e.floor_id<>o.floor_id");
        var raw = new LinkedHashMap<String,Object>();
        checks.forEach((name, sql) -> raw.put(name, jdbc.queryForObject(sql, Long.class)));
        target.put("invariants", raw);
        for (var table : new String[]{"governance_case", "seat_entitlement", "long_application_penalty", "preparation_batch", "long_offer", "space_block", "repair_ticket", "feedback_report"}) {
            target.put(table+"States", jdbc.queryForList("SELECT status FROM "+table+" ORDER BY status,id", String.class));
        }
    }

    static void governanceFault(JdbcTemplate jdbc, Map<String,Object> target, long caseId, long entitlementId, String key) {
        target.put("faultCaseStatus", jdbc.queryForObject("SELECT status FROM governance_case WHERE id=?", String.class, caseId));
        target.put("faultEntitlementStatus", jdbc.queryForObject("SELECT status FROM seat_entitlement WHERE id=?", String.class, entitlementId));
        target.put("faultPenaltyStates", jdbc.queryForList("SELECT status FROM long_application_penalty WHERE case_id=? ORDER BY status", String.class, caseId));
        target.put("faultReceipts", jdbc.queryForObject("SELECT COUNT(*) FROM idempotency_receipt WHERE request_key=?", Long.class, key));
    }
}
