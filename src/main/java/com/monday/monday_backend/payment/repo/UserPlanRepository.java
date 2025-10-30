package com.monday.monday_backend.payment.repo;

import com.monday.monday_backend.payment.entity.UserPlan;
import com.monday.monday_backend.payment.utils.PlanTier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface UserPlanRepository extends JpaRepository<UserPlan, Long> {
    Optional<UserPlan> findByUserId(String userId);

    Optional<UserPlan> findByStripeCustomerId(String stripeCustomerId);

    boolean existsByUserId(String userId);

    // Optional: fast path to flip tier without fetching the row
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
           update UserPlan p
              set p.tier = :tier,
                  p.stripeCustomerId = :customerId,
                  p.stripeSubscriptionId = :subscriptionId,
                  p.currentPeriodEnd = :currentPeriodEnd,
                  p.updatedAt = :updatedAt
            where p.userId = :userId
           """)
    int updateEntitlement(@Param("userId") String userId,
                          @Param("tier") PlanTier tier,
                          @Param("customerId") String customerId,
                          @Param("subscriptionId") String subscriptionId,
                          @Param("currentPeriodEnd") Instant currentPeriodEnd,
                          @Param("updatedAt") Instant updatedAt);
}
