package com.dotcms.security.multipart;

import com.dotcms.util.SecurityUtils;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.google.common.collect.ImmutableList;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import org.apache.commons.io.IOUtils;

/**
 * This request wrapper prevents DoS attacks on multipart requests
 * @author jsanca
 */
public class MultiPartSecurityRequestWrapper extends HttpServletRequestWrapper {

    private static final File tmpdir = Path.of(System.getProperty("java.io.tmpdir")).toFile();

    private final byte[] body;
    private final File tmpFile;

    private static final SecurityUtils securityUtils = new SecurityUtils();

    // if greater than 50MB, cache to disk
    private static final long CACHE_IF_LARGER = Config.getIntProperty("MULTI_PART_CACHE_IF_LARGER",1024*1000*50);

    boolean shouldCacheToDisk(final HttpServletRequest request) {

        final int contentLength = request.getContentLength();
        return contentLength <= 0 || contentLength > CACHE_IF_LARGER;
    }

    public MultiPartSecurityRequestWrapper(final HttpServletRequest request) throws IOException {

        super(request);

        Logger.debug(this, ()-> "Creating a MultiPartSecurityRequestWrapper");
        if(this.shouldCacheToDisk(request)) {

            Logger.debug(this, ()-> "Should Cache To Disk...");
            this.body = null;
            final Path tempFilePath = Files.createTempFile(tmpdir.toPath(),"multipartSec", ".tmp");
            this.tmpFile = tempFilePath.toFile();
            // security demands we add this check here
            if (tmpFile.getCanonicalPath().startsWith(tmpdir.getCanonicalPath())) {
                try (final OutputStream outputStream = Files.newOutputStream(tempFilePath)) {
                    IOUtils.copy(request.getInputStream(), outputStream);
                    this.checkFile(tmpFile);
                }
            }
            return;
        }

        this.tmpFile = null;
        this.body = IOUtils.toByteArray(super.getInputStream());
        this.checkMemory(this.body);
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {

        if(null != body) {

            Logger.debug(this, ()-> "Has a body precached");
            return new ServletInputStreamImpl(new ByteArrayInputStream(body));
        }

        Logger.debug(this, ()-> "Streaming the body from file system");
        return new ServletInputStreamImpl(new BufferedInputStream(Files.newInputStream(tmpFile.toPath())));
    }

    @Override
    public BufferedReader getReader() throws IOException {

        String enc = getCharacterEncoding();
        if (enc == null) {

            enc = "UTF-8";
        }

        return new BufferedReader(new InputStreamReader(getInputStream(), enc));
    }

    private static class ServletInputStreamImpl extends ServletInputStream {

        private final InputStream is;
        private boolean closed = false;

        public ServletInputStreamImpl(final InputStream is) {

            this.is = is;
        }

        @Override
        public void close() throws IOException {

            this.is.close();
            this.closed = true;
        }

        public int read() throws IOException {

            return is.read();
        }

        @Override
        public boolean markSupported() {

            return this.is.markSupported();
        }

        @Override
        public synchronized void mark(final int i) {

            this.is.mark(i);
        }

        @Override
        public synchronized void reset() throws IOException {

            this.is.reset();
        }

        @Override
        public boolean isFinished() {

            return this.closed;
        }

        @Override
        public boolean isReady() {

            return false;
        }

        @Override
        public void setReadListener(ReadListener readListener) {
            //Override on purpose to disable listeners
        }
    }

    private void checkMemory(final byte[] bodyBytes) throws IOException {

        try(InputStream in = new ByteArrayInputStream(bodyBytes)) {

            checkSecurityInputStream(in);
        }
    }

    private void checkFile(final File tmpFile) throws IOException {
        //Even though this is might seem redundant. Security demands we check paths everytime we manipulate file paths
        if (tmpFile.getCanonicalPath().startsWith(tmpdir.getCanonicalPath())) {
            try (InputStream in = new BufferedInputStream(Files.newInputStream(tmpFile.toPath()))) {
                checkSecurityInputStream(in);
            }
        }
    }

    private void checkSecurityInputStream(final InputStream tmpStream) throws IOException {

        try(BoundedBufferedReader reader = new BoundedBufferedReader(new InputStreamReader(tmpStream, StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {

                testString(line);
            }
        }
    }

    private void testString(final String lineToTest) {

        final String lineToTestLower = lineToTest.toLowerCase();

        if (!lineToTestLower.contains("content-disposition:") || ! lineToTestLower.contains("filename=")) {

            return;
        }

        final String fileName = ContentDispositionFileNameParser.parse(lineToTestLower);
        securityUtils.validateFile(fileName);
    }
}
