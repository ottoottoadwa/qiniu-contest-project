package com.prreview.web.webhook;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Receives GitHub webhook events.
 * Handles issue_comment events to trigger PR reviews.
 */
@Slf4j
@RestController
@RequestMapping("/api/webhook")
@RequiredArgsConstructor
public class WebhookController {

    private final WebhookService webhookService;

    /**
     * GitHub webhook endpoint.
     * Receives events like issue_comment, pull_request, etc.
     *
     * @param eventType GitHub event type from X-GitHub-Event header
     * @param signature GitHub signature from X-Hub-Signature-256 header
     * @param payload   webhook payload
     * @return 200 OK if processed, 202 Accepted if ignored
     */
    @PostMapping("/github")
    public ResponseEntity<Map<String, String>> handleGitHubWebhook(
            @RequestHeader(value = "X-GitHub-Event", required = false) String eventType,
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
            @RequestBody Map<String, Object> payload) {

        log.info("Received GitHub webhook: event={}", eventType);
        log.debug("Webhook payload keys: {}", payload.keySet());

        try {
            // Verify signature (optional for MVP, required for production)
            // webhookService.verifySignature(payload, signature);

            // Handle different event types
            if ("pull_request".equals(eventType)) {
                // Auto-review on PR open/update
                boolean handled = webhookService.handlePullRequest(payload);
                if (handled) {
                    return ResponseEntity.ok(Map.of("status", "processing"));
                } else {
                    return ResponseEntity.accepted().body(Map.of("status", "ignored"));
                }
            } else if ("issue_comment".equals(eventType)) {
                // Manual trigger via /review comment
                boolean handled = webhookService.handleIssueComment(payload);
                if (handled) {
                    return ResponseEntity.ok(Map.of("status", "processing"));
                } else {
                    return ResponseEntity.accepted().body(Map.of("status", "ignored"));
                }
            } else {
                log.debug("Unhandled event type: {}", eventType);
                return ResponseEntity.accepted().body(Map.of("status", "ignored"));
            }
        } catch (Exception e) {
            log.error("Webhook processing failed: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Health check endpoint for webhook configuration.
     */
    @GetMapping("/github/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "service", "prreview-webhook"
        ));
    }
}
