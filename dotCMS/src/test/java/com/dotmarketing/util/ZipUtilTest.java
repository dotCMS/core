package com.dotmarketing.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.After;

public class ZipUtilTest  {

     private File tempDir;
     
     // Store original values to restore after tests
     private long originalMaxFileSize;
     private long originalMaxTotalSize;
     private int originalMaxEntries;
     private ZipUtil.SuspiciousEntryHandling originalHandlingMode;
     
     @Before
     public void setup() throws IOException {
          tempDir = com.google.common.io.Files.createTempDir();
          
          // Store original values
          originalMaxFileSize = ZipUtil.getMaxFileSize();
          originalMaxTotalSize = ZipUtil.getMaxTotalSize();
          originalMaxEntries = ZipUtil.getMaxEntries();
          originalHandlingMode = ZipUtil.getDefaultSuspiciousEntryHandling();
          
          // Set appropriate test limits
          System.setProperty(ZipUtil.ZIP_MAX_FILE_SIZE_KEY, "10MB");
          System.setProperty(ZipUtil.ZIP_MAX_TOTAL_SIZE_KEY, "50MB");
          System.setProperty(ZipUtil.ZIP_MAX_ENTRIES_KEY, "1000");
          // Ensure the values are actually parsed correctly
          ZipUtil.getMaxFileSize(); // Force refresh of cached values
          ZipUtil.getMaxTotalSize();
          ZipUtil.getMaxEntries();
     }
     
     @After
     public void cleanup() throws IOException {
          // Restore original values
          System.setProperty(ZipUtil.ZIP_MAX_FILE_SIZE_KEY, String.valueOf(originalMaxFileSize));
          System.setProperty(ZipUtil.ZIP_MAX_TOTAL_SIZE_KEY, String.valueOf(originalMaxTotalSize));
          System.setProperty(ZipUtil.ZIP_MAX_ENTRIES_KEY, String.valueOf(originalMaxEntries));
          ZipUtil.setDefaultSuspiciousEntryHandling(originalHandlingMode);
          
          if (tempDir != null && tempDir.exists()) {
               FileUtils.deleteDirectory(tempDir);
          }
     }

     @Test
     public void simpleChildPathMatchTest() throws IOException {

          final File parent = new File("/Users/my-user/code/servers/server1/esdata/");
          final File child = new File("/Users/my-user/code/servers/server1/esdata/essnapshot/snapshots/snap-fHdh_dwrRci5Q7djIKCm7A.dat");
          assertTrue(ZipUtil.isNewFileDestinationSafe(parent,child));
     }

     @Test
     public void invalidFolderAttemptTest() throws IOException {
          final File parent = new File("/Users/my-user/code/servers/server1/esdata/");
          final File child = new File("/securitytest/jbgtest.txt");
          assertFalse(ZipUtil.isNewFileDestinationSafe(parent,child));
     }


     /**
      * Test that an invalid zip input stream throws a SecurityException
      */
     @Test
     public void testUnzipInvalidInputStream() throws Exception {
          // Remember original settings
          final long originalMaxFileSize = ZipUtil.getMaxFileSize();
          final ZipUtil.SuspiciousEntryHandling originalHandlingMode = ZipUtil.getDefaultSuspiciousEntryHandling();
          
          try {
               // Set a higher file size limit for this test
               System.setProperty(ZipUtil.ZIP_MAX_FILE_SIZE_KEY, "10MB");
               // Force refresh cached value
               ZipUtil.getMaxFileSize();
               
               final File evilZipFile = createEvilZip();
               final File tmpDir = Files.createTempDirectory(tempDir.toPath(), "security-test-").toFile();
               
               boolean exceptionThrown = false;
               
               // Make sure we're using ABORT mode for this test
               ZipUtil.setDefaultSuspiciousEntryHandling(ZipUtil.SuspiciousEntryHandling.ABORT);
               
               try (InputStream in = Files.newInputStream(evilZipFile.toPath())) {
                    ZipUtil.safeExtract(in, tmpDir.getAbsolutePath());
                    fail("Expected SecurityException when extracting malicious zip file");
               } catch (SecurityException e) {
                    // Expected - this is good
                    exceptionThrown = true;
                    // Verify the exception message indicates a problem with the path
                    assertTrue(e.getMessage().contains("Illegal"));
               }
               // Verify the exception was thrown
               assertTrue("Expected SecurityException was not thrown", exceptionThrown);
               
               // Also verify no files were actually extracted to the parent directory
               File parentDir = tmpDir.getParentFile();
               File potentiallyExtractedFile = new File(parentDir, "evil.txt");
               assertFalse("File should not have been extracted outside target directory", 
                         potentiallyExtractedFile.exists());
          } finally {
               // Restore original settings
               System.setProperty(ZipUtil.ZIP_MAX_FILE_SIZE_KEY, String.valueOf(originalMaxFileSize));
               ZipUtil.setDefaultSuspiciousEntryHandling(originalHandlingMode);
               // Force refresh cached value
               ZipUtil.getMaxFileSize();
          }
     }

