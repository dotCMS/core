package com.dotcms.http;

import com.dotcms.rest.EmptyHttpResponse;
import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.dotcms.rest.exception.BadRequestException;
import com.dotcms.util.network.IPUtils;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import io.vavr.Lazy;
import io.vavr.control.Try;
import net.jodah.failsafe.CircuitBreaker;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.FailsafeException;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;

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
    private Header[] responseHeaders;
    private final boolean allowRedirects;

    private static final Lazy<Integer> circuitBreakerMaxConnTotal = Lazy.of(()->Config.getIntProperty("CIRCUIT_BREAKER_MAX_CONN_TOTAL",100));
    private static final Lazy<Boolean> allowAccessToPrivateSubnets = Lazy.of(()->Config.getBooleanProperty("ALLOW_ACCESS_TO_PRIVATE_SUBNETS", false));
    private static final CircuitBreakerConnectionControl circuitBreakerConnectionControl = new CircuitBreakerConnectionControl(circuitBreakerMaxConnTotal.get());
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
    
    
    @VisibleForTesting
    public CircuitBreakerUrl(String proxyUrl, long timeoutMs, CircuitBreaker circuitBreaker, HttpRequestBase request,
            Map<String, String> params, Map<String, String> headers, boolean verbose, final String rawData) {
        this(proxyUrl, timeoutMs, circuitBreaker, request,
                        params, headers,  verbose, rawData, false); 
        
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
            Map<String, String> params, Map<String, String> headers, boolean verbose, final String rawData, boolean allowRedirects) {
        this.proxyUrl = proxyUrl;
        this.timeoutMs = timeoutMs;
        this.circuitBreaker = circuitBreaker;
        this.verbose=verbose;
        this.request = request;
        this.rawData=rawData;
        this.allowRedirects=allowRedirects;
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

    public String doString() throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            doOut(out);
            return new String(out.toByteArray());
        }
    }

    public void doOut(final OutputStream out ) throws IOException {
        doOut(new EmptyHttpResponse() {
            @Override
            public ServletOutputStream getOutputStream() throws IOException {
                return new ServletOutputStream(){

                    @Override
                    public void write(int b) throws IOException {
                        out.write(b);
                    }

                    @Override
                    public boolean isReady() {
                        return false;
                    }

                    @Override
                    public void setWriteListener(WriteListener writeListener) {

                    }
                };
            }
        });
    }

    public void doOut(final HttpServletResponse response) throws IOException {

        circuitBreakerConnectionControl.check(this.proxyUrl);

        try (final OutputStream out = response.getOutputStream()) {

            circuitBreakerConnectionControl.start(Thread.currentThread().getId());

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
                                .setRedirectsEnabled(allowRedirects)
                                .setConnectionRequestTimeout(Math.toIntExact(this.timeoutMs))
                                .setSocketTimeout(Math.toIntExact(this.timeoutMs)).build();
                        try (CloseableHttpClient httpclient = HttpClientBuilder.create()
                                .setMaxConnTotal(circuitBreakerMaxConnTotal.get())
                                .setDefaultRequestConfig(config).build()) {
                            
                            if(IPUtils.isIpPrivateSubnet(this.request.getURI().getHost()) && !allowAccessToPrivateSubnets.get()){
                                throw new DotRuntimeException("Remote HttpRequests cannot access private subnets.  Set ALLOW_ACCESS_TO_PRIVATE_SUBNETS=true to allow");
                            }

                            try (CloseableHttpResponse innerResponse = httpclient.execute(this.request)) {

                                this.responseHeaders = innerResponse.getAllHeaders();

                                copyHeaders(innerResponse, response);

                                this.response = innerResponse.getStatusLine().getStatusCode();

                                IOUtils.copy(innerResponse.getEntity().getContent(), out);
                            }
                            
                            // throw an error if the request is bad
                            if(this.response<200 || this.response>299){
                                throw new BadRequestException("got invalid response for url: " + this.proxyUrl + " response: " + this.response);
                            }
                        }
                    });
        } catch (FailsafeException ee) {

            Logger.debug(this.getClass(), ee.getMessage() + " " + toString());
        } finally {

            circuitBreakerConnectionControl.end(Thread.currentThread().getId());
        }
    }

    private void copyHeaders(final HttpResponse innerResponse, final HttpServletResponse response) {
        final Header contentTypeHeader = innerResponse.getFirstHeader("Content-Type");

        if (UtilMethods.isSet(contentTypeHeader)) {
            response.setHeader(contentTypeHeader.getName(), contentTypeHeader.getValue());
        }

        final Header contentLengthHeader = innerResponse.getFirstHeader("Content-Length");

        if (UtilMethods.isSet(contentLengthHeader)) {
            response.setHeader(contentLengthHeader.getName(), contentLengthHeader.getValue());
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

    final static TypeReference<HashMap<String,Object>> typeRef = new TypeReference<HashMap<String,Object>>() {};


    public Map<String,Object> doMap() {
        return (Map<String,Object>) Try.of(()-> DotObjectMapperProvider.getInstance().getDefaultObjectMapper().readValue(doString(), typeRef)).onFailure(e->Logger.warnAndDebug(CircuitBreakerUrl.class,  e)).getOrElse(HashMap::new);
    }

    public Header[] getResponseHeaders() {
        return responseHeaders;
    }

    public enum Method {
        GET, POST, PUT, DELETE, PATCH;

    }

    public static class CircuitBreakerConnectionControl {

        private final int maxConnTotal;
        private final Set<Long> threadIdConnectionCountSet = ConcurrentHashMap.newKeySet();

        public CircuitBreakerConnectionControl(final int maxConnTotal) {
            this.maxConnTotal = maxConnTotal;
        }

        public void check(final String proxyUrl) {

            if (threadIdConnectionCountSet.size() >= maxConnTotal) {

                Logger.info(this, "The maximum number of connections has been reached, size: " +
                        threadIdConnectionCountSet.size() + ", url: " + proxyUrl);
                throw new RejectedExecutionException("The maximum number of connections has been reached.");
            }
        }

        public void start(final long id) {

            threadIdConnectionCountSet.add(id);
        }

        public void end(final long id) {

            threadIdConnectionCountSet.remove(id);
        }
    }
}
