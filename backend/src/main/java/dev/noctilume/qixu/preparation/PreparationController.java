package dev.noctilume.qixu.preparation;

import dev.noctilume.qixu.common.*;
import dev.noctilume.qixu.identity.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

@RestController
public class PreparationController {
    private final PreparationBatches batches;private final BatchFreezer freezer;private final AllocationWorker worker;private final LongSeats seats;private final BatchTermination termination;
    public PreparationController(PreparationBatches batches,BatchFreezer freezer,AllocationWorker worker,LongSeats seats,BatchTermination termination) {this.batches=batches;this.freezer=freezer;this.worker=worker;this.seats=seats;this.termination=termination;}
    @GetMapping("/api/v1/preparation-batches") Object list(HttpServletRequest r) {return Api.ok(batches.list(),r);}
    @GetMapping("/api/v1/preparation-batches/{id}") Object detail(@PathVariable long id,HttpServletRequest r) {return Api.ok(batches.detail(id),r);}
    @PostMapping("/api/v1/preparation-batches") Object create(@RequestHeader(value="Idempotency-Key",required=false)String key,@RequestBody PreparationBatches.Create body,HttpServletRequest r) {return Api.ok(batches.create(SessionFilter.session(r),key,body,r.getAttribute("requestId").toString()),r);}
    @GetMapping("/api/v1/preparation-batches/{id}/application") Object application(@PathVariable long id,HttpServletRequest r) {return Api.ok(seats.mine(SessionFilter.session(r).actor().id(),id),r);}
    @GetMapping("/api/v1/preparation-applications") Object myApplications(@RequestParam(defaultValue="1")int page,HttpServletRequest r) {return Api.ok(seats.minePage(SessionFilter.session(r).actor().id(),page),r);}
    @PostMapping("/api/v1/preparation-batches/{id}/application") Object submit(@PathVariable long id,@RequestHeader(value="Idempotency-Key",required=false)String key,@RequestBody PreparationBatches.Submit body,HttpServletRequest r) {return Api.ok(batches.submit(SessionFilter.session(r),id,key,body,r.getAttribute("requestId").toString()),r);}
    @PostMapping("/api/v1/preparation-batches/{id}/withdraw") Object withdraw(@PathVariable long id,@RequestHeader(value="Idempotency-Key",required=false)String key,@RequestBody PreparationBatches.Action body,HttpServletRequest r) {return Api.ok(batches.withdrawApplication(SessionFilter.session(r),id,key,body),r);}
    @PostMapping("/api/v1/preparation-batches/{id}/actions") Object action(@PathVariable long id,@RequestHeader(value="Idempotency-Key",required=false)String key,@RequestBody PreparationBatches.Action body,HttpServletRequest r) {
        return Api.ok(switch(body.action()==null?"":body.action()) {
            case "FREEZE" -> freezer.freeze(SessionFilter.session(r),id,key,body);
            case "ALLOCATE" -> worker.allocate(SessionFilter.session(r),id,key,body);
            default -> throw DomainException.invalid("请选择冻结或分配，不提供指定中签者/种子接口。");
        },r);
    }
    @PostMapping("/api/v1/long-offers/{id}/actions") Object offer(@PathVariable long id,@RequestHeader(value="Idempotency-Key",required=false)String key,@RequestBody LongSeats.Response body,HttpServletRequest r) {return Api.ok(seats.respond(SessionFilter.session(r),id,key,body),r);}
    @PostMapping("/api/v1/preparation-batches/{id}/exit") Object exit(@PathVariable long id,@RequestHeader(value="Idempotency-Key",required=false)String key,@RequestBody PreparationBatches.Action body,HttpServletRequest r) {return Api.ok(seats.exit(SessionFilter.session(r),id,key,body),r);}
    @PostMapping("/api/v1/preparation-batches/{id}/cancel") Object cancel(@PathVariable long id,@RequestHeader(value="Idempotency-Key",required=false)String key,@RequestBody BatchTermination.Cancel body,HttpServletRequest r) {return Api.ok(termination.cancel(SessionFilter.session(r),id,key,body,r.getAttribute("requestId").toString()),r);}
    @PostMapping("/api/v1/preparation-batches/{id}/waitlist-exit") Object waitlistExit(@PathVariable long id,@RequestHeader(value="Idempotency-Key",required=false)String key,@RequestBody PreparationBatches.Action body,HttpServletRequest r) {return Api.ok(seats.exitWaitlist(SessionFilter.session(r),id,key,body),r);}
    @GetMapping("/api/public/batches/{publicId}/verification") Object verification(@PathVariable String publicId,HttpServletRequest r) {return Api.ok(freezer.verification(publicId),r);}
}
