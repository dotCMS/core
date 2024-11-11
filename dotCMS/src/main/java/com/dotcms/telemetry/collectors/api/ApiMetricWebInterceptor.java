package com.dotcms.telemetry.collectors.api;

import com.dotcms.cdi.CDIUtils;
import com.dotcms.filters.interceptor.Result;
import com.dotcms.filters.interceptor.WebInterceptor;
import com.liferay.util.servlet.ServletInputStreamWrapper;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Intercept all the hit to the Endpoint that we wish to track.
 */
public class ApiMetricWebInterceptor implements WebInterceptor {

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
        return new Result.Builder().next()
                .wrap(new RereadInputStreamRequest(req))
                .build();
    }

    @Override
    public boolean afterIntercept(final HttpServletRequest req, final HttpServletResponse res) {
        if (res.getStatus() == HttpServletResponse.SC_OK) {
            ApiMetricTypes.INSTANCE.interceptBy(req)
                    .stream()
                    .filter(apiMetricType -> apiMetricType.shouldCount(req, res))
                    .forEach(apiMetricType ->
                            apiStatAPI.save(apiMetricType, (RereadInputStreamRequest) req)
                    );
        }
        return true;
    }

    public static class RereadInputStreamRequest extends HttpServletRequestWrapper {

        private RereadAfterCloseInputStream servletInputStream;

        public RereadInputStreamRequest(final HttpServletRequest request) {
            super(request);
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            if (servletInputStream == null) {
                servletInputStream = new RereadAfterCloseInputStream(getRequest().getInputStream());
            }

            return servletInputStream;
        }

        public String getReadContent() {
            int contentLength = getRequest().getContentLength();

            if (contentLength > 0) {
                return servletInputStream.getReadContent();
            } else {
                return "";
            }
        }
    }

    /**
     * This InputStream allows us to read the content multiple times as needed. It enables us to
     * read the Request Body several times: once to check if the request is unique, and another time
     * to process the request
     */
    private static class RereadAfterCloseInputStream extends ServletInputStreamWrapper {

        final ByteArrayOutputStream byteArrayOutputStream;
        private final ServletInputStream proxy;

        RereadAfterCloseInputStream(final ServletInputStream proxy) {
            super(proxy);
            this.proxy = proxy;

            byteArrayOutputStream = new ByteArrayOutputStream();
        }

        @Override
        public int read() throws IOException {
            int read = this.proxy.read();

            if (read != -1) {
                byteArrayOutputStream.write(read);
            }

            return read;
        }

        @Override
        public int read(byte[] b) throws IOException {
            int read = this.proxy.read(b);

            if (read != -1) {
                byteArrayOutputStream.write(b);
            }

            return read;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int read = this.proxy.read(b, off, len);

            if (read != -1) {
                byteArrayOutputStream.write(b);
            }

            return read;
        }

        @Override
        public int readLine(byte[] b, int off, int len) throws IOException {
            int read = this.proxy.readLine(b, off, len);

            if (read != -1) {
                byteArrayOutputStream.write(b);
            }

            return read;
        }

        @Override
        public void close() throws IOException {
            super.close();
            byteArrayOutputStream.close();
        }

        public String getReadContent() {

            if (!this.isFinished()) {
                return "";
            }

            return byteArrayOutputStream.toString();
        }

    }

}
