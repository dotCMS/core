package com.dotcms.analytics;

import com.dotcms.analytics.app.AnalyticsApp;
import com.dotcms.analytics.helper.AnalyticsHelper;
import com.dotcms.analytics.model.AccessToken;
import com.dotcms.analytics.model.AccessTokenFetchMode;
import com.dotcms.analytics.model.AccessTokenStatus;
import com.dotcms.analytics.model.AnalyticsKey;
import com.dotcms.analytics.model.TokenStatus;
import com.dotcms.business.SystemTableUpdatedKeyEvent;
import com.dotcms.exception.AnalyticsException;
import com.dotcms.exception.UnrecoverableAnalyticsException;
import com.dotcms.http.CircuitBreakerUrl;
import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.dotcms.rest.validation.Preconditions;
import com.dotcms.system.event.local.model.EventSubscriber;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableMap;
import io.vavr.control.Try;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Analytics API class which provides convenience methods to fetch analytics access tokens and analytics keys based on
 * a {@link AnalyticsApp} configuration.
 * The access tokens are kept in memory. They are issued and fetched from an IDP server configure
 * through {@link Config} properties. Access tokens are needed to interact with the analytics infrastructure
 * The actual analytics keys (one for each host) are stores at Analytics App level along with its configuration.
 * They are issued from a config server which its url is in the app configuration.
 *
 * @author vico
 */
public class AnalyticsAPIImpl implements AnalyticsAPI, EventSubscriber<SystemTableUpdatedKeyEvent> {

    private final AtomicReference<String> analyticsIdpUrl;
    private final AtomicLong accessTokenRenewTimeout;
    private final AtomicInteger accessTokenRenewAttempts;
    private final AtomicLong analyticsKeyRenewTimeout;
    private final AtomicInteger analyticsKeyRenewAttempts;

    private final boolean useDummyToken;

    public AnalyticsAPIImpl() {
        analyticsIdpUrl = new AtomicReference<>(resolveAnalyticsIdpUrl());
        accessTokenRenewTimeout = new AtomicLong(resolveAccessTokenRenewTimeout());
        accessTokenRenewAttempts = new AtomicInteger(resolveAccessTokenRenewAttempts());
        analyticsKeyRenewTimeout = new AtomicLong(resolveAnalyticsKeyRenewTimeout());
        analyticsKeyRenewAttempts = new AtomicInteger(resolveAnalyticsKeyRenewAttempts());
        APILocator.getLocalSystemEventsAPI().subscribe(SystemTableUpdatedKeyEvent.class, this);
        useDummyToken = Config.getBooleanProperty(ANALYTICS_USE_DUMMY_TOKEN_KEY, false);
    }

