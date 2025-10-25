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
 * Primary Use Case: Used when the memory is tied to a single, user session or interaction chain
 * (literally "Hey, what's happened within our chat so far")
 * Requirements: user JWT or a scoped session token (Short TTL)
 * Typical Use: Used after a chat prompt appears or when the guest rotates to important topics to them
 *
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
