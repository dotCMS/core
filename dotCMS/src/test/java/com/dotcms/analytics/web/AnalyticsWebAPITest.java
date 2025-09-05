package com.dotcms.analytics.web;

import com.dotcms.analytics.app.AnalyticsApp;
import com.dotcms.experiments.business.ConfigExperimentUtil;
import com.dotcms.rest.api.v1.analytics.content.util.ContentAnalyticsUtil;
import com.dotcms.security.apps.AppSecrets;
import com.dotcms.security.apps.AppsAPI;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.web.HostWebAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static com.dotcms.rest.api.v1.analytics.content.util.ContentAnalyticsUtil.CONTENT_ANALYTICS_APP_KEY;

/**
 * Test cases for {@link AnalyticsWebAPI}
 * @author jsanca
 */
public class AnalyticsWebAPITest {

    /**
     * Method to test: {@link com.dotcms.analytics.web.AnalyticsWebAPIImpl#isAutoJsInjectionEnabled(HttpServletRequest)}
     * Given Scenario: The FF is on and the app is configured with secrets
     * ExpectedResult: The injection is allowed because all prerequisites are met
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
                CONTENT_ANALYTICS_APP_KEY, true, host, systemUser)).thenReturn(Optional.of(secrets));

        final AnalyticsWebAPI analyticsWebAPI = new AnalyticsWebAPIImpl(
                isAutoInjectTurnedOn, hostWebAPI, appsAPI, systemUserSupplier,
                currentHost-> ContentAnalyticsUtil.getSiteKeyFromAppSecrets(currentHost).orElse(StringPool.BLANK));

        Assert.assertTrue(analyticsWebAPI.isAutoJsInjectionEnabled(request));
    }

    /**
     * Method to test: {@link com.dotcms.analytics.web.AnalyticsWebAPIImpl#isAutoJsInjectionEnabled(HttpServletRequest)}
     * Given Scenario: The FF is off and the app is configured with secrets
     * ExpectedResult: The injection won't be allowed because not all prerequisites are met
     */
    @Test
    public void test_auto_js_injection_ff_off_not_allowed() throws DotDataException, DotSecurityException {
        // FF off
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
                CONTENT_ANALYTICS_APP_KEY, true, host, systemUser)).thenReturn(Optional.of(secrets));

        final AnalyticsWebAPI analyticsWebAPI = new AnalyticsWebAPIImpl(
                isAutoInjectTurnedOn, hostWebAPI, appsAPI, systemUserSupplier,
                currentHost-> ContentAnalyticsUtil.getSiteKeyFromAppSecrets(currentHost).orElse(StringPool.BLANK));

        Assert.assertFalse(analyticsWebAPI.isAutoJsInjectionEnabled(request));
    }

    /**
     * Method to test: {@link com.dotcms.analytics.web.AnalyticsWebAPIImpl#isAutoJsInjectionEnabled(HttpServletRequest)}
     * Given Scenario: The FF is on but the app is not configured (no secrets)
     * ExpectedResult: The injection won't be allowed because not all prerequisites are met
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
                CONTENT_ANALYTICS_APP_KEY, true, host, systemUser)).thenReturn(Optional.empty());

        final AnalyticsWebAPI analyticsWebAPI = new AnalyticsWebAPIImpl(
                isAutoInjectTurnedOn, hostWebAPI, appsAPI, systemUserSupplier,
                currentHost-> ContentAnalyticsUtil.getSiteKeyFromAppSecrets(currentHost).orElse(StringPool.BLANK));

        Assert.assertFalse(analyticsWebAPI.isAutoJsInjectionEnabled(request));
    }

    /**
     * Method to test: {@link com.dotcms.analytics.web.AnalyticsWebAPIImpl#getCode(Host, HttpServletRequest)}
     * Given Scenario: The FF is on and no app secrets are configured, fallback analytics key is provided
     * ExpectedResult: Returns processed HTML with placeholders replaced:
     *   - ${site_auth} -> analytics key
     *   - ${debug} -> "false" (default)
     *   - ${auto_page_view} -> "true" (default)
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
                CONTENT_ANALYTICS_APP_KEY, true, host, systemUser)).thenReturn(Optional.empty());

        final AnalyticsWebAPI analyticsWebAPI = new AnalyticsWebAPIImpl(
                isAutoInjectTurnedOn, hostWebAPI, appsAPI, systemUserSupplier,
                currentHost->analyticsKey);

        final Optional<String> codeOpt = analyticsWebAPI.getCode(host, request);
        Assert.assertTrue(codeOpt.isPresent());
        Assert.assertEquals("<script src=\"/ext/analytics/ca.min.js\" data-analytics-auth=\"12345678\"  data-analytics-debug=\"false\" data-analytics-auto-page-view=\"true\" ></script>", codeOpt.get().trim());
    }

    /**
     * Method to test: {@link com.dotcms.analytics.web.AnalyticsWebAPIImpl#getCode(Host, HttpServletRequest)}
     * Given Scenario: Verify all placeholders are correctly replaced
     * ExpectedResult: Generated code contains correct values for all placeholders
     */
    @Test
    public void test_get_code_placeholders_replacement() throws DotDataException, DotSecurityException {
        final AtomicBoolean isAutoInjectTurnedOn = new AtomicBoolean(true);
        final HostWebAPI hostWebAPI = Mockito.mock(HostWebAPI.class);
        final AppsAPI appsAPI = Mockito.mock(AppsAPI.class);
        final User systemUser = Mockito.mock(User.class);
        final Host host = Mockito.mock(Host.class);
        final Supplier<User> systemUserSupplier = () -> systemUser;
        final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        final String testAnalyticsKey = "test-site-key-abc123";

        Mockito.when(request.getScheme()).thenReturn("https");
        Mockito.when(request.getLocalName()).thenReturn("localhost");
        Mockito.when(request.getLocalPort()).thenReturn(8090);
        Mockito.when(hostWebAPI.getCurrentHostNoThrow(request)).thenReturn(host);

        final AnalyticsWebAPI analyticsWebAPI = new AnalyticsWebAPIImpl(
                isAutoInjectTurnedOn, hostWebAPI, appsAPI, systemUserSupplier,
                currentHost -> testAnalyticsKey);

        final Optional<String> codeOpt = analyticsWebAPI.getCode(host, request);

        Assert.assertTrue("Code should be present", codeOpt.isPresent());

        final String generatedCode = codeOpt.get().trim();

        // Verify all placeholders were replaced
        Assert.assertFalse("Should not contain ${site_auth} placeholder", generatedCode.contains("${site_auth}"));
        Assert.assertFalse("Should not contain ${debug} placeholder", generatedCode.contains("${debug}"));
        Assert.assertFalse("Should not contain ${auto_page_view} placeholder", generatedCode.contains("${auto_page_view}"));

        // Verify correct values
        Assert.assertTrue("Should contain analytics auth", generatedCode.contains("data-analytics-auth=\"" + testAnalyticsKey + "\""));
        Assert.assertTrue("Should contain debug=false", generatedCode.contains("data-analytics-debug=\"false\""));
        Assert.assertTrue("Should contain auto-page-view=true", generatedCode.contains("data-analytics-auto-page-view=\"true\""));
        Assert.assertTrue("Should contain correct script src", generatedCode.contains("src=\"/ext/analytics/ca.min.js\""));
    }
}
