package com.monday.monday_backend.memory.entity;

import com.monday.monday_backend.auth.guests.GuestService;
import com.monday.monday_backend.auth.principal.PrincipalContext;
import com.monday.monday_backend.memory.repo.SessionMemoryRepository;
import com.monday.monday_backend.memory.service.MemoryAggregationService;
import com.monday.monday_backend.memory.service.SessionService;
import com.monday.monday_backend.memory.utils.MemoryChunkUtils;
import com.monday.shared.auth.utils.AccessLevel;
import com.monday.shared.memory.plan.EffectivePlan;
import com.monday.shared.memory.session.dto.CreateSessionRequestDTO;
import com.monday.shared.memory.session.dto.SessionMemoryResponseDTO;
import com.monday.shared.memory.session.dto.SessionOptionRequestDTO;
import com.monday.shared.memory.session.utils.PrincipalType;
import com.monday.shared.memory.session.utils.SessionScope;
import com.monday.shared.memory.session.utils.SessionSource;
import com.monday.shared.memory.session.utils.SessionState;
import com.monday.shared.recording.RecordingScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for SessionService.createOrReuseSession focusing on SessionOptions wiring.
 */
@ExtendWith(MockitoExtension.class)
class SessionServiceTests {

    @Mock
    private SessionMemoryRepository sessionMemoryRepository;

    @Mock
    private MemoryAggregationService memoryAggregationService;

    @Mock
    private MemoryChunkUtils memoryChunkUtils;


    // Add any other deps your SessionService needs and mock them here.
    // e.g. @Mock private UserService userService;

    private SessionService sessionService;

    @BeforeEach
    void setUp() {
        // Adjust constructor args to match your actual SessionService.
        // If there are more deps, pass the mocks here.
        sessionService = new SessionService(sessionMemoryRepository, memoryAggregationService, memoryChunkUtils);
    }

    private PrincipalContext buildPrincipal(UUID principalId) {
        return PrincipalContext.builder()
                .principalId(principalId)
                .principalType(PrincipalType.USER)
                .accessLevel(AccessLevel.USER)
                .plan(EffectivePlan.USER_FREE)
                .quota(null)
                .recordingScope(RecordingScope.PRIVATE)
                .recallScope(null)
                .user(null)              // or a fake User if your code expects it
                .externalGuestKey(null)
                .build();
    }

    @Test
    void createSession_withOptions_persistsOptions() {
        // Arrange
        UUID principalId = UUID.randomUUID();
        PrincipalContext principal = buildPrincipal(principalId);

        CreateSessionRequestDTO request = new CreateSessionRequestDTO(
                "hawk-tuah-guest",          // guestKey
                "Test Topic",               // topicName
                SessionSource.DISCORD,      // source
                "channel-123",              // sourceConversationKey
                RecordingScope.PRIVATE,     // recording scope
                new SessionOptionRequestDTO(
                        SessionScope.CHANNEL, // session scope
                        42                     // maxChunksPerSession
                )
        );

        ArgumentCaptor<SessionMemoryEntity> entityCaptor =
                ArgumentCaptor.forClass(SessionMemoryEntity.class);

        UUID generatedSessionId = UUID.randomUUID();

        when(sessionMemoryRepository.saveAndFlush(any(SessionMemoryEntity.class)))
                .thenAnswer(invocation -> {
                    SessionMemoryEntity e = invocation.getArgument(0);
                    // simulate DB assigning an ID
                    e.setSessionId(generatedSessionId);
                    return e;
                });

        // Act
        SessionMemoryResponseDTO response =
                sessionService.createOrReuseSession(principal, request, "idem-key-123");

        // Assert: repository interaction
        verify(sessionMemoryRepository).saveAndFlush(entityCaptor.capture());
        SessionMemoryEntity saved = entityCaptor.getValue();

        assertEquals(PrincipalType.USER, saved.getPrincipalType());
        assertEquals(principalId.toString(), saved.getPrincipalId());
        assertEquals(SessionSource.DISCORD, saved.getSource());
        assertEquals("channel-123", saved.getSourceConversation());
        assertEquals(0, saved.getChunkCount());
        assertEquals(SessionState.ACTIVE, saved.getSessionState());

        assertNotNull(saved.getOptions(), "SessionOptionsEntity must not be null");
        assertEquals(SessionScope.CHANNEL, saved.getOptions().getScope());
        assertEquals(42, saved.getOptions().getMaxChunksPerSession());

        // Assert: DTO returned
        assertEquals(HttpStatus.OK, response.statusCode());
        assertEquals(RecordingScope.PRIVATE, response.scope());
        assertEquals("Saved Session Memory Successfully", response.message());
        assertEquals(generatedSessionId.toString(), response.sessionIds().get(0));
    }

