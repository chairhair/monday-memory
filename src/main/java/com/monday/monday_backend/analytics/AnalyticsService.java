package com.monday.monday_backend.analytics;

import com.monday.monday_backend.auth.users.UserEntity;
import com.monday.monday_backend.auth.users.UserRepository;
import com.monday.shared.analytics.AnalyticsEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsService {
    private final UserRepository userRepository;
    private final AnalyticsRepository analyticsRepository;

    public void emit(AnalyticsEvent event) {
        log.info("Analytics {}", event);

        UUID userUUIDPresent = null;
        try {
            userUUIDPresent = UUID.fromString(event.principalKey());
        } catch (Exception ignored) {}
        try {
            UserEntity user = (userUUIDPresent != null) ? userRepository.findByUserId(userUUIDPresent).orElse(null) : null;

            AnalyticsEventEntity analyticsEntity = new AnalyticsEventEntity();
            analyticsEntity.setEventName(event.eventName());
            analyticsEntity.setPrincipalKey(event.principalKey());
            analyticsEntity.setPrincipalType(event.principalType().toString());
            analyticsEntity.setSessionId(event.sessionId());
            analyticsEntity.setHttpResult(event.result().value());
            analyticsEntity.setErrorCode(event.errorCode());
            analyticsEntity.setLatencyMs(event.latencyMs());
            analyticsEntity.setOccurredAt(event.occurredAt());
            analyticsEntity.setCreatedAt(Instant.now());
            if (user != null) {
                analyticsEntity.setUser(user);
            }
            analyticsRepository.save(analyticsEntity);
        } catch (Exception e) {
            log.error("Could not provide analytics: {}", event, e);
        }
    }

}
