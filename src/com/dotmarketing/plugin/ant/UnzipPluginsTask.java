package com.dotmarketing.plugin.ant;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import com.dotmarketing.plugin.util.PluginUtil;

/**
 * This task will unzip the plugins in the correct order into the directory that gets used to build the bundle plugin for war distribution.
 * @author Andres Olarte
 *
 */
public class UnzipPluginsTask  extends Task {
	/**
	 * Destination of the unziped files
	 */
	private String destination;
	

	/**
	 * The location of the plugins
	 */
	private String pluginPath;
	
	/**
	 * Place where the mock plugin zips will be placed.  These are used since we need their MANIFEST 
	 */
	private String mockDestination;
	
	/**
	 * Set the root of the web app (Servlet context)
	 */
	private String rootPath;
	
	
	public void setDestination(String destination) {
		this.destination = destination;
	}

	public void setPluginPath(String pluginPath) {
		this.pluginPath = pluginPath;
	}

	public void setMockDestination(String mockDestination) {
		this.mockDestination = mockDestination;
	}
	
	/**
	 * Set the root of the web app (Servlet context)
	 * 
	 * @param root
	 *            The root of the web app
	 */
	public synchronized void setRootPath(String rootPath) {
		this.rootPath = rootPath;
	}
	
	public static void main (String[] args) {
		Logger logRoot = Logger.getRootLogger();
		if (!logRoot.getAllAppenders().hasMoreElements()) {
			logRoot.addAppender(new ConsoleAppender(   new PatternLayout("%m%n")));
		}
		UnzipPluginsTask task=new UnzipPluginsTask();
		task.setRootPath(args[0]);
		task.setPluginPath(args[1]);
		task.setDestination(args[2]);
		task.setMockDestination(args[3]);
		task.execute();
	}
	
	@Override
	public void execute() throws BuildException {
		//Init log4j to see the messages in ant's output
		Logger logRoot = Logger.getRootLogger();
		if (!logRoot.getAllAppenders().hasMoreElements()) {
			logRoot.addAppender(new ConsoleAppender(   new PatternLayout("%m%n")));
		}
		File destFile=new File(destination);
		List<File> plugins = PluginUtil.getPluginJars(rootPath, pluginPath);
		for (File plugin : plugins) {
			try {
				ZipFile zFile=new ZipFile(plugin);
				extract(zFile, destFile);
				JarFile jFile=new JarFile(plugin);
				Manifest manifest=jFile.getManifest();
				createFakeJar(mockDestination+File.separator+plugin.getName(),manifest);
				
			} catch (ZipException e) {
				throw new BuildException(e);
			} catch (IOException e) {
				throw new BuildException(e);
			}
		}
		
		
	}
	
	   public void extract(ZipFile zipFile, File toDir) throws IOException{
		   if (! toDir.exists()){
		   		toDir.mkdirs();
		   }
		   Enumeration entries = zipFile.entries();
		   while (entries.hasMoreElements()) {
			   ZipEntry zipEntry = (ZipEntry) entries.nextElement();
			   if (zipEntry.isDirectory()) {
				   File dir = new File(toDir, zipEntry.getName());
				   if (! dir.exists()){ // make sure also empty directories get created!
					   dir.mkdirs();
				   }
			   } else {
				   extract(zipFile, zipEntry, toDir);
			   }
	   		}
	   }	
	   
	   public void createFakeJar(String fileName,Manifest manifest) throws IOException {
 
		   File zipFile=new File(fileName);
		   BufferedOutputStream bout = new BufferedOutputStream(new FileOutputStream(zipFile));
		   manifest.toString();
		   ZipOutputStream zos = new ZipOutputStream(bout);
		   ZipEntry anEntry = new ZipEntry("META-INF/MANIFEST.MF"); 
			//place the zip entry in the ZipOutputStream object 
			zos.putNextEntry(anEntry); 
			manifest.write(zos);
			zos.finish();
			bout.flush();
			bout.close();
		
	   }
	
	   public void extract(ZipFile zipFile, ZipEntry zipEntry, File toDir) throws IOException {
	        File file = new File(toDir, zipEntry.getName());
	        File parentDir = file.getParentFile();
	        if (! parentDir.exists()){
	            parentDir.mkdirs();
	        }
	        
	        BufferedInputStream bis = null;
	        BufferedOutputStream bos = null;
	        try{
	            InputStream istr = zipFile.getInputStream(zipEntry);
	            bis = new BufferedInputStream(istr);
	            FileOutputStream fos = new FileOutputStream(file);
	            bos  = new BufferedOutputStream(fos);
	            int byte_;
	            while ((byte_ = bis.read ()) != -1) {
	              bos.write (byte_);
	            }
	        } finally {
	            if (bis !=  null){
	                bis.close();
	            }
	            if (bos != null){
	                bos.close();
	            }
	        }
	    }
	   
	
	
}
