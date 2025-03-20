package com.dotcms.analytics.web;

import com.dotcms.analytics.app.AnalyticsApp;
import com.dotcms.experiments.business.ConfigExperimentUtil;
import com.dotcms.security.apps.AppSecrets;
import com.dotcms.security.apps.AppsAPI;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.web.HostWebAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.model.User;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * Test cases for {@link AnalyticsWebAPI}
 * @author jsanca
 */
public class AnalyticsWebAPITest {

    /**
     * Method to test: {@link com.dotcms.analytics.web.AnalyticsWebAPIImpl#isAutoJsInjectionEnabled(HttpServletRequest)}
     * Given Scenario: The FF is on and the app is configured
     * ExpectedResult: the injection is allowed b.c all pre at meet
     */
    @Test
    public void test_auto_js_injection_allowed() throws DotDataException, DotSecurityException {
        // FF on
        final AtomicBoolean isAutoInjectTurnedOn = new AtomicBoolean(true);
        final HostWebAPI hostWebAPI = Mockito.mock(HostWebAPI.class);
        final AppsAPI appsAPI  = Mockito.mock(AppsAPI.class);
        final User systemUser = Mockito.mock(User.class);
        final Host host = Mockito.mock(Host.class);
        final Supplier<User> systemUserSupplier = () -> systemUser;
        final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);

        final AppSecrets secrets = new AppSecrets.Builder().build();
        Mockito.when(hostWebAPI.getCurrentHostNoThrow(request)).thenReturn(host);
        Mockito.when(appsAPI.getSecrets(
                AnalyticsApp.ANALYTICS_APP_KEY, true, host, systemUser)).thenReturn(Optional.of(secrets));

        final AnalyticsWebAPI analyticsWebAPI = new AnalyticsWebAPIImpl(
                isAutoInjectTurnedOn, hostWebAPI, appsAPI, systemUserSupplier,
                currentHost-> ConfigExperimentUtil.INSTANCE.getAnalyticsKey(currentHost));

        Assert.assertTrue(analyticsWebAPI.isAutoJsInjectionEnabled(request));
    }

    /**
     * Method to test: {@link com.dotcms.analytics.web.AnalyticsWebAPIImpl#isAutoJsInjectionEnabled(HttpServletRequest)}
     * Given Scenario: The FF is off and the app is configured
     * ExpectedResult: the injection wont be allowed b.c not all pre at meet
     */
    @Test
    public void test_auto_js_injection_ff_off_not_allowed() throws DotDataException, DotSecurityException {
        // FF on
        final AtomicBoolean isAutoInjectTurnedOn = new AtomicBoolean(false);
        final HostWebAPI hostWebAPI = Mockito.mock(HostWebAPI.class);
        final AppsAPI appsAPI  = Mockito.mock(AppsAPI.class);
        final User systemUser = Mockito.mock(User.class);
        final Host host = Mockito.mock(Host.class);
        final Supplier<User> systemUserSupplier = () -> systemUser;
        final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);

        final AppSecrets secrets = new AppSecrets.Builder().build();
        Mockito.when(hostWebAPI.getCurrentHostNoThrow(request)).thenReturn(host);
        Mockito.when(appsAPI.getSecrets(
                AnalyticsApp.ANALYTICS_APP_KEY, true, host, systemUser)).thenReturn(Optional.of(secrets));

        final AnalyticsWebAPI analyticsWebAPI = new AnalyticsWebAPIImpl(
                isAutoInjectTurnedOn, hostWebAPI, appsAPI, systemUserSupplier,
                currentHost->ConfigExperimentUtil.INSTANCE.getAnalyticsKey(currentHost));

        Assert.assertFalse(analyticsWebAPI.isAutoJsInjectionEnabled(request));
    }

    /**
     * Method to test: {@link com.dotcms.analytics.web.AnalyticsWebAPIImpl#isAutoJsInjectionEnabled(HttpServletRequest)}
     * Given Scenario: The FF is off and the app is configured
     * ExpectedResult: the injection wont be allowed b.c not all pre at meet
     */
    @Test
    public void test_auto_js_injection_ff_on_but_app_not_config_then_not_allowed() throws DotDataException, DotSecurityException {
        // FF on
        final AtomicBoolean isAutoInjectTurnedOn = new AtomicBoolean(true);
        final HostWebAPI hostWebAPI = Mockito.mock(HostWebAPI.class);
        final AppsAPI appsAPI  = Mockito.mock(AppsAPI.class);
        final User systemUser = Mockito.mock(User.class);
        final Host host = Mockito.mock(Host.class);
        final Supplier<User> systemUserSupplier = () -> systemUser;
        final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);

        Mockito.when(hostWebAPI.getCurrentHostNoThrow(request)).thenReturn(host);
        Mockito.when(appsAPI.getSecrets(
                AnalyticsApp.ANALYTICS_APP_KEY, true, host, systemUser)).thenReturn(Optional.empty());

        final AnalyticsWebAPI analyticsWebAPI = new AnalyticsWebAPIImpl(
                isAutoInjectTurnedOn, hostWebAPI, appsAPI, systemUserSupplier,
                currentHost->ConfigExperimentUtil.INSTANCE.getAnalyticsKey(currentHost));

        Assert.assertFalse(analyticsWebAPI.isAutoJsInjectionEnabled(request));
    }

    /**
     * Method to test: {@link com.dotcms.analytics.web.AnalyticsWebAPIImpl#getCode(Host, HttpServletRequest)}
     * Given Scenario: The FF is off and the app is configured
     * ExpectedResult: the injection wont be allowed b.c not all pre at meet
     */
    @Test
    public void test_get_code() throws DotDataException, DotSecurityException {
        // FF on
        final AtomicBoolean isAutoInjectTurnedOn = new AtomicBoolean(true);
        final HostWebAPI hostWebAPI = Mockito.mock(HostWebAPI.class);
        final AppsAPI appsAPI  = Mockito.mock(AppsAPI.class);
        final User systemUser = Mockito.mock(User.class);
        final Host host = Mockito.mock(Host.class);
        final Supplier<User> systemUserSupplier = () -> systemUser;
        final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        final String analyticsKey = "12345678";

        Mockito.when(request.getScheme()).thenReturn("https");
        Mockito.when(request.getLocalName()).thenReturn("localhost");
        Mockito.when(request.getLocalPort()).thenReturn(8090);
        Mockito.when(hostWebAPI.getCurrentHostNoThrow(request)).thenReturn(host);
        Mockito.when(appsAPI.getSecrets(
                AnalyticsApp.ANALYTICS_APP_KEY, true, host, systemUser)).thenReturn(Optional.empty());

        final AnalyticsWebAPI analyticsWebAPI = new AnalyticsWebAPIImpl(
                isAutoInjectTurnedOn, hostWebAPI, appsAPI, systemUserSupplier,
                currentHost->analyticsKey);

        final Optional<String> codeOpt = analyticsWebAPI.getCode(host, request);
        Assert.assertTrue(codeOpt.isPresent());
        Assert.assertEquals("<script src=\"/s/ca-lib.js\" data-analytics-key=\"12345678\"  data-analytics-debug data-analytics-auto-page-view></script>", codeOpt.get().trim());
    }
}
