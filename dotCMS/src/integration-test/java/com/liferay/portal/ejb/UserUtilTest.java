package com.liferay.portal.ejb;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.NoSuchCompanyException;
import com.liferay.portal.NoSuchUserException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import com.liferay.util.dao.hibernate.OrderByComparator;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by Nollymar Longa on 8/5/16.
 */
public class UserUtilTest {

    private static User systemUser;
    private static UserAPI userAPI;
    private static RoleAPI roleAPI;

    @BeforeClass
    public static void prepare() throws Exception {

        //Setting the test user
    	IntegrationTestInitService.getInstance().init();
        systemUser = APILocator.getUserAPI().getSystemUser();
        userAPI = APILocator.getUserAPI();
        roleAPI = APILocator.getRoleAPI();
    }

    @Test
    public void testFindByCompanyId() throws DotSecurityException, DotDataException, SystemException {
        String companyId;
        String id;
        String userName;
        User user;

        companyId = PublicCompanyFactory.getDefaultCompanyId();

        id = String.valueOf(new Date().getTime());
        userName = "user" + id;
        user = UserTestUtil.getUser(userName, true, true);

        List users = UserUtil.findByCompanyId(companyId);

        assertNotNull(users);
        assertTrue(users.size() > 0);
        assertTrue(!users.contains(user));

        userAPI.delete(user, userAPI.getDefaultUser(), userAPI.getSystemUser(), false);

    }

    @Test
    public void testFindByCompanyIdWithLimit() throws DotSecurityException, DotDataException, SystemException {
        String companyId;
        String id;
        String userName;
        User user;

        companyId = PublicCompanyFactory.getDefaultCompanyId();

        id = String.valueOf(new Date().getTime());
        userName = "user" + id;
        user = UserTestUtil.getUser(userName, true, true);

        List users = UserUtil.findByCompanyId(companyId, 0, 5, null);

        assertNotNull(users);
        assertTrue(users.size() > 0 && users.size() <= 5);
        assertTrue(!users.contains(user));

        userAPI.delete(user, userAPI.getDefaultUser(), userAPI.getSystemUser(), false);

    }

    @Test
    public void testFindByC_U() throws DotSecurityException, DotDataException, SystemException, NoSuchUserException {
        String companyId;
        String id;
        String userName;
        User user;

        companyId = PublicCompanyFactory.getDefaultCompanyId();

        id = String.valueOf(new Date().getTime());
        userName = "user" + id;
        user = UserTestUtil.getUser(userName, false, true);

        User result = UserUtil.findByC_U(companyId, userName);

        assertNotNull(result);
        assertTrue(result.equals(user));

        userAPI.delete(user, userAPI.getDefaultUser(), userAPI.getSystemUser(), false);
    }

    @Test(expected = NoSuchUserException.class)
    public void testFindByC_UMarkDeleted()
        throws DotSecurityException, DotDataException, SystemException, NoSuchUserException {
        String companyId;
        String id;
        String userName;
        User user;

        companyId = PublicCompanyFactory.getDefaultCompanyId();

        id = String.valueOf(new Date().getTime());
        userName = "user" + id;
        user = UserTestUtil.getUser(userName, true, true);

        UserPool.remove(userName);
        try {
            UserUtil.findByC_U(companyId, userName);
        } finally {
            userAPI.delete(user, userAPI.getDefaultUser(), userAPI.getSystemUser(), false);
        }
    }

    @Test
    public void testFindByC_P() throws DotSecurityException, DotDataException, SystemException, NoSuchUserException {
        String companyId;
        String id;
        String userName;
        User user;

        companyId = PublicCompanyFactory.getDefaultCompanyId();

        id = String.valueOf(new Date().getTime());
        userName = "user" + id;
        user = UserTestUtil.getUser(userName, false, false);
        user.setPassword(userName);
        userAPI.save(user, systemUser, false);

        List users = UserUtil.findByC_P(companyId, userName);

        assertNotNull(users);
        assertTrue(users.size() == 1);
        assertTrue(users.contains(user));

        userAPI.delete(user, userAPI.getDefaultUser(), userAPI.getSystemUser(), false);
    }

