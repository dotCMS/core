package com.liferay.portal.ejb;

import com.dotcms.repackage.com.ibm.icu.util.GregorianCalendar;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.model.User;

/**
 * Created by Nollymar Longa on 8/8/16.
 */
public class UserTestUtil {

    /**
     * Create a new user given a user name. Also, consider if the new user must be marked as an user to be deleted
     */
    public static User getUser(String userName, boolean toBeDeleted)
        throws DotSecurityException, DotDataException {

        User user;
        user = APILocator.getUserAPI().createUser(userName, userName + "@fake.org");

        user.setDeleteInProgress(toBeDeleted);

        if (toBeDeleted) {
            user.setDeleteDate(GregorianCalendar.getInstance().getTime());

        }

        return user;
    }
}
