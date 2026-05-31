package com.prreview.web.config;

import org.springframework.context.annotation.Configuration;

/**
 * JPA configuration.
 * JPA Auditing is disabled because we manually set timestamps in the mapper
 * to handle LocalDateTime (database) to OffsetDateTime (domain) conversion.
 */
@Configuration
public class JpaConfig {
}
