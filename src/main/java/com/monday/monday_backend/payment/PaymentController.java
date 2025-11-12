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
        Long userId = (authUser != null) ? AuthHelper.safeToLong(authUser.id()) : null;
        try {
            return paymentService.createSubscriptionCheckout(userId, req.planCode(), req.successUrl(), req.cancelUrl());
        } catch (StripeException se) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Stripe could not process the payment: "+se);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Could not continue: "+ex);
        }
    }


}
