package com.monday.monday_backend.llm.services;

import com.monday.monday_backend.llm.LlmClient;
import com.monday.shared.llm.LlmMessage;
import com.monday.shared.llm.LlmRequestDTO;
import com.monday.shared.llm.LlmResponseDTO;
import com.monday.shared.openai.OpenAiChatRequest;
import com.monday.shared.openai.OpenAiChatResponse;
import org.apache.http.HttpHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Primary
@Service
@ConditionalOnProperty(name = "llm.provider", havingValue = "openai")
public class OpenAiLlmClient implements LlmClient {

    private final WebClient webClient;
    private final String defaultModel;
    private final Duration defaultTimeout;

    public OpenAiLlmClient(
            WebClient.Builder webClientBuilder,
            @Value("${openai.base-url}") String baseUrl,
            @Value("${openai.api-key}") String apiKey,
            @Value("${openai.default-model:gpt-5.1}") String defaultModel,
            @Value("${llm.model.timeout}") long timeoutSeconds
    ) {
        this.defaultModel = defaultModel;
        this.defaultTimeout = Duration.of(timeoutSeconds, ChronoUnit.SECONDS);
        this.webClient = webClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer "+apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

    }

    @Override
    public LlmResponseDTO chat(LlmRequestDTO requestDTO) {
        String model = requestDTO.model() != null ? requestDTO.model() : defaultModel;
        Double temperature = requestDTO.temperature() != null ? requestDTO.temperature() : 0.7;

        OpenAiChatRequest payload = new OpenAiChatRequest(
                model,
                mapMessages(requestDTO.messages()),
                temperature
        );

        OpenAiChatResponse response = webClient.post()
                .uri("/v1/chat/completions")
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(OpenAiChatResponse.class)
                .timeout(defaultTimeout)
                .onErrorResume(ex -> {
                    // TODO: add better logging + custom exception
                    return Mono.error(new RuntimeException("LLM call failed", ex));
                })
                .block();

        if (response == null || response.choices().isEmpty()) {
            throw new IllegalStateException("Empty response from OpenAI");
        }

        OpenAiChatResponse.Choice first = response.choices().get(0);

        return new LlmResponseDTO(
                first.message().content(),
                response.model(),
                new LlmResponseDTO.Usage(
                        response.usage() != null ? response.usage().prompt_tokens() : null,
                        response.usage() != null ? response.usage().completion_tokens() : null,
                        response.usage() != null ? response.usage().total_tokens() : null
                ),
                false // fromCache – will matter when you add caching
        );
    }

    private List<OpenAiChatRequest.Message> mapMessages(List<LlmMessage> messages) {
        return messages.stream()
                .map(m -> new OpenAiChatRequest.Message(
                        mapRole(m.role()),
                        m.content()
                )).toList();
    }

    private String mapRole(LlmMessage.Role role) {
        return switch(role) {
            case SYSTEM -> "system";
            case USER -> "user";
            case ASSISTANT -> "assistant";
        };
    }
}
