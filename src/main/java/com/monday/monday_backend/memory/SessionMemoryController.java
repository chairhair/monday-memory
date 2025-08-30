package com.monday.monday_backend.memory;

import com.monday.monday_backend.memory.dto.SessionMemoryFilterRequestDTO;
import com.monday.monday_backend.memory.dto.SessionMemoryRequestDTO;
import com.monday.monday_backend.memory.dto.SessionMemoryResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * "Hey, what just happened?" - This class
 *
 * This class details what just happened under a chat.
 * Regards:
 * - Short-lived, recency-weighted context (e.g. last N Turns/ last 2-24 hours)
 * - Stored under a Redis/Elasticsearch.
 * - Used when we need to immediate render the convo state without searching
 */
@RestController
@RequestMapping("/v1/memory/session")
@RequiredArgsConstructor
public class SessionMemoryController {

    private final MemoryService memoryService;

    @PostMapping
    public SessionMemoryResponseDTO createMemory(@RequestBody SessionMemoryRequestDTO memoryRequestDTO){
        return new SessionMemoryResponseDTO(200, null, null, null, null, null);
    }

    @PostMapping("/list")
    public SessionMemoryResponseDTO getMemoryList(
            @RequestBody SessionMemoryFilterRequestDTO sessionMemoryFilterRequestDTO) {
        return new SessionMemoryResponseDTO(200, null, null, null, null, null);
    }

    @DeleteMapping
    public boolean deleteMemory(@RequestParam List<String> sessionIds) {
        return true;
    }
}