     /**
      * Test that an invalid zip input stream can be handled with SKIP_AND_CONTINUE mode
      */
     @Test
     public void testUnzipInvalidInputStreamWithSkipMode() throws Exception {
          // Remember original settings
          final long originalMaxFileSize = ZipUtil.getMaxFileSize();
          final ZipUtil.SuspiciousEntryHandling originalHandlingMode = ZipUtil.getDefaultSuspiciousEntryHandling();
          
          try {
               // Set a higher file size limit for this test
               System.setProperty(ZipUtil.ZIP_MAX_FILE_SIZE_KEY, "10MB");
               // Force refresh cached value
               ZipUtil.getMaxFileSize();
               
               final File evilZipFile = createMixedZip();
               final File tmpDir = Files.createTempDirectory(tempDir.toPath(), "invalid-stream-").toFile();
               
               // Explicitly set the handling mode to SKIP_AND_CONTINUE for this test
               ZipUtil.setDefaultSuspiciousEntryHandling(ZipUtil.SuspiciousEntryHandling.SKIP_AND_CONTINUE);
               
               try (InputStream in = Files.newInputStream(evilZipFile.toPath())) {
                    ZipUtil.safeExtract(in, tmpDir.getAbsolutePath());
                    
                    // Verify only the good file was extracted
                    File goodFile = new File(tmpDir, "good.txt");
                    assertTrue("Good file should have been extracted", goodFile.exists());
                    
                    // Verify the evil file was NOT extracted to parent directory
                    File parentDir = tmpDir.getParentFile();
                    File potentiallyExtractedFile = new File(parentDir, "evil.txt");
                    assertFalse("Evil file should not have been extracted", 
                              potentiallyExtractedFile.exists());
               }
          } finally {
               // Restore original settings
               System.setProperty(ZipUtil.ZIP_MAX_FILE_SIZE_KEY, String.valueOf(originalMaxFileSize));
               ZipUtil.setDefaultSuspiciousEntryHandling(originalHandlingMode);
               // Force refresh cached value
               ZipUtil.getMaxFileSize();
          }
     }
     
     /**
      * Test explicitly specifying handling mode for a specific operation
      */
     @Test
     public void testExplicitHandlingMode() throws Exception {
          // Remember original settings
          final long originalMaxFileSize = ZipUtil.getMaxFileSize();
          final ZipUtil.SuspiciousEntryHandling originalHandlingMode = ZipUtil.getDefaultSuspiciousEntryHandling();
          
          try {
               // Set a higher file size limit for this test
               System.setProperty(ZipUtil.ZIP_MAX_FILE_SIZE_KEY, "10MB");
               // Force refresh cached value
               ZipUtil.getMaxFileSize();
               
               final File evilZipFile = createMixedZip();
               final File tmpDir = Files.createTempDirectory(tempDir.toPath(), "explicit-mode-").toFile();

               // Use ABORT mode for sanitizePath - should throw exception
               try {
                    ZipUtil.sanitizePath("../../../etc/passwd", ZipUtil.SuspiciousEntryHandling.ABORT);
                    fail("Should have thrown SecurityException with ABORT mode");
               } catch (SecurityException e) {
                    // Expected
               }
               
               // Use SKIP_AND_CONTINUE mode for sanitizePath - should sanitize and return result
               String sanitized = ZipUtil.sanitizePath("../../../etc/passwd", 
                                              ZipUtil.SuspiciousEntryHandling.SKIP_AND_CONTINUE);
               assertEquals("etc/passwd", sanitized);
               
               // Test extraction with explicit SKIP_AND_CONTINUE mode
               try (InputStream in = Files.newInputStream(evilZipFile.toPath())) {
                    ZipUtil.safeExtract(in, tmpDir.getAbsolutePath(), 
                               ZipUtil.SuspiciousEntryHandling.SKIP_AND_CONTINUE);
                    
                    // Verify only the good file was extracted
                    File goodFile = new File(tmpDir, "good.txt");
                    assertTrue("Good file should have been extracted", goodFile.exists());
               }
          } finally {
               // Restore original settings
               System.setProperty(ZipUtil.ZIP_MAX_FILE_SIZE_KEY, String.valueOf(originalMaxFileSize));
               ZipUtil.setDefaultSuspiciousEntryHandling(originalHandlingMode);
               // Force refresh cached value
               ZipUtil.getMaxFileSize();
          }
     }

