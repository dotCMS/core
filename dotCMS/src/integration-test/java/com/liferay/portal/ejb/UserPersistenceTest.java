package com.liferay.portal.ejb;

import com.dotcms.util.IntegrationTestInitService;
import com.liferay.portal.model.User;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Unit test for the {@link UserPersistence}
 */
public class UserPersistenceTest {

    @BeforeClass
    public static void prepare() throws Exception{
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    @Test
    public void test_diff_same_user_nodiff()  {

        final UserPersistence userPersistence = new UserPersistence();
        final UserHBM userHBM = new UserHBM();
        final User    user    = new User();

        userHBM.setPassword("xxx");
        user.setPassword("xxx");

        userHBM.setCompanyId("yyy");
        user.setCompanyId("yyy");

        userHBM.setEmailAddress("eee");
        user.setEmailAddress("eee");

        userHBM.setLayoutIds("iii");
        user.setLayoutIds("iii");

        userHBM.setUserId("111");
        user.setUserId("111");

        Assert.assertFalse(userPersistence.diff(userHBM, user));
    }

    @Test
    public void test_diff_diff_pass_diff()  {

        final UserPersistence userPersistence = new UserPersistence();
        final UserHBM userHBM = new UserHBM();
        final User    user    = new User();

        userHBM.setPassword("xxx");
        user.setPassword("ppp");

        userHBM.setCompanyId("yyy");
        user.setCompanyId("yyy");

        userHBM.setEmailAddress("eee");
        user.setEmailAddress("eee");

        userHBM.setLayoutIds("iii");
        user.setLayoutIds("iii");

        userHBM.setUserId("111");
        user.setUserId("111");

        Assert.assertTrue(userPersistence.diff(userHBM, user));
    }

    @Test
    public void test_diff_diff_company_diff()  {

        final UserPersistence userPersistence = new UserPersistence();
        final UserHBM userHBM = new UserHBM();
        final User    user    = new User();

        userHBM.setPassword("xxx");
        user.setPassword("xxx");

        userHBM.setCompanyId("yyy");
        user.setCompanyId("mmm");

        userHBM.setEmailAddress("eee");
        user.setEmailAddress("eee");

        userHBM.setLayoutIds("iii");
        user.setLayoutIds("iii");

        userHBM.setUserId("111");
        user.setUserId("111");

        Assert.assertTrue(userPersistence.diff(userHBM, user));
    }

    @Test
    public void test_diff_diff_email_diff()  {

        final UserPersistence userPersistence = new UserPersistence();
        final UserHBM userHBM = new UserHBM();
        final User    user    = new User();

        userHBM.setPassword("xxx");
        user.setPassword("xxx");

        userHBM.setCompanyId("yyy");
        user.setCompanyId("yyy");

        userHBM.setEmailAddress("eee");
        user.setEmailAddress("eeeeeee");

        userHBM.setLayoutIds("iii");
        user.setLayoutIds("iii");

        userHBM.setUserId("111");
        user.setUserId("111");

        Assert.assertTrue(userPersistence.diff(userHBM, user));
    }

    @Test
    public void test_diff_diff_layout_diff()  {

        final UserPersistence userPersistence = new UserPersistence();
        final UserHBM userHBM = new UserHBM();
        final User    user    = new User();

        userHBM.setPassword("xxx");
        user.setPassword("xxx");

        userHBM.setCompanyId("yyy");
        user.setCompanyId("yyy");

        userHBM.setEmailAddress("eee");
        user.setEmailAddress("eee");

        userHBM.setLayoutIds("iii");
        user.setLayoutIds("iiixyz");

        userHBM.setUserId("111");
        user.setUserId("111");

        Assert.assertTrue(userPersistence.diff(userHBM, user));
    }

    @Test
    public void test_diff_diff_id_diff()  {

        final UserPersistence userPersistence = new UserPersistence();
        final UserHBM userHBM = new UserHBM();
        final User    user    = new User();

        userHBM.setPassword("xxx");
        user.setPassword("xxx");

        userHBM.setCompanyId("yyy");
        user.setCompanyId("yyy");

        userHBM.setEmailAddress("eee");
        user.setEmailAddress("eee");

        userHBM.setLayoutIds("iii");
        user.setLayoutIds("iii");

        userHBM.setUserId("111222");
        user.setUserId("111");

        Assert.assertTrue(userPersistence.diff(userHBM, user));
    }

    @Test
    public void test_diff_diff_id_null_diff()  {

        final UserPersistence userPersistence = new UserPersistence();
        final UserHBM userHBM = new UserHBM();
        final User    user    = new User();

        userHBM.setPassword("xxx");
        user.setPassword("xxx");

        userHBM.setCompanyId("yyy");
        user.setCompanyId("yyy");

        userHBM.setEmailAddress("eee");
        user.setEmailAddress("eee");

        userHBM.setLayoutIds("iii");
        user.setLayoutIds("iii");

        userHBM.setUserId(null);
        user.setUserId("111");

        Assert.assertTrue(userPersistence.diff(userHBM, user));
    }

    @Test
    public void test_diff_diff_id2_null_diff()  {

        final UserPersistence userPersistence = new UserPersistence();
        final UserHBM userHBM = new UserHBM();
        final User    user    = new User();

        userHBM.setPassword("xxx");
        user.setPassword("xxx");

        userHBM.setCompanyId("yyy");
        user.setCompanyId("yyy");

        userHBM.setEmailAddress("eee");
        user.setEmailAddress("eee");

        userHBM.setLayoutIds("iii");
        user.setLayoutIds("iii");

        userHBM.setUserId("111");
        user.setUserId(null);

        Assert.assertTrue(userPersistence.diff(userHBM, user));
    }
}
