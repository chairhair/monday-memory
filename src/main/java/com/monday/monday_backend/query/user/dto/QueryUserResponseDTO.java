package com.monday.monday_backend.query.user.dto;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

public record QueryUserResponseDTO(int statusCode, String queryId, String injectedPrompt, String[] rawMemory, Integer tokensUsed, String userId) {
    public static QueryUserResponseDTO successfulDTO(String queryId, String injectedPrompt, String[] rawMemory, Integer tokensUsed, String userId) {
        return new QueryUserResponseDTO(HttpStatus.OK.value(), queryId, injectedPrompt, rawMemory, tokensUsed, userId);
    }

    public static QueryUserResponseDTO failedDTO(HttpStatusCode statusCode, String queryId, String injectedPrompt, String[] rawMemory, Integer tokensUsed, String userId) {
        return new QueryUserResponseDTO(statusCode.value(), queryId, injectedPrompt, rawMemory, tokensUsed, userId);
    }
}
