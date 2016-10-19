package com.dotmarketing.quartz.job;

import com.dotcms.notifications.business.NotificationAPI;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.com.google.common.base.Preconditions;
import com.dotcms.repackage.com.google.common.base.Strings;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.structure.factories.FieldFactory;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.quartz.QuartzUtils;
import com.dotmarketing.services.ContentletMapServices;
import com.dotmarketing.services.ContentletServices;
import com.dotmarketing.services.StructureServices;
import com.dotmarketing.util.ActivityLogger;
import com.dotmarketing.util.AdminLogger;
import com.dotmarketing.util.Logger;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.model.User;
import org.quartz.*;

import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public class DeleteFieldJob implements Job {

    private final PermissionAPI permAPI;
    private final ContentletAPI conAPI;
    private final NotificationAPI notfAPI;
    private final DeleteFieldJobHelper deleteFieldJobHelper;

    public DeleteFieldJob() {
        this(APILocator.getPermissionAPI(),
            APILocator.getContentletAPI(),
            APILocator.getNotificationAPI(),
            DeleteFieldJobHelper.INSTANCE);
    }

    @VisibleForTesting
    public DeleteFieldJob(final PermissionAPI permAPI,
                          final ContentletAPI conAPI,
                          final NotificationAPI notfAPI,
                          final DeleteFieldJobHelper deleteFieldJobHelper) {

        this.permAPI = permAPI;
        this.conAPI = conAPI;
        this.notfAPI = notfAPI;
        this.deleteFieldJobHelper = deleteFieldJobHelper;
    }

    public static void triggerDeleteFieldJob(Structure structure, Field field, User user) {
        Preconditions.checkNotNull(structure, "Structure can't be null");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(structure.getInode()),
                "Structure Id can't be null or empty");
        Preconditions.checkNotNull(field, "Field can't be null");
        Preconditions.checkNotNull(user, "User can't be null");

        JobDataMap dataMap = new JobDataMap();
        dataMap.put("structure", structure);
        dataMap.put("field", field);
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
                String.format("Deleting Field '%s' for Structure with id: %s",
                        field.getVelocityVarName(), structure.getInode()));
    }

    public void execute(JobExecutionContext jobContext) throws JobExecutionException {
        JobDataMap map = jobContext.getJobDetail().getJobDataMap();
        Structure structure = (Structure) map.get("structure");
        Field field = (Field) map.get("field");
        User user = (User) map.get("user");

        Preconditions.checkNotNull(structure, "Structure can't be null");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(structure.getInode()),
                "Structure Id can't be null or empty");
        Preconditions.checkNotNull(field, "Field can't be null");
        Preconditions.checkNotNull(user, "User can't be null");

        try {
            if (!permAPI.doesUserHavePermission(structure,
                    PermissionAPI.PERMISSION_PUBLISH, user, false)) {
                throw new DotSecurityException("Must be able to publish structure to clean all the fields with user: "
                        + (user != null ? user.getUserId() : "Unknown"));
            }

            String type = field.getFieldType();
            final Locale userLocale = user.getLocale();

            this.deleteFieldJobHelper.generateNotificationStartDeleting(this.notfAPI, userLocale, user.getUserId(),
                    field.getVelocityVarName(), field.getInode(), structure.getInode());

            HibernateUtil.startTransaction();

            if (!APILocator.getFieldAPI().isElementConstant(field)
                    && !Field.FieldType.LINE_DIVIDER.toString().equals(type)
                    && !Field.FieldType.TAB_DIVIDER.toString().equals(type)
                    && !Field.FieldType.RELATIONSHIPS_TAB.toString().equals(type)
                    && !Field.FieldType.CATEGORIES_TAB.toString().equals(type)
                    && !Field.FieldType.PERMISSIONS_TAB.toString().equals(type)
                    && !Field.FieldType.HOST_OR_FOLDER.toString().equals(type)) {

                conAPI.cleanField(structure, field, APILocator.getUserAPI().getSystemUser(), false);
            }
            FieldFactory.deleteField(field);
            // Call the commit method to avoid a deadlock
            HibernateUtil.commitTransaction();

            this.deleteFieldJobHelper.generateNotificationEndDeleting(this.notfAPI, userLocale, user.getUserId(),
                    field.getVelocityVarName(), field.getInode(), structure.getInode());

            ActivityLogger.logInfo(ActivityLogger.class, "Delete Field Action", "User " + user.getUserId() + "/"
                    + user.getFirstName() + " deleted field " + field.getFieldName() + " to " + structure.getName()
                    + " Structure.");

            FieldsCache.removeFields(structure);

            CacheLocator.getContentTypeCache().remove(structure);
            StructureServices.removeStructureFile(structure);

            //Refreshing permissions
            if (field.getFieldType().equals("host or folder")) {
                conAPI.cleanHostField(structure, APILocator.getUserAPI().getSystemUser(), false);
                permAPI.resetChildrenPermissionReferences(structure);
            }
            StructureFactory.saveStructure(structure);
            // rebuild contentlets indexes
            conAPI.reindex(structure);
            // remove the file from the cache
            ContentletServices.removeContentletFile(structure);
            ContentletMapServices.removeContentletMapFile(structure);
        } catch (Exception e) {
            try {
                HibernateUtil.rollbackTransaction();
            } catch (DotHibernateException e1) {
                Logger.error(this, "Error in rollback transaction", e);
            }
            Logger.error(this, String.format("Unable to delete field '%s'. Field Inode: %s, Structure Inode: %s",
                    field.getVelocityVarName(), field.getInode(), structure.getInode()), e);

            try {

                this.deleteFieldJobHelper.generateNotificationUnableDelete(this.notfAPI,
                        user.getLocale(), user.getUserId(), field.getVelocityVarName(), field.getInode(), structure.getInode());
            } catch (LanguageException | DotDataException e1) {
                Logger.error(this, e1.getMessage(), e1);
            }
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
