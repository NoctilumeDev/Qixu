package dev.noctilume.qixu;

import static org.junit.jupiter.api.Assertions.*;
import java.net.*;
import java.net.http.*;
import java.util.*;
import java.nio.file.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
@org.springframework.test.annotation.DirtiesContext(classMode=org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS)
@ActiveProfiles("demo")
class AdminProjectionIT {
    @LocalServerPort int port; @Autowired JdbcTemplate jdbc; @Autowired JsonMapper json;
    private final HttpClient client=HttpClient.newHttpClient();
    private static final Map<String,Object> observations=new LinkedHashMap<>();
    private final List<Map<String,Object>> requests=new ArrayList<>();private String name;
    @DynamicPropertySource static void database(DynamicPropertyRegistry r) {String url=System.getenv("QIXU_TEST_DB_URL");String password=System.getenv("QIXU_TEST_DB_PASSWORD");if(url==null||!url.matches("jdbc:mysql://[^/]+/(qixu_test|qixu_ci)(\\?.*)?")||password==null||password.isBlank())throw new IllegalStateException("Dedicated real MySQL required");r.add("spring.datasource.url",()->url);r.add("spring.datasource.username",()->System.getenv("QIXU_TEST_DB_USER"));r.add("spring.datasource.password",()->password);r.add("qixu.tasks-enabled",()->"false");}
    @BeforeEach void reset(TestInfo info) {name=info.getTestMethod().orElseThrow().getName();TestData.clearBusiness(jdbc);jdbc.update("DELETE FROM audit_entry");jdbc.update("DELETE FROM auth_session");jdbc.update("DELETE FROM login_attempt");jdbc.update("UPDATE identity_user SET active=TRUE,auth_version=1 WHERE id BETWEEN 1 AND 5");jdbc.update("INSERT IGNORE INTO admin_scope(user_id,floor_id) VALUES(4,100),(4,101),(5,100)");jdbc.update("DELETE FROM admin_scope WHERE user_id=5 AND floor_id<>100");}
    @AfterEach void record() {observations.put(name,Map.of("requests",new ArrayList<>(requests)));}
    @AfterAll static void retain() throws Exception {Path p=Path.of("target/failsafe-reports/qixu-m5-projection-observation.json");Files.createDirectories(p.getParent());Files.writeString(p,JsonMapper.builder().build().writeValueAsString(observations));}
    record Reply(int status,JsonNode data,String raw) {}
    Reply request(String method,String path,Object body,String token) throws Exception {var b=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path)).timeout(java.time.Duration.ofSeconds(10));if(token!=null)b.header("Authorization","Bearer "+token);if(body!=null)b.header("Content-Type","application/json");var raw=client.send(b.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build(),HttpResponse.BodyHandlers.ofString());requests.add(Map.of("method",method,"path",path,"status",raw.statusCode()));return new Reply(raw.statusCode(),json.readTree(raw.body()).path("data"),raw.body());}
    String token(String user) throws Exception {var r=request("POST","/api/v1/auth/login",Map.of("username",user,"password","qixu-demo","mode","BEARER"),null);assertEquals(200,r.status);return r.data.path("token").asString();}
    Reply get(String path,String t) throws Exception {return request("GET","/api/v1/admin/"+path,null,t);}
    void block(long seat,long floor) {jdbc.update("INSERT INTO space_block(space_id,floor_id,kind,starts_at,ends_at,reason,creator_id,status,version,created_at) VALUES(?,?,'MAINTENANCE','2027-01-01 08:00:00','2027-01-01 10:00:00','private scope reason',4,'ACTIVE',1,UTC_TIMESTAMP())",seat,floor);}
    void notice(String key,String entity,long id) {jdbc.update("INSERT INTO notification_outbox(recipient_id,event_key,title,message,entity_type,entity_id,created_at) VALUES(1,?,'private title','private message',?,?,UTC_TIMESTAMP())",key,entity,id);}
    void audit(long actor,String type,String id) {jdbc.update("INSERT INTO audit_entry(actor_id,action,entity_type,entity_id,request_id,detail_json,created_at) VALUES(?,'OBSERVED',?,?,UUID(),JSON_OBJECT('secret','private detail'),UTC_TIMESTAMP())",actor,type,id);}
    @Test void studentsAndTeachersCannotReadAnyManagementProjection() throws Exception {for(String user:List.of("student1","teacher1")){String t=token(user);for(String path:List.of("space-blocks","operations","audit","long-entitlements"))assertEquals(403,get(path,t).status);}assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM space_block",Integer.class));}
    @Test void blockPagesReflectCurrentScopeAndEnforceExplicitFloor() throws Exception {block(2000,100);block(2200,101);String t=token("admin2");var r=get("space-blocks?size=1",t);assertEquals(200,r.status);assertEquals(1,r.data.path("total").asInt());assertEquals(100,r.data.at("/items/0/floor_id").asInt());assertEquals(403,get("space-blocks?floor=101",t).status);jdbc.update("DELETE FROM admin_scope WHERE user_id=5");assertEquals(0,get("space-blocks",t).data.path("total").asInt());}
    @Test void onlyKnownAuthorizedEventsContributeToOutboxCounts() throws Exception {notice("local","SPACE",2000);notice("other","SPACE",2200);notice("unknown","NEW_UNKNOWN_KIND",2000);notice("missing","SPACE",999999);String t=token("admin2");var r=get("operations",t);assertEquals(200,r.status);assertEquals(1,r.data.at("/notifications/total").asInt());assertEquals(1,r.data.at("/notifications/pending").asInt());assertFalse(r.raw.contains("private message"));assertFalse(r.raw.contains("recipient_id"));jdbc.update("DELETE FROM admin_scope WHERE user_id=5");assertEquals(0,get("operations",t).data.at("/notifications/total").asInt());}
    @Test void auditUnknownEntitiesDoNotGrantGlobalAccessAndDetailsNeverLeak() throws Exception {audit(4,"SPACE","2000");audit(4,"SPACE","2200");audit(4,"NEW_UNKNOWN_KIND","2000");audit(5,"NEW_UNKNOWN_KIND","2000");String t=token("admin2");var r=get("audit",t);assertEquals(200,r.status);assertEquals(3,r.data.path("total").asInt());assertFalse(r.raw.contains("private detail"));assertFalse(r.raw.contains("detail_json"));jdbc.update("DELETE FROM admin_scope WHERE user_id=5");assertEquals(2,get("audit",t).data.path("total").asInt());}
    @Test void mixedFloorBatchCannotExposeWholeBatchThroughOneAuthorizedSeat() throws Exception {jdbc.update("INSERT INTO preparation_batch(public_id,owner_id,title,purpose,cycle_starts_at,cycle_ends_at,opens_at,closes_at,freeze_deadline,random_at,result_deadline,confirmation_deadline,promotion_until,promotion_seconds,algorithm,source_chain,source_round,created_at) VALUES(UUID(),4,'mixed','OTHER','2027-01-02','2027-02-01','2026-12-01','2026-12-02','2026-12-03','2026-12-04','2026-12-05','2026-12-06','2027-01-31',60,'test',REPEAT('0',64),1,UTC_TIMESTAMP())");long id=jdbc.queryForObject("SELECT id FROM preparation_batch",Long.class);jdbc.update("INSERT INTO preparation_pool(batch_id,space_id,floor_id,space_version) VALUES(?,2000,100,1),(?,2200,101,1)",id,id);notice("mixed","BATCH",id);audit(4,"BATCH",Long.toString(id));assertEquals(0,get("operations",token("admin2")).data.at("/notifications/total").asInt());assertEquals(1,get("operations",token("admin1")).data.at("/notifications/total").asInt());}
    @Test void pageBoundsAreRejectedWithoutWritingFacts() throws Exception {String t=token("admin1");for(String path:List.of("space-blocks?page=0","space-blocks?size=51","audit?page=10001","audit?size=0","long-entitlements?page=0","long-entitlements?size=51"))assertEquals(422,get(path,t).status);assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM idempotency_receipt",Integer.class));}
    @Test void entitlementProjectionUsesCurrentFloorScopeAndPublicWhitelist() throws Exception {
        // Relational fixture proves read authorization only, never lottery qualification.
        jdbc.update("INSERT INTO preparation_batch(public_id,owner_id,title,purpose,status,cycle_starts_at,cycle_ends_at,opens_at,closes_at,freeze_deadline,random_at,result_deadline,confirmation_deadline,promotion_until,promotion_seconds,algorithm,source_chain,source_round,created_at) VALUES(UUID(),4,'projection fixture','OTHER','RESULT_PUBLISHED','2027-01-02','2027-02-01','2026-12-01','2026-12-02','2026-12-03','2026-12-04','2026-12-05','2026-12-06','2027-01-31',60,'fixture',REPEAT('0',64),1,UTC_TIMESTAMP())");
        long batch=jdbc.queryForObject("SELECT id FROM preparation_batch",Long.class);
        jdbc.update("INSERT INTO preparation_pool(batch_id,space_id,floor_id,space_version) VALUES(?,2000,100,1),(?,2200,101,1)",batch,batch);
        jdbc.update("INSERT INTO frozen_input(batch_id,input_hash,input_bytes,frozen_at) VALUES(?,REPEAT('0',64),'{}',UTC_TIMESTAMP())",batch);
        jdbc.update("INSERT INTO allocation_run(batch_id,input_hash,output_hash,proof_json,output_json,status,created_at) VALUES(?,REPEAT('0',64),REPEAT('1',64),'{}','{}','PUBLISHED',UTC_TIMESTAMP())",batch);
        jdbc.update("INSERT INTO allocation_result(batch_id,input_hash,output_hash,maximum_count,candidate_count,published_at) VALUES(?,REPEAT('0',64),REPEAT('1',64),2,2,UTC_TIMESTAMP())",batch);
        for(long user:List.of(1L,2L)) {
            long seat=user==1?2000:2200,floor=user==1?100:101;
            jdbc.update("INSERT INTO preparation_application(batch_id,user_id,anonymous_id,current_version,frozen_version,created_at) VALUES(?,?,UUID(),1,1,UTC_TIMESTAMP())",batch,user);
            long application=jdbc.queryForObject("SELECT id FROM preparation_application WHERE batch_id=? AND user_id=?",Long.class,batch,user);
            jdbc.update("INSERT INTO application_version(application_id,revision,accepted_at,preferences_json,keep_waitlist) VALUES(?,1,UTC_TIMESTAMP(),'[]',TRUE)",application);
            jdbc.update("INSERT INTO long_offer(batch_id,application_id,user_id,space_id,floor_id,preference_rank,deadline,status,created_at) VALUES(?,?,?,?,?,1,'2026-12-06','ACCEPTED',UTC_TIMESTAMP())",batch,application,user,seat,floor);
            long offer=jdbc.queryForObject("SELECT id FROM long_offer WHERE application_id=?",Long.class,application);
            jdbc.update("INSERT INTO seat_entitlement(offer_id,batch_id,user_id,space_id,floor_id,starts_at,ends_at) VALUES(?,?,?,?,?,'2027-01-02','2027-02-01')",offer,batch,user,seat,floor);
        }
        String local=token("admin2"),global=token("admin1");var page=get("long-entitlements?size=1",local);
        assertEquals(200,page.status);assertEquals(1,page.data.path("total").asInt());assertEquals("A001",page.data.at("/items/0/code").asString());assertEquals(100,page.data.at("/items/0/floor_id").asInt());
        var fields=new TreeSet<String>();page.data.at("/items/0").properties().forEach(p->fields.add(p.getKey()));
        assertEquals(Set.of("id","batch_id","user_id","space_id","floor_id","version","status","starts_at","ends_at","code","name"),fields);
        assertEquals(2,get("long-entitlements",global).data.path("total").asInt());assertEquals(403,get("long-entitlements?floor=101",local).status);
        jdbc.update("DELETE FROM admin_scope WHERE user_id=5");assertEquals(0,get("long-entitlements",local).data.path("total").asInt());assertEquals(403,get("long-entitlements?floor=100",local).status);
        assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM seat_entitlement",Integer.class));assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM idempotency_receipt",Integer.class));
    }
    @Test void personalApplicationPagesIgnorePublicRecencyAndCannotSelectAnotherActor() throws Exception {
        // Read-only relational fixture, not a qualified allocation.
        long oldest=0;
        for(int i=0;i<47;i++) {
            jdbc.update("INSERT INTO preparation_batch(public_id,owner_id,title,purpose,cycle_starts_at,cycle_ends_at,opens_at,closes_at,freeze_deadline,random_at,result_deadline,confirmation_deadline,promotion_until,promotion_seconds,algorithm,source_chain,source_round,created_at) VALUES(UUID(),4,?,'OTHER','2027-01-02','2027-02-01','2026-12-01','2026-12-02','2026-12-03','2026-12-04','2026-12-05','2026-12-06','2027-01-31',60,'read-fixture',REPEAT('0',64),1,UTC_TIMESTAMP())","read fixture "+i);
            long batch=jdbc.queryForObject("SELECT MAX(id) FROM preparation_batch",Long.class);if(i==0)oldest=batch;
            long user=i<22?1:2;
            jdbc.update("INSERT INTO preparation_application(batch_id,user_id,anonymous_id,current_version,created_at) VALUES(?,?,?,1,UTC_TIMESTAMP())",batch,user,"private-anonymous-"+user+"-"+i);
            long application=jdbc.queryForObject("SELECT MAX(id) FROM preparation_application",Long.class);
            jdbc.update("INSERT INTO application_version(application_id,revision,accepted_at,preferences_json,keep_waitlist) VALUES(?,1,UTC_TIMESTAMP(),'[]',TRUE)",application);
        }
        String first=token("student1"),other=token("student2");
        var page=request("GET","/api/v1/preparation-applications?page=1&userId=2",null,first);
        assertEquals(200,page.status);assertEquals(22,page.data.path("total").asInt());assertEquals(20,page.data.path("items").size());assertFalse(page.raw.contains("private-anonymous-2-"));
        var last=request("GET","/api/v1/preparation-applications?page=2",null,first);assertEquals(2,last.data.path("items").size());assertEquals(oldest,last.data.at("/items/1/batch_id").asLong());
        assertEquals(25,request("GET","/api/v1/preparation-applications",null,other).data.path("total").asInt());
        for(int invalid:List.of(0,-1,10001))assertEquals(422,request("GET","/api/v1/preparation-applications?page="+invalid,null,first).status);
        assertEquals(0,request("GET","/api/v1/preparation-applications?page=10000",null,first).data.path("items").size());
        assertEquals(47,jdbc.queryForObject("SELECT COUNT(*) FROM preparation_application",Integer.class));assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM idempotency_receipt",Integer.class));
    }

}
