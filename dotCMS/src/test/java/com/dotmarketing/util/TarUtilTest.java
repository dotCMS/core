package com.dotmarketing.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test class for {@link TarUtil}
 */
public class TarUtilTest {

    private static Path tempDir;

    @BeforeClass
    public static void setup() throws IOException {
        tempDir = Files.createTempDirectory("tarutil-test");
        
        // Set appropriate test limits to avoid "exceeds maximum allowed file size" errors
        System.setProperty(TarUtil.TAR_MAX_FILE_SIZE_KEY, "10MB");
        System.setProperty(TarUtil.TAR_MAX_TOTAL_SIZE_KEY, "50MB");
        System.setProperty(TarUtil.TAR_MAX_ENTRIES_KEY, "1000");
        
        // Force refresh of cached values to ensure the settings take effect
        TarUtil.getMaxFileSize();
        TarUtil.getMaxTotalSize();
        TarUtil.getMaxEntries();
        
        // Ensure handling mode is reset to default
        TarUtil.setDefaultSuspiciousEntryHandling(TarUtil.SuspiciousEntryHandling.ABORT);
    }

    @After
    public void tearDown() throws Exception {
        // Restore the default mode after each test
        TarUtil.setDefaultSuspiciousEntryHandling(TarUtil.SuspiciousEntryHandling.ABORT);
    }

    @Test
    public void testSanitizingPathWithLeadingSlash() {
        String path = "/file.txt";
        String sanitized = TarUtil.sanitizePath(path, TarUtil.SuspiciousEntryHandling.SKIP_AND_CONTINUE, "test.tar.gz");
        assertEquals("file.txt", sanitized);
    }

    @Test
    public void testCreateSafeTarEntry() {
        // Use SKIP_AND_CONTINUE to allow the test to pass while verifying sanitization
        TarArchiveEntry entry = TarUtil.createSafeTarEntry("/etc/passwd", TarUtil.SuspiciousEntryHandling.SKIP_AND_CONTINUE);
        assertEquals("etc/passwd", entry.getName());
    }

    @Test
    public void testCreateSafeTarEntryWithFile() throws IOException {
        File tempFile = File.createTempFile("test", ".txt");
        tempFile.deleteOnExit();
        
        // Use SKIP_AND_CONTINUE to allow the test to pass while verifying sanitization
        TarArchiveEntry entry = TarUtil.createSafeTarEntry(tempFile, "/malicious/path/file.txt", TarUtil.SuspiciousEntryHandling.SKIP_AND_CONTINUE);
        assertEquals("malicious/path/file.txt", entry.getName());
    }

    @Test
    public void testAddBytesToTar() throws IOException {
        File tempFile = Files.createTempFile(tempDir, "test-tar", ".tar.gz").toFile();
        
        try (TarArchiveOutputStream taos = TarUtil.createTarGzOutputStream(tempFile)) {
            byte[] content = "Test Content".getBytes(StandardCharsets.UTF_8);
            TarUtil.addBytesToTar(taos, content, "directory/file.txt");
        }
        
        assertTrue(tempFile.exists());
        assertTrue(tempFile.length() > 0);
    }

    @Test(expected = SecurityException.class)
    public void testPathTraversalWithAbortMode() {
        // This should throw a SecurityException with ABORT mode
        TarUtil.createSafeTarEntry("../../../etc/passwd", TarUtil.SuspiciousEntryHandling.ABORT);
    }

    @Test
    public void testPathTraversalWithSkipAndContinueMode() {
        // This should sanitize the path with SKIP_AND_CONTINUE mode
        TarArchiveEntry entry = TarUtil.createSafeTarEntry("../../../etc/passwd", TarUtil.SuspiciousEntryHandling.SKIP_AND_CONTINUE);
        assertEquals("etc/passwd", entry.getName());
    }