    @Test
    public void testFindByC_PMarkDeleted()
        throws DotSecurityException, DotDataException, SystemException, NoSuchUserException {
        String companyId;
        String id;
        String userName;
        User user;

        companyId = PublicCompanyFactory.getDefaultCompanyId();

        id = String.valueOf(new Date().getTime());
        userName = "user" + id;
        user = UserTestUtil.getUser(userName, true, false);
        user.setPassword(userName);
        userAPI.save(user, systemUser, false);

        List users = UserUtil.findByC_P(companyId, userName);

        assertNotNull(users);
        assertTrue(users.size() == 0);

        userAPI.delete(user, userAPI.getDefaultUser(), userAPI.getSystemUser(), false);
    }

    @Test
    public void testFindByC_PWithLimit()
        throws DotSecurityException, DotDataException, SystemException, NoSuchUserException {
        String companyId;
        String id;
        String userName;
        User user;

        id = String.valueOf(new Date().getTime());
        companyId = PublicCompanyFactory.getDefaultCompanyId();

        userName = "user" + id;
        user = UserTestUtil.getUser(userName, true, false);
        user.setPassword("1:1:EBk/HSdzfiWh52GO9xxbBJhZgsb2jd9Q:i=4e20:LnjrBImIZ2XRA6woT8lSZmGNrDP8LKgE");
        user.setCompanyId(companyId);
        userAPI.save(user, systemUser, false);

        List
            users =
            UserUtil
                .findByC_P(companyId, "1:1:EBk/HSdzfiWh52GO9xxbBJhZgsb2jd9Q:i=4e20:LnjrBImIZ2XRA6woT8lSZmGNrDP8LKgE", 0,
                    5, null);

        assertNotNull(users);
        assertTrue(users.size() > 0 && users.size() <= 5);
        assertTrue(!users.contains(user));

        userAPI.delete(user, userAPI.getDefaultUser(), userAPI.getSystemUser(), false);
    }

    @Test
    public void testFindByC_P_PrevAndNext()
        throws DotSecurityException, DotDataException, SystemException, NoSuchUserException {
        String companyId;
        String id;
        String userName;
        User user1, user2, user3, user4;

        companyId = PublicCompanyFactory.getDefaultCompanyId();

        id = String.valueOf(new Date().getTime());
        user1 = UserTestUtil.getUser("user" + id, false, false);
        user1.setPassword("password");
        userAPI.save(user1, systemUser, false);

        id = String.valueOf(new Date().getTime());
        userName = "user" + id;
        user2 = UserTestUtil.getUser(userName, false, false);
        user2.setPassword("password");
        userAPI.save(user2, systemUser, false);

        id = String.valueOf(new Date().getTime());
        user3 = UserTestUtil.getUser("user" + id, true, false);
        user3.setPassword("password");
        userAPI.save(user3, systemUser, false);

        id = String.valueOf(new Date().getTime());
        user4 = UserTestUtil.getUser("user" + id, false, false);
        user4.setPassword("password");
        userAPI.save(user4, systemUser, false);

        User[] users = UserUtil.findByC_P_PrevAndNext(userName, companyId, "password", new UserUtilComparatorTest());

        assertNotNull(users);
        assertTrue(users.length == 3);
        assertTrue(!users[0].equals(user3));
        assertTrue(!users[1].equals(user3));
        assertTrue(!users[2].equals(user3));

        userAPI.delete(user1, userAPI.getDefaultUser(), userAPI.getSystemUser(), false);
        userAPI.delete(user2, userAPI.getDefaultUser(), userAPI.getSystemUser(), false);
        userAPI.delete(user3, userAPI.getDefaultUser(), userAPI.getSystemUser(), false);
        userAPI.delete(user4, userAPI.getDefaultUser(), userAPI.getSystemUser(), false);
    }

