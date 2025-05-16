package com.dotmarketing.util;

import com.dotmarketing.util.TarUtil.SuspiciousEntryHandling;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPOutputStream;

import static org.junit.Assert.*;

/**
 * Test class for {@link TarUtil}
 */
public class TarUtilTest {

    private static Path tempDir;

    @BeforeClass
    public static void setup() throws IOException {
        tempDir = Files.createTempDirectory("tarutil-test");
    }

    @AfterClass
    public static void cleanup() throws IOException {
        // Recursively delete the temporary directory
        org.apache.commons.io.FileUtils.deleteDirectory(tempDir.toFile());
    }

    @Test
    public void testSanitizingPathWithLeadingSlash() {
        String path = "/file.txt";
        String sanitized = ZipUtil.sanitizePath(path, ZipUtil.SuspiciousEntryHandling.SKIP_AND_CONTINUE);
        assertEquals("file.txt", sanitized);
    }

    @Test
    public void testCreateSafeTarEntry() {
        // Use SKIP_AND_CONTINUE to allow the test to pass while verifying sanitization
        TarArchiveEntry entry = TarUtil.createSafeTarEntry("/etc/passwd", SuspiciousEntryHandling.SKIP_AND_CONTINUE);
        assertEquals("etc/passwd", entry.getName());
    }

    @Test
    public void testCreateSafeTarEntryWithFile() throws IOException {
        File tempFile = Files.createTempFile(tempDir, "test", ".txt").toFile();
        
        // Use SKIP_AND_CONTINUE to allow the test to pass while verifying sanitization
        TarArchiveEntry entry = TarUtil.createSafeTarEntry(tempFile, "/malicious/path/file.txt", SuspiciousEntryHandling.SKIP_AND_CONTINUE);
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
        TarUtil.createSafeTarEntry("../../../etc/passwd", SuspiciousEntryHandling.ABORT);
    }

    @Test
    public void testPathTraversalWithSkipAndContinueMode() {
        // This should sanitize the path with SKIP_AND_CONTINUE mode
        TarArchiveEntry entry = TarUtil.createSafeTarEntry("../../../etc/passwd", SuspiciousEntryHandling.SKIP_AND_CONTINUE);
        assertEquals("etc/passwd", entry.getName());
    }

    @Test
    public void testSafeExtractTarGz() throws IOException {
        // Create a test tar.gz file with a regular entry and a potentially malicious entry
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        // Create a tar.gz archive in memory
        try (TarArchiveOutputStream taos = new TarArchiveOutputStream(new GZIPOutputStream(baos))) {
            taos.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_STAR);
            taos.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);
            
            // Add a normal file
            TarArchiveEntry normalEntry = new TarArchiveEntry("normal/file.txt");
            byte[] normalContent = "Normal content".getBytes(StandardCharsets.UTF_8);
            normalEntry.setSize(normalContent.length);
            taos.putArchiveEntry(normalEntry);
            taos.write(normalContent);
            taos.closeArchiveEntry();
            