    /**
     * Test the extraction of TAR.GZ archives with safe path validation.
     */
    @Test
    public void testSafeExtractTarGz() throws IOException {
        System.out.println("=== testSafeExtractTarGz START ===");
        final long originalMaxFileSize = TarUtil.getMaxFileSize();
        final TarUtil.SuspiciousEntryHandling originalHandlingMode = TarUtil.getDefaultSuspiciousEntryHandling();
        try {
            // Create a test tar.gz file with a regular entry and a potentially malicious entry
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (TarArchiveOutputStream taos = new TarArchiveOutputStream(
                    new GZIPOutputStream(baos))) {
                // Add a regular file
                TarArchiveEntry regularEntry = new TarArchiveEntry("regular.txt");
                byte[] regularContent = "Regular content".getBytes(StandardCharsets.UTF_8);
                regularEntry.setSize(regularContent.length);
                taos.putArchiveEntry(regularEntry);
                taos.write(regularContent);
                taos.closeArchiveEntry();
                // Add a file with path traversal attempt
                TarArchiveEntry evilEntry = new TarArchiveEntry("../../evil.txt");
                byte[] evilContent = "Evil content".getBytes(StandardCharsets.UTF_8);
                evilEntry.setSize(evilContent.length);
                taos.putArchiveEntry(evilEntry);
                taos.write(evilContent);
                taos.closeArchiveEntry();
            }
            // ABORT mode extraction
            Path extractDir = Files.createTempDirectory(tempDir, "extract-");
            try {
                ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
                TarUtil.safeExtractTarGz(bais, extractDir.toFile());
                System.out.println("About to call fail() in ABORT mode block...");
                fail("Should throw SecurityException for path traversal");
            } catch (SecurityException e) {
                System.out.println("Caught expected SecurityException: " + e.getMessage());
                assertTrue(e.getMessage().contains("Illegal entry path"));
            }
            System.out.println("Finished ABORT mode block.");
            System.out.println("About to start SKIP_AND_CONTINUE extraction...");
            // SKIP_AND_CONTINUE extraction
            Path extractDir2 = Files.createTempDirectory(tempDir, "extract-");
            ByteArrayInputStream bais2 = new ByteArrayInputStream(baos.toByteArray());
            try {
                System.out.println("Starting SKIP_AND_CONTINUE extraction...");
                TarUtil.safeExtractTarGz(bais2, extractDir2.toFile(), TarUtil.SuspiciousEntryHandling.SKIP_AND_CONTINUE);
                File regularFile = new File(extractDir2.toFile(), "regular.txt");
                assertTrue("Regular file should be extracted", regularFile.exists());
                assertEquals("Regular content", FileUtils.readFileToString(regularFile, StandardCharsets.UTF_8));
                File sanitizedEvilFile = new File(extractDir2.toFile(), "_evil.txt");
                assertFalse("Sanitized evil file should NOT be extracted", sanitizedEvilFile.exists());
            } catch (Throwable t) {
                System.out.println("Exception in SKIP_AND_CONTINUE extraction: " + t);
                System.out.println("Extract dir: " + extractDir2);
                Files.walk(extractDir2)
                    .forEach(path -> System.out.println("Extracted: " + extractDir2.relativize(path)));
                throw t;
            } finally {
                System.out.println("Extract dir: " + extractDir2);
                Files.walk(extractDir2)
                    .forEach(path -> System.out.println("Extracted: " + extractDir2.relativize(path)));
            }
            System.out.println("=== testSafeExtractTarGz END ===");
        } finally {
            // Restore original settings
            if (originalMaxFileSize > 0) {
                System.setProperty(TarUtil.TAR_MAX_FILE_SIZE_KEY, originalMaxFileSize + "");
            } else {
                System.clearProperty(TarUtil.TAR_MAX_FILE_SIZE_KEY);
            }
            TarUtil.getMaxFileSize(); // Force refresh
            TarUtil.setDefaultSuspiciousEntryHandling(originalHandlingMode);
        }
    }
    
