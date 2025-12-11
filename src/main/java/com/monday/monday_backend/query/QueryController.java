package com.monday.monday_backend.query;

import com.monday.monday_backend.auth.principal.AuthUser;
import com.monday.monday_backend.auth.principal.PrincipalContext;
import com.monday.monday_backend.auth.principal.PrincipalResolver;
import com.monday.monday_backend.memory.service.MemoryService;
import com.monday.shared.memory.dto.RequestMemoryChunkDTO;
import com.monday.shared.memory.dto.RequestMemoryQueryDTO;
import com.monday.shared.memory.dto.ResponseMemoryChunkDTO;
import com.monday.shared.query.QueryRequestDTO;
import com.monday.shared.query.QueryResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/**
 * "Hey, what can I give my user?" - This class
 * Primary Use Case: For non-authenticated guest and user queries.
 * Requirements: None
 * Typical Use: For when the GUEST/USER is about to inject some feedback into the ChatGPT Model
 *For Guests...
 * This is an on-demand retrieval request. It can:
 * - Search across both session + topic memories.
 * - Used when the FE needs to retrieve known info on a guest from their session.
 * - It can search across session + topic memories.
 * For Users
 *  Primary Use Case: Searches through user memories (by text, tags, time) to access user memories.
 *  This is an on-demand retrieval request. It can:
 *  - Search across both session + topic memories.
 *  - Used when the FE needs to find info on a user.
 *  - It can search across session + topic memories.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/query")
public class QueryController {

    private final PrincipalResolver principalResolver;
    private final MemoryService memory;

    /**
     * Searches/reads a curated/public subset
     */
    @PostMapping("/public/q")
    public ResponseMemoryChunkDTO searchQuery(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestBody RequestMemoryQueryDTO requestDTO) {


        if (requestDTO.memoryChunkDTO().content() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "There's no query!");
        }

        PrincipalContext principal = principalResolver.fromAuthUser(authUser, requestDTO.memoryChunkDTO().sourceToGuestSource());

        return memory.query(principal, requestDTO);
    }


    /**
     * Fetches a specific shared view
     */
    @GetMapping("/shares/{shareToken}")
    public QueryResponseDTO searchSharedQuery(@RequestParam("{shareToken}") String shareToken) {
        return null;
    }

    /**
     * Grabs multiple memories based on prior context provided.
     *
     * Primary Use Case: Performs a merge sort based on what's present based on the likelihood of being discussed.
     *
     * Requirements: JWT User Auth
     * Typical Use: For when the USER is about to inject some feedback into the ChatGPT Model
     *
     * @param dto - The initial query parameters that were found
     * @return - Our response in the form of what our user currently has.
     */
    @PostMapping("/q")
    public QueryResponseDTO fetchFilteredMemorySet(@AuthenticationPrincipal AuthUser authUser,
                                                       @RequestBody QueryRequestDTO dto) {
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Cannot fetch filtered memory yet");
    }

    /**
     * Grabs one single memory based around a most recent topic or point of interests.
     *
     * Requirements: JWT User Auth
     * Typical Use Case: User selects a tag on what they would like to pre-inject ChatGPT with.
     *
     * @param memoryId - The ID of the actual memory that we have on file
     * @return - our memory id.
     */
    @GetMapping("/{memoryId}")
    public QueryResponseDTO fetchMemory(@AuthenticationPrincipal AuthUser authUser,
                                            @PathVariable Long memoryId) {
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Cannot fetch filtered memory yet");
    }
}