            // Add a file with path traversal attempt
            TarArchiveEntry maliciousEntry = new TarArchiveEntry("../../../../etc/malicious.txt");
            byte[] maliciousContent = "Malicious content".getBytes(StandardCharsets.UTF_8);
            maliciousEntry.setSize(maliciousContent.length);
            taos.putArchiveEntry(maliciousEntry);
            taos.write(maliciousContent);
            taos.closeArchiveEntry();
        }
        
        // Create a fresh output directory for extraction
        Path extractDir = Files.createTempDirectory(tempDir, "extract");
        
        // Set handling mode to SKIP_AND_CONTINUE to test that it skips bad entries
        TarUtil.setDefaultSuspiciousEntryHandling(SuspiciousEntryHandling.SKIP_AND_CONTINUE);
        
        // Extract the archive
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        TarUtil.safeExtractTarGz(bais, extractDir.toFile());
        
        // Verify the normal file was extracted
        File normalFile = new File(extractDir.toFile(), "normal/file.txt");
        assertTrue("Normal file should be extracted", normalFile.exists());
        
        // Verify the malicious file was sanitized and extracted in a safe location
        File sanitizedFile = new File(extractDir.toFile(), "etc/malicious.txt");
        assertTrue("Sanitized file should be extracted", sanitizedFile.exists());
        
        // Verify the malicious file was NOT extracted in the original malicious path
        File maliciousOutsideFile = new File(extractDir.getParent().toFile(), "etc/malicious.txt");
        assertFalse("Malicious file should not be extracted outside the target dir", maliciousOutsideFile.exists());
        
        // Restore the default handling mode
        TarUtil.setDefaultSuspiciousEntryHandling(SuspiciousEntryHandling.ABORT);
    }
    
    @Test(expected = SecurityException.class)
    public void testSafeExtractTarGzWithAbortMode() throws IOException {
        // Create a malicious tar file
        File tarFile = Files.createTempFile(tempDir, "malicious", ".tar.gz").toFile();
        
        try (TarArchiveOutputStream taos = TarUtil.createTarGzOutputStream(tarFile)) {
            // Add a malicious file with path traversal
            TarArchiveEntry maliciousEntry = new TarArchiveEntry("../../../../etc/passwd");
            byte[] maliciousContent = "fake passwd content".getBytes(StandardCharsets.UTF_8);
            maliciousEntry.setSize(maliciousContent.length);
            taos.putArchiveEntry(maliciousEntry);
            taos.write(maliciousContent);
            taos.closeArchiveEntry();
        }
        
        // Set mode to ABORT
        TarUtil.setDefaultSuspiciousEntryHandling(SuspiciousEntryHandling.ABORT);
        
        // This should throw a SecurityException
        Path extractDir = Files.createTempDirectory(tempDir, "extract-abort");
        TarUtil.safeExtractTarGz(tarFile, extractDir.toFile());
    }
    
    @Test
    public void testComplexDirectoryExtraction() throws IOException {
        // Create a tar file with explicit directory entries and then files
        File tarFile = Files.createTempFile(tempDir, "complex", ".tar.gz").toFile();
        
        try (TarArchiveOutputStream taos = TarUtil.createTarGzOutputStream(tarFile)) {
            // Create directory entries first
            TarArchiveEntry dir1Entry = new TarArchiveEntry("dir1/");
            dir1Entry.setSize(0);
            dir1Entry.setMode(TarArchiveEntry.DEFAULT_DIR_MODE);
            dir1Entry.setModTime(System.currentTimeMillis());
            taos.putArchiveEntry(dir1Entry);
            taos.closeArchiveEntry();
            
            TarArchiveEntry dir2Entry = new TarArchiveEntry("dir2/");
            dir2Entry.setSize(0);
            dir2Entry.setMode(TarArchiveEntry.DEFAULT_DIR_MODE);
            dir2Entry.setModTime(System.currentTimeMillis());
            taos.putArchiveEntry(dir2Entry);
            taos.closeArchiveEntry();
            
            // Add file entries after directory entries
            byte[] content1 = "content1".getBytes(StandardCharsets.UTF_8);
            TarArchiveEntry file1Entry = new TarArchiveEntry("dir1/file1.txt");
            file1Entry.setSize(content1.length);
            taos.putArchiveEntry(file1Entry);
            taos.write(content1);
            taos.closeArchiveEntry();
            
            byte[] content2 = "content2".getBytes(StandardCharsets.UTF_8);
            TarArchiveEntry file2Entry = new TarArchiveEntry("dir2/file2.txt");
            file2Entry.setSize(content2.length);
            taos.putArchiveEntry(file2Entry);
            taos.write(content2);
            taos.closeArchiveEntry();
        }
        
        // Extract
        Path extractDir = Files.createTempDirectory(tempDir, "extract-complex");
        TarUtil.safeExtractTarGz(tarFile, extractDir.toFile());
        
        // Verify extracted structure using File objects directly
        File dir1 = new File(extractDir.toFile(), "dir1");
        File dir2 = new File(extractDir.toFile(), "dir2");
        assertTrue("dir1 should exist", dir1.exists());
        assertTrue("dir2 should exist", dir2.exists());
        
        File file1 = new File(dir1, "file1.txt"); 
        File file2 = new File(dir2, "file2.txt");
        assertTrue("file1.txt should exist", file1.exists());
        assertTrue("file2.txt should exist", file2.exists());
        
        // Verify content
        assertEquals("content1", new String(Files.readAllBytes(file1.toPath()), StandardCharsets.UTF_8));
        assertEquals("content2", new String(Files.readAllBytes(file2.toPath()), StandardCharsets.UTF_8));
    }
    
    @Test
    public void testAddingMultipleFilesToTar() throws IOException {
        // Create files
        File textFile = Files.createTempFile(tempDir, "text", ".txt").toFile();
        Files.write(textFile.toPath(), "Text content".getBytes(StandardCharsets.UTF_8));
        
        File dataFile = Files.createTempFile(tempDir, "data", ".dat").toFile();
        Files.write(dataFile.toPath(), "Binary data".getBytes(StandardCharsets.UTF_8));
        
        // Create tar file
        File tarFile = Files.createTempFile(tempDir, "multifile", ".tar.gz").toFile();
        
        try (TarArchiveOutputStream taos = TarUtil.createTarGzOutputStream(tarFile)) {
            // Add files
            TarUtil.addFileToTar(taos, textFile, "documents/text.txt");
            TarUtil.addFileToTar(taos, dataFile, "data/binary.dat");
            
            // Add bytes directly
            byte[] xmlContent = "<root>XML content</root>".getBytes(StandardCharsets.UTF_8);
            TarUtil.addBytesToTar(taos, xmlContent, "config/settings.xml");
        }
        
        // Extract
        Path extractDir = Files.createTempDirectory(tempDir, "extract-multi");
        TarUtil.safeExtractTarGz(tarFile, extractDir.toFile());
        
        // Verify extracted files
        assertTrue(Files.exists(extractDir.resolve("documents/text.txt")));
        assertTrue(Files.exists(extractDir.resolve("data/binary.dat")));
        assertTrue(Files.exists(extractDir.resolve("config/settings.xml")));
        
        // Verify content
        assertEquals("Text content", 
                new String(Files.readAllBytes(extractDir.resolve("documents/text.txt")), StandardCharsets.UTF_8));
        assertEquals("Binary data", 
                new String(Files.readAllBytes(extractDir.resolve("data/binary.dat")), StandardCharsets.UTF_8));
        assertEquals("<root>XML content</root>", 
                new String(Files.readAllBytes(extractDir.resolve("config/settings.xml")), StandardCharsets.UTF_8));
    }
} 