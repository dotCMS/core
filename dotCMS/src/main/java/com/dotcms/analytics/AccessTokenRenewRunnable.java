package com.dotcms.analytics;

import com.dotcms.analytics.app.AnalyticsApp;
import com.dotcms.analytics.helper.AnalyticsHelper;
import com.dotcms.analytics.model.AccessToken;
import com.dotcms.analytics.model.AnalyticsAppWithStatus;
import com.dotcms.analytics.model.TokenStatus;
import com.dotcms.exception.AnalyticsException;
import com.dotcms.exception.UnrecoverableAnalyticsException;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.quartz.job.AccessTokenRenewJob;
import com.dotmarketing.util.Logger;

import java.util.Optional;
import java.util.Set;

/**
 * Access Token renew thread.
 * Meant to perform several attempts to renew {@link AccessToken} when is required to.
 *
 * @author vico
 */
public class AccessTokenRenewRunnable implements Runnable {

    private final AccessTokenRenewJob callerJob;
    private final Set<AnalyticsAppWithStatus> appsWithStatus;
    private final AnalyticsAPI analyticsAPI;

    public AccessTokenRenewRunnable(final AccessTokenRenewJob callerJob,
                                    final Set<AnalyticsAppWithStatus> appsWithStatus) {
        this.callerJob = callerJob;
        this.appsWithStatus = appsWithStatus;
        analyticsAPI = APILocator.getAnalyticsAPI();
    }

    /**
     * Run access token renew thread's main logic.
     */
    @Override
    public void run() {
        synchronized (this) {
            if (callerJob.isRenewRunning()) {
                return;
            }

            try {
                callerJob.setRenewRunning(true);
                appsWithStatus.forEach(this::renewToken);
            } finally {
                callerJob.setRenewRunning(false);
            }
        }
    }

    /**
     * Renews access token for provided {@link AnalyticsApp} by refreshing token from the IDP host and if everything
     * works successfully it will put the new token to the cache.
     *
     * @param appWithStatus provided analytics app - token status tuple
     */
    private void renewToken(final AnalyticsAppWithStatus appWithStatus) {
        final AnalyticsApp analyticsApp = appWithStatus.getAnalyticsApp();
        final String clientId = analyticsApp.getAnalyticsProperties().clientId();
        final TokenStatus tokenStatus = appWithStatus.getTokenStatus();
        Logger.debug(
            this,
            String.format(
                "Starting access token renew thread for clientId %s and tokenStatus %s",
                clientId,
                tokenStatus));

        boolean restore = false;
        final AccessToken found = analyticsAPI.getCachedAccessToken(analyticsApp);
        final AccessToken blocked = AnalyticsHelper.get().createBlockedToken(
            analyticsApp,
            "ACCESS_TOKEN is blocked due to access token renew");
        if (tokenStatus == TokenStatus.EXPIRED) {
            AccessTokens.get().putAccessToken(blocked);
        }

        try {
            analyticsAPI.refreshAccessToken(analyticsApp);
            Logger.info(this, String.format("ACCESS_TOKEN for clientId %s has been successfully renewed", clientId));
        } catch (AnalyticsException e) {
            Logger.error(this, String.format("Could not renew token for clientId %s", clientId), e);
            if (e instanceof UnrecoverableAnalyticsException) {
                Logger.error(
                    this,
                    String.format("Unrecoverable error while renewing ACCESS_TOKEN for clientId %s", clientId),
                    e);
                final AccessToken noop = AnalyticsHelper.get().createNoopToken(
                    analyticsApp,
                    String.format("Setting NOOP ACCESS_TOKEN for clientId %s due to %s", clientId, e.getMessage()));
                AccessTokens.get().putAccessToken(noop);
            } else {
                restore = tokenStatus == TokenStatus.EXPIRED;
            }
        } finally {
            if (restore) {
                Optional.ofNullable(found)
                    .ifPresentOrElse(
                        token -> {
                                Logger.debug(this, String.format("Restoring ACCESS_TOKEN for clientId %s", clientId));
                                AccessTokens.get().putAccessToken(token);
                            },
                        () -> AccessTokens.get().removeAccessToken(blocked));
            }
        }
    }

}
