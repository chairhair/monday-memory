package com.monday.monday_backend.memory.service;

import com.monday.monday_backend.auth.principal.PrincipalContext;
import com.monday.monday_backend.llm.LlmClient;
import com.monday.monday_backend.memory.entity.MemoryChunkEntity;
import com.monday.monday_backend.memory.entity.SessionMemoryEntity;
import com.monday.monday_backend.memory.entity.SessionOptionsEntity;
import com.monday.monday_backend.memory.repo.MemoryChunkRepository;
import com.monday.monday_backend.memory.utils.MemoryChunkUtils;
import com.monday.monday_backend.payment.entity.UserPlanEntity;
import com.monday.shared.llm.LlmMessage;
import com.monday.shared.llm.LlmRequestDTO;
import com.monday.shared.llm.LlmResponseDTO;
import com.monday.shared.memory.dto.RequestMemoryChunkDTO;
import com.monday.shared.memory.dto.RequestMemoryQueryDTO;
import com.monday.shared.memory.dto.ResponseMemoryChunkDTO;
import com.monday.shared.memory.quota.QuotaDecision;
import com.monday.shared.memory.quota.QuotaSnapshot;
import com.monday.shared.memory.session.dto.CreateSessionRequestDTO;
import com.monday.shared.memory.session.dto.SessionMemoryResponseDTO;
import com.monday.shared.memory.session.dto.SessionOptionRequestDTO;
import com.monday.shared.memory.session.utils.MemoryAggregationOptions;
import com.monday.shared.memory.session.utils.PrincipalType;
import com.monday.shared.memory.session.utils.SessionScope;
import com.monday.shared.memory.session.utils.SessionState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
    private final QuotaService quotaService;
    private final MemoryChunkRepository memoryChunkRepository;
    private final MemoryAggregationService memoryAggregationService;
    private final MemoryChunkUtils memoryChunkUtils;
    private final LlmClient llmClient;

    // Limits the llm chat so it doesn't exceed budget. Keeps us in check
    private final static long MAX_TOKENS_PER_QUERY = 1000L;

    /**
     * High-level entry: ensure session exists, record a chunk, and optionally query the LLM.
     * Used by QueryProcessingService for both guest + user flows via PrincipalContext.
     */
    @Transactional
    public ResponseMemoryChunkDTO query(PrincipalContext principal,
                                        RequestMemoryQueryDTO dto) {

        quotaService.resetTokensIfMonthPassed(principal.getUserPlan());

        QuotaSnapshot currentUserSnapshot = quotaService.snapshotFor(principal.getUser(), principal.getGuest(), principal.getUserPlan(), principal.getPlan());
        QuotaDecision systemDecision = quotaService.decide(currentUserSnapshot);
        if (systemDecision.compareTo(QuotaDecision.BLOCK) == 0) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot process - User has surpassed their allowed quota limits");
        }

        // We build recall context from prior sessions excluding the current one, to avoid echoing the live conversation while recording continues.
        RequestMemoryChunkDTO memDto = dto.memoryChunkDTO();
        MemoryAggregationOptions options = dto.options().toBuilder();
        UUID currentSessionId = memDto.sessionId();
        SessionMemoryEntity session = sessionService.getSessionPresent(currentSessionId, principal, false);

        Instant since = options.getSince();
        Instant until = options.getUntil();

        long totalTokenCount = currentUserSnapshot.getTokensUsed() + quotaService.countTokens(memDto.content()) + MAX_TOKENS_PER_QUERY;

        if (currentUserSnapshot.getTokenLimit() <= totalTokenCount) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot process - User has surpassed their allowed quota limits");
        }

        // 1) Ensure we have a session for this principal
        List<SessionMemoryEntity> ourSessions = new ArrayList<>();
        try {
            ourSessions = sessionService.getUserSessions(principal, since, until);
        } catch (Exception ex) {
            log.warn(ex.getMessage());
        }

        // 2) Build LLM context from past chunks in that session
        List<LlmMessage> messages = new ArrayList<>();

        LinkedHashMap<UUID, List<MemoryChunkEntity>> chunks = new LinkedHashMap<>();
        for (SessionMemoryEntity iterSesh : ourSessions) {
            if (currentSessionId != null && currentSessionId.equals(iterSesh.getSessionId()) && iterSesh.getSessionState() == SessionState.ACTIVE) { continue; }
            chunks.put(iterSesh.getSessionId(), memoryAggregationService.aggregate(session, options));
        }

        String contextText = buildContextFromChunks(chunks);

        String formattedContext = """
                            You are MondayMemory, a personal memory assistant.
                            You are given prior context from prior user sessions. Use it when relevant.
                            
                            Keep the max token number at: %d
                            
                            Prior context (most recent first):
                            %s
                            """.formatted(MAX_TOKENS_PER_QUERY, contextText);

        if (!contextText.equalsIgnoreCase("No prior context.")) {
            messages.add(new LlmMessage(
                    LlmMessage.Role.SYSTEM,
                    formattedContext
            ));
        }

        messages.add(new LlmMessage(LlmMessage.Role.USER, memDto.content()));

        LlmRequestDTO llmRequestDTO = new LlmRequestDTO(
                null,                        // model override (use default)
                messages,
                null,                        // temperature
                null,                        // maxTokens/timeout
                principal.getPrincipalId().toString(),// userId
                memDto.sessionId() != null ? memDto.sessionId().toString() : null,  // sessionId
                null,                        // metadata
                null                         // providerOverride
        );

        LlmResponseDTO llmResponseDTO = llmClient.chat(llmRequestDTO);
        quotaService.incrementTokensUsed(principal.getUserPlan(), llmResponseDTO.usage().totalTokens());

        Instant responseAt = Instant.now();

        ResponseMemoryChunkDTO response = new ResponseMemoryChunkDTO(memoryChunkUtils.toJson(Map.of(
                "kind", "chat_message",
                "role", "ASSISTANT",
                "text", llmResponseDTO.content(),
                "source", memDto.source()
        )), principal.getPrincipalType(), principal.getPrincipalId().toString(), responseAt, null);

        // If we're still recording, now is the time that we incorporate the session data.
        if (session == null) {
            return response;
        }

        MemoryChunkEntity userChunk =
                memoryChunkUtils.forUserMessage(session, memDto.content(), memDto.source().toString());
        MemoryChunkEntity assistantChunk =
                memoryChunkUtils.forAssistantMessage(session, llmResponseDTO.content(), memDto.source().toString());

        memoryChunkRepository.saveAll(List.of(userChunk, assistantChunk));

        // 4) Map to response DTO
        sessionService.updateChunkCount(assistantChunk.getSession(), 2);

        // TODO : Tags ignored for now
        return response;
    }

    /**
     * Handle pure "record memory" without LLM call if you have that case.
     */
    public ResponseMemoryChunkDTO recordOnly(PrincipalContext principal,
                                             RequestMemoryChunkDTO dto) {
        SessionMemoryEntity session = resolveOrCreateSession(principal, dto);
        if (session.getSessionState() != SessionState.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot append memory to closed session");
        }

        MemoryChunkEntity chunk = createChunkFromDto(principal, session, dto);
        memoryChunkRepository.save(chunk);

        return new ResponseMemoryChunkDTO(dto.content(),principal.getPrincipalType(),principal.getPrincipalId().toString(), chunk.getOccurredAt(), null);
    }

    // ---------------- internal helpers ----------------

    private SessionMemoryEntity findSession(PrincipalContext principal, UUID sessionId, boolean throwExceptionOnFail) {
        SessionMemoryEntity existing =
                sessionService.getSessionPresent(sessionId, principal, false);
        if (existing != null) {
            if (!Objects.equals(existing.getPrincipalId(), principal.getPrincipalId().toString())
                    || existing.getPrincipalType() != principal.getPrincipalType()) {
                if (throwExceptionOnFail) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User and Principal did not match up");
                }
                return null;
            }

            // This is the freshest version of our user entity. Thus, we should represent it as so
            UserPlanEntity userPlanEntity = existing.getUser() == null ? null : existing.getUser().getUserPlan();
            QuotaSnapshot snapshot = quotaService.snapshotFor(userPlanEntity);
            SessionOptionsEntity options = existing.getOptions();
            Long maxChunks = (options != null) ? options.getMaxChunksPerSession() : null;

            if (maxChunks != null && maxChunks <= existing.getChunkCount()) {
                if (throwExceptionOnFail) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Went past our max chunks");
                }
                return null;
            }
            if (snapshot == null) {
                if (throwExceptionOnFail) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Snapshot is considered null");
                }
                return null;
            }
            if (quotaService.decide(snapshot) == QuotaDecision.BLOCK) {
                if (throwExceptionOnFail) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot process quota because it's reached it's limits on tokens/topics");
                }
                return null;
            }
            return existing;
        } else {
            if (throwExceptionOnFail) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Session Id has either been expired or lost");
            }
        }
        return null;
    }

    private SessionMemoryEntity resolveOrCreateSession(PrincipalContext principal,
                                                       RequestMemoryChunkDTO dto) {
        if (dto.sessionId() != null) {
            return findSession(principal, dto.sessionId(), true);
        }

        // If we don't have an existing session, create one
        CreateSessionRequestDTO createReq = new CreateSessionRequestDTO(
                principal.getExternalGuestKey(),
                dto.sourceConversationKey(),
                dto.source(),
                dto.sourceConversationKey(),
                principal.getRecordingScope(),
                new SessionOptionRequestDTO(SessionScope.CHANNEL, 10L)
        );

        SessionMemoryResponseDTO sessionDTO =
                sessionService.createOrReuseSession(principal, createReq, null);

        if (!sessionDTO.statusCode().is2xxSuccessful()
                || sessionDTO.sessionIds() == null
                || sessionDTO.sessionIds().isEmpty()) {
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

        Map<String, Object> content = new HashMap<>();
        content.put("body", dto.content());
        content.put("principalType", principalType.name());
        content.put("principalId", (principal.getPrincipalId() == null) ? "" : principalId);
        content.put("guestKey", principal.getExternalGuestKey());
        content.put("source", dto.source().toString());
        content.put("sourceConversationKey", dto.sourceConversationKey());

        // however you're currently serializing to JSON or some column:
        newChunk.setContent(content);

        String normalizedText = normalize(dto.content());
        newChunk.setHashSha256(sha256(normalizedText));

        // Increment session chunk count here if you’re tracking it
        session.setChunkCount(session.getChunkCount() + 1);

        return newChunk;
    }

    private String buildContextFromChunks(LinkedHashMap<UUID,List<MemoryChunkEntity>> chunks) {
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
