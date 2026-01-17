package com.monday.monday_backend.payment;

import com.google.gson.JsonObject;
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
import java.util.List;
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
    public void handleStripeEvent(Event event) {
        // Idempotency: skip if we've already processed this event
        if (paymentEventRepository.existsByStripeEventId(event.getId())) {
            return;
        }

        EventDataObjectDeserializer deser = event.getDataObjectDeserializer();
        StripeObject obj = deser.getObject()
                .orElseThrow(() -> new IllegalStateException("Stripe event missing data object"));

        switch (event.getType()) {
            case "checkout.session.completed" -> onCheckoutCompleted((Session) obj, event);
            case "invoice.paid"               -> onInvoicePaid((Invoice) obj, event);
            case "customer.subscription.deleted",
                 "customer.subscription.updated" -> onSubUpdated((Subscription) obj, event);
            case "invoice.payment_failed" -> onInvoiceDeclined((Invoice) obj, event);
            default -> {
                // log + optionally persist PaymentEvent with type "IGNORED"
            }
        }
    }

    private void onCheckoutCompleted(Session session, Event event) {
        final String customerId = session.getCustomer();
        final String subId      = session.getSubscription();

        if (customerId == null || subId == null) {
            throw new IllegalStateException("Missing customer or subscription on session");
        }

        UserPlanEntity userPlan = userPlanRepository.findByStripeCustomerId(customerId).orElseThrow(() -> new RuntimeException("User Plan Id could not be identified. Throwing Runtime Exception..."));

        UserEntity user = savePayment(userPlan, event);

        SubscriptionRetrieveParams subParams = SubscriptionRetrieveParams.builder()
                .addExpand("items.data.price")
                .build();

        Subscription sub;
        try {
            sub = Subscription.retrieve(subId, subParams, null);
        } catch (StripeException e) {
            throw new IllegalStateException("Subscription doesn't exist: " + subId, e);
        }

        SubscriptionItem subItem = sub.getItems().getData().getFirst();
        Instant periodEnd = subItem.getCurrentPeriodEnd() != null
                ? Instant.ofEpochSecond(subItem.getCurrentPeriodEnd())
                : null;

        PricePlanEntity pricePlanEntity = pricePlanRepository.findByStripePriceId(subItem.getPrice().getId())
                .orElseThrow(() -> new IllegalStateException("Unknown stripe price: " + subItem.getPrice().getId()));

        upsertPro(user, sub.getId(), customerId, periodEnd, pricePlanEntity);
    }

    private void onInvoicePaid(Invoice inv, Event event) {
        String customerId = inv.getCustomer();
        String subId = (inv.getLines().getData().getFirst().getSubscription() != null)
                ? inv.getLines().getData().getFirst().getSubscription()
                : null;

        UserPlanEntity userPlan = userPlanRepository.findByStripeCustomerId(customerId).orElseThrow(() -> new RuntimeException("User Plan Id could not be identified. Throwing Runtime Exception..."));

        UserEntity user = savePayment(userPlan, event);

        InvoiceLineItem lineItem = inv.getLines().getData().getFirst();
        Instant periodEnd = Instant.ofEpochSecond(lineItem.getPeriod().getEnd());
        String priceId = lineItem.getPricing().getPriceDetails().getPrice();

        PricePlanEntity pricePlanEntity = pricePlanRepository.findByStripePriceId(priceId)
                .orElseThrow(() -> new IllegalStateException("unknown prices provided"));

        upsertPro(user, subId, customerId, periodEnd, pricePlanEntity);
    }

    private void onInvoiceDeclined(Invoice inv, Event event) {
        String customerId = inv.getCustomer();

        UserPlanEntity userPlan = userPlanRepository.findByStripeCustomerId(customerId).orElseThrow(() -> new RuntimeException("User Plan Id could not be identified. Throwing Runtime Exception..."));

        UserEntity user = userPlan.getUser();
        saveAnalyticsEvent(user, "Could not process credit card information because user's card declined");
    }

    private void onSubUpdated(Subscription sub, Event event) {
        String customerId = sub.getCustomer();
        String priceId    = sub.getItems().getData().getFirst().getPrice().getId();

        UserPlanEntity userPlan = userPlanRepository.findByStripeCustomerId(customerId).orElseThrow(() -> new RuntimeException("User Plan Id could not be identified. Throwing Runtime Exception..."));

        UserEntity user = savePayment(userPlan, event);

        PricePlanEntity pricePlanEntity = pricePlanRepository.findByStripePriceId(priceId)
                .orElseThrow(() -> new IllegalStateException("unknown prices provided"));

        SubscriptionItem subItem = sub.getItems().getData().getFirst();
        Instant periodEnd = subItem.getCurrentPeriodEnd() != null
                ? Instant.ofEpochSecond(subItem.getCurrentPeriodEnd())
                : null;

        setTier(user, pricePlanEntity, customerId, sub.getId(), periodEnd);
    }


    private void upsertPro(UserEntity user, String subId, String customerId, Instant periodEnd, PricePlanEntity pricePlanEntity) {
        UserPlanEntity plan = user.getUserPlan();
        if (plan == null) {
            plan = new UserPlanEntity();
        }
        plan.setPlan(pricePlanEntity);
        plan.setStripeCustomerId(customerId);
        plan.setStripeSubscriptionId(subId);
        plan.setUpdatedAt(Instant.now());
        if (periodEnd != null) plan.setCurrentPeriodEnd(periodEnd);
        plan.setUser(user);
        user.setUserPlan(plan);
        userPlanRepository.save(plan);
        userRepository.save(user);

        saveAnalyticsEvent(user, "Customer subscribed to a new plan: "+pricePlanEntity.getDisplayName());
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
        plan.setUser(user);
        user.setUserPlan(plan);
        userPlanRepository.save(plan);
        userRepository.save(user);

        saveAnalyticsEvent(user, "Customer updated to a new plan: "+tier.getDisplayName());
    }

    private void saveAnalyticsEvent(UserEntity user, String message) {
        AnalyticsEventEntity analyticsEvent = new AnalyticsEventEntity();
        analyticsEvent.setEventName(AnalyticsEventName.PAYMENT);
        analyticsEvent.setUser(user);
        analyticsEvent.setPrincipalKey(user.getUserId().toString());
        analyticsEvent.setPrincipalType(PrincipalType.USER);
        analyticsEvent.setMessage(message);
        analyticsEvent.setCreatedAt(Instant.now());
        analyticsEvent.setOccurredAt(Instant.now());

        analyticsRepository.save(analyticsEvent);
    }

    private UserEntity savePayment(UserPlanEntity userPlan, Event event) {
        UserEntity user = userPlan.getUser();
        if (user == null) {
            throw new IllegalStateException("Cannot have no user assigned to our plan");
        }
        PaymentEvent pe = new PaymentEvent();
        pe.setStripeEventId(event.getId());
        pe.setType(event.getType());
        pe.setReceivedAt(Instant.now());
        pe.setPayloadJson(event.toJson());
        pe.setUser(user);
        paymentEventRepository.save(pe);

        saveAnalyticsEvent(user, "User payment saved under the payments event table!");
        return user;
    }
}