    @Override
    public void notify(final SystemTableUpdatedKeyEvent event) {
        Logger.info(this, String.format("Received event with key [%s]", event.getKey()));
        if (event.getKey().contains(ANALYTICS_IDP_URL_KEY)) {
            analyticsIdpUrl.set(resolveAnalyticsIdpUrl());
        } else if (event.getKey().contains(ANALYTICS_ACCESS_TOKEN_RENEW_TIMEOUT_KEY)) {
            accessTokenRenewTimeout.set(resolveAccessTokenRenewTimeout());
        } else if (event.getKey().contains(ANALYTICS_ACCESS_TOKEN_RENEW_ATTEMPTS_KEY)) {
            accessTokenRenewAttempts.set(resolveAccessTokenRenewAttempts());
        }  else if (event.getKey().contains(ANALYTICS_KEY_RENEW_TIMEOUT_KEY)) {
            analyticsKeyRenewTimeout.set(resolveAnalyticsKeyRenewTimeout());
        } else if (event.getKey().contains(ANALYTICS_KEY_RENEW_ATTEMPTS_KEY)) {
            analyticsKeyRenewAttempts.set(resolveAnalyticsKeyRenewAttempts());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AccessToken getCachedAccessToken(final AnalyticsApp analyticsApp) {
        if (useDummyToken) {
            return DUMMY_TOKEN;
        }

        return AccessTokens.get()
            .getAccessToken(
                analyticsApp.getAnalyticsProperties().clientId(),
                AnalyticsHelper.get().resolveAudience(analyticsApp))
            .orElse(null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AccessToken getAccessToken(final AnalyticsApp analyticsApp,
                                      final AccessTokenFetchMode fetchMode) throws AnalyticsException {
        if (fetchMode == AccessTokenFetchMode.FORCE_RENEW) {
            Logger.info(
                this,
                String.format(
                    "Forcing ACCESS_TOKEN refresh for clientId %s",
                    analyticsApp.getAnalyticsProperties().clientId()));
            // renew it right away
            return refreshAccessToken(analyticsApp);
        }

        // check for token at cache and not expired
        final AccessToken accessToken = getCachedAccessToken(analyticsApp);

        if (Objects.isNull(accessToken)) {
            if (fetchMode == AccessTokenFetchMode.BACKEND_FALLBACK) {
                // try to get it from backend only when previous attempt return null
                return refreshAccessToken(analyticsApp);
            }

            Logger.debug(
                this,
                String.format(
                    "Ignoring to get ACCESS_TOKEN from backend for clientId %s, returning null",
                    analyticsApp.getAnalyticsProperties().clientId()));
            return null;
        }

        return accessToken;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AccessToken getAccessToken(final AnalyticsApp analyticsApp) throws AnalyticsException {
        return resolveAccessToken(analyticsApp, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AccessToken refreshAccessToken(final AnalyticsApp analyticsApp) throws AnalyticsException {
        try {
            // validates that we have the url
            validateAnalytics();
        } catch (DotStateException e) {
            // there is no IDP server defined, this is an "unrecoverable" error
            throw new UnrecoverableAnalyticsException(e.getMessage(), e);
        }

        try {
            final CircuitBreakerUrl.Response<AccessToken> response = requestAccessToken(analyticsApp);

            AnalyticsHelper.get().throwFromResponse(
                response,
                String.format("Could not extract ACCESS_TOKEN from response at %s", analyticsIdpUrl.get()));

            return AnalyticsHelper.get()
                .extractToken(response)
                .map(accessToken -> {
                    Logger.debug(this, "Saving ACCESS_TOKEN to memory");
                    final AccessToken enriched = accessToken
                        .withClientId(analyticsApp.getAnalyticsProperties().clientId())
                        .withIssueDate(Instant.now())
                        .withStatus(AccessTokenStatus.builder().tokenStatus(TokenStatus.OK).build());
                    AccessTokens.get().putAccessToken(enriched);
                    return enriched;
                })
                .orElseThrow(() -> new AnalyticsException("ACCESS_TOKEN is missing from response"));
        } catch (ProcessingException e) {
            Logger.error(
                this,
                String.format(
                    "Error refreshing ACCESS_TOKEN for clientId %s",
                    analyticsApp.getAnalyticsProperties().clientId()),
                    e);
            // Meaning this is probably due to an error at the other end
            throw new AnalyticsException(
                    String.format("Could not request ACCESS_TOKEN from %s", analyticsIdpUrl.get()),
                    e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void resetAccessToken(final AnalyticsApp analyticsApp) {
        Optional
            .ofNullable(analyticsApp)
            .ifPresentOrElse(
                app -> AccessTokens.get()
                    .removeAccessToken(
                        app.getAnalyticsProperties().clientId(),
                        AnalyticsHelper.get().resolveAudience(app)),
                () -> Logger.warn(this, "Analytics app is missing"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getAnalyticsKey(final Host host) throws AnalyticsException {
        final AnalyticsApp analyticsApp = AnalyticsHelper.get().appFromHost(host);
        try {
            validateAnalyticsApp(analyticsApp);
        } catch (DotStateException e) {
            // Analytics app does not exist or is not configured correctly, this is an "unrecoverable" error
            throw new UnrecoverableAnalyticsException("Analytics App is missing or it is not configured correctly");
        }

        return analyticsApp.getAnalyticsProperties().analyticsKey();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void resetAnalyticsKey(final AnalyticsApp analyticsApp, final boolean force) throws AnalyticsException {
        // validates app
        validateAnalyticsApp(analyticsApp);

        // fetches access token and if not found than throw exception
        try {
            final AccessToken accessToken = resolveAccessToken(analyticsApp, force);
            final CircuitBreakerUrl.Response<AnalyticsKey> response = requestAnalyticsKey(analyticsApp, accessToken);
            Logger.info(
                this,
                String.format(
                    "For clientId %s found this ANALYTICS_KEY response:%s%s",
                    analyticsApp.getAnalyticsProperties().clientId(),
                    System.lineSeparator(),
                    DotObjectMapperProvider.getInstance().getDefaultObjectMapper().writeValueAsString(response)));

            final AnalyticsKey analyticsKey = AnalyticsHelper.get()
                .extractAnalyticsKey(response)
                .map(key -> Try
                    .of(() -> {
                        analyticsApp.saveAnalyticsKey(key);
                        return key;
                    })
                    .getOrElseGet(e -> {
                            Logger.error(this, String.format("Could not save ANALYTICS_KEY %s at app", key), e);
                            return null;
                    }))
                .orElseThrow(() -> new AnalyticsException("Could not fetch ANALYTICS_KEY"));
            Logger.debug(this, String.format("Analytics key reset [%s]", analyticsKey.jsKey()));
        } catch (ProcessingException | JsonProcessingException e) {
            throw new AnalyticsException("Could not request ANALYTICS_KEY", e);
        }
    }

    private String resolveAnalyticsIdpUrl() {
        return Config.getStringProperty(ANALYTICS_IDP_URL_KEY, null);
    }

    private long resolveAccessTokenRenewTimeout() {
        return Config.getLongProperty(ANALYTICS_ACCESS_TOKEN_RENEW_TIMEOUT_KEY, 4000L);
    }

    private int resolveAccessTokenRenewAttempts() {
        return Config.getIntProperty(ANALYTICS_ACCESS_TOKEN_RENEW_ATTEMPTS_KEY, 3);
    }

    private long resolveAnalyticsKeyRenewTimeout() {
        return Config.getLongProperty(ANALYTICS_KEY_RENEW_TIMEOUT_KEY, 4000L);
    }

    private int resolveAnalyticsKeyRenewAttempts() {
        return Config.getIntProperty(ANALYTICS_KEY_RENEW_ATTEMPTS_KEY, 3);
    }

    /**
     * Resolves token for analytics key taking into consideration if the app is being saved
     * (thai is fetchMode == {@link AccessTokenFetchMode#FORCE_RENEW}).
     * If so, it will try to refresh the access token.
     *
     * @param analyticsApp analytics app
     * @param force force flag
     * @return ready to use {@link AccessToken} instance
     * @throws AnalyticsException if is null and force is disabled
     */
    private AccessToken resolveAccessToken(final AnalyticsApp analyticsApp,
                                           final boolean force) throws AnalyticsException {
        final AccessToken accessToken = getAccessToken(analyticsApp, AccessTokenFetchMode.BACKEND_FALLBACK);

        if (Objects.isNull(accessToken) && !force) {
            throw new AnalyticsException(String.format(
                "ACCESS_TOKEN could not be fetched for clientId %s from %s",
                analyticsApp.getAnalyticsProperties().clientId(),
                    analyticsIdpUrl.get()));
        }

        final TokenStatus tokenStatus = AnalyticsHelper.get().resolveTokenStatus(accessToken);
        if (force || tokenStatus.matchesAny(TokenStatus.EXPIRED, TokenStatus.NONE)) {
            return getAccessToken(analyticsApp, AccessTokenFetchMode.FORCE_RENEW);
        }

        return accessToken;
    }

    /**
     * Validates by evaluating that analytics IDP url is not empty, otherwise throw an exception.
     */
    private void validateAnalytics() {
        Preconditions.checkNotEmpty(analyticsIdpUrl.get(), DotStateException.class, "Analytics IDP url is missing");
    }

    /**
     * Validates by evaluating that provided {@link AnalyticsApp} instance has the minimum properties to work.
     *
     * @param analyticsApp provided analytics app
     */
    private void validateAnalyticsApp(final AnalyticsApp analyticsApp) {
        Preconditions.checkNotNull(analyticsApp, DotStateException.class, "Analytics App cannot be null");

        if (!analyticsApp.isConfigValid()) {
            throw new DotStateException("Provided Analytics App is not configured correctly");
        }
    }

    /**
     * Logs access token response from a http interaction.
     *
     * @param response http response
     * @param analyticsApp analytics app
     */
    private void logTokenResponse(final CircuitBreakerUrl.Response<AccessToken> response, AnalyticsApp analyticsApp) {
        if (CircuitBreakerUrl.isSuccessResponse(response)) {
            return;
        }

        Logger.error(
            this,
            String.format(
                "Error requesting ACCESS_TOKEN with clientId %s from IDP server %s (response: %s)",
                analyticsApp.getAnalyticsProperties().clientId(),
                analyticsIdpUrl.get(),
                response.getStatusCode()));
    }

    /**
     * Creates map with required http headers to request a token.
     *
     * @return map representation of http headers
     */
    private Map<String, String> accessTokenHeaders() {
        return ImmutableMap.<String, String>builder()
            .put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED)
            .build();
    }

    /**
     * Request an access token by sending an HTTP post with analytics app data to defined IDP.
     *
     * @param analyticsApp provided analytics app
     * @return a http response representation
     */
    private CircuitBreakerUrl.Response<AccessToken> requestAccessToken(final AnalyticsApp analyticsApp) {
        final CircuitBreakerUrl.Response<AccessToken> response = CircuitBreakerUrl.builder()
            .setMethod(CircuitBreakerUrl.Method.POST)
            .setUrl(analyticsIdpUrl.get())
            .setTimeout(accessTokenRenewTimeout.get())
            .setTryAgainAttempts(accessTokenRenewAttempts.get())
            .setHeaders(accessTokenHeaders())
            .setRawData(prepareRequestData(analyticsApp))
            .setThrowWhenError(false)
            .build()
            .doResponse(AccessToken.class);
        logTokenResponse(response, analyticsApp);
        return response;
    }

    private String prepareRequestData(final AnalyticsApp analyticsApp) {
        return String.format(
            "client_id=%s&client_secret=%s&%s",
            analyticsApp.getAnalyticsProperties().clientId(),
            analyticsApp.getAnalyticsProperties().clientSecret(),
            "grant_type=client_credentials");
    }

    /**
     * Logs analytics key response from a http interaction.
     *
     * @param response http response
     * @param analyticsApp analytics app instance
     */
    private void logKeyResponse(final CircuitBreakerUrl.Response<AnalyticsKey> response,
                                final AnalyticsApp analyticsApp) {

        if (CircuitBreakerUrl.isSuccessResponse(response)) {
            return;
        }

        Logger.error(this, String.format(
            "Error requesting analytics key from analytics config server %s (status code: %d)",
            analyticsApp.getAnalyticsProperties().analyticsConfigUrl(),
            response.getStatusCode()));
    }

    /**
     * Request an access token by sending an HTTP post with app's data to defined IDP.
     *
     * @param analyticsApp provided analytics app
     * @param accessToken access token
     * @return a http response representation
     * @throws AnalyticsException if access token cannot be fetched
     */
    private CircuitBreakerUrl.Response<AnalyticsKey> requestAnalyticsKey(final AnalyticsApp analyticsApp,
                                                                         final AccessToken accessToken)
            throws AnalyticsException {
        AnalyticsHelper.get().checkAccessToken(accessToken);
        final CircuitBreakerUrl.Response<AnalyticsKey> response = CircuitBreakerUrl.builder()
            .setMethod(CircuitBreakerUrl.Method.GET)
            .setUrl(analyticsApp.getAnalyticsProperties().analyticsConfigUrl())
            .setTimeout(analyticsKeyRenewTimeout.get())
            .setTryAgainAttempts(analyticsKeyRenewAttempts.get())
            .setHeaders(analyticsKeyHeaders(accessToken))
            .setThrowWhenError(false)
            .build()
            .doResponse(AnalyticsKey.class);
        logKeyResponse(response, analyticsApp);

        return response;
    }

    /**
     * Prepares access token request headers in a {@link Map} with values found in a {@link AccessToken} instance.
     *
     * @param accessToken access token
     * @return map representation of http headers
     */
    private Map<String, String> analyticsKeyHeaders(final AccessToken accessToken) throws AnalyticsException {
        return CircuitBreakerUrl.authHeaders(AnalyticsHelper.get().formatBearer(accessToken));
    }

}
