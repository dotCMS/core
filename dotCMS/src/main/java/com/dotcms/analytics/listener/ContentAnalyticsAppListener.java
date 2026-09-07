package com.dotcms.analytics.listener;

import com.dotcms.http.CircuitBreakerUrl;
import com.dotcms.rest.api.v1.analytics.content.util.ContentAnalyticsUtil;
import static com.dotcms.rest.api.v1.analytics.event.EventAnalyticsProxyHelper.DOT_ANALYTICS_BASE_URL;
import static com.dotcms.rest.api.v1.analytics.event.EventAnalyticsProxyHelper.DOT_ANALYTICS_TENANT;
import com.dotcms.security.apps.AppSecretSavedEvent;
import com.dotcms.security.apps.AppSecrets;
import com.dotcms.security.apps.AppsAPI;
import com.dotcms.security.apps.Secret;
import com.dotcms.security.apps.Type;
import com.dotcms.system.event.local.model.EventSubscriber;
import com.dotcms.system.event.local.model.KeyFilterable;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONObject;
import com.dotcms.api.system.event.message.MessageSeverity;
import com.dotcms.api.system.event.message.SystemMessageEventUtil;
import com.dotcms.api.system.event.message.builder.SystemMessage;
import com.dotcms.api.system.event.message.builder.SystemMessageBuilder;
import com.dotmarketing.util.DateUtil;
import io.vavr.control.Try;
import org.apache.commons.lang3.StringUtils;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Listens for Content Analytics app config saves and automatically exchanges
 * admin credentials for a bearer token from the event manager's token endpoint.
 *
 * <p>When a user saves the Content Analytics app with {@code adminPassword} populated,
 * this listener pairs it with the tenant from {@code DOT_ANALYTICS_TENANT} and calls
 * {@code POST {DOT_ANALYTICS_BASE_URL}/v1/admin/token}. The returned bearer token is
 * stored as a hidden secret and the admin password is cleared from the app config.
 */
