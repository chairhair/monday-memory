package com.monday.monday_backend.service.DevBypassTests;

import com.monday.monday_backend.memory.entity.SessionMemoryEntity;
import com.monday.monday_backend.memory.repo.SessionMemoryRepository;
import com.monday.monday_backend.memory.service.SessionService;
import com.monday.shared.memory.session.dto.SessionMemoryResponseDTO;
import com.monday.shared.memory.session.dto.UpdateSessionRequestDTO;
import com.monday.shared.memory.session.utils.PrincipalType;
import com.monday.shared.memory.session.utils.SessionState;
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

import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
class DevBypassStopSessionTests {

    @Mock
    private SessionMemoryRepository sessionMemoryRepository;

    @InjectMocks
    private SessionService sessionService;

    // Helper to build an entity; adjust to match your actual constructor/setters
    private SessionMemoryEntity buildSession(String sessionId, SessionState state,
                                             Instant lastOccurredAt, Instant endedAt) {
        SessionMemoryEntity entity = new SessionMemoryEntity();
        // If you don't have setters, replace with builder / constructor you *do* have
        entity.setSessionState(state);
        entity.setLastOccurredAt(lastOccurredAt);
        entity.setEndedAt(endedAt);
        entity.setUpdatedAt(Instant.now().minusSeconds(3600)); // initial value
        return entity;
    }

    private UpdateSessionRequestDTO buildUpdateRequest(String sessionId) {
        // Adjust ctor if your DTO is different
        return new UpdateSessionRequestDTO(sessionId, "guest-key-123");
    }

    @Test
    void stopSession_activeSession_becomesStopped_andPersists() {
        // given
        String sessionId = "session-123";
        SessionMemoryEntity active = buildSession(
                sessionId,
                SessionState.ACTIVE,
                Instant.now().minusSeconds(300),
                null
        );

        when(sessionMemoryRepository.findById(sessionId))
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
    }

    @Test
    void stopSession_alreadyStopped_isIdempotent_andDoesNotResave() {
        // given
        String sessionId = "session-456";
        Instant originalEndedAt = Instant.now().minusSeconds(600);
        Instant originalUpdatedAt = Instant.now().minusSeconds(600);

        SessionMemoryEntity stopped = buildSession(
                sessionId,
                SessionState.STOPPED,
                Instant.now().minusSeconds(900),
                originalEndedAt
        );
        stopped.setUpdatedAt(originalUpdatedAt);

        when(sessionMemoryRepository.findById(sessionId))
                .thenReturn(Optional.of(stopped));

        UpdateSessionRequestDTO dto = buildUpdateRequest(sessionId);

        // when
        SessionMemoryResponseDTO response = sessionService.stopSessionMemory(
                PrincipalType.GUEST,
                "guest-key-123",
                dto
        );

        // then
        // Idempotent STOP: should *not* call save again in many designs.
        // If your implementation *does* save, loosen this accordingly.
        verify(sessionMemoryRepository, never()).save(any(SessionMemoryEntity.class));

        Assertions.assertEquals(SessionState.STOPPED, stopped.getSessionState());
        Assertions.assertEquals(HttpStatus.OK, response.statusCode());
        // Optional: keep endedAt / updatedAt untouched on idempotent call
        Assertions.assertEquals(originalEndedAt, stopped.getEndedAt(),
                "endedAt should not change on idempotent STOP");
    }

    @Test
    void stopSession_nonExistent_throwsNotFound() {
        // given
        String sessionId = "missing-session";
        when(sessionMemoryRepository.findById(sessionId))
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
        String sessionId = "session-expired";
        SessionMemoryEntity expired = buildSession(
                sessionId,
                SessionState.EXPIRED,
                Instant.now().minusSeconds(3600),
                Instant.now().minusSeconds(1800)
        );

        when(sessionMemoryRepository.findById(sessionId))
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