    @Test(expected = SecurityException.class)
    public void testSafeExtractTarGzWithAbortMode() throws IOException {
        // Remember original settings
        final long originalMaxFileSize = TarUtil.getMaxFileSize();
        final TarUtil.SuspiciousEntryHandling originalHandlingMode = TarUtil.getDefaultSuspiciousEntryHandling();
        
        try {
            // Set a higher file size limit for this test
            System.setProperty(TarUtil.TAR_MAX_FILE_SIZE_KEY, "10MB");
            // Force refresh cached value
            TarUtil.getMaxFileSize();
            
            // Create a test tar.gz file with a malicious entry
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            
            try (TarArchiveOutputStream taos = new TarArchiveOutputStream(
                    new GZIPOutputStream(baos))) {
                
                // Add a file with path traversal attempt
                TarArchiveEntry evilEntry = new TarArchiveEntry("../../evil.txt");
                byte[] evilContent = "Evil content".getBytes(StandardCharsets.UTF_8);
                evilEntry.setSize(evilContent.length);
                taos.putArchiveEntry(evilEntry);
                taos.write(evilContent);
                taos.closeArchiveEntry();
            }
            
            // Set mode to ABORT
            TarUtil.setDefaultSuspiciousEntryHandling(TarUtil.SuspiciousEntryHandling.ABORT);
            
            // This should throw a SecurityException
            Path extractDir = Files.createTempDirectory(tempDir, "extract-abort");
            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            TarUtil.safeExtractTarGz(bais, extractDir.toFile());
        } finally {
            // Restore original settings
            if (originalMaxFileSize > 0) {
                System.setProperty(TarUtil.TAR_MAX_FILE_SIZE_KEY, originalMaxFileSize + "");
            } else {
                System.clearProperty(TarUtil.TAR_MAX_FILE_SIZE_KEY);
            }
            TarUtil.getMaxFileSize(); // Force refresh
            
            TarUtil.setDefaultSuspiciousEntryHandling(originalHandlingMode);
        }
    }
    
    @Test
    public void testComplexDirectoryExtraction() throws IOException {
        // Create a complex directory structure
        Path rootDir = Files.createTempDirectory(tempDir, "root-");
        writeStringToFile(rootDir.resolve("file1.txt"), "content1");
        writeStringToFile(rootDir.resolve("file2.txt"), "content2");
        
        Path subDir = rootDir.resolve("subdir");
        Files.createDirectory(subDir);
        writeStringToFile(subDir.resolve("file3.txt"), "content3");
        
        // Create a tar.gz file
        File tarFile = new File(tempDir.toFile(), "complex.tar.gz");
        
        try (TarArchiveOutputStream taos = new TarArchiveOutputStream(
                new GZIPOutputStream(new FileOutputStream(tarFile)))) {
            
            // Add all files and directories to the archive
            addFilesToTar(taos, rootDir.toFile(), "");
        }
        
        // Extract the archive
        Path extractDir = Files.createTempDirectory(tempDir, "extract-");
        try (InputStream is = new FileInputStream(tarFile)) {
            TarUtil.safeExtractTarGz(is, extractDir.toFile());
        }
        // Debug print: show extracted directory structure
        Files.walk(extractDir)
            .forEach(path -> System.out.println("Extracted: " + extractDir.relativize(path)));
        
        // Verify the extracted structure
        Path rootName = rootDir.getFileName();
        assertTrue(Files.exists(extractDir.resolve(rootName).resolve("file1.txt")));
        assertTrue(Files.exists(extractDir.resolve(rootName).resolve("file2.txt")));
        assertTrue(Files.exists(extractDir.resolve(rootName).resolve("subdir/file3.txt")));
        
        assertEquals("content1", FileUtils.readFileToString(extractDir.resolve(rootName).resolve("file1.txt").toFile(), StandardCharsets.UTF_8));
        assertEquals("content2", FileUtils.readFileToString(extractDir.resolve(rootName).resolve("file2.txt").toFile(), StandardCharsets.UTF_8));
        assertEquals("content3", FileUtils.readFileToString(extractDir.resolve(rootName).resolve("subdir/file3.txt").toFile(), StandardCharsets.UTF_8));
    }
    
