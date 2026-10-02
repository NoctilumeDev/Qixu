package dev.noctilume.qixu.preparation;

import dev.noctilume.qixu.common.DomainException;
import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/** Fetch and offline signature verification. Call only without database locks/transactions. */
@Component
public class HttpRandomnessSource implements RandomnessSource {
    private final JsonMapper json; private final HttpClient http;
    private final String node; private final Path helper;
    public HttpRandomnessSource(JsonMapper json,@Value("${qixu.randomness.node:node}")String node,
            @Value("${qixu.randomness.helper:randomness/verify.mjs}")String helper,
            @Value("${qixu.randomness.proxy:}")String proxy) {
        this.json=json;this.node=node;this.helper=Path.of(helper).toAbsolutePath().normalize();
        var builder=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).followRedirects(HttpClient.Redirect.NEVER);
        if(!proxy.isBlank()) {
            var uri=URI.create(proxy);
            if(!"http".equals(uri.getScheme()) || uri.getHost()==null || uri.getPort()<1 || uri.getUserInfo()!=null)throw new IllegalArgumentException("Invalid randomness proxy configuration");
            builder.proxy(ProxySelector.of(new InetSocketAddress(uri.getHost(),uri.getPort())));
        }
        http=builder.build();
    }
    private static DomainException unavailable(String code) {return new DomainException(503,code,"随机证明暂不可验证，仍保留固定轮次，请在结果期限内重试。");}
    @Override public Proof fetch(long round) {
        if(org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive())throw new IllegalStateException("Random source inside transaction");
        if(round<1 || round>9_007_199_254_740_991L)throw new IllegalArgumentException("Invalid round");
        var request=HttpRequest.newBuilder(URI.create("https://api.drand.sh/"+CHAIN+"/public/"+round)).timeout(Duration.ofSeconds(5)).GET().build();
        var body=new ByteArrayOutputStream();
        var future=http.sendAsync(request,HttpResponse.BodyHandlers.ofByteArrayConsumer(part->part.ifPresent(bytes->{
            if(body.size()+bytes.length>12000)throw new IllegalStateException("SOURCE_SIZE_LIMIT");
            body.writeBytes(bytes);
        })));
        try {
            var response=future.get(5,TimeUnit.SECONDS);
            if(response.statusCode()!=200)throw unavailable("RANDOM_SOURCE_UNAVAILABLE");
            return verify(body.toByteArray(),round);
        } catch(InterruptedException e) {Thread.currentThread().interrupt();throw unavailable("RANDOM_SOURCE_INTERRUPTED");}
        catch(ExecutionException|TimeoutException e) {throw unavailable("RANDOM_SOURCE_UNAVAILABLE");}
        finally {if(!future.isDone())future.cancel(true);}
    }
    Proof verify(byte[] body,long round) {
        Process child=null;
        try {
            if(body.length>12000 || !Files.isRegularFile(helper))throw unavailable("RANDOM_VERIFIER_UNAVAILABLE");
            var beacon=json.readValue(body,Map.class);
            if(!(beacon.get("round") instanceof Number number) || number.longValue()!=round || number.doubleValue()!=(double)round
                    || !(beacon.get("signature") instanceof String sig) || !sig.matches("[0-9a-f]{96}")
                    || !(beacon.get("randomness") instanceof String rand) || !rand.matches("[0-9a-f]{64}") || beacon.containsKey("previous_signature"))throw unavailable("RANDOM_SOURCE_INVALID");
            // A small allowlisted message cannot fill the child's stdin pipe before waitFor's budget starts.
            var input=json.writeValueAsString(Map.of("chain",CHAIN,"round",round,"beacon",Map.of("round",round,"signature",sig,"randomness",rand))).getBytes(StandardCharsets.UTF_8);
            if(input.length>12000)throw unavailable("RANDOM_SOURCE_INVALID");
            child=new ProcessBuilder(node,helper.toString()).redirectError(ProcessBuilder.Redirect.DISCARD).start();
            try(var stream=child.getOutputStream()){stream.write(input);}
            if(!child.waitFor(5,TimeUnit.SECONDS))throw unavailable("RANDOM_VERIFIER_TIMEOUT");
            byte[] output;
            try(var stream=child.getInputStream()){output=stream.readNBytes(4097);}
            if(child.exitValue()!=0 || output.length>4096)throw unavailable("RANDOM_SOURCE_INVALID");
            var result=json.readValue(output,Map.class);
            if(!Boolean.TRUE.equals(result.get("verified")) || !CHAIN.equals(result.get("chain")) || !VERIFIER.equals(result.get("verifier"))
                    || !(result.get("round") instanceof Number n) || n.longValue()!=round || n.doubleValue()!=(double)round
                    || !(result.get("signature") instanceof String signature) || !signature.matches("[0-9a-f]{96}")
                    || !(result.get("randomness") instanceof String randomness) || !randomness.matches("[0-9a-f]{64}"))throw unavailable("RANDOM_SOURCE_INVALID");
            return new Proof(CHAIN,round,signature,randomness,VERIFIER);
        } catch(InterruptedException e) {Thread.currentThread().interrupt();throw unavailable("RANDOM_VERIFIER_INTERRUPTED");}
        catch(IOException|tools.jackson.core.JacksonException e) {throw unavailable("RANDOM_VERIFIER_UNAVAILABLE");}
        finally {if(child!=null && child.isAlive())child.destroyForcibly();}
    }
}
