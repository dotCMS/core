package com.dotcms.cube;

import com.dotcms.analytics.AnalyticsAPI;
import com.dotcms.analytics.app.AnalyticsApp;
import com.dotcms.analytics.helper.AnalyticsHelper;
import com.dotcms.analytics.model.AccessToken;
import com.dotcms.analytics.model.AccessTokenFetchMode;
import com.dotcms.exception.AnalyticsException;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;
import javax.enterprise.context.ApplicationScoped;

/**
 * Factory to create {@link CubeJSClient} instances.
 *
 * @author vico
 */
@ApplicationScoped
public class CubeJSClientFactoryImpl implements CubeJSClientFactory {

    private static AnalyticsHelper analyticsHelper = AnalyticsHelper.get();
    private final AnalyticsAPI analyticsAPI = APILocator.getAnalyticsAPI();

    /**
     * {@inheritDoc}
     */
    @Override
    public CubeJSClient create(final AnalyticsApp analyticsApp)
        throws DotDataException, DotSecurityException {

        final AccessToken accessToken;
        try {
            accessToken = analyticsAPI.getAccessToken(analyticsApp);
        } catch (AnalyticsException e) {
            throw new DotDataException("AccessToken cannot be resolved", e);
        }

        return new CubeJSClient(analyticsApp.getAnalyticsProperties().analyticsReadUrl(), accessToken);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CubeJSClient create(final User user) throws DotDataException, DotSecurityException {
        return create(analyticsHelper.resolveAnalyticsApp(user));
    }

    @Override
    public CubeJSClient create(final User user, final String siteId) throws DotDataException, DotSecurityException {
        return create(analyticsHelper.resolveAnalyticsApp(user, siteId));
    }

    @VisibleForTesting
    public static void setAnalyticsHelper(final AnalyticsHelper analyticsHelper) {
        CubeJSClientFactoryImpl.analyticsHelper = analyticsHelper;
    }

}