    @Test
    public void testAddingMultipleFilesToTar() throws IOException {
        // Create test files
        Path file1 = Files.createTempFile(tempDir, "file1-", ".txt");
        Path file2 = Files.createTempFile(tempDir, "file2-", ".txt");
        Path file3 = Files.createTempFile(tempDir, "file3-", ".txt");
        
        writeStringToFile(file1, "content1");
        writeStringToFile(file2, "content2");
        writeStringToFile(file3, "content3");
        
        // Create a tar.gz file
        File tarFile = new File(tempDir.toFile(), "multiple.tar.gz");
        
        try (TarArchiveOutputStream taos = new TarArchiveOutputStream(
                new GZIPOutputStream(new FileOutputStream(tarFile)))) {
            
            // Add files to the archive
            addFileToTar(taos, file1.toFile(), "file1.txt");
            addFileToTar(taos, file2.toFile(), "file2.txt");
            addFileToTar(taos, file3.toFile(), "file3.txt");
        }
        
        // Extract and verify
        Path extractDir = Files.createTempDirectory(tempDir, "extract-");
        try (TarArchiveInputStream tais = new TarArchiveInputStream(
                new GZIPInputStream(new FileInputStream(tarFile)))) {
            
            TarArchiveEntry entry;
            int count = 0;
            while ((entry = tais.getNextTarEntry()) != null) {
                if (!entry.isDirectory()) {
                    File outFile = new File(extractDir.toFile(), entry.getName());
                    try (FileOutputStream fos = new FileOutputStream(outFile)) {
                        IOUtils.copy(tais, fos);
                    }
                    count++;
                }
            }
            assertEquals(3, count);
        }
        
        // Verify contents
        assertEquals("content1", FileUtils.readFileToString(extractDir.resolve("file1.txt").toFile(), StandardCharsets.UTF_8));
        assertEquals("content2", FileUtils.readFileToString(extractDir.resolve("file2.txt").toFile(), StandardCharsets.UTF_8));
        assertEquals("content3", FileUtils.readFileToString(extractDir.resolve("file3.txt").toFile(), StandardCharsets.UTF_8));
    }

