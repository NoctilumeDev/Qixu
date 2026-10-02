package dev.noctilume.qixu.events;

import dev.noctilume.qixu.common.*;
import dev.noctilume.qixu.identity.*;
import dev.noctilume.qixu.spaces.SpaceRights;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
public class Events {
    private final Business b; private final SpaceRights rights;
    public record Create(long venueRequestId,String title,String eventType,String speaker,String description,String notice,int capacity,OffsetDateTime opensAt,OffsetDateTime closesAt,OffsetDateTime promotionUntil) {}
    public record Change(long version,String action,Long venueRequestId,String reason) {}
    public record Participate(String action) {}
    public Events(Business b,SpaceRights rights) { this.b=b; this.rights=rights; }
    private Map<String,Object> lock(long id) {
        var ids=b.jdbc.queryForList("SELECT id FROM campus_event WHERE id=? FOR UPDATE",id); if(ids.isEmpty()) throw DomainException.missing();
        return b.row("campus_event",id);
    }
    private void owner(Actor actor,Map<String,Object> event,Map<String,Object> venue) {
        Business.organizer(actor);
        if(actor.role().equals("ADMIN") || Business.number(event,"owner_id")!=actor.id()) b.admin(actor,Business.number(venue,"floor_id"));
    }
    private List<Long> affected(long event,long actor,long owner) {
        var users=new ArrayList<Long>(List.of(actor,owner));
        users.addAll(rights.venueLimitUsers(Business.number(b.row("campus_event",event),"venue_request_id")));
        b.jdbc.queryForList("SELECT user_id FROM event_participation WHERE event_id=? AND status IN ('CONFIRMED','WAITLISTED')",event).forEach(r->users.add(Business.number(r,"user_id"))); return users;
    }
    private void binding(Map<String,Object> venue,int capacity) {
        var now=b.now(); var space=rights.space(Business.number(venue,"space_id"));
        if(!venue.get("status").equals("APPROVED") || !Business.date(venue,"starts_at").isAfter(now) || capacity>Business.number(venue,"people") || capacity>Business.number(space,"capacity") || b.jdbc.queryForObject("SELECT COUNT(*) FROM venue_entitlement WHERE request_id=? AND status='ACTIVE'",Integer.class,Business.number(venue,"id"))!=1)
            throw Business.conflict("VENUE_NOT_APPROVED","活动需要有效、容量合规且尚未开始的已批准场地。");
        rights.free(Business.number(venue,"space_id"),Business.number(venue,"floor_id"),Business.date(venue,"starts_at"),Business.date(venue,"ends_at"),Business.number(venue,"id"));
    }
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public Map<String,Object> create(AuthService.Session session,String key,Create body,String requestId) {
        var initial=b.row("venue_request",body.venueRequestId()); rights.lockFloors(List.of(Business.number(initial,"floor_id"))); b.users(List.of(session.actor().id())); var actor=b.current(session); Business.organizer(actor);
        if(Business.number(initial,"user_id")!=actor.id()) throw DomainException.forbidden();
        if(actor.role().equals("ADMIN")) b.admin(actor,Business.number(initial,"floor_id"));
        return b.once(actor,key,"event.create",body,()->{
            var venue=b.row("venue_request",body.venueRequestId());
            Business.text(body.title(),120,true); Business.text(body.speaker(),100,true); Business.text(body.description(),2000,true); Business.text(body.notice(),500,false);
            if(!Set.of("READING","CLASS","LECTURE","OTHER").contains(body.eventType()==null?"":body.eventType()) || body.capacity()<1 || body.capacity()>Business.number(venue,"people")) throw DomainException.invalid("活动类型或人数上限不正确。");
            var open=Business.time(body.opensAt()); var close=Business.time(body.closesAt()); var until=Business.time(body.promotionUntil());
            if(!open.isBefore(close) || close.isAfter(until) || until.isAfter(Business.date(venue,"starts_at")) || !close.isAfter(b.now())) throw DomainException.invalid("报名窗口与递补期限必须在活动开始前。");
            if(b.jdbc.queryForObject("SELECT COUNT(*) FROM campus_event WHERE venue_request_id=?",Integer.class,body.venueRequestId())>0) throw Business.conflict("EVENT_BOUND","该场地申请已关联活动，请使用已有活动或新的申请。");
            long id=b.insert("INSERT INTO campus_event(owner_id,venue_request_id,title,event_type,speaker,description,notice,registration_opens_at,registration_closes_at,promotion_until,capacity,created_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)",actor.id(),body.venueRequestId(),body.title(),body.eventType(),body.speaker(),body.description(),body.notice(),open,close,until,body.capacity(),b.now());
            b.audit(actor.id(),"EVENT_DRAFT","EVENT",id,requestId); return detail(actor,id);
        });
    }
    public Map<String,Object> detail(Actor actor,long id) {
        var event=b.row("campus_event",id); var venue=b.row("venue_request",Business.number(event,"venue_request_id"));
        if(event.get("status").equals("DRAFT")) owner(actor,event,venue);
        var value=b.view(event); value.put("startsAt",Business.iso(Business.date(venue,"starts_at"))); value.put("endsAt",Business.iso(Business.date(venue,"ends_at"))); value.put("spaceId",venue.get("space_id")); value.put("floorId",venue.get("floor_id"));
        String phase=event.get("status").toString(); var now=b.now();
        if(phase.equals("PUBLISHED")) {
            if(!Business.date(venue,"ends_at").isAfter(now)) phase="COMPLETED";
            else if(!Business.date(venue,"starts_at").isAfter(now)) phase="IN_PROGRESS";
            else if(!Business.date(event,"registration_closes_at").isAfter(now)) phase="REGISTRATION_CLOSED";
            else if(!Business.date(event,"registration_opens_at").isAfter(now)) phase="REGISTRATION_OPEN";
        }
        value.put("phase",phase); value.put("serverNow",Business.iso(now)); value.put("remaining",Business.number(event,"capacity")-Business.number(event,"confirmed_count"));
        var part=b.jdbc.queryForList("SELECT id,status,sequence_number,version FROM event_participation WHERE event_id=? AND user_id=?",id,actor.id()); value.put("myParticipation",part.isEmpty()?Map.of():b.view(part.get(0)));
        return value;
    }
    public List<Map<String,Object>> list(Actor actor,boolean own) {
        if(own) Business.organizer(actor);
        return b.jdbc.queryForList(own?"SELECT id FROM campus_event WHERE owner_id=? ORDER BY id DESC LIMIT 100":"SELECT id FROM campus_event WHERE status<>'DRAFT' ORDER BY id DESC LIMIT 100",own?new Object[]{actor.id()}:new Object[]{}).stream().map(r->detail(actor,Business.number(r,"id"))).toList();
    }
    public List<Map<String,Object>> participations(Actor actor) {
        return b.jdbc.queryForList("SELECT event_id FROM event_participation WHERE user_id=? ORDER BY id DESC LIMIT 100",actor.id()).stream().map(r->detail(actor,Business.number(r,"event_id"))).toList();
    }
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public Map<String,Object> change(AuthService.Session session,long id,String key,Change body,String requestId) {
        var initial=lock(id); var oldVenue=b.row("venue_request",Business.number(initial,"venue_request_id"));
        var newVenue=body.venueRequestId()==null?oldVenue:b.row("venue_request",body.venueRequestId());
        rights.lockFloors(List.of(Business.number(oldVenue,"floor_id"),Business.number(newVenue,"floor_id"))); b.users(affected(id,session.actor().id(),Business.number(initial,"owner_id"))); var actor=b.current(session); owner(actor,initial,oldVenue);
        return b.once(actor,key,"event.change:"+id,body,()->{
            var event=b.row("campus_event",id); Business.version(Business.number(event,"version"),body.version()); Business.text(body.reason(),500,true);
            switch(body.action()==null?"":body.action()) {
                case "PUBLISH" -> {
                    if(!event.get("status").equals("DRAFT")) throw Business.conflict("STATE_CONFLICT","仅草稿可以发布。");
                    var venue=b.row("venue_request",Business.number(event,"venue_request_id")); binding(venue,(int)Business.number(event,"capacity"));
                    if(!Business.date(event,"registration_closes_at").isAfter(b.now())) throw Business.conflict("REGISTRATION_CLOSED","报名截止时间已过。");
                    b.jdbc.update("UPDATE campus_event SET status='PUBLISHED',version=version+1 WHERE id=?",id);
                }
                case "REBIND" -> {
                    if(!event.get("status").equals("PUBLISHED") || !Business.date(oldVenue,"starts_at").isAfter(b.now()) || body.venueRequestId()==null || body.venueRequestId()==Business.number(event,"venue_request_id")) throw Business.conflict("STATE_CONFLICT","请为尚未开始的已发布活动选择新的获批申请。");
                    if(Business.number(newVenue,"user_id")!=Business.number(event,"owner_id")) throw DomainException.forbidden();
                    if(actor.role().equals("ADMIN")) b.admin(actor,Business.number(newVenue,"floor_id"));
                    if(b.jdbc.queryForObject("SELECT COUNT(*) FROM campus_event WHERE venue_request_id=?",Integer.class,body.venueRequestId())>0) throw Business.conflict("EVENT_BOUND","新申请已关联其他活动。");
                    binding(newVenue,(int)Business.number(event,"capacity"));
                    if(Business.date(event,"promotion_until").isAfter(Business.date(newVenue,"starts_at"))) throw Business.conflict("EVENT_WINDOW_CONFLICT","新时间早于既有报名/递补期限，请取消重发或选择更晚时间。");
                    b.jdbc.update("UPDATE venue_entitlement SET status='CLOSED' WHERE request_id=?",Business.number(event,"venue_request_id"));
                    rights.closeVenueBlock(Business.number(event,"venue_request_id"),actor.id(),body.reason());
                    b.jdbc.update("UPDATE venue_request SET status='CANCELED',decision_note=?,version=version+1 WHERE id=?","活动已换场地/时间："+body.reason(),Business.number(event,"venue_request_id"));
                    b.jdbc.update("UPDATE campus_event SET venue_request_id=?,version=version+1 WHERE id=?",body.venueRequestId(),id);
                    notifyParticipants(id,"changed:"+(body.version()+1),"活动时间或场地已变更","报名继续有效，请查看新安排；如不合适可以取消。"+body.reason());
                }
                case "CANCEL" -> {
                    if(event.get("status").equals("CANCELED") || !Business.date(oldVenue,"ends_at").isAfter(b.now())) throw Business.conflict("STATE_CONFLICT","活动已结束。");
                    notifyParticipants(id,"canceled:"+(body.version()+1),"活动已取消",body.reason());
                    b.jdbc.update("UPDATE event_participation SET status='EVENT_CANCELED',version=version+1 WHERE event_id=? AND status IN ('CONFIRMED','WAITLISTED')",id);
                    b.jdbc.update("UPDATE campus_event SET status='CANCELED',confirmed_count=0,version=version+1 WHERE id=?",id);
                    b.jdbc.update("UPDATE venue_entitlement SET status='CLOSED' WHERE request_id=?",Business.number(event,"venue_request_id"));
                    rights.closeVenueBlock(Business.number(event,"venue_request_id"),actor.id(),body.reason());
                    b.jdbc.update("UPDATE venue_request SET status='CANCELED',decision_note=?,version=version+1 WHERE id=?",body.reason(),Business.number(event,"venue_request_id"));
                }
                default -> throw DomainException.invalid("活动操作不正确。");
            }
            b.audit(actor.id(),"EVENT_"+body.action(),"EVENT",id,requestId);
            b.notify(Business.number(event,"owner_id"),"event:"+id+":"+(body.version()+1),"活动状态已更新",body.reason(),"EVENT",id);
            return detail(actor,id);
        });
    }
    private void notifyParticipants(long id,String kind,String title,String message) {
        b.jdbc.queryForList("SELECT user_id FROM event_participation WHERE event_id=? AND status IN ('CONFIRMED','WAITLISTED')",id).forEach(r->b.notify(Business.number(r,"user_id"),"event:"+id+":"+kind,title,message,"EVENT",id));
    }
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public Map<String,Object> participate(AuthService.Session session,long id,String key,Participate body,String requestId) {
        var event=lock(id); var venue=b.row("venue_request",Business.number(event,"venue_request_id")); rights.lockFloors(List.of(Business.number(venue,"floor_id"))); b.users(affected(id,session.actor().id(),Business.number(event,"owner_id"))); var actor=b.current(session); Business.student(actor);
        return b.once(actor,key,"event.participate:"+id,body,()->{
            var now=b.now(); if(!event.get("status").equals("PUBLISHED") || !Business.date(venue,"starts_at").isAfter(now)) throw Business.conflict("EVENT_UNAVAILABLE","活动尚未发布、已取消或已开始。");
            var parts=b.jdbc.queryForList("SELECT * FROM event_participation WHERE event_id=? AND user_id=?",id,actor.id()); var old=parts.isEmpty()?null:parts.get(0);
            switch(body.action()==null?"":body.action()) {
                case "JOIN" -> {
                    if(now.isBefore(Business.date(event,"registration_opens_at")) || !now.isBefore(Business.date(event,"registration_closes_at"))) throw Business.conflict("REGISTRATION_CLOSED","当前不在报名窗口内。");
                    if(old!=null && Set.of("CONFIRMED","WAITLISTED").contains(old.get("status"))) return detail(actor,id);
                    promote(event);
                    int count=b.jdbc.queryForObject("SELECT confirmed_count FROM campus_event WHERE id=?",Integer.class,id);
                    String status=count<Business.number(event,"capacity")?"CONFIRMED":"WAITLISTED"; long seq=Business.number(event,"next_sequence");
                    if(old==null) b.insert("INSERT INTO event_participation(event_id,user_id,sequence_number,status,created_at) VALUES(?,?,?,?,?)",id,actor.id(),seq,status,now);
                    else b.jdbc.update("UPDATE event_participation SET sequence_number=?,status=?,version=version+1,created_at=? WHERE id=?",seq,status,now,Business.number(old,"id"));
                    b.jdbc.update("UPDATE campus_event SET next_sequence=next_sequence+1,confirmed_count=confirmed_count+?,version=version+1 WHERE id=?",status.equals("CONFIRMED")?1:0,id);
                    b.notify(actor.id(),"event:"+id+":join:"+seq,status.equals("CONFIRMED")?"活动预约成功":"已进入活动候补","请在我的活动查看当前结果；候补按既定顺序递补。","EVENT",id);
                }
                case "CANCEL" -> {
                    if(old==null || !Set.of("CONFIRMED","WAITLISTED").contains(old.get("status"))) throw Business.conflict("STATE_CONFLICT","没有可取消的有效报名。");
                    b.jdbc.update("UPDATE event_participation SET status='CANCELED',version=version+1 WHERE id=?",Business.number(old,"id"));
                    b.jdbc.update("UPDATE campus_event SET confirmed_count=confirmed_count-?,version=version+1 WHERE id=?",old.get("status").equals("CONFIRMED")?1:0,id);
                    promote(event);
                    b.notify(actor.id(),"event:"+id+":leave:"+(Business.number(old,"version")+1),"活动预约已取消","已释放本次参与名额，不影响其他空间使用权。","EVENT",id);
                }
                default -> throw DomainException.invalid("报名操作不正确。");
            }
            b.audit(actor.id(),"PARTICIPATION_"+body.action(),"EVENT",id,requestId); return detail(actor,id);
        });
    }
    private void promote(Map<String,Object> event) {
        if(!b.now().isBefore(Business.date(event,"promotion_until"))) return;
        long id=Business.number(event,"id");
        var waiting=b.jdbc.queryForList("SELECT p.* FROM event_participation p JOIN identity_user u ON p.user_id=u.id WHERE p.event_id=? AND p.status='WAITLISTED' AND u.active=TRUE AND u.student_verified=TRUE ORDER BY p.sequence_number",id);
        long count=b.jdbc.queryForObject("SELECT confirmed_count FROM campus_event WHERE id=?",Long.class,id);
        for(var p:waiting) {
            if(count>=Business.number(event,"capacity")) break;
            b.jdbc.update("UPDATE event_participation SET status='CONFIRMED',version=version+1 WHERE id=?",Business.number(p,"id")); count++;
            b.notify(Business.number(p,"user_id"),"event:"+id+":promoted:"+Business.number(p,"sequence_number"),"活动候补已递补","已取得参与名额，请查看活动最新安排。","EVENT",id);
        }
        b.jdbc.update("UPDATE campus_event SET confirmed_count=? WHERE id=?",count,id);
    }
}
