package com.monday.monday_backend.payment.repo;

import com.monday.monday_backend.payment.entity.PaymentEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentEventRepository extends JpaRepository<PaymentEvent, Long> {

    // Helpful for our Idempotency Keys.
    boolean existsByStripeEventId(String stripeEventId);

    // Handy for debugging/replay tools
    Optional<PaymentEvent> findTopByUserIdOrderByReceivedAtDesc(String userId);
}
