package dev.noctilume.qixu.spaces;

import dev.noctilume.qixu.common.*;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.stereotype.Component;

/** Facilities change under all affected coordinates; this does not grant or withdraw rights. */
@Component
public class SpaceFactChanges {
    private final Business b;private final SpatialFacts facts;private final BlockNotices notices;
    public SpaceFactChanges(Business b,SpatialFacts facts,BlockNotices notices) {this.b=b;this.facts=facts;this.notices=notices;}
    public record Context(Set<Long> batches,Set<Long> events,Set<Long> floors,Set<Long> users,Set<Long> recipients) {}
    private Context discover(long space,Collection<Long> extraBatches,Collection<Long> extraUsers,Collection<Long> extraFloors) {
        var now=b.now();var end=LocalDateTime.of(9999,12,31,23,59,59);var snapshot=facts.snapshot(now,end);var place=snapshot.spaces().get(space);if(place==null)throw DomainException.missing();
        var batches=new TreeSet<>(extraBatches);var events=new TreeSet<Long>();var floors=new TreeSet<>(extraFloors);floors.add(Business.number(place,"floor_id"));var users=new TreeSet<>(extraUsers);var recipients=new TreeSet<Long>();
        for(var root:snapshot.roots())if(SpatialFacts.related(space,root.space(),snapshot.parents())||facts.pieces(snapshot,root,now,end).stream().anyMatch(p->p.locations().stream().anyMatch(at->SpatialFacts.related(space,at,snapshot.parents())))) {
            floors.add(root.floor());if(root.batch()>0)batches.add(root.batch());if(root.user()>0)recipients.add(root.user());
            if(root.kind().equals("VENUE"))for(var e:b.jdbc.queryForList("SELECT id FROM campus_event WHERE venue_request_id=? AND status<>'CANCELED'",root.id()))events.add(Business.number(e,"id"));
        }
        for(long batch:batches)for(var a:b.jdbc.queryForList("SELECT user_id FROM preparation_application WHERE batch_id=? AND status='SUBMITTED' ORDER BY user_id LIMIT 2001",batch))recipients.add(Business.number(a,"user_id"));
        for(long event:events) {recipients.add(Business.number(b.row("campus_event",event),"owner_id"));for(var p:b.jdbc.queryForList("SELECT user_id FROM event_participation WHERE event_id=? AND status IN ('CONFIRMED','WAITLISTED') ORDER BY user_id LIMIT 2001",event))recipients.add(Business.number(p,"user_id"));}
        users.addAll(recipients);if(users.size()>2000||batches.size()>2000||events.size()>2000)throw Business.conflict("IMPACT_RECIPIENT_LIMIT","设施变化关联对象超过首版上限，未截断。");return new Context(batches,events,facts.connected(floors),users,recipients);
    }
    public Context lock(long space,Collection<Long> batches,Collection<Long> users,Collection<Long> floors) {
        var initial=discover(space,batches,users,floors);notices.lockBatches(initial.batches());for(long event:initial.events())b.jdbc.queryForList("SELECT id FROM campus_event WHERE id=? FOR UPDATE",event);var lockedFloors=facts.lockFloors(initial.floors());
        var current=discover(space,batches,users,floors);if(!initial.batches().containsAll(current.batches())||!initial.events().containsAll(current.events())||!lockedFloors.containsAll(current.floors()))throw Business.conflict("IMPACT_COORDINATES_CHANGED","设施变化等待期间出现新关联，请重试，未补逆序锁。");b.users(current.users());
        var after=discover(space,batches,users,floors);if(!current.users().containsAll(after.users())||!initial.batches().containsAll(after.batches())||!initial.events().containsAll(after.events())||!lockedFloors.containsAll(after.floors()))throw Business.conflict("IMPACT_COORDINATES_CHANGED","设施变化等待期间出现新通知对象，请重试。");return after;
    }
    public void notify(long space,Context context) {
        long version=b.jdbc.queryForObject("SELECT version FROM space WHERE id=?",Long.class,space);
        for(long user:context.recipients())b.notify(user,"space:"+space+":facts:"+version,"空间设施事实已更新","与你的申请或使用安排有关的空间设施经管理员核实发生变化。请查看最新档案、限制及临时位置；原归属和冻结结果未改写。","SPACE",space);
    }
}
