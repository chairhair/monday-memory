package com.monday.monday_backend.auth.dto;


import org.springframework.http.HttpStatus;

public record VerificationDTO(HttpStatus statusCode, String explanation) {
}
