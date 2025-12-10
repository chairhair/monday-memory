package com.monday.monday_backend.memory.service;

import com.monday.monday_backend.auth.guests.GuestEntity;
import com.monday.monday_backend.auth.principal.PrincipalContext;
import com.monday.monday_backend.auth.users.UserEntity;
import com.monday.monday_backend.payment.entity.UserPlanEntity;
import com.monday.shared.memory.plan.EffectivePlan;
import com.monday.shared.memory.quota.QuotaDecision;
import com.monday.shared.memory.quota.QuotaSnapshot;

public interface QuotaService {
    QuotaSnapshot snapshotFor(UserEntity user,
                              GuestEntity guest,
                              UserPlanEntity userPlan,
                              EffectivePlan effectivePlan);

    QuotaSnapshot snapshotFor(UserPlanEntity userPlan);

    QuotaDecision decide(QuotaSnapshot snapshot);

    String buildWarningMessage(PrincipalContext principal);
}
