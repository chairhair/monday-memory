package com.monday.monday_backend.payment.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix="billing.stripe")
@Getter
@Setter
public class StripeConfiguration {
    private String secretKey;
    private String webhookSecret;

    @PostConstruct
    void init() { com.stripe.Stripe.apiKey = secretKey; }
}
