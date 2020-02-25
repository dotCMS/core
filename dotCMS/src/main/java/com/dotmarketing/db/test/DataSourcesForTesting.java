package com.dotmarketing.db.test;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;

import javax.sql.DataSource;

import com.dotcms.repackage.org.apache.commons.dbcp.BasicDataSource;
import com.dotmarketing.business.DotStateException;
import com.dotcms.rendering.velocity.viewtools.XmlTool;
import com.liferay.util.FileUtil;

/**
 * @deprecated This class is not being used
 */
@Deprecated
public class DataSourcesForTesting {

   final File contextFile;

   public DataSourcesForTesting(File useMe) {
      this.contextFile = useMe;
   }

   public DataSourcesForTesting() {
      this.contextFile = findContextFile();
   }

   public List<DataSource> dataSources() throws FileNotFoundException, Exception {


      List<DataSource> sources = new ArrayList<DataSource>();

      XmlTool xml = new XmlTool().parse(getContextAsString());
      xml = xml.children().find("Resource");
      Iterator<XmlTool> it = xml.iterator();
      while (it.hasNext()) {
         XmlTool tool = it.next();
         if (tool.attr("name").equals("jdbc/dotCMSPool")) {
            String driver = tool.attr("driverClassName");
            String url = tool.attr("url");
            String username = tool.attr("username");
            String password = tool.attr("password");
            sources.add(buildDS(driver, url, username, password));
         }
      }

      return sources;

   }



   private static DataSource buildDS(String driver, String url, String username,
         String password) throws Exception {
      BasicDataSource dataSource = new BasicDataSource();
      dataSource.setDriverClassName(driver);
      dataSource.setUrl(url);
      dataSource.setUsername(username);
      dataSource.setPassword(password);
      dataSource.setMaxActive(100);
      return dataSource;
   }

   private File findContextFile() {

      FileFilter ff = new FileFilter() {
         @Override
         public boolean accept(File pathname) {
            return ("context.xml".equals(pathname.getName()) || pathname.isDirectory());
         }
      };

      File root = findProjectRoot(new File("."), 0);

      List<File> files;

       files = FileUtil.listFilesRecursively(root, ff);

       for (File f : files) {
          if (f.isFile()) {
             return f;
          }
       }

      throw new DotStateException("cannot find context.xml");
   }


   private String getContextAsString() throws FileNotFoundException {


      System.out.println("Using context file:" + this.contextFile.getAbsolutePath());
      try (Scanner scan = new Scanner(this.contextFile)) {
         return scan.useDelimiter("\\Z").next();
      }


   }


   private File findProjectRoot(File start, int i) {
      File[] files = start.listFiles();
      for (File file : files) {
         if (file.isDirectory() && "META-INF".equals(file.getName())) {
            return file;
         }
         if (file.isDirectory() && "extra".equals(file.getName())) {
            return file;
         }
      }
      File parent = start.getParentFile();
      if (parent == null || parent.equals(start) || i > 500) {
         throw new DotStateException("Unable to find the directory root");
      }
      return findProjectRoot(start.getParentFile(), ++i);
   }



}
