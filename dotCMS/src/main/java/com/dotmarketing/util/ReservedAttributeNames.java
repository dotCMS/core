package com.dotmarketing.util;

import com.dotcms.auth.providers.oauth.OAuthConstants;
import com.dotmarketing.filters.TimeMachineFilter;
import com.liferay.portal.util.WebKeys;

import java.util.Set;

/**
 * Session and request attribute names that only dotCMS itself sets: the signed-in user, the
 * Login As principal, the edit-mode redirect after login, the native-login flag, and the Time
 * Machine preview date, language and site. Code that lets content authors choose an attribute
 * name, such as the session and request attribute rule actions, uses this set to leave those
 * attributes alone.
 */
public final class ReservedAttributeNames {

    /**
     * The reserved attribute names. Names are compared exactly, as servlet attribute names are.
     */
    public static final Set<String> NAMES = Set.of(
            WebKeys.USER_ID,
            WebKeys.USER,
            WebKeys.PRINCIPAL_USER_ID,
            WebKeys.LOGIN_TO_EDIT_MODE,
            OAuthConstants.SESSION_NATIVE_LOGIN,
            TimeMachineFilter.TM_DATE_VAR,
            TimeMachineFilter.TM_LANG_VAR,
            TimeMachineFilter.TM_HOST_VAR);

    private ReservedAttributeNames() {
    }

    /**
     * Tells whether an attribute name is one that only dotCMS sets.
     *
     * @param name The attribute name, may be {@code null}.
     * @return {@code true} when the name is exactly one of {@link #NAMES}.
     */
    public static boolean isReserved(final String name) {
        return name != null && NAMES.contains(name);
    }
}
