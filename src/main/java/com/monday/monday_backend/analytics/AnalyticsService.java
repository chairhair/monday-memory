package com.monday.monday_backend.analytics;

import com.monday.monday_backend.auth.users.UserEntity;
import com.monday.monday_backend.auth.users.UserRepository;
import com.monday.shared.analytics.AnalyticsEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnalyticsService {
    private final UserRepository userRepository;
    private final AnalyticsRepository analyticsRepository;

    public void emit(AnalyticsEvent event) {
        log.info("Analytics {}", event);

        try {
            Optional<UserEntity> user = userRepository.findByUserId(UUID.fromString(event.userId()));

            AnalyticsEventEntity analyticsEntity = new AnalyticsEventEntity();
            analyticsEntity.setEventName(event.eventName());
            analyticsEntity.setGuestKey(event.userId());
            analyticsEntity.setCreatedAt(Instant.now());
            user.ifPresent(analyticsEntity::setUser);
            analyticsRepository.save(analyticsEntity);
        } catch (Exception e) {
            log.error("Could not provide analytics: "+e.getMessage());
        }
    }

}
