package com.monday.monday_backend.memory.entity;

import com.monday.monday_backend.memory.repo.SessionMemoryRepository;
import com.monday.monday_backend.memory.service.SessionService;
import com.monday.shared.memory.session.dto.SessionMemoryResponseDTO;
import com.monday.shared.memory.session.dto.UpdateSessionRequestDTO;
import com.monday.shared.memory.session.utils.PrincipalType;
import com.monday.shared.memory.session.utils.SessionSource;
import com.monday.shared.memory.session.utils.SessionState;
import com.monday.shared.recording.RecordingScope;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
class StopSessionTests {

    @Mock
    private SessionMemoryRepository sessionMemoryRepository;

    @InjectMocks
    private SessionService sessionService;

    // Helper to build an entity; adjust to match your actual constructor/setters
    private SessionMemoryEntity buildSession(SessionState state,
                                             Instant lastOccurredAt, Instant endedAt) {
        SessionMemoryEntity entity = new SessionMemoryEntity();
        // If you don't have setters, replace with builder / constructor you *do* have
        entity.setSource(SessionSource.DISCORD);
        entity.setSourceConversation("hawk-tuah-man");
        entity.setPrincipalType(PrincipalType.GUEST);
        entity.setPrincipalId("guest-key-123");
        entity.setScope(RecordingScope.PRIVATE);
        entity.setChunkCount(0);
        entity.setSessionState(state);
        entity.setLastOccurredAt(lastOccurredAt);
        entity.setEndedAt(endedAt);
        entity.setCreatedAt(Instant.now().minusSeconds(7200));
        entity.setUpdatedAt(Instant.now().minusSeconds(3600));
        entity.setIdempotencyKey("test-idemp-key"); // initial value
        return entity;
    }

    private UpdateSessionRequestDTO buildUpdateRequest(String sessionId) {
        // Adjust ctor if your DTO is different
        return new UpdateSessionRequestDTO(sessionId, "guest-key-123");
    }

    @Test
    void stopSession_activeSession_becomesStopped_andPersists() {
        // given
        SessionMemoryEntity active = buildSession(
                SessionState.ACTIVE,
                Instant.now().minusSeconds(300),
                null
        );
        UUID generatedId = UUID.randomUUID();
        String sessionId = generatedId.toString();

        active.setSessionId(generatedId);


        when(sessionMemoryRepository.findById(sessionId))
                .thenReturn(Optional.of(active));
        when(sessionMemoryRepository.findBySessionIdAndPrincipalTypeAndPrincipalId(generatedId, PrincipalType.GUEST, "guest-key-123"))
                .thenReturn(Optional.of(active));
        when(sessionMemoryRepository.save(any(SessionMemoryEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        UpdateSessionRequestDTO dto = buildUpdateRequest(sessionId);

        // when
        SessionMemoryResponseDTO response = sessionService.stopSessionMemory(
                PrincipalType.GUEST,
                "guest-key-123",
                dto
        );

        // then
        ArgumentCaptor<SessionMemoryEntity> captor =
                ArgumentCaptor.forClass(SessionMemoryEntity.class);
        verify(sessionMemoryRepository).save(captor.capture());
        SessionMemoryEntity saved = captor.getValue();

        Assertions.assertEquals(SessionState.STOPPED, saved.getSessionState(),
                "Session state should transition to STOPPED");
        Assertions.assertNotNull(saved.getEndedAt(), "endedAt should be set when stopping");
        Assertions.assertNotNull(saved.getUpdatedAt(), "updatedAt should be set when stopping");

        // If your toDTO() sets these fields, assert them too
        Assertions.assertEquals(HttpStatus.OK, response.statusCode());

        sessionMemoryRepository.delete(active);
    }

    @Test
    void stopSession_alreadyStopped_isIdempotent_andDoesNotResave() {
        // given
        UUID generatedId = UUID.randomUUID();
        String sessionId = generatedId.toString();

        Instant originalEndedAt = Instant.now().minusSeconds(600);
        Instant originalUpdatedAt = Instant.now().minusSeconds(600);

        SessionMemoryEntity stopped = buildSession(
                SessionState.STOPPED,
                Instant.now().minusSeconds(900),
                originalEndedAt
        );
        stopped.setSessionId(generatedId);
        stopped.setUpdatedAt(originalUpdatedAt);

        when(sessionMemoryRepository.findById(sessionId))
                .thenReturn(Optional.of(stopped));

        when(sessionMemoryRepository.findBySessionIdAndPrincipalTypeAndPrincipalId(generatedId, PrincipalType.GUEST, "guest-key-123"))
                .thenReturn(Optional.of(stopped));

        UpdateSessionRequestDTO dto = buildUpdateRequest(sessionId);

        // when
        Executable exec = () -> sessionService.stopSessionMemory(
                PrincipalType.GUEST,
                "guest-key-123",
                dto
        );

        // then
        ResponseStatusException ex = Assertions.assertThrows(
                ResponseStatusException.class,
                exec
        );

    }

    @Test
    void stopSession_nonExistent_throwsNotFound() {
        // given
        UUID generatedId = UUID.randomUUID();
        String sessionId = generatedId.toString();

        when(sessionMemoryRepository.findById(sessionId))
                .thenReturn(Optional.empty());

        when(sessionMemoryRepository.findBySessionIdAndPrincipalTypeAndPrincipalId(generatedId, PrincipalType.GUEST, "guest-key-123"))
                .thenReturn(Optional.empty());

        UpdateSessionRequestDTO dto = buildUpdateRequest(sessionId);

        // when
        Executable exec = () -> sessionService.stopSessionMemory(
                PrincipalType.GUEST,
                "guest-key-123",
                dto
        );

        // then
        ResponseStatusException ex = Assertions.assertThrows(
                ResponseStatusException.class,
                exec
        );
        Assertions.assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void stopSession_invalidState_throwsConflict() {
        // given
        UUID generatedId = UUID.randomUUID();
        String sessionId = generatedId.toString();

        SessionMemoryEntity expired = buildSession(
                SessionState.EXPIRED,
                Instant.now().minusSeconds(3600),
                Instant.now().minusSeconds(1800)
        );
        expired.setSessionId(generatedId);

        when(sessionMemoryRepository.findById(sessionId))
                .thenReturn(Optional.of(expired));

        when(sessionMemoryRepository.findBySessionIdAndPrincipalTypeAndPrincipalId(generatedId, PrincipalType.GUEST, "guest-key-123"))
                .thenReturn(Optional.of(expired));

        UpdateSessionRequestDTO dto = buildUpdateRequest(sessionId);

        // when
        Executable exec = () -> sessionService.stopSessionMemory(
                PrincipalType.GUEST,
                "guest-key-123",
                dto
        );

        // then
        ResponseStatusException ex = Assertions.assertThrows(
                ResponseStatusException.class,
                exec
        );

        // If you used CONFLICT (recommended), assert that
        Assertions.assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }
}
