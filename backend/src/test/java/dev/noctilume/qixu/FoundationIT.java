package dev.noctilume.qixu;

import static org.junit.jupiter.api.Assertions.*;
import dev.noctilume.qixu.common.Digests;
import dev.noctilume.qixu.config.DemoInitializer;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.context.ApplicationContext;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.ReadinessState;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Real HTTP and real MySQL. Never silently falls back to an embedded database. */
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("demo")
class FoundationIT {
    @LocalServerPort int port;
    @Autowired JdbcTemplate jdbc;
    @Autowired JsonMapper json;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired ApplicationContext applicationContext;
    private final HttpClient client=HttpClient.newHttpClient();
    private static final Map<String,Object> OBSERVATIONS=new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<String,Object> measures=new LinkedHashMap<>();
    private final java.util.List<Map<String,Object>> requests=new ArrayList<>();
    private String caseName;
    record Reply(int status,JsonNode body,HttpResponse<String> raw) {}

    @DynamicPropertySource static void database(DynamicPropertyRegistry r) {
        String url=System.getenv("QIXU_TEST_DB_URL");
        String password=System.getenv("QIXU_TEST_DB_PASSWORD");
        if (url==null || !url.matches("jdbc:mysql://[^/]+/(qixu_test|qixu_ci)(\\?.*)?") || password==null || password.isBlank())
            throw new IllegalStateException("Real dedicated QIXU_TEST_DB_URL (qixu_test/qixu_ci) and password are required");
        r.add("spring.datasource.url",()->url);
        r.add("spring.datasource.username",()->System.getenv().getOrDefault("QIXU_TEST_DB_USER","qixu_test_app"));
        r.add("spring.datasource.password",()->password);
        r.add("qixu.allowed-origins",()->"http://localhost:6968");
        r.add("qixu.tasks-enabled",()->"false");
    }
    @BeforeEach void reset(TestInfo info) {
        caseName=info.getTestMethod().orElseThrow().getName();
        assertTrue(java.util.Set.of("qixu_test","qixu_ci").contains(jdbc.queryForObject("SELECT DATABASE()",String.class)));
        TestData.clearBusiness(jdbc);
        jdbc.update("DELETE FROM auth_session");
        jdbc.update("DELETE FROM login_attempt");
        jdbc.update("DELETE FROM audit_entry");
        jdbc.update("DELETE FROM external_identity");
        jdbc.update("UPDATE identity_user SET active=TRUE,auth_version=1 WHERE id BETWEEN 1 AND 5");
    }
    @AfterEach void record() { OBSERVATIONS.put(caseName,Map.of("requests",requests,"database",measures)); }
    @AfterAll static void retainNativeMeasurements() throws Exception {
        var target=Path.of("target/failsafe-reports/qixu-m1-observation.json"); Files.createDirectories(target.getParent());
        Files.writeString(target,JsonMapper.builder().build().writeValueAsString(OBSERVATIONS));
    }
    void measure(String key,Object value) { measures.put(key,value); }
    Reply request(String method,String path,Object body,String... headers) throws Exception {
        var builder=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path)).timeout(java.time.Duration.ofSeconds(10));
        for(int i=0;i<headers.length;i+=2) builder.header(headers[i],headers[i+1]);
        if (body!=null) builder.header("Content-Type","application/json");
        builder.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)));
        var response=client.send(builder.build(),HttpResponse.BodyHandlers.ofString());
        requests.add(Map.of("method",method,"path",path,"status",response.statusCode()));
        return new Reply(response.statusCode(),response.body().isBlank()?json.createObjectNode():json.readTree(response.body()),response);
    }
    Reply login(String user,String mode) throws Exception { return request("POST","/api/v1/auth/login",Map.of("username",user,"password","qixu-demo","mode",mode)); }
    String token(String user) throws Exception { var r=login(user,"BEARER"); assertEquals(200,r.status()); return r.body().path("data").path("token").asString(); }
    Reply get(String path,String token) throws Exception { return request("GET",path,null,"Authorization","Bearer "+token); }
    void error(Reply r,int status,String code) {
        assertEquals(status,r.status()); assertEquals(code,r.body().path("error").path("code").asString());
        assertTrue(r.body().path("requestId").asString().matches("[a-f0-9-]{36}"));
        assertFalse(r.raw().body().contains("password_hash")); assertFalse(r.raw().body().contains("jdbc:mysql"));
        assertFalse(r.raw().body().contains("stackTrace"));
    }
    @Test void bearerStoredAsDigestAndAuditContainsNoCredentials() throws Exception {
        String token=token("student1");
        var session=get("/api/v1/auth/session",token);
        assertEquals(200,session.status()); assertEquals("STUDENT",session.body().at("/data/actor/role").asString());
        assertEquals(Digests.sha256(token),jdbc.queryForObject("SELECT token_hash FROM auth_session",String.class));
        measure("hashedSessionRows",jdbc.queryForObject("SELECT COUNT(*) FROM auth_session WHERE token_hash=?",Integer.class,Digests.sha256(token)));
        measure("rawTokenRows",jdbc.queryForObject("SELECT COUNT(*) FROM auth_session WHERE token_hash=?",Integer.class,token));
        String audit=jdbc.queryForObject("SELECT CAST(detail_json AS CHAR) FROM audit_entry",String.class);
        assertFalse(audit.contains(token)); assertFalse(audit.contains("qixu-demo"));
        measure("rawCredentialAuditRows",jdbc.queryForObject("SELECT COUNT(*) FROM audit_entry WHERE CAST(detail_json AS CHAR) LIKE ? OR CAST(detail_json AS CHAR) LIKE '%qixu-demo%'",Integer.class,"%"+token+"%"));
        assertEquals("no-store",session.raw().headers().firstValue("Cache-Control").orElse(""));
    }
    @Test void cookieSessionNeedsBoundCsrfAndLogoutOnlyRevokesOneSession() throws Exception {
        String other=token("student1"); var login=login("student1","COOKIE");
        String set=login.raw().headers().firstValue("Set-Cookie").orElseThrow();
        assertTrue(set.contains("HttpOnly")); assertTrue(set.contains("SameSite=Lax")); assertTrue(set.contains("Path=/api"));
        assertTrue(login.body().at("/data/token").isMissingNode());
        String cookie=set.split(";")[0]; String csrf=login.body().at("/data/csrfToken").asString();
        error(request("POST","/api/v1/auth/logout",Map.of(),"Cookie",cookie),403,"CSRF_REQUIRED");
        error(request("POST","/api/v1/auth/logout",Map.of(),"Cookie",cookie,"X-CSRF-Token","wrong"),403,"CSRF_REQUIRED");
        assertEquals(200,request("POST","/api/v1/auth/logout",Map.of(),"Cookie",cookie,"X-CSRF-Token",csrf).status());
        error(request("GET","/api/v1/auth/session",null,"Cookie",cookie),401,"SESSION_REQUIRED");
        assertEquals(200,get("/api/v1/auth/session",other).status());
    }
    @Test void cookieReadBindingRejectsAnotherDocumentSessionAndSameActorRotation() throws Exception {
        Reply a=login("admin1","COOKIE"),b=login("teacher1","COOKIE"),c=login("admin1","COOKIE");
        String old=a.body().at("/data/csrfToken").asString(),cookieB=b.raw().headers().firstValue("Set-Cookie").orElseThrow().split(";")[0],cookieC=c.raw().headers().firstValue("Set-Cookie").orElseThrow().split(";")[0];
        error(request("GET","/api/v1/inbox",null,"Cookie",cookieB,"X-CSRF-Token",old),409,"SESSION_OWNER_CHANGED");
        error(request("GET","/api/v1/inbox",null,"Cookie",cookieC,"X-CSRF-Token",old),409,"SESSION_OWNER_CHANGED");
        assertEquals(200,request("GET","/api/v1/inbox",null,"Cookie",cookieB,"X-CSRF-Token",b.body().at("/data/csrfToken").asString()).status());
        assertEquals(200,request("GET","/api/v1/inbox",null,"Cookie",cookieB).status());
        assertEquals(3,request("GET","/api/v1/auth/session",null,"Cookie",cookieB,"X-CSRF-Token",old).body().at("/data/actor/id").asInt());
        assertEquals(200,request("GET","/api/v1/inbox",null,"Authorization","Bearer "+token("student1"),"X-CSRF-Token",old).status());
    }
    @Test void originAndMalformedAuthorizationDoNotFallBackToCookie() throws Exception {
        var login=login("student1","COOKIE"); String cookie=login.raw().headers().firstValue("Set-Cookie").orElseThrow().split(";")[0];
        error(request("GET","/api/v1/auth/session",null,"Cookie",cookie,"Authorization","Basic bad"),401,"SESSION_REQUIRED");
        error(request("GET","/api/health",null,"Origin","https://untrusted.example"),403,"ORIGIN_FORBIDDEN");
        var preflight=request("OPTIONS","/api/v1/auth/logout",null,"Origin","http://localhost:6968");
        assertEquals(204,preflight.status()); assertEquals("http://localhost:6968",preflight.raw().headers().firstValue("Access-Control-Allow-Origin").orElse(""));
    }
    @Test void expiryActiveFlagAndAuthVersionAreRechecked() throws Exception {
        String token=token("student1");
        jdbc.update("UPDATE auth_session SET expires_at=?",Timestamp.valueOf(LocalDateTime.now(ZoneOffset.UTC).minusSeconds(1)));
        error(get("/api/v1/auth/session",token),401,"SESSION_REQUIRED");
        token=token("student1"); jdbc.update("UPDATE identity_user SET auth_version=2 WHERE id=1");
        error(get("/api/v1/auth/session",token),401,"SESSION_REQUIRED");
        token=token("student1"); jdbc.update("UPDATE identity_user SET active=FALSE WHERE id=1");
        error(get("/api/v1/auth/session",token),401,"SESSION_REQUIRED");
    }
    @Test void claimedRoleDoesNotGrantAuthorityAndAdminFloorScopeIsEnforced() throws Exception {
        var attempt=request("POST","/api/v1/auth/login",Map.of("username","student1","password","qixu-demo","mode","BEARER","role","ADMIN","userId",4,"scope",101));
        assertEquals(200,attempt.status()); String student=attempt.body().at("/data/token").asString();
        error(get("/api/v1/admin/scope",student),403,"SCOPE_FORBIDDEN");
        error(get("/api/v1/admin/scope",token("teacher1")),403,"SCOPE_FORBIDDEN");
        String admin=token("admin2"); assertEquals(200,get("/api/v1/admin/scope?floor=100",admin).status());
        error(get("/api/v1/admin/scope?floor=101",admin),403,"SCOPE_FORBIDDEN");
        assertEquals(200,get("/api/v1/admin/scope?floor=101",token("admin1")).status());
    }
    @Test void invalidPasswordAttemptsSurviveRollbackAndResetOnlyAfterWindow() throws Exception {
        for(int i=0;i<5;i++) error(request("POST","/api/v1/auth/login",Map.of("username","student1","password","wrong")),401,"LOGIN_REJECTED");
        error(login("student1","BEARER"),429,"LOGIN_RATE_LIMIT");
        assertEquals(5,jdbc.queryForObject("SELECT failures FROM login_attempt",Integer.class));
        measure("persistedFailures",jdbc.queryForObject("SELECT failures FROM login_attempt",Integer.class));
        jdbc.update("UPDATE login_attempt SET window_end=UTC_TIMESTAMP(6)-INTERVAL 1 SECOND");
        assertEquals(200,login("student1","BEARER").status());
        assertEquals(0,jdbc.queryForObject("SELECT failures FROM login_attempt",Integer.class));
        measure("resetFailures",jdbc.queryForObject("SELECT failures FROM login_attempt",Integer.class));
    }
    @Test void unknownAccountAndInactiveIdentityUseSameRejection() throws Exception {
        error(request("POST","/api/v1/auth/login",Map.of("username","unknown1","password","qixu-demo")),401,"LOGIN_REJECTED");
        jdbc.update("UPDATE identity_user SET active=FALSE WHERE id=1");
        error(login("student1","BEARER"),401,"LOGIN_REJECTED");
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM auth_session",Integer.class));
        measure("createdSessions",jdbc.queryForObject("SELECT COUNT(*) FROM auth_session",Integer.class));
    }
    @Test void publicProfilesDoNotLeakOccupantAndFiltersAreLiteral() throws Exception {
        String token=token("student1");
        var profiles=get("/api/v1/spaces?floor=100&kind=SEAT&tag=window&size=50",token);
        assertEquals(200,profiles.status()); assertEquals(8,profiles.body().at("/data/total").asInt());
        measure("windowSeatMatches",profiles.body().at("/data/total").asInt());
        for(var space:profiles.body().at("/data/items")) {
            assertTrue(space.at("/profile/features/window").asBoolean()); assertEquals("NOT_QUERIED",space.path("availability").asString());
            assertEquals("DEMO",space.at("/profile/source").asString()); assertTrue(space.path("userId").isMissingNode());
        }
        var injection=get("/api/v1/spaces?search=%27%20OR%201%3D1%20--",token);
        assertEquals(0,injection.body().at("/data/total").asInt());
        measure("injectedQueryMatches",injection.body().at("/data/total").asInt());
        assertEquals(0,get("/api/v1/spaces?search=%25",token).body().at("/data/total").asInt());
        assertEquals(200,get("/api/v1/spaces/2000",token).status());
    }
    @Test void inputAndMissingResourceErrorsHaveSafeStableEnvelopes() throws Exception {
        String token=token("student1");
        error(get("/api/v1/spaces?page=0",token),422,"INVALID_INPUT");
        error(get("/api/v1/spaces?size=51",token),422,"INVALID_INPUT");
        error(get("/api/v1/spaces?tag=%24.password",token),422,"INVALID_INPUT");
        error(get("/api/v1/spaces?kind=DROP",token),422,"INVALID_INPUT");
        error(get("/api/v1/spaces?floor=abc",token),422,"INVALID_INPUT");
        error(get("/api/v1/spaces/999999",token),404,"RESOURCE_NOT_FOUND");
        error(get("/api/v1/does-not-exist",token),404,"NOT_FOUND");
        error(request("POST","/api/v1/auth/login",Map.of("username","student1","password","啊".repeat(25))),422,"INVALID_INPUT");
    }
    @Test void migrationAndIsolationAreRealMysqlAndSchemaPrivilegesAreContained() {
        String version=jdbc.queryForObject("SELECT VERSION()",String.class); measure("mysqlVersion",version); assertTrue(version.startsWith("8."));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM flyway_schema_history WHERE success=TRUE AND version='1'",Integer.class));
        measure("successfulV1Migrations",jdbc.queryForObject("SELECT COUNT(*) FROM flyway_schema_history WHERE success=TRUE AND version='1'",Integer.class));
        var tx=new TransactionTemplate(transactionManager); tx.setIsolationLevel(org.springframework.transaction.TransactionDefinition.ISOLATION_READ_COMMITTED);
        String isolation=tx.execute(status->jdbc.queryForObject("SELECT @@transaction_isolation",String.class)); measure("transactionIsolation",isolation); assertEquals("READ-COMMITTED",isolation);
        String schema=jdbc.queryForObject("SELECT DATABASE()",String.class);
        int outside=0;
        for(String grant:jdbc.query("SHOW GRANTS",(rs,n)->rs.getString(1))) {
            if (!grant.startsWith("GRANT USAGE ON *.*") && !grant.contains("`"+schema+"`.*")) outside++;
            if (grant.contains(" ON *.*")) assertTrue(grant.startsWith("GRANT USAGE ON *.*"));
            else assertTrue(grant.contains("`"+schema+"`.*"));
        }
        measure("outsideSchemaGrants",outside);
    }
    @Test void databaseRejectsCrossFloorParentAndInvalidGeometry() {
        assertThrows(DataIntegrityViolationException.class,()->jdbc.update("UPDATE space SET parent_id=1000 WHERE id=2200"));
        for(String sql:java.util.List.of("UPDATE space SET map_x=999 WHERE id=2000","UPDATE space SET capacity=0 WHERE id=2000")) {
            var failure=assertThrows(DataAccessException.class,()->jdbc.update(sql));
            assertInstanceOf(java.sql.SQLException.class,failure.getMostSpecificCause());
            assertEquals(3819,((java.sql.SQLException)failure.getMostSpecificCause()).getErrorCode());
        }
        assertEquals(65,jdbc.queryForObject("SELECT map_x FROM space WHERE id=2000",Integer.class));
        assertEquals(1,jdbc.queryForObject("SELECT capacity FROM space WHERE id=2000",Integer.class));
        measure("mapXAfterRejectedWrites",jdbc.queryForObject("SELECT map_x FROM space WHERE id=2000",Integer.class));
        measure("capacityAfterRejectedWrites",jdbc.queryForObject("SELECT capacity FROM space WHERE id=2000",Integer.class));
    }
    @Test void externalIdentityNamesAndIdsCannotCollideAcrossProviders() {
        jdbc.update("INSERT INTO external_identity(provider,subject,user_id) VALUES('darkroom','7',1),('campus','7',2)");
        assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM external_identity WHERE subject='7'",Integer.class));
        measure("sameSubjectDistinctProviderRows",jdbc.queryForObject("SELECT COUNT(*) FROM external_identity WHERE subject='7'",Integer.class));
        assertThrows(DataIntegrityViolationException.class,()->jdbc.update("INSERT INTO external_identity(provider,subject,user_id) VALUES('darkroom','7',2)"));
        assertEquals("STUDENT",jdbc.queryForObject("SELECT role FROM identity_user WHERE id=1",String.class));
        measure("localRoleAfterExternalBinding",jdbc.queryForObject("SELECT role FROM identity_user WHERE id=1",String.class));
    }
    @Test void demoCannotSeedWithoutExplicitProfileOrInProduction() {
        for(String[] profiles:new String[][]{{},{"prod"},{"demo","prod"},{"demo","production"}}) {
            var environment=new MockEnvironment(); environment.setActiveProfiles(profiles);
            assertThrows(IllegalStateException.class,()->new DemoInitializer(null,json,environment,true).run(null));
        }
        assertDoesNotThrow(()->new DemoInitializer(null,json,new MockEnvironment(),false).run(null));
    }
    @Test void initializationAndDrainDoNotAdvertiseFalseReadiness() throws Exception {
        AvailabilityChangeEvent.publish(applicationContext,ReadinessState.REFUSING_TRAFFIC);
        try {
            error(request("GET","/api/health",null),503,"APPLICATION_NOT_READY");
            error(login("student1","BEARER"),503,"APPLICATION_NOT_READY");
            measure("sessionsBeforeReadiness",jdbc.queryForObject("SELECT COUNT(*) FROM auth_session",Integer.class));
        } finally { AvailabilityChangeEvent.publish(applicationContext,ReadinessState.ACCEPTING_TRAFFIC); }
        assertEquals(200,request("GET","/api/health",null).status());
        assertEquals(200,login("student1","BEARER").status());
    }
}
