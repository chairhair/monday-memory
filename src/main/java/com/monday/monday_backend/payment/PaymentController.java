package com.monday.monday_backend.payment;

import com.monday.monday_backend.auth.principal.AuthUser;
import com.monday.monday_backend.payment.dto.StartCheckoutRequestDTO;
import com.monday.monday_backend.payment.dto.StartCheckoutResponseDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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
        Long userId = authUser != null && authUser.id() != null ? Long.parseLong(authUser.id()) : null;
        return paymentService.createSubscriptionCheckout(userId, req.planCode(), req.successUrl(), req.cancelUrl());
    }


}
