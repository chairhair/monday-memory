package com.monday.monday_backend.auth.utils;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ClockConfig {
    @Bean
    public Clock systemClock() {
        return Clock.systemUTC();
    }
}
