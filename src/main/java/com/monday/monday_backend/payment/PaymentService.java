package com.monday.monday_backend.payment;

import com.monday.monday_backend.payment.core.PaymentProvider;
import com.monday.monday_backend.payment.dto.CreateCheckoutResponse;
import com.stripe.param.billingportal.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class PaymentService implements PaymentProvider {

    @Override
    public CreateCheckoutResponse createSubscriptionCheckout(String userId, String successUrl, String cancelUrl) {
        Map<String, Object> meta = Map.of("userId", userId);

        SessionCreateParams params = SessionCreateParams.builder()

                .build();
        return null;
    }
}
