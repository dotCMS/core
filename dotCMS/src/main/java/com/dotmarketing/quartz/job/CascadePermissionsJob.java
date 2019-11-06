/**
 * 
 */
package com.dotmarketing.quartz.job;

import com.dotcms.api.system.event.Visibility;
import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.notifications.bean.NotificationLevel;
import com.dotcms.notifications.bean.NotificationType;
import com.dotcms.notifications.business.NotificationAPI;
import com.dotcms.util.I18NMessage;
import com.dotmarketing.business.web.WebAPILocator;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;

import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.business.VersionableAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.quartz.QuartzUtils;
import com.dotmarketing.quartz.ScheduledTask;
import com.dotmarketing.util.AdminLogger;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import org.quartz.StatefulJob;

/**
 * @author David H Torres
 */
public class CascadePermissionsJob implements StatefulJob {
	
	/* (non-Javadoc)
	 * @see org.quartz.Job#execute(org.quartz.JobExecutionContext)
	 */
    
    private final ContentletAPI contAPI;
    private final FolderAPI folderAPI;
    private final HostAPI hostAPI;
    private final IdentifierAPI identAPI;
    private final LanguageAPI langAPI;
    private final NotificationAPI notificationAPI;
    private final PermissionAPI permissionAPI;
    private final RoleAPI roleAPI;
    private final UserAPI userAPI;
    private final VersionableAPI verAPI;

	public CascadePermissionsJob() {
		contAPI   = APILocator.getContentletAPI();
		folderAPI = APILocator.getFolderAPI();
		hostAPI   = APILocator.getHostAPI();
		identAPI  = APILocator.getIdentifierAPI();
		langAPI   = APILocator.getLanguageAPI();
		roleAPI   = APILocator.getRoleAPI();
		userAPI   = APILocator.getUserAPI();
		verAPI    = APILocator.getVersionableAPI();

		notificationAPI = APILocator.getNotificationAPI();
		permissionAPI   = APILocator.getPermissionAPI();
	}
    
	public static void triggerJobImmediately (Permissionable perm, Role role) {
		String randomID = UUID.randomUUID().toString();
		String userId = null;
		JobDataMap dataMap = new JobDataMap();
		
		dataMap.put("permissionableId", perm.getPermissionId());
		dataMap.put("roleId", role.getId());

		//TODO: For a major release, remove this logic and get userId as parameter
		if (UtilMethods.isSet(HttpServletRequestThreadLocal.INSTANCE.getRequest())) {
			userId = WebAPILocator.getUserWebAPI()
					.getLoggedInUser(HttpServletRequestThreadLocal.INSTANCE.getRequest())
					.getUserId();
		}
		dataMap.put("userId", userId);

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

	@CloseDBIfOpened
	public void execute(JobExecutionContext jobContext) throws JobExecutionException {
		
	    Permissionable permissionable;
	    
		final JobDataMap map = jobContext.getJobDetail().getJobDataMap();

		final String permissionableId = (String) map.get("permissionableId");
		final String roleId = (String) map.get("roleId");
		final String userId = (String) map.get("userId");
		try {
			permissionable  = retrievePermissionable(permissionableId);
			final Role role = roleAPI.loadRoleById(roleId);
			permissionAPI.cascadePermissionUnder(permissionable, role);
			permissionAPI.removePermissionableFromCache(permissionable.getPermissionId());

			if (UtilMethods.isSet(userId)){
				notificationAPI.generateNotification(
						new I18NMessage("notification.identifier.cascadepermissionsjob.info.title"),
						new I18NMessage("notification.cascade.permissions.success"),
						null, // no actions
						NotificationLevel.INFO,
						NotificationType.GENERIC, Visibility.USER, userId, userId,
						userAPI.getSystemUser().getLocale()
				);
			}
		} catch (DotDataException | DotSecurityException e) {
			Logger.error(CascadePermissionsJob.class, e.getMessage(), e);

			if (UtilMethods.isSet(userId)){
				try {
					notificationAPI.generateNotification(
                            new I18NMessage("notification.identifier.cascadepermissionsjob.info.title"),
                            new I18NMessage("notification.cascade.permissions.error"),
                            null, // no actions
                            NotificationLevel.ERROR,
                            NotificationType.GENERIC, Visibility.USER, userId, userId,
                            userAPI.getSystemUser().getLocale()
                    );
				} catch (DotDataException e1) {
					Logger.error(CascadePermissionsJob.class, e.getMessage(), e);
				}
			}
			throw new DotRuntimeException(e.getMessage(), e);
		}
	}
	
	@SuppressWarnings("unchecked")
	private Permissionable retrievePermissionable (String assetId) throws DotDataException, DotSecurityException {
		
		Permissionable perm;
		
		//Determining the type
		
		//Host?
		perm = hostAPI.find(assetId, userAPI.getSystemUser(), false);
		
		if(perm == null) {
			//Content?
			try {
				perm = contAPI.findContentletByIdentifier(assetId, false, langAPI.getDefaultLanguage().getId(), userAPI.getSystemUser(), false);
			} catch (DotContentletStateException e) {
			    Logger.warn(this, e.getMessage(), e);
			}
		}
		
		if(perm == null) {
			// is it an identifier?
			Identifier ident = identAPI.find(assetId);
			if(ident != null && UtilMethods.isSet(ident.getId())) {
    			if(("folder").equals(ident.getAssetType()))
    			    perm = folderAPI.findFolderByPath(ident.getURI(), ident.getHostId(), userAPI.getSystemUser(), false);
    			else 
    			    perm = (Permissionable) verAPI.findWorkingVersion(assetId, userAPI.getSystemUser(), false);
			}
		}

		if(perm == null || !UtilMethods.isSet(perm.getPermissionId())) {
		    // is it an inode?
		    
			perm = InodeFactory.getInode(assetId, Inode.class);
		}
		return perm;
	}

}
