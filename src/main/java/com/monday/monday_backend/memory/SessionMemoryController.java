package com.monday.monday_backend.memory;

import com.monday.monday_backend.auth.principal.AuthUser;
import com.monday.monday_backend.memory.service.MemoryService;
import com.monday.shared.memory.session.dto.CreateSessionRequestDTO;
import com.monday.shared.memory.session.dto.SessionMemoryRequestDTO;
import com.monday.shared.memory.session.dto.SessionMemoryResponseDTO;
import com.monday.shared.memory.session.utils.PrincipalType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.monday.shared.memory.session.dto.SessionMemoryFilterRequestDTO;
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

    @PostMapping("/memory")
    public SessionMemoryResponseDTO createMemory(@RequestBody SessionMemoryRequestDTO memoryRequestDTO) {
        return new SessionMemoryResponseDTO(200, null, null, null, null, null, null);
    }

    @PostMapping
    public SessionMemoryResponseDTO createSession(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestBody CreateSessionRequestDTO createRequestDTO) {
        // When starting the createSession, we must first check if we have a topic.
        PrincipalType principalType;
        String principalId;

        if (authUser != null) {
            principalType = PrincipalType.USER;
            principalId = authUser.id(); // or whatever your ID type is
        } else {
            principalType = PrincipalType.GUEST;
            principalId = createRequestDTO.guestKey(); // or guestKey, depending on how you've modeled it
        }

        if (principalId == null && principalType == PrincipalType.USER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "principalId must be provided!");
        }

        if (principalType == PrincipalType.GUEST) {
            return memoryService.upsertToSession(principalType, principalId, createRequestDTO);
        } else {
            return memoryService.upsertToTopic(principalType, principalId, createRequestDTO);
        }
    }

    @PostMapping("/list")
    public SessionMemoryResponseDTO getMemoryList(
            @RequestBody SessionMemoryFilterRequestDTO sessionMemoryFilterRequestDTO) {
        return new SessionMemoryResponseDTO(200, null, null, null, null, null, null);
    }

    @DeleteMapping
    public boolean deleteMemory(@RequestParam List<String> sessionIds) {
        return true;
    }
}
