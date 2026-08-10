package com.dotmarketing.quartz.job;

import com.dotcms.content.index.IndexContentletScroll;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.notifications.bean.NotificationLevel;
import com.dotcms.notifications.bean.NotificationType;
import com.dotcms.util.I18NMessage;
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
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.quartz.QuartzUtils;
import com.dotmarketing.util.AdminLogger;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;

/**
 * @author Oscar Arrieta
 *
 */
public class IdentifierDateJob implements Job {

	/** Identifiers are updated this many at a time, one transaction per batch. */
	private static final int BATCH_SIZE = 500;

	/* (non-Javadoc)
	 * @see org.quartz.Job#execute(org.quartz.JobExecutionContext)
	 */
	@Override
	public void execute(final JobExecutionContext jobContext) throws JobExecutionException {
		final ContentletAPI contentletAPI = APILocator.getContentletAPI();

		final JobDataMap map = jobContext.getJobDetail().getJobDataMap();
		final ContentType type = (ContentType) map.get("contenttype");
		final User user = (User) map.get("user");

		try{
			//Lucene query to be sure that I will get all fields of the contentlet
			final String luceneQuery = "+structureName:" + type.variable() +
								" +working:true" +
								" +languageId:" + APILocator.getLanguageAPI().getDefaultLanguage().getId();

			// Identifiers are updated in batches, one transaction each, over a scroll cursor.
			//
			// The previous offset pagination was doubling the page size and then adding the NEW
			// size to the offset (limit += limit; offset += limit), so it walked
			// [0,500) [1000,2000) [3000,5000) [7000,11000)… — skipping an ever-widening gap
			// between pages. Worse, once the doubled limit passed ContentletAPI's MAX_LIMIT
			// (10000), searchIndex switches to the scroll branch and ignores the offset entirely:
			// it returned the FULL result set, which is never empty, so on a content type with
			// more than ~15000 working contentlets the loop never terminated at all and
			// reprocessed everything on every pass. Randomly sorting the result made it worse
			// still — pages of an offset-paginated random sort have no relation to one another.
			//
			// A scroll cursor holds one consistent snapshot for the whole walk, which is exactly
			// the guarantee this job needs: every matching contentlet visited once.
			try (final IndexContentletScroll contentletScroll =
					contentletAPI.createScrollQuery(luceneQuery, user, false, BATCH_SIZE, null)) {

				List<ContentletSearch> contentletSearchList;

				while((contentletSearchList = contentletScroll.nextBatch()) != null
						&& !contentletSearchList.isEmpty()){
					//Start batch transaction
					HibernateUtil.startTransaction();

					for(final ContentletSearch contentletSearch : contentletSearchList){
						updateIdentifierDates(contentletSearch, type, user, contentletAPI);
					}

					//Commit batch transaction
					HibernateUtil.closeAndCommitTransaction();
				}
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
	 * Copies the content type's publish/expire date values from one contentlet onto its identifier,
	 * then evicts the identifier and contentlet cache entries the change invalidates.
	 *
	 * @param contentletSearch the index hit to process
	 * @param type             the content type whose date vars are being applied
	 * @param user             the user the job runs as
	 * @param contentletAPI    resolved once by the caller
	 */
	private void updateIdentifierDates(final ContentletSearch contentletSearch, final ContentType type,
			final User user, final ContentletAPI contentletAPI)
			throws DotDataException, DotSecurityException {

		//Get the identifier of each contentlet
		final Identifier identifier = APILocator.getIdentifierAPI().find(contentletSearch.getIdentifier());

		//Gets contentlet info
		final Contentlet contentlet = contentletAPI.find(contentletSearch.getInode(), user, false);

		// A hit the database cannot resolve is skipped, not fatal. The index and the database drift
		// for ordinary reasons (an interrupted delete, a stale shadow index during the ES→OS
		// migration), and neither unresolved value is safe to carry into the body below: a null
		// contentlet throws on getMap() and takes down the whole job, so every remaining contentlet
		// of this type silently keeps a stale date; and an unresolved identifier comes back from
		// find() as an EMPTY Identifier (IdentifierFactoryImpl.check404), which save() would persist
		// as a brand-new row under a generated id, with null parent_path / asset_name / host_inode —
		// rejected by identifier_parent_path_check, or corrupt data on any engine that lets it
		// through. See issue #36501.
		if (contentlet == null || !UtilMethods.isSet(identifier.getId())) {
			Logger.warn(this, "Skipping index hit that does not resolve in the database"
					+ " — identifier: " + contentletSearch.getIdentifier()
					+ ", inode: " + contentletSearch.getInode()
					+ ", content type: " + type.variable());
			return;
		}

		//Check if the new Publish Date Var is not null
		if (UtilMethods.isSet(type.publishDateVar())) {
			//Sets the identifier SysPublishDate to the new Structure/Content Publish Date Var
			identifier.setSysPublishDate((Date) contentlet.getMap().get(type.publishDateVar()));
		} else {
			identifier.setSysPublishDate(null);
		}

		//Check if the new Expire Date Var is not null
		if (UtilMethods.isSet(type.expireDateVar())) {
			//Sets the identifier SysExpireDate to the new Structure/Content Expire Date Var
			identifier.setSysExpireDate((Date) contentlet.getMap().get(type.expireDateVar()));
		} else {
			identifier.setSysExpireDate(null);
		}

		//Saves the update
		APILocator.getIdentifierAPI().save(identifier);
		//Clears Identifier Cache
		CacheLocator.getIdentifierCache().removeFromCacheByIdentifier(contentletSearch.getIdentifier());
		//Clears Contentlet Cache for each language and version
		for (final Language lan : APILocator.getLanguageAPI().getLanguages()) {
			final Optional<ContentletVersionInfo> versionInfo =
					APILocator.getVersionableAPI().getContentletVersionInfo(identifier.getId(), lan.getId());
			if (versionInfo.isPresent() && UtilMethods.isSet(versionInfo.get().getIdentifier())) {
				CacheLocator.getContentletCache().remove(versionInfo.get().getWorkingInode());
				if (UtilMethods.isSet(versionInfo.get().getLiveInode())) {
					CacheLocator.getContentletCache().remove(versionInfo.get().getLiveInode());
				}
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
			Scheduler sched = QuartzUtils.getScheduler();
			sched.scheduleJob(jd, trigger);
		} catch (SchedulerException e) {
			Logger.error(IdentifierDateJob.class, "Error scheduling the Identifier Date Job", e);
			throw new DotRuntimeException("Error scheduling the Identifier Date Job", e);
		}
		AdminLogger.log(IdentifierDateJob.class, "triggerJobImmediately", "Updating Identifiers Dates of: "+ type.name());
	
	}
}
