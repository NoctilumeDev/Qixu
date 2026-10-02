package dev.noctilume.qixu.notifications;

import dev.noctilume.qixu.common.*;
import dev.noctilume.qixu.identity.AuthService;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
public class Notifications {
    private final Business b;
    public Notifications(Business b) { this.b=b; }
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public int dispatch() {
        var pending=b.jdbc.queryForList("SELECT * FROM notification_outbox WHERE delivered_at IS NULL ORDER BY id LIMIT 32 FOR UPDATE SKIP LOCKED");
        for(var row:pending) {
            b.jdbc.update("INSERT IGNORE INTO inbox(outbox_id,recipient_id,title,message,entity_type,entity_id,created_at) VALUES(?,?,?,?,?,?,?)",row.get("id"),row.get("recipient_id"),row.get("title"),row.get("message"),row.get("entity_type"),row.get("entity_id"),row.get("created_at"));
            b.jdbc.update("UPDATE notification_outbox SET delivered_at=? WHERE id=?",b.now(),row.get("id"));
        }
        return pending.size();
    }
    public Map<String,Object> mine(long user) {
        var items=b.jdbc.queryForList("SELECT * FROM inbox WHERE recipient_id=? ORDER BY id DESC LIMIT 100",user).stream().map(b::view).toList();
        long unread=b.jdbc.queryForObject("SELECT COUNT(*) FROM inbox WHERE recipient_id=? AND read_at IS NULL",Long.class,user);
        return Map.of("items",items,"unread",unread,"channel","PERSISTENT_INBOX");
    }
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public Map<String,Object> read(AuthService.Session session,String key,long id) {
        b.users(List.of(session.actor().id())); var actor=b.current(session);
        return b.once(actor,key,"inbox.read",Map.of("id",id),()->{
            if(b.jdbc.queryForObject("SELECT COUNT(*) FROM inbox WHERE id=? AND recipient_id=?",Integer.class,id,actor.id())!=1) throw DomainException.missing();
            b.jdbc.update("UPDATE inbox SET read_at=COALESCE(read_at,?) WHERE id=? AND recipient_id=?",b.now(),id,actor.id()); return mine(actor.id());
        });
    }
}
