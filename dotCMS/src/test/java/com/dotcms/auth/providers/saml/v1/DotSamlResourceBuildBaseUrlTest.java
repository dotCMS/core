package com.dotcms.auth.providers.saml.v1;

import com.dotcms.UnitTestBase;
import com.dotcms.rest.WebResource;
import com.dotcms.saml.IdentityProviderConfigurationFactory;
import com.dotcms.saml.SamlAuthenticationService;
import com.dotcms.saml.SamlConfigurationService;
import com.dotcms.saml.SamlName;
import com.dotmarketing.util.Config;
import org.junit.After;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DotSamlResource#buildBaseUrlFromRequest(HttpServletRequest)}.
 *
 * Verifies that the logout base URL prefers the configured {@code sPEndpointHostname}
 * over the raw origin hostname from the servlet request, preventing the origin server
 * address from leaking through the SAML logout redirect.
 */
public class DotSamlResourceBuildBaseUrlTest extends UnitTestBase {

    private static final String CONFIGURED_HOST = "https://public.cdn.example.com";
    private static final String REQUEST_SCHEME  = "https";
    private static final String REQUEST_HOST    = "origin-server.internal.example.com";
    private static final int    REQUEST_PORT    = 443;

    @After
    public void cleanup() {
        Config.setProperty(SamlName.DOT_SAML_SERVICE_PROVIDER_HOST_NAME.getPropertyName(), null);
    }

    private DotSamlResource buildResource() {
        return new DotSamlResource(
                mock(SamlConfigurationService.class),
                mock(SAMLHelper.class),
                mock(SamlAuthenticationService.class),
                mock(IdentityProviderConfigurationFactory.class),
                mock(WebResource.class)
        );
    }

    private HttpServletRequest mockRequest() {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getScheme()).thenReturn(REQUEST_SCHEME);
        when(request.getServerName()).thenReturn(REQUEST_HOST);
        when(request.getServerPort()).thenReturn(REQUEST_PORT);
        return request;
    }

    /**
     * Method to test: {@link DotSamlResource#buildBaseUrlFromRequest(HttpServletRequest)}
     * Given Scenario: {@code sPEndpointHostname} is configured with a CDN/public hostname
     * ExpectedResult: Returned URL uses the configured hostname, not the request server name,
     *                 preventing the origin hostname from being exposed in the SAML redirect
     */
    @Test
    public void testBuildBaseUrl_usesConfiguredHostname_whenSet() {
        Config.setProperty(SamlName.DOT_SAML_SERVICE_PROVIDER_HOST_NAME.getPropertyName(), CONFIGURED_HOST);

        final String result = buildResource().buildBaseUrlFromRequest(mockRequest());

        assertEquals(CONFIGURED_HOST + "/dotAdmin/show-logout", result);
    }

    /**
     * Method to test: {@link DotSamlResource#buildBaseUrlFromRequest(HttpServletRequest)}
     * Given Scenario: {@code sPEndpointHostname} is not configured
     * ExpectedResult: Falls back to constructing the URL from the request scheme, host and port
     */
    @Test
    public void testBuildBaseUrl_fallsBackToRequestHostname_whenNotConfigured() {
        Config.setProperty(SamlName.DOT_SAML_SERVICE_PROVIDER_HOST_NAME.getPropertyName(), null);

        final String result = buildResource().buildBaseUrlFromRequest(mockRequest());

        final String expected = REQUEST_SCHEME + "://" + REQUEST_HOST + ":" + REQUEST_PORT + "/dotAdmin/show-logout";
        assertEquals(expected, result);
    }

    /**
     * Method to test: {@link DotSamlResource#buildBaseUrlFromRequest(HttpServletRequest)}
     * Given Scenario: {@code sPEndpointHostname} is set to an empty string
     * ExpectedResult: Treated as not-set; falls back to the request hostname
     */
    @Test
    public void testBuildBaseUrl_fallsBackToRequestHostname_whenConfiguredHostIsEmpty() {
        Config.setProperty(SamlName.DOT_SAML_SERVICE_PROVIDER_HOST_NAME.getPropertyName(), "");

        final String result = buildResource().buildBaseUrlFromRequest(mockRequest());

        assertTrue("Should fall back to request hostname when config is empty",
                result.contains(REQUEST_HOST));
    }

    /**
     * Method to test: {@link DotSamlResource#buildBaseUrlFromRequest(HttpServletRequest)}
     * Given Scenario: Configured hostname without trailing slash, result must end in the logout path
     * ExpectedResult: URL always ends with {@code /dotAdmin/show-logout}
     */
    @Test
    public void testBuildBaseUrl_alwaysEndsWithLogoutPath() {
        Config.setProperty(SamlName.DOT_SAML_SERVICE_PROVIDER_HOST_NAME.getPropertyName(), CONFIGURED_HOST);

        final String result = buildResource().buildBaseUrlFromRequest(mockRequest());

        assertTrue("URL must end with /dotAdmin/show-logout", result.endsWith("/dotAdmin/show-logout"));
    }

    /**
     * Method to test: {@link DotSamlResource#buildBaseUrlFromRequest(HttpServletRequest)}
     * Given Scenario: Configured hostname is set — result must not contain the origin request hostname
     * ExpectedResult: Origin hostname is absent from the returned URL, preventing origin leakage
     */
    @Test
    public void testBuildBaseUrl_doesNotLeakOriginHostname_whenConfigured() {
        Config.setProperty(SamlName.DOT_SAML_SERVICE_PROVIDER_HOST_NAME.getPropertyName(), CONFIGURED_HOST);

        final String result = buildResource().buildBaseUrlFromRequest(mockRequest());

        assertTrue("Origin hostname must not appear in the URL when sPEndpointHostname is configured",
                !result.contains(REQUEST_HOST));
    }
}
