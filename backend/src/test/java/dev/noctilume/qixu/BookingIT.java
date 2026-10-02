package dev.noctilume.qixu;

import static org.junit.jupiter.api.Assertions.*;
import dev.noctilume.qixu.booking.ShortReservations;
import dev.noctilume.qixu.common.Business;
import dev.noctilume.qixu.notifications.Notifications;
import java.net.*;
import java.net.http.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.nio.file.*;
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

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("demo")
@Import(BookingIT.TimeConfig.class)
class BookingIT {
    static class MutableClock extends Clock {
        volatile Instant instant=Instant.parse("2026-10-10T01:00:00Z");
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return instant; }
    }
    @TestConfiguration static class TimeConfig { @Bean @Primary MutableClock testClock() { return new MutableClock(); } }
    @DynamicPropertySource static void database(DynamicPropertyRegistry r) { FoundationIT.database(r); }
    @LocalServerPort int port;
    @Autowired JdbcTemplate jdbc;
    @Autowired JsonMapper json;
    @Autowired MutableClock clock;
    @Autowired Notifications notifications;
    @Autowired ShortReservations shorts;
    @Autowired PlatformTransactionManager manager;
    @MockitoSpyBean Business business;
    private final HttpClient client=HttpClient.newHttpClient();
    private final List<Map<String,Object>> requests=new CopyOnWriteArrayList<>();
    private final Map<String,Object> facts=new LinkedHashMap<>();
    private static final Map<String,Object> OBSERVATIONS=new ConcurrentHashMap<>();
    private String name;
    record Reply(int status,JsonNode body) { JsonNode data() { return body.path("data"); } long id() { return data().path("id").asLong(); } }
    @BeforeEach void reset(TestInfo info) {
        name=info.getTestMethod().orElseThrow().getName(); clock.instant=Instant.parse("2026-10-10T01:00:00Z");
        TestData.clearBusiness(jdbc); jdbc.update("DELETE FROM auth_session"); jdbc.update("DELETE FROM login_attempt"); jdbc.update("DELETE FROM audit_entry");
        jdbc.update("UPDATE identity_user SET active=TRUE,auth_version=1 WHERE id BETWEEN 1 AND 5");
        jdbc.update("INSERT IGNORE INTO space(id,floor_id,code,name,kind,use_mode,capacity,map_x,map_y,map_w,map_h,profile_json) VALUES(3900,101,'R-C','验收专用研讨室','ROOM','VENUE',8,0,700,100,100,JSON_OBJECT('source','TEST_FIXTURE'))");
    }
    @AfterEach void record() { OBSERVATIONS.put(name,Map.of("requests",requests,"database",facts)); }
    @AfterAll static void retain() throws Exception { var p=Path.of("target/failsafe-reports/qixu-m2-observation.json"); Files.createDirectories(p.getParent()); Files.writeString(p,JsonMapper.builder().build().writeValueAsString(OBSERVATIONS)); }
    Reply request(String method,String path,Object body,String token,String key) throws Exception {
        var builder=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api/v1"+path)).timeout(Duration.ofSeconds(15));
        if(token!=null) builder.header("Authorization","Bearer "+token); if(key!=null) builder.header("Idempotency-Key",key);
        if(body!=null) builder.header("Content-Type","application/json");
        builder.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)));
        var result=client.send(builder.build(),HttpResponse.BodyHandlers.ofString()); requests.add(Map.of("method",method,"path",path,"status",result.statusCode()));
        return new Reply(result.statusCode(),json.readTree(result.body()));
    }
    String token(String user) throws Exception { var r=request("POST","/auth/login",Map.of("username",user,"password","qixu-demo","mode","BEARER"),null,null); assertEquals(200,r.status()); return r.data().path("token").asString(); }
    void error(Reply r,int status,String code) { assertEquals(status,r.status(),r.body().toString()); assertEquals(code,r.body().at("/error/code").asString()); }
    Map<String,Object> shortBody(long seat,String start,String end) { return Map.of("spaceId",seat,"startsAt",start,"endsAt",end); }
    long seat(String code,long floor) { return jdbc.queryForObject("SELECT id FROM space WHERE code=? AND floor_id=?",Long.class,code,floor); }
    Map<String,Object> venueBody(long space) { return Map.of("spaceId",space,"startsAt","2026-10-10T03:00:00Z","endsAt","2026-10-10T04:00:00Z","people",1,"purpose","公开课","description","真实HTTP验收","contact","组织者"); }
    Reply venue(String token,long space,String key) throws Exception { var r=request("POST","/venue-requests",venueBody(space),token,key); assertEquals(200,r.status(),r.body().toString()); return r; }
    Reply decision(String admin,long id,long version,String action,String key) throws Exception { return request("POST","/venue-requests/"+id+"/actions",Map.of("version",version,"action",action,"reason","验收处置"),admin,key); }
    Reply event(String teacher,long venue,String key) throws Exception { return request("POST","/organizer/events",Map.of("venueRequestId",venue,"title","阅读公开课","eventType","CLASS","speaker","老师","description","活动说明","notice","准时参加","capacity",1,"opensAt","2026-10-10T00:00:00Z","closesAt","2026-10-10T02:30:00Z","promotionUntil","2026-10-10T03:00:00Z"),teacher,key); }
    Reply eventAction(String token,long id,long version,String action,Long venue,String key) throws Exception { var body=new HashMap<String,Object>(); body.put("version",version); body.put("action",action); body.put("reason","验收处置"); if(venue!=null) body.put("venueRequestId",venue); return request("POST","/organizer/events/"+id+"/actions",body,token,key); }
    long published(String teacher,String admin) throws Exception {
        var venue=venue(teacher,seat("R-A",100),"create-venue"); assertEquals(200,decision(admin,venue.id(),1,"APPROVE","approve-venue").status());
        var event=event(teacher,venue.id(),"create-event"); assertEquals(200,event.status(),event.body().toString()); assertEquals(200,eventAction(teacher,event.id(),1,"PUBLISH",null,"publish-event").status()); return event.id();
    }
    List<Reply> race(Callable<Reply> a,Callable<Reply> b) throws Exception {
        var pool=Executors.newFixedThreadPool(2); var ready=new CountDownLatch(2); var go=new CountDownLatch(1);
        try {
            var fa=pool.submit(()->{ready.countDown(); go.await(); return a.call();}); var fb=pool.submit(()->{ready.countDown(); go.await(); return b.call();});
            assertTrue(ready.await(5,TimeUnit.SECONDS)); go.countDown(); return List.of(fa.get(20,TimeUnit.SECONDS),fb.get(20,TimeUnit.SECONDS));
        } finally { pool.shutdownNow(); }
    }
    @Test void sameSeatAndCrossFloorPersonalConflictsSerialize() throws Exception {
        String a=token("student1"),c=token("student2"); var body=shortBody(seat("A001",100),"2026-10-10T01:01:00Z","2026-10-10T02:00:00Z");
        var result=race(()->request("POST","/reservations",body,a,"seat-race-a"),()->request("POST","/reservations",body,c,"seat-race-c"));
        assertEquals(List.of(200,409),result.stream().map(Reply::status).sorted().toList()); facts.put("sameSeatRights",jdbc.queryForObject("SELECT COUNT(*) FROM short_reservation",Integer.class));
        TestData.clearBusiness(jdbc);
        result=race(()->request("POST","/reservations",shortBody(seat("A001",100),"2026-10-10T01:01:00Z","2026-10-10T02:00:00Z"),a,"personal-one"),()->request("POST","/reservations",shortBody(seat("C001",101),"2026-10-10T01:01:00Z","2026-10-10T02:00:00Z"),a,"personal-two"));
        assertEquals(List.of(200,409),result.stream().map(Reply::status).sorted().toList()); facts.put("personalRights",jdbc.queryForObject("SELECT COUNT(*) FROM short_reservation",Integer.class));
    }
    @Test void sameKeyRecoversCommittedResultAndRejectsChangedBody() throws Exception {
        String a=token("student1"),c=token("student2"); var body=shortBody(seat("A001",100),"2026-10-10T01:01:00Z","2026-10-10T02:00:00Z");
        var result=race(()->request("POST","/reservations",body,a,"same-request"),()->request("POST","/reservations",body,a,"same-request"));
        assertEquals(List.of(200,200),result.stream().map(Reply::status).toList()); assertEquals(result.get(0).id(),result.get(1).id());
        error(request("POST","/reservations",shortBody(seat("A002",100),"2026-10-10T01:01:00Z","2026-10-10T02:00:00Z"),a,"same-request"),409,"IDEMPOTENCY_CONFLICT");
        error(request("GET","/receipts/same-request",null,c,null),404,"RESOURCE_NOT_FOUND");
        assertEquals(result.get(0).id(),request("GET","/receipts/same-request",null,a,null).data().at("/result/id").asLong());
        facts.put("rights",jdbc.queryForObject("SELECT COUNT(*) FROM short_reservation",Integer.class)); facts.put("receipts",jdbc.queryForObject("SELECT COUNT(*) FROM idempotency_receipt",Integer.class));
    }
    @Test void adjacentIntervalsAndDeadlineUseCurrentServerTime() throws Exception {
        String a=token("student1"),c=token("student2"); long seat=seat("A001",100);
        var first=request("POST","/reservations",shortBody(seat,"2026-10-10T01:01:00Z","2026-10-10T02:00:00Z"),a,"create-first"); assertEquals(200,first.status());
        assertEquals(200,request("POST","/reservations",shortBody(seat,"2026-10-10T02:00:00Z","2026-10-10T03:00:00Z"),c,"create-adjacent").status());
        error(request("POST","/reservations/"+first.id()+"/actions",Map.of("version",1,"action","CHECK_IN"),a,"early-checkin"),409,"CHECK_IN_WINDOW");
        clock.instant=Instant.parse("2026-10-10T01:10:59Z"); assertEquals(200,request("POST","/reservations/"+first.id()+"/actions",Map.of("version",1,"action","CHECK_IN"),a,"valid-checkin").status());
        // Old expiry work cannot release the newly checked-in right.
        clock.instant=Instant.parse("2026-10-10T01:11:00Z"); shorts.expire(first.id());
        facts.put("checkedInAfterOldSweep",jdbc.queryForObject("SELECT status FROM short_reservation WHERE id=?",String.class,first.id()));
        error(request("POST","/reservations",shortBody(seat("A002",100),"2026-10-10T01:12:00Z","2026-10-10T02:00:00Z"),a,"still-conflicts"),409,"PERSONAL_CONFLICT");
        clock.instant=Instant.parse("2026-10-10T02:10:00Z");
        error(request("POST","/reservations/"+request("GET","/reservations",null,c,null).data().get(0).path("id").asLong()+"/actions",Map.of("version",1,"action","CHECK_IN"),c,"at-deadline"),409,"RESERVATION_EXPIRED");
        assertEquals(200,request("POST","/reservations",shortBody(seat,"2026-10-10T02:11:00Z","2026-10-10T03:00:00Z"),a,"released-pending").status());
    }
    @Test void lockWaitRechecksSessionAndClockBeforeWriting() throws Exception {
        final String a=token("student1"); long seat=seat("A001",100); var body=shortBody(seat,"2026-10-10T01:01:00Z","2026-10-10T02:00:00Z");
        var pool=Executors.newSingleThreadExecutor();
        var barrier=new java.util.concurrent.atomic.AtomicReference<>(new CountDownLatch(1));
        org.mockito.Mockito.doAnswer(invocation->{barrier.get().countDown(); return invocation.callRealMethod();}).when(business).floors(org.mockito.ArgumentMatchers.anyCollection());
        try {
            var tx=new TransactionTemplate(manager); final Future<Reply>[] future=new Future[1];
            tx.execute(status->{
                jdbc.queryForList("SELECT id FROM floor WHERE id=100 FOR UPDATE");
                future[0]=pool.submit(()->request("POST","/reservations",body,a,"queued-authority"));
                awaitBarrier(barrier.get()); jdbc.update("UPDATE identity_user SET auth_version=auth_version+1 WHERE id=1"); return null;
            });
            error(future[0].get(15,TimeUnit.SECONDS),401,"SESSION_REQUIRED");
            final String current=token("student1"); barrier.set(new CountDownLatch(1));
            tx.execute(status->{
                jdbc.queryForList("SELECT id FROM floor WHERE id=100 FOR UPDATE"); future[0]=pool.submit(()->request("POST","/reservations",body,current,"queued-clock")); awaitBarrier(barrier.get()); clock.instant=Instant.parse("2026-10-10T01:02:00Z"); return null;
            });
            error(future[0].get(15,TimeUnit.SECONDS),422,"INVALID_INPUT"); facts.put("createdRights",jdbc.queryForObject("SELECT COUNT(*) FROM short_reservation",Integer.class));
            barrier.set(new CountDownLatch(1));
            var later=shortBody(seat,"2026-10-10T01:03:00Z","2026-10-10T02:00:00Z");
            tx.execute(status->{
                jdbc.queryForList("SELECT id FROM floor WHERE id=100 FOR UPDATE"); future[0]=pool.submit(()->request("POST","/reservations",later,current,"queued-logout")); awaitBarrier(barrier.get());
                try { assertEquals(200,request("POST","/auth/logout",Map.of(),current,null).status()); } catch(Exception e) { throw new RuntimeException(e); }
                return null;
            });
            error(future[0].get(15,TimeUnit.SECONDS),401,"SESSION_REQUIRED"); facts.put("rightsAfterLogout",jdbc.queryForObject("SELECT COUNT(*) FROM short_reservation",Integer.class));
        } finally { pool.shutdownNow(); }
    }
    void awaitBarrier(CountDownLatch barrier) {
        // Capture the real request after authentication and before acquiring the
        // held floor guard. No sleep or elevated DB telemetry privileges.
        try { assertTrue(barrier.await(5,TimeUnit.SECONDS)); } catch(InterruptedException e) { Thread.currentThread().interrupt(); throw new RuntimeException(e); }
    }
    @Test void areaApprovalAndSeatBookingCannotBothCommit() throws Exception {
        String teacher=token("teacher1"),admin=token("admin1"),student=token("student1"); long area=jdbc.queryForObject("SELECT parent_id FROM space WHERE code='A001' AND floor_id=100",Long.class);
        var v=venue(teacher,area,"area-request");
        var result=race(()->decision(admin,v.id(),1,"APPROVE","area-approval"),()->request("POST","/reservations",shortBody(seat("A001",100),"2026-10-10T03:00:00Z","2026-10-10T04:00:00Z"),student,"child-booking"));
        assertEquals(List.of(200,409),result.stream().map(Reply::status).sorted().toList());
        facts.put("exclusiveEffects",jdbc.queryForObject("SELECT (SELECT COUNT(*) FROM venue_entitlement)+(SELECT COUNT(*) FROM short_reservation)",Integer.class));
    }
    @Test void pendingVenueDoesNotBlockAndParallelApprovalsSerialize() throws Exception {
        String teacher=token("teacher1"),admin=token("admin1"),other=token("admin2"); long room=seat("R-A",100);
        var a=venue(teacher,room,"venue-pending-a"); var c=venue(teacher,room,"venue-pending-c");
        assertTrue(request("GET","/spaces/"+room+"/availability?start=2026-10-10T03:00:00Z&end=2026-10-10T04:00:00Z",null,teacher,null).data().path("available").asBoolean());
        var result=race(()->decision(admin,a.id(),1,"APPROVE","parallel-admin-a"),()->decision(other,c.id(),1,"APPROVE","parallel-admin-c")); assertEquals(List.of(200,409),result.stream().map(Reply::status).sorted().toList());
        facts.put("venueRights",jdbc.queryForObject("SELECT COUNT(*) FROM venue_entitlement WHERE status='ACTIVE'",Integer.class));
        var elsewhere=venue(teacher,seat("R-C",101),"outside-request"); error(decision(other,elsewhere.id(),1,"APPROVE","outside-approve"),403,"SCOPE_FORBIDDEN");
        error(request("GET","/venue-requests/"+a.id(),null,token("student1"),null),403,"SCOPE_FORBIDDEN");
    }
    @Test void eventRequiresApprovedVenueAndCannotBypassOwnership() throws Exception {
        String teacher=token("teacher1"),admin=token("admin1"),student=token("student1"); var v=venue(teacher,seat("R-A",100),"event-venue"); var e=event(teacher,v.id(),"event-draft"); assertEquals(200,e.status());
        error(eventAction(teacher,e.id(),1,"PUBLISH",null,"unapproved-publish"),409,"VENUE_NOT_APPROVED");
        error(eventAction(student,e.id(),1,"PUBLISH",null,"student-publish"),403,"SCOPE_FORBIDDEN");
        assertEquals(200,decision(admin,v.id(),1,"APPROVE","event-approve").status()); assertEquals(200,eventAction(teacher,e.id(),1,"PUBLISH",null,"event-publish").status());
        error(decision(admin,v.id(),2,"CANCEL","bound-venue-cancel"),409,"EVENT_BOUND");
        facts.put("publishedEvents",jdbc.queryForObject("SELECT COUNT(*) FROM campus_event WHERE status='PUBLISHED'",Integer.class));
    }
    @Test void finalEventSlotAndCancellationPreserveFixedQueue() throws Exception {
        String teacher=token("teacher1"),admin=token("admin1"),a=token("student1"),c=token("student2"); long id=published(teacher,admin);
        var joined=race(()->request("POST","/events/"+id+"/participation",Map.of("action","JOIN"),a,"event-join-a"),()->request("POST","/events/"+id+"/participation",Map.of("action","JOIN"),c,"event-join-c")); assertTrue(joined.stream().allMatch(r->r.status()==200));
        facts.put("confirmedAtCapacity",jdbc.queryForObject("SELECT confirmed_count FROM campus_event WHERE id=?",Integer.class,id)); facts.put("waitingAtCapacity",jdbc.queryForObject("SELECT COUNT(*) FROM event_participation WHERE event_id=? AND status='WAITLISTED'",Integer.class,id));
        long first=jdbc.queryForObject("SELECT user_id FROM event_participation WHERE event_id=? AND status='CONFIRMED'",Long.class,id); String winner=first==1?a:c,waiting=first==1?c:a;
        assertEquals(200,request("POST","/events/"+id+"/participation",Map.of("action","CANCEL"),winner,"cancel-winner").status());
        assertEquals("CONFIRMED",request("GET","/events/"+id,null,waiting,null).data().at("/myParticipation/status").asString());
        assertEquals(200,request("POST","/events/"+id+"/participation",Map.of("action","JOIN"),winner,"join-again").status());
        facts.put("newSequence",jdbc.queryForObject("SELECT sequence_number FROM event_participation WHERE event_id=? AND user_id=?",Long.class,id,first));
        clock.instant=Instant.parse("2026-10-10T02:30:00Z"); error(request("POST","/events/"+id+"/participation",Map.of("action","JOIN"),winner,"closed-rejoin"),409,"REGISTRATION_CLOSED");
        facts.put("countAfterPromotion",jdbc.queryForObject("SELECT confirmed_count FROM campus_event WHERE id=?",Integer.class,id));
    }
    @Test void eventRebindingKeepsParticipationAndOutboxRetriesDeduplicate() throws Exception {
        String teacher=token("teacher1"),admin=token("admin1"),a=token("student1"); long id=published(teacher,admin);
        assertEquals(200,request("POST","/events/"+id+"/participation",Map.of("action","JOIN"),a,"join-before-move").status());
        var replacement=venue(teacher,seat("R-B",100),"replacement-request"); assertEquals(200,decision(admin,replacement.id(),1,"APPROVE","replacement-approve").status());
        long version=request("GET","/events/"+id,null,teacher,null).data().path("version").asLong(); assertEquals(200,eventAction(teacher,id,version,"REBIND",replacement.id(),"event-rebind").status());
        facts.put("participationAfterMove",request("GET","/events/"+id,null,a,null).data().at("/myParticipation/status").asString());
        facts.put("activeVenueAfterMove",jdbc.queryForObject("SELECT COUNT(*) FROM venue_entitlement WHERE status='ACTIVE'",Integer.class));
        while(notifications.dispatch()>0) {} notifications.dispatch();
        facts.put("inboxDuplicates",jdbc.queryForObject("SELECT COUNT(*)-COUNT(DISTINCT outbox_id) FROM inbox",Integer.class));
        facts.put("pendingNotifications",jdbc.queryForObject("SELECT COUNT(*) FROM notification_outbox WHERE delivered_at IS NULL",Integer.class));
        version=request("GET","/events/"+id,null,teacher,null).data().path("version").asLong(); assertEquals(200,eventAction(teacher,id,version,"CANCEL",null,"event-cancel").status());
        facts.put("participationAfterCancel",jdbc.queryForObject("SELECT status FROM event_participation WHERE event_id=?",String.class,id)); facts.put("activeVenueAfterCancel",jdbc.queryForObject("SELECT COUNT(*) FROM venue_entitlement WHERE status='ACTIVE'",Integer.class));
    }
    @Test void notificationFailureRollsBackEffectAndSameKeyCanRecover() throws Exception {
        String a=token("student1"); var body=shortBody(seat("A001",100),"2026-10-10T01:01:00Z","2026-10-10T02:00:00Z");
        org.mockito.Mockito.doThrow(new org.springframework.dao.DataAccessResourceFailureException("Injected outbox failure")).when(business).notify(org.mockito.ArgumentMatchers.anyLong(),org.mockito.ArgumentMatchers.anyString(),org.mockito.ArgumentMatchers.anyString(),org.mockito.ArgumentMatchers.anyString(),org.mockito.ArgumentMatchers.anyString(),org.mockito.ArgumentMatchers.anyLong());
        error(request("POST","/reservations",body,a,"recover-write"),503,"DATABASE_UNAVAILABLE");
        facts.put("rightsAfterFailure",jdbc.queryForObject("SELECT COUNT(*) FROM short_reservation",Integer.class)); facts.put("receiptsAfterFailure",jdbc.queryForObject("SELECT COUNT(*) FROM idempotency_receipt",Integer.class));
        org.mockito.Mockito.doCallRealMethod().when(business).notify(org.mockito.ArgumentMatchers.anyLong(),org.mockito.ArgumentMatchers.anyString(),org.mockito.ArgumentMatchers.anyString(),org.mockito.ArgumentMatchers.anyString(),org.mockito.ArgumentMatchers.anyString(),org.mockito.ArgumentMatchers.anyLong());
        assertEquals(200,request("POST","/reservations",body,a,"recover-write").status());
        facts.put("rightsAfterRecovery",jdbc.queryForObject("SELECT COUNT(*) FROM short_reservation",Integer.class)); facts.put("notificationEvents",jdbc.queryForObject("SELECT COUNT(*) FROM notification_outbox",Integer.class));
    }
    @Test void invalidWindowsAndForeignActionsHaveNoEffects() throws Exception {
        String a=token("student1"),c=token("student2"); long seat=seat("A001",100);
        error(request("POST","/reservations",shortBody(seat,"2026-10-10T14:00:00Z","2026-10-10T15:00:00Z"),a,"invalid-night"),422,"INVALID_INPUT");
        error(request("POST","/reservations",shortBody(seat,"2026-10-10T01:01:00Z","2026-10-10T06:00:00Z"),a,"invalid-length"),422,"INVALID_INPUT");
        error(request("POST","/reservations",shortBody(seat,"2026-10-10T01:01:00Z","2026-10-10T02:00:00Z"),a,null),422,"INVALID_INPUT");
        facts.put("rightsAfterInvalid",jdbc.queryForObject("SELECT COUNT(*) FROM short_reservation",Integer.class));
        var created=request("POST","/reservations",shortBody(seat,"2026-10-10T01:01:00Z","2026-10-10T02:00:00Z"),a,"valid-owner"); assertEquals(200,created.status());
        error(request("POST","/reservations/"+created.id()+"/actions",Map.of("version",1,"action","CANCEL"),c,"foreign-cancel"),403,"SCOPE_FORBIDDEN");
        error(request("POST","/reservations/"+created.id()+"/actions",Map.of("version",9,"action","CANCEL"),a,"stale-cancel"),409,"STALE_VERSION");
        facts.put("statusAfterForeignWrites",jdbc.queryForObject("SELECT status FROM short_reservation WHERE id=?",String.class,created.id()));
    }
    @Test void lostConfirmationResponseAfterCommitRecoversPastDeadline() throws Exception {
        String a=token("student1");
        var created=request("POST","/reservations",shortBody(seat("A001",100),"2026-10-10T01:01:00Z","2026-10-10T02:00:00Z"),a,"create-loss-case"); assertEquals(200,created.status());
        clock.instant=Instant.parse("2026-10-10T01:10:59Z");
        var relay=com.sun.net.httpserver.HttpServer.create(new InetSocketAddress("127.0.0.1",0),0); var executor=Executors.newSingleThreadExecutor(); relay.setExecutor(executor);
        var upstream=new java.util.concurrent.atomic.AtomicInteger(); var committed=new CountDownLatch(1);
        String path="/reservations/"+created.id()+"/actions"; var body=Map.of("version",1,"action","CHECK_IN");
        relay.createContext("/drop",exchange->{
            try {
                exchange.getRequestBody().readAllBytes(); var r=request("POST",path,body,a,"confirm-response-lost"); upstream.set(r.status());
                clock.instant=Instant.parse("2026-10-10T01:11:01Z"); committed.countDown();
                // Upstream responded after its transaction committed. Deliberately
                // close the transport without returning any headers or body.
            } catch(Exception e) { committed.countDown(); } finally { exchange.close(); }
        }); relay.start();
        try {
            var wire=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+relay.getAddress().getPort()+"/drop")).timeout(Duration.ofSeconds(5)).POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build();
            assertThrows(java.io.IOException.class,()->client.send(wire,HttpResponse.BodyHandlers.ofString())); assertTrue(committed.await(5,TimeUnit.SECONDS)); assertEquals(200,upstream.get());
            var replay=request("POST",path,body,a,"confirm-response-lost"); assertEquals(200,replay.status()); assertEquals("CHECKED_IN",replay.data().path("status").asString());
            shorts.expire(created.id());
            facts.put("rightAfterResponseLoss",jdbc.queryForObject("SELECT status FROM short_reservation WHERE id=?",String.class,created.id())); facts.put("confirmReceipts",jdbc.queryForObject("SELECT COUNT(*) FROM idempotency_receipt WHERE request_key='confirm-response-lost'",Integer.class)); facts.put("rights",jdbc.queryForObject("SELECT COUNT(*) FROM short_reservation",Integer.class));
        } finally { relay.stop(0); executor.shutdownNow(); }
    }
    @Test void missingReceiptDuringFlightIsNotProofOfNoCommit() throws Exception {
        String a=token("student1"); var body=shortBody(seat("A001",100),"2026-10-10T01:01:00Z","2026-10-10T02:00:00Z"); var entered=new CountDownLatch(1);
        org.mockito.Mockito.doAnswer(invocation->{entered.countDown(); return invocation.callRealMethod();}).when(business).floors(org.mockito.ArgumentMatchers.anyCollection());
        var pool=Executors.newSingleThreadExecutor(); final Future<Reply>[] future=new Future[1];
        try {
            new TransactionTemplate(manager).execute(status->{
                jdbc.queryForList("SELECT id FROM floor WHERE id=100 FOR UPDATE"); future[0]=pool.submit(()->request("POST","/reservations",body,a,"in-flight-request")); awaitBarrier(entered);
                try { error(request("GET","/receipts/in-flight-request",null,a,null),404,"RESOURCE_NOT_FOUND"); } catch(Exception e) { throw new RuntimeException(e); }
                return null;
            });
            var first=future[0].get(15,TimeUnit.SECONDS); assertEquals(200,first.status()); var retry=request("POST","/reservations",body,a,"in-flight-request"); assertEquals(200,retry.status()); assertEquals(first.id(),retry.id());
            facts.put("rights",jdbc.queryForObject("SELECT COUNT(*) FROM short_reservation",Integer.class)); facts.put("receipts",jdbc.queryForObject("SELECT COUNT(*) FROM idempotency_receipt WHERE request_key='in-flight-request'",Integer.class));
        } finally { pool.shutdownNow(); }
    }
}
