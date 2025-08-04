package com.monday.monday_backend.auth.dto;

public record UserRequestDTO(String serviceName, String userName, String password) {
    public boolean isGuest() {
        return userName == null && password == null;
    }
}
