package com.monday.monday_backend.memory;

import com.monday.monday_backend.auth.principal.AuthUser;
import com.monday.monday_backend.auth.principal.PrincipalContext;
import com.monday.monday_backend.auth.principal.PrincipalResolver;
import com.monday.monday_backend.auth.users.helper.PrincipalEntry;
import com.monday.monday_backend.memory.service.MemoryService;
import com.monday.monday_backend.memory.service.SessionService;
import com.monday.shared.memory.dto.RecallRequestDTO;
import com.monday.shared.memory.dto.RecallResponseDTO;
import com.monday.shared.memory.dto.RequestMemoryChunkDTO;
import com.monday.shared.memory.dto.ResponseMemoryChunkDTO;
import com.monday.shared.memory.session.GuestHandle;
import com.monday.shared.memory.session.dto.*;
import com.monday.shared.memory.session.utils.PrincipalType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

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

    private final PrincipalResolver principalResolver;
    private final MemoryService memoryService;
    private final SessionService sessionService;    // Incorporated for quick Entity retrievals

    /**
     * Start or reuse a session for the authenticated user.
     * Idempotency is handled via the X-Idempotency-Key header.
     */
    @PostMapping
    public SessionMemoryResponseDTO createSession(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestBody CreateSessionRequestDTO createRequestDTO,
            @RequestHeader(name = "X-Idempotency-Key", required = false)
            String idempotencyKey) {

        PrincipalContext principal = principalResolver.resolve(authUser, createRequestDTO.toGuestHandle());

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            idempotencyKey = UUID.randomUUID().toString();
        }

        // Let SessionService own creation/upsert logic.
        return sessionService.createOrReuseSession(principal, createRequestDTO, idempotencyKey);
    }

    /**
     * Append a memory chunk to an existing session for the authenticated user.
     * This still uses the old MemoryService signature, but we derive
     * principalType + principalId from PrincipalContext instead of PrincipalEntry.
     */
    @PostMapping("/memory-chunk")
    public ResponseMemoryChunkDTO createMemoryChunk(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestBody RequestMemoryChunkDTO memoryChunkDTO) {

        PrincipalContext principal = principalResolver.resolve(authUser, memoryChunkDTO.toGuestHandle());

        return memoryService.recordOnly(
                principal,
                memoryChunkDTO
        );
    }

    @GetMapping("/session-search?sessionId={sessionId}")
    public SessionMemoryResponseDTO searchSession(@RequestParam("sessionId") String sessionId) {
        return sessionService.getSession(UUID.fromString(sessionId));
    }

    @PutMapping("/stop-session")
    public SessionMemoryResponseDTO stopSession(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestBody UpdateSessionRequestDTO updateRequestDTO) {

        PrincipalContext principal = principalResolver.resolve(authUser, updateRequestDTO.toGuestHandle());

        return sessionService.stopSessionState(
                UUID.fromString(updateRequestDTO.sessionId()),
                principal
        );
    }

    @PostMapping("/recall")
    public RecallResponseDTO recallSession(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestBody RecallRequestDTO requestDTO
            ) {
        PrincipalContext principal = principalResolver.resolve(authUser, new GuestHandle(requestDTO.principalKey(), requestDTO.sourceToGuestSource()));
        principal.validateShape();

        return sessionService.recallSessionInfo(principal, requestDTO);
    }

    @PostMapping("/list")
    public SessionMemoryResponseDTO getMemoryList(
            @RequestBody SessionMemoryFilterRequestDTO sessionMemoryFilterRequestDTO) {
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "API not implemented yet");
    }

    @DeleteMapping
    public boolean deleteMemory(@RequestParam List<String> sessionIds) {
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "API not implemented yet");
    }

    /**
     * Get by source conversation (used for if we don't have a good way to grab the information via principalId)
     * @return
     */
    @Deprecated
    @GetMapping("/source-conversation/{key}")
    public SessionMemoryResponseDTO getSessionBySourceConversation(@PathVariable("key") String key) {
        return sessionService.findBySourceConversation(key);
    }
}
