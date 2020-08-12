package com.dotmarketing.quartz.job;

import com.dotcms.notifications.business.NotificationAPI;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.quartz.QuartzUtils;
import com.dotmarketing.util.AdminLogger;
import com.dotmarketing.util.Logger;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;

import org.quartz.*;

import java.text.MessageFormat;
import java.util.Date;
import java.util.UUID;

/**
 * Created by nollymar on 7/19/16.
 */
public class DeleteUserJob implements StatefulJob {

    private final UserAPI uAPI;
    private final NotificationAPI notfAPI;

    public DeleteUserJob() {
        uAPI = APILocator.getUserAPI();
        notfAPI = APILocator.getNotificationAPI();
    }

    public static void triggerDeleteUserJob(User userToDelete, User replacementUser, User user,
                                            boolean respectFrontEndRoles) {
        JobDataMap dataMap = new JobDataMap();
        dataMap.put("userToDelete", userToDelete);
        dataMap.put("replacementUser", replacementUser);
        dataMap.put("user", user);
        dataMap.put("respectFrontEndRoles", respectFrontEndRoles);

        String randomID = UUID.randomUUID().toString();

        JobDetail jd = new JobDetail("DeleteUserJob-" + randomID, "delete_user_jobs", DeleteUserJob.class);
        jd.setJobDataMap(dataMap);
        jd.setDurability(false);
        jd.setVolatility(false);
        jd.setRequestsRecovery(true);

        long startTime = System.currentTimeMillis();
        SimpleTrigger trigger = new SimpleTrigger("deleteUserTrigger-" + randomID, "delete_user_triggers",
            new Date(startTime));

        try {
            Scheduler sched = QuartzUtils.getSequentialScheduler();
            UserAPI userAPI = APILocator.getUserAPI();
            NotificationAPI notAPI = APILocator.getNotificationAPI();

            String deleteInProgress = MessageFormat.format(LanguageUtil.get(user,
                "com.dotmarketing.business.UserAPI.delete.inProgress"),
                userToDelete.getUserId() + "/" + userToDelete.getFullName());

            synchronized (userToDelete.getUserId().intern()) {
                User freshUser = userAPI.loadUserById(userToDelete.getUserId());
                if(! freshUser.isDeleteInProgress()) {
                    userAPI.markToDelete(userToDelete);
                    sched.scheduleJob(jd, trigger);
                } else {
                    notAPI.info(deleteInProgress, user.getUserId());
                }
            }


        } catch (SchedulerException e) {
            Logger.error(DeleteUserJob.class, "Error scheduling DeleteUserJob", e);

            //Rolling back of user status (deleteInProgress)
            userToDelete.setDeleteDate(null);
            userToDelete.setDeleteInProgress(false);
            try {
                APILocator.getUserAPI().save(userToDelete, user, false);
            } catch (DotDataException | DotSecurityException e1) {
                Logger.error(DeleteUserJob.class, "Error in rollback transaction", e);
            }

            throw new DotRuntimeException("Error scheduling DeleteUserJob", e);
        } catch (Exception e) {
            Logger.error(DeleteUserJob.class, "Error scheduling DeleteUserJob", e);
            throw new DotRuntimeException("Error scheduling DeleteUserJob", e);
        }

        AdminLogger.log(DeleteUserJob.class, "triggerJobImmediately",
            String.format("Deleting User '%s'", userToDelete.getFullName()));

    }

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {

        JobDataMap map = jobExecutionContext.getJobDetail().getJobDataMap();
        User userToDelete = (User) map.get("userToDelete");
        User replacementUser = (User) map.get("replacementUser");
        User user = (User) map.get("user");
        Boolean respectFrontEndRoles = (Boolean) map.get("respectFrontEndRoles");
        String errorMessage = null;

        try {
            String startMessage = MessageFormat.format(LanguageUtil.get(user,
                "com.dotmarketing.business.UserAPI.delete.start"),
                userToDelete.getUserId() + "/" + userToDelete.getFullName(),
                replacementUser.getUserId() + "/" + replacementUser.getFullName());

            String finishMessage = MessageFormat.format(LanguageUtil.get(user,
                "com.dotmarketing.business.UserAPI.delete.success"),
                userToDelete.getUserId() + "/" + userToDelete.getFullName());

            errorMessage = MessageFormat.format(LanguageUtil.get(user,
                "com.dotmarketing.business.UserAPI.delete.error"),
                userToDelete.getUserId() + "/" + userToDelete.getFullName());

            notfAPI.info(startMessage, user.getUserId());

            HibernateUtil.startTransaction();

            uAPI.delete(userToDelete, replacementUser, user, respectFrontEndRoles);
            HibernateUtil.closeAndCommitTransaction();

            notfAPI.info(finishMessage, user.getUserId());
        } catch (Exception e) {
            try {
                HibernateUtil.rollbackTransaction();

                //Rolling back of user status (deleteInProgress)
                userToDelete.setDeleteDate(null);
                userToDelete.setDeleteInProgress(false);
                uAPI.save(userToDelete, user, false);
            } catch (DotDataException | DotSecurityException e1) {
                Logger.error(this, "Error in rollback transaction", e);
            }

            Logger.error(this, String.format("Unable to delete user '%s'.",
                userToDelete.getUserId() + "/" + userToDelete.getFullName()), e);

            notfAPI.error(errorMessage, user.getUserId());
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
