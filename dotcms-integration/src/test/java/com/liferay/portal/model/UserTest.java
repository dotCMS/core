package com.liferay.portal.model;

import org.junit.Assert;
import org.junit.Test;

public class UserTest {

    /**
     * Method to test: {@link User#setUserId(String)}
     * Given Scenario: The user id can not be override
     * ExpectedResult: Try to set the user id twice, the second time won't work
     *
     */
    @Test()
    public void test_immutable_user_id() {

        final User user = new User();
        user.setUserId("dotcms.1");
        user.setUserId("dotcms.2");

        Assert.assertTrue("dotcms.1".equals(user.getUserId()));
        Assert.assertFalse("dotcms.2".equals(user.getUserId()));
    }
}