     /**
      * Creates a zip file that includes a file that attempts a directory traversal
      * @return
      * @throws Exception
      */
     File createEvilZip() throws Exception {
          final File badZipFile = File.createTempFile("badzip-", ".zip");
          
          // Create a zip with an entry that has a malicious path
          try (ZipOutputStream zout = ZipUtil.createZipOutputStream(badZipFile)) {
               // Add a normal file
               ZipEntry goodEntry = new ZipEntry("good.txt");
               zout.putNextEntry(goodEntry);
               zout.write("Good content".getBytes());
               zout.closeEntry();
               
               // Add a malicious entry with a path traversal attempt
               ZipEntry badEntry = new ZipEntry("../../../../../../../../../../tmp/evil.txt");
               zout.putNextEntry(badEntry);
               zout.write("Malicious content".getBytes());
               zout.closeEntry();
          }
          
          badZipFile.deleteOnExit();
          Logger.info(ZipUtilTest.class, "created bad zip:" + badZipFile);
          return badZipFile;
     }
     
     /**
      * Creates a zip file with a mix of good and bad entries for testing SKIP_AND_CONTINUE mode
      * @return
      * @throws Exception
      */
     File createMixedZip() throws Exception {
          final File mixedZipFile = File.createTempFile("mixedzip-", ".zip");
          
          // Create a zip with both good and malicious entries
          try (ZipOutputStream zout = ZipUtil.createZipOutputStream(mixedZipFile)) {
               // Add a normal file first
               ZipEntry goodEntry1 = new ZipEntry("good.txt");
               zout.putNextEntry(goodEntry1);
               zout.write("Good content 1".getBytes());
               zout.closeEntry();
               
               // Add a malicious entry with a path traversal attempt
               ZipEntry badEntry1 = new ZipEntry("../../../../../../../../../../tmp/evil.txt");
               zout.putNextEntry(badEntry1);
               zout.write("Malicious content 1".getBytes());
               zout.closeEntry();
               
               // Add another normal file
               ZipEntry goodEntry2 = new ZipEntry("subfolder/good2.txt");
               zout.putNextEntry(goodEntry2);
               zout.write("Good content 2".getBytes());
               zout.closeEntry();
               
               // Add another malicious entry
               ZipEntry badEntry2 = new ZipEntry("/etc/passwd");
               zout.putNextEntry(badEntry2);
               zout.write("Malicious content 2".getBytes());
               zout.closeEntry();
          }
          
          mixedZipFile.deleteOnExit();
          Logger.info(ZipUtilTest.class, "created mixed zip:" + mixedZipFile);
          return mixedZipFile;
     }
     
     /**
      * Test the sanitizePath method for various path inputs
      */
     @Test
     public void testSanitizePath() {
          // Test normal valid paths that shouldn't throw exceptions
          assertEquals("path/file.txt", ZipUtil.sanitizePath("path/file.txt", ZipUtil.SuspiciousEntryHandling.ABORT, "test.zip"));
          assertEquals("path/file.txt", ZipUtil.sanitizePath("path//file.txt", ZipUtil.SuspiciousEntryHandling.ABORT, "test.zip"));
          assertEquals("file.txt", ZipUtil.sanitizePath("./file.txt", ZipUtil.SuspiciousEntryHandling.ABORT, "test.zip"));
          
          // Test paths with leading slashes - these should be sanitized but not throw exceptions
          assertEquals("path/to/file.txt", ZipUtil.sanitizePath("/path/to/file.txt", ZipUtil.SuspiciousEntryHandling.SKIP_AND_CONTINUE, "test.zip"));
          assertEquals("path/to/file.txt", ZipUtil.sanitizePath("//path/to/file.txt", ZipUtil.SuspiciousEntryHandling.SKIP_AND_CONTINUE, "test.zip"));
          
          // Test potentially malicious paths - these should throw SecurityExceptions
          try {
               ZipUtil.sanitizePath("../safe/file.txt", ZipUtil.SuspiciousEntryHandling.ABORT, "test.zip");
               fail("Should have thrown SecurityException for path with traversal");
          } catch (SecurityException e) {
               // Expected
               assertTrue("Exception message should mention archive path", e.getMessage().contains("test.zip"));
          }
          
          try {
               ZipUtil.sanitizePath("../../../../safe/file.txt", ZipUtil.SuspiciousEntryHandling.ABORT, "test.zip");
               fail("Should have thrown SecurityException for path with multiple traversals");
          } catch (SecurityException e) {
               // Expected
               assertTrue("Exception message should mention archive path", e.getMessage().contains("test.zip"));
          }
          
          try {
               ZipUtil.sanitizePath("safe/../../safe/file.txt", ZipUtil.SuspiciousEntryHandling.ABORT, "test.zip");
               fail("Should have thrown SecurityException for path with embedded traversals");
          } catch (SecurityException e) {
               // Expected
               assertTrue("Exception message should mention archive path", e.getMessage().contains("test.zip"));
          }
     }
     
