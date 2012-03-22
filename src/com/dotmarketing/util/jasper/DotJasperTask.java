package com.dotmarketing.util.jasper;

import org.apache.jasper.JasperException;
import org.apache.jasper.JspC;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

public class DotJasperTask extends JspC {

	@Override
	protected void processFile(String arg0) throws JasperException {
		//Init log4j to see the messages in ant's output
		Logger logRoot = Logger.getRootLogger();
		if (!logRoot.getAllAppenders().hasMoreElements()) {
			logRoot.addAppender(new ConsoleAppender(   new PatternLayout("%m%n")));
		}		
		if ( (!arg0.endsWith("_inc.jsp")) && (!arg0.startsWith("/html/plugins/"))) {
			super.processFile(arg0);
		} else {
			//System.err.println("Skipping: " + arg0);
				
		}
	}
	

}
