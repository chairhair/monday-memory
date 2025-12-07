package com.monday.monday_backend.memory.entity;

import com.monday.monday_backend.auth.principal.PrincipalContext;
import com.monday.monday_backend.auth.principal.PrincipalResolver;
import com.monday.monday_backend.memory.repo.SessionMemoryRepository;
import com.monday.monday_backend.memory.service.SessionService;
import com.monday.shared.auth.utils.AccessLevel;
import com.monday.shared.memory.plan.EffectivePlan;
import com.monday.shared.memory.session.dto.SessionMemoryResponseDTO;
import com.monday.shared.memory.session.utils.PrincipalType;
import com.monday.shared.memory.session.utils.SessionSource;
import com.monday.shared.memory.session.utils.SessionState;
import com.monday.shared.recording.RecordingScope;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StopSessionTests {

    @Mock
    private SessionMemoryRepository sessionMemoryRepository;

    @MockBean
    private PrincipalResolver principalResolver;

    @InjectMocks
    private SessionService sessionService;

    // --- helpers ----------------------------------------------------------

    private PrincipalContext guestContext(UUID principalId) {
        return PrincipalContext.builder()
                .principalId(principalId)                // guest: using guestKey for principalId in entity
                .principalType(PrincipalType.GUEST)
                .accessLevel(AccessLevel.GUEST)
                .plan(EffectivePlan.GUEST_FREE)
                .quota(null)
                .recordingScope(RecordingScope.PRIVATE)
                .recallScope(null)
                .build();
    }

    private SessionMemoryEntity buildSession(SessionState state,
                                             Instant lastOccurredAt,
                                             Instant endedAt) {
        SessionMemoryEntity entity = new SessionMemoryEntity();
        entity.setSource(SessionSource.DISCORD);
        entity.setSourceConversation("hawk-tuah-man");
        entity.setPrincipalType(PrincipalType.GUEST);
        entity.setPrincipalId(UUID.randomUUID().toString());
        entity.setScope(RecordingScope.PRIVATE);
        entity.setChunkCount(0);
        entity.setSessionState(state);
        entity.setLastOccurredAt(lastOccurredAt);
        entity.setEndedAt(endedAt);
        entity.setCreatedAt(Instant.now().minusSeconds(7200));
        entity.setUpdatedAt(Instant.now().minusSeconds(3600));
        entity.setIdempotencyKey("test-idemp-key");
        return entity;
    }

    // --- tests ------------------------------------------------------------

    @Test
    void stopSession_activeSession_becomesStopped_andPersists() {
        // given
        UUID principalId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        SessionMemoryEntity active = buildSession(
                SessionState.ACTIVE,
                Instant.now().minusSeconds(300),
                null
        );
        active.setPrincipalId(principalId.toString());
        active.setSessionId(sessionId);

        when(sessionMemoryRepository.findBySessionIdAndPrincipalTypeAndPrincipalId(
                sessionId,
                PrincipalType.GUEST,
                principalId.toString()
        )).thenReturn(Optional.of(active));

        PrincipalContext ctx = guestContext(principalId);

        when(sessionMemoryRepository.save(any(SessionMemoryEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // when
        SessionMemoryResponseDTO response =
                sessionService.stopSessionState(sessionId, ctx);

        // then
        ArgumentCaptor<SessionMemoryEntity> captor =
                ArgumentCaptor.forClass(SessionMemoryEntity.class);
        verify(sessionMemoryRepository).save(captor.capture());

        SessionMemoryEntity saved = captor.getValue();

        Assertions.assertEquals(SessionState.STOPPED, saved.getSessionState(),
                "Session state should transition to STOPPED");
        Assertions.assertNotNull(saved.getEndedAt(), "endedAt should be set when stopping");
        Assertions.assertNotNull(saved.getUpdatedAt(), "updatedAt should be updated when stopping");

        Assertions.assertEquals(HttpStatus.OK, response.statusCode());
    }

    @Test
    void stopSession_alreadyStopped_isIdempotent_orThrowsConflict() {
        // given
        UUID principalId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        Instant originalEndedAt = Instant.now().minusSeconds(600);
        Instant originalUpdatedAt = Instant.now().minusSeconds(600);

        SessionMemoryEntity stopped = buildSession(
                SessionState.STOPPED,
                Instant.now().minusSeconds(900),
                originalEndedAt
        );
        stopped.setPrincipalId(principalId.toString());
        stopped.setSessionId(sessionId);
        stopped.setUpdatedAt(originalUpdatedAt);

        PrincipalContext ctx = guestContext(principalId);

        when(sessionMemoryRepository.findBySessionIdAndPrincipalTypeAndPrincipalId(
                sessionId,
                PrincipalType.GUEST,
                principalId.toString()
        )).thenReturn(Optional.of(stopped));

        // when
        Executable exec = () -> sessionService.stopSessionState(sessionId, ctx);

        // then – depending on semantics, adjust this section

        ResponseStatusException ex = Assertions.assertThrows(
                ResponseStatusException.class,
                exec,
                "Stopping an already STOPPED session should fail, not silently pass"
        );
        Assertions.assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void stopSession_nonExistent_throwsNotFound() {
        // given
        UUID principalId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        PrincipalContext ctx = guestContext(principalId);

        when(sessionMemoryRepository.findBySessionIdAndPrincipalTypeAndPrincipalId(
                sessionId,
                PrincipalType.GUEST,
                principalId.toString()
        )).thenReturn(Optional.empty());

        // when
        Executable exec = () -> sessionService.stopSessionState(sessionId, ctx);

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
        UUID principalId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        SessionMemoryEntity expired = buildSession(
                SessionState.EXPIRED,
                Instant.now().minusSeconds(3600),
                Instant.now().minusSeconds(1800)
        );
        expired.setPrincipalId(principalId.toString());
        expired.setSessionId(sessionId);

        PrincipalContext ctx = guestContext(principalId);

        when(sessionMemoryRepository.findBySessionIdAndPrincipalTypeAndPrincipalId(
                sessionId,
                PrincipalType.GUEST,
                principalId.toString()
        )).thenReturn(Optional.of(expired));

        // when
        Executable exec = () -> sessionService.stopSessionState(sessionId, ctx);

        // then
        ResponseStatusException ex = Assertions.assertThrows(
                ResponseStatusException.class,
                exec
        );
        Assertions.assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }
}
