package com.dotmarketing.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.dotcms.UnitTestBase;
import java.io.File;
import java.io.IOException;
import org.junit.Test;

public class ZipUtilTest extends UnitTestBase {


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


}
