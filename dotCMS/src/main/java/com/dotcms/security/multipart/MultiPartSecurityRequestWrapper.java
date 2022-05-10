package com.dotcms.security.multipart;

import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.google.common.collect.ImmutableList;
import org.apache.commons.io.IOUtils;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.List;

/**
 * This request wrapper prevents DoS attacks on multipart requests
 * @author jsanca
 */
public class MultiPartSecurityRequestWrapper extends HttpServletRequestWrapper {

    private final byte[] body;
    private final File tmpFile;

    private static final List<SecureFileValidator> secureFileValidatorList = new ImmutableList.Builder<SecureFileValidator>()
            .add(new IllegalTraversalFilePathValidator()).add(new IllegalFileExtensionsValidator()).build();

    // if greater than 50MB, cache to disk
    private final static long CACHE_IF_LARGER = Config.getIntProperty("MULTI_PART_CACHE_IF_LARGER",1024*1000*50);

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
            this.tmpFile = Files.createTempFile("multipartSec", ".tmp").toFile();
            IOUtils.copy(request.getInputStream(), Files.newOutputStream(tmpFile.toPath()));
            this.checkFile(tmpFile);
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

    private class ServletInputStreamImpl extends ServletInputStream {

        private final InputStream is;
        private boolean closed = false;

        public ServletInputStreamImpl(final InputStream is) {

            this.is = is;
        }

        public void close() throws IOException {

            this.is.close();
            this.closed = true;
        }

        public int read() throws IOException {

            return is.read();
        }

        public boolean markSupported() {

            return this.is.markSupported();
        }

        public synchronized void mark(final int i) {

            this.is.mark(i);
        }

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

        }
    }

    private void checkMemory(final byte[] bodyBytes) throws IOException {

        try(InputStream in = new ByteArrayInputStream(bodyBytes)) {

            checkSecurityInputStream(in);
        }
    }

    private void checkFile(final File tmpFile) throws IOException {

        try(InputStream in = new BufferedInputStream(Files.newInputStream(tmpFile.toPath()))) {

            checkSecurityInputStream(in);
        }
    }

    private void checkSecurityInputStream(final InputStream tmpStream) throws IOException {

        try(BoundedBufferedReader reader = new BoundedBufferedReader(new InputStreamReader(tmpStream, "UTF-8"))) {

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

        for (final SecureFileValidator secureFileValidator : this.secureFileValidatorList) {

            secureFileValidator.validate(fileName);
        }
    }
}
