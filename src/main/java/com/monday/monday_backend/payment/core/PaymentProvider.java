package com.monday.monday_backend.payment.core;

import com.monday.monday_backend.auth.principal.AuthUser;
import com.monday.shared.payment.dto.StartCheckoutResponseDTO;
import com.stripe.exception.StripeException;

public interface PaymentProvider {
    StartCheckoutResponseDTO createSubscriptionCheckout(AuthUser authUser, String pricePlan, String successUrl, String cancelUrl) throws StripeException;
}
