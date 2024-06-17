package com.dotmarketing.quartz.job;

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
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.quartz.TriggerBuilder;

/**
 * @author Oscar Arrieta
 */
public class IdentifierDateJob implements Job {

	@Override
	public void execute(final JobExecutionContext jobContext) throws JobExecutionException {
		final ContentletAPI contentletAPI = APILocator.getContentletAPI();
		final JobDataMap map = jobContext.getJobDetail().getJobDataMap();
		final ContentType type = (ContentType) map.get("contenttype");
		final User user = (User) map.get("user");

		try {
			final String luceneQuery = "+structureName:" + type.variable() +
					" +working:true" +
					" +languageId:" + APILocator.getLanguageAPI().getDefaultLanguage().getId();
			Integer limit = 500;
			Integer offset = 0;
			List<ContentletSearch> contenletSearchList = contentletAPI.searchIndex(luceneQuery, limit, offset, "random", user, false);

			while (!contenletSearchList.isEmpty()) {
				HibernateUtil.startTransaction();

				for (ContentletSearch contentletSearch : contenletSearchList) {
					final Identifier identifier = APILocator.getIdentifierAPI().find(contentletSearch.getIdentifier());
					final Contentlet contentlet = contentletAPI.find(contentletSearch.getInode(), user, false);

					if (UtilMethods.isSet(type.publishDateVar())) {
						identifier.setSysPublishDate((Date) contentlet.getMap().get(type.publishDateVar()));
					} else {
						identifier.setSysPublishDate(null);
					}

					if (UtilMethods.isSet(type.expireDateVar())) {
						identifier.setSysExpireDate((Date) contentlet.getMap().get(type.expireDateVar()));
					} else {
						identifier.setSysExpireDate(null);
					}

					APILocator.getIdentifierAPI().save(identifier);
					CacheLocator.getIdentifierCache().removeFromCacheByIdentifier(contentletSearch.getIdentifier());

					for (Language lan : APILocator.getLanguageAPI().getLanguages()) {
						Optional<ContentletVersionInfo> versionInfo = APILocator.getVersionableAPI().getContentletVersionInfo(identifier.getId(), lan.getId());
						if (versionInfo.isPresent() && UtilMethods.isSet(versionInfo.get().getIdentifier())) {
							CacheLocator.getContentletCache().remove(versionInfo.get().getWorkingInode());
							if (UtilMethods.isSet(versionInfo.get().getLiveInode())) {
								CacheLocator.getContentletCache().remove(versionInfo.get().getLiveInode());
							}
						}
					}
				}

				HibernateUtil.closeAndCommitTransaction();
				offset += limit;
				contenletSearchList = contentletAPI.searchIndex(luceneQuery, limit, offset, "random", user, false);
			}

			APILocator.getNotificationAPI().generateNotification(
					new I18NMessage("notification.identifier.datejob.info.title"),
					new I18NMessage("notifications_structure_identifiers_updated"),
					null,
					NotificationLevel.INFO,
					NotificationType.GENERIC,
					user.getUserId(),
					user.getLocale()
			);
		} catch (DotDataException | DotSecurityException e) {
			Logger.error(this, e.getMessage(), e);
			throw new DotRuntimeException(e.getMessage(), e);
		} finally {
			try {
				HibernateUtil.closeSession();
			} catch (DotHibernateException e) {
				Logger.warn(this, e.getMessage(), e);
			} finally {
				DbConnectionFactory.closeConnection();
			}
		}
	}

	public static void triggerJobImmediately(ContentType type, User user) {
		String randomID = UUID.randomUUID().toString();
		JobDataMap dataMap = new JobDataMap();
		dataMap.put("contenttype", type);
		dataMap.put("user", user);

		JobDetail jd = JobBuilder.newJob(IdentifierDateJob.class)
				.withIdentity("IdentifierDateJob-" + randomID, "identifier_date_job")
				.usingJobData(dataMap)
				.storeDurably(false)
				.requestRecovery(true)
				.build();

		SimpleTrigger trigger = (SimpleTrigger) TriggerBuilder.newTrigger()
				.withIdentity("IdentifierDateTrigger-" + randomID, "identifier_data_triggers")
				.startNow()
				.build();

		try {
			Scheduler sched = QuartzUtils.getScheduler();
			sched.scheduleJob(jd, trigger);
		} catch (SchedulerException e) {
			Logger.error(IdentifierDateJob.class, "Error scheduling the Identifier Date Job", e);
			throw new DotRuntimeException("Error scheduling the Identifier Date Job", e);
		}
		AdminLogger.log(IdentifierDateJob.class, "triggerJobImmediately", "Updating Identifiers Dates of: " + type.name());
	}
}