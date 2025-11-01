package com.dotcms.cmsmaintenance.ajax;

import java.lang.management.LockInfo;
import java.lang.management.ManagementFactory;
import java.lang.management.MonitorInfo;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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
            if(user == null || !APILocator.getLayoutAPI().doesUserHaveAccessToPortlet("maintenance", user)){
                throw new DotSecurityException("User does not have access to the CMS Maintance Portlet");
            }
            return true;
        } catch (Exception e) {
            Logger.error(this, e.getMessage(),e);
            throw new DotRuntimeException (e.getMessage());
        }
    }

	/**
	  *	Helper method; stringfies the ThreadInfos and returns them as a string array
	*/
	public String[] getThreads() {
		// Validate user has access to the CMS Maintenance Portlet
		if (!validateUser()) {
			throw new DotRuntimeException("User does not have access to the CMS Maintenance Portlet");
		}

		ThreadMXBean mxBean = ManagementFactory.getThreadMXBean(); 

	    StringBuilder sb = new StringBuilder();
	    sb.append( "<pre>" );   
	    sb.append("\n" + new Date() + "\n");
	    sb.append( "Full thread dump "  + System.getProperty("java.vm.name")+ " " + System.getProperty("java.runtime.version") +   " (" + System.getProperty("java.vm.version") + " " + System.getProperty("java.vm.info")  + "):");
	    sb.append( "\n\n" );    

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
	    
	    
	    
	    for( ThreadInfo info : infos ) {
	        Thread thread = threadMap.get(info.getThreadId());           
	        LockInfo lockInfo = info.getLockInfo();
	        MonitorInfo[] monitors = info.getLockedMonitors() ;
	        LockInfo[] locks= info.getLockedSynchronizers();
	        
	        
	        if( thread == null ) continue;
    

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
	        
	        sb.append("\"");
	        sb.append(info.getThreadName());
	        sb.append("\"");
	        sb.append(" ");
        	sb.append(thread.isDaemon() ? "daemon " : "");
        	sb.append(thread.isInterrupted() ? "interrupted " : "");
        	sb.append("prio=" + thread.getPriority());        
	        sb.append(" ");
	        sb.append("tid=" + hexMe(tid));
	        if((lockInfo!=null)  ){
		        sb.append(" waiting on condition [");
		        sb.append( hexMe(lockInfo.getIdentityHashCode()));
		        sb.append("]");
	        }
	        
	        
	        Object blocker =  java.util.concurrent.locks.LockSupport.getBlocker(thread);
	        if(blocker !=null){
	        //sb.append(" ");
	       // sb.append("nativeParkPointer=" + hexMe(blocker.hashCode()));
	        }
	        
	        

	        

	        
	        sb.append("\n");
	        
	        sb.append("  java.lang.Thread.State: ");
	        sb.append(thread.getState());
	        if(info.getStackTrace()!=null && info.getStackTrace().length>0){
		        StackTraceElement first = info.getStackTrace()[0];
		        if("sleep".equals(first.getMethodName())){
		        	sb.append("  (sleeping)");
		        }else if("park".equals(first.getMethodName())){
		        	sb.append("  (parking)");
		        }else if("wait".equals(first.getMethodName())){
		        	sb.append("  (on object monitor)");
		        }
	        }
	        
	        sb.append("\n"); 
	        int i=0;
        	for(StackTraceElement trace : info.getStackTrace()){

        		sb.append("\tat " + trace.toString() + "\n");

        		
        		if(i==0 && lockInfo != null){
        			
        			if(thread.getState().equals(Thread.State.WAITING) || thread.getState().equals(Thread.State.TIMED_WAITING))
        			sb.append("\t- waiting on " + lockInfo+ ")\n");
        		}
        		
        		
    	        if(monitors != null){
    	        	for(MonitorInfo mi : monitors){
    	        		if(i==mi.getLockedStackDepth()){
    	        			sb.append("\t- locked <");
    	        			sb.append(hexMe(mi.getIdentityHashCode()));
    	        			sb.append("> (a ");
    	        			sb.append(mi.getClassName());
    	        			sb.append(")\n");
    	        		}
    	        		
    	        	}
    	        }
        		
	        	i++;
	        }



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

		Map<String, String> sysProps = new HashMap<>();

		sysProps.put("System Startup Time ", sdf.format(rmxbean.getStartTime()));
		sysProps.put("Thread Count - Current ", (tb.getThreadCount() + ""));
		sysProps.put("Thread Count - Peak ", (tb.getPeakThreadCount() + ""));

		return sysProps;
	}

	
	private String hexMe(long x){
		
		return String.format("0x%016X", x).toLowerCase();
		
	}
}
