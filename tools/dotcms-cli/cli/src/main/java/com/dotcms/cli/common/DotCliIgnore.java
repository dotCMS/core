package com.dotcms.cli.common;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.List;
import org.jboss.logging.Logger;

/**
 * Parses and manages .dotcliignore files for filtering files and directories during push operations.
 * Supports standard glob patterns similar to .gitignore:
 * <ul>
 *   <li>'*' - matches any sequence of characters within a path segment</li>
 *   <li>'**' - matches any sequence of characters across path segments</li>
 *   <li>'?' - matches any single character</li>
 *   <li>'[]' - matches a character class</li>
 *   <li>'!' - negates a pattern (includes files that were previously excluded)</li>
 * </ul>
 * <p>
 * Patterns are evaluated relative to the .dotcliignore file location.
 * Comments (lines starting with '#') and blank lines are ignored.
 * </p>
 */
public class DotCliIgnore {

    private static final Logger logger = Logger.getLogger(DotCliIgnore.class);
    private static final String DOTCLIIGNORE_FILE = ".dotcliignore";

    private final List<PatternRule> rules;
    private final Path basePath;

    /**
     * Pattern rule representing a single line from .dotcliignore file.
     */
    private static class PatternRule {
        final PathMatcher matcher;
        final boolean negated;
        final String originalPattern;

        PatternRule(PathMatcher matcher, boolean negated, String originalPattern) {
            this.matcher = matcher;
            this.negated = negated;
            this.originalPattern = originalPattern;
        }
    }

    /**
     * Creates a DotCliIgnore instance with patterns loaded from the .dotcliignore file
     * located in the specified base directory.
     *
     * @param basePath the base directory containing the .dotcliignore file
     * @param workspaceRoot the workspace root directory for hierarchical pattern loading
     * @throws IOException if an I/O error occurs while reading the file
     */
    public DotCliIgnore(Path basePath, Path workspaceRoot) throws IOException {
        this.basePath = basePath;
        this.rules = new ArrayList<>();
        loadIgnoreFilesHierarchically(basePath, workspaceRoot);
    }

    /**
     * Creates an empty DotCliIgnore without loading any patterns.
     * Used when error occurs during file loading.
     *
     * @param basePath the base directory for pattern matching
     */
    private DotCliIgnore(Path basePath) {
        this.basePath = basePath;
        this.rules = new ArrayList<>();
    }

    /**
     * Creates a DotCliIgnore instance. Loads patterns hierarchically from the base directory
     * up to the workspace root, merging all .dotcliignore files found along the way.
     * Patterns closer to the base directory take precedence over patterns from parent directories.
     *
     * @param basePath the base directory to start loading patterns from
     * @return a new DotCliIgnore instance
     */
    public static DotCliIgnore create(Path basePath) {
        return create(basePath, basePath);
    }

    /**
     * Creates a DotCliIgnore instance with hierarchical pattern loading.
     * Loads patterns from .dotcliignore files starting from basePath up to workspaceRoot.
     *
     * @param basePath the base directory to start loading patterns from
     * @param workspaceRoot the workspace root directory (top-level boundary for pattern search)
     * @return a new DotCliIgnore instance
     */
    public static DotCliIgnore create(Path basePath, Path workspaceRoot) {
        try {
            // Normalize paths for comparison
            Path normalizedBase = basePath.toAbsolutePath().normalize();
            Path normalizedRoot = workspaceRoot.toAbsolutePath().normalize();

            logger.debug(String.format("Creating DotCliIgnore with hierarchical loading from %s to %s",
                                      normalizedBase, normalizedRoot));

            return new DotCliIgnore(normalizedBase, normalizedRoot);
        } catch (IOException e) {
            logger.warn("Error reading .dotcliignore files: " + e.getMessage());
            return new DotCliIgnore(basePath);
        }
    }

