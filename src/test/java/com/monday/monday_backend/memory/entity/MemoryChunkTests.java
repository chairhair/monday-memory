package com.monday.monday_backend.memory.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.monday.monday_backend.auth.principal.AuthUser;
import com.monday.monday_backend.auth.principal.PrincipalContext;
import com.monday.monday_backend.auth.principal.PrincipalResolver;
import com.monday.monday_backend.auth.users.UserService;
import com.monday.monday_backend.memory.SessionMemoryController;
import com.monday.monday_backend.memory.repo.MemoryChunkRepository;
import com.monday.monday_backend.memory.service.MemoryService;
import com.monday.monday_backend.memory.service.SessionService;
import com.monday.monday_backend.memory.utils.MemoryChunkUtils;
import com.monday.shared.auth.utils.AccessLevel;
import com.monday.shared.memory.dto.RequestMemoryChunkDTO;
import com.monday.shared.memory.dto.ResponseMemoryChunkDTO;
import com.monday.shared.memory.plan.EffectivePlan;
import com.monday.shared.memory.quota.QuotaSnapshot;
import com.monday.shared.memory.session.GuestHandle;
import com.monday.shared.memory.session.utils.PrincipalType;
import com.monday.shared.memory.session.utils.SessionScope;
import com.monday.shared.memory.session.utils.SessionSource;
import com.monday.shared.memory.session.utils.SessionState;
import com.monday.shared.recording.RecordingScope;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SessionMemoryController.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
public class MemoryChunkTests {
    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper mapper;

    @Mock MemoryChunkRepository memoryChunkRepository;

    @MockBean UserService userService;
    @MockBean MemoryService memoryService;
    @MockBean SessionService sessionService;
    @MockBean PrincipalResolver principalResolver;

    MemoryChunkUtils memoryChunkUtils = new MemoryChunkUtils(mapper);

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
    void createMemoryChunk_usingGuest() throws Exception {
        UUID principalId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        QuotaSnapshot quotaSnapshot = QuotaSnapshot.builder()
                .topicsUsed(0)
                .tokensUsed(0)
                .topicLimit(100)
                .tokenLimit(10_000L)
                .build();

        PrincipalContext guestContext = PrincipalContext.builder()
                .principalId(principalId)                        // ✅ NON-NULL
                .principalType(PrincipalType.GUEST)
                .accessLevel(AccessLevel.GUEST)
                .plan(EffectivePlan.GUEST_FREE)
                .quota(quotaSnapshot)
                .recordingScope(RecordingScope.PRIVATE)
                .recallScope(null)
                .externalGuestKey("hawk-tuah-man")
                .build();

        RequestMemoryChunkDTO requestBody = new RequestMemoryChunkDTO(
                sessionId,
                PrincipalType.GUEST,
                principalId.toString(),
                "hawk-tuah-man",
                SessionSource.DISCORD,
                "hawk-tuah-man",
                "Neato Dorito",
                Instant.now(),
                null
        );

        ResponseMemoryChunkDTO responseMemoryChunkDTO = new ResponseMemoryChunkDTO(
                "Neato Dorito",
                PrincipalType.GUEST,
                principalId.toString(),
                Instant.now(),
                null
        );

        when(principalResolver.resolve(any(), any()))
                .thenReturn(guestContext);

        when(memoryService.recordOnly(any(PrincipalContext.class), any(RequestMemoryChunkDTO.class)))
                .thenReturn(responseMemoryChunkDTO);

        ArgumentCaptor<PrincipalContext> principalContextCaptor =
                ArgumentCaptor.forClass(PrincipalContext.class);
        ArgumentCaptor<RequestMemoryChunkDTO> dtoCaptor =
                ArgumentCaptor.forClass(RequestMemoryChunkDTO.class);

        mvc.perform(post("/v1/memory/session/memory-chunk")
                        .contentType("application/json")
                        .content(mapper.writeValueAsString(requestBody)))
                .andDo(print())
                .andExpect(status().isOk());

        verify(principalResolver).resolve(isNull(), any(GuestHandle.class));
        verify(memoryService).recordOnly(principalContextCaptor.capture(), dtoCaptor.capture());

        Assertions.assertNotNull(
                principalContextCaptor.getValue(),
                "PrincipalContext passed to memoryService should not be null"
        );

        Assertions.assertNotNull(
                dtoCaptor.getValue(),
                "DTO values passed to memoryService should not be null"
        );

        System.out.println("Captured PrincipalContext: " + principalContextCaptor.getValue());
        System.out.println("Captured ctxReqDTO: " + dtoCaptor.getValue());

        Assertions.assertEquals(PrincipalType.GUEST, principalContextCaptor.getValue().getPrincipalType());
        Assertions.assertEquals(AccessLevel.GUEST, principalContextCaptor.getValue().getAccessLevel());

        RequestMemoryChunkDTO captured = dtoCaptor.getValue();
        Assertions.assertEquals("hawk-tuah-man", captured.guestKey());
        Assertions.assertEquals(SessionSource.DISCORD, captured.source());
        Assertions.assertEquals("hawk-tuah-man", captured.sourceConversationKey());
    }


