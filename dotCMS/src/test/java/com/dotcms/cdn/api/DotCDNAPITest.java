package com.dotcms.cdn.api;

import com.dotcms.cdn.CDNConstants;
import com.dotcms.http.CircuitBreakerUrl;
import com.dotcms.http.CircuitBreakerUrlBuilder;
import com.dotcms.rest.RestClientBuilder;
import com.dotcms.rest.exception.BadRequestException;
import com.dotcms.security.apps.AppSecrets;
import com.dotcms.security.apps.AppsAPI;
import com.dotcms.security.apps.Secret;
import com.dotcms.security.apps.Type;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import org.apache.http.conn.ConnectTimeoutException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the DotCDNAPI class. These tests verify the behavior of the API methods
 * related to fetching CDN stats and invalidating URLs using a mock setup.
 */
class DotCDNAPITest {

    private AutoCloseable closeable;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() throws Exception {
        closeable.close();
    }

    @Mock
    private AppsAPI appsAPI;

    @Mock
    private AppSecrets appSecrets;

    @Mock
    private CircuitBreakerUrl circuitBreakerUrl;

    @Mock
    private Host host;

    @Mock
    private Client restClient;

    @Mock
    private WebTarget webTarget;

    @Mock
    private Invocation.Builder invocationBuilder;

    @Mock
    private Response restResponse;

    private static final String TEST_API_KEY = "test-api-key";
    private static final String TEST_CDN_DOMAIN = "demo.dotcms.com";
    private static final String TEST_ZONE_ID = "1234567890";
    private static final String TEST_HOST_IDENTIFIER = "1234567890";

    @Test
    void test_getStats_Should_Return_Stats_For_Valid_Date_Range()
            throws DotDataException, DotSecurityException, IOException {

        try (MockedStatic<APILocator> apiLocator = mockStatic(APILocator.class)) {

            setupAppsAPIMock(apiLocator);
            setupCDNSecrets();
            when(host.getIdentifier()).thenReturn(TEST_HOST_IDENTIFIER);

            try (MockedConstruction<CircuitBreakerUrlBuilder> ignoredUrlBuilder =
                    mockConstruction(CircuitBreakerUrlBuilder.class,
                         (urlBuilder, context) ->
                             setCircuitBreakerUrlBuilderMock(urlBuilder))) {

                final String statsResponse = Files.readString(Paths.get(
                        "src/test/resources/test-stats-response.json"),
                        StandardCharsets.UTF_8);
                when(circuitBreakerUrl.doString()).thenReturn(statsResponse);

                final DotCDNAPI dotCDNAPI = DotCDNAPI.api(host);
                final DotCDNStats dotCDNStats = dotCDNAPI.getStats("2023-10-01", "2023-10-02");
                assertNotNull(dotCDNStats);
            }
        }
    }

    @Test
    void test_getStats_Should_Throw_BadRequestException_For_Invalid_Dates()
            throws DotDataException, DotSecurityException, IOException {

        try (MockedStatic<APILocator> apiLocator = mockStatic(APILocator.class)) {

            setupAppsAPIMock(apiLocator);
            setupCDNSecrets();
            when(host.getIdentifier()).thenReturn(TEST_HOST_IDENTIFIER);

            try (MockedConstruction<CircuitBreakerUrlBuilder> ignoredUrlBuilder =
                    mockConstruction(CircuitBreakerUrlBuilder.class,
                        (urlBuilder, context) ->
                            setCircuitBreakerUrlBuilderMock(urlBuilder))) {

                when(circuitBreakerUrl.doString()).thenThrow(new BadRequestException("Invalid date range"));
                final DotCDNAPI dotCDNAPI = DotCDNAPI.api(host);

                assertThrows(BadRequestException.class, () ->
                        dotCDNAPI.getStats("2023-10-01", "2023-10-02")
                );
            }
        }
    }

