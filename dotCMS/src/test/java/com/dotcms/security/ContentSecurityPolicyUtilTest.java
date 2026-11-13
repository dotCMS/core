package com.dotcms.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.mock.request.DotCMSMockRequestWithSession;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

public class ContentSecurityPolicyUtilTest {

    /**
     * Method to test: {@link ContentSecurityPolicyUtil#addHeader(HttpServletResponse)} and {@link ContentSecurityPolicyUtil#apply(String)}
     * When: a html has a script block
     * Should: calculate a new nonce, set the Content-Secutiry-Policy header and set the nonce in the script blocks
     */
    @Test
    public void calculateContentSecurityPolicyWithSciptBlocks(){
        final String previousValue = Config.getStringProperty("ContentSecurityPolicy.header");
        final HttpServletRequest previousRequest = HttpServletRequestThreadLocal.INSTANCE.getRequest();

        ContentSecurityPolicyUtil.overwriteConfigValue("script-src {script-src nonce}");

        try{
            final HttpServletResponse response = mock(HttpServletResponse.class);
            final HttpSession session = mock(HttpSession.class);
            final DotCMSMockRequestWithSession request = new DotCMSMockRequestWithSession(session, false);
            HttpServletRequestThreadLocal.INSTANCE.setRequest(request);

            ContentSecurityPolicyUtil.init(request);
            final String hmlCode = "<script>console.log('This is a test')</script> <h1>This is a example</h1>";
            final String htmlCodeResult = ContentSecurityPolicyUtil.apply(hmlCode);
            ContentSecurityPolicyUtil.addHeader(response);

            final String nonce = (String) request.getAttribute("NONCE_REQUEST_ATTRIBUTE");

            final String hmlCodeExpected = String.format(
                    "<script nonce='%s'>console.log('This is a test')</script> <h1>This is a example</h1>"
            , nonce);

            assertEquals(hmlCodeExpected, htmlCodeResult);

            verify(response)
                    .addHeader(
                        "Content-Security-Policy", String.format("script-src 'nonce-%s'", nonce));
        }  finally {
            ContentSecurityPolicyUtil.overwriteConfigValue(null);
            HttpServletRequestThreadLocal.INSTANCE.setRequest(previousRequest);
        }
    }


    /**
     * Method to test: {@link ContentSecurityPolicyUtil#addHeader(HttpServletResponse)} and {@link ContentSecurityPolicyUtil#apply(String)}
     * When: a html has a style block
     * Should: calculate a new nonce, set the Content-Secutiry-Policy header and set the nonce in the styles blocks
     */
    @Test
    public void calculateContentSecurityPolicyWithStyleBlocks(){
        final String previousValue = Config.getStringProperty("ContentSecurityPolicy.header");
        ContentSecurityPolicyUtil.overwriteConfigValue("style-src {style-src nonce}");
        final HttpServletRequest previousRequest = HttpServletRequestThreadLocal.INSTANCE.getRequest();

        try{
            final HttpServletResponse response = mock(HttpServletResponse.class);
            final HttpSession session = mock(HttpSession.class);
            final DotCMSMockRequestWithSession request = new DotCMSMockRequestWithSession(session, false);
            HttpServletRequestThreadLocal.INSTANCE.setRequest(request);

            ContentSecurityPolicyUtil.init(request);

            final String hmlCode = "<style>.h1{background-color: red;}</style> <h1>This is a example</h1>";
            final String htmlCodeResult = ContentSecurityPolicyUtil.apply(hmlCode);
            ContentSecurityPolicyUtil.addHeader(response);

            final String nonce = (String) request.getAttribute("NONCE_REQUEST_ATTRIBUTE");

            final String hmlCodeExpected = String.format(
                    "<style nonce='%s'>.h1{background-color: red;}</style> <h1>This is a example</h1>"
                    , nonce);

            assertEquals(hmlCodeExpected, htmlCodeResult);
            verify(response)
                    .addHeader(
                            "Content-Security-Policy", String.format("style-src 'nonce-%s'", nonce));
        }  finally {
            ContentSecurityPolicyUtil.overwriteConfigValue( previousValue);
            HttpServletRequestThreadLocal.INSTANCE.setRequest(previousRequest);
        }
    }

