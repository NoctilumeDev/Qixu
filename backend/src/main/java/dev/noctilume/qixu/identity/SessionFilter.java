package dev.noctilume.qixu.identity;

import dev.noctilume.qixu.common.Api;
import dev.noctilume.qixu.common.Digests;
import dev.noctilume.qixu.common.DomainException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.json.JsonMapper;

@Component
public class SessionFilter extends OncePerRequestFilter {
    private final AuthService auth;
    private final JsonMapper json;
    private final Set<String> origins;
    private final ApplicationAvailability availability;
    public SessionFilter(AuthService auth,JsonMapper json,ApplicationAvailability availability,@Value("${qixu.allowed-origins}") String origins) {
        this.auth=auth; this.json=json; this.availability=availability; this.origins=Arrays.stream(origins.split(",")).map(String::trim).collect(Collectors.toUnmodifiableSet());
    }
    public static AuthService.Session session(HttpServletRequest r) {
        var value=r.getAttribute("qixu.session");
        if (!(value instanceof AuthService.Session session)) throw DomainException.unauthorized();
        return session;
    }
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws ServletException,IOException {
        request.setAttribute("requestId",UUID.randomUUID().toString());
        response.setHeader("X-Content-Type-Options","nosniff");
        response.setHeader("Cache-Control","no-store");
        String origin=request.getHeader("Origin");
        try {
            if (origin!=null) {
                if (!origins.contains(origin)) throw new DomainException(403,"ORIGIN_FORBIDDEN","请求来源未获允许。");
                response.setHeader("Access-Control-Allow-Origin",origin);
                response.setHeader("Access-Control-Allow-Credentials","true");
                response.setHeader("Vary","Origin");
                if (request.getMethod().equals("OPTIONS")) {
                    response.setHeader("Access-Control-Allow-Methods","GET,POST,PUT,DELETE,OPTIONS");
                    response.setHeader("Access-Control-Allow-Headers","Content-Type,Authorization,X-CSRF-Token,Idempotency-Key");
                    response.setStatus(204); return;
                }
            }
            String path=request.getRequestURI();
            if (path.startsWith("/api/") && availability.getReadinessState()!=ReadinessState.ACCEPTING_TRAFFIC)
                throw new DomainException(503,"APPLICATION_NOT_READY","服务正在准备，请稍后重试。");
            boolean anonymous=(path.equals("/api/health") && request.getMethod().equals("GET"))
                || (path.equals("/api/v1/auth/login") && request.getMethod().equals("POST"))
                || (path.equals("/api/v1/auth/options") && request.getMethod().equals("GET"));
            if (path.startsWith("/api/") && !anonymous) {
                String authorization=request.getHeader("Authorization");
                boolean bearer=authorization!=null && authorization.startsWith("Bearer ");
                if (authorization!=null && !bearer) throw DomainException.unauthorized();
                String token=bearer?authorization.substring(7):null;
                if (!bearer && request.getCookies()!=null) for (Cookie cookie:request.getCookies()) if (cookie.getName().equals("qixu_session")) token=cookie.getValue();
                var session=auth.authenticate(token);
                if (!bearer && !Set.of("GET","HEAD","OPTIONS").contains(request.getMethod()) && !Digests.equal(session.csrf(),request.getHeader("X-CSRF-Token"))) {
                    throw new DomainException(403,"CSRF_REQUIRED","页面会话已变化，请刷新后重试。");
                }
                request.setAttribute("qixu.session",session);
            }
            chain.doFilter(request,response);
        } catch (DomainException e) {
            response.setStatus(e.status()); response.setContentType("application/json;charset=UTF-8");
            json.writeValue(response.getOutputStream(),Api.error(e.code(),e.getMessage(),request));
        } catch (DataAccessException e) {
            response.setStatus(503); response.setContentType("application/json;charset=UTF-8");
            json.writeValue(response.getOutputStream(),Api.error("DATABASE_UNAVAILABLE","数据服务暂时不可用，请稍后重试。",request));
        }
    }
}
