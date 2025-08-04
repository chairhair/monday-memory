package com.monday.monday_backend.auth.dto;

public record UserRequestDTO(Long uuid, String serviceName, String emailAddress, String password) {
}
