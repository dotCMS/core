package com.liferay.portal.ejb;

import com.dotcms.repackage.com.ibm.icu.util.GregorianCalendar;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.User;

import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by nollymar on 8/4/16.
 */
public class UserFinderTest {

    private static User systemUser;
    private static UserAPI userAPI;


    @BeforeClass
    public static void prepare() throws DotDataException {

        //Setting the test user
        systemUser = APILocator.getUserAPI().getSystemUser();
        userAPI = APILocator.getUserAPI();
    }


    @Test
    public void testFindBySkinId() throws DotDataException, DotSecurityException, SystemException {
        String id;
        String userName;
        User user;

        id = String.valueOf(new Date().getTime());
        userName = "user" + id;

        user = getUser(userName, false);
        user.setSkinId(userName);

        userAPI.save(user, systemUser, false);

        List<User> users = UserFinder.findBySkinId();

        assertNotNull(users);
        assertTrue(users.size() == 1);
        assertTrue(users.contains(user));

        try {
            userAPI.delete(user, userAPI.getDefaultUser(), userAPI.getSystemUser(), false);
        } catch (DotSecurityException e) {
            // no need to validate this
        }

    }

    @Test
    public void testFindBySkinIdMarkDeleted() throws DotDataException, DotSecurityException, SystemException {
        String id;
        String userName;
        User user;

        id = String.valueOf(new Date().getTime());
        userName = "user" + id;

        user = getUser(userName, true);

        user.setSkinId(userName);

        userAPI.save(user, systemUser, false);

        List<User> users = UserFinder.findBySkinId();

        assertNotNull(users);
        assertTrue(users.size() == 0);

        try {
            userAPI.delete(user, userAPI.getDefaultUser(), userAPI.getSystemUser(), false);
        } catch (DotSecurityException e) {
            // no need to validate this
        }
    }

    @Test
    public void testFindByC_SMS() throws DotDataException, DotSecurityException, SystemException {

        String id;
        String companyId;
        String userName;
        User user;

        id = String.valueOf(new Date().getTime());
        userName = "user" + id;

        companyId = PublicCompanyFactory.getDefaultCompanyId();

        user = getUser(userName, false);
        user.setSmsId(user.getEmailAddress());

        userAPI.save(user, systemUser, false);

        List<User> users = UserFinder.findByC_SMS(companyId);

        assertNotNull(users);
        assertTrue(users.size() == 1);
        assertTrue(users.contains(user));

        try {
            userAPI.delete(user, userAPI.getDefaultUser(), userAPI.getSystemUser(), false);
        } catch (DotSecurityException e) {
            // no need to validate this
        }
    }

    @Test
    public void testFindByC_SMSMarkDeleted() throws DotSecurityException, DotDataException, SystemException {
        String id;
        String companyId;
        String userName;
        User user;

        id = String.valueOf(new Date().getTime());
        userName = "user" + id;

        companyId = PublicCompanyFactory.getDefaultCompanyId();

        user = getUser(userName, true);
        user.setSmsId(user.getEmailAddress());

        userAPI.save(user, systemUser, false);

        List<User> users = UserFinder.findByC_SMS(companyId);

        assertNotNull(users);
        assertTrue(users.size() == 0);

        try {
            userAPI.delete(user, userAPI.getDefaultUser(), userAPI.getSystemUser(), false);
        } catch (DotSecurityException e) {
            // no need to validate this
        }
    }

    @Test
    public void testFindByAnd_C_FN_MN_LN_EA_M_BD_IM_A() throws DotDataException, DotSecurityException, SystemException {

        Calendar calendar;
        String id;
        String companyId;
        String userName;
        User user;

        id = String.valueOf(new Date().getTime());
        userName = "user" + id;

        companyId = PublicCompanyFactory.getDefaultCompanyId();

        calendar = Calendar.getInstance();
        calendar.add(Calendar.YEAR, -30);

        user = getUser(userName, false);
        user.setBirthday(calendar.getTime());

        userAPI.save(user, systemUser, false);

        List<User>
            users =
            UserFinder
                .findByAnd_C_FN_MN_LN_EA_M_BD_IM_A(companyId, userName, null, userName, user.getEmailAddress(), true,
                    new Timestamp(calendar.getTimeInMillis()), new Timestamp(Calendar.getInstance().getTimeInMillis()),
                    null, null, null, null, null, null, null, null, null);

        assertNotNull(users);
        assertTrue(users.size() == 1);
        assertTrue(users.contains(user));

        try {
            userAPI.delete(user, userAPI.getDefaultUser(), userAPI.getSystemUser(), false);
        } catch (DotSecurityException e) {
            // no need to validate this
        }
    }