    @Test
    public void testFindByC_EA() throws DotSecurityException, DotDataException, SystemException, NoSuchUserException {
        String companyId;
        String id;
        String userName;
        User user;

        companyId = PublicCompanyFactory.getDefaultCompanyId();

        id = String.valueOf(new Date().getTime());
        userName = "user" + id;
        user = UserTestUtil.getUser(userName, false, true);

        User result = UserUtil.findByC_EA(companyId, user.getEmailAddress());

        assertNotNull(result);
        assertTrue(result.equals(user));

        userAPI.delete(user, userAPI.getDefaultUser(), userAPI.getSystemUser(), false);
    }

    @Test(expected = NoSuchUserException.class)
    public void testFindByC_EAMarkDeleted()
        throws DotSecurityException, DotDataException, SystemException, NoSuchUserException {
        String companyId;
        String id;
        String userName;
        User user;

        companyId = PublicCompanyFactory.getDefaultCompanyId();

        id = String.valueOf(new Date().getTime());
        userName = "user" + id;
        user = UserTestUtil.getUser(userName, true, true);

        try {
            UserUtil.findByC_EA(companyId, user.getEmailAddress());
        } finally {
            userAPI.delete(user, userAPI.getDefaultUser(), userAPI.getSystemUser(), false);
        }
    }

    @Test
    public void testFindAll() throws DotSecurityException, DotDataException, SystemException {
        String id;
        String userName;
        User user;

        id = String.valueOf(new Date().getTime());
        userName = "user" + id;
        user = UserTestUtil.getUser(userName, true, true);

        List users = UserUtil.findAll();

        assertNotNull(users);
        assertTrue(users.size() > 0);
        assertTrue(!users.contains(user));

        userAPI.delete(user, userAPI.getDefaultUser(), userAPI.getSystemUser(), false);
    }

    @Test(expected = NoSuchUserException.class)
    public void testRemoveByCompanyId()
        throws DotSecurityException, DotDataException, SystemException, NoSuchUserException, NoSuchCompanyException {

        String id, userName, companyName;
        User user;

        id = String.valueOf(new Date().getTime());
        userName = "user" + id;
        companyName = "fake" + id + ".org";
        user = UserTestUtil.getUser(userName, false, false);

        Company company = PublicCompanyFactory.create(companyName);
        company.setHomeURL("localhost");
        company.setPortalURL("localhost");
        company.setMx(companyName);
        company.setName(companyName);
        company.setShortName(companyName);
        PublicCompanyFactory.update(company);
        user.setCompanyId(company.getCompanyId());

        userAPI.save(user, systemUser, false);

        roleAPI.removeRoleFromUser(roleAPI.getUserRole(user), user);

        UserUtil.removeByCompanyId(company.getCompanyId());

        PublicCompanyFactory.remove(company.getCompanyId());
        UserUtil.findByC_EA(userName, user.getEmailAddress());

    }

    @Test
    public void testRemoveByCompanyIdMarkDeleted()
        throws DotSecurityException, DotDataException, SystemException, NoSuchUserException, NoSuchCompanyException {

        String id, companyName, userName;
        User user;

        id = String.valueOf(new Date().getTime());
        userName = "user" + id;
        companyName = "fake" + id + ".org";
        user = UserTestUtil.getUser(userName, true, false);

        Company company = PublicCompanyFactory.create(companyName);
        company.setHomeURL("localhost");
        company.setPortalURL("localhost");
        company.setMx(companyName);
        company.setName(companyName);
        company.setShortName(companyName);
        PublicCompanyFactory.update(company);
        user.setCompanyId(company.getCompanyId());

        userAPI.save(user, systemUser, false);

        UserUtil.removeByCompanyId(company.getCompanyId());

        UserPool.remove(userName);

        assertNotNull(UserUtil.findByPrimaryKey(userName));

        userAPI.delete(user, userAPI.getDefaultUser(), userAPI.getSystemUser(), false);
        PublicCompanyFactory.remove(company.getCompanyId());

    }

    @Test(expected = NoSuchUserException.class)
    public void testRemoveByC_U() throws DotSecurityException, DotDataException, SystemException, NoSuchUserException {
        String companyId;
        String id;
        String userName;
        User user;

        companyId = PublicCompanyFactory.getDefaultCompanyId();
        id = String.valueOf(new Date().getTime());
        userName = "user" + id;
        user = UserTestUtil.getUser(userName, false, true);

        roleAPI.removeRoleFromUser(roleAPI.getUserRole(user), user);

        UserUtil.removeByC_U(companyId, userName);

        UserUtil.findByC_EA(userName, user.getEmailAddress());

    }

