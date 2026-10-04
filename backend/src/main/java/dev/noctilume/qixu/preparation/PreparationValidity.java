package dev.noctilume.qixu.preparation;

import dev.noctilume.qixu.common.Business;
import java.time.LocalDateTime;
import java.util.Map;

/** Irreversible deadlines affect rights even before a task persists the failure record. */
public final class PreparationValidity {
  private PreparationValidity() {}

  // Both callers use alias t and bind the same observed server time to these two placeholders.
  public static final String ACTIVE_SQL =
      "(t.status='RESULT_PUBLISHED' OR (t.status='OPEN' AND t.freeze_deadline>?) OR (t.status='FROZEN' AND t.result_deadline>?))";

  public static String failure(Map<String, Object> batch, LocalDateTime now) {
    if ("OPEN".equals(batch.get("status"))
        && !now.isBefore(Business.date(batch, "freeze_deadline"))) return "FREEZE_DEADLINE_MISSED";
    if ("FROZEN".equals(batch.get("status"))
        && !now.isBefore(Business.date(batch, "result_deadline"))) return "RESULT_DEADLINE_MISSED";
    return null;
  }
}
