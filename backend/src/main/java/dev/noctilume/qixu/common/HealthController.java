package dev.noctilume.qixu.common;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {
  private final JdbcTemplate jdbc;

  public HealthController(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @GetMapping("/api/health")
  Object health(HttpServletRequest request) {
    jdbc.queryForObject("SELECT 1", Integer.class);
    return Api.ok(
        Map.of("application", "qixu", "status", "READY", "boundary", "M1_FOUNDATION"), request);
  }
}
