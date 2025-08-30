package com.monday.monday_backend.memory;

import com.monday.monday_backend.memory.dto.TopicMemoryResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * "Hey, what do I know?" - This class
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
            @RequestBody TopicMemoryResponseDTO collateMemoryResponseDTO
    ) {
        return new TopicMemoryResponseDTO(0, null, null, 0, null);
    }
}
