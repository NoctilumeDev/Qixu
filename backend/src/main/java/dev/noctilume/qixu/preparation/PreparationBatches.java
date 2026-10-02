package dev.noctilume.qixu.preparation;

import dev.noctilume.qixu.common.*;
import dev.noctilume.qixu.identity.*;
import dev.noctilume.qixu.spaces.SpaceRights;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import tools.jackson.databind.json.JsonMapper;

@Service
public class PreparationBatches {
    final Business b; final SpaceRights rights; final JsonMapper json; final FrozenJson frozenJson;
    public record Create(String title,String purpose,OffsetDateTime startsAt,OffsetDateTime endsAt,
        OffsetDateTime opensAt,OffsetDateTime closesAt,OffsetDateTime freezeDeadline,OffsetDateTime randomAt,
        OffsetDateTime resultDeadline,OffsetDateTime confirmationDeadline,OffsetDateTime promotionUntil,int promotionSeconds,List<Long> seatIds) {}
    public record Preference(long seatId,int rank) {}
    public record Submit(int version,List<Preference> preferences,boolean keepWaitlist) {}
    public record Action(long version,String action) {}
    public PreparationBatches(Business b,SpaceRights rights,JsonMapper json) {this.b=b;this.rights=rights;this.json=json;this.frozenJson=new FrozenJson(json);}
    Map<String,Object> batch(long id,boolean lock) {
        var rows=b.jdbc.queryForList("SELECT * FROM preparation_batch WHERE id=?"+(lock?" FOR UPDATE":""),id);
        if(rows.isEmpty())throw DomainException.missing();return rows.get(0);
    }
    List<Map<String,Object>> pool(long id) {return b.jdbc.queryForList("SELECT p.*,s.code,s.name,s.kind,s.use_mode,s.active,s.version AS current_space_version FROM preparation_pool p JOIN space s ON p.space_id=s.id WHERE p.batch_id=? ORDER BY p.space_id",id);}
    List<Long> floors(long id) {return pool(id).stream().map(p->Business.number(p,"floor_id")).distinct().sorted().toList();}
    void scope(Actor actor,long id) {for(long floor:floors(id))b.admin(actor,floor);}
    static void state(Map<String,Object> row,String expected) {if(!expected.equals(row.get("status")))throw Business.conflict("BATCH_STATE_CONFLICT","批次阶段已经变化，请刷新查看。");}
    void event(long batch,Long user,Long actor,String action,Object detail) {
        b.jdbc.update("INSERT INTO allocation_event(batch_id,user_id,actor_id,action,detail_json,created_at) VALUES(?,?,?,?,?,?)",batch,user,actor,action,json.writeValueAsString(detail),b.now());
    }
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public Map<String,Object> create(AuthService.Session session,String key,Create body,String requestId) {
        if(body.seatIds()==null || body.seatIds().isEmpty() || body.seatIds().size()>400 || body.seatIds().stream().anyMatch(Objects::isNull) || new HashSet<>(body.seatIds()).size()!=body.seatIds().size())throw DomainException.invalid("资源池须包含1–400个不同席位。");
        var selected=body.seatIds().stream().map(rights::space).toList();
        var floorIds=selected.stream().map(r->Business.number(r,"floor_id")).distinct().sorted().toList();
        b.floors(floorIds);b.users(List.of(session.actor().id()));var actor=b.current(session);floorIds.forEach(f->b.admin(actor,f));
        return b.once(actor,key,"preparation.create",body,()->{
            Business.text(body.title(),100,true);
            if(!Set.of("POSTGRADUATE","CIVIL_SERVICE","OTHER").contains(body.purpose()==null?"":body.purpose()))throw DomainException.invalid("请选择明确的备考批次用途。");
            var start=Business.time(body.startsAt());var end=Business.time(body.endsAt());var open=Business.time(body.opensAt());var close=Business.time(body.closesAt());
            var freeze=Business.time(body.freezeDeadline());var random=RandomnessSource.roundTime(RandomnessSource.roundAtOrAfter(Business.time(body.randomAt())));
            var result=Business.time(body.resultDeadline());var confirm=Business.time(body.confirmationDeadline());var promote=Business.time(body.promotionUntil());
            var now=b.now();
            if(!open.isBefore(close) || !close.isAfter(now) || close.isAfter(now.plusDays(30)) || open.isBefore(now.minusDays(1))
                || !close.isBefore(freeze) || !freeze.isBefore(random) || !random.isBefore(result) || result.isAfter(close.plusHours(2))
                || !result.isBefore(confirm) || confirm.isAfter(start) || !start.isBefore(end) || end.isAfter(start.plusDays(366))
                || !confirm.isBefore(promote) || promote.isAfter(end) || body.promotionSeconds()<60 || body.promotionSeconds()>86400)
                throw DomainException.invalid("批次时间须按申请、冻结、未来随机、结果、确认和使用周期先后排列，结果窗口最多2小时。");
            var current=body.seatIds().stream().map(rights::space).toList();
            for(var seat:current) {
                long floor=Business.number(seat,"floor_id");
                if(!floorIds.contains(floor) || !"SEAT".equals(seat.get("kind")) || !Set.of("BOOKABLE","PREPARATION").contains(seat.get("use_mode")))throw DomainException.invalid("资源池只能选择负责范围内的预约/备考单人席位。");
                rights.freeForBatch(Business.number(seat,"id"),floor,start,end,-1);
            }
            long id=b.insert("INSERT INTO preparation_batch(public_id,owner_id,title,purpose,cycle_starts_at,cycle_ends_at,opens_at,closes_at,freeze_deadline,random_at,result_deadline,confirmation_deadline,promotion_until,promotion_seconds,algorithm,source_chain,source_round,created_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                UUID.randomUUID().toString(),actor.id(),body.title(),body.purpose(),start,end,open,close,freeze,random,result,confirm,promote,body.promotionSeconds(),AllocationGraph.VERSION,RandomnessSource.CHAIN,RandomnessSource.roundAtOrAfter(random),now);
            for(var seat:current)b.jdbc.update("INSERT INTO preparation_pool(batch_id,space_id,floor_id,space_version) VALUES(?,?,?,?)",id,Business.number(seat,"id"),Business.number(seat,"floor_id"),Business.number(seat,"version"));
            b.audit(actor.id(),"PREPARATION_CREATED","BATCH",id,requestId);event(id,null,actor.id(),"CREATED",Map.of("poolCount",current.size()));
            return detail(id);
        });
    }
    public List<Map<String,Object>> list() {return b.jdbc.queryForList("SELECT * FROM preparation_batch ORDER BY id DESC LIMIT 100").stream().map(this::view).toList();}
    Map<String,Object> view(Map<String,Object> row) {
        var result=b.view(row); result.remove("owner_id");
        var now=b.now();String status=row.get("status").toString(),phase=status;
        if(status.equals("OPEN"))phase=now.isBefore(Business.date(row,"opens_at"))?"NOT_OPEN":now.isBefore(Business.date(row,"closes_at"))?"APPLICATION_OPEN":now.isBefore(Business.date(row,"freeze_deadline"))?"FREEZE_DUE":"FAILED_NO_RESULT";
        if(status.equals("FROZEN") && !now.isBefore(Business.date(row,"result_deadline")))phase="FAILED_NO_RESULT";
        if(status.equals("RESULT_PUBLISHED") && !now.isBefore(Business.date(row,"cycle_ends_at")))phase="CLOSED";
        result.put("effectivePhase",phase);result.put("serverNow",Business.iso(now));result.put("maxApplicants",200);result.put("maxSeats",400);
        result.put("allocationRule","先满足冻结硬约束内最多人数，再按抽签顺序尽量满足个人偏好；要约需确认，候补不重抽。");return result;
    }
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public Map<String,Object> detail(long id) {
        var row=view(batch(id,false));row.put("pool",pool(id).stream().map(b::view).toList());
        var results=b.jdbc.queryForList("SELECT maximum_count,candidate_count,published_at,input_hash,output_hash FROM allocation_result WHERE batch_id=?",id);
        row.put("result",results.isEmpty()?Map.of():b.view(results.get(0)));return row;
    }
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public Map<String,Object> mine(long user,long id) {
        batch(id,false);
        var rows=b.jdbc.queryForList("SELECT a.*,v.preferences_json,v.keep_waitlist,v.accepted_at FROM preparation_application a JOIN application_version v ON a.id=v.application_id AND a.current_version=v.revision WHERE a.batch_id=? AND a.user_id=?",id,user);
        if(rows.isEmpty())return Map.of("applied",false,"batch",detail(id));
        var result=b.view(rows.get(0));result.put("preferences",json.readValue(result.remove("preferences_json").toString(),List.class));
        result.put("offers",b.jdbc.queryForList("SELECT o.*,s.code,s.name FROM long_offer o JOIN space s ON o.space_id=s.id WHERE o.batch_id=? AND o.user_id=? ORDER BY o.id DESC",id,user).stream().map(row->{var v=b.view(row);v.put("effectiveStatus","OPEN".equals(row.get("status")) && !b.now().isBefore(Business.date(row,"deadline"))?"EXPIRED":row.get("status"));return v;}).toList());
        result.put("entitlements",b.jdbc.queryForList("SELECT e.*,s.code,s.name FROM seat_entitlement e JOIN space s ON e.space_id=s.id WHERE e.batch_id=? AND e.user_id=? ORDER BY e.id DESC",id,user).stream().map(row->{var v=b.view(row);v.put("effectiveStatus","ACTIVE".equals(row.get("status")) && !b.now().isBefore(Business.date(row,"ends_at"))?"EXPIRED":row.get("status"));return v;}).toList());
        var waits=b.jdbc.queryForList("SELECT * FROM waitlist_entry WHERE batch_id=? AND user_id=?",id,user);
        if(waits.isEmpty())result.put("waitlist",Map.of());
        else {var wait=b.view(waits.get(0));wait.put("effectiveStatus","ACTIVE".equals(wait.get("status")) && !b.now().isBefore(Business.date(batch(id,false),"promotion_until"))?"FINISHED":wait.get("status"));result.put("waitlist",wait);}
        var outcomes=b.jdbc.queryForList("SELECT * FROM allocation_outcome WHERE batch_id=? AND application_id=?",id,result.get("id"));
        result.put("outcome",outcomes.isEmpty()?Map.of():b.view(outcomes.get(0)));
        result.put("nextSteps",List.of("查看具体要约与确认期限","保留或退出稳定候补","选择真实可用的普通短约/其他空间","明确退出本轮"));
        result.put("applied",true);result.put("batch",detail(id));return result;
    }
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public Map<String,Object> submit(AuthService.Session session,long id,String key,Submit body,String requestId) {
        var batch=batch(id,true);b.users(List.of(session.actor().id()));var actor=b.current(session);Business.student(actor);
        return b.once(actor,key,"preparation.submit:"+id,body,()->{
            state(batch,"OPEN");var now=b.now();
            if(now.isBefore(Business.date(batch,"opens_at")) || !now.isBefore(Business.date(batch,"closes_at")))throw Business.conflict("APPLICATION_WINDOW_CLOSED","当前不在申请窗口，原已提交版本仍可查看。");
            checkOtherParticipation(actor.id(),id,Business.date(batch,"cycle_starts_at"),Business.date(batch,"cycle_ends_at"));
            var poolIds=new HashSet<>(pool(id).stream().map(p->Business.number(p,"space_id")).toList());
            if(body.preferences()==null || body.preferences().isEmpty() || body.preferences().size()>400)throw DomainException.invalid("请明确填写可接受席位及1–3级志愿。");
            var seats=new HashSet<Long>();var ranks=new TreeSet<Integer>();
            for(var preference:body.preferences()) {
                if(preference==null || !poolIds.contains(preference.seatId()) || !seats.add(preference.seatId()) || preference.rank()<1 || preference.rank()>3)throw DomainException.invalid("志愿中包含无效/重复席位或序号。");ranks.add(preference.rank());
            }
            int expected=1;for(int rank:ranks)if(rank!=expected++)throw DomainException.invalid("志愿等级须从1开始连续填写。");
            var previous=b.jdbc.queryForList("SELECT * FROM preparation_application WHERE batch_id=? AND user_id=?",id,actor.id());long application;int revision;
            if(previous.isEmpty()) {
                if(body.version()!=0)throw Business.conflict("STALE_VERSION","首次申请版本应为0。");
                if(b.jdbc.queryForObject("SELECT COUNT(*) FROM preparation_application WHERE batch_id=?",Integer.class,id)>=200)throw Business.conflict("BATCH_APPLICATION_LIMIT","本版批次最多200个申请，不能静默截断候选集合。");
                revision=1;application=b.insert("INSERT INTO preparation_application(batch_id,user_id,anonymous_id,current_version,created_at) VALUES(?,?,?,?,?)",id,actor.id(),UUID.randomUUID().toString(),revision,now);
            } else {
                var old=previous.get(0);Business.version(Business.number(old,"current_version"),body.version());
                application=Business.number(old,"id");revision=Math.toIntExact(Business.number(old,"current_version"))+1;
                b.jdbc.update("UPDATE preparation_application SET current_version=?,status='SUBMITTED',reason=NULL WHERE id=?",revision,application);
            }
            var ordered=body.preferences().stream().sorted(Comparator.comparingLong(Preference::seatId)).toList();
            b.jdbc.update("INSERT INTO application_version(application_id,revision,accepted_at,preferences_json,keep_waitlist) VALUES(?,?,?,?,?)",application,revision,now,json.writeValueAsString(ordered),body.keepWaitlist());
            event(id,actor.id(),actor.id(),"APPLICATION_VERSION",Map.of("application",application,"revision",revision));
            b.audit(actor.id(),"PREPARATION_SUBMITTED","APPLICATION",application,requestId);
            b.notify(actor.id(),"application:"+application+":version:"+revision,"志愿已提交","本次已提交版本进入待冻结集合；以我的申请中的回执和版本为准。","BATCH",id);
            return mine(actor.id(),id);
        });
    }
    void checkOtherParticipation(long user,long batch,LocalDateTime start,LocalDateTime end) {
        if(b.jdbc.queryForObject("SELECT COUNT(*) FROM preparation_application a JOIN preparation_batch t ON a.batch_id=t.id WHERE a.user_id=? AND t.id<>? AND a.status='SUBMITTED' AND t.status IN ('OPEN','FROZEN','RESULT_PUBLISHED') AND t.cycle_starts_at<? AND t.cycle_ends_at>? AND t.cycle_ends_at>?",Integer.class,user,batch,end,start,b.now())>0
            || b.jdbc.queryForObject("SELECT COUNT(*) FROM seat_entitlement WHERE user_id=? AND batch_id<>? AND status='ACTIVE' AND starts_at<? AND ends_at>? AND ends_at>?",Integer.class,user,batch,end,start,b.now())>0)
            throw Business.conflict("OVERLAPPING_LONG_CYCLE","你已参与另一重叠长期周期，请先明确退出原批次；不能同时占用两个长期席位。");
    }
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public Map<String,Object> withdrawApplication(AuthService.Session session,long id,String key,Action body) {
        var row=batch(id,true);b.users(List.of(session.actor().id()));var actor=b.current(session);
        return b.once(actor,key,"preparation.withdraw:"+id,body,()->{
            state(row,"OPEN");if(!b.now().isBefore(Business.date(row,"closes_at")))throw Business.conflict("APPLICATION_WINDOW_CLOSED","申请窗口已关闭，请使用结果后的退出操作。");
            var values=b.jdbc.queryForList("SELECT * FROM preparation_application WHERE batch_id=? AND user_id=?",id,actor.id());if(values.isEmpty())throw DomainException.missing();
            var application=values.get(0);Business.version(Business.number(application,"current_version"),body.version());
            if(!"WITHDRAW".equals(body.action()))throw DomainException.invalid("请选择明确退出申请。");
            if(!"SUBMITTED".equals(application.get("status")))throw Business.conflict("APPLICATION_STATE_CONFLICT","申请已经退出或不再有效。");
            b.jdbc.update("UPDATE preparation_application SET status='WITHDRAWN' WHERE id=?",Business.number(application,"id"));
            event(id,actor.id(),actor.id(),"APPLICATION_WITHDRAWN",Map.of("revision",body.version()));
            b.notify(actor.id(),"application:"+Business.number(application,"id")+":withdraw:"+body.version(),"已退出本轮申请","原志愿版本保留历史，不再参加本轮分配。","BATCH",id);
            return mine(actor.id(),id);
        });
    }
    void failLocked(Map<String,Object> batch,String reason,Long actor) {
        if(!Set.of("OPEN","FROZEN").contains(batch.get("status")))return;
        long id=Business.number(batch,"id");
        b.jdbc.update("UPDATE preparation_batch SET status='FAILED_NO_RESULT',failure_reason=?,version=version+1 WHERE id=?",reason,id);
        event(id,null,actor,"FAILED_NO_RESULT",Map.of("reason",reason,"originalDeadline",Business.iso(Business.date(batch,"result_deadline"))));
        for(var application:b.jdbc.queryForList("SELECT user_id FROM preparation_application WHERE batch_id=? AND status<>'WITHDRAWN'",id))b.notify(Business.number(application,"user_id"),"batch:"+id+":failed","本轮未能产生合法分配结果","本轮已明确停止，不是未中签。可查看故障原因、其他普通空间或退出；重新组织须另开新批次。","BATCH",id);
    }
}
