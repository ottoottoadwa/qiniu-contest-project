package com.prreview.domain.model;

import com.prreview.domain.model.pr.RepositoryRef;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for RepositoryRef value object.
 * Verifies self-validation and parsing behavior.
 */
class RepositoryRefTest {

    @Test
    @DisplayName("shouldCreateRepositoryRef_whenValidOwnerAndName")
    void shouldCreateRepositoryRef_whenValidOwnerAndName() {
        // Act
        RepositoryRef ref = new RepositoryRef("octocat", "hello-world");

        // Assert
        assertThat(ref.owner()).isEqualTo("octocat");
        assertThat(ref.name()).isEqualTo("hello-world");
        assertThat(ref.toSlashNotation()).isEqualTo("octocat/hello-world");
    }

    @Test
    @DisplayName("shouldParseSlashNotation_whenValidFormat")
    void shouldParseSlashNotation_whenValidFormat() {
        // Act
        RepositoryRef ref = RepositoryRef.parse("spring-projects/spring-boot");

        // Assert
        assertThat(ref.owner()).isEqualTo("spring-projects");
        assertThat(ref.name()).isEqualTo("spring-boot");
    }

    @Test
    @DisplayName("shouldThrowIllegalArgumentException_whenOwnerIsBlank")
    void shouldThrowIllegalArgumentException_whenOwnerIsBlank() {
        assertThatThrownBy(() -> new RepositoryRef("", "hello-world"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("owner");
    }

    @Test
    @DisplayName("shouldThrowIllegalArgumentException_whenNameIsNull")
    void shouldThrowIllegalArgumentException_whenNameIsNull() {
        assertThatThrownBy(() -> new RepositoryRef("octocat", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name");
    }

    @Test
    @DisplayName("shouldThrowIllegalArgumentException_whenParsingInvalidFormat")
    void shouldThrowIllegalArgumentException_whenParsingInvalidFormat() {
        assertThatThrownBy(() -> RepositoryRef.parse("no-slash-here"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("owner/repo");
    }

    @Test
    @DisplayName("shouldThrowIllegalArgumentException_whenParsingNull")
    void shouldThrowIllegalArgumentException_whenParsingNull() {
        assertThatThrownBy(() -> RepositoryRef.parse(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
