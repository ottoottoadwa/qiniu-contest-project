package com.prreview.application.config;

import com.prreview.domain.service.ConfidenceScoringService;
import com.prreview.domain.service.RiskMergeService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for domain services that need to be registered as Spring beans.
 */
@Configuration
public class DomainServiceConfig {

    @Bean
    public ConfidenceScoringService confidenceScoringService() {
        return new ConfidenceScoringService();
    }

    @Bean
    public RiskMergeService riskMergeService(ConfidenceScoringService confidenceScoringService) {
        return new RiskMergeService(confidenceScoringService);
    }
}
