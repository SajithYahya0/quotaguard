package com.sajith.quotaguard.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** API keys are stored and looked up by SHA-256 hash; the raw key is shown to the client once. */
public final class ApiKeys {

    private ApiKeys() {}

    // A fast hash is the right tool here: keys are 122 random bits (UUIDv4), so there is no
    // dictionary to brute-force, and a slow password hash (bcrypt) would add tens of ms per request.
    public static String hash(String apiKey) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(
                apiKey.getBytes(StandardCharsets.UTF_8)
            );
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Every JVM ships SHA-256", e);
        }
    }
}
