package com.dotmarketing.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Test class for {@link ArchiveUtil}
 */
public class ArchiveUtilTest {

    private Path tempDir;
    
    @Before
    public void setup() throws IOException {
        tempDir = Files.createTempDirectory("archiveutil-test");
        
        // Set appropriate test limits to avoid "exceeds maximum allowed file size" errors
        System.setProperty(ZipUtil.ZIP_MAX_FILE_SIZE_KEY, "10MB");
        System.setProperty(ZipUtil.ZIP_MAX_TOTAL_SIZE_KEY, "50MB");
        System.setProperty(ZipUtil.ZIP_MAX_ENTRIES_KEY, "1000");
        System.setProperty(TarUtil.TAR_MAX_FILE_SIZE_KEY, "10MB");
        System.setProperty(TarUtil.TAR_MAX_TOTAL_SIZE_KEY, "50MB");
        System.setProperty(TarUtil.TAR_MAX_ENTRIES_KEY, "1000");
        
        // Force refresh of cached values
        ZipUtil.getMaxFileSize();
        ZipUtil.getMaxTotalSize();
        ZipUtil.getMaxEntries();
        TarUtil.getMaxFileSize();
        TarUtil.getMaxTotalSize();
        TarUtil.getMaxEntries();
    }
    
    @After
    public void cleanup() throws IOException {
        // Recursively delete the temporary directory
        org.apache.commons.io.FileUtils.deleteDirectory(tempDir.toFile());
        
        // Clean up system properties
        System.clearProperty(ZipUtil.ZIP_MAX_FILE_SIZE_KEY);
        System.clearProperty(ZipUtil.ZIP_MAX_TOTAL_SIZE_KEY);
        System.clearProperty(ZipUtil.ZIP_MAX_ENTRIES_KEY);
        System.clearProperty(TarUtil.TAR_MAX_FILE_SIZE_KEY);
        System.clearProperty(TarUtil.TAR_MAX_TOTAL_SIZE_KEY);
        System.clearProperty(TarUtil.TAR_MAX_ENTRIES_KEY);
    }
    
    @Test
    public void testSanitizePath() {
        // Test normal paths
        assertEquals("file.txt", ArchiveUtil.sanitizePath("file.txt", ArchiveUtil.SuspiciousEntryHandling.ABORT, "test.zip"));
        assertEquals("dir/file.txt", ArchiveUtil.sanitizePath("dir/file.txt", ArchiveUtil.SuspiciousEntryHandling.ABORT, "test.zip"));
        
        // Test leading slashes removal
        assertEquals("path/file.txt", ArchiveUtil.sanitizePath("/path/file.txt", ArchiveUtil.SuspiciousEntryHandling.SKIP_AND_CONTINUE, "test.zip"));
        assertEquals("path/file.txt", ArchiveUtil.sanitizePath("//path/file.txt", ArchiveUtil.SuspiciousEntryHandling.SKIP_AND_CONTINUE, "test.zip"));
        
        // Test path traversal handling
        try {
            ArchiveUtil.sanitizePath("../file.txt", ArchiveUtil.SuspiciousEntryHandling.ABORT, "test.zip");
            fail("Should throw SecurityException for path traversal with ABORT mode");
        } catch (SecurityException e) {
            // Expected
            assertTrue("Exception message should mention archive path", e.getMessage().contains("test.zip"));
        }
        
        assertEquals("file.txt", ArchiveUtil.sanitizePath("../file.txt", ArchiveUtil.SuspiciousEntryHandling.SKIP_AND_CONTINUE, "test.zip"));
        assertEquals("etc/passwd", ArchiveUtil.sanitizePath("../../../etc/passwd", ArchiveUtil.SuspiciousEntryHandling.SKIP_AND_CONTINUE, "test.zip"));
    }
    
