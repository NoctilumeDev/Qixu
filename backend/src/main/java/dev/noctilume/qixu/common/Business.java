package dev.noctilume.qixu.common;

import dev.noctilume.qixu.identity.Actor;
import dev.noctilume.qixu.identity.AuthService;
import java.time.*;
import java.util.*;
import java.util.function.Supplier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/** Shared mechanics, not a shared domain state machine. Call inside an RC transaction. */
@Component
public class Business {
    public final JdbcTemplate jdbc;
    private final Clock clock;
    private final JsonMapper json;
    public Business(JdbcTemplate jdbc,Clock clock,JsonMapper json) { this.jdbc=jdbc; this.clock=clock; this.json=json; }
    public LocalDateTime now() { return LocalDateTime.ofInstant(clock.instant(),ZoneOffset.UTC); }
    public static String iso(LocalDateTime time) { return time.atOffset(ZoneOffset.UTC).toString(); }
    public static LocalDateTime time(OffsetDateTime time) {
        if (time==null || time.getNano()!=0) throw DomainException.invalid("时间须含时区并精确到秒。");
        return LocalDateTime.ofInstant(time.toInstant(),ZoneOffset.UTC);
    }
    public static void version(long current,long expected) { if(current!=expected) throw conflict("STALE_VERSION","内容已经变化，请刷新后再操作。"); }
    public static DomainException conflict(String code,String message) { return new DomainException(409,code,message); }
    public static void text(String value,int max,boolean required) {
        if(value==null || value.length()>max || (required && value.isBlank())) throw DomainException.invalid("填写内容为空或超出长度限制。");
    }
    public static void student(Actor actor) { if(!actor.studentVerified()) throw DomainException.forbidden(); }
    public static void organizer(Actor actor) { if(!Set.of("TEACHER","ADMIN").contains(actor.role())) throw DomainException.forbidden(); }
    public void floors(Collection<Long> floors) {
        for(long id:new TreeSet<>(floors)) if(jdbc.query("SELECT id FROM floor WHERE id=? FOR UPDATE",(rs,n)->rs.getLong(1),id).isEmpty()) throw DomainException.missing();
    }
    public void users(Collection<Long> users) {
        for(long id:new TreeSet<>(users)) if(jdbc.query("SELECT id FROM identity_user WHERE id=? FOR UPDATE",(rs,n)->rs.getLong(1),id).isEmpty()) throw DomainException.missing();
    }
    public Actor current(AuthService.Session session) {
        var result=jdbc.query("SELECT u.* FROM auth_session s JOIN identity_user u ON u.id=s.user_id WHERE s.token_hash=? AND u.id=? AND s.expires_at>? AND s.auth_version=u.auth_version AND u.active=TRUE",(rs,n)->new Actor(rs.getLong("id"),rs.getString("username"),rs.getString("display_name"),rs.getString("role"),rs.getBoolean("student_verified"),rs.getLong("auth_version")),session.tokenHash(),session.actor().id(),now());
        if(result.isEmpty()) throw DomainException.unauthorized();
        return result.get(0);
    }
    public void admin(Actor actor,long floor) {
        if(!actor.role().equals("ADMIN") || jdbc.queryForObject("SELECT COUNT(*) FROM admin_scope WHERE user_id=? AND floor_id=?",Integer.class,actor.id(),floor)!=1) throw DomainException.forbidden();
    }
    public long insert(String sql,Object... values) {
        var holder=new GeneratedKeyHolder();
        jdbc.update(connection->{var p=connection.prepareStatement(sql,java.sql.Statement.RETURN_GENERATED_KEYS); for(int i=0;i<values.length;i++) p.setObject(i+1,values[i]); return p;},holder);
        return Objects.requireNonNull(holder.getKey()).longValue();
    }
    public Map<String,Object> row(String table,long id) {
        if(!Set.of("short_reservation","venue_request","campus_event","event_participation").contains(table)) throw new IllegalArgumentException();
        var values=jdbc.queryForList("SELECT * FROM "+table+" WHERE id=?",id);
        if(values.isEmpty()) throw DomainException.missing();
        return values.get(0);
    }
    public Map<String,Object> view(Map<String,Object> row) {
        var result=new LinkedHashMap<String,Object>();
        row.forEach((k,v)->result.put(k,v instanceof LocalDateTime t?iso(t):v instanceof java.sql.Timestamp t?iso(t.toLocalDateTime()):v));
        return result;
    }
    public static long number(Map<String,Object> row,String key) { return ((Number)row.get(key)).longValue(); }
    public static LocalDateTime date(Map<String,Object> row,String key) { var v=row.get(key); return v instanceof LocalDateTime t?t:((java.sql.Timestamp)v).toLocalDateTime(); }
    public Map<String,Object> once(Actor actor,String key,String operation,Object body,Supplier<Map<String,Object>> effect) {
        if(key==null || !key.matches("[A-Za-z0-9_-]{8,80}")) throw DomainException.invalid("请提供有效的请求标识。");
        String digest=Digests.sha256(json.writeValueAsString(body));
        var receipts=jdbc.queryForList("SELECT operation,body_hash,response_json FROM idempotency_receipt WHERE actor_id=? AND request_key=?",actor.id(),key);
        if(!receipts.isEmpty()) {
            var r=receipts.get(0);
            if(!r.get("operation").equals(operation) || !r.get("body_hash").equals(digest)) throw conflict("IDEMPOTENCY_CONFLICT","同一请求标识不能用于不同内容。");
            return json.readValue(r.get("response_json").toString(),Map.class);
        }
        var value=effect.get();
        jdbc.update("INSERT INTO idempotency_receipt(actor_id,request_key,operation,body_hash,response_json,created_at) VALUES(?,?,?,?,?,?)",actor.id(),key,operation,digest,json.writeValueAsString(value),now());
        return value;
    }
    public Object receipt(long actor,String key) {
        var rows=jdbc.queryForList("SELECT operation,response_json,created_at FROM idempotency_receipt WHERE actor_id=? AND request_key=?",actor,key);
        if(rows.isEmpty()) throw DomainException.missing();
        return Map.of("operation",rows.get(0).get("operation"),"result",json.readValue(rows.get(0).get("response_json").toString(),Map.class),"status","COMMITTED");
    }
    public void audit(long actor,String action,String entity,long id,String requestId) {
        jdbc.update("INSERT INTO audit_entry(actor_id,action,entity_type,entity_id,request_id,detail_json,created_at) VALUES(?,?,?,?,?,JSON_OBJECT(),?)",actor,action,entity,Long.toString(id),requestId,now());
    }
    public void notify(long user,String key,String title,String message,String entity,long id) {
        jdbc.update("INSERT IGNORE INTO notification_outbox(recipient_id,event_key,title,message,entity_type,entity_id,created_at) VALUES(?,?,?,?,?,?,?)",user,key,title,message,entity,id,now());
    }
    public void window(LocalDateTime start,LocalDateTime end,boolean shortBooking) {
        var current=now(); var zone=ZoneId.of("Asia/Shanghai");
        var a=start.atOffset(ZoneOffset.UTC).atZoneSameInstant(zone); var z=end.atOffset(ZoneOffset.UTC).atZoneSameInstant(zone);
        if(!start.isBefore(end) || start.isBefore(current) || a.getHour()<8 || z.toLocalTime().isAfter(LocalTime.of(22,0)) || !a.toLocalDate().equals(z.toLocalDate())) throw DomainException.invalid("时间须在同一天的08:00–22:00开放时段内，且尚未开始。");
        if(shortBooking && (Duration.between(start,end).compareTo(Duration.ofHours(4))>0 || start.isAfter(current.plusHours(4)) || !a.toLocalDate().equals(current.atOffset(ZoneOffset.UTC).atZoneSameInstant(zone).toLocalDate()))) throw DomainException.invalid("短约仅开放当天未来4小时，最长4小时。");
        if(!shortBooking && start.isAfter(current.plusDays(30))) throw DomainException.invalid("场地只接受未来30天申请。");
    }
}
