package com.monday.monday_backend.query.guest;

import com.monday.monday_backend.auth.principal.AuthUser;
import com.monday.monday_backend.auth.users.helper.PrincipalEntry;
import com.monday.monday_backend.memory.service.MemoryService;
import com.monday.shared.memory.session.utils.PrincipalType;
import com.monday.shared.query.guest.dto.QueryGuestRequestDTO;
import com.monday.shared.query.guest.dto.QueryGuestResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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
    @PostMapping("/public/q")
    public QueryGuestResponseDTO searchQuery(
            @AuthenticationPrincipal AuthUser authUser,
            QueryGuestRequestDTO requestDTO) {

        PrincipalEntry principalEntry = PrincipalEntry.authRetrieval(authUser, requestDTO);
        PrincipalType principalType = principalEntry.principalType();
        String principalId = principalEntry.principalId();

        return memory.query(principalType, principalId, requestDTO);
    }


    /**
     * Fetches a specific shared view
     */
    @GetMapping("/shares/{shareToken}")
    public QueryGuestResponseDTO searchSharedQuery(@RequestParam("{shareToken}") String shareToken) {
        return null;
    }
}
