package com.monday.monday_backend.query.memory.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Response containing grouped or summarized memory across sessions")
public record TopicMemoryResponseDTO(
        @Schema(description = "HTTP status code", example = "200")
        int statusCode,

        @Schema(description = "Grouped or summarized memory result", example = "User discussed savings goals and budget planning.")
        String groupedMemory,

        @Schema(description = "Sessions used to generate the grouped memory", example = "[\"abc123\", \"def456\"]")
        List<String> sourceSessions,

        @Schema(description = "Estimated token usage for grouping or summarization", example = "312")
        int tokensUsed,

        @Schema(description = "Shortened summary for UI display", example = "Savings & Budgeting")
        String summaryPreview
) {}