    @Test
    public void testValidatePathWithinDirectory() throws IOException {
        File parent = tempDir.toFile();
        
        // Create the parent directory if it doesn't exist
        parent.mkdirs();
        
        // Create necessary subdirectories
        File dirFile = new File(parent, "dir");
        dirFile.mkdirs();
        
        // Test valid paths - these should all return true with ABORT mode
        assertTrue("Plain file in root directory should be valid", 
            ArchiveUtil.validatePathWithinDirectory(parent, "file.txt", ArchiveUtil.SuspiciousEntryHandling.ABORT));
        
        assertTrue("File in subdirectory should be valid", 
            ArchiveUtil.validatePathWithinDirectory(parent, "dir/file.txt", ArchiveUtil.SuspiciousEntryHandling.ABORT));
        
        // Test path traversal with ABORT mode - should throw exception
        try {
            ArchiveUtil.validatePathWithinDirectory(parent, "../file.txt", ArchiveUtil.SuspiciousEntryHandling.ABORT);
            fail("Should throw SecurityException for path traversal with ABORT mode");
        } catch (SecurityException e) {
            // Expected behavior
        }
        
        // Test path traversal with SKIP_AND_CONTINUE mode - should return false
        assertFalse("Path traversal should return false with SKIP_AND_CONTINUE", 
            ArchiveUtil.validatePathWithinDirectory(parent, "../file.txt", ArchiveUtil.SuspiciousEntryHandling.SKIP_AND_CONTINUE));
    }
    
    @Test
    public void testFileFilters() throws IOException {
        // Create test files
        File textFile = Files.createTempFile(tempDir, "text", ".txt").toFile();
        File jsonFile = Files.createTempFile(tempDir, "data", ".json").toFile();
        File binFile = Files.createTempFile(tempDir, "binary", ".bin").toFile();
        File hiddenFile = new File(tempDir.toFile(), ".hidden");
        hiddenFile.createNewFile();
        
        // Create directory
        File directory = Files.createDirectory(tempDir.resolve("testdir")).toFile();
        
        // Test built-in filters
        assertTrue(ArchiveUtil.ACCEPT_ALL_FILTER.accept(textFile));
        assertTrue(ArchiveUtil.ACCEPT_ALL_FILTER.accept(directory));
        
        assertTrue(ArchiveUtil.REGULAR_FILES_ONLY.accept(textFile));
        assertFalse(ArchiveUtil.REGULAR_FILES_ONLY.accept(directory));
        
        assertFalse(ArchiveUtil.DIRECTORIES_ONLY.accept(textFile));
        assertTrue(ArchiveUtil.DIRECTORIES_ONLY.accept(directory));
        
        // Test extension filter
        ArchiveUtil.FileFilter txtFilter = ArchiveUtil.byExtension("txt");
        assertTrue(txtFilter.accept(textFile));
        assertFalse(txtFilter.accept(jsonFile));
        assertFalse(txtFilter.accept(directory));
        
        // Test multi-extension filter
        ArchiveUtil.FileFilter documentFilter = ArchiveUtil.byExtension("txt", "json", "md");
        assertTrue(documentFilter.accept(textFile));
        assertTrue(documentFilter.accept(jsonFile));
        assertFalse(documentFilter.accept(binFile));
        
        // Test pattern filter
        ArchiveUtil.FileFilter hiddenFilter = ArchiveUtil.byNamePattern("^\\..+");
        assertTrue(hiddenFilter.accept(hiddenFile));
        assertFalse(hiddenFilter.accept(textFile));
        
        // Test AND combination
        ArchiveUtil.FileFilter textFilesOnly = ArchiveUtil.and(
            ArchiveUtil.REGULAR_FILES_ONLY,
            ArchiveUtil.byExtension("txt")
        );
        assertTrue(textFilesOnly.accept(textFile));
        assertFalse(textFilesOnly.accept(jsonFile));
        assertFalse(textFilesOnly.accept(directory));
        
        // Test OR combination
        ArchiveUtil.FileFilter textOrJson = ArchiveUtil.or(
            ArchiveUtil.byExtension("txt"),
            ArchiveUtil.byExtension("json")
        );
        assertTrue(textOrJson.accept(textFile));
        assertTrue(textOrJson.accept(jsonFile));
        assertFalse(textOrJson.accept(binFile));
        
        // Test NOT combination
        ArchiveUtil.FileFilter notHidden = ArchiveUtil.not(hiddenFilter);
        assertTrue(notHidden.accept(textFile));
        assertFalse(notHidden.accept(hiddenFile));
        
        // Test complex combination
        ArchiveUtil.FileFilter complexFilter = ArchiveUtil.and(
            ArchiveUtil.REGULAR_FILES_ONLY,
            ArchiveUtil.or(
                ArchiveUtil.byExtension("txt", "json"),
                ArchiveUtil.not(hiddenFilter)
            )
        );
        assertTrue(complexFilter.accept(textFile));
        assertTrue(complexFilter.accept(jsonFile));
        assertTrue(complexFilter.accept(binFile)); // Not hidden, so included
        assertFalse(complexFilter.accept(hiddenFile)); // Hidden, so excluded
        assertFalse(complexFilter.accept(directory)); // Not a file, so excluded
    }
    