    /**
     * Method to test: {@link ContentSecurityPolicyUtil#addHeader(HttpServletResponse)} and {@link ContentSecurityPolicyUtil#apply(String)}
     * When: a html has a style and script blocks but the PageMODE is equals to {@link com.dotmarketing.util.PageMode#PREVIEW_MODE},
     * the ContentSecurityPolicy.header is set to style-src script-src {script-src nonce} {style-src nonce}
     * Should: not calculate a new nonce nor set the Content-Secutiry-Policy header neither
     */
    @Test
    public void noCalculateContentSecurityPolicyWHenPageModeIsDifferentThanLIVE(){
        final String previousValue = Config.getStringProperty("ContentSecurityPolicy.header");
        ContentSecurityPolicyUtil.overwriteConfigValue("style-src script-src {script-src nonce} {style-src nonce}");
        final HttpServletRequest previousRequest = HttpServletRequestThreadLocal.INSTANCE.getRequest();

        try{
            final HttpServletResponse response = mock(HttpServletResponse.class);
            final HttpSession session = mock(HttpSession.class);
            final DotCMSMockRequestWithSession request = new DotCMSMockRequestWithSession(session, false);
            request.setAttribute(WebKeys.PAGE_MODE_PARAMETER, PageMode.PREVIEW_MODE);

            final User user = mock(User.class);
            when(user.isBackendUser()).thenReturn(true);

            request.setAttribute(com.liferay.portal.util.WebKeys.USER, user);

            HttpServletRequestThreadLocal.INSTANCE.setRequest(request);

            ContentSecurityPolicyUtil.init(request);

            final String hmlCode = "<style>.h1{background-color: red;}</style><script>console.log('Test')</script><h1>This is a example</h1>";
            final String htmlCodeResult = ContentSecurityPolicyUtil.apply(hmlCode);
            ContentSecurityPolicyUtil.addHeader(response);

            final String nonce = (String) request.getAttribute("NONCE_REQUEST_ATTRIBUTE");

            final String hmlCodeExpected = String.format(
                    "<style>.h1{background-color: red;}</style><script>console.log('Test')</script><h1>This is a example</h1>"
                    , nonce);

            assertEquals(hmlCodeExpected, htmlCodeResult);
            verify(response, never())
                    .addHeader(
                            "Content-Security-Policy", String.format("style-src 'nonce-%s'", nonce));
        }  finally {
            ContentSecurityPolicyUtil.overwriteConfigValue( previousValue);
            HttpServletRequestThreadLocal.INSTANCE.setRequest(previousRequest);
        }
    }

    /**
     * Method to test: {@link ContentSecurityPolicyUtil#addHeader(HttpServletResponse)} and {@link ContentSecurityPolicyUtil#apply(String)}
     * When: a html has more than one script block
     * Should: calculate a new nonce, set the Content-Secutiry-Policy header and set the nonce in each script blocks
     */
    @Test
    public void calculateContentSecurityPolicyWithMoreThanOneSciptBlocks(){
        final String previousValue = Config.getStringProperty("ContentSecurityPolicy.header");
        ContentSecurityPolicyUtil.overwriteConfigValue("script-src {script-src nonce}");
        final HttpServletRequest previousRequest = HttpServletRequestThreadLocal.INSTANCE.getRequest();

        try {
            final HttpServletResponse response = mock(HttpServletResponse.class);
            final HttpSession session = mock(HttpSession.class);
            final DotCMSMockRequestWithSession request = new DotCMSMockRequestWithSession(session, false);
            HttpServletRequestThreadLocal.INSTANCE.setRequest(request);

            ContentSecurityPolicyUtil.init(request);

            final String hmlCode = "<script>console.log('This is a test')</script> <h1>This is a example</h1> <script>console.log('This is a test 2')</script>";
            final String htmlCodeResult = ContentSecurityPolicyUtil.apply(hmlCode);
            ContentSecurityPolicyUtil.addHeader(response);

            final String nonce = (String) request.getAttribute("NONCE_REQUEST_ATTRIBUTE");

            final String hmlCodeExpected = String.format(
                    "<script nonce='%s'>console.log('This is a test')</script> <h1>This is a example</h1> <script nonce='%s'>console.log('This is a test 2')</script>"
                    , nonce, nonce);

            assertEquals(hmlCodeExpected, htmlCodeResult);
            verify(response)
                    .addHeader(
                            "Content-Security-Policy",
                            String.format("script-src 'nonce-%s'", nonce));
        }  finally {
            ContentSecurityPolicyUtil.overwriteConfigValue(previousValue);
            HttpServletRequestThreadLocal.INSTANCE.setRequest(previousRequest);
        }

    }

