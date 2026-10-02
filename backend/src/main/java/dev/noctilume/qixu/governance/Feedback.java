package dev.noctilume.qixu.governance;

import dev.noctilume.qixu.common.*;
import dev.noctilume.qixu.identity.*;
import dev.noctilume.qixu.spaces.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import tools.jackson.databind.json.JsonMapper;

/** A private report, a verified public fact and a repair are three different facts. */
@Service
public class Feedback {
    private final Business b;private final SpaceRights rights;private final SpaceFactChanges changes;private final BlockNotices notices;private final SpaceBlocks blocks;private final JsonMapper json;
    public record Create(long spaceId,String category,String description) {}
    public record Fact(String key,Object value) {}
    public record Action(long version,String action,String reason,List<Fact> facts) {}
    public record Supplement(long version,String action,String message) {}
    public record ReportRef(long id,long version) {}
    public record RepairCreate(List<ReportRef> reports,String description) {}
    public record RepairAction(long version,String action,String reason,String assignee,Boolean verified,List<Fact> facts,List<ReportRef> reports) {}
    public record LimitLink(long version,long blockId,long blockVersion,String reason) {}
    public record Attachment(byte[] bytes,String type,String sha256,long id) {}
    public Feedback(Business b,SpaceRights rights,SpaceFactChanges changes,BlockNotices notices,SpaceBlocks blocks,JsonMapper json) {this.b=b;this.rights=rights;this.changes=changes;this.notices=notices;this.blocks=blocks;this.json=json;}
    private Map<String,Object> row(String table,long id) {
        if(!Set.of("feedback_report","repair_ticket","feedback_attachment").contains(table))throw new IllegalArgumentException();
        var rows=b.jdbc.queryForList("SELECT * FROM "+table+" WHERE id=?",id);if(rows.isEmpty())throw DomainException.missing();return rows.get(0);
    }
    private void readable(Actor actor,Map<String,Object> report) {
        if(Business.number(report,"user_id")==actor.id())return;
        if(!actor.role().equals("ADMIN"))throw DomainException.missing();b.admin(actor,Business.number(report,"floor_id"));
    }
    private List<Long> reportUsers(Collection<Map<String,Object>> reports,long actor) {
        var ids=new ArrayList<Long>(List.of(actor));for(var r:reports)ids.add(Business.number(r,"user_id"));return ids;
    }
    private void history(long report,long actor,String action,String message) {
        b.jdbc.update("INSERT INTO feedback_history(report_id,actor_id,action,message,created_at) VALUES(?,?,?,?,?)",report,actor,action,message,b.now());
    }
    private void repairHistory(long repair,long actor,String action,String message) {
        b.jdbc.update("INSERT INTO repair_history(repair_id,actor_id,action,message,created_at) VALUES(?,?,?,?,?)",repair,actor,action,message,b.now());
    }
    private void reportState(long id,String status,String reason) {
        b.jdbc.update("UPDATE feedback_report SET status=?,decision_note=?,updated_at=?,version=version+1 WHERE id=?",status,reason,b.now(),id);
    }
    private void notice(Map<String,Object> report,String suffix,String message) {
        b.notify(Business.number(report,"user_id"),"feedback:"+report.get("id")+":"+suffix,"空间反馈处理进展",message,"FEEDBACK",Business.number(report,"id"));
    }
    public Map<String,Object> detail(Actor actor,long id) {
        var report=row("feedback_report",id);readable(actor,report);var value=b.view(report);
        value.put("history",b.jdbc.queryForList("SELECT id,action,message,created_at FROM feedback_history WHERE report_id=? ORDER BY id",id).stream().map(b::view).toList());
        value.put("attachments",b.jdbc.queryForList("SELECT id,media_type,sha256,width,height,created_at FROM feedback_attachment WHERE report_id=? ORDER BY id",id).stream().map(b::view).toList());
        value.put("repairs",b.jdbc.queryForList("SELECT t.id,t.status,t.version,t.updated_at FROM repair_ticket t JOIN repair_report r ON t.id=r.repair_id WHERE r.report_id=? ORDER BY t.id DESC",id).stream().map(b::view).toList());
        return value;
    }
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public Map<String,Object> get(Actor actor,long id) {return detail(actor,id);}
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public Map<String,Object> list(Actor actor,boolean administrative,int page) {
        if(page<1 || page>10_000)throw DomainException.invalid("页码超出范围。");
        if(administrative && !actor.role().equals("ADMIN"))throw DomainException.forbidden();
        String where=administrative?" EXISTS(SELECT 1 FROM admin_scope s WHERE s.floor_id=r.floor_id AND s.user_id=?)":" r.user_id=?";
        long total=b.jdbc.queryForObject("SELECT COUNT(*) FROM feedback_report r WHERE"+where,Long.class,actor.id());
        var values=b.jdbc.queryForList("SELECT r.id,r.space_id,r.floor_id,r.category,r.status,r.version,r.created_at,r.updated_at FROM feedback_report r WHERE"+where+" ORDER BY r.id DESC LIMIT 50 OFFSET ?",actor.id(),(page-1)*50).stream().map(b::view).toList();
        return Map.of("items",values,"total",total,"page",page,"size",50);
    }
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public Map<String,Object> create(AuthService.Session session,String key,Create body,String requestId) {
        var initial=rights.space(body.spaceId());long floor=Business.number(initial,"floor_id");rights.lockFloors(List.of(floor));b.users(List.of(session.actor().id()));var actor=b.current(session);Business.student(actor);
        return b.once(actor,key,"feedback.create",body,()->{
            rights.space(body.spaceId());Business.text(body.description(),2000,true);
            if(!Set.of("OUTLET","LIGHT","DESK","ENVIRONMENT","INFORMATION","OTHER").contains(body.category()==null?"":body.category()))throw DomainException.invalid("反馈类别不正确。");
            long id=b.insert("INSERT INTO feedback_report(user_id,space_id,floor_id,category,description,created_at,updated_at) VALUES(?,?,?,?,?,?,?)",actor.id(),body.spaceId(),floor,body.category(),body.description(),b.now(),b.now());
            history(id,actor.id(),"REPORTED",body.description());b.audit(actor.id(),"FEEDBACK_REPORTED","FEEDBACK",id,requestId);notice(row("feedback_report",id),"reported","反馈已保存，等待核实；报告不会自动改变公开空间状态。");return detail(actor,id);
        });
    }
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public Map<String,Object> supplement(AuthService.Session session,long id,String key,Supplement body,String requestId) {
        var initial=row("feedback_report",id);rights.lockFloors(List.of(Business.number(initial,"floor_id")));b.users(List.of(session.actor().id(),Business.number(initial,"user_id")));var actor=b.current(session);
        if(Business.number(initial,"user_id")!=actor.id())throw DomainException.missing();
        return b.once(actor,key,"feedback.supplement:"+id,body,()->{
            var report=row("feedback_report",id);Business.version(Business.number(report,"version"),body.version());Business.text(body.message(),2000,true);
            switch(body.action()==null?"":body.action()) {
                case "SUPPLEMENT" -> {history(id,actor.id(),"SUPPLEMENT",body.message());b.jdbc.update("UPDATE feedback_report SET version=version+1,updated_at=? WHERE id=?",b.now(),id);}
                case "REOPEN" -> {
                    if(!Set.of("RESOLVED","NOT_REPRODUCED","REJECTED").contains(report.get("status")))throw Business.conflict("FEEDBACK_STATE_CONFLICT","该反馈仍在处理中，可追加说明。");
                    reportState(id,"REPORTED","提交者申请重新核实");history(id,actor.id(),"REOPENED",body.message());
                }
                default -> throw DomainException.invalid("反馈操作不正确。");
            }
            b.audit(actor.id(),"FEEDBACK_"+body.action(),"FEEDBACK",id,requestId);return detail(actor,id);
        });
    }
    private void facts(List<Fact> facts,long space,long floor,Long report,Long repair,long actor,String reason) {
        if(facts==null || facts.isEmpty())return;if(facts.size()>8)throw DomainException.invalid("一次核实最多8个事实项。");
        var keys=new HashSet<String>();var profile=new LinkedHashMap<String,Object>(json.readValue(rights.space(space).get("profile_json").toString(),Map.class));
        var features=new LinkedHashMap<String,Object>();if(profile.get("features") instanceof Map<?,?> values)values.forEach((k,v)->features.put(k.toString(),v));
        var conditions=new LinkedHashMap<String,Object>();if(profile.get("conditions") instanceof Map<?,?> values)values.forEach((k,v)->conditions.put(k.toString(),v));
        for(var fact:facts) {
            if(fact==null || fact.key()==null || !keys.add(fact.key()))throw DomainException.invalid("事实项不能为空或重复。");
            if(Set.of("window","outlet","quiet","accessible").contains(fact.key())) {
                if(!(fact.value() instanceof Boolean))throw DomainException.invalid("设施事实须为明确布尔值；未知不要填写。");features.put(fact.key(),fact.value());
            } else if(Set.of("outletCondition","lightCondition","deskCondition","environmentCondition").contains(fact.key())) {
                if(!(fact.value() instanceof String text) || !Set.of("UNKNOWN","WORKING","BROKEN","REPAIRING").contains(text))throw DomainException.invalid("设施状态事实不正确。");conditions.put(fact.key(),fact.value());
            } else throw DomainException.invalid("不支持这个公开事实项。");
            b.jdbc.update("INSERT INTO space_fact(space_id,floor_id,feature_key,value_json,report_id,repair_id,actor_id,reason,verified_at) VALUES(?,?,?,?,?,?,?,?,?)",space,floor,fact.key(),json.writeValueAsString(fact.value()),report,repair,actor,reason,b.now());
        }
        profile.put("features",features);profile.put("conditions",conditions);profile.put("factsReviewedAt",Business.iso(b.now()));
        b.jdbc.update("UPDATE space SET profile_json=?,version=version+1 WHERE id=?",json.writeValueAsString(profile),space);
    }
    private void repairedFact(Map<String,Object> ticket,List<Fact> facts) {
        String condition=switch(ticket.get("category").toString()) {case "OUTLET"->"outletCondition";case "LIGHT"->"lightCondition";case "DESK"->"deskCondition";case "ENVIRONMENT"->"environmentCondition";default->null;};
        if(condition==null)return;
        var profile=json.readValue(rights.space(Business.number(ticket,"space_id")).get("profile_json").toString(),Map.class);
        Object old=profile.get("conditions") instanceof Map<?,?> values?values.get(condition):null;
        Object proposed=null,outlet=profile.get("features") instanceof Map<?,?> values?values.get("outlet"):null;
        if(facts!=null)for(var fact:facts)if(fact!=null) {if(condition.equals(fact.key()))proposed=fact.value();if("outlet".equals(fact.key()))outlet=fact.value();}
        if(Set.of("BROKEN","REPAIRING").contains(Objects.toString(proposed,"")) || Set.of("BROKEN","REPAIRING").contains(Objects.toString(old,""))&&!"WORKING".equals(proposed) || condition.equals("outletCondition")&&Boolean.FALSE.equals(outlet))
            throw Business.conflict("REPAIR_FACT_REQUIRED","复验通过须明确修正这项设施的已知损坏事实，不能同时保留损坏或缺失状态。");
    }
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public Map<String,Object> decide(AuthService.Session session,long id,String key,Action body,String requestId) {
        var initial=row("feedback_report",id);long floor=Business.number(initial,"floor_id");var impact=changes.lock(Business.number(initial,"space_id"),List.of(),List.of(session.actor().id(),Business.number(initial,"user_id")),List.of(floor));var actor=b.current(session);b.admin(actor,floor);
        return b.once(actor,key,"feedback.decide:"+id,body,()->{
            var report=row("feedback_report",id);Business.version(Business.number(report,"version"),body.version());Business.text(body.reason(),500,true);
            if(!Set.of("REPORTED","ACKNOWLEDGED").contains(report.get("status")))throw Business.conflict("FEEDBACK_STATE_CONFLICT","该反馈已进入其他处理阶段，请查看当前记录。");
            String next=switch(body.action()==null?"":body.action()) {case "ACKNOWLEDGE"->"ACKNOWLEDGED";case "VERIFY"->"VERIFIED";case "NOT_REPRODUCED"->"NOT_REPRODUCED";case "REJECT"->"REJECTED";default->throw DomainException.invalid("核实操作不正确。");};
            if(!next.equals("VERIFIED") && body.facts()!=null && !body.facts().isEmpty())throw DomainException.invalid("未核实的报告不能发布空间事实。");
            if(next.equals("VERIFIED")) {facts(body.facts(),Business.number(report,"space_id"),floor,id,null,actor.id(),body.reason());if(body.facts()!=null&&!body.facts().isEmpty())changes.notify(Business.number(report,"space_id"),impact);}
            reportState(id,next,body.reason());history(id,actor.id(),next,body.reason());b.audit(actor.id(),"FEEDBACK_"+next,"FEEDBACK",id,requestId);notice(report,"decision:"+(body.version()+1),"处理状态："+next+"；"+body.reason());return detail(actor,id);
        });
    }
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public List<Map<String,Object>> publicFacts(long space) {
        rights.space(space);
        return b.jdbc.queryForList("SELECT f.feature_key,f.value_json,f.verified_at FROM space_fact f JOIN (SELECT feature_key,MAX(id) AS id FROM space_fact WHERE space_id=? GROUP BY feature_key) latest ON f.id=latest.id ORDER BY f.feature_key",space).stream().map(r->{var value=b.view(r);value.put("value",json.readValue(value.remove("value_json").toString(),Object.class));return value;}).toList();
    }
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public Map<String,Object> attach(AuthService.Session session,long id,String key,byte[] bytes,String requestId) {
        var info=RasterEvidence.inspect(bytes);String digest=Digests.sha256(bytes);
        var initial=row("feedback_report",id);rights.lockFloors(List.of(Business.number(initial,"floor_id")));b.users(List.of(session.actor().id(),Business.number(initial,"user_id")));var actor=b.current(session);
        if(Business.number(initial,"user_id")!=actor.id())throw DomainException.missing();
        var fingerprint=Map.of("report",id,"sha256",digest,"type",info.type(),"size",bytes.length);
        return b.once(actor,key,"feedback.attach:"+id,fingerprint,()->{
            if(b.jdbc.queryForObject("SELECT COUNT(*) FROM feedback_attachment WHERE report_id=?",Integer.class,id)>=3)throw Business.conflict("ATTACHMENT_LIMIT","每份反馈最多3张照片。");
            long attachment=b.insert("INSERT INTO feedback_attachment(report_id,media_type,sha256,content,width,height,created_at) VALUES(?,?,?,?,?,?,?)",id,info.type(),digest,bytes,info.width(),info.height(),b.now());
            b.audit(actor.id(),"FEEDBACK_ATTACHMENT","FEEDBACK",id,requestId);return Map.of("id",attachment,"reportId",id,"sha256",digest,"width",info.width(),"height",info.height());
        });
    }
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public Attachment attachment(Actor actor,long report,long id) {
        readable(actor,row("feedback_report",report));var rows=b.jdbc.queryForList("SELECT content,media_type,sha256 FROM feedback_attachment WHERE report_id=? AND id=?",report,id);if(rows.isEmpty())throw DomainException.missing();var value=rows.get(0);return new Attachment((byte[])value.get("content"),value.get("media_type").toString(),value.get("sha256").toString(),id);
    }
    private List<Map<String,Object>> refs(List<ReportRef> refs,boolean checkVersion) {
        if(refs==null || refs.isEmpty() || refs.size()>50)throw DomainException.invalid("请选择1至50份已核实反馈。");var seen=new HashSet<Long>();var result=new ArrayList<Map<String,Object>>();
        for(var ref:refs) {if(ref==null || !seen.add(ref.id()))throw DomainException.invalid("反馈不能重复。");var report=row("feedback_report",ref.id());if(checkVersion)Business.version(Business.number(report,"version"),ref.version());result.add(report);}return result;
    }
    private void link(long repair,List<Map<String,Object>> reports,long actor) {
        var ticket=row("repair_ticket",repair);
        for(var report:reports) {
            if(Business.number(report,"space_id")!=Business.number(ticket,"space_id") || !report.get("category").equals(ticket.get("category")) || !report.get("status").equals("VERIFIED"))throw Business.conflict("REPAIR_LINK_INVALID","只能关联同一空间、同类且已核实的未处理反馈。");
            long id=Business.number(report,"id");b.jdbc.update("INSERT INTO repair_report(repair_id,report_id,space_id,floor_id) VALUES(?,?,?,?)",repair,id,ticket.get("space_id"),ticket.get("floor_id"));
            reportState(id,"IN_REPAIR","已关联维修事项 "+repair);history(id,actor,"REPAIR_LINKED","维修事项 "+repair);notice(report,"repair:"+repair,"已关联维修事项，安排维修不代表问题已修复。");
        }
    }
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public Map<String,Object> createRepair(AuthService.Session session,String key,RepairCreate body,String requestId) {
        if(!session.actor().role().equals("ADMIN"))throw DomainException.forbidden();
        var initial=refs(body.reports(),false);var first=initial.get(0);long floor=Business.number(first,"floor_id");rights.lockFloors(initial.stream().map(r->Business.number(r,"floor_id")).toList());b.users(reportUsers(initial,session.actor().id()));var actor=b.current(session);for(var report:initial)b.admin(actor,Business.number(report,"floor_id"));
        return b.once(actor,key,"repair.create",body,()->{
            var reports=refs(body.reports(),true);Business.text(body.description(),2000,true);
            if(b.jdbc.queryForObject("SELECT COUNT(*) FROM repair_ticket WHERE space_id=? AND category=? AND status<>'VERIFIED_CLOSED'",Integer.class,first.get("space_id"),first.get("category"))>0)throw Business.conflict("REPAIR_ALREADY_OPEN","已有同类维修事项，请把反馈关联到现有事项。");
            long id=b.insert("INSERT INTO repair_ticket(space_id,floor_id,category,description,created_at,updated_at) VALUES(?,?,?,?,?,?)",first.get("space_id"),floor,first.get("category"),body.description(),b.now(),b.now());
            link(id,reports,actor.id());repairHistory(id,actor.id(),"OPENED",body.description());b.audit(actor.id(),"REPAIR_OPENED","REPAIR",id,requestId);return repairDetail(actor,id);
        });
    }
    public Map<String,Object> repairDetail(Actor actor,long id) {
        var ticket=row("repair_ticket",id);b.admin(actor,Business.number(ticket,"floor_id"));var value=b.view(ticket);value.remove("active_space");
        value.put("reports",b.jdbc.queryForList("SELECT report_id FROM repair_report WHERE repair_id=? ORDER BY report_id",id));
        value.put("limits",b.jdbc.queryForList("SELECT l.block_id,l.reason,l.created_at,k.status,k.version,k.kind FROM repair_limit l JOIN space_block k ON l.block_id=k.id WHERE l.repair_id=?",id).stream().map(b::view).toList());
        value.put("history",b.jdbc.queryForList("SELECT id,action,message,created_at FROM repair_history WHERE repair_id=? ORDER BY id",id).stream().map(b::view).toList());return value;
    }
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public Map<String,Object> getRepair(Actor actor,long id) {return repairDetail(actor,id);}
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public Map<String,Object> repairs(Actor actor,int page) {
        if(!actor.role().equals("ADMIN"))throw DomainException.forbidden();if(page<1 || page>10_000)throw DomainException.invalid("页码超出范围。");
        long count=b.jdbc.queryForObject("SELECT COUNT(*) FROM repair_ticket t JOIN admin_scope s ON t.floor_id=s.floor_id WHERE s.user_id=?",Long.class,actor.id());
        return Map.of("items",b.jdbc.queryForList("SELECT t.id,t.space_id,t.category,t.status,t.version,t.assignee,t.updated_at FROM repair_ticket t JOIN admin_scope s ON t.floor_id=s.floor_id WHERE s.user_id=? ORDER BY t.id DESC LIMIT 50 OFFSET ?",actor.id(),(page-1)*50).stream().map(b::view).toList(),"total",count,"page",page,"size",50);
    }
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public Map<String,Object> repairAction(AuthService.Session session,long id,String key,RepairAction body,String requestId) {
        if(!session.actor().role().equals("ADMIN"))throw DomainException.forbidden();
        var initial=row("repair_ticket",id);long floor=Business.number(initial,"floor_id");var added=body.reports()==null?List.<Map<String,Object>>of():refs(body.reports(),false);var floors=new ArrayList<Long>(List.of(floor));added.forEach(r->floors.add(Business.number(r,"floor_id")));var reports=b.jdbc.queryForList("SELECT f.* FROM feedback_report f JOIN repair_report r ON f.id=r.report_id WHERE r.repair_id=? ORDER BY f.id",id);
        var users=reportUsers(reports,session.actor().id());users.addAll(reportUsers(added,session.actor().id()));var source=repairLimit(id);var batches=new TreeSet<Long>();if(source!=null) {batches.addAll(notices.batches(Business.number(source,"id")));users.addAll(notices.users(Business.number(source,"id")));}
        var impact=changes.lock(Business.number(initial,"space_id"),batches,users,floors);var currentSource=repairLimit(id);if(source==null&&currentSource!=null||source!=null&&(currentSource==null||Business.number(source,"id")!=Business.number(currentSource,"id")))throw Business.conflict("IMPACT_COORDINATES_CHANGED","维修来源在等待期间变化，请重试。");var actor=b.current(session);b.admin(actor,floor);for(var report:added)b.admin(actor,Business.number(report,"floor_id"));
        return b.once(actor,key,"repair.action:"+id,body,()->{
            var ticket=row("repair_ticket",id);Business.version(Business.number(ticket,"version"),body.version());Business.text(body.reason(),500,true);
            if(ticket.get("status").equals("VERIFIED_CLOSED"))throw Business.conflict("REPAIR_STATE_CONFLICT","维修事项已复验关闭；新问题请重新报告。");
            String next,action;
            switch(body.action()==null?"":body.action()) {
                case "ASSIGN" -> {if(!Set.of("OPEN","ASSIGNED").contains(ticket.get("status")))throw Business.conflict("REPAIR_STATE_CONFLICT","请先复验或重新安排维修。");Business.text(body.assignee(),100,true);b.jdbc.update("UPDATE repair_ticket SET assignee=? WHERE id=?",body.assignee(),id);next="ASSIGNED";action="ASSIGNED";}
                case "WORK_DONE" -> {if(!ticket.get("status").equals("ASSIGNED"))throw Business.conflict("REPAIR_STATE_CONFLICT","只有已安排的维修可以记录工作完成。");next="WORK_DONE";action="WORK_DONE";}
                case "LINK_REPORTS" -> {link(id,refs(body.reports(),true),actor.id());next=ticket.get("status").equals("WORK_DONE")?"OPEN":ticket.get("status").toString();action="REPORT_LINKED";}
                case "VERIFY" -> {
                    if(!ticket.get("status").equals("WORK_DONE") || body.verified()==null)throw Business.conflict("REPAIR_STATE_CONFLICT","请对已完成工作的事项给出明确复验结果。");
                    if(body.verified()) {repairedFact(ticket,body.facts());next="VERIFIED_CLOSED";action="VERIFIED_CLOSED";facts(body.facts(),Business.number(ticket,"space_id"),floor,null,id,actor.id(),body.reason());}
                    else {if(body.facts()!=null && !body.facts().isEmpty())throw DomainException.invalid("复验失败不能写入已修复事实。");next="OPEN";action="VERIFICATION_FAILED";}
                    b.jdbc.update("UPDATE repair_ticket SET verification_note=? WHERE id=?",body.reason(),id);
                }
                default -> throw DomainException.invalid("维修操作不正确。");
            }
            if(!body.action().equals("VERIFY") && body.facts()!=null && !body.facts().isEmpty())throw DomainException.invalid("只有复验通过才能发布修复事实。");
            b.jdbc.update("UPDATE repair_ticket SET status=?,updated_at=?,version=version+1 WHERE id=?",next,b.now(),id);repairHistory(id,actor.id(),action,body.reason());
            if(next.equals("VERIFIED_CLOSED")) {
                if(body.facts()!=null&&!body.facts().isEmpty())changes.notify(Business.number(ticket,"space_id"),impact);
                var limit=repairLimit(id);if(limit!=null&&limit.get("status").equals("ACTIVE"))blocks.revoke(session,Business.number(limit,"id"),"repair-"+id+"-"+Digests.sha256(key).substring(0,24),new SpaceBlocks.Revoke(Business.number(limit,"version"),"维修事项 "+id+" 已复验通过，详见原维修记录。"),requestId);
            }
            for(var report:b.jdbc.queryForList("SELECT f.* FROM feedback_report f JOIN repair_report r ON f.id=r.report_id WHERE r.repair_id=? ORDER BY f.id",id)) {
                if(next.equals("VERIFIED_CLOSED")) {reportState(Business.number(report,"id"),"RESOLVED",body.reason());history(Business.number(report,"id"),actor.id(),"RESOLVED",body.reason());}
                else if(next.equals("WORK_DONE"))history(Business.number(report,"id"),actor.id(),"REPAIR_DONE","维修工作完成，等待复验；尚未宣称问题已解决。");
                notice(report,"repair:"+id+":"+(body.version()+1),"维修进展："+next+"；"+body.reason());
            }
            b.audit(actor.id(),"REPAIR_"+action,"REPAIR",id,requestId);return repairDetail(actor,id);
        });
    }
    private Map<String,Object> repairLimit(long repair) {var rows=b.jdbc.queryForList("SELECT k.* FROM repair_limit l JOIN space_block k ON l.block_id=k.id WHERE l.repair_id=?",repair);return rows.isEmpty()?null:rows.get(0);}
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public Map<String,Object> linkLimit(AuthService.Session session,long id,String key,LimitLink body,String requestId) {
        if(!b.current(session).role().equals("ADMIN"))throw DomainException.forbidden();
        var ticket=row("repair_ticket",id);var sourceRows=b.jdbc.queryForList("SELECT * FROM space_block WHERE id=?",body.blockId());if(sourceRows.isEmpty())throw DomainException.missing();var source=sourceRows.get(0);var users=new TreeSet<Long>(List.of(session.actor().id()));users.addAll(notices.users(body.blockId()));for(var r:b.jdbc.queryForList("SELECT f.user_id FROM feedback_report f JOIN repair_report r ON f.id=r.report_id WHERE r.repair_id=?",id))users.add(Business.number(r,"user_id"));
        changes.lock(Business.number(ticket,"space_id"),notices.batches(body.blockId()),users,List.of(Business.number(ticket,"floor_id"),Business.number(source,"floor_id")));var actor=b.current(session);b.admin(actor,Business.number(ticket,"floor_id"));blocks.get(actor,body.blockId());
        return b.once(actor,key,"repair.limit:"+id,body,()->{
            var current=row("repair_ticket",id);Business.version(Business.number(current,"version"),body.version());var block=b.jdbc.queryForMap("SELECT * FROM space_block WHERE id=?",body.blockId());Business.version(Business.number(block,"version"),body.blockVersion());Business.text(body.reason(),450,true);
            if(current.get("status").equals("VERIFIED_CLOSED")||!block.get("status").equals("ACTIVE")||!b.now().isBefore(Business.date(block,"ends_at"))||!Set.of("MAINTENANCE","SAFETY").contains(block.get("kind"))||Business.number(current,"space_id")!=Business.number(block,"space_id")||Business.number(current,"floor_id")!=Business.number(block,"floor_id"))throw Business.conflict("REPAIR_LIMIT_INVALID","只能关联未关闭维修与同一精确空间的有效安全维护来源。");
            if(b.jdbc.queryForObject("SELECT COUNT(*) FROM repair_limit WHERE repair_id=? OR block_id=?",Integer.class,id,body.blockId())>0)throw Business.conflict("REPAIR_LIMIT_ALREADY_LINKED","维修或来源已经有关联，不能暗换来源。");
            b.jdbc.update("INSERT INTO repair_limit(repair_id,block_id,space_id,floor_id,actor_id,reason,created_at) VALUES(?,?,?,?,?,?,?)",id,body.blockId(),current.get("space_id"),current.get("floor_id"),actor.id(),body.reason(),b.now());b.jdbc.update("UPDATE repair_ticket SET status=IF(status='WORK_DONE','OPEN',status),version=version+1,updated_at=? WHERE id=?",b.now(),id);b.audit(actor.id(),"REPAIR_LIMIT_LINKED","REPAIR",id,requestId);return repairDetail(actor,id);
        });
    }
}
