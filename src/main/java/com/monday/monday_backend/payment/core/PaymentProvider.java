package com.monday.monday_backend.payment.core;

import com.monday.monday_backend.payment.dto.CreateCheckoutResponse;

public interface PaymentProvider {
    CreateCheckoutResponse createSubscriptionCheckout(String userId, String successUrl, String cancelUrl);
}
