package com.monday.monday_backend.memory.service;

import com.monday.monday_backend.auth.guests.GuestService;
import com.monday.monday_backend.auth.principal.PrincipalContext;
import com.monday.monday_backend.memory.entity.SessionMemoryEntity;
import com.monday.monday_backend.memory.repo.SessionMemoryRepository;
import com.monday.shared.memory.session.dto.CreateSessionRequestDTO;
import com.monday.shared.memory.session.dto.SessionMemoryResponseDTO;
import com.monday.shared.memory.session.utils.PrincipalType;
import com.monday.shared.memory.session.utils.SessionState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionService {

    private final SessionMemoryRepository sessionMemoryRepository;
    private final GuestService guestService;

    /**
     * Create or reuse an ACTIVE session for the given principal.
     */
    public SessionMemoryResponseDTO createOrReuseSession(PrincipalContext principal,
                                                         CreateSessionRequestDTO request,
                                                         String idempotencyKey) {
        String principalId = principal.getPrincipalId().toString();
        PrincipalType principalType = principal.getPrincipalType();

        String currentIdempotencyKey = null;
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            currentIdempotencyKey = UUID.randomUUID().toString();
        }

        try {
            SessionMemoryEntity entity = new SessionMemoryEntity();
            entity.setPrincipalType(principalType);
            entity.setPrincipalId(principalId);
            entity.setSource(request.source());
            entity.setSessionState(SessionState.ACTIVE);
            entity.setSourceConversation(request.sourceConversationKey());
            entity.setIdempotencyKey(currentIdempotencyKey);
            entity.setCreatedAt(Instant.now());
            entity.setUpdatedAt(entity.getCreatedAt());
            // user comes from principal's backing user
            if (principal.getUser() != null) {
                entity.setUser(principal.getUser());
            }
            entity.setChunkCount(0);

            SessionMemoryEntity saved = sessionMemoryRepository.saveAndFlush(entity);
            return saved.toDTO(HttpStatus.OK, saved.getScope(),
                    "Saved Session Memory Successfully");
        } catch (DataIntegrityViolationException de) {
            // Idempotency: if a session already exists for this principal+key, return it
            Optional<SessionMemoryEntity> existing =
                    sessionMemoryRepository.findByPrincipalIdAndIdempotencyKey(principalId, idempotencyKey);

            if (existing.isPresent()) {
                SessionMemoryEntity session = existing.get();
                return session.toDTO(
                        HttpStatus.CONFLICT,
                        session.getScope(),
                        "Cannot create a new session memory when one is recording"
                );
            }

            log.error("Failed to save SessionMemoryEntity", de);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to create session memory");
        }
    }

    /**
     * Lookup a session by source conversation key for this principal.
     */
    public SessionMemoryResponseDTO getSessionBySourceConversation(PrincipalContext principal,
                                                                   String sourceConversationKey) {
        String principalId = principal.getPrincipalId().toString();
        PrincipalType principalType = principal.getPrincipalType();

        Optional<SessionMemoryEntity> sessionMemoryEntity =
                sessionMemoryRepository.findTopBySourceConversationAndPrincipalTypeAndPrincipalIdOrderByUpdatedAtDesc(
                        sourceConversationKey,
                        principalType,
                        principalId
                );

        if (sessionMemoryEntity.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No session found for this conversation");
        }

        SessionMemoryEntity entity = sessionMemoryEntity.get();
        return entity.toDTO(HttpStatus.OK, entity.getScope(), "Found Source Conversation");
    }

    /**
     * Internal helper used by MemoryService / QueryProcessingService
     * to ensure a session belongs to this principal and is in the right state.
     */
    public SessionMemoryEntity getSessionPresent(UUID sessionId,
                                                 PrincipalContext principal,
                                                 boolean availabilityRequired) {
        String principalId = principal.getPrincipalId().toString();
        PrincipalType principalType = principal.getPrincipalType();

        Optional<SessionMemoryEntity> sesh =
                sessionMemoryRepository.findBySessionIdAndPrincipalTypeAndPrincipalId(
                        sessionId,
                        principalType,
                        principalId
                );

        if (sesh.isPresent()) {
            SessionMemoryEntity session = sesh.get();
            if (session.getSessionState() == SessionState.ACTIVE || !availabilityRequired) {
                return session;
            }
        }

        return null;
    }

    /**
     * Updates the session chunk count
     */
    public SessionMemoryEntity updateChunkCount(SessionMemoryEntity sessionMemory, int amount) {
        sessionMemory.setChunkCount(sessionMemory.getChunkCount()+amount);
        return sessionMemoryRepository.save(sessionMemory);
    }

    // DEPRECATED METHODS

    public SessionMemoryResponseDTO stopSessionState(UUID sessionId, PrincipalContext principal) {
        SessionMemoryEntity currentSession = getSessionPresent(sessionId, principal, false);

        if (!currentSession.getPrincipalType().equals(principal.getPrincipalType())
                || !currentSession.getPrincipalId().equals(principal.getPrincipalId().toString())) {
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

    public SessionMemoryResponseDTO findBySourceConversation(String key) {
        Optional<SessionMemoryEntity> sessionMemoryEntity = sessionMemoryRepository.findBySourceConversation(key);
        if (sessionMemoryEntity.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return sessionMemoryEntity.get().toDTO(HttpStatus.OK, sessionMemoryEntity.get().getScope(), "Found Source Conversation");
    }
}
