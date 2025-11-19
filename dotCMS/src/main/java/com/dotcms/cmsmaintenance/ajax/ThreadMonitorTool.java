package com.dotcms.cmsmaintenance.ajax;

import com.dotcms.repackage.org.directwebremoting.WebContextFactory;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import java.lang.management.LockInfo;
import java.lang.management.ManagementFactory;
import java.lang.management.MonitorInfo;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

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
            if(user == null || !APILocator.getLayoutAPI().doesUserHaveAccessToPortlet("maintenance", user)){
                throw new DotSecurityException("User does not have access to the CMS Maintance Portlet");
            }
            return true;
        } catch (Exception e) {
            Logger.error(this, e.getMessage(),e);
            throw new DotRuntimeException (e.getMessage());
        }
    }

    public String[] getThreads() {
        return getThreads(false);
    }


	/**
	  *	Helper method; stringfies the ThreadInfos and returns them as a string array
	*/
    public String[] getThreads(boolean hideSystemThreads) {
		// Validate user has access to the CMS Maintenance Portlet
		if (!validateUser()) {
			throw new DotRuntimeException("User does not have access to the CMS Maintenance Portlet");
		}

        ThreadMXBean mxBean = ManagementFactory.getThreadMXBean();

        StringBuilder mainString = new StringBuilder();
        mainString.append("<pre>");
        mainString.append("\n" + new Date() + "\n");
        mainString.append("Full thread dump " + System.getProperty("java.vm.name") + " " + System.getProperty(
                "java.runtime.version") + " (" + System.getProperty("java.vm.version") + " " + System.getProperty(
                "java.vm.info") + "):");
        mainString.append("\n\n");

	    long[] deadLockedArray = mxBean.findDeadlockedThreads();
	    Set<Long> deadlocks = new HashSet<>();
	    if( deadLockedArray != null ) {
	        for( long i : deadLockedArray ) {
	            deadlocks.add(i);
	        }
	    }

	    // Build a TID map for looking up more specific thread
	    // information than provided by ThreadInfo
	    Map<Long, Thread> threadMap = new HashMap<>();
	    for( Thread t : Thread.getAllStackTraces().keySet() ) {
	        threadMap.put( t.getId(), t );
        }


	    ThreadInfo[] infos = mxBean.dumpAllThreads(true, true);

	    Map<String, String> blockers = new HashMap<>();

        for (ThreadInfo info : infos) {
            StringBuilder builder = new StringBuilder();
            Thread thread = threadMap.get(info.getThreadId());
	        LockInfo lockInfo = info.getLockInfo();
	        MonitorInfo[] monitors = info.getLockedMonitors() ;
	        LockInfo[] locks= info.getLockedSynchronizers();

            if (thread == null) {
                continue;
            }


	        long tid = info.getThreadId();

	        try{
	        	Field f = Thread.class.getDeclaredField("eetop");
	        	f.setAccessible(true);
	        	Object x = f.get(thread);
	        	tid = Long.parseLong(x.toString());
	        }catch(Exception e){

	        }

            long nativeParkPointer = 0;
	        try{
	        	Field f = Thread.class.getDeclaredField("nativeParkEventPointer");
	        	f.setAccessible(true);
	        	Object x = f.get(thread);
	        	nativeParkPointer = Long.parseLong(x.toString());
	        }catch(Exception e){

	        }

            builder.append("\"");
            builder.append(info.getThreadName());
            builder.append("\"");
            builder.append(" ");
            builder.append(thread.isDaemon() ? "daemon " : "");
            builder.append(thread.isInterrupted() ? "interrupted " : "");
            builder.append("prio=" + thread.getPriority());
            builder.append(" ");
            builder.append("tid=" + hexMe(tid));
	        if((lockInfo!=null)  ){
                builder.append(" waiting on condition [");
                builder.append(hexMe(lockInfo.getIdentityHashCode()));
                builder.append("]");
	        }

            Object blocker = java.util.concurrent.locks.LockSupport.getBlocker(thread);
	        if(blocker !=null){
	        //sb.append(" ");
	       // sb.append("nativeParkPointer=" + hexMe(blocker.hashCode()));
	        }

            builder.append("\n");

            builder.append("  java.lang.Thread.State: ");
            builder.append(thread.getState());
	        if(info.getStackTrace()!=null && info.getStackTrace().length>0){
		        StackTraceElement first = info.getStackTrace()[0];
		        if("sleep".equals(first.getMethodName())){
                    builder.append("  (sleeping)");
		        }else if("park".equals(first.getMethodName())){
                    builder.append("  (parking)");
		        }else if("wait".equals(first.getMethodName())){
                    builder.append("  (on object monitor)");
		        }
	        }

            builder.append("\n");
	        int i=0;
        	for(StackTraceElement trace : info.getStackTrace()){

                builder.append("\tat " + trace.toString() + "\n");


        		if(i==0 && lockInfo != null){

                    if (thread.getState().equals(Thread.State.WAITING) || thread.getState()
                            .equals(Thread.State.TIMED_WAITING)) {
                        builder.append("\t- waiting on " + lockInfo + ")\n");
                    }
        		}

                if (monitors != null) {
    	        	for(MonitorInfo mi : monitors){
    	        		if(i==mi.getLockedStackDepth()){
                            builder.append("\t- locked <");
                            builder.append(hexMe(mi.getIdentityHashCode()));
                            builder.append("> (a ");
                            builder.append(mi.getClassName());
                            builder.append(")\n");
    	        		}

                    }
    	        }

                i++;
	        }

            builder.append("\n");

	        if( deadlocks.contains(info.getThreadId()) ) {
                builder.append(" ** Deadlocked **");
                builder.append("\n");
	        }

            builder.append("\n");
            if (!hideSystemThreads || builder.indexOf("com.dotmarketing") > 1 || builder.indexOf("com.dotcms") > 1) {
                mainString.append(builder.toString());
            }
	    }
        mainString.append("</pre>");
        return new String[]{mainString.toString()};

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

		Map<String, String> sysProps = new HashMap<>();

		sysProps.put("System Startup Time ", sdf.format(rmxbean.getStartTime()));
		sysProps.put("Thread Count - Current ", (tb.getThreadCount() + ""));
		sysProps.put("Thread Count - Peak ", (tb.getPeakThreadCount() + ""));

		return sysProps;
	}


    private String hexMe(long x) {

        return String.format("0x%016X", x).toLowerCase();

    }
}