    @Test
    void createMemoryChunk_usingUser() throws Exception {
        UUID principalId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        QuotaSnapshot quotaSnapshot = QuotaSnapshot.builder()
                .topicsUsed(0)
                .tokensUsed(0)
                .topicLimit(100)
                .tokenLimit(10_000L)
                .build();

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

        RequestMemoryChunkDTO requestBody = new RequestMemoryChunkDTO(
                sessionId,
                PrincipalType.USER,
                principalId.toString(),
                "hawk-tuah-man",
                SessionSource.DISCORD,
                "hawk-tuah-man",
                "Neato Dorito",
                Instant.now(),
                null
        );

        ResponseMemoryChunkDTO responseMemoryChunkDTO = new ResponseMemoryChunkDTO(
                "Neato Dorito",
                PrincipalType.USER,
                principalId.toString(),
                Instant.now(),
                null
        );

        when(principalResolver.resolve(any(), any()))
                .thenReturn(guestContext);

        when(memoryService.recordOnly(any(PrincipalContext.class), any(RequestMemoryChunkDTO.class)))
                .thenReturn(responseMemoryChunkDTO);

        ArgumentCaptor<PrincipalContext> principalContextCaptor =
                ArgumentCaptor.forClass(PrincipalContext.class);
        ArgumentCaptor<RequestMemoryChunkDTO> dtoCaptor =
                ArgumentCaptor.forClass(RequestMemoryChunkDTO.class);

        mvc.perform(post("/v1/memory/session/memory-chunk")
                        .contentType("application/json")
                        .content(mapper.writeValueAsString(requestBody)))
                .andDo(print())
                .andExpect(status().isOk());

        verify(principalResolver).resolve(isNull(), any(GuestHandle.class));
        verify(memoryService).recordOnly(principalContextCaptor.capture(), dtoCaptor.capture());

        Assertions.assertNotNull(
                principalContextCaptor.getValue(),
                "PrincipalContext passed to memoryService should not be null"
        );

        Assertions.assertNotNull(
                dtoCaptor.getValue(),
                "DTO values passed to memoryService should not be null"
        );

        System.out.println("Captured PrincipalContext: " + principalContextCaptor.getValue());
        System.out.println("Captured ctxReqDTO: " + dtoCaptor.getValue());

        Assertions.assertEquals(PrincipalType.USER, principalContextCaptor.getValue().getPrincipalType());
        Assertions.assertEquals(AccessLevel.USER, principalContextCaptor.getValue().getAccessLevel());

        RequestMemoryChunkDTO captured = dtoCaptor.getValue();
        Assertions.assertEquals("hawk-tuah-man", captured.guestKey());
        Assertions.assertEquals(SessionSource.DISCORD, captured.source());
        Assertions.assertEquals("hawk-tuah-man", captured.sourceConversationKey());
    }

    /////////////////////////////////////////////////////////////
    /// UTILS TESTS
    /////////////////////////////////////////////////////////////

