package dev.noctilume.qixu.preparation;

import java.time.*;

public interface RandomnessSource {
    String CHAIN="52db9ba70e0cc0f6eaf7803dd07447a1f5477735fd3f661792ba94600c84e971";
    String VERIFIER="drand-client/1.4.2:qixu-offline/0.1";
    long GENESIS=1692803367L;
    record Proof(String chain,long round,String signature,String randomness,String verifier) {}
    Proof fetch(long round);
    static long roundAtOrAfter(LocalDateTime time) {
        long seconds=time.toEpochSecond(ZoneOffset.UTC);
        if(seconds<GENESIS || time.getNano()!=0)throw new IllegalArgumentException("Invalid beacon time");
        return Math.floorDiv(seconds-GENESIS+2,3)+1;
    }
    static LocalDateTime roundTime(long round) {
        if(round<1)throw new IllegalArgumentException("Invalid round");
        return LocalDateTime.ofEpochSecond(Math.addExact(GENESIS,Math.multiplyExact(round-1,3)),0,ZoneOffset.UTC);
    }
}
