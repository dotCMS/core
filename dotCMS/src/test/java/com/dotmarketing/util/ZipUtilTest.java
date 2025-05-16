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
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.After;

public class ZipUtilTest  {

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
          final File evilZipFile = createEvilZip();
          final File tmpDir = com.google.common.io.Files.createTempDir();
          try {
               boolean exceptionThrown = false;
               try (InputStream in = Files.newInputStream(evilZipFile.toPath())) {
                    ZipUtil.safeExtract(in, tmpDir.getAbsolutePath());
                    fail("Expected SecurityException when extracting malicious zip file");
               } catch (SecurityException e) {
                    // Expected - this is good
                    exceptionThrown = true;
                    // Verify the exception message indicates a problem with the path
                    assertTrue(e.getMessage().contains("Illegal zip entry path"));
               }
               // Verify the exception was thrown
               assertTrue("Expected SecurityException was not thrown", exceptionThrown);
               
               // Also verify no files were actually extracted to the parent directory
               File parentDir = tmpDir.getParentFile();
               File potentiallyExtractedFile = new File(parentDir, "evil.txt");
               assertFalse("File should not have been extracted outside target directory", 
                           potentiallyExtractedFile.exists());
          } finally {
               FileUtils.deleteDirectory(tmpDir);
          }
     }

     /**
      * Test that an invalid zip input stream can be handled with SKIP_AND_CONTINUE mode
      */
     @Test
     public void testUnzipInvalidInputStreamWithSkipMode() throws Exception {
          final File evilZipFile = createMixedZip();
          final File tmpDir = com.google.common.io.Files.createTempDir();
          try {
               // Temporarily change the handling mode
               ZipUtil.SuspiciousEntryHandling originalMode = ZipUtil.getDefaultSuspiciousEntryHandling();
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
               } finally {
                    // Restore original handling mode
                    ZipUtil.setDefaultSuspiciousEntryHandling(originalMode);
               }
          } finally {
               FileUtils.deleteDirectory(tmpDir);
          }
     }
     
     /**
      * Test explicitly specifying handling mode for a specific operation
      */
     @Test
     public void testExplicitHandlingMode() throws Exception {
          final File evilZipFile = createMixedZip();
          final File tmpDir = com.google.common.io.Files.createTempDir();
          try {
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
               FileUtils.deleteDirectory(tmpDir);
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
          assertEquals("path/file.txt", ZipUtil.sanitizePath("path/file.txt"));
          assertEquals("path/file.txt", ZipUtil.sanitizePath("path//file.txt"));
          assertEquals("file.txt", ZipUtil.sanitizePath("./file.txt"));
          
          // Test potentially malicious paths - these should throw SecurityExceptions now
          try {
               ZipUtil.sanitizePath("/path/to/file.txt");
               fail("Should have thrown SecurityException for path with leading slash");
          } catch (SecurityException e) {
               // Expected
          }
          
          try {
               ZipUtil.sanitizePath("../safe/file.txt");
               fail("Should have thrown SecurityException for path with traversal");
          } catch (SecurityException e) {
               // Expected
          }
          
          try {
               ZipUtil.sanitizePath("../../../../safe/file.txt");
               fail("Should have thrown SecurityException for path with multiple traversals");
          } catch (SecurityException e) {
               // Expected
          }
          
          try {
               ZipUtil.sanitizePath("safe/../../safe/file.txt");
               fail("Should have thrown SecurityException for path with embedded traversals");
          } catch (SecurityException e) {
               // Expected
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
          // Create a zip file with complex but safe paths only
          File zipFile = createSafeComplexPathsZip();
          
          try {
               try (ZipFile zf = new ZipFile(zipFile)) {
                    // Extract using our safe method
                    ZipUtil.safeExtractAll(zf, tempDir);
                    
                    // Verify files were extracted with proper paths
                    assertTrue(new File(tempDir, "safe/file.txt").exists());
                    assertTrue(new File(tempDir, "safe/subfolder/file.txt").exists());
               }
          } finally {
               zipFile.delete();
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
          // Create a temporary directory
          Path tempDir = Files.createTempDirectory("zip-max-size-test");
          File tmpDir = tempDir.toFile();
          
          // Create a temporary ZIP file
          File zipFile = new File(tempDir.toFile(), "test-max-file-size.zip");
          
          try {
               // Create a large file content exceeding the limit
               // We're only creating the mock zip structure, not actual large content
               final long originalMaxFileSize = ZipUtil.getMaxFileSize();
               
               try {
                    // Configure a smaller limit for testing
                    System.setProperty(ZipUtil.ZIP_MAX_FILE_SIZE_KEY, "1024"); // 1KB limit
                    
                    // Create a ZIP with a file exceeding the limit
                    try (ZipOutputStream zos = ZipUtil.createZipOutputStream(zipFile)) {
                         ZipEntry entry = ZipUtil.createSafeZipEntry("large-file.dat");
                         // Set size to exceed the limit - JDK allows this for creating test cases
                         entry.setSize(2048); // 2KB, exceeding our 1KB limit
                         zos.putNextEntry(entry);
                         // Write some content (but not 2KB, it's just for the test metadata)
                         zos.write(new byte[10]);
                         zos.closeEntry();
                    }
                    
                    // Try to extract - should throw SecurityException in ABORT mode
                    try {
                         ZipUtil.setDefaultSuspiciousEntryHandling(ZipUtil.SuspiciousEntryHandling.ABORT);
                         ZipUtil.safeExtract(new FileInputStream(zipFile), tmpDir.getAbsolutePath());
                         fail("Should have thrown SecurityException due to max file size");
                    } catch (SecurityException e) {
                         // Expected exception
                         assertTrue(e.getMessage().contains("exceeds maximum allowed file size"));
                    }
                    
                    // Try with SKIP_AND_CONTINUE - should not extract but not throw exception
                    ZipUtil.setDefaultSuspiciousEntryHandling(ZipUtil.SuspiciousEntryHandling.SKIP_AND_CONTINUE);
                    ZipUtil.safeExtract(new FileInputStream(zipFile), tmpDir.getAbsolutePath());
                    
                    // Verify file wasn't extracted
                    assertFalse(new File(tmpDir, "large-file.dat").exists());
               } finally {
                    // Restore original setting
                    System.setProperty(ZipUtil.ZIP_MAX_FILE_SIZE_KEY, String.valueOf(originalMaxFileSize));
               }
          } finally {
               // Clean up
               FileUtils.deleteDirectory(tmpDir);
          }
     }
     
     @Test
     public void testMaxEntriesLimit() throws IOException {
          // Create a temporary directory
          Path tempDir = Files.createTempDirectory("zip-max-entries-test");
          File tmpDir = tempDir.toFile();
          
          // Create a temporary ZIP file
          File zipFile = new File(tempDir.toFile(), "test-max-entries.zip");
          
          try {
               // Store original value
               final int originalMaxEntries = ZipUtil.getMaxEntries();
               
               try {
                    // Configure a smaller limit for testing
                    System.setProperty(ZipUtil.ZIP_MAX_ENTRIES_KEY, "5"); // 5 entries limit
                    
                    // Create a ZIP with more than the limit of entries
                    try (ZipOutputStream zos = ZipUtil.createZipOutputStream(zipFile)) {
                         for (int i = 0; i < 10; i++) { // 10 entries, exceeding our 5 limit
                              ZipEntry entry = ZipUtil.createSafeZipEntry("file" + i + ".txt");
                              zos.putNextEntry(entry);
                              zos.write(("Content " + i).getBytes());
                              zos.closeEntry();
                         }
                    }
                    
                    // Try to extract - should throw SecurityException in ABORT mode
                    try {
                         ZipUtil.setDefaultSuspiciousEntryHandling(ZipUtil.SuspiciousEntryHandling.ABORT);
                         ZipUtil.safeExtract(new FileInputStream(zipFile), tmpDir.getAbsolutePath());
                         fail("Should have thrown SecurityException due to max entries exceeded");
                    } catch (SecurityException e) {
                         // Expected exception
                         assertTrue(e.getMessage().contains("Maximum number of entries"));
                    }
                    
                    // Try with SKIP_AND_CONTINUE - should extract only up to the limit
                    ZipUtil.setDefaultSuspiciousEntryHandling(ZipUtil.SuspiciousEntryHandling.SKIP_AND_CONTINUE);
                    ZipUtil.safeExtract(new FileInputStream(zipFile), tmpDir.getAbsolutePath());
                    
                    // Count extracted files - should be less than or equal to the limit
                    File[] extractedFiles = tmpDir.listFiles(file -> file.isFile() && file.getName().startsWith("file"));
                    assertTrue(extractedFiles.length > 0 && extractedFiles.length <= 5);
                    
               } finally {
                    // Restore original setting
                    System.setProperty(ZipUtil.ZIP_MAX_ENTRIES_KEY, String.valueOf(originalMaxEntries));
               }
          } finally {
               // Clean up
               FileUtils.deleteDirectory(tmpDir);
          }
     }
     
     @Test
     public void testMaxTotalSizeLimit() throws IOException {
          // Create a temporary directory
          Path tempDir = Files.createTempDirectory("zip-max-total-size-test");
          File tmpDir = tempDir.toFile();
          
          // Create a temporary ZIP file
          File zipFile = new File(tempDir.toFile(), "test-max-total-size.zip");
          
          try {
               // Store original value
               final long originalMaxTotalSize = ZipUtil.getMaxTotalSize();
               
               try {
                    // Configure a smaller limit for testing
                    System.setProperty(ZipUtil.ZIP_MAX_TOTAL_SIZE_KEY, "2048"); // 2KB total limit
                    
                    // Create a ZIP with total size exceeding the limit
                    try (ZipOutputStream zos = ZipUtil.createZipOutputStream(zipFile)) {
                         // First file (1KB)
                         ZipEntry entry1 = ZipUtil.createSafeZipEntry("file1.txt");
                         entry1.setSize(1024);
                         zos.putNextEntry(entry1);
                         zos.write(new byte[10]); // Just some content
                         zos.closeEntry();
                         
                         // Second file (1KB)
                         ZipEntry entry2 = ZipUtil.createSafeZipEntry("file2.txt");
                         entry2.setSize(1024);
                         zos.putNextEntry(entry2);
                         zos.write(new byte[10]); // Just some content
                         zos.closeEntry();
                         
                         // Third file (1KB) - this one should exceed the total limit
                         ZipEntry entry3 = ZipUtil.createSafeZipEntry("file3.txt");
                         entry3.setSize(1024);
                         zos.putNextEntry(entry3);
                         zos.write(new byte[10]); // Just some content
                         zos.closeEntry();
                    }
                    
                    // Try to extract - should throw SecurityException in ABORT mode
                    try {
                         ZipUtil.setDefaultSuspiciousEntryHandling(ZipUtil.SuspiciousEntryHandling.ABORT);
                         ZipUtil.safeExtract(new FileInputStream(zipFile), tmpDir.getAbsolutePath());
                         fail("Should have thrown SecurityException due to max total size");
                    } catch (SecurityException e) {
                         // Expected exception
                         assertTrue(e.getMessage().contains("maximum total extraction size"));
                    }
                    
                    // Try with SKIP_AND_CONTINUE - should extract only up to the limit
                    ZipUtil.setDefaultSuspiciousEntryHandling(ZipUtil.SuspiciousEntryHandling.SKIP_AND_CONTINUE);
                    ZipUtil.safeExtract(new FileInputStream(zipFile), tmpDir.getAbsolutePath());
                    
                    // Check what was extracted - should be at least some files but not all
                    assertTrue(new File(tmpDir, "file1.txt").exists());
                    assertTrue(new File(tmpDir, "file2.txt").exists());
                    // The third file should not exist as it would exceed the total size limit
                    assertFalse(new File(tmpDir, "file3.txt").exists());
                    
               } finally {
                    // Restore original setting
                    System.setProperty(ZipUtil.ZIP_MAX_TOTAL_SIZE_KEY, String.valueOf(originalMaxTotalSize));
               }
          } finally {
               // Clean up
               FileUtils.deleteDirectory(tmpDir);
          }
     }
}
