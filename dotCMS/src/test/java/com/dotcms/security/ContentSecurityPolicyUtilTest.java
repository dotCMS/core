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

}
