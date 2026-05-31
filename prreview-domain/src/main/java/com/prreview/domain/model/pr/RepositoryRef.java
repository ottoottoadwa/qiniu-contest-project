package com.prreview.domain.model.pr;

/**
 * Identifies a GitHub repository by owner and name.
 * Self-validating value object — illegal states are unrepresentable.
 */
public record RepositoryRef(String owner, String name) {

    public RepositoryRef {
        if (owner == null || owner.isBlank()) {
            throw new IllegalArgumentException("Repository owner must not be blank");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Repository name must not be blank");
        }
    }

    /** Parses "owner/repo" format. */
    public static RepositoryRef parse(String ownerSlashRepo) {
        if (ownerSlashRepo == null) {
            throw new IllegalArgumentException("Repository reference must not be null");
        }
        String[] parts = ownerSlashRepo.split("/", 2);
        if (parts.length != 2) {
            throw new IllegalArgumentException(
                    "Repository reference must be in 'owner/repo' format, got: " + ownerSlashRepo);
        }
        return new RepositoryRef(parts[0], parts[1]);
    }

    /** Returns "owner/repo" string representation. */
    public String toSlashNotation() {
        return owner + "/" + name;
    }
}
