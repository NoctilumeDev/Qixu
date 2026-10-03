package dev.noctilume.qixu;

import static org.junit.jupiter.api.Assertions.*;
import com.sun.net.httpserver.HttpServer;
import dev.noctilume.qixu.common.Business;
import dev.noctilume.qixu.common.Digests;
import dev.noctilume.qixu.identity.DarkRoomGateway;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.*;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Real HTTP and MySQL. The upstream is explicitly a controlled protocol source, not DarkRoom. */
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("demo")
@Import(ExternalIdentityIT.TimeConfig.class)
class ExternalIdentityIT {
    static final String TICKET="synthetic.payload.signature";
    static final HttpServer source;
    static volatile int upstreamStatus=200;
    static volatile String profile=validProfile();
    static final AtomicInteger sourceCalls=new AtomicInteger();
    static {try{source=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);source.createContext("/api/dark-room-library/v1/user/auth",x->{sourceCalls.incrementAndGet();byte[] bytes=profile.getBytes(StandardCharsets.UTF_8);x.getResponseHeaders().set("Content-Type","application/json");x.sendResponseHeaders(upstreamStatus,bytes.length);x.getResponseBody().write(bytes);x.close();});source.start();}catch(Exception e){throw new ExceptionInInitializerError(e);}}
    static String validProfile(){return "{\"code\":200,\"data\":{\"id\":1,\"accountStatus\":0,\"isLogin\":false,\"userRole\":0,\"coordinatorAdmin\":true,\"userName\":\"admin1\"}}";}
    static String endpoint(){return "http://127.0.0.1:"+source.getAddress().getPort()+"/api/dark-room-library/v1/user/auth";}
    static class MutableClock extends Clock {Instant value=Instant.parse("2026-10-10T01:00:00Z");public ZoneId getZone(){return ZoneOffset.UTC;}public Clock withZone(ZoneId zone){return this;}public Instant instant(){return value;}}
    @TestConfiguration static class TimeConfig {@Bean @Primary MutableClock externalClock(){return new MutableClock();}}
    @DynamicPropertySource static void db(DynamicPropertyRegistry r){FoundationIT.database(r);r.add("qixu.external.dark-room.enabled",()->true);r.add("qixu.external.dark-room.auth-uri",ExternalIdentityIT::endpoint);r.add("qixu.external.dark-room.allow-loopback-http",()->true);r.add("qixu.external.ticket-key",()->Base64.getEncoder().encodeToString(new byte[32]));}
    @LocalServerPort int port;@Autowired JdbcTemplate jdbc;@Autowired JsonMapper json;@Autowired MutableClock clock;@Autowired PlatformTransactionManager manager;
    @MockitoSpyBean Business business;
    private final HttpClient client=HttpClient.newHttpClient();
    private final List<Map<String,Object>> requests=new ArrayList<>();private final Map<String,Object> facts=new LinkedHashMap<>();private String name;
    private static final Map<String,Object> observations=new LinkedHashMap<>();
    @BeforeEach void reset(TestInfo info){name=info.getTestMethod().orElseThrow().getName();clock.value=Instant.parse("2026-10-10T01:00:00Z");TestData.clearBusiness(jdbc);jdbc.update("DELETE FROM auth_session");jdbc.update("DELETE FROM external_identity");jdbc.update("DELETE FROM login_attempt");jdbc.update("DELETE FROM audit_entry");jdbc.update("UPDATE identity_user SET active=TRUE,auth_version=1,local_login_enabled=TRUE,student_verified=(role='STUDENT') WHERE id BETWEEN 1 AND 5");upstreamStatus=200;profile=validProfile();sourceCalls.set(0);}
    @AfterEach void retain(){org.mockito.Mockito.reset(business);facts.put("sessions",count("auth_session"));facts.put("receipts",count("idempotency_receipt"));facts.put("reservations",count("short_reservation"));facts.put("sourceCalls",sourceCalls.get());observations.put(name,Map.of("requests",requests,"database",facts,"upstream","CONTROLLED_PROTOCOL_SOURCE_NOT_DARK_ROOM"));jdbc.update("DELETE FROM auth_session");jdbc.update("DELETE FROM external_identity");jdbc.update("UPDATE identity_user SET local_login_enabled=TRUE WHERE id BETWEEN 1 AND 5");}
    @AfterAll static void output()throws Exception{source.stop(0);Path p=Path.of("target/failsafe-reports/qixu-m6-identity-observation.json");Files.createDirectories(p.getParent());Files.writeString(p,JsonMapper.builder().build().writeValueAsString(observations));}
    int count(String table){return jdbc.queryForObject("SELECT COUNT(*) FROM "+table,Integer.class);}
    record Reply(int status,JsonNode body,String raw){JsonNode data(){return body.path("data");}}
    Reply api(String method,String path,Object body,String token,String key)throws Exception{
        var b=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api/v1"+path)).timeout(Duration.ofSeconds(10));if(token!=null)b.header("Authorization","Bearer "+token);if(key!=null)b.header("Idempotency-Key",key);if(body!=null)b.header("Content-Type","application/json");
        var r=client.send(b.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build(),HttpResponse.BodyHandlers.ofString());requests.add(Map.of("method",method,"path",path,"status",r.statusCode()));return new Reply(r.statusCode(),json.readTree(r.body()),r.body());
    }
    void error(Reply r,int status,String code){assertEquals(status,r.status);assertEquals(code,r.body.at("/error/code").asString());assertFalse(r.raw.contains(TICKET));assertFalse(r.raw.contains("password_hash"));}
    long bind(long user){jdbc.update("UPDATE identity_user SET local_login_enabled=FALSE,auth_version=auth_version+1 WHERE id=?",user);jdbc.update("INSERT INTO external_identity(provider,subject,user_id,active,issuer_hash) VALUES(?, '1',?,TRUE,?)",DarkRoomGateway.PROVIDER,user,Digests.sha256(endpoint()));return jdbc.queryForObject("SELECT id FROM external_identity WHERE provider=?",Long.class,DarkRoomGateway.PROVIDER);}
    Reply exchange()throws Exception{return api("POST","/auth/external/dark-room",Map.of("ticket",TICKET,"mode","BEARER"),null,null);}
    String token()throws Exception{var r=exchange();assertEquals(200,r.status);return r.data().path("token").asString();}
    Map<String,Object> reservation(){return Map.of("spaceId",2000,"startsAt",clock.value.plusSeconds(600).toString(),"endsAt",clock.value.plusSeconds(4200).toString());}
    @Test void upstreamAdminCannotGrantLocalRoleOrAutoEnroll()throws Exception{
        error(exchange(),403,"EXTERNAL_BINDING_REQUIRED");assertEquals(0,count("external_identity"));assertEquals(0,count("auth_session"));facts.put("autoEnrolled",count("external_identity"));bind(1);String t=token();var r=api("GET","/auth/session",null,t,null);assertEquals("STUDENT",r.data().at("/actor/role").asString());error(api("GET","/admin/operations",null,t,null),403,"SCOPE_FORBIDDEN");facts.put("localRole",r.data().at("/actor/role").asString());
    }
    @Test void subjectAndIssuerMustMatchExplicitActiveBinding()throws Exception{
        jdbc.update("INSERT INTO external_identity(provider,subject,user_id,active,issuer_hash) VALUES('OTHER_PROVIDER','1',1,TRUE,?)",Digests.sha256(endpoint()));error(exchange(),403,"EXTERNAL_BINDING_REQUIRED");bind(1);jdbc.update("UPDATE external_identity SET issuer_hash=? WHERE provider=?",Digests.sha256("other issuer"),DarkRoomGateway.PROVIDER);error(exchange(),403,"EXTERNAL_BINDING_REQUIRED");jdbc.update("UPDATE external_identity SET issuer_hash=?,active=FALSE WHERE provider=?",Digests.sha256(endpoint()),DarkRoomGateway.PROVIDER);error(exchange(),403,"EXTERNAL_BINDING_REQUIRED");assertEquals(0,count("auth_session"));
    }
    @Test void externalOnlyCannotUsePasswordAndUpstreamCannotVerifyStudent()throws Exception{
        bind(1);jdbc.update("UPDATE identity_user SET student_verified=FALSE WHERE id=1");error(api("POST","/auth/login",Map.of("username","student1","password","qixu-demo","mode","BEARER"),null,null),401,"LOGIN_REJECTED");String t=token();error(api("POST","/reservations",reservation(),t,"external-unverified-student"),403,"SCOPE_FORBIDDEN");facts.put("localLoginEnabled",jdbc.queryForObject("SELECT local_login_enabled FROM identity_user WHERE id=1",Boolean.class));facts.put("studentVerified",jdbc.queryForObject("SELECT student_verified FROM identity_user WHERE id=1",Boolean.class));
    }
    @Test void externalTicketIsAadBoundEncryptedAndNeverInAudit()throws Exception{
        bind(1);String t=token();String cipher=jdbc.queryForObject("SELECT external_ticket_cipher FROM auth_session",String.class);assertTrue(cipher.startsWith("gcm1."));assertFalse(cipher.contains(TICKET));assertFalse(jdbc.queryForObject("SELECT CAST(detail_json AS CHAR) FROM audit_entry",String.class).contains(TICKET));assertEquals(200,api("GET","/auth/session",null,t,null).status);
        jdbc.update("UPDATE auth_session SET external_ticket_cipher=CONCAT(external_ticket_cipher,'x')");error(api("GET","/auth/session",null,t,null),503,"IDENTITY_UNAVAILABLE");facts.put("plaintextTicketRows",jdbc.queryForObject("SELECT COUNT(*) FROM auth_session WHERE external_ticket_cipher LIKE ?",Integer.class,"%"+TICKET+"%"));facts.put("credentialAuditRows",jdbc.queryForObject("SELECT COUNT(*) FROM audit_entry WHERE CAST(detail_json AS CHAR) LIKE ?",Integer.class,"%"+TICKET+"%"));
    }
    @Test void definiteUpstreamRejectionRevokesWhileUnknownPreservesSession()throws Exception{
        bind(1);String t=token();upstreamStatus=503;error(api("GET","/auth/session",null,t,null),503,"IDENTITY_UNAVAILABLE");assertEquals(1,count("auth_session"));upstreamStatus=200;assertEquals(200,api("GET","/auth/session",null,t,null).status);upstreamStatus=401;error(api("GET","/auth/session",null,t,null),401,"EXTERNAL_IDENTITY_REJECTED");assertEquals(0,count("auth_session"));upstreamStatus=200;error(api("GET","/auth/session",null,t,null),401,"SESSION_REQUIRED");
    }
    @Test void activeBindingVersionAndLocalRevocationAreCurrentFacts()throws Exception{
        long id=bind(1);String t=token();jdbc.update("UPDATE external_identity SET version=version+1 WHERE id=?",id);error(api("GET","/auth/session",null,t,null),401,"SESSION_REQUIRED");String second=token();jdbc.update("UPDATE identity_user SET auth_version=auth_version+1 WHERE id=1");error(api("GET","/auth/session",null,second,null),401,"SESSION_REQUIRED");String third=token();jdbc.update("UPDATE external_identity SET active=FALSE WHERE id=?",id);error(api("GET","/auth/session",null,third,null),401,"SESSION_REQUIRED");facts.put("reservationsAfterRevocation",0);
    }
    @Test void malformedProfilesAndDisabledFlagsNeverAuthorize()throws Exception{
        bind(1);for(String raw:List.of("{\"code\":\"200\",\"data\":{\"id\":1}}",validProfile().replace("\"id\":1","\"id\":\"1\""),validProfile().replace("\"isLogin\":false","\"isLogin\":0"),validProfile().replace("\"id\":1","\"id\":1,\"id\":2"),validProfile()+"{}",validProfile().replace("\"accountStatus\":0","\"accountStatus\":4294967296"))){profile=raw;error(exchange(),503,"IDENTITY_UNAVAILABLE");}
        profile=validProfile().replace("\"isLogin\":false","\"isLogin\":true");error(exchange(),401,"EXTERNAL_IDENTITY_REJECTED");profile=validProfile().replace("\"accountStatus\":0","\"accountStatus\":1");error(exchange(),401,"EXTERNAL_IDENTITY_REJECTED");assertEquals(0,count("auth_session"));
    }
    @Test void localIdentityAndLogoutDoNotNeedUnavailableProvider()throws Exception{
        bind(1);String t=token();upstreamStatus=503;var local=api("POST","/auth/login",Map.of("username","student2","password","qixu-demo","mode","BEARER"),null,null);assertEquals(200,local.status);assertEquals(200,api("GET","/spaces",null,local.data().path("token").asString(),null).status);int before=sourceCalls.get();assertEquals(200,api("POST","/auth/logout",null,t,null).status);assertEquals(before,sourceCalls.get());facts.put("localIndependent",true);facts.put("logoutUpstreamCalls",0);
    }
    @Test void proofExpiresAfterActualFloorWaitWithoutNetworkInsideLocks()throws Exception{
        bind(1);String t=token();var entered=new CountDownLatch(1);org.mockito.Mockito.doAnswer(i->{entered.countDown();return i.callRealMethod();}).when(business).floors(org.mockito.ArgumentMatchers.anyCollection());var pool=Executors.newSingleThreadExecutor();final Future<Reply>[] pending=new Future[1];int calls=sourceCalls.get();
        try{new TransactionTemplate(manager).executeWithoutResult(status->{jdbc.queryForList("SELECT id FROM floor WHERE id=100 FOR UPDATE");pending[0]=pool.submit(()->api("POST","/reservations",reservation(),t,"expired-external-floor-wait"));try{assertTrue(entered.await(5,TimeUnit.SECONDS));}catch(Exception e){throw new RuntimeException(e);}clock.value=clock.value.plusSeconds(4);});error(pending[0].get(10,TimeUnit.SECONDS),503,"IDENTITY_UNAVAILABLE");assertEquals(calls+1,sourceCalls.get());assertEquals(0,count("short_reservation"));assertEquals(0,count("idempotency_receipt"));facts.put("inLockRenewalCalls",0);}finally{pool.shutdownNow();}
    }
    @Test void bindingRemovalDuringFloorWaitRollsBackOriginal()throws Exception{
        long binding=bind(1);String t=token();var entered=new CountDownLatch(1);org.mockito.Mockito.doAnswer(i->{entered.countDown();return i.callRealMethod();}).when(business).floors(org.mockito.ArgumentMatchers.anyCollection());var pool=Executors.newSingleThreadExecutor();final Future<Reply>[] pending=new Future[1];
        try{new TransactionTemplate(manager).executeWithoutResult(status->{jdbc.queryForList("SELECT id FROM floor WHERE id=100 FOR UPDATE");pending[0]=pool.submit(()->api("POST","/reservations",reservation(),t,"removed-external-floor-wait"));try{assertTrue(entered.await(5,TimeUnit.SECONDS));}catch(Exception e){throw new RuntimeException(e);}jdbc.update("UPDATE external_identity SET active=FALSE,version=version+1 WHERE id=?",binding);});error(pending[0].get(10,TimeUnit.SECONDS),401,"SESSION_REQUIRED");assertEquals(0,count("short_reservation"));assertEquals(0,count("idempotency_receipt"));}finally{pool.shutdownNow();}
    }
    @Test void ingressRateLimitPersistsAcrossRejectedExchanges()throws Exception{
        for(int i=0;i<20;i++)error(exchange(),403,"EXTERNAL_BINDING_REQUIRED");error(exchange(),429,"LOGIN_RATE_LIMIT");assertEquals(20,sourceCalls.get());assertEquals(20,jdbc.queryForObject("SELECT failures FROM login_attempt",Integer.class));facts.put("admittedRequests",20);
    }
    @Test void providerOutageDoesNotEraseCommittedReceiptOrDuplicateRecoveredWrite()throws Exception{
        bind(1);String t=token(),key="external-committed-before-outage";var body=reservation();var original=api("POST","/reservations",body,t,key);assertEquals(200,original.status);upstreamStatus=503;
        error(api("GET","/receipts/"+key,null,t,null),503,"IDENTITY_UNAVAILABLE");error(api("POST","/reservations",body,t,key),503,"IDENTITY_UNAVAILABLE");assertEquals(1,count("short_reservation"));assertEquals(1,count("idempotency_receipt"));upstreamStatus=200;
        assertEquals(original.data(),api("GET","/receipts/"+key,null,t,null).data().path("result"));assertEquals(original.data(),api("POST","/reservations",body,t,key).data());
        var changed=new HashMap<String,Object>(body);changed.put("spaceId",2001);error(api("POST","/reservations",changed,t,key),409,"IDEMPOTENCY_CONFLICT");
        var other=api("POST","/auth/login",Map.of("username","student2","password","qixu-demo","mode","BEARER"),null,null);error(api("GET","/receipts/"+key,null,other.data().path("token").asString(),null),404,"RESOURCE_NOT_FOUND");
        facts.put("shortAuditRows",jdbc.queryForObject("SELECT COUNT(*) FROM audit_entry WHERE action='SHORT_CREATED'",Integer.class));facts.put("shortOutboxRows",jdbc.queryForObject("SELECT COUNT(*) FROM notification_outbox WHERE entity_type='SHORT'",Integer.class));
    }
}
