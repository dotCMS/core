package com.dotcms.saml.service.impl;

import com.dotcms.saml.IdentityProviderConfiguration;
import com.dotcms.saml.SamlAuthenticationService;
import com.dotcms.saml.service.handler.AuthenticationHandler;
import com.dotcms.saml.service.handler.AuthenticationResolverHandlerFactory;
import com.dotcms.saml.utils.EndpointHostRedirect;
import org.junit.Assert;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Proxy;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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

    /** The alias hop must preserve special characters without creating extra query parameters. */
    @Test
    public void aliasRedirectCarriesTheComputedDestinationAsOneEncodedParameter() {

        final SamlAuthenticationService service = new SamlServiceBuilderImpl().buildAuthenticationService(
                new MockIdentityProviderConfigurationFactory(), null, new MockMessageObserver(),
                new MockSamlConfigurationService());
        final List<String> redirects = new ArrayList<>();
        final List<String> cookies = new ArrayList<>();
        final String destination = "/members/page?filter=café&next=/report#section";

        service.authentication(request("alias.example.org"), response(redirects, cookies), idp(), destination);

        Assert.assertEquals(Collections.singletonList("https://dotcms.example.com/dotsaml/login/" + CONFIG_ID
                + "?" + EndpointHostRedirect.MARKER_PARAMETER + "=1&RelayState="
                + URLEncoder.encode(destination, StandardCharsets.UTF_8)), redirects);
        Assert.assertTrue("no request cookie on the alias", cookies.isEmpty());
    }

    /** A new endpoint-host session must not replace the visitor's destination with the configured default. */
    @Test
    public void endpointRestartUsesTheForwardedDestinationInsteadOfRecomputingIt() {

        final List<String> relayStates = new ArrayList<>();
        final SamlAuthenticationService service = capturingService(relayStates);
        service.authentication(request("dotcms.example.com", Map.of(
                        EndpointHostRedirect.MARKER_PARAMETER, "1", "RelayState", "/members/page?x=1&y=2")),
                response(new ArrayList<>(), new ArrayList<>()), idp(), "/configured-default");

        Assert.assertEquals(Collections.singletonList("/members/page?x=1&y=2"), relayStates);
    }

    /** Ordinary login requests still use the server-computed RelayState. */
    @Test
    public void relayStateParameterWithoutHostRedirectDoesNotOverrideTheComputedValue() {

        final List<String> relayStates = new ArrayList<>();
        capturingService(relayStates).authentication(
                request("dotcms.example.com", Map.of("RelayState", "/injected")),
                response(new ArrayList<>(), new ArrayList<>()), idp(), "/configured-default");

        Assert.assertEquals(Collections.singletonList("/configured-default"), relayStates);
    }

    /** Redirects that carry no destination retain the configured strategy or template result. */
    @Test
    public void endpointRestartWithoutForwardedDestinationKeepsTheComputedValue() {

        final List<String> relayStates = new ArrayList<>();
        capturingService(relayStates).authentication(
                request("dotcms.example.com", Map.of(EndpointHostRedirect.MARKER_PARAMETER, "1")),
                response(new ArrayList<>(), new ArrayList<>()), idp(), "/configured-default");

        Assert.assertEquals(Collections.singletonList("/configured-default"), relayStates);
    }

    /** Captures the value handed to the binding handler without involving an external IdP. */
    private static SamlAuthenticationService capturingService(final List<String> relayStates) {

        final AuthenticationResolverHandlerFactory handlers = new AuthenticationResolverHandlerFactory(null, null, null, null) {
            /** Returns a recording handler instead of producing an AuthnRequest. */
            @Override
            public AuthenticationHandler getAuthenticationHandlerForSite(final IdentityProviderConfiguration config) {
                return (request, response, idp, relayState) -> relayStates.add(relayState);
            }
        };
        return new OpenSamlAuthenticationServiceImpl(null, handlers, null, null, null,
                new MockMessageObserver(), null, null);
    }

    /** Builds a login request without query parameters. */
    private static HttpServletRequest request(final String serverName) {
        return request(serverName, Collections.emptyMap());
    }

    /** Builds a request on either host with servlet-decoded query parameter values. */
    private static HttpServletRequest request(final String serverName, final Map<String, String> parameters) {

        return (HttpServletRequest) Proxy.newProxyInstance(TestLoginStartHost.class.getClassLoader(),
                new Class<?>[]{HttpServletRequest.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getServerName": return serverName;
                        case "getRequestURI": return "/dotAdmin/";
                        case "getParameter":  return parameters.get((String) args[0]);
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
