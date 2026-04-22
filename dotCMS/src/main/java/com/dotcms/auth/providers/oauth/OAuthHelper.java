package com.dotcms.auth.providers.oauth;

import com.dotcms.auth.providers.oauth.provider.OAuthProvider;
import com.dotcms.enterprise.PasswordFactoryProxy;
import com.dotcms.enterprise.de.qaware.heimdall.PasswordException;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.cms.factories.PublicEncryptionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.SecurityLogger;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import io.vavr.control.Try;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Resolves and authenticates a dotCMS {@link User} from an OAuth/OIDC userinfo payload.
 * Follows the same contract as {@code SAMLHelper}: lookup → create if missing → role mapping →
 * {@code LoginServiceAPI.doCookieLogin()}.
 */
public class OAuthHelper {

    private static final String[] EMAIL_CLAIMS      = {"email", "email_address", "emailaddress", "userPrincipalName"};
    private static final String[] FIRST_NAME_CLAIMS = {"first_name", "firstname", "given_name", "givenname"};
    private static final String[] LAST_NAME_CLAIMS  = {"last_name", "lastname", "family_name", "familyname", "surname"};

    /**
     * Resolve (or create) a dotCMS user from the userinfo payload and log them in via cookie.
     * Returns the authenticated user.
     */
    public User authenticate(final HttpServletRequest request,
                             final HttpServletResponse response,
                             final OAuthProvider provider,
                             final String accessToken,
                             final Map<String, Object> userInfo,
                             final OAuthAppConfig config,
                             final boolean frontEndLogin) throws DotDataException {

        if (userInfo == null || userInfo.isEmpty()) {
            throw new DotRuntimeException("OAuth userinfo response was empty — cannot authenticate");
        }

        final String email   = getEmail(userInfo);
        final String subject = str(userInfo, "sub");

        if (!UtilMethods.isSet(email) && !UtilMethods.isSet(subject)) {
            throw new DotRuntimeException("OAuth userinfo response contained neither a usable email nor a subject claim");
        }

        // Namespace the IdP subject so two providers issuing the same "sub" (or a raw
        // subject that happens to collide with a local dotCMS user id) can never conflict.
        final String externalId = namespacedSubject(provider, subject);

        User user = resolveUser(email, externalId);

        if (user == null) {
            user = createUser(externalId, email, userInfo);
        }

        if (!user.isActive()) {
            throw new DotRuntimeException("OAuth user " + user.getEmailAddress() + " is not active");
        }

        applySystemRole(user, frontEndLogin);
        applyExtraRoles(user, config);
        applyProviderGroups(user, provider, accessToken, userInfo);

        final boolean loggedIn = APILocator.getLoginServiceAPI().doCookieLogin(
                PublicEncryptionFactory.encryptString(user.getUserId()), request, response, false);

        if (loggedIn) {
            // Mitigate session fixation: rotate the session id so any pre-auth
            // JSESSIONID that an attacker may have forced on the victim is no longer valid.
            Try.run(() -> request.changeSessionId())
                    .onFailure(e -> Logger.debug(OAuthHelper.class,
                            "changeSessionId() unsupported or failed: " + e.getMessage()));
            final HttpSession session = request.getSession(false);
            if (session != null) {
                session.setAttribute(com.liferay.portal.util.WebKeys.USER_ID, user.getUserId());
                session.setAttribute(com.liferay.portal.util.WebKeys.USER,    user);
                session.setAttribute(OAuthConstants.SESSION_ACCESS_TOKEN,     accessToken);
                session.setAttribute(OAuthConstants.SESSION_PROVIDER_TYPE,    provider.getProviderType());
            }
            SecurityLogger.logInfo(OAuthHelper.class,
                    new Date() + ": Successful OAuth login for " + user.getEmailAddress()
                    + " via " + provider.getProviderType() + " from " + request.getRemoteAddr());
        } else {
            throw new DotRuntimeException("doCookieLogin failed for OAuth user " + user.getEmailAddress());
        }

        return user;
    }

    private User resolveUser(final String email, final String externalId) {
        // Try email first (the common case), then the namespaced external id. Swallow lookup
        // failures — a null return just means "user doesn't exist" and we'll fall through to createUser.
        if (UtilMethods.isSet(email)) {
            final User u = Try.of(() -> APILocator.getUserAPI().loadByUserByEmail(email, APILocator.systemUser(), false))
                    .getOrNull();
            if (u != null) {
                return u;
            }
        }
        if (UtilMethods.isSet(externalId)) {
            return Try.of(() -> APILocator.getUserAPI().loadUserById(externalId)).getOrNull();
        }
        return null;
    }

