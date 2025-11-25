package com.dotcms.cli.common;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class FilesUtilsTest {

    @Test
    void cleanFileName() {
        String doubleQuotesFileName = "IMG_\" 550D397\" 2B98B-1.jpeg";
        String cleaned = FilesUtils.cleanFileName(doubleQuotesFileName);
        assertNotEquals(doubleQuotesFileName, cleaned);

        String singleQuotesFileName = "IMG_550D397 '2B98B-1.jpeg";
        cleaned = FilesUtils.cleanFileName(singleQuotesFileName);
        assertEquals(singleQuotesFileName, cleaned);
    }

    @Test
    void testCleanFileName() {
        // Arrange
        String badFileName = "file<>name|with*illegal?characters.txt";
        String expectedCleanFileName = "filenamewithillegalcharacters.txt";

        // Act
        String cleanedFileName = FilesUtils.cleanFileName(badFileName);

        // Assert
        assertEquals(expectedCleanFileName, cleanedFileName);
    }

    @Test
    void testCleanFileName_NoIllegalCharacters() {
        // Arrange
        String goodFileName = "goodfilename.txt";

        // Act
        String cleanedFileName = FilesUtils.cleanFileName(goodFileName);

        // Assert
        assertEquals(goodFileName, cleanedFileName);
    }

    @Test
    void testCleanFileName_EmptyFileName() {
        // Arrange
        String emptyFileName = "";

        // Act
        String cleanedFileName = FilesUtils.cleanFileName(emptyFileName);

        // Assert
        assertEquals(emptyFileName, cleanedFileName);
    }

}