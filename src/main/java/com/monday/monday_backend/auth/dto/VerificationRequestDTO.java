package com.monday.monday_backend.auth.dto;


import org.springframework.http.HttpStatus;

public record VerificationRequestDTO(String serviceName, String requestedRole, String email, String password) {
    public static VerificationRequestDTO guestVerification(String serviceName, String requestedRole) {
        return new VerificationRequestDTO(serviceName, requestedRole, null, null);
    }

    public boolean isGuest() {
        return email == null && password == null;
    }
}