    /**
     * Namespace the IdP subject claim with the provider type so unrelated providers that
     * happen to issue the same {@code sub} value cannot collide on one dotCMS user row.
     * Falls back to a generated UUID when the provider omits a subject.
     */
    private static String namespacedSubject(final OAuthProvider provider, final String subject) {
        final String providerType = provider == null || provider.getProviderType() == null
                ? "unknown" : provider.getProviderType();
        if (!UtilMethods.isSet(subject)) {
            return "oauth:" + providerType + ":" + UUIDGenerator.generateUuid();
        }
        return "oauth:" + providerType + ":" + subject;
    }

    private User createUser(final String externalId, final String email, final Map<String, Object> userInfo)
            throws DotDataException {
        final String userId    = UtilMethods.isSet(externalId) ? externalId : UUIDGenerator.generateUuid();
        final String firstName = firstNonEmpty(userInfo, FIRST_NAME_CLAIMS, "unknown");
        final String lastName  = firstNonEmpty(userInfo, LAST_NAME_CLAIMS,  "unknown");

        try {
            final User user = APILocator.getUserAPI().createUser(userId, email);
            user.setNickName(firstName);
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setActive(true);
            user.setCreateDate(new Date());
            user.setPassword(PasswordFactoryProxy.generateHash(
                    UUIDGenerator.generateUuid() + "/" + UUIDGenerator.generateUuid()));
            user.setPasswordEncrypted(true);
            APILocator.getUserAPI().save(user, APILocator.systemUser(), false);
            Logger.info(this, "Created OAuth user: " + email + " (" + userId + ")");
            return user;
        } catch (final DotSecurityException | PasswordException e) {
            throw new DotDataException("Failed creating OAuth user " + email + ": " + e.getMessage(), e);
        }
    }

    private void applySystemRole(final User user, final boolean frontEnd) {
        final Role roleToAdd = frontEnd
                ? Try.of(() -> APILocator.getRoleAPI().loadLoggedinSiteRole()).getOrNull()
                : Try.of(() -> APILocator.getRoleAPI().loadBackEndUserRole()).getOrNull();
        if (roleToAdd != null) {
            Try.run(() -> APILocator.getRoleAPI().addRoleToUser(roleToAdd, user))
                    .onFailure(e -> Logger.warn(this, "Could not assign system role: " + e.getMessage()));
        }
    }

    private void applyExtraRoles(final User user, final OAuthAppConfig config) {
        if (config.extraRoles == null) {
            return;
        }
        for (final String roleKey : config.extraRoles) {
            addRoleByKey(user, roleKey);
        }
    }

    private void applyProviderGroups(final User user,
                                     final OAuthProvider provider,
                                     final String accessToken,
                                     final Map<String, Object> userInfo) {
        final Collection<String> groups = Try.of(() -> provider.getGroups(accessToken, userInfo))
                .getOrElse(java.util.Collections.emptyList());
        for (final String roleKey : groups) {
            addRoleByKey(user, roleKey);
        }
    }

    private void addRoleByKey(final User user, final String roleKey) {
        if (!UtilMethods.isSet(roleKey)) {
            return;
        }
        try {
            final Role role = APILocator.getRoleAPI().loadRoleByKey(roleKey);
            if (role != null && !APILocator.getRoleAPI().doesUserHaveRole(user, role)) {
                APILocator.getRoleAPI().addRoleToUser(role, user);
            }
        } catch (final DotDataException e) {
            Logger.warn(this, "Could not assign role '" + roleKey + "' to " + user.getEmailAddress() + ": " + e.getMessage());
        }
    }

    private static String getEmail(final Map<String, Object> userInfo) {
        final String email = firstNonEmpty(userInfo, EMAIL_CLAIMS, null);
        return UtilMethods.isValidEmail(email) ? email : null;
    }

    private static String firstNonEmpty(final Map<String, Object> userInfo, final String[] keys, final String fallback) {
        for (final String key : keys) {
            final String v = str(userInfo, key);
            if (UtilMethods.isSet(v)) {
                return v;
            }
        }
        return fallback;
    }

    private static String str(final Map<String, Object> userInfo, final String key) {
        final Object v = userInfo.get(key);
        return v == null ? null : v.toString();
    }
}
