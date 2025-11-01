package com.monday.monday_backend.payment.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix="billing.stripe")
@Getter
public class StripeConfiguration {
    private String secretKey;
    private String webhookSecret;
    private String priceIdMonthly;
    private String priceIdAnnually;

    @PostConstruct
    void init() { com.stripe.Stripe.apiKey = secretKey; }
}