    /**
     * Loads patterns hierarchically from .dotcliignore files starting from startPath
     * up to workspaceRoot. Patterns are loaded from parent to child (root to leaf),
     * so patterns closer to the file being checked have higher precedence and are
     * evaluated last.
     *
     * @param startPath the starting directory to load patterns from
     * @param workspaceRoot the workspace root directory (top boundary)
     * @throws IOException if an I/O error occurs while reading files
     */
    private void loadIgnoreFilesHierarchically(Path startPath, Path workspaceRoot) throws IOException {
        // Normalize paths for proper comparison
        Path currentPath = startPath.toAbsolutePath().normalize();
        final Path rootPath = workspaceRoot.toAbsolutePath().normalize();

        // Collect all .dotcliignore file paths from child to parent
        List<Path> ignoreFilePaths = new ArrayList<>();

        // Walk up the directory tree from startPath to workspaceRoot
        while (currentPath != null) {
            final Path ignoreFile = currentPath.resolve(DOTCLIIGNORE_FILE);

            if (Files.exists(ignoreFile) && Files.isRegularFile(ignoreFile)) {
                ignoreFilePaths.add(ignoreFile);
            }

            // Stop if we've reached the workspace root or can't go higher
            if (currentPath.equals(rootPath)) {
                break;
            }

            // Move up to parent directory
            currentPath = currentPath.getParent();
        }

        // Load patterns in reverse order (parent to child) so child patterns
        // are evaluated last and have higher precedence
        for (int i = ignoreFilePaths.size() - 1; i >= 0; i--) {
            Path ignoreFile = ignoreFilePaths.get(i);
            logger.debug("Loading .dotcliignore from: " + ignoreFile);
            loadIgnoreFile(ignoreFile);
        }

        if (!ignoreFilePaths.isEmpty()) {
            logger.debug(String.format("Loaded %d .dotcliignore file(s) hierarchically", ignoreFilePaths.size()));
        } else {
            logger.debug("No .dotcliignore files found in hierarchy");
        }
    }

