package dev.noctilume.qixu.spaces;

import dev.noctilume.qixu.common.Api;
import dev.noctilume.qixu.common.DomainException;
import dev.noctilume.qixu.identity.AuthService;
import dev.noctilume.qixu.identity.SessionFilter;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class SpaceController {
    private final SpaceService spaces;
    private final AuthService auth;
    public SpaceController(SpaceService spaces,AuthService auth) { this.spaces=spaces; this.auth=auth; }
    @GetMapping("/floors") Object floors(HttpServletRequest r) { return Api.ok(spaces.floors(),r); }
    @GetMapping("/spaces") Object list(@RequestParam(required=false) Long floor,@RequestParam(required=false) String kind,@RequestParam(required=false) String search,@RequestParam(required=false) String tag,@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="50") int size,HttpServletRequest r) { return Api.ok(spaces.list(floor,kind,search,tag,page,size),r); }
    @GetMapping("/spaces/{id}") Object get(@PathVariable long id,HttpServletRequest r) { return Api.ok(spaces.get(id),r); }
    @GetMapping("/admin/scope") Object scope(@RequestParam(required=false) Long floor,HttpServletRequest r) {
        var actor=SessionFilter.session(r).actor();
        if (!actor.role().equals("ADMIN")) throw DomainException.forbidden();
        if (floor!=null) auth.requireAdmin(actor,floor);
        return Api.ok(Map.of("floorIds",auth.scopes(actor)),r);
    }
}
