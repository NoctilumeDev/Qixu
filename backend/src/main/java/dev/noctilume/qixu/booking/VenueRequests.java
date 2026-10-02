package dev.noctilume.qixu.booking;

import dev.noctilume.qixu.common.*;
import dev.noctilume.qixu.identity.*;
import dev.noctilume.qixu.spaces.SpaceRights;
import java.time.OffsetDateTime;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
public class VenueRequests {
    private final Business b; private final SpaceRights rights;
    public record Create(long spaceId,OffsetDateTime startsAt,OffsetDateTime endsAt,int people,String purpose,String description,String contact) {}
    public record Decision(long version,String action,String reason) {}
    public VenueRequests(Business b,SpaceRights rights) { this.b=b; this.rights=rights; }
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public Map<String,Object> create(AuthService.Session session,String key,Create body,String requestId) {
        var initial=rights.space(body.spaceId()); long floor=Business.number(initial,"floor_id"); b.floors(List.of(floor)); b.users(List.of(session.actor().id())); var actor=b.current(session);
        if(actor.role().equals("STUDENT")) Business.student(actor);
        if(actor.role().equals("ADMIN")) b.admin(actor,floor);
        return b.once(actor,key,"venue.create",body,()->{
            var space=rights.space(body.spaceId()); var start=Business.time(body.startsAt()); var end=Business.time(body.endsAt());
            if(!Set.of("AREA","ROOM","HALL").contains(space.get("kind")) || body.people()<1 || body.people()>Business.number(space,"capacity")) throw DomainException.invalid("场地类型或申请人数不符合容量。");
            b.window(start,end,false); Business.text(body.purpose(),200,true); Business.text(body.description(),500,false); Business.text(body.contact(),80,true);
            long id=b.insert("INSERT INTO venue_request(user_id,space_id,floor_id,starts_at,ends_at,people,purpose,description,contact,created_at) VALUES(?,?,?,?,?,?,?,?,?,?)",actor.id(),body.spaceId(),floor,start,end,body.people(),body.purpose(),body.description(),body.contact(),b.now());
            b.audit(actor.id(),"VENUE_SUBMITTED","VENUE",id,requestId);
            b.notify(actor.id(),"venue:"+id+":submitted","场地申请已提交","待审批不产生使用权，请等待明确审批结果。","VENUE",id);
            return detail(actor,id);
        });
    }
    public Map<String,Object> detail(Actor actor,long id) {
        var row=b.row("venue_request",id); if(Business.number(row,"user_id")!=actor.id()) b.admin(actor,Business.number(row,"floor_id"));
        return b.view(row);
    }
    public List<Map<String,Object>> mine(Actor actor) { return b.jdbc.queryForList("SELECT * FROM venue_request WHERE user_id=? ORDER BY id DESC LIMIT 100",actor.id()).stream().map(b::view).toList(); }
    public List<Map<String,Object>> pending(Actor actor) {
        if(!actor.role().equals("ADMIN")) throw DomainException.forbidden();
        return b.jdbc.queryForList("SELECT v.* FROM venue_request v JOIN admin_scope s ON v.floor_id=s.floor_id WHERE s.user_id=? ORDER BY v.created_at DESC LIMIT 200",actor.id()).stream().map(b::view).toList();
    }
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public Map<String,Object> decide(AuthService.Session session,long id,String key,Decision body,String requestId) {
        // Event-bound cancellation uses Events.cancel, which acquires event before floor.
        var initial=b.row("venue_request",id); b.floors(List.of(Business.number(initial,"floor_id"))); b.users(List.of(session.actor().id(),Business.number(initial,"user_id"))); var actor=b.current(session);
        String action=body.action()==null?"":body.action();
        if(action.equals("CANCEL")) { if(Business.number(initial,"user_id")!=actor.id()) b.admin(actor,Business.number(initial,"floor_id")); }
        else b.admin(actor,Business.number(initial,"floor_id"));
        return b.once(actor,key,"venue.decide:"+id,body,()->{
            var row=b.row("venue_request",id); Business.version(Business.number(row,"version"),body.version()); Business.text(body.reason(),500,true); String next;
            if(action.equals("CANCEL")) {
                if(!Set.of("SUBMITTED","APPROVED").contains(row.get("status"))) throw Business.conflict("STATE_CONFLICT","申请已经结束。");
                if(b.jdbc.queryForObject("SELECT COUNT(*) FROM campus_event WHERE venue_request_id=? AND status<>'CANCELED'",Integer.class,id)>0) throw Business.conflict("EVENT_BOUND","该申请关联活动，请在活动中处理取消。");
                next="CANCELED";
            } else {
                if(!row.get("status").equals("SUBMITTED")) throw Business.conflict("STATE_CONFLICT","仅待审申请可以审批。");
                if(action.equals("APPROVE")) {
                    var space=rights.space(Business.number(row,"space_id"));
                    if(Business.number(row,"people")>Business.number(space,"capacity") || !Business.date(row,"starts_at").isAfter(b.now())) throw Business.conflict("APPROVAL_INVALID","容量或申请时间已变化，不能批准。");
                    rights.free(Business.number(row,"space_id"),Business.number(row,"floor_id"),Business.date(row,"starts_at"),Business.date(row,"ends_at"),-1);
                    b.insert("INSERT INTO venue_entitlement(request_id,user_id,space_id,floor_id,starts_at,ends_at,status) VALUES(?,?,?,?,?,?,'ACTIVE')",id,Business.number(row,"user_id"),Business.number(row,"space_id"),Business.number(row,"floor_id"),Business.date(row,"starts_at"),Business.date(row,"ends_at"));
                    next="APPROVED";
                } else if(action.equals("REJECT")) next="REJECTED";
                else throw DomainException.invalid("审批操作不正确。");
            }
            b.jdbc.update("UPDATE venue_request SET status=?,decision_note=?,version=version+1 WHERE id=?",next,body.reason(),id);
            if(next.equals("CANCELED")) b.jdbc.update("UPDATE venue_entitlement SET status='CLOSED' WHERE request_id=?",id);
            b.audit(actor.id(),"VENUE_"+next,"VENUE",id,requestId);
            b.notify(Business.number(row,"user_id"),"venue:"+id+":"+(body.version()+1),"场地申请结果","状态："+next+"；"+body.reason(),"VENUE",id);
            return detail(actor,id);
        });
    }
}
