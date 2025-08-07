package com.monday.monday_backend.query.memory.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Response containing stored memory chunks for one or more sessions")
public record SessionMemoryResponseDTO(
        @Schema(description = "Session IDs retrieved", example = "[\"abc123\"]")
        List<String> sessionIds,

        @Schema(description = "Filtered tags used", example = "[\"finance\"]")
        List<String> filterTags,

        @Schema(description = "Limit applied to response", example = "10")
        Integer limit,

        @Schema(description = "User ID for whom the memory was retrieved", example = "user-456")
        String userId,

        @Schema(description = "List of retrieved memory chunks")
        List<MemoryChunkDTO> memoryChunks
) {}

