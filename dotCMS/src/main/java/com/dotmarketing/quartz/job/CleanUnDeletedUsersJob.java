package com.dotmarketing.quartz.job;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.StatefulJob;

import java.util.List;

/**
 * Clean user_ table when an user deletion could no be completed for any reason.
 * Restart status of those users that did not finish deletion process (rollback)
 *
 * Created by Nollymar Longa on 7/25/16.
 */
public class CleanUnDeletedUsersJob implements StatefulJob {


    private final UserAPI userAPI;

    public CleanUnDeletedUsersJob() {
        userAPI = APILocator.getUserAPI();
    }

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        try {
            User systemUser = userAPI.getSystemUser();
            HibernateUtil.startTransaction();

            //Search for dirty registers
            List<User> users = userAPI.getUnDeletedUsers();

            //Clean registers
            for (User user : users) {
                user.setDeleteDate(null);
                user.setDeleteInProgress(false);
                userAPI.save(user, systemUser, false);
            }
            HibernateUtil.closeAndCommitTransaction();
        } catch (DotDataException | DotSecurityException e) {
            try {
                HibernateUtil.rollbackTransaction();
            } catch (DotHibernateException e1) {
                Logger.error(CleanUnDeletedUsersJob.class, "Error executing rollback", e1);
            }
            Logger.error(CleanUnDeletedUsersJob.class, "Error executing CleanUnDeletedUsersJob", e);
        }
    }
}
