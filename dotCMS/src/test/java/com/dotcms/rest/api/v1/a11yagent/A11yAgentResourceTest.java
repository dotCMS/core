package com.dotcms.rest.api.v1.a11yagent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.auth.providers.jwt.factories.ApiTokenAPI;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.WebResource;
import com.dotcms.security.apps.AppSecrets;
import com.dotcms.security.apps.AppsAPI;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.HostWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.liferay.portal.model.User;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import java.net.http.HttpClient;
import java.util.Optional;

/**
 * Unit tests for {@link A11yAgentResource}.
 *
 * <p>The resource is a proxy that authenticates the caller, reads {@code apiUrl} and
 * {@code apiAuthToken} from App secrets, mints a short-lived JWT, resolves the page, and
 * forwards to an external agent service. These tests cover the config-resolution and
 * early-exit branches — the paths that decide whether anything is forwarded at all — without
 * touching the network.</p>
 *
 * <p>Mirrors {@code PageScannerResourceTest}, the sibling proxy this resource was modelled on,
 * and uses the package-private constructor seam the resource exposes for exactly this.</p>
 */
public class A11yAgentResourceTest {

    private WebResource webResource;
    private HttpClient httpClient;
    private A11yAgentResource resource;

    private HttpServletRequest request;
    private HttpServletResponse response;

    private AppsAPI appsAPI;
    private ApiTokenAPI apiTokenAPI;

    private MockedStatic<APILocator> mockedApiLocator;
    private MockedStatic<WebAPILocator> mockedWebApiLocator;

    // -----------------------------------------------------------------------
    // Setup / teardown
    // -----------------------------------------------------------------------

    @BeforeEach
    void setUp() throws Exception {
        webResource = mock(WebResource.class);
        httpClient = mock(HttpClient.class);
        resource = new A11yAgentResource(webResource, httpClient);

        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        when(request.getScheme()).thenReturn("https");
        when(request.getServerName()).thenReturn("demo.dotcms.com");
        when(request.getServerPort()).thenReturn(443);
        when(request.getRemoteAddr()).thenReturn("10.0.0.9");

        final User user = mock(User.class);
        when(user.getUserId()).thenReturn("test-user");

        final InitDataObject initData = mock(InitDataObject.class);
        when(initData.getUser()).thenReturn(user);
        when(webResource.init(any(WebResource.InitBuilder.class))).thenReturn(initData);

        appsAPI = mock(AppsAPI.class);
        apiTokenAPI = mock(ApiTokenAPI.class);

        mockedApiLocator = mockStatic(APILocator.class);
        final Host systemHost = mock(Host.class);
        final User systemUser = mock(User.class);
        mockedApiLocator.when(APILocator::systemHost).thenReturn(systemHost);
        mockedApiLocator.when(APILocator::systemUser).thenReturn(systemUser);
        mockedApiLocator.when(APILocator::getAppsAPI).thenReturn(appsAPI);
        mockedApiLocator.when(APILocator::getApiTokenAPI).thenReturn(apiTokenAPI);

        mockedWebApiLocator = mockStatic(WebAPILocator.class);
        final HostWebAPI hostWebAPI = mock(HostWebAPI.class);
        when(hostWebAPI.getCurrentHost(request)).thenReturn(systemHost);
        mockedWebApiLocator.when(WebAPILocator::getHostWebAPI).thenReturn(hostWebAPI);

        // Default: no App configured. Tests that need one override this.
        when(appsAPI.getSecrets(anyString(), anyBoolean(), any(Host.class), any(User.class)))
                .thenReturn(Optional.empty());
    }

    @AfterEach
    void tearDown() {
        mockedApiLocator.close();
        mockedWebApiLocator.close();
    }

    /** Configure the App with the given secrets, omitting any passed as null. */
    private void configureApp(final String apiUrl, final String apiAuthToken)
            throws Exception {
        final AppSecrets.Builder builder = AppSecrets.builder().withKey(A11yAgentResource.APP_KEY);
        if (apiUrl != null) {
            builder.withSecret("apiUrl", apiUrl);
        }
        if (apiAuthToken != null) {
            builder.withSecret("apiAuthToken", apiAuthToken);
        }
        when(appsAPI.getSecrets(anyString(), anyBoolean(), any(Host.class), any(User.class)))
                .thenReturn(Optional.of(builder.build()));
    }

    private static A11yAgentFixForm fixForm(final String identifier) {
        final A11yAgentFixForm.Builder builder = A11yAgentFixForm.builder();
        if (identifier != null) {
            builder.identifier(identifier);
        }
        return builder.build();
    }

    // -----------------------------------------------------------------------
    // App configuration
    // -----------------------------------------------------------------------

    /**
     * Method to test: {@link A11yAgentResource#fix}
     * Given scenario: The Page Scanner App is not configured in the Apps portlet.
     * Expected result: 503 SERVICE_UNAVAILABLE, and nothing is forwarded upstream.
     */
    @Test
    void fix_appNotConfigured_returns503() {
        final Response resp = resource.fix(request, response, fixForm("page-id"));

        assertEquals(Response.Status.SERVICE_UNAVAILABLE.getStatusCode(), resp.getStatus());
    }

    /**
     * Method to test: {@link A11yAgentResource#fix}
     * Given scenario: The App exists but {@code apiAuthToken} is absent.
     * Expected result: 503 — a half-configured App is treated as not configured, so the
     *   proxy never forwards an unauthenticated request to the agent service.
     */
    @Test
    void fix_missingApiAuthToken_returns503() throws Exception {
        configureApp("https://agent.example.com", null);

        final Response resp = resource.fix(request, response, fixForm("page-id"));

        assertEquals(Response.Status.SERVICE_UNAVAILABLE.getStatusCode(), resp.getStatus());
    }

