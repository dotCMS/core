package com.dotcms.rendering.js.fetch;

import com.dotcms.http.CircuitBreakerUrl;
import com.dotcms.rendering.js.JsHeaders;
import com.dotcms.rendering.js.proxy.JsProxyObject;
import org.apache.http.Header;
import org.graalvm.polyglot.HostAccess;

import java.io.Serializable;

public class JsFetchResponse implements Serializable, JsProxyObject<CircuitBreakerUrl.Response<String>> {

    private final CircuitBreakerUrl.Response<String> response;

    public JsFetchResponse(final CircuitBreakerUrl.Response<String> response) {
        this.response = response;
    }

    @Override
    public CircuitBreakerUrl.Response<String> getWrappedObject() {
        return this.response;
    }

    @HostAccess.Export
    public boolean ok() {
        return this.response.getStatusCode() >= 200 && this.response.getStatusCode() <= 299;
    }

    @HostAccess.Export
    public String getBody() {
        return this.response.getResponse();
    }

    @HostAccess.Export
    public int getStatus() {
        return this.response.getStatusCode();
    }

    @HostAccess.Export
    public JsHeaders getHeaders() {

        final Header[] headers = this.response.getResponseHeaders();
        final JsHeaders jsHeaders = new JsHeaders();

        for (final Header header : headers) {

            jsHeaders.append(header.getName(), header.getValue());
        }

        return jsHeaders;
    }
}