    @Test
    void test_getStats_Should_Throw_Bad_Request_For_Timeout()
            throws DotDataException, DotSecurityException, IOException {

        try (MockedStatic<APILocator> apiLocator = mockStatic(APILocator.class)) {

            setupAppsAPIMock(apiLocator);
            setupCDNSecrets();
            when(host.getIdentifier()).thenReturn(TEST_HOST_IDENTIFIER);

            try (MockedConstruction<CircuitBreakerUrlBuilder> ignoredUrlBuilder =
                         mockConstruction(CircuitBreakerUrlBuilder.class,
                                 (urlBuilder, context) ->
                                         setCircuitBreakerUrlBuilderMock(urlBuilder))) {

                when(circuitBreakerUrl.doString()).thenThrow(new ConnectTimeoutException("Request timed out"));
                final DotCDNAPI dotCDNAPI = DotCDNAPI.api(host);

                assertThrows(BadRequestException.class, () ->
                        dotCDNAPI.getStats("2023-10-01", "2023-10-02")
                );
            }
        }
    }

    @Test
    void test_invalidate_Should_Return_True_For_Valid_Urls()
            throws DotDataException, DotSecurityException, IOException {
        try (MockedStatic<APILocator> apiLocator = mockStatic(APILocator.class)) {

            setupAppsAPIMock(apiLocator);
            setupCDNSecrets();
            when(host.getIdentifier()).thenReturn(TEST_HOST_IDENTIFIER);

            @SuppressWarnings("unchecked")
            final CircuitBreakerUrl.Response<String> mockResponse =
                    org.mockito.Mockito.mock(CircuitBreakerUrl.Response.class);
            when(mockResponse.getStatusCode()).thenReturn(200);
            when(mockResponse.getResponse()).thenReturn("");

            try (MockedConstruction<CircuitBreakerUrlBuilder> ignoredUrlBuilder =
                mockConstruction(CircuitBreakerUrlBuilder.class,
                    (urlBuilder, context) ->
                        setCircuitBreakerUrlBuilderMock(urlBuilder))) {

                when(circuitBreakerUrl.doResponse()).thenReturn(mockResponse);

                final DotCDNAPI dotCDNAPI = DotCDNAPI.api(host);
                final boolean result = dotCDNAPI.invalidate(List.of(
                        "demo.dotcms.com/page1", "demo.dotcms.com/page2"));

                assertTrue(result);
            }
        }
    }

    @Test
    void test_invalidate_Should_Return_False_For_Bad_Request_Response()
            throws DotDataException, DotSecurityException, IOException {
        try (MockedStatic<APILocator> apiLocator = mockStatic(APILocator.class)) {

            setupAppsAPIMock(apiLocator);
            setupCDNSecrets();
            when(host.getIdentifier()).thenReturn(TEST_HOST_IDENTIFIER);

            @SuppressWarnings("unchecked")
            final CircuitBreakerUrl.Response<String> mockResponse =
                    org.mockito.Mockito.mock(CircuitBreakerUrl.Response.class);
            when(mockResponse.getStatusCode()).thenReturn(400);
            when(mockResponse.getResponse()).thenReturn("Bad Request");

            try (MockedConstruction<CircuitBreakerUrlBuilder> ignoredUrlBuilder =
                    mockConstruction(CircuitBreakerUrlBuilder.class,
                        (urlBuilder, context) ->
                            setCircuitBreakerUrlBuilderMock(urlBuilder))) {

                when(circuitBreakerUrl.doResponse()).thenReturn(mockResponse);

                final DotCDNAPI dotCDNAPI = DotCDNAPI.api(host);
                final boolean result = dotCDNAPI.invalidate(List.of(
                        "demo.dotcms.com/invalid-url-1"
                ));

                assertFalse(result);
            }
        }
    }

