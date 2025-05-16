package com.dotmarketing.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ArchiveUtilTest {

    private File tempDir;
    
    @Before
    public void setup() throws IOException {
        tempDir = com.google.common.io.Files.createTempDir();
    }
    
    @After
    public void cleanup() throws IOException {
        if (tempDir != null && tempDir.exists()) {
            FileUtils.deleteDirectory(tempDir);
        }
    }
    
    @Test
    public void testSanitizePath() {
        // Test valid paths (shouldn't change)
        assertEquals("path/to/file.txt", 
            ArchiveUtil.sanitizePath("path/to/file.txt", ArchiveUtil.SuspiciousEntryHandling.ABORT));
        
        // Test with leading slash (should be removed)
        assertEquals("path/to/file.txt", 
            ArchiveUtil.sanitizePath("/path/to/file.txt", ArchiveUtil.SuspiciousEntryHandling.SKIP_AND_CONTINUE));
        
        // Test with parent directory references
        assertEquals("etc/passwd", 
            ArchiveUtil.sanitizePath("../../../etc/passwd", ArchiveUtil.SuspiciousEntryHandling.SKIP_AND_CONTINUE));
        
        // Test with ABORT mode (should throw Exception)
        try {
            ArchiveUtil.sanitizePath("../../../etc/passwd", ArchiveUtil.SuspiciousEntryHandling.ABORT);
            fail("Expected SecurityException");
        } catch (SecurityException e) {
            // Expected
        }
        
        // Test complex path with mixed directory references
        assertEquals("unsafe/safe/file.txt", 
            ArchiveUtil.sanitizePath("unsafe/../safe/./file.txt", ArchiveUtil.SuspiciousEntryHandling.SKIP_AND_CONTINUE));
    }
    
    @Test
    public void testCheckSecurity() throws IOException {
        // Create test files
        File parent = new File(tempDir, "extracted");
        parent.mkdir();
        
        File safeFile = new File(parent, "safe.txt");
        File subDir = new File(parent, "subdir");
        File safeFileInSubDir = new File(subDir, "safe.txt");
        File unsafeFile = new File(tempDir, "unsafe.txt"); // Outside the parent directory
        
        // Test safe files
        assertTrue(ArchiveUtil.checkSecurity(parent, safeFile, ArchiveUtil.SuspiciousEntryHandling.ABORT));
        assertTrue(ArchiveUtil.checkSecurity(parent, subDir, ArchiveUtil.SuspiciousEntryHandling.ABORT));
        assertTrue(ArchiveUtil.checkSecurity(parent, safeFileInSubDir, ArchiveUtil.SuspiciousEntryHandling.ABORT));
        
        // Test unsafe file with ABORT mode
        try {
            ArchiveUtil.checkSecurity(parent, unsafeFile, ArchiveUtil.SuspiciousEntryHandling.ABORT);
            fail("Expected SecurityException");
        } catch (SecurityException e) {
            // Expected
        }
        
        // Test unsafe file with SKIP_AND_CONTINUE mode
        assertFalse(ArchiveUtil.checkSecurity(parent, unsafeFile, ArchiveUtil.SuspiciousEntryHandling.SKIP_AND_CONTINUE));
    }
    
    @Test
    public void testCheckEntrySizeLimit() {
        // Test file within size limit
        assertTrue(ArchiveUtil.checkEntrySizeLimit(
            "small.txt", 1000, 10000, ArchiveUtil.SuspiciousEntryHandling.ABORT));
        
        // Test file exceeding size limit with SKIP_AND_CONTINUE
        assertFalse(ArchiveUtil.checkEntrySizeLimit(
            "large.txt", 20000, 10000, ArchiveUtil.SuspiciousEntryHandling.SKIP_AND_CONTINUE));
        
        // Test file exceeding size limit with ABORT
        try {
            ArchiveUtil.checkEntrySizeLimit(
                "large.txt", 20000, 10000, ArchiveUtil.SuspiciousEntryHandling.ABORT);
            fail("Expected SecurityException");
        } catch (SecurityException e) {
            // Expected
        }
    }
    
    @Test
    public void testCheckTotalSizeLimit() {
        AtomicLong counter = new AtomicLong(5000);
        
        // Test file that would fit under total size limit
        assertTrue(ArchiveUtil.checkTotalSizeLimit(
            "file1.txt", 3000, counter, 10000, ArchiveUtil.SuspiciousEntryHandling.ABORT));
        
        // Test file that would exceed total size limit with SKIP_AND_CONTINUE
        assertFalse(ArchiveUtil.checkTotalSizeLimit(
            "file2.txt", 6000, counter, 10000, ArchiveUtil.SuspiciousEntryHandling.SKIP_AND_CONTINUE));
        
        // Test file that would exceed total size limit with ABORT
        try {
            ArchiveUtil.checkTotalSizeLimit(
                "file2.txt", 6000, counter, 10000, ArchiveUtil.SuspiciousEntryHandling.ABORT);
            fail("Expected SecurityException");
        } catch (SecurityException e) {
            // Expected
        }
    }
    
    @Test
    public void testCheckEntryCountLimit() {
        // Test count within limit
        assertTrue(ArchiveUtil.checkEntryCountLimit(50, 100, ArchiveUtil.SuspiciousEntryHandling.ABORT));
        
        // Test count exceeding limit with SKIP_AND_CONTINUE
        assertFalse(ArchiveUtil.checkEntryCountLimit(150, 100, ArchiveUtil.SuspiciousEntryHandling.SKIP_AND_CONTINUE));
        
        // Test count exceeding limit with ABORT
        try {
            ArchiveUtil.checkEntryCountLimit(150, 100, ArchiveUtil.SuspiciousEntryHandling.ABORT);
            fail("Expected SecurityException");
        } catch (SecurityException e) {
            // Expected
        }
    }
    
    @Test
    public void testIsNewFileDestinationSafe() throws IOException {
        // Create test files
        File parent = new File(tempDir, "parent");
        parent.mkdir();
        
        File safeFile = new File(parent, "safe.txt");
        File safeSubDir = new File(parent, "subdir");
        File safeFileInSubDir = new File(safeSubDir, "safe.txt");
        
        // Create a file outside the parent directory
        File outsideDir = new File(tempDir, "outside");
        outsideDir.mkdir();
        File unsafeFile = new File(outsideDir, "unsafe.txt");
        
        // Test safe paths
        assertTrue(ArchiveUtil.isNewFileDestinationSafe(parent, safeFile));
        assertTrue(ArchiveUtil.isNewFileDestinationSafe(parent, safeSubDir));
        assertTrue(ArchiveUtil.isNewFileDestinationSafe(parent, safeFileInSubDir));
        
        // Test unsafe path
        assertFalse(ArchiveUtil.isNewFileDestinationSafe(parent, unsafeFile));
        
        // Test with attempt to traverse upward
        File traversalAttempt = new File(parent, "../traversal.txt");
        assertFalse(ArchiveUtil.isNewFileDestinationSafe(parent, traversalAttempt));
    }
} 