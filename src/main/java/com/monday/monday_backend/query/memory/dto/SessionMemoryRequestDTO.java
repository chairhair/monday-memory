package com.monday.monday_backend.query.memory.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request to save structured memory chunks for a specific session")
public record SessionMemoryRequestDTO(
        @Schema(description = "Service initiating the save request", example = "MONDAY_EXTENSION")
        String serviceName,

        @Schema(description = "Session ID to associate with the memory", example = "abc123")
        String sessionId,

        @Schema(description = "List of memory chunks to save")
        List<MemoryChunkDTO> memoryChunk
) {}

