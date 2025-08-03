package com.monday.monday_backend.auth.dto;


import org.springframework.http.HttpStatus;

public record VerificationRequestDTO(String serviceName, String role, String userName, String password) {
    public static VerificationRequestDTO guestVerification(String serviceName, String role) {
        return new VerificationRequestDTO(serviceName, role, null, null);
    }
}
