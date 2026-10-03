package dev.noctilume.qixu;

import static org.junit.jupiter.api.Assertions.*;
import java.net.*;
import java.net.http.*;
import java.util.*;
import java.util.concurrent.*;
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
@ActiveProfiles("demo")
class IntentRecoveryIT {
    @LocalServerPort int port; @Autowired JdbcTemplate jdbc; @Autowired JsonMapper json;
    private final HttpClient client=HttpClient.newHttpClient();
    private static final Map<String,Object> observations=new LinkedHashMap<>();
    private final List<Map<String,Object>> requests=Collections.synchronizedList(new ArrayList<>());private String caseName;
    @DynamicPropertySource static void database(DynamicPropertyRegistry r) {
        String url=System.getenv("QIXU_TEST_DB_URL"),password=System.getenv("QIXU_TEST_DB_PASSWORD");
        if(url==null||!url.matches("jdbc:mysql://[^/]+/(qixu_test|qixu_ci)(\\?.*)?")||password==null||password.isBlank())throw new IllegalStateException("Dedicated real MySQL required");
        r.add("spring.datasource.url",()->url);r.add("spring.datasource.username",()->System.getenv("QIXU_TEST_DB_USER"));r.add("spring.datasource.password",()->password);r.add("qixu.tasks-enabled",()->"false");
    }
    @BeforeEach void reset(TestInfo info) {caseName=info.getTestMethod().orElseThrow().getName();TestData.clearBusiness(jdbc);jdbc.update("DELETE FROM auth_session");jdbc.update("DELETE FROM login_attempt");jdbc.update("UPDATE identity_user SET active=TRUE,auth_version=1 WHERE id BETWEEN 1 AND 5");}
    @AfterEach void record() {observations.put(caseName,Map.of("requests",new ArrayList<>(requests),"database",Map.of("receipts",count("idempotency_receipt"),"reports",count("feedback_report"),"favorites",count("favorite_space"))));}
    @AfterAll static void retain() throws Exception {Path p=Path.of("target/failsafe-reports/qixu-m5-intent-observation.json");Files.createDirectories(p.getParent());Files.writeString(p,JsonMapper.builder().build().writeValueAsString(observations));}
    record Reply(int status,JsonNode data) {}
    Reply request(String method,String path,Object body,String token,String key) throws Exception {
        var b=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api/v1"+path)).timeout(java.time.Duration.ofSeconds(15));
        if(token!=null)b.header("Authorization","Bearer "+token);if(body!=null)b.header("Content-Type","application/json");if(key!=null)b.header("Idempotency-Key",key);
        var response=client.send(b.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build(),HttpResponse.BodyHandlers.ofString());
        requests.add(Map.of("method",method,"path",path,"status",response.statusCode()));
        return new Reply(response.statusCode(),json.readTree(response.body()).path("data"));
    }
    String login(String user) throws Exception {var r=request("POST","/auth/login",Map.of("username",user,"password","qixu-demo","mode","BEARER"),null,null);assertEquals(200,r.status);return r.data.path("token").asString();}
    Reply stop(String token,String key) throws Exception {return request("POST","/receipts/"+key+"/stop",null,token,null);}
    Reply report(String token,String key) throws Exception {return request("POST","/feedback",Map.of("spaceId",2000,"category","OUTLET","description","synthetic test report"),token,key);}
    int count(String table) {return jdbc.queryForObject("SELECT COUNT(*) FROM "+table,Integer.class);}
    @Test void stopBeforeOriginalMakesLateOriginalImpossibleAndIsRepeatable() throws Exception {
        String t=login("student1"),key="stop_before_original";var a=stop(t,key);assertEquals(200,a.status);assertEquals("STOPPED_WITHOUT_EFFECT",a.data.at("/result/intentOutcome").asString());
        assertEquals(409,report(t,key).status);assertEquals(0,count("feedback_report"));assertEquals(a.data,stop(t,key).data);assertEquals(1,count("idempotency_receipt"));
    }
    @Test void existingOriginalAlwaysWinsAndStopCannotCancelBusinessFacts() throws Exception {
        String t=login("student1"),key="original_before_stop";var original=report(t,key);assertEquals(200,original.status);var r=stop(t,key);assertEquals(200,r.status);assertEquals(original.data,r.data.path("result"));assertFalse(r.data.path("result").has("intentOutcome"));
        assertEquals(1,count("feedback_report"));assertEquals(1,count("idempotency_receipt"));assertEquals(original.data,report(t,key).data);
        var favorite=request("POST","/favorites",Map.of("spaceId",2001,"selected",true),t,"favorite_preserved");assertEquals(200,favorite.status);stop(t,"favorite_preserved");assertEquals(1,count("favorite_space"));
    }
    @Test void anotherActorHasSeparateKeyNamespaceAndCannotStopThisOriginal() throws Exception {
        String a=login("student1"),b=login("student2"),key="same_text_two_actors";assertEquals(200,report(a,key).status);assertEquals("STOPPED_WITHOUT_EFFECT",stop(b,key).data.at("/result/intentOutcome").asString());
        assertEquals(1,count("feedback_report"));assertEquals(2,count("idempotency_receipt"));assertEquals("feedback.create",stop(a,key).data.path("operation").asString());
    }
    @Test void unknown404IsNotProofButBarrierIsARealPersistentFact() throws Exception {
        String t=login("student1"),key="absent_then_stopped";assertEquals(404,request("GET","/receipts/"+key,null,t,null).status);assertEquals(0,count("idempotency_receipt"));assertEquals(200,stop(t,key).status);
        assertEquals("client.intent-stop",request("GET","/receipts/"+key,null,t,null).data.path("operation").asString());assertEquals(409,report(t,key).status);
    }
    @Test void expiredSessionAndInvalidKeyDoNotCreateBarrier() throws Exception {
        String t=login("student1");assertEquals(422,stop(t,"short").status);jdbc.update("UPDATE identity_user SET auth_version=auth_version+1 WHERE id=1");assertEquals(401,stop(t,"expired_identity_key").status);assertEquals(0,count("idempotency_receipt"));
    }
    @Test void racingOriginalAndStopProduceOneFactAndExactlyTheCorrespondingEffect() throws Exception {
        String t=login("student1");var pool=Executors.newFixedThreadPool(2);int committed=0;
        try {for(int i=0;i<20;i++) {
            String key="race_original_stop_"+i;var start=new CyclicBarrier(2);
            var original=pool.submit(()->{start.await();return report(t,key);});var stopping=pool.submit(()->{start.await();return stop(t,key);});
            var a=original.get(20,TimeUnit.SECONDS);var b=stopping.get(20,TimeUnit.SECONDS);assertEquals(200,b.status);
            if(b.data.at("/result/intentOutcome").asString().equals("STOPPED_WITHOUT_EFFECT")){assertEquals(409,a.status);}else{assertEquals(200,a.status);assertEquals(a.data,b.data.path("result"));committed++;}
            assertEquals(i+1,count("idempotency_receipt"));assertEquals(committed,count("feedback_report"));
        }} finally {pool.shutdownNow();}
        assertEquals(20,count("idempotency_receipt"));assertEquals(committed,count("feedback_report"));
    }
}
