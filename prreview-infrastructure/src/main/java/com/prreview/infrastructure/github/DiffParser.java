package com.prreview.infrastructure.github;

import com.prreview.domain.model.pr.DiffHunk;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses a unified diff patch string into a list of DiffHunk domain objects.
 * Handles the standard GitHub patch format returned by the files API.
 */
public class DiffParser {

    /** Pattern matching hunk headers: @@ -oldStart,oldLines +newStart,newLines @@ */
    private static final Pattern HUNK_HEADER = Pattern.compile(
            "@@ -(\\d+)(?:,(\\d+))? \\+(\\d+)(?:,(\\d+))? @@.*");

    private DiffParser() {}

    /**
     * Parses a patch string into a list of DiffHunks.
     *
     * @param patch the raw patch string from GitHub API
     * @return list of parsed diff hunks
     */
    public static List<DiffHunk> parse(String patch) {
        if (patch == null || patch.isBlank()) {
            return List.of();
        }

        List<DiffHunk> hunks = new ArrayList<>();
        String[] lines = patch.split("\n");
        int i = 0;

        while (i < lines.length) {
            Matcher m = HUNK_HEADER.matcher(lines[i]);
            if (m.matches()) {
                int oldStart = Integer.parseInt(m.group(1));
                int oldLines = m.group(2) != null ? Integer.parseInt(m.group(2)) : 1;
                int newStart = Integer.parseInt(m.group(3));
                int newLines = m.group(4) != null ? Integer.parseInt(m.group(4)) : 1;

                // Collect hunk content until next hunk header or end
                StringBuilder content = new StringBuilder();
                content.append(lines[i]).append("\n");
                i++;
                while (i < lines.length && !lines[i].startsWith("@@")) {
                    content.append(lines[i]).append("\n");
                    i++;
                }

                hunks.add(new DiffHunk(oldStart, oldLines, newStart, newLines,
                        content.toString()));
            } else {
                i++;
            }
        }

        return hunks;
    }
}
