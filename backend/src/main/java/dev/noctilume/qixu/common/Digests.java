package dev.noctilume.qixu.common;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

public final class Digests {
    private static final SecureRandom RANDOM = new SecureRandom();
    private Digests() {}
    public static String sha256(String input) {
        return sha256(input.getBytes(StandardCharsets.UTF_8));
    }
    public static String sha256(byte[] input) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(input)); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    public static String token() { byte[] bytes=new byte[32]; RANDOM.nextBytes(bytes); return HexFormat.of().formatHex(bytes); }
    public static boolean equal(String a, String b) {
        return a != null && b != null && MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8),b.getBytes(StandardCharsets.UTF_8));
    }
}
