package com.monday.monday_backend.payment.entity;

import jakarta.persistence.*;
import lombok.Getter;

@Getter
@Entity
@Table(name = "price_plan")
public class PricePlanEntity {

    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    Long id;
    @Column(nullable = false, unique = true)
    String code; // e.g. "PRO_MONTHLY","PRO_ANNUALLY"
    @Column(nullable = false, unique = true)
    String stripePriceId;
    String displayName;
    Integer monthlyAmount;
    Integer annualAmount;
}
