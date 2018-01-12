package com.dotmarketing.quartz.job;

import java.util.ArrayList;
import java.util.Date;
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
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.reindex.ReindexThread;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.quartz.QuartzUtils;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

/**
 * This job is called when the permissions on a given {@link Permissionable} have changed. A
 * Permissionable is basically a dotCMS domain object that can be permissioned to be accessed or
 * modified by specific roles or users only.
 * 
 * @author David H Torres
 */
public class ResetPermissionsJob implements Job {
	
    /**
     * Triggers the execution of this permission rest job on the specified {@link Permissionable}.
     * 
     * @param perm - The {@link Permissionable} object, i.e., a Contentlet, a Content Type, an HTML
     *        Page, etc.
     */
	public static void triggerJobImmediately (final Permissionable perm) {
		final String randomID = UUID.randomUUID().toString();
		final JobDataMap dataMap = new JobDataMap();
		
		dataMap.put("permissionableId", perm.getPermissionId());
		
		final JobDetail jd = new JobDetail("ResetPermissionsJob-" + randomID, "dotcms_jobs", ResetPermissionsJob.class);
		jd.setJobDataMap(dataMap);
		jd.setDurability(false);
		jd.setVolatility(false);
		jd.setRequestsRecovery(true);
		
		final long startTime = System.currentTimeMillis();
		final SimpleTrigger trigger = new SimpleTrigger("permissionsResetTrigger-"+randomID, "dotcms_triggers",  new Date(startTime));
		
		try {
		    // First, make sure the Reindex Thread is up and running
            if (!ReindexThread.getInstance().isWorking()) {
                ReindexThread.getInstance().unpause();
            }
			final Scheduler sched = QuartzUtils.getSequentialScheduler();
			sched.scheduleJob(jd, trigger);
		} catch (SchedulerException e) {
			Logger.error(ResetPermissionsJob.class, "Error scheduling the reset of permissions", e);
			throw new DotRuntimeException("Error scheduling the reset of permissions", e);
		}

	}
	
	public ResetPermissionsJob() {
		
	}

	/**
     * Triggers the permission reset operation on a given Permissionable object.
     * 
     * @param jobContext - The {@link JobExecutionContext} containing details and execution
     *        parameters of the job.
     */
	public void execute(JobExecutionContext jobContext) throws JobExecutionException {
		
		JobDataMap map = jobContext.getJobDetail().getJobDataMap();

		PermissionAPI permissionAPI = APILocator.getPermissionAPI();
		
		String permissionableId = (String) map.get("permissionableId");
		try {
			HibernateUtil.startTransaction();
			Permissionable permissionable = (Permissionable) retrievePermissionable(permissionableId);
			permissionAPI.resetPermissionsUnder(permissionable);
			HibernateUtil.closeAndCommitTransaction();
		} catch (Exception e) {
			Logger.error(this, e.getMessage(), e);
			try {
				HibernateUtil.rollbackTransaction();
			} catch (DotHibernateException e1) {
				Logger.error(ResetPermissionsJob.class,e1.getMessage(),e1);
			}
			throw new DotRuntimeException(e.getMessage(), e);
		} finally {
		    try {
                HibernateUtil.closeSession();
            } catch (DotHibernateException e) {
                Logger.warn(this, e.getMessage(), e);
            }
            finally {
                DbConnectionFactory.closeConnection();
            }
		}
		
	}
	
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
			ArrayList results = new ArrayList();
			String assetType ="";
			dc.setSQL("Select asset_type from identifier where id =?");
			dc.addParam(assetId);
			ArrayList assetResult = dc.loadResults();
			if(assetResult.size()>0){
				assetType = (String) ((Map)assetResult.get(0)).get("asset_type");
			}
			if(UtilMethods.isSet(assetType)){
				dc.setSQL("select i.inode, type from inode i,"+assetType+" a where i.inode = a.inode and a.identifier = ?");
				dc.addParam(assetId);
				results = dc.loadResults();
			}
			if(results.size() > 0) {
				String inode = (String) ((Map)results.get(0)).get("inode");
				perm = InodeFactory.getInode(inode, Inode.class);
			}
			
		}

		if(perm == null || !UtilMethods.isSet(perm.getPermissionId())) {
		    perm = InodeUtils.getInode(assetId);
		}
		return perm;
	}

}
