package com.monday.monday_backend.payment.repo;

import com.monday.monday_backend.payment.entity.PricePlanEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PricePlanRepository extends JpaRepository<PricePlanEntity, Long> {
    Optional<PricePlanEntity> findByCode(String code);

    Optional<PricePlanEntity> findByStripePriceId(String stripePriceId);
}