    @Test
    public void testRemoveByC_UMarkDeleted()
        throws DotSecurityException, DotDataException, SystemException, NoSuchUserException {
        String companyId;
        String id;
        String userName;
        User user;

        companyId = PublicCompanyFactory.getDefaultCompanyId();
        id = String.valueOf(new Date().getTime());
        userName = "user" + id;
        user = UserTestUtil.getUser(userName, true, true);

        UserUtil.removeByC_U(companyId, userName);

        UserPool.remove(userName);

        assertNotNull(UserUtil.findByPrimaryKey(userName));

        userAPI.delete(user, userAPI.getDefaultUser(), userAPI.getSystemUser(), false);
    }

    @Test(expected = NoSuchUserException.class)
    public void testRemoveByC_P() throws DotSecurityException, DotDataException, SystemException, NoSuchUserException {
        String companyId;
        String id;
        String userName;
        User user;

        id = String.valueOf(new Date().getTime());
        companyId = PublicCompanyFactory.getDefaultCompanyId();
        userName = "user" + id;
        user = UserTestUtil.getUser(userName, false, false);
        user.setPassword(userName);
        userAPI.save(user, systemUser, false);

        roleAPI.removeRoleFromUser(roleAPI.getUserRole(user), user);

        UserUtil.removeByC_P(companyId, userName);

        UserUtil.findByC_EA(userName, user.getEmailAddress());

    }

    @Test
    public void testRemoveByC_PMarkDeleted()
        throws DotSecurityException, DotDataException, SystemException, NoSuchUserException {
        String companyId;
        String id;
        String userName;
        User user;

        id = String.valueOf(new Date().getTime());
        companyId = PublicCompanyFactory.getDefaultCompanyId();
        userName = "user" + id;
        user = UserTestUtil.getUser(userName, true, false);
        user.setPassword(userName);
        userAPI.save(user, systemUser, false);

        UserUtil.removeByC_P(companyId, userName);

        UserPool.remove(userName);

        assertNotNull(UserUtil.findByPrimaryKey(userName));

        userAPI.delete(user, userAPI.getDefaultUser(), userAPI.getSystemUser(), false);

    }

    @Test(expected = NoSuchUserException.class)
    public void testRemoveByC_EA() throws DotSecurityException, DotDataException, SystemException, NoSuchUserException {
        String companyId;
        String id;
        String userName;
        User user;

        id = String.valueOf(new Date().getTime());
        companyId = PublicCompanyFactory.getDefaultCompanyId();
        userName = "user" + id;
        user = UserTestUtil.getUser(userName, false, true);

        roleAPI.removeRoleFromUser(roleAPI.getUserRole(user), user);

        UserUtil.removeByC_EA(companyId, user.getEmailAddress());

        UserUtil.findByC_EA(userName, user.getEmailAddress());

    }

    @Test
    public void testRemoveByC_EAMarkDeleted()
        throws DotSecurityException, DotDataException, SystemException, NoSuchUserException {
        String companyId;
        String id;
        String userName;
        User user;

        id = String.valueOf(new Date().getTime());
        companyId = PublicCompanyFactory.getDefaultCompanyId();
        userName = "user" + id;
        user = UserTestUtil.getUser(userName, true, true);

        UserUtil.removeByC_EA(companyId, user.getEmailAddress());

        UserPool.remove(userName);

        assertNotNull(UserUtil.findByPrimaryKey(userName));

        userAPI.delete(user, userAPI.getDefaultUser(), userAPI.getSystemUser(), false);
    }

    @Test
    public void testCountByCompanyId() throws DotSecurityException, DotDataException, SystemException {
        String companyId;

        companyId = PublicCompanyFactory.getDefaultCompanyId();

        int total = UserUtil.countByCompanyId(companyId);

        assertTrue(total > 0);
    }

