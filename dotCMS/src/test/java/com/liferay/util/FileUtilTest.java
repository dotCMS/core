package com.liferay.util;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.liferay.util.FileUtil;

public class FileUtilTest {

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
      assert(FileUtil.listFilesRecursively(tempDir).size()==0);
      
      for(int i=0;i<5;i++) {
        File parent = new File(tempDir,"folder" + i);
        parent.mkdirs();
        File child = new File(parent,"file" + i);
        new FileOutputStream(child).close();
        child.setLastModified(hours.get(i).getTime());
        parent.setLastModified(hours.get(i).getTime());
      }
      
      // create an old folder with a new file in it
      File parent = new File(tempDir,"folderx");
      parent.mkdirs();
   
      File child = new File(parent,"filex");
      new FileOutputStream(child).close();
      child.setLastModified(System.currentTimeMillis());
      parent.setLastModified(hours.get(4).getTime());
      
      assert(FileUtil.listFilesRecursively(tempDir).size()==12);
      
      FileUtil.cleanTree(tempDir,hours.get(3));
      
      assert(FileUtil.listFilesRecursively(tempDir).size()==10);
    
      FileUtil.cleanTree(tempDir,hours.get(2));
      
      
      assert(FileUtil.listFilesRecursively(tempDir).size()==8);
      
      FileUtil.cleanTree(tempDir,hours.get(0));
      assert(FileUtil.listFilesRecursively(tempDir).size()==4);
      
    
  }

}
