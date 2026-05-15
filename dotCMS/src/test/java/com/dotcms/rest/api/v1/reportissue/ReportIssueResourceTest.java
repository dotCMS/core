package com.dotcms.rest.api.v1.reportissue;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.rest.ErrorEntity;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotmarketing.util.Config;
import com.liferay.portal.model.User;
import com.liferay.portal.util.ReleaseInfo;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.file.StreamDataBodyPart;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class ReportIssueResourceTest {

    private WebResource webResource;
    private CapturingForwarder forwarder;
    private ReportIssueResource resource;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private User user;

    @BeforeEach
    void setUp() {
        webResource = mock(WebResource.class);
        forwarder = new CapturingForwarder();
        resource = new ReportIssueResource(webResource, forwarder);
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        user = mock(User.class);

        final InitDataObject initDataObject = mock(InitDataObject.class);
        when(initDataObject.getUser()).thenReturn(user);
        when(webResource.init(any(WebResource.InitBuilder.class))).thenReturn(initDataObject);

        when(user.getUserId()).thenReturn("user-1");
        when(user.getEmailAddress()).thenReturn("user@example.com");
        when(user.getFullName()).thenReturn("Test User");
        when(request.getHeader("User-Agent")).thenReturn("Chrome 120");
        when(request.getHeader("Referer")).thenReturn("https://example.com/admin");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getServerName()).thenReturn("example.com");
        when(request.getRequestURI()).thenReturn("/api/v1/report-issue");
        when(request.getRequestURL()).thenReturn(new StringBuffer("https://example.com/dotAdmin"));
    }

    @AfterEach
    void tearDown() {
        Config.setProperty(ReportIssueResource.WORKFLOW_URL_PROPERTY, null);
        Config.setProperty("REPORT_ISSUE_SCREENSHOT_MAX_BYTES", null);
    }

    @Test
    void reportIssue_descriptionOnly_forwardsBugContentlet() {
        final Response response = resource.reportIssue(request, this.response,
                formWithDescription("Login button is broken\nOn Safari"));

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals("https://corpsites-headless.dotcms.cloud/api/v1/workflow/actions/default/fire/NEW",
                forwarder.request.upstreamUri().toString());
        assertEquals("Bug", forwarder.request.contentlet().get("contentType"));
        assertEquals("/api/v1/report-issue [" + ReleaseInfo.getVersion() + "]",
                forwarder.request.contentlet().get("title"));
        assertEquals("Login button is broken\nOn Safari", forwarder.request.contentlet().get("description"));
        assertFalse(forwarder.request.contentlet().containsKey("binaryFields"));
        assertTrue(forwarder.request.screenshot().isEmpty());
        assertTrue(forwarder.request.binaryFields().isEmpty());

        final Map<String, Object> metadata = metadata();
        assertEquals(ReleaseInfo.getVersion(), metadata.get("dotcmsVersion"));
        assertEquals(ReleaseInfo.getBuildDateString(), metadata.get("dotcmsBuildDate"));
        assertEquals("Chrome 120", metadata.get("userAgent"));
        assertEquals("https://example.com/admin", metadata.get("referer"));
        assertEquals("https://example.com/dotAdmin", metadata.get("requestUrl"));
        assertEquals("example.com", metadata.get("serverName"));
    }

    @Test
    void reportIssue_withScreenshot_forwardsMultipartFields() {
        final byte[] screenshot = "fake-png".getBytes();
        final FormDataMultiPart multipart = formWithDescription("Editor panel overlaps");
        multipart.field("metadata", "{\"browser\":\"Safari 17\",\"url\":\"https://example.com/dotAdmin/#/content\",\"viewport\":\"1440x900\"}");
        multipart.bodyPart(new StreamDataBodyPart(
                "screenshot",
                new ByteArrayInputStream(screenshot),
                "screenshot.png",
                MediaType.valueOf("image/png")));

        final Response response = resource.reportIssue(request, this.response, multipart);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals("/content [" + ReleaseInfo.getVersion() + " - Safari]",
                forwarder.request.contentlet().get("title"));
        assertFalse(forwarder.request.contentlet().containsKey("binaryFields"));
        assertEquals(List.of("screenshot"), forwarder.request.binaryFields());
        assertTrue(forwarder.request.screenshot().isPresent());
        assertEquals("image/png", forwarder.request.screenshot().get().mediaType());
        assertArrayEquals(screenshot, forwarder.request.screenshot().get().bytes());

        final Map<String, Object> metadata = metadata();
        assertEquals(ReleaseInfo.getVersion(), metadata.get("dotcmsVersion"));
        assertEquals(ReleaseInfo.getBuildDateString(), metadata.get("dotcmsBuildDate"));
        final Map<String, Object> clientMetadata = clientMetadata(metadata);
        assertEquals("Safari 17", clientMetadata.get("browser"));
        assertEquals("https://example.com/dotAdmin/#/content", clientMetadata.get("url"));
        assertEquals("1440x900", clientMetadata.get("viewport"));
    }

    @Test
    void reportIssue_withUserAgentMetadata_buildsTitleFromPathAndParsedBrowser() {
        final FormDataMultiPart multipart = formWithDescription("Editor panel overlaps");
        multipart.field("metadata", "{\"browser\":\"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.5 Safari/605.1.15\",\"url\":\"https://example.com/dotAdmin/#/pages\"}");

        final Response response = resource.reportIssue(request, this.response, multipart);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals("/pages [" + ReleaseInfo.getVersion() + " - Safari]",
                forwarder.request.contentlet().get("title"));
    }

    @Test
    void reportIssue_blankDescription_returns400() {
        final Response response = resource.reportIssue(request, this.response, formWithDescription("   "));

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertEquals(ReportIssueResource.ERROR_INVALID_REQUEST, firstError(response).getErrorCode());
    }

    @Test
    void reportIssue_invalidScreenshotType_returns400() {
        final FormDataMultiPart multipart = formWithDescription("Broken screen");
        multipart.bodyPart(new FormDataBodyPart(
                "screenshot",
                new ByteArrayInputStream("text".getBytes()),
                MediaType.TEXT_PLAIN_TYPE));

        final Response response = resource.reportIssue(request, this.response, multipart);

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertEquals(ReportIssueResource.ERROR_INVALID_REQUEST, firstError(response).getErrorCode());
    }

    @Test
    void reportIssue_oversizedScreenshot_returns400() {
        Config.setProperty("REPORT_ISSUE_SCREENSHOT_MAX_BYTES", "3");
        final FormDataMultiPart multipart = formWithDescription("Broken screen");
        multipart.bodyPart(new StreamDataBodyPart(
                "screenshot",
                new ByteArrayInputStream("too-large".getBytes()),
                "screenshot.png",
                MediaType.valueOf("image/png")));

        final Response response = resource.reportIssue(request, this.response, multipart);

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertEquals(ReportIssueResource.ERROR_INVALID_REQUEST, firstError(response).getErrorCode());
    }

    @Test
    void reportIssue_upstream401_returnsProxyNotAuthorized502() {
        forwarder.response = new ReportIssueForwardResponse(
                Response.Status.UNAUTHORIZED.getStatusCode(),
                "",
                Optional.of(MediaType.APPLICATION_JSON));

        final Response response = resource.reportIssue(request, this.response,
                formWithDescription("Unable to save page"));

        assertEquals(Response.Status.BAD_GATEWAY.getStatusCode(), response.getStatus());
        assertEquals(ReportIssueResource.ERROR_PROXY_NOT_AUTHORIZED, firstError(response).getErrorCode());
    }

    @Test
    void reportIssue_upstream403_returnsProxyNotAuthorized502() {
        forwarder.response = new ReportIssueForwardResponse(
                Response.Status.FORBIDDEN.getStatusCode(),
                "",
                Optional.of(MediaType.APPLICATION_JSON));

        final Response response = resource.reportIssue(request, this.response,
                formWithDescription("Unable to save page"));

        assertEquals(Response.Status.BAD_GATEWAY.getStatusCode(), response.getStatus());
        assertEquals(ReportIssueResource.ERROR_PROXY_NOT_AUTHORIZED, firstError(response).getErrorCode());
    }

    @Test
    void reportIssue_networkFailure_returnsServiceUnavailable502() {
        forwarder.exception = new IOException("Connection refused");

        final Response response = resource.reportIssue(request, this.response,
                formWithDescription("Unable to save page"));

        assertEquals(Response.Status.BAD_GATEWAY.getStatusCode(), response.getStatus());
        assertEquals(ReportIssueResource.ERROR_SERVICE_UNAVAILABLE, firstError(response).getErrorCode());
    }

    @Test
    @SuppressWarnings("unchecked")
    void httpForwarder_doesNotSendAuthorizationHeader() throws Exception {
        final HttpClient httpClient = mock(HttpClient.class);
        final HttpResponse<String> httpResponse = mock(HttpResponse.class);
        when(httpResponse.statusCode()).thenReturn(Response.Status.OK.getStatusCode());
        when(httpResponse.body()).thenReturn("{}");
        when(httpResponse.headers()).thenReturn(java.net.http.HttpHeaders.of(Map.of(), (name, value) -> true));
        when(httpClient.send(any(), any(HttpResponse.BodyHandler.class))).thenReturn(httpResponse);

        final HttpReportIssueForwarder httpForwarder = new HttpReportIssueForwarder(httpClient);
        httpForwarder.forward(new ReportIssueForwardRequest(
                URI.create("https://corpsites.example.com/api/v1/workflow/actions/default/fire/NEW"),
                Map.of("contentType", "Bug", "title", "Title", "description", "Description"),
                Optional.empty(),
                List.of()));

        final ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        org.mockito.Mockito.verify(httpClient).send(requestCaptor.capture(), any(HttpResponse.BodyHandler.class));

        assertTrue(requestCaptor.getValue().headers().firstValue("Authorization").isEmpty());
        assertEquals("POST", requestCaptor.getValue().method());
        assertEquals("application/json",
                requestCaptor.getValue().headers().firstValue("Content-Type").orElse(""));
    }

    @Test
    @SuppressWarnings("unchecked")
    void httpForwarder_withScreenshot_sendsMultipartJsonAndFileWithoutAuthorization() throws Exception {
        final HttpClient httpClient = mock(HttpClient.class);
        final HttpResponse<String> httpResponse = mock(HttpResponse.class);
        when(httpResponse.statusCode()).thenReturn(Response.Status.OK.getStatusCode());
        when(httpResponse.body()).thenReturn("{}");
        when(httpResponse.headers()).thenReturn(java.net.http.HttpHeaders.of(Map.of(), (name, value) -> true));
        when(httpClient.send(any(), any(HttpResponse.BodyHandler.class))).thenReturn(httpResponse);

        final HttpReportIssueForwarder httpForwarder = new HttpReportIssueForwarder(httpClient);
        httpForwarder.forward(new ReportIssueForwardRequest(
                URI.create("https://corpsites.example.com/api/v1/workflow/actions/default/fire/NEW"),
                Map.of("contentType", "Bug", "title", "Title", "description", "Description"),
                Optional.of(new ReportIssueScreenshot("screen.png", "image/png", "fake-png".getBytes())),
                List.of("screenshot")));

        final ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        org.mockito.Mockito.verify(httpClient).send(requestCaptor.capture(), any(HttpResponse.BodyHandler.class));

        final HttpRequest capturedRequest = requestCaptor.getValue();
        assertTrue(capturedRequest.headers().firstValue("Authorization").isEmpty());
        assertEquals("PUT", capturedRequest.method());
        assertTrue(capturedRequest.headers().firstValue("Content-Type").orElse("")
                .startsWith("multipart/form-data; boundary=dotcms-report-issue-"));

        final String body = bodyAsString(capturedRequest);
        assertTrue(body.contains("Content-Disposition: form-data; name=\"json\""));
        assertTrue(body.contains("\"contentlet\""));
        assertTrue(body.contains("\"binaryFields\":[\"screenshot\"]"));
        assertTrue(body.contains("\"contentType\":\"Bug\""));
        assertTrue(body.contains("Content-Disposition: form-data; name=\"file\"; filename=\"screen.png\""));
        assertTrue(body.contains("Content-Type: image/png"));
        assertTrue(body.contains("fake-png"));
    }

    private FormDataMultiPart formWithDescription(final String description) {
        return new FormDataMultiPart().field("description", description);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> metadata() {
        return (Map<String, Object>) forwarder.request.contentlet().get("metadata");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> clientMetadata(final Map<String, Object> metadata) {
        final Object client = metadata.get("client");
        return client instanceof Map ? (Map<String, Object>) client : Map.of();
    }

    private ErrorEntity firstError(final Response response) {
        return ((ResponseEntityView<?>) response.getEntity()).getErrors().get(0);
    }

    private String bodyAsString(final HttpRequest request) throws Exception {
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        final CountDownLatch completed = new CountDownLatch(1);
        final AtomicReference<Throwable> error = new AtomicReference<>();

        request.bodyPublisher().orElseThrow().subscribe(new Flow.Subscriber<>() {
            @Override
            public void onSubscribe(final Flow.Subscription subscription) {
                subscription.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(final ByteBuffer item) {
                final byte[] bytes = new byte[item.remaining()];
                item.get(bytes);
                outputStream.write(bytes, 0, bytes.length);
            }

            @Override
            public void onError(final Throwable throwable) {
                error.set(throwable);
                completed.countDown();
            }

            @Override
            public void onComplete() {
                completed.countDown();
            }
        });

        assertTrue(completed.await(1, TimeUnit.SECONDS));
        if (error.get() != null) {
            throw new IOException(error.get());
        }

        return outputStream.toString(StandardCharsets.UTF_8);
    }

    private static class CapturingForwarder implements ReportIssueForwarder {
        private ReportIssueForwardRequest request;
        private ReportIssueForwardResponse response = new ReportIssueForwardResponse(
                Response.Status.OK.getStatusCode(),
                "{}",
                Optional.of(MediaType.APPLICATION_JSON));
        private IOException exception;

        @Override
        public ReportIssueForwardResponse forward(final ReportIssueForwardRequest request) throws IOException {
            this.request = request;
            if (exception != null) {
                throw exception;
            }
            return response;
        }
    }
}
