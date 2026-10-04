package dev.noctilume.qixu.identity;

import dev.noctilume.qixu.common.DomainException;
import java.time.*;
import org.springframework.jdbc.core.JdbcTemplate;

/** Current DB facts and request-specific external proof, including after coordination waits. */
public final class SessionValidity {
  private SessionValidity() {}

  public static Actor current(JdbcTemplate jdbc, Clock clock, AuthService.Session session) {
    var rows =
        jdbc.query(
            "SELECT u.*,s.external_binding_id,s.external_binding_version,s.external_issuer_hash,s.external_subject FROM auth_session s JOIN identity_user u ON u.id=s.user_id LEFT JOIN external_identity e ON e.id=s.external_binding_id "
                + "WHERE s.token_hash=? AND u.id=? AND s.expires_at>? AND s.auth_version=u.auth_version AND u.active=TRUE "
                + "AND (s.external_binding_id IS NULL OR (e.active=TRUE AND e.provider=? AND e.user_id=u.id AND e.version=s.external_binding_version AND e.issuer_hash=s.external_issuer_hash AND e.subject=s.external_subject AND u.local_login_enabled=FALSE))",
            (rs, n) -> {
              if (rs.getObject("external_binding_id") != null) {
                var p = session.externalProof();
                if (p == null
                    || p.binding() != rs.getLong("external_binding_id")
                    || p.version() != rs.getLong("external_binding_version")
                    || !p.issuer().equals(rs.getString("external_issuer_hash"))
                    || !p.subject().equals(rs.getString("external_subject")))
                  throw DomainException.unauthorized();
                if (!clock.instant().isBefore(p.expiresAt())
                    || System.nanoTime() - p.deadlineNanos() >= 0)
                  throw DarkRoomGateway.unavailable();
              } else if (session.externalProof() != null) throw DomainException.unauthorized();
              return AuthService.actor(rs);
            },
            session.tokenHash(),
            session.actor().id(),
            LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC),
            DarkRoomGateway.PROVIDER);
    if (rows.isEmpty()) throw DomainException.unauthorized();
    return rows.get(0);
  }
}
