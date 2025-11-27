package com.monday.monday_backend.llm.services;

import com.monday.monday_backend.llm.LlmClient;
import com.monday.shared.llm.LlmRequestDTO;
import com.monday.shared.llm.LlmResponseDTO;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "llm.provider", havingValue = "vllm")
public class RemoteLlmClient implements LlmClient {
    @Override
    public LlmResponseDTO chat(LlmRequestDTO requestDTO) {
        return null;
    }
}
