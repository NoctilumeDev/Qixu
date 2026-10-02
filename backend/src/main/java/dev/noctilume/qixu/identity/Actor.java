package dev.noctilume.qixu.identity;

public record Actor(long id, String username, String displayName, String role, boolean studentVerified, long authVersion) {}
