package com.dotcms.rendering.js;

import com.dotmarketing.util.json.JSONObject;
import io.vavr.control.Try;
import org.graalvm.polyglot.HostAccess;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Abstraction of the Request for the Javascript engine.
 * @author jsanca
 */
public class JsHttpRequest {

    private boolean bodyUsed = false;
    private final HttpServletRequest request;
    public JsHttpRequest(final HttpServletRequest request) {
        this.request = request;
    }

    @HostAccess.Export
    public String getParameter(final String name) {
        return request.getParameter(name);
    }

    @HostAccess.Export
    public String getBody() {
        // todo: read the body and returns as a string
        bodyUsed = true;
        return null;
    }

    @HostAccess.Export
    public boolean getBodyUsed() {
        return bodyUsed;
    }

    @HostAccess.Export
    public Map<String, String> getHeaders() {

        final Enumeration<String> headerNames = this.request.getHeaderNames();
        final Map<String, String> headersMap = new HashMap<>();
        if (headerNames != null) {
            while (headerNames.hasMoreElements()) {
                final String headerName = headerNames.nextElement();
                final String headerValue = request.getHeader(headerName);
                headersMap.put(headerName, headerValue);
            }
        }

        return headersMap;
    }

    @HostAccess.Export
    public String getMethod() {

        return this.request.getMethod();
    }

    @HostAccess.Export
    public String getReferrer() {

        final String referrer = this.request.getHeader("referer");
        return referrer;
    }

    @HostAccess.Export
    public String getUrl() {

        final String url = this.request.getRequestURL().toString();
        return url;
    }

    @HostAccess.Export
    public Collection<JsPart> getBlob() {

        return Try.of(()->this.request.getParts().stream().map(JsPart::new).collect(Collectors.toList())).getOrNull();
    }

    @HostAccess.Export
    public JsFormData getFormData() {

        final Map<String, String[]> paramMap = request.getParameterMap();
        final Map<String, String> entries    = new HashMap<>();
        // Iterate through the map to access all parameters
        for (final Map.Entry<String, String[]> entry : paramMap.entrySet()) {
            final String paramName = entry.getKey();

            entries.put(paramName, entry.getValue()[0]);
        }

        return new JsFormData(entries);
    }

    @HostAccess.Export
    public JSONObject getJson() {

        // todo: implement me
        return null;
    }

    @HostAccess.Export
    public String getText() {

        // todo: implement me
        return null;
    }
}
