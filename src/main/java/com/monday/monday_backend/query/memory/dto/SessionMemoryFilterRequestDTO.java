package com.monday.monday_backend.query.memory.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Filter-based request to retrieve stored memory chunks")
public record SessionMemoryFilterRequestDTO(
        @Schema(description = "List of session IDs to pull memory from", example = "[\"abc123\", \"xyz456\"]")
        List<String> sessionIds,

        @Schema(description = "Tags to filter memory by", example = "[\"finance\", \"todo\"]")
        List<String> filterTags,

        @Schema(description = "Maximum number of memory chunks to return", example = "10")
        Integer limit,

        @Schema(description = "User ID requesting the data", example = "user-456")
        String userId
) {}

