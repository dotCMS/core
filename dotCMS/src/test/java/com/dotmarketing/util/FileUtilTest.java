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
     * Test to verify that the isValidFilePath method correctly validates file paths.
     * Tests various valid and invalid path patterns for security and format compliance.
     */
    @Test
    public void test_isValidFilePath() throws Exception {

        // Test valid file paths
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

        // Test invalid file paths - directory traversal
        String[] invalidTraversalPaths = {
            "../../../etc/passwd",
            "folder/../../../attack",
            "normal/path/../../../evil",
            "..\\windows\\system32",
            "folder\\..\\..\\attack"
        };

        for (String invalidPath : invalidTraversalPaths) {
            assertFalse("Should reject directory traversal: " + invalidPath,
                FileUtil.isValidFilePath(invalidPath));
        }

        // Test invalid file paths - absolute paths
        String[] invalidAbsolutePaths = {
            "/etc/passwd",
            "/var/log/system.log",
            "\\Windows\\System32\\file.exe",
            "C:\\Users\\file.txt",
            "D:/Program Files/app.exe",
            "/usr/bin/malicious"
        };

        for (String invalidPath : invalidAbsolutePaths) {
            assertFalse("Should reject absolute path: " + invalidPath,
                FileUtil.isValidFilePath(invalidPath));
        }

        // Test invalid file paths - double slashes
        String[] invalidDoubleSlashPaths = {
            "folder//file.txt",
            "path/folder//nested/file.log",
            "data\\\\file.txt",
            "folder\\\\subfolder\\file.exe"
        };

        for (String invalidPath : invalidDoubleSlashPaths) {
            assertFalse("Should reject double slashes: " + invalidPath,
                FileUtil.isValidFilePath(invalidPath));
        }

        // Test invalid file paths - invalid characters
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
            "file>arrow.txt"
        };

        for (String invalidPath : invalidCharacterPaths) {
            assertFalse("Should reject invalid characters: " + invalidPath,
                FileUtil.isValidFilePath(invalidPath));
        }

        // Test invalid file paths - excessive depth
        String excessiveDepthPath = "a/b/c/d/e/f/g/h/i/j/k/l/m/file.txt"; // More than 10 levels
        assertFalse("Should reject excessive path depth",
            FileUtil.isValidFilePath(excessiveDepthPath));

        // Test edge cases - null and empty
        assertFalse("Should reject null path", FileUtil.isValidFilePath(null));
        assertFalse("Should reject empty path", FileUtil.isValidFilePath(""));
        assertFalse("Should reject blank path", FileUtil.isValidFilePath("   "));

        // Test valid edge cases - exactly 10 levels (boundary test)
        String maxDepthPath = "a/b/c/d/e/f/g/h/i/j.txt"; // Exactly 10 levels
        assertTrue("Should accept maximum allowed depth",
            FileUtil.isValidFilePath(maxDepthPath));

        // Test valid characters - comprehensive character set
        assertTrue("Should accept underscores", FileUtil.isValidFilePath("file_name.txt"));
        assertTrue("Should accept hyphens", FileUtil.isValidFilePath("file-name.txt"));
        assertTrue("Should accept numbers", FileUtil.isValidFilePath("file123.txt"));
        assertTrue("Should accept dots in filename", FileUtil.isValidFilePath("file.backup.txt"));
        assertTrue("Should accept mixed case", FileUtil.isValidFilePath("FileNAME.TXT"));
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
