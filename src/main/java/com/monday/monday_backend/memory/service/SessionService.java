package com.monday.monday_backend.memory.service;

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
        if (existing.isPresent()) {
            return existing.get().toDTO(HttpStatus.CONFLICT, "Cannot create a new session memory when one is recording");
        }
        SessionMemoryEntity entity = new SessionMemoryEntity();
        entity.setPrincipalId(principalId);
        entity.setPrincipalType(principalType);
        entity.setSource(request.source());
        entity.setSessionState(SessionState.ACTIVE);
        entity.setSourceConversation(request.sourceConversationKey());
        entity.setIdempotencyKey(idempotencyKey);
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(entity.getCreatedAt());
        entity.setChunkCount(0);

        try {
            SessionMemoryEntity saved = sessionMemoryRepository.saveAndFlush(entity);
            return saved.toDTO(HttpStatus.OK, "Saved Session Memory Successfully");
        } catch (DataIntegrityViolationException de) {
            Optional<SessionMemoryEntity> seshMem = sessionMemoryRepository.findByPrincipalIdAndIdempotencyKey(principalId, idempotencyKey);
            if (seshMem.isPresent()){
                return seshMem.get().toDTO(HttpStatus.CONFLICT, "Cannot create a new session memory when one is recording");
            }
            return entity.toDTO(HttpStatus.NOT_FOUND, "Something is wrong with the database: "+de);
        }
    }

    public SessionMemoryResponseDTO stopSessionMemory(PrincipalType principalType, String principalId, UpdateSessionRequestDTO updateSessionRequestDTO) {
        SessionMemoryEntity currentSession = sessionMemoryRepository
                .findBySessionIdAndPrincipalTypeAndPrincipalId(UUID.fromString(updateSessionRequestDTO.sessionId()), principalType, principalId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Could not identify the session"
                ));

        SessionState state = currentSession.getSessionState();

        if (state == SessionState.STOPPED) {
            // idempotent behavior
            return currentSession.toDTO(HttpStatus.OK, "Session already stopped");
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
        return saved.toDTO(HttpStatus.OK, "Session memory stopped successfully");
    }

}
