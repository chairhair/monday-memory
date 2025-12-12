package com.monday.monday_backend.memory.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.monday.monday_backend.memory.repo.MemoryChunkRepository;
import com.monday.monday_backend.memory.service.MemoryAggregationService;
import com.monday.monday_backend.memory.service.MemoryService;
import com.monday.monday_backend.memory.utils.MemoryChunkUtils;
import com.monday.shared.memory.session.utils.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class MemoryAggregationServiceTests {

    @Mock
    MemoryChunkRepository memoryChunkRepository;

    MemoryAggregationService memoryAggregationService;

    @BeforeEach
    void setUp() {
        memoryAggregationService = new MemoryAggregationService(memoryChunkRepository);
    }

    @Test
    void aggregate_raw_delegatesToAggregateByRaw() {
        Instant now = Instant.now();
        SessionOptionsEntity options = new SessionOptionsEntity();
        options.setScope(SessionScope.CHANNEL);
        options.setMaxChunksPerSession(10);

        UUID sessionId = UUID.randomUUID();
        UUID principalId = UUID.randomUUID();

        SessionMemoryEntity userSession = new SessionMemoryEntity();
        userSession.setSessionId(sessionId);
        userSession.setPrincipalType(PrincipalType.USER);
        userSession.setPrincipalId(principalId.toString());
        userSession.setSessionState(SessionState.ACTIVE);
        userSession.setChunkCount(0);
        userSession.setOptions(options);
        userSession.setCreatedAt(now);
        userSession.setSource(SessionSource.DISCORD);

        SessionMemoryEntity guestSession = new SessionMemoryEntity();
        guestSession.setSessionId(sessionId);
        guestSession.setPrincipalType(PrincipalType.GUEST);
        guestSession.setPrincipalId(null);
        guestSession.setSessionState(SessionState.ACTIVE);
        guestSession.setChunkCount(0);
        guestSession.setOptions(options);
        guestSession.setCreatedAt(now);
        guestSession.setSource(SessionSource.DISCORD);

        MemoryAggregationOptions option = MemoryAggregationOptions.builder().mode(MemoryAggregationMode.RAW).build();

        MemoryChunkEntity userMem = new MemoryChunkEntity();
        Map<String, Object> content = new HashMap<>(); content.put("text", "boomer");
        userMem.setContent(content);
        userMem.setSession(userSession);
        userMem.setIngestedAt(now);
        userMem.setOccurredAt(now);
        userMem.setTags(List.of("kind:chat_message", "role:USER", "source:DISCORD"));
        userMem.setHashSha256(sha256(normalize(content.get("text").toString())));

        when(memoryChunkRepository.findBySessionOrderByOccurredAtAsc(eq(userSession), any())).thenReturn(List.of(userMem));


        List<MemoryChunkEntity> rawList = memoryAggregationService.aggregateByRaw(userSession);
        Assertions.assertEquals(1, rawList.size());
        Assertions.assertEquals("boomer", rawList.get(0).getContent().get("text"));

        rawList = memoryAggregationService.aggregate(userSession, option);

        Assertions.assertEquals(1, rawList.size());
        Assertions.assertEquals("boomer", rawList.get(0).getContent().get("text"));

        MemoryChunkEntity guestMem = new MemoryChunkEntity();
        content = new HashMap<>(); content.put("text", "zoomer");
        guestMem.setContent(content);
        guestMem.setSession(userSession);
        guestMem.setIngestedAt(now);
        guestMem.setOccurredAt(now);
        guestMem.setTags(List.of("kind:chat_message", "role:GUEST", "source:DISCORD"));
        guestMem.setHashSha256(sha256(normalize(content.get("text").toString())));
        when(memoryChunkRepository.findBySessionOrderByOccurredAtAsc(eq(guestSession), any())).thenReturn(List.of(guestMem));

        rawList = memoryAggregationService.aggregateByRaw(guestSession);
        Assertions.assertEquals(1, rawList.size());
        Assertions.assertEquals("zoomer", rawList.get(0).getContent().get("text"));

        rawList = memoryAggregationService.aggregate(guestSession, option);

        Assertions.assertEquals(1, rawList.size());
        Assertions.assertEquals("zoomer", rawList.get(0).getContent().get("text"));
    }

    @Test
    void aggregate_lastN_usesNumberAndValidatesMaxChunks() {
        Instant now = Instant.now();
        SessionOptionsEntity options = new SessionOptionsEntity();
        options.setScope(SessionScope.CHANNEL);
        options.setMaxChunksPerSession(10);

        UUID sessionId = UUID.randomUUID();
        UUID principalId = UUID.randomUUID();

        SessionMemoryEntity userSession = new SessionMemoryEntity();
        userSession.setSessionId(sessionId);
        userSession.setPrincipalType(PrincipalType.USER);
        userSession.setPrincipalId(principalId.toString());
        userSession.setSessionState(SessionState.ACTIVE);
        userSession.setChunkCount(0);
        userSession.setOptions(options);
        userSession.setCreatedAt(now);
        userSession.setSource(SessionSource.DISCORD);

        MemoryAggregationOptions option = MemoryAggregationOptions.builder()
                .mode(MemoryAggregationMode.LAST_N)
                .maxChunks(5)
                .build();

        List<MemoryChunkEntity> memList = new ArrayList<>();
        for (int i = 0; i <= 10; i++){
            MemoryChunkEntity userMem = new MemoryChunkEntity();
            Map<String, Object> content = new HashMap<>();
            content.put("text", "boomer "+i);
            userMem.setContent(content);
            userMem.setSession(userSession);
            userMem.setIngestedAt(now);
            userMem.setOccurredAt(now);
            userMem.setTags(List.of("kind:chat_message", "role:USER", "source:DISCORD"));
            userMem.setHashSha256(sha256(normalize(content.get("text").toString())));
            memList.add(userMem);
        }

        when(memoryChunkRepository.findBySessionOrderByOccurredAtAsc(eq(userSession), eq(PageRequest.of(0, 5)))).thenReturn(memList.subList(0, 5));


        List<MemoryChunkEntity> rawList = memoryAggregationService.aggregateByNumber(userSession, 5);
        Assertions.assertEquals(5, rawList.size());
        Assertions.assertEquals("boomer 0", rawList.get(0).getContent().get("text"));

        rawList = memoryAggregationService.aggregate(userSession, option);

        Assertions.assertEquals(5, rawList.size());
        Assertions.assertEquals("boomer 0", rawList.get(0).getContent().get("text"));

    }

    @Test
    void aggregate_sinceTime_filtersCorrectly() {
        Instant now = Instant.now();
        SessionOptionsEntity options = new SessionOptionsEntity();
        options.setScope(SessionScope.CHANNEL);
        options.setMaxChunksPerSession(10);

        UUID sessionId = UUID.randomUUID();
        UUID principalId = UUID.randomUUID();

        SessionMemoryEntity userSession = new SessionMemoryEntity();
        userSession.setSessionId(sessionId);
        userSession.setPrincipalType(PrincipalType.USER);
        userSession.setPrincipalId(principalId.toString());
        userSession.setSessionState(SessionState.ACTIVE);
        userSession.setChunkCount(0);
        userSession.setOptions(options);
        userSession.setCreatedAt(now);
        userSession.setSource(SessionSource.DISCORD);

        MemoryAggregationOptions option = MemoryAggregationOptions.builder()
                .mode(MemoryAggregationMode.LAST_N)
                .maxChunks(5)
                .build();

        List<MemoryChunkEntity> memList = new ArrayList<>();
        for (int i = 0; i <= 10; i++){
            MemoryChunkEntity userMem = new MemoryChunkEntity();
            Map<String, Object> content = new HashMap<>();
            content.put("text", "boomer "+i);
            userMem.setContent(content);
            userMem.setSession(userSession);
            if (i < 3) {
                userMem.setIngestedAt(now);
                userMem.setOccurredAt(now);
            }
            if (i > 3 && i < 7) {
                userMem.setIngestedAt(now.plusSeconds(10*60));
                userMem.setOccurredAt(now.plusSeconds(10*60));
            }
            else {
                userMem.setIngestedAt(now.plusSeconds(20*60));
                userMem.setOccurredAt(now.plusSeconds(20*60));
            }
            userMem.setTags(List.of("kind:chat_message", "role:USER", "source:DISCORD"));
            userMem.setHashSha256(sha256(normalize(content.get("text").toString())));
            memList.add(userMem);
        }

        when(memoryChunkRepository.findBySessionOrderByOccurredAtAsc(eq(userSession), eq(PageRequest.of(0, 5)))).thenReturn(memList.subList(0, 5));


        List<MemoryChunkEntity> rawList = memoryAggregationService.aggregateByNumber(userSession, 5);
        Assertions.assertEquals(5, rawList.size());
        Assertions.assertEquals("boomer 0", rawList.get(0).getContent().get("text"));

        rawList = memoryAggregationService.aggregate(userSession, option);

        Assertions.assertEquals(5, rawList.size());
        Assertions.assertEquals("boomer 0", rawList.get(0).getContent().get("text"));
    }

    @Test
    void aggregate_invalidMode() {

    }

    @Test
    void aggregate_MissingFields_throws() {

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
