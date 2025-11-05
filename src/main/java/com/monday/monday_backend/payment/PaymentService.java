package com.monday.monday_backend.payment;

import com.monday.monday_backend.payment.config.StripeConfiguration;
import com.monday.monday_backend.payment.core.PaymentProvider;
import com.monday.monday_backend.payment.dto.StartCheckoutResponseDTO;
import com.monday.monday_backend.payment.entity.PricePlanEntity;
import com.monday.monday_backend.payment.entity.UserPlanEntity;
import com.monday.monday_backend.payment.repo.PricePlanRepository;
import com.monday.monday_backend.payment.repo.UserPlanRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class PaymentService implements PaymentProvider {

    private final StripeConfiguration cfg;

    private final UserPlanRepository userPlanRepository;
    private final PricePlanRepository pricePlanRepository;

    @Override
    public StartCheckoutResponseDTO createSubscriptionCheckout(Long userId, String pricePlan, String successUrl, String cancelUrl) throws RuntimeException, StripeException {

        UserPlanEntity userPlan = null;
        if (userId != null) {
            userPlan = userPlanRepository.findByUser_Id(userId).orElseThrow(() -> new IllegalArgumentException("User not found!"));
        }

        PricePlanEntity pricePlanEntity = pricePlanRepository.findByCode(pricePlan).orElseThrow(() -> new IllegalArgumentException("Cannot find price plan: "+pricePlan));

        String knownUserId = userPlan == null ? null : userPlan.getId().toString();

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                .setSuccessUrl(successUrl + "?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(cancelUrl)
                .setClientReferenceId(knownUserId)                   // may be null; Stripe accepts it
                .putMetadata("userId", String.valueOf(userId))  // metadata always String
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setPrice(pricePlanEntity.getStripePriceId())
                        .setQuantity(1L)
                        .build())
                .build();
        Session session = Session.create(params);
        return new StartCheckoutResponseDTO(session.getUrl(), session.getId());
    }
}
