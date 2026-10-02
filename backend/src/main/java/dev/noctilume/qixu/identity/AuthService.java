package dev.noctilume.qixu.identity;

import dev.noctilume.qixu.common.Digests;
import dev.noctilume.qixu.common.DomainException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final JdbcTemplate jdbc;
    private final Clock clock;
    private final int sessionHours;
    private final BCryptPasswordEncoder encoder=new BCryptPasswordEncoder(12);
    private final String dummyHash=encoder.encode(Digests.token());
    public record Session(Actor actor,String tokenHash,String csrf) {}
    public record Login(Actor actor,String token,String csrf) {}
    public AuthService(JdbcTemplate jdbc,Clock clock,@Value("${qixu.session-hours:12}") int sessionHours) {
        this.jdbc=jdbc; this.clock=clock; this.sessionHours=sessionHours;
    }
    private LocalDateTime now() { return LocalDateTime.ofInstant(clock.instant(),ZoneOffset.UTC); }
    static Actor actor(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new Actor(rs.getLong("id"),rs.getString("username"),rs.getString("display_name"),rs.getString("role"),rs.getBoolean("student_verified"),rs.getLong("auth_version"));
    }
    @Transactional(isolation=Isolation.READ_COMMITTED,noRollbackFor=DomainException.class)
    public Login login(String username,String password,String remote,String requestId) {
        if (username == null || !username.matches("[A-Za-z0-9_.-]{2,64}") || password == null || password.isEmpty() || password.getBytes(StandardCharsets.UTF_8).length>72) {
            throw DomainException.invalid("账号或密码格式不正确。");
        }
        String account=username.toLowerCase(Locale.ROOT);
        String attempt=Digests.sha256(remote+"\n"+account);
        var at=now();
        jdbc.update("INSERT IGNORE INTO login_attempt(attempt_key,failures,window_end) VALUES(?,0,?)",attempt,at.plusMinutes(10));
        Map<String,Object> limits=jdbc.queryForMap("SELECT failures,window_end FROM login_attempt WHERE attempt_key=? FOR UPDATE",attempt);
        LocalDateTime end=((java.sql.Timestamp)limits.get("window_end")).toLocalDateTime();
        int failures=((Number)limits.get("failures")).intValue();
        if (!end.isAfter(at)) { failures=0; jdbc.update("UPDATE login_attempt SET failures=0,window_end=? WHERE attempt_key=?",at.plusMinutes(10),attempt); }
        if (failures>=5) throw new DomainException(429,"LOGIN_RATE_LIMIT","尝试次数较多，请稍后再登录。");
        var users=jdbc.query("SELECT * FROM identity_user WHERE username=?",(rs,n)->Map.entry(actor(rs),rs.getString("password_hash")),account);
        boolean matches=encoder.matches(password,users.isEmpty()?dummyHash:users.get(0).getValue());
        boolean active=!users.isEmpty() && Boolean.TRUE.equals(jdbc.queryForObject("SELECT active FROM identity_user WHERE id=?",Boolean.class,users.get(0).getKey().id()));
        if (!matches || !active) {
            jdbc.update("UPDATE login_attempt SET failures=failures+1 WHERE attempt_key=?",attempt);
            throw new DomainException(401,"LOGIN_REJECTED","账号、密码或身份状态不正确。");
        }
        Actor actor=users.get(0).getKey();
        String token=Digests.token(),csrf=Digests.token();
        jdbc.update("UPDATE login_attempt SET failures=0 WHERE attempt_key=?",attempt);
        jdbc.update("INSERT INTO auth_session(token_hash,user_id,auth_version,csrf_token,expires_at) VALUES(?,?,?,?,?)",Digests.sha256(token),actor.id(),actor.authVersion(),csrf,at.plusHours(sessionHours));
        jdbc.update("INSERT INTO audit_entry(actor_id,action,entity_type,entity_id,request_id,detail_json,created_at) VALUES(?,'LOGIN','SESSION',?,?,JSON_OBJECT(),?)",actor.id(),Digests.sha256(token).substring(0,12),requestId,at);
        return new Login(actor,token,csrf);
    }
    public Session authenticate(String token) {
        if (token==null || !token.matches("[a-f0-9]{64}")) throw DomainException.unauthorized();
        String hash=Digests.sha256(token);
        List<Session> sessions=jdbc.query("SELECT u.*,s.csrf_token FROM auth_session s JOIN identity_user u ON u.id=s.user_id WHERE s.token_hash=? AND s.expires_at>? AND u.active=TRUE AND u.auth_version=s.auth_version",(rs,n)->new Session(actor(rs),hash,rs.getString("csrf_token")),hash,now());
        if (sessions.isEmpty()) throw DomainException.unauthorized();
        return sessions.get(0);
    }
    public List<Long> scopes(Actor actor) {
        if (!actor.role().equals("ADMIN")) return List.of();
        return jdbc.query("SELECT floor_id FROM admin_scope WHERE user_id=? ORDER BY floor_id",(rs,n)->rs.getLong(1),actor.id());
    }
    public void requireAdmin(Actor actor,long floor) {
        if (!actor.role().equals("ADMIN") || !scopes(actor).contains(floor)) throw DomainException.forbidden();
    }
    public void logout(Session session) {
        jdbc.update("DELETE FROM auth_session WHERE token_hash=? AND user_id=?",session.tokenHash(),session.actor().id());
    }
}
