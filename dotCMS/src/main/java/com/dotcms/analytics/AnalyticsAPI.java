package com.dotcms.analytics;

import com.dotcms.analytics.app.AnalyticsApp;
import com.dotcms.analytics.model.AccessToken;
import com.dotmarketing.beans.Host;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Config;


/**
 * Analytics functionality interface.
 *
 * @author vico
 */
public interface AnalyticsAPI {

    String ANALYTICS_IDP_URL_KEY = "analytics.idp.url";
    String ANALYTICS_ACCESS_TOKEN_TTL_MINUTES_KEY = "analytics.access.token.ttl.minutes";
    int ANALYTICS_ACCESS_TOKEN_TTL_MINUTES = Config.getIntProperty(ANALYTICS_ACCESS_TOKEN_TTL_MINUTES_KEY, 2) * 60;

    /**
     * Fetches an {@link java.util.Optional <AccessToken>} instance from cache falling back to get the access token
     * from analytics IDP. It also saves it in cache.
     *
     * @param host host to associate app's data with
     * @return the access token if found, otherwise empty
     */
    AccessToken fetchAccessToken(Host host) throws DotDataException;

    /**
     * Fetches the analytics key to be used to capture analytics data.
     *
     * @param host host associated with analytics app governing analytics key
     * @return an {@link String} with analytics key when found, otherwise empty
     */
    String fetchAnalyticsKey(Host host) throws DotDataException;

    /**
     * Reset analytics key to the app storage by requesting it again to the configuration server.
     *
     * @param analyticsApp resolved analytics app
     * @throws DotDataException if analytics key cannot be extracted from response or when saving to app storage
     */
    void resetAnalyticsKey(AnalyticsApp analyticsApp) throws DotDataException;

}
