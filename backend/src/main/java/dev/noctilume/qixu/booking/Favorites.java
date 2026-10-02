package dev.noctilume.qixu.booking;

import dev.noctilume.qixu.common.*;
import dev.noctilume.qixu.identity.AuthService;
import dev.noctilume.qixu.spaces.SpaceService;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
public class Favorites {
    private final Business b; private final SpaceService spaces;
    public record Change(long spaceId,boolean selected) {}
    public Favorites(Business b,SpaceService spaces) { this.b=b; this.spaces=spaces; }
    public Object mine(long user) { return b.jdbc.queryForList("SELECT space_id FROM favorite_space WHERE user_id=? ORDER BY created_at,space_id",user).stream().map(r->spaces.get(Business.number(r,"space_id"))).toList(); }
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public Map<String,Object> change(AuthService.Session session,String key,Change body) {
        b.users(List.of(session.actor().id())); var actor=b.current(session);
        return b.once(actor,key,"favorites.change",body,()->{
            spaces.get(body.spaceId());
            if(body.selected()) {
                int count=b.jdbc.queryForObject("SELECT COUNT(*) FROM favorite_space WHERE user_id=?",Integer.class,actor.id());
                int old=b.jdbc.queryForObject("SELECT COUNT(*) FROM favorite_space WHERE user_id=? AND space_id=?",Integer.class,actor.id(),body.spaceId());
                if(count>=6 && old==0) throw Business.conflict("FAVORITES_LIMIT","最多收藏6个空间用于对比，请先移除一项。");
                b.jdbc.update("INSERT IGNORE INTO favorite_space(user_id,space_id,created_at) VALUES(?,?,?)",actor.id(),body.spaceId(),b.now());
            } else b.jdbc.update("DELETE FROM favorite_space WHERE user_id=? AND space_id=?",actor.id(),body.spaceId());
            return Map.of("items",mine(actor.id()));
        });
    }
}