public final class ContentAnalyticsAppListener
        implements EventSubscriber<AppSecretSavedEvent>, KeyFilterable {

    private static final String ADMIN_USER_KEY = "adminUser";
    private static final String ADMIN_PASSWORD_KEY = "adminPassword";
    private static final String BEARER_TOKEN_KEY = ContentAnalyticsUtil.BEARER_TOKEN_KEY;

    private final HostAPI hostAPI;

    // Package-private for unit testing — production callers go through Instance.get().
    ContentAnalyticsAppListener(final HostAPI hostAPI) {
        this.hostAPI = hostAPI;
    }

    private ContentAnalyticsAppListener() {
        this(APILocator.getHostAPI());
    }

    @Override
    public Comparable<String> getKey() {
        return ContentAnalyticsUtil.CONTENT_ANALYTICS_APP_KEY;
    }

    @Override
    public void notify(final AppSecretSavedEvent event) {
        if (Objects.isNull(event)) {
            Logger.debug(this, "Missing event, aborting");
            return;
        }
        if (StringUtils.isBlank(event.getHostIdentifier())) {
            Logger.debug(this, "Missing event host id, aborting");
            return;
        }

        final Map<String, Secret> secrets = event.getAppSecrets().getSecrets();
        final String password = secretString(secrets, ADMIN_PASSWORD_KEY);

        if (!UtilMethods.isSet(password)) {
            Logger.debug(this, "Admin password not set, skipping token exchange");
            return;
        }

        final String adminUser = secretString(secrets, ADMIN_USER_KEY);
        if (!UtilMethods.isSet(adminUser)) {
            Logger.warn(this, "Admin username not set, cannot exchange credentials for bearer token");
            clearAdminCredentials(event.getHostIdentifier(), event.getUserId(), secrets);
            notifyError(event.getUserId(),
                    "Cannot exchange credentials: enter both the admin username and password,"
                            + " then save again.");
            return;
        }

        final String tenant = Config.getStringProperty(DOT_ANALYTICS_TENANT, "");
        if (!UtilMethods.isSet(tenant)) {
            Logger.warn(this, DOT_ANALYTICS_TENANT + " is not configured, cannot exchange credentials for bearer token");
            clearAdminCredentials(event.getHostIdentifier(), event.getUserId(), secrets);
            notifyError(event.getUserId(),
                    "Cannot exchange credentials: " + DOT_ANALYTICS_TENANT + " is not configured on this server.");
            return;
        }

        final String baseUrl = Config.getStringProperty(DOT_ANALYTICS_BASE_URL, "");
        if (!UtilMethods.isSet(baseUrl)) {
            Logger.warn(this, DOT_ANALYTICS_BASE_URL + " is not configured, cannot exchange credentials for bearer token");
            clearAdminCredentials(event.getHostIdentifier(), event.getUserId(), secrets);
            notifyError(event.getUserId(),
                    "Cannot exchange credentials: " + DOT_ANALYTICS_BASE_URL + " is not configured on this server.");
            return;
        }

        final String token = exchangeToken(baseUrl, tenant, adminUser, password);
        if (token != null) {
            persistTokenAndClearCredentials(event.getHostIdentifier(), event.getUserId(), secrets, token);
        } else {
            clearAdminCredentials(event.getHostIdentifier(), event.getUserId(), secrets);
            notifyError(event.getUserId(),
                    "Failed to exchange admin credentials for a bearer token. "
                            + "Verify the admin username, password, and event manager URL are correct, "
                            + "then re-enter both credentials and save again.");
        }
    }

    private String exchangeToken(final String baseUrl, final String tenant,
                                 final String adminUser, final String password) {
        final String cleanBase = baseUrl.endsWith("/")
                ? baseUrl.substring(0, baseUrl.length() - 1)
                : baseUrl;
        final String tokenUrl = cleanBase + "/v1/admin/token?clientId="
                + URLEncoder.encode(tenant, StandardCharsets.UTF_8);

        // Basic auth carries the event manager's GLOBAL admin credentials, not the tenant's
        // — the event manager uses one admin to mint tokens for any tenant (JIT model). The
        // tenant identifier is passed via the clientId query param only.
        final String basicAuth = "Basic " + Base64.getEncoder().encodeToString(
                (adminUser + ":" + password).getBytes(StandardCharsets.UTF_8));

        try {
            final CircuitBreakerUrl.Response<String> response = CircuitBreakerUrl.builder()
                    .setUrl(tokenUrl)
                    .setMethod(CircuitBreakerUrl.Method.POST)
                    // No request body — Content-Type omitted per RFC 9110 §8.3.
                    .setHeaders(Map.of(
                            HttpHeaders.AUTHORIZATION, basicAuth,
                            HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON))
                    .setTimeout(10_000)
                    .setThrowWhenError(false)
                    .build()
                    .doResponse();

            if (response == null
                    || response.getStatusCode() != 200
                    || !UtilMethods.isSet(response.getResponse())) {
                Logger.warn(this, "Token exchange failed — "
                        + (response == null ? "no response" : "HTTP " + response.getStatusCode()));
                return null;
            }

            final JSONObject json = new JSONObject(response.getResponse());
            final String token = json.optString("token", "");
            if (!UtilMethods.isSet(token)) {
                Logger.warn(this, "Token exchange response missing 'token' field");
                return null;
            }

            Logger.info(this, "Bearer token obtained for tenant '" + tenant + "'");
            return token;
        } catch (final Exception e) {
            Logger.warnAndDebug(ContentAnalyticsAppListener.class,
                    "Token exchange request failed: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * Persists the exchanged bearer token and clears the admin credentials
     * ({@code adminUser} + {@code adminPassword}) in a single atomic save. Neither
     * user-entered credential is retained in the app configuration — only the resulting
     * {@code bearerToken} is stored. This produces one {@link AppSecretSavedEvent} which
     * re-enters this listener and exits early via the "admin password not set" guard.
     */
    // Package-private for unit testing — direct callers in this class only.
    void persistTokenAndClearCredentials(final String hostIdentifier,
            final String userId,
            final Map<String, Secret> currentSecrets, final String token) {
        Try.run(() -> {
            final Host host = hostAPI.find(hostIdentifier, APILocator.systemUser(), false);
            final Secret bearerSecret = Secret.builder()
                    .withValue(token)
                    .withHidden(true)
                    .withType(Type.STRING)
                    .build();

            final AppSecrets.Builder builder = new AppSecrets.Builder()
                    .withKey(ContentAnalyticsUtil.CONTENT_ANALYTICS_APP_KEY);
            for (final Map.Entry<String, Secret> entry : currentSecrets.entrySet()) {
                if (ADMIN_USER_KEY.equals(entry.getKey())
                        || ADMIN_PASSWORD_KEY.equals(entry.getKey())
                        || BEARER_TOKEN_KEY.equals(entry.getKey())) {
                    continue;
                }
                builder.withSecret(entry.getKey(), entry.getValue());
            }
            builder.withSecret(BEARER_TOKEN_KEY, bearerSecret);

            final AppsAPI appsAPI = APILocator.getAppsAPI();
            appsAPI.saveSecrets(builder.build(), host, APILocator.systemUser());
            Logger.info(this,
                    "Bearer token persisted and admin credentials cleared for host " + hostIdentifier);
        }).onFailure(e -> {
            Logger.error(this,
                    "Failed to persist bearer token / clear credentials for host "
                            + hostIdentifier + ": " + e.getMessage(), e);
            // Best-effort credential cleanup. The user's original save wrote adminUser and
            // adminPassword to encrypted storage; without this fallback, a failure mid-
            // token-persist would leave them sitting there indefinitely — a re-save with
            // an empty adminPassword would short-circuit on the "password not set" guard
            // above. clearAdminCredentials wraps its own Try.run and notifies on its own
            // failure, so a secondary failure is surfaced separately.
            clearAdminCredentials(hostIdentifier, userId, currentSecrets);
            notifyError(userId,
                    "Could not write the new auth token to the app config. Re-enter"
                            + " the admin username and password and save again to retry.");
        });
    }

    /**
     * Clears the {@code adminUser} and {@code adminPassword} secrets on the app config,
     * preserving every other field including any previously-stored bearer token. Used on
     * failure paths so neither credential is retained — the user re-enters both on the
     * next attempt.
     */
    private void clearAdminCredentials(final String hostIdentifier, final String userId,
            final Map<String, Secret> currentSecrets) {
        Try.run(() -> {
            final Host host = hostAPI.find(hostIdentifier, APILocator.systemUser(), false);
            final AppSecrets.Builder builder = new AppSecrets.Builder()
                    .withKey(ContentAnalyticsUtil.CONTENT_ANALYTICS_APP_KEY);
            for (final Map.Entry<String, Secret> entry : currentSecrets.entrySet()) {
                if (ADMIN_USER_KEY.equals(entry.getKey())
                        || ADMIN_PASSWORD_KEY.equals(entry.getKey())) {
                    continue;
                }
                builder.withSecret(entry.getKey(), entry.getValue());
            }
            APILocator.getAppsAPI().saveSecrets(builder.build(), host, APILocator.systemUser());
            Logger.info(this, "Admin credentials cleared after failed exchange for host " + hostIdentifier);
        }).onFailure(e -> {
            Logger.error(this,
                    "Failed to clear admin credentials for host " + hostIdentifier + ": " + e.getMessage(), e);
            notifyError(userId,
                    "The credential exchange failed AND dotCMS could not clear the admin credentials"
                            + " from the app config — they may still be persisted. Open the"
                            + " Content Analytics App and clear the fields manually, then re-save.");
        });
    }

    private void notifyError(final String userId, final String message) {
        if (StringUtils.isBlank(userId)) {
            // No user context (system init, automated task) — log instead of pushing a
            // null-userId notification that would NPE in the subscriber chain.
            Logger.warn(this, "Content Analytics save error (no user context): " + message);
            return;
        }
        final SystemMessage systemMessage = new SystemMessageBuilder()
                .setMessage(message)
                .setSeverity(MessageSeverity.ERROR)
                .setLife(DateUtil.TEN_SECOND_MILLIS)
                .create();
        SystemMessageEventUtil.getInstance()
                .pushMessage(systemMessage, Collections.singletonList(userId));
    }

    private static String secretString(final Map<String, Secret> secrets, final String key) {
        final Secret secret = secrets.get(key);
        return secret != null ? secret.getString() : "";
    }

    public enum Instance {
        SINGLETON;
        private final ContentAnalyticsAppListener provider = new ContentAnalyticsAppListener();

        public static ContentAnalyticsAppListener get() {
            return Instance.SINGLETON.provider;
        }
    }
}
