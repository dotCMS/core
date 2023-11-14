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

   // private final Context context;
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

    @HostAccess.Export
    public JsFetchResponse fetch(final String resource, final Map options) {

        final CircuitBreakerUrlBuilder builder = this.circuitBreakerUrlSupplier.get()
                .setMethod(this.getMethod(options))
                .setHeaders(this.getHeaders(options))
                .setUrl(resource)
                .setTimeout(getTimeout(options));

        // most of these values may be handled by headers
        // todo: mode do not supported
        // todo: credentials do not supported
        // todo: cache do not supported
        // todo: referrer  and referrerPolicy do not supported
        // todo: the keep alive is not supported


        this.tryBody (builder, options);
        this.tryAllowRedirects (builder, options);
        this.tryParams (builder, options);
        this.tryVerbose (builder, options);

        return new JsFetchResponse(builder.build().doResponse());
        /*final CompletableFuture<JsFetchResponse> responseFuture = CompletableFuture.supplyAsync(()-> {
            try {
                return new JsFetchResponse(circuitBreakerUrl.doResponse());
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        });

        return wrapPromise(responseFuture);*/
    }

   /* private Value wrapPromise(final CompletableFuture<JsFetchResponse> responseFuture) {

        final Value global = context.getBindings("js");
        final Value promiseConstructor = global.getMember("Promise");
        return promiseConstructor.newInstance((ProxyExecutable) arguments -> {
            final Value resolve = arguments[0];
            final Value reject = arguments[1];
            responseFuture.whenComplete((result, ex) -> {
                if (result != null) {
                    resolve.execute(result);
                } else {
                    reject.execute(ex);
                }
            });
            return null;
        });
    }*/

    private void tryVerbose(CircuitBreakerUrlBuilder builder, Map options) {

            if (options.containsKey("verbose")) {

                builder.setVerbose("true".equalsIgnoreCase(options.get("verbose").toString()));
            }
    }

    private void tryParams(final CircuitBreakerUrlBuilder builder, final Map options) {

            if (options.containsKey("params")) {

                builder.setParams((Map)options.get("params"));
            }
    }

    private void tryAllowRedirects(final CircuitBreakerUrlBuilder builder, final Map options) {

            if (options.containsKey("redirect")) {

                builder.setAllowRedirects("follow".equalsIgnoreCase(options.get("redirect").toString()));
            }
    }

    private void tryBody(final CircuitBreakerUrlBuilder builder, final Map options) {

        if (options.containsKey("body")) {

            builder.setRawData(options.get("body").toString());
        }
    }

    private CircuitBreakerUrl.Method getMethod(final Map options) {

        final String methodName = options.getOrDefault("method", "GET").toString();
        return methodMapMapping.getOrDefault(methodName, CircuitBreakerUrl.Method.GET);
    }

    private long getTimeout(final Map options) {

        return ConversionUtils.toLong(options.getOrDefault("timeout",
                Config.getLongProperty("URL_CONNECTION_TIMEOUT", 2000l)), 2000L);
    }

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
