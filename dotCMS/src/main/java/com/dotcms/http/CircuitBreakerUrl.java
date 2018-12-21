package com.dotcms.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import com.dotmarketing.business.DotStateException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;

import net.jodah.failsafe.CircuitBreaker;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.FailsafeException;

/**
 * Defaults to GET requests with 2000 timeout
 * @author will
 *
 */
public class CircuitBreakerUrl {

    private final String proxyUrl;
    private final long timeoutMs;
    private final CircuitBreaker circuitBreaker;
    private final HttpRequestBase request;
    /**
     * 
     * @param proxyUrl
     */
    public CircuitBreakerUrl(final String proxyUrl) {
        this(proxyUrl, Config.getIntProperty("URL_CONNECTION_TIMEOUT", 2000));
    }

    /**
     * Timeout value in MS
     * @param proxyUrl
     * @param timeoutMs
     */
    public CircuitBreakerUrl(final String proxyUrl, final long timeoutMs) {
        this(proxyUrl, timeoutMs, CurcuitBreakerPool.getBreaker(proxyUrl + timeoutMs), new HttpGet(proxyUrl), ImmutableMap.of(),
                ImmutableMap.of());
    }
    
    /**
     * Pass in a pre-constructed circuit breaker
     * Timeout value in MS
     * @param proxyUrl
     * @param timeoutMs
     * @param circuitBreaker
     */
    public CircuitBreakerUrl(final String proxyUrl, final long timeoutMs, final CircuitBreaker circuitBreaker) {
        this(proxyUrl, timeoutMs, circuitBreaker, new HttpGet(proxyUrl), ImmutableMap.of(), ImmutableMap.of());
    }

    /**
     * Pass in the String "key" for your circuit breaker, e.g. the url or hostname + params
     * @param proxyUrl
     * @param timeoutMs
     * @param circuitBreakerKey
     */
    public CircuitBreakerUrl(final String proxyUrl, final long timeoutMs, final String circuitBreakerKey) {
        this(proxyUrl, timeoutMs, CurcuitBreakerPool.getBreaker(circuitBreakerKey), new HttpGet(proxyUrl), ImmutableMap.of(),
                ImmutableMap.of());
    }

    /**
     * Full featured constructor
     * 
     * @param proxyUrl
     * @param timeoutMs
     * @param circuitBreaker
     * @param request
     * @param params
     * @param headers
     */
    @VisibleForTesting
    public CircuitBreakerUrl(String proxyUrl, long timeoutMs, CircuitBreaker circuitBreaker, HttpRequestBase request,
            Map<String, String> params, Map<String, String> headers) {
        this.proxyUrl = proxyUrl;
        this.timeoutMs = timeoutMs;
        this.circuitBreaker = circuitBreaker;

        this.request = request;
        for (final String head : headers.keySet()) {
            request.addHeader(head, headers.get(head));
        }
        try {
            URIBuilder uriBuilder = new URIBuilder(this.proxyUrl);
            for (final String param : params.keySet()) {
                uriBuilder.addParameter(param, params.get(param));
            }
            this.request.setURI(uriBuilder.build());
        } catch (URISyntaxException e) {
            throw new DotStateException(e);
        }


    }

    public void doOut(HttpServletResponse response) throws IOException {
        try (OutputStream out = response.getOutputStream()) {
            doOut(out);
        }
    }

    public String doString() throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            doOut(out);
            return new String(out.toByteArray());
        }
    }

    public void doOut(OutputStream outer) throws IOException {
        try (OutputStream out = outer) {
            Logger.info(this.getClass(), "Circuitbreaker to " + request + " is " + circuitBreaker.getState());
            Failsafe.with(circuitBreaker).onSuccess(connection -> Logger.info(this, "Connected to " + this.proxyUrl))
                    .onFailure(failure -> Logger.warn(this, "Connection attempts failed " + failure.getMessage())).run(ctx -> {
                        RequestConfig config = RequestConfig.custom()
                                .setConnectTimeout(Math.toIntExact(this.timeoutMs))
                                .setConnectionRequestTimeout(Math.toIntExact(this.timeoutMs))
                                .setSocketTimeout(Math.toIntExact(this.timeoutMs)).build();
                        try (CloseableHttpClient httpclient = HttpClientBuilder.create().setDefaultRequestConfig(config).build()) {
                            HttpResponse response = httpclient.execute(this.request);
                            IOUtils.copy(response.getEntity().getContent(), out);
                        }
                    });
        } catch (FailsafeException ee) {
            Logger.debug(this.getClass(), ee.getMessage() + " " + toString());
        }
    }


    public static CircuitBreakerBuilder builder() {
        return new CircuitBreakerBuilder();
    }


    @Override
    public String toString() {
        return "CircuitBreakerUrl [proxyUrl=" + proxyUrl + ", timeoutMs=" + timeoutMs + ", circuitBreaker=" + circuitBreaker + "]";
    }


    public enum Method {
        GET, POST, PUT, DELETE

    }


}
