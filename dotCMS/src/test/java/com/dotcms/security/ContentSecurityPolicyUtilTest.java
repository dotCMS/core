package com.dotcms.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

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
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final String hmlCode = "<script>console.log('This is a test')</script> <h1>This is a example</h1>";
        final String htmlCodeResult = ContentSecurityPolicyUtil.calculateContentSecurityPolicy(hmlCode,
                response);

        final String nonce = getNonce(htmlCodeResult);
        assertNotNull(nonce);

        final String hmlCodeExpected = String.format(
                "<script nonce='%s'>console.log('This is a test')</script> <h1>This is a example</h1>"
        , nonce);

        assertEquals(hmlCodeExpected, htmlCodeResult);
        verify(response)
                .addHeader(
                    "Content-Security-Policy", String.format("script-src 'nonce-%s'", nonce));
    }

    /**
     * Method to test: {@link ContentSecurityPolicyUtil#calculateContentSecurityPolicy(String, HttpServletResponse)}
     * When: a html has more than one script block
     * Should: calculate a new nonce, set the Content-Secutiry-Policy header and set the nonce in each script blocks
     */
    @Test
    public void calculateContentSecurityPolicyWithMoreThanOneSciptBlocks(){
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final String hmlCode = "<script>console.log('This is a test')</script> <h1>This is a example</h1> <script>console.log('This is a test 2')</script>";
        final String htmlCodeResult = ContentSecurityPolicyUtil.calculateContentSecurityPolicy(hmlCode,
                response);

        final String nonce = getNonce(htmlCodeResult);
        assertNotNull(nonce);

        final String hmlCodeExpected = String.format(
                "<script nonce='%s'>console.log('This is a test')</script> <h1>This is a example</h1> <script nonce='%s'>console.log('This is a test 2')</script>"
                , nonce, nonce);

        assertEquals(hmlCodeExpected, htmlCodeResult);
        verify(response)
                .addHeader(
                        "Content-Security-Policy", String.format("script-src 'nonce-%s'", nonce));
    }

    /**
     * Method to test: {@link ContentSecurityPolicyUtil#calculateContentSecurityPolicy(String, HttpServletResponse)}
     * When: a html no has any script block
     * Should: calculate a new nonce and set the Content-Secutiry-Policy header
     */
    @Test
    public void calculateContentSecurityPolicyWithNoSciptBlocks(){
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final String hmlCode = "<h1>This is a example</h1>";
        final String htmlCodeResult = ContentSecurityPolicyUtil.calculateContentSecurityPolicy(hmlCode,
                response);

        final String hmlCodeExpected = "<h1>This is a example</h1>";

        assertEquals(hmlCodeExpected, htmlCodeResult);
        verify(response).addHeader(eq("Content-Security-Policy"), any());
    }

    private String getNonce(final String htmlCodeResult) {
        final String nOnceString = "<scrip nonce='";
        final int startNonce = htmlCodeResult.indexOf(nOnceString);

        if (startNonce == -1) {
            return null;
        }

        final int startNonceIndex = startNonce + nOnceString.length();
        final int endNonceIndex = htmlCodeResult.indexOf("'", startNonceIndex);

        return htmlCodeResult.substring(startNonceIndex, endNonceIndex);
    }
}
