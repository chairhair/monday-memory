package com.monday.monday_backend.auth.dto;


import org.springframework.http.HttpStatus;

public record VerificationRequestDTO(String userName, String password) {
}
