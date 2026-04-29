package com.dotcms.auth.providers.oauth;

import com.dotcms.security.apps.AppSecrets;
import com.dotcms.security.apps.Secret;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.SecurityLogger;
import com.dotmarketing.util.UtilMethods;
import io.vavr.control.Try;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

/**
 * Strongly-typed configuration read from the {@code dotAuth} App secrets.
 * <p>
 * Falls back to the {@code SYSTEM_HOST} secrets if the current site has none —
 * standard behavior for App-based integrations in dotCMS.
 */
public final class OAuthAppConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    // Secret keys — previously mirrored dotOAuth.yml params; since phase 2 they are
    // edited by the dotAuth portlet, not a YAML descriptor.
    public static final String KEY_ENABLED             = "enabled";
    public static final String KEY_PROVIDER_TYPE       = "providerType";
    public static final String KEY_ISSUER_URL          = "issuerUrl";
    public static final String KEY_CLIENT_ID           = "clientId";
    public static final String KEY_CLIENT_SECRET       = "clientSecret";
    public static final String KEY_SCOPES              = "scopes";
    public static final String KEY_AUTHORIZATION_URL   = "authorizationUrl";
    public static final String KEY_TOKEN_URL           = "tokenUrl";
    public static final String KEY_USERINFO_URL        = "userinfoUrl";
    public static final String KEY_REVOCATION_URL      = "revocationUrl";
    public static final String KEY_LOGOUT_URL          = "logoutUrl";
    public static final String KEY_GROUPS_CLAIM        = "groupsClaim";
    public static final String KEY_GROUPS_URL          = "groupsUrl";
    public static final String KEY_ENABLE_BACKEND      = "enableBackend";
    public static final String KEY_ENABLE_FRONTEND     = "enableFrontend";
    public static final String KEY_EXTRA_ROLES         = "extraRoles";
    public static final String KEY_BUILD_ROLES_STRATEGY = "buildRolesStrategy";
    public static final String KEY_CALLBACK_URL        = "callbackUrl";
    public static final String KEY_HASH_USERID         = "hashUserId";

    // Headless OIDC token-exchange keys. These intentionally live beside the
    // browser-login keys in the same dotAuth App so a site can use one IdP/client
    // for dotCMS back-end sign-in and another for SPA session-ref exchange.
    public static final String KEY_EXCHANGE_ENABLED              = "exchangeEnabled";
    public static final String KEY_EXCHANGE_PROVIDER_TYPE        = "exchangeProviderType";
    public static final String KEY_EXCHANGE_ISSUER_URL           = "exchangeIssuerUrl";
    public static final String KEY_EXCHANGE_CLIENT_ID            = "exchangeClientId";
    public static final String KEY_EXCHANGE_CLIENT_SECRET        = "exchangeClientSecret";
    public static final String KEY_EXCHANGE_SCOPES               = "exchangeScopes";
    public static final String KEY_EXCHANGE_AUTHORIZATION_URL    = "exchangeAuthorizationUrl";
    public static final String KEY_EXCHANGE_TOKEN_URL            = "exchangeTokenUrl";
    public static final String KEY_EXCHANGE_USERINFO_URL         = "exchangeUserinfoUrl";
    public static final String KEY_EXCHANGE_REVOCATION_URL       = "exchangeRevocationUrl";
    public static final String KEY_EXCHANGE_LOGOUT_URL           = "exchangeLogoutUrl";
    public static final String KEY_EXCHANGE_GROUPS_CLAIM         = "exchangeGroupsClaim";
    public static final String KEY_EXCHANGE_GROUPS_URL           = "exchangeGroupsUrl";
    public static final String KEY_EXCHANGE_EXTRA_ROLES          = "exchangeExtraRoles";
    public static final String KEY_EXCHANGE_BUILD_ROLES_STRATEGY = "exchangeBuildRolesStrategy";
    public static final String KEY_EXCHANGE_CALLBACK_URL         = "exchangeCallbackUrl";
    public static final String KEY_EXCHANGE_HASH_USERID          = "exchangeHashUserId";

    public final boolean  enabled;
    public final boolean  enableBackend;
    public final boolean  enableFrontend;
    public final boolean  hashUserId;
    public final String   providerType;
    public final String   issuerUrl;
    public final String   clientId;
    public final char[]   clientSecret;
    public final String   scopes;
    public final String   authorizationUrl;
    public final String   tokenUrl;
    public final String   userinfoUrl;
    public final String   revocationUrl;
    public final String   logoutUrl;
    public final String   groupsClaim;
    public final String   groupsUrl;
    public final String[] extraRoles;
    public final String   buildRolesStrategy;
    public final String   callbackUrl;

    private OAuthAppConfig(final Map<String, Secret> secrets) {
        this.enabled          = bool(secrets, KEY_ENABLED,          false);
        this.enableBackend    = bool(secrets, KEY_ENABLE_BACKEND,   true);
        this.enableFrontend   = bool(secrets, KEY_ENABLE_FRONTEND,  false);
        // Default ON. Hashing the namespaced subject keeps user IDs free of ':' (which is
        // overloaded as a separator across the cache/permission layers) and bounds them to
        // dotcms.user.id.maxlength. Mirrors SAMLHelper's hash.userid default behavior.
        this.hashUserId       = bool(secrets, KEY_HASH_USERID,      true);
        this.providerType     = str (secrets, KEY_PROVIDER_TYPE,    OAuthConstants.PROVIDER_TYPE_OIDC);
        this.issuerUrl        = validateUrl(str(secrets, KEY_ISSUER_URL,        null), KEY_ISSUER_URL);
        this.clientId         = str (secrets, KEY_CLIENT_ID,        null);
        this.clientSecret     = chars(secrets, KEY_CLIENT_SECRET);
        this.scopes           = str (secrets, KEY_SCOPES,           "openid email profile");
        this.authorizationUrl = validateUrl(str(secrets, KEY_AUTHORIZATION_URL, null), KEY_AUTHORIZATION_URL);
        this.tokenUrl         = validateUrl(str(secrets, KEY_TOKEN_URL,         null), KEY_TOKEN_URL);
        this.userinfoUrl      = validateUrl(str(secrets, KEY_USERINFO_URL,      null), KEY_USERINFO_URL);
        this.revocationUrl    = validateUrl(str(secrets, KEY_REVOCATION_URL,    null), KEY_REVOCATION_URL);
        this.logoutUrl        = validateUrl(str(secrets, KEY_LOGOUT_URL,        null), KEY_LOGOUT_URL);
        this.groupsClaim      = str (secrets, KEY_GROUPS_CLAIM,     null);
        this.groupsUrl        = validateUrl(str(secrets, KEY_GROUPS_URL,        null), KEY_GROUPS_URL);
        this.extraRoles       = split(str(secrets, KEY_EXTRA_ROLES, null));
        this.buildRolesStrategy = str(secrets, KEY_BUILD_ROLES_STRATEGY,
                Config.getStringProperty("OAUTH_BUILD_ROLES_STRATEGY", "ALL"));
        this.callbackUrl      = validateUrl(str(secrets, KEY_CALLBACK_URL,      null), KEY_CALLBACK_URL);
    }

    private OAuthAppConfig(final Map<String, Secret> secrets, final boolean exchange) {
        this.enabled          = bool(secrets, KEY_EXCHANGE_ENABLED,
                bool(secrets, KEY_ENABLED, false));
        this.enableBackend    = false;
        this.enableFrontend   = true;
        this.hashUserId       = bool(secrets, KEY_EXCHANGE_HASH_USERID,
                bool(secrets, KEY_HASH_USERID, true));
        this.providerType     = str (secrets, KEY_EXCHANGE_PROVIDER_TYPE,
                str(secrets, KEY_PROVIDER_TYPE, OAuthConstants.PROVIDER_TYPE_OIDC));
        this.issuerUrl        = validateUrl(str(secrets, KEY_EXCHANGE_ISSUER_URL,
                str(secrets, KEY_ISSUER_URL, null)), KEY_EXCHANGE_ISSUER_URL);
        this.clientId         = str (secrets, KEY_EXCHANGE_CLIENT_ID,
                str(secrets, KEY_CLIENT_ID, null));
        this.clientSecret     = chars(secrets, KEY_EXCHANGE_CLIENT_SECRET,
                chars(secrets, KEY_CLIENT_SECRET));
        this.scopes           = str (secrets, KEY_EXCHANGE_SCOPES,
                str(secrets, KEY_SCOPES, "openid email profile"));
        this.authorizationUrl = validateUrl(str(secrets, KEY_EXCHANGE_AUTHORIZATION_URL,
                str(secrets, KEY_AUTHORIZATION_URL, null)), KEY_EXCHANGE_AUTHORIZATION_URL);
        this.tokenUrl         = validateUrl(str(secrets, KEY_EXCHANGE_TOKEN_URL,
                str(secrets, KEY_TOKEN_URL, null)), KEY_EXCHANGE_TOKEN_URL);
        this.userinfoUrl      = validateUrl(str(secrets, KEY_EXCHANGE_USERINFO_URL,
                str(secrets, KEY_USERINFO_URL, null)), KEY_EXCHANGE_USERINFO_URL);
        this.revocationUrl    = validateUrl(str(secrets, KEY_EXCHANGE_REVOCATION_URL,
                str(secrets, KEY_REVOCATION_URL, null)), KEY_EXCHANGE_REVOCATION_URL);
        this.logoutUrl        = validateUrl(str(secrets, KEY_EXCHANGE_LOGOUT_URL,
                str(secrets, KEY_LOGOUT_URL, null)), KEY_EXCHANGE_LOGOUT_URL);
        this.groupsClaim      = str (secrets, KEY_EXCHANGE_GROUPS_CLAIM,
                str(secrets, KEY_GROUPS_CLAIM, null));
        this.groupsUrl        = validateUrl(str(secrets, KEY_EXCHANGE_GROUPS_URL,
                str(secrets, KEY_GROUPS_URL, null)), KEY_EXCHANGE_GROUPS_URL);
        this.extraRoles       = split(str(secrets, KEY_EXCHANGE_EXTRA_ROLES,
                str(secrets, KEY_EXTRA_ROLES, null)));
        this.buildRolesStrategy = str(secrets, KEY_EXCHANGE_BUILD_ROLES_STRATEGY,
                str(secrets, KEY_BUILD_ROLES_STRATEGY,
                        Config.getStringProperty("OAUTH_BUILD_ROLES_STRATEGY", "ALL")));
        this.callbackUrl      = validateUrl(str(secrets, KEY_EXCHANGE_CALLBACK_URL,
                str(secrets, KEY_CALLBACK_URL, null)), KEY_EXCHANGE_CALLBACK_URL);
    }

    /**
     * Look up the OAuth config for the request's host, falling back to SYSTEM_HOST.
     * Returns empty when no App secrets are set or when the app is not enabled.
     */
    public static Optional<OAuthAppConfig> config(final HttpServletRequest request) {
        final Host host = WebAPILocator.getHostWebAPI().getCurrentHostNoThrow(request);
        return loadSecrets(host).map(OAuthAppConfig::new).filter(c -> c.enabled);
    }

    /**
     * Site-scoped lookup for the headless OIDC exchange flow. Exchange keys override
     * browser-login keys, but existing installations that only saved the original keys
     * continue to work until they opt into a separate exchange config.
     */
    public static Optional<OAuthAppConfig> exchangeConfig(final HttpServletRequest request) {
        final Host host = WebAPILocator.getHostWebAPI().getCurrentHostNoThrow(request);
        return loadSecrets(host).map(secrets -> new OAuthAppConfig(secrets, true)).filter(c -> c.enabled);
    }

    /** Site-scoped lookup (used by the ViewTool). */
    public static Optional<OAuthAppConfig> config(final Host host) {
        return loadSecrets(host).map(OAuthAppConfig::new).filter(c -> c.enabled);
    }

    private static Optional<Map<String, Secret>> loadSecrets(final Host host) {
        if (host == null) {
            return Optional.empty();
        }
        final Optional<AppSecrets> appSecrets = Try.of(() ->
                APILocator.getAppsAPI().getSecrets(OAuthConstants.APP_KEY, true, host, APILocator.systemUser()))
                .getOrElse(Optional.empty());
        return appSecrets.map(AppSecrets::getSecrets);
    }

    private static String str(final Map<String, Secret> s, final String key, final String def) {
        final Secret v = s.get(key);
        if (v == null) {
            return def;
        }
        final String val = Try.of(v::getString).getOrNull();
        return UtilMethods.isSet(val) ? val.trim() : def;
    }

    private static boolean bool(final Map<String, Secret> s, final String key, final boolean def) {
        final Secret v = s.get(key);
        if (v == null) {
            return def;
        }
        return Try.of(v::getBoolean).getOrElse(def);
    }

    private static char[] chars(final Map<String, Secret> s, final String key) {
        final Secret v = s.get(key);
        if (v == null) {
            return new char[0];
        }
        return Try.of(v::getValue).getOrElse(new char[0]);
    }

    private static char[] chars(final Map<String, Secret> s, final String key, final char[] def) {
        final Secret v = s.get(key);
        if (v == null) {
            return def == null ? new char[0] : def;
        }
        return Try.of(v::getValue).getOrElse(def == null ? new char[0] : def);
    }

    private static String[] split(final String csv) {
        return UtilMethods.isSet(csv)
                ? Arrays.stream(csv.split(",")).map(String::trim).filter(UtilMethods::isSet).toArray(String[]::new)
                : new String[0];
    }

    public boolean isOidc() {
        return OAuthConstants.PROVIDER_TYPE_OIDC.equalsIgnoreCase(providerType);
    }

    // ---------- URL validation (SSRF / TLS guards) ----------

    private static final Set<String> ALLOWED_SCHEMES = Set.of("https", "http");

    /**
     * Validate a configured IdP URL. Rejects non-HTTPS (unless the dev override
     * {@code OAUTH_ALLOW_INSECURE_URLS} is set), non-HTTP(S) schemes, and any host
     * that resolves to a loopback, link-local, site-local, or any-local address —
     * the standard SSRF defense against IMDS (169.254.169.254), localhost, and
     * RFC1918 ranges. Returns null on rejection; callers treat a null URL as
     * "not configured" and fall through cleanly.
     */
    private static String validateUrl(final String url, final String fieldName) {
        if (!UtilMethods.isSet(url)) {
            return null;
        }
        final URI uri;
        try {
            uri = new URI(url);
        } catch (final URISyntaxException e) {
            SecurityLogger.logInfo(OAuthAppConfig.class,
                    "OAuth " + fieldName + " rejected: not a valid URI (" + e.getMessage() + ")");
            return null;
        }
        final String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase();
        if (!ALLOWED_SCHEMES.contains(scheme)) {
            SecurityLogger.logInfo(OAuthAppConfig.class,
                    "OAuth " + fieldName + " rejected: scheme '" + scheme + "' not allowed");
            return null;
        }
        final boolean allowInsecure = Config.getBooleanProperty("OAUTH_ALLOW_INSECURE_URLS", false);
        if ("http".equals(scheme) && !allowInsecure) {
            SecurityLogger.logInfo(OAuthAppConfig.class,
                    "OAuth " + fieldName + " rejected: http:// is not allowed unless OAUTH_ALLOW_INSECURE_URLS=true");
            return null;
        }
        final String host = uri.getHost();
        if (!UtilMethods.isSet(host)) {
            SecurityLogger.logInfo(OAuthAppConfig.class,
                    "OAuth " + fieldName + " rejected: URI is missing a host");
            return null;
        }
        if (!allowInsecure && isInternalHost(host)) {
            SecurityLogger.logInfo(OAuthAppConfig.class,
                    "OAuth " + fieldName + " rejected: host '" + host + "' resolves to an internal/private address (SSRF guard)");
            return null;
        }
        return url;
    }

    private static boolean isInternalHost(final String host) {
        try {
            final InetAddress[] addresses = InetAddress.getAllByName(host);
            for (final InetAddress addr : addresses) {
                if (addr.isAnyLocalAddress()
                        || addr.isLoopbackAddress()
                        || addr.isLinkLocalAddress()
                        || addr.isSiteLocalAddress()
                        || addr.isMulticastAddress()) {
                    return true;
                }
            }
            return false;
        } catch (final UnknownHostException e) {
            // If the host can't be resolved at config time, be conservative and allow it through
            // rather than blocking a valid public host that's temporarily unresolvable.
            Logger.debug(OAuthAppConfig.class,
                    "SSRF guard skipped for unresolvable host '" + host + "': " + e.getMessage());
            return false;
        }
    }
}
