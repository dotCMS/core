package com.dotcms.analytics.listener;

import com.dotcms.http.CircuitBreakerUrl;
import com.dotcms.rest.api.v1.analytics.content.util.ContentAnalyticsUtil;
import com.dotcms.security.apps.AppSecretSavedEvent;
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
import io.vavr.Tuple2;
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
 * <p>When a user saves the Content Analytics app with {@code adminTenant} and
 * {@code adminPassword} populated, this listener calls
 * {@code POST {DOT_ANALYTICS_BASE_URL}/v1/admin/token} and stores the returned
 * bearer token back as a hidden secret.
 */
public final class ContentAnalyticsAppListener
        implements EventSubscriber<AppSecretSavedEvent>, KeyFilterable {

    private static final String BASE_URL_PROP = "DOT_ANALYTICS_BASE_URL";
    private static final String ADMIN_TENANT_KEY = "adminTenant";
    private static final String ADMIN_PASSWORD_KEY = "adminPassword";
    private static final String BEARER_TOKEN_KEY = "bearerToken";

    private final HostAPI hostAPI;

    private ContentAnalyticsAppListener(final HostAPI hostAPI) {
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
        final String tenant = secretString(secrets, ADMIN_TENANT_KEY);
        final String password = secretString(secrets, ADMIN_PASSWORD_KEY);
        final String existingToken = secretString(secrets, BEARER_TOKEN_KEY);

        // Re-entrancy guard: if the bearer token is already populated and
        // credentials are present, this is the write-back event — skip.
        if (UtilMethods.isSet(existingToken)
                && UtilMethods.isSet(tenant)
                && UtilMethods.isSet(password)) {
            Logger.debug(this, "Bearer token already populated, skipping token exchange");
            return;
        }

        if (!UtilMethods.isSet(tenant) || !UtilMethods.isSet(password)) {
            Logger.debug(this, "Admin credentials incomplete, skipping token exchange");
            return;
        }

        final String baseUrl = Config.getStringProperty(BASE_URL_PROP, "");
        if (!UtilMethods.isSet(baseUrl)) {
            Logger.warn(this, BASE_URL_PROP + " is not configured, cannot exchange credentials for bearer token");
            return;
        }

        final String token = exchangeToken(baseUrl, tenant, password);
        if (token != null) {
            storeBearerToken(event.getHostIdentifier(), token);
        } else {
            notifyError(event.getUserId(),
                    "Failed to exchange admin credentials for a bearer token. "
                            + "Verify the tenant ID, password, and event manager URL are correct.");
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

            if (response.getStatusCode() != 200 || !UtilMethods.isSet(response.getResponse())) {
                Logger.warn(this, "Token exchange failed — HTTP " + response.getStatusCode());
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

    private void storeBearerToken(final String hostIdentifier, final String token) {
        Try.run(() -> {
            final Host host = hostAPI.find(hostIdentifier, APILocator.systemUser(), false);
            final Secret bearerSecret = Secret.builder()
                    .withValue(token)
                    .withHidden(true)
                    .withType(Type.STRING)
                    .build();
            final AppsAPI appsAPI = APILocator.getAppsAPI();
            appsAPI.saveSecret(
                    ContentAnalyticsUtil.CONTENT_ANALYTICS_APP_KEY,
                    new Tuple2<>(BEARER_TOKEN_KEY, bearerSecret),
                    host,
                    APILocator.systemUser());
            Logger.info(this, "Stored bearer token for host " + hostIdentifier);
        }).onFailure(e -> Logger.error(this,
                "Failed to store bearer token for host " + hostIdentifier + ": " + e.getMessage(), e));
    }

    private void notifyError(final String userId, final String message) {
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
