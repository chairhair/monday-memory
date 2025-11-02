package com.monday.monday_backend.payment.core;

import com.monday.monday_backend.payment.dto.StartCheckoutResponseDTO;

public interface PaymentProvider {
    StartCheckoutResponseDTO createSubscriptionCheckout(Long userId, String pricePlan, String successUrl, String cancelUrl);
}
