package com.dotcms.rendering.js.proxy;

import com.dotcms.http.CircuitBreakerUrl;
import com.dotcms.rendering.js.JsHeaders;
import com.dotmarketing.util.json.JSONObject;
import org.apache.http.Header;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.proxy.ProxyHashMap;

import java.io.Serializable;
import java.util.Map;

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

    @HostAccess.Export
    public ProxyHashMap getJson() {

        final JSONObject json = new JSONObject(this.getBody());
        return ProxyHashMap.from(json);
    }

    @HostAccess.Export
    public ProxyHashMap json() {
        return this.getJson();
    }
}