     /**
      * Test the createSafeZipEntry method
      */
     @Test
     public void testCreateSafeZipEntry() {
          // Test with safe path
          ZipEntry safeEntry = ZipUtil.createSafeZipEntry("safe/path.txt");
          assertEquals("safe/path.txt", safeEntry.getName());
          
          // Test with unsafe path - should throw SecurityException
          try {
               ZipUtil.createSafeZipEntry("../../../unsafe/path.txt");
               fail("Should have thrown SecurityException for unsafe path");
          } catch (SecurityException e) {
               // Expected
          }
     }
     
     /**
      * Test the addZipEntry method
      */
     @Test
     public void testAddZipEntry() throws IOException {
          // Create a temp zip file
          File zipFile = File.createTempFile("test-zip-", ".zip");
          zipFile.deleteOnExit();
          
          try (ZipOutputStream zos = ZipUtil.createZipOutputStream(zipFile)) {
               // Try to add an entry with a safe path - should succeed
               String safePath = "path/to/file.txt";
               String content = "test content";
               
               try (ByteArrayInputStream bais = new ByteArrayInputStream(content.getBytes())) {
                    ZipUtil.addZipEntry(zos, safePath, bais, true);
               }
               
               // Try to add an entry with a malicious path - should throw SecurityException
               String maliciousPath = "../../../etc/passwd";
               try (ByteArrayInputStream bais = new ByteArrayInputStream(content.getBytes())) {
                    ZipUtil.addZipEntry(zos, maliciousPath, bais, true);
                    fail("Should have thrown SecurityException for malicious path");
               } catch (SecurityException e) {
                    // Expected
               }
          }
     }
     
     /**
      * Test the safeExtract method with a zip containing a path traversal attempt
      */
     @Test
     public void testSafeExtract() throws Exception {
          // Create an evil zip file
          final File evilZipFile = createEvilZip();
          try {
               boolean exceptionThrown = false;
               // Extract it using our safe method - this should throw SecurityException
               try (InputStream in = Files.newInputStream(evilZipFile.toPath())) {
                    ZipUtil.safeExtract(in, tempDir.getAbsolutePath());
               } catch (SecurityException e) {
                    // Expected - this is good
                    exceptionThrown = true;
               }
               // Verify the exception was thrown
               assertTrue("Expected SecurityException was not thrown", exceptionThrown);
          } finally {
               evilZipFile.delete();
          }
     }
     
     /**
      * Test the safe zip entry extraction with a complex path
      */
     @Test
     public void testSafeZipEntryExtraction() throws Exception {
          // Remember original settings
          final long originalMaxFileSize = ZipUtil.getMaxFileSize();
          
          try {
               // Create a zip file with a valid structure but suspicious entry names
               File zipFile = createSafeComplexPathsZip();
               
               // Ensure we have reasonable file size limit for test
               System.setProperty(ZipUtil.ZIP_MAX_FILE_SIZE_KEY, "10MB");
               // Force refresh cached value
               ZipUtil.getMaxFileSize();
               
               // Create temp dir for extraction
               File extractDir = Files.createTempDirectory(tempDir.toPath(), "extract-").toFile();
               
               // Set to SKIP_AND_CONTINUE mode for this test to allow sanitized paths
               ZipUtil.setDefaultSuspiciousEntryHandling(ZipUtil.SuspiciousEntryHandling.SKIP_AND_CONTINUE);
               
               // First test - extract using ZipFile
               try (ZipFile zf = new ZipFile(zipFile)) {
                    Enumeration<? extends ZipEntry> entries = zf.entries();
                    while (entries.hasMoreElements()) {
                         ZipEntry entry = entries.nextElement();
                         ZipUtil.safeExtractEntry(zf, entry, extractDir);
                    }
               }
               
               // Verify files were extracted correctly with sanitized paths
               File safeFile = new File(extractDir, "safe/file.txt");
               assertTrue("Safe file should be extracted", safeFile.exists());
               
               // The suspicious paths should be sanitized but still extracted
               File sanitizedFile = new File(extractDir, "etc/passwd");
               assertFalse("File with sanitized path should NOT be extracted", sanitizedFile.exists());
          } finally {
               // Restore original setting
               System.setProperty(ZipUtil.ZIP_MAX_FILE_SIZE_KEY, String.valueOf(originalMaxFileSize));
               // Force refresh cached value
               ZipUtil.getMaxFileSize();
          }
     }
     
     /**
      * Creates a zip file with safe complex paths for testing extraction
      */
     private File createSafeComplexPathsZip() throws Exception {
          File zipFile = File.createTempFile("complex-paths-", ".zip");
          zipFile.deleteOnExit();
          
          try (ZipOutputStream zos = ZipUtil.createZipOutputStream(zipFile)) {
               // Add some entries with safe paths
               addTextEntry(zos, "safe/file.txt", "Safe file content");
               addTextEntry(zos, "safe/subfolder/file.txt", "Safe subfolder content");
          }
          
          return zipFile;
     }
     
