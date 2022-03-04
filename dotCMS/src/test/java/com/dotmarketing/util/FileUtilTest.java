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


}