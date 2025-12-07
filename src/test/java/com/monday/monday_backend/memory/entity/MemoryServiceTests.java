package com.monday.monday_backend.memory.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.monday.monday_backend.auth.principal.PrincipalContext;
import com.monday.monday_backend.memory.repo.MemoryChunkRepository;
import com.monday.monday_backend.memory.repo.SessionMemoryRepository;
import com.monday.monday_backend.memory.service.MemoryService;
import com.monday.monday_backend.memory.service.SessionService;
import com.monday.shared.auth.utils.AccessLevel;
import com.monday.shared.memory.dto.RequestMemoryChunkDTO;
import com.monday.shared.memory.dto.ResponseMemoryChunkDTO;
import com.monday.shared.memory.plan.EffectivePlan;
import com.monday.shared.memory.quota.QuotaSnapshot;
import com.monday.shared.memory.session.utils.PrincipalType;
import com.monday.shared.memory.session.utils.SessionSource;
import com.monday.shared.memory.session.utils.SessionState;
import com.monday.shared.recording.RecordingScope;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
public class MemoryServiceTests {

    @Mock
    private SessionMemoryRepository sessionMemoryRepository;

    @Mock
    private MemoryChunkRepository memoryChunkRepository;

    @Mock
    private Clock clock;

    @MockBean
    private SessionService sessionService;

    @InjectMocks
    private MemoryService memoryService;

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

    @Test
    void appendChunk_happyPath() {
        // Creates memory chunk that's present under our Session Memory Controller
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
//        TODO: Set options when entity exists.
//        SessionOptions options = new SessionOptions();
//        options.setScope(SessionScope.CHANNEL);
//        options.setMaxChunksPerSession(10);

        SessionMemoryEntity session = new SessionMemoryEntity();
        session.setSessionId(sessionId);
        session.setPrincipalType(PrincipalType.USER);
        session.setPrincipalId("user-123");
        session.setSessionState(SessionState.ACTIVE);
        session.setChunkCount(0);
//        session.setOptions(options);

        Instant now = Instant.parse("2025-12-06T12:00:00Z");

        when(sessionMemoryRepository
                .findBySessionIdAndPrincipalTypeAndPrincipalId(
                        sessionId,
                        PrincipalType.GUEST,
                        principalId.toString()))
                .thenReturn(Optional.of(session));

        when(sessionService.getSessionPresent(any(), any(), eq(false))).thenReturn(session);
        when(clock.instant()).thenReturn(now);

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
        Assertions.assertEquals(now, session.getLastOccurredAt());

        // chunk saved with correct fields
        verify(memoryChunkRepository).save(chunkCaptor.capture());
        MemoryChunkEntity savedChunk = chunkCaptor.getValue();

        Assertions.assertEquals(sessionId, savedChunk.getSession().getSessionId());
        Assertions.assertEquals(dto.content(), savedChunk.getContent().get("body"));

        // service returned mapped DTO
        Assertions.assertNotNull(response);
        Assertions.assertEquals(dto.content(), response.content());
        Assertions.assertEquals(dto.principalId(), response.principalId());

        // session persisted
        verify(sessionMemoryRepository).save(session);

    }

    @Test
    void appendChunk_wrongPrincipal_forbidden() {

    }

    @Test
    void appendChunk_sessionClosed_rejected() {

    }

    @Test
    void appendChunk_maxChunksExceeded_sessionFull() {

    }

    @Test
    void appendChunk_defaultsApplied_whenOptionsNull() {

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
