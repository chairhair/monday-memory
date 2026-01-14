package com.monday.monday_backend.payment;

import com.monday.monday_backend.payment.entity.PricePlanEntity;
import com.monday.monday_backend.payment.entity.UserPlanEntity;
import com.monday.monday_backend.payment.repo.PricePlanRepository;
import com.monday.monday_backend.payment.repo.UserPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.stereotype.Service;

/**
 * Centralizes Plan classification
 */
@Service
@RequiredArgsConstructor
public class PlanDefaultsService {

    private final PricePlanRepository pricePlanRepository;
    private final UserPlanRepository userPlanRepository;

    public UserPlanEntity getDefaultGuestPlan() {
        PricePlanEntity guestPricePlan = pricePlanRepository
                .findByCode("FREE")
                .orElseThrow(() -> new DataRetrievalFailureException("Could not get basic plan information"));
        UserPlanEntity userPlan = new UserPlanEntity();
        userPlan.setPlan(guestPricePlan);

        return userPlan;
    }

    public boolean isProPlan(UserPlanEntity userPlan) {
        if (userPlan == null || userPlan.getPlan() == null) return false;
        PricePlanEntity plan = userPlan.getPlan();
        return plan.getCode().contains("PRO");
    }

}
