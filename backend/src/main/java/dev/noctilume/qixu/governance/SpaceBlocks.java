package dev.noctilume.qixu.governance;

import dev.noctilume.qixu.common.*;
import dev.noctilume.qixu.identity.*;
import dev.noctilume.qixu.spaces.*;
import dev.noctilume.qixu.booking.VenueRequests;
import dev.noctilume.qixu.events.Events;
import dev.noctilume.qixu.preparation.FrozenJson;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import tools.jackson.databind.json.JsonMapper;

/** Plans act on complete current footprints. They do not rewrite allocation or original ownership. */
@Service
public class SpaceBlocks {
    private final Business b;private final SpaceRights rights;private final SpatialFacts facts;private final Events events;private final VenueRequests venues;private final JsonMapper json;private final FrozenJson canonical;
    public SpaceBlocks(Business b,SpaceRights rights,SpatialFacts facts,Events events,VenueRequests venues,JsonMapper json) {this.b=b;this.rights=rights;this.facts=facts;this.events=events;this.venues=venues;this.json=json;canonical=new FrozenJson(json);}
    public record Resolution(String impactKey,String action,Long targetSpaceId,Long replacementVenueId) {}
    public record Plan(long spaceId,String kind,OffsetDateTime startsAt,OffsetDateTime endsAt,String reason,Long venueRequestId,Long venueVersion,List<Resolution> resolutions,String impactHash) {}
    public record Revoke(long version,String reason) {}
    private record Impact(String key,SpatialFacts.Root root,long from,LocalDateTime start,LocalDateTime end,Long currentLocation) {}
    private record Context(SpatialFacts.Snapshot snapshot,Set<Long> floors,Set<Long> batches,Set<Long> eventIds,Set<Long> users,List<Impact> impacts,List<Map<String,Object>> eventRows,List<Map<String,Object>> applicantRows,String hash) {}
    private void validate(Plan plan) {
        if(!Set.of("MAINTENANCE","SAFETY","COURSE","EVENT").contains(plan.kind()==null?"":plan.kind()))throw DomainException.invalid("请选择明确的限制来源。");
        Business.text(plan.reason(),500,true);var start=Business.time(plan.startsAt());var end=Business.time(plan.endsAt());var now=b.now();
        if(!start.isBefore(end)||end.isAfter(start.plusDays(30))||!end.isAfter(now)||start.isAfter(now.plusDays(30))||start.isBefore(now.minusMinutes(5)))throw DomainException.invalid("限制须尚未结束、未来30天内开始、窗口不超过30天；即时安全限制最多追溯5分钟。");
        if(Set.of("EVENT","COURSE").contains(plan.kind())&&!start.isAfter(now))throw DomainException.invalid("计划活动和课程须在开始前完成审批。");
        if(plan.kind().equals("EVENT")!=(plan.venueRequestId()!=null)||plan.venueRequestId()!=null&&plan.venueVersion()==null)throw DomainException.invalid("活动限制须绑定明确场地申请和当前版本。");
        if(plan.resolutions()!=null&&(plan.resolutions().size()>500||plan.resolutions().stream().anyMatch(Objects::isNull)))throw DomainException.invalid("处置方案超过上限或含空项目。");
    }
    private List<Resolution> resolutions(Plan p) {return p.resolutions()==null?List.of():p.resolutions();}
    private Set<Long> bodyFloors(Plan p) {
        var result=new TreeSet<Long>(List.of(coordinateFloor(p.spaceId())));
        for(var r:resolutions(p)) {if(r.targetSpaceId()!=null)result.add(coordinateFloor(r.targetSpaceId()));if(r.replacementVenueId()!=null)result.add(Business.number(b.row("venue_request",r.replacementVenueId()),"floor_id"));}
        if(p.venueRequestId()!=null)result.add(Business.number(b.row("venue_request",p.venueRequestId()),"floor_id"));return result;
    }
    private long coordinateFloor(long space) {var rows=b.jdbc.queryForList("SELECT floor_id FROM space WHERE id=?",space);if(rows.isEmpty())throw DomainException.missing();return Business.number(rows.get(0),"floor_id");}
    private Map<String,Object> normalized(Plan p) {
        var v=new LinkedHashMap<String,Object>();v.put("spaceId",p.spaceId());v.put("kind",p.kind());v.put("startsAt",Business.iso(Business.time(p.startsAt())));v.put("endsAt",Business.iso(Business.time(p.endsAt())));v.put("reason",p.reason());v.put("venueRequestId",p.venueRequestId());v.put("venueVersion",p.venueVersion());v.put("resolutions",resolutions(p));return v;
    }
    private Context context(Plan p) {
        var start=Business.time(p.startsAt());var end=Business.time(p.endsAt());var s=facts.snapshot(start,end);
        if(resolutions(p).stream().anyMatch(r->"MOVE".equals(r.action()))) {
            var outerStart=start;var outerEnd=end;for(var root:s.roots())if(root.kind().equals("SHORT")&&SpatialFacts.related(p.spaceId(),root.space(),s.parents())) {if(root.start().isBefore(outerStart))outerStart=root.start();if(root.end().isAfter(outerEnd))outerEnd=root.end();}s=facts.snapshot(outerStart,outerEnd);
        }
        var floors=facts.connected(bodyFloors(p));var impacts=new ArrayList<Impact>();var batches=new TreeSet<Long>();var eventIds=new TreeSet<Long>();var users=new TreeSet<Long>();
        for(var root:s.roots())for(var piece:facts.pieces(s,root,start,end))for(long at:piece.locations())if(SpatialFacts.related(p.spaceId(),at,s.parents())) {
            var key=Digests.sha256(canonical.encode(Map.of("kind",root.kind(),"id",root.id(),"batch",root.batch(),"from",at,"start",Business.iso(piece.start()),"end",Business.iso(piece.end()))));
            impacts.add(new Impact(key,root,at,piece.start(),piece.end(),piece.location()));if(root.batch()>0)batches.add(root.batch());if(root.user()>0)users.add(root.user());floors.add(root.floor());
            if(root.kind().equals("VENUE"))for(var e:b.jdbc.queryForList("SELECT id FROM campus_event WHERE venue_request_id=? AND status<>'CANCELED'",root.id()))eventIds.add(Business.number(e,"id"));
        }
        if(impacts.stream().filter(i->!i.root().kind().equals("POOL")).count()>500||impacts.stream().filter(i->i.root().kind().equals("POOL")).count()>2000)throw Business.conflict("IMPACT_LIMIT","影响范围超过首版上限，请分段处理；未截断任何权。");
        if(p.venueRequestId()!=null) {users.add(Business.number(b.row("venue_request",p.venueRequestId()),"user_id"));for(var e:b.jdbc.queryForList("SELECT id FROM campus_event WHERE venue_request_id=? AND status<>'CANCELED'",p.venueRequestId()))eventIds.add(Business.number(e,"id"));}
        for(var r:resolutions(p))if(r.replacementVenueId()!=null) {users.add(Business.number(b.row("venue_request",r.replacementVenueId()),"user_id"));for(var e:b.jdbc.queryForList("SELECT id FROM campus_event WHERE venue_request_id=? AND status<>'CANCELED'",r.replacementVenueId()))eventIds.add(Business.number(e,"id"));}
        var applicants=new ArrayList<Map<String,Object>>();for(long batch:batches)for(var a:b.jdbc.queryForList("SELECT id,batch_id,user_id,current_version,status FROM preparation_application WHERE batch_id=? AND status='SUBMITTED' ORDER BY id",batch)) {applicants.add(a);users.add(Business.number(a,"user_id"));}
        var eventRows=new ArrayList<Map<String,Object>>();for(long id:eventIds) {var e=b.row("campus_event",id);eventRows.add(b.view(e));users.add(Business.number(e,"owner_id"));for(var r:b.jdbc.queryForList("SELECT * FROM event_participation WHERE event_id=? AND status IN ('CONFIRMED','WAITLISTED') ORDER BY id",id)) {users.add(Business.number(r,"user_id"));eventRows.add(b.view(r));}}
        if(users.size()>2000)throw Business.conflict("IMPACT_RECIPIENT_LIMIT","相关用户超过首版通知协调上限，未截断通知。");
        floors=facts.connected(floors);var boundFloors=floors;
        var evidence=new TreeMap<String,Object>();evidence.put("plan",normalized(p));evidence.put("spaces",s.spaces().values().stream().filter(r->boundFloors.contains(Business.number(r,"floor_id"))).map(b::view).toList());evidence.put("roots",s.roots().stream().filter(r->boundFloors.contains(r.floor())).map(r->b.view(r.fact())).toList());evidence.put("limits",s.limits().stream().filter(k->boundFloors.contains(k.floor())).map(k->b.view(k.fact())).toList());evidence.put("arrangements",s.arrangements().stream().filter(a->boundFloors.contains(Business.number(a.fact(),"from_floor_id"))).map(a->b.view(a.fact())).toList());evidence.put("events",eventRows);evidence.put("applicants",applicants.stream().map(b::view).toList());
        var replacement=new ArrayList<Map<String,Object>>();for(var r:resolutions(p))if(r.replacementVenueId()!=null)replacement.add(b.view(b.row("venue_request",r.replacementVenueId())));if(p.venueRequestId()!=null)replacement.add(b.view(b.row("venue_request",p.venueRequestId())));evidence.put("venueRequests",replacement);
        return new Context(s,floors,batches,eventIds,users,List.copyOf(impacts),eventRows,applicants,Digests.sha256(canonical.encode(evidence)));
    }
    private void scope(Actor actor,Plan p,Context c) {for(long floor:bodyFloors(p))b.admin(actor,floor);for(var i:c.impacts())b.admin(actor,i.root().floor());}
    private Map<String,Object> impactView(Impact i) {
        var r=new LinkedHashMap<String,Object>();r.put("impactKey",i.key());r.put("kind",i.root().kind());r.put("originId",i.root().id());r.put("userId",i.root().user());r.put("batchId",i.root().batch());r.put("version",i.root().version());r.put("fromSpaceId",i.from());r.put("currentSpaceId",i.currentLocation());r.put("startsAt",Business.iso(i.start()));r.put("endsAt",Business.iso(i.end()));r.put("needsResolution",!i.root().kind().equals("POOL"));return r;
    }
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public Map<String,Object> preview(Actor actor,Plan p) {validate(p);var c=context(p);scope(actor,p,c);return Map.of("impactHash",c.hash(),"impacts",c.impacts().stream().map(this::impactView).toList(),"notifyCount",c.users().size(),"asOf",Business.iso(b.now()),"meaning","OBSERVATION_NOT_AUTHORITY");}
    private Context locked(Plan p,Context initial,long actor) {
        for(long id:initial.batches())b.jdbc.queryForList("SELECT id FROM preparation_batch WHERE id=? FOR UPDATE",id);
        for(long id:initial.eventIds())b.jdbc.queryForList("SELECT id FROM campus_event WHERE id=? FOR UPDATE",id);
        var floors=rights.lockFloors(initial.floors());var users=new TreeSet<>(initial.users());users.add(actor);b.users(users);var c=context(p);
        if(!floors.containsAll(c.floors())||!initial.batches().containsAll(c.batches())||!initial.eventIds().containsAll(c.eventIds())||!users.containsAll(c.users()))throw Business.conflict("IMPACT_COORDINATES_CHANGED","等待期间出现新的权或关联，请重新预览，未补取逆序锁。");return c;
    }
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public Map<String,Object> submit(AuthService.Session session,String key,Plan p,String requestId) {
        var first=b.current(session);bodyFloors(p).forEach(f->b.admin(first,f));var old=b.replay(first,key,"block.create",p);if(old.isPresent())return old.get();
        validate(p);var c=locked(p,context(p),first.id());var actor=b.current(session);scope(actor,p,c);
        return b.once(actor,key,"block.create",p,()->{
            if(p.impactHash()==null||!p.impactHash().equals(c.hash()))throw Business.conflict("STALE_IMPACT","空间、使用权或通知对象已变化，请保留方案重新预览。");
            var choices=new HashMap<String,Resolution>();for(var r:resolutions(p))if(r.impactKey()==null||choices.put(r.impactKey(),r)!=null)throw DomainException.invalid("每个影响只能有一项明确处置。");
            var needed=c.impacts().stream().filter(i->!i.root().kind().equals("POOL")).map(Impact::key).collect(java.util.stream.Collectors.toSet());if(!choices.keySet().equals(needed))throw Business.conflict("INCOMPLETE_RESOLUTION","完整影响集合中的每项使用权都须处置，不接受遗漏或额外指定人。");
            long floor=Business.number(rights.space(p.spaceId()),"floor_id");var start=Business.time(p.startsAt());var end=Business.time(p.endsAt());
            if(p.venueRequestId()!=null) {var v=b.row("venue_request",p.venueRequestId());Business.version(Business.number(v,"version"),p.venueVersion());if(!v.get("status").equals("SUBMITTED")||Business.number(v,"space_id")!=p.spaceId()||!Business.date(v,"starts_at").equals(start)||!Business.date(v,"ends_at").equals(end))throw Business.conflict("VENUE_PLAN_CHANGED","活动处置须精确对应当前待审申请的场地和时间。");}
            long id=b.insert("INSERT INTO space_block(space_id,floor_id,kind,starts_at,ends_at,reason,venue_request_id,creator_id,created_at) VALUES(?,?,?,?,?,?,?,?,?)",p.spaceId(),floor,p.kind(),start,end,p.reason(),p.venueRequestId(),actor.id(),b.now());
            var changed=new HashMap<String,Resolution>();for(var i:c.impacts()) {
                var root=i.root();var choice=choices.get(i.key());String action=root.kind().equals("POOL")?"RESOURCE_NOTICE":choice.action();
                if(root.kind().equals("LONG")) {
                    boolean unavailable="UNAVAILABLE".equals(action);if(!("TEMPORARY".equals(action)||unavailable)||unavailable&&(!Set.of("MAINTENANCE","SAFETY").contains(p.kind())||choice.targetSpaceId()!=null)||!unavailable&&choice.targetSpaceId()==null||choice.replacementVenueId()!=null)throw DomainException.invalid("长期权只允许明确临时替代；仅安全维护可说明真实无替代。");
                    var source=c.snapshot().spaces().get(i.from());Map<String,Object> target=unavailable?null:rights.space(choice.targetSpaceId());if(target!=null)compatible(c.snapshot().spaces().get(root.space()),target);
                    b.insert("INSERT INTO long_temporary_arrangement(block_id,offer_id,from_space_id,from_floor_id,target_space_id,target_floor_id,starts_at,ends_at,created_at) VALUES(?,?,?,?,?,?,?,?,?)",id,root.id(),i.from(),source.get("floor_id"),target==null?null:target.get("id"),target==null?null:target.get("floor_id"),i.start(),i.end(),b.now());
                } else if(root.kind().equals("SHORT")) {
                    if(!Set.of("CANCEL","MOVE").contains(action==null?"":action)||choice.replacementVenueId()!=null)throw DomainException.invalid("短约须明确取消或迁移。");
                    var previous=changed.putIfAbsent("SHORT:"+root.id(),choice);if(previous==null)shortChange(id,root,choice,p.reason());else sameChoice(previous,choice);
                } else if(root.kind().equals("VENUE")) {
                    if(!Set.of("CANCEL","RELOCATE").contains(action==null?"":action)||choice.targetSpaceId()!=null)throw DomainException.invalid("场地须明确取消或使用原组织者的新申请换地。");
                    var previous=changed.putIfAbsent("VENUE:"+root.id(),choice);if(previous==null)venueChange(session,key,id,root,choice,p.reason(),requestId);else sameChoice(previous,choice);
                }
                b.insert("INSERT INTO block_impact(block_id,impact_key,kind,origin_id,from_space_id,starts_at,ends_at,action,detail_json) VALUES(?,?,?,?,?,?,?,?,?)",id,i.key(),root.kind(),root.id(),i.from(),i.start(),i.end(),action,json.writeValueAsString(choice==null?Map.of("batchId",root.batch()):choice));
            }
            // Every final target is tested against the same completed plan, including other new arrangements.
            var finalFacts=facts.snapshot(start,end);
            for(var i:c.impacts())if(i.root().kind().equals("LONG"))facts.freeForOffer(i.root().id(),i.root().user(),start,end,i.root().batch(),i.root().fact().get("upgrade_from_id")==null?null:Business.number(i.root().fact(),"upgrade_from_id"));
            for(var k:finalFacts.limits())if(k.id()!=id&&Set.of("COURSE","EVENT").contains(k.kind())&&SpatialFacts.related(p.spaceId(),k.space(),finalFacts.parents())&&SpatialFacts.overlap(start,end,k.start(),k.end()))throw Business.conflict("EXCLUSIVE_BLOCK_CONFLICT","该窗口仍有未处置课程或活动占用，不能覆盖。");
            if(p.venueRequestId()!=null)approveResolved(actor,p,id);
            for(long user:c.users())b.notify(user,"block:"+id+":created","空间使用安排发生变化",p.reason()+"。请查看原申请和每段临时安排；原长期归属保留，限制不改变原确认期限。","BLOCK",id);
            b.jdbc.update("INSERT INTO block_history(block_id,actor_id,action,reason,impact_hash,created_at) VALUES(?,?,'CREATED',?,?,?)",id,actor.id(),p.reason(),c.hash(),b.now());b.audit(actor.id(),"BLOCK_CREATED","BLOCK",id,requestId);
            if(!b.now().isBefore(end))throw Business.conflict("LIMIT_WINDOW_ENDED","提交过程中限制窗口已结束，整次处置未生效。");return get(actor,id);
        });
    }
    private void compatible(Map<String,Object> origin,Map<String,Object> target) {
        if(!"SEAT".equals(target.get("kind"))||!Set.of("BOOKABLE","PREPARATION").contains(target.get("use_mode"))||Business.number(origin,"id")==Business.number(target,"id"))throw DomainException.invalid("临时位置须为不同的有效单人席位。");
        var a=json.readTree(origin.get("profile_json").toString()).path("features");var z=json.readTree(target.get("profile_json").toString()).path("features");for(String f:List.of("window","outlet","quiet","accessible"))if(a.path(f).asBoolean()&&!z.path(f).asBoolean())throw Business.conflict("INCOMPATIBLE_ALTERNATIVE","替代席位不满足原空间已公开的设施条件。");
    }
    private void sameChoice(Resolution a,Resolution z) {if(!Objects.equals(a.action(),z.action())||!Objects.equals(a.targetSpaceId(),z.targetSpaceId())||!Objects.equals(a.replacementVenueId(),z.replacementVenueId()))throw DomainException.invalid("同一整约的时间片段须使用一致处置，不能指定冲突操作。");}
    private void shortChange(long block,SpatialFacts.Root root,Resolution choice,String reason) {
        var row=b.row("short_reservation",root.id());Business.version(Business.number(row,"version"),root.version());
        b.jdbc.update("UPDATE short_reservation SET status='CANCELED',version=version+1 WHERE id=?",root.id());
        if(choice.action().equals("MOVE")) {
            if(choice.targetSpaceId()==null)throw DomainException.invalid("迁移短约须选择真实替代位置。");var target=rights.space(choice.targetSpaceId());compatible(rights.space(root.space()),target);
            var identity=b.jdbc.queryForMap("SELECT active,student_verified FROM identity_user WHERE id=?",root.user());if(!Boolean.TRUE.equals(identity.get("active"))||!Boolean.TRUE.equals(identity.get("student_verified")))throw Business.conflict("IDENTITY_CHANGED","使用者资格已变化，请取消原短约，不产生新的使用权。");
            rights.free(choice.targetSpaceId(),Business.number(target,"floor_id"),root.start(),root.end(),-1);rights.personalFree(root.user(),root.start(),root.end());
            long replacement=b.insert("INSERT INTO short_reservation(user_id,space_id,floor_id,starts_at,ends_at,check_in_deadline,status,created_at) VALUES(?,?,?,?,?,?,?,?)",root.user(),choice.targetSpaceId(),Business.number(target,"floor_id"),root.start(),root.end(),row.get("check_in_deadline"),root.status(),b.now());
            b.jdbc.update("INSERT INTO short_relocation(original_id,replacement_id,block_id,created_at) VALUES(?,?,?,?)",root.id(),replacement,block,b.now());
        } else if(choice.targetSpaceId()!=null)throw DomainException.invalid("取消动作不能夹带替代位置。");
        b.notify(root.user(),"block:"+block+":short:"+root.id(),"短期预约已明确调整",choice.action()+"："+reason+"；请查看新旧记录，原期限不延长。","SHORT",root.id());
    }
    private void venueChange(AuthService.Session session,String key,long block,SpatialFacts.Root root,Resolution choice,String reason,String requestId) {
        var bound=b.jdbc.queryForList("SELECT * FROM campus_event WHERE venue_request_id=? AND status<>'CANCELED'",root.id());var actor=b.current(session);String inner="block-"+block+"-"+Digests.sha256(key+":"+root.id()).substring(0,24);
        if(choice.action().equals("RELOCATE")) {
            if(choice.replacementVenueId()==null||choice.replacementVenueId()==root.id())throw DomainException.invalid("换场地须选择新的申请。");var next=b.row("venue_request",choice.replacementVenueId());if(Business.number(next,"user_id")!=root.user())throw DomainException.forbidden();
            if(next.get("status").equals("SUBMITTED"))venues.decide(session,choice.replacementVenueId(),inner+"-approve",new VenueRequests.Decision(Business.number(next,"version"),"APPROVE",reason),requestId);
            else if(!next.get("status").equals("APPROVED"))throw Business.conflict("VENUE_NOT_APPROVED","替代申请当前不可批准。");
            if(!bound.isEmpty()) {var event=bound.get(0);events.change(session,Business.number(event,"id"),inner+"-event",new Events.Change(Business.number(event,"version"),"REBIND",choice.replacementVenueId(),reason),requestId);return;}
        } else if(choice.replacementVenueId()!=null)throw DomainException.invalid("取消不能夹带替代申请。");
        if(!bound.isEmpty()) {var event=bound.get(0);events.change(session,Business.number(event,"id"),inner+"-event",new Events.Change(Business.number(event,"version"),"CANCEL",null,reason),requestId);}
        else {b.jdbc.update("UPDATE venue_entitlement SET status='CLOSED' WHERE request_id=?",root.id());b.jdbc.update("UPDATE venue_request SET status='CANCELED',version=version+1,decision_note=? WHERE id=?",reason,root.id());rights.closeVenueBlock(root.id(),actor.id(),reason);b.notify(root.user(),"block:"+block+":venue:"+root.id(),"场地使用权已调整",choice.action()+"："+reason,"VENUE",root.id());}
    }
    private void approveResolved(Actor actor,Plan p,long block) {
        var v=b.row("venue_request",p.venueRequestId());var s=rights.space(p.spaceId());if(Business.number(v,"people")>Business.number(s,"capacity")||!Business.time(p.startsAt()).isAfter(b.now()))throw Business.conflict("APPROVAL_INVALID","审批写入前容量或开始时间已变化。");
        rights.free(p.spaceId(),Business.number(v,"floor_id"),Business.time(p.startsAt()),Business.time(p.endsAt()),p.venueRequestId());
        b.insert("INSERT INTO venue_entitlement(request_id,user_id,space_id,floor_id,starts_at,ends_at,status) VALUES(?,?,?,?,?,?,'ACTIVE')",p.venueRequestId(),v.get("user_id"),p.spaceId(),v.get("floor_id"),Business.time(p.startsAt()),Business.time(p.endsAt()));b.jdbc.update("UPDATE venue_request SET status='APPROVED',version=version+1,decision_note=? WHERE id=?",p.reason(),p.venueRequestId());b.audit(actor.id(),"VENUE_APPROVED_WITH_RESOLUTION","VENUE",p.venueRequestId(),"block:"+block);b.notify(Business.number(v,"user_id"),"venue:"+p.venueRequestId()+":resolved","场地已按完整处置批准",p.reason(),"VENUE",p.venueRequestId());
    }
    private Map<String,Object> row(long id) {var rows=b.jdbc.queryForList("SELECT * FROM space_block WHERE id=?",id);if(rows.isEmpty())throw DomainException.missing();return rows.get(0);}
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public Map<String,Object> get(Actor actor,long id) {
        var row=row(id);b.admin(actor,Business.number(row,"floor_id"));for(var r:b.jdbc.queryForList("SELECT DISTINCT o.floor_id,a.target_floor_id FROM long_temporary_arrangement a JOIN long_offer o ON a.offer_id=o.id WHERE a.block_id=?",id)) {b.admin(actor,Business.number(r,"floor_id"));if(r.get("target_floor_id")!=null)b.admin(actor,Business.number(r,"target_floor_id"));}
        var value=b.view(row);value.put("impacts",b.jdbc.queryForList("SELECT * FROM block_impact WHERE block_id=? ORDER BY id",id).stream().map(b::view).toList());value.put("arrangements",b.jdbc.queryForList("SELECT * FROM long_temporary_arrangement WHERE block_id=? ORDER BY id",id).stream().map(b::view).toList());value.put("history",b.jdbc.queryForList("SELECT * FROM block_history WHERE block_id=? ORDER BY id",id).stream().map(b::view).toList());return value;
    }
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public Map<String,Object> revoke(AuthService.Session session,long id,String key,Revoke body,String requestId) {
        var first=row(id);rights.lockFloors(List.of(Business.number(first,"floor_id")));var users=new TreeSet<Long>(List.of(session.actor().id()));for(var r:b.jdbc.queryForList("SELECT DISTINCT o.user_id FROM long_temporary_arrangement a JOIN long_offer o ON a.offer_id=o.id WHERE a.block_id=?",id))users.add(Business.number(r,"user_id"));if(users.size()>2000)throw Business.conflict("IMPACT_RECIPIENT_LIMIT","相关用户超过通知协调上限。");b.users(users);var actor=b.current(session);get(actor,id);
        return b.once(actor,key,"block.revoke:"+id,body,()->{var current=row(id);Business.version(Business.number(current,"version"),body.version());Business.text(body.reason(),500,true);if(current.get("venue_request_id")!=null)throw Business.conflict("EVENT_BOUND","活动来源限制必须随其场地/活动变更或取消，不能单独撤销。");if(!current.get("status").equals("ACTIVE"))throw Business.conflict("BLOCK_STATE_CONFLICT","限制已撤销。");
            b.jdbc.update("UPDATE space_block SET status='REVOKED',version=version+1,revoked_at=?,revoke_reason=? WHERE id=?",b.now(),body.reason(),id);b.jdbc.update("INSERT INTO block_history(block_id,actor_id,action,reason,impact_hash,created_at) VALUES(?,?,'REVOKED',?,?,?)",id,actor.id(),body.reason(),"0".repeat(64),b.now());for(long user:users)if(user!=actor.id())b.notify(user,"block:"+id+":revoked","一项空间限制已撤销",body.reason()+"；实际位置仍由其他有效限制共同决定，原长期归属不变。","BLOCK",id);b.audit(actor.id(),"BLOCK_REVOKED","BLOCK",id,requestId);return get(actor,id);});
    }
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public List<Map<String,Object>> publicLimits(long space,LocalDateTime start,LocalDateTime end) {
        rights.space(space);var s=facts.snapshot(start,end);return s.limits().stream().filter(k->SpatialFacts.related(space,k.space(),s.parents())).map(k->Map.<String,Object>of("id",k.id(),"spaceId",k.space(),"kind",k.kind(),"startsAt",Business.iso(k.start()),"endsAt",Business.iso(k.end()),"reason",k.fact().get("reason"))).toList();
    }
}