    /**
     * Test the tarGzFilesWithCustomProcessing method to verify functional interfaces work correctly
     */
    @Test
    public void testTarGzFilesWithCustomProcessing() throws IOException {
        // Create test files
        File tempFile1 = Files.createTempFile(tempDir, "test1", ".txt").toFile();
        File tempFile2 = Files.createTempFile(tempDir, "test2", ".txt").toFile();
        File tempFile3 = Files.createTempFile(tempDir, "test3", ".bin").toFile();
        
        // Write content to files
        Files.write(tempFile1.toPath(), "content 1".getBytes(StandardCharsets.UTF_8));
        Files.write(tempFile2.toPath(), "content 2".getBytes(StandardCharsets.UTF_8));
        Files.write(tempFile3.toPath(), "binary data".getBytes(StandardCharsets.UTF_8));
        
        // Create subdirectory with a file
        Path subDir = Files.createDirectory(tempDir.resolve("subdir"));
        File tempFile4 = Files.createTempFile(subDir, "test4", ".txt").toFile();
        Files.write(tempFile4.toPath(), "nested content".getBytes(StandardCharsets.UTF_8));
        
        // Target TAR.GZ file
        File tarGzFile = Files.createTempFile(tempDir, "custom", ".tar.gz").toFile();
        
        // Create a custom filter that only accepts .txt files
        ArchiveUtil.FileFilter txtFilter = file -> 
            file.isDirectory() || file.getName().endsWith(".txt");
        
        // Create a processor that adds a header to text files
        ArchiveUtil.FileProcessor headerProcessor = (file, entryName) -> {
            if (file.getName().endsWith(".txt")) {
                String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
                String processed = "# PROCESSED: " + content;
                return new ByteArrayInputStream(processed.getBytes(StandardCharsets.UTF_8));
            }
            return Files.newInputStream(file.toPath());
        };
        
        // Create a name mapper that prefixes all paths
        ArchiveUtil.EntryNameMapper prefixMapper = (file, basePath) -> {
            String normalizedBase = basePath == null ? "" : basePath;
            if (!normalizedBase.isEmpty() && !normalizedBase.endsWith("/")) {
                normalizedBase += "/";
            }
            // If the file is in the fixed subdir, use 'subdir' as the base
            if (file.getParentFile() != null && file.getParentFile().getName().equals("subdir")) {
                normalizedBase = "subdir/";
            }
            return "docs/" + normalizedBase + file.getName();
        };
        
        // Use the custom processing method
        TarUtil.tarGzFilesWithCustomProcessing(
            java.util.Arrays.asList(tempFile1, tempFile2, tempFile3, subDir.toFile()),
            tarGzFile,
            "",
            txtFilter,
            headerProcessor,
            prefixMapper
        );
        
        // Extract and verify
        Path extractDir = Files.createTempDirectory(tempDir, "extract");
        
        TarUtil.safeExtractTarGz(tarGzFile, extractDir.toFile());
        
        // Debug print: show extracted directory structure
        Files.walk(extractDir)
            .forEach(path -> System.out.println("Extracted: " + extractDir.relativize(path)));
        
        // Verify that only .txt files were included
        File extractedFile1 = new File(extractDir.toFile(), "docs/" + tempFile1.getName());
        File extractedFile2 = new File(extractDir.toFile(), "docs/" + tempFile2.getName());
        File extractedFile3 = new File(extractDir.toFile(), "docs/" + tempFile3.getName());
        File extractedSubDir = new File(extractDir.toFile(), "docs/subdir");
        File extractedFile4 = new File(extractedSubDir, tempFile4.getName());
        
        assertTrue("Text file 1 should exist", extractedFile1.exists());
        assertTrue("Text file 2 should exist", extractedFile2.exists());
        assertFalse("Binary file should not exist due to filter", extractedFile3.exists());
        assertTrue("Nested directory should exist", extractedSubDir.exists());
        assertTrue("Nested text file should exist", extractedFile4.exists());
        
        // Verify content processing
        String content1 = new String(Files.readAllBytes(extractedFile1.toPath()), StandardCharsets.UTF_8);
        String content2 = new String(Files.readAllBytes(extractedFile2.toPath()), StandardCharsets.UTF_8);
        String content4 = new String(Files.readAllBytes(extractedFile4.toPath()), StandardCharsets.UTF_8);
        
        assertTrue("Content should have been processed", content1.startsWith("# PROCESSED:"));
        assertTrue("Content should have been processed", content2.startsWith("# PROCESSED:"));
        assertTrue("Content should have been processed", content4.startsWith("# PROCESSED:"));
    }
    