    /**
     * Method to test: {@link ContentSecurityPolicyUtil#addHeader(HttpServletResponse)} and {@link ContentSecurityPolicyUtil#apply(String)}
     * When: a html has more than one style block
     * Should: calculate a new nonce, set the Content-Secutiry-Policy header and set the nonce in each style blocks
     */
    @Test
    public void calculateContentSecurityPolicyWithMoreThanOneStyleBlocks(){
        final String previousValue = Config.getStringProperty("ContentSecurityPolicy.header");
        ContentSecurityPolicyUtil.overwriteConfigValue("style-src {style-src nonce}");
        final HttpServletRequest previousRequest = HttpServletRequestThreadLocal.INSTANCE.getRequest();

        try {
            final HttpServletResponse response = mock(HttpServletResponse.class);
            final HttpSession session = mock(HttpSession.class);
            final DotCMSMockRequestWithSession request = new DotCMSMockRequestWithSession(session, false);
            HttpServletRequestThreadLocal.INSTANCE.setRequest(request);

            ContentSecurityPolicyUtil.init(request);

            final String hmlCode = "<style>.h1{background-color: red;}</style>  <h1>This is a example</h1> <style>.h1{background-color: blue;}</style> ";
            final String htmlCodeResult = ContentSecurityPolicyUtil.apply(hmlCode);
            ContentSecurityPolicyUtil.addHeader(response);

            final String nonce = (String) request.getAttribute("NONCE_REQUEST_ATTRIBUTE");
            assertNotNull(nonce);

            final String hmlCodeExpected = String.format(
                    "<style nonce='%s'>.h1{background-color: red;}</style>  <h1>This is a example</h1> <style nonce='%s'>.h1{background-color: blue;}</style> "
                    , nonce, nonce);

            assertEquals(hmlCodeExpected, htmlCodeResult);
            verify(response)
                    .addHeader(
                            "Content-Security-Policy",
                            String.format("style-src 'nonce-%s'", nonce));
        }  finally {
            ContentSecurityPolicyUtil.overwriteConfigValue(previousValue);
            HttpServletRequestThreadLocal.INSTANCE.setRequest(previousRequest);
        }

    }

    /**
     * Method to test: {@link ContentSecurityPolicyUtil#addHeader(HttpServletResponse)} and {@link ContentSecurityPolicyUtil#apply(String)}
     * When: a html no has any script block
     * Should: calculate a new nonce and set the Content-Secutiry-Policy header
     */
    @Test
    public void calculateContentSecurityPolicyWithNoSciptBlocks(){
        final String previousValue = Config.getStringProperty("ContentSecurityPolicy.header");
        ContentSecurityPolicyUtil.overwriteConfigValue("script-src {script-src nonce}");
        final HttpServletRequest previousRequest = HttpServletRequestThreadLocal.INSTANCE.getRequest();

        try {
            final HttpServletResponse response = mock(HttpServletResponse.class);
            final HttpSession session = mock(HttpSession.class);
            final DotCMSMockRequestWithSession request = new DotCMSMockRequestWithSession(session, false);
            HttpServletRequestThreadLocal.INSTANCE.setRequest(request);

            ContentSecurityPolicyUtil.init(request);

            final String hmlCode = "<h1>This is a example</h1>";
            final String htmlCodeResult = ContentSecurityPolicyUtil.apply(hmlCode);
            ContentSecurityPolicyUtil.addHeader(response);

            final String hmlCodeExpected = "<h1>This is a example</h1>";

            assertEquals(hmlCodeExpected, htmlCodeResult);
            verify(response).addHeader(eq("Content-Security-Policy"), any());
        }  finally {
            ContentSecurityPolicyUtil.overwriteConfigValue(previousValue);
            HttpServletRequestThreadLocal.INSTANCE.setRequest(previousRequest);
        }
    }

