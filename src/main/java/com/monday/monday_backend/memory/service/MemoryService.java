package com.monday.monday_backend.memory.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.monday.monday_backend.auth.guests.GuestService;
import com.monday.monday_backend.memory.entity.MemoryChunkEntity;
import com.monday.monday_backend.memory.entity.SessionMemoryEntity;
import com.monday.monday_backend.memory.repo.MemoryChunkRepository;
import com.monday.monday_backend.query.utils.HashUtil;
import com.monday.shared.memory.dto.RequestMemoryChunkDTO;
import com.monday.shared.memory.dto.ResponseMemoryChunkDTO;
import com.monday.shared.memory.session.dto.CreateSessionRequestDTO;
import com.monday.shared.memory.session.dto.SessionMemoryResponseDTO;
import com.monday.shared.memory.session.dto.UpdateSessionRequestDTO;
import com.monday.shared.memory.session.utils.PrincipalType;
import com.monday.shared.recording.RecordingScope;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;


/**
 * The purpose of this class is to ensure that when we go to perform CRUD operations on our memory, that we:
 * - Distinguish between guest and user services
 * - Distinguish the action that's being performed
 * - Provide the appropriate feedback upon receipt.
 *
 * As such, the primary service this class performs is:
 * - Managing the caching of our DB responses
 *   + This works as an interceptor for new queries coming in to ensure that request isn't being performed again.
 *   + Works also as an cache for our session and topic data (If we know this is frequent data, we can just grab it)
 * - Performing CRUD operations on Topics AND Sessions
 * - Redirecting query requests to their appropriate classes
 */
@Service
@RequiredArgsConstructor
public class MemoryService {

    private final MemoryChunkRepository memoryChunkRepository;
    private final SessionService sessionService;
    private final TopicService topicService;
    private final GuestService guestService;

    /**
     * - If Username present, calls TopicService. TopicService will most relevant information based on query and will compare it to session info later.
     * - Session info is pulled up and a TFIDF is pulled to graph latest topics.
     * - Compares and returns most relevant data back the user (for our plugin, this may be a "Hey, you might want to copy and paste this!)
     */
    @Transactional(readOnly = true)
    public void retrieveQuery() {

    }

    /**
     * 	- Exactly as it implies: Grabs most recent session data, compares it to TopicService, and then pushes it into our Topic memory
     * 	- This occurs when we're about to exit chat or we just want to save. Can be implicit/explicit.
     * 	- If it doesn't make the top k, just store a small topic blurb about it (no more than 5 words of the core concepts).
     * 		+ We can say something like "Oh, I kinda remember this. Can you tell me more?"
     * 	- EXPLICITLY FOR USERS!
     */
    @Transactional
    public SessionMemoryResponseDTO upsertToTopic(PrincipalType principalType, String id, CreateSessionRequestDTO createRequestDTO) {
        return new SessionMemoryResponseDTO(HttpStatus.ACCEPTED, RecordingScope.PRIVATE, null, null, null, null, null, null);
    }

    /**
     * - Exactly as it implies: Creates an entirely new session based on what's available.
     * - This occurs when we're about to exit chat or we just want to save. Can be implicit/explicit.
     * - EXPLICITLY FOR GUESTS
     */
    @Transactional
    public SessionMemoryResponseDTO upsertToSession(PrincipalType principalType, String id, CreateSessionRequestDTO createRequestDTO) {
        String principalId = switch (principalType) {
            case USER -> {
                if (id == null) {
                    throw new IllegalArgumentException("userId must be provided for USER principalType");
                }
                yield id;
            }
            case GUEST -> {
                if (createRequestDTO.guestKey() == null || createRequestDTO.guestKey().isBlank()) {
                    throw new IllegalArgumentException("guestKey must be provided for GUEST principalType");
                }
                yield guestService.resolveOrCreateGuestId(createRequestDTO.guestKey(), createRequestDTO.sourceToGuestSource());
            }
        };

        return sessionService.findOrCreateSessionMemory(principalType, principalId, createRequestDTO);
    }

    /**
     * Appends a new session memory to the key
     */
    @Transactional
    public ResponseMemoryChunkDTO appendMemoryChunk(PrincipalType principalType, String principalId, RequestMemoryChunkDTO dto) {
        SessionMemoryEntity session = sessionService.getSessionPresent(dto.sessionId(), principalType, principalId, true);
        if (session == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Session couldn't be identified!");
        }
        MemoryChunkEntity newMemoryChunkEntity = new MemoryChunkEntity();
        newMemoryChunkEntity.setSession(session);
        newMemoryChunkEntity.setOccurredAt(dto.timestamp() != null ? dto.timestamp() : Instant.now());
        newMemoryChunkEntity.setIngestedAt(Instant.now());
        newMemoryChunkEntity.setTags(dto.tags());

        Map<String, Object> content = Map.of(
                "body", dto.content(),
                "principalType", dto.principalType().name(),
                "principalId", dto.principalId(),
                "source", dto.source().toString(),
                "sourceConversationKey", dto.sourceConversationKey()
        );
        newMemoryChunkEntity.setContent(content);
        newMemoryChunkEntity.setHashSha256(HashUtil.computeChunkHash(dto.sessionId(), principalType, principalId, newMemoryChunkEntity.getOccurredAt(), dto.content()));

        MemoryChunkEntity responseChunkEntity;
        try {
            responseChunkEntity = memoryChunkRepository.save(newMemoryChunkEntity);
            return responseChunkEntity.toDTO();
        } catch (DataIntegrityViolationException de) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot save memory chunk entity: " + de);
        } catch (JsonProcessingException ex) {
            return new ResponseMemoryChunkDTO(dto.content(), principalType, principalId, newMemoryChunkEntity.getIngestedAt(), newMemoryChunkEntity.getTags());
        }
    }

    /**
     * Update known session state
     */
    public SessionMemoryResponseDTO stopSessionState(PrincipalType principalType, String principalId, UpdateSessionRequestDTO updateRequestDTO) {
        return sessionService.stopSessionMemory(principalType, principalId, updateRequestDTO);
    }

    /**
     * Completely remove any information related to a given session, both on redis and on postgres
     */
    @Transactional
    public void deleteSession() {

    }

    /**
     * Completely remove any information related to a given topic, both on redis and on postgres
     */
    @Transactional
    public void deleteTopic() {

    }

}
