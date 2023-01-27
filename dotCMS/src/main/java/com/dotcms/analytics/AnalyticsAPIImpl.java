package com.dotcms.analytics;

import com.dotcms.analytics.app.AnalyticsApp;
import com.dotcms.analytics.cache.AnalyticsCache;
import com.dotcms.analytics.helper.AnalyticsHelper;
import com.dotcms.analytics.model.AccessToken;
import com.dotcms.analytics.model.AccessTokenStatus;
import com.dotcms.analytics.model.AnalyticsKey;
import com.dotcms.analytics.model.TokenStatus;
import com.dotcms.exception.AnalyticsException;
import com.dotcms.exception.UnrecoverableAnalyticsException;
import com.dotcms.http.CircuitBreakerUrl;
import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.dotcms.rest.validation.Preconditions;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang.StringUtils;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;


/**
 * Analytics API class which provides convenience methods to fetch analytics access tokens and analytics keys based on
 * a {@link AnalyticsApp} configuration.
 * The access tokens are kept in the Caffeine cache layer. They are issued and fetched from an IDP server configure
 * through {@link Config} properties. Access tokens are needed to interact with the analytics infrastructure
 * The actual analytics keys (one for each host) are stores at Analytics App level along with its configuration.
 * They are issued from a config server which its url is in the app configuration.
 *
 * @author vico
 */
public class AnalyticsAPIImpl implements AnalyticsAPI {

    private final String analyticsIdpUrl;
    private final AnalyticsCache analyticsCache;


    public AnalyticsAPIImpl(final String analyticsIdpUrl, final AnalyticsCache analyticsCache) {
        this.analyticsIdpUrl = analyticsIdpUrl;
        this.analyticsCache = analyticsCache;
    }

