package com.monday.monday_backend.query.memory.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Request to group or summarize memory across multiple sessions")
public record TopicMemoryRequestDTO(
        @Schema(description = "User ID requesting the grouped memory", example = "user-456")
        String userId,

        @Schema(description = "List of session IDs to include", example = "[\"abc123\", \"def789\"]")
        List<String> sessionIds,

        @Schema(description = "Optional tags for filtering memory chunks", example = "[\"todo\"]")
        List<String> filterTags,

        @Schema(description = "Maximum number of entries to include", example = "20")
        Integer limit
) {}

