package dev.noctilume.qixu.booking;

import dev.noctilume.qixu.common.*;
import dev.noctilume.qixu.identity.AuthService;
import dev.noctilume.qixu.spaces.SpaceRights;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
public class ShortReservations {
    private final Business b;
    private final SpaceRights rights;
    public record Create(long spaceId,OffsetDateTime startsAt,OffsetDateTime endsAt) {}
    public record Change(long version,String action) {}
    public ShortReservations(Business b,SpaceRights rights) { this.b=b; this.rights=rights; }
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public Map<String,Object> create(AuthService.Session session,String key,Create body,String requestId) {
        var initial=rights.space(body.spaceId()); long floor=Business.number(initial,"floor_id");
        b.floors(List.of(floor)); b.users(List.of(session.actor().id())); var actor=b.current(session); Business.student(actor);
        return b.once(actor,key,"short.create",body,()->{
            var space=rights.space(body.spaceId()); var start=Business.time(body.startsAt()); var end=Business.time(body.endsAt());
            if(!space.get("kind").equals("SEAT")) throw DomainException.invalid("短期预约只适用于单人座位。");
            b.window(start,end,true); rights.free(body.spaceId(),floor,start,end,-1); rights.personalFree(actor.id(),start,end);
            var deadline=start.plusMinutes(10).isAfter(end)?end:start.plusMinutes(10);
            long id=b.insert("INSERT INTO short_reservation(user_id,space_id,floor_id,starts_at,ends_at,check_in_deadline,status,created_at) VALUES(?,?,?,?,?,?,'PENDING',?)",actor.id(),body.spaceId(),floor,start,end,deadline,b.now());
            b.audit(actor.id(),"SHORT_CREATED","SHORT",id,requestId);
            b.notify(actor.id(),"short:"+id+":created","座位预约已建立","请在开始后10分钟内到场确认；未到会释放本次短约。","SHORT",id);
            return detail(actor.id(),id);
        });
    }
    public Map<String,Object> detail(long user,long id) {
        var row=b.row("short_reservation",id); if(Business.number(row,"user_id")!=user) throw DomainException.forbidden();
        var view=b.view(row); var status=row.get("status").toString(); var now=b.now();
        if(status.equals("PENDING") && !Business.date(row,"check_in_deadline").isAfter(now)) status="EXPIRED";
        else if(status.equals("CHECKED_IN") && !Business.date(row,"ends_at").isAfter(now)) status="ENDED";
        view.put("effectiveStatus",status); view.put("serverNow",Business.iso(now)); return view;
    }
    public List<Map<String,Object>> mine(long user) {
        return b.jdbc.queryForList("SELECT id FROM short_reservation WHERE user_id=? ORDER BY id DESC LIMIT 100",user).stream().map(r->detail(user,Business.number(r,"id"))).toList();
    }
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public Map<String,Object> change(AuthService.Session session,long id,String key,Change body,String requestId) {
        var initial=b.row("short_reservation",id); b.floors(List.of(Business.number(initial,"floor_id"))); b.users(List.of(session.actor().id())); var actor=b.current(session);
        if(Business.number(initial,"user_id")!=actor.id()) throw DomainException.forbidden();
        return b.once(actor,key,"short.change:"+id,body,()->{
            var row=b.row("short_reservation",id); Business.version(Business.number(row,"version"),body.version());
            var status=row.get("status").toString(); var now=b.now(); String next;
            if(!Set.of("PENDING","CHECKED_IN").contains(status)) throw Business.conflict("STATE_CONFLICT","该预约已结束，请查看当前记录。");
            if(!Business.date(row,"ends_at").isAfter(now) || (status.equals("PENDING") && !Business.date(row,"check_in_deadline").isAfter(now))) throw Business.conflict("RESERVATION_EXPIRED","预约已到期，无法继续变更。");
            switch(body.action()==null?"":body.action()) {
                case "CHECK_IN" -> { if(!status.equals("PENDING") || now.isBefore(Business.date(row,"starts_at"))) throw Business.conflict("CHECK_IN_WINDOW","请在预约开始后、宽限期结束前确认到场。"); next="CHECKED_IN"; }
                case "CANCEL" -> next="CANCELED";
                case "END" -> { if(!status.equals("CHECKED_IN")) throw Business.conflict("STATE_CONFLICT","尚未确认到场，请使用取消预约。"); next="ENDED"; }
                default -> throw DomainException.invalid("预约操作不正确。");
            }
            b.jdbc.update("UPDATE short_reservation SET status=?,version=version+1 WHERE id=?",next,id);
            b.audit(actor.id(),"SHORT_"+next,"SHORT",id,requestId);
            b.notify(actor.id(),"short:"+id+":"+(body.version()+1),"座位预约状态已更新","当前状态："+next,"SHORT",id);
            return detail(actor.id(),id);
        });
    }
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public void expire(long id) {
        var initial=b.row("short_reservation",id); b.floors(List.of(Business.number(initial,"floor_id"))); b.users(List.of(Business.number(initial,"user_id")));
        var row=b.row("short_reservation",id); var now=b.now(); String next=null;
        if(row.get("status").equals("PENDING") && !Business.date(row,"check_in_deadline").isAfter(now)) next="EXPIRED";
        if(row.get("status").equals("CHECKED_IN") && !Business.date(row,"ends_at").isAfter(now)) next="ENDED";
        if(next!=null) {
            b.jdbc.update("UPDATE short_reservation SET status=?,version=version+1 WHERE id=?",next,id);
            b.notify(Business.number(row,"user_id"),"short:"+id+":"+(Business.number(row,"version")+1),"座位预约已结束",next.equals("EXPIRED")?"本次短约未在宽限期内确认，已释放；这不影响长期席位资格。":"预约时段已结束。","SHORT",id);
        }
    }
}
