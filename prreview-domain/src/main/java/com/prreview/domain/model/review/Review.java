package com.prreview.domain.model.review;

import com.prreview.domain.model.risk.RiskItem;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Aggregate root representing a complete PR review task and its results.
 * Mutable only through domain methods; state transitions are explicit.
 */
public class Review {

    private final UUID id;
    private final String repository;
    private final int prNumber;
    private final AnalysisProfile analysisProfile;
    private final String idempotencyKey;

    private ReviewStatus status;
    private double progress;
    private int filesTotal;
    private int filesAnalyzed;
    private String failureReason;

    private ChangeSummary summary;
    private List<RiskItem> riskItems;

    private final OffsetDateTime createdAt;
    private OffsetDateTime startedAt;
    private OffsetDateTime completedAt;

    /** Factory method for creating a new review task. */
    public static Review create(String repository, int prNumber,
                                AnalysisProfile profile, String idempotencyKey) {
        return new Review(UUID.randomUUID(), repository, prNumber, profile, idempotencyKey);
    }

    /** Reconstitution constructor for loading from persistence (bypasses validation). */
    public static Review reconstitute(UUID id, String repository, int prNumber,
                                      AnalysisProfile profile, String idempotencyKey,
                                      ReviewStatus status, double progress,
                                      int filesTotal, int filesAnalyzed,
                                      String failureReason, ChangeSummary summary,
                                      List<RiskItem> riskItems,
                                      OffsetDateTime createdAt, OffsetDateTime startedAt,
                                      OffsetDateTime completedAt) {
        Review r = new Review(id, repository, prNumber, profile, idempotencyKey);
        r.status = status;
        r.progress = progress;
        r.filesTotal = filesTotal;
        r.filesAnalyzed = filesAnalyzed;
        r.failureReason = failureReason;
        r.summary = summary;
        r.riskItems = riskItems == null ? List.of() : List.copyOf(riskItems);
        r.startedAt = startedAt;
        r.completedAt = completedAt;
        return r;
    }

    private Review(UUID id, String repository, int prNumber,
                   AnalysisProfile profile, String idempotencyKey) {
        this.id = id;
        this.repository = repository;
        this.prNumber = prNumber;
        this.analysisProfile = profile != null ? profile : AnalysisProfile.STANDARD;
        this.idempotencyKey = idempotencyKey;
        this.status = ReviewStatus.PENDING;
        this.progress = 0.0;
        this.filesTotal = 0;
        this.filesAnalyzed = 0;
        this.riskItems = List.of();
        this.createdAt = OffsetDateTime.now();
    }

    /** Transition to RUNNING state. */
    public void start(int filesTotal) {
        if (this.status != ReviewStatus.PENDING) {
            throw new IllegalStateException("Can only start a PENDING review, current: " + status);
        }
        this.status = ReviewStatus.RUNNING;
        this.filesTotal = filesTotal;
        this.startedAt = OffsetDateTime.now();
    }

    /** Update progress as files are analyzed. */
    public void updateProgress(int filesAnalyzed) {
        this.filesAnalyzed = filesAnalyzed;
        this.progress = filesTotal > 0 ? (double) filesAnalyzed / filesTotal : 0.0;
    }

    /** Complete the review with results. */
    public void complete(ChangeSummary summary, List<RiskItem> riskItems) {
        this.summary = summary;
        this.riskItems = riskItems == null ? List.of() : List.copyOf(riskItems);
        this.status = ReviewStatus.COMPLETED;
        this.progress = 1.0;
        this.completedAt = OffsetDateTime.now();
    }

    /** Mark as partially completed (budget/size truncation). */
    public void completePartial(ChangeSummary summary, List<RiskItem> riskItems) {
        this.summary = summary;
        this.riskItems = riskItems == null ? List.of() : List.copyOf(riskItems);
        this.status = ReviewStatus.PARTIAL;
        this.completedAt = OffsetDateTime.now();
    }

    /** Mark as failed. */
    public void fail(String reason) {
        this.status = ReviewStatus.FAILED;
        this.failureReason = reason;
        this.completedAt = OffsetDateTime.now();
    }

    // --- Getters (no setters — state changes through domain methods) ---

    public UUID getId() { return id; }
    public String getRepository() { return repository; }
    public int getPrNumber() { return prNumber; }
    public AnalysisProfile getAnalysisProfile() { return analysisProfile; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public ReviewStatus getStatus() { return status; }
    public double getProgress() { return progress; }
    public int getFilesTotal() { return filesTotal; }
    public int getFilesAnalyzed() { return filesAnalyzed; }
    public String getFailureReason() { return failureReason; }
    public ChangeSummary getSummary() { return summary; }
    public List<RiskItem> getRiskItems() { return riskItems; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getStartedAt() { return startedAt; }
    public OffsetDateTime getCompletedAt() { return completedAt; }

    public boolean isCompleted() {
        return status == ReviewStatus.COMPLETED || status == ReviewStatus.PARTIAL;
    }
}
