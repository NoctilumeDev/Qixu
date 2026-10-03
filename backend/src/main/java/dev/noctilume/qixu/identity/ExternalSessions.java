package dev.noctilume.qixu.identity;

import dev.noctilume.qixu.common.Digests;
import dev.noctilume.qixu.common.DomainException;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class ExternalSessions {
    private final JdbcTemplate jdbc;private final Clock clock;private final DarkRoomGateway gateway;private final ExternalTicketVault vault;
    private final TransactionTemplate tx;private final int hours;
    public ExternalSessions(JdbcTemplate jdbc,Clock clock,DarkRoomGateway gateway,ExternalTicketVault vault,PlatformTransactionManager manager,@Value("${qixu.session-hours:12}")int hours){
        this.jdbc=jdbc;this.clock=clock;this.gateway=gateway;this.vault=vault;this.hours=hours;tx=new TransactionTemplate(manager);tx.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
    }
    private static long number(Map<String,Object> r,String key){return ((Number)r.get(key)).longValue();}
    private LocalDateTime now(){return LocalDateTime.ofInstant(clock.instant(),ZoneOffset.UTC);}
    public AuthService.Login exchange(String ticket,String remote,String requestId){
        DarkRoomGateway.ticketFormat(ticket);
        if(!gateway.enabled())throw DarkRoomGateway.unavailable();
        // Admission is a separate short transaction; network budget is never spent under DB locks.
        tx.executeWithoutResult(status->{String key=Digests.sha256("qixu-external-admission/1\n"+remote);var at=now();
            jdbc.update("INSERT IGNORE INTO login_attempt(attempt_key,failures,window_end) VALUES(?,0,?)",key,at.plusMinutes(1));
            var row=jdbc.queryForMap("SELECT failures,window_end FROM login_attempt WHERE attempt_key=? FOR UPDATE",key);
            LocalDateTime end=row.get("window_end") instanceof LocalDateTime t?t:((java.sql.Timestamp)row.get("window_end")).toLocalDateTime();
            if(!end.isAfter(at))jdbc.update("UPDATE login_attempt SET failures=0,window_end=? WHERE attempt_key=?",at.plusMinutes(1),key);
            else if(number(row,"failures")>=20)throw new DomainException(429,"LOGIN_RATE_LIMIT","尝试次数较多，请稍后再登录。");
            jdbc.update("UPDATE login_attempt SET failures=failures+1 WHERE attempt_key=?",key);
        });
        var expires=clock.instant().plusSeconds(3);long monotonic=System.nanoTime()+Duration.ofSeconds(3).toNanos();
        String subject=gateway.verify(ticket),issuer=gateway.issuerHash();
        var bindings=jdbc.queryForList("SELECT * FROM external_identity WHERE provider=? AND subject=? AND issuer_hash=? AND active=TRUE",DarkRoomGateway.PROVIDER,subject,issuer);
        if(bindings.isEmpty())throw new DomainException(403,"EXTERNAL_BINDING_REQUIRED","该身份尚未登记期序使用权限，请联系管理人员。");
        var binding=bindings.get(0);long id=number(binding,"id"),user=number(binding,"user_id"),version=number(binding,"version");
        return tx.execute(status->{
            // User is also the business commit coordinator. Do not acquire an external row before it.
            var users=jdbc.query("SELECT * FROM identity_user WHERE id=? AND active=TRUE AND local_login_enabled=FALSE FOR UPDATE",(rs,n)->AuthService.actor(rs),user);
            if(users.isEmpty())throw DomainException.unauthorized();
            var current=jdbc.queryForList("SELECT id FROM external_identity WHERE id=? AND user_id=? AND version=? AND issuer_hash=? AND subject=? AND provider=? AND active=TRUE FOR UPDATE",id,user,version,issuer,subject,DarkRoomGateway.PROVIDER);
            if(current.isEmpty())throw DomainException.unauthorized();
            if(!clock.instant().isBefore(expires) || System.nanoTime()-monotonic>=0)throw DarkRoomGateway.unavailable();
            var actor=users.get(0);String token=Digests.token(),hash=Digests.sha256(token),csrf=Digests.token();
            String encrypted=vault.seal(ticket,ExternalTicketVault.aad(hash,user,id,version,issuer,subject));
            jdbc.update("INSERT INTO auth_session(token_hash,user_id,auth_version,csrf_token,expires_at,external_binding_id,external_binding_version,external_issuer_hash,external_subject,external_ticket_cipher) VALUES(?,?,?,?,?,?,?,?,?,?)",hash,user,actor.authVersion(),csrf,now().plusHours(hours),id,version,issuer,subject,encrypted);
            jdbc.update("INSERT INTO audit_entry(actor_id,action,entity_type,entity_id,request_id,detail_json,created_at) VALUES(?,'EXTERNAL_LOGIN','SESSION',?,?,JSON_OBJECT('provider',?),?)",user,hash.substring(0,12),requestId,DarkRoomGateway.PROVIDER,now());
            return new AuthService.Login(actor,token,csrf);
        });
    }
    public AuthService.Session verify(AuthService.Session session,boolean logout){
        var rows=jdbc.queryForList("SELECT s.*,e.active AS binding_active,e.version AS binding_version,e.issuer_hash AS binding_issuer,e.subject AS binding_subject,e.user_id AS binding_user,e.provider,u.local_login_enabled FROM auth_session s LEFT JOIN external_identity e ON e.id=s.external_binding_id JOIN identity_user u ON u.id=s.user_id WHERE s.token_hash=?",session.tokenHash());
        if(rows.isEmpty())throw DomainException.unauthorized();var row=rows.get(0);
        if(row.get("external_binding_id")==null){var actor=SessionValidity.current(jdbc,clock,session);return new AuthService.Session(actor,session.tokenHash(),session.csrf());}
        if(logout)return session;
        String issuer=gateway.issuerHash(),subject=row.get("external_subject").toString();
        if(!Boolean.TRUE.equals(row.get("binding_active")) || Boolean.TRUE.equals(row.get("local_login_enabled")) || !DarkRoomGateway.PROVIDER.equals(row.get("provider"))
           || number(row,"binding_user")!=session.actor().id() || number(row,"binding_version")!=number(row,"external_binding_version")
           || !issuer.equals(row.get("binding_issuer")) || !issuer.equals(row.get("external_issuer_hash")) || !subject.equals(row.get("binding_subject")))throw DomainException.unauthorized();
        String ticket=vault.open(row.get("external_ticket_cipher").toString(),ExternalTicketVault.aad(session.tokenHash(),session.actor().id(),number(row,"external_binding_id"),number(row,"external_binding_version"),issuer,subject));
        var expires=clock.instant().plusSeconds(3);long deadline=System.nanoTime()+Duration.ofSeconds(3).toNanos();String verified;
        try{verified=gateway.verify(ticket);}catch(DomainException e){
            if(e.status()==401)tx.executeWithoutResult(status->{jdbc.queryForList("SELECT id FROM identity_user WHERE id=? FOR UPDATE",session.actor().id());jdbc.update("DELETE FROM auth_session WHERE token_hash=? AND user_id=?",session.tokenHash(),session.actor().id());});
            throw e;
        }
        if(!verified.equals(subject))throw DomainException.unauthorized();
        var proof=new AuthService.ExternalProof(number(row,"external_binding_id"),number(row,"external_binding_version"),issuer,subject,expires,deadline);
        var value=new AuthService.Session(session.actor(),session.tokenHash(),session.csrf(),proof);
        var current=SessionValidity.current(jdbc,clock,value);
        return new AuthService.Session(current,value.tokenHash(),value.csrf(),proof);
    }
}
