package com.monday.monday_backend.payment.core;

import com.monday.shared.payment.dto.StartCheckoutResponseDTO;
import com.stripe.exception.StripeException;

import java.util.UUID;

public interface PaymentProvider {
    StartCheckoutResponseDTO createSubscriptionCheckout(UUID userId, String pricePlan, String successUrl, String cancelUrl) throws StripeException;
}
