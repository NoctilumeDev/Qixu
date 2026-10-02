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
    public SpaceRights(Business b) { this.b=b; }
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
        if(!start.isBefore(end)) throw DomainException.invalid("结束时间须晚于开始时间。");
        var parents=new HashMap<Long,Long>();
        b.jdbc.queryForList("SELECT id,parent_id FROM space WHERE floor_id=?",floor).forEach(r->parents.put(Business.number(r,"id"),r.get("parent_id")==null?null:Business.number(r,"parent_id")));
        var now=b.now(); var rows=new ArrayList<Map<String,Object>>();
        rows.addAll(b.jdbc.queryForList("SELECT space_id,starts_at,ends_at,'SHORT' AS conflict_type FROM short_reservation WHERE floor_id=? AND starts_at<? AND ends_at>? AND ends_at>? AND (status='CHECKED_IN' OR (status='PENDING' AND check_in_deadline>?))",floor,end,start,now,now));
        rows.addAll(b.jdbc.queryForList("SELECT space_id,starts_at,ends_at,'VENUE' AS conflict_type FROM venue_entitlement WHERE floor_id=? AND request_id<>? AND status='ACTIVE' AND starts_at<? AND ends_at>? AND ends_at>?",floor,excludeVenue,end,start,now));
        return rows.stream().filter(r->related(space,Business.number(r,"space_id"),parents)).map(b::view).toList();
    }
    public void free(long space,long floor,LocalDateTime start,LocalDateTime end,long excludeVenue) {
        if(!conflicts(space,floor,start,end,excludeVenue).isEmpty()) throw Business.conflict("SPACE_CONFLICT","该空间或所属区域在此时段已有使用权，请选择其他时间或空间。");
    }
    public void personalFree(long user,LocalDateTime start,LocalDateTime end) {
        var now=b.now();
        if(b.jdbc.queryForObject("SELECT COUNT(*) FROM short_reservation WHERE user_id=? AND starts_at<? AND ends_at>? AND ends_at>? AND (status='CHECKED_IN' OR (status='PENDING' AND check_in_deadline>?))",Integer.class,user,end,start,now,now)>0)
            throw Business.conflict("PERSONAL_CONFLICT","你在此时段已有座位预约，请先调整原预约。");
    }
    public Map<String,Object> availability(long id,LocalDateTime start,LocalDateTime end) {
        var space=space(id); var conflicts=conflicts(id,Business.number(space,"floor_id"),start,end,-1);
        return Map.of("spaceId",id,"startsAt",Business.iso(start),"endsAt",Business.iso(end),"asOf",Business.iso(b.now()),"available",conflicts.isEmpty(),"conflicts",conflicts,"meaning","RIGHTS_ONLY_NOT_PHYSICAL_OCCUPANCY");
    }
}
