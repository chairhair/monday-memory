package com.monday.monday_backend.auth.dto;


import org.springframework.http.HttpStatus;

import java.util.HashMap;
import java.util.Map;

public record VerificationResponseDTO(int statusCode, Map<String, Object> authentication) {
    public static VerificationResponseDTO successfulDTO(Map<String, Object> authentication) {return new VerificationResponseDTO(HttpStatus.OK.value(), authentication);}
    public static VerificationResponseDTO failedDTO(int statusCode, String reason) {return new VerificationResponseDTO(statusCode, Map.of("explanation",reason));}
}
