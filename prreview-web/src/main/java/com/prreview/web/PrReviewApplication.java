package com.prreview.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Spring Boot entry point for the AI PR Review Assistant.
 *
 * <p>Key configuration:
 * <ul>
 *   <li>@EnableAsync — enables @Async for ReviewTaskRunner (virtual threads via config)</li>
 *   <li>@ConfigurationPropertiesScan — auto-discovers @ConfigurationProperties beans</li>
 *   <li>JPA Auditing — enabled in JpaConfig (separate to allow @WebMvcTest exclusion)</li>
 * </ul>
 */
@SpringBootApplication(scanBasePackages = "com.prreview")
@EnableAsync
@ConfigurationPropertiesScan(basePackages = "com.prreview")
public class PrReviewApplication {

    public static void main(String[] args) {
        SpringApplication.run(PrReviewApplication.class, args);
    }
}
