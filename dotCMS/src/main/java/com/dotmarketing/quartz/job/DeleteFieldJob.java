package com.dotmarketing.quartz.job;

import java.util.Date;
import java.util.Locale;
import java.util.UUID;

import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;

import com.dotcms.contenttype.business.FieldAPI;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.transform.contenttype.StructureTransformer;
import com.dotcms.contenttype.transform.field.LegacyFieldTransformer;
import com.dotcms.notifications.business.NotificationAPI;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.com.google.common.base.Preconditions;
import com.dotcms.repackage.com.google.common.base.Strings;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.structure.factories.FieldFactory;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.quartz.DotStatefulJob;
import com.dotmarketing.quartz.QuartzUtils;
import com.dotmarketing.util.ActivityLogger;
import com.dotmarketing.util.AdminLogger;
import com.dotmarketing.util.Logger;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.model.User;

/**
 * @deprecated This job was replaced by {@link FieldAPI#delete(com.dotcms.contenttype.model.field.Field)}
 * which internally calls {@link CleanUpFieldReferencesJob}
 * This Quartz Job is in charge of deleting a field from a specified Content
 * Type and its respective content from all the dotCMS contentlets that use such
 * a Content Type. The time taken by this job is determined by the number of
 * contentlet records that need to be updated. System notifications will be
 * issued under the following 3 circumstances:
 * <ol>
 * <li>The job has started with the deletion process.</li>
 * <li>The job has finished with the deletion process.</li>
 * <li>An error occurred the deletion process.</li>
 * </ol>
 * This job is triggered from the Content Type edition portlet, when clicking
 * the {@code Delete} button of any field.
 * <p>
 *
 * @author Daniel Silva
 * @version 3.5.1
 * @since Apr 26, 2016
 *
 */
@Deprecated
public class DeleteFieldJob extends DotStatefulJob {

	public static final String JOB_DATA_MAP_CONTENT_TYPE = "structure";
	public static final String JOB_DATA_MAP_FIELD = "field";
	public static final String JOB_DATA_MAP_USER = "user";

    private final FieldAPI contentTypeFieldAPI;
    private final com.dotmarketing.portlets.structure.business.FieldAPI fieldAPI;
    private final NotificationAPI notfAPI;
    private final DeleteFieldJobHelper deleteFieldJobHelper;

    /**
     * Default class constructor.
     */
    public DeleteFieldJob() {
        this(APILocator.getContentTypeFieldAPI(),
            APILocator.getFieldAPI(),
            APILocator.getNotificationAPI(),
            DeleteFieldJobHelper.INSTANCE);
    }

    @VisibleForTesting
    public DeleteFieldJob(
                          final FieldAPI contentTypeFieldAPI,
                          final com.dotmarketing.portlets.structure.business.FieldAPI fieldAPI,
                          final NotificationAPI notfAPI,
                          final DeleteFieldJobHelper deleteFieldJobHelper) {
        this.contentTypeFieldAPI = contentTypeFieldAPI;
        this.fieldAPI = fieldAPI;
        this.notfAPI = notfAPI;
        this.deleteFieldJobHelper = deleteFieldJobHelper;
    }

    /**
	 * Specifies the execution parameters for the field deletion job and
	 * schedules it for immediate execution.
	 *
	 * @param contentType
	 *            - The Content Type ({@linkplain Structure}) whose field will
	 *            be deleted.
	 * @param field
	 *            - The {@link Field} that will be deleted from the Content Type
	 *            and all its associated contentlets.
	 * @param user
	 *            - The {@link User} performing this action.
	 */
	public static void triggerDeleteFieldJob(final ContentType contentType,
			final com.dotcms.contenttype.model.field.Field field, final User user) {
		triggerDeleteFieldJob(new StructureTransformer(contentType).asStructure(),
				new LegacyFieldTransformer(field).asOldField(), user);
	}

	/**
	 * Specifies the execution parameters for the field deletion job and
	 * schedules it for immediate execution.
	 *
	 * @param contentType
	 *            - The Content Type ({@linkplain Structure}) whose field will
	 *            be deleted.
	 * @param field
	 *            - The {@link Field} that will be deleted from the Content Type
	 *            and all its associated contentlets.
	 * @param user
	 *            - The {@link User} performing this action.
	 */
    public static void triggerDeleteFieldJob(final Structure contentType, final Field field, final User user) {
        Preconditions.checkNotNull(contentType, "Content Type can't be null");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(contentType.getInode()),
                "Content Type Id can't be null or empty");
        Preconditions.checkNotNull(field, "Field can't be null");
        Preconditions.checkNotNull(user, "User can't be null");

        JobDataMap dataMap = new JobDataMap();
        dataMap.put(JOB_DATA_MAP_CONTENT_TYPE, contentType);
        dataMap.put("fieldInode", field.getInode());
        dataMap.put("user", user);

        String randomID = UUID.randomUUID().toString();

