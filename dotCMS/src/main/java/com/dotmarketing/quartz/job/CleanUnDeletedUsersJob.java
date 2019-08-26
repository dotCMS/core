package com.dotmarketing.quartz.job;

import com.dotcms.business.WrapInTransaction;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import java.util.List;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.StatefulJob;

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

    @WrapInTransaction
    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        try {
            User systemUser = userAPI.getSystemUser();

            //Search for dirty registers
            List<User> users = userAPI.getUnDeletedUsers();

            //Clean registers
            for (User user : users) {
                user.setDeleteDate(null);
                user.setDeleteInProgress(false);
                userAPI.save(user, systemUser, false);
            }
        } catch (DotDataException | DotSecurityException e) {
            Logger.error(CleanUnDeletedUsersJob.class, "Error executing CleanUnDeletedUsersJob", e);
        }
    }
}
