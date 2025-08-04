package com.monday.monday_backend.auth.dto;

import com.monday.monday_backend.auth.roles.AccessLevel;
import org.springframework.http.HttpStatus;

import java.util.Map;
import java.util.Set;

public record UserResponseDTO(int statusCode, Map<String,Object> response, String email, String serviceName, Set<AccessLevel> roles, Set<String> tokens) {
    public static UserResponseDTO successfulDTO(String email, String serviceName, Set<AccessLevel> roles, Set<String> tokens) {return new UserResponseDTO(HttpStatus.OK.value(), null, email, serviceName, roles, tokens);}
    public static UserResponseDTO failedDTO(int statusCode, String reason) {return new UserResponseDTO(statusCode, Map.of("explanation", reason),  null, null, null, null);}
}
