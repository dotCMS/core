package com.dotcms.http;

import com.dotcms.rest.exception.BadRequestException;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;
import net.jodah.failsafe.CircuitBreaker;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.FailsafeException;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

/**
 * Defaults to GET requests with 2000 timeout
 * <p>
 * Usage:
 * <pre>
 *     String pageString = CircuitBreakerUrl.builder()
 *           .setUrl("https://google.com")
 *           .setHeaders(ImmutableMap.of("X-CUSTOM-HEADER", "TESTING"))
 *           .setMethod(CircuitBreakerUrl.Method.POST)
 *           .setParams(ImmutableMap.of("param1", "12345"))
 *           .setTimeout(2000)
 *           .build()
 *           .doString();
 *  </pre>
 * </p>
 *
 * @author will
 */
public class CircuitBreakerUrl {

    private final String proxyUrl;
    private final long timeoutMs;
    private final CircuitBreaker circuitBreaker;
    private final HttpRequestBase request;
    private final boolean verbose;
    private final String rawData;
    private int response=-1;
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
        this(proxyUrl, timeoutMs, CurcuitBreakerPool.getBreaker(proxyUrl + timeoutMs), false);
    }
    
    /**
     * Pass in a pre-constructed circuit breaker
     * Timeout value in MS
     * @param proxyUrl
     * @param timeoutMs
     * @param circuitBreaker
     */
    public CircuitBreakerUrl(final String proxyUrl, final long timeoutMs, final CircuitBreaker circuitBreaker) {
        this(proxyUrl, timeoutMs, circuitBreaker, false);
    }

    public CircuitBreakerUrl(String proxyUrl, long timeoutMs, CircuitBreaker circuitBreaker, boolean verbose) {
      this(proxyUrl, timeoutMs, circuitBreaker, new HttpGet(proxyUrl), ImmutableMap.of(),ImmutableMap.of(), verbose, null);
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
            Map<String, String> params, Map<String, String> headers, boolean verbose, final String rawData) {
        this.proxyUrl = proxyUrl;
        this.timeoutMs = timeoutMs;
        this.circuitBreaker = circuitBreaker;
        this.verbose=verbose;
        this.request = request;
        this.rawData=rawData;
        for (final String head : headers.keySet()) {
            request.addHeader(head, headers.get(head));
        }
        try {
            final URIBuilder uriBuilder = new URIBuilder(this.proxyUrl);
            if(this.rawData!=null) {
              try {
                final String contentType = this.rawData.trim().charAt(0)=='{' ? "application/json" : this.rawData.trim().startsWith("<") ? "application/xml" : "application/x-www-form-urlencoded";
                
                final StringEntity postingString = new StringEntity(rawData, ContentType.create(contentType, "UTF-8"));
                if(request instanceof HttpEntityEnclosingRequestBase) {
                  ((HttpEntityEnclosingRequestBase)request).setEntity(postingString);
                }
              }
              catch(Exception e) {
                Logger.warnAndDebug(this.getClass(), "unable to set rawData",e);
              }
            }
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
            if(verbose) {
                Logger.info(this.getClass(), "Circuitbreaker to " + request + " is " + circuitBreaker.getState());
            }
            Failsafe.with(circuitBreaker)
                    .onSuccess(connection -> { 
                        if(verbose) Logger.info(this, "success to " + this.proxyUrl);
                    })
                    .onFailure(failure -> Logger.warn(this, "Connection attempts failed " + failure.getMessage()))
                    .run(ctx -> {
                        RequestConfig config = RequestConfig.custom()
                                .setConnectTimeout(Math.toIntExact(this.timeoutMs))
                                .setConnectionRequestTimeout(Math.toIntExact(this.timeoutMs))
                                .setSocketTimeout(Math.toIntExact(this.timeoutMs)).build();
                        try (CloseableHttpClient httpclient = HttpClientBuilder.create().setDefaultRequestConfig(config).build()) {
                            HttpResponse response = httpclient.execute(this.request);
                            this.response = response.getStatusLine().getStatusCode();
                            switch (this.response){
                                case 200:
                                    IOUtils.copy(response.getEntity().getContent(), out);
                                    break;
                                default:
                                    throw new BadRequestException("got invalid response for url: " + this.proxyUrl + " response:" + this.response);
                            }
                        }
                    });
        } catch (FailsafeException ee) {
            Logger.debug(this.getClass(), ee.getMessage() + " " + toString());
        }
    }

    public int response() {
      return this.response;
    }

    public static CircuitBreakerUrlBuilder builder() {
        return new CircuitBreakerUrlBuilder();
    }


    @Override
    public String toString() {
        return "CircuitBreakerUrl [proxyUrl=" + proxyUrl + ", timeoutMs=" + timeoutMs + ", circuitBreaker=" + circuitBreaker + "]";
    }


    public enum Method {
        GET, POST, PUT, DELETE

    }


}
