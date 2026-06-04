package com.prreview.application.review;

import java.util.UUID;

/**
 * Domain event published when a review is completed.
 * Used to decouple review execution from result posting.
 */
public class ReviewCompletedEvent {
    private final Object source;
    private final UUID reviewId;
    private final String repository;
    private final int prNumber;

    public ReviewCompletedEvent(Object source, UUID reviewId, String repository, int prNumber) {
        this.source = source;
        this.reviewId = reviewId;
        this.repository = repository;
        this.prNumber = prNumber;
    }

    public Object getSource() { return source; }
    public UUID getReviewId() { return reviewId; }
    public String getRepository() { return repository; }
    public int getPrNumber() { return prNumber; }
}
