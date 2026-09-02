package com.dotcms.rest.api.v1.pagescanner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.dotcms.auth.providers.jwt.beans.ApiToken;
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
import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.util.Optional;

/**
 * Unit tests for {@link PageScannerResource}.
 *
 * <p>The resource is a thin proxy that:</p>
 * <ol>
 *   <li>Authenticates the caller via {@link WebResource}</li>
 *   <li>Reads {@code apiUrl} and {@code apiAuthToken} from App secrets</li>
 *   <li>Generates a short-lived JWT</li>
 *   <li>Forwards the payload to an external HTTP service</li>
 * </ol>
 *
 * <p>The tests cover each early-exit branch and the upstream-response mapping
 * without hitting any real network.</p>
 */
public class PageScannerResourceTest {

    private WebResource webResource;
    private HttpClient httpClient;
    private PageScannerResource resource;

    private HttpServletRequest request;
    private HttpServletResponse response;

    private InitDataObject initData;
    private User user;
    private AppsAPI appsAPI;
    private ApiTokenAPI apiTokenAPI;

    private MockedStatic<APILocator> mockedApiLocator;
    private MockedStatic<WebAPILocator> mockedWebApiLocator;

    // -----------------------------------------------------------------------
    // Setup / teardown
    // -----------------------------------------------------------------------

    @BeforeEach
    void setUp() throws Exception {
        webResource  = mock(WebResource.class);
        httpClient   = mock(HttpClient.class);
        resource     = new PageScannerResource(webResource, httpClient);

        request  = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);

        user     = mock(User.class);
        when(user.getUserId()).thenReturn("test-user");

        initData = mock(InitDataObject.class);
        when(initData.getUser()).thenReturn(user);
        when(webResource.init(any(WebResource.InitBuilder.class))).thenReturn(initData);

        appsAPI      = mock(AppsAPI.class);
        apiTokenAPI  = mock(ApiTokenAPI.class);

        // Static stubs for APILocator
        mockedApiLocator = mockStatic(APILocator.class);
        final Host systemHost   = mock(Host.class);
        final User systemUser   = mock(User.class);
        mockedApiLocator.when(APILocator::systemHost).thenReturn(systemHost);
        mockedApiLocator.when(APILocator::systemUser).thenReturn(systemUser);
        mockedApiLocator.when(APILocator::getAppsAPI).thenReturn(appsAPI);
        mockedApiLocator.when(APILocator::getApiTokenAPI).thenReturn(apiTokenAPI);

        // Static stubs for WebAPILocator
        mockedWebApiLocator = mockStatic(WebAPILocator.class);
        final HostWebAPI hostWebAPI = mock(HostWebAPI.class);
        when(hostWebAPI.getCurrentHost(request)).thenReturn(systemHost);
        mockedWebApiLocator.when(WebAPILocator::getHostWebAPI).thenReturn(hostWebAPI);

