package com.monday.monday_backend.query.user;

import com.monday.monday_backend.query.user.dto.QueryUserRequestDTO;
import com.monday.monday_backend.query.user.dto.QueryUserResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;

/**
 * "Hey, what can I give my user?" - This class
 *
 * Primary Use Case: Searches through user memories (by text, tags, time) to access user memories.
 *
 * This is an on-demand retrieval request. It can:
 * - Search across both session + topic memories.
 * - Used when the FE needs to find info on a user.
 * - It can search across session + topic memories.
 *
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/query/user")
public class QueryUserController {

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
    public QueryUserResponseDTO fetchFilteredMemorySet(@RequestBody QueryUserRequestDTO dto) {
        return null;
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
    public QueryUserResponseDTO fetchMemory(@PathVariable Long memoryId) {
        return null;
    }

}
