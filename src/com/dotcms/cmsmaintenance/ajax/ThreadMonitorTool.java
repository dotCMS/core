package com.dotcms.cmsmaintenance.ajax;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import com.dotcms.repackage.org.directwebremoting.WebContextFactory;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;

/**
 *  This class gets various system property data.
 *  Each method collects information that might be helpful for system admins.
 *
 *  By: IPFW Web Team
 *  Author: Marat Kurbanov
 */

public class ThreadMonitorTool{
    public boolean validateUser() {
        HttpServletRequest req = WebContextFactory.get().getHttpServletRequest();
        User user = null;
        try {
            user = com.liferay.portal.util.PortalUtil.getUser(req);
            if(user == null || !APILocator.getLayoutAPI().doesUserHaveAccessToPortlet("EXT_CMS_MAINTENANCE", user)){
                throw new DotSecurityException("User does not have access to the CMS Maintance Portlet");
            }
            return true;
        } catch (Exception e) {
            Logger.error(this, e.getMessage());
            throw new DotRuntimeException (e.getMessage());
        }
    }

	/**
	  *	Helper method; stringfies the ThreadInfos and returns them as a string array
	*/
	public String[] getThreads() {
		 ThreadMXBean threads = ManagementFactory.getThreadMXBean(); 

		    StringBuilder sb = new StringBuilder();
		    sb.append( "<pre>" );   
		    sb.append( String.format("Thread dump at: %1$tF %1$tT", new java.util.Date()) );
		    sb.append( "\n\n" );    

		    long[] deadLockedArray = threads.findDeadlockedThreads();
		    Set<Long> deadlocks = new HashSet<Long>();
		    if( deadLockedArray != null ) {
		        for( long i : deadLockedArray ) {
		            deadlocks.add(i);
		        }
		    }

		    // Build a TID map for looking up more specific thread
		    // information than provided by ThreadInfo
		    Map<Long, Thread> threadMap = new HashMap<Long, Thread>();
		    for( Thread t : Thread.getAllStackTraces().keySet() ) {
		        threadMap.put( t.getId(), t );
		    } 

		    // This only works in 1.6+... but I think that's ok
		    ThreadInfo[] infos = threads.dumpAllThreads(true, true);
		    for( ThreadInfo info : infos ) {
		    	
		        StringBuilder threadMetaData = new StringBuilder();
		        Thread thread = threadMap.get(info.getThreadId());            
		        if( thread != null ) {
		            threadMetaData.append("(");
		            threadMetaData.append(thread.isDaemon() ? "daemon " : "");
		            threadMetaData.append(thread.isInterrupted() ? "interrupted " : "");
		            threadMetaData.append("prio=" + thread.getPriority());
		            threadMetaData.append(")");                
		        }  
		        String s = info.toString().trim();
		        if(s.indexOf("\t...")>-1){
	
		        	s=s.substring(0, s.indexOf("\t..."));
		        	boolean printTrace = false;
		        	for(StackTraceElement trace : info.getStackTrace()){
		        		if(printTrace || !s.contains(trace.toString())) {
		        			printTrace = true;
		        	
			        		s+="\tat " + trace.toString() + "\n";
			        	}
			        }
		        }
		        

		        // Inject the meta-data after the ID... Presumes a 
		        // certain format but it's low priority information.            
		        s = s.replaceFirst( "Id=\\d+", "$0 " + threadMetaData );
		        sb.append( s );
		        sb.append( "\n" );

		        if( deadlocks.contains(info.getThreadId()) ) {
		            sb.append( " ** Deadlocked **" );
		            sb.append( "\n" );
		        }

		        sb.append( "\n" );
		    }
		    sb.append( "</pre>" );   
		    return new String[] {sb.toString()};    

	} // end of getThreadArray method

	/**
	  * Generates an array of thread infos. Adopted from
	  * http://nadeausoftware.com/articles/2008/04/java_tip_how_list_and_find_threads_and_thread_groups
	**/
	private ThreadInfo[] getAllThreadInfos() {
	    validateUser();
		final ThreadMXBean thbean = ManagementFactory.getThreadMXBean();




		final ThreadInfo[] notNulls = thbean.dumpAllThreads(true, true);

		return notNulls;
	}// end of getAllThreadInfos method

	/**
	  * Utility method generates various application specific data
	**/
	public Map<String, String> getSysProps(){
	    validateUser();
		final RuntimeMXBean rmxbean = ManagementFactory.getRuntimeMXBean();
		ThreadMXBean tb = ManagementFactory.getThreadMXBean();

		SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy HH:mm:ss");

		Map<String, String> sysProps = new HashMap<String, String>();

		sysProps.put("System Startup Time ", sdf.format(rmxbean.getStartTime()));
		sysProps.put("Thread Count - Current ", (tb.getThreadCount() + ""));
		sysProps.put("Thread Count - Peak ", (tb.getPeakThreadCount() + ""));

		return sysProps;
	}

}
