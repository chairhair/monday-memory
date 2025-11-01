package com.monday.monday_backend.payment;

import com.monday.monday_backend.auth.users.UserEntity;
import com.monday.monday_backend.auth.users.UserRepository;
import com.monday.monday_backend.payment.core.PaymentProvider;
import com.monday.monday_backend.payment.dto.CreateCheckoutResponse;
import com.monday.monday_backend.payment.entity.PaymentEvent;
import com.monday.monday_backend.payment.entity.UserPlanEntity;
import com.monday.monday_backend.payment.repo.PaymentEventRepository;
import com.monday.monday_backend.payment.repo.UserPlanRepository;
import com.monday.monday_backend.payment.utils.PlanTier;
import com.stripe.model.Event;
import com.stripe.model.Invoice;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class BillingService {

    private final UserRepository userRepository;
    private final PaymentProvider provider;
    private final UserPlanRepository userPlanRepository;
    private final PaymentEventRepository paymentEventRepository;

    @Transactional
    public CreateCheckoutResponse startProCheckout(Long userId, String successUrl, String cancelUrl) {
        return provider.createSubscriptionCheckout(userId, successUrl, cancelUrl);
    }

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
        Session session = (Session) event.getDataObjectDeserializer()
                .getObject().orElseThrow();
        Long userId = Long.parseLong(session.getMetadata().get("userId"));
        String subId = session.getSubscription();
        String customerId = session.getCustomer();

        upsertPro(userId, subId, customerId, null);
    }

    private void onInvoicePaid(Event event) {
        Invoice inv = (Invoice) event.getDataObjectDeserializer().getObject().orElseThrow();
        String customerId = inv.getCustomer();
        // FIXME: I need to confirm that inventory is actually getting the subscription.
        String subId = inv.getMetadata().get("SUBSCRIPTION");
        Instant periodEnd = Instant.ofEpochSecond(inv.getLines().getData().get(0).getPeriod().getEnd());
        upsertPro(resolveUserIdFromCustomer(customerId), customerId, subId, periodEnd);
    }

    private void onSubUpdated(Event event) {
        Subscription sub = (Subscription) event.getDataObjectDeserializer().getObject().orElseThrow();
        String customerId = sub.getCustomer();

        // FIXME: We're going to need to create our own entity class here
        Instant periodEnd = Instant.ofEpochSecond(sub.getCurrentPeriodEnd());
        PlanTier tier = sub.getStatus().equals("active") ? PlanTier.PRO : PlanTier.FREE;
        setTier(resolveUserIdFromCustomer(customerId), tier, customerId, sub.getId(), periodEnd);
    }

    private void upsertPro(Long userId, String subId, String customerId, Instant periodEnd) {

        UserPlanEntity userPlan = userPlanRepository.findByUser_Id(userId).orElseGet(() -> {
            UserPlanEntity p = new UserPlanEntity();
            p.setUser(userRepository.getReferenceById(userId)); p.setTier(PlanTier.FREE);
            p.setUpdatedAt(Instant.now());
            return p;
        });

        //userPlan.setTier(PlanTier.PRO);
        userPlan.setStripeCustomerId(customerId);
        userPlan.setStripeSubscriptionId(subId);
        if (periodEnd != null) userPlan.setCurrentPeriodEnd(periodEnd);
        userPlan.setUpdatedAt(Instant.now());
        userPlanRepository.save(userPlan);
    }

    private void setTier(Long userId, PlanTier tier, String cust, String sub, Instant periodEnd) {
        userPlanRepository.findByUser_Id(userId).ifPresent(plan -> {
            plan.setTier(tier);
            plan.setStripeCustomerId(cust);
            plan.setStripeSubscriptionId(sub);
            plan.setCurrentPeriodEnd(periodEnd);
            plan.setUpdatedAt(Instant.now());
            userPlanRepository.save(plan);
        });
    }

    private Long resolveUserIdFromCustomer(String customerId) {
        return userPlanRepository.findByStripeCustomerId(customerId)
                .map(UserPlanEntity::getId)
                .orElse(null);
    }
}
