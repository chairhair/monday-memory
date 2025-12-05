package com.monday.monday_backend.query;

import com.monday.monday_backend.auth.principal.PrincipalContext;
import com.monday.monday_backend.memory.service.QuotaService;
import com.monday.monday_backend.payment.repo.UserPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class QueryProcessingService {

    private final UserPlanRepository userPlanRepository;
    private final QuotaService quotaService;

    private void bumpUsage(PrincipalContext principal, int topicsDelta, long tokensDelta) {
        var userPlan = principal.getUserPlan();
        if (userPlan == null) {
            return; // guests with no user plan -> either ignore or handle separately
        }

        userPlan.setTopicsUsed(
                (userPlan.getTopicsUsed() != null ? userPlan.getTopicsUsed() : 0) + topicsDelta
        );

        userPlan.setTokensUsed(
                (userPlan.getTokensUsed() != null ? userPlan.getTokensUsed() : 0L) + tokensDelta
        );

        userPlanRepository.save(userPlan);
    }


}
