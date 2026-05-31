package com.prreview.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prreview.application.port.in.GetReviewQuery;
import com.prreview.application.port.in.SubmitReviewUseCase;
import com.prreview.application.service.GetReviewService;
import com.prreview.domain.model.review.ReviewStatus;
import com.prreview.web.advice.GlobalExceptionHandler;
import com.prreview.web.config.ApiKeyAuthFilter;
import com.prreview.web.config.PrReviewProperties;
import com.prreview.web.config.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Web layer tests for ReviewController using @WebMvcTest.
 * Tests HTTP contract: status codes, headers, response structure.
 * Business logic is mocked — tested separately in domain/application tests.
 */
@WebMvcTest(controllers = ReviewController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class, ApiKeyAuthFilter.class})
@EnableConfigurationProperties(PrReviewProperties.class)
@TestPropertySource(properties = {
        "prreview.api-key=dev-api-key-change-in-production",
        "prreview.max-concurrent-reviews=10",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.jpa.show-sql=false"
})
@EnableAutoConfiguration(exclude = {
        org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class,
        org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class,
        org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration.class,
        org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration.class
})
class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SubmitReviewUseCase submitReview;

    @MockitoBean
    private GetReviewQuery getReview;

    private static final String VALID_API_KEY = "dev-api-key-change-in-production";

    @Test
    @DisplayName("POST /api/reviews/v1 should return 202 with Location header")
    void submitReview_shouldReturn202WithLocation() throws Exception {
        // Arrange
        UUID reviewId = UUID.randomUUID();
        when(submitReview.submit(any(), anyString()))
                .thenReturn(new SubmitReviewUseCase.ReviewAccepted(
                        reviewId, "/api/reviews/v1/" + reviewId + "/status"));

        String requestBody = """
                {
                  "repository": "octocat/hello-world",
                  "pullRequestNumber": 42,
                  "analysisProfile": "STANDARD"
                }
                """;

        // Act & Assert
        mockMvc.perform(post("/api/reviews/v1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-API-Key", VALID_API_KEY)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .content(requestBody))
                .andExpect(status().isAccepted())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.reviewId").value(reviewId.toString()))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("POST /api/reviews/v1 should return 400 when repository format is invalid")
    void submitReview_shouldReturn400_whenRepositoryFormatInvalid() throws Exception {
        // Arrange
        String requestBody = """
                {
                  "repository": "invalid-no-slash",
                  "pullRequestNumber": 42
                }
                """;

        // Act & Assert
        mockMvc.perform(post("/api/reviews/v1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-API-Key", VALID_API_KEY)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value(
                        "https://prreview.dev/problems/validation-error"))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    @DisplayName("POST /api/reviews/v1 should return 400 when pullRequestNumber is zero")
    void submitReview_shouldReturn400_whenPrNumberIsZero() throws Exception {
        // Arrange
        String requestBody = """
                {
                  "repository": "octocat/hello-world",
                  "pullRequestNumber": 0
                }
                """;

        // Act & Assert
        mockMvc.perform(post("/api/reviews/v1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-API-Key", VALID_API_KEY)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/reviews/v1/{id}/status should return 200 with status")
    void getStatus_shouldReturn200() throws Exception {
        // Arrange
        UUID reviewId = UUID.randomUUID();
        when(getReview.status(reviewId))
                .thenReturn(new GetReviewQuery.ReviewStatusView(
                        reviewId, ReviewStatus.RUNNING, 0.5, 10, 5,
                        OffsetDateTime.now(), 15, null));

        // Act & Assert
        mockMvc.perform(get("/api/reviews/v1/{id}/status", reviewId)
                        .header("X-API-Key", VALID_API_KEY))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.reviewId").value(reviewId.toString()))
                .andExpect(jsonPath("$.status").value("RUNNING"))
                .andExpect(jsonPath("$.progress").value(0.5));
    }

    @Test
    @DisplayName("GET /api/reviews/v1/{id}/status should return 404 when review not found")
    void getStatus_shouldReturn404_whenReviewNotFound() throws Exception {
        // Arrange
        UUID reviewId = UUID.randomUUID();
        when(getReview.status(reviewId))
                .thenThrow(new GetReviewService.ReviewNotFoundException(reviewId));

        // Act & Assert
        mockMvc.perform(get("/api/reviews/v1/{id}/status", reviewId)
                        .header("X-API-Key", VALID_API_KEY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value(
                        "https://prreview.dev/problems/resource-not-found"));
    }

    @Test
    @DisplayName("GET /api/reviews/v1/{id} should return 409 when review not ready")
    void getResult_shouldReturn409_whenReviewNotReady() throws Exception {
        // Arrange
        UUID reviewId = UUID.randomUUID();
        when(getReview.result(reviewId))
                .thenThrow(new GetReviewService.ReviewNotReadyException(reviewId));

        // Act & Assert
        mockMvc.perform(get("/api/reviews/v1/{id}", reviewId)
                        .header("X-API-Key", VALID_API_KEY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value(
                        "https://prreview.dev/problems/review-not-ready"));
    }

    @Test
    @DisplayName("POST /api/reviews/v1 should return 401 when API key is missing")
    void submitReview_shouldReturn401_whenApiKeyMissing() throws Exception {
        // Arrange
        String requestBody = """
                {
                  "repository": "octocat/hello-world",
                  "pullRequestNumber": 42
                }
                """;

        // Act & Assert — no X-API-Key header
        mockMvc.perform(post("/api/reviews/v1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized());
    }
}
