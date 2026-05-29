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
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.SecurityLogger;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.Encryptor;
import io.vavr.control.Try;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
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
     * Per-user intrinsic locks that guard the role-wipe + re-apply block in
     * {@link #applyBuildRolesStrategy}. Without a lock, two concurrent logins for
     * the same user (SPA opening two tabs on session expiry, simultaneous
     * requests through {@code /oauth/exchange}) can both step into the
     * "removed all, not yet reapplied" window and see the user with zero roles.
     *
     * <p>Per-JVM only — this does not coordinate across cluster nodes. Two nodes
     * racing on the same user each apply the full role set independently; worst
     * case is a redundant reapply of the same roles, which is idempotent. That
     * is an accepted cost versus the complexity of a distributed lock.
     */
    private static final Cache<String, Object> ROLE_SYNC_LOCKS =
            CacheBuilder.newBuilder().maximumSize(10_000).build();

    /**
     * Per-login role-sync strategy, mirroring {@code SAMLHelper}'s {@code build.roles}
     * configuration. Controlled per dotAuth OAuth config by {@code buildRolesStrategy},
     * with {@code OAUTH_BUILD_ROLES_STRATEGY} kept as a fallback for configs saved
     * before the option existed. Defaults to {@link BuildRolesStrategy#ALL} so an OAuth
     * user's dotCMS roles strictly reflect their current IdP group memberships on every login.
     *
     * <p>Same semantics as SAMLHelper — any strategy other than {@code STATICADD} or
     * {@code NONE} wipes all existing roles from the user before reapplying. This is
     * what lets IdP-side group removal actually take effect in dotCMS.
     *
     * <p><b>Behavior change vs. pre-dotAuth OAuth:</b> the earlier OAuth interceptor
     * was purely additive — {@code applySystemRole} + {@code applyExtraRoles} +
     * {@code applyProviderGroups} with no wipe — so any roles an admin assigned
     * through the back-end UI stuck across logins. Under the new default, those
     * admin-assigned roles are cleared on the user's next OAuth login unless they
     * happen to also be emitted by the IdP as a group. Deployments that relied on
     * admin-side role curation should set {@code OAUTH_BUILD_ROLES_STRATEGY=STATICADD}
     * (additive, legacy behavior) or {@code NONE} (leaves roles untouched). The
     * new {@code ALL} default is the right contract for IdP-driven access control,
     * but the switch needs to be called out explicitly in release notes so operators
     * know which knob to turn if their flow breaks.
     */
    public enum BuildRolesStrategy {
        /** Remove all roles, then apply system role + extraRoles + provider groups. */
        ALL,
        /** Remove all roles, then apply provider groups only. No baseline user role. */
        IDP,
        /** Remove all roles, then apply system role + extraRoles only (ignore groups). */
        STATICONLY,
        /** Additive — do NOT remove existing roles, then apply the full role set. */
        STATICADD,
        /** Do nothing with roles. Roles must be managed outside the OAuth flow. */
        NONE;

        static BuildRolesStrategy resolve() {
            final String configured = Config.getStringProperty("OAUTH_BUILD_ROLES_STRATEGY", ALL.name());
            return resolve(configured);
        }

        static BuildRolesStrategy resolve(final String configured) {
            return Try.of(() -> BuildRolesStrategy.valueOf(configured.trim().toUpperCase()))
                    .getOrElse(ALL);
        }
    }

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

        final User user = resolveOrProvisionUser(provider, accessToken, userInfo, config, frontEndLogin);

        return login(request, response, provider, accessToken, user);
    }

    /**
     * Issue the dotCMS login cookie for an already resolved OAuth user.
     */
    public User login(final HttpServletRequest request,
                      final HttpServletResponse response,
                      final OAuthProvider provider,
                      final String accessToken,
                      final User user) {
        final boolean loggedIn = APILocator.getLoginServiceAPI().doCookieLogin(
                PublicEncryptionFactory.encryptString(user.getUserId()), request, response);

        if (loggedIn) {
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

    /**
     * Resolve (or JIT-provision) a dotCMS {@link User} from an OAuth/OIDC userinfo
     * payload, applying system + extra + provider-sourced roles. Does not touch the
     * HTTP session or issue any cookie — it is reusable from both the browser
     * callback flow (which then wraps it with {@code doCookieLogin}) and the
     * stateless SPA token-exchange endpoint (which then issues a dotCMS JWT).
     *
     * @throws DotRuntimeException when the payload is empty / missing required
     *         claims or the resolved user is not active.
     */
    public User resolveOrProvisionUser(final OAuthProvider provider,
                                       final String accessToken,
                                       final Map<String, Object> userInfo,
                                       final OAuthAppConfig config,
                                       final boolean frontEndLogin) throws DotDataException {
        return resolveOrProvisionUser(provider, accessToken, userInfo, null, config, frontEndLogin);
    }

    /**
     * Variant that also accepts the verified id_token claim set (OIDC). Those claims —
     * not the unsigned userinfo response — are authoritative for deciding whether the
     * asserted email may be trusted to match an existing dotCMS account. Browser-flow
     * callers must enforce that the userinfo subject equals the id_token subject before
     * calling this; the exchange flow passes the verified claims as {@code userInfo}.
     */
    public User resolveOrProvisionUser(final OAuthProvider provider,
                                       final String accessToken,
                                       final Map<String, Object> userInfo,
                                       final Map<String, Object> verifiedClaims,
                                       final OAuthAppConfig config,
                                       final boolean frontEndLogin) throws DotDataException {

        if (userInfo == null || userInfo.isEmpty()) {
            throw new DotRuntimeException("OAuth userinfo response was empty — cannot authenticate");
        }

        final String email   = getEmail(userInfo, config);
        final String subject = str(userInfo, "sub");

        if (!UtilMethods.isSet(email) && !UtilMethods.isSet(subject)) {
            throw new DotRuntimeException("OAuth userinfo response contained neither a usable email nor a subject claim");
        }

        // Namespace the IdP subject with provider type + verified issuer so two
        // trusted IdPs that emit the same "sub" can never collide on one user row.
        // The iss claim is authoritative for the exchange flow (verified id_token);
        // for the browser SSO flow the userinfo endpoint typically omits iss, so we
        // fall back to the configured issuerUrl.
        final boolean hashUserId = config == null || config.hashUserId;
        final String issuer = str(userInfo, "iss");
        final String effectiveIssuer = UtilMethods.isSet(issuer) ? issuer
                : (config != null ? config.issuerUrl : null);
        final String externalId = namespacedSubject(provider, subject, effectiveIssuer, hashUserId);

        final boolean emailVerified = isEmailVerified(userInfo, verifiedClaims);
        User user = resolveUser(email, emailVerified, externalId, provider, subject, effectiveIssuer);

        if (user == null) {
            if (config != null && !config.autoProvision) {
                throw new DotRuntimeException(
                        "Auto-provisioning is disabled and no matching dotCMS user was found for this OAuth identity");
            }
            user = createUser(externalId, email, userInfo, config);
        } else {
            updateUserProfileFromClaims(user, userInfo, config);
        }

        if (!user.isActive()) {
            throw new DotRuntimeException("OAuth user " + user.getEmailAddress() + " is not active");
        }

        applyBuildRolesStrategy(user, provider, accessToken, userInfo, config, frontEndLogin);

        // Reload the user so the caller sees the roles just assigned by
        // applyBuildRolesStrategy. Without this, isBackendUser()/isFrontendUser() may
        // read stale role state immediately after login.
        final String userId = user.getUserId();
        return Try.of(() -> APILocator.getUserAPI()
                .loadUserById(userId, APILocator.systemUser(), false))
                .getOrElse(user);
    }

    private User resolveUser(final String email,
                             final boolean emailVerified,
                             final String externalId,
                             final OAuthProvider provider,
                             final String subject,
                             final String issuer) {
        // Email may only match an EXISTING account when the IdP asserts it is verified.
        // Otherwise an IdP (or a user-editable profile) could claim an arbitrary address
        // and take over an account it doesn't own — the issuer-namespaced subject is the
        // trustworthy identity key, so an unverified email falls through to it instead.
        if (emailVerified && UtilMethods.isSet(email)) {
            final User u = Try.of(() -> APILocator.getUserAPI().loadByUserByEmail(email, APILocator.systemUser(), false))
                    .getOrNull();
            if (u != null) {
                return u;
            }
        }
        if (!UtilMethods.isSet(externalId)) {
            return null;
        }
        for (final String candidate : externalIdCandidates(externalId, provider, subject, issuer)) {
            final User u = Try.of(() -> APILocator.getUserAPI().loadUserById(candidate)).getOrNull();
            if (u != null) {
                return u;
            }
        }
        return null;
    }

    /**
     * Namespace the IdP subject with provider type AND verified issuer so two
     * trusted IdPs that emit the same {@code sub} can never collide. The issuer
     * is sanitized to keep the raw id free of {@code :} and {@code /}. When
     * {@code hashUserId} is true (default) the full string is SHA-256 hashed.
     */
    private static String namespacedSubject(final OAuthProvider provider,
                                            final String subject,
                                            final String issuer,
                                            final boolean hashUserId) {
        final String providerType = provider == null || provider.getProviderType() == null
                ? "unknown" : provider.getProviderType();
        final String issuerSegment = UtilMethods.isSet(issuer)
                ? "_" + sanitizeForId(issuer) : "";
        final String raw = UtilMethods.isSet(subject)
                ? "oauth_" + providerType + issuerSegment + "_" + subject
                : "oauth_" + providerType + issuerSegment + "_" + UUIDGenerator.generateUuid();
        return hashUserId ? hashIt(raw) : raw;
    }

    private static String sanitizeForId(final String issuer) {
        return issuer.replaceAll("^https?://", "")
                     .replaceAll("[^a-zA-Z0-9._-]", "_")
                     .replaceAll("_+$", "");
    }

    /**
     * Build the list of external-id forms to try for an existing-user lookup. The first
     * element is the primary id (issuer-namespaced); the rest include the pre-issuer
     * legacy forms so existing users aren't stranded after the namespace change.
     */
    private static List<String> externalIdCandidates(final String primary,
                                                     final OAuthProvider provider,
                                                     final String subject,
                                                     final String issuer) {
        if (!UtilMethods.isSet(subject)) {
            return Collections.singletonList(primary);
        }
        final String providerType = provider == null || provider.getProviderType() == null
                ? "unknown" : provider.getProviderType();
        final LinkedHashSet<String> ordered = new LinkedHashSet<>();
        ordered.add(primary);

        // Current format with issuer (hashed and unhashed)
        if (UtilMethods.isSet(issuer)) {
            final String withIssuer = "oauth_" + providerType + "_" + sanitizeForId(issuer) + "_" + subject;
            ordered.add(withIssuer);
            ordered.add(hashIt(withIssuer));
        }

        // Legacy formats (without issuer) for backward compatibility
        final String underscore = "oauth_" + providerType + "_" + subject;
        ordered.add(underscore);
        ordered.add(hashIt(underscore));
        final String colon = "oauth:" + providerType + ":" + subject;
        ordered.add(colon);
        return new ArrayList<>(ordered);
    }

    private static String hashIt(final String token) {
        try {
            final String hashed = Encryptor.Hashing.sha256()
                    .append(token.getBytes(StandardCharsets.UTF_8))
                    .buildUnixHash();
            return org.apache.commons.lang3.StringUtils.abbreviate(
                    hashed, Config.getIntProperty("dotcms.user.id.maxlength", 100));
        } catch (final NoSuchAlgorithmException e) {
            // SHA-256 is mandated by the JCA spec; if it's missing the JVM is broken and
            // there is no sensible fallback for the auth layer.
            throw new DotRuntimeException("SHA-256 unavailable for OAuth user id hashing", e);
        }
    }

    /**
     * Orchestrate per-login role sync: wipe existing roles (for strategies that call
     * for it), then reapply the layers configured by the resolved strategy. Mirrors
     * {@code SAMLHelper.addRoles} so OAuth/OIDC users behave the same way SAML users
     * do on repeat logins — IdP-side group removal actually takes effect in dotCMS.
     */
    private void applyBuildRolesStrategy(final User user,
                                         final OAuthProvider provider,
                                         final String accessToken,
                                         final Map<String, Object> userInfo,
                                         final OAuthAppConfig config,
                                         final boolean frontEndLogin) {
        final BuildRolesStrategy strategy = BuildRolesStrategy.resolve(
                config == null ? null : config.buildRolesStrategy);

        if (strategy == BuildRolesStrategy.NONE) {
            Logger.debug(this, () -> "OAUTH_BUILD_ROLES_STRATEGY=NONE — leaving user roles untouched");
            return;
        }

        // Serialize the remove + reapply block per user so concurrent logins for the
        // same user never observe the "wiped but not yet reapplied" intermediate state.
        // Intrinsic-monitor lock on a per-user sentinel is enough — the block runs
        // DB-bound work for well under a second and we hold nothing else inside it.
        final Object userLock;
        try {
            userLock = ROLE_SYNC_LOCKS.get(user.getUserId(), Object::new);
        } catch (final ExecutionException e) {
            throw new DotRuntimeException("Failed to acquire role sync lock", e);
        }
        synchronized (userLock) {
            // Remove all existing roles before reapplying, unless the strategy is STATICADD
            // (additive / legacy behavior). Matches SAMLHelper.addRoles exactly.
            if (strategy != BuildRolesStrategy.STATICADD) {
                Try.run(() -> APILocator.getRoleAPI().removeAllRolesFromUser(user))
                        .onFailure(e -> Logger.warn(this,
                                "Could not remove existing roles from OAuth user " + user.getUserId()
                                        + " before reapplying: " + e.getMessage()));
            }

            switch (strategy) {
                case ALL:
                case STATICADD:
                    applySystemRoles(user, config, frontEndLogin);
                    applyExtraRoles(user, config);
                    applyProviderGroups(user, provider, accessToken, userInfo, config);
                    break;
                case IDP:
                    // IdP-only: skip the logged-in / back-end baseline, add provider groups only.
                    applyProviderGroups(user, provider, accessToken, userInfo, config);
                    break;
                case STATICONLY:
                    // Static-only: system + extra roles, ignore whatever groups the IdP claims.
                    applySystemRoles(user, config, frontEndLogin);
                    applyExtraRoles(user, config);
                    break;
                default:
                    break;
            }
        }
    }

    private User createUser(final String externalId, final String email,
                            final Map<String, Object> userInfo, final OAuthAppConfig config)
            throws DotDataException {
        final String userId    = UtilMethods.isSet(externalId) ? externalId : UUIDGenerator.generateUuid();
        final String firstName = claimValue(userInfo, config == null ? null : config.firstNameClaim, FIRST_NAME_CLAIMS, "unknown");
        final String lastName  = claimValue(userInfo, config == null ? null : config.lastNameClaim,  LAST_NAME_CLAIMS,  "unknown");

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

    private void updateUserProfileFromClaims(final User user, final Map<String, Object> userInfo,
                                              final OAuthAppConfig config) throws DotDataException {
        boolean changed = false;
        final String firstName = claimValue(userInfo, config == null ? null : config.firstNameClaim, FIRST_NAME_CLAIMS, null);
        if (UtilMethods.isSet(firstName) && !firstName.equals(user.getFirstName())) {
            user.setFirstName(firstName);
            user.setNickName(firstName);
            changed = true;
        }
        final String lastName = claimValue(userInfo, config == null ? null : config.lastNameClaim, LAST_NAME_CLAIMS, null);
        if (UtilMethods.isSet(lastName) && !lastName.equals(user.getLastName())) {
            user.setLastName(lastName);
            changed = true;
        }
        if (!changed) {
            return;
        }
        try {
            APILocator.getUserAPI().save(user, APILocator.systemUser(), false);
        } catch (final DotSecurityException e) {
            throw new DotDataException("Failed updating OAuth user profile "
                    + user.getUserId() + ": " + e.getMessage(), e);
        }
    }

    private void applySystemRoles(final User user, final OAuthAppConfig config, final boolean frontEndLogin) {
        final boolean addBackend  = config != null && config.enableBackend;
        final boolean addFrontend = config != null && config.enableFrontend;

        if (addBackend) {
            addRoleIfPresent(user, Try.of(() -> APILocator.getRoleAPI().loadBackEndUserRole()).getOrNull());
        }
        if (addFrontend) {
            addRoleIfPresent(user, Try.of(() -> APILocator.getRoleAPI().loadLoggedinSiteRole()).getOrNull());
        }
        if (!addBackend && !addFrontend) {
            final Role fallback = frontEndLogin
                    ? Try.of(() -> APILocator.getRoleAPI().loadLoggedinSiteRole()).getOrNull()
                    : Try.of(() -> APILocator.getRoleAPI().loadBackEndUserRole()).getOrNull();
            addRoleIfPresent(user, fallback);
        }
    }

    private void addRoleIfPresent(final User user, final Role role) {
        if (role == null) return;
        Try.run(() -> APILocator.getRoleAPI().addRoleToUser(role, user))
                .onFailure(e -> Logger.warn(this, "Could not assign system role: " + e.getMessage()));
    }

    private void applyExtraRoles(final User user, final OAuthAppConfig config) {
        if (config == null || config.extraRoles == null) {
            return;
        }
        for (final String roleKey : config.extraRoles) {
            addRoleByKey(user, roleKey);
        }
    }

    private void applyProviderGroups(final User user,
                                     final OAuthProvider provider,
                                     final String accessToken,
                                     final Map<String, Object> userInfo,
                                     final OAuthAppConfig config) {
        final Collection<String> groups = Try.of(() -> provider.getGroups(accessToken, userInfo))
                .getOrElse(java.util.Collections.emptyList());
        if (groups.isEmpty()) {
            Logger.info(this, "OAuth provider returned no groups for " + user.getEmailAddress()
                    + " — check the dotAuth App's groupsClaim and that the IdP actually emits it in the id_token / userinfo");
            return;
        }
        Logger.info(this, "OAuth provider returned groups for " + user.getEmailAddress() + ": " + groups);
        final Map<String, String> mappings = parseGroupMappings(config);
        for (final String group : groups) {
            final String roleKey = mappings.getOrDefault(group, group);
            addRoleByKey(user, roleKey);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> parseGroupMappings(final OAuthAppConfig config) {
        if (config == null || !UtilMethods.isSet(config.groupMappingsJson)) {
            return java.util.Collections.emptyMap();
        }
        try {
            final List<Map<String, String>> list = com.dotcms.rest.api.v1.DotObjectMapperProvider
                    .getInstance().getDefaultObjectMapper()
                    .readValue(config.groupMappingsJson, List.class);
            final Map<String, String> result = new java.util.HashMap<>();
            for (final Map<String, String> entry : list) {
                final String idpGroup = entry.get("idpGroup");
                final String dotcmsRole = entry.get("dotcmsRole");
                if (UtilMethods.isSet(idpGroup) && UtilMethods.isSet(dotcmsRole)) {
                    result.put(idpGroup, dotcmsRole);
                }
            }
            return result;
        } catch (final Exception e) {
            Logger.warn(OAuthHelper.class, "Failed to parse groupMappings config: " + e.getMessage());
            return java.util.Collections.emptyMap();
        }
    }

    private void addRoleByKey(final User user, final String roleKey) {
        if (!UtilMethods.isSet(roleKey)) {
            return;
        }
        try {
            final Role role = APILocator.getRoleAPI().loadRoleByKey(roleKey);
            if (role == null) {
                Logger.info(this, "OAuth group '" + roleKey + "' has no matching dotCMS role (case-sensitive lookup) — skipping for " + user.getEmailAddress());
                return;
            }
            if (!APILocator.getRoleAPI().doesUserHaveRole(user, role)) {
                APILocator.getRoleAPI().addRoleToUser(role, user);
            }
        } catch (final DotDataException e) {
            Logger.warn(this, "Could not assign role '" + roleKey + "' to " + user.getEmailAddress() + ": " + e.getMessage());
        }
    }

    private static String getEmail(final Map<String, Object> userInfo, final OAuthAppConfig config) {
        final String email = claimValue(userInfo, config == null ? null : config.emailClaim, EMAIL_CLAIMS, null);
        return UtilMethods.isValidEmail(email) ? email : null;
    }

    /**
     * Whether the IdP asserts {@code email_verified}. The verified id_token claims take
     * precedence over the unsigned userinfo response when both are present.
     */
    private static boolean isEmailVerified(final Map<String, Object> userInfo,
                                           final Map<String, Object> verifiedClaims) {
        return claimsAssertEmailVerified(verifiedClaims) || claimsAssertEmailVerified(userInfo);
    }

    private static boolean claimsAssertEmailVerified(final Map<String, Object> claims) {
        if (claims == null) {
            return false;
        }
        final Object value = claims.get("email_verified");
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return value != null && "true".equalsIgnoreCase(value.toString());
    }

    private static String claimValue(final Map<String, Object> userInfo,
                                     final String configuredClaim,
                                     final String[] fallbackKeys,
                                     final String defaultValue) {
        if (UtilMethods.isSet(configuredClaim)) {
            final String v = str(userInfo, configuredClaim);
            if (UtilMethods.isSet(v)) {
                return v;
            }
        }
        for (final String key : fallbackKeys) {
            final String v = str(userInfo, key);
            if (UtilMethods.isSet(v)) {
                return v;
            }
        }
        return defaultValue;
    }

    private static String str(final Map<String, Object> userInfo, final String key) {
        final Object v = userInfo.get(key);
        return v == null ? null : v.toString();
    }
}