        // Default: appsAPI returns empty so most tests that need a configured
        // app will override this in their own body.
        when(appsAPI.getSecrets(anyString(), anyBoolean(), any(Host.class), any(User.class)))
                .thenReturn(Optional.empty());
    }

    @AfterEach
    void tearDown() {
        mockedApiLocator.close();
        mockedWebApiLocator.close();
    }

    // -----------------------------------------------------------------------
    // 503 SERVICE_UNAVAILABLE — App not configured at all
    // -----------------------------------------------------------------------

    /**
     * Method to test: {@link PageScannerResource#a11yCheck}
     * Given scenario: The Page Scanner app is not configured in the Apps portlet
     *   (appsAPI returns an empty Optional).
     * Expected result: 503 SERVICE_UNAVAILABLE with error code PAGE_SCANNER_NOT_CONFIGURED.
     */
    @Test
    void a11yCheck_appNotConfigured_returns503() {
        // appsAPI already returns Optional.empty() from setUp — no override needed.

        final Response resp = resource.a11yCheck(request, response,
                new PageScanCheckForm.Builder().url("https://example.com").build());

        assertEquals(Response.Status.SERVICE_UNAVAILABLE.getStatusCode(), resp.getStatus());
    }

    /**
     * Method to test: {@link PageScannerResource#geoCheck}
     * Given scenario: The Page Scanner app is not configured in the Apps portlet.
     * Expected result: 503 SERVICE_UNAVAILABLE — same behaviour for geo endpoint.
     */
    @Test
    void geoCheck_appNotConfigured_returns503() {
        final Response resp = resource.geoCheck(request, response,
                new PageScanCheckForm.Builder().url("https://example.com").build());

        assertEquals(Response.Status.SERVICE_UNAVAILABLE.getStatusCode(), resp.getStatus());
    }

    // -----------------------------------------------------------------------
    // 503 SERVICE_UNAVAILABLE — App configured but required keys missing
    // -----------------------------------------------------------------------

    /**
     * Method to test: {@link PageScannerResource#a11yCheck}
     * Given scenario: App secrets exist but {@code apiAuthToken} is absent
     *   (not present in the secrets map, so Try falls back to null).
     * Expected result: 503 SERVICE_UNAVAILABLE because apiAuthToken is not set.
     */
    @Test
    void a11yCheck_missingApiAuthToken_returns503() throws Exception {
        final AppSecrets secrets = AppSecrets.builder()
                .withKey(PageScannerResource.APP_KEY)
                .withSecret("apiUrl", "https://scanner.example.com")
                // apiAuthToken deliberately omitted
                .build();

        when(appsAPI.getSecrets(anyString(), anyBoolean(), any(Host.class), any(User.class)))
                .thenReturn(Optional.of(secrets));

        final Response resp = resource.a11yCheck(request, response,
                new PageScanCheckForm.Builder().url("https://example.com").build());

        assertEquals(Response.Status.SERVICE_UNAVAILABLE.getStatusCode(), resp.getStatus());
    }

    /**
     * Method to test: {@link PageScannerResource#a11yCheck}
     * Given scenario: Both {@code apiUrl} and {@code apiAuthToken} are present
     *   in App secrets but hold empty strings.
     * Expected result: 503 SERVICE_UNAVAILABLE because neither value passes
     *   the {@code UtilMethods.isSet} check.
     */
    @Test
    void a11yCheck_emptyApiUrlAndToken_returns503() throws Exception {
        final AppSecrets secrets = AppSecrets.builder()
                .withKey(PageScannerResource.APP_KEY)
                .withSecret("apiUrl", "")
                .withSecret("apiAuthToken", "")
                .build();

        when(appsAPI.getSecrets(anyString(), anyBoolean(), any(Host.class), any(User.class)))
                .thenReturn(Optional.of(secrets));

        final Response resp = resource.a11yCheck(request, response,
                new PageScanCheckForm.Builder().url("https://example.com").build());

        assertEquals(Response.Status.SERVICE_UNAVAILABLE.getStatusCode(), resp.getStatus());
    }

    // -----------------------------------------------------------------------
    // 500 INTERNAL_SERVER_ERROR — JWT generation fails
    // -----------------------------------------------------------------------

    /**
     * Method to test: {@link PageScannerResource#a11yCheck}
     * Given scenario: App is fully configured but {@link ApiTokenAPI#persistApiToken}
     *   throws an exception, so no JWT can be generated.
     * Expected result: 500 INTERNAL_SERVER_ERROR with error code TOKEN_GENERATION_FAILED.
     */
    @Test
    void a11yCheck_tokenGenerationFails_returns500() throws Exception {
        configureValidSecrets();

        when(apiTokenAPI.persistApiToken(anyString(), any(), anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("DB unavailable"));

        final Response resp = resource.a11yCheck(request, response,
                new PageScanCheckForm.Builder().url("https://example.com").build());

        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), resp.getStatus());
    }

    // -----------------------------------------------------------------------
    // 502 BAD_GATEWAY — Upstream auth errors must not leak to the client
    // -----------------------------------------------------------------------

    /**
     * Method to test: {@link PageScannerResource#a11yCheck}
     * Given scenario: Upstream Page Scanner service responds 401.
     * Expected result: 502 BAD_GATEWAY — a 401 from the external service must
     *   never be forwarded verbatim because browsers treat it as a session error
     *   and would log the user out of dotCMS.
     */
    @Test
    @SuppressWarnings("unchecked")
    void a11yCheck_upstream401_returns502() throws Exception {
        configureValidSecrets();
        configureTokenGeneration();
        configureHttpClient(401, "");

        final Response resp = resource.a11yCheck(request, response,
                new PageScanCheckForm.Builder().url("https://example.com").build());

        assertEquals(Response.Status.BAD_GATEWAY.getStatusCode(), resp.getStatus());
    }

    /**
     * Method to test: {@link PageScannerResource#a11yCheck}
     * Given scenario: Upstream Page Scanner service responds 403.
     * Expected result: 502 BAD_GATEWAY — same auth-leak prevention as for 401.
     */
    @Test
    @SuppressWarnings("unchecked")
    void a11yCheck_upstream403_returns502() throws Exception {
        configureValidSecrets();
        configureTokenGeneration();
        configureHttpClient(403, "");

        final Response resp = resource.a11yCheck(request, response,
                new PageScanCheckForm.Builder().url("https://example.com").build());

        assertEquals(Response.Status.BAD_GATEWAY.getStatusCode(), resp.getStatus());
    }

    // -----------------------------------------------------------------------
    // 502 BAD_GATEWAY — Network error
    // -----------------------------------------------------------------------

    /**
     * Method to test: {@link PageScannerResource#a11yCheck}
     * Given scenario: The HTTP call to the upstream service throws an IOException
     *   (e.g. connection refused, timeout).
     * Expected result: 502 BAD_GATEWAY with error code PAGE_SCANNER_UNREACHABLE.
     */
    @Test
    @SuppressWarnings("unchecked")
    void a11yCheck_networkError_returns502() throws Exception {
        configureValidSecrets();
        configureTokenGeneration();

        when(httpClient.send(any(), any())).thenThrow(new java.io.IOException("Connection refused"));

        final Response resp = resource.a11yCheck(request, response,
                new PageScanCheckForm.Builder().url("https://example.com").build());

        assertEquals(Response.Status.BAD_GATEWAY.getStatusCode(), resp.getStatus());
    }

    // -----------------------------------------------------------------------
    // 200 OK — Successful proxy round-trip
    // -----------------------------------------------------------------------

    /**
     * Method to test: {@link PageScannerResource#a11yCheck}
     * Given scenario: App is configured, JWT is generated, and the upstream
     *   service returns 200 with a JSON body.
     * Expected result: 200 OK with the upstream body passed through unchanged.
     */
    @Test
    @SuppressWarnings("unchecked")
    void a11yCheck_upstreamSuccess_forwardsStatusAndBody() throws Exception {
        configureValidSecrets();
        configureTokenGeneration();
        final String upstreamBody = "{\"score\":100}";
        configureHttpClient(200, upstreamBody);

        final Response resp = resource.a11yCheck(request, response,
                new PageScanCheckForm.Builder().url("https://example.com").build());

        assertEquals(200, resp.getStatus());
        assertEquals(upstreamBody, resp.getEntity());
    }

    /**
     * Method to test: {@link PageScannerResource#geoCheck}
     * Given scenario: App is configured, JWT is generated, and the upstream
     *   service returns 200.
     * Expected result: 200 OK — confirms the geo endpoint follows the same
     *   happy-path as the a11y endpoint.
     */
    @Test
    @SuppressWarnings("unchecked")
    void geoCheck_upstreamSuccess_returns200() throws Exception {
        configureValidSecrets();
        configureTokenGeneration();
        configureHttpClient(200, "{\"geo\":\"US\"}");

        final Response resp = resource.geoCheck(request, response,
                new PageScanCheckForm.Builder().url("https://example.com").build());

        assertEquals(200, resp.getStatus());
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    /** Stubs appsAPI to return a fully-populated {@link AppSecrets}. */
    private void configureValidSecrets() throws Exception {
        final AppSecrets secrets = AppSecrets.builder()
                .withKey(PageScannerResource.APP_KEY)
                .withSecret("apiUrl", "https://scanner.example.com")
                .withHiddenSecret("apiAuthToken", "super-secret-token")
                .build();

        when(appsAPI.getSecrets(anyString(), anyBoolean(), any(Host.class), any(User.class)))
                .thenReturn(Optional.of(secrets));
    }

    /** Stubs apiTokenAPI so {@link PageScannerResource#generateShortLivedToken} succeeds. */
    private void configureTokenGeneration() throws Exception {
        final ApiToken apiToken = mock(ApiToken.class);
        when(apiTokenAPI.persistApiToken(anyString(), any(), anyString(), anyString(), anyString()))
                .thenReturn(apiToken);
        when(apiTokenAPI.getJWT(any(ApiToken.class), any(User.class)))
                .thenReturn("mock.jwt.token");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
    }

    /** Stubs {@link HttpClient#send} to return the given status code and body. */
    @SuppressWarnings("unchecked")
    private void configureHttpClient(final int statusCode, final String body) throws Exception {
        final HttpResponse<String> httpResponse = mock(HttpResponse.class);
        when(httpResponse.statusCode()).thenReturn(statusCode);
        when(httpResponse.body()).thenReturn(body);
        when(httpClient.send(any(), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                .thenReturn(httpResponse);
    }
}
