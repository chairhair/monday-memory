package com.monday.monday_backend.auth.dto;


import org.springframework.http.HttpStatus;

import java.util.Map;

public record VerificationResponseDTO(int statusCode, Map<String, Object> authentication) {
    public static VerificationResponseDTO successfulDTO(Map<String, Object> authentication) {return new VerificationResponseDTO(HttpStatus.OK.value(), authentication);}
}