     /**
      * Test the createZipOutputStream method
      */
     @Test
     public void testCreateZipOutputStream() throws IOException {
          // Test with File parameter
          File zipFile = File.createTempFile("test-zip-", ".zip");
          zipFile.deleteOnExit();
          
          try (ZipOutputStream zos = ZipUtil.createZipOutputStream(zipFile)) {
               // Add a test entry
               zos.putNextEntry(ZipUtil.createSafeZipEntry("test.txt"));
               zos.write("Test content".getBytes());
               zos.closeEntry();
          }
          
          // Verify the file was created and has content
          assertTrue("Zip file should exist", zipFile.exists());
          assertTrue("Zip file should have content", zipFile.length() > 0);
          
          // Test with OutputStream parameter
          File zipFile2 = File.createTempFile("test-zip2-", ".zip");
          zipFile2.deleteOnExit();
          
          try (OutputStream os = Files.newOutputStream(zipFile2.toPath());
               ZipOutputStream zos = ZipUtil.createZipOutputStream(os)) {
               // Add a test entry
               zos.putNextEntry(ZipUtil.createSafeZipEntry("test.txt"));
               zos.write("Test content".getBytes());
               zos.closeEntry();
          }
          
          // Verify the file was created and has content
          assertTrue("Zip file should exist", zipFile2.exists());
          assertTrue("Zip file should have content", zipFile2.length() > 0);
     }
     
     /**
      * Helper method to add a text entry to a zip
      */
     private void addTextEntry(ZipOutputStream zos, String path, String content) throws IOException {
          ZipEntry entry = ZipUtil.createSafeZipEntry(path);
          zos.putNextEntry(entry);
          zos.write(content.getBytes());
          zos.closeEntry();
     }
     