    @Test
    public void testEntryNameMappers() {
        File file = new File("/path/to/test.txt");
        
        // Test DEFAULT_NAME_MAPPER
        assertEquals("test.txt", ArchiveUtil.DEFAULT_NAME_MAPPER.mapEntryName(file, null));
        assertEquals("prefix/test.txt", ArchiveUtil.DEFAULT_NAME_MAPPER.mapEntryName(file, "prefix"));
        assertEquals("prefix/test.txt", ArchiveUtil.DEFAULT_NAME_MAPPER.mapEntryName(file, "prefix/"));
        
        // Test withPrefix mapper
        ArchiveUtil.EntryNameMapper prefixMapper = ArchiveUtil.withPrefix("docs");
        assertEquals("docs/test.txt", prefixMapper.mapEntryName(file, null));
        assertEquals("docs/prefix/test.txt", prefixMapper.mapEntryName(file, "prefix"));
        
        // Test withExtension mapper
        ArchiveUtil.EntryNameMapper extensionMapper = ArchiveUtil.withExtension("md");
        // Replace .txt with .md
        assertEquals("test.md", extensionMapper.mapEntryName(file, null));
        assertEquals("prefix/test.md", extensionMapper.mapEntryName(file, "prefix"));
        
        // Test with a file that doesn't have an extension
        File noExtFile = new File("/path/to/test");
        assertEquals("test.md", extensionMapper.mapEntryName(noExtFile, null));
    }
    
    @Test
    public void testFileProcessor() throws IOException {
        // Create test file
        File textFile = Files.createTempFile(tempDir, "test", ".txt").toFile();
        Files.write(textFile.toPath(), "Hello, world!".getBytes(StandardCharsets.UTF_8));
        
        // Test NO_PROCESSING
        try (InputStream is = ArchiveUtil.NO_PROCESSING.process(textFile, "test.txt")) {
            String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            assertEquals("Hello, world!", content);
        }
        
        // Test custom processor
        ArchiveUtil.FileProcessor headerProcessor = (file, entryName) -> {
            String original = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            String processed = "HEADER: " + original + " (" + entryName + ")";
            return new ByteArrayInputStream(processed.getBytes(StandardCharsets.UTF_8));
        };
        
        try (InputStream is = headerProcessor.process(textFile, "docs/test.txt")) {
            String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            assertEquals("HEADER: Hello, world! (docs/test.txt)", content);
        }
    }
    
    @Test
    public void testHelperMethods() {
        // Test normalizeBasePath
        assertEquals("", ArchiveUtil.normalizeBasePath(null));
        assertEquals("", ArchiveUtil.normalizeBasePath(""));
        assertEquals("path/", ArchiveUtil.normalizeBasePath("path"));
        assertEquals("path/", ArchiveUtil.normalizeBasePath("path/"));
        
        // Test singleFileCollection
        File file = new File("test.txt");
        Collection<File> collection = ArchiveUtil.singleFileCollection(file);
        assertEquals(1, collection.size());
        assertTrue(collection.contains(file));
    }
    
