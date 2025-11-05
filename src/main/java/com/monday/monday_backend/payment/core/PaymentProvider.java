package com.monday.monday_backend.payment.core;

import com.monday.monday_backend.payment.dto.StartCheckoutResponseDTO;
import com.stripe.exception.StripeException;

public interface PaymentProvider {
    StartCheckoutResponseDTO createSubscriptionCheckout(Long userId, String pricePlan, String successUrl, String cancelUrl) throws StripeException;
}