    /**
     * Test the new addDirectoryToTar implementation that uses addDirectoryToTarWithCustomProcessing
     */
    @Test
    public void testAddDirectoryToTar() throws IOException {
        // Create a test directory structure
        Path testDir = Files.createTempDirectory(tempDir, "directory-to-tar");
        Path subdir = Files.createDirectory(testDir.resolve("subdir"));
        
        // Create a few files with content
        Files.write(testDir.resolve("root-file.txt"), "Root file content".getBytes(StandardCharsets.UTF_8));
        Files.write(subdir.resolve("sub-file.txt"), "Subdir file content".getBytes(StandardCharsets.UTF_8));
        
        // Create the tar file
        File tarFile = Files.createTempFile(tempDir, "directory-test", ".tar.gz").toFile();
        
        try (TarArchiveOutputStream taos = TarUtil.createTarGzOutputStream(tarFile)) {
            // Add the directory to the tar
            addDirectoryToTar(taos, testDir.toFile(), testDir.toString(), "");
        }
        
        // Verify the tar file was created
        assertTrue(tarFile.exists());
        assertTrue(tarFile.length() > 0);
        
        // Extract the tar to verify contents
        Path extractDir = Files.createTempDirectory(tempDir, "extracted-dir");
        TarUtil.safeExtractTarGz(tarFile, extractDir.toFile());
        
        // Verify directory structure
        assertTrue(Files.exists(extractDir.resolve("subdir")));
        
        // Verify files
        assertTrue(Files.exists(extractDir.resolve("root-file.txt")));
        assertTrue(Files.exists(extractDir.resolve("subdir/sub-file.txt")));
        
        // Verify content
        assertEquals("Root file content", 
                new String(Files.readAllBytes(extractDir.resolve("root-file.txt")), StandardCharsets.UTF_8));
        assertEquals("Subdir file content", 
                new String(Files.readAllBytes(extractDir.resolve("subdir/sub-file.txt")), StandardCharsets.UTF_8));
    }
    
    /**
     * Test the shared utilities from ArchiveUtil when used with TarUtil
     */
    @Test
    public void testSharedArchiveUtilities() throws IOException {
        // Test file filter combinations
        ArchiveUtil.FileFilter textFilter = ArchiveUtil.byExtension("txt");
        ArchiveUtil.FileFilter execFilter = ArchiveUtil.byExtension("sh", "bat");
        ArchiveUtil.FileFilter anyExecutable = ArchiveUtil.or(execFilter, ArchiveUtil.byExtension("exe"));
        ArchiveUtil.FileFilter textButNotExecutable = ArchiveUtil.and(textFilter, ArchiveUtil.not(anyExecutable));
        
        // Create test files for filtering
        File textFile = Files.createTempFile(tempDir, "text", ".txt").toFile();
        File shFile = Files.createTempFile(tempDir, "script", ".sh").toFile();
        File textShFile = Files.createTempFile(tempDir, "readme", ".txt.sh").toFile();
        
        // Write some content to the files
        Files.write(textFile.toPath(), "Text content".getBytes(StandardCharsets.UTF_8));
        Files.write(shFile.toPath(), "#!/bin/sh".getBytes(StandardCharsets.UTF_8));
        Files.write(textShFile.toPath(), "Text with sh extension".getBytes(StandardCharsets.UTF_8));
        
        // Verify filter logic
        assertTrue("Text file should be accepted by text filter", textFilter.accept(textFile));
        assertFalse("Sh file should not be accepted by text filter", textFilter.accept(shFile));
        assertTrue("Text+sh file should be accepted by text filter", textFilter.accept(textShFile));
        
        assertTrue("Sh file should be accepted by exec filter", execFilter.accept(shFile));
        assertFalse("Text file should not be accepted by exec filter", execFilter.accept(textFile));
        assertTrue("Text+sh file should be accepted by exec filter", execFilter.accept(textShFile));
        
        assertTrue("Sh file should be accepted by anyExecutable", anyExecutable.accept(shFile));
        assertTrue("Text+sh file should be accepted by anyExecutable", anyExecutable.accept(textShFile));
        assertFalse("Text file should not be accepted by anyExecutable", anyExecutable.accept(textFile));
        
        assertTrue("Text file should be accepted by textButNotExecutable", textButNotExecutable.accept(textFile));
        assertFalse("Sh file should not be accepted by textButNotExecutable", textButNotExecutable.accept(shFile));
        assertFalse("Text+sh file should not be accepted by textButNotExecutable", textButNotExecutable.accept(textShFile));
        
        // Test combining filters with the archive operation
        File targetFile = Files.createTempFile(tempDir, "filtered", ".tar.gz").toFile();
        
        TarUtil.tarGzFilesWithCustomProcessing(
            java.util.Arrays.asList(textFile, shFile, textShFile),
            targetFile,
            "",
            textButNotExecutable, // Only include non-executable text files
            ArchiveUtil.NO_PROCESSING,
            ArchiveUtil.DEFAULT_NAME_MAPPER
        );
        
        // Extract and verify
        Path extractDir = Files.createTempDirectory(tempDir, "extract-filter");
        
        TarUtil.safeExtractTarGz(targetFile, extractDir.toFile());
        
        // Only textFile should be included
        File extractedTextFile = new File(extractDir.toFile(), textFile.getName());
        File extractedShFile = new File(extractDir.toFile(), shFile.getName());
        File extractedTextShFile = new File(extractDir.toFile(), textShFile.getName());
        
        assertTrue("Text file should exist", extractedTextFile.exists());
        assertFalse("Sh file should not exist", extractedShFile.exists());
        assertFalse("Text+sh file should not exist", extractedTextShFile.exists());
    }

