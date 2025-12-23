package com.monday.monday_backend.memory.service;

import com.monday.monday_backend.auth.guests.GuestEntity;
import com.monday.monday_backend.auth.principal.PrincipalContext;
import com.monday.monday_backend.auth.users.UserEntity;
import com.monday.monday_backend.payment.entity.UserPlanEntity;
import com.monday.monday_backend.payment.repo.UserPlanRepository;
import com.monday.monday_backend.payment.utils.MonthKey;
import com.monday.shared.memory.plan.EffectivePlan;
import com.monday.shared.memory.quota.QuotaDecision;
import com.monday.shared.memory.quota.QuotaSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;

@Primary
@Service
@RequiredArgsConstructor
public class SimpleQuotaService implements QuotaService {

    private final Clock clock;
    private final UserPlanRepository userPlanRepository;

    @Override
    public QuotaSnapshot snapshotFor(UserEntity user,
                                     GuestEntity guest,
                                     UserPlanEntity userPlan,
                                     EffectivePlan effectivePlan) {
        var pricePlan = userPlan != null ? userPlan.getPlan() : null;

        int topicLimit = pricePlan != null && pricePlan.getMaxTopicsPerPeriod() != null
                ? pricePlan.getMaxTopicsPerPeriod()
                : Integer.MAX_VALUE; // or some default

        long tokenLimit = pricePlan != null && pricePlan.getMaxTokensPerPeriod() != null
                ? pricePlan.getMaxTokensPerPeriod()
                : Long.MAX_VALUE;

        int topicsUsed = userPlan != null && userPlan.getTopicsUsed() != null
                ? userPlan.getTopicsUsed()
                : 0;

        long tokensUsed = userPlan != null && userPlan.getTokensUsed() != null
                ? userPlan.getTokensUsed()
                : 0L;

        int currentMonth = MonthKey.currentUtcYYYYMM(clock);

        long tokensUsedThisMonth =
                (userPlan != null
                        && userPlan.getTokensUsedMonth() != null
                        && userPlan.getTokensUsedMonth() == currentMonth)
                        ? (userPlan.getTokensUsed() != null ? userPlan.getTokensUsed() : 0L)
                        : 0L;

        Double warningRatio = (pricePlan != null && pricePlan.getWarningThresholdRatio() != null)
                ? pricePlan.getWarningThresholdRatio()
                : 0.8; // safe global default if not set

        return QuotaSnapshot.builder()
                .topicsUsed(topicsUsed)
                .topicLimit(topicLimit)
                .tokensUsed(tokensUsed)
                .tokensUsedMonth(tokensUsedThisMonth)
                .tokenLimit(tokenLimit)
                .warningThresholdRatio(warningRatio)
                .build();
    }

    @Override
    public QuotaSnapshot snapshotFor(UserPlanEntity userPlan) {
        return snapshotFor(null, null, userPlan, null);
    }

    @Override
    public QuotaDecision decide(QuotaSnapshot snapshot) {
        if (snapshot.getTopicsUsed() >= snapshot.getTopicLimit()
                || snapshot.getTokensUsed() >= snapshot.getTokenLimit()) {
            return QuotaDecision.BLOCK;
        }

        boolean nearTopics = snapshot.getTopicsUsed()
                >= snapshot.getTopicLimit() * snapshot.getWarningThresholdRatio();

        boolean nearTokens = snapshot.getTokensUsed()
                >= snapshot.getTokenLimit() * snapshot.getWarningThresholdRatio();

        if (nearTopics || nearTokens) {
            return QuotaDecision.ALLOW_WITH_WARNING;
        }

        return QuotaDecision.ALLOW;
    }

    @Override
    public void reset(UserPlanEntity userPlan) {
        if (userPlan == null) {
            throw new IllegalStateException("Cannot continue; userPlan does not exist");
        }

        userPlanRepository.ensureCurrentMonthBucket(userPlan.getId(), MonthKey.currentUtcYYYYMM(clock));
    }

    @Override
    public String buildWarningMessage(PrincipalContext principal) {
        var q = principal.getQuota();

        boolean nearTopics = q.getTopicsUsed()
                >= q.getTopicLimit() * q.getWarningThresholdRatio();
        boolean nearTokens = q.getTokensUsed()
                >= q.getTokenLimit() * q.getWarningThresholdRatio();

        StringBuilder sb = new StringBuilder("Heads up: ");

        if (nearTopics) {
            sb.append("you've used ")
                    .append(q.getTopicsUsed())
                    .append("/")
                    .append(q.getTopicLimit())
                    .append(" topics this period");
        }

        if (nearTokens) {
            if (nearTopics) sb.append(" and ");
            sb.append("you've used ")
                    .append(q.getTokensUsed())
                    .append("/")
                    .append(q.getTokenLimit())
                    .append(" tokens this period");
        }

        sb.append(" on your ").append(principal.getPlan()).append(" plan.");

        if (!principal.isPro()) {
            sb.append(" Consider upgrading if you don't want me to stop recording memory.");
        }

        return sb.toString();
    }
}

