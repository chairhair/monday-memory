package com.monday.monday_backend.payment;

import com.monday.monday_backend.auth.principal.AuthUser;
import com.monday.monday_backend.auth.principal.PrincipalResolver;
import com.monday.monday_backend.payment.config.StripeConfiguration;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Stripe tells us what happened following our Payment Controller.
 */
@RestController
@RequestMapping("/api/billing")
@RequiredArgsConstructor
public class BillingController {

    private final StripeConfiguration cfg;
    private final BillingService billingService;

    @PostMapping
    public ResponseEntity<String> handleWebhook(@AuthenticationPrincipal AuthUser authUser,
                                                @RequestHeader("Stripe-Signature") String signature,
                                                @RequestBody String payload) {
        try {
            Event event = Webhook.constructEvent(payload, signature, cfg.getWebhookSecret());
            billingService.handleStripeEvent(authUser, event);
            return ResponseEntity.ok("ok");
        } catch (SignatureVerificationException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("invalid signature");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("error");
        }
    }

}
