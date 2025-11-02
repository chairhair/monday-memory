package com.monday.monday_backend.payment;

import com.monday.monday_backend.payment.dto.StartCheckoutRequestDTO;
import com.monday.monday_backend.payment.dto.StartCheckoutResponseDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/checkout")
    public StartCheckoutResponseDTO startCheckout(
            @RequestParam(required = false) Long userId,
            @RequestBody @Valid StartCheckoutRequestDTO req
            ) {
        return paymentService.createSubscriptionCheckout(userId, req.planCode(), req.successUrl(), req.cancelUrl());
    }
}