    @Test
    void memoryChunkUtils_toDtoAndEntity() {
        Instant now = Instant.now();
        // Initializing Memory Chunk
        UUID sessionId = UUID.randomUUID();
        UUID principalId = UUID.randomUUID();

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
        session.setCreatedAt(now);
        session.setSource(SessionSource.DISCORD);


        // Now, we create the memory chunk entity and dto
        MemoryChunkEntity memoryChunkEntity = new MemoryChunkEntity();
        Map<String, Object> content = new HashMap<>();
        content.put("text", "boomer");
        memoryChunkEntity.setContent(content);
        memoryChunkEntity.setSession(session);
        memoryChunkEntity.setIngestedAt(now);
        memoryChunkEntity.setOccurredAt(now);
        memoryChunkEntity.setTags(null);
        memoryChunkEntity.setHashSha256(sha256(content.get("text").toString()));

        ResponseMemoryChunkDTO dto = new ResponseMemoryChunkDTO(
                "boomer",
                PrincipalType.USER,
                principalId.toString(),
                now,
                null);

        // Now, we execute the MemoryChunkUtils
        MemoryChunkEntity utilsChunk = memoryChunkUtils.toMemoryEntity(dto, session);
        ResponseMemoryChunkDTO utilsDTO = memoryChunkUtils.toDto(memoryChunkEntity);

        Assertions.assertEquals(dto.content(), utilsDTO.content());
        Assertions.assertEquals(dto.principalId(), utilsDTO.principalId());
        Assertions.assertEquals(dto.principalType(), utilsDTO.principalType());
        Assertions.assertEquals(dto.tags(), utilsDTO.tags());
        Assertions.assertEquals(dto.timestamp(), utilsDTO.timestamp());

        Assertions.assertEquals(memoryChunkEntity.getMemoryId(), utilsChunk.getMemoryId());
        Assertions.assertEquals(memoryChunkEntity.getContent(), utilsChunk.getContent());
        Assertions.assertEquals(memoryChunkEntity.getIngestedAt(), utilsChunk.getIngestedAt());
        Assertions.assertEquals(memoryChunkEntity.getOccurredAt(), utilsChunk.getOccurredAt());
        Assertions.assertEquals(memoryChunkEntity.getHashSha256(), utilsChunk.getHashSha256());
        Assertions.assertEquals(memoryChunkEntity.getSession(), utilsChunk.getSession());
    }

    @Test
    void memoryChunkUtils_forUserMessage() {
        Instant now = Instant.now();
        // Initializing Memory Chunk
        UUID sessionId = UUID.randomUUID();
        UUID principalId = UUID.randomUUID();

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
        session.setCreatedAt(now);
        session.setSource(SessionSource.DISCORD);

        MemoryChunkEntity memoryChunk = memoryChunkUtils.forUserMessage(session, "boomer", SessionSource.DISCORD.toString());
        List<String> tags = memoryChunk.getTags();
        Assertions.assertEquals(3, tags.size());
        Assertions.assertEquals("kind:chat_message", tags.get(0));
        Assertions.assertEquals("role:USER", tags.get(1));
        Assertions.assertEquals("source:DISCORD", tags.get(2));

        Map<String, Object> chunkContent = memoryChunk.getContent();
        Assertions.assertEquals("chat_message", chunkContent.get("kind").toString());
        Assertions.assertEquals("USER", chunkContent.get("role").toString());
        Assertions.assertEquals("boomer", chunkContent.get("text").toString());
        Assertions.assertEquals("DISCORD", chunkContent.get("source").toString());

        Assertions.assertEquals("chat_message", chunkContent.get("kind").toString());
        Assertions.assertEquals("USER", chunkContent.get("role").toString());
        Assertions.assertEquals("boomer", chunkContent.get("text").toString());
        Assertions.assertEquals("DISCORD", chunkContent.get("source").toString());

    }