    public AnalyticsAPIImpl() {
        this(Config.getStringProperty(ANALYTICS_IDP_URL_KEY, null), CacheLocator.getAnalyticsCache());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AccessToken getAccessToken(final AnalyticsApp analyticsApp) {
        return analyticsCache
            .getAccessToken(
                analyticsApp.getAnalyticsProperties().clientId(),
                AnalyticsHelper.resolveAudience(analyticsApp))
            .orElse(null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AccessToken getAccessToken(final AnalyticsApp analyticsApp,
                                      final boolean fetchWhenNotCached) throws AnalyticsException {
        // check for token at cache and not expired
        final AccessToken accessToken = getAccessToken(analyticsApp);

        if (Objects.isNull(accessToken)) {
            if (fetchWhenNotCached) {
                return fetchAccessToken(analyticsApp);
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
    public AccessToken refreshAccessToken(final AnalyticsApp analyticsApp) throws AnalyticsException {
        try {
            final CircuitBreakerUrl.Response<AccessToken> response = requestAccessToken(analyticsApp);
            AnalyticsHelper.throwFromResponse(
                response,
                String.format("Could not extract ACCESS_TOKEN from response at %s", analyticsIdpUrl));

            return AnalyticsHelper
                .extractToken(response)
                .map(accessToken -> {
                    Logger.debug(this, "Saving ACCESS_TOKEN to cache");
                    final AccessToken enriched = accessToken
                        .withClientId(analyticsApp.getAnalyticsProperties().clientId())
                        .withIssueDate(Instant.now())
                        .withStatus(AccessTokenStatus.builder().tokenStatus(TokenStatus.OK).build());
                    analyticsCache.putAccessToken(enriched);
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
            throw new AnalyticsException(String.format("Could not request ACCESS_TOKEN from %s", analyticsIdpUrl), e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void resetAccessToken(final AnalyticsApp analyticsApp) {
        if (Objects.isNull(analyticsApp)) {
            Logger.warn(this, "Analytics app is missing");
            return;
        }

        analyticsCache.removeAccessToken(
            analyticsApp.getAnalyticsProperties().clientId(),
            AnalyticsHelper.resolveAudience(analyticsApp));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getAnalyticsKey(final Host host) throws AnalyticsException {
        final AnalyticsApp analyticsApp = AnalyticsHelper.appFromHost(host);
        try {
            validateAnalyticsApp(analyticsApp);
        } catch (DotStateException e) {
            // Analytics app does not exist or is not configured correctly, this is an "unrecoverable" error
            throw new UnrecoverableAnalyticsException("Analytics App is missing or it is not configured correctly");
        }

        final String analyticsKey = analyticsApp.getAnalyticsProperties().analyticsKey();
        // check if it found and the return it
        if (StringUtils.isNotBlank(analyticsKey)) {
            return analyticsKey;
        }

        Logger.warn(
            this,
            String.format(
                "Analytics key from clientId %s could not be resolved, resetting it",
                analyticsApp.getAnalyticsProperties().clientId()));
        _resetAnalyticsKey(analyticsApp, false);

        return AnalyticsHelper.appFromHost(host).getAnalyticsProperties().analyticsKey();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void resetAnalyticsKey(final AnalyticsApp analyticsApp, final boolean force) throws AnalyticsException {
        // validates app
        validateAnalyticsApp(analyticsApp);

        _resetAnalyticsKey(analyticsApp, force);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void resetAnalyticsKey(final AnalyticsApp analyticsApp) throws AnalyticsException {
        resetAnalyticsKey(analyticsApp, false);
    }

    /**
     * Reset analytics key to the app storage by requesting it again to the configuration server.
     *
     * @param analyticsApp resolved analytics app
     * @param force force flag
     * @throws AnalyticsException if analytics key cannot be extracted from response or when saving to app storage
     */
    private void _resetAnalyticsKey(final AnalyticsApp analyticsApp, final boolean force) throws AnalyticsException {
        // fetches access token and if not found than throw exception
        try {
            final CircuitBreakerUrl.Response<AnalyticsKey> response = requestAnalyticsKey(analyticsApp, force);
            Logger.info(
                this,
                String.format(
                    "For clientId %s foond this ANALYTICS_KEY response:\n%s",
                    analyticsApp.getAnalyticsProperties().clientId(),
                    DotObjectMapperProvider.getInstance().getDefaultObjectMapper().writeValueAsString(response)));

            AnalyticsHelper.extractAnalyticsKey(response)
                .map(key -> {
                    try {
                        analyticsApp.saveAnalyticsKey(key);
                    } catch (Exception e) {
                        Logger.error(this, String.format("Could not save ANALYTICS_KEY %s at app", key), e);
                        return null;
                    }
                    return key;
                })
                .orElseThrow(() -> new AnalyticsException("Could not fetch ANALYTICS_KEY"));
        } catch (ProcessingException | JsonProcessingException e) {
            throw new AnalyticsException("Could not request ANALYTICS_KEY", e);
        }
    }

    /**
     * Validates by evaluating that analytics IDP url is not empty, otherwise throw an exception.
     */
    private void validateAnalytics() {
        Preconditions.checkNotEmpty(analyticsIdpUrl, DotStateException.class, "Analytics IDP url is missing");
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
        if (!AnalyticsHelper.isSuccessResponse(response)) {
            return;
        }
        Logger.error(
            this,
            String.format(
                "Error requesting ACCESS_TOKEN with clientId %s from IDP server %s (response: %s)",
                analyticsApp.getAnalyticsProperties().clientId(),
                analyticsIdpUrl,
                response));
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
            .setUrl(analyticsIdpUrl)
            .setTimeout(ANALYTICS_ACCESS_TOKEN_RENEW_TIMEOUT)
            .setTryAgainAttempts(ANALYTICS_ACCESS_TOKEN_RENEW_ATTEMPTS)
            .setHeaders(accessTokenHeaders())
            .setRawData(prepareRequestData(analyticsApp))
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
     * Given an {@link AnalyticsApp} instance it will try to fetch an {@link AccessToken} from the analytics
     * infrastructure and if found it will save it in the cache.
     *
     * @param analyticsApp analytics app
     * @return the actual access token
     * @throws AnalyticsException when access token could not be fetched
     */
    private AccessToken fetchAccessToken(AnalyticsApp analyticsApp) throws AnalyticsException {
        try {
            // validates that we have the url
            validateAnalytics();
        } catch (DotStateException e) {
            // there is no IDP server defined, this is an "unrecoverable" error
            throw new UnrecoverableAnalyticsException(e.getMessage(), e);
        }

        // Extract token and verify that is not expired (it shouldn't but anyway)
        return refreshAccessToken(analyticsApp);
    }

    /**
     * Prepares access token request headers in a {@link Map} with values found in a {@link AccessToken} instance.
     *
     * @param accessToken access token
     * @return map representation of http headers
     */
    private Map<String, String> analyticsKeyHeaders(final AccessToken accessToken) throws AnalyticsException {
        return ImmutableMap.<String, String>builder()
            .put(HttpHeaders.AUTHORIZATION, AnalyticsHelper.formatBearer(accessToken))
            .put(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON)
            .build();
    }

    /**
     * Logs analytics key response from a http interaction.
     *
     * @param response http response
     * @param analyticsApp analytics app instance
     */
    private void logKeyResponse(final CircuitBreakerUrl.Response<AnalyticsKey> response,
                                final AnalyticsApp analyticsApp) {
        if (AnalyticsHelper.isSuccessResponse(response)) {
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
     * @param force force flag
     * @return a http response representation
     * @throws AnalyticsException if access token cannot be fetched
     */
    private CircuitBreakerUrl.Response<AnalyticsKey> requestAnalyticsKey(final AnalyticsApp analyticsApp,
                                                                         final boolean force)
        throws AnalyticsException {
        final AccessToken accessToken = getAccessToken(analyticsApp, force);
        if (Objects.isNull(accessToken)) {
            throw new AnalyticsException(String.format(
                "ACCESS_TOKEN could not be fetched for clientId %s from %s",
                analyticsApp.getAnalyticsProperties().clientId(),
                analyticsIdpUrl));
        }

        final CircuitBreakerUrl.Response<AnalyticsKey> response = CircuitBreakerUrl.builder()
            .setMethod(CircuitBreakerUrl.Method.GET)
            .setUrl(analyticsApp.getAnalyticsProperties().analyticsConfigUrl())
            .setTimeout(ANALYTICS_KEY_RENEW_TIMEOUT)
            .setTryAgainAttempts(ANALYTICS_KEY_RENEW_ATTEMPTS)
            .setHeaders(analyticsKeyHeaders(accessToken))
            .build()
            .doResponse(AnalyticsKey.class);
        logKeyResponse(response, analyticsApp);

        return response;
    }

}
