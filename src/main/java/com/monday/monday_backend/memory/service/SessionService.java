package com.monday.monday_backend.memory.service;

import com.monday.monday_backend.memory.entity.SessionMemoryEntity;
import com.monday.monday_backend.memory.repo.SessionMemoryRepository;
import com.monday.shared.memory.session.dto.CreateSessionRequestDTO;
import com.monday.shared.memory.session.dto.SessionMemoryResponseDTO;
import lombok.RequiredArgsConstructor;
import org.apache.http.HttpStatus;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SessionService {

    private final SessionMemoryRepository sessionMemoryRepository;

    private String resolveIdempotencyKey(CreateSessionRequestDTO request, String principalId) {
        if (request == null) {
            throw new NullPointerException("Request cannot be empty; it must contain a valid User or Guest");
        }
        if (request.isGuest() && request.userId() == null) {
            return request.toIdempotencyKey(principalId);
        }

        return request.toIdempotencyKey();
    }

    public SessionMemoryResponseDTO findOrCreateSessionMemory(CreateSessionRequestDTO request, String principalId) {
        String idempotencyKey = resolveIdempotencyKey(request, principalId);

        Optional<SessionMemoryEntity> existing = sessionMemoryRepository.findByPrincipalIdAndIdempotencyKey(request.userId(), principalId);
        if (existing.isPresent()) {
            return existing.get().toDTO(HttpStatus.SC_CONFLICT, "Cannot create a new session memory when one is recording");
        }
        SessionMemoryEntity entity = new SessionMemoryEntity();
        entity.setPrincipalId(principalId);
        entity.setSource(request.source().toString());
        entity.setSourceConversation(request.sourceConversationKey());
        entity.setIdempotencyKey(idempotencyKey);
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(entity.getCreatedAt());
        entity.setChunkCount(0);

        try {
            SessionMemoryEntity saved = sessionMemoryRepository.saveAndFlush(entity);
            return saved.toDTO(HttpStatus.SC_OK, "Saved Session Memory Successfully");
        } catch (DataIntegrityViolationException de) {
            Optional<SessionMemoryEntity> seshMem = sessionMemoryRepository.findByPrincipalIdAndIdempotencyKey(principalId, idempotencyKey);
            if (seshMem.isPresent()){
                return seshMem.get().toDTO(HttpStatus.SC_CONFLICT, "Cannot create a new session memory when one is recording");
            }
            return entity.toDTO(HttpStatus.SC_NOT_FOUND, "Something is wrong with the database: "+de);
        }
    }

}
