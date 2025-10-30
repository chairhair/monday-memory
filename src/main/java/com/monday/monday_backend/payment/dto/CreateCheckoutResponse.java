package com.monday.monday_backend.payment.dto;

public record CreateCheckoutResponse(String checkoutUrl, String sessionId) {
}
