package com.prreview.application.port.in;

import com.prreview.domain.model.feedback.FeedbackVerdict;

import java.util.UUID;

/**
 * Inbound port for submitting feedback on a risk item.
 * Drives the confidence calibration loop.
 */
public interface SubmitFeedbackUseCase {

    /**
     * Records user feedback on a specific risk item.
     *
     * @param command the feedback command
     * @return the ID of the created feedback record
     */
    UUID submitFeedback(SubmitFeedbackCommand command);

    /** Command object for submitting feedback. */
    record SubmitFeedbackCommand(
            UUID reviewId,
            String riskItemId,
            FeedbackVerdict verdict,
            String comment,
            String submittedBy) {

        public SubmitFeedbackCommand {
            if (reviewId == null) {
                throw new IllegalArgumentException("reviewId must not be null");
            }
            if (riskItemId == null || riskItemId.isBlank()) {
                throw new IllegalArgumentException("riskItemId must not be blank");
            }
            if (verdict == null) {
                throw new IllegalArgumentException("verdict must not be null");
            }
        }
    }
}
