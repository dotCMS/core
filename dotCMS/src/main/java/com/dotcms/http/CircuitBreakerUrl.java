package com.dotcms.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;

import net.jodah.failsafe.CircuitBreaker;
import net.jodah.failsafe.CircuitBreakerOpenException;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.FailsafeException;


public class CircuitBreakerUrl {

    private final String proxyUrl;
    private final long timeout;
    private final CircuitBreaker circuitBreaker;

    private final HttpRequestBase request;

    public CircuitBreakerUrl(String forwardUrl) {
        this(forwardUrl, Config.getIntProperty("URL_CONNECTION_TIMEOUT", 2000));
    }

    public CircuitBreakerUrl(String forwardUrl, long timeout) {
        this(forwardUrl, timeout, CurcuitBreakerPool.getBreaker(forwardUrl + timeout), new HttpGet(forwardUrl), ImmutableMap.of(),
                ImmutableMap.of());
    }

    public CircuitBreakerUrl(String forwardUrl, long timeout, CircuitBreaker circuitBreaker) {
        this(forwardUrl, timeout, circuitBreaker, new HttpGet(forwardUrl), ImmutableMap.of(), ImmutableMap.of());
    }

    public CircuitBreakerUrl(String forwardUrl, long timeout, String circuitBreakerKey) {
        this(forwardUrl, timeout, CurcuitBreakerPool.getBreaker(circuitBreakerKey), new HttpGet(forwardUrl), ImmutableMap.of(),
                ImmutableMap.of());
    }


    @VisibleForTesting
    public CircuitBreakerUrl(String forwardUrl, long timeout, CircuitBreaker circuitBreaker, HttpRequestBase request,
            Map<String, String> params, Map<String, String> headers) {
        this.proxyUrl = forwardUrl;
        this.timeout = timeout;
        this.circuitBreaker = circuitBreaker;

        this.request = request;
        for(final String head : headers.keySet()) {
            request.addHeader(head, headers.get(head));
        }
        try {
            URIBuilder uriBuilder = new URIBuilder(this.proxyUrl);
            for(final String param : params.keySet()) {
                uriBuilder.addParameter(param, params.get(param));
            }
            request.setURI(uriBuilder.build());
            System.out.println(request.getURI());
        } catch (URISyntaxException e) {
            Logger.warn(this.getClass(), e.getMessage());
        }
        

        
        
    }

    public void doOut(HttpServletResponse response) throws IOException, CircuitBreakerOpenException {
        try (OutputStream out = response.getOutputStream()) {
            doOut(out);
        }
    }

    public String doString() throws CircuitBreakerOpenException, IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            doOut(out);
            return new String(out.toByteArray());
        }
    }

    public void doOut(OutputStream outer) throws CircuitBreakerOpenException, IOException {
        try (OutputStream out = outer) {
            Failsafe.with(circuitBreaker).onSuccess(connection -> System.err.println("Connected to " + proxyUrl))
                    .onFailure(failure -> Logger.warn(this, "Connection attempts failed " + failure.getMessage())).run(ctx -> {
                        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
                            
                            Timer timer = new Timer();
                            timer.schedule(new TimerTask() {
                                public void run() {
                                    if (request != null) {
                                        request.abort();
                                        System.err.println("Connection attempt timed out after " + timeout + "ms");
                                    }
                                }
                            }, timeout);


                            HttpResponse response = httpclient.execute(request);
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
        return "CircuitBreakerUrl [proxyUrl=" + proxyUrl + ", timeout=" + timeout + ", circuitBreaker=" + circuitBreaker + "]";
    }


    public enum Method {
        GET, POST, PUT, DELETE

    }




}
