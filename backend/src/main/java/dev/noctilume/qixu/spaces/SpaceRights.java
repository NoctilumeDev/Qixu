package dev.noctilume.qixu.spaces;

import dev.noctilume.qixu.common.Business;
import dev.noctilume.qixu.common.DomainException;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.stereotype.Service;

/** Physical hierarchy and current rights. Occupancy reports are not rights. */
@Service
public class SpaceRights {
    private final Business b;
    private final SpatialFacts facts;
    private final BlockNotices notices;
    public SpaceRights(Business b,SpatialFacts facts,BlockNotices notices) { this.b=b; this.facts=facts; this.notices=notices; }
    public Set<Long> lockFloors(Collection<Long> floors) {return facts.lockFloors(floors);}
    public Map<String,Object> offerImpact(long id,long owner) {return facts.offerImpact(id,owner);}
    public void freeForOffer(long id,long owner,LocalDateTime start,LocalDateTime end,long batch,Long upgrade) {facts.freeForOffer(id,owner,start,end,batch,upgrade);}
    public Set<Long> venueLimitUsers(long venue) {return notices.venueUsers(venue);}
    public Set<Long> venueLimitBatches(long venue) {return notices.venueBatches(venue);}
    public void lockLimitBatches(Collection<Long> batches) {notices.lockBatches(batches);}
    public void checkVenueLimitBatches(long venue,Collection<Long> batches) {notices.checkVenueBatches(venue,batches);}
    /** Called only under the venue's coordinated floors, inside its cancellation/rebinding transaction. */
    public void closeVenueBlock(long venue,long actor,String reason) {
        for(var block:b.jdbc.queryForList("SELECT id FROM space_block WHERE venue_request_id=? AND status='ACTIVE'",venue)) {
            long id=Business.number(block,"id");var hash=notices.currentHash(id);var users=notices.users(id);b.jdbc.update("UPDATE space_block SET status='REVOKED',version=version+1,revoked_at=?,revoke_reason=? WHERE id=?",b.now(),reason,id);
            b.jdbc.update("INSERT INTO block_history(block_id,actor_id,action,reason,impact_hash,created_at) VALUES(?,?,'VENUE_CLOSED',?,?,?)",id,actor,reason,hash,b.now());
            for(long user:users)b.notify(user,"block:"+id+":venue-closed","活动空间限制已撤销","原归属保持，实际安排由所有仍有效限制共同决定。"+reason,"BLOCK",id);
        }
    }
    public Map<String,Object> space(long id) {
        var rows=b.jdbc.queryForList("SELECT * FROM space WHERE id=? AND active=TRUE",id);
        if(rows.isEmpty()) throw DomainException.missing();
        return rows.get(0);
    }
    private boolean related(long a,long c,Map<Long,Long> parents) {
        return ancestor(a,c,parents) || ancestor(c,a,parents);
    }
    private boolean ancestor(long a,long c,Map<Long,Long> parents) {
        var seen=new HashSet<Long>(); Long at=c;
        while(at!=null && seen.add(at)) { if(at==a) return true; at=parents.get(at); }
        return false;
    }
    public List<Map<String,Object>> conflicts(long space,long floor,LocalDateTime start,LocalDateTime end,long excludeVenue) {
        return conflicts(space,floor,start,end,excludeVenue,-1);
    }
    public List<Map<String,Object>> conflicts(long space,long floor,LocalDateTime start,LocalDateTime end,long excludeVenue,long excludeBatch) {
        if(!start.isBefore(end)) throw DomainException.invalid("结束时间须晚于开始时间。");
        return facts.conflicts(facts.snapshot(start,end),space,start,end,excludeVenue,excludeBatch,null,null);
    }
    public void free(long space,long floor,LocalDateTime start,LocalDateTime end,long excludeVenue) {
        if(!conflicts(space,floor,start,end,excludeVenue).isEmpty()) throw Business.conflict("SPACE_CONFLICT","该空间或所属区域在此时段已有使用权，请选择其他时间或空间。");
    }
    public void freeForBatch(long space,long floor,LocalDateTime start,LocalDateTime end,long batch) {
        if(!conflicts(space,floor,start,end,-1,batch).isEmpty())throw Business.conflict("SPACE_CONFLICT","本批次资源与其他当前有效事实冲突。");
    }
    public void freeForRestoration(long space,LocalDateTime start,LocalDateTime end,long batch,long entitlement) {
        var snapshot=facts.snapshot(start,end);var conflicts=facts.conflicts(snapshot,space,start,end,-1,-1,null,entitlement);
        conflicts=conflicts.stream().filter(c->!("BATCH_PROTECTION".equals(c.get("conflict_type"))&&Business.number(c,"space_id")==space&&snapshot.roots().stream().anyMatch(r->r.kind().equals("POOL")&&r.batch()==batch&&r.space()==space))).toList();
        if(!conflicts.isEmpty())throw Business.conflict("SPACE_CONFLICT","原位当前已有真实权或限制，不能为申诉恢复覆盖别人。");
    }
    public void personalFree(long user,LocalDateTime start,LocalDateTime end) {
        var now=b.now();
        if(b.jdbc.queryForObject("SELECT COUNT(*) FROM short_reservation WHERE user_id=? AND starts_at<? AND ends_at>? AND ends_at>? AND (status='CHECKED_IN' OR (status='PENDING' AND check_in_deadline>?))",Integer.class,user,end,start,now,now)>0)
            throw Business.conflict("PERSONAL_CONFLICT","你在此时段已有座位预约，请先调整原预约。");
        if(b.jdbc.queryForObject("SELECT COUNT(*) FROM seat_entitlement WHERE user_id=? AND status='ACTIVE' AND starts_at<? AND ends_at>? AND ends_at>?",Integer.class,user,end,start,now)>0)
            throw Business.conflict("PERSONAL_LONG_CONFLICT","你在此时段已有长期席位，请查看原使用权。");
    }
    public Map<String,Object> availability(long id,LocalDateTime start,LocalDateTime end) {
        var space=space(id); var conflicts=conflicts(id,Business.number(space,"floor_id"),start,end,-1);
        return Map.of("spaceId",id,"startsAt",Business.iso(start),"endsAt",Business.iso(end),"asOf",Business.iso(b.now()),"available",conflicts.isEmpty(),"conflicts",conflicts,"meaning","RIGHTS_ONLY_NOT_PHYSICAL_OCCUPANCY");
    }
}
