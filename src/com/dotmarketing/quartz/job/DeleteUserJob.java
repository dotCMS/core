package com.dotmarketing.quartz.job;

import com.dotcms.notifications.business.NotificationAPI;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.quartz.QuartzUtils;
import com.dotmarketing.util.AdminLogger;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;

import java.util.Date;
import java.util.UUID;

/**
 * Created by nollymar on 7/19/16.
 */
public class DeleteUserJob implements Job {

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
            sched.scheduleJob(jd, trigger);
        } catch (SchedulerException e) {
            Logger.error(DeleteFieldJob.class, "Error scheduling DeleteUserJob", e);
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
        notfAPI.info(String.format("Deletion of User '%s' has been started. Replacement user: '%s",
            userToDelete.getUserId() + "/" + userToDelete.getFullName(),
            replacementUser.getUserId() + "/" + replacementUser.getFullName()), user.getUserId());

        try {
            HibernateUtil.startTransaction();

            uAPI.delete(userToDelete, replacementUser, user, respectFrontEndRoles);
            HibernateUtil.commitTransaction();

            notfAPI.info(String.format("User '%s' was deleted succesfully.",
                userToDelete.getUserId()), user.getUserId());
        } catch (Exception e) {
            try {
                HibernateUtil.rollbackTransaction();
            } catch (DotHibernateException e1) {
                Logger.error(this, "Error in rollback transaction", e);
            }
            Logger.error(this, String.format("Unable to delete user '%s'.",
                userToDelete.getUserId() + "/" + userToDelete.getFullName()), e);
            notfAPI.error(String.format("Unable to delete user '%s'.",
                userToDelete.getUserId() + "/" + userToDelete.getFullName()), user.getUserId());
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
