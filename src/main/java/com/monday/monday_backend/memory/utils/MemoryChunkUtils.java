package com.monday.monday_backend.memory.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.monday.monday_backend.memory.entity.MemoryChunkEntity;
import com.monday.monday_backend.memory.entity.SessionMemoryEntity;
import com.monday.shared.memory.dto.ResponseMemoryChunkDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class MemoryChunkUtils {

    private final ObjectMapper objectMapper;

    /**
     * Convert a MemoryChunkEntity into a ResponseMemoryChunkDTO for API responses.
     */
    public ResponseMemoryChunkDTO toDto(MemoryChunkEntity entity) {
        String contentJson = toJson(entity.getContent());

        return new ResponseMemoryChunkDTO(
                contentJson,
                entity.getSession().getPrincipalType(),
                entity.getSession().getPrincipalId(),
                entity.getIngestedAt(),
                entity.getTags()
        );
    }

    /**
     * Convert a ResponseMemoryChunkDTO to a MemoryChunkEntity
     */
    public MemoryChunkEntity toMemoryEntity(ResponseMemoryChunkDTO dto, SessionMemoryEntity sessionMemory) {
        Instant now = Instant.now();
        MemoryChunkEntity memoryChunkEntity = new MemoryChunkEntity();
        Map<String, Object> content = toMap(dto.content());
        memoryChunkEntity.setContent(content);
        if (sessionMemory != null) {
            memoryChunkEntity.setSession(sessionMemory);
        }
        memoryChunkEntity.setIngestedAt(now);
        memoryChunkEntity.setOccurredAt(now);
        memoryChunkEntity.setTags(dto.tags());
        memoryChunkEntity.setHashSha256(sha256(content.get("text").toString()));
        return memoryChunkEntity;
    }

    /**
     * Build a MemoryChunkEntity for a *user* message in a chat-like interaction.
     */
    public MemoryChunkEntity forUserMessage(
            SessionMemoryEntity session,
            String text,
            String source // e.g. "DISCORD", "QUERY_API"
    ) {
        MemoryChunkEntity chunk = new MemoryChunkEntity();
        Instant now = Instant.now();

        chunk.setSession(session);
        chunk.setOccurredAt(now);
        chunk.setIngestedAt(now);

        chunk.setTags(List.of(
                "kind:chat_message",
                "role:"+session.getPrincipalType().toString(),
                "source:" + source
        ));

        chunk.setContent(Map.of(
                "kind", "chat_message",
                "role", session.getPrincipalType().toString(),
                "text", text,
                "source", source
        ));

        String normalized = normalize(text);
        chunk.setHashSha256(sha256(normalized));
        return chunk;
    }

    /**
     * Build a MemoryChunkEntity for an *assistant* message in a chat-like interaction.
     */
    public MemoryChunkEntity forAssistantMessage(
            SessionMemoryEntity session,
            String text,
            String source // e.g. "DISCORD", "QUERY_API"
    ) {
        MemoryChunkEntity chunk = new MemoryChunkEntity();
        Instant now = Instant.now();

        chunk.setSession(session);
        chunk.setOccurredAt(now);
        chunk.setIngestedAt(now);

        chunk.setTags(List.of(
                "kind:chat_message",
                "role:ASSISTANT",
                "source:" + source
        ));

        chunk.setContent(Map.of(
                "kind", "chat_message",
                "role", "ASSISTANT",
                "text", text,
                "source", source
        ));

        String normalized = normalize(text);
        chunk.setHashSha256(sha256(normalized));
        return chunk;
    }

    /**
     * Deserialize arbitrary content to Map using a shared Object Mapper.
     */
    public Map<String, Object> toMap(String content) {
        if (content == null) {
            return null;
        }
        try {
            return objectMapper.readValue(content, new TypeReference<Map<String,Object>>() {
            });
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize memory chunk content: {}", content, e);
            throw new IllegalStateException("Failed to serialize memory chunk content", e);
        }
    }

    /**
     * Serialize arbitrary content to JSON using the shared ObjectMapper.
     * This is used by both DTO mapping and any other place that needs
     * a JSON string from the chunk content.
     */
    public String toJson(Object content) {
        if (content == null) {
            return "null";
        }
        try {
            return objectMapper.writeValueAsString(content);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize memory chunk content: {}", content, e);
            throw new IllegalStateException("Failed to serialize memory chunk content", e);
        }
    }

    /**
     * Build a textual context summary from a list of recent chunks,
     * suitable for dropping into the LLM system prompt.
     *
     * Expected (but not strictly required) content shape per chunk:
     *   {
     *      "kind": "chat_message",
     *      "role": "user" | "assistant",
     *      "text": "some text",
     *      "source": "DISCORD" | "QUERY_API" | ...
     *   }
     *
     * If the structure is different, we fall back to JSON for that chunk.
     */
    public String buildContext(HashMap<String,List<MemoryChunkEntity>> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return "No prior context.";
        }

        StringBuilder sb = new StringBuilder();

        // We assume caller is already giving us "most recent first" (e.g. ORDER BY occurredAt DESC).
        // We'll keep that order, top to bottom, so the LLM sees recent stuff first.
        for (Map.Entry<String, List<MemoryChunkEntity>> chunkWithSession : chunks.entrySet()) {
            sb.append("Session Id - ").append(chunkWithSession.getKey());
            for (MemoryChunkEntity chunk : chunkWithSession.getValue()) {
                String line = renderChunkForContext(chunk);
                if (!line.isBlank()) {
                    sb.append(line).append("\n");
                }
            }
        }

        return sb.toString().trim();
    }

    // ----------------- internal helpers -----------------

    private String renderChunkForContext(MemoryChunkEntity chunk) {
        Object rawContent = chunk.getContent();

        Map<String, Object> map = null;
        if (rawContent instanceof Map<?, ?> m) {
            @SuppressWarnings("unchecked")
            Map<String, Object> cast = (Map<String, Object>) m;
            map = cast;
        } else if (rawContent instanceof String s) {
            // Try to parse JSON string back to a map; if it fails, just return the raw string.
            try {
                map = objectMapper.readValue(s, new TypeReference<Map<String, Object>>() {});
            } catch (JsonProcessingException e) {
                log.warn("Failed to parse chunk content JSON, falling back to raw string: {}", s, e);
                return s;
            }
        }

        if (map == null) {
            // Unknown content type; last-resort JSON
            return toJson(rawContent);
        }

        Object roleObj = map.get("principalType");
        Object textObj = map.get("body");

        String role = roleObj != null ? roleObj.toString() : "unknown";
        String text;

        if (textObj != null) {
            text = textObj.toString();
        } else {
            // If there's no "text" field, just dump the whole map
            text = toJson(map);
        }

        // You can include timestamps if you want; for now we keep it simple.
        return role + ": " + text;
    }

    /**
     * Normalize text so hashing is deterministic and resilient to whitespace noise:
     *  - Trim leading/trailing whitespace
     *  - Collapse repeated whitespace into a single space
     *  - Preserve punctuation/case
     */
    private String normalize(String text) {
        if (text == null) return "";
        return text
                .trim()
                .replaceAll("\\s+", " ");
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
