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

    private static final String TOKEN_XXX = "xxx";
    private static final String TOKEN_YYY = "yyy";
    private static final String TOKEN_EEE = "eee";
    private static final String TOKEN_III = "iii";
    private static final String TOKEN_USER_ID = "111";

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

        userHBM.setPassword(TOKEN_XXX);
        user.setPassword(TOKEN_XXX);

        userHBM.setCompanyId(TOKEN_YYY);
        user.setCompanyId(TOKEN_YYY);

        userHBM.setEmailAddress(TOKEN_EEE);
        user.setEmailAddress(TOKEN_EEE);

        userHBM.setLayoutIds(TOKEN_III);
        user.setLayoutIds(TOKEN_III);

        userHBM.setUserId(TOKEN_USER_ID);
        user.setUserId(TOKEN_USER_ID);

        Assert.assertFalse(userPersistence.diff(userHBM, user));
    }

    @Test
    public void test_diff_diff_pass_diff()  {

        final UserPersistence userPersistence = new UserPersistence();
        final UserHBM userHBM = new UserHBM();
        final User    user    = new User();

        userHBM.setPassword(TOKEN_XXX);
        user.setPassword("ppp");

        userHBM.setCompanyId(TOKEN_YYY);
        user.setCompanyId(TOKEN_YYY);

        userHBM.setEmailAddress(TOKEN_EEE);
        user.setEmailAddress(TOKEN_EEE);

        userHBM.setLayoutIds(TOKEN_III);
        user.setLayoutIds(TOKEN_III);

        userHBM.setUserId(TOKEN_USER_ID);
        user.setUserId(TOKEN_USER_ID);

        Assert.assertTrue(userPersistence.diff(userHBM, user));
    }

    @Test
    public void test_diff_diff_company_diff()  {

        final UserPersistence userPersistence = new UserPersistence();
        final UserHBM userHBM = new UserHBM();
        final User    user    = new User();

        userHBM.setPassword(TOKEN_XXX);
        user.setPassword(TOKEN_XXX);

        userHBM.setCompanyId(TOKEN_YYY);
        user.setCompanyId("mmm");

        userHBM.setEmailAddress(TOKEN_EEE);
        user.setEmailAddress(TOKEN_EEE);

        userHBM.setLayoutIds(TOKEN_III);
        user.setLayoutIds(TOKEN_III);

        userHBM.setUserId(TOKEN_USER_ID);
        user.setUserId(TOKEN_USER_ID);

        Assert.assertTrue(userPersistence.diff(userHBM, user));
    }

    @Test
    public void test_diff_diff_email_diff()  {

        final UserPersistence userPersistence = new UserPersistence();
        final UserHBM userHBM = new UserHBM();
        final User    user    = new User();

        userHBM.setPassword(TOKEN_XXX);
        user.setPassword(TOKEN_XXX);

        userHBM.setCompanyId(TOKEN_YYY);
        user.setCompanyId(TOKEN_YYY);

        userHBM.setEmailAddress(TOKEN_EEE);
        user.setEmailAddress("eeeeeee");

        userHBM.setLayoutIds(TOKEN_III);
        user.setLayoutIds(TOKEN_III);

        userHBM.setUserId(TOKEN_USER_ID);
        user.setUserId(TOKEN_USER_ID);

        Assert.assertTrue(userPersistence.diff(userHBM, user));
    }

    @Test
    public void test_diff_diff_layout_diff()  {

        final UserPersistence userPersistence = new UserPersistence();
        final UserHBM userHBM = new UserHBM();
        final User    user    = new User();

        userHBM.setPassword(TOKEN_XXX);
        user.setPassword(TOKEN_XXX);

        userHBM.setCompanyId(TOKEN_YYY);
        user.setCompanyId(TOKEN_YYY);

        userHBM.setEmailAddress(TOKEN_EEE);
        user.setEmailAddress(TOKEN_EEE);

        userHBM.setLayoutIds(TOKEN_III);
        user.setLayoutIds("iiixyz");

        userHBM.setUserId(TOKEN_USER_ID);
        user.setUserId(TOKEN_USER_ID);

        Assert.assertTrue(userPersistence.diff(userHBM, user));
    }

    @Test
    public void test_diff_diff_id_diff()  {

        final UserPersistence userPersistence = new UserPersistence();
        final UserHBM userHBM = new UserHBM();
        final User    user    = new User();

        userHBM.setPassword(TOKEN_XXX);
        user.setPassword(TOKEN_XXX);

        userHBM.setCompanyId(TOKEN_YYY);
        user.setCompanyId(TOKEN_YYY);

        userHBM.setEmailAddress(TOKEN_EEE);
        user.setEmailAddress(TOKEN_EEE);

        userHBM.setLayoutIds(TOKEN_III);
        user.setLayoutIds(TOKEN_III);

        userHBM.setUserId("111222");
        user.setUserId(TOKEN_USER_ID);

        Assert.assertTrue(userPersistence.diff(userHBM, user));
    }

    @Test
    public void test_diff_diff_id_null_diff()  {

        final UserPersistence userPersistence = new UserPersistence();
        final UserHBM userHBM = new UserHBM();
        final User    user    = new User();

        userHBM.setPassword(TOKEN_XXX);
        user.setPassword(TOKEN_XXX);

        userHBM.setCompanyId(TOKEN_YYY);
        user.setCompanyId(TOKEN_YYY);

        userHBM.setEmailAddress(TOKEN_EEE);
        user.setEmailAddress(TOKEN_EEE);

        userHBM.setLayoutIds(TOKEN_III);
        user.setLayoutIds(TOKEN_III);

        userHBM.setUserId(null);
        user.setUserId(TOKEN_USER_ID);

        Assert.assertTrue(userPersistence.diff(userHBM, user));
    }

    @Test
    public void test_diff_diff_id2_null_diff()  {

        final UserPersistence userPersistence = new UserPersistence();
        final UserHBM userHBM = new UserHBM();
        final User    user    = new User();

        userHBM.setPassword(TOKEN_XXX);
        user.setPassword(TOKEN_XXX);

        userHBM.setCompanyId(TOKEN_YYY);
        user.setCompanyId(TOKEN_YYY);

        userHBM.setEmailAddress(TOKEN_EEE);
        user.setEmailAddress(TOKEN_EEE);

        userHBM.setLayoutIds(TOKEN_III);
        user.setLayoutIds(TOKEN_III);

        userHBM.setUserId(TOKEN_USER_ID);
        user.setUserId(null);

        Assert.assertTrue(userPersistence.diff(userHBM, user));
    }
}
