package dev.noctilume.qixu.preparation;

import dev.noctilume.qixu.common.DomainException;
import java.nio.file.*;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;

class HttpRandomnessSourceTest {
    private static final Path ROOT=Path.of(System.getProperty("basedir")).toAbsolutePath().getParent();
    private final JsonMapper json=JsonMapper.builder().build();
    private HttpRandomnessSource source(String helper) {return new HttpRandomnessSource(json,System.getenv().getOrDefault("QIXU_NODE","node"),helper,"");}
    @Test void javaProcessProtocolVerifiesRetainedProofAndRejectsBadRoundAndSignature() throws Exception {
        byte[] fixture=Files.readAllBytes(ROOT.resolve("randomness/fixtures/quicknet-32721736.json"));
        var source=source(ROOT.resolve("randomness/verify.mjs").toString());
        var proof=source.verify(fixture,32721736);
        assertEquals(RandomnessSource.CHAIN,proof.chain());assertEquals(RandomnessSource.VERIFIER,proof.verifier());
        assertEquals(32721736,proof.round());
        assertEquals("RANDOM_SOURCE_INVALID",assertThrows(DomainException.class,()->source.verify(fixture,32721737)).code());
        var fractional=json.readValue(fixture,Map.class);fractional.put("round",32721736.5);
        assertEquals("RANDOM_SOURCE_INVALID",assertThrows(DomainException.class,()->source.verify(json.writeValueAsBytes(fractional),32721736)).code());
        var broken=json.readValue(fixture,Map.class);broken.put("signature","00".repeat(48));
        var invalid=assertThrows(DomainException.class,()->source.verify(json.writeValueAsBytes(broken),32721736));
        assertEquals(503,invalid.status());assertEquals("RANDOM_SOURCE_INVALID",invalid.code());
    }
    @Test void missingHelperIsNotAnAuthorizationToUseLocalRandomness() {
        var result=assertThrows(DomainException.class,()->source(ROOT.resolve("randomness/not-installed.mjs").toString()).verify("{}".getBytes(java.nio.charset.StandardCharsets.UTF_8),32721736));
        assertEquals(503,result.status());assertEquals("RANDOM_VERIFIER_UNAVAILABLE",result.code());
    }
}
