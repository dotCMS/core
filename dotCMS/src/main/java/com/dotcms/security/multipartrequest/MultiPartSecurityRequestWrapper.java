package com.dotcms.security.multipartrequest;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import org.apache.commons.io.IOUtils;
import com.dotmarketing.util.SecurityLogger;

public class MultiPartSecurityRequestWrapper extends HttpServletRequestWrapper {


    final byte[] body;
    final File tmpFile;

    // if greater than 50MB, cache to disk
    final long cacheIfLarger =1024*1000*50;


    boolean shouldCacheToDisk(final HttpServletRequest request) {

        int contentLength = request.getContentLength();
        return contentLength<=0 || contentLength > cacheIfLarger ;

    }


    public MultiPartSecurityRequestWrapper(final HttpServletRequest request) throws IOException {
        super(request);

        if(shouldCacheToDisk(request)) {
            body=null;
            tmpFile =  Files.createTempFile("multipartSec", ".tmp").toFile();
            IOUtils.copy(request.getInputStream(), Files.newOutputStream(tmpFile.toPath()));
            checkFile(tmpFile);
            return;
        }


        tmpFile=null;
        body = IOUtils.toByteArray(super.getInputStream());
        checkMemory(body);

    }

    @Override
    public ServletInputStream getInputStream() throws IOException {

        if(null!=body) {
            return new ServletInputStreamImpl(new ByteArrayInputStream(body));
        }
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

        public ServletInputStreamImpl(InputStream is) {
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

        public synchronized void mark(int i) {
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

    private void checkMemory(byte[] bodyBytes) throws IOException {

        try(InputStream in = new ByteArrayInputStream(bodyBytes)){
            checkSecurityInputStream(in);
        }
    }



    private void checkFile(final File tmpFile) throws IOException {
        try(InputStream in = new BufferedInputStream(Files.newInputStream(tmpFile.toPath()))){
            checkSecurityInputStream(in);
        }

    }

    private void checkSecurityInputStream(final InputStream tmpStream) throws IOException {
        try(BoundedBufferedReader reader = new BoundedBufferedReader(new InputStreamReader(tmpStream, "UTF-8"))){

            String line;
            while ((line = reader.readLine()) != null) {
                testString(line);
            }
        }
    }






    private void testString(String lineToTest) {
        if(null == lineToTest) {
            return;
        }
        lineToTest = lineToTest.length()>1024 ? lineToTest.substring(0,1024) : lineToTest;
        lineToTest = lineToTest.toLowerCase().trim();
        if (!lineToTest.startsWith("content-disposition:") || ! lineToTest.contains("filename=")) {
            return;
        }

        lineToTest = lineToTest.substring(lineToTest.indexOf("filename="), lineToTest.length());


        if (lineToTest.indexOf("/") != -1 || lineToTest.indexOf("\\") != -1 || lineToTest.indexOf("..") != -1) {

            SecurityLogger.logInfo(this.getClass(), "The filename: " + lineToTest + " is invalid");
            throw new IllegalArgumentException("Illegal Request");
        }

    }

}