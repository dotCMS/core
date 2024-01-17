package com.dotcms.security.multipart;

import com.dotcms.exception.ExceptionUtil;
import com.dotcms.util.SecurityUtils;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import io.vavr.Lazy;
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
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * This request wrapper prevents DoS attacks on multipart requests
 * @author jsanca
 */
public class MultiPartSecurityRequestWrapper extends HttpServletRequestWrapper {

    private static final File tmpdir = Path.of(System.getProperty("java.io.tmpdir")).toFile();

    private final byte[] body;
    private final File tmpFile;

    private static final SecurityUtils securityUtils = new SecurityUtils();

    /** By default, we'll always cache files greater than 50MB to disk */
    private static final Lazy<Long> CACHE_IF_LARGER = Lazy.of(() -> Config.getLongProperty(
            "MULTI_PART_CACHE_IF_LARGER", (long) 1024 * 1000 * 50));

    boolean shouldCacheToDisk(final HttpServletRequest request) {

        final int contentLength = request.getContentLength();
        return contentLength <= 0 || contentLength > CACHE_IF_LARGER.get();
    }

    public MultiPartSecurityRequestWrapper(final HttpServletRequest request) throws IOException {

        super(request);

        Logger.debug(this, "Creating a MultiPartSecurityRequestWrapper");
        if(this.shouldCacheToDisk(request)) {

            Logger.debug(this, String.format("File being uploaded is greater than %d bytes. It must be cached to disk...", CACHE_IF_LARGER.get()));
            this.body = null;
            final Path tempFilePath = Files.createTempFile(Path.of(ConfigUtils.getAssetTempPath(),File.separator),"multipartSec", ".tmp");
            this.tmpFile = tempFilePath.toFile();
            // security demands we add this check here
            if (tmpFile.getCanonicalPath().startsWith(new File(ConfigUtils.getAssetTempPath()).getCanonicalPath())) {
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
            try (final InputStream in = new BufferedInputStream(Files.newInputStream(tmpFile.toPath()))) {
                checkSecurityInputStream(in);
            } catch (final Exception e) {
                Logger.warn(this.getClass(), String.format("Could not check the path for file " +
                        "'%s': %s", tmpFile.getCanonicalPath(), ExceptionUtil.getErrorMessage(e)), e);
                throw e;
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

    private static final String[] checkPatterns  = {"content-disposition","form-data","filename"};

    private void testString(final String lineToTest) {

        final String lineToTestLower = lineToTest.toLowerCase();
        for (String p : checkPatterns) {
            if (!lineToTestLower.contains(p)) {
                return;
            }
        }

        final String fileName = ContentDispositionFileNameParser.parse(lineToTestLower);
        if (fileName == null) {
            return;
        }
        securityUtils.validateFile(fileName);
    }
}
