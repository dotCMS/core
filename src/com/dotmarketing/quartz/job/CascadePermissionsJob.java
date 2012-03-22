/**
 * 
 */
package com.dotmarketing.quartz.job;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;

import com.dotmarketing.beans.Inode;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.quartz.QuartzUtils;
import com.dotmarketing.quartz.ScheduledTask;
import com.dotmarketing.util.AdminLogger;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

/**
 * @author David H Torres
 */
public class CascadePermissionsJob implements Job {
	
	/* (non-Javadoc)
	 * @see org.quartz.Job#execute(org.quartz.JobExecutionContext)
	 */
	
	public static void triggerJobImmediately (Permissionable perm, Role role) {
		String randomID = UUID.randomUUID().toString();
		JobDataMap dataMap = new JobDataMap();
		
		dataMap.put("permissionableId", perm.getPermissionId());
		dataMap.put("roleId", role.getId());
		
		JobDetail jd = new JobDetail("CascadePermissionsJob-" + randomID, "cascade_permissions_jobs", CascadePermissionsJob.class);
		jd.setJobDataMap(dataMap);
		jd.setDurability(false);
		jd.setVolatility(false);
		jd.setRequestsRecovery(true);
		
		long startTime = System.currentTimeMillis();
		SimpleTrigger trigger = new SimpleTrigger("permissionsCascadeTrigger-"+randomID, "cascade_permissions_triggers",  new Date(startTime));
		
		try {
			Scheduler sched = QuartzUtils.getSequentialScheduler();
			sched.scheduleJob(jd, trigger);
		} catch (SchedulerException e) {
			Logger.error(CascadePermissionsJob.class, "Error scheduling the cascading of permissions", e);
			throw new DotRuntimeException("Error scheduling the cascading of permissions", e);
		}
		AdminLogger.log(CascadePermissionsJob.class, "triggerJobImmediately", "Cascading permissions of : "+perm.getPermissionId());

	}
	
	public static List<ScheduledTask> getCurrentScheduledJobs () {
		try {
			return QuartzUtils.getSequentialScheduledTasks("cascade_permissions_jobs");
		} catch (SchedulerException e) {
			Logger.error(CascadePermissionsJob.class, "Unable to retrieve jobs info");
			throw new DotRuntimeException(e.getMessage(), e);
		}
	}
	
	public CascadePermissionsJob() {
		
	}
	
	public void execute(JobExecutionContext jobContext) throws JobExecutionException {
		
		JobDataMap map = jobContext.getJobDetail().getJobDataMap();

		PermissionAPI permissionAPI = APILocator.getPermissionAPI();
		permissionAPI.clearCache();
		String permissionableId = (String) map.get("permissionableId");
		String roleId = (String) map.get("roleId");
		try {
			RoleAPI roleAPI = APILocator.getRoleAPI();
			Permissionable permissionable = (Permissionable) retrievePermissionable(permissionableId);
			Role role = (Role) roleAPI.loadRoleById(roleId);
			permissionAPI.cascadePermissionUnder(permissionable, role);
		} catch (DotDataException e) {
			Logger.error(this, e.getMessage(), e);
			permissionAPI.clearCache();
			throw new DotRuntimeException(e.getMessage(), e);
		} catch (DotSecurityException e) {
			Logger.error(CascadePermissionsJob.class, e.getMessage(), e);
			permissionAPI.clearCache();
			throw new DotRuntimeException(e.getMessage(), e);
		}
		permissionAPI.clearCache();
	}
	
	@SuppressWarnings("unchecked")
	private Permissionable retrievePermissionable (String assetId) throws DotDataException, DotSecurityException {
		
		UserAPI userAPI = APILocator.getUserAPI();
		HostAPI hostAPI = APILocator.getHostAPI();
		
		Permissionable perm = null;
		
		//Determining the type
		
		//Host?
		perm = hostAPI.find(assetId, userAPI.getSystemUser(), false);
		
		if(perm == null) {
			//Content?
			ContentletAPI contAPI = APILocator.getContentletAPI();
			try {
				perm = contAPI.findContentletByIdentifier(assetId, false, APILocator.getLanguageAPI().getDefaultLanguage().getId(), userAPI.getSystemUser(), false);
			} catch (DotContentletStateException e) {
			}
		}
		
		if(perm == null) {
			
			DotConnect dc = new DotConnect();
			dc.setSQL("select inode, type from inode where identifier = ?");
			dc.addParam(assetId);
			ArrayList results = dc.loadResults();
			
			if(results.size() > 0) {
				String inode = (String) ((Map)results.get(0)).get("inode");
				perm = InodeFactory.getInode(inode, Inode.class);
			}
			
		}

		if(perm == null || !UtilMethods.isSet(perm.getPermissionId())) {
			perm = InodeFactory.getInode(assetId, Inode.class);
		}
		return perm;
	}

}