    @Test
    void createSession_withoutOptions_usesDefaults() {
        // Arrange
        UUID principalId = UUID.randomUUID();
        PrincipalContext principal = buildPrincipal(principalId);

        CreateSessionRequestDTO request = new CreateSessionRequestDTO(
                "hawk-tuah-guest",
                "Test Topic",
                SessionSource.DISCORD,
                "channel-123",
                RecordingScope.PRIVATE,
                null // <- no options provided
        );

        ArgumentCaptor<SessionMemoryEntity> entityCaptor =
                ArgumentCaptor.forClass(SessionMemoryEntity.class);

        UUID generatedSessionId = UUID.randomUUID();

        when(sessionMemoryRepository.saveAndFlush(any(SessionMemoryEntity.class)))
                .thenAnswer(invocation -> {
                    SessionMemoryEntity e = invocation.getArgument(0);
                    e.setSessionId(generatedSessionId);
                    return e;
                });

        // Act
        SessionMemoryResponseDTO response =
                sessionService.createOrReuseSession(principal, request, null);

        // Assert
        verify(sessionMemoryRepository).saveAndFlush(entityCaptor.capture());
        SessionMemoryEntity saved = entityCaptor.getValue();

        assertNotNull(saved.getOptions(), "SessionOptionsEntity must not be null");

        // Your defaults from the implementation:
        assertEquals(SessionScope.CHANNEL, saved.getOptions().getScope());
        assertEquals(10, saved.getOptions().getMaxChunksPerSession());

        assertEquals(HttpStatus.OK, response.statusCode());
        assertEquals(RecordingScope.PRIVATE, response.scope());
        assertEquals("Saved Session Memory Successfully", response.message());
        assertEquals(generatedSessionId.toString(), response.sessionIds().get(0));
    }

    @Test
    void createSession_idempotencyConflict_returnsExisting() {
        // Arrange
        UUID principalId = UUID.randomUUID();
        PrincipalContext principal = buildPrincipal(principalId);

        String idempotencyKey = "idem-key-123";

        CreateSessionRequestDTO request = new CreateSessionRequestDTO(
                "hawk-tuah-guest",
                "Test Topic",
                SessionSource.DISCORD,
                "channel-123",
                RecordingScope.PRIVATE,
                null
        );

        // Simulate DB unique constraint violation on save
        when(sessionMemoryRepository.saveAndFlush(any(SessionMemoryEntity.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        SessionMemoryEntity existing = new SessionMemoryEntity();
        existing.setSessionId(UUID.randomUUID());
        existing.setScope(request.scope());
        existing.setPrincipalType(PrincipalType.USER);
        existing.setPrincipalId(principalId.toString());

        SessionOptionsEntity options = new SessionOptionsEntity();
        options.setScope(SessionScope.CHANNEL);
        options.setMaxChunksPerSession(10);
        existing.setOptions(options);

        when(sessionMemoryRepository
                .findByPrincipalIdAndIdempotencyKey(principalId.toString(), idempotencyKey))
                .thenReturn(Optional.of(existing));

        // Act
        SessionMemoryResponseDTO response =
                sessionService.createOrReuseSession(principal, request, idempotencyKey);

        // Assert
        assertEquals(HttpStatus.CONFLICT, response.statusCode());
        assertEquals(RecordingScope.PRIVATE, response.scope());
        assertEquals("Cannot create a new session memory when one is recording", response.message());
        assertEquals(existing.getSessionId().toString(), response.sessionIds().get(0));
    }
}