    /**
     * A URL that only starts with the CDN domain as a string, like demo.dotcms.com.example.net, is
     * a different host. It must never be sent upstream as if it were on the CDN. With a scheme it
     * is skipped and counted as a failure. Without one it is indistinguishable from a relative
     * path, so it is purged as a path under the CDN domain, never as a URL on the other host.
     */
    @Test
    void test_invalidate_Should_Not_Purge_Urls_On_Another_Host_That_Start_With_The_Cdn_Domain()
            throws DotDataException, DotSecurityException, IOException {
        try (MockedStatic<APILocator> apiLocator = mockStatic(APILocator.class)) {

            setupAppsAPIMock(apiLocator);
            setupCDNSecrets();
            when(host.getIdentifier()).thenReturn(TEST_HOST_IDENTIFIER);

            final List<String> sentUrls = new ArrayList<>();
            try (MockedConstruction<CircuitBreakerUrlBuilder> ignoredUrlBuilder =
                    mockConstruction(CircuitBreakerUrlBuilder.class,
                        (urlBuilder, context) ->
                            setCircuitBreakerUrlBuilderMock(urlBuilder, sentUrls))) {

                final CircuitBreakerUrl.Response<String> okResponse = okResponse();
                when(circuitBreakerUrl.doResponse()).thenReturn(okResponse);

                final boolean schemeResult = DotCDNAPI.api(host).invalidate(List.of(
                        "https://demo.dotcms.com.example.net/x"));

                assertFalse(schemeResult, "a URL on another host counts as a failed purge");
                assertTrue(sentUrls.isEmpty(),
                        "nothing should have reached the CDN API, but sent: " + sentUrls);

                DotCDNAPI.api(host).invalidate(List.of("demo.dotcms.com.example.net/x"));

                assertEquals(List.of("demo.dotcms.com/demo.dotcms.com.example.net/x"),
                        purgedUrls(sentUrls));
            }
        }
    }

    /**
     * Every way of naming a file on the CDN domain, as a path, a bare path, the domain with or
     * without a scheme, ends up as a purge of that file on the configured domain.
     */
    @Test
    void test_invalidate_Should_Purge_Every_Form_Of_A_Url_On_The_Cdn_Domain()
            throws DotDataException, DotSecurityException, IOException {
        try (MockedStatic<APILocator> apiLocator = mockStatic(APILocator.class)) {

            setupAppsAPIMock(apiLocator);
            setupCDNSecrets();
            when(host.getIdentifier()).thenReturn(TEST_HOST_IDENTIFIER);

            final List<String> sentUrls = new ArrayList<>();
            try (MockedConstruction<CircuitBreakerUrlBuilder> ignoredUrlBuilder =
                    mockConstruction(CircuitBreakerUrlBuilder.class,
                        (urlBuilder, context) ->
                            setCircuitBreakerUrlBuilderMock(urlBuilder, sentUrls))) {

                final CircuitBreakerUrl.Response<String> okResponse = okResponse();
                when(circuitBreakerUrl.doResponse()).thenReturn(okResponse);

                final boolean result = DotCDNAPI.api(host).invalidate(List.of(
                        "/page1",
                        "demo.dotcms.com/page2",
                        "https://demo.dotcms.com/page3?x=1",
                        "page4"));

                assertTrue(result);
                assertEquals(List.of(
                        "demo.dotcms.com/page1",
                        "demo.dotcms.com/page2",
                        "demo.dotcms.com/page3?x=1",
                        "demo.dotcms.com/page4"), purgedUrls(sentUrls));
            }
        }
    }

