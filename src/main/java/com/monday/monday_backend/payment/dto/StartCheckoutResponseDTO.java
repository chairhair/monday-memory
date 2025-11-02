package com.monday.monday_backend.payment.dto;

public record StartCheckoutResponseDTO(String checkoutUrl, String sessionId) {
}
