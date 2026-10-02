package dev.noctilume.qixu.preparation;

import dev.noctilume.qixu.common.*;
import dev.noctilume.qixu.identity.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
public class AllocationPublisher {
    private final PreparationBatches p;
    public record Work(long batch,String inputHash,AllocationGraph.Input graph,long round) {}
    public AllocationPublisher(PreparationBatches p) {this.p=p;}
    public Work work(long id) {
        var batch=p.batch(id,false);PreparationBatches.state(batch,"FROZEN");var now=p.b.now();
        if(now.isBefore(Business.date(batch,"random_at")))throw Business.conflict("RANDOM_NOT_DUE","尚未到公布的固定随机轮次。");
        if(!now.isBefore(Business.date(batch,"result_deadline")))throw Business.conflict("RESULT_DEADLINE_PASSED","结果时限已过，旧执行不能发布。");
        var snapshot=p.b.jdbc.queryForMap("SELECT input_hash,input_bytes FROM frozen_input WHERE batch_id=?",id);
        var raw=snapshot.get("input_bytes").toString();var hash=snapshot.get("input_hash").toString();
        if(!Digests.sha256(raw).equals(hash))throw new IllegalStateException("Frozen bytes changed");
        var graph=p.json.readTree(raw).get("graph");
        return new Work(id,hash,p.json.treeToValue(graph,AllocationGraph.Input.class),Business.number(batch,"source_round"));
    }
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public boolean retainCandidate(Work work,RandomnessSource.Proof proof,AllocationGraph.Result candidate) {
        var batch=p.batch(work.batch(),true);
        if(!"FROZEN".equals(batch.get("status")))return false;
        if(!p.b.now().isBefore(Business.date(batch,"result_deadline"))) {p.failLocked(batch,"RESULT_DEADLINE_MISSED",null);return false;}
        if(p.b.now().isBefore(Business.date(batch,"random_at")))throw new IllegalStateException("Beacon before promised round time");
        var frozen=p.b.jdbc.queryForMap("SELECT input_hash FROM frozen_input WHERE batch_id=?",work.batch());
        if(!work.inputHash().equals(frozen.get("input_hash")) || work.round()!=Business.number(batch,"source_round")
            || !RandomnessSource.CHAIN.equals(proof.chain()) || proof.round()!=work.round() || !RandomnessSource.VERIFIER.equals(proof.verifier())
            || !AllocationGraph.VERSION.equals(candidate.algorithm()) || !Digests.sha256(AllocationGraph.VERSION+"\0"+work.inputHash()+"\0"+proof.randomness()).equals(candidate.seedDigest()))
            throw new IllegalStateException("Candidate coordinates do not match frozen input");
        String output=p.frozenJson.encode(candidate),outputHash=Digests.sha256(output);
        var existing=p.b.jdbc.queryForList("SELECT input_hash,output_hash FROM allocation_run WHERE batch_id=?",work.batch());
        if(!existing.isEmpty()) {
            if(!work.inputHash().equals(existing.get(0).get("input_hash")) || !outputHash.equals(existing.get(0).get("output_hash")))throw new IllegalStateException("Immutable run changed");
            return true;
        }
        p.b.jdbc.update("INSERT INTO allocation_run(batch_id,input_hash,output_hash,proof_json,output_json,created_at) VALUES(?,?,?,?,?,?)",work.batch(),work.inputHash(),outputHash,p.json.writeValueAsString(proof),output,p.b.now());
        p.event(work.batch(),null,null,"CANDIDATE_RETAINED",Map.of("inputHash",work.inputHash(),"outputHash",outputHash));return true;
    }
    private List<Map<String,Object>> people(long id) {return p.b.jdbc.queryForList("SELECT * FROM frozen_person WHERE batch_id=? ORDER BY anonymous_id",id);}
    private void guards(long id,List<Map<String,Object>> people,AuthService.Session session) {
        p.b.floors(p.floors(id));var ids=new ArrayList<>(people.stream().map(row->Business.number(row,"user_id")).toList());
        if(session!=null)ids.add(session.actor().id());p.b.users(ids);
    }
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public Map<String,Object> publish(AuthService.Session session,long id,String key,PreparationBatches.Action body) {
        var batch=p.batch(id,true);var people=people(id);guards(id,people,session);var actor=p.b.current(session);p.scope(actor,id);
        return p.b.once(actor,key,"preparation.allocate:"+id,body,()->{
            if(!"ALLOCATE".equals(body.action()))throw DomainException.invalid("分配操作不正确。");
            Business.version(Business.number(batch,"version"),body.version());PreparationBatches.state(batch,"FROZEN");
            publishLocked(batch,people,actor.id());return p.detail(id);
        });
    }
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public void publishDue(long id) {
        var batch=p.batch(id,true);if(!"FROZEN".equals(batch.get("status")))return;
        var people=people(id);guards(id,people,null);publishLocked(batch,people,null);
    }
    private void publishLocked(Map<String,Object> batch,List<Map<String,Object>> people,Long actor) {
        long id=Business.number(batch,"id");var now=p.b.now();
        if(!now.isBefore(Business.date(batch,"result_deadline"))) {p.failLocked(batch,"RESULT_DEADLINE_MISSED",actor);return;}
        if(now.isBefore(Business.date(batch,"random_at")))throw Business.conflict("RANDOM_NOT_DUE","还未到固定随机轮次。");
        var runs=p.b.jdbc.queryForList("SELECT * FROM allocation_run WHERE batch_id=?",id);if(runs.isEmpty())throw Business.conflict("NO_VERIFIED_CANDIDATE","尚未得到已验证来源的完整候选结果。");
        var run=runs.get(0);var snapshot=p.b.jdbc.queryForMap("SELECT input_hash,input_bytes FROM frozen_input WHERE batch_id=?",id);
        String output=p.frozenJson.normalize(run.get("output_json").toString());
        if(!run.get("input_hash").equals(snapshot.get("input_hash")) || !Digests.sha256(snapshot.get("input_bytes").toString()).equals(snapshot.get("input_hash")) || !Digests.sha256(output).equals(run.get("output_hash")))throw new IllegalStateException("Frozen input or candidate digest changed");
        var proof=p.json.readValue(run.get("proof_json").toString(),RandomnessSource.Proof.class);
        if(!RandomnessSource.CHAIN.equals(proof.chain()) || proof.round()!=Business.number(batch,"source_round") || !RandomnessSource.VERIFIER.equals(proof.verifier()))throw new IllegalStateException("Stored proof coordinates changed");
        var candidate=p.json.readValue(output,AllocationGraph.Result.class);
        if(!AllocationGraph.VERSION.equals(candidate.algorithm()) || !Digests.sha256(AllocationGraph.VERSION+"\0"+snapshot.get("input_hash")+"\0"+proof.randomness()).equals(candidate.seedDigest()))throw new IllegalStateException("Stored algorithm or seed changed");
        if(candidate.assignments().size()!=people.size() || candidate.order().size()!=people.size()
            || candidate.maximum()!=candidate.assignments().stream().filter(a->a.seat()!=null).count())throw new IllegalStateException("Candidate result is incomplete");
        var pool=p.pool(id);var seats=new HashMap<String,Map<String,Object>>();
        for(var seat:pool) {
            if(!Boolean.TRUE.equals(seat.get("active")) || Business.number(seat,"space_version")!=Business.number(seat,"current_space_version")
                || !p.rights.conflicts(Business.number(seat,"space_id"),Business.number(seat,"floor_id"),Business.date(batch,"cycle_starts_at"),Business.date(batch,"cycle_ends_at"),-1,id).isEmpty()) {p.failLocked(batch,"INPUT_INVALIDATED_RESOURCE",actor);return;}
            seats.put("s"+Business.number(seat,"space_id"),seat);
        }
        var mapping=new HashMap<String,Map<String,Object>>();people.forEach(person->mapping.put(person.get("anonymous_id").toString(),person));
        for(var person:people) {
            long user=Business.number(person,"user_id");var identity=p.b.jdbc.queryForMap("SELECT active,student_verified FROM identity_user WHERE id=?",user);
            if(!Boolean.TRUE.equals(identity.get("active")) || !Boolean.TRUE.equals(identity.get("student_verified"))) {p.failLocked(batch,"INPUT_INVALIDATED_ELIGIBILITY",actor);return;}
            try{p.checkOtherParticipation(user,id,Business.date(batch,"cycle_starts_at"),Business.date(batch,"cycle_ends_at"));}catch(DomainException e){p.failLocked(batch,"INPUT_INVALIDATED_PERSONAL_CYCLE",actor);return;}
        }
        var seenCandidates=new HashSet<String>();var seenSeats=new HashSet<String>();
        for(int index=0;index<candidate.assignments().size();index++) {
            var assignment=candidate.assignments().get(index);var person=mapping.get(assignment.candidate());
            if(person==null || !seenCandidates.add(assignment.candidate()) || !candidate.order().get(index).equals(assignment.candidate()))throw new IllegalStateException("Candidate mapping changed");
            if(assignment.seat()!=null) {
                if(!seats.containsKey(assignment.seat()) || !seenSeats.add(assignment.seat()))throw new IllegalStateException("Duplicate or invalid seat");
                var version=p.b.jdbc.queryForMap("SELECT preferences_json FROM application_version WHERE application_id=? AND revision=?",Business.number(person,"application_id"),Business.number(person,"revision"));
                var accepted=p.json.readValue(version.get("preferences_json").toString(),PreparationBatches.Preference[].class);
                boolean found=Arrays.stream(accepted).anyMatch(pref->assignment.seat().equals("s"+pref.seatId()) && Objects.equals(assignment.rank(),pref.rank()));
                if(!found)throw new IllegalStateException("Assignment breaks frozen hard constraints");
            }
        }
        p.b.jdbc.update("INSERT INTO allocation_result(batch_id,input_hash,output_hash,maximum_count,candidate_count,published_at) VALUES(?,?,?,?,?,?)",id,run.get("input_hash"),run.get("output_hash"),candidate.maximum(),people.size(),now);
        int position=0;
        for(var assignment:candidate.assignments()) {
            position++;var person=mapping.get(assignment.candidate());long application=Business.number(person,"application_id"),user=Business.number(person,"user_id");
            var seat=assignment.seat()==null?null:seats.get(assignment.seat());
            p.b.jdbc.update("INSERT INTO allocation_outcome(batch_id,application_id,lottery_position,space_id,floor_id,preference_rank,reason) VALUES(?,?,?,?,?,?,?)",id,application,position,seat==null?null:seat.get("space_id"),seat==null?null:seat.get("floor_id"),assignment.rank(),assignment.reason());
            var version=p.b.jdbc.queryForMap("SELECT keep_waitlist FROM application_version WHERE application_id=? AND revision=?",application,Business.number(person,"revision"));
            boolean keep=Boolean.TRUE.equals(version.get("keep_waitlist"));
            if(seat!=null)p.b.insert("INSERT INTO long_offer(batch_id,application_id,user_id,space_id,floor_id,preference_rank,deadline,created_at) VALUES(?,?,?,?,?,?,?,?)",id,application,user,seat.get("space_id"),seat.get("floor_id"),assignment.rank(),Business.date(batch,"confirmation_deadline"),now);
            if((seat==null && !assignment.reason().equals("NO_ACCEPTABLE_EDGE")) || (seat!=null && assignment.rank()>1 && keep))p.b.jdbc.update("INSERT INTO waitlist_entry(batch_id,application_id,user_id,lottery_position) VALUES(?,?,?,?)",id,application,user,position);
            String message=seat==null?"本轮未获得可接受席位，结果及稳定候补/普通短约入口已经可查询。":assignment.rank()==1?"已获得首选席位要约，请在公布的截止前确认；要约不等于正式使用权。":"首选未获得，已按预授权提供其他席位要约；请在期限前确认。"+(keep?"较高志愿候补仍按原顺序保留。":"你未选择保留较高志愿候补。");
            p.b.notify(user,"batch:"+id+":result","长期席位分配结果已公布",message,"BATCH",id);
        }
        // The final authorization occurs after all result/outbox writes. Late work rolls back the entire publication.
        var acceptedAt=p.b.now();
        if(!acceptedAt.isBefore(Business.date(batch,"result_deadline")))throw Business.conflict("RESULT_DEADLINE_PASSED","发布写入跨过结果期限，整批未生效。");
        p.b.jdbc.update("UPDATE allocation_result SET published_at=? WHERE batch_id=?",acceptedAt,id);
        p.b.jdbc.update("UPDATE allocation_run SET status='PUBLISHED' WHERE batch_id=?",id);
        p.b.jdbc.update("UPDATE preparation_batch SET status='RESULT_PUBLISHED',published_at=?,version=version+1 WHERE id=?",acceptedAt,id);
        p.event(id,null,actor,"RESULT_PUBLISHED",Map.of("outputHash",run.get("output_hash"),"maximum",candidate.maximum(),"candidates",people.size()));
    }
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public void failDue(long id) {
        var batch=p.batch(id,true);
        if("FROZEN".equals(batch.get("status")) && !p.b.now().isBefore(Business.date(batch,"result_deadline")))p.failLocked(batch,"RESULT_DEADLINE_MISSED",null);
    }
}
