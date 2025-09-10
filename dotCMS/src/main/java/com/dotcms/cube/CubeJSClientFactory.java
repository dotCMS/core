package com.dotcms.cube;

import com.dotcms.analytics.app.AnalyticsApp;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.model.User;

/**
 * Factory to create {@link CubeJSClient} instances.
 *
 * @author vico
 */
public interface CubeJSClientFactory {

    /**
     * Creates a {@link CubeJSClient} instance for the given {@link AnalyticsApp}.
     *
     * @param analyticsApp analytics app to use to fetch the access token
     * @return cube js client instance
     *
     * @throws DotDataException
     * @throws DotSecurityException
     */
    CubeJSClient create(final AnalyticsApp analyticsApp) throws DotDataException, DotSecurityException;

    /**
     * Creates a {@link CubeJSClient} instance.
     *
     * @param user user to use to fetch the access token
     * @return cube js client instance
     */
    CubeJSClient create(final User user) throws DotDataException, DotSecurityException;


    /**
     * Creates a {@link CubeJSClient} instance for the given {@link AnalyticsApp}.
     *
     * @param user user to use to fetch the access token
     * @param siteId SiteId to take the configuration
     * @return The Analytics App for this Site
     *
     * @throws DotDataException
     * @throws DotSecurityException
     */
    CubeJSClient create(final User user, final String siteId) throws DotDataException, DotSecurityException;

}
