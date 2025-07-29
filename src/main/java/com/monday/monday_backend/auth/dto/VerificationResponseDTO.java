package com.monday.monday_backend.auth.dto;


import org.springframework.http.HttpStatus;

public record VerificationResponseDTO(HttpStatus statusCode, String token, String explanation) {
    public static VerificationResponseDTO successfulDTO(String token) {return new VerificationResponseDTO(HttpStatus.ACCEPTED, token, "");}
}
