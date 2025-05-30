package com.dotcms.telemetry.collectors.api;

import com.dotcms.cdi.CDIUtils;
import com.dotcms.filters.interceptor.Result;
import com.dotcms.filters.interceptor.WebInterceptor;
import com.dotcms.rest.api.v1.HTTPMethod;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.WebKeys;
import com.liferay.util.Xss;
import org.apache.commons.lang.StringUtils;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Intercept all the hit to the Endpoint that we wish to track.
 */
public class ApiMetricWebInterceptor implements WebInterceptor {

    /**
     * Maximum size of request content to buffer (1MB)
     */
    private static final int MAX_REQUEST_BUFFER_SIZE = 1024 * 1024;

    public static final ApiMetricAPI apiStatAPI = CDIUtils.getBeanThrows(ApiMetricAPI.class);

    public ApiMetricWebInterceptor() {
        ApiMetricFactorySubmitter.INSTANCE.start();
    }

    @Override
    public String[] getFilters() {
        return ApiMetricTypes.INSTANCE.get().stream()
                .map(ApiMetricType::getAPIUrl)
                .toArray(String[]::new);
    }

    @Override
    public Result intercept(HttpServletRequest req, HttpServletResponse res) throws IOException {
        final String requestURI = req.getRequestURI();
        
        // Only process API requests
        if (!requestURI.contains("/api/")) {
            return new Result.Builder().next().build();
        }

        // Only wrap requests for supported methods to avoid unnecessary overhead
        final String method = req.getMethod();
        if (ApiMetricTypes.INSTANCE.isMethodSupported(method)) {
            return new Result.Builder().next()
                    .wrap(new RereadInputStreamRequest(req))
                    .build();
        } else {
            return new Result.Builder().next().build();
        }
    }

    private boolean isSupportedMethod(String method) {
        try {
            return ApiMetricTypes.INSTANCE.isMethodSupported(method);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean afterIntercept(final HttpServletRequest req, final HttpServletResponse res) {
        // Only process successful requests to reduce overhead
        if (res.getStatus() == HttpServletResponse.SC_OK && req instanceof RereadInputStreamRequest) {
            try {
                ApiMetricTypes.INSTANCE.interceptBy(req)
                        .stream()
                        .filter(apiMetricType -> apiMetricType.shouldCount(req, res))
                        .forEach(apiMetricType ->
                                apiStatAPI.save(apiMetricType, (RereadInputStreamRequest) req)
                        );
            } catch (Exception e) {
                // Don't let telemetry issues affect the main application
                Logger.debug(this, "Error processing telemetry for request " + req.getRequestURI() + ": " + e.getMessage(), e);
            }
        }
        return true;
    }

    public static class RereadInputStreamRequest extends HttpServletRequestWrapper {

        private ByteArrayOutputStream byteArrayOutputStream;
        private RereadAfterCloseInputStream inputStream;
        private boolean contentRead = false;

        public RereadInputStreamRequest(final HttpServletRequest request) {
            super(request);
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            if (null == this.inputStream) {
                // Get the original input stream
                final ServletInputStream originalInputStream = super.getInputStream();
                
                // Check if we should buffer the content
                final long contentLength = getContentLength();
                boolean shouldBuffer = contentLength > 0 && contentLength < MAX_REQUEST_BUFFER_SIZE;
                
                // Create wrapping stream
                if (shouldBuffer) {
                    this.byteArrayOutputStream = new ByteArrayOutputStream();
                    this.inputStream = new RereadAfterCloseInputStream(originalInputStream, byteArrayOutputStream, this);
                } else {
                    // For large requests, just pass through the original stream without buffering
                    this.inputStream = new RereadAfterCloseInputStream(originalInputStream, null, this);
                }
            }
            return this.inputStream;
        }

        /**
         * Mark content as read for proper cleanup
         */
        void markContentRead() {
            this.contentRead = true;
        }

        /**
         * Clean up any resources
         */
        @Override
        protected void finalize() throws Throwable {
            try {
                // Close and release resources if not already read
                if (!contentRead && byteArrayOutputStream != null) {
                    byteArrayOutputStream.close();
                    byteArrayOutputStream = null;
                }
            } finally {
                super.finalize();
            }
        }

        /**
         * This class manages reading and buffering an input stream, allowing it to be re-read after closing
         */
        private class RereadAfterCloseInputStream extends ServletInputStream {

            private final ServletInputStream originalInputStream;
            private final ByteArrayOutputStream byteArrayOutputStream;
            private final RereadInputStreamRequest request;
            private ByteArrayInputStream byteArrayInputStream;
            private boolean bufferingCompleted = false;

            RereadAfterCloseInputStream(
                    final ServletInputStream originalInputStream,
                    final ByteArrayOutputStream byteArrayOutputStream,
                    final RereadInputStreamRequest request) {
                this.originalInputStream = originalInputStream;
                this.byteArrayOutputStream = byteArrayOutputStream;
                this.request = request;
            }

            private boolean isBuffering() {
                return byteArrayOutputStream != null && !bufferingCompleted;
            }

            @Override
            public int read() throws IOException {
                if (bufferingCompleted) {
                    return byteArrayInputStream.read();
                }

                final int value = originalInputStream.read();
                if (isBuffering() && value != -1) {
                    byteArrayOutputStream.write(value);
                } else if (isBuffering() && value == -1) {
                    // Reading completed, prepare for reread
                    bufferingCompleted = true;
                    byteArrayInputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
                    request.markContentRead();
                }
                return value;
            }

            @Override
            public int read(final byte[] b, final int off, final int len) throws IOException {
                if (bufferingCompleted) {
                    return byteArrayInputStream.read(b, off, len);
                }

                final int result = originalInputStream.read(b, off, len);
                if (isBuffering() && result > 0) {
                    byteArrayOutputStream.write(b, off, result);
                } else if (isBuffering() && result == -1) {
                    // Reading completed, prepare for reread
                    bufferingCompleted = true;
                    byteArrayInputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
                    request.markContentRead();
                }
                return result;
            }

            @Override
            public void close() throws IOException {
                if (isBuffering() && !bufferingCompleted) {
                    // Buffer wasn't fully read - ensure we have the content for reread
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = originalInputStream.read(buffer)) != -1) {
                        byteArrayOutputStream.write(buffer, 0, bytesRead);
                        // Stop if we exceed the maximum buffer size
                        if (byteArrayOutputStream.size() >= MAX_REQUEST_BUFFER_SIZE) {
                            break;
                        }
                    }
                    bufferingCompleted = true;
                    byteArrayInputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
                    request.markContentRead();
                }
                originalInputStream.close();
            }

            @Override
            public boolean isFinished() {
                return bufferingCompleted || originalInputStream.isFinished();
            }

            @Override
            public boolean isReady() {
                return bufferingCompleted || originalInputStream.isReady();
            }

            @Override
            public void setReadListener(ReadListener readListener) {
                originalInputStream.setReadListener(readListener);
            }
        }
    }
}
