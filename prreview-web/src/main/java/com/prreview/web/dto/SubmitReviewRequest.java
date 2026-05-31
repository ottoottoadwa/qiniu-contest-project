package com.prreview.web.dto;

import com.prreview.domain.model.review.AnalysisProfile;
import com.prreview.domain.model.risk.RiskCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.util.List;

/**
 * Request DTO for submitting a PR review.
 * Uses Java record for immutability. Bean Validation annotations enforce input constraints.
 */
public record SubmitReviewRequest(
        @NotBlank(message = "repository must not be blank")
        @Pattern(regexp = "^[\\w.-]+/[\\w.-]+$", message = "repository must be in 'owner/repo' format")
        String repository,

        @Positive(message = "pullRequestNumber must be a positive integer")
        int pullRequestNumber,

        AnalysisProfile analysisProfile,

        List<RiskCategory> riskCategories,

        @org.hibernate.validator.constraints.URL(message = "callbackUrl must be a valid URL")
        String callbackUrl) {

    /** Converts to the application-layer command. */
    public com.prreview.application.port.in.SubmitReviewUseCase.SubmitReviewCommand toCommand() {
        return new com.prreview.application.port.in.SubmitReviewUseCase.SubmitReviewCommand(
                repository, pullRequestNumber, analysisProfile, riskCategories, callbackUrl);
    }
}
