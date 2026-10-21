package com.dotcms.saml.service.impl;

import com.dotcms.saml.IdentityProviderConfiguration;
import com.dotcms.saml.SamlAuthenticationService;
import com.dotcms.saml.utils.EndpointHostRedirect;
import org.junit.Assert;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A login started on a site alias is moved to the Service Provider Endpoint Hostname before any AuthnRequest
 * (and request-binding cookie) is issued, so the cookie is set on the host the IdP posts back to.
 */
public class TestLoginStartHost {

    private static final String CONFIG_ID = "48190c8c-42c4-46af-8d1a-0cd5db894797";

    @Test
    public void loginStartedOnAnAliasIsRedirectedToTheEndpointHostBeforeTheAuthnRequest() {

        final SamlAuthenticationService authenticationService = new SamlServiceBuilderImpl().buildAuthenticationService(
                new MockIdentityProviderConfigurationFactory(), null, new MockMessageObserver(),
                new MockSamlConfigurationService());
        final List<String> redirects = new ArrayList<>();
        final List<String> setCookies = new ArrayList<>();

        authenticationService.authentication(request("alias.example.org"), response(redirects, setCookies), idp(), null);

        Assert.assertEquals(Collections.singletonList("https://dotcms.example.com/dotsaml/login/" + CONFIG_ID
                + "?" + EndpointHostRedirect.MARKER_PARAMETER + "=1"), redirects);
        Assert.assertTrue("no AuthnRequest or request cookie on the alias host", setCookies.isEmpty());
    }

    private static HttpServletRequest request(final String serverName) {

        return (HttpServletRequest) Proxy.newProxyInstance(TestLoginStartHost.class.getClassLoader(),
                new Class<?>[]{HttpServletRequest.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getServerName": return serverName;
                        case "getRequestURI": return "/dotAdmin/";
                        default:              return null;
                    }
                });
    }

    private static HttpServletResponse response(final List<String> redirects, final List<String> setCookies) {

        return (HttpServletResponse) Proxy.newProxyInstance(TestLoginStartHost.class.getClassLoader(),
                new Class<?>[]{HttpServletResponse.class},
                (proxy, method, args) -> {
                    if ("sendRedirect".equals(method.getName())) {
                        redirects.add((String) args[0]);
                    } else if ("addHeader".equals(method.getName()) && "Set-Cookie".equals(args[0])) {
                        setCookies.add((String) args[1]);
                    }
                    return null;
                });
    }

    private static IdentityProviderConfiguration idp() {

        return (IdentityProviderConfiguration) Proxy.newProxyInstance(TestLoginStartHost.class.getClassLoader(),
                new Class<?>[]{IdentityProviderConfiguration.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getId":                    return CONFIG_ID;
                        case "getIdpName":               return "Test IdP";
                        case "getSpEndpointHostname":    return "dotcms.example.com";
                        case "isEnabled":                return true;
                        case "containsOptionalProperty": return false;
                        default:                         return null;
                    }
                });
    }
}
