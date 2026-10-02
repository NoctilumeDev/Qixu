package dev.noctilume.qixu;

import static org.junit.jupiter.api.Assertions.*;
import dev.noctilume.qixu.common.Business;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.*;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.*;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("demo")
@Import(FeedbackIT.TimeConfig.class)
class FeedbackIT {
    static class TestClock extends Clock {
        @Override public ZoneId getZone() {return ZoneOffset.UTC;}
        @Override public Clock withZone(ZoneId zone) {return this;}
        @Override public Instant instant() {return Instant.parse("2026-10-10T01:00:00Z");}
    }
    @TestConfiguration static class TimeConfig {@Bean @Primary TestClock testClock() {return new TestClock();}}
    @DynamicPropertySource static void database(DynamicPropertyRegistry r) {FoundationIT.database(r);}
    @LocalServerPort int port;
    @Autowired JdbcTemplate jdbc;
    @Autowired JsonMapper json;
    @Autowired org.springframework.transaction.PlatformTransactionManager manager;
    @MockitoSpyBean Business business;
    private final HttpClient client=HttpClient.newHttpClient();
    private final List<Map<String,Object>> requests=new CopyOnWriteArrayList<>();
    private final Map<String,Object> facts=new LinkedHashMap<>();
    private static final Map<String,Object> OBSERVATIONS=new ConcurrentHashMap<>();
    private String name;
    record Reply(int status,JsonNode body) {JsonNode data() {return body.path("data");}long id() {return data().path("id").asLong();}}
    @BeforeEach void reset(TestInfo info) {
        name=info.getTestMethod().orElseThrow().getName();TestData.clearBusiness(jdbc);jdbc.update("DELETE FROM auth_session");jdbc.update("DELETE FROM login_attempt");jdbc.update("DELETE FROM audit_entry");
        jdbc.update("UPDATE identity_user SET active=TRUE,auth_version=1 WHERE id BETWEEN 1 AND 5");
        jdbc.update("INSERT IGNORE INTO admin_scope(user_id,floor_id) VALUES(4,100),(4,101),(5,100)");
        jdbc.update("INSERT IGNORE INTO space(id,floor_id,code,name,kind,use_mode,capacity,map_x,map_y,map_w,map_h,profile_json) VALUES(4990,101,'FEEDBACK-TEST','反馈验收专用位置','SEAT','BOOKABLE',1,10,700,30,30,JSON_OBJECT())");
        jdbc.update("UPDATE space SET floor_id=101,version=1,active=TRUE,profile_json=JSON_OBJECT('source','TEST_FIXTURE','features',JSON_OBJECT('window',TRUE,'outlet',TRUE,'quiet',TRUE,'accessible',FALSE)) WHERE id=4990");
    }
    @AfterEach void retain() {M4Measurements.collect(jdbc,facts);OBSERVATIONS.put(name,Map.of("requests",requests,"database",facts));}
    @AfterAll static void write() throws Exception {var p=Path.of("target/failsafe-reports/qixu-m4-feedback-observation.json");Files.createDirectories(p.getParent());Files.writeString(p,JsonMapper.builder().build().writeValueAsString(OBSERVATIONS));}
    Reply request(String method,String path,Object body,String token,String key) throws Exception {
        var builder=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api/v1"+path)).timeout(Duration.ofSeconds(15));if(token!=null)builder.header("Authorization","Bearer "+token);if(key!=null)builder.header("Idempotency-Key",key);
        if(body!=null)builder.header("Content-Type","application/json");builder.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)));
        var result=client.send(builder.build(),HttpResponse.BodyHandlers.ofString());requests.add(Map.of("method",method,"path",path,"status",result.statusCode()));return new Reply(result.statusCode(),json.readTree(result.body()));
    }
    String token(String user) throws Exception {var r=request("POST","/auth/login",Map.of("username",user,"password","qixu-demo","mode","BEARER"),null,null);assertEquals(200,r.status(),r.body().toString());return r.data().path("token").asString();}
    void error(Reply r,int status,String code) {assertEquals(status,r.status(),r.body().toString());assertEquals(code,r.body().at("/error/code").asString());}
    Reply report(String student,String key) throws Exception {var r=request("POST","/feedback",Map.of("spaceId",4990,"category","OUTLET","description","测试报告：插座没有供电；含私密联系方式 TEST_PRIVATE_7788"),student,key);assertEquals(200,r.status(),r.body().toString());return r;}
    Reply verify(String admin,long id) throws Exception {var r=request("POST","/admin/feedback/"+id+"/actions",Map.of("version",1,"action","VERIFY","reason","验收专用核实记录","facts",List.of(Map.of("key","outlet","value",false),Map.of("key","outletCondition","value","BROKEN"))),admin,"verify-report-"+id);assertEquals(200,r.status(),r.body().toString());return r;}
    Map<String,Object> repairBody(List<Reply> reports) {return Map.of("reports",reports.stream().map(r->Map.of("id",r.id(),"version",r.data().path("version").asLong())).toList(),"description","维修验收专用事项");}
    Reply repair(String admin,List<Reply> reports,String key) throws Exception {var r=request("POST","/admin/repairs",repairBody(reports),admin,key);assertEquals(200,r.status(),r.body().toString());return r;}
    Reply repairAction(String admin,long id,long version,String action,Boolean passed,List<Map<String,Object>> values,String key) throws Exception {
        var body=new HashMap<String,Object>();body.put("version",version);body.put("action",action);body.put("reason","验收专用处理依据");body.put("assignee","测试维修人员");if(passed!=null)body.put("verified",passed);if(values!=null)body.put("facts",values);return request("POST","/admin/repairs/"+id+"/actions",body,admin,key);
    }
    @Test void reportsArePrivateAndNeverBecomeFactsWithoutScopedVerification() throws Exception {
        var one=token("student1");var two=token("student2");var admin=token("admin1");var outside=token("admin2");var r=report(one,"report-private");
        error(request("GET","/feedback/"+r.id(),null,two,null),404,"RESOURCE_NOT_FOUND");error(request("GET","/feedback/"+r.id(),null,outside,null),403,"SCOPE_FORBIDDEN");
        error(request("POST","/admin/feedback/"+r.id()+"/actions",Map.of("version",1,"action","VERIFY","reason","模拟创始人权限"),one,"student-claims-admin"),403,"SCOPE_FORBIDDEN");
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM space_fact",Integer.class));assertTrue(request("GET","/spaces/4990",null,two,null).data().at("/profile/features/outlet").asBoolean());
        var publicView=request("GET","/spaces/4990/facts",null,two,null);assertEquals(0,publicView.data().size());assertFalse(publicView.body().toString().contains("TEST_PRIVATE"));
        verify(admin,r.id());assertFalse(request("GET","/spaces/4990",null,two,null).data().at("/profile/features/outlet").asBoolean());
        var verified=request("GET","/spaces/4990/facts",null,two,null);assertEquals(2,verified.data().size());assertFalse(verified.body().toString().contains("TEST_PRIVATE"));assertFalse(verified.body().toString().contains("user_id"));assertFalse(verified.body().toString().contains("report_id"));
        facts.put("verifiedFacts",jdbc.queryForObject("SELECT COUNT(*) FROM space_fact",Integer.class));facts.put("reports",jdbc.queryForObject("SELECT COUNT(*) FROM feedback_report",Integer.class));
    }
    @Test void duplicateRepairCreationReturnsOriginalReceiptAfterReportVersionsAdvance() throws Exception {
        var student=token("student1");var admin=token("admin1");var r=verify(admin,report(student,"report-duplicate").id());var body=repairBody(List.of(r));var created=repair(admin,List.of(r),"create-repair-duplicate");
        var again=request("POST","/admin/repairs",body,admin,"create-repair-duplicate");assertEquals(200,again.status(),again.body().toString());assertEquals(created.data(),again.data());
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM repair_ticket",Integer.class));assertEquals("IN_REPAIR",request("GET","/feedback/"+r.id(),null,student,null).data().path("status").asString());facts.put("repairs",jdbc.queryForObject("SELECT COUNT(*) FROM repair_ticket",Integer.class));
    }
    @Test void workDoneIsNotResolvedAndFailedVerificationCanContinue() throws Exception {
        var student=token("student1");var admin=token("admin1");var r=verify(admin,report(student,"report-workflow").id());var repair=repair(admin,List.of(r),"create-repair-workflow");
        assertEquals(200,repairAction(admin,repair.id(),1,"ASSIGN",null,null,"assign-repair").status());assertEquals(200,repairAction(admin,repair.id(),2,"WORK_DONE",null,null,"work-done-repair").status());
        assertEquals("IN_REPAIR",request("GET","/feedback/"+r.id(),null,student,null).data().path("status").asString());assertFalse(request("GET","/spaces/4990",null,student,null).data().at("/profile/features/outlet").asBoolean());
        assertEquals(200,repairAction(admin,repair.id(),3,"VERIFY",false,null,"failed-verify-repair").status());assertEquals("OPEN",request("GET","/admin/repairs/"+repair.id(),null,admin,null).data().path("status").asString());
        assertEquals(200,repairAction(admin,repair.id(),4,"ASSIGN",null,null,"assign-again-repair").status());assertEquals(200,repairAction(admin,repair.id(),5,"WORK_DONE",null,null,"work-done-again-repair").status());
        assertEquals(200,repairAction(admin,repair.id(),6,"VERIFY",true,List.of(Map.of("key","outlet","value",true),Map.of("key","outletCondition","value","WORKING")),"verified-repair").status());
        var resolved=request("GET","/feedback/"+r.id(),null,student,null);assertEquals("RESOLVED",resolved.data().path("status").asString());assertTrue(resolved.data().path("history").toString().contains("REPAIR_DONE"));assertTrue(request("GET","/spaces/4990",null,student,null).data().at("/profile/features/outlet").asBoolean());
        assertEquals(200,request("POST","/feedback/"+r.id()+"/supplements",Map.of("version",resolved.data().path("version").asLong(),"action","REOPEN","message","复验之后又出现问题，请重新核实"),student,"reopen-report").status());assertEquals("REPORTED",request("GET","/feedback/"+r.id(),null,student,null).data().path("status").asString());
        facts.put("failedVerificationHistory",jdbc.queryForObject("SELECT COUNT(*) FROM repair_history WHERE action='VERIFICATION_FAILED'",Integer.class));facts.put("closedRepairs",jdbc.queryForObject("SELECT COUNT(*) FROM repair_ticket WHERE status='VERIFIED_CLOSED'",Integer.class));
    }
    @Test void secondReporterNotificationFaultRollsBackFactsAndAllResolutions() throws Exception {
        var one=token("student1");var two=token("student2");var admin=token("admin1");var r1=verify(admin,report(one,"report-fault-one").id());var r2=verify(admin,report(two,"report-fault-two").id());var repair=repair(admin,List.of(r1,r2),"create-repair-fault");
        assertEquals(200,repairAction(admin,repair.id(),1,"ASSIGN",null,null,"assign-repair-fault").status());assertEquals(200,repairAction(admin,repair.id(),2,"WORK_DONE",null,null,"done-repair-fault").status());
        int initialFacts=jdbc.queryForObject("SELECT COUNT(*) FROM space_fact",Integer.class);var counter=new java.util.concurrent.atomic.AtomicInteger();
        org.mockito.Mockito.doAnswer(invocation->{if(invocation.getArgument(2).equals("空间反馈处理进展") && counter.incrementAndGet()==2)throw new org.springframework.dao.DataAccessResourceFailureException("TEST_SECOND_NOTIFICATION_FAULT");return invocation.callRealMethod();}).when(business).notify(org.mockito.ArgumentMatchers.anyLong(),org.mockito.ArgumentMatchers.anyString(),org.mockito.ArgumentMatchers.anyString(),org.mockito.ArgumentMatchers.anyString(),org.mockito.ArgumentMatchers.anyString(),org.mockito.ArgumentMatchers.anyLong());
        try {error(repairAction(admin,repair.id(),3,"VERIFY",true,List.of(Map.of("key","outlet","value",true),Map.of("key","outletCondition","value","WORKING")),"verify-fault-repair"),503,"DATABASE_UNAVAILABLE");}finally {org.mockito.Mockito.reset(business);}
        assertEquals("WORK_DONE",jdbc.queryForObject("SELECT status FROM repair_ticket WHERE id=?",String.class,repair.id()));assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM feedback_report WHERE status='RESOLVED'",Integer.class));assertEquals(initialFacts,jdbc.queryForObject("SELECT COUNT(*) FROM space_fact",Integer.class));
        assertEquals(200,repairAction(admin,repair.id(),3,"VERIFY",true,List.of(Map.of("key","outlet","value",true),Map.of("key","outletCondition","value","WORKING")),"verify-fault-repair").status());assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM feedback_report WHERE status='RESOLVED'",Integer.class));
        facts.put("resolutionsAfterRecovery",jdbc.queryForObject("SELECT COUNT(*) FROM feedback_report WHERE status='RESOLVED'",Integer.class));facts.put("formalRepairClosures",jdbc.queryForObject("SELECT COUNT(*) FROM repair_history WHERE action='VERIFIED_CLOSED'",Integer.class));
    }
    @Test void verificationCannotCloseTheSameFacilityWhileItsKnownBrokenFactRemains() throws Exception {
        var student=token("student1");var admin=token("admin1");var r=verify(admin,report(student,"report-known-broken").id());var ticket=repair(admin,List.of(r),"repair-known-broken");
        assertEquals(200,repairAction(admin,ticket.id(),1,"ASSIGN",null,null,"assign-known-broken").status());assertEquals(200,repairAction(admin,ticket.id(),2,"WORK_DONE",null,null,"done-known-broken").status());
        assertEquals("BROKEN",request("GET","/spaces/4990",null,student,null).data().at("/profile/conditions/outletCondition").asString());
        var result=repairAction(admin,ticket.id(),3,"VERIFY",true,List.of(Map.of("key","outlet","value",true)),"verify-known-broken");
        var current=request("GET","/spaces/4990",null,student,null);var reportNow=request("GET","/feedback/"+r.id(),null,student,null);
        facts.put("verificationStatus",result.status());facts.put("publicCondition",current.data().at("/profile/conditions/outletCondition").asString());facts.put("reportStatus",reportNow.data().path("status").asString());
        assertFalse(result.status()==200 && reportNow.data().path("status").asString().equals("RESOLVED") && Set.of("BROKEN","REPAIRING").contains(current.data().at("/profile/conditions/outletCondition").asString()),"successful repair closure contradicted its own public facility fact");
        error(result,409,"REPAIR_FACT_REQUIRED");assertEquals("WORK_DONE",jdbc.queryForObject("SELECT status FROM repair_ticket WHERE id=?",String.class,ticket.id()));assertEquals("IN_REPAIR",reportNow.data().path("status").asString());
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM idempotency_receipt WHERE request_key='verify-known-broken'",Integer.class));
        for(String value:List.of("UNKNOWN","BROKEN","REPAIRING"))error(repairAction(admin,ticket.id(),3,"VERIFY",true,List.of(Map.of("key","outlet","value",true),Map.of("key","outletCondition","value",value)),"verify-known-"+value),409,"REPAIR_FACT_REQUIRED");
        error(repairAction(admin,ticket.id(),3,"VERIFY",true,List.of(Map.of("key","outletCondition","value","WORKING")),"verify-absent-outlet"),409,"REPAIR_FACT_REQUIRED");
        assertEquals(200,repairAction(admin,ticket.id(),3,"VERIFY",true,List.of(Map.of("key","outlet","value",true),Map.of("key","outletCondition","value","WORKING")),"verify-known-working").status());
        assertEquals("WORKING",request("GET","/spaces/4990",null,student,null).data().at("/profile/conditions/outletCondition").asString());assertEquals("RESOLVED",request("GET","/feedback/"+r.id(),null,student,null).data().path("status").asString());
    }
    byte[] image(int rgb) throws Exception {var image=new BufferedImage(2,2,BufferedImage.TYPE_INT_RGB);image.setRGB(0,0,rgb);var stream=new ByteArrayOutputStream();assertTrue(ImageIO.write(image,"png",stream));return stream.toByteArray();}
    Reply upload(String token,long report,byte[] content,String key) throws Exception {
        String boundary="QixuEvidenceBoundary";var stream=new ByteArrayOutputStream();stream.write(("--"+boundary+"\r\nContent-Disposition: form-data; name=\"file\"; filename=\"untrusted.svg\"\r\nContent-Type: image/png\r\n\r\n").getBytes(StandardCharsets.US_ASCII));stream.write(content);stream.write(("\r\n--"+boundary+"--\r\n").getBytes(StandardCharsets.US_ASCII));
        var result=client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api/v1/feedback/"+report+"/attachments")).timeout(Duration.ofSeconds(15)).header("Authorization","Bearer "+token).header("Idempotency-Key",key).header("Content-Type","multipart/form-data; boundary="+boundary).POST(HttpRequest.BodyPublishers.ofByteArray(stream.toByteArray())).build(),HttpResponse.BodyHandlers.ofString());requests.add(Map.of("method","MULTIPART","path","/feedback/"+report+"/attachments","status",result.statusCode()));return new Reply(result.statusCode(),json.readTree(result.body()));
    }
    int download(String token,long report,long attachment) throws Exception {
        var result=client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api/v1/feedback/"+report+"/attachments/"+attachment)).header("Authorization","Bearer "+token).GET().build(),HttpResponse.BodyHandlers.ofByteArray());requests.add(Map.of("method","BINARY_GET","path","/feedback/"+report+"/attachments/"+attachment,"status",result.statusCode()));
        if(result.statusCode()==200) {assertEquals("image/png",result.headers().firstValue("Content-Type").orElseThrow());assertEquals("no-store",result.headers().firstValue("Cache-Control").orElseThrow());assertEquals("nosniff",result.headers().firstValue("X-Content-Type-Options").orElseThrow());}return result.statusCode();
    }
    @Test void attachmentsAreBoundedPrivateAndContentVerifiedWithIdempotentReplay() throws Exception {
        var one=token("student1");var two=token("student2");var admin=token("admin1");var outside=token("admin2");var report=report(one,"report-image");var bytes=image(0xff1122);
        var uploaded=upload(one,report.id(),bytes,"attach-original");assertEquals(200,uploaded.status(),uploaded.body().toString());assertEquals(uploaded.data(),upload(one,report.id(),bytes,"attach-original").data());
        assertEquals(200,download(one,report.id(),uploaded.id()));assertEquals(200,download(admin,report.id(),uploaded.id()));int peerStatus=download(two,report.id(),uploaded.id());assertEquals(404,peerStatus);assertEquals(403,download(outside,report.id(),uploaded.id()));
        error(upload(one,report.id(),"<svg onload='alert(1)'/>".getBytes(StandardCharsets.UTF_8),"attach-svg-control"),422,"INVALID_INPUT");error(upload(one,report.id(),Arrays.copyOf(bytes,12),"attach-corrupt-control"),422,"INVALID_INPUT");
        byte[] huge=bytes.clone();java.nio.ByteBuffer.wrap(huge).putInt(16,100_000).putInt(20,100_000);var crc=new java.util.zip.CRC32();crc.update(huge,12,17);java.nio.ByteBuffer.wrap(huge).putInt(29,(int)crc.getValue());error(upload(one,report.id(),huge,"attach-pixel-bomb-control"),422,"INVALID_INPUT");
        error(upload(one,report.id(),new byte[1_048_577],"attach-large-control"),413,"PAYLOAD_TOO_LARGE");
        assertEquals(200,upload(one,report.id(),image(0x445566),"attach-second").status());assertEquals(200,upload(one,report.id(),image(0x778899),"attach-third").status());error(upload(one,report.id(),image(0x998877),"attach-fourth"),409,"ATTACHMENT_LIMIT");
        assertEquals(3,jdbc.queryForObject("SELECT COUNT(*) FROM feedback_attachment",Integer.class));facts.put("attachments",jdbc.queryForObject("SELECT COUNT(*) FROM feedback_attachment",Integer.class));facts.put("privatePeerStatus",peerStatus);
    }
    @Test void competingReportDecisionsRejectStaleVersionAndKeepOriginal() throws Exception {
        var student=token("student1");var admin=token("admin1");var report=report(student,"report-race");var original=report.data().path("description").asString();
        var executor=Executors.newFixedThreadPool(2);var go=new CountDownLatch(1);
        try {
            var a=executor.submit(()->{go.await();return request("POST","/admin/feedback/"+report.id()+"/actions",Map.of("version",1,"action","ACKNOWLEDGE","reason","第一位处理者"),admin,"competing-report-a");});
            var c=executor.submit(()->{go.await();return request("POST","/admin/feedback/"+report.id()+"/actions",Map.of("version",1,"action","NOT_REPRODUCED","reason","第二位处理者"),admin,"competing-report-b");});go.countDown();var outcomes=List.of(a.get(20,TimeUnit.SECONDS),c.get(20,TimeUnit.SECONDS));assertEquals(1,outcomes.stream().filter(r->r.status()==200).count());assertEquals(1,outcomes.stream().filter(r->r.status()==409).count());
        } finally {executor.shutdownNow();}
        assertEquals(original,jdbc.queryForObject("SELECT description FROM feedback_report WHERE id=?",String.class,report.id()));assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM feedback_history WHERE report_id=?",Integer.class,report.id()));facts.put("history",jdbc.queryForObject("SELECT COUNT(*) FROM feedback_history",Integer.class));
    }
    @Test void queuedVerificationLosesAuthorityWhenScopeIsRevokedBeforeFloorRelease() throws Exception {
        var student=token("student1");var admin=token("admin1");var report=report(student,"report-revoke-scope");var barrier=new CountDownLatch(1);var executor=Executors.newSingleThreadExecutor();
        org.mockito.Mockito.doAnswer(invocation->{barrier.countDown();return invocation.callRealMethod();}).when(business).floors(org.mockito.ArgumentMatchers.anyCollection());
        try {
            final Future<Reply>[] pending=new Future[1];new org.springframework.transaction.support.TransactionTemplate(manager).execute(status->{
                jdbc.queryForList("SELECT id FROM floor WHERE id=101 FOR UPDATE");pending[0]=executor.submit(()->request("POST","/admin/feedback/"+report.id()+"/actions",Map.of("version",1,"action","VERIFY","reason","等待期间撤权","facts",List.of(Map.of("key","outlet","value",false))),admin,"verify-scope-revoked"));
                try {assertTrue(barrier.await(5,TimeUnit.SECONDS));}catch(InterruptedException e){throw new RuntimeException(e);}jdbc.update("DELETE FROM admin_scope WHERE user_id=4 AND floor_id=101");return null;
            });
            error(pending[0].get(15,TimeUnit.SECONDS),403,"SCOPE_FORBIDDEN");assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM space_fact",Integer.class));assertEquals("REPORTED",jdbc.queryForObject("SELECT status FROM feedback_report WHERE id=?",String.class,report.id()));facts.put("factsAfterRevoke",jdbc.queryForObject("SELECT COUNT(*) FROM space_fact",Integer.class));
        } finally {org.mockito.Mockito.reset(business);executor.shutdownNow();jdbc.update("INSERT IGNORE INTO admin_scope(user_id,floor_id) VALUES(4,101)");}
    }
    @Test void malformedSecondFactCannotPartiallyPublishTheFirstFactOrDecision() throws Exception {
        var student=token("student1");var admin=token("admin1");var report=report(student,"report-invalid-fact");
        error(request("POST","/admin/feedback/"+report.id()+"/actions",Map.of("version",1,"action","VERIFY","reason","验证混合事实回滚","facts",List.of(Map.of("key","outlet","value",false),Map.of("key","quiet","value","true"))),admin,"verify-invalid-fact"),422,"INVALID_INPUT");
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM space_fact",Integer.class));assertEquals(1L,jdbc.queryForObject("SELECT version FROM space WHERE id=4990",Long.class));assertEquals("REPORTED",jdbc.queryForObject("SELECT status FROM feedback_report WHERE id=?",String.class,report.id()));assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM idempotency_receipt WHERE request_key='verify-invalid-fact'",Integer.class));facts.put("partialFacts",jdbc.queryForObject("SELECT COUNT(*) FROM space_fact",Integer.class));
    }
}
