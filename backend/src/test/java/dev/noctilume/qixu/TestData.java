package dev.noctilume.qixu;

import org.springframework.jdbc.core.JdbcTemplate;

final class TestData {
    static void clearBusiness(JdbcTemplate jdbc) {
        if(!java.util.Set.of("qixu_test","qixu_ci").contains(jdbc.queryForObject("SELECT DATABASE()",String.class))) throw new IllegalStateException("Refuse to reset outside isolated test schema");
        for(String table:java.util.List.of("inbox","notification_outbox","idempotency_receipt","favorite_space","event_participation","campus_event","venue_entitlement","venue_request","short_reservation")) jdbc.update("DELETE FROM "+table);
    }
}
