package com.dotcms.auth;

import com.dotmarketing.business.UserAPI;
import com.dotmarketing.util.Config;
import com.liferay.portal.model.User;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

/**
 * Unit tests for the rollback switch of {@link InteractiveUserPolicy}: key
 * {@code USER_RESOLUTION_EXCLUDE_SYSTEM_USER}, on by default, read on every call.
 */
public class InteractiveUserPolicyKillSwitchTest {

    private static final String KEY = "USER_RESOLUTION_EXCLUDE_SYSTEM_USER";

    @BeforeClass
    public static void prepare() {
        Config.initializeConfig();
    }

    @Before
    @After
    public void reset() {
        Config.setProperty(KEY, null);
        InteractiveUserPolicy.resetDisabledWarningForTests();
    }

    private static User system() {
        final User user = new User();
        user.setUserId(UserAPI.SYSTEM_USER_ID);
        return user;
    }

    /**
     * On by default: the built-in account is refused with no configuration at all.
     */
    @Test
    public void test_guard_is_on_by_default() {
        assertNull(InteractiveUserPolicy.refuseIfNotEligible(system(), null, null));
    }

    /**
     * Turning the switch off takes effect without a restart, and turning it on again restores the guard.
     */
    @Test
    public void test_switch_off_accepts_every_user_and_is_read_per_call() {
        final User system = system();
        Config.setProperty(KEY, false);
        assertSame(system, InteractiveUserPolicy.refuseIfNotEligible(system, null, null));
        Config.setProperty(KEY, true);
        assertNull(InteractiveUserPolicy.refuseIfNotEligible(system, null, null));
    }

    /**
     * Finding the switch off logs one warning per JVM, not one per call.
     */
    @Test
    public void test_disabled_warning_is_emitted_once() {
        Config.setProperty(KEY, false);
        assertEquals(0, InteractiveUserPolicy.disabledWarningCount());
        InteractiveUserPolicy.refuseIfNotEligible(system(), null, null);
        InteractiveUserPolicy.refuseIfNotEligible(system(), null, null);
        InteractiveUserPolicy.refuseIfNotEligible(system(), null, null);
        assertEquals(1, InteractiveUserPolicy.disabledWarningCount());
    }

    /**
     * A regular user passes through unchanged with the switch on.
     */
    @Test
    public void test_regular_user_is_returned_unchanged() {
        final User regular = new User();
        regular.setUserId("dotcms.org.1");
        assertNotNull(InteractiveUserPolicy.refuseIfNotEligible(regular, null, null));
    }
}
