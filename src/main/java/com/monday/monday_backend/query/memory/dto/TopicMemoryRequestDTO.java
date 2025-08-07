package com.monday.monday_backend.query.memory.dto;

import java.util.List;

public record TopicMemoryRequestDTO(int userId, List<String> sessionIds, List<String> filterTags, Integer limit) {
}
