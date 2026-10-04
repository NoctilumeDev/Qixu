package dev.noctilume.qixu.operations;

import dev.noctilume.qixu.common.Api;
import dev.noctilume.qixu.identity.SessionFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminProjectionController {
  private final AdminProjection projection;

  public AdminProjectionController(AdminProjection projection) {
    this.projection = projection;
  }

  @GetMapping("/space-blocks")
  Object blocks(
      @RequestParam(required = false) Long floor,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "50") int size,
      HttpServletRequest r) {
    return Api.ok(projection.blocks(SessionFilter.session(r), floor, page, size), r);
  }

  @GetMapping("/operations")
  Object operations(HttpServletRequest r) {
    return Api.ok(projection.operations(SessionFilter.session(r)), r);
  }

  @GetMapping("/audit")
  Object audit(
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "50") int size,
      HttpServletRequest r) {
    return Api.ok(projection.audit(SessionFilter.session(r), page, size), r);
  }

  @GetMapping("/long-entitlements")
  Object entitlements(
      @RequestParam(required = false) Long floor,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "50") int size,
      HttpServletRequest r) {
    return Api.ok(projection.entitlements(SessionFilter.session(r), floor, page, size), r);
  }
}
