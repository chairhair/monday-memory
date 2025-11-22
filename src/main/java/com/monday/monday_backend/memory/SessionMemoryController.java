package com.monday.monday_backend.memory;

import com.monday.monday_backend.auth.principal.AuthUser;
import com.monday.monday_backend.auth.users.helper.PrincipalEntry;
import com.monday.monday_backend.memory.service.MemoryService;
import com.monday.monday_backend.memory.service.SessionService;
import com.monday.shared.memory.session.dto.*;
import com.monday.shared.memory.session.utils.PrincipalType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * "Hey, what just happened?" - This class
 *
 * Primary Use Case: Used when the memory is tied to a single, user session or interaction chain
 * (literally "Hey, what's happened within our chat so far")
 * Requirements: user JWT or a scoped session token (Short TTL)
 * Typical Use: Used after a chat prompt appears or when the guest rotates to important topics to them
 *
 * Regards:
 * - Short-lived, recency-weighted context (e.g. last N Turns/ last 2-24 hours)
 * - Stored under a Redis/Elasticsearch.
 * - Used when we need to immediate render the convo state without searching
 */
@RestController
@RequestMapping("/v1/memory/session")
@RequiredArgsConstructor
public class SessionMemoryController {

    private final MemoryService memoryService;
    private final SessionService sessionService;    // Incorporated for quick Entity retrievals

    @PostMapping("/memory")
    public SessionMemoryResponseDTO createMemory(@RequestBody SessionMemoryRequestDTO memoryRequestDTO) {
        return new SessionMemoryResponseDTO(HttpStatus.ACCEPTED, null, null, null, null, null, null);
    }

    @PostMapping
    public SessionMemoryResponseDTO createSession(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestBody CreateSessionRequestDTO createRequestDTO) {
        // When starting the createSession, we must first check if we have a topic.
        PrincipalEntry principalEntry = PrincipalEntry.authRetrieval(authUser, createRequestDTO);
        PrincipalType principalType = principalEntry.principalType();
        String principalId = principalEntry.principalId();

        if (principalId == null && principalType == PrincipalType.USER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "principalId must be provided!");
        }

        if (principalType == PrincipalType.GUEST) {
            return memoryService.upsertToSession(principalType, principalId, createRequestDTO);
        } else {
            return memoryService.upsertToTopic(principalType, principalId, createRequestDTO);
        }
    }

    @PutMapping("/stop-session")
    public SessionMemoryResponseDTO stopSession(@AuthenticationPrincipal AuthUser authUser,
                                                  @RequestBody UpdateSessionRequestDTO updateRequestDTO) {
        // When starting the updateSession, we must first check if we have a topic.
        PrincipalEntry principalEntry = PrincipalEntry.authRetrieval(authUser, updateRequestDTO);
        PrincipalType principalType = principalEntry.principalType();
        String principalId = principalEntry.principalId();

        return memoryService.stopSessionState(principalType, principalId, updateRequestDTO);
    }

    @PostMapping("/list")
    public SessionMemoryResponseDTO getMemoryList(
            @RequestBody SessionMemoryFilterRequestDTO sessionMemoryFilterRequestDTO) {
        return new SessionMemoryResponseDTO(HttpStatus.ACCEPTED, null, null, null, null, null, null);
    }

    @DeleteMapping
    public boolean deleteMemory(@RequestParam List<String> sessionIds) {
        return true;
    }

    /**
     * Get by source conversation (used for if we don't have a good way to grab the information via principalId)
     * @return
     */
    @GetMapping("/source-conversation/{key}")
    public SessionMemoryResponseDTO getSessionBySourceConversation(@PathVariable("key") String key) {
        return sessionService.findBySourceConversation(key);
    }
}
