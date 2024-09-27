package com.dotcms.analytics.track;

import com.dotcms.analytics.app.AnalyticsApp;
import com.dotcms.analytics.track.matchers.RequestMatcher;
import com.dotcms.security.apps.AppSecrets;
import com.dotcms.security.apps.AppsAPI;
import com.dotcms.util.WhiteBlackList;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.HostWebAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Config;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This test is for the AnalyticsTrackWebInterceptor class.
 * @author jsanca
 */
public class AnalyticsTrackWebInterceptorTest {

    private final class TestMatcher implements RequestMatcher {

        final MutableBoolean wasMatcherCalled = new MutableBoolean(false);

        @Override
        public boolean match(HttpServletRequest request, HttpServletResponse response) {
            wasMatcherCalled.setValue(true);
            return false;
        }

        public boolean wasCalled() {
            return wasMatcherCalled.booleanValue();
        }

        @Override
        public boolean runBeforeRequest() {
            return true;
        }

    }

    /**
     * Method to test: AnalyticsTrackWebInterceptor#intercept
     * Given Scenario: the feature flag is off, so the matcher test should be not called
     * ExpectedResult: The test matcher should be not called
     */
    @Test
    public void test_intercept_feature_flag_turn_off() throws IOException {

        Config.CONTEXT = Mockito.mock(ServletContext.class);
        final HostWebAPI hostWebAPI = Mockito.mock(HostWebAPI.class);
        final AppsAPI appsAPI  = Mockito.mock(AppsAPI.class);
        final WhiteBlackList whiteBlackList  = Mockito.mock(WhiteBlackList.class);
        final AtomicBoolean isTurnedOn = new AtomicBoolean(false); // turn off the feature flag
        final TestMatcher testMatcher = new TestMatcher();
        final User user = new User();
        final AnalyticsTrackWebInterceptor interceptor = new AnalyticsTrackWebInterceptor(
                hostWebAPI, appsAPI, whiteBlackList, isTurnedOn, ()->user, testMatcher);
        final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        final HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        try {
            interceptor.intercept(request, response);
        }catch (Exception e) {}

        Assert.assertFalse("The test matcher should be not called, ff is off", testMatcher.wasCalled());
    }

    /**
     * Method to test: AnalyticsTrackWebInterceptor#intercept
     * Given Scenario: the feature flag is on but not config, so the matcher test should be not called
     * ExpectedResult: The test matcher should be not called
     */
    @Test
    public void test_intercept_feature_flag_turn_on_and_no_analytics_app() throws IOException, DotDataException, DotSecurityException {

        Config.CONTEXT = Mockito.mock(ServletContext.class);
        final HostWebAPI hostWebAPI = Mockito.mock(HostWebAPI.class);
        final AppsAPI appsAPI  = Mockito.mock(AppsAPI.class);
        final WhiteBlackList whiteBlackList  = Mockito.mock(WhiteBlackList.class);
        final AtomicBoolean isTurnedOn = new AtomicBoolean(true); // turn on the feature flag
        final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        final HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        final TestMatcher testMatcher = new TestMatcher();
        final Host currentHost = Mockito.mock(Host.class);
        final User user = new User();

        Mockito.when(hostWebAPI.getCurrentHostNoThrow(request)).thenReturn(currentHost);
        Mockito.when(appsAPI.getSecrets(AnalyticsApp.ANALYTICS_APP_KEY,
                true, currentHost, user)).thenReturn(Optional.empty()); // no config

        final AnalyticsTrackWebInterceptor interceptor = new AnalyticsTrackWebInterceptor(
                hostWebAPI, appsAPI, whiteBlackList, isTurnedOn, ()->user, testMatcher);

        try {
            interceptor.intercept(request, response);
        }catch (Exception e) {}

        Assert.assertFalse("The test matcher should be not called, no config set", testMatcher.wasCalled());
    }

    /**
     * Method to test: AnalyticsTrackWebInterceptor#intercept
     * Given Scenario: the feature flag is on and there is config, so the matcher test should be called
     * ExpectedResult: The test matcher should be  called
     */
    @Test
    public void test_intercept_feature_flag_turn_on_and_with_analytics_app() throws IOException, DotDataException, DotSecurityException {

        Config.CONTEXT = Mockito.mock(ServletContext.class);
        final HostWebAPI hostWebAPI = Mockito.mock(HostWebAPI.class);
        final AppsAPI appsAPI  = Mockito.mock(AppsAPI.class);
        final WhiteBlackList whiteBlackList  = new WhiteBlackList.Builder()
                .addWhitePatterns(new String[]{StringPool.BLANK}) // allows everything
                .addBlackPatterns(new String[]{StringPool.BLANK}).build();
        final AtomicBoolean isTurnedOn = new AtomicBoolean(true); // turn on the feature flag
        final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        final HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        final TestMatcher testMatcher = new TestMatcher();
        final Host currentHost = Mockito.mock(Host.class);
        final AppSecrets appSecrets = new AppSecrets.Builder().withKey(AnalyticsApp.ANALYTICS_APP_KEY).build();
        final User user = Mockito.mock(User.class);

        Mockito.when(hostWebAPI.getCurrentHostNoThrow(request)).thenReturn(currentHost);
        Mockito.when(appsAPI.getSecrets(AnalyticsApp.ANALYTICS_APP_KEY,
                true, currentHost, user)).thenReturn(Optional.of(appSecrets)); // no config
        Mockito.when(request.getRequestURI()).thenReturn("/some-uri");

        try {

            final AnalyticsTrackWebInterceptor interceptor = new AnalyticsTrackWebInterceptor(
                    hostWebAPI, appsAPI, whiteBlackList, isTurnedOn, ()->user, testMatcher);
            interceptor.intercept(request, response);
        }catch (Exception e) {}

        Assert.assertTrue("The test matcher should be called", testMatcher.wasCalled());
    }

}
