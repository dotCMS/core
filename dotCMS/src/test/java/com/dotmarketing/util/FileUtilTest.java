package com.dotmarketing.util;

import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.Test;

public class FileUtilTest {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {}

    final String[] badFileNames = {
            "../../../../../../../../../Users/will/test/our-test.jsp", 
            ".\\.\\test.jsp\\]]\\"};

    final String[] goodFileNames = {
            "our-test.jsp",
            "Here is - a strange File4_ _name.mpg"};



    @Test
    public void test_file_name_sanitizer() throws Exception {

        // test bad fileNames
        for (final String badFileName : badFileNames) {

            final String newFileName = FileUtil.sanitizeFileName(badFileName);
            assertNotEquals(badFileName, newFileName);


        }

        // test good fileNames
        for (final String goodFileName : goodFileNames) {

            final String newFileName = FileUtil.sanitizeFileName(goodFileName);
            assertEquals(goodFileName, newFileName);


        }

    }

    /**
     * Test to verify that isValidFilePath accepts valid file paths.
     * Tests various valid path patterns including single files, nested directories,
     * and acceptable characters.
     */
    @Test
    public void test_isValidFilePath_acceptsValidPaths() throws Exception {
        String[] validPaths = {
            "file.log",
            "app.txt",
            "document.pdf",
            "logs/file.log",
            "app-logs/server.log",
            "data/subfolder/file.txt",
            "reports/2024/january/report_01.csv",
            "app-data/config_file.properties",
            "temp/cache/session_123.dat",
            "uploads/images/photo.jpg"
        };

        for (String validPath : validPaths) {
            assertTrue("Should accept valid path: " + validPath,
                FileUtil.isValidFilePath(validPath));
        }
    }

    /**
     * Test to verify that isValidFilePath rejects directory traversal attacks.
     * Tests various patterns that attempt to escape the allowed directory structure.
     */
    @Test
    public void test_isValidFilePath_rejectsDirectoryTraversal() throws Exception {
        String[] invalidTraversalPaths = {
            "../../../etc/passwd",
            "folder/../../../attack",
            "normal/path/../../../evil",
            "..\\windows\\system32",
            "folder\\..\\..\\attack",
            "../../../../sensitive/file.txt",
            "sub/../../outside.txt"
        };

        for (String invalidPath : invalidTraversalPaths) {
            assertFalse("Should reject directory traversal: " + invalidPath,
                FileUtil.isValidFilePath(invalidPath));
        }
    }

    /**
     * Test to verify that isValidFilePath rejects absolute paths.
     * Tests various absolute path patterns on different operating systems.
     */
    @Test
    public void test_isValidFilePath_rejectsAbsolutePaths() throws Exception {
        String[] invalidAbsolutePaths = {
            "/etc/passwd",
            "/var/log/system.log",
            "\\Windows\\System32\\file.exe",
            "C:\\Users\\file.txt",
            "D:/Program Files/app.exe",
            "/usr/bin/malicious",
            "/home/user/sensitive.txt",
            "\\\\server\\share\\file.txt"
        };

        for (String invalidPath : invalidAbsolutePaths) {
            assertFalse("Should reject absolute path: " + invalidPath,
                FileUtil.isValidFilePath(invalidPath));
        }
    }

    /**
     * Test to verify that isValidFilePath rejects paths with double slashes.
     * Tests various double slash patterns that could be used to bypass security.
     */
    @Test
    public void test_isValidFilePath_rejectsDoubleSlashes() throws Exception {
        String[] invalidDoubleSlashPaths = {
            "folder//file.txt",
            "path/folder//nested/file.log",
            "data\\\\file.txt",
            "folder\\\\subfolder\\file.exe",
            "normal//double//slash.txt",
            "mixed/\\backforward.txt"
        };

        for (String invalidPath : invalidDoubleSlashPaths) {
            assertFalse("Should reject double slashes: " + invalidPath,
                FileUtil.isValidFilePath(invalidPath));
        }
    }

