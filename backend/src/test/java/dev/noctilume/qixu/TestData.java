package dev.noctilume.qixu;

import org.springframework.jdbc.core.JdbcTemplate;

final class TestData {
    static void clearBusiness(JdbcTemplate jdbc) {
        if(!java.util.Set.of("qixu_test","qixu_ci").contains(jdbc.queryForObject("SELECT DATABASE()",String.class))) throw new IllegalStateException("Refuse to reset outside isolated test schema");
        // Clear the nullable cyclic historical upgrade pointer before deleting its referenced right.
        jdbc.update("UPDATE long_offer SET upgrade_from_id=NULL");
        for(String table:java.util.List.of("block_history","short_relocation","long_temporary_arrangement","block_impact","space_block"))jdbc.update("DELETE FROM "+table);
        for(String table:java.util.List.of("space_fact","repair_history","repair_report","repair_ticket","feedback_attachment","feedback_history","feedback_report"))jdbc.update("DELETE FROM "+table);
        for(String table:java.util.List.of("allocation_event","waitlist_entry","seat_entitlement","long_offer","allocation_outcome","allocation_result","allocation_run","frozen_person","frozen_input","application_version","preparation_application","preparation_pool","preparation_batch","inbox","notification_outbox","idempotency_receipt","favorite_space","event_participation","campus_event","venue_entitlement","venue_request","short_reservation")) jdbc.update("DELETE FROM "+table);
    }
}
