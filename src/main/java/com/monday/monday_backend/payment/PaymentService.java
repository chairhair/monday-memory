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
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import com.stripe.param.CustomerUpdateParams;
import com.stripe.param.SubscriptionCancelParams;
import com.stripe.param.SubscriptionUpdateParams;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
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

    @Value("${stripe.env.mode}")
    private String envMode;

    @Transactional
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
        PricePlanEntity pricePlanEntity = userPlan.getPlan();
        boolean isSamePricePlan = (pricePlanEntity.getCode().contains("PRO_MONTHLY") && pricePlan.equals("PRO_MONTHLY")) || (pricePlanEntity.getCode().contains("PRO_ANNUAL") && pricePlan.equals("PRO_ANNUAL"));
        if (isSamePricePlan) {
            throw new IllegalStateException("Cannot continue - User has already purchased this plan!");
        }

        String knownPlanId = userPlan.getId().toString();
        pricePlanEntity = pricePlanRepository.findByCode(pricePlan).orElseThrow(() -> new IllegalArgumentException("Cannot find price plan: "+pricePlan));

        SessionCreateParams.Builder params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                .setSuccessUrl(successUrl + "?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(cancelUrl)
                .setClientReferenceId(knownPlanId)
                .putMetadata("userId", user.getUserId().toString())
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setPrice(pricePlanEntity.getStripePriceId())
                        .setQuantity(1L)
                        .build());
        if (userPlan.getStripeCustomerId() != null) {
            params.setCustomer(userPlan.getStripeCustomerId());
        } else {
            if (envMode.equalsIgnoreCase("TEST")) {
                Customer c = Customer.create(new HashMap<>());
                userPlan.setStripeCustomerId(c.getId());
                userPlanRepository.save(userPlan);
            } else {
                params.setCustomerEmail(authUser.email());
            }
        }

        Session session = Session.create(params.build());

        if (userPlan.getStripeCustomerId() == null && session.getCustomer() != null) {
            userPlan.setStripeCustomerId(session.getCustomer());
            userPlanRepository.save(userPlan);
        }
        return new StartCheckoutResponseDTO(session.getUrl(), session.getId());
    }

    @Transactional
    public void requestCancelAtPeriodEnd(AuthUser authUser) throws StripeException {
        if (authUser.id() == null) {
            throw new IllegalStateException("Cannot use this if we don't have a user Id that's present");
        }
        UUID authUserId = UUID.fromString(authUser.id());
        UserEntity user = userRepository.findByUserId(authUserId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        UserPlanEntity plan = getPaidUserPlanEntity(user);

        Subscription sub = Subscription.retrieve(plan.getStripeSubscriptionId());

        SubscriptionCancelParams params = SubscriptionCancelParams.builder().build();
        sub.cancel(params);

        // Optional but recommended: mirror Stripe state locally
        plan.setStripeSubscriptionId(null);
        plan.setPlan(pricePlanRepository.findByCode("FREE_INTERNAL").orElseThrow(() -> new IllegalArgumentException("State not found")));
        userPlanRepository.save(plan);
        userPlanRepository.save(plan);
    }

    @NotNull
    private UserPlanEntity getPaidUserPlanEntity(UserEntity user) {
        UserPlanEntity plan = user.getUserPlan();
        if (plan == null) {
            throw new IllegalStateException("No active subscription to cancel.");
        }
        PricePlanEntity pricePlanEntity = plan.getPlan();
        if (pricePlanEntity == null || (!pricePlanEntity.getCode().contains("PRO") && envMode.equalsIgnoreCase("TEST"))) {
            if (plan.getStripeSubscriptionId() != null) {
                throw new IllegalStateException("Strange state identified - Subscription Stripe ID is present, but we're free.");
            }
            throw new IllegalStateException("Price Plan is not PRO");
        }
        if (plan.getStripeSubscriptionId() == null) {
            throw new IllegalStateException("No active subscription to cancel.");
        }
        return plan;
    }
}