    /**
     * Test to verify that isValidFilePath rejects paths with invalid characters.
     * Tests various special characters that should not be allowed in file paths.
     */
    @Test
    public void test_isValidFilePath_rejectsInvalidCharacters() throws Exception {
        String[] invalidCharacterPaths = {
            "file with spaces.txt",
            "file@name.log",
            "file#hash.txt",
            "file&name.log",
            "file*wildcard.txt",
            "file?query.log",
            "file[bracket].txt",
            "file{brace}.log",
            "file|pipe.txt",
            "file<angle>.log",
            "file>arrow.txt",
            "file\"quote.txt",
            "file'apostrophe.txt"
        };

        for (String invalidPath : invalidCharacterPaths) {
            assertFalse("Should reject invalid characters: " + invalidPath,
                FileUtil.isValidFilePath(invalidPath));
        }
    }

    /**
     * Test to verify that isValidFilePath enforces path depth limits.
     * Tests paths that exceed the maximum allowed directory nesting levels.
     */
    @Test
    public void test_isValidFilePath_rejectsExcessiveDepth() throws Exception {
        // Test excessive depth - more than allowed levels
        String excessiveDepthPath = "a/b/c/d/e/f/g/h/i/j/k/l/m/file.txt"; // More than 10 levels
        assertFalse("Should reject excessive path depth",
            FileUtil.isValidFilePath(excessiveDepthPath));

        // Test maximum allowed depth (boundary test)
        String maxDepthPath = "a/b/c/d/e/f/g/h/i/j.txt"; // Exactly 10 levels
        assertTrue("Should accept maximum allowed depth",
            FileUtil.isValidFilePath(maxDepthPath));
    }

    /**
     * Test to verify that isValidFilePath handles edge cases properly.
     * Tests null, empty, and whitespace-only inputs.
     */
    @Test
    public void test_isValidFilePath_handlesEdgeCases() throws Exception {
        // Test edge cases - null and empty
        assertFalse("Should reject null path", FileUtil.isValidFilePath(null));
        assertFalse("Should reject empty path", FileUtil.isValidFilePath(""));
        assertFalse("Should reject blank path", FileUtil.isValidFilePath("   "));
        assertFalse("Should reject tab-only path", FileUtil.isValidFilePath("\t"));
        assertFalse("Should reject newline path", FileUtil.isValidFilePath("\n"));
    }

    /**
     * Test to verify that isValidFilePath accepts valid character sets.
     * Tests various valid characters and combinations that should be allowed.
     */
    @Test
    public void test_isValidFilePath_acceptsValidCharacters() throws Exception {
        // Test valid characters - comprehensive character set
        assertTrue("Should accept underscores", FileUtil.isValidFilePath("file_name.txt"));
        assertTrue("Should accept hyphens", FileUtil.isValidFilePath("file-name.txt"));
        assertTrue("Should accept numbers", FileUtil.isValidFilePath("file123.txt"));
        assertTrue("Should accept dots in filename", FileUtil.isValidFilePath("file.backup.txt"));
        assertTrue("Should accept mixed case", FileUtil.isValidFilePath("FileNAME.TXT"));
        assertTrue("Should accept alphanumeric", FileUtil.isValidFilePath("abc123XYZ.txt"));
        assertTrue("Should accept extension variations", FileUtil.isValidFilePath("file.log.old"));
    }

    /**
     * Test to verify that sanitizeFilePath properly sanitizes simple file names.
     * Tests single file names without directory components.
     */
    @Test
    public void test_sanitizeFilePath_simpleFileNames() throws Exception {
        // Test simple file names - should use sanitizeFileName for single files
        assertEquals("Should sanitize simple filename",
            "test.txt", FileUtil.sanitizeFilePath("test.txt"));

        assertEquals("Should sanitize filename with numbers",
            "file123.log", FileUtil.sanitizeFilePath("file123.log"));

        assertEquals("Should sanitize filename with underscores",
            "my_file.txt", FileUtil.sanitizeFilePath("my_file.txt"));

        // Test that dangerous characters in filename are removed
        assertNotEquals("Should sanitize filename with spaces",
            "file with spaces.txt", FileUtil.sanitizeFilePath("file with spaces.txt"));
    }

