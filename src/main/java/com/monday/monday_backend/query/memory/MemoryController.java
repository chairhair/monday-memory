package com.monday.monday_backend.query.memory;

import com.monday.monday_backend.query.memory.dto.TopicMemoryRequestDTO;
import com.monday.monday_backend.query.memory.dto.GroupMemoryResponseDTO;
import com.monday.monday_backend.query.memory.dto.MemoryRequestDTO;
import com.monday.monday_backend.query.memory.dto.MemoryResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/memory")
@RequiredArgsConstructor
public class MemoryController {

    private final MemoryService memoryService;

    @PostMapping
    public MemoryResponseDTO createMemory(@RequestBody MemoryRequestDTO memoryRequestDTO){
        return new MemoryResponseDTO();
    }

    /**
     * This controller will return a list of collated text based on the tags provided
     */
    @GetMapping("/collate")
    public GroupMemoryResponseDTO collateMemory(
        @RequestBody GroupMemoryResponseDTO collateMemoryResponseDTO
    ) {
        return new GroupMemoryResponseDTO();
    }

    @GetMapping
    public GroupMemoryResponseDTO getMemoryList(@RequestBody TopicMemoryRequestDTO groupMemoryRequestDTO) {
        return new GroupMemoryResponseDTO();
    }

    @PostMapping("/delete")
    public boolean deleteMemory(@RequestBody TopicMemoryRequestDTO groupMemoryRequestDTO) {
        return true;
    }

    @PostMapping("/update")
    public GroupMemoryResponseDTO updateMemory(@RequestBody TopicMemoryRequestDTO groupMemoryRequestDTO) {
        return new GroupMemoryResponseDTO();
    }
}