    /**
     * Method to test: {@link ContentSecurityPolicyUtil#addHeader(HttpServletResponse)} and {@link ContentSecurityPolicyUtil#apply(String)}
     * When: a html no has any style block
     * Should: calculate a new nonce and set the Content-Secutiry-Policy header
     */
    @Test
    public void calculateContentSecurityPolicyWithNoStyleBlocks(){
        final String previousValue = Config.getStringProperty("ContentSecurityPolicy.header");
        ContentSecurityPolicyUtil.overwriteConfigValue("style-src {style-src nonce}");
        final HttpServletRequest previousRequest = HttpServletRequestThreadLocal.INSTANCE.getRequest();

        try {
            final HttpServletResponse response = mock(HttpServletResponse.class);
            final HttpSession session = mock(HttpSession.class);
            final DotCMSMockRequestWithSession request = new DotCMSMockRequestWithSession(session, false);
            HttpServletRequestThreadLocal.INSTANCE.setRequest(request);

            ContentSecurityPolicyUtil.init(request);

            final String hmlCode = "<h1>This is a example</h1>";
            final String htmlCodeResult = ContentSecurityPolicyUtil.apply(hmlCode);
            ContentSecurityPolicyUtil.addHeader(response);

            final String hmlCodeExpected = "<h1>This is a example</h1>";

            assertEquals(hmlCodeExpected, htmlCodeResult);
            verify(response).addHeader(eq("Content-Security-Policy"), any());
        }  finally {
            ContentSecurityPolicyUtil.overwriteConfigValue(previousValue);
            HttpServletRequestThreadLocal.INSTANCE.setRequest(previousRequest);
        }
    }

    private static final String ENABLED = "CONTENT_SECURITY_POLICY_BACKEND_ENABLED";
    private static final String REPORT_ONLY = "CONTENT_SECURITY_POLICY_BACKEND_REPORT_ONLY";
    private static final String DEFAULT_POLICY = "frame-ancestors 'self'; object-src 'none'";

    /**
     * Writes the back-end policy to a mock response while the given property holds the given
     * string, which is how Config returns a value read from the system table, where no
     * boolean parsing has happened.
     */
    private HttpServletResponse backendHeaderWith(final String property, final String value) {
        try (MockedStatic<Config> config = Mockito.mockStatic(Config.class)) {
            config.when(() -> Config.getStringProperty(any(), any())).thenAnswer(
                    invocation -> property.equals(invocation.getArgument(0))
                            ? value : invocation.getArgument(1));
            final HttpServletResponse response = mock(HttpServletResponse.class);
            ContentSecurityPolicyUtil.addBackendHeader(response);
            return response;
        }
    }

    /**
     * Method to test: {@link ContentSecurityPolicyUtil#addBackendHeader(HttpServletResponse)}
     * Given scenario: the on/off switch holds a misspelled value such as {@code flase}
     * Expected result: the switch falls back to enabled, so the policy is still sent
     */
    @Test
    public void addBackendHeader_whenEnabledValueMisspelled_shouldStillSendPolicy() {
        verify(backendHeaderWith(ENABLED, "flase")).setHeader("Content-Security-Policy", DEFAULT_POLICY);
    }

    /**
     * Method to test: {@link ContentSecurityPolicyUtil#addBackendHeader(HttpServletResponse)}
     * Given scenario: the on/off switch holds {@code  FALSE } with different case and spaces
     * Expected result: the value is honoured and no header is sent
     */
    @Test
    public void addBackendHeader_whenEnabledValueFalseWithCaseAndSpaces_shouldSendNothing() {
        verify(backendHeaderWith(ENABLED, " FALSE "), never()).setHeader(any(), any());
    }

    /**
     * Method to test: {@link ContentSecurityPolicyUtil#addBackendHeader(HttpServletResponse)}
     * Given scenario: the report-only switch holds a misspelled value
     * Expected result: it falls back to enforcing the policy
     */
    @Test
    public void addBackendHeader_whenReportOnlyValueMisspelled_shouldEnforcePolicy() {
        final HttpServletResponse response = backendHeaderWith(REPORT_ONLY, "ture");
        verify(response).setHeader("Content-Security-Policy", DEFAULT_POLICY);
        verify(response, never()).setHeader(eq("Content-Security-Policy-Report-Only"), any());
    }

    /**
     * Method to test: {@link ContentSecurityPolicyUtil#addBackendHeader(HttpServletResponse)}
     * Given scenario: the report-only switch holds {@code True}
     * Expected result: the policy is sent as report-only
     */
    @Test
    public void addBackendHeader_whenReportOnlyValueTrue_shouldSendReportOnly() {
        verify(backendHeaderWith(REPORT_ONLY, "True"))
                .setHeader("Content-Security-Policy-Report-Only", DEFAULT_POLICY);
    }

}
