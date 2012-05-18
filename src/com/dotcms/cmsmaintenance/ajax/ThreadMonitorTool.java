package com.dotcms.cmsmaintenance.ajax;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.directwebremoting.WebContextFactory;

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
		validateUser();
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
	    validateUser();
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
	    validateUser();
		final RuntimeMXBean rmxbean = ManagementFactory.getRuntimeMXBean();
		ThreadMXBean tb = ManagementFactory.getThreadMXBean();
		
		SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy");
		
		Map<String, String> sysProps = new HashMap<String, String>();
		
		sysProps.put("System Startup Time ", sdf.format(rmxbean.getStartTime()));
		sysProps.put("Thread Count - Current ", (tb.getThreadCount() + ""));
		sysProps.put("Thread Count - Peak ", (tb.getPeakThreadCount() + ""));		

		return sysProps;
	}
	
}
