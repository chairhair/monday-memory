package com.monday.monday_backend.query.memory.dto;

import java.util.List;

public record TopicMemoryResponseDTO(int statusCode, String groupedMemory, List<String> sourceSessions, int tokensUsed, String summaryPreview) {
}