    /**
     * Test to verify that sanitizeFilePath properly sanitizes file paths with directories.
     * Tests paths with multiple directory components where each part is sanitized individually.
     */
    @Test
    public void test_sanitizeFilePath_withDirectories() throws Exception {
        // Test nested directory paths
        String result = FileUtil.sanitizeFilePath("logs/app/error.log");
        assertEquals("Should preserve valid directory structure",
            "logs" + java.io.File.separator + "app" + java.io.File.separator + "error.log", result);

        // Test deeper nesting
        result = FileUtil.sanitizeFilePath("data/2024/january/report.csv");
        assertEquals("Should handle deep directory nesting",
            "data" + java.io.File.separator + "2024" + java.io.File.separator +
            "january" + java.io.File.separator + "report.csv", result);
    }

    /**
     * Test to verify that sanitizeFilePath sanitizes each path component individually.
     * Tests that malicious content in directory names is properly cleaned.
     */
    @Test
    public void test_sanitizeFilePath_sanitizesEachComponent() throws Exception {
        // Test that each directory component gets sanitized individually
        String input = "normal/bad component/file.txt";
        String result = FileUtil.sanitizeFilePath(input);

        // The result should not contain the original "bad component" with spaces
        assertFalse("Should sanitize directory components with spaces",
            result.contains("bad component"));
        assertTrue("Should still contain sanitized components",
            result.contains("normal") && result.contains("file.txt"));
    }

    /**
     * Test to verify that sanitizeFilePath handles edge cases properly.
     * Tests null, empty, and problematic inputs.
     */
    @Test
    public void test_sanitizeFilePath_handlesEdgeCases() throws Exception {
        // Test empty and null cases
        assertEquals("Should handle empty string", "", FileUtil.sanitizeFilePath(""));

        // Test single character filename
        assertEquals("Should handle single character", "a", FileUtil.sanitizeFilePath("a"));

        // Test filename with only extension
        assertEquals("Should handle extension-only file", ".txt", FileUtil.sanitizeFilePath(".txt"));
    }

    /**
     * Test to verify that sanitizeFilePath maintains proper path separators.
     * Tests that the method uses the correct file separator for the current OS.
     */
    @Test
    public void test_sanitizeFilePath_maintainsProperSeparators() throws Exception {
        String input = "folder1/folder2/file.txt";
        String result = FileUtil.sanitizeFilePath(input);

        // Should use the system's file separator
        String expected = "folder1" + java.io.File.separator + "folder2" + java.io.File.separator + "file.txt";
        assertEquals("Should use correct file separators", expected, result);

        // Should not contain forward slashes on Windows or backslashes on Unix in wrong places
        if (java.io.File.separator.equals("\\")) {
            assertFalse("Should not contain forward slashes on Windows", result.contains("/"));
        } else {
            assertFalse("Should not contain backslashes on Unix", result.contains("\\"));
        }
    }

    /**
     * Test to verify that sanitizeFilePath works with various file extensions.
     * Tests different file types and extension patterns.
     */
    @Test
    public void test_sanitizeFilePath_variousFileExtensions() throws Exception {
        // Test various file extensions
        assertEquals("Should handle log files",
            "application.log", FileUtil.sanitizeFilePath("application.log"));
        assertEquals("Should handle json files",
            "config.json", FileUtil.sanitizeFilePath("config.json"));
        assertEquals("Should handle multiple dots",
            "backup.tar.gz", FileUtil.sanitizeFilePath("backup.tar.gz"));
        assertEquals("Should handle no extension",
            "README", FileUtil.sanitizeFilePath("README"));
    }

    /**
     * Test to verify that the isFileEditableAsText method correctly identifies
     * various MIME types as editable text files.
     */
    @Test
    public void test_isFileEditableAsText() throws Exception {
        //List of possible mime types that should be editable as text
        String[] editableMimeTypes = {
                // Scripts and source code
                "application/javascript",
                "application/ecmascript",
                "application/x-typescript",
                "application/x-sh",              // Shell script
                "application/x-httpd-php",       // PHP scripts
                "application/x-latex",           // LaTeX documents

                // Structured data formats
                "application/json",
                "application/xml",
                "application/x-yaml",
                "application/toml",
                "application/x-toml",
                "application/x-www-form-urlencoded",
                "application/x-sql",

                // React/TSX extensions
                "application/jsx",
                "application/tsx"
        };

        for (final String mimeType : editableMimeTypes) {
            final boolean isEditable = FileUtil.isFileEditableAsText(mimeType);
            assertTrue("MIME type " + mimeType + " should be editable as text", isEditable);
        }
    }


}
