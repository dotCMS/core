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
        final com.dotcms.cms.login.SessionUserResolver sessionResolver =
                org.mockito.Mockito.mock(com.dotcms.cms.login.SessionUserResolver.class);
        org.mockito.Mockito.when(sessionResolver.resolve(org.mockito.ArgumentMatchers.any()))
                .thenReturn(java.util.Optional.of(account));
        try (org.mockito.MockedStatic<com.dotcms.cms.login.SessionUserResolver> resolver =
                     org.mockito.Mockito.mockStatic(com.dotcms.cms.login.SessionUserResolver.class)) {
            resolver.when(com.dotcms.cms.login.SessionUserResolver::getInstance).thenReturn(sessionResolver);
            assertNotNull(RequestUserResolver.resolveRequestUser(request));
        }
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

    /** Off: the filter hands the request down the chain unwrapped. */
    @Test
    public void test_switch_off_filter_passes_the_request_through_unwrapped() throws Exception {
        Config.setProperty(KEY, false);
        final HttpServletRequest container = IdentityMocks.request(new HashMap<>(), IdentityMocks.session(new HashMap<>()));
        final java.util.concurrent.atomic.AtomicReference<Object> seen = new java.util.concurrent.atomic.AtomicReference<>();
        final javax.servlet.FilterChain chain = org.mockito.Mockito.mock(javax.servlet.FilterChain.class);
        org.mockito.Mockito.doAnswer(i -> {
            seen.set(i.getArgument(0));
            return null;
        }).when(chain).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());

        new InteractiveIdentityFilter().doFilter(container,
                org.mockito.Mockito.mock(javax.servlet.http.HttpServletResponse.class), chain);

        org.junit.Assert.assertSame(container, seen.get());
    }

    /** Off: an open scope is inert, so the resolver reads the request as before. */
    @Test
    public void test_switch_off_scope_is_inert() {
        Config.setProperty(KEY, false);
        final HttpServletRequest request = IdentityMocks.request(new HashMap<>(), null);
        try (SystemUserScope ignored = SystemUserScope.open()) {
            assertNull(RequestUserResolver.resolveRequestUser(request));
        }
    }
}
