package dev.noctilume.qixu.booking;

import dev.noctilume.qixu.common.*;
import dev.noctilume.qixu.events.Events;
import dev.noctilume.qixu.identity.*;
import dev.noctilume.qixu.notifications.Notifications;
import dev.noctilume.qixu.spaces.SpaceRights;
import jakarta.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class BookingController {
    private final Business b; private final ShortReservations shorts; private final VenueRequests venues; private final Events events; private final SpaceRights rights; private final Favorites favorites; private final Notifications notifications;
    public BookingController(Business b,ShortReservations shorts,VenueRequests venues,Events events,SpaceRights rights,Favorites favorites,Notifications notifications) { this.b=b; this.shorts=shorts; this.venues=venues; this.events=events; this.rights=rights; this.favorites=favorites; this.notifications=notifications; }
    private AuthService.Session session(HttpServletRequest r) { return SessionFilter.session(r); }
    private String requestId(HttpServletRequest r) { return r.getAttribute("requestId").toString(); }
    @GetMapping("/spaces/{id}/availability") Object availability(@PathVariable long id,@RequestParam OffsetDateTime start,@RequestParam OffsetDateTime end,HttpServletRequest r) { return Api.ok(rights.availability(id,Business.time(start),Business.time(end)),r); }
    @GetMapping("/booking-rules") Object rules(HttpServletRequest r) { return Api.ok(Map.of("zone","Asia/Shanghai","opens","08:00","closes","22:00","shortHorizonHours",4,"maxShortHours",4,"checkInGraceMinutes",10,"serverNow",Business.iso(b.now())),r); }
    @GetMapping("/reservations") Object mine(HttpServletRequest r) { return Api.ok(shorts.mine(session(r).actor().id()),r); }
    @GetMapping("/reservations/{id}") Object shortDetail(@PathVariable long id,HttpServletRequest r) { return Api.ok(shorts.detail(session(r).actor().id(),id),r); }
    @PostMapping("/reservations") Object create(@RequestHeader(value="Idempotency-Key",required=false) String key,@RequestBody ShortReservations.Create body,HttpServletRequest r) { return Api.ok(shorts.create(session(r),key,body,requestId(r)),r); }
    @PostMapping("/reservations/{id}/actions") Object shortChange(@PathVariable long id,@RequestHeader(value="Idempotency-Key",required=false) String key,@RequestBody ShortReservations.Change body,HttpServletRequest r) { return Api.ok(shorts.change(session(r),id,key,body,requestId(r)),r); }
    @GetMapping("/venue-requests") Object requests(HttpServletRequest r) { return Api.ok(venues.mine(session(r).actor()),r); }
    @GetMapping("/venue-requests/{id}") Object venue(@PathVariable long id,HttpServletRequest r) { return Api.ok(venues.detail(session(r).actor(),id),r); }
    @PostMapping("/venue-requests") Object venueCreate(@RequestHeader(value="Idempotency-Key",required=false) String key,@RequestBody VenueRequests.Create body,HttpServletRequest r) { return Api.ok(venues.create(session(r),key,body,requestId(r)),r); }
    @PostMapping("/venue-requests/{id}/actions") Object venueDecision(@PathVariable long id,@RequestHeader(value="Idempotency-Key",required=false) String key,@RequestBody VenueRequests.Decision body,HttpServletRequest r) { return Api.ok(venues.decide(session(r),id,key,body,requestId(r)),r); }
    @GetMapping("/admin/venue-requests") Object pending(HttpServletRequest r) { return Api.ok(venues.pending(session(r).actor()),r); }
    @GetMapping("/events") Object events(HttpServletRequest r) { return Api.ok(events.list(session(r).actor(),false),r); }
    @GetMapping("/events/{id}") Object event(@PathVariable long id,HttpServletRequest r) { return Api.ok(events.detail(session(r).actor(),id),r); }
    @GetMapping("/organizer/events") Object organizerEvents(HttpServletRequest r) { return Api.ok(events.list(session(r).actor(),true),r); }
    @PostMapping("/organizer/events") Object eventCreate(@RequestHeader(value="Idempotency-Key",required=false) String key,@RequestBody Events.Create body,HttpServletRequest r) { return Api.ok(events.create(session(r),key,body,requestId(r)),r); }
    @PostMapping("/organizer/events/{id}/actions") Object eventChange(@PathVariable long id,@RequestHeader(value="Idempotency-Key",required=false) String key,@RequestBody Events.Change body,HttpServletRequest r) { return Api.ok(events.change(session(r),id,key,body,requestId(r)),r); }
    @PostMapping("/events/{id}/participation") Object participate(@PathVariable long id,@RequestHeader(value="Idempotency-Key",required=false) String key,@RequestBody Events.Participate body,HttpServletRequest r) { return Api.ok(events.participate(session(r),id,key,body,requestId(r)),r); }
    @GetMapping("/participations") Object participations(HttpServletRequest r) { return Api.ok(events.participations(session(r).actor()),r); }
    @GetMapping("/favorites") Object favorites(HttpServletRequest r) { return Api.ok(favorites.mine(session(r).actor().id()),r); }
    @PostMapping("/favorites") Object favoriteChange(@RequestHeader(value="Idempotency-Key",required=false) String key,@RequestBody Favorites.Change body,HttpServletRequest r) { return Api.ok(favorites.change(session(r),key,body),r); }
    @GetMapping("/receipts/{key}") Object receipt(@PathVariable String key,HttpServletRequest r) { return Api.ok(b.receipt(session(r).actor().id(),key),r); }
    @GetMapping("/inbox") Object inbox(HttpServletRequest r) { return Api.ok(notifications.mine(session(r).actor().id()),r); }
    @PostMapping("/inbox/{id}/read") Object read(@PathVariable long id,@RequestHeader(value="Idempotency-Key",required=false) String key,HttpServletRequest r) { return Api.ok(notifications.read(session(r),key,id),r); }
}
