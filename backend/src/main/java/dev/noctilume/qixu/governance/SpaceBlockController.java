package dev.noctilume.qixu.governance;

import dev.noctilume.qixu.common.*;
import dev.noctilume.qixu.identity.*;
import dev.noctilume.qixu.spaces.SpaceRights;
import jakarta.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class SpaceBlockController {
    private final SpaceBlocks service;private final SpaceRights rights;
    public SpaceBlockController(SpaceBlocks service,SpaceRights rights) {this.service=service;this.rights=rights;}
    @PostMapping("/admin/space-blocks/preview") Object preview(@RequestBody SpaceBlocks.Plan body,HttpServletRequest r) {return Api.ok(service.preview(SessionFilter.session(r).actor(),body),r);}
    @PostMapping("/admin/space-blocks") Object create(@RequestHeader(value="Idempotency-Key",required=false) String key,@RequestBody SpaceBlocks.Plan body,HttpServletRequest r) {return Api.ok(service.submit(SessionFilter.session(r),key,body,r.getAttribute("requestId").toString()),r);}
    @GetMapping("/admin/space-blocks/{id}") Object get(@PathVariable long id,HttpServletRequest r) {return Api.ok(service.get(SessionFilter.session(r).actor(),id),r);}
    @PostMapping("/admin/space-blocks/{id}/revoke") Object revoke(@PathVariable long id,@RequestHeader(value="Idempotency-Key",required=false)String key,@RequestBody SpaceBlocks.Revoke body,HttpServletRequest r) {return Api.ok(service.revoke(SessionFilter.session(r),id,key,body,r.getAttribute("requestId").toString()),r);}
    @GetMapping("/spaces/{id}/limits") Object limits(@PathVariable long id,@RequestParam OffsetDateTime startsAt,@RequestParam OffsetDateTime endsAt,HttpServletRequest r) {return Api.ok(service.publicLimits(id,Business.time(startsAt),Business.time(endsAt)),r);}
    @GetMapping("/long-offers/{id}/impact") Object impact(@PathVariable long id,HttpServletRequest r) {return Api.ok(rights.offerImpact(id,SessionFilter.session(r).actor().id()),r);}
}
