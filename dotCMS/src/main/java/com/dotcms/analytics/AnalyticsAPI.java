package com.dotcms.analytics;

import com.dotcms.analytics.app.AnalyticsApp;
import com.dotcms.analytics.model.AccessToken;
import com.dotcms.analytics.model.AccessTokenFetchMode;
import com.dotcms.analytics.model.AccessTokenStatus;
import com.dotcms.analytics.model.TokenStatus;
import com.dotcms.exception.AnalyticsException;
import com.dotmarketing.beans.Host;

import java.time.Instant;

/**
 * Analytics functionality interface.
 *
 * @author vico
 */
public interface AnalyticsAPI {

    String BEARER = "Bearer ";
    String ANALYTICS_IDP_URL_KEY = "ANALYTICS_IDP_URL";
    String ANALYTICS_USE_DUMMY_TOKEN_KEY = "ANALYTICS_USE_DUMMY_TOKEN";
    String ANALYTICS_ACCESS_TOKEN_KEY_PREFIX = "ANALYTICS_ACCESS_TOKEN";
    String ANALYTICS_ACCESS_TOKEN_TTL_KEY = "ANALYTICS_ACCESSTOKEN_TTL";
    String ANALYTICS_ACCESS_TOKEN_TTL_WINDOW_KEY = "ANALYTICS_ACCESSTOKEN_TTLWINDOW";
    String ANALYTICS_ACCESS_TOKEN_RENEW_ATTEMPTS_KEY = "ANALYTICS_ACCESSTOKEN_RENEWATTEMPTS";
    String ANALYTICS_ACCESS_TOKEN_RENEW_TIMEOUT_KEY = "ANALYTICS_ACCESSTOKEN_RENEWTIMEOUT";
    String ANALYTICS_KEY_RENEW_ATTEMPTS_KEY = "ANALYTICS_KEY_RENEWATTEMPTS";
    String ANALYTICS_KEY_RENEW_TIMEOUT_KEY = "ANALYTICS_KEY_RENEWTIMEOUT";
    String ANALYTICS_ACCESS_TOKEN_THREAD_NAME = "access-token-renew";

    AccessToken DUMMY_TOKEN = AccessToken.builder()
        .accessToken("dummy_token")
        .clientId("dummy")
        .tokenType(BEARER.trim())
        .expiresIn(Integer.MAX_VALUE)
        .status(AccessTokenStatus.builder().tokenStatus(TokenStatus.OK).build())
        .issueDate(Instant.now())
        .build();

    /**
     * Fetches an {@link AccessToken} instance from cache.
     *
     * @param analyticsApp app to associate app's data with
     * @return the access token if found, otherwise null
     * @throws AnalyticsException if access token cannot be fetched
     */
    AccessToken getCachedAccessToken(AnalyticsApp analyticsApp);

    /**
     * Fetches an {@link AccessToken} instance from cache falling back to get the access token
     * from analytics IDP when not found.
     *
     * @param analyticsApp app to associate app's data with
     * @param fetchMode fetch mode to use
     * @return the access token if found, otherwise null
     * @throws AnalyticsException if access token cannot be fetched
     */
    AccessToken getAccessToken(AnalyticsApp analyticsApp, AccessTokenFetchMode fetchMode) throws AnalyticsException;

    /**
     * Fetches an {@link AccessToken} instance from cache falling back to get the access token
     * from analytics IDP when not found.
     *
     * @param analyticsApp app to associate app's data with
     * @return the access token if found, otherwise null
     * @throws AnalyticsException if access token cannot be fetched
     */
    AccessToken getAccessToken(AnalyticsApp analyticsApp) throws AnalyticsException;

    /**
     * Requests an {@link AccessToken} with associated analytics app data and saves it to be accessible when found.
     *
     * @param analyticsApp provided analytics app
     * @return {@link AccessToken} with value when a not expired token is found, otherwise null
     */
    AccessToken refreshAccessToken(final AnalyticsApp analyticsApp) throws AnalyticsException;

    /**
     * Resets the {@link AccessToken} associated to the provided by the {@link AnalyticsApp} instance.
     * That is removing it from the cache.
     *
     * @param analyticsApp provided analytics app
     */
    void resetAccessToken(final AnalyticsApp analyticsApp);

    /**
     * Fetches the analytics key to be used to capture analytics data.
     *
     * @param host host associated with analytics app governing analytics key
     * @return an {@link String} with analytics key when found, otherwise empty
     * @throws AnalyticsException if analytics key cannot be fetched
     */
    String getAnalyticsKey(Host host) throws AnalyticsException;

    /**
     * Reset analytics key to the app storage by requesting it again to the configuration server.
     *
     * @param analyticsApp resolved analytics app
     * @param force force flag
     * @throws AnalyticsException if analytics key cannot be extracted from response or when saving to app storage
     */
    void resetAnalyticsKey(AnalyticsApp analyticsApp, boolean force) throws AnalyticsException;

}
