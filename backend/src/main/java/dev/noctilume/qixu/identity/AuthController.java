package dev.noctilume.qixu.identity;

import dev.noctilume.qixu.common.Api;
import dev.noctilume.qixu.common.DomainException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService auth;
    private final ExternalSessions external;
    private final JdbcTemplate jdbc;
    private final boolean demo;
    private final boolean secure;
    private final int sessionHours;
    public record LoginRequest(@NotBlank @Size(max=64) String username,@NotBlank @Size(max=128) String password,String mode) {}
    public record ExternalLoginRequest(@NotBlank @Size(max=4096) String ticket,String mode) {}
    public AuthController(AuthService auth,ExternalSessions external,JdbcTemplate jdbc,@Value("${qixu.demo-enabled:false}") boolean demo,@Value("${qixu.cookie-secure:true}") boolean secure,@Value("${qixu.session-hours:12}") int sessionHours) {
        this.auth=auth; this.external=external; this.jdbc=jdbc; this.demo=demo; this.secure=secure; this.sessionHours=sessionHours;
    }
    @GetMapping("/options") Object options(HttpServletRequest r) {
        if (!demo) throw DomainException.missing();
        return Api.ok(jdbc.query("SELECT username,display_name,role FROM identity_user WHERE active=TRUE AND local_login_enabled=TRUE ORDER BY id",(rs,n)->Map.of("username",rs.getString(1),"displayName",rs.getString(2),"role",rs.getString(3))),r);
    }
    @PostMapping("/login") Object login(@Valid @RequestBody LoginRequest input,HttpServletRequest r,HttpServletResponse response) {
        String mode=input.mode()==null?"COOKIE":input.mode();
        if (!mode.equals("COOKIE") && !mode.equals("BEARER")) throw DomainException.invalid("登录方式不正确。");
        var login=auth.login(input.username(),input.password(),r.getRemoteAddr(),r.getAttribute("requestId").toString());
        return loginResponse(login,mode,r,response);
    }
    @PostMapping("/external/dark-room") Object externalLogin(@Valid @RequestBody ExternalLoginRequest input,HttpServletRequest r,HttpServletResponse response){
        String mode=input.mode()==null?"COOKIE":input.mode();
        if(!mode.equals("COOKIE") && !mode.equals("BEARER"))throw DomainException.invalid("登录方式不正确。");
        return loginResponse(external.exchange(input.ticket(),r.getRemoteAddr(),r.getAttribute("requestId").toString()),mode,r,response);
    }
    private Object loginResponse(AuthService.Login login,String mode,HttpServletRequest r,HttpServletResponse response){
        var data=new LinkedHashMap<String,Object>(); data.put("actor",login.actor()); data.put("csrfToken",login.csrf());
        if (mode.equals("BEARER")) data.put("token",login.token());
        else response.addHeader(HttpHeaders.SET_COOKIE,ResponseCookie.from("qixu_session",login.token()).httpOnly(true).secure(secure).sameSite("Lax").path("/api").maxAge(Duration.ofHours(sessionHours)).build().toString());
        return Api.ok(data,r);
    }
    @GetMapping("/session") Object current(HttpServletRequest r) {
        var session=SessionFilter.session(r);
        return Api.ok(Map.of("actor",session.actor(),"csrfToken",session.csrf(),"adminFloors",auth.scopes(session.actor())),r);
    }
    @PostMapping("/logout") Object logout(HttpServletRequest r,HttpServletResponse response) {
        auth.logout(SessionFilter.session(r));
        if("COOKIE".equals(r.getAttribute("qixu.authenticationMode")))
            response.addHeader(HttpHeaders.SET_COOKIE,ResponseCookie.from("qixu_session","").httpOnly(true).secure(secure).sameSite("Lax").path("/api").maxAge(Duration.ZERO).build().toString());
        return Api.ok(Map.of("loggedOut",true),r);
    }
}
