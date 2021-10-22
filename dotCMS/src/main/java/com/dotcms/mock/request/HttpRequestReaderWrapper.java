package com.dotcms.mock.request;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.UtilMethods;
import com.google.common.io.CharStreams;


/**
 * Proxy for HTTPServletRequest
 *
 */
public class HttpRequestReaderWrapper extends HttpServletRequestWrapper {


    final String rawRequest;

    public HttpRequestReaderWrapper(HttpServletRequest request) {
        super(request);

        try {
            try (BufferedReader reader = request.getReader()) {
                rawRequest = CharStreams.toString(reader);
            }

        } catch (Exception e) {
            throw new DotRuntimeException(e);
        }
    }

    @Override
    public BufferedReader getReader() throws IOException {

        return new BufferedReader(new StringReader(rawRequest));
    }


    public Optional<String> getRawRequest() {


        final String rawRequest = UtilMethods.isSet(this.rawRequest) ? this.rawRequest.trim() : null;

        return Optional.ofNullable(rawRequest);


    }


}
