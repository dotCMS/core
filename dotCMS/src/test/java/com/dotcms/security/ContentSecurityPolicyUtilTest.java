package com.dotcms.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.dotmarketing.util.Config;
import javax.servlet.http.HttpServletResponse;
import org.junit.Test;

public class ContentSecurityPolicyUtilTest {

    /**
     * Method to test: {@link ContentSecurityPolicyUtil#calculateContentSecurityPolicy(String, HttpServletResponse)}
     * When: a html has a script block
     * Should: calculate a new nonce, set the Content-Secutiry-Policy header and set the nonce in the script blocks
     */
    @Test
    public void calculateContentSecurityPolicyWithSciptBlocks(){
        final String previousValue = Config.getStringProperty("ContentSecurityPolicy.header");
        Config.setProperty("ContentSecurityPolicy.header", "script-src {script-src nonce}");

        try{
            final HttpServletResponse response = mock(HttpServletResponse.class);
            final String hmlCode = "<script>console.log('This is a test')</script> <h1>This is a example</h1>";
            final String htmlCodeResult = ContentSecurityPolicyUtil.calculateContentSecurityPolicy(hmlCode,
                    response);

            final String nonce = getNonce(htmlCodeResult, "<script nonce='");
            assertNotNull(nonce);

            final String hmlCodeExpected = String.format(
                    "<script nonce='%s'>console.log('This is a test')</script> <h1>This is a example</h1>"
            , nonce);

            assertEquals(hmlCodeExpected, htmlCodeResult);
            verify(response)
                    .addHeader(
                        "Content-Security-Policy", String.format("script-src 'nonce-%s'", nonce));
        }  finally {
            Config.setProperty("ContentSecurityPolicy.header", previousValue);
        }
    }


    /**
     * Method to test: {@link ContentSecurityPolicyUtil#calculateContentSecurityPolicy(String, HttpServletResponse)}
     * When: a html has a style block
     * Should: calculate a new nonce, set the Content-Secutiry-Policy header and set the nonce in the styles blocks
     */
    @Test
    public void calculateContentSecurityPolicyWithStyleBlocks(){
        final String previousValue = Config.getStringProperty("ContentSecurityPolicy.header");
        Config.setProperty("ContentSecurityPolicy.header", "style-src {style-src nonce}");

        try{
            final HttpServletResponse response = mock(HttpServletResponse.class);
            final String hmlCode = "<style>.h1{background-color: red;}</style> <h1>This is a example</h1>";
            final String htmlCodeResult = ContentSecurityPolicyUtil.calculateContentSecurityPolicy(hmlCode,
                    response);

            final String nonce = getNonce(htmlCodeResult, "<style nonce='");
            assertNotNull(nonce);

            final String hmlCodeExpected = String.format(
                    "<style nonce='%s'>.h1{background-color: red;}</style> <h1>This is a example</h1>"
                    , nonce);

            assertEquals(hmlCodeExpected, htmlCodeResult);
            verify(response)
                    .addHeader(
                            "Content-Security-Policy", String.format("style-src 'nonce-%s'", nonce));
        }  finally {
            Config.setProperty("ContentSecurityPolicy.header", previousValue);
        }
    }

    /**
     * Method to test: {@link ContentSecurityPolicyUtil#calculateContentSecurityPolicy(String, HttpServletResponse)}
     * When: a html has more than one script block
     * Should: calculate a new nonce, set the Content-Secutiry-Policy header and set the nonce in each script blocks
     */
    @Test
    public void calculateContentSecurityPolicyWithMoreThanOneSciptBlocks(){
        final String previousValue = Config.getStringProperty("ContentSecurityPolicy.header");
        Config.setProperty("ContentSecurityPolicy.header", "script-src {script-src nonce}");

        try {
            final HttpServletResponse response = mock(HttpServletResponse.class);
            final String hmlCode = "<script>console.log('This is a test')</script> <h1>This is a example</h1> <script>console.log('This is a test 2')</script>";
            final String htmlCodeResult = ContentSecurityPolicyUtil.calculateContentSecurityPolicy(
                    hmlCode,
                    response);

            final String nonce = getNonce(htmlCodeResult, "<script nonce='");
            assertNotNull(nonce);

            final String hmlCodeExpected = String.format(
                    "<script nonce='%s'>console.log('This is a test')</script> <h1>This is a example</h1> <script nonce='%s'>console.log('This is a test 2')</script>"
                    , nonce, nonce);

            assertEquals(hmlCodeExpected, htmlCodeResult);
            verify(response)
                    .addHeader(
                            "Content-Security-Policy",
                            String.format("script-src 'nonce-%s'", nonce));
        }  finally {
            Config.setProperty("ContentSecurityPolicy.header", previousValue);
        }

    }