    @Test
    void test_toCdnPath_Should_Reduce_Urls_On_A_Domain_Configured_Without_A_Scheme()
            throws DotDataException, DotSecurityException {
        try (MockedStatic<APILocator> apiLocator = mockStatic(APILocator.class)) {

            setupAppsAPIMock(apiLocator);
            setupCDNSecrets("demo.dotcms.com");
            when(host.getIdentifier()).thenReturn(TEST_HOST_IDENTIFIER);
            final DotCDNAPI dotCDNAPI = DotCDNAPI.api(host);

            assertEquals(Optional.of("/a/b"), dotCDNAPI.toCdnPath("/a/b"));
            assertEquals(Optional.of("a/b"), dotCDNAPI.toCdnPath("a/b"));
            assertEquals(Optional.of("/images/*"),
                    dotCDNAPI.toCdnPath("https://demo.dotcms.com/images/*"));
            assertEquals(Optional.of("/a?b=1"),
                    dotCDNAPI.toCdnPath("HTTP://DEMO.dotcms.com/a?b=1#fragment"));
            assertEquals(Optional.of("/"), dotCDNAPI.toCdnPath("https://demo.dotcms.com"));
            assertEquals(Optional.of("/a"), dotCDNAPI.toCdnPath("demo.dotcms.com/a"));
            assertEquals(Optional.of("/dA/x?next=http://y"),
                    dotCDNAPI.toCdnPath("/dA/x?next=http://y"));
        }
    }

    @Test
    void test_toCdnPath_Should_Reject_Urls_On_Another_Host()
            throws DotDataException, DotSecurityException {
        try (MockedStatic<APILocator> apiLocator = mockStatic(APILocator.class)) {

            setupAppsAPIMock(apiLocator);
            setupCDNSecrets("https://cdn.client.com");
            when(host.getIdentifier()).thenReturn(TEST_HOST_IDENTIFIER);
            final DotCDNAPI dotCDNAPI = DotCDNAPI.api(host);

            assertEquals(Optional.of("/x"), dotCDNAPI.toCdnPath("https://cdn.client.com/x"));
            // the CDN domain is only a text prefix of these hosts
            assertEquals(Optional.empty(), dotCDNAPI.toCdnPath("https://cdn.client.com.example.net/x"));
            assertEquals(Optional.empty(), dotCDNAPI.toCdnPath("https://cdn.client.com@evil.example/x"));
            assertEquals(Optional.empty(), dotCDNAPI.toCdnPath("https://other.example/x"));
            // not a web URL, and not a parseable one
            assertEquals(Optional.empty(), dotCDNAPI.toCdnPath("ftp://cdn.client.com/x"));
            assertEquals(Optional.empty(), dotCDNAPI.toCdnPath("https://cdn.client.com/a b"));
        }
    }

    @Test
    void test_invalidateAll_Should_Return_True_For_Success()
            throws DotDataException, DotSecurityException {
        try (MockedStatic<APILocator> apiLocator = mockStatic(APILocator.class)) {

            setupAppsAPIMock(apiLocator);
            setupCDNSecrets();
            when(host.getIdentifier()).thenReturn(TEST_HOST_IDENTIFIER);

            try (MockedStatic<RestClientBuilder> restClientBuilder = mockStatic(RestClientBuilder.class)) {

                setupRestClientMock(restClientBuilder);
                when(restResponse.getStatus()).thenReturn(Response.Status.NO_CONTENT.getStatusCode());

                final DotCDNAPI dotCDNAPI = DotCDNAPI.api(host);
                final boolean result = dotCDNAPI.invalidateAll();

                assertTrue(result);
                verify(restClient).close();
            }
        }
    }

    @Test
    void test_invalidateAll_Should_Throw_Bad_Request_For_Invalid_Request()
            throws DotDataException, DotSecurityException {
        try (MockedStatic<APILocator> apiLocator = mockStatic(APILocator.class)) {

            setupAppsAPIMock(apiLocator);
            setupCDNSecrets();
            when(host.getIdentifier()).thenReturn(TEST_HOST_IDENTIFIER);

            try (MockedStatic<RestClientBuilder> restClientBuilder = mockStatic(RestClientBuilder.class)) {

                setupRestClientMock(restClientBuilder);
                when(restResponse.getStatus()).thenReturn(Response.Status.BAD_REQUEST.getStatusCode());

                final DotCDNAPI dotCDNAPI = DotCDNAPI.api(host);

                assertThrows(BadRequestException.class, dotCDNAPI::invalidateAll);
                verify(restClient).close();
            }
        }
    }

