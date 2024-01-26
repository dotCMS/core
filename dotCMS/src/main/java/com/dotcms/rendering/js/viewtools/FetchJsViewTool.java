package com.dotcms.rendering.js.viewtools;

import com.dotcms.http.CircuitBreakerUrl;
import com.dotcms.http.CircuitBreakerUrlBuilder;
import com.dotcms.rendering.js.JsViewTool;
import com.dotcms.rendering.js.proxy.JsFetchResponse;
import com.dotcms.util.ConversionUtils;
import com.dotmarketing.util.Config;
import org.graalvm.polyglot.HostAccess;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Implementation of the Fetch API in Java for the Js Context
 * @author jsanca
 */
public class FetchJsViewTool implements JsViewTool {

    private final Supplier<CircuitBreakerUrlBuilder> circuitBreakerUrlSupplier;
    private final Map<String, CircuitBreakerUrl.Method> methodMapMapping = Map.of(
            "GET", CircuitBreakerUrl.Method.GET,
            "POST", CircuitBreakerUrl.Method.POST,
            "PUT", CircuitBreakerUrl.Method.PUT,
            "PATCH", CircuitBreakerUrl.Method.PATCH,
            "DELETE", CircuitBreakerUrl.Method.DELETE);

    public FetchJsViewTool() {
        super();
        this.circuitBreakerUrlSupplier = CircuitBreakerUrl::builder;
    }

    @HostAccess.Export
    public JsFetchResponse fetch(final String resource) {
        return fetch(resource, Map.of());
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @HostAccess.Export
    public JsFetchResponse fetch(final String resource, final Map options) {

        final CircuitBreakerUrlBuilder builder = this.circuitBreakerUrlSupplier.get()
                .setMethod(this.getMethod(options))
                .setHeaders(this.getHeaders(options))
                .setUrl(resource)
                .setTimeout(getTimeout(options));

        this.tryBody (builder, options);
        this.tryAllowRedirects (builder, options);
        this.tryParams (builder, options);
        this.tryVerbose (builder, options);

        return new JsFetchResponse(builder.build().doResponse());
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void tryVerbose(CircuitBreakerUrlBuilder builder, Map options) {

            if (options.containsKey("verbose")) {

                builder.setVerbose("true".equalsIgnoreCase(options.get("verbose").toString()));
            }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void tryParams(final CircuitBreakerUrlBuilder builder, final Map options) {

            if (options.containsKey("params")) {

                builder.setParams((Map)options.get("params"));
            }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void tryAllowRedirects(final CircuitBreakerUrlBuilder builder, final Map options) {

            if (options.containsKey("redirect")) {

                builder.setAllowRedirects("follow".equalsIgnoreCase(options.get("redirect").toString()));
            }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void tryBody(final CircuitBreakerUrlBuilder builder, final Map options) {

        if (options.containsKey("body")) {

            builder.setRawData(options.get("body").toString());
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private CircuitBreakerUrl.Method getMethod(final Map options) {

        final String methodName = options.getOrDefault("method", "GET").toString();
        return methodMapMapping.getOrDefault(methodName, CircuitBreakerUrl.Method.GET);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private long getTimeout(final Map options) {

        return ConversionUtils.toLong(options.getOrDefault("timeout",
                Config.getLongProperty("URL_CONNECTION_TIMEOUT", 2000l)), 2000L);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private Map<String, String> getHeaders(final Map options) {
        return options.containsKey("headers")?
                (Map)options.get("headers"):
                Map.of();
    }

    @Override
    public String getName() {
        return "fetchtool";
    }
}
