package com.monday.monday_backend.query.user.dto;

import java.util.Map;

public record QueryUserRequestDTO(String sessionId, String queryText, Map<String, String> metadata, Boolean autoInject) {
}
