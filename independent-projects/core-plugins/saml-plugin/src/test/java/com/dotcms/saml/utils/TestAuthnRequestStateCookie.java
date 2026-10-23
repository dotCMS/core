package com.dotcms.saml.utils;

import com.dotcms.saml.IdentityProviderConfiguration;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The request-binding cookie: one per IdP configuration, bounded, expiring, single use.
 */
public class TestAuthnRequestStateCookie {

    private static final String CONFIG_ID = "48190c8c-42c4-46af-8d1a-0cd5db894797";

    private final AtomicLong now = new AtomicLong(1_790_950_000_000L);
    private Cookie browserCookie;

    @Before
    public void setUp() {

        this.browserCookie = null;
    }

    @Test
    public void cookieIsHostPrefixedSecureAndScopedToTheIdpConfiguration() {

        final List<String> headers = new ArrayList<>();
        AuthnRequestStateCookie.remember(request(), response(headers), idp(CONFIG_ID), "_request1", this.now::get);

        final String header = headers.get(0);
        Assert.assertTrue(header, header.startsWith("__Host-dotsaml_req_" + CONFIG_ID + "="));
        for (final String attribute : new String[]{"; Path=/", "; Secure", "; HttpOnly", "; SameSite=None"}) {
            Assert.assertTrue(attribute, header.contains(attribute));
        }
        Assert.assertFalse("__Host- cookies must not have a Domain", header.contains("Domain"));
    }

    @Test
    public void requestStartedInThisBrowserIsAcceptedOnce() {

        remember("_request1");

        Assert.assertTrue(consume("_request1"));
        Assert.assertFalse("a request can only be answered once", consume("_request1"));
    }

    @Test
    public void severalTabsCanHaveOutstandingRequests() {

        remember("_tab1");
        remember("_tab2");

        Assert.assertTrue(consume("_tab1"));
        Assert.assertTrue(consume("_tab2"));
    }

    @Test
    public void onlyTheMostRecentRequestsAreKept() {

        for (int i = 1; i <= AuthnRequestStateCookie.MAX_OUTSTANDING_REQUESTS + 2; i++) {
            remember("_request" + i);
        }

        Assert.assertFalse("the oldest requests are dropped", consume("_request1"));
        Assert.assertFalse(consume("_request2"));
        Assert.assertTrue(consume("_request" + (AuthnRequestStateCookie.MAX_OUTSTANDING_REQUESTS + 2)));
        Assert.assertTrue(this.browserCookie.getValue().split("\\.").length < AuthnRequestStateCookie.MAX_OUTSTANDING_REQUESTS);
    }

    @Test
    public void expiredRequestIsRejected() {

        remember("_request1");
        this.now.addAndGet((AuthnRequestStateCookie.getMaxAgeSeconds(idp(CONFIG_ID)) + 1) * 1000L);

        Assert.assertFalse(consume("_request1"));
    }

    @Test
    public void requestForAnotherIdpConfigurationIsRejected() {

        remember("_request1");

        Assert.assertFalse(AuthnRequestStateCookie.consume(request(), response(new ArrayList<>()),
                idp("8a7d5e23-da1e-420a-b4f0-471e7da8ea2d"), "_request1", this.now::get));
    }

    @Test
    public void malformedCookieValuesAreIgnored() {

        this.browserCookie = new Cookie(AuthnRequestStateCookie.COOKIE_PREFIX + CONFIG_ID,
                "_bad id:1.<script>:2._request1:notanumber");

        Assert.assertFalse(consume("_request1"));
    }

    // ---------------------------------------------------------------------------------------------------------

    private void remember(final String requestId) {

        AuthnRequestStateCookie.remember(request(), response(new ArrayList<>()), idp(CONFIG_ID), requestId, this.now::get);
    }

    private boolean consume(final String requestId) {

        return AuthnRequestStateCookie.consume(request(), response(new ArrayList<>()), idp(CONFIG_ID), requestId, this.now::get);
    }

    /** The browser: sends back the last cookie the server set. */
    private HttpServletRequest request() {

        return (HttpServletRequest) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{HttpServletRequest.class},
                (proxy, method, args) -> "getCookies".equals(method.getName())
                        ? (null == this.browserCookie ? null : new Cookie[]{this.browserCookie}) : null);
    }

    private HttpServletResponse response(final List<String> headers) {

        return (HttpServletResponse) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{HttpServletResponse.class},
                (proxy, method, args) -> {
                    if ("addHeader".equals(method.getName()) && "Set-Cookie".equals(args[0])) {
                        final String header = (String) args[1];
                        headers.add(header);
                        final String nameValue = header.substring(0, header.indexOf(';'));
                        final String name  = nameValue.substring(0, nameValue.indexOf('='));
                        final String value = nameValue.substring(nameValue.indexOf('=') + 1);
                        this.browserCookie = header.contains("Max-Age=0") ? null : new Cookie(name, value);
                    }
                    return null;
                });
    }

    private static IdentityProviderConfiguration idp(final String id) {

        return (IdentityProviderConfiguration) Proxy.newProxyInstance(TestAuthnRequestStateCookie.class.getClassLoader(),
                new Class<?>[]{IdentityProviderConfiguration.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getId":                    return id;
                        case "getIdpName":               return "Test IdP";
                        case "containsOptionalProperty": return false;
                        default:                         return null;
                    }
                });
    }
}
