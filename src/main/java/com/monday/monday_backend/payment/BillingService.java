package com.monday.monday_backend.payment;

import com.monday.monday_backend.auth.users.UserRepository;
import com.monday.monday_backend.payment.entity.PaymentEvent;
import com.monday.monday_backend.payment.entity.PricePlanEntity;
import com.monday.monday_backend.payment.entity.UserPlanEntity;
import com.monday.monday_backend.payment.repo.PaymentEventRepository;
import com.monday.monday_backend.payment.repo.PricePlanRepository;
import com.monday.monday_backend.payment.repo.UserPlanRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.*;
import com.stripe.model.checkout.Session;
import com.stripe.param.SubscriptionRetrieveParams;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BillingService {

    private final UserRepository userRepository;
    private final UserPlanRepository userPlanRepository;
    private final PaymentEventRepository paymentEventRepository;
    private final PricePlanRepository pricePlanRepository;

    @Transactional
    public void handleStripeEvent(Event event) {
        // First, we persist the audit
        if (paymentEventRepository.existsByStripeEventId(event.getId())) {
            return;
        }
        PaymentEvent pe = new PaymentEvent();
        pe.setStripeEventId(event.getId());
        pe.setType(event.getType());
        pe.setReceivedAt(Instant.now());
        pe.setPayloadJson(event.toJson());
        paymentEventRepository.save(pe);

        switch (event.getType()) {
            case "checkout.session.completed" -> onCheckoutCompleted(event);
            case "invoice.paid"              -> onInvoicePaid(event);
            case "customer.subscription.deleted",
                 "customer.subscription.updated" -> onSubUpdated(event);
            default -> { /* log and ignore for now */ }
        }
    }

    public void onCheckoutCompleted(Event event) {
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

        upsertPro(resolveUserIdFromCustomer(customerId), subId, customerId, periodEnd, pricePlanEntity);
    }

    private void onInvoicePaid(Event event) {
        Invoice inv = (Invoice) event.getDataObjectDeserializer().getObject().orElseThrow();
        String customerId = inv.getCustomer();
        // TODO: I need to confirm that inventory is actually getting the subscription.
        String subId = inv.getMetadata().get("subscription"); // inv.getSubscription();
        Instant periodEnd = Instant.ofEpochSecond(inv.getLines().getData().get(0).getPeriod().getEnd());

        String priceId = inv.getLines().getData().get(0).getMetadata().get("PRICE_ID");
        PricePlanEntity pricePlanEntity = pricePlanRepository.findByStripePriceId(priceId).orElseThrow(() -> new IllegalStateException("unknown prices provided"));

        upsertPro(resolveUserIdFromCustomer(customerId), customerId, subId, periodEnd, pricePlanEntity);
    }

    private void onSubUpdated(Event event) {
        Subscription sub = (Subscription) event.getDataObjectDeserializer().getObject().orElseThrow();
        String customerId = sub.getCustomer();
        String priceId = sub.getItems().getData().get(0).getPrice().getId();

        PricePlanEntity pricePlanEntity = pricePlanRepository.findByStripePriceId(priceId).orElseThrow(() -> new IllegalStateException("unknown prices provided"));

        Instant periodEnd = Instant.ofEpochSecond(sub.getTrialEnd());

        setTier(resolveUserIdFromCustomer(customerId), pricePlanEntity, customerId, sub.getId(), periodEnd);
    }

    private void upsertPro(UUID userId, String subId, String customerId, Instant periodEnd, PricePlanEntity pricePlanEntity) {

        UserPlanEntity userPlan = userPlanRepository.findByUser_UserId(userId).orElseGet(() -> {
            UserPlanEntity p = new UserPlanEntity();
            p.setUser(userRepository.findByUserId(userId).orElseThrow(()->new RuntimeException("User Id Doesn't exist!"))); p.setPlan(pricePlanEntity);
            p.setUpdatedAt(Instant.now());
            return p;
        });

        userPlan.setStripeCustomerId(customerId);
        userPlan.setStripeSubscriptionId(subId);
        if (periodEnd != null) userPlan.setCurrentPeriodEnd(periodEnd);
        userPlan.setUpdatedAt(Instant.now());
        userPlanRepository.save(userPlan);
    }

    private void setTier(UUID userId, PricePlanEntity tier, String cust, String sub, Instant periodEnd) {
        userPlanRepository.findByUser_UserId(userId).ifPresent(plan -> {
            plan.setPlan(tier);
            plan.setStripeCustomerId(cust);
            plan.setStripeSubscriptionId(sub);
            plan.setCurrentPeriodEnd(periodEnd);
            plan.setUpdatedAt(Instant.now());
            userPlanRepository.save(plan);
        });
    }

    private UUID resolveUserIdFromCustomer(String customerId) {
        return userPlanRepository.findByStripeCustomerId(customerId)
                .map(UserPlanEntity::getId)
                .orElse(null);
    }
}
