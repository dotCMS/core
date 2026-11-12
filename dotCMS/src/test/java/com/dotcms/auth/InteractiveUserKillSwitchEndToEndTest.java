package com.dotcms.auth;

import com.dotmarketing.business.UserAPI;
import com.dotmarketing.util.Config;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Rollback switch end to end: with {@code USER_RESOLUTION_EXCLUDE_SYSTEM_USER=false} every piece (wrapper
 * write and read, resolver, token processors) accepts the built-in account as before and one warning is
 * logged; switching it back on restores the refusal without a restart.
 */
public class InteractiveUserKillSwitchEndToEndTest {

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

    /** Off: the wrapper stores and returns the account, the resolver answers with it, one warning in all. */
    @Test
    public void test_switch_off_accepts_the_account_in_wrapper_and_resolver_with_one_warning() {
        Config.setProperty(KEY, false);
        final Map<String, Object> store = new HashMap<>();
        final HttpSession session = InteractiveIdentityWrapper.wrap(IdentityMocks.session(store));
        final HttpServletRequest request = IdentityMocks.request(new HashMap<>(), session);
        final User account = system();

        session.setAttribute(WebKeys.USER, account);
        assertEquals(account, store.get(WebKeys.USER));
        assertNotNull(session.getAttribute(WebKeys.USER));
        assertNotNull(RequestUserResolver.resolveRequestUser(request));
        assertEquals(1, InteractiveUserPolicy.disabledWarningCount());
    }

    /** Turning it back on restores the refusal in the same JVM. */
    @Test
    public void test_switch_back_on_restores_the_refusal() {
        Config.setProperty(KEY, false);
        final Map<String, Object> store = new HashMap<>();
        final HttpSession session = InteractiveIdentityWrapper.wrap(IdentityMocks.session(store));
        session.setAttribute(WebKeys.USER, system());
        Config.setProperty(KEY, true);

        assertNull(session.getAttribute(WebKeys.USER));
        assertNull(RequestUserResolver.resolveRequestUser(IdentityMocks.request(new HashMap<>(), session)));
        session.setAttribute(WebKeys.USER_ID, UserAPI.SYSTEM_USER_ID);
        assertNull(session.getAttribute(WebKeys.USER_ID));
    }
}
