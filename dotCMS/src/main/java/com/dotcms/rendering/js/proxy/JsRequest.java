package com.dotcms.rendering.js.proxy;

import com.dotcms.rendering.JsEngineException;
import com.dotcms.rendering.js.JsFormData;
import com.dotmarketing.util.json.JSONObject;
import io.vavr.control.Try;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.proxy.ProxyHashMap;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Serializable;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * Abstraction of the Request for the Javascript engine.
 * @author jsanca
 */
public class JsRequest implements Serializable, JsProxyObject<HttpServletRequest> {

    private boolean bodyUsed = false;

    private final transient HttpServletRequest request;
    //  may be this one has to be moved as a hashmap to make it serializable
    private final transient Map<String, Object> contextParams;
    public JsRequest(final HttpServletRequest request, final Map<String, Object> contextParams) {

        this.request = request;
        this.contextParams = contextParams;
    }

    public HttpServletRequest getRequest() {
        return request;
    }

    @Override
    public HttpServletRequest  getWrappedObject() {
        return this.getRequest();
    }

    @HostAccess.Export
    public String getParameter(final String name) {

        return request.getParameter(name);
    }

    @HostAccess.Export
    public String getBody() {

        final StringBuilder bodyText = new StringBuilder();
        // Get the request's input stream and create a BufferedReader
        try (BufferedReader reader = request.getReader()) {

            // Read the body text from the input stream
            String line;
            while ((line = reader.readLine()) != null) {
                bodyText.append(line);
            }

            bodyUsed = true;
        } catch (IOException e) {

            throw new JsEngineException(e);
        }

        return bodyText.toString();
    }

    @HostAccess.Export
    public boolean getBodyUsed() {

        return Try.of(()->request.getInputStream().isFinished()).getOrElse(bodyUsed);
    }

    @HostAccess.Export
    public ProxyHashMap getHeaders() {

        final Enumeration<String> headerNames = this.request.getHeaderNames();
        final Map<Object, Object> headersMap  = new HashMap<>();
        if (headerNames != null) {
            while (headerNames.hasMoreElements()) {

                final String headerName  = headerNames.nextElement();
                final String headerValue = request.getHeader(headerName);
                headersMap.put(headerName, headerValue);
            }
        }

        return ProxyHashMap.from(headersMap);
    }

    @HostAccess.Export
    public String getMethod() {

        return this.request.getMethod();
    }

    @HostAccess.Export
    public String getReferer() {

        return this.request.getHeader("referer");
    }

    @HostAccess.Export
    public String getUrl() {

        return  this.request.getRequestURL().toString();
    }

    @HostAccess.Export
    public Object getBlob() {

        return Try.of(()->JsProxyFactory.createProxy(this.request.getParts())).getOrNull();
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
    public ProxyHashMap json() {
        return this.getJson();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @HostAccess.Export
    public ProxyHashMap getJson() {

        if (null != this.contextParams && this.contextParams.containsKey("bodyMap")) {
            return  (ProxyHashMap) this.contextParams.get("bodyMap");
        }

        final Map json = new JSONObject(this.getText());
        return ProxyHashMap.from(json);
    }

    @HostAccess.Export
    public String text() {

        return this.getText();
    }

    @HostAccess.Export
    public String getText() {

        return this.getBody();
    }
}
