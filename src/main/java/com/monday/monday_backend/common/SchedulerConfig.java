package com.monday.monday_backend.common;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
@RequiredArgsConstructor
public class SchedulerConfig {

    @Value("${spring.task.scheduling.pool.size}")
    private final int defaultScheduledPoolSize;

    @Value("${spring.task.scheduling.thread-name-prefix}")
    private final String defaultThreadNamePrefix;

    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(defaultScheduledPoolSize);
        scheduler.setThreadNamePrefix(defaultThreadNamePrefix);
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        return scheduler;
    }
}
