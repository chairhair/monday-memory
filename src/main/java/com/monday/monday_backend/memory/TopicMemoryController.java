package com.monday.monday_backend.memory;

import com.monday.monday_backend.auth.principal.AuthUser;
import com.monday.monday_backend.memory.dto.MemoryChunkDTO;
import com.monday.monday_backend.memory.dto.TopicMemoryResponseDTO;
import com.monday.monday_backend.memory.dto.TopicMemorySearchRequestDTO;
import com.monday.monday_backend.memory.dto.TopicMemoryUpdateRequestDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * "Hey, what do I know?" - This class
 *
 * Primary Use Case: For memory that's long-lived and grouped by a user-defined topic/tag/collection
 * Auth: User JWT required
 * Typical Use: Used after a chat prompt appears or when the user rotates to important topics to them
 *
 * This enforces a single source of truth.
 * - Used for when we're looking for a particular topic.
 * - Handles our Postgres database.
 * - Used when we need a durable recall that's not tied to a single sesh
 *
 */
@RestController
@RequestMapping("/v1/memory/topic")
@RequiredArgsConstructor
public class TopicMemoryController {

    private final MemoryService memory;

    /**
     * This will create a topic
     */
    @PostMapping
    public TopicMemoryResponseDTO createTopic(@AuthenticationPrincipal AuthUser user, @RequestBody TopicMemoryResponseDTO req) {
        return new TopicMemoryResponseDTO(0, null, null, 0, null);
    }

    /**
     * This controller will return a list of collated text based on the tags provided
     */
    @PostMapping("/list")
    public TopicMemoryResponseDTO collateMemory(
            @AuthenticationPrincipal AuthUser user,
            @RequestBody TopicMemorySearchRequestDTO collateMemoryResponseDTO
    ) {
        return new TopicMemoryResponseDTO(0, null, null, 0, null);
    }

    @PutMapping("/{topicId}")
    public TopicMemoryResponseDTO updateTopic(@PathVariable("topicId") String topicId, @RequestBody TopicMemoryUpdateRequestDTO update) {
        return new TopicMemoryResponseDTO(0, null, null, 0, null);
    }

    @DeleteMapping("/{topicId}")
    public TopicMemoryResponseDTO deleteTopic(@PathVariable("topicId") String topicId) {
        return null;
    }

    @PutMapping("/{topicId}/memories/{id}")
    public TopicMemoryResponseDTO updateMemoryUnderTopic(@PathVariable("topicId") String topicId,
                                                         @PathVariable("id") String memoryId,
                                                         @RequestBody MemoryChunkDTO memoryChunkDTO) {
        return new TopicMemoryResponseDTO(0, null, null, 0, null);
    }

    @DeleteMapping("/{topicId}/memories/{id}")
    public TopicMemoryResponseDTO deleteMemoryUnderTopic(@PathVariable("topicId") String topicId,
                                               @PathVariable("id") String memoryId) {
        return null;
    }
}
