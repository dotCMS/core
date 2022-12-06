package com.dotmarketing.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

public class ZipUtilTest  {


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


     @Test(expected = SecurityException.class)
     public void testUnzipInvalidInputStream() throws Exception {
          final File evilZipFile = createEvilZip();
          final File tmpDir = com.google.common.io.Files.createTempDir();
          try (InputStream in = Files.newInputStream(evilZipFile.toPath())) {
               ZipUtil.extract(in, tmpDir.getAbsolutePath());
          }
     }

     /**
      * Simply test that we cn only unzip under the properly configured out dir
      * Here we expect success
      * @throws IOException
      */
     @Test
     public void testUnzipUtilSecurityCheckHappyScenario() throws IOException {
          final long time = java.lang.System.nanoTime();
          final File zipEntry = File.createTempFile("zip-entry-" + time, ".txt");
          FileUtils.writeStringToFile(zipEntry, "" + System.currentTimeMillis(),
                  Charset.defaultCharset());
          final File zip = File.createTempFile("zip-test" + time, ".zip");
          makeZip(zipEntry, zip.getCanonicalPath());
          final String destinationDir = ConfigUtils.getIntegrityPath();
          try (
                  final FileInputStream inputStream = new FileInputStream(zip);
          ) {
               ZipUtil.extract(inputStream, destinationDir);
          }
     }

     /**
      * Good zip
      * @param file
      * @param zipFileName
      * @throws IOException
      */
     private static void makeZip(final File file, final String zipFileName) throws IOException {

          try (
                  //create ZipOutputStream to write to the zip file
                  FileOutputStream fos = new FileOutputStream(zipFileName);
                  ZipOutputStream zos = new ZipOutputStream(fos);
          ) {
               //add a new Zip Entry to the ZipOutputStream
               ZipEntry ze = new ZipEntry(file.getName());
               zos.putNextEntry(ze);
               //read the file and write to ZipOutputStream

               try (FileInputStream fis = new FileInputStream(file);) {
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = fis.read(buffer)) > 0) {
                         zos.write(buffer, 0, len);
                    }
               }
          }
     }

     /**
      * Creates a zip file that includes a file that attempts a directory traversal
      * @return
      * @throws Exception
      */
     File createEvilZip() throws Exception {

          final File tmpDir = com.google.common.io.Files.createTempDir();
          final File goodFile = new File(tmpDir, System.currentTimeMillis() + ".tmp");

          FileUtils.touch(goodFile);

          final String badFileName = "../../../../../../../../../../tmp/" + System.currentTimeMillis() + ".tmp";
          File badFile = new File(badFileName);

          FileUtils.touch(badFile);

          final List<File> files = List.of(tmpDir, goodFile, badFile);

          final File badZipFile = File.createTempFile("badzip-", ".zip");
          try (ZipOutputStream zout = new ZipOutputStream(Files.newOutputStream(badZipFile.toPath()))) {

               for (File file : files) {
                    final ZipEntry ze = file.getPath().contains("..") ? new ZipEntry(file.getPath()) : new ZipEntry(file.getName());
                    zout.putNextEntry(ze);
                    zout.closeEntry();
               }
          }
          badZipFile.deleteOnExit();
          Logger.info(ZipUtilTest.class, "created bad zip:" + badZipFile);
          return badZipFile;
     }


}
