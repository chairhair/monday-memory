package com.monday.monday_backend.payment.dto;

public record StartCheckoutRequestDTO(String planCode, String successUrl, String cancelUrl) {
}
