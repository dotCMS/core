package com.dotcms.saml.utils;

import org.junit.Assert;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Logins that start on another host are moved to the host the IdP posts back to.
 */
public class TestEndpointHostRedirect {

    private static final String ACS_URL = "https://dotcms.example.com/dotsaml/login/48190c8c-42c4-46af-8d1a-0cd5db894797";

    @Test
    public void loginOnTheEndpointHostIsNotRedirected() throws Exception {

        final List<String> redirects = new ArrayList<>();

        Assert.assertFalse(EndpointHostRedirect.redirectIfNeeded(request("dotcms.example.com"), response(redirects), ACS_URL));
        Assert.assertTrue(redirects.isEmpty());
    }

    @Test
    public void hostIsComparedWithoutCaseOrPort() throws Exception {

        final List<String> redirects = new ArrayList<>();

        Assert.assertFalse(EndpointHostRedirect.redirectIfNeeded(request("DotCMS.Example.com"), response(redirects),
                "https://dotcms.example.com:8443/dotsaml/login/1"));
        Assert.assertTrue(redirects.isEmpty());
    }

    @Test
    public void loginOnAnAliasIsSentToTheAssertionConsumerUrl() throws Exception {

        final List<String> redirects = new ArrayList<>();

        Assert.assertTrue(EndpointHostRedirect.redirectIfNeeded(request("alias.example.org"), response(redirects), ACS_URL));
        Assert.assertEquals(Collections.singletonList(ACS_URL + "?" + EndpointHostRedirect.MARKER_PARAMETER + "=1"), redirects);
    }

    @Test
    public void redirectedRequestIsNeverRedirectedAgain() throws Exception {

        // e.g. a proxy that rewrites the Host header: the marker stops a loop
        final List<String> redirects = new ArrayList<>();

        Assert.assertFalse(EndpointHostRedirect.redirectIfNeeded(
                request("internal-node-1", Map.of(EndpointHostRedirect.MARKER_PARAMETER, "1")), response(redirects), ACS_URL));
        Assert.assertTrue(redirects.isEmpty());
    }

    @Test
    public void relativeOrMissingAssertionConsumerUrlIsNotRedirected() throws Exception {

        final List<String> redirects = new ArrayList<>();

        Assert.assertFalse(EndpointHostRedirect.redirectIfNeeded(request("alias.example.org"), response(redirects),
                "/dotsaml/login/1"));
        Assert.assertFalse(EndpointHostRedirect.redirectIfNeeded(request("alias.example.org"), response(redirects), null));
        Assert.assertTrue(redirects.isEmpty());
    }

    // ---------------------------------------------------------------------------------------------------------

    static HttpServletRequest request(final String serverName) {

        return request(serverName, Collections.emptyMap());
    }

    static HttpServletRequest request(final String serverName, final Map<String, String> parameters) {

        return (HttpServletRequest) Proxy.newProxyInstance(TestEndpointHostRedirect.class.getClassLoader(),
                new Class<?>[]{HttpServletRequest.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getServerName": return serverName;
                        case "getParameter":  return parameters.get((String) args[0]);
                        default:              return null;
                    }
                });
    }

    static HttpServletResponse response(final List<String> redirects) {

        return (HttpServletResponse) Proxy.newProxyInstance(TestEndpointHostRedirect.class.getClassLoader(),
                new Class<?>[]{HttpServletResponse.class},
                (proxy, method, args) -> {
                    if ("sendRedirect".equals(method.getName())) {
                        redirects.add((String) args[0]);
                    }
                    return null;
                });
    }
}
