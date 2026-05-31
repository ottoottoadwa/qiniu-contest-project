package com.prreview.domain.port.out;

import com.prreview.domain.model.pr.FileChange;
import com.prreview.domain.model.pr.PullRequest;
import com.prreview.domain.model.pr.RepositoryRef;

import java.util.List;

/**
 * Outbound port for fetching pull request data from a code hosting platform.
 * Implemented by GitHubPrSourceAdapter in infrastructure.
 */
public interface PrSource {

    /** Fetches PR metadata (title, description, author, SHAs, state). */
    PullRequest fetchPullRequest(RepositoryRef repo, int number);

    /** Fetches the list of changed files with their diff hunks. */
    List<FileChange> fetchChangedFiles(RepositoryRef repo, int number);

    /** Fetches the full content of a file at a specific ref (commit SHA or branch). */
    String fetchFileContent(RepositoryRef repo, String path, String ref);
}
