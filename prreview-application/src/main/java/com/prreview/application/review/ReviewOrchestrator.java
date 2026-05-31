package com.prreview.application.review;

import com.prreview.application.context.ContextAssembler;
import com.prreview.application.context.ContextPackage;
import com.prreview.domain.model.pr.FileChange;
import com.prreview.domain.model.pr.PullRequest;
import com.prreview.domain.model.pr.RepositoryRef;
import com.prreview.domain.model.review.AnalysisProfile;
import com.prreview.domain.model.review.Review;
import com.prreview.domain.model.risk.RiskCategory;
import com.prreview.domain.port.out.PrSource;
import com.prreview.domain.port.out.ReviewRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Orchestrates the full PR review flow:
 * fetch PR → assemble context → analyze → persist results.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewOrchestrator {

    private final PrSource prSource;
    private final ContextAssembler contextAssembler;
    private final AnalysisEngine analysisEngine;
    private final ReviewRepositoryPort reviewRepository;

    /**
     * Executes the full review pipeline for a given review task.
     * Called asynchronously by ReviewTaskRunner.
     *
     * @param reviewId   ID of the review to execute
     * @param categories risk categories to analyze (empty = all)
     */
    @Transactional
    public void executeReview(UUID reviewId, Set<RiskCategory> categories) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Review not found: " + reviewId));

        log.info("Starting review execution: reviewId={}, repo={}, pr={}",
                reviewId, review.getRepository(), review.getPrNumber());

        try {
            RepositoryRef repoRef = RepositoryRef.parse(review.getRepository());

            // Step 1: Fetch PR data
            PullRequest pr = prSource.fetchPullRequest(repoRef, review.getPrNumber());
            List<FileChange> fileChanges = prSource.fetchChangedFiles(repoRef, review.getPrNumber());

            review.start(fileChanges.size());
            reviewRepository.save(review);

            // Step 2: Assemble context
            List<ContextPackage> contextPackages = contextAssembler.assemble(pr, fileChanges);

            // Step 3: Analyze
            AnalysisEngine.AnalysisResult result = analysisEngine.analyze(
                    reviewId.toString(), contextPackages,
                    review.getAnalysisProfile(),
                    categories.isEmpty() ? Set.of(RiskCategory.values()) : categories,
                    Map.of()); // calibration map — empty for MVP

            // Step 4: Complete
            review.complete(result.summary(), result.riskItems());
            reviewRepository.save(review);

            log.info("Review completed: reviewId={}, riskItems={}",
                    reviewId, result.riskItems().size());

        } catch (Exception e) {
            log.error("Review execution failed: reviewId={}", reviewId, e);
            review.fail(e.getMessage());
            reviewRepository.save(review);
        }
    }
}
