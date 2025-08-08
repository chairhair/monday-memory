package com.monday.monday_backend.query.memory.dto;

import java.time.Instant;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A single memory entry stored for a session")
public record MemoryChunkDTO(
        @Schema(description = "The actual content of the memory chunk", example = "User asked about setting budget goals.")
        String content,
        @Schema(description = "ISO timestamp for when the memory was recorded", example = "2025-08-06T19:45:00Z")
        Instant timestamp,
        @Schema(description = "Tags used for filtering or semantic relevance", example = "[\"budget\", \"goal\"]")
        List<String> tags) {
}
