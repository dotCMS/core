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
