package com.dotcms.mock.response;

import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

/**
 * Response mock to collect the response status and headers.
 * @author jsanca
 */
public class MockHttpStatusAndHeadersResponse extends MockHttpStatusResponse {

    private final Map<String, String> headerMap = new HashMap<>();
    public MockHttpStatusAndHeadersResponse(final HttpServletResponse response) {
        super(response);
    }

    @Override
    public HttpServletResponse response() {
        return this;
    }

    @Override
    public void addHeader(String name, String value) {
        headerMap.put(name, value);
    }

    @Override
    public String getHeader(String name) {
        return headerMap.get(name);
    }

    @Override
    public void setHeader(String name, String value) {
        headerMap.put(name, value);
    }
}
