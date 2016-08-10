package com.liferay.portal.ejb;

import com.dotcms.repackage.com.ibm.icu.util.GregorianCalendar;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

import java.util.Date;

/**
 * Created by Nollymar Longa on 8/8/16.
 */
public class UserTestUtil {

    /**
     * Create a new user given a user name. Also, consider if the new user must be marked as an user to be deleted.
     * The new user will be saved in DB if saveInDatabase is set to true
     */
    public static User getUser(String userName, boolean toBeDeleted, boolean saveInDatabase)
        throws DotSecurityException, DotDataException {
        return getUser(userName, toBeDeleted, saveInDatabase, GregorianCalendar.getInstance().getTime());
    }

    /**
     * Create a new user given a user name. Also, consider if the new user must be marked as an user to be deleted.
     * A deletion date can be set.
     * The new user will be saved in DB if saveInDatabase is set to true
     */
    public static User getUser(String userName, boolean toBeDeleted, boolean saveInDatabase, Date deletionDate)
        throws DotSecurityException, DotDataException {

        User user = null;
        User sysuser = APILocator.getUserAPI().getSystemUser();
        UserAPI userAPI = APILocator.getUserAPI();

        try {
            user = userAPI.loadUserById(userName, sysuser, false);
        } catch (Exception ex) {
            user = null;
        } finally {
            if (user == null || !UtilMethods.isSet(user.getUserId())) {
                user = userAPI.createUser(userName, userName + "@fake.org");

                user.setDeleteInProgress(toBeDeleted);

                if (toBeDeleted) {
                    user.setDeleteDate(deletionDate);

                }

                if (saveInDatabase) {
                    userAPI.save(user, sysuser, false);
                }
            }
        }
        return user;
    }
}