    @Test
    void memoryChunkUtils_forAssistantMessage() {
        Instant now = Instant.now();
        // Initializing Memory Chunk
        UUID sessionId = UUID.randomUUID();
        UUID principalId = UUID.randomUUID();

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
        session.setCreatedAt(now);
        session.setSource(SessionSource.API);

        MemoryChunkEntity memoryChunk = memoryChunkUtils.forAssistantMessage(session, "boomer", SessionSource.API.toString());
        List<String> tags = memoryChunk.getTags();
        Assertions.assertEquals(3, tags.size());
        Assertions.assertEquals("kind:chat_message", tags.get(0));
        Assertions.assertEquals("role:assistant", tags.get(1));
        Assertions.assertEquals("source:API", tags.get(2));

        Map<String, Object> chunkContent = memoryChunk.getContent();
        Assertions.assertEquals("chat_message", chunkContent.get("kind").toString());
        Assertions.assertEquals("assistant", chunkContent.get("role").toString());
        Assertions.assertEquals("boomer", chunkContent.get("text").toString());
        Assertions.assertEquals("API", chunkContent.get("source").toString());

        Assertions.assertEquals("chat_message", chunkContent.get("kind").toString());
        Assertions.assertEquals("assistant", chunkContent.get("role").toString());
        Assertions.assertEquals("boomer", chunkContent.get("text").toString());
        Assertions.assertEquals("API", chunkContent.get("source").toString());

    }

    @Test
    void memoryChunkUtils_buildContext() {
        Assertions.assertEquals("No prior context.", memoryChunkUtils.buildContext(null));
        Assertions.assertEquals("No prior context.", memoryChunkUtils.buildContext(new ArrayList<>()));

        String[] testPrompt = """
                user: This is the number of testing prompts that have been provided: 0
                user: This is the number of testing prompts that have been provided: 1
                user: This is the number of testing prompts that have been provided: 2
                user: This is the number of testing prompts that have been provided: 3
                user: This is the number of testing prompts that have been provided: 4
                user: This is the number of testing prompts that have been provided: 5
                user: This is the number of testing prompts that have been provided: 6
                user: This is the number of testing prompts that have been provided: 7
                user: This is the number of testing prompts that have been provided: 8
                user: This is the number of testing prompts that have been provided: 9
                """.trim().split("\n");

        List<MemoryChunkEntity> bulkMemChunk = generateBulkMemoryChunks();
        String[] currentPrompt = memoryChunkUtils.buildContext(bulkMemChunk).trim().split("\n");
        Assertions.assertEquals(testPrompt.length, currentPrompt.length);
        for (int i = 0; i < currentPrompt.length; i++) {
            Assertions.assertEquals(currentPrompt[i].trim(), testPrompt[i].trim());
        }
    }

    /////////////////////////////////////////////////////////////
    /// HELPERS
    /////////////////////////////////////////////////////////////

    private List<MemoryChunkEntity> generateBulkMemoryChunks() {
        Instant now = Instant.now();
        // Initializing Memory Chunk
        UUID sessionId = UUID.randomUUID();
        UUID principalId = UUID.randomUUID();

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
        session.setCreatedAt(now);
        session.setSource(SessionSource.DISCORD);

        ArrayList<MemoryChunkEntity> memList = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            now = Instant.now();
            MemoryChunkEntity memoryChunkEntity = new MemoryChunkEntity();
            Map<String, Object> content = new HashMap<>();
            content.put("kind", "chat_message");
            content.put("role", "user");
            content.put("source", "DISCORD");
            content.put("text", "This is the number of testing prompts that have been provided: "+i);
            memoryChunkEntity.setContent(content);
            memoryChunkEntity.setSession(session);
            memoryChunkEntity.setIngestedAt(now);
            memoryChunkEntity.setOccurredAt(now);

            memoryChunkEntity.setTags(List.of("kind:chat_message", "role:assistant", "source:DISCORD"));
            memoryChunkEntity.setHashSha256(sha256(content.get("text").toString()));
            memList.add(memoryChunkEntity);
        }
        return memList;
    }

    private void auth(List<SimpleGrantedAuthority> simpleAuthorities) {
        var auth = new UsernamePasswordAuthenticationToken(
                /* principal: replace with whatever your app expects */
                new AuthUser("123", "user@example.com", simpleAuthorities),
                null,
                simpleAuthorities
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
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
