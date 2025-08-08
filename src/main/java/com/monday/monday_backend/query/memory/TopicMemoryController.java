package com.monday.monday_backend.query.memory;

import com.monday.monday_backend.query.memory.dto.TopicMemoryResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/memory/topic")
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
