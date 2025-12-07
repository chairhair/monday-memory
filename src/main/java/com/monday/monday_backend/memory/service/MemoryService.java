package com.monday.monday_backend.memory.service;

import com.monday.monday_backend.auth.principal.PrincipalContext;
import com.monday.monday_backend.llm.LlmClient;
import com.monday.monday_backend.memory.entity.MemoryChunkEntity;
import com.monday.monday_backend.memory.entity.SessionMemoryEntity;
import com.monday.monday_backend.memory.repo.MemoryChunkRepository;
import com.monday.monday_backend.memory.utils.MemoryChunkUtils;
import com.monday.shared.llm.LlmMessage;
import com.monday.shared.llm.LlmRequestDTO;
import com.monday.shared.llm.LlmResponseDTO;
import com.monday.shared.memory.dto.RequestMemoryChunkDTO;
import com.monday.shared.memory.dto.ResponseMemoryChunkDTO;
import com.monday.shared.memory.session.dto.CreateSessionRequestDTO;
import com.monday.shared.memory.session.dto.SessionMemoryResponseDTO;
import com.monday.shared.memory.session.utils.PrincipalType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemoryService {

    private final SessionService sessionService;
    private final MemoryChunkRepository memoryChunkRepository;
    private final MemoryChunkUtils memoryChunkUtils;
    private final LlmClient llmClient;

    /**
     * High-level entry: ensure session exists, record a chunk, and optionally query the LLM.
     * Used by QueryProcessingService for both guest + user flows via PrincipalContext.
     */
    public ResponseMemoryChunkDTO query(PrincipalContext principal,
                                        RequestMemoryChunkDTO dto) {

        // 1) Ensure we have a session for this principal
        SessionMemoryEntity session = resolveOrCreateSession(principal, dto);

        // 2) Build LLM context from past chunks in that session
        List<LlmMessage> messages = new ArrayList<>();

        List<MemoryChunkEntity> chunks = new ArrayList<>();
        if (session != null) {
            chunks = memoryChunkRepository.findTop5BySessionOrderByOccurredAtDesc(session);
        }

        String contextText = buildContextFromChunks(chunks);

        messages.add(new LlmMessage(
                LlmMessage.Role.SYSTEM,
                """
                        You are MondayMemory, a personal memory assistant.
                        You are given prior context from this user's session. Use it when relevant.
                        
                        Prior context (most recent first):
                        %s
                        """.formatted(contextText)
        ));

        messages.add(new LlmMessage(LlmMessage.Role.USER, dto.content()));

        LlmRequestDTO llmRequestDTO = new LlmRequestDTO(
                null,                        // model override (use default)
                messages,
                null,                        // temperature
                null,                        // maxTokens/timeout
                principal.getPrincipalId().toString(),// userId
                dto.sessionId().toString(),  // sessionId
                null,                        // metadata
                null                         // providerOverride
        );

        LlmResponseDTO llmResponseDTO = llmClient.chat(llmRequestDTO);

        MemoryChunkEntity userChunk =
                memoryChunkUtils.forUserMessage(session, dto.content(), dto.source().toString());
        MemoryChunkEntity assistantChunk =
                memoryChunkUtils.forAssistantMessage(session, llmResponseDTO.content(), dto.source().toString());

        memoryChunkRepository.save(userChunk);
        memoryChunkRepository.save(assistantChunk);

        // 4) Map to response DTO
        SessionMemoryEntity sessionMemory = sessionService.updateChunkCount(assistantChunk.getSession(), 2);

        // TODO: Ignoring tags for now.
        return new ResponseMemoryChunkDTO(assistantChunk.getContent().toString(), sessionMemory.getPrincipalType(), sessionMemory.getPrincipalId(), Instant.now(), null);
    }

    /**
     * Handle pure "record memory" without LLM call if you have that case.
     */
    public ResponseMemoryChunkDTO recordOnly(PrincipalContext principal,
                                             RequestMemoryChunkDTO dto) {
        SessionMemoryEntity session = resolveOrCreateSession(principal, dto);
        MemoryChunkEntity chunk = createChunkFromDto(principal, session, dto);
        memoryChunkRepository.save(chunk);

        sessionService.updateChunkCount(session, 1);

        return new ResponseMemoryChunkDTO(dto.content(),principal.getPrincipalType(),principal.getPrincipalId().toString(), chunk.getOccurredAt(), null);
    }

    // ---------------- internal helpers ----------------

    private SessionMemoryEntity resolveOrCreateSession(PrincipalContext principal,
                                                       RequestMemoryChunkDTO dto) {
        if (dto.sessionId() != null) {
            SessionMemoryEntity existing =
                    sessionService.getSessionPresent(dto.sessionId(), principal, false);
            if (existing != null) {
                return existing;
            }
        }

        // If we don't have an existing session, create one
        CreateSessionRequestDTO createReq = new CreateSessionRequestDTO(
                principal.getExternalGuestKey(),
                dto.sourceConversationKey(),
                dto.source(),
                dto.sourceConversationKey(),
                principal.getRecordingScope()
        );

        SessionMemoryResponseDTO sessionDTO =
                sessionService.createOrReuseSession(principal, createReq, null);

        if (!sessionDTO.statusCode().is2xxSuccessful()
                && sessionDTO.sessionIds() == null || sessionDTO.sessionIds().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Unable to create or resolve session for memory");
        }

        // In your real DTO, sessionId should be present
        return sessionService.getSessionPresent(
                UUID.fromString(sessionDTO.sessionIds().getFirst()),
                principal,
                false
        );
    }

    private MemoryChunkEntity createChunkFromDto(PrincipalContext principal,
                                                 SessionMemoryEntity session,
                                                 RequestMemoryChunkDTO dto) {

        String principalId = (principal.getPrincipalId() != null) ? principal.getPrincipalId().toString() : null;
        PrincipalType principalType = principal.getPrincipalType();

        MemoryChunkEntity newChunk = new MemoryChunkEntity();
        newChunk.setSession(session);
        newChunk.setOccurredAt(dto.timestamp() != null ? dto.timestamp() : Instant.now());
        newChunk.setIngestedAt(Instant.now());
        newChunk.setTags(dto.tags());

        Map<String, Object> content = Map.of(
                "body", dto.content(),
                "principalType", principalType.name(),
                "principalId", (principal.getPrincipalId() == null) ? "" : principalId,
                    "guestKey", principal.getExternalGuestKey(),
                "source", dto.source().toString(),
                "sourceConversationKey", dto.sourceConversationKey()
        );

        // however you're currently serializing to JSON or some column:
        newChunk.setContent(content);

        String normalizedText = normalize(dto.content());
        newChunk.setHashSha256(sha256(normalizedText));

        // Increment session chunk count here if you’re tracking it
        session.setChunkCount(session.getChunkCount() + 1);

        return newChunk;
    }

    private String buildContextFromChunks(List<MemoryChunkEntity> chunks) {
        return memoryChunkUtils.buildContext(chunks);
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
