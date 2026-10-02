package dev.noctilume.qixu.preparation;

import dev.noctilume.qixu.common.*;
import org.springframework.stereotype.Component;

/** Current participation qualification, evaluated under the person's coordination lock for writes. */
@Component
public class LongEligibility {
    private final Business b;
    public LongEligibility(Business b) {this.b=b;}
    public String reason(long user) {
        var rows=b.jdbc.queryForList("SELECT active,student_verified FROM identity_user WHERE id=?",user);
        if(rows.isEmpty() || !Boolean.TRUE.equals(rows.get(0).get("active")) || !Boolean.TRUE.equals(rows.get(0).get("student_verified")))return "STUDENT_ELIGIBILITY_REQUIRED";
        var now=b.now();
        if(b.jdbc.queryForObject("SELECT COUNT(*) FROM long_application_penalty WHERE user_id=? AND status='ACTIVE' AND starts_at<=? AND ends_at>?",Integer.class,user,now,now)>0)return "LONG_APPLICATION_PENALTY";
        return null;
    }
    public void require(long user) {String reason=reason(user);if(reason!=null)throw Business.conflict(reason,"当前长期席位参与资格不成立，请查看资格或治理记录；旧提交回执仍可查询。");}
}
