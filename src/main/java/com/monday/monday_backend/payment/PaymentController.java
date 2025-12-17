package com.monday.monday_backend.payment;

import com.monday.monday_backend.auth.principal.AuthUser;
import com.monday.monday_backend.auth.utils.AuthHelper;
import com.monday.shared.payment.dto.StartCheckoutRequestDTO;
import com.monday.shared.payment.dto.StartCheckoutResponseDTO;
import com.stripe.exception.StripeException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/**
 * The Payment controller here focuses on where our money starts to flow
 * and is USER INITIATED.
 * - This does not start the subscription
 * - It just gives us the checkout URL.
 */
@RestController
@RequestMapping("/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/checkout")
    public StartCheckoutResponseDTO startCheckout(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestBody @Valid StartCheckoutRequestDTO req
            ) {
        try {
            return paymentService.createSubscriptionCheckout(UUID.fromString(authUser.id()), req.planCode(), req.successUrl(), req.cancelUrl());
        } catch (StripeException se) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Stripe could not process the payment: "+se);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Could not continue: "+ex);
        }
    }


}
