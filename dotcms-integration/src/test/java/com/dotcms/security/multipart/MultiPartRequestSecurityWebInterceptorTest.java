package com.dotcms.security.multipart;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.filters.interceptor.Result;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.util.ConfigUtils;
import java.io.File;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Checks that {@link MultiPartRequestSecurityWebInterceptor} wraps every multipart upload in
 * {@link MultiPartSecurityRequestWrapper}, which checks the part file names, whatever the letter
 * case of the {@code Content-Type} header, and leaves other requests alone. The wrapper reads the
 * body in memory or from a file under the assets temp directory depending on the content length,
 * so both paths are covered.
 */
public class MultiPartRequestSecurityWebInterceptorTest {

    private static final String BOUNDARY = "boundary";

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Given a POST whose Content-Type is the usual lower-case {@code multipart/form-data} and a
     * plain file name, the interceptor wraps the request and lets it continue.
     */
    @Test
    public void test_intercept_lowerCaseMultipart_wrapsRequest() throws IOException {
        final Result result = new MultiPartRequestSecurityWebInterceptor().intercept(
                multipartRequest("POST", "multipart/form-data; boundary=" + BOUNDARY, "report.txt"),
                mock(HttpServletResponse.class));

        assertSame(Result.NEXT.getType(), result.getType());
        assertTrue(result.getRequest() instanceof MultiPartSecurityRequestWrapper);
    }

    /**
     * Given a POST whose Content-Type is spelled {@code Multipart/Form-Data}, which HTTP treats
     * as the same media type, the interceptor still wraps the request.
     */
    @Test
    public void test_intercept_capitalisedMultipart_wrapsRequest() throws IOException {
        final Result result = new MultiPartRequestSecurityWebInterceptor().intercept(
                multipartRequest("POST", "Multipart/Form-Data; boundary=" + BOUNDARY, "report.txt"),
                mock(HttpServletResponse.class));

        assertSame(Result.NEXT.getType(), result.getType());
        assertTrue(result.getRequest() instanceof MultiPartSecurityRequestWrapper);
    }

    /**
     * Given a lower-case multipart POST whose part file name is not a plain file name, the
     * wrapper refuses the request.
     */
    @Test(expected = IllegalArgumentException.class)
    public void test_intercept_lowerCaseMultipart_disallowedFileName_isRejected() throws IOException {
        new MultiPartRequestSecurityWebInterceptor().intercept(
                multipartRequest("POST", "multipart/form-data; boundary=" + BOUNDARY, "../../outside-marker/x.txt"),
                mock(HttpServletResponse.class));
    }

    /**
     * Given a capitalised multipart PUT whose part file name is not a plain file name, the
     * wrapper refuses the request the same way it does for the lower-case spelling.
     */
    @Test(expected = IllegalArgumentException.class)
    public void test_intercept_capitalisedMultipart_disallowedFileName_isRejected() throws IOException {
        new MultiPartRequestSecurityWebInterceptor().intercept(
                multipartRequest("PUT", "MULTIPART/FORM-DATA; boundary=" + BOUNDARY, "../../outside-marker/x.txt"),
                mock(HttpServletResponse.class));
    }

    /**
     * Given a multipart POST whose content length is unknown, the wrapper keeps the body in a
     * file under the assets temp directory instead of in memory; the part file names are checked
     * on that path too, so a name that is not a plain file name is still refused.
     */
    @Test(expected = IllegalArgumentException.class)
    public void test_intercept_unknownContentLength_disallowedFileName_isRejected() throws IOException {
        new File(ConfigUtils.getAssetTempPath()).mkdirs();
        final HttpServletRequest request = multipartRequest("POST",
                "multipart/form-data; boundary=" + BOUNDARY, "../../outside-marker/x.txt");
        when(request.getContentLength()).thenReturn(-1);

        new MultiPartRequestSecurityWebInterceptor().intercept(request, mock(HttpServletResponse.class));
    }

    /**
     * Given a multipart POST whose content length is unknown and whose part file name is plain,
     * the file-backed path lets the request continue.
     */
    @Test
    public void test_intercept_unknownContentLength_plainFileName_isWrapped() throws IOException {
        new File(ConfigUtils.getAssetTempPath()).mkdirs();
        final HttpServletRequest request = multipartRequest("POST",
                "multipart/form-data; boundary=" + BOUNDARY, "report.txt");
        when(request.getContentLength()).thenReturn(-1);

        final Result result = new MultiPartRequestSecurityWebInterceptor().intercept(request,
                mock(HttpServletResponse.class));
        assertTrue(result.getRequest() instanceof MultiPartSecurityRequestWrapper);
    }

    /**
     * Given a GET, even with a multipart Content-Type, the interceptor does not wrap anything.
     */
    @Test
    public void test_intercept_get_isNotWrapped() throws IOException {
        final Result result = new MultiPartRequestSecurityWebInterceptor().intercept(
                multipartRequest("GET", "multipart/form-data; boundary=" + BOUNDARY, "report.txt"),
                mock(HttpServletResponse.class));

        assertSame(Result.NEXT, result);
    }

    /**
     * Given a POST that is not multipart, or has no Content-Type at all, the interceptor does
     * not wrap anything.
     */
    @Test
    public void test_intercept_postWithoutMultipart_isNotWrapped() throws IOException {
        final MultiPartRequestSecurityWebInterceptor interceptor = new MultiPartRequestSecurityWebInterceptor();

        assertSame(Result.NEXT, interceptor.intercept(
                multipartRequest("POST", "application/json", "report.txt"), mock(HttpServletResponse.class)));
        assertSame(Result.NEXT, interceptor.intercept(
                multipartRequest("POST", null, "report.txt"), mock(HttpServletResponse.class)));
        assertFalse(interceptor.intercept(multipartRequest("POST", "text/plain", "report.txt"),
                mock(HttpServletResponse.class)).getRequest() instanceof MultiPartSecurityRequestWrapper);
    }

    /**
     * Builds a request with a one-part multipart body naming the given file. The content length
     * is set to the body size so the wrapper inspects the body in memory.
     */
    private static HttpServletRequest multipartRequest(final String method, final String contentType,
            final String fileName) throws IOException {
        final String body = "--" + BOUNDARY + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"\r\n"
                + "Content-Type: text/plain\r\n\r\n"
                + "hello\r\n"
                + "--" + BOUNDARY + "--\r\n";
        final byte[] bytes = body.getBytes(StandardCharsets.UTF_8);

        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn(method);
        when(request.getHeader("content-type")).thenReturn(contentType);
        when(request.getContentType()).thenReturn(contentType);
        when(request.getContentLength()).thenReturn(bytes.length);
        when(request.getInputStream()).thenReturn(new MemoryServletInputStream(new ByteArrayInputStream(bytes)));
        return request;
    }

    /**
     * Minimal {@link ServletInputStream} over a byte array, enough for the wrapper to read a body.
     */
    private static final class MemoryServletInputStream extends ServletInputStream {

        private final InputStream delegate;

        private MemoryServletInputStream(final InputStream delegate) {
            this.delegate = delegate;
        }

        @Override
        public int read() throws IOException {
            return delegate.read();
        }

        @Override
        public boolean isFinished() {
            return false;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(final ReadListener readListener) {
            // not needed for an in-memory stream
        }
    }
}
