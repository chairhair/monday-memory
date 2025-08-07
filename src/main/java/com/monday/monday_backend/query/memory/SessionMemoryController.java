package com.monday.monday_backend.query.memory;

import com.monday.monday_backend.query.memory.dto.MemoryChunkDTO;
import com.monday.monday_backend.query.memory.dto.SessionMemoryRequestDTO;
import com.monday.monday_backend.query.memory.dto.SessionMemoryResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/memory/session")
@RequiredArgsConstructor
public class SessionMemoryController {

    private final MemoryService memoryService;

    @PostMapping
    public SessionMemoryResponseDTO createMemory(@RequestBody SessionMemoryRequestDTO memoryRequestDTO){
        return new SessionMemoryResponseDTO(null, null, null, null);
    }

    @PostMapping
    public SessionMemoryResponseDTO getMemoryList(
            @RequestParam(required = true) List<String> sessionIds,
            @RequestParam(required = false) List<String> tags,
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer size) {
        return new SessionMemoryResponseDTO(null, null, null, null);
    }

    @DeleteMapping
    public boolean deleteMemory(@RequestParam List<String> sessionIds) {
        return true;
    }
}
