package dev.noctilume.qixu;

import static org.junit.jupiter.api.Assertions.*;
import dev.noctilume.qixu.notifications.Notifications;
import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Small bounded protocol traces and an independent raw-SQL projection oracle. */
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("demo")
class ProtocolProjectionIT {
    @LocalServerPort int port;
    @Autowired JdbcTemplate jdbc; @Autowired JsonMapper json; @Autowired Notifications notifications;
    private final HttpClient client=HttpClient.newHttpClient();
    private static final Map<String,Object> observations=new LinkedHashMap<>();
    private final List<Map<String,Object>> requests=new ArrayList<>();
    private final Map<String,Object> measures=new LinkedHashMap<>(); private String caseName;
    @DynamicPropertySource static void database(DynamicPropertyRegistry r) {
        String url=System.getenv("QIXU_TEST_DB_URL"),password=System.getenv("QIXU_TEST_DB_PASSWORD");
        if(url==null||!url.matches("jdbc:mysql://[^/]+/(qixu_test|qixu_ci)(\\?.*)?")||password==null||password.isBlank())throw new IllegalStateException("Dedicated real MySQL required");
        r.add("spring.datasource.url",()->url);r.add("spring.datasource.username",()->System.getenv("QIXU_TEST_DB_USER"));r.add("spring.datasource.password",()->password);r.add("qixu.tasks-enabled",()->"false");
    }
    @BeforeEach void reset(TestInfo info) {
        caseName=info.getTestMethod().orElseThrow().getName();TestData.clearBusiness(jdbc);
        jdbc.update("DELETE FROM auth_session");jdbc.update("DELETE FROM login_attempt");
        jdbc.update("UPDATE identity_user SET active=TRUE,auth_version=1 WHERE id BETWEEN 1 AND 5");
    }
    @AfterEach void record() {observations.put(caseName,Map.of("requests",new ArrayList<>(requests),"database",new LinkedHashMap<>(measures)));}
    @AfterAll static void retain() throws Exception {
        var path=Path.of("target/failsafe-reports/qixu-m7-protocol-observation.json");Files.createDirectories(path.getParent());
        Files.writeString(path,JsonMapper.builder().build().writeValueAsString(observations));
    }
    record Reply(int status,JsonNode body) {JsonNode data(){return body.path("data");}}
    Reply request(String method,String path,String raw,String token,String key) throws Exception {
        var builder=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api/v1"+path)).timeout(java.time.Duration.ofSeconds(10));
        if(token!=null)builder.header("Authorization","Bearer "+token);
        if(raw!=null)builder.header("Content-Type","application/json");if(key!=null)builder.header("Idempotency-Key",key);
        var response=client.send(builder.method(method,raw==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(raw)).build(),HttpResponse.BodyHandlers.ofString());
        requests.add(Map.of("method",method,"path",path,"status",response.statusCode()));
        return new Reply(response.statusCode(),json.readTree(response.body()));
    }
    String login(String user) throws Exception {
        var r=request("POST","/auth/login",json.writeValueAsString(Map.of("username",user,"password","qixu-demo","mode","BEARER")),null,null);
        assertEquals(200,r.status);return r.data().path("token").asString();
    }
    int count(String table){return jdbc.queryForObject("SELECT COUNT(*) FROM "+table,Integer.class);}
    void safeInvalid(Reply reply){assertEquals(422,reply.status);assertEquals("INVALID_INPUT",reply.body.at("/error/code").asString());assertTrue(reply.body.path("requestId").asString().matches("[a-f0-9-]{36}"));}
    @Test void duplicateAndEscapedObjectKeysNeverChooseAnImplicitWinner() throws Exception {
        String token=login("student1");
        List<String> payloads=List.of("{\"spaceId\":2000,\"spaceId\":2001,\"selected\":true}","{\"spaceId\":2000,\"selected\":true,\"selected\":false}","{\"spaceId\":2000,\"space\\u0049d\":2001,\"selected\":true}");
        var replies=new ArrayList<Reply>();for(int i=0;i<payloads.size();i++)replies.add(request("POST","/favorites",payloads.get(i),token,"m7_duplicate_key_"+i));
        measures.put("payloads",payloads);measures.put("favorites",count("favorite_space"));measures.put("receipts",count("idempotency_receipt"));measures.put("rejected",replies.stream().filter(r->r.status==422).count());
        for(var reply:replies)safeInvalid(reply);assertEquals(0,count("favorite_space"));assertEquals(0,count("idempotency_receipt"));
    }
    @Test void trailingDocumentsRejectWithoutEffectsAndWhitespaceRemainsValid() throws Exception {
        String token=login("student1"),body="{\"spaceId\":2000,\"selected\":true}";
        var replies=new ArrayList<Reply>();int i=0;for(String suffix:List.of("{}","true","[]"))replies.add(request("POST","/favorites",body+suffix,token,"m7_trailing_doc_"+(i++)));
        measures.put("favoritesAfterInvalid",count("favorite_space"));measures.put("receiptsAfterInvalid",count("idempotency_receipt"));measures.put("rejected",replies.stream().filter(r->r.status==422).count());
        var valid=request("POST","/favorites",body+" \r\n\t",token,"m7_whitespace_valid");
        measures.put("favoritesAfterValid",count("favorite_space"));measures.put("receiptsAfterValid",count("idempotency_receipt"));
        for(var reply:replies)safeInvalid(reply);assertEquals(200,valid.status);assertEquals(1,count("favorite_space"));assertEquals(1,count("idempotency_receipt"));
    }
    @Test void inboxPagingFindsOldUnreadResultsWithoutForeignRowsOrFalseCompleteness() throws Exception {
        for(int i=0;i<151;i++){
            jdbc.update("INSERT INTO notification_outbox(recipient_id,event_key,title,message,entity_type,entity_id,created_at) VALUES(1,?,?,?,'BATCH',7,UTC_TIMESTAMP(6))","m7_own_"+i,"本轮明确结果 "+i,"原始通知 "+i);
            if(i<5)jdbc.update("INSERT INTO notification_outbox(recipient_id,event_key,title,message,entity_type,entity_id,created_at) VALUES(2,?,?,?,'BATCH',8,UTC_TIMESTAMP(6))","m7_foreign_"+i,"其他人通知","不得出现");
        }
        while(notifications.dispatch()>0){}assertEquals(156,count("inbox"));
        List<Long> oracle=jdbc.queryForList("SELECT id FROM inbox WHERE recipient_id=1 ORDER BY id DESC",Long.class);
        String token=login("student1");var observed=new ArrayList<Long>();var first=request("GET","/inbox?page=1&size=20",null,token,null);
        measures.put("firstPageSize",first.data().path("items").size());measures.put("reportedTotal",first.data().path("total").asLong(-1));measures.put("sqlTotal",oracle.size());
        for(int page=1;page<=8;page++){
            var r=page==1?first:request("GET","/inbox?page="+page+"&size=20",null,token,null);assertEquals(200,r.status);
            for(var item:r.data().path("items")){assertEquals(1,item.path("recipient_id").asLong());observed.add(item.path("id").asLong());}
        }
        measures.put("observedUnique",new HashSet<>(observed).size());measures.put("oracleEqual",oracle.equals(observed));
        long oldest=oracle.get(oracle.size()-1),foreign=jdbc.queryForObject("SELECT MIN(id) FROM inbox WHERE recipient_id=2",Long.class);
        assertEquals(404,request("POST","/inbox/"+foreign+"/read",null,token,"m7_foreign_inbox").status);
        assertEquals(200,request("POST","/inbox/"+oldest+"/read",null,token,"m7_old_inbox_read").status);
        assertEquals(200,request("POST","/inbox/"+oldest+"/read",null,token,"m7_old_inbox_read").status);
        long unread=jdbc.queryForObject("SELECT COUNT(*) FROM inbox WHERE recipient_id=1 AND read_at IS NULL",Long.class);measures.put("unreadAfter",unread);
        var overflow=request("GET","/inbox?page=999&size=20",null,token,null);assertEquals(200,overflow.status);assertEquals(0,overflow.data().path("items").size());
        for(String invalid:List.of("page=0&size=20","page=1&size=0","page=1&size=101","page=2147483647&size=20"))safeInvalid(request("GET","/inbox?"+invalid,null,token,null));
        assertEquals(20,first.data().path("items").size());assertEquals(151,first.data().path("total").asLong());assertEquals(151,first.data().path("unread").asLong());
        assertEquals(oracle,observed);assertEquals(150,unread);assertEquals(1,count("idempotency_receipt"));
    }
}
