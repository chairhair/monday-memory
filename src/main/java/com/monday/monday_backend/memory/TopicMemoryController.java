package com.monday.monday_backend.memory;

import com.monday.monday_backend.memory.dto.TopicMemoryRequestDTO;
import com.monday.monday_backend.memory.dto.TopicMemoryResponseDTO;
import lombok.RequiredArgsConstructor;
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

    /**
     * This controller will return a list of collated text based on the tags provided
     */
    @PostMapping
    public TopicMemoryResponseDTO collateMemory(
            @RequestBody TopicMemoryRequestDTO collateMemoryResponseDTO
    ) {
        return new TopicMemoryResponseDTO(0, null, null, 0, null);
    }
}
