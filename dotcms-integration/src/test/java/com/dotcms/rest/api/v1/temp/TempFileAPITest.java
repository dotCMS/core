package com.dotcms.rest.api.v1.temp;

import static com.dotcms.datagen.TestDataUtils.getFileAssetContent;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.datagen.TestDataUtils;
import com.dotcms.datagen.TestDataUtils.TestFile;
import com.dotcms.http.server.mock.MockHttpServer;
import com.dotcms.http.server.mock.MockHttpServerContext;
import com.dotcms.mock.request.MockSession;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.util.network.IPUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.FileUtil;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.util.WebKeys;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import javax.servlet.http.HttpServletRequest;
import org.jetbrains.annotations.NotNull;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

public class TempFileAPITest {

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    @Test
    public void testValidURL_urlReturn400_throwsException() {
        final String url =  "https://raw.githubusercontent.com/url/throws/400.jpg";
        assertFalse(APILocator.getTempFileAPI().validUrl(url));
    }

    @Test
    public void testValidURL_urlReturn404_throwsException() {
        final String url =  "https://raw.githubusercontent.com/dotCMS/core/throws/dotCMS/404.jpg";
        assertFalse(APILocator.getTempFileAPI().validUrl(url));
    }

    @Test
    public void testValidURL_urlReturn200_returnTrue() {
        final String url =  "https://raw.githubusercontent.com/dotCMS/core/main/dotCMS/src/main/webapp/html/images/skin/logo.gif";
        assertTrue(APILocator.getTempFileAPI().validUrl(url));
    }

    @Test
    public void testValidURL_noHTTP_returnFalse() {
        final String url =  "test://raw.githubusercontent.com/dotCMS/core/main/dotCMS/src/main/webapp/html/images/skin/logo.gif";
        assertFalse(APILocator.getTempFileAPI().validUrl(url));
    }

    /**
     * Method to test: {@link TempFileAPI#getTempResourceId(File)}
     * Test scenario: create a temp file using the api
     * Expected: see how the method succeeds
     * @throws DotSecurityException
     * @throws IOException
     */
    @Test
    public void Test_Get_Temp_Id_From_Temp_File() throws DotSecurityException, IOException {
        final File file = FileUtil.createTemporaryFile("test", "txt", "");
        final HttpServletRequest request = mockHttpServletRequest();
        final DotTempFile dotTempFile = APILocator.getTempFileAPI().createTempFile("temp", request, com.liferay.util.FileUtil.createInputStream(file));
        assertTrue(dotTempFile.file.exists());
        final Optional<String> tempId = APILocator.getTempFileAPI().getTempResourceId(dotTempFile.file);
        assertTrue(tempId.isPresent());
        assertEquals(tempId.get(), dotTempFile.id);
    }

    /**
     * Method to test: {@link TempFileAPI#getTempResourceId(File)}
     * Test scenario: Create any non-temp file
     * Expected: See how the method fails to extract the temp_id
     * @throws DotSecurityException
     * @throws IOException
     */
    @Test
    public void Test_Get_Temp_Id_From_Regular_File() throws IOException {
        final File file = getFileAssetContent(true, 1L, TestFile.PNG).getBinary("fileAsset");
        final Optional<String> tempId = APILocator.getTempFileAPI().getTempResourceId(file);
        assertFalse(tempId.isPresent());
    }

    @NotNull
    public static HttpServletRequest mockHttpServletRequest() {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute(WebKeys.USER)).thenReturn(APILocator.systemUser());
        final MockSession mockSession = new MockSession(UUIDGenerator.generateUuid());
        when(request.getSession()).thenReturn(mockSession);
        when(request.getSession(Mockito.anyBoolean())).thenReturn(mockSession);

