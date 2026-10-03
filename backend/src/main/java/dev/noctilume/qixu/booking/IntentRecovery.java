package dev.noctilume.qixu.booking;

import dev.noctilume.qixu.common.*;
import dev.noctilume.qixu.identity.AuthService;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** A per-actor barrier, not cancellation of any existing business right. */
@Service
public class IntentRecovery {
    private final Business b;
    public IntentRecovery(Business b) { this.b=b; }
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public Object stop(AuthService.Session session,String key) {
        if(key==null || !key.matches("[A-Za-z0-9_-]{8,80}")) throw DomainException.invalid("请提供有效的原请求标识。");
        b.users(List.of(session.actor().id()));
        var actor=b.current(session);
        if(b.jdbc.queryForObject("SELECT COUNT(*) FROM idempotency_receipt WHERE actor_id=? AND request_key=?",Integer.class,actor.id(),key)==0) {
            b.once(actor,key,"client.intent-stop",Map.of(),()->Map.of("intentOutcome","STOPPED_WITHOUT_EFFECT"));
        }
        return b.receipt(actor.id(),key);
    }
}
