package com.monday.monday_backend.auth.tokens;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

@RequiredArgsConstructor
@Service
public class JwtTokenCleanupTask {
    private final TaskScheduler scheduler;
    private final TokensRepository tokensRepository;
    private final Logger logger = LoggerFactory.getLogger(JwtTokenCleanupTask.class);

    private ScheduledFuture<?> cleanupHandle;

    @Value("${jwt.expiration}")
    private Long tokenCleanupInterval;

    @PostConstruct
    public void scheduleTokenCleanupTask() {
        cleanupHandle = scheduler.scheduleAtFixedRate(this::cleanExpiredTokens, Duration.ofMillis(tokenCleanupInterval));
    }

    private void cleanExpiredTokens() {
        Instant cutoff = Instant.now().minus(Duration.ofHours(1));

        List<TokensEntity> expired = tokensRepository.findByExpiredFalseAndTimeCreatedBefore(cutoff);
        for (TokensEntity token : expired) {
            token.setExpired(true);
            logger.info("Marked token as expired: {}", token.getToken());
        }
        tokensRepository.saveAll(expired);
    }

    public void cancelCleanupTask() {
        if (cleanupHandle != null && !cleanupHandle.isCancelled()) {
            cleanupHandle.cancel(true);
            logger.info("Token cleanup task cancelled.");
        }
    }

    public void rescheduleTokenCleanup(Duration newRate) {
        cancelCleanupTask();
        cleanupHandle = scheduler.scheduleAtFixedRate(this::cleanExpiredTokens, newRate);
    }
}
