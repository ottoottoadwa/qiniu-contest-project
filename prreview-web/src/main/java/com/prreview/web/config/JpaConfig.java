package com.prreview.web.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA configuration for auditing support.
 * Separated from main application class to allow @WebMvcTest to exclude it.
 */
@Configuration
@EnableJpaAuditing
public class JpaConfig {
}
