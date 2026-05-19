package com.dotcms.analytics.listener;

import com.dotcms.http.CircuitBreakerUrl;
import com.dotcms.rest.api.v1.analytics.content.util.ContentAnalyticsUtil;
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

    private static final String BASE_URL_PROP = "DOT_ANALYTICS_BASE_URL";
    private static final String TENANT_PROP = "DOT_ANALYTICS_TENANT";
    private static final String ADMIN_PASSWORD_KEY = "adminPassword";
    private static final String BEARER_TOKEN_KEY = "bearerToken";

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

        final AppSecrets appSecrets = event.getAppSecrets();
        if (appSecrets == null || appSecrets.getSecrets() == null) {
            Logger.debug(this, "Event carries no secrets, aborting");
            return;
        }
        final Map<String, Secret> secrets = appSecrets.getSecrets();
        final String password = secretString(secrets, ADMIN_PASSWORD_KEY);

        if (!UtilMethods.isSet(password)) {
            Logger.debug(this, "Admin password not set, skipping token exchange");
            return;
        }

        final String tenant = Config.getStringProperty(TENANT_PROP, "");
        if (!UtilMethods.isSet(tenant)) {
            Logger.warn(this, TENANT_PROP + " is not configured, cannot exchange credentials for bearer token");
            clearAdminPassword(event.getHostIdentifier(), secrets);
            notifyError(event.getUserId(),
                    "Cannot exchange credentials: " + TENANT_PROP + " is not configured on this server.");
            return;
        }

        final String baseUrl = Config.getStringProperty(BASE_URL_PROP, "");
        if (!UtilMethods.isSet(baseUrl)) {
            Logger.warn(this, BASE_URL_PROP + " is not configured, cannot exchange credentials for bearer token");
            clearAdminPassword(event.getHostIdentifier(), secrets);
            notifyError(event.getUserId(),
                    "Cannot exchange credentials: " + BASE_URL_PROP + " is not configured on this server.");
            return;
        }

        final String token = exchangeToken(baseUrl, tenant, password);
        if (token != null) {
            persistTokenAndClearCredentials(event.getHostIdentifier(), secrets, token);
        } else {
            clearAdminPassword(event.getHostIdentifier(), secrets);
            notifyError(event.getUserId(),
                    "Failed to exchange admin credentials for a bearer token. "
                            + "Verify the admin password and event manager URL are correct, "
                            + "then re-enter the password and save again.");
        }
    }

    private String exchangeToken(final String baseUrl, final String tenant, final String password) {
        final String cleanBase = baseUrl.endsWith("/")
                ? baseUrl.substring(0, baseUrl.length() - 1)
                : baseUrl;
        final String tokenUrl = cleanBase + "/v1/admin/token?clientId="
                + URLEncoder.encode(tenant, StandardCharsets.UTF_8);

        final String basicAuth = "Basic " + Base64.getEncoder().encodeToString(
                (tenant + ":" + password).getBytes(StandardCharsets.UTF_8));

        try {
            final CircuitBreakerUrl.Response<String> response = CircuitBreakerUrl.builder()
                    .setUrl(tokenUrl)
                    .setMethod(CircuitBreakerUrl.Method.POST)
                    .setHeaders(Map.of(
                            HttpHeaders.AUTHORIZATION, basicAuth,
                            HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON,
                            HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON))
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
     * Persists the exchanged bearer token and clears the admin password in a single
     * atomic save. The user-entered {@code adminPassword} is intentionally not retained in
     * the app configuration — only the resulting {@code bearerToken} is stored. This
     * produces one {@link AppSecretSavedEvent} which re-enters this listener and exits
     * early via the "admin password not set" guard.
     */
    private void persistTokenAndClearCredentials(final String hostIdentifier,
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
                if (ADMIN_PASSWORD_KEY.equals(entry.getKey())
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
        }).onFailure(e -> Logger.error(this,
                "Failed to persist bearer token / clear credentials for host "
                        + hostIdentifier + ": " + e.getMessage(), e));
    }

    /**
     * Clears the {@code adminPassword} secret on the app config, preserving every other
     * field including any previously-stored bearer token. Used on failure paths so the
     * user's password is never retained — the user re-enters it on the next attempt.
     */
    private void clearAdminPassword(final String hostIdentifier,
            final Map<String, Secret> currentSecrets) {
        if (!currentSecrets.containsKey(ADMIN_PASSWORD_KEY)) {
            return;
        }
        Try.run(() -> {
            final Host host = hostAPI.find(hostIdentifier, APILocator.systemUser(), false);
            final AppSecrets.Builder builder = new AppSecrets.Builder()
                    .withKey(ContentAnalyticsUtil.CONTENT_ANALYTICS_APP_KEY);
            for (final Map.Entry<String, Secret> entry : currentSecrets.entrySet()) {
                if (ADMIN_PASSWORD_KEY.equals(entry.getKey())) {
                    continue;
                }
                builder.withSecret(entry.getKey(), entry.getValue());
            }
            APILocator.getAppsAPI().saveSecrets(builder.build(), host, APILocator.systemUser());
            Logger.info(this, "Admin password cleared after failed exchange for host " + hostIdentifier);
        }).onFailure(e -> Logger.error(this,
                "Failed to clear admin password for host " + hostIdentifier + ": " + e.getMessage(), e));
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