    /**
     * Method to test: {@link ContentSecurityPolicyUtil#calculateContentSecurityPolicy(String, HttpServletResponse)}
     * When: a html has more than one style block
     * Should: calculate a new nonce, set the Content-Secutiry-Policy header and set the nonce in each style blocks
     */
    @Test
    public void calculateContentSecurityPolicyWithMoreThanOneStyleBlocks(){
        final String previousValue = Config.getStringProperty("ContentSecurityPolicy.header");
        Config.setProperty("ContentSecurityPolicy.header", "style-src {style-src nonce}");

        try {
            final HttpServletResponse response = mock(HttpServletResponse.class);
            final String hmlCode = "<style>.h1{background-color: red;}</style>  <h1>This is a example</h1> <style>.h1{background-color: blue;}</style> ";
            final String htmlCodeResult = ContentSecurityPolicyUtil.calculateContentSecurityPolicy(
                    hmlCode,
                    response);

            final String nonce = getNonce(htmlCodeResult, "<style nonce='");
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
            Config.setProperty("ContentSecurityPolicy.header", previousValue);
        }

    }

    /**
     * Method to test: {@link ContentSecurityPolicyUtil#calculateContentSecurityPolicy(String, HttpServletResponse)}
     * When: a html no has any script block
     * Should: calculate a new nonce and set the Content-Secutiry-Policy header
     */
    @Test
    public void calculateContentSecurityPolicyWithNoSciptBlocks(){
        final String previousValue = Config.getStringProperty("ContentSecurityPolicy.header");
        Config.setProperty("ContentSecurityPolicy.header", "script-src {script-src nonce}");

        try {
            final HttpServletResponse response = mock(HttpServletResponse.class);
            final String hmlCode = "<h1>This is a example</h1>";
            final String htmlCodeResult = ContentSecurityPolicyUtil.calculateContentSecurityPolicy(hmlCode,
                    response);

            final String hmlCodeExpected = "<h1>This is a example</h1>";

            assertEquals(hmlCodeExpected, htmlCodeResult);
            verify(response).addHeader(eq("Content-Security-Policy"), any());
        }  finally {
            Config.setProperty("ContentSecurityPolicy.header", previousValue);
        }
    }

    /**
     * Method to test: {@link ContentSecurityPolicyUtil#calculateContentSecurityPolicy(String, HttpServletResponse)}
     * When: a html no has any style block
     * Should: calculate a new nonce and set the Content-Secutiry-Policy header
     */
    @Test
    public void calculateContentSecurityPolicyWithNoStyleBlocks(){
        final String previousValue = Config.getStringProperty("ContentSecurityPolicy.header");
        Config.setProperty("ContentSecurityPolicy.header", "style-src {style-src nonce}");

        try {
            final HttpServletResponse response = mock(HttpServletResponse.class);
            final String hmlCode = "<h1>This is a example</h1>";
            final String htmlCodeResult = ContentSecurityPolicyUtil.calculateContentSecurityPolicy(hmlCode,
                    response);

            final String hmlCodeExpected = "<h1>This is a example</h1>";

            assertEquals(hmlCodeExpected, htmlCodeResult);
            verify(response).addHeader(eq("Content-Security-Policy"), any());
        }  finally {
            Config.setProperty("ContentSecurityPolicy.header", previousValue);
        }
    }

    private String getNonce(final String htmlCodeResult, final String template) {
        final int startNonce = htmlCodeResult.indexOf(template);

        if (startNonce == -1) {
            return null;
        }

        final int startNonceIndex = startNonce + template.length();
        final int endNonceIndex = htmlCodeResult.indexOf("'", startNonceIndex);

        return htmlCodeResult.substring(startNonceIndex, endNonceIndex);
    }
}
