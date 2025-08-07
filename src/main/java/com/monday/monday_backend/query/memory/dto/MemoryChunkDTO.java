package com.monday.monday_backend.query.memory.dto;

import java.time.Instant;
import java.util.List;

public record MemoryChunkDTO(String content, Instant timestamp, List<String> tags) {
}
