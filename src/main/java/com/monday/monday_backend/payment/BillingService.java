package com.monday.monday_backend.payment;

import com.monday.monday_backend.auth.users.UserRepository;
import com.monday.monday_backend.payment.core.PaymentProvider;
import com.monday.monday_backend.payment.dto.CreateCheckoutResponse;
import com.monday.monday_backend.payment.entity.PaymentEvent;
import com.monday.monday_backend.payment.entity.UserPlan;
import com.monday.monday_backend.payment.repo.PaymentEventRepository;
import com.monday.monday_backend.payment.repo.UserPlanRepository;
import com.monday.monday_backend.payment.utils.PlanTier;
import com.stripe.model.Event;
import com.stripe.model.Invoice;
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
    public CreateCheckoutResponse startProCheckout(String userId, String successUrl, String cancelUrl) {
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
        String userId = session.getMetadata().get("userId");
        String subId = session.getSubscription();
        String customerId = session.getCustomer();

        upsertPro(userId, subId, customerId, null);
    }

    private void onInvoicePaid(Event event) {
        Invoice inv = (Invoice) event.getDataObjectDeserializer().getObject().orElseThrow();
        String customerId = inv.getCustomer();
        String subId = inv.getMetadata().get("SUBSCRIPTION");
        Instant periodEnd = Instant.ofEpochSecond(inv.getLines().getData().get(0).getPeriod().getEnd());
        upsertPro(resolveUserIdFromCustomer(customerId), customerId, subId, periodEnd);
    }

    private void upsertPro(String userId, String subId, String customerId, Instant periodEnd) {
        UserPlan userPlan = userPlanRepository.findByUserId(userId).orElseGet(() -> {
            UserPlan p = new UserPlan();
            p.setUserId(userId); p.setTier(PlanTier.FREE);
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

    private String resolveUserIdFromCustomer(String customerId) {
        return userPlanRepository.findByStripeCustomerId(customerId)
                .map(UserPlan::getUserId)
                .orElse(null);
    }
}
