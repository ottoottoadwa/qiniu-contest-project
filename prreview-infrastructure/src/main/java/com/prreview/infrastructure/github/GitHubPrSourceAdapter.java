package com.prreview.infrastructure.github;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.prreview.domain.model.pr.ChangeType;
import com.prreview.domain.model.pr.FileChange;
import com.prreview.domain.model.pr.PrState;
import com.prreview.domain.model.pr.PullRequest;
import com.prreview.domain.model.pr.RepositoryRef;
import com.prreview.domain.port.out.PrSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Implements PrSource using the GitHub REST API v3.
 * Uses PAT authentication for MVP; GitHub App tokens for production.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GitHubPrSourceAdapter implements PrSource {

    private final RestClient gitHubRestClient;
    private final GitHubProperties properties;

    @Override
    public PullRequest fetchPullRequest(RepositoryRef repo, int number) {
        log.debug("Fetching PR: {}/{}", repo.toSlashNotation(), number);
        try {
            GitHubPrResponse response = gitHubRestClient.get()
                    .uri("/repos/{owner}/{repo}/pulls/{number}",
                            repo.owner(), repo.name(), number)
                    .retrieve()
                    .body(GitHubPrResponse.class);

            if (response == null) {
                throw new IllegalStateException("Empty response from GitHub for PR " + number);
            }

            return new PullRequest(
                    repo, number,
                    response.title(),
                    response.body(),
                    response.user() != null ? response.user().login() : "unknown",
                    response.base() != null ? response.base().sha() : null,
                    response.head() != null ? response.head().sha() : null,
                    mapState(response.state()));
        } catch (HttpClientErrorException.NotFound e) {
            throw new PrNotFoundException(repo.toSlashNotation(), number);
        }
    }

    @Override
    public List<FileChange> fetchChangedFiles(RepositoryRef repo, int number) {
        log.debug("Fetching changed files: {}/{}", repo.toSlashNotation(), number);
        List<FileChange> result = new ArrayList<>();
        int page = 1;
        int perPage = 100;

        while (result.size() < properties.maxFilesPerPr()) {
            List<GitHubFileResponse> files = gitHubRestClient.get()
                    .uri("/repos/{owner}/{repo}/pulls/{number}/files?per_page={perPage}&page={page}",
                            repo.owner(), repo.name(), number, perPage, page)
                    .retrieve()
                    .body(new org.springframework.core.ParameterizedTypeReference<>() {});

            if (files == null || files.isEmpty()) {
                break;
            }

            for (GitHubFileResponse file : files) {
                result.add(new FileChange(
                        file.filename(),
                        mapChangeType(file.status()),
                        file.additions(),
                        file.deletions(),
                        DiffParser.parse(file.patch()),
                        file.patch() == null && file.additions() + file.deletions() > 0));
            }

            if (files.size() < perPage) {
                break; // last page
            }
            page++;
        }

        log.debug("Fetched {} changed files for PR {}", result.size(), number);
        return result;
    }

    @Override
    public String fetchFileContent(RepositoryRef repo, String path, String ref) {
        log.debug("Fetching file content: {}/{} @ {}", repo.toSlashNotation(), path, ref);
        try {
            GitHubContentResponse response = gitHubRestClient.get()
                    .uri("/repos/{owner}/{repo}/contents/{path}?ref={ref}",
                            repo.owner(), repo.name(), path, ref)
                    .retrieve()
                    .body(GitHubContentResponse.class);

            if (response == null || response.content() == null) {
                return "";
            }

            // GitHub returns base64-encoded content
            String cleaned = response.content().replaceAll("\\s", "");
            return new String(Base64.getDecoder().decode(cleaned));
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("File not found: {}/{} @ {}", repo.toSlashNotation(), path, ref);
            return "";
        }
    }

    private PrState mapState(String state) {
        if (state == null) return PrState.OPEN;
        return switch (state.toLowerCase()) {
            case "closed" -> PrState.CLOSED;
            case "merged" -> PrState.MERGED;
            default -> PrState.OPEN;
        };
    }

    private ChangeType mapChangeType(String status) {
        if (status == null) return ChangeType.MODIFIED;
        return switch (status.toLowerCase()) {
            case "added" -> ChangeType.ADDED;
            case "removed" -> ChangeType.DELETED;
            case "renamed" -> ChangeType.RENAMED;
            default -> ChangeType.MODIFIED;
        };
    }

    // --- GitHub API response records ---

    @JsonIgnoreProperties(ignoreUnknown = true)
    record GitHubPrResponse(
            String title,
            String body,
            String state,
            GitHubUserResponse user,
            GitHubRefResponse base,
            GitHubRefResponse head) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record GitHubUserResponse(String login) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record GitHubRefResponse(String sha, String ref) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record GitHubFileResponse(
            String filename,
            String status,
            int additions,
            int deletions,
            String patch) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record GitHubContentResponse(
            String content,
            String encoding) {}

    /** Thrown when a PR is not found on GitHub. */
    public static class PrNotFoundException extends RuntimeException {
        public PrNotFoundException(String repo, int number) {
            super("PR not found: " + repo + "#" + number);
        }
    }
}
