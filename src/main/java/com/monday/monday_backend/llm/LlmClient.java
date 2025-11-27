package com.monday.monday_backend.llm;

import com.monday.shared.llm.LlmRequestDTO;
import com.monday.shared.llm.LlmResponseDTO;

public interface LlmClient {

    LlmResponseDTO chat(LlmRequestDTO requestDTO);


}
