package com.monday.monday_backend.payment.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Entity
@Table(name = "price_plan")
public class PricePlanEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false, unique = true)
    String code; // e.g. "FREE_DISCORD", "PRO_MONTHLY"

    @Setter
    @Column(name = "stripe_price_id")
    String stripePriceId;

    @Column(name = "display_name")
    String displayName;

    @Column(name = "monthly_amount")
    Integer monthlyAmount;   // in cents

    @Column(name = "annual_amount")
    Integer annualAmount;    // in cents

    // 🔽 NEW: usage limits for this plan
    @Column(name = "max_topics_per_period")
    Integer maxTopicsPerPeriod;      // e.g. 20 for free, 200 for pro

    @Column(name = "max_tokens_per_period")
    Long maxTokensPerPeriod;         // e.g. 50_000L, 1_000_000L

    @Column(name = "warning_threshold_ratio")
    Double warningThresholdRatio;    // e.g. 0.8 for 80% warning
}
