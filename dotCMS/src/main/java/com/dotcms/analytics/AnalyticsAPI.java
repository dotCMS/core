package com.dotcms.analytics;


import com.dotcms.analytics.app.AnalyticsApp;
import com.dotcms.analytics.model.AccessToken;
import com.dotcms.analytics.model.AnalyticsKey;
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

    AccessToken fetchAccessToken(Host host) throws DotDataException;

    void resetAnalyticsKey(AnalyticsApp analyticsApp) throws DotDataException;

    AnalyticsKey fetchAnalyticsKey(Host host) throws DotDataException;

}
