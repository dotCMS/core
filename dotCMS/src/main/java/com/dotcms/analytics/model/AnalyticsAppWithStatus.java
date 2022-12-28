package com.dotcms.analytics.model;

import com.dotcms.analytics.app.AnalyticsApp;

import java.io.Serializable;
import java.util.Objects;

/**
 * Wraps an {@link AnalyticsApp} instance with a {@link TokenStatus}.
 *
 * @author vico
 */
public class AnalyticsAppWithStatus implements Serializable {

    private final AnalyticsApp analyticsApp;
    private final TokenStatus tokenStatus;

    public AnalyticsAppWithStatus(AnalyticsApp analyticsApp, TokenStatus tokenStatus) {
        this.analyticsApp = analyticsApp;
        this.tokenStatus = tokenStatus;
    }

    public AnalyticsApp getAnalyticsApp() {
        return analyticsApp;
    }

    public TokenStatus getTokenStatus() {
        return tokenStatus;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AnalyticsAppWithStatus that = (AnalyticsAppWithStatus) o;
        return Objects.equals(analyticsApp, that.analyticsApp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(analyticsApp);
    }

}
