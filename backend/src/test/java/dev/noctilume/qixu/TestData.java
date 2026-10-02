package dev.noctilume.qixu;

import org.springframework.jdbc.core.JdbcTemplate;

final class TestData {
    static void clearBusiness(JdbcTemplate jdbc) {
        if(!java.util.Set.of("qixu_test","qixu_ci").contains(jdbc.queryForObject("SELECT DATABASE()",String.class))) throw new IllegalStateException("Refuse to reset outside isolated test schema");
        // Clear the nullable cyclic historical upgrade pointer before deleting its referenced right.
        jdbc.update("UPDATE long_offer SET upgrade_from_id=NULL");
        jdbc.update("DELETE FROM repair_limit");
        for(String table:java.util.List.of("block_recipient","block_batch","block_history","short_relocation","long_temporary_arrangement","block_impact","space_block"))jdbc.update("DELETE FROM "+table);
        for(String table:java.util.List.of("space_fact","repair_history","repair_report","repair_ticket","feedback_attachment","feedback_history","feedback_report"))jdbc.update("DELETE FROM "+table);
        for(String table:java.util.List.of("allocation_event","waitlist_entry","seat_entitlement","long_offer","allocation_outcome","allocation_result","allocation_run","frozen_person","frozen_input","application_version","preparation_application","preparation_pool","preparation_batch","inbox","notification_outbox","idempotency_receipt","favorite_space","event_participation","campus_event","venue_entitlement","venue_request","short_reservation")) jdbc.update("DELETE FROM "+table);
        // All referencing business rows are gone. Remove only explicit, known test coordinates;
        // keeping these seats across suites changes the public-profile baseline.
        jdbc.update("UPDATE space SET parent_id=NULL WHERE id IN (3900,4990,5801,5802,5803,9990)");
        jdbc.update("DELETE FROM space WHERE id IN (3900,4990,5801,5802,5803,9990)");
        // Test facts may deliberately mutate a demo seat. Restore the declared fixture features,
        // rather than reusing another case's verified facts as the next case's starting world.
        var json=tools.jackson.databind.json.JsonMapper.builder().build();
        for(var row:jdbc.queryForList("SELECT id,profile_json FROM space WHERE id IN (1000,1001,1002,3000,3001,3002) OR id BETWEEN 2000 AND 2023 OR id BETWEEN 2100 AND 2123 OR id BETWEEN 2200 AND 2211")) {
            long id=((Number)row.get("id")).longValue();var profile=new java.util.LinkedHashMap<String,Object>(json.readValue(row.get("profile_json").toString(),java.util.Map.class));
            if(!"DEMO".equals(profile.get("source")))throw new IllegalStateException("Refuse to restore non-demo fixture");
            boolean window=false,outlet=false,quiet=true;
            if(id>=2000&&id<=2023) {long i=id-2000;window=i%6==0;outlet=true;quiet=i%4==0;}
            else if(id>=2100&&id<=2123) {window=(id-2100)%6==5;outlet=true;}
            else if(id>=2200&&id<=2211) {long i=id-2200;window=i%6==0;outlet=i%2==0;quiet=false;}
            else if(id>=3000&&id<=3002) {window=id==3000;outlet=true;quiet=id!=3002;}
            profile.put("features",java.util.Map.of("window",window,"outlet",outlet,"quiet",quiet,"accessible",true));profile.remove("conditions");profile.remove("factsReviewedAt");
            jdbc.update("UPDATE space SET profile_json=?,active=TRUE,version=1 WHERE id=?",json.writeValueAsString(profile),id);
        }
    }
}