        when(request.getHeader("User-Agent")).thenReturn("any");
        when(request.getHeader("Host")).thenReturn("localhost");
        when(request.getHeader("Accept-Language")).thenReturn("any");
        when(request.getHeader("Accept-Encoding")).thenReturn("any");
        when(request.getHeader("X-Forwarded-For")).thenReturn("any");
        when(request.getHeader("Origin")).thenReturn("any");
        when(request.getRemoteHost()).thenReturn("127.0.0.1");
        when(request.isSecure()).thenReturn(false);
        return request;
    }

    /**
     * Method to test: {@link TempFileAPI#getBrowserHeaders()}
     * Test scenario: Verify that all required browser-compatible header keys are present
     * Expected: The map contains the 5 required header keys (no Sec-Fetch-* headers)
     */
    @Test
    public void testBrowserHeaders_containsAllRequiredKeys() {
        final List<String> requiredHeaders = Arrays.asList(
                "User-Agent",
                "Accept",
                "Accept-Language",
                "Accept-Encoding",
                "Connection"
        );

        final Map<String, String> headers = TempFileAPI.getBrowserHeaders();
        assertNotNull("getBrowserHeaders() must not return null", headers);
        for (final String header : requiredHeaders) {
            assertTrue(
                    "getBrowserHeaders() must contain header: " + header,
                    headers.containsKey(header));
            assertNotNull(
                    "Value for header '" + header + "' must not be null",
                    headers.get(header));
            assertFalse(
                    "Value for header '" + header + "' must not be empty",
                    headers.get(header).isEmpty());
        }
    }

    /**
     * Method to test: {@link TempFileAPI#getBrowserHeaders()}
     * Test scenario: Verify the default values of the browser-compatible headers
     * Expected:
     * - Accept defaults to {@code *}{@code /*} (generic, not image-specific)
     * - Connection defaults to {@code keep-alive} and is configurable
     * - Accept-Encoding does not advertise Brotli (br) — Apache HttpClient has no brotli decoder
     * - No Sec-Fetch-* headers are present (they are browser-generated metadata, not for server use)
     */
    @Test
    public void testBrowserHeaders_defaultValues() {
        final Map<String, String> headers = TempFileAPI.getBrowserHeaders();

        // Connection defaults to keep-alive (configurable via TEMP_FILE_URL_CONNECTION)
        assertEquals("keep-alive", headers.get("Connection"));

        // Accept must default to */* — the endpoint downloads any file type, not just images
        assertEquals("*/*", headers.get("Accept"));

        // Configurable headers must be present and non-empty (may be overridden via Config)
        assertTrue("User-Agent must not be empty",
                !headers.get("User-Agent").isEmpty());
        assertTrue("Accept-Language must not be empty",
                !headers.get("Accept-Language").isEmpty());

        // Accept-Encoding must not advertise brotli; Apache HttpClient has no brotli decoder
        final String acceptEncoding = headers.get("Accept-Encoding");
        assertTrue("Accept-Encoding must not be empty", !acceptEncoding.isEmpty());
        assertFalse("Accept-Encoding must not advertise brotli (br)", acceptEncoding.contains("br"));

        // Sec-Fetch-* headers must NOT be present — they are browser-generated Fetch Metadata
        // headers whose purpose is to let servers verify a request came from a real browser context.
        // Sending them from a server-side HTTP client is misleading and may cause rejections.
        assertFalse("Sec-Fetch-Dest must not be present", headers.containsKey("Sec-Fetch-Dest"));
        assertFalse("Sec-Fetch-Mode must not be present", headers.containsKey("Sec-Fetch-Mode"));
        assertFalse("Sec-Fetch-Site must not be present", headers.containsKey("Sec-Fetch-Site"));
    }

    /**
     * Method to test: {@link TempFileAPI#validUrl(String)}
     * Test scenario: Verify that browser-like headers are actually sent in the outbound HTTP request
     * Expected: All headers returned by getBrowserHeaders() arrive at the mock server
     */
    @Test
    public void testValidUrl_sendsBrowserHeaders() {
        final String mockIp = "127.0.0.1";
        final int mockPort = 50881;
        final String path = "/image.png";

        // Capture the incoming request headers via an AtomicReference
        final AtomicReference<com.sun.net.httpserver.Headers> capturedHeaders =
                new AtomicReference<>();

        final MockHttpServerContext context = new MockHttpServerContext.Builder()
                .uri(path)
                .responseStatus(HttpURLConnection.HTTP_OK)
                .responseBody("OK")
                .requestCondition(
                        "Capture request headers",
                        requestCtx -> {
                            capturedHeaders.set(requestCtx.getHeaders());
                            return true;
                        })
                .mustBeCalled()
                .build();

        final MockHttpServer mockHttpServer = new MockHttpServer(mockIp, mockPort);
        mockHttpServer.addContext(context);
        mockHttpServer.start();
        IPUtils.disabledIpPrivateSubnet(true);

        try {
            final boolean valid = APILocator.getTempFileAPI()
                    .validUrl("http://" + mockIp + ":" + mockPort + path);

            assertTrue("validUrl should return true for a 200 response", valid);
            mockHttpServer.validate();

            final com.sun.net.httpserver.Headers received = capturedHeaders.get();
            assertNotNull("Request headers must have been captured", received);

            // Every header in getBrowserHeaders() must have been sent to the server
            for (final String expectedHeader : TempFileAPI.getBrowserHeaders().keySet()) {
                assertTrue(
                        "Outbound request must include header: " + expectedHeader,
                        received.containsKey(expectedHeader));
            }
        } finally {
            mockHttpServer.stop();
            IPUtils.disabledIpPrivateSubnet(false);
        }
    }

}
