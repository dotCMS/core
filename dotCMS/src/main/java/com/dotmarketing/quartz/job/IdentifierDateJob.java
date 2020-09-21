/**
 * 
 */
package com.dotmarketing.quartz.job;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import com.dotcms.notifications.bean.NotificationType;
import com.dotcms.util.I18NMessage;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.notifications.bean.NotificationLevel;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.common.model.ContentletSearch;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.quartz.DotSchedulerFactory;
import com.dotmarketing.quartz.QuartzUtils;
import com.dotmarketing.util.AdminLogger;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

/**
 * @author Oscar Arrieta
 *
 */
public class IdentifierDateJob implements Job {

	/* (non-Javadoc)
	 * @see org.quartz.Job#execute(org.quartz.JobExecutionContext)
	 */
	@Override
	public void execute(JobExecutionContext jobContext) throws JobExecutionException {
		ContentletAPI contentletAPI = APILocator.getContentletAPI();

		JobDataMap map = jobContext.getJobDetail().getJobDataMap();
		ContentType type = (ContentType) map.get("contenttype");
		User user = (User) map.get("user");

		try{
			//Lucene query to be sure that I will get all fields of the contentlet
			String luceneQuery = "+structureName:" + type.variable() +
								" +working:true" +
								" +languageId:" + APILocator.getLanguageAPI().getDefaultLanguage().getId();

			//Identifiers will be updated 500 at a time
			Integer limit = 500;
			Integer offset = 0;

			//Get all the ContentletSearch
			List<ContentletSearch> contenletSearchList = contentletAPI.searchIndex(luceneQuery, limit, offset, "random", user, false);

			//If the query result is not empty
			while(!contenletSearchList.isEmpty()){
				//Start 500 (limit) transaction
				HibernateUtil.startTransaction();

				//Iterates all the ContentletSearch of the query
				for(ContentletSearch contentletSearch : contenletSearchList){
					//Get the identifier of each contentlet
					Identifier identifier= APILocator.getIdentifierAPI().find(contentletSearch.getIdentifier());

					//Gets from hibernate all the Data of the Contentlet
					com.dotmarketing.portlets.contentlet.business.Contentlet fatty =
							(com.dotmarketing.portlets.contentlet.business.Contentlet)HibernateUtil
							.load(com.dotmarketing.portlets.contentlet.business.Contentlet.class, contentletSearch.getInode());

					//Check if the new Publish Date Var is not null
					if(UtilMethods.isSet(type.publishDateVar())){
						//Sets the identifier SysPublishDate to the new Structure/Content Publish Date Var
						identifier.setSysPublishDate((Date)fatty.getMap().get(type.publishDateVar()));
					}else{
						identifier.setSysPublishDate(null);
					}

					//Check if the new Expire Date Var is not null
					if(UtilMethods.isSet(type.expireDateVar())){
						//Sets the identifier SysExpireDate to the new Structure/Content Expire Date Var
						identifier.setSysExpireDate((Date)fatty.getMap().get(type.expireDateVar()));
					}else{
						identifier.setSysExpireDate(null);
					}

					//Saves the update
					APILocator.getIdentifierAPI().save(identifier);
					//Clears Identifier Cache
					CacheLocator.getIdentifierCache().removeFromCacheByIdentifier(contentletSearch.getIdentifier());
					//Clears Contentlet Cache for each language and version
					for(Language lan : APILocator.getLanguageAPI().getLanguages()) {
						ContentletVersionInfo versionInfo = APILocator.getVersionableAPI().getContentletVersionInfo(identifier.getId(), lan.getId()) ;
						if(versionInfo!=null && UtilMethods.isSet(versionInfo.getIdentifier())) {
							CacheLocator.getContentletCache().remove(versionInfo.getWorkingInode());
							if(UtilMethods.isSet(versionInfo.getLiveInode())) {
								CacheLocator.getContentletCache().remove(versionInfo.getLiveInode());

							}
						}
					}
				}
				//Commit 500 (limit) transaction
				HibernateUtil.closeAndCommitTransaction();

				//Next 500
				limit += limit;
				offset += limit;
				contenletSearchList = contentletAPI.searchIndex(luceneQuery, limit, offset, "random", user, false);
			}

			//Send Notification
			APILocator.getNotificationAPI().generateNotification(
					new I18NMessage("notification.identifier.datejob.info.title"), // title = Identifier Notification
					new I18NMessage("notifications_structure_identifiers_updated"),
					null, // no actions
					NotificationLevel.INFO,
					NotificationType.GENERIC,
					user.getUserId(),
					user.getLocale()
			);
		} catch (DotDataException e) {
			Logger.error(this, e.getMessage(), e);
			throw new DotRuntimeException(e.getMessage(), e);
		} catch (DotSecurityException e) {
			Logger.error(CascadePermissionsJob.class, e.getMessage(), e);
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
	
	/**
	 * Setup the job and trigger it immediately
	 * 
	 * @param type {@link ContentType}
	 * @param user      {@link User}

	 */
	public static void triggerJobImmediately (ContentType type, User user) {

		String randomID = UUID.randomUUID().toString();
		JobDataMap dataMap = new JobDataMap();
		
		dataMap.put("contenttype", type);
		dataMap.put("user", user);
		
		JobDetail jd = new JobDetail("IdentifierDateJob-" + randomID, "identifier_date_job", IdentifierDateJob.class);
		jd.setJobDataMap(dataMap);
		jd.setDurability(false);
		jd.setVolatility(false);
		jd.setRequestsRecovery(true);
		
		long startTime = System.currentTimeMillis();
		SimpleTrigger trigger = new SimpleTrigger("IdentifierDateTrigger-" + randomID, "identifier_data_triggers",  new Date(startTime));
		
		try {
			Scheduler sched = DotSchedulerFactory.getInstance().getScheduler();
			sched.scheduleJob(jd, trigger);
		} catch (SchedulerException e) {
			Logger.error(IdentifierDateJob.class, "Error scheduling the Identifier Date Job", e);
			throw new DotRuntimeException("Error scheduling the Identifier Date Job", e);
		}
		AdminLogger.log(IdentifierDateJob.class, "triggerJobImmediately", "Updating Identifiers Dates of: "+ type.name());
	
	}
}
