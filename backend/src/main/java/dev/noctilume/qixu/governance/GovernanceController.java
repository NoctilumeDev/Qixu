package dev.noctilume.qixu.governance;

import dev.noctilume.qixu.common.*;
import dev.noctilume.qixu.identity.SessionFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class GovernanceController {
  private final GovernanceCases cases;

  public GovernanceController(GovernanceCases cases) {
    this.cases = cases;
  }

  @GetMapping("/governance-cases")
  Object mine(@RequestParam(defaultValue = "1") int page, HttpServletRequest r) {
    return Api.ok(cases.list(SessionFilter.session(r).actor(), false, page), r);
  }

  @GetMapping("/admin/governance-cases")
  Object administrative(@RequestParam(defaultValue = "1") int page, HttpServletRequest r) {
    return Api.ok(cases.list(SessionFilter.session(r).actor(), true, page), r);
  }

  @GetMapping("/governance-cases/{id}")
  Object get(@PathVariable long id, HttpServletRequest r) {
    return Api.ok(cases.get(SessionFilter.session(r).actor(), id), r);
  }

  @PostMapping("/admin/governance-cases")
  Object create(
      @RequestHeader(value = "Idempotency-Key", required = false) String key,
      @RequestBody GovernanceCases.Create body,
      HttpServletRequest r) {
    return Api.ok(
        cases.create(SessionFilter.session(r), key, body, r.getAttribute("requestId").toString()),
        r);
  }

  @PostMapping("/governance-cases/{id}/statements")
  Object statement(
      @PathVariable long id,
      @RequestHeader(value = "Idempotency-Key", required = false) String key,
      @RequestBody GovernanceCases.Statement body,
      HttpServletRequest r) {
    return Api.ok(
        cases.statement(
            SessionFilter.session(r), id, key, body, r.getAttribute("requestId").toString()),
        r);
  }

  @PostMapping("/admin/governance-cases/{id}/decisions")
  Object decision(
      @PathVariable long id,
      @RequestHeader(value = "Idempotency-Key", required = false) String key,
      @RequestBody GovernanceCases.Decision body,
      HttpServletRequest r) {
    return Api.ok(
        cases.decide(
            SessionFilter.session(r), id, key, body, r.getAttribute("requestId").toString()),
        r);
  }

  @PostMapping("/governance-cases/{id}/appeals")
  Object appeal(
      @PathVariable long id,
      @RequestHeader(value = "Idempotency-Key", required = false) String key,
      @RequestBody GovernanceCases.Statement body,
      HttpServletRequest r) {
    return Api.ok(
        cases.appeal(
            SessionFilter.session(r), id, key, body, r.getAttribute("requestId").toString()),
        r);
  }

  @PostMapping("/admin/governance-cases/{id}/reviews")
  Object review(
      @PathVariable long id,
      @RequestHeader(value = "Idempotency-Key", required = false) String key,
      @RequestBody GovernanceCases.Review body,
      HttpServletRequest r) {
    return Api.ok(
        cases.review(
            SessionFilter.session(r), id, key, body, r.getAttribute("requestId").toString()),
        r);
  }
}
