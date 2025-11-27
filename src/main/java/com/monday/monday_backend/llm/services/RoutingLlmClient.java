package com.monday.monday_backend.llm.services;

import com.monday.monday_backend.llm.LlmClient;
import com.monday.shared.llm.LlmRequestDTO;
import com.monday.shared.llm.LlmResponseDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "llm.provider", havingValue = "routing")
public class RoutingLlmClient implements LlmClient {

    private final OpenAiLlmClient openAi;
    private final RemoteLlmClient vllm;
    private final String defaultProvider;

    public RoutingLlmClient(
            OpenAiLlmClient openAi,
            RemoteLlmClient vllm,
            @Value("${llm.provider.default:openai}") String defaultProvider
    ) {
        this.openAi = openAi;
        this.vllm = vllm;
        this.defaultProvider = defaultProvider;
    }

    @Override
    public LlmResponseDTO chat(LlmRequestDTO requestDTO) {
        String provider = requestDTO.providerOverride() != null ? requestDTO.providerOverride() : defaultProvider;
        return switch(provider) {
            case "openai" -> openAi.chat(requestDTO);
            case "vllm" -> vllm.chat(requestDTO);
            default -> null;
        };
    }
}
