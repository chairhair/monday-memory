package com.monday.monday_backend.memory.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.monday.monday_backend.auth.principal.PrincipalContext;
import com.monday.monday_backend.auth.principal.PrincipalResolver;
import com.monday.monday_backend.memory.service.MemoryService;
import com.monday.monday_backend.memory.service.SessionService;
import com.monday.shared.auth.utils.AccessLevel;
import com.monday.shared.memory.plan.EffectivePlan;
import com.monday.shared.memory.quota.QuotaSnapshot;
import com.monday.shared.memory.session.dto.CreateSessionRequestDTO;
import com.monday.shared.memory.session.utils.PrincipalType;
import com.monday.shared.memory.session.utils.SessionSource;
import com.monday.shared.memory.session.utils.SessionState;
import com.monday.shared.recording.RecordingScope;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
public class MemoryServiceTests {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper mapper;

    @Mock
    MemoryService memoryService;

    @Mock
    SessionService sessionService;

    @MockBean
    PrincipalResolver principalResolver;

    private SessionMemoryEntity buildSession(SessionState state,
                                             String sessionId,
                                             String principalId,
                                             PrincipalType principalType,
                                             RecordingScope scope,
                                             Instant endedAt) {
        SessionMemoryEntity entity = new SessionMemoryEntity();
        entity.setSessionId(UUID.fromString(sessionId));
        entity.setSource(SessionSource.DISCORD);
        entity.setSourceConversation("hawk-tuah-man");
        entity.setPrincipalType(principalType);
        entity.setPrincipalId(principalId);
        entity.setScope(scope);
        entity.setChunkCount(0);
        entity.setSessionState(state);
        entity.setLastOccurredAt(Instant.now());
        entity.setEndedAt(endedAt);
        entity.setCreatedAt(Instant.now().minusSeconds(7200));
        entity.setUpdatedAt(Instant.now().minusSeconds(3600));
        entity.setIdempotencyKey("test-idemp-key");
        return entity;
    }

    @Test
    void appendChunk_happyPath() {
//        // Creates memory chunk that's present under our Session Memory Controller
//        QuotaSnapshot quotaSnapshot = QuotaSnapshot.builder()
//                .topicsUsed(0)
//                .tokenLimit(0)
//                .topicLimit(100)
//                .tokenLimit(10000L)
//                .build();
//
//        // This is what we expect the PrincipalResolver to return when no auth headers exist (guest)
//        PrincipalContext guestContext = PrincipalContext.builder()
//                .principalId(null) // or a UUID if you care
//                .principalType(PrincipalType.GUEST)
//                .accessLevel(AccessLevel.GUEST)
//                .plan(EffectivePlan.GUEST_FREE)
//                .quota(quotaSnapshot)
//                .recordingScope(RecordingScope.PRIVATE)
//                .recallScope(null)
//                .build();
//
//        UUID principalId = UUID.randomUUID();
//        UUID sessionId = UUID.randomUUID();
//
//        SessionMemoryEntity active = buildSession(SessionState.ACTIVE, sessionId.toString(), principalId.toString(), PrincipalType.GUEST, RecordingScope.PRIVATE, null);
//

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

}
