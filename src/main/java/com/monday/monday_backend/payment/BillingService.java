package com.monday.monday_backend.payment;

import com.monday.monday_backend.analytics.AnalyticsEventEntity;
import com.monday.monday_backend.analytics.AnalyticsRepository;
import com.monday.monday_backend.auth.principal.AuthUser;
import com.monday.monday_backend.auth.users.UserEntity;
import com.monday.monday_backend.auth.users.UserRepository;
import com.monday.monday_backend.payment.entity.PaymentEvent;
import com.monday.monday_backend.payment.entity.PricePlanEntity;
import com.monday.monday_backend.payment.entity.UserPlanEntity;
import com.monday.monday_backend.payment.repo.PaymentEventRepository;
import com.monday.monday_backend.payment.repo.PricePlanRepository;
import com.monday.monday_backend.payment.repo.UserPlanRepository;
import com.monday.shared.analytics.AnalyticsEventName;
import com.monday.shared.memory.session.utils.PrincipalType;
import com.stripe.exception.StripeException;
import com.stripe.model.*;
import com.stripe.model.checkout.Session;
import com.stripe.param.SubscriptionRetrieveParams;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BillingService {

    private final AnalyticsRepository analyticsRepository;
    private final UserRepository userRepository;
    private final UserPlanRepository userPlanRepository;
    private final PaymentEventRepository paymentEventRepository;
    private final PricePlanRepository pricePlanRepository;

    @Transactional
    public void handleStripeEvent(AuthUser authUser, Event event) {
        // First, we persist the audit
        if (paymentEventRepository.existsByStripeEventId(event.getId())) {
            return;
        }

        UserEntity user = userRepository.findByUserId(UUID.fromString(authUser.id())).orElseThrow(() -> new RuntimeException("ERROR: Payment went through, but user could not be saved to payment repository. Authenticated user having difficulty."));

        PaymentEvent pe = new PaymentEvent();
        pe.setStripeEventId(event.getId());
        pe.setType(event.getType());
        pe.setReceivedAt(Instant.now());
        pe.setPayloadJson(event.toJson());
        pe.setUser(user);
        paymentEventRepository.save(pe);

        saveAnalyticsEvent(authUser.id(), "Payment Event logged under user");

        switch (event.getType()) {
            case "checkout.session.completed" -> onCheckoutCompleted(user, event);
            case "invoice.paid"              -> onInvoicePaid(user, event);
            case "customer.subscription.deleted",
                 "customer.subscription.updated" -> onSubUpdated(user, event);
            default -> { /* log and ignore for now */ }
        }
    }

    public void onCheckoutCompleted(UserEntity user, Event event) {
        // 1) Get the Checkout Session from the webhook payload
        EventDataObjectDeserializer des = event.getDataObjectDeserializer();
        StripeObject obj = des.getObject().orElseThrow(() -> new IllegalStateException("checkout.session.completed without object"));
        if (!(obj instanceof Session session)) {
            throw new IllegalStateException("Webhook object is not a Checkout Session");
        }

        final String customerId = session.getCustomer();
        final String subId = session.getSubscription();

        if (customerId == null || subId == null) {
            throw new IllegalStateException("Missing customer or subscription on session");
        }

        // 2) Retrieve Subscription with expanded price so we can map plan
        SubscriptionRetrieveParams subParams = SubscriptionRetrieveParams.builder()
                .addExpand("items.data.price")
                .build();
        Subscription sub = null;
        try {
            sub = Subscription.retrieve(subId, subParams, null);
        } catch (StripeException e) {
            throw new IllegalStateException("Subscription doesn't exist: "+subId, e);
        }

        String priceId = sub.getItems().getData().get(0).getPrice().getId();
        Instant periodEnd = Instant.ofEpochSecond(sub.getTrialEnd());

        // 3) Now, map Stripe price -> Price Plan Entity in your DB
        PricePlanEntity pricePlanEntity = pricePlanRepository.findByStripePriceId(priceId).orElseThrow(() -> new IllegalStateException("Unknown stripe price: " + priceId));

        upsertPro(user, subId, customerId, periodEnd, pricePlanEntity);
    }

    private void onInvoicePaid(UserEntity user, Event event) {
        Invoice inv = (Invoice) event.getDataObjectDeserializer().getObject().orElseThrow();
        String customerId = inv.getCustomer();
        String subId = inv.getMetadata().get("subscription"); // inv.getSubscription();
        Instant periodEnd = Instant.ofEpochSecond(inv.getLines().getData().get(0).getPeriod().getEnd());

        String priceId = inv.getLines().getData().get(0).getMetadata().get("PRICE_ID");
        PricePlanEntity pricePlanEntity = pricePlanRepository.findByStripePriceId(priceId).orElseThrow(() -> new IllegalStateException("unknown prices provided"));

        upsertPro(user, customerId, subId, periodEnd, pricePlanEntity);
    }

    private void onSubUpdated(UserEntity user, Event event) {
        Subscription sub = (Subscription) event.getDataObjectDeserializer().getObject().orElseThrow();
        String customerId = sub.getCustomer();
        String priceId = sub.getItems().getData().get(0).getPrice().getId();

        PricePlanEntity pricePlanEntity = pricePlanRepository.findByStripePriceId(priceId).orElseThrow(() -> new IllegalStateException("unknown prices provided"));

        Instant periodEnd = Instant.ofEpochSecond(sub.getTrialEnd());

        setTier(user, pricePlanEntity, customerId, sub.getId(), periodEnd);
    }

    private void upsertPro(UserEntity user, String subId, String customerId, Instant periodEnd, PricePlanEntity pricePlanEntity) {
        UserPlanEntity plan = user.getUserPlan();
        if (plan == null) {
            plan = new UserPlanEntity();
        }
        plan.setUser(user);
        plan.setPlan(pricePlanEntity);
        plan.setStripeCustomerId(customerId);
        plan.setStripeSubscriptionId(subId);
        plan.setCurrentPeriodEnd(periodEnd);
        plan.setUpdatedAt(Instant.now());
        if (periodEnd != null) plan.setCurrentPeriodEnd(periodEnd);
        userPlanRepository.save(plan);
        user.setUserPlan(plan);
        userRepository.save(user);

        saveAnalyticsEvent(user.getUserId().toString(), "Customer subscribed to a new plan: "+pricePlanEntity.getDisplayName());
    }

    private void setTier(UserEntity user, PricePlanEntity tier, String cust, String sub, Instant periodEnd) {
        UserPlanEntity plan = user.getUserPlan();
        if (plan == null) {
            plan = new UserPlanEntity();
        }
        plan.setPlan(tier);
        plan.setStripeCustomerId(cust);
        plan.setStripeSubscriptionId(sub);
        plan.setCurrentPeriodEnd(periodEnd);
        plan.setUpdatedAt(Instant.now());
        userPlanRepository.save(plan);
        user.setUserPlan(plan);
        userRepository.save(user);

        saveAnalyticsEvent(user.getUserId().toString(), "Customer updated to a new plan: "+tier.getDisplayName());
    }

    private void saveAnalyticsEvent(String userId, String message) {
        AnalyticsEventEntity analyticsEvent = new AnalyticsEventEntity();
        analyticsEvent.setEventName(AnalyticsEventName.PAYMENT);
        analyticsEvent.setPrincipalKey(userId);
        analyticsEvent.setPrincipalType(PrincipalType.USER);
        analyticsEvent.setMessage(message);
        analyticsEvent.setOccurredAt(Instant.now());

        analyticsRepository.save(analyticsEvent);
    }
}