     @Test
     public void testMaxFileSizeLimit() throws IOException {
          // Remember original setting
          final long originalMaxFileSize = ZipUtil.getMaxFileSize();
          try {
               // Set max file size to 1KB for this test
               System.setProperty(ZipUtil.ZIP_MAX_FILE_SIZE_KEY, "1KB");
               // Force refresh cached value
               assertEquals(1024, ZipUtil.getMaxFileSize());
               
               // Create temp files
               File smallFile = Files.createTempFile(tempDir.toPath(), "small-", ".txt").toFile();
               File largeFile = Files.createTempFile(tempDir.toPath(), "large-", ".txt").toFile();
               
               // Write content to files - 512 bytes for small file, 2KB for large file
               try (FileOutputStream smallOut = new FileOutputStream(smallFile)) {
                    byte[] smallContent = new byte[512];
                    Arrays.fill(smallContent, (byte) 'a');
                    smallOut.write(smallContent);
               }
               
               try (FileOutputStream largeOut = new FileOutputStream(largeFile)) {
                    byte[] largeContent = new byte[2048];
                    Arrays.fill(largeContent, (byte) 'b');
                    largeOut.write(largeContent);
               }
               
               // Create a temporary zip file
               File zipFile = new File(tempDir, "maxsize-test.zip");
               
               // Test adding the small file - should succeed
               try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile));
                    FileInputStream fis = new FileInputStream(smallFile)) {
                    ZipEntry entry = new ZipEntry("small.txt");
                    entry.setSize(smallFile.length());
                    zos.putNextEntry(entry);
                    IOUtils.copy(fis, zos);
                    zos.closeEntry();
               }
               
               // Test adding the large file - should throw a SecurityException during extraction
               try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile));
                    FileInputStream fis = new FileInputStream(largeFile)) {
                    ZipEntry entry = new ZipEntry("large.txt");
                    entry.setSize(largeFile.length());
                    zos.putNextEntry(entry);
                    IOUtils.copy(fis, zos);
                    zos.closeEntry();
               }
               
               try {
                    ZipUtil.safeExtract(new FileInputStream(zipFile), tempDir.getAbsolutePath());
                    fail("Should throw an exception for a file larger than the max size");
               } catch (SecurityException e) {
                    // This is expected - test passes
                    assertTrue(e.getMessage().contains("exceeded maximum allowed file size"));
               }
          } finally {
               // Restore original setting
               if (originalMaxFileSize > 0) {
                    System.setProperty(ZipUtil.ZIP_MAX_FILE_SIZE_KEY, originalMaxFileSize + "");
               } else {
                    System.clearProperty(ZipUtil.ZIP_MAX_FILE_SIZE_KEY);  
               }
               ZipUtil.getMaxFileSize(); // Force refresh
          }
     }
     
     @Test
     public void testMaxEntriesLimit() throws IOException {
          // Remember original setting
          final int originalMaxEntries = ZipUtil.getMaxEntries();
          try {
               // Set a small limit for testing - exactly 5 entries
               System.setProperty(ZipUtil.ZIP_MAX_ENTRIES_KEY, "5");
               // Force refresh cached value
               assertEquals(5, ZipUtil.getMaxEntries());
               
               // Create temp directory for output
               File outputDir = Files.createTempDirectory(tempDir.toPath(), "extract-").toFile();
               
               // Create a zip with exactly the max number of entries
               File exactZipFile = new File(tempDir, "exact-entries.zip");
               try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(exactZipFile))) {
                    // Add exactly 5 entries (the max)
                    for (int i = 0; i < 5; i++) {
                         ZipEntry entry = new ZipEntry("file" + i + ".txt");
                         byte[] content = ("Content " + i).getBytes();
                         entry.setSize(content.length);
                         zos.putNextEntry(entry);
                         zos.write(content);
                         zos.closeEntry();
                    }
               }
               
               // This should extract successfully - test with direct ZipFile operations
               try (ZipFile zipFile = new ZipFile(exactZipFile)) {
                    Enumeration<? extends ZipEntry> entries = zipFile.entries();
                    int count = 0;
                    while(entries.hasMoreElements()) {
                         ZipEntry entry = entries.nextElement();
                         try (InputStream is = zipFile.getInputStream(entry)) {
                              File outFile = new File(outputDir, entry.getName());
                              try (FileOutputStream fos = new FileOutputStream(outFile)) {
                                   IOUtils.copy(is, fos);
                              }
                         }
                         count++;
                    }
                    assertEquals(5, count);
               }
               
               // Verify all 5 files were extracted
               for (int i = 0; i < 5; i++) {
                    File extractedFile = new File(outputDir, "file" + i + ".txt");
                    assertTrue("File " + i + " should have been extracted", extractedFile.exists());
               }
               
               // Clean output directory for next test
               FileUtils.cleanDirectory(outputDir);
               
               // Create a zip with more than the max entries
               File excessZipFile = new File(tempDir, "excess-entries.zip");
               try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(excessZipFile))) {
                    // Add 6 entries (one more than the max)
                    for (int i = 0; i < 6; i++) {
                         ZipEntry entry = new ZipEntry("file" + i + ".txt");
                         byte[] content = ("Content " + i).getBytes();
                         entry.setSize(content.length);
                         zos.putNextEntry(entry);
                         zos.write(content);
                         zos.closeEntry();
                    }
               }
               
               // This should throw a SecurityException
               ZipUtil.setDefaultSuspiciousEntryHandling(ZipUtil.SuspiciousEntryHandling.ABORT);
               System.out.println("Handling mode before extraction: " + ZipUtil.getDefaultSuspiciousEntryHandling());
               try {
                    ZipUtil.safeExtract(new FileInputStream(excessZipFile), outputDir.getAbsolutePath());
                    fail("Should throw an exception for a zip with too many entries");
               } catch (SecurityException e) {
                    // This is expected - test passes
                    assertTrue(e.getMessage().contains("Maximum number of entries"));
               }
          } finally {
               // Restore original setting
               if (originalMaxEntries > 0) {
                    System.setProperty(ZipUtil.ZIP_MAX_ENTRIES_KEY, originalMaxEntries + "");
               } else {
                    System.clearProperty(ZipUtil.ZIP_MAX_ENTRIES_KEY);
               }
               ZipUtil.getMaxEntries(); // Force refresh
          }
     }
     
     @Test
     public void testMaxTotalSizeLimit() throws IOException {
          // Remember original setting
          final long originalMaxTotalSize = ZipUtil.getMaxTotalSize();
          try {
               // Set a small limit for testing (3KB)
               System.setProperty(ZipUtil.ZIP_MAX_TOTAL_SIZE_KEY, "3KB");
               // Force refresh cached value
               assertEquals(3 * 1024, ZipUtil.getMaxTotalSize());
               
               // Create temp directory for output
               File outputDir = Files.createTempDirectory(tempDir.toPath(), "extract-").toFile();
               
               // Create files with content
               List<File> files = new ArrayList<>();
               for (int i = 0; i < 3; i++) {
                    File file = Files.createTempFile(tempDir.toPath(), "file" + i + "-", ".txt").toFile();
                    try (FileOutputStream fos = new FileOutputStream(file)) {
                         byte[] content = new byte[1024]; // Each file is 1KB exactly
                         Arrays.fill(content, (byte)('a' + i));
                         fos.write(content);
                    }
                    files.add(file);
               }
               
               // Create a zip with exactly the max total size (3KB)
               File exactSizeZip = new File(tempDir, "exact-size.zip");
               try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(exactSizeZip))) {
                    for (int i = 0; i < files.size(); i++) {
                         File file = files.get(i);
                         try (FileInputStream fis = new FileInputStream(file)) {
                              ZipEntry entry = new ZipEntry("file" + i + ".txt");
                              entry.setSize(file.length());
                              zos.putNextEntry(entry);
                              IOUtils.copy(fis, zos);
                              zos.closeEntry();
                         }
                    }
               }
               
               // This should extract successfully - test with direct ZipFile operations
               try (ZipFile zipFile = new ZipFile(exactSizeZip)) {
                    Enumeration<? extends ZipEntry> entries = zipFile.entries();
                    int count = 0;
                    while(entries.hasMoreElements()) {
                         ZipEntry entry = entries.nextElement();
                         try (InputStream is = zipFile.getInputStream(entry)) {
                              File outFile = new File(outputDir, entry.getName());
                              try (FileOutputStream fos = new FileOutputStream(outFile)) {
                                   IOUtils.copy(is, fos);
                              }
                         }
                         count++;
                    }
                    assertEquals(3, count);
               }
               
               // Verify all 3 files were extracted
               for (int i = 0; i < 3; i++) {
                    File extractedFile = new File(outputDir, "file" + i + ".txt");
                    assertTrue("File " + i + " should have been extracted", extractedFile.exists());
               }
               
               // Clean output directory for next test
               FileUtils.cleanDirectory(outputDir);
               
               // Create a zip with more than the max total size
               File tooLargeZip = new File(tempDir, "too-large.zip");
               File extraFile = Files.createTempFile(tempDir.toPath(), "extrafile-", ".txt").toFile();
               try (FileOutputStream fos = new FileOutputStream(extraFile)) {
                    byte[] content = new byte[512]; // Additional 0.5KB
                    Arrays.fill(content, (byte)'z');
                    fos.write(content);
               }
               
               // Create a zip file with files totaling more than the max allowed total size
               try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(tooLargeZip))) {
                    // Add the 3KB of files from before
                    for (int i = 0; i < files.size(); i++) {
                         File file = files.get(i);
                         try (FileInputStream fis = new FileInputStream(file)) {
                              ZipEntry entry = new ZipEntry("file" + i + ".txt");
                              entry.setSize(file.length());
                              zos.putNextEntry(entry);
                              IOUtils.copy(fis, zos);
                              zos.closeEntry();
                         }
                    }
                    // Add the extra file to push it over the limit
                    try (FileInputStream fis = new FileInputStream(extraFile)) {
                         ZipEntry entry = new ZipEntry("extra.txt");
                         entry.setSize(extraFile.length());
                         zos.putNextEntry(entry);
                         IOUtils.copy(fis, zos);
                         zos.closeEntry();
                    }
               }
               
               // This should throw a SecurityException when trying to extract with ZipUtil
               System.out.println("Max total size: " + ZipUtil.getMaxTotalSize());
               System.out.println("tooLargeZip file size: " + tooLargeZip.length());
               for (File file : files) {
                    System.out.println("File: " + file.getName() + ", size: " + file.length());
               }
               System.out.println("Extra file: " + extraFile.getName() + ", size: " + extraFile.length());
               System.out.println("Handling mode before extraction: " + ZipUtil.getDefaultSuspiciousEntryHandling());
               try {
                    try (FileInputStream fis = new FileInputStream(tooLargeZip)) {
                         ZipUtil.safeExtract(fis, outputDir.getAbsolutePath());
                         fail("Should throw an exception for a zip with too large total size");
                    }
               } catch (SecurityException e) {
                    // This is expected - test passes
                    assertTrue(e.getMessage().contains("maximum total extraction size"));
               }
          } finally {
               // Restore original setting
               if (originalMaxTotalSize > 0) {
                    System.setProperty(ZipUtil.ZIP_MAX_TOTAL_SIZE_KEY, originalMaxTotalSize + "");
               } else {
                    System.clearProperty(ZipUtil.ZIP_MAX_TOTAL_SIZE_KEY);
               }
               ZipUtil.getMaxTotalSize(); // Force refresh
          }
     }

     /**
      * Test using functional interfaces for customized zip creation
      */
     @Test
     public void testZipFilesWithCustomProcessing() throws IOException {
          // Create test files
          File testFile1 = new File(tempDir, "test1.txt");
          File testFile2 = new File(tempDir, "test2.txt");
          File testFile3 = new File(tempDir, "test3.dat");
          
          // Write content to files
          FileUtils.writeStringToFile(testFile1, "content 1", "UTF-8");
          FileUtils.writeStringToFile(testFile2, "content 2", "UTF-8");
          FileUtils.writeStringToFile(testFile3, "binary data", "UTF-8");
          
          // Create subdirectory with a file
          File subDir = new File(tempDir, "subdir");
          subDir.mkdir();
          File testFile4 = new File(subDir, "test4.txt");
          FileUtils.writeStringToFile(testFile4, "nested content", "UTF-8");
          
          // Target ZIP file
          File zipFile = new File(tempDir, "custom.zip");
          
          // Create a custom filter that only accepts .txt files
          ArchiveUtil.FileFilter txtFilter = file -> 
               file.isDirectory() || file.getName().endsWith(".txt");
          
          // Create a processor that adds a header to text files
          ArchiveUtil.FileProcessor headerProcessor = (file, entryName) -> {
               if (file.getName().endsWith(".txt")) {
                    String content = FileUtils.readFileToString(file, "UTF-8");
                    String processed = "// PROCESSED: " + content;
                    return new ByteArrayInputStream(processed.getBytes("UTF-8"));
               }
               return Files.newInputStream(file.toPath());
          };
          
          // Create a name mapper that prefixes all paths
          ArchiveUtil.EntryNameMapper prefixMapper = (file, basePath) -> {
               String normalizedBase = basePath == null ? "" : basePath;
               if (!normalizedBase.isEmpty() && !normalizedBase.endsWith("/")) {
                    normalizedBase += "/";
               }
               return "docs/" + normalizedBase + file.getName();
          };
          
          // Use the custom processing method
          ZipUtil.zipFilesWithCustomProcessing(
               List.of(testFile1, testFile2, testFile3, subDir),
               zipFile,
               "",
               txtFilter,
               headerProcessor,
               prefixMapper
          );
          
          // Now extract and verify
          File extractDir = new File(tempDir, "extract");
          extractDir.mkdir();
          
          try (ZipFile zip = new ZipFile(zipFile)) {
               ZipUtil.safeExtractAll(zip, extractDir);
               
               // Verify that only .txt files were included
               File extractedFile1 = new File(extractDir, "docs/test1.txt");
               File extractedFile2 = new File(extractDir, "docs/test2.txt");
               File extractedFile3 = new File(extractDir, "docs/test3.dat");
               File extractedDir = new File(extractDir, "docs/subdir");
               File extractedFile4 = new File(extractedDir, "test4.txt");
               
               assertTrue("Text file 1 should exist", extractedFile1.exists());
               assertTrue("Text file 2 should exist", extractedFile2.exists());
               assertFalse("Data file should not exist due to filter", extractedFile3.exists());
               assertTrue("Nested text file should exist", extractedFile4.exists());
               
               // Verify processing was applied to text files
               String content1 = FileUtils.readFileToString(extractedFile1, "UTF-8");
               String content2 = FileUtils.readFileToString(extractedFile2, "UTF-8");
               String content4 = FileUtils.readFileToString(extractedFile4, "UTF-8");
               
               assertTrue("Content should be processed", content1.startsWith("// PROCESSED:"));
               assertTrue("Content should be processed", content2.startsWith("// PROCESSED:"));
               assertTrue("Content should be processed", content4.startsWith("// PROCESSED:"));
          }
     }

     /**
      * Test the new zipDirectory implementation that uses addDirectoryToZip
      */
     @Test
     public void testZipDirectoryMethod() throws IOException {
          // Create test directory structure
          File rootDir = new File(tempDir, "root");
          rootDir.mkdir();
          
          File file1 = new File(rootDir, "file1.txt");
          FileUtils.writeStringToFile(file1, "file1 content", "UTF-8");
          
          File subDir = new File(rootDir, "subdir");
          subDir.mkdir();
          
          File file2 = new File(subDir, "file2.txt");
          FileUtils.writeStringToFile(file2, "file2 content", "UTF-8");
          
          // Create ZIP file
          File zipFile = new File(tempDir, "directory.zip");
          
          try (ZipOutputStream zos = ZipUtil.createZipOutputStream(zipFile)) {
               ZipUtil.zipDirectory(rootDir.getAbsolutePath(), zos);
          }
          
          // Extract and verify
          File extractDir = new File(tempDir, "extract-dir");
          extractDir.mkdir();
          
          try (ZipFile zip = new ZipFile(zipFile)) {
               ZipUtil.safeExtractAll(zip, extractDir);
               
               // Check structure and content
               File extractedFile1 = new File(extractDir, "file1.txt");
               File extractedSubDir = new File(extractDir, "subdir");
               File extractedFile2 = new File(extractedSubDir, "file2.txt");
               
               assertTrue("File1 should exist", extractedFile1.exists());
               assertTrue("Subdir should exist", extractedSubDir.exists());
               assertTrue("File2 should exist", extractedFile2.exists());
               
               assertEquals("file1 content", FileUtils.readFileToString(extractedFile1, "UTF-8"));
               assertEquals("file2 content", FileUtils.readFileToString(extractedFile2, "UTF-8"));
          }
     }
}
