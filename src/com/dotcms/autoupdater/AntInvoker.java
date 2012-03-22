package com.dotcms.autoupdater;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import com.dotcms.autoupdater.ActivityIndicator;
import com.dotcms.autoupdater.Messages;
import com.dotcms.autoupdater.UpdateAgent;
import com.dotcms.autoupdater.UpdateException;



public class AntInvoker {
	
	private String home;


	public AntInvoker(String home){
		super();
		this.home = home;
		
	}
	
	public class InputStreamLogger extends Thread{
		private InputStream is;
		private Logger logger;
		private Level level;
		public InputStreamLogger(InputStream is, Logger logger, Level level) {
			super();
			this.is = is;
			this.logger = logger;
			this.level = level;
		}
		
		
		public void run() {
			int c;
			StringBuffer sb=new StringBuffer();
			 try {
				while ((c = is.read()) != -1) {
					 if (((char)c) == '\n') {
						 logger.log(level, sb.toString());
						 sb=new StringBuffer();						 
					 } else {
				       sb.append((char)c);
					 }
					 
					 
				    }
			} catch (IOException e) {
//				System.out.rp"IOException: " + e.getMessage(),e);
			}
			
			
		}
	}
	
	public boolean runTask(String task, String buildFile) throws IOException {
		//Try to find java's location
		String javaHome=getJavaHome();
		
		//Startup ant
		String libDir= home + File.separator +"bin" +File.separator +"ant"+File.separator; //$NON-NLS-1$ //$NON-NLS-2$
		String arg1= libDir +"ant-launcher.jar"; //$NON-NLS-1$
		String javaExe=javaHome+File.separator+"bin"+File.separator+"java"; //$NON-NLS-1$ //$NON-NLS-2$
		if (System.getProperty("os.name").toLowerCase().startsWith("windows")){ //$NON-NLS-1$ //$NON-NLS-2$
			javaExe+=".exe"; //$NON-NLS-1$
		}
		 
		ProcessBuilder pb = null;
		if (buildFile==null) {
			pb = new ProcessBuilder(javaExe, "-jar", arg1, task); //$NON-NLS-1$
		} else {
			pb = new ProcessBuilder(javaExe, "-jar", arg1, task, "-f",buildFile); //$NON-NLS-1$ //$NON-NLS-2$
		}
		 Map<String, String> env = pb.environment();
		 env.put("JAVA_HOME", javaHome); //$NON-NLS-1$
		 pb.directory(new File(home));
		 Process p = pb.start();
		 InputStreamLogger outLogger=new InputStreamLogger(p.getInputStream(),UpdateAgent.logger,Level.DEBUG);
		 outLogger.start();
		 InputStreamLogger errLogger=new InputStreamLogger(p.getErrorStream(),UpdateAgent.logger,Level.DEBUG);
		 errLogger.start();
		 ActivityIndicator.startIndicator();
		 try {			
			p.waitFor();
		} catch (InterruptedException e) {
			
		}
		
		

		try {
			outLogger.join();
		} catch (InterruptedException e) {
			
		}
		try {
			errLogger.join();
		} catch (InterruptedException e) {
			
		}
		
		p.getErrorStream().close();
		p.getInputStream().close();
		p.getOutputStream().close();
		ActivityIndicator.endIndicator();
		
		boolean ret=false;
		if (p.exitValue()==0) {
			ret=true;
		}
		return ret;
	}
	
	
	public String getJavaHome() {
		
		String ret=System.getenv("JAVA_HOME"); //$NON-NLS-1$
		if (ret==null) {
			ret=System.getenv("JRE_HOME"); //$NON-NLS-1$
		}
		
		return ret;
	}

	public boolean checkRequisites() throws UpdateException  {
		if (getJavaHome()!=null) {
			UpdateAgent.logger.info(Messages.getString("AntInvoker.text.java.home") + getJavaHome()); //$NON-NLS-1$
			return true;
		}
		throw new UpdateException(Messages.getString("AntInvoker.error.no.java.home"),UpdateException.ERROR); //$NON-NLS-1$
		
	}
	
}
