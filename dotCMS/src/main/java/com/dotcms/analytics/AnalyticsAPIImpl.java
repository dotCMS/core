package com.dotcms.analytics;

import com.dotcms.analytics.helper.AnalyticsHelper;
import com.dotcms.analytics.app.AnalyticsApp;
import com.dotcms.analytics.cache.AnalyticsCache;
import com.dotcms.analytics.model.AccessToken;
import com.dotcms.analytics.model.AnalyticsKey;
import com.dotcms.rest.validation.Preconditions;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import org.apache.commons.lang.StringUtils;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;


/**
 * Analytics API class which gathers analytics functionality.
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
     * Fetches an {@link Optional<AccessToken>} instance from cache falling back to get the access token
     * from analytics IDP. It also saves it in cache.
     *
     * @param host host to associate app's data with
     * @return the access token if found, otherwise empty
     */
    @Override
    public AccessToken fetchAccessToken(final Host host) throws DotDataException {
        return fetchAccessToken(AnalyticsHelper.getHostApp(host));
    }

    /**
     * Reset analytics key to the app storage by requesting it again to the configuration server.
     *
     * @param analyticsApp resolved analytics app
     * @throws DotDataException if analytics key cannot be extracted from response or when saving to app storage
     */
    @Override
    public void resetAnalyticsKey(final AnalyticsApp analyticsApp) throws DotDataException {
        // validates app
        validateAnalyticsApp(analyticsApp);

        _resetAnalyticsKey(analyticsApp);
    }

    /**
     * Fetches the analytics key to be used to capture analytics data.
     *
     * @param host host associated with analytics app governing analytics key
     * @return an {@link Optional<String>} with analytics key when found, otherwise empty
     */
    @Override
    public AnalyticsKey fetchAnalyticsKey(final Host host) throws DotDataException {
        final AnalyticsApp analyticsApp = AnalyticsHelper.getHostApp(host);
        validateAnalyticsApp(analyticsApp);

        final AnalyticsKey analyticsKey = analyticsApp.getAnalyticsProperties().analyticsKey();

        // check if it found and the return it
        if (isAnalyticsKeyValid(analyticsKey)) {
            Logger.info(this, String.format("Analytics key found: %s", analyticsKey.toString()));
            return analyticsKey;
        }

        _resetAnalyticsKey(analyticsApp);

        return AnalyticsHelper.getHostApp(host).getAnalyticsProperties().analyticsKey();
    }

    /**
     * Fetches an {@link Optional<AccessToken>} instance from cache falling back to get the access token
     * from analytics IDP. It also saves it in cache.
     *
     * @param analyticsApp app to associate app's data with
     * @return the access token if found, otherwise empty
     */
    private AccessToken fetchAccessToken(final AnalyticsApp analyticsApp) throws DotDataException {
        // check for token at cache and not expired
        final Optional<AccessToken> accessToken = analyticsCache
            .getAccessToken()
            .filter(token -> !AnalyticsHelper.isExpired(token));
        if (accessToken.isPresent()) {
            return accessToken.get();
        }

        // validates that we have the url
        validateAnalytics();

        // Extract token and verify that is not expired (it shouldn't but anyway)
        return refreshAccessToken(analyticsApp);
    }

    /**
     * Reset analytics key to the app storage by requesting it again to the configuration server.
     *
     * @param analyticsApp resolved analytics app
     * @throws DotDataException if analytics key cannot be extracted from response or when saving to app storage
     */
    private void _resetAnalyticsKey(final AnalyticsApp analyticsApp) throws DotDataException {
        // fetches access token and if not found than throw exception
        final Response response = requestAnalyticsKey(analyticsApp);

        AnalyticsHelper.extractAnalyticsKey(response)
            .map(key -> {
                try {
                    analyticsApp.saveAnalyticsKey(key);
                } catch (Exception e) {
                    Logger.error(this, String.format("Could not save analytics key %s at app", key));
                    return null;
                }
                return key;
            })
            .orElseThrow(() -> new DotStateException("Could not fetch analytics key"));
    }

    /**
     * Validates if {@link AnalyticsKey} instance is not null and has non-blank key.
     *
     * @param analyticsKey provided analytics key
     * @return true if is valid, otherwise false
     */
    private boolean isAnalyticsKeyValid(final AnalyticsKey analyticsKey) {
        return Objects.nonNull(analyticsKey) && StringUtils.isNotBlank(analyticsKey.jsKey());
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
     * Logs response from a http interaction.
     *
     * @param response http response
     */
    private void logResponse(final Response response) {
        if (AnalyticsHelper.isSuccessResponse(response)) {
            Logger.info(this, "Access Token requested and fetched correctly");
        } else {
            Logger.error(this, String.format(
                "Error requesting access token from IDP server %s due to: %s (status code: %d)",
                analyticsIdpUrl,
                response.getStatusInfo().getReasonPhrase(),
                response.getStatusInfo().getStatusCode()));
        }
    }

    /**
     * Request an access token by sending an HTTP post with analytics app data to defined IDP.
     *
     * @param analyticsApp provided analytics app
     * @return a http response representation
     */
    private Response requestAccessToken(final AnalyticsApp analyticsApp) {
        final Response response = ClientBuilder.newClient()
            .target(analyticsIdpUrl)
            .request(MediaType.APPLICATION_FORM_URLENCODED)
            .header(
                HttpHeaders.AUTHORIZATION,
                String.format("Basic %s", analyticsApp.clientIdAndSecret()))
            .post(Entity.entity("grant_type=client_credentials", MediaType.APPLICATION_JSON_TYPE));
        logResponse(response);
        return response;
    }

    /**
     * Requests an {@link AccessToken} with associated analytics app data and saves it to be accessible when found.
     *
     * @param analyticsApp provided analytics app
     * @return {@link Optional<AccessToken>} with value when a not expired token is found, otherwise empty\
     */
    private AccessToken refreshAccessToken(final AnalyticsApp analyticsApp) throws DotDataException {
        final Response response = requestAccessToken(analyticsApp);

        return AnalyticsHelper.extractToken(response)
            .map(accessToken -> {
                Logger.info(this, "Saving access token to cache");
                analyticsCache.putAccessToken(accessToken.withIssueDate(new Date()));
                return accessToken;
            })
            .orElseThrow(() -> new DotDataException("Could not extract access token from response, saving aborted"));
    }

    /**
     * Request an access token by sending an HTTP post with app's data to defined IDP.
     *
     * @param analyticsApp provided analytics app
     * @return a http response representation
     */
    private Response requestAnalyticsKey(final AnalyticsApp analyticsApp) throws DotDataException {
        final AccessToken accessToken = fetchAccessToken(analyticsApp);
        final Response response = ClientBuilder.newClient()
            .target(analyticsApp.getAnalyticsProperties().analyticsConfigUrl())
            .request(MediaType.APPLICATION_FORM_URLENCODED)
            .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", accessToken.accessToken()))
            .post(Entity.entity("", MediaType.TEXT_PLAIN_TYPE));

        logResponse(response);

        return response;
    }

}
