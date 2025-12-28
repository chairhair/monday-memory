package com.monday.monday_backend.payment;

import com.monday.monday_backend.auth.principal.AuthUser;
import com.monday.monday_backend.auth.users.UserEntity;
import com.monday.monday_backend.auth.users.UserRepository;
import com.monday.monday_backend.payment.config.StripeConfiguration;
import com.monday.monday_backend.payment.core.PaymentProvider;
import com.monday.monday_backend.payment.entity.PricePlanEntity;
import com.monday.monday_backend.payment.entity.UserPlanEntity;
import com.monday.monday_backend.payment.repo.PricePlanRepository;
import com.monday.monday_backend.payment.repo.UserPlanRepository;
import com.monday.shared.payment.dto.StartCheckoutResponseDTO;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.checkout.Session;
import com.stripe.param.CustomerUpdateParams;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


@Service
@RequiredArgsConstructor
public class PaymentService implements PaymentProvider {

    private final StripeConfiguration cfg;

    private final UserRepository userRepository;
    private final UserPlanRepository userPlanRepository;
    private final PricePlanRepository pricePlanRepository;

    @Override
    public StartCheckoutResponseDTO createSubscriptionCheckout(AuthUser authUser, String pricePlan, String successUrl, String cancelUrl) throws RuntimeException, StripeException {
        if (authUser.id() == null) {
            throw new IllegalStateException("Cannot use this if we don't have a user Id that's present");
        }
        UUID authUserId = UUID.fromString(authUser.id());
        UserEntity user = userRepository.findByUserId(authUserId).orElseThrow(() -> new IllegalArgumentException("User not found!"));
        if (user.getUserPlan() == null) {
            throw new IllegalStateException("Cannot continue - User does not have a plan associated with their account");
        }
        UserPlanEntity userPlan = user.getUserPlan();

        String knownPlanId = userPlan.getId().toString();
        PricePlanEntity pricePlanEntity = pricePlanRepository.findByCode(pricePlan).orElseThrow(() -> new IllegalArgumentException("Cannot find price plan: "+pricePlan));

        Customer customer = (userPlan.getStripeCustomerId() != null) ? null : Customer.create(new HashMap<>());

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                .setCustomer((customer == null) ? userPlan.getStripeCustomerId()  : customer.getId())
                .setSuccessUrl(successUrl + "?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(cancelUrl)
                .setClientReferenceId(knownPlanId)
                .setCustomerEmail(authUser.email())
                .putMetadata("userId", user.getUserId().toString())
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setPrice(pricePlanEntity.getStripePriceId())
                        .setQuantity(1L)
                        .build())
                .build();
        Session session = Session.create(params);

        String customerId = session.getCustomer();
        if (userPlan.getStripeCustomerId() == null) {
            userPlan.setStripeCustomerId(customerId);
        }
        userPlanRepository.save(userPlan);
        return new StartCheckoutResponseDTO(session.getUrl(), session.getId());
    }
}
