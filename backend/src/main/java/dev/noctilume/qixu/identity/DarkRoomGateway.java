package dev.noctilume.qixu.identity;

import dev.noctilume.qixu.common.Digests;
import dev.noctilume.qixu.common.DomainException;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.*;
import java.nio.ByteBuffer;
import java.nio.charset.*;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.Flow;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

/** Current-account verification only. Never trusts profile roles or follows redirects. */
@Component
public class DarkRoomGateway {
    public static final String PROVIDER="DARK_ROOM_LIBRARY";
    private static final String AUTH_PATH="/api/dark-room-library/v1/user/auth";
    private final boolean enabled;
    private final URI uri;
    private final HttpClient client;
    private final Semaphore budget=new Semaphore(4);
    private final JsonMapper json=JsonMapper.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).build();
    public DarkRoomGateway(@Value("${qixu.external.dark-room.enabled:false}") boolean enabled,
                           @Value("${qixu.external.dark-room.auth-uri:}") String endpoint,
                           @Value("${qixu.external.dark-room.allow-loopback-http:false}") boolean allowLoopback) {
        this.enabled=enabled;
        this.uri=enabled?validate(endpoint,allowLoopback):null;
        this.client=HttpClient.newBuilder().connectTimeout(Duration.ofMillis(500)).followRedirects(HttpClient.Redirect.NEVER).build();
    }
    static URI validate(String endpoint,boolean allowLoopback) {
        URI u;
        try {u=URI.create(endpoint);}catch(IllegalArgumentException e){throw new IllegalStateException("Invalid identity endpoint");}
        boolean local=allowLoopback && "http".equals(u.getScheme()) && List.of("127.0.0.1","[::1]").contains(u.getHost());
        if(!u.isAbsolute() || u.getHost()==null || (!"https".equals(u.getScheme()) && !local)
           || u.getRawUserInfo()!=null || u.getRawQuery()!=null || u.getRawFragment()!=null || !AUTH_PATH.equals(u.getRawPath()))
            throw new IllegalStateException("Identity endpoint must be a fixed HTTPS auth URI (explicit loopback test exception only)");
        return u;
    }
    public boolean enabled(){return enabled;}
    public String issuerHash(){if(!enabled)throw unavailable();return Digests.sha256(uri.toASCIIString());}
    public static DomainException unavailable(){return new DomainException(503,"IDENTITY_UNAVAILABLE","身份来源暂时不可用，请稍后核对原操作结果。");}
    static DomainException rejected(){return new DomainException(401,"EXTERNAL_IDENTITY_REJECTED","外部身份已失效，请重新登录。");}
    public static void ticketFormat(String ticket) {
        if(ticket==null || ticket.length()>4096 || !ticket.matches("[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+"))
            throw DomainException.invalid("外部身份票据格式不正确。");
    }
    public String verify(String ticket) {
        if(!enabled)throw unavailable();
        ticketFormat(ticket);
        if(TransactionSynchronizationManager.isActualTransactionActive())throw new IllegalStateException("Identity network verification cannot run in a transaction");
        if(!budget.tryAcquire())throw unavailable();
        CompletableFuture<HttpResponse<byte[]>> call=null;
        try {
            var req=HttpRequest.newBuilder(uri).timeout(Duration.ofMillis(1800)).header("Accept","application/json")
                    .header("Authorization","Bearer "+ticket).GET().build();
            call=client.sendAsync(req,info->new BoundedBody());
            var response=call.get(2,TimeUnit.SECONDS);
            if(response.statusCode()==401 || response.statusCode()==403)throw rejected();
            if(response.statusCode()!=200)throw unavailable();
            var type=response.headers().firstValue("Content-Type").orElse("").toLowerCase(java.util.Locale.ROOT);
            if(!type.startsWith("application/json"))throw unavailable();
            String text=StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(response.body())).toString();
            JsonNode root=json.readTree(text),data=root.path("data");
            if(!root.isObject() || !root.path("code").isIntegralNumber() || !root.path("code").canConvertToInt() || root.path("code").asInt()!=200 || !data.isObject()
                || !data.path("id").isIntegralNumber() || !data.path("id").canConvertToLong() || data.path("id").asLong()<=0
                || !data.path("accountStatus").isIntegralNumber() || !data.path("accountStatus").canConvertToInt() || !data.path("isLogin").isBoolean())throw unavailable();
            if(data.path("accountStatus").asInt()!=0 || data.path("isLogin").asBoolean())throw rejected();
            return Long.toString(data.path("id").asLong());
        }catch(DomainException e){throw e;}
        catch(InterruptedException e){Thread.currentThread().interrupt();throw unavailable();}
        catch(Exception e){throw unavailable();}
        finally {if(call!=null && !call.isDone())call.cancel(true);budget.release();}
    }
    private static final class BoundedBody implements HttpResponse.BodySubscriber<byte[]> {
        private final CompletableFuture<byte[]> result=new CompletableFuture<>();
        private final ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        private Flow.Subscription subscription;
        public CompletionStage<byte[]> getBody(){return result;}
        public void onSubscribe(Flow.Subscription value){subscription=value;value.request(1);}
        public void onNext(List<ByteBuffer> values){
            for(var value:values){if(bytes.size()+value.remaining()>32768){subscription.cancel();result.completeExceptionally(new IllegalStateException("Identity response exceeds budget"));return;}
                byte[] b=new byte[value.remaining()];value.get(b);bytes.writeBytes(b);}
            subscription.request(1);
        }
        public void onError(Throwable error){result.completeExceptionally(error);}
        public void onComplete(){result.complete(bytes.toByteArray());}
    }
}