    @Test
    public void testCheckSecurity() throws IOException {
        // Create test files
        File parent = new File(tempDir.toFile(), "extracted");
        parent.mkdir();
        
        File safeFile = new File(parent, "safe.txt");
        File subDir = new File(parent, "subdir");
        File safeFileInSubDir = new File(subDir, "safe.txt");
        File unsafeFile = new File(tempDir.toFile(), "unsafe.txt"); // Outside the parent directory
        
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
        File parent = new File(tempDir.toFile(), "parent");
        parent.mkdir();
        
        File safeFile = new File(parent, "safe.txt");
        File safeSubDir = new File(parent, "subdir");
        File safeFileInSubDir = new File(safeSubDir, "safe.txt");
        
        // Create a file outside the parent directory
        File outsideDir = new File(tempDir.toFile(), "outside");
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
    
    /**
     * Test the integration between ZipUtil and TarUtil through ArchiveUtil
     * This verifies that the same filter/processor/mapper works identically for both formats
     */
    @Test
    public void testZipAndTarIntegration() throws IOException {
        // Create test directory with files
        Path sourceDir = Files.createDirectory(tempDir.resolve("source"));
        File textFile = new File(sourceDir.toFile(), "file.txt");
        File jsonFile = new File(sourceDir.toFile(), "data.json");
        File hiddenFile = new File(sourceDir.toFile(), ".hidden");
        
        // Create content - keep content very small
        Files.writeString(textFile.toPath(), "Text content");
        Files.writeString(jsonFile.toPath(), "{}");
        Files.writeString(hiddenFile.toPath(), "Hidden");
        
        // Define common filter, processor and mapper
        ArchiveUtil.FileFilter filter = ArchiveUtil.and(
            ArchiveUtil.REGULAR_FILES_ONLY,
            ArchiveUtil.not(ArchiveUtil.byNamePattern("^\\..+")) // Exclude hidden files
        );
        
        ArchiveUtil.FileProcessor processor = (file, entryName) -> {
            if (file.getName().endsWith(".txt")) {
                String content = Files.readString(file.toPath());
                return new ByteArrayInputStream(("PROC:" + content).getBytes());
            }
            return Files.newInputStream(file.toPath());
        };
        
        ArchiveUtil.EntryNameMapper mapper = ArchiveUtil.withPrefix("archive");
        
        // Create ZIP file
        File zipFile = new File(tempDir.toFile(), "archive.zip");
        ZipUtil.zipFilesWithCustomProcessing(
            Arrays.asList(textFile, jsonFile, hiddenFile),
            zipFile,
            "",
            filter,
            processor,
            mapper
        );
        
        // Create TAR.GZ file
        File tarFile = new File(tempDir.toFile(), "archive.tar.gz");
        TarUtil.tarGzFilesWithCustomProcessing(
            Arrays.asList(textFile, jsonFile, hiddenFile),
            tarFile,
            "",
            filter,
            processor,
            mapper
        );
        
        // Verify the archive files were created successfully
        assertTrue("ZIP file should exist", zipFile.exists());
        assertTrue("TAR file should exist", tarFile.exists());
        
        // Override system properties for the extraction
        // Use very large limits to avoid size limit errors
        String oldZipMaxFileSize = System.getProperty(ZipUtil.ZIP_MAX_FILE_SIZE_KEY);
        String oldTarMaxFileSize = System.getProperty(TarUtil.TAR_MAX_FILE_SIZE_KEY);
        String oldZipMaxTotalSize = System.getProperty(ZipUtil.ZIP_MAX_TOTAL_SIZE_KEY);
        String oldTarMaxTotalSize = System.getProperty(TarUtil.TAR_MAX_TOTAL_SIZE_KEY);
        
        try {
            System.setProperty(ZipUtil.ZIP_MAX_FILE_SIZE_KEY, "10GB");
            System.setProperty(TarUtil.TAR_MAX_FILE_SIZE_KEY, "10GB");
            System.setProperty(ZipUtil.ZIP_MAX_TOTAL_SIZE_KEY, "50GB");
            System.setProperty(TarUtil.TAR_MAX_TOTAL_SIZE_KEY, "50GB");
            
            // Extract both archives with the override settings in effect
            Path zipExtractDir = Files.createTempDirectory(tempDir, "zip-extract");
            try (InputStream zipIs = Files.newInputStream(zipFile.toPath())) {
                ZipUtil.safeExtract(zipIs, zipExtractDir.toString());
            }
            
            Path tarExtractDir = Files.createTempDirectory(tempDir, "tar-extract");
            TarUtil.safeExtractTarGz(tarFile, tarExtractDir.toFile());
            
            // Expected files
            File zipTextFile = new File(zipExtractDir.toFile(), "archive/file.txt");
            File zipJsonFile = new File(zipExtractDir.toFile(), "archive/data.json");
            File zipHiddenFile = new File(zipExtractDir.toFile(), "archive/.hidden");
            
            File tarTextFile = new File(tarExtractDir.toFile(), "archive/file.txt");
            File tarJsonFile = new File(tarExtractDir.toFile(), "archive/data.json");
            File tarHiddenFile = new File(tarExtractDir.toFile(), "archive/.hidden");
            
            // Debug output
            System.out.println("Looking for zipTextFile: " + zipTextFile.getAbsolutePath());
            if (!zipTextFile.exists()) {
                System.out.println("Files in zip extract dir: " + Arrays.toString(zipExtractDir.toFile().list()));
                File archiveDir = new File(zipExtractDir.toFile(), "archive");
                if (archiveDir.exists()) {
                    System.out.println("Files in archive dir: " + Arrays.toString(archiveDir.list()));
                }
            }
            
            // Verify files exist
            assertTrue("Text file should exist in ZIP", zipTextFile.exists());
            assertTrue("JSON file should exist in ZIP", zipJsonFile.exists());
            assertFalse("Hidden file should not exist in ZIP", zipHiddenFile.exists());
            
            assertTrue("Text file should exist in TAR", tarTextFile.exists());
            assertTrue("JSON file should exist in TAR", tarJsonFile.exists());
            assertFalse("Hidden file should not exist in TAR", tarHiddenFile.exists());
            
            // Verify content is identical between formats
            String zipTextContent = Files.readString(zipTextFile.toPath());
            String tarTextContent = Files.readString(tarTextFile.toPath());
            assertEquals("Text file content should be identical between formats", zipTextContent, tarTextContent);
            assertTrue("Text file should have been processed", zipTextContent.startsWith("PROC:"));
            
            String zipJsonContent = Files.readString(zipJsonFile.toPath());
            String tarJsonContent = Files.readString(tarJsonFile.toPath());
            assertEquals("JSON file content should be identical between formats", zipJsonContent, tarJsonContent);
        } finally {
            // Restore previous system properties
            if (oldZipMaxFileSize != null) {
                System.setProperty(ZipUtil.ZIP_MAX_FILE_SIZE_KEY, oldZipMaxFileSize);
            } else {
                System.clearProperty(ZipUtil.ZIP_MAX_FILE_SIZE_KEY);
            }
            
            if (oldTarMaxFileSize != null) {
                System.setProperty(TarUtil.TAR_MAX_FILE_SIZE_KEY, oldTarMaxFileSize);
            } else {
                System.clearProperty(TarUtil.TAR_MAX_FILE_SIZE_KEY);
            }
            
            if (oldZipMaxTotalSize != null) {
                System.setProperty(ZipUtil.ZIP_MAX_TOTAL_SIZE_KEY, oldZipMaxTotalSize);
            } else {
                System.clearProperty(ZipUtil.ZIP_MAX_TOTAL_SIZE_KEY);
            }
            
            if (oldTarMaxTotalSize != null) {
                System.setProperty(TarUtil.TAR_MAX_TOTAL_SIZE_KEY, oldTarMaxTotalSize);
            } else {
                System.clearProperty(TarUtil.TAR_MAX_TOTAL_SIZE_KEY);
            }
        }
    }
} 