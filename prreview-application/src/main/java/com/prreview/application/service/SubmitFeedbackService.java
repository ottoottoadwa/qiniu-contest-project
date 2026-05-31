package com.prreview.application.service;

import com.prreview.application.port.in.SubmitFeedbackUseCase;
import com.prreview.domain.model.feedback.ReviewFeedback;
import com.prreview.domain.model.review.Review;
import com.prreview.domain.model.risk.RiskItem;
import com.prreview.domain.port.out.ReviewRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Implements SubmitFeedbackUseCase.
 * Records user feedback on risk items to drive the calibration loop.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubmitFeedbackService implements SubmitFeedbackUseCase {

    private final ReviewRepositoryPort reviewRepository;

    @Override
    @Transactional
    public UUID submitFeedback(SubmitFeedbackCommand command) {
        Review review = reviewRepository.findById(command.reviewId())
                .orElseThrow(() -> new GetReviewService.ReviewNotFoundException(command.reviewId()));

        // Find the risk item to get its pattern key
        RiskItem riskItem = review.getRiskItems().stream()
                .filter(ri -> ri.id().equals(command.riskItemId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Risk item not found: " + command.riskItemId()));

        ReviewFeedback feedback = ReviewFeedback.create(
                command.riskItemId(),
                command.reviewId().toString(),
                riskItem.patternKey(),
                command.verdict(),
                command.comment(),
                command.submittedBy());

        log.info("Feedback recorded: riskItemId={}, verdict={}", command.riskItemId(), command.verdict());

        // In a full implementation, feedback would be persisted via a FeedbackRepositoryPort.
        // For MVP, we log and return the feedback ID.
        return UUID.fromString(feedback.id());
    }
}
