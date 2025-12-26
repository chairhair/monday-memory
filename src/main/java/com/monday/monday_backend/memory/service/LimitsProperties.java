package com.monday.monday_backend.memory.service;

import com.monday.shared.auth.dto.UserPreferencesDTO;
import com.monday.shared.memory.plan.EffectivePlan;
import com.monday.shared.memory.session.utils.SessionScope;
import com.monday.shared.recording.RecordingScope;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "mm.limits")
@Getter
@Setter
public class LimitsProperties {
    private TierLimits guest;
    private TierLimits user;
    private TierLimits pro;

    public LimitsProperties.TierLimits forTier(EffectivePlan plan) {
        return switch (plan) {
            case GUEST_FREE -> guest;
            case USER_FREE  -> user;
            case USER_PRO   -> pro;
        };
    }

    public UserPreferencesDTO toPrefsDTO(SessionScope sessionScope, RecordingScope comScope, EffectivePlan plan) {
        return switch (plan) {
            case GUEST_FREE -> new UserPreferencesDTO(sessionScope, comScope, guest.getMaxTopics(), guest.getMonthlyTokens());
            case USER_FREE  -> new UserPreferencesDTO(sessionScope, comScope, user.getMaxTopics(), user.getMonthlyTokens());
            case USER_PRO   -> new UserPreferencesDTO(sessionScope, comScope, pro.getMaxTopics(), pro.getMonthlyTokens());
        };
    }

    @Getter @Setter
    public static class TierLimits {
        private long maxTopics;
        private int maxSessionsPerTopic;
        private long monthlyTokens;
    }
}
