package com.dotcms.auth;

import com.dotmarketing.business.UserAPI;
import com.liferay.portal.model.User;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link InteractiveUserPolicy#isEligible(User)}, which decides whether an account may be
 * used as the identity of a client request or session. No database is involved.
 */
public class InteractiveUserPolicyTest {

    private static User userWithId(final String id, final String email) {
        final User user = new User();
        user.setUserId(id);
        user.setEmailAddress(email);
        return user;
    }

    /**
     * The built-in account is not eligible, whatever the case of its id.
     */
    @Test
    public void test_built_in_account_is_not_eligible_by_id() {
        assertFalse(InteractiveUserPolicy.isEligible(userWithId(UserAPI.SYSTEM_USER_ID, null)));
        assertFalse(InteractiveUserPolicy.isEligible(userWithId("SYSTEM", null)));
        assertFalse(InteractiveUserPolicy.isEligible(userWithId("SyStEm", null)));
    }

    /**
     * Where only the email identifies the account, the built-in email is not eligible either.
     */
    @Test
    public void test_built_in_account_is_not_eligible_by_email() {
        assertFalse(InteractiveUserPolicy.isEligible(userWithId("other-id", UserAPI.SYSTEM_USER_EMAIL)));
        assertFalse(InteractiveUserPolicy.isEligible(userWithId("other-id", UserAPI.SYSTEM_USER_EMAIL.toUpperCase())));
    }

    /**
     * Regular accounts and the anonymous account stay eligible.
     */
    @Test
    public void test_regular_and_anonymous_accounts_are_eligible() {
        assertTrue(InteractiveUserPolicy.isEligible(userWithId("dotcms.org.1", "admin@dotcms.com")));
        assertTrue(InteractiveUserPolicy.isEligible(userWithId(UserAPI.CMS_ANON_USER_ID, null)));
        assertTrue(InteractiveUserPolicy.isEligible(userWithId("systemic", "systemic@example.com")));
    }

    /**
     * No user is not an error: it is "no user".
     */
    @Test
    public void test_null_user_is_not_eligible_and_does_not_throw() {
        assertFalse(InteractiveUserPolicy.isEligible(null));
    }
}
