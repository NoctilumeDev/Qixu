package dev.noctilume.qixu;

import static org.junit.jupiter.api.Assertions.*;
import dev.noctilume.qixu.common.*;
import dev.noctilume.qixu.preparation.*;
import dev.noctilume.qixu.notifications.Notifications;
import java.net.*;
import java.net.http.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.nio.file.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.*;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.*;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Real HTTP/MySQL; controlled Clock and provider witness transactions, not public beacon qualification. */
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
@org.springframework.test.annotation.DirtiesContext(classMode=org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS)
@ActiveProfiles("demo")
@Import(PreparationIT.FaultConfig.class)
class PreparationIT {
    static class MutableClock extends Clock {
        volatile Instant value=Instant.parse("2026-10-10T01:00:00Z");
        public ZoneId getZone(){return ZoneOffset.UTC;}public Clock withZone(ZoneId z){return this;}public Instant instant(){return value;}
    }
    static class ControlledSource implements RandomnessSource {
        volatile boolean unavailable;volatile String randomness="ab".repeat(32);final AtomicInteger calls=new AtomicInteger();
        public Proof fetch(long round) {
            assertFalse(org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive());calls.incrementAndGet();
            if(unavailable)throw new DomainException(503,"CONTROLLED_SOURCE_UNAVAILABLE","受控故障，非公开随机证明。");
            return new Proof(CHAIN,round,"00".repeat(48),randomness,VERIFIER);
        }
    }
    @TestConfiguration static class FaultConfig {
        @Bean @Primary MutableClock testClock(){return new MutableClock();}
        @Bean @Primary ControlledSource testSource(){return new ControlledSource();}
    }
    @DynamicPropertySource static void db(DynamicPropertyRegistry r){FoundationIT.database(r);}
    @LocalServerPort int port;
    @Autowired JdbcTemplate jdbc;@Autowired JsonMapper json;@Autowired MutableClock clock;@Autowired ControlledSource source;
    @Autowired LongSeats seats;@Autowired BatchFreezer freezer;@Autowired AllocationPublisher publisher;@Autowired Notifications notifications;@Autowired PlatformTransactionManager manager;
    @Autowired AllocationWorker worker;@Autowired org.springframework.boot.availability.ApplicationAvailability availability;@Autowired dev.noctilume.qixu.recovery.RecoveryFence recovery;
    @MockitoSpyBean Business business;
    private final HttpClient http=HttpClient.newHttpClient();private final List<Map<String,Object>> requests=new CopyOnWriteArrayList<>();private final Map<String,Object> facts=new LinkedHashMap<>();
    private static final Map<String,Object> OBSERVATIONS=new ConcurrentHashMap<>();private String name;
    record Reply(int status,JsonNode body){JsonNode data(){return body.path("data");}long id(){return data().path("id").asLong();}}
    @BeforeEach void reset(TestInfo info) {
        name=info.getTestMethod().orElseThrow().getName();clock.value=Instant.parse("2026-10-10T01:00:00Z");source.unavailable=false;source.randomness="ab".repeat(32);source.calls.set(0);
        TestData.clearBusiness(jdbc);jdbc.update("DELETE FROM auth_session");jdbc.update("DELETE FROM login_attempt");jdbc.update("DELETE FROM audit_entry");
        jdbc.update("UPDATE identity_user SET active=TRUE,student_verified=(role='STUDENT'),auth_version=1 WHERE id BETWEEN 1 AND 5");
        jdbc.update("UPDATE space SET active=TRUE,version=1 WHERE id BETWEEN 2000 AND 2220");
        jdbc.update("INSERT IGNORE INTO identity_user(id,username,password_hash,display_name,role,student_verified) SELECT 90,'student3',password_hash,'第三测试申请者','STUDENT',TRUE FROM identity_user WHERE id=1");
        jdbc.update("UPDATE identity_user SET active=TRUE,student_verified=TRUE,auth_version=1 WHERE id=90");
    }
    @AfterEach void retain(){
        OBSERVATIONS.put(name,Map.of("requests",requests,"database",facts,"randomnessProvider","CONTROLLED_TEST_DOUBLE_NOT_PUBLIC_BLS"));
        TestData.clearBusiness(jdbc);jdbc.update("DELETE FROM auth_session WHERE user_id IN (90,91)");jdbc.update("DELETE FROM audit_entry WHERE actor_id IN (90,91)");jdbc.update("DELETE FROM identity_user WHERE id IN (90,91)");
        jdbc.update("UPDATE space SET active=TRUE,version=1 WHERE id BETWEEN 2000 AND 2220");
    }
    @AfterAll static void finish() throws Exception {var p=Path.of("target/failsafe-reports/qixu-m3-observation.json");Files.createDirectories(p.getParent());Files.writeString(p,JsonMapper.builder().build().writeValueAsString(OBSERVATIONS));}
    Reply req(String method,String path,Object body,String token,String key) throws Exception {
        var builder=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path)).timeout(Duration.ofSeconds(20));if(token!=null)builder.header("Authorization","Bearer "+token);if(key!=null)builder.header("Idempotency-Key",key);if(body!=null)builder.header("Content-Type","application/json");
        builder.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)));var response=http.send(builder.build(),HttpResponse.BodyHandlers.ofString());requests.add(Map.of("method",method,"path",path,"status",response.statusCode()));return new Reply(response.statusCode(),json.readTree(response.body()));
    }
    Reply api(String method,String path,Object body,String token,String key)throws Exception{return req(method,"/api/v1"+path,body,token,key);}
    String login(String name)throws Exception{var r=api("POST","/auth/login",Map.of("username",name,"password","qixu-demo","mode","BEARER"),null,null);ok(r);return r.data().path("token").asString();}
    void ok(Reply r){assertEquals(200,r.status(),r.body().toString());}void error(Reply r,int status,String code){assertEquals(status,r.status(),r.body().toString());assertEquals(code,r.body().at("/error/code").asString());}
    int count(String table){return jdbc.queryForObject("SELECT COUNT(*) FROM "+table,Integer.class);}
    Map<String,Object> createBody(List<Long> pool){return Map.ofEntries(Map.entry("title","工程测试批次"),Map.entry("purpose","POSTGRADUATE"),Map.entry("startsAt","2026-10-10T02:10:00Z"),Map.entry("endsAt","2026-10-20T02:10:00Z"),Map.entry("opensAt","2026-10-10T00:59:00Z"),Map.entry("closesAt","2026-10-10T01:05:00Z"),Map.entry("freezeDeadline","2026-10-10T01:06:00Z"),Map.entry("randomAt","2026-10-10T01:07:00Z"),Map.entry("resultDeadline","2026-10-10T01:20:00Z"),Map.entry("confirmationDeadline","2026-10-10T02:00:00Z"),Map.entry("promotionUntil","2026-10-20T02:10:00Z"),Map.entry("promotionSeconds",600),Map.entry("seatIds",pool));}
    long create(String admin,List<Long> pool,String key)throws Exception{var r=api("POST","/preparation-batches",createBody(pool),admin,key);ok(r);return r.id();}
    Reply submit(String token,long batch,int version,List<Map<String,Object>> prefs,String key)throws Exception{return api("POST","/preparation-batches/"+batch+"/application",Map.of("version",version,"preferences",prefs,"keepWaitlist",true),token,key);}
    List<Map<String,Object>> prefs(long... seats){var result=new ArrayList<Map<String,Object>>();for(int i=0;i<seats.length;i++)result.add(Map.of("seatId",seats[i],"rank",i+1));return result;}
    Reply action(String token,long batch,long version,String action,String key)throws Exception{return api("POST","/preparation-batches/"+batch+"/actions",Map.of("version",version,"action",action),token,key);}
    void freeze(String admin,long batch)throws Exception{clock.value=Instant.parse("2026-10-10T01:05:00Z");ok(action(admin,batch,1,"FREEZE","freeze-batch"));}
    void allocate(String admin,long batch)throws Exception{clock.value=Instant.parse("2026-10-10T01:07:03Z");ok(action(admin,batch,2,"ALLOCATE","allocate-batch"));}
    long offer(long batch,long user){return jdbc.queryForObject("SELECT id FROM long_offer WHERE batch_id=? AND user_id=? AND status='OPEN'",Long.class,batch,user);}
    Reply respond(String token,long offer,String action,String key)throws Exception{return api("POST","/long-offers/"+offer+"/actions",Map.of("version",1,"action",action),token,key);}
    Reply exit(String token,long batch,String key)throws Exception{return api("POST","/preparation-batches/"+batch+"/exit",Map.of("version",1,"action","EXIT"),token,key);}
    Reply mine(String token,long batch)throws Exception{return api("GET","/preparation-batches/"+batch+"/application",null,token,null);}
    List<Reply> race(Callable<Reply> a,Callable<Reply> b)throws Exception{var pool=Executors.newFixedThreadPool(2);var ready=new CountDownLatch(2);var go=new CountDownLatch(1);try{var first=pool.submit(()->{ready.countDown();go.await();return a.call();});var second=pool.submit(()->{ready.countDown();go.await();return b.call();});assertTrue(ready.await(5,TimeUnit.SECONDS));go.countDown();return List.of(first.get(25,TimeUnit.SECONDS),second.get(25,TimeUnit.SECONDS));}finally{pool.shutdownNow();}}
    @Test void roleScopeAndPoolRightsAreNotCreationPrivileges()throws Exception {
        String a=login("admin1"),limited=login("admin2"),teacher=login("teacher1"),s=login("student1");
        error(api("POST","/preparation-batches",createBody(List.of(2200L)),limited,"wrong-floor"),403,"SCOPE_FORBIDDEN");
        assertEquals(403,api("POST","/preparation-batches",createBody(List.of(2000L)),teacher,"wrong-teacher").status());assertEquals(403,api("POST","/preparation-batches",createBody(List.of(2000L)),s,"wrong-student").status());
        long batch=create(a,List.of(2000L),"valid-create");error(api("POST","/preparation-batches",createBody(List.of(2000L)),a,"overlap-pool"),409,"SPACE_CONFLICT");
        error(api("POST","/reservations",Map.of("spaceId",2000,"startsAt","2026-10-10T02:11:00Z","endsAt","2026-10-10T03:00:00Z"),s,"protected-seat"),409,"SPACE_CONFLICT");facts.put("batches",count("preparation_batch"));facts.put("shorts",count("short_reservation"));
    }
    @Test void publishedBatchAfterFirstTenReceivesPromotionWithoutStarvation()throws Exception {
        String admin=login("admin1"),one=login("student1"),two=login("student2");
        var ids=new ArrayList<Long>();
        for(int i=0;i<11;i++) {
            var body=new LinkedHashMap<>(createBody(List.of(2000L)));
            var start=Instant.parse("2026-10-11T02:10:00Z").plus(Duration.ofDays(i));
            var end=start.plus(Duration.ofDays(1));
            body.put("startsAt",start.toString());body.put("endsAt",end.toString());body.put("promotionUntil",end.toString());
            var created=api("POST","/preparation-batches",body,admin,"m8-maint-create-"+i);ok(created);ids.add(created.id());
            ok(submit(one,created.id(),0,prefs(2000),"m8-maint-one-"+i));ok(submit(two,created.id(),0,prefs(2000),"m8-maint-two-"+i));
        }
        clock.value=Instant.parse("2026-10-10T01:05:00Z");
        for(long id:ids)ok(action(admin,id,1,"FREEZE","m8-maint-freeze-"+id));
        clock.value=Instant.parse("2026-10-10T01:07:03Z");
        for(long id:ids)ok(action(admin,id,2,"ALLOCATE","m8-maint-allocate-"+id));
        for(int i=0;i<10;i++) {
            long id=ids.get(i),owner=jdbc.queryForObject("SELECT user_id FROM long_offer WHERE batch_id=? AND status='OPEN'",Long.class,id);
            ok(respond(owner==1?one:two,offer(id,owner),"ACCEPT","m8-maint-accept-"+id));
        }
        long late=ids.get(10),owner=jdbc.queryForObject("SELECT user_id FROM long_offer WHERE batch_id=? AND status='OPEN'",Long.class,late);
        ok(exit(owner==1?one:two,late,"m8-maint-last-exit"));
        assertEquals(10,jdbc.queryForObject("SELECT COUNT(*) FROM seat_entitlement WHERE status='ACTIVE'",Integer.class));
        assertEquals(11,jdbc.queryForObject("SELECT COUNT(*) FROM waitlist_entry WHERE status='ACTIVE'",Integer.class));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM long_offer WHERE batch_id=? AND status='OPEN'",Integer.class,late));
        var tasks=new PreparationTasks(business,freezer,worker,publisher,seats,availability,recovery,true);
        tasks.run();tasks.run();notifications.deliver();notifications.deliver();
        int offers=jdbc.queryForObject("SELECT COUNT(*) FROM long_offer WHERE batch_id=? AND status='OPEN'",Integer.class,late);
        facts.put("legalBatches",ids.size());facts.put("earlyActiveRights",jdbc.queryForObject("SELECT COUNT(*) FROM seat_entitlement WHERE status='ACTIVE'",Integer.class));facts.put("taskScanRounds",2);facts.put("lateOpenOffers",offers);
        facts.put("latePromotionNotices",jdbc.queryForObject("SELECT COUNT(*) FROM notification_outbox WHERE entity_type='BATCH' AND entity_id=? AND event_key LIKE 'offer:%:promotion'",Integer.class,late));
        assertEquals(1,offers,"legal lower-id waitlists must not permanently hide the eleventh batch");
    }
    @Test void immutableVersionsFreezeExactWinnerAndRejectOfflineOverwrite()throws Exception {
        String a=login("admin1"),s=login("student1");long batch=create(a,List.of(2000L,2100L),"version-create");ok(submit(s,batch,0,prefs(2100,2000),"version-one"));
        var races=race(()->submit(s,batch,1,prefs(2000,2100),"version-two"),()->submit(s,batch,1,prefs(2100,2000),"version-three"));assertEquals(List.of(200,409),races.stream().map(Reply::status).sorted().toList());
        freeze(a,batch);error(submit(s,batch,2,prefs(2000),"offline-late"),409,"BATCH_STATE_CONFLICT");
        assertEquals(2,mine(s,batch).data().path("frozen_version").asInt());facts.put("versions",count("application_version"));facts.put("frozenRevision",jdbc.queryForObject("SELECT revision FROM frozen_person WHERE batch_id=?",Integer.class,batch));
        allocate(a,batch);assertEquals(1,count("long_offer"));
    }
    @Test void crossBatchConcurrentApplicationsHaveOneIntervalFact()throws Exception {
        String a=login("admin1"),s=login("student1");long one=create(a,List.of(2000L),"create-one"),two=create(a,List.of(2200L),"create-two");
        var result=race(()->submit(s,one,0,prefs(2000),"apply-one"),()->submit(s,two,0,prefs(2200),"apply-two"));assertEquals(List.of(200,409),result.stream().map(Reply::status).sorted().toList());facts.put("applications",count("preparation_application"));
    }
    @Test void publicPacketIsCompleteReproducibleAndContainsNoIdentityMap()throws Exception {
        String a=login("admin1"),s=login("student1"),t=login("student2");long batch=create(a,List.of(2000L,2100L),"packet-create");ok(submit(s,batch,0,prefs(2100,2000),"packet-one"));ok(submit(t,batch,0,prefs(2100),"packet-two"));freeze(a,batch);
        String publicId=jdbc.queryForObject("SELECT public_id FROM preparation_batch WHERE id=?",String.class,batch);String path="/api/public/batches/"+publicId+"/verification";var frozen=req("GET",path,null,null,null);ok(frozen);assertTrue(frozen.data().path("result").isEmpty());
        allocate(a,batch);var packet=req("GET",path,null,null,null);ok(packet);String input=packet.data().path("input_bytes").asString(),output=packet.data().at("/result/outputBytes").asString();
        assertEquals(packet.data().path("input_hash").asString(),Digests.sha256(input));assertEquals(packet.data().at("/result/output_hash").asString(),Digests.sha256(output));
        var graph=json.treeToValue(json.readTree(input).get("graph"),AllocationGraph.Input.class);var computed=AllocationGraph.allocate(graph,Digests.sha256(input),"ab".repeat(32));assertEquals(output,new FrozenJson(json).encode(computed));
        assertFalse(packet.body().toString().contains("student1"));assertFalse(input.contains("user_id"));assertFalse(input.contains("application_id"));assertFalse(input.contains("林同学"));
        assertEquals(2000,jdbc.queryForObject("SELECT space_id FROM allocation_outcome o JOIN preparation_application a ON o.application_id=a.id WHERE a.user_id=1",Long.class));assertEquals(2100,jdbc.queryForObject("SELECT space_id FROM allocation_outcome o JOIN preparation_application a ON o.application_id=a.id WHERE a.user_id=2",Long.class));
        assertEquals(401,req("POST",path,Map.of(),null,null).status());facts.put("maximum",computed.maximum());facts.put("outcomes",count("allocation_outcome"));facts.put("resultNotices",jdbc.queryForObject("SELECT COUNT(*) FROM notification_outbox WHERE event_key=?",Integer.class,"batch:"+batch+":result"));
    }
    @Test void sourceUnavailableAndResultDeadlineCannotSwitchEntropy()throws Exception {
        String a=login("admin1"),s=login("student1");long batch=create(a,List.of(2100L),"source-create");ok(submit(s,batch,0,prefs(2100),"source-apply"));freeze(a,batch);clock.value=Instant.parse("2026-10-10T01:07:03Z");source.unavailable=true;
        error(action(a,batch,2,"ALLOCATE","source-allocate"),503,"CONTROLLED_SOURCE_UNAVAILABLE");assertEquals(0,count("allocation_run"));clock.value=Instant.parse("2026-10-10T01:20:00Z");publisher.failDue(batch);source.unavailable=false;
        error(action(a,batch,2,"ALLOCATE","source-allocate"),409,"STALE_VERSION");facts.put("results",count("allocation_result"));facts.put("failedNotices",jdbc.queryForObject("SELECT COUNT(*) FROM notification_outbox WHERE event_key=?",Integer.class,"batch:"+batch+":failed"));facts.put("sourceCalls",source.calls.get());
    }
    @Test void resourceChangeInvalidatesEntireFrozenInput()throws Exception {
        String a=login("admin1"),s=login("student1");long batch=create(a,List.of(2100L),"resource-create");ok(submit(s,batch,0,prefs(2100),"resource-apply"));freeze(a,batch);String hash=jdbc.queryForObject("SELECT input_hash FROM frozen_input WHERE batch_id=?",String.class,batch);
        jdbc.update("UPDATE space SET active=FALSE,version=version+1 WHERE id=2100");allocate(a,batch);assertEquals(hash,jdbc.queryForObject("SELECT input_hash FROM frozen_input WHERE batch_id=?",String.class,batch));
        facts.put("status",jdbc.queryForObject("SELECT status FROM preparation_batch WHERE id=?",String.class,batch));facts.put("results",count("allocation_result"));facts.put("offers",count("long_offer"));
    }
    @Test void publicationFaultRetainsCandidateButRollsBackWholeResult()throws Exception {
        String a=login("admin1"),s=login("student1"),t=login("student2");long batch=create(a,List.of(2000L,2100L),"atomic-create");ok(submit(s,batch,0,prefs(2100,2000),"atomic-one"));ok(submit(t,batch,0,prefs(2100),"atomic-two"));freeze(a,batch);clock.value=Instant.parse("2026-10-10T01:07:03Z");var hit=new AtomicInteger();
        org.mockito.Mockito.doAnswer(inv->{if(inv.getArgument(1).equals("batch:"+batch+":result") && hit.incrementAndGet()==2)throw new org.springframework.dao.DataAccessResourceFailureException("CONTROLLED_AFTER_FIRST_RESULT");return inv.callRealMethod();}).when(business).notify(org.mockito.ArgumentMatchers.anyLong(),org.mockito.ArgumentMatchers.anyString(),org.mockito.ArgumentMatchers.anyString(),org.mockito.ArgumentMatchers.anyString(),org.mockito.ArgumentMatchers.anyString(),org.mockito.ArgumentMatchers.anyLong());
        assertEquals(503,action(a,batch,2,"ALLOCATE","atomic-publish").status());assertEquals(0,count("allocation_result"));assertEquals(0,count("allocation_outcome"));assertEquals(0,count("long_offer"));assertEquals(1,count("allocation_run"));
        org.mockito.Mockito.doCallRealMethod().when(business).notify(org.mockito.ArgumentMatchers.anyLong(),org.mockito.ArgumentMatchers.anyString(),org.mockito.ArgumentMatchers.anyString(),org.mockito.ArgumentMatchers.anyString(),org.mockito.ArgumentMatchers.anyString(),org.mockito.ArgumentMatchers.anyLong());
        ok(action(a,batch,2,"ALLOCATE","atomic-publish"));facts.put("resultsAfterRecovery",count("allocation_result"));facts.put("offersAfterRecovery",count("long_offer"));facts.put("sourceCalls",source.calls.get());facts.put("formalRuns",jdbc.queryForObject("SELECT COUNT(*) FROM allocation_run WHERE status='PUBLISHED'",Integer.class));
    }
    @Test void publicationCrossingDeadlineHasNoPartialRights()throws Exception {
        String a=login("admin1"),s=login("student1");long batch=create(a,List.of(2100L),"late-create");ok(submit(s,batch,0,prefs(2100),"late-apply"));freeze(a,batch);clock.value=Instant.parse("2026-10-10T01:07:03Z");
        org.mockito.Mockito.doAnswer(inv->{var result=inv.callRealMethod();if(inv.getArgument(1).equals("batch:"+batch+":result"))clock.value=Instant.parse("2026-10-10T01:20:00Z");return result;}).when(business).notify(org.mockito.ArgumentMatchers.anyLong(),org.mockito.ArgumentMatchers.anyString(),org.mockito.ArgumentMatchers.anyString(),org.mockito.ArgumentMatchers.anyString(),org.mockito.ArgumentMatchers.anyString(),org.mockito.ArgumentMatchers.anyLong());
        error(action(a,batch,2,"ALLOCATE","late-publish"),409,"RESULT_DEADLINE_PASSED");publisher.failDue(batch);facts.put("results",count("allocation_result"));facts.put("offers",count("long_offer"));facts.put("candidateRuns",count("allocation_run"));
    }
    @Test void allLosingApplicantsReceivePersistentResultsAndStableWaitlist()throws Exception {
        String a=login("admin1"),s=login("student1"),t=login("student2");long batch=create(a,List.of(2100L),"scarce-create");ok(submit(s,batch,0,prefs(2100),"scarce-one"));ok(submit(t,batch,0,prefs(2100),"scarce-two"));freeze(a,batch);allocate(a,batch);notifications.dispatch();notifications.dispatch();
        long loser=jdbc.queryForObject("SELECT user_id FROM waitlist_entry WHERE batch_id=?",Long.class,batch);String token=loser==1?s:t;var mine=mine(token,batch);ok(mine);assertFalse(mine.data().path("nextSteps").isEmpty());assertTrue(mine.data().path("waitlist").path("lottery_position").asInt()>0);
        facts.put("offers",count("long_offer"));facts.put("waitlist",count("waitlist_entry"));facts.put("resultInbox",jdbc.queryForObject("SELECT COUNT(*) FROM inbox i JOIN notification_outbox o ON i.outbox_id=o.id WHERE o.event_key=?",Integer.class,"batch:"+batch+":result"));
    }
    long fallbackBatch(String a,String s,String t)throws Exception {long batch=create(a,List.of(2000L,2100L),"fallback-create");ok(submit(s,batch,0,prefs(2100,2000),"fallback-one"));ok(submit(t,batch,0,prefs(2100),"fallback-two"));freeze(a,batch);allocate(a,batch);ok(respond(s,offer(batch,1),"ACCEPT","accept-fallback"));ok(respond(t,offer(batch,2),"ACCEPT","accept-preferred"));ok(exit(t,batch,"exit-preferred"));seats.maintain(batch);return batch;}
    @Test void fallbackUpgradeIsAtomicAndDuplicateConfirmationReturnsReceipt()throws Exception {
        String a=login("admin1"),s=login("student1"),t=login("student2");long batch=fallbackBatch(a,s,t);assertEquals(2000,jdbc.queryForObject("SELECT space_id FROM seat_entitlement WHERE user_id=1 AND status='ACTIVE'",Long.class));long upgrade=offer(batch,1);
        var results=race(()->respond(s,upgrade,"ACCEPT","confirm-upgrade"),()->respond(s,upgrade,"ACCEPT","confirm-upgrade"));results.forEach(this::ok);assertEquals(results.get(0).data().path("confirmedEntitlementId"),results.get(1).data().path("confirmedEntitlementId"));
        clock.value=Instant.parse("2026-10-10T03:00:00Z");ok(respond(s,upgrade,"ACCEPT","confirm-upgrade"));
        facts.put("activeSeat",jdbc.queryForObject("SELECT space_id FROM seat_entitlement WHERE user_id=1 AND status='ACTIVE'",Long.class));facts.put("activeRights",jdbc.queryForObject("SELECT COUNT(*) FROM seat_entitlement WHERE user_id=1 AND status='ACTIVE'",Integer.class));facts.put("replaced",jdbc.queryForObject("SELECT COUNT(*) FROM seat_entitlement WHERE user_id=1 AND status='REPLACED'",Integer.class));
    }
    @Test void expiredUpgradeAndOldTaskKeepFallbackAndRejectForeignOwner()throws Exception {
        String a=login("admin1"),s=login("student1"),t=login("student2");long batch=fallbackBatch(a,s,t),upgrade=offer(batch,1);error(respond(t,upgrade,"ACCEPT","foreign-offer"),404,"RESOURCE_NOT_FOUND");
        clock.value=jdbc.queryForObject("SELECT deadline FROM long_offer WHERE id=?",LocalDateTime.class,upgrade).toInstant(ZoneOffset.UTC);error(respond(s,upgrade,"ACCEPT","late-upgrade"),409,"OFFER_EXPIRED");seats.maintain(batch);seats.maintain(batch);
        facts.put("fallbackSeat",jdbc.queryForObject("SELECT space_id FROM seat_entitlement WHERE user_id=1 AND status='ACTIVE'",Long.class));facts.put("openOffers",jdbc.queryForObject("SELECT COUNT(*) FROM long_offer WHERE user_id=1 AND status='OPEN'",Integer.class));facts.put("upgradeState",jdbc.queryForObject("SELECT status FROM long_offer WHERE id=?",String.class,upgrade));
    }
    @Test void upgradeWriteFaultAndDeadlineNeverReleaseOldRight()throws Exception {
        String a=login("admin1"),s=login("student1"),t=login("student2");long batch=fallbackBatch(a,s,t),upgrade=offer(batch,1);
        org.mockito.Mockito.doAnswer(inv->{if(inv.getArgument(1).equals("offer:"+upgrade+":ACCEPT"))throw new org.springframework.dao.DataAccessResourceFailureException("CONTROLLED_UPGRADE_OUTBOX");return inv.callRealMethod();}).when(business).notify(org.mockito.ArgumentMatchers.anyLong(),org.mockito.ArgumentMatchers.anyString(),org.mockito.ArgumentMatchers.anyString(),org.mockito.ArgumentMatchers.anyString(),org.mockito.ArgumentMatchers.anyString(),org.mockito.ArgumentMatchers.anyLong());
        assertEquals(503,respond(s,upgrade,"ACCEPT","fault-upgrade").status());assertEquals(2000,jdbc.queryForObject("SELECT space_id FROM seat_entitlement WHERE user_id=1 AND status='ACTIVE'",Long.class));
        org.mockito.Mockito.doAnswer(inv->{var value=inv.callRealMethod();if(inv.getArgument(1).equals("offer:"+upgrade+":ACCEPT"))clock.value=jdbc.queryForObject("SELECT deadline FROM long_offer WHERE id=?",LocalDateTime.class,upgrade).toInstant(ZoneOffset.UTC);return value;}).when(business).notify(org.mockito.ArgumentMatchers.anyLong(),org.mockito.ArgumentMatchers.anyString(),org.mockito.ArgumentMatchers.anyString(),org.mockito.ArgumentMatchers.anyString(),org.mockito.ArgumentMatchers.anyString(),org.mockito.ArgumentMatchers.anyLong());
        error(respond(s,upgrade,"ACCEPT","fault-upgrade"),409,"OFFER_EXPIRED");facts.put("fallbackSeat",jdbc.queryForObject("SELECT space_id FROM seat_entitlement WHERE user_id=1 AND status='ACTIVE'",Long.class));facts.put("acceptedUpgrades",jdbc.queryForObject("SELECT COUNT(*) FROM long_offer WHERE id=? AND status='ACCEPTED'",Integer.class,upgrade));
    }
    @Test void concurrentExitAndConfirmCannotResurrectAndCycleCloses()throws Exception {
        String a=login("admin1"),s=login("student1");long batch=create(a,List.of(2100L),"exit-create");ok(submit(s,batch,0,prefs(2100),"exit-apply"));freeze(a,batch);allocate(a,batch);long offer=offer(batch,1);
        var responses=race(()->respond(s,offer,"ACCEPT","accept-exit-race"),()->exit(s,batch,"exit-race"));assertTrue(responses.stream().allMatch(r->r.status()==200 || r.status()==409));assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM seat_entitlement WHERE status='ACTIVE'",Integer.class));seats.maintain(batch);
        clock.value=Instant.parse("2026-10-20T02:10:00Z");seats.maintain(batch);assertEquals("CLOSED",jdbc.queryForObject("SELECT status FROM preparation_batch WHERE id=?",String.class,batch));respond(s,offer,"ACCEPT","accept-exit-race");
        facts.put("activeRights",jdbc.queryForObject("SELECT COUNT(*) FROM seat_entitlement WHERE status='ACTIVE'",Integer.class));facts.put("openOffers",jdbc.queryForObject("SELECT COUNT(*) FROM long_offer WHERE status='OPEN'",Integer.class));
    }
    @Test void invalidManualWriteNeverObtainsCandidateAndMissedFreezeIsExplicit()throws Exception {
        String a=login("admin1"),s=login("student1");long batch=create(a,List.of(2100L),"bad-write-create");ok(submit(s,batch,0,prefs(2100),"bad-write-apply"));freeze(a,batch);clock.value=Instant.parse("2026-10-10T01:07:03Z");error(action(a,batch,2,"ALLOCATE","bad"),422,"INVALID_INPUT");error(action(a,batch,1,"ALLOCATE","bad-version"),409,"STALE_VERSION");assertEquals(0,source.calls.get());
        clock.value=Instant.parse("2026-10-10T01:00:00Z");long second=create(a,List.of(2000L),"missed-create");clock.value=Instant.parse("2026-10-10T01:06:00Z");freezer.freezeDue(second);
        facts.put("sourceCalls",source.calls.get());facts.put("candidateRuns",count("allocation_run"));facts.put("missedFreezeStatus",jdbc.queryForObject("SELECT status FROM preparation_batch WHERE id=?",String.class,second));
    }
    @Test void residualPromotionsCannotRecreateArtificialScarcity()throws Exception {
        jdbc.update("INSERT INTO identity_user(id,username,password_hash,display_name,role,student_verified) SELECT 91,'student4',password_hash,'第四测试申请者','STUDENT',TRUE FROM identity_user WHERE id=1");
        String a=login("admin1"),s=login("student1"),t=login("student2"),u=login("student3"),v=login("student4");long batch=create(a,List.of(2000L,2100L),"residual-create");
        ok(submit(s,batch,0,prefs(2100),"residual-one"));ok(submit(t,batch,0,prefs(2000),"residual-two"));ok(submit(u,batch,0,prefs(2100,2000),"residual-three"));ok(submit(v,batch,0,prefs(2100),"residual-four"));freeze(a,batch);
        var frozen=jdbc.queryForMap("SELECT input_hash,input_bytes FROM frozen_input WHERE batch_id=?",batch);var graph=json.treeToValue(json.readTree(frozen.get("input_bytes").toString()).get("graph"),AllocationGraph.Input.class);
        var mapping=jdbc.queryForList("SELECT anonymous_id,user_id FROM frozen_person WHERE batch_id=?",batch);var desired=new HashSet<>(mapping.stream().filter(r->Business.number(r,"user_id")==1 || Business.number(r,"user_id")==2).map(r->r.get("anonymous_id").toString()).toList());
        boolean fixtureFound=false;for(int n=0;n<1000;n++) {String random=Digests.sha256("controlled-residual-fixture-"+n);var result=AllocationGraph.allocate(graph,frozen.get("input_hash").toString(),random);var winners=new HashSet<>(result.assignments().stream().filter(r->r.seat()!=null).map(AllocationGraph.Assignment::candidate).toList());if(winners.equals(desired)){source.randomness=random;fixtureFound=true;break;}}
        assertTrue(fixtureFound,"Controlled fixture must select the two initial holders; it is not a public random-source claim.");allocate(a,batch);
        ok(respond(s,offer(batch,1),"DECLINE","residual-decline-q"));ok(respond(t,offer(batch,2),"DECLINE","residual-decline-n"));seats.maintain(batch);seats.maintain(batch);
        facts.put("openPromotions",jdbc.queryForObject("SELECT COUNT(*) FROM long_offer WHERE status='OPEN'",Integer.class));facts.put("flexibleSeat",jdbc.queryForObject("SELECT space_id FROM long_offer WHERE user_id=90 AND status='OPEN'",Long.class));facts.put("constrainedSeat",jdbc.queryForObject("SELECT space_id FROM long_offer WHERE user_id=91 AND status='OPEN'",Long.class));
        assertEquals(2,facts.get("openPromotions"));assertEquals(2000L,facts.get("flexibleSeat"));assertEquals(2100L,facts.get("constrainedSeat"));
    }
    @Test void queuedFreezeRechecksDeadlineAndDoesNotExposeLateInput()throws Exception {
        String a=login("admin1"),s=login("student1");long batch=create(a,List.of(2100L),"queued-freeze-create");ok(submit(s,batch,0,prefs(2100),"queued-freeze-apply"));clock.value=Instant.parse("2026-10-10T01:05:00Z");
        var barrier=new CountDownLatch(1);org.mockito.Mockito.doAnswer(inv->{barrier.countDown();return inv.callRealMethod();}).when(business).floors(org.mockito.ArgumentMatchers.anyCollection());var pool=Executors.newSingleThreadExecutor();
        try {
            var tx=new TransactionTemplate(manager);final Future<Reply>[] future=new Future[1];tx.execute(status->{jdbc.queryForList("SELECT id FROM floor WHERE id=100 FOR UPDATE");future[0]=pool.submit(()->action(a,batch,1,"FREEZE","queued-freeze"));try{assertTrue(barrier.await(5,TimeUnit.SECONDS));}catch(InterruptedException e){throw new RuntimeException(e);}clock.value=Instant.parse("2026-10-10T01:06:00Z");return null;});
            ok(future[0].get(15,TimeUnit.SECONDS));facts.put("frozenInputs",count("frozen_input"));facts.put("status",jdbc.queryForObject("SELECT status FROM preparation_batch WHERE id=?",String.class,batch));facts.put("failedNotices",jdbc.queryForObject("SELECT COUNT(*) FROM notification_outbox WHERE event_key=?",Integer.class,"batch:"+batch+":failed"));
        }finally{pool.shutdownNow();}
    }
}
