package com.monday.monday_backend.payment.entity;

import com.monday.monday_backend.auth.users.UserEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Table(
        name = "user_plan",
        indexes = {
                @Index(name = "ix_user_plan_customer_id", columnList = "stripe_customer_id", unique = true)
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_plan_user_id", columnNames = {"user_id"})
        }
)
@Entity
public class UserPlanEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    UUID id;

    @Setter
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true,
                foreignKey = @ForeignKey(name = "fk_user_plan_user"))
    private UserEntity user;

    @Setter
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="price_plan_id")
    private PricePlanEntity plan;

    @Setter
    @Column(name = "stripe_customer_id", length = 255, unique = true)
    private String stripeCustomerId;

    @Setter
    @Column(name = "stripe_subscription_id", length = 255)
    private String stripeSubscriptionId;

    @Setter
    @Column(name = "current_period_end")
    private Instant currentPeriodEnd;

    @Setter
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Setter
    @Column(name = "period_start")
    private Instant periodStart;

    @Setter
    @Column(name = "period_end")
    private Instant periodEnd;

    @Setter
    @Column(name = "topics_used")
    private Integer topicsUsed;

    @Setter
    @Column(name = "tokens_used")
    private Long tokensUsed;

    @Version
    private Long version;

    @PrePersist
    @PreUpdate
    private void touch() {
        this.updatedAt = Instant.now();
    }
}
