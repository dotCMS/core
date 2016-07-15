package com.dotmarketing.db.test;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;

import javax.sql.DataSource;

import com.dotcms.repackage.org.apache.commons.dbcp.BasicDataSource;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.viewtools.XmlTool;
import com.liferay.util.FileUtil;

public class DataSourceForTesting {


	public void setup() throws FileNotFoundException, Exception{
		
		if(DbConnectionFactory.ready()){
			return ;
		}

		
		XmlTool xml = new XmlTool().parse(getContextAsString());
		xml = xml.children().find("Resource");
		Iterator<XmlTool> it = xml.iterator();
		XmlTool source = new XmlTool();
		while (it.hasNext()) {
			XmlTool tool = it.next();
			if (tool.attr("name").equals("jdbc/dotCMSPool")) {
				source = tool;
				break;
			}
		}
		String driver = source.attr("driverClassName");
		String url = source.attr("url");
		String username = source.attr("username");
		String password = source.attr("password");
		

		System.out.println("Building test datasource : " +url );
		
		

		DbConnectionFactory.overrideDefaultDatasource(getDataSource(driver, url, username, password));
		
	}
	

	
	private static DataSource getDataSource(String driver, String url, String username, String password) throws Exception {
		BasicDataSource dataSource = new BasicDataSource();
        dataSource.setDriverClassName(driver);
        dataSource.setUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setMaxActive(100);
        return dataSource;
	}

	private String getContextAsString() throws FileNotFoundException{
		FileFilter ff = new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				return ("context.xml".equals(pathname.getName()) || pathname.isDirectory());
			}
		};

		File root = findProjectRoot(new File("."), 0);
		File context = null;
		List<File> files = FileUtil.listFilesRecursively(root, ff);
		for(File f: files){
			if(f.isFile()){
				context=f;
				break;
			}
		}
		if(context==null){
			throw new DotStateException("Unable to find the context.xml");
		}
		System.out.println("Using context file:" + context.getAbsolutePath());
		try (Scanner scan = new Scanner(context)){
			return scan.useDelimiter("\\Z").next();
		}

		
	}
	
	
	private File findProjectRoot(File start, int i){
		File[] files = start.listFiles();
		for(File file : files){
			if(file.isDirectory() && "META-INF".equals(file.getName())){
				return file;
			}
			if(file.isDirectory() && "extra".equals(file.getName())){
				return file;
			}
		}
		File parent = start.getParentFile();
		if(parent ==null || parent.equals(start) || i > 500){
			throw new DotStateException("Unable to find the directory root");
		}
		return findProjectRoot(start.getParentFile(), ++i);
	}
	
	
	
}
