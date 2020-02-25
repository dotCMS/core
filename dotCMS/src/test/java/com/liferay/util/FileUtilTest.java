package com.liferay.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import com.dotcms.util.CloseUtils;
import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import static org.junit.Assert.assertEquals;

public class FileUtilTest {

    @Test
    public void test_uncompress_outputstream()  throws Exception {

        final File file = com.dotmarketing.util.FileUtil.createTemporalFile("nocompre_");
        final OutputStream out = FileUtil.createOutputStream(file);

        final String  string = "testuncompress";
        final byte [] bytes  = string.getBytes();
        out.write(bytes);
        CloseUtils.closeQuietly(out);

        final InputStream in   = FileUtil.createInputStream(file);
        final byte [] buff     = new byte[bytes.length];
        in.read(buff);
        CloseUtils.closeQuietly(in);

        Assert.assertEquals("The file recovery should be the file saved", new String(buff), string);
    }

    @Test
    public void test_compress_gzip_outputstream()  throws Exception {

        final File file = com.dotmarketing.util.FileUtil.createTemporalFile("gzip");
        final OutputStream out = FileUtil.createOutputStream(file, "gzip");

        final String  string = "testcompress";
        final byte [] bytes  = string.getBytes();
        out.write(bytes);
        CloseUtils.closeQuietly(out);

        final InputStream in   = FileUtil.createInputStream(file, "gzip");
        final byte [] buff     = new byte[bytes.length];
        in.read(buff);
        CloseUtils.closeQuietly(in);

        Assert.assertEquals("The file recovery should be the file saved", new String(buff), string);
    }

    @Test
    public void test_compress_gzip_enum_outputstream()  throws Exception {

        final File file = com.dotmarketing.util.FileUtil.createTemporalFile("egzip");
        final OutputStream out = FileUtil.createOutputStream(file.toPath(), FileUtil.StreamCompressorType.GZIP);

        final String  string = "testcompress";
        final byte [] bytes  = string.getBytes();
        out.write(bytes);
        CloseUtils.closeQuietly(out);

        final InputStream in   = FileUtil.createInputStream(file.toPath(), FileUtil.StreamCompressorType.GZIP);
        final byte [] buff     = new byte[bytes.length];
        in.read(buff);
        CloseUtils.closeQuietly(in);

        Assert.assertEquals("The file recovery should be the file saved", new String(buff), string);
    }

    @Test
    public void test_compress_bzip2_outputstream()  throws Exception {

        final File file = com.dotmarketing.util.FileUtil.createTemporalFile("bzip2");
        final OutputStream out = FileUtil.createOutputStream(file, "bzip2");

        final String  string = "testcompress";
        final byte [] bytes  = string.getBytes();
        out.write(bytes);
        CloseUtils.closeQuietly(out);

        final InputStream in   = FileUtil.createInputStream(file, "bzip2");
        final byte [] buff     = new byte[bytes.length];
        in.read(buff);
        CloseUtils.closeQuietly(in);

        Assert.assertEquals("The file recovery should be the file saved", new String(buff), string);
    }

    @Test
    public void test_compress_bzip2_enum_outputstream()  throws Exception {

        final File file = com.dotmarketing.util.FileUtil.createTemporalFile("ebzip2");
        final OutputStream out = FileUtil.createOutputStream(file.toPath(), FileUtil.StreamCompressorType.BZIP2);

        final String  string = "testcompress";
        final byte [] bytes  = string.getBytes();
        out.write(bytes);
        CloseUtils.closeQuietly(out);

        final InputStream in   = FileUtil.createInputStream(file.toPath(), FileUtil.StreamCompressorType.BZIP2);
        final byte [] buff     = new byte[bytes.length];
        in.read(buff);
        CloseUtils.closeQuietly(in);

        Assert.assertEquals("The file recovery should be the file saved", new String(buff), string);
    }

    @Test
  public void test_clean_tree_deletes_folders_files_by_date()  throws Exception {
      
      final Calendar cal = Calendar.getInstance();
      cal.set(Calendar.MILLISECOND, 0);
      cal.set(Calendar.SECOND, 0);
      
      List<Date> hours = ImmutableList.of(
          cal.getTime(),                                     // now
          new Date(cal.getTimeInMillis()-(60*60*1000)),      // 1 hour ago
          new Date(cal.getTimeInMillis()-(2*60*60*1000)),    // 2 hours ago
          new Date(cal.getTimeInMillis()-(5*60*60*1000)),    // 5 hours ago
          new Date(cal.getTimeInMillis()-(10*60*60*1000)));  // 10 hours ago

      final File tempDir =  Files.createTempDirectory("temp").toFile();
      
      tempDir.mkdirs();
      FileUtil.deltree(tempDir, false);
      // a clean start
      assertEquals(0,FileUtil.listFilesRecursively(tempDir).size());
      
      for(int i=0;i<5;i++) {
        final File parent = new File(tempDir,"folder" + i);
        parent.mkdirs();
        final File child = new File(parent,"file" + i);
        new FileOutputStream(child).close();
        child.setLastModified(hours.get(i).getTime());
        parent.setLastModified(hours.get(i).getTime());
      }
      
      // create an old folder with a new file in it
      File parent = new File(tempDir,"folderxx");
      parent.mkdirs();
   
      File child = new File(parent,"filex");
      new FileOutputStream(child).close();
      child.setLastModified(System.currentTimeMillis());
      parent.setLastModified(hours.get(4).getTime());
      
      assertEquals(12,FileUtil.listFilesRecursively(tempDir).size());
      
      FileUtil.cleanTree(tempDir,hours.get(3));
      
      assertEquals(10,FileUtil.listFilesRecursively(tempDir).size());
    
      FileUtil.cleanTree(tempDir,hours.get(2));


      assertEquals(8,FileUtil.listFilesRecursively(tempDir).size());
      
      FileUtil.cleanTree(tempDir,hours.get(0));
      assertEquals(4,FileUtil.listFilesRecursively(tempDir).size());
      
    
  }

}
