package com.monday.monday_backend.memory.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.monday.monday_backend.auth.principal.PrincipalContext;
import com.monday.monday_backend.llm.LlmClient;
import com.monday.monday_backend.memory.repo.MemoryChunkRepository;
import com.monday.monday_backend.memory.service.MemoryAggregationService;
import com.monday.monday_backend.memory.service.MemoryService;
import com.monday.monday_backend.memory.service.QuotaService;
import com.monday.monday_backend.memory.service.SessionService;
import com.monday.monday_backend.memory.utils.MemoryChunkUtils;
import com.monday.shared.auth.utils.AccessLevel;
import com.monday.shared.memory.dto.RequestMemoryChunkDTO;
import com.monday.shared.memory.dto.ResponseMemoryChunkDTO;
import com.monday.shared.memory.plan.EffectivePlan;
import com.monday.shared.memory.quota.QuotaDecision;
import com.monday.shared.memory.quota.QuotaSnapshot;
import com.monday.shared.memory.session.dto.SessionMemoryResponseDTO;
import com.monday.shared.memory.session.utils.PrincipalType;
import com.monday.shared.memory.session.utils.SessionScope;
import com.monday.shared.memory.session.utils.SessionSource;
import com.monday.shared.memory.session.utils.SessionState;
import com.monday.shared.recording.RecordingScope;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MemoryServiceTests {

    @Mock
    private SessionService sessionService;

    @Mock
    private MemoryChunkRepository memoryChunkRepository;

    @Mock
    private LlmClient llmClient;

    @Mock
    private MemoryAggregationService memoryAggregationService;

    @Mock
    private QuotaService quotaService;

    private MemoryService memoryService;
    private MemoryChunkUtils memoryChunkUtils;

    @BeforeEach
    void setUp() {
        memoryChunkUtils = new MemoryChunkUtils(new ObjectMapper());
        memoryService = new MemoryService(sessionService, quotaService, memoryChunkRepository, memoryAggregationService, memoryChunkUtils, llmClient);
    }

    private MemoryChunkEntity buildMemory(String content, String guestKey, SessionMemoryEntity session) {
        MemoryChunkEntity chunk = new MemoryChunkEntity();
        chunk.setMemoryId(UUID.randomUUID());
        chunk.setSession(session);
        chunk.setOccurredAt(Instant.now());
        chunk.setIngestedAt(Instant.now());
        chunk.setTags(null);
        chunk.setContent(Map.of(
                "body", content,
                "principalType", session.getPrincipalType(),
                "principalId", (session.getPrincipalId() == null) ? "" : session.getPrincipalId(),
                "guestKey", guestKey,
                "source", session.getSource(),
                "sourceConversationKey", session.getSourceConversation()
        ));
        String normalized = normalize(content);
        chunk.setHashSha256(sha256(normalized));

        return chunk;
    }

    // NOTE: Works as anticipated
    @Test
    void appendChunk_happyPath() {
        // Creates memory chunk that's present under our Session Memory Controller
        QuotaSnapshot quotaSnapshot = QuotaSnapshot.builder()
                .topicsUsed(0)
                .tokensUsed(0)
                .topicLimit(100)
                .tokenLimit(10000L)
                .build();

        UUID sessionId = UUID.randomUUID();
        UUID principalId = UUID.randomUUID();

        RequestMemoryChunkDTO dto = new RequestMemoryChunkDTO(
                sessionId,
                PrincipalType.USER,
                principalId.toString(),
                "hawk-tauh-man",
                SessionSource.DISCORD,
                "hawk-tauh-man",
                "Hello, this is a test message",
                Instant.now(),
                null
        );

        PrincipalContext guestContext = PrincipalContext.builder()
                .principalId(principalId)                        // ✅ NON-NULL
                .principalType(PrincipalType.USER)
                .accessLevel(AccessLevel.USER)
                .plan(EffectivePlan.USER_FREE)
                .quota(quotaSnapshot)
                .recordingScope(RecordingScope.PRIVATE)
                .recallScope(null)
                .externalGuestKey("hawk-tuah-man")
                .build();
        SessionOptionsEntity options = new SessionOptionsEntity();
        options.setScope(SessionScope.CHANNEL);
        options.setMaxChunksPerSession(10);

        SessionMemoryEntity session = new SessionMemoryEntity();
        session.setSessionId(sessionId);
        session.setPrincipalType(PrincipalType.USER);
        session.setPrincipalId(principalId.toString());
        session.setSessionState(SessionState.ACTIVE);
        session.setChunkCount(0);
        session.setOptions(options);

        when(quotaService.decide(eq(quotaSnapshot))).thenReturn(QuotaDecision.ALLOW);
        when(quotaService.snapshotFor(any())).thenReturn(quotaSnapshot);
        when(sessionService.getSessionPresent(any(), any(), eq(false))).thenReturn(session);

        // memoryChunkRepository.save returns the entity we give it
        // you can capture it to assert fields:
        ArgumentCaptor<MemoryChunkEntity> chunkCaptor = ArgumentCaptor.forClass(MemoryChunkEntity.class);
        when(memoryChunkRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ResponseMemoryChunkDTO response = memoryService.recordOnly(guestContext, dto);

        // Assert
        // session updated in memory
        Assertions.assertEquals(1, session.getChunkCount());

        // chunk saved with correct fields
        verify(memoryChunkRepository).save(chunkCaptor.capture());
        MemoryChunkEntity savedChunk = chunkCaptor.getValue();

        Assertions.assertEquals(sessionId, savedChunk.getSession().getSessionId());
        Assertions.assertEquals(dto.content(), savedChunk.getContent().get("body"));

        // service returned mapped DTO
        Assertions.assertNotNull(response);
        Assertions.assertEquals(dto.content(), response.content());
        Assertions.assertEquals(dto.principalId(), response.principalId());
    }

    @Test
    void appendChunk_forbidden() {
        // Arrange
        QuotaSnapshot quotaSnapshot = QuotaSnapshot.builder()
                .topicsUsed(0)
                .tokenLimit(0)
                .topicLimit(100)
                .tokenLimit(10000L)
                .build();

        UUID sessionId = UUID.randomUUID();
        UUID principalId = UUID.randomUUID();
        UUID fakePrincipalId = UUID.randomUUID();

        RequestMemoryChunkDTO dto = new RequestMemoryChunkDTO(
                sessionId,
                PrincipalType.GUEST,
                principalId.toString(),
                "hawk-tauh-man",
                SessionSource.DISCORD,
                "hawk-tauh-man",
                "Hello, this is a test message",
                Instant.now(),
                null
        );

        PrincipalContext guestContext = PrincipalContext.builder()
                .principalId(principalId)                        // ✅ NON-NULL
                .principalType(PrincipalType.USER)
                .accessLevel(AccessLevel.USER)
                .plan(EffectivePlan.USER_FREE)
                .quota(quotaSnapshot)
                .recordingScope(RecordingScope.PRIVATE)
                .recallScope(null)
                .externalGuestKey("hawk-tuah-man")
                .build();

        SessionOptionsEntity options = new SessionOptionsEntity();
        options.setScope(SessionScope.CHANNEL);
        options.setMaxChunksPerSession(10);

        SessionMemoryEntity session = new SessionMemoryEntity();
        session.setSessionId(sessionId);
        session.setPrincipalType(PrincipalType.USER);
        session.setPrincipalId(fakePrincipalId.toString());
        session.setSessionState(SessionState.ACTIVE);
        session.setChunkCount(0);
        session.setOptions(options);

        when(sessionService.getSessionPresent(any(), any(), eq(false))).thenReturn(null);
        when(sessionService.createOrReuseSession(any(), any(), any())).thenReturn(new SessionMemoryResponseDTO(HttpStatus.FORBIDDEN, null, null, null, null, null, null, null, null));

        // Act + Assert
        Assertions.assertThrows(ResponseStatusException.class, () -> {
            memoryService.recordOnly(guestContext, dto);
        });

        // Ensure nothing was saved
        verify(memoryChunkRepository, never()).save(any());
    }

    @Test
    void appendChunk_wrongPrincipal() {
        // Arrange
        QuotaSnapshot quotaSnapshot = QuotaSnapshot.builder()
                .topicsUsed(0)
                .tokenLimit(0)
                .topicLimit(100)
                .tokenLimit(10000L)
                .build();

        UUID sessionId = UUID.randomUUID();
        UUID principalId = UUID.randomUUID();

        RequestMemoryChunkDTO dto = new RequestMemoryChunkDTO(
                sessionId,
                PrincipalType.GUEST,
                principalId.toString(),
                "hawk-tauh-man",
                SessionSource.DISCORD,
                "hawk-tauh-man",
                "Hello, this is a test message",
                Instant.now(),
                null
        );

        SessionOptionsEntity options = new SessionOptionsEntity();
        options.setScope(SessionScope.CHANNEL);
        options.setMaxChunksPerSession(10);

        PrincipalContext guestContext = PrincipalContext.builder()
                .principalId(principalId)                        // ✅ NON-NULL
                .principalType(PrincipalType.USER)
                .accessLevel(AccessLevel.USER)
                .plan(EffectivePlan.USER_FREE)
                .quota(quotaSnapshot)
                .recordingScope(RecordingScope.PRIVATE)
                .recallScope(null)
                .externalGuestKey("hawk-tuah-man")
                .build();

        UUID wrongPrincipalId = UUID.randomUUID();

        SessionMemoryEntity session = new SessionMemoryEntity();
        session.setSessionId(sessionId);
        session.setPrincipalType(PrincipalType.USER);
        session.setPrincipalId(wrongPrincipalId.toString());
        session.setSessionState(SessionState.ACTIVE);
        session.setChunkCount(0);
        session.setOptions(options);

        when(sessionService.getSessionPresent(any(), any(), eq(false))).thenReturn(session);

        // Act + Assert
        Assertions.assertThrows(ResponseStatusException.class, () -> {
            memoryService.recordOnly(guestContext, dto);
        });

        // Ensure nothing was saved
        verify(memoryChunkRepository, never()).save(any());
    }

    @Test
    void appendChunk_sessionClosed_rejected() {
        // Arrange
        QuotaSnapshot quotaSnapshot = QuotaSnapshot.builder()
                .topicsUsed(0)
                .tokenLimit(0)
                .topicLimit(100)
                .tokenLimit(10000L)
                .build();

        UUID sessionId = UUID.randomUUID();
        UUID principalId = UUID.randomUUID();

        RequestMemoryChunkDTO dto = new RequestMemoryChunkDTO(
                sessionId,
                PrincipalType.GUEST,
                principalId.toString(),
                "hawk-tauh-man",
                SessionSource.DISCORD,
                "hawk-tauh-man",
                "Hello, this is a test message",
                Instant.now(),
                null
        );

        PrincipalContext guestContext = PrincipalContext.builder()
                .principalId(principalId)                        // ✅ NON-NULL
                .principalType(PrincipalType.USER)
                .accessLevel(AccessLevel.USER)
                .plan(EffectivePlan.USER_FREE)
                .quota(quotaSnapshot)
                .recordingScope(RecordingScope.PRIVATE)
                .recallScope(null)
                .externalGuestKey("hawk-tuah-man")
                .build();
        SessionOptionsEntity options = new SessionOptionsEntity();
        options.setScope(SessionScope.CHANNEL);
        options.setMaxChunksPerSession(10);

        SessionMemoryEntity session = new SessionMemoryEntity();
        session.setSessionId(sessionId);
        session.setPrincipalType(PrincipalType.USER);
        session.setPrincipalId(principalId.toString());
        session.setSessionState(SessionState.STOPPED);
        session.setChunkCount(0);
        session.setOptions(options);

        when(sessionService.getSessionPresent(any(), any(), eq(false))).thenReturn(session);

        // Act + Assert
        Assertions.assertThrows(ResponseStatusException.class, () -> {
            memoryService.recordOnly(guestContext, dto);
        });

        // Ensure nothing was saved
        verify(memoryChunkRepository, never()).save(any());
    }

    @Test
    void appendChunk_maxChunksExceeded_sessionFull() {
        // Arrange
        QuotaSnapshot quotaSnapshot = QuotaSnapshot.builder()
                .topicsUsed(0)
                .tokensUsed(0L)
                .topicLimit(100)
                .tokenLimit(10000L)
                .build();

        UUID sessionId = UUID.randomUUID();
        UUID principalId = UUID.randomUUID();

        RequestMemoryChunkDTO dto = new RequestMemoryChunkDTO(
                sessionId,
                PrincipalType.GUEST,
                principalId.toString(),
                "hawk-tauh-man",
                SessionSource.DISCORD,
                "hawk-tauh-man",
                "Hello, this is a test message",
                Instant.now(),
                null
        );

        PrincipalContext guestContext = PrincipalContext.builder()
                .principalId(principalId)                        // ✅ NON-NULL
                .principalType(PrincipalType.USER)
                .accessLevel(AccessLevel.USER)
                .plan(EffectivePlan.USER_FREE)
                .quota(quotaSnapshot)
                .recordingScope(RecordingScope.PRIVATE)
                .recallScope(null)
                .externalGuestKey("hawk-tuah-man")
                .build();
        SessionOptionsEntity options = new SessionOptionsEntity();
        options.setScope(SessionScope.CHANNEL);
        options.setMaxChunksPerSession(10);

        SessionMemoryEntity session = new SessionMemoryEntity();
        session.setSessionId(sessionId);
        session.setPrincipalType(PrincipalType.USER);
        session.setPrincipalId(principalId.toString());
        session.setSessionState(SessionState.ACTIVE);
        session.setChunkCount(10);
        session.setOptions(options);

        when(quotaService.snapshotFor(any())).thenReturn(quotaSnapshot);
        when(sessionService.getSessionPresent(any(), any(), eq(false))).thenReturn(session);

        // Act + Assert
        Assertions.assertThrows(ResponseStatusException.class, () -> {
            memoryService.recordOnly(guestContext, dto);
        });

        // Ensure nothing was saved
        verify(memoryChunkRepository, never()).save(any());
    }

    @Test
    void appendChunk_tokensExceeded_sessionFull() {
        // Arrange
        QuotaSnapshot quotaSnapshot = QuotaSnapshot.builder()
                .topicsUsed(0)
                .tokensUsed(10001L)
                .topicLimit(100)
                .tokenLimit(10000L)
                .build();

        UUID sessionId = UUID.randomUUID();
        UUID principalId = UUID.randomUUID();

        RequestMemoryChunkDTO dto = new RequestMemoryChunkDTO(
                sessionId,
                PrincipalType.GUEST,
                principalId.toString(),
                "hawk-tauh-man",
                SessionSource.DISCORD,
                "hawk-tauh-man",
                "Hello, this is a test message",
                Instant.now(),
                null
        );

        PrincipalContext guestContext = PrincipalContext.builder()
                .principalId(principalId)                        // ✅ NON-NULL
                .principalType(PrincipalType.USER)
                .accessLevel(AccessLevel.USER)
                .plan(EffectivePlan.USER_FREE)
                .quota(quotaSnapshot)
                .recordingScope(RecordingScope.PRIVATE)
                .recallScope(null)
                .externalGuestKey("hawk-tuah-man")
                .build();
        SessionOptionsEntity options = new SessionOptionsEntity();
        options.setScope(SessionScope.CHANNEL);
        options.setMaxChunksPerSession(10);

        SessionMemoryEntity session = new SessionMemoryEntity();
        session.setSessionId(sessionId);
        session.setPrincipalType(PrincipalType.USER);
        session.setPrincipalId(principalId.toString());
        session.setSessionState(SessionState.ACTIVE);
        session.setChunkCount(0);
        session.setOptions(options);

        when(sessionService.getSessionPresent(any(), any(), eq(false))).thenReturn(session);
        when(quotaService.decide(eq(quotaSnapshot))).thenReturn(QuotaDecision.BLOCK);
        when(quotaService.snapshotFor(any())).thenReturn(quotaSnapshot);

        // Act + Assert
        Assertions.assertThrows(ResponseStatusException.class, () -> {
            memoryService.recordOnly(guestContext, dto);
        });

        // Ensure nothing was saved
        verify(memoryChunkRepository, never()).save(any());
    }

    @Test
    void appendChunk_nullOptionsAndQuotaSnapshot_throws() {
        UUID sessionId = UUID.randomUUID();
        UUID principalId = UUID.randomUUID();

        RequestMemoryChunkDTO dto = new RequestMemoryChunkDTO(
                sessionId,
                PrincipalType.GUEST,
                principalId.toString(),
                "hawk-tauh-man",
                SessionSource.DISCORD,
                "hawk-tauh-man",
                "Hello, this is a test message",
                Instant.now(),
                null
        );

        PrincipalContext guestContext = PrincipalContext.builder()
                .principalId(principalId)                        // ✅ NON-NULL
                .principalType(PrincipalType.USER)
                .accessLevel(AccessLevel.USER)
                .plan(EffectivePlan.USER_FREE)
                .quota(null)
                .recordingScope(RecordingScope.PRIVATE)
                .recallScope(null)
                .externalGuestKey("hawk-tuah-man")
                .build();
        SessionOptionsEntity options = new SessionOptionsEntity();
        options.setScope(SessionScope.CHANNEL);
        options.setMaxChunksPerSession(10);

        SessionMemoryEntity session = new SessionMemoryEntity();
        session.setSessionId(sessionId);
        session.setPrincipalType(PrincipalType.USER);
        session.setPrincipalId(principalId.toString());
        session.setSessionState(SessionState.ACTIVE);
        session.setChunkCount(0);
        session.setOptions(null);

        when(sessionService.getSessionPresent(any(), any(), eq(false))).thenReturn(session);
        when(quotaService.snapshotFor(any())).thenReturn(null);

        // Act + Assert
        Assertions.assertThrows(ResponseStatusException.class, () -> {
            memoryService.recordOnly(guestContext, dto);
        });

        // Ensure nothing was saved
        verify(memoryChunkRepository, never()).save(any());
    }

    private String normalize(String text) {
        if (text == null) return "";
        return text
                .trim()
                .replaceAll("\\s+", " ");
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm missing", e);
        }
    }

}
