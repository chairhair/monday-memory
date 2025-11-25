package com.monday.monday_backend.query.utils;

import com.monday.shared.memory.session.utils.PrincipalType;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class HashUtil {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_INSTANT;

    public static String computeChunkHash(UUID sessionId,
                                          PrincipalType principalType,
                                          String principalId,
                                          Instant occurredAt,
                                          String content) {

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            // --- Normalize content ---
            String normalized = normalizeContent(content);

            // --- Build canonical string ---
            String canonical = String.join("\n",
                    "sessionId:" + sessionId,
                    "principal:" + principalType + ":" + (principalId == null ? "" : principalId),
                    "occurredAt:" + ISO.format(occurredAt),
                    "content:" + normalized
            );

            byte[] hash = digest.digest(canonical.getBytes(StandardCharsets.UTF_8));

            return bytesToHex(hash);

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not supported", e);
        }
    }

    private static String normalizeContent(String content) {
        if (content == null) return "";
        return content
                .trim()
                .replaceAll("\\s+", " "); // collapse whitespace
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