    private void setCircuitBreakerUrlBuilderMock(CircuitBreakerUrlBuilder urlBuilder) {
        setCircuitBreakerUrlBuilderMock(urlBuilder, new ArrayList<>());
    }

    /**
     * Same as the single-argument version, and also records every URL the code under test asks the
     * builder to call, so a test can assert on what would have gone upstream.
     */
    private void setCircuitBreakerUrlBuilderMock(CircuitBreakerUrlBuilder urlBuilder,
            List<String> sentUrls) {
        when(urlBuilder.setHeaders(anyMap())).thenReturn(urlBuilder);
        when(urlBuilder.setTimeout(anyLong())).thenReturn(urlBuilder);
        when(urlBuilder.setUrl(anyString())).thenAnswer(invocation -> {
            sentUrls.add(invocation.getArgument(0));
            return urlBuilder;
        });
        when(urlBuilder.setMethod(any())).thenReturn(urlBuilder);
        when(urlBuilder.setThrowWhenError(anyBoolean())).thenReturn(urlBuilder);
        when(urlBuilder.build()).thenReturn(circuitBreakerUrl);
    }

    /** A successful response from the CDN's purge endpoint. */
    private CircuitBreakerUrl.Response<String> okResponse() {
        @SuppressWarnings("unchecked")
        final CircuitBreakerUrl.Response<String> response =
                org.mockito.Mockito.mock(CircuitBreakerUrl.Response.class);
        when(response.getStatusCode()).thenReturn(200);
        when(response.getResponse()).thenReturn("");
        return response;
    }

    /** The URLs the CDN was asked to purge, decoded from the purge endpoint's query string. */
    private List<String> purgedUrls(final List<String> sentUrls) {
        return sentUrls.stream()
                .map(sent -> URLDecoder.decode(
                        sent.substring(sent.indexOf("url=") + "url=".length()),
                        StandardCharsets.UTF_8))
                .collect(Collectors.toList());
    }

    private void setupAppsAPIMock(final MockedStatic<APILocator> apiLocator)
            throws DotDataException, DotSecurityException {
        apiLocator.when(APILocator::getAppsAPI).thenReturn(appsAPI);
        final Optional<AppSecrets> appSecretsResult = Optional.of(appSecrets);
        when(appsAPI.getSecrets(eq(CDNConstants.DOT_CDN_APP_KEY),
                anyBoolean(), any(), any())).thenReturn(appSecretsResult);
    }

    private void setupCDNSecrets() {
        setupCDNSecrets(TEST_CDN_DOMAIN);
    }

    private void setupCDNSecrets(final String cdnDomain) {
        final Secret apiKeySecret = createSecret(TEST_API_KEY);
        final Secret cdnDomainSecret = createSecret(cdnDomain);
        final Secret zoneIdSecret = createSecret(TEST_ZONE_ID);

        when(appSecrets.getSecrets()).thenReturn(Map.of(
                CDNConstants.DOT_CDN_API_KEY, apiKeySecret,
                CDNConstants.DOT_CDN_DOMAIN, cdnDomainSecret,
                CDNConstants.DOT_CDN_ZONEID, zoneIdSecret
        ));
    }

    private Secret createSecret(String value) {
        return Secret.builder()
                .withValue(value)
                .withHidden(false)
                .withType(Type.STRING)
                .build();
    }

    private void setupRestClientMock(MockedStatic<RestClientBuilder> restClientBuilder) {
        restClientBuilder.when(RestClientBuilder::newClient).thenReturn(restClient);
        when(restClient.target(anyString())).thenReturn(webTarget);
        when(webTarget.request(any(MediaType.class))).thenReturn(invocationBuilder);
        when(invocationBuilder.header(anyString(), anyString())).thenReturn(invocationBuilder);
        when(invocationBuilder.post(any())).thenReturn(restResponse);
    }
}
