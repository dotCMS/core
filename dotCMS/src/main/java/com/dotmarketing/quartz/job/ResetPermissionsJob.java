package com.dotmarketing.quartz.job;

import com.dotcms.api.system.event.Visibility;
import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.notifications.bean.NotificationLevel;
import com.dotcms.notifications.bean.NotificationType;
import com.dotcms.notifications.business.NotificationAPI;
import com.dotcms.util.I18NMessage;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.quartz.DotStatefulJob;
import com.google.common.collect.ImmutableMap;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

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
import com.dotmarketing.exception.DotDataException;
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
import org.quartz.StatefulJob;
import org.quartz.Trigger;

/**
 * This job is called when the permissions on a given {@link Permissionable} have changed. A
 * Permissionable is basically a dotCMS domain object that can be permissioned to be accessed or
 * modified by specific roles or users only.
 * 
 * @author David H Torres
 */
public class ResetPermissionsJob extends DotStatefulJob {

	private final UserAPI userAPI;
	private final NotificationAPI notificationAPI;
	private final HostAPI hostAPI;

	public ResetPermissionsJob() {
		hostAPI = APILocator.getHostAPI();
		userAPI = APILocator.getUserAPI();

		notificationAPI = APILocator.getNotificationAPI();
	}
	
    /**
     * Triggers the execution of this permission rest job on the specified {@link Permissionable}.
     * 
     * @param perm - The {@link Permissionable} object, i.e., a Contentlet, a Content Type, an HTML
     *        Page, etc.
     */
	public static void triggerJobImmediately (final Permissionable perm) {
		String userId = null;

		//TODO: For a major release, remove this logic and get userId as parameter
		if (UtilMethods.isSet(HttpServletRequestThreadLocal.INSTANCE.getRequest())) {
			userId = WebAPILocator.getUserWebAPI()
					.getLoggedInUser(HttpServletRequestThreadLocal.INSTANCE.getRequest())
					.getUserId();
		}
		final Map<String, Serializable> nextExecutionData = ImmutableMap
				.of(
						"permissionableId", perm.getPermissionId(),
						"userId", userId);
		
		try {
			DotStatefulJob.enqueueTrigger(nextExecutionData, ResetPermissionsJob.class);

		} catch (Exception e) {
			Logger.error(ResetPermissionsJob.class, "Error scheduling the reset of permissions", e);
			throw new DotRuntimeException("Error scheduling the reset of permissions", e);
		}

	}

	/**
     * Triggers the permission reset operation on a given Permissionable object.
     * 
     * @param jobContext - The {@link JobExecutionContext} containing details and execution
     *        parameters of the job.
     */
	@WrapInTransaction
	public void run(final JobExecutionContext jobContext) throws JobExecutionException {

		final Trigger trigger = jobContext.getTrigger();
		final Map<String, Serializable> map = getExecutionData(trigger, ResetPermissionsJob.class);

		final PermissionAPI permissionAPI = APILocator.getPermissionAPI();
		
		final String permissionableId = (String) map.get("permissionableId");
		final String userId = (String) map.get("userId");

		try {
			final Permissionable permissionable = retrievePermissionable(permissionableId);
			permissionAPI.resetPermissionsUnder(permissionable);

			if (UtilMethods.isSet(userId)){
				notificationAPI.generateNotification(
						new I18NMessage("notification.identifier.resetpermissionsjob.info.title"),
						new I18NMessage("notification.reset.permissions.success"),
						null, // no actions
						NotificationLevel.INFO,
						NotificationType.GENERIC, Visibility.USER, userId, userId,
						userAPI.getSystemUser().getLocale()

				);
			}
		} catch (DotDataException | DotSecurityException e) {
			Logger.error(this, e.getMessage(), e);
			if (UtilMethods.isSet(userId)){
				try {
					notificationAPI.generateNotification(
                            new I18NMessage("notification.identifier.resetpermissionsjob.info.title"),
                            new I18NMessage("notification.reset.permissions.error"),
                            null, // no actions
                            NotificationLevel.ERROR,
							NotificationType.GENERIC, Visibility.USER, userId, userId,
							userAPI.getSystemUser().getLocale()
					);
				} catch (DotDataException e1) {
					Logger.error(this, e.getMessage(), e);
				}
			}
			throw new DotRuntimeException(e.getMessage(), e);
		}
	}
	
	private Permissionable retrievePermissionable (String assetId) throws DotDataException, DotSecurityException {

		Permissionable perm;
		
		//Determining the type
		
		//Host?
		perm = hostAPI.find(assetId, userAPI.getSystemUser(), false);

		//Folder?
		if (perm == null) {
			perm = APILocator.getFolderAPI().find(assetId, userAPI.getSystemUser(), false);
		}

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
