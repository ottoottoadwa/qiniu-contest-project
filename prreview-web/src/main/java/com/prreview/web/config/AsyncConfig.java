package com.prreview.web.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Async configuration for review task execution.
 * With spring.threads.virtual.enabled=true, Spring Boot 3.2+ automatically
 * uses virtual threads for @Async tasks. This config provides a named executor
 * for explicit control and observability.
 */
@Configuration
public class AsyncConfig {

    /**
     * Named executor for review tasks.
     * Virtual threads are enabled globally via spring.threads.virtual.enabled=true.
     * This executor is referenced by @Async("reviewTaskExecutor") in ReviewTaskRunner.
     */
    @Bean(name = "reviewTaskExecutor")
    public Executor reviewTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("review-task-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }
}