    @Test
    public void testCountByC_U() throws DotSecurityException, DotDataException, SystemException, NoSuchUserException {
        String companyId;
        String id;
        String userName;
        User user;

        companyId = PublicCompanyFactory.getDefaultCompanyId();

        id = String.valueOf(new Date().getTime());
        userName = "user" + id;
        user = UserTestUtil.getUser(userName, false, true);

        int total = UserUtil.countByC_U(companyId, userName);

        assertTrue(total == 1);

        userAPI.delete(user, userAPI.getDefaultUser(), userAPI.getSystemUser(), false);
    }

    @Test
    public void testCountByC_UMarkDeleted()
        throws DotSecurityException, DotDataException, SystemException, NoSuchUserException {
        String companyId;
        String id;
        String userName;
        User user;

        companyId = PublicCompanyFactory.getDefaultCompanyId();

        id = String.valueOf(new Date().getTime());
        userName = "user" + id;
        user = UserTestUtil.getUser(userName, true, true);

        int total = UserUtil.countByC_U(companyId, userName);

        assertTrue(total == 0);

        userAPI.delete(user, userAPI.getDefaultUser(), userAPI.getSystemUser(), false);
    }

    @Test
    public void testCountByC_P() throws DotSecurityException, DotDataException, SystemException, NoSuchUserException {
        String companyId;
        String id;
        String userName;
        User user;

        companyId = PublicCompanyFactory.getDefaultCompanyId();

        id = String.valueOf(new Date().getTime());
        userName = "user" + id;
        user = UserTestUtil.getUser(userName, false, false);
        user.setPassword(userName);
        userAPI.save(user, systemUser, false);

        int total = UserUtil.countByC_P(companyId, userName);

        assertTrue(total == 1);

        userAPI.delete(user, userAPI.getDefaultUser(), userAPI.getSystemUser(), false);

    }

    @Test
    public void testCountByC_PMarkDeleted()
        throws DotSecurityException, DotDataException, SystemException, NoSuchUserException {
        String companyId;
        String id;
        String userName;
        User user;

        companyId = PublicCompanyFactory.getDefaultCompanyId();

        id = String.valueOf(new Date().getTime());
        userName = "user" + id;
        user = UserTestUtil.getUser(userName, true, false);
        user.setPassword(userName);
        userAPI.save(user, systemUser, false);

        int total = UserUtil.countByC_P(companyId, userName);

        assertTrue(total == 0);

        userAPI.delete(user, userAPI.getDefaultUser(), userAPI.getSystemUser(), false);
    }

    @Test
    public void testCountByC_EA() throws DotSecurityException, DotDataException, SystemException, NoSuchUserException {
        String companyId;
        String id;
        String userName;
        User user;

        companyId = PublicCompanyFactory.getDefaultCompanyId();

        id = String.valueOf(new Date().getTime());
        userName = "user" + id;
        user = UserTestUtil.getUser(userName, false, true);

        int total = UserUtil.countByC_EA(companyId, user.getEmailAddress());

        assertTrue(total == 1);

        userAPI.delete(user, userAPI.getDefaultUser(), userAPI.getSystemUser(), false);
    }

    @Test
    public void testCountByC_EAMarkDeleted()
        throws DotSecurityException, DotDataException, SystemException, NoSuchUserException {
        String companyId;
        String id;
        String userName;
        User user;

        companyId = PublicCompanyFactory.getDefaultCompanyId();

        id = String.valueOf(new Date().getTime());
        userName = "user" + id;
        user = UserTestUtil.getUser(userName, true, true);

        int total = UserUtil.countByC_EA(companyId, user.getEmailAddress());

        assertTrue(total == 0);

        userAPI.delete(user, userAPI.getDefaultUser(), userAPI.getSystemUser(), false);
    }

    private class UserUtilComparatorTest extends OrderByComparator {

        public String getOrderBy() {

            return "firstName ASC";
        }

        @Override
        public int compare(Object obj1, Object obj2) {
            User user1 = (User) obj1;
            User user2 = (User) obj2;

            return ((User) obj1).compareTo(obj2);
        }
    }
}
