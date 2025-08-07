package com.monday.monday_backend.query.memory.dto;

import java.util.List;

public record SessionMemoryRequestDTO(String sessionId, List<MemoryChunkDTO> memoryChunk) {
}
