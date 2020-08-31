package com.dotmarketing.quartz.job;

import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.concurrent.lock.IdentifierStripedLock;
import com.dotcms.notifications.business.NotificationAPI;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.quartz.DotStatefulJob;
import com.dotmarketing.util.AdminLogger;
import com.dotmarketing.util.Logger;
import com.google.common.collect.ImmutableMap;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import java.io.Serializable;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.Map;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerException;
import org.quartz.Trigger;

/**
 * Created by nollymar on 7/19/16.
 */
public class DeleteUserJob extends DotStatefulJob {

    private final UserAPI uAPI;
    private final NotificationAPI notfAPI;

    public DeleteUserJob() {
        uAPI = APILocator.getUserAPI();
        notfAPI = APILocator.getNotificationAPI();
    }

    public static void triggerDeleteUserJob(final User userToDelete, final User replacementUser, final User user,
                                           final boolean respectFrontEndRoles) {

        final Map<String, Serializable> nextExecutionData = ImmutableMap
                .of("userToDelete", userToDelete,
                        "replacementUser", replacementUser,
                        "user", user,
                        "respectFrontEndRoles", respectFrontEndRoles);


        try {
            final UserAPI userAPI = APILocator.getUserAPI();
            final NotificationAPI notAPI = APILocator.getNotificationAPI();

            final String deleteInProgress = MessageFormat.format(LanguageUtil.get(user,
                "com.dotmarketing.business.UserAPI.delete.inProgress"),
                userToDelete.getUserId() + "/" + userToDelete.getFullName());

            final IdentifierStripedLock lockManager = DotConcurrentFactory.getInstance().getIdentifierStripedLock();
            lockManager.tryLock(userToDelete.getUserId(), () -> {
                final User freshUser = userAPI.loadUserById(userToDelete.getUserId());
                if (!freshUser.isDeleteInProgress()) {
                    userAPI.markToDelete(userToDelete);
                    try {
                        DotStatefulJob.enqueueTrigger(nextExecutionData, DeleteUserJob.class);
                    }catch (ParseException | SchedulerException | ClassNotFoundException e){
                        Logger.error(DeleteUserJob.class, "Error scheduling DeleteUserJob", e);
                        throw new DotRuntimeException("Error scheduling DeleteUserJob", e);
                    };
                } else {
                    notAPI.info(deleteInProgress, user.getUserId());
                }
            });

        } catch (Throwable e) {
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
        }

        AdminLogger.log(DeleteUserJob.class, "triggerJobImmediately",
            String.format("Deleting User '%s'", userToDelete.getFullName()));

    }

    @Override
    public void run(final JobExecutionContext jobExecutionContext) throws JobExecutionException {

        final Trigger trigger = jobExecutionContext.getTrigger();
        final Map<String, Serializable> map = getExecutionData(trigger, DeleteUserJob.class);
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
