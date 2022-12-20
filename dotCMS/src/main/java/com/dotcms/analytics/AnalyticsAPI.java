package com.dotcms.analytics;

import com.dotcms.analytics.app.AnalyticsApp;
import com.dotcms.analytics.model.AccessToken;
import com.dotcms.exception.AnalyticsException;
import com.dotmarketing.beans.Host;
import com.dotmarketing.util.Config;

import java.util.concurrent.TimeUnit;


/**
 * Analytics functionality interface.
 *
 * @author vico
 */
public interface AnalyticsAPI {

    String ANALYTICS_IDP_URL_KEY = "analytics.idp.url";

    String ANALYTICS_ACCESS_TOKEN_TTL_KEY = "analytics.access-token.ttl";
    int ANALYTICS_ACCESS_TOKEN_TTL = Config.getIntProperty(
        ANALYTICS_ACCESS_TOKEN_TTL_KEY,
        (int) TimeUnit.HOURS.toSeconds(1));

    String ANALYTICS_ACCESS_TOKEN_TTL_WINDOW_KEY = "analytics.access-token.ttl.window";
    int ANALYTICS_ACCESS_TOKEN_TTL_WINDOW = Config.getIntProperty(
        ANALYTICS_ACCESS_TOKEN_TTL_WINDOW_KEY,
        (int) TimeUnit.MINUTES.toSeconds(1));

    String ANALYTICS_ACCESS_TOKEN_RENEW_ATTEMPTS_KEY = "analytics.access-token.renew-attempts";
    int ANALYTICS_ACCESS_TOKEN_RENEW_ATTEMPTS = Config.getIntProperty(ANALYTICS_ACCESS_TOKEN_RENEW_ATTEMPTS_KEY, 3);
    int ANALYTICS_ACCESS_TOKEN_RENEW_TIMEOUT = 1000;

    String ANALYTICS_KEY_RENEW_ATTEMPTS_KEY = "analytics.key.renew-attempts";
    int ANALYTICS_KEY_RENEW_ATTEMPTS = Config.getIntProperty(ANALYTICS_KEY_RENEW_ATTEMPTS_KEY, 3);
    int ANALYTICS_KEY_RENEW_TIMEOUT = 1000;

    String ANALYTICS_ACCESS_TOKEN_THREAD_NAME = "access-token-renew";

    /**
     * Fetches an {@link AccessToken} instance from cache falling back to get the access token
     * from analytics IDP when not found int the cache.
     *
     * @param analyticsApp app to associate app's data with
     * @return the access token if found, otherwise null
     * @throws AnalyticsException if access token cannot be fetched
     */
    AccessToken getAccessToken(AnalyticsApp analyticsApp);

    /**
     * Fetches an {@link AccessToken} instance from cache falling back to get the access token
     * from analytics IDP when not found.
     *
     * @param analyticsApp app to associate app's data with
     * @param fetchWhenNotCached when token is not found in cache layer, get ot from backend
     * @return the access token if found, otherwise null
     * @throws AnalyticsException if access token cannot be fetched
     */
    AccessToken getAccessToken(AnalyticsApp analyticsApp, boolean fetchWhenNotCached) throws AnalyticsException;

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
     * @throws AnalyticsException if analytics key cannot be extracted from response or when saving to app storage
     */
    void resetAnalyticsKey(AnalyticsApp analyticsApp) throws AnalyticsException;

}
