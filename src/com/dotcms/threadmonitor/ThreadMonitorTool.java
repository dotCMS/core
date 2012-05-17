package com.dotcms.threadmonitor;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/** 
 *  This class gets various system property data.
 *  Each method collects information that might be helpful for system admins.
 *  
 *  By: IPFW Web Team
 *  Author: Marat Kurbanov
 */

public class ThreadMonitorTool{

	public ThreadMonitorTool(){}

	/**
	  *	Helper method; stringfies the ThreadInfos and returns them as a string array
	*/
	public String[] getThreads() {
		
		ThreadInfo[] threadInfos = getAllThreadInfos();
		int arraySize = threadInfos.length;
		String[] threads = new String[arraySize];
		
		for (int i = 0; i < arraySize; i++ ) {
			threads[i] = threadInfos[i].toString().replace("at ", "<br />&nbsp; &nbsp; &nbsp; at ");
		}
		return threads;

	} // end of getThreadArray method	
	
	/**
	  * Generates an array of thread infos. Adopted from
	  * http://nadeausoftware.com/articles/2008/04/java_tip_how_list_and_find_threads_and_thread_groups
	**/
	private ThreadInfo[] getAllThreadInfos() {
		final ThreadMXBean thbean = ManagementFactory.getThreadMXBean();
		final long[] ids = thbean.getAllThreadIds();

		ThreadInfo[] infos;
		if (!thbean.isObjectMonitorUsageSupported()
				|| !thbean.isSynchronizerUsageSupported())
			infos = thbean.getThreadInfo(ids);
		else
			infos = thbean.getThreadInfo(ids, true, true);

		final ThreadInfo[] notNulls = new ThreadInfo[infos.length];
		
		int nNotNulls = 0;
		for (ThreadInfo info : infos){
			if (info != null){
				notNulls[nNotNulls++] = info;				
			}
		}		
		
		if (nNotNulls == infos.length)
			return infos;
		return java.util.Arrays.copyOf(notNulls, nNotNulls);
	}// end of getAllThreadInfos method

	/**
	  * Utility method generates various application specific data
	**/
	public Map<String, String> getSysProps(){

		final RuntimeMXBean rmxbean = ManagementFactory.getRuntimeMXBean();
		ThreadMXBean tb = ManagementFactory.getThreadMXBean();
		List<MemoryPoolMXBean> pools = ManagementFactory.getMemoryPoolMXBeans();
		
		SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy");
		
		Map<String, String> sysProps = new HashMap<String, String>();
		
		sysProps.put("System Startup Time ", sdf.format(rmxbean.getStartTime()));
		sysProps.put("Thread Count - Current ", (tb.getThreadCount() + ""));
		sysProps.put("Thread Count - Peak ", (tb.getPeakThreadCount() + ""));
		sysProps.put("VM Version ", rmxbean.getVmVersion());
		
		//add up total memory used by the Tomcat along with DotCMS
		//this includes 'PS Eden Space', 'PS Perm Gen', and etc.
        long totalMemory = 0;
        for (MemoryPoolMXBean pool : pools) {
			MemoryUsage peak = pool.getPeakUsage();
			totalMemory = totalMemory + peak.getUsed();		
        }
		
		sysProps.put("Memory Used by the System ", ((totalMemory/1048576) + " MB"));		

		return sysProps;
	}
	
}