        JobDetail jd = new JobDetail("DeleteFieldJob-" + randomID, "delete_field_jobs", DeleteFieldJob.class);
        jd.setJobDataMap(dataMap);
        jd.setDurability(false);
        jd.setVolatility(false);
        jd.setRequestsRecovery(true);

        long startTime = System.currentTimeMillis();
        SimpleTrigger trigger = new SimpleTrigger("deleteFieldTrigger-"+randomID, "delete_field_triggers",
                new Date(startTime));

        try {
            Scheduler sched = QuartzUtils.getSequentialScheduler();
            sched.scheduleJob(jd, trigger);
        } catch (SchedulerException e) {
            Logger.error(DeleteFieldJob.class, "Error scheduling DeleteFieldJob", e);
            throw new DotRuntimeException("Error scheduling DeleteFieldJob", e);
        }

        AdminLogger.log(DeleteFieldJob.class, "triggerJobImmediately",
                String.format("Deleting Field '%s' for Content Type with id: %s",
                        field.getVelocityVarName(), contentType.getInode()));
    }

    /**
	 * Performs the field deletion process based on a set of specific
	 * parameters. This process involves deleting the Field object and then
	 * saving the Content Type.
	 *
	 * @param jobContext
	 *            - The {@link JobExecutionContext} object contains the
	 *            execution parameters of the job, including the data associated
	 *            to the Content Type and the Field to delete.
	 * @throws JobExecutionException
	 *             An error occurred when deleting the Field.
	 */
	@Override
	public void run(JobExecutionContext jobContext) throws JobExecutionException {
		final JobDataMap map = jobContext.getJobDetail().getJobDataMap();
		Structure contentType = null;
		if (map.get(JOB_DATA_MAP_CONTENT_TYPE) instanceof Structure) {
			contentType = Structure.class.cast(map.get(JOB_DATA_MAP_CONTENT_TYPE));
		} else {
			contentType = new StructureTransformer(ContentType.class.cast(map.get(JOB_DATA_MAP_CONTENT_TYPE))).asStructure();
		}
		Field field = null;
		if (map.containsKey("fieldInode")) {
			field = FieldFactory.getFieldByInode(map.getString("fieldInode"));
		} else {
			if (map.get(JOB_DATA_MAP_FIELD) instanceof Field) {
				field = Field.class.cast(map.get(JOB_DATA_MAP_FIELD));
			} else {
				field = new LegacyFieldTransformer(
						com.dotcms.contenttype.model.field.Field.class.cast(map.get(JOB_DATA_MAP_FIELD))).asOldField();
			}
		}
        final User user = (User) map.get(JOB_DATA_MAP_USER);
        Preconditions.checkNotNull(contentType, "Content Type can't be null");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(contentType.getInode()),
                "Content Type Id can't be null or empty");
        Preconditions.checkNotNull(field, "Field can't be null");
        Preconditions.checkNotNull(user, "User can't be null");

        try {
            final Locale userLocale = user.getLocale();

            this.deleteFieldJobHelper.generateNotificationStartDeleting(this.notfAPI, userLocale, user.getUserId(),
                    field.getVelocityVarName(), field.getInode(), contentType.getInode());

            contentTypeFieldAPI.delete(new LegacyFieldTransformer(field).from(), user);

            ActivityLogger.logInfo(ActivityLogger.class, "Delete Field Action", "User " + user.getUserId() + "/"
                    + user.getFirstName() + " deleted field " + field.getFieldName() + " from " + contentType.getName()
                    + " Content Type.");

            this.deleteFieldJobHelper.generateNotificationEndDeleting(this.notfAPI, userLocale, user.getUserId(),
                    field.getVelocityVarName(), field.getInode(), contentType.getInode());
        } catch (Exception e) {
            try {
                HibernateUtil.rollbackTransaction();
            } catch (DotHibernateException e1) {
                Logger.error(this, "Error in rollback transaction", e);
            }
            Logger.error(this, String.format("Unable to delete field '%s'. Field Inode: %s, Content Type Inode: %s, Content Type name: %s",
                    field.getVelocityVarName(), field.getInode(), contentType.getInode(), contentType.getName()), e);

            try {
                this.deleteFieldJobHelper.generateNotificationUnableDelete(this.notfAPI,
                        user.getLocale(), user.getUserId(), field.getVelocityVarName(), field.getInode(), contentType.getInode());
            } catch (LanguageException | DotDataException e1) {
                Logger.error(this, e1.getMessage(), e1);
            }

            throw new JobExecutionException(String.format("Unable to delete field '%s'. Field Inode: %s, Content Type Inode: %s, Content Type name: %s",
                    field.getVelocityVarName(), field.getInode(), contentType.getInode(), contentType.getName()), e);
        } finally {
            try {
                HibernateUtil.closeSession();
            } catch (DotHibernateException e) {
                Logger.warn(this, "exception while calling HibernateUtil.closeSession()", e);
            } finally {
                DbConnectionFactory.closeConnection();
            }
        }
	}

}
