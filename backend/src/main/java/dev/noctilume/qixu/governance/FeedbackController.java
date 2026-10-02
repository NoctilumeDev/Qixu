package dev.noctilume.qixu.governance;

import dev.noctilume.qixu.common.*;
import dev.noctilume.qixu.identity.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1")
public class FeedbackController {
    private final Feedback service;
    public FeedbackController(Feedback service) {this.service=service;}
    private AuthService.Session session(HttpServletRequest request) {return SessionFilter.session(request);}
    private String requestId(HttpServletRequest request) {return request.getAttribute("requestId").toString();}
    @GetMapping("/feedback") Object mine(@RequestParam(defaultValue="1") int page,HttpServletRequest r) {return Api.ok(service.list(session(r).actor(),false,page),r);}
    @GetMapping("/feedback/{id}") Object get(@PathVariable long id,HttpServletRequest r) {return Api.ok(service.get(session(r).actor(),id),r);}
    @PostMapping("/feedback") Object create(@RequestHeader(value="Idempotency-Key",required=false) String key,@RequestBody Feedback.Create body,HttpServletRequest r) {return Api.ok(service.create(session(r),key,body,requestId(r)),r);}
    @PostMapping("/feedback/{id}/supplements") Object supplement(@PathVariable long id,@RequestHeader(value="Idempotency-Key",required=false) String key,@RequestBody Feedback.Supplement body,HttpServletRequest r) {return Api.ok(service.supplement(session(r),id,key,body,requestId(r)),r);}
    @PostMapping(value="/feedback/{id}/attachments",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) Object upload(@PathVariable long id,@RequestHeader(value="Idempotency-Key",required=false) String key,@RequestPart("file") MultipartFile file,HttpServletRequest r) throws java.io.IOException {
        if(file.getSize()>1_048_576)throw new DomainException(413,"PAYLOAD_TOO_LARGE","照片须不超过1MiB。");
        return Api.ok(service.attach(session(r),id,key,file.getBytes(),requestId(r)),r);
    }
    @GetMapping("/feedback/{report}/attachments/{id}") ResponseEntity<byte[]> download(@PathVariable long report,@PathVariable long id,HttpServletRequest r) {
        var attachment=service.attachment(session(r).actor(),report,id);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(attachment.type())).cacheControl(CacheControl.noStore()).header("Pragma","no-cache").header("X-Content-Type-Options","nosniff").header("Content-Security-Policy","default-src 'none'; sandbox").header("Content-Disposition","inline; filename=feedback-"+id+(attachment.type().equals("image/png")?".png":".jpg")).body(attachment.bytes());
    }
    @GetMapping("/spaces/{id}/facts") Object facts(@PathVariable long id,HttpServletRequest r) {return Api.ok(service.publicFacts(id),r);}
    @GetMapping("/admin/feedback") Object administrative(@RequestParam(defaultValue="1") int page,HttpServletRequest r) {return Api.ok(service.list(session(r).actor(),true,page),r);}
    @PostMapping("/admin/feedback/{id}/actions") Object action(@PathVariable long id,@RequestHeader(value="Idempotency-Key",required=false) String key,@RequestBody Feedback.Action body,HttpServletRequest r) {return Api.ok(service.decide(session(r),id,key,body,requestId(r)),r);}
    @GetMapping("/admin/repairs") Object repairs(@RequestParam(defaultValue="1") int page,HttpServletRequest r) {return Api.ok(service.repairs(session(r).actor(),page),r);}
    @GetMapping("/admin/repairs/{id}") Object repair(@PathVariable long id,HttpServletRequest r) {return Api.ok(service.getRepair(session(r).actor(),id),r);}
    @PostMapping("/admin/repairs") Object createRepair(@RequestHeader(value="Idempotency-Key",required=false) String key,@RequestBody Feedback.RepairCreate body,HttpServletRequest r) {return Api.ok(service.createRepair(session(r),key,body,requestId(r)),r);}
    @PostMapping("/admin/repairs/{id}/actions") Object repairAction(@PathVariable long id,@RequestHeader(value="Idempotency-Key",required=false) String key,@RequestBody Feedback.RepairAction body,HttpServletRequest r) {return Api.ok(service.repairAction(session(r),id,key,body,requestId(r)),r);}
    @PostMapping("/admin/repairs/{id}/limits") Object linkLimit(@PathVariable long id,@RequestHeader(value="Idempotency-Key",required=false) String key,@RequestBody Feedback.LimitLink body,HttpServletRequest r) {return Api.ok(service.linkLimit(session(r),id,key,body,requestId(r)),r);}
}
