package com.dotcms.auth;

import com.dotcms.cms.login.PreventSessionFixationUtil;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.util.Config;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Session-fixation protection copies the attributes of the old session into a new one. With a wrapped request
 * it must neither fail on a hidden user key nor carry the built-in account over.
 */
public class SessionFixationWithWrapperTest {

    @BeforeClass
    public static void prepare() {
        Config.initializeConfig();
    }

    private static HttpServletRequest requestWith(final Map<String, Object> oldStore,
            final Map<String, Object> newStore) {
        final HttpServletRequest container = mock(HttpServletRequest.class);
        final HttpSession oldSession = IdentityMocks.session(oldStore);
        final HttpSession newSession = IdentityMocks.session(newStore);
        when(container.getSession(false)).thenReturn(oldSession);
        when(container.getSession()).thenReturn(newSession);
        return InteractiveIdentityWrapper.wrap(container);
    }

    /**
     * The old session holds the built-in account; the new one does not receive it, and other attributes are
     * copied as before.
     */
    @Test
    public void test_built_in_account_is_not_copied_and_nothing_throws() {
        final Map<String, Object> oldStore = new HashMap<>();
        oldStore.put(WebKeys.USER, userWithId(UserAPI.SYSTEM_USER_ID));
        oldStore.put(WebKeys.USER_ID, UserAPI.SYSTEM_USER_ID);
        oldStore.put("other", "value");
        final Map<String, Object> newStore = new HashMap<>();

        PreventSessionFixationUtil.getInstance().preventSessionFixation(requestWith(oldStore, newStore), true);

        assertFalse(newStore.containsKey(WebKeys.USER));
        assertFalse(newStore.containsKey(WebKeys.USER_ID));
        assertEquals("value", newStore.get("other"));
    }

    /**
     * A real user's attributes are copied unchanged.
     */
    @Test
    public void test_real_user_is_copied() {
        final User real = userWithId("dotcms.org.1");
        final Map<String, Object> oldStore = new HashMap<>();
        oldStore.put(WebKeys.USER, real);
        oldStore.put(WebKeys.USER_ID, "dotcms.org.1");
        final Map<String, Object> newStore = new HashMap<>();

        PreventSessionFixationUtil.getInstance().preventSessionFixation(requestWith(oldStore, newStore), true);

        assertSame(real, newStore.get(WebKeys.USER));
        assertEquals("dotcms.org.1", newStore.get(WebKeys.USER_ID));
    }

    private static User userWithId(final String id) {
        final User user = new User();
        user.setUserId(id);
        return user;
    }
}
