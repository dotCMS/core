package com.dotmarketing.util.jasper;

import com.dotmarketing.loggers.Log4jUtil;
import org.apache.jasper.JasperException;
import org.apache.jasper.JspC;

import java.util.HashSet;
import java.util.Set;

public class DotJasperTask extends JspC {

	@Override
	protected void processFile(String arg0) throws JasperException {

		//Create and add a new ConsoleAppender to the log4j configuration
		Log4jUtil.createAndAddConsoleAppender();

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
