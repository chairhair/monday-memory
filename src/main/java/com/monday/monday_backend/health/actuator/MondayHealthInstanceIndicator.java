package com.monday.monday_backend.health.actuator;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Most processes under Monday is automatic, but given that users
 * may have difficulty handling tokens, we want to make educated decisions about
 * when they need to start a new session.
 */
@Component
public class MondayHealthInstanceIndicator implements HealthIndicator {

    @Override
    public Health health() {
        return Health.up().build();
    }
}
