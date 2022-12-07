package com.dotmarketing.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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
