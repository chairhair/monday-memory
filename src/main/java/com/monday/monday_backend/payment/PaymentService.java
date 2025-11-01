package com.monday.monday_backend.payment;

import com.monday.monday_backend.auth.users.UserRepository;
import com.monday.monday_backend.payment.config.StripeConfiguration;
import com.monday.monday_backend.payment.core.PaymentProvider;
import com.monday.monday_backend.payment.dto.CreateCheckoutResponse;
import com.monday.monday_backend.payment.repo.PaymentEventRepository;
import com.monday.monday_backend.payment.repo.UserPlanRepository;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class PaymentService implements PaymentProvider {

    private final StripeConfiguration cfg;

    @Override
    public CreateCheckoutResponse createSubscriptionCheckout(String userId, String successUrl, String cancelUrl) {
        Map<String, String> meta = Map.of("userId", userId);

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                .setSuccessUrl(successUrl + "?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(cancelUrl)
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setPrice(cfg.getPriceIdMonthly())
                        .setQuantity(1L)
                        .build())    // Creates Monthly Program
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setPrice(cfg.getPriceIdAnnually())
                        .setQuantity(1L)
                        .build())    // Creates Monthly Program
                .putAllMetadata(meta)
                .build();
        try {
            Session session = Session.create(params);
            return new CreateCheckoutResponse(session.getUrl(), session.getId());
        } catch (Exception e) {
            throw new RuntimeException("Stripe checkout create failed", e);
        }
    }
}
