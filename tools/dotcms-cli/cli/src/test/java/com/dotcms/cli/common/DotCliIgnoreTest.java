package com.dotcms.cli.common;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DotCliIgnoreTest {

    @TempDir
    Path tempDir;

    /**
     * Creates a default .dotcliignore file with common patterns for testing.
     * This helper method provides a standard set of patterns that can be used
     * across multiple tests.
     *
     * @param targetDir the directory where the .dotcliignore file should be created
     * @throws IOException if an I/O error occurs while writing the file
     */
    private void createDefaultDotCliIgnore(Path targetDir) throws IOException {
        Path ignoreFile = targetDir.resolve(".dotcliignore");
        String defaultPatterns =
                "# Version control\n" +
                ".git/\n" +
                ".svn/\n" +
                "\n" +
                "# Dependencies\n" +
                "node_modules/\n" +
                "vendor/\n" +
                "\n" +
                "# Build outputs\n" +
                "build/\n" +
                "dist/\n" +
                "target/\n" +
                "*.class\n" +
                "\n" +
                "# OS files\n" +
                "**/.DS_Store\n" +
                "**/Thumbs.db\n" +
                "\n" +
                "# Log files\n" +
                "*.log\n" +
                "\n" +
                "# Temporary files\n" +
                "*.tmp\n" +
                "*.temp\n";
        Files.writeString(ignoreFile, defaultPatterns);
    }

    /**
     * Creates a custom .dotcliignore file with the specified patterns.
     *
     * @param targetDir the directory where the .dotcliignore file should be created
     * @param patterns the patterns to write to the file
     * @throws IOException if an I/O error occurs while writing the file
     */
    private void createCustomDotCliIgnore(Path targetDir, String patterns) throws IOException {
        Path ignoreFile = targetDir.resolve(".dotcliignore");
        Files.writeString(ignoreFile, patterns);
    }

    @Test
    void testNoPatternsWhenNoFileExists() {
        // Arrange & Act
        DotCliIgnore dotCliIgnore = DotCliIgnore.create(tempDir);

        // Assert - verify no patterns are loaded when no .dotcliignore file exists
        assertEquals(0, dotCliIgnore.getPatternCount(), "Should have no patterns loaded");

        // Nothing should be ignored
        assertFalse(dotCliIgnore.shouldIgnore(tempDir.resolve(".git").resolve("config")));
        assertFalse(dotCliIgnore.shouldIgnore(tempDir.resolve("node_modules").resolve("package.json")));
        assertFalse(dotCliIgnore.shouldIgnore(tempDir.resolve("test.log")));
        assertFalse(dotCliIgnore.shouldIgnore(tempDir.resolve("subdir").resolve(".DS_Store")));
    }

    @Test
    void testSimpleFilePatterns() throws IOException {
        // Arrange
        createCustomDotCliIgnore(tempDir,
                "*.log\n" +
                "*.tmp\n" +
                "test.txt\n"
        );

        // Act
        DotCliIgnore dotCliIgnore = DotCliIgnore.create(tempDir);

        // Assert
        assertTrue(dotCliIgnore.shouldIgnore(tempDir.resolve("error.log")));
        assertTrue(dotCliIgnore.shouldIgnore(tempDir.resolve("temp.tmp")));
        assertTrue(dotCliIgnore.shouldIgnore(tempDir.resolve("test.txt")));
        assertFalse(dotCliIgnore.shouldIgnore(tempDir.resolve("readme.md")));
    }

    @Test
    void testDefaultPatterns() throws IOException {
        // Arrange
        createDefaultDotCliIgnore(tempDir);

        // Act
        DotCliIgnore dotCliIgnore = DotCliIgnore.create(tempDir);

        // Assert - verify default patterns work
        assertTrue(dotCliIgnore.getPatternCount() > 0, "Should have patterns loaded");

        // Test default patterns
        assertTrue(dotCliIgnore.shouldIgnore(tempDir.resolve(".git").resolve("config")));
        assertTrue(dotCliIgnore.shouldIgnore(tempDir.resolve("node_modules").resolve("package.json")));
        assertTrue(dotCliIgnore.shouldIgnore(tempDir.resolve("test.log")));
        assertTrue(dotCliIgnore.shouldIgnore(tempDir.resolve("subdir").resolve(".DS_Store")));
        assertTrue(dotCliIgnore.shouldIgnore(tempDir.resolve("build").resolve("output.jar")));
        assertTrue(dotCliIgnore.shouldIgnore(tempDir.resolve("temp.tmp")));
        assertFalse(dotCliIgnore.shouldIgnore(tempDir.resolve("readme.md")));
    }

    @Test
    void testDirectoryPatterns() throws IOException {
        // Arrange
        createCustomDotCliIgnore(tempDir,
                "build/\n" +
                "dist/\n" +
                "target/\n"
        );

        // Act
        DotCliIgnore dotCliIgnore = DotCliIgnore.create(tempDir);

        // Assert
        assertTrue(dotCliIgnore.shouldIgnore(tempDir.resolve("build")));
        assertTrue(dotCliIgnore.shouldIgnore(tempDir.resolve("build").resolve("output.jar")));
        assertTrue(dotCliIgnore.shouldIgnore(tempDir.resolve("dist").resolve("bundle.js")));
        assertTrue(dotCliIgnore.shouldIgnore(tempDir.resolve("target").resolve("classes").resolve("Main.class")));
        assertFalse(dotCliIgnore.shouldIgnore(tempDir.resolve("src").resolve("Main.java")));
    }

    @Test
    void testDoubleStarPattern() throws IOException {
        // Arrange
        createCustomDotCliIgnore(tempDir,
                "**/.DS_Store\n" +
                "**/node_modules/\n"
        );

        // Act
        DotCliIgnore dotCliIgnore = DotCliIgnore.create(tempDir);

        // Assert
        assertTrue(dotCliIgnore.shouldIgnore(tempDir.resolve(".DS_Store")));
        assertTrue(dotCliIgnore.shouldIgnore(tempDir.resolve("src").resolve(".DS_Store")));
        assertTrue(dotCliIgnore.shouldIgnore(tempDir.resolve("src").resolve("main").resolve("java").resolve(".DS_Store")));
        assertTrue(dotCliIgnore.shouldIgnore(tempDir.resolve("node_modules").resolve("package.json")));
        assertTrue(dotCliIgnore.shouldIgnore(tempDir.resolve("project").resolve("node_modules").resolve("lib.js")));
    }

    @Test
    void testNegationPattern() throws IOException {
        // Arrange
        createCustomDotCliIgnore(tempDir,
                "*.log\n" +
                "!important.log\n"
        );

        // Act
        DotCliIgnore dotCliIgnore = DotCliIgnore.create(tempDir);

        // Assert
        assertTrue(dotCliIgnore.shouldIgnore(tempDir.resolve("error.log")));
        assertTrue(dotCliIgnore.shouldIgnore(tempDir.resolve("debug.log")));
        assertFalse(dotCliIgnore.shouldIgnore(tempDir.resolve("important.log")),
                   "important.log should not be ignored due to negation pattern");
    }

    @Test
    void testCommentsAndBlankLines() throws IOException {
        // Arrange
        createCustomDotCliIgnore(tempDir,
                "# This is a comment\n" +
                "\n" +
                "*.log\n" +
                "  \n" +
                "# Another comment\n" +
                "*.tmp\n"
        );

        // Act
        DotCliIgnore dotCliIgnore = DotCliIgnore.create(tempDir);

        // Assert
        assertTrue(dotCliIgnore.shouldIgnore(tempDir.resolve("test.log")));
        assertTrue(dotCliIgnore.shouldIgnore(tempDir.resolve("temp.tmp")));
        assertFalse(dotCliIgnore.shouldIgnore(tempDir.resolve("readme.md")));
    }

    @Test
    void testInvalidPatternThrowsException() throws IOException {
        // Arrange
        createCustomDotCliIgnore(tempDir, "!\n"); // Negation with no pattern

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            DotCliIgnore.create(tempDir);
        });
    }

    @Test
    void testEmptyIgnoreFile() throws IOException {
        // Arrange
        createCustomDotCliIgnore(tempDir, "");

        // Act
        DotCliIgnore dotCliIgnore = DotCliIgnore.create(tempDir);

        // Assert - should have no patterns since file is empty
        assertEquals(0, dotCliIgnore.getPatternCount(), "Should have no patterns for empty file");
        assertFalse(dotCliIgnore.shouldIgnore(tempDir.resolve(".git").resolve("config")));
    }

    @Test
    void testGetBasePath() {
        // Act
        DotCliIgnore dotCliIgnore = DotCliIgnore.create(tempDir);

        // Assert
        assertEquals(tempDir.toAbsolutePath().normalize(),
                    dotCliIgnore.getBasePath().toAbsolutePath().normalize());
    }

    @Test
    void testFileFilter() throws IOException {
        // Arrange
        createCustomDotCliIgnore(tempDir, "*.log\n");

        DotCliIgnore dotCliIgnore = DotCliIgnore.create(tempDir);

        // Create test files
        Path logFile = tempDir.resolve("test.log");
        Path txtFile = tempDir.resolve("test.txt");
        Files.createFile(logFile);
        Files.createFile(txtFile);

        // Act
        boolean logIgnored = dotCliIgnore.shouldIgnore(logFile.toFile());
        boolean txtIgnored = dotCliIgnore.shouldIgnore(txtFile.toFile());

        // Assert
        assertTrue(logIgnored);
        assertFalse(txtIgnored);
    }

    @Test
    void testHierarchicalPatternLoading() throws IOException {
        // Arrange - create nested directory structure with multiple .dotcliignore files
        Path rootDir = tempDir;
        Path subDir1 = rootDir.resolve("project");
        Path subDir2 = subDir1.resolve("src");
        Path subDir3 = subDir2.resolve("main");

        Files.createDirectories(subDir3);

        // Root .dotcliignore - ignores all .log files
        createCustomDotCliIgnore(rootDir, "*.log\n");

        // Subdirectory .dotcliignore - ignores .tmp files and re-includes important.log
        createCustomDotCliIgnore(subDir1, "*.tmp\n!important.log\n");

        // Deep subdirectory .dotcliignore - ignores .bak files
        createCustomDotCliIgnore(subDir3, "*.bak\n");

        // Act - create DotCliIgnore from the deepest directory
        DotCliIgnore dotCliIgnore = DotCliIgnore.create(subDir3, rootDir);

        // Assert - patterns from all levels should be active
        assertTrue(dotCliIgnore.shouldIgnore(subDir3.resolve("error.log")),
                  "Should ignore .log from root");
        assertTrue(dotCliIgnore.shouldIgnore(subDir3.resolve("temp.tmp")),
                  "Should ignore .tmp from project/");
        assertTrue(dotCliIgnore.shouldIgnore(subDir3.resolve("backup.bak")),
                  "Should ignore .bak from main/");
        assertFalse(dotCliIgnore.shouldIgnore(subDir3.resolve("important.log")),
                   "Should not ignore important.log due to negation in project/");
        assertFalse(dotCliIgnore.shouldIgnore(subDir3.resolve("readme.md")),
                   "Should not ignore unmatched files");
    }

    @Test
    void testHierarchicalPatternPrecedence() throws IOException {
        // Arrange - test that closer patterns override parent patterns
        Path rootDir = tempDir;
        Path subDir = rootDir.resolve("src");
        Files.createDirectories(subDir);

        // Root ignores all .txt files
        createCustomDotCliIgnore(rootDir, "*.txt\n");

        // Subdirectory re-includes specific .txt file
        createCustomDotCliIgnore(subDir, "!important.txt\n");

        // Act
        DotCliIgnore dotCliIgnore = DotCliIgnore.create(subDir, rootDir);

        // Assert - negation in child should override parent ignore
        assertTrue(dotCliIgnore.shouldIgnore(subDir.resolve("readme.txt")),
                  "Should ignore regular .txt files");
        assertFalse(dotCliIgnore.shouldIgnore(subDir.resolve("important.txt")),
                   "Should not ignore important.txt due to child negation");
    }

    @Test
    void testNoPatternsInHierarchy() throws IOException {
        // Arrange - no .dotcliignore files in hierarchy
        Path rootDir = tempDir;
        Path subDir = rootDir.resolve("project").resolve("src");
        Files.createDirectories(subDir);

        // Act
        DotCliIgnore dotCliIgnore = DotCliIgnore.create(subDir, rootDir);

        // Assert - should have no patterns when no .dotcliignore files exist
        assertEquals(0, dotCliIgnore.getPatternCount(), "Should have no patterns");
        assertFalse(dotCliIgnore.shouldIgnore(subDir.resolve(".git").resolve("config")));
        assertFalse(dotCliIgnore.shouldIgnore(subDir.resolve("test.log")));
    }

    @Test
    void testTrailingSpaceTrimming() throws IOException {
        // Arrange
        // Pattern with trailing spaces (should be trimmed)
        // Pattern with escaped trailing space (should be preserved)
        createCustomDotCliIgnore(tempDir,
                "*.log   \n" +  // Trailing spaces should be removed
                "*.tmp\\ \n" +  // Escaped trailing space should be preserved
                "test.txt    \n" +  // Multiple trailing spaces should be removed
                "   *.bak\n"  // Leading spaces should be removed
        );

        // Act
        DotCliIgnore dotCliIgnore = DotCliIgnore.create(tempDir);

        // Assert
        // Patterns with trimmed trailing spaces should still match
        assertTrue(dotCliIgnore.shouldIgnore(tempDir.resolve("error.log")),
                  "Pattern with trailing spaces should work after trimming");
        assertTrue(dotCliIgnore.shouldIgnore(tempDir.resolve("test.txt")),
                  "Pattern with multiple trailing spaces should work after trimming");

        // Pattern with escaped space should preserve the space
        assertTrue(dotCliIgnore.shouldIgnore(tempDir.resolve("file.tmp ")),
                  "Pattern with escaped trailing space should match filename with trailing space");

        // Pattern with leading spaces should match after trimming
        assertTrue(dotCliIgnore.shouldIgnore(tempDir.resolve("backup.bak")),
                  "Pattern with leading spaces should work after trimming");
    }

    @Test
    void testEscapedBackslashBeforeTrailingSpace() throws IOException {
        // Arrange
        // Test escaped backslash (\\) followed by trailing space
        createCustomDotCliIgnore(tempDir,
                "pattern\\\\   \n"  // Escaped backslash, trailing spaces should be removed
        );

        // Act
        DotCliIgnore dotCliIgnore = DotCliIgnore.create(tempDir);

        // Assert
        // The pattern should have the backslash but trailing spaces removed
        assertTrue(dotCliIgnore.shouldIgnore(tempDir.resolve("pattern\\")),
                  "Pattern with escaped backslash should work");
        assertFalse(dotCliIgnore.shouldIgnore(tempDir.resolve("pattern\\ ")),
                   "Pattern should not match with trailing space when backslash is escaped");
    }

    @Test
    void testDirectorySpecificFilePatterns() throws IOException {
        // Arrange - test patterns like "src/*.log" that specify directory + filename
        createCustomDotCliIgnore(tempDir,
                "src/*.log\n" +
                "test/data/*.tmp\n" +
                "docs/**/*.draft\n"
        );

        // Act
        DotCliIgnore dotCliIgnore = DotCliIgnore.create(tempDir);

        // Assert - files in specified directories should be ignored
        assertTrue(dotCliIgnore.shouldIgnore(tempDir.resolve("src").resolve("test.log")),
                  "Should ignore .log files in src/ directory");
        assertTrue(dotCliIgnore.shouldIgnore(tempDir.resolve("src").resolve("error.log")),
                  "Should ignore .log files in src/ directory");

        assertTrue(dotCliIgnore.shouldIgnore(tempDir.resolve("test").resolve("data").resolve("temp.tmp")),
                  "Should ignore .tmp files in test/data/ directory");

        // Pattern with ** in the middle matches files in subdirectories
        // Note: docs/**/*.draft won't match docs/file.draft (no subdir), but will match docs/sub/file.draft
        assertTrue(dotCliIgnore.shouldIgnore(tempDir.resolve("docs").resolve("sub").resolve("file.draft")),
                  "Should ignore .draft files in docs/ subdirectories");
        assertTrue(dotCliIgnore.shouldIgnore(tempDir.resolve("docs").resolve("a").resolve("b").resolve("file.draft")),
                  "Should ignore .draft files in deeply nested docs/ subdirectories");

        // Files in other directories should NOT be ignored
        assertFalse(dotCliIgnore.shouldIgnore(tempDir.resolve("lib").resolve("test.log")),
                   "Should not ignore .log files in other directories");
        assertFalse(dotCliIgnore.shouldIgnore(tempDir.resolve("test.log")),
                   "Should not ignore .log files at root level");

        // Nested src/ directories should also match (due to **/src/*.log pattern)
        assertTrue(dotCliIgnore.shouldIgnore(tempDir.resolve("project").resolve("src").resolve("debug.log")),
                  "Should ignore .log files in nested src/ directories");
    }
}
