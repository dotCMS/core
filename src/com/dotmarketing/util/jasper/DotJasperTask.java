package com.dotmarketing.util.jasper;

import java.util.HashSet;
import java.util.Set;

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
		if ( ((!arg0.endsWith("_inc.jsp")) && (!arg0.startsWith("/html/plugins/"))) || includeJSP(arg0)) {
			super.processFile(arg0);
		} else {
			//System.err.println("Skipping: " + arg0);
				
		}
	}

	private static Set<String> includeList = null;
	private static final Integer mutex = new Integer(0);

	private static void buildIncludeList() {
		synchronized (mutex) {
			if (includeList != null)
				return;
			Set<String> set = new HashSet<String>();
			// Load some defaults
			set.add("contentlet_versions_inc.jsp");
			set.add("view_contentlet_popup_inc.jsp");
			includeList = set;
		}
	}

	public static boolean includeJSP(String jsp) {
		if (includeList == null)
			buildIncludeList();
		for (String str : includeList) {
			if (jsp.endsWith(str))
				return true;
		}
		return false;
	}
}