    /**
     * Loads patterns from a specific .dotcliignore file.
     *
     * @param ignoreFile the path to the .dotcliignore file
     * @throws IOException if an I/O error occurs while reading the file
     */
    private void loadIgnoreFile(Path ignoreFile) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(ignoreFile)) {
            String line;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                try {
                    processLine(trimIgnoreLine(line), lineNumber);
                } catch (IllegalArgumentException e) {
                    logger.error(String.format("Invalid pattern at line %d in %s: %s",
                                              lineNumber, ignoreFile, e.getMessage()));
                    throw e;
                }
            }
        }
    }

    /**
     * Trims a line from .dotcliignore file, preserving escaped trailing spaces.
     * Leading whitespace is always removed. Trailing whitespace is removed unless
     * preceded by a backslash (escaped).
     *
     * @param line the line to trim
     * @return the trimmed line with escaped spaces preserved
     */
    private String trimIgnoreLine(String line) {
        if (line == null) {
            return "";
        }

        // Remove leading whitespace
        String trimmedLeft = line.replaceFirst("^\\s+", "");

        // Handle trailing spaces: remove unless escaped with backslash
        // We need to check if trailing spaces are escaped
        StringBuilder result = new StringBuilder(trimmedLeft);

        // Remove trailing whitespace working backwards
        int lastNonSpace = result.length() - 1;
        while (lastNonSpace >= 0 && Character.isWhitespace(result.charAt(lastNonSpace))) {
            lastNonSpace--;
        }

        // Check if the last non-space character is a backslash (escape)
        if (lastNonSpace >= 0 && result.charAt(lastNonSpace) == '\\') {
            // Count consecutive backslashes before the trailing spaces
            int backslashCount = 0;
            int pos = lastNonSpace;
            while (pos >= 0 && result.charAt(pos) == '\\') {
                backslashCount++;
                pos--;
            }

            // If odd number of backslashes, the last one escapes the trailing space
            if (backslashCount % 2 == 1) {
                // Keep one trailing space and remove the escape backslash
                result.delete(lastNonSpace + 1, result.length()); // Remove trailing spaces
                result.setCharAt(lastNonSpace, ' '); // Replace last backslash with space
                return result.toString();
            }
        }

        // No escaped space, remove all trailing whitespace
        return result.substring(0, lastNonSpace + 1);
    }


    /**
     * Processes a single line from the .dotcliignore file.
     *
     * @param line the line to process
     * @param lineNumber the line number (for error reporting)
     */
    private void processLine(String line, int lineNumber) {
        // Skip empty lines and comments
        if (line.isEmpty() || line.startsWith("#")) {
            return;
        }

        // Check for negation pattern
        boolean negated = false;
        if (line.startsWith("!")) {
            negated = true;
            line = line.substring(1).trim();

            if (line.isEmpty()) {
                throw new IllegalArgumentException(
                    "Invalid negation pattern at line " + lineNumber + ": pattern cannot be empty after '!'"
                );
            }
        }

        addPattern(line, negated);
    }

    /**
     * Adds a pattern to the rules list.
     *
     * @param pattern the glob pattern to add
     * @param negated whether this is a negation pattern
     * @throws IllegalArgumentException if the pattern is invalid
     */
    private void addPattern(String pattern, boolean negated) {
        try {
            // Convert .gitignore-style patterns to glob patterns
            String globPattern = convertToGlobPattern(pattern);

            FileSystem fileSystem = FileSystems.getDefault();
            PathMatcher matcher = fileSystem.getPathMatcher("glob:" + globPattern);

            rules.add(new PatternRule(matcher, negated, pattern));
            logger.debug("Added " + (negated ? "negation " : "") + "pattern: " + pattern + " (glob: " + globPattern + ")");

        } catch (Exception e) {
            throw new IllegalArgumentException(
                "Invalid glob pattern: '" + pattern + "'. " +
                "Supported patterns: *, **, ?, [], and ! for negation. Error: " + e.getMessage()
            );
        }
    }

    /**
     * Converts a .gitignore-style pattern to a Java glob pattern.
     * Java's glob syntax requires special handling to match .gitignore behavior:
     * - Simple patterns like "*.log" should match at root AND in subdirectories
     * - Directory patterns like "build/" should match the directory and its contents
     *
     * @param pattern the .gitignore-style pattern
     * @return the equivalent glob pattern
     */
    private String convertToGlobPattern(String pattern) {
        // Handle patterns starting with / (absolute from workspace root)
        if (pattern.startsWith("/")) {
            // Absolute path from workspace root (relative to base path)
            return pattern.substring(1);
        }

        // Handle patterns starting with ** and ending with / (e.g., "**/node_modules/")
        if (pattern.startsWith("**/") && pattern.endsWith("/")) {
            // Remove both prefix and suffix, then build comprehensive pattern
            String dirName = pattern.substring(3, pattern.length() - 1); // Remove "**/" and "/"
            // Match: dir, dir/*, dir/subdir, dir/subdir/*, **/dir, **/dir/*, etc.
            return "{" + dirName + "," + dirName + "/**,**/" + dirName + ",**/" + dirName + "/**}";
        }

        // Handle patterns already containing ** (full path wildcards)
        if (pattern.startsWith("**/")) {
            // Pattern explicitly uses **, but also needs to match at root level
            // e.g., "**/.DS_Store" should match both ".DS_Store" and "dir/.DS_Store"
            String patternWithoutPrefix = pattern.substring(3); // Remove "**/"
            return "{" + patternWithoutPrefix + "," + pattern + "}";
        }

        // Handle directory patterns (ending with /)
        if (pattern.endsWith("/")) {
            // Match directory and everything inside it
            // Remove trailing slash and add patterns for both directory and contents
            String dirPattern = pattern.substring(0, pattern.length() - 1);
            // Match: dir, dir/*, dir/subdir, dir/subdir/*, etc.
            return "{" + dirPattern + "," + dirPattern + "/**,**/" + dirPattern + ",**/" + dirPattern + "/**}";
        }

        // Handle patterns containing / (path patterns)
        if (pattern.contains("/")) {
            // Match both at root and in subdirectories
            // e.g., "src/*.log" should match "src/error.log" and "project/src/error.log"
            return "{" + pattern + ",**/" + pattern + "}";
        }

        // Simple filename patterns (no directory separators)
        // Must match at root level AND in any subdirectory
        // e.g., "*.log" should match "error.log" and "dir/error.log"
        return "{" + pattern + ",**/" + pattern + "}";
    }

    /**
     * Checks if a file or directory should be ignored based on the loaded patterns.
     * Patterns are evaluated in order, with later patterns overriding earlier ones.
     * Negation patterns (starting with !) can re-include files that were previously excluded.
     *
     * @param file the file to check
     * @return true if the file should be ignored, false otherwise
     */
    public boolean shouldIgnore(File file) {
        return shouldIgnore(file.toPath());
    }

    /**
     * Checks if a path should be ignored based on the loaded patterns.
     *
     * @param path the path to check
     * @return true if the path should be ignored, false otherwise
     */
    public boolean shouldIgnore(Path path) {
        // Calculate relative path from base path
        Path relativePath;
        try {
            if (path.isAbsolute()) {
                relativePath = basePath.relativize(path);
            } else {
                relativePath = path;
            }
        } catch (IllegalArgumentException e) {
            // Path is outside the base path, don't ignore it
            logger.debug("Path outside base path, not ignoring: " + path);
            return false;
        }

        // Evaluate patterns in order
        boolean ignored = false;
        for (PatternRule rule : rules) {
            if (rule.matcher.matches(relativePath)) {
                ignored = !rule.negated; // Negation flips the result
                logger.debug("Pattern '" + rule.originalPattern + "' matched path: " + relativePath +
                           " (ignored=" + ignored + ")");
            }
        }

        return ignored;
    }

    /**
     * Gets the base path for this DotCliIgnore instance.
     *
     * @return the base path
     */
    public Path getBasePath() {
        return basePath;
    }

    /**
     * Gets the number of loaded patterns (including default patterns).
     *
     * @return the number of patterns
     */
    public int getPatternCount() {
        return rules.size();
    }
}