    /**
     * Method to test: {@link A11yAgentResource#fix}
     * Given scenario: The App exists but {@code apiUrl} is absent.
     * Expected result: 503 for the same reason as a missing token.
     */
    @Test
    void fix_missingApiUrl_returns503() throws Exception {
        configureApp(null, "secret-token");

        final Response resp = resource.fix(request, response, fixForm("page-id"));

        assertEquals(Response.Status.SERVICE_UNAVAILABLE.getStatusCode(), resp.getStatus());
    }

    /**
     * Method to test: {@link A11yAgentResource#stop}
     * Given scenario: The App is not configured.
     * Expected result: 503 — /stop resolves the same config as /fix, so it fails the same way.
     */
    @Test
    void stop_appNotConfigured_returns503() {
        final Response resp = resource.stop(request, response,
                A11yAgentStopForm.builder().runId("r_1").build());

        assertEquals(Response.Status.SERVICE_UNAVAILABLE.getStatusCode(), resp.getStatus());
    }

    /**
     * Method to test: {@link A11yAgentResource#activeRun}
     * Given scenario: The App is not configured.
     * Expected result: 503, and no token is minted for a call that cannot go anywhere.
     */
    @Test
    void activeRun_appNotConfigured_returns503() {
        final Response resp = resource.activeRun(request, response);

        assertEquals(Response.Status.SERVICE_UNAVAILABLE.getStatusCode(), resp.getStatus());
        verify(apiTokenAPI, never()).persistApiToken(anyString(), any(), anyString(), anyString(),
                anyString());
    }

    // -----------------------------------------------------------------------
    // Request validation
    // -----------------------------------------------------------------------

    /**
     * Method to test: {@link A11yAgentResource#fix}
     * Given scenario: A configured App, but the body carries no identifier.
     * Expected result: 400 MISSING_IDENTIFIER. The form declares identifier as nullable
     *   precisely so this surfaces here rather than as a Jackson deserialization failure.
     */
    @Test
    void fix_missingIdentifier_returns400() throws Exception {
        configureApp("https://agent.example.com", "secret-token");

        final Response resp = resource.fix(request, response, fixForm(null));

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), resp.getStatus());
    }

    /**
     * Method to test: {@link A11yAgentResource#fix}
     * Given scenario: A configured App and a null body.
     * Expected result: 400 rather than an NPE.
     */
    @Test
    void fix_nullBody_returns400() throws Exception {
        configureApp("https://agent.example.com", "secret-token");

        final Response resp = resource.fix(request, response, null);

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), resp.getStatus());
    }

    /**
     * Method to test: {@link A11yAgentResource#stop}
     * Given scenario: No runId in the body.
     * Expected result: 400 MISSING_RUN_ID, checked BEFORE any config or token work — a
     *   malformed request must not mint a token or touch App secrets.
     */
    @Test
    void stop_missingRunId_returns400WithoutMintingAToken() throws Exception {
        configureApp("https://agent.example.com", "secret-token");

        final Response resp = resource.stop(request, response,
                A11yAgentStopForm.builder().build());

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), resp.getStatus());
        verify(apiTokenAPI, never()).persistApiToken(anyString(), any(), anyString(), anyString(),
                anyString());
    }

    /**
     * Method to test: {@link A11yAgentResource#stop}
     * Given scenario: A null body.
     * Expected result: 400 rather than an NPE.
     */
    @Test
    void stop_nullBody_returns400() throws Exception {
        configureApp("https://agent.example.com", "secret-token");

        final Response resp = resource.stop(request, response, null);

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), resp.getStatus());
    }

    // -----------------------------------------------------------------------
    // SSE relay — a configuration failure cannot use an HTTP status
    // -----------------------------------------------------------------------

    /**
     * Method to test: {@link A11yAgentResource#fixStream}
     * Given scenario: The App is not configured.
     * Expected result: an EventOutput is still returned and closed, carrying an SSE error
     *   frame. The stream endpoint cannot report the failure as an HTTP status the way /fix
     *   does, because the response has already begun.
     */
    @Test
    void fixStream_appNotConfigured_returnsClosedErrorStream() {
        final var output = resource.fixStream(request, response, fixForm("page-id"));

        assertNotNull(output);
        assertTrue(output.isClosed(),
                "the error frame is terminal, so the stream must be closed behind it");
    }

    /**
     * Method to test: {@link A11yAgentResource#fixStream}
     * Given scenario: A configured App and a valid identifier that resolves to no page.
     * Expected result: still an EventOutput rather than a thrown exception — the relay
     *   reports upstream problems in-band.
     */
    @Test
    void fixStream_pageNotFound_returnsClosedErrorStream() throws Exception {
        configureApp("https://agent.example.com", "secret-token");

        final var output = resource.fixStream(request, response, fixForm("no-such-page"));

        assertNotNull(output);
        assertTrue(output.isClosed());
    }

    // -----------------------------------------------------------------------
    // Secrets must not leak
    // -----------------------------------------------------------------------

    /**
     * Method to test: {@link A11yAgentResource#fix}
     * Given scenario: A configured App whose auth token has a recognisable value, and a
     *   request that fails before forwarding.
     * Expected result: the token never appears in the response body. The proxy is the auth
     *   boundary, so a relayed error is the most likely place for a secret to escape.
     */
    @Test
    void fix_errorResponse_neverCarriesTheAuthToken() throws Exception {
        configureApp("https://agent.example.com", "super-secret-token-value");

        final Response resp = resource.fix(request, response, fixForm(null));

        final String body = String.valueOf(resp.getEntity());
        assertFalse(body.contains("super-secret-token-value"),
                "the App auth token must never reach the client");
    }
}
