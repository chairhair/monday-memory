package com.monday.monday_backend.query.guest;

import com.monday.monday_backend.memory.MemoryService;
import com.monday.monday_backend.query.guest.dto.QueryGuestRequestDTO;
import com.monday.monday_backend.query.guest.dto.QueryGuestResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * "Hey, what can I give Anon?" - This class
 *
 * Primary Use Case: For non-authenticated guest queries.
 * Requirements: None
 * Typical Use: For when the GUEST is about to inject some feedback into the ChatGPT Model
 *
 * This is an on-demand retrieval request. It can:
 * - Search across both session + topic memories.
 * - Used when the FE needs to retrieve known info on a guest from their session.
 * - It can search across session + topic memories.
 *
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/query/guest")
public class QueryGuestController {

    private final MemoryService memory;

    /**
     * Searches/reads a curated/public subset
     */
    @GetMapping("/public/q")
    public QueryGuestResponseDTO searchQuery(QueryGuestRequestDTO query) {
        return null;
    }


    /**
     * Fetches a specific shared view
     */
    @GetMapping("/shares/{shareToken}")
    public QueryGuestResponseDTO searchSharedQuery(@RequestParam("{shareToken}") String shareToken) {
        return null;
    }
}
