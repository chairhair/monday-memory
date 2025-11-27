package com.monday.monday_backend.memory.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.monday.monday_backend.memory.entity.MemoryChunkEntity;
import com.monday.monday_backend.memory.entity.SessionMemoryEntity;
import com.monday.shared.memory.dto.ResponseMemoryChunkDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;


@Component
@RequiredArgsConstructor
public class MemoryChunkUtils {

    private final ObjectMapper objectMapper;

    public ResponseMemoryChunkDTO toDto(MemoryChunkEntity entity) {
        String contentJson = null;
        try {
            contentJson = objectMapper.writeValueAsString(entity.getContent());
        } catch (JsonProcessingException e) {
            // up to you: wrap in runtime, log, or fallback
            throw new IllegalStateException("Failed to serialize memory chunk content", e);
        }

        return new ResponseMemoryChunkDTO(
                contentJson,
                entity.getSession().getPrincipalType(),
                entity.getSession().getPrincipalId(),
                entity.getIngestedAt(),
                entity.getTags()
        );
    }

    public MemoryChunkEntity forUserMessage(
            SessionMemoryEntity session,
            String text,
            String source                  // "query_api", "discord", etc.
    ) {
        MemoryChunkEntity chunk = new MemoryChunkEntity();
        Instant now = Instant.now();

        chunk.setSession(session);
        chunk.setOccurredAt(now);
        chunk.setIngestedAt(now);

        chunk.setTags(List.of(
            "kind:chat_message",
            "role:user",
            "source:" + source
        ));

        chunk.setContent(Map.of(
        "kind", "chat_message",
        "role", "user",
        "text", text,
        "source", source
        ));

        String normalized = normalize(text);
        chunk.setHashSha256(sha256(normalized));
        return chunk;
    }

    public MemoryChunkEntity forAssistantMessage(
            SessionMemoryEntity session,
            String text,
            String source
    ) {
        MemoryChunkEntity chunk = new MemoryChunkEntity();
        Instant now = Instant.now();

        chunk.setSession(session);
        chunk.setOccurredAt(now);
        chunk.setIngestedAt(now);

        chunk.setTags(List.of(
                "kind:chat_message",
                "role:assistant",
                "source:" + source
        ));

        chunk.setContent(Map.of(
                "kind", "chat_message",
                "role", "assistant",
                "text", text,
                "source", source
        ));

        String normalized = normalize(text);
        chunk.setHashSha256(sha256(normalized));
        return chunk;
    }

    /**
     * Normalize text so hashing is deterministic and resilient to whitespace noise:
     * - Trim leading/trailing whitespace
     * - Collapse repeated whitespace into a single space
     * - Preserve punctuation/case (important for context relevance)
     */
    private String normalize(String text) {
        if (text == null) return "";
        return text
                .trim()
                .replaceAll("\\s+", " "); // collapse all whitespace runs
    }

    /**
     * Compute SHA-256 hex digest of normalized text.
     */
    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm missing", e);
        }
    }
}

