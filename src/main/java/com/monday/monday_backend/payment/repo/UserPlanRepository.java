package com.monday.monday_backend.payment.repo;

import com.monday.monday_backend.payment.entity.PricePlanEntity;
import com.monday.monday_backend.payment.entity.UserPlanEntity;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface UserPlanRepository extends JpaRepository<UserPlanEntity, Long> {
    Optional<UserPlanEntity> findByUser_Id(Long userId);

    Optional<UserPlanEntity> findByStripeCustomerId(String stripeCustomerId);

    boolean existsByUserId(String userId);

    // Optional: fast path to flip tier without fetching the row
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("""
      update UserPlanEntity p
         set p.plan = :plan,
             p.stripeCustomerId = :customerId,
             p.stripeSubscriptionId = :subscriptionId,
             p.currentPeriodEnd = :currentPeriodEnd,
             p.updatedAt = :updatedAt
       where p.user.id = :userId
    """)
    int updateEntitlement(@Param("userId") Long userId,
                          @Param("plan") PricePlanEntity plan,  // pass an entity ref
                          @Param("customerId") String customerId,
                          @Param("subscriptionId") String subscriptionId,
                          @Param("currentPeriodEnd") Instant currentPeriodEnd,
                          @Param("updatedAt") Instant updatedAt);
}
