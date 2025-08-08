package com.monday.monday_backend.query.guest.dto;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

public record QueryGuestResponseDTO(int statusCode, String queryId, String injectedPrompt, String[] rawMemory) {
    public static QueryGuestResponseDTO successfulDTO(String queryId, String injectedPrompt, String[] rawMemory) {
        return new QueryGuestResponseDTO(HttpStatus.OK.value(), queryId, injectedPrompt, rawMemory);
    }

    public static QueryGuestResponseDTO failedDTO(HttpStatusCode statusCode, String queryId, String injectedPrompt, String[] rawMemory) {
        return new QueryGuestResponseDTO(statusCode.value(), queryId, injectedPrompt, rawMemory);
    }
}
