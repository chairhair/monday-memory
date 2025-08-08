package com.monday.monday_backend.query.guest.dto;

import com.monday.monday_backend.query.utils.ServiceName;

public record QueryGuestRequestDTO(ServiceName serviceName, String query, String sessionId) {
}
