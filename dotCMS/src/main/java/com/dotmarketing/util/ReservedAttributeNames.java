package com.dotmarketing.util;

import com.dotcms.auth.providers.oauth.OAuthConstants;
import com.dotcms.filters.interceptor.saml.SamlWebInterceptor;
import com.dotcms.saml.DotSamlConstants;
import com.dotmarketing.filters.TimeMachineFilter;
import com.liferay.portal.util.WebKeys;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Session and request attribute names that only dotCMS itself sets: the signed-in user, the
 * Login As principal, the edit-mode redirect after login, the Time Machine preview date, language
 * and site, and the keys of the OAuth and SAML login flows. Code that lets content authors choose
 * an attribute name, such as the session and request attribute rule actions, uses this class to
 * leave those attributes alone.
 */
public final class ReservedAttributeNames {

    /**
     * The reserved names. Names are compared exactly, as servlet attribute names are.
     */
    public static final Set<String> NAMES = Set.of(
            WebKeys.USER_ID,
            WebKeys.USER,
            WebKeys.PRINCIPAL_USER_ID,
            WebKeys.LOGIN_TO_EDIT_MODE,
            TimeMachineFilter.TM_DATE_VAR,
            TimeMachineFilter.TM_LANG_VAR,
            TimeMachineFilter.TM_HOST_VAR,
            OAuthConstants.SESSION_NATIVE_LOGIN,
            OAuthConstants.SESSION_REDIRECT_URI,
            OAuthConstants.SESSION_STATE,
            OAuthConstants.SESSION_NONCE,
            OAuthConstants.SESSION_CODE_VERIFIER,
            OAuthConstants.SESSION_ACCESS_TOKEN,
            OAuthConstants.SESSION_ID_TOKEN,
            OAuthConstants.SESSION_PROVIDER_TYPE,
            OAuthConstants.SESSION_FRONT_END_LOGIN,
            OAuthConstants.SESSION_ORIGINAL_REQUEST,
            SamlWebInterceptor.ORIGINAL_REQUEST);

    /**
     * SAML session keys that dotCMS stores with the site id in front of them. Any name that ends
     * with one of these keys is reserved, whatever precedes it. Compared case-sensitively.
     */
    public static final List<String> SITE_KEY_SUFFIXES = List.of(
            DotSamlConstants.SAML_USER_ID,
            DotSamlConstants.SAML_SESSION_INDEX,
            DotSamlConstants.SAML_NAME_ID);

    private ReservedAttributeNames() {
    }

    /**
     * Tells whether an attribute name is one that only dotCMS sets.
     *
     * @param name The attribute name, may be {@code null}.
     * @return {@code true} when the name is exactly one of {@link #NAMES} or ends with one of
     * {@link #SITE_KEY_SUFFIXES}.
     */
    public static boolean isReserved(final String name) {
        return reservedName(name).isPresent();
    }

    /**
     * Returns the reserved name an attribute name matches, so callers can report it without
     * repeating what precedes a site key.
     *
     * @param name The attribute name, may be {@code null}.
     * @return The name itself when it is one of {@link #NAMES}; the key it ends with when it ends
     * with one of {@link #SITE_KEY_SUFFIXES}; empty when the name is not reserved.
     */
    public static Optional<String> reservedName(final String name) {
        if (name == null) {
            return Optional.empty();
        }
        if (NAMES.contains(name)) {
            return Optional.of(name);
        }
        return SITE_KEY_SUFFIXES.stream().filter(name::endsWith).findFirst();
    }
}
