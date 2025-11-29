package com.dotcms.mock.response;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;

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

    @Override
    public boolean containsHeader(String name) {
        return headerMap.containsKey(name);
    }

    @Override
    public Collection<String> getHeaders(String name) {
        return List.of(headerMap.get(name));
    }

    @Override
    public Collection<String> getHeaderNames() {
        return headerMap.keySet();
    }
}
