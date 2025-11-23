package com.monday.monday_backend.memory.service;

import com.monday.monday_backend.auth.guests.GuestService;
import com.monday.monday_backend.memory.entity.SessionMemoryEntity;
import com.monday.monday_backend.memory.repo.SessionMemoryRepository;
import com.monday.shared.memory.session.dto.CreateSessionRequestDTO;
import com.monday.shared.memory.session.dto.SessionMemoryResponseDTO;
import com.monday.shared.memory.session.dto.UpdateSessionRequestDTO;
import com.monday.shared.memory.session.utils.PrincipalType;
import com.monday.shared.memory.session.utils.SessionState;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SessionService {

    private final SessionMemoryRepository sessionMemoryRepository;
    private final GuestService guestService;

    private String resolveIdempotencyKey(PrincipalType principalType, String principalId, CreateSessionRequestDTO request) {
        if (request == null) {
            throw new NullPointerException("Request cannot be empty; it must contain a valid User or Guest");
        }
        if (principalType == PrincipalType.GUEST && principalId == null) {
            return request.toIdempotencyKey(UUID.randomUUID().toString(), principalType);
        }

        return request.toIdempotencyKey(principalId, principalType);
    }

    public SessionMemoryResponseDTO findOrCreateSessionMemory(PrincipalType principalType, String principalId, CreateSessionRequestDTO request) {
        String idempotencyKey = resolveIdempotencyKey(principalType, principalId, request);

        Optional<SessionMemoryEntity> existing = sessionMemoryRepository.findByPrincipalIdAndIdempotencyKey(principalId, idempotencyKey);
        if (existing.isPresent() && existing.get().getSessionState() != SessionState.STOPPED) {
            return existing.get().toDTO(HttpStatus.CONFLICT, request.scope(), "Cannot create a new session memory when one is recording");
        }
        SessionMemoryEntity entity = new SessionMemoryEntity();
        entity.setPrincipalId(principalId);
        entity.setPrincipalType(principalType);
        entity.setScope(request.scope());
        entity.setSource(request.source());
        entity.setSessionState(SessionState.ACTIVE);
        entity.setSourceConversation(request.sourceConversationKey());
        entity.setIdempotencyKey(idempotencyKey);
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(entity.getCreatedAt());
        entity.setChunkCount(0);

        try {
            SessionMemoryEntity saved = sessionMemoryRepository.saveAndFlush(entity);
            return saved.toDTO(HttpStatus.OK, saved.getScope(), "Saved Session Memory Successfully");
        } catch (DataIntegrityViolationException de) {
            Optional<SessionMemoryEntity> seshMem = sessionMemoryRepository.findByPrincipalIdAndIdempotencyKey(principalId, idempotencyKey);
            if (seshMem.isPresent()){
                return seshMem.get().toDTO(HttpStatus.CONFLICT, request.scope(), "Cannot create a new session memory when one is recording");
            }
            return entity.toDTO(HttpStatus.NOT_FOUND, request.scope(),"Something is wrong with the database: "+de);
        }
    }

    public SessionMemoryResponseDTO stopSessionMemory(PrincipalType principalType, String principalId, UpdateSessionRequestDTO updateSessionRequestDTO) {
        String knownId = principalId;
        if (principalType == PrincipalType.GUEST) {
            knownId = guestService.resolveGuestId(principalId, updateSessionRequestDTO.sourceToGuestSource());
        }

        SessionMemoryEntity currentSession = sessionMemoryRepository
                .findBySessionIdAndPrincipalTypeAndPrincipalId(UUID.fromString(updateSessionRequestDTO.sessionId()), principalType, knownId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Could not identify the session"
                ));

        if (!currentSession.getPrincipalType().equals(principalType)
                || !currentSession.getPrincipalId().equals(knownId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Principal cannot stop a session they do not own"
            );
        }

        SessionState state = currentSession.getSessionState();

        if (state == SessionState.STOPPED) {
            // idempotent behavior
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Session already stopped"
            );
        }

        if (state != SessionState.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Session is not active and cannot be stopped"
            );
        }

        Instant now = Instant.now();
        currentSession.setSessionState(SessionState.STOPPED);
        currentSession.setEndedAt(now);      // assuming you have this field
        currentSession.setUpdatedAt(now);

        SessionMemoryEntity saved = sessionMemoryRepository.save(currentSession);
        return saved.toDTO(HttpStatus.OK, saved.getScope(),"Session memory stopped successfully");
    }


    // PURE ENTITY RETRIEVAL FUNCTIONS BELOW

    public SessionMemoryResponseDTO findBySourceConversation(String key) {
        Optional<SessionMemoryEntity> sessionMemoryEntity = sessionMemoryRepository.findBySourceConversation(key);
        if (sessionMemoryEntity.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return sessionMemoryEntity.get().toDTO(HttpStatus.OK, sessionMemoryEntity.get().getScope(), "Found Source Conversation");
    }
}