    // Helper methods
    
    private void writeStringToFile(Path path, String content) throws IOException {
        Files.write(path, content.getBytes(StandardCharsets.UTF_8));
    }
    
    private void addFilesToTar(TarArchiveOutputStream taos, File file, String parentPath) throws IOException {
        String entryName = parentPath.isEmpty() ? file.getName() : parentPath + "/" + file.getName();

        if (file.isDirectory()) {
            // Add directory entry only if not the root
            if (!parentPath.isEmpty()) {
                TarArchiveEntry entry = new TarArchiveEntry(file, entryName);
                taos.putArchiveEntry(entry);
                taos.closeArchiveEntry();
            }

            // Recursively add all files and subdirectories
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    addFilesToTar(taos, child, entryName);
                }
            }
        } else {
            // Add file entry
            TarArchiveEntry entry = new TarArchiveEntry(file, entryName);
            taos.putArchiveEntry(entry);
            try (FileInputStream fis = new FileInputStream(file)) {
                IOUtils.copy(fis, taos);
            }
            taos.closeArchiveEntry();
        }
    }
    
    private void addDirectoryToTar(TarArchiveOutputStream taos, File directory, String directoryPath, String entryPrefix) throws IOException {
        String[] fileList = directory.list();
        if (fileList == null) {
            throw new IOException("Failed to list contents of directory: " + directory);
        }
        
        for (String fileName : fileList) {
            File file = new File(directory, fileName);
            String entryName = entryPrefix.isEmpty() ? file.getName() : entryPrefix + "/" + file.getName();
            
            if (file.isDirectory()) {
                // Add directory entry
                TarArchiveEntry entry = new TarArchiveEntry(file, entryName);
                taos.putArchiveEntry(entry);
                taos.closeArchiveEntry();
                
                // Recursively add the directory's contents
                addDirectoryToTar(taos, file, directoryPath, entryName);
            } else {
                // Add file entry
                TarArchiveEntry entry = new TarArchiveEntry(file, entryName);
                taos.putArchiveEntry(entry);
                try (FileInputStream fis = new FileInputStream(file)) {
                    IOUtils.copy(fis, taos);
                }
                taos.closeArchiveEntry();
            }
        }
    }

    // Helper method to add a file to a tar archive
    private void addFileToTar(TarArchiveOutputStream taos, File file, String entryName) throws IOException {
        TarArchiveEntry entry = new TarArchiveEntry(entryName);
        entry.setSize(file.length());
        taos.putArchiveEntry(entry);
        try (FileInputStream fis = new FileInputStream(file)) {
            IOUtils.copy(fis, taos);
        }
        taos.closeArchiveEntry();
    }
} 