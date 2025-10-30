package com.monday.monday_backend.payment.entity;

import com.monday.monday_backend.payment.utils.PlanTier;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "user_plan")
@Getter
public class UserPlan {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id;

    @Setter
    @Column(nullable=false)
    String userIdFk;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    PlanTier tier;

    @Setter
    @Column
    String stripeCustomerId;

    @Setter
    @Column
    String stripeSubscriptionId;

    @Setter
    @Column
    Instant currentPeriodEnd;

    @Setter
    @Column(nullable = false)
    Instant updatedAt;
}
