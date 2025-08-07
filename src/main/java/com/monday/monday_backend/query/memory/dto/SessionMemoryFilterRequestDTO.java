package com.monday.monday_backend.query.memory.dto;

import java.util.List;

public record SessionMemoryFilterRequestDTO(List<String> sessionIds, List<String> filterTags, Integer limit, String userId) {
}