    @Test
    public void testFindByAnd_C_FN_MN_LN_EA_M_BD_IM_AMarkDeleted()
        throws DotDataException, DotSecurityException, SystemException {

        Calendar calendar;
        String id;
        String companyId;
        String userName;
        User user;

        id = String.valueOf(new Date().getTime());
        userName = "user" + id;

        companyId = PublicCompanyFactory.getDefaultCompanyId();

        calendar = Calendar.getInstance();
        calendar.add(Calendar.YEAR, -30);

        user = getUser(userName, true);
        user.setBirthday(calendar.getTime());

        userAPI.save(user, systemUser, false);

        List<User>
            users =
            UserFinder
                .findByAnd_C_FN_MN_LN_EA_M_BD_IM_A(companyId, userName, null, userName, user.getEmailAddress(), true,
                    new Timestamp(calendar.getTimeInMillis()), new Timestamp(Calendar.getInstance().getTimeInMillis()),
                    null, null, null, null, null, null, null, null, null);

        assertNotNull(users);
        assertTrue(users.size() == 0);

        try {
            userAPI.delete(user, userAPI.getDefaultUser(), userAPI.getSystemUser(), false);
        } catch (DotSecurityException e) {
            // no need to validate this
        }
    }

    @Test
    public void testFindByOr_C_FN_MN_LN_EA_M_BD_IM_A() throws DotDataException, DotSecurityException, SystemException {

        Calendar calendar;
        String id;
        String companyId;
        String userName;
        User user;

        id = String.valueOf(new Date().getTime());
        userName = "user" + id;

        companyId = PublicCompanyFactory.getDefaultCompanyId();

        calendar = Calendar.getInstance();
        calendar.add(Calendar.YEAR, -30);

        user = getUser(userName, false);
        user.setBirthday(calendar.getTime());

        userAPI.save(user, systemUser, false);

        List<User>
            users =
            UserFinder
                .findByOr_C_FN_MN_LN_EA_M_BD_IM_A(companyId, userName, null, userName, user.getEmailAddress(), true,
                    new Timestamp(calendar.getTimeInMillis()), new Timestamp(Calendar.getInstance().getTimeInMillis()),
                    null, null, null, null, null, null, null, null, null);

        assertNotNull(users);
        assertTrue(users.size() == 1);
        assertTrue(users.contains(user));

        try {
            userAPI.delete(user, userAPI.getDefaultUser(), userAPI.getSystemUser(), false);
        } catch (DotSecurityException e) {
            // no need to validate this
        }
    }

    @Test
    public void testFindByOr_C_FN_MN_LN_EA_M_BD_IM_AMarkDeleted()
        throws DotDataException, DotSecurityException, SystemException {

        Calendar calendar;
        String id;
        String companyId;
        String userName;
        User user;

        id = String.valueOf(new Date().getTime());
        userName = "user" + id;

        companyId = PublicCompanyFactory.getDefaultCompanyId();

        user = getUser(userName, true);

        calendar = Calendar.getInstance();
        calendar.add(Calendar.YEAR, -30);

        user.setBirthday(calendar.getTime());

        userAPI.save(user, systemUser, false);

        List<User>
            users =
            UserFinder
                .findByOr_C_FN_MN_LN_EA_M_BD_IM_A(companyId, userName, null, userName, user.getEmailAddress(), true,
                    new Timestamp(calendar.getTimeInMillis()), new Timestamp(Calendar.getInstance().getTimeInMillis()),
                    null, null, null, null, null, null, null, null, null);

        assertNotNull(users);
        assertTrue(users.size() == 0);

        try {
            userAPI.delete(user, userAPI.getDefaultUser(), userAPI.getSystemUser(), false);
        } catch (DotSecurityException e) {
            // no need to validate this
        }
    }


    /**
     * Create a new user given a user name. Also, consider if the new user must be marked as an user to be deleted
     */
    private User getUser(String userName, boolean toBeDeleted)
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
