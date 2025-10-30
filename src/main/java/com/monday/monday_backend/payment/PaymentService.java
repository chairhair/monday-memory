package com.monday.monday_backend.payment;

import com.monday.monday_backend.payment.core.PaymentProvider;
import com.monday.monday_backend.payment.dto.CreateCheckoutResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentService implements PaymentProvider {

    @Override
    public CreateCheckoutResponse createSubscriptionCheckout(String userId, String successUrl, String cancelUrl) {
        return null;
    }
}
