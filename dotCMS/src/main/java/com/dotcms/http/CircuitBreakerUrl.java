package com.dotcms.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Timer;
import java.util.TimerTask;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.google.common.annotations.VisibleForTesting;

import net.jodah.failsafe.CircuitBreaker;
import net.jodah.failsafe.CircuitBreakerOpenException;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.FailsafeException;


public class CircuitBreakerUrl {

    final String proxyUrl;
    final long timeout;
    final CircuitBreaker circuitBreaker;


    public CircuitBreakerUrl(String forwardUrl) {
        this(forwardUrl, Config.getIntProperty("URL_CONNECTION_TIMEOUT", 2000));
    }

    public CircuitBreakerUrl(String forwardUrl, long timeout) {
        this(forwardUrl, timeout, CurcuitBreakerPool.getBreaker(forwardUrl + timeout));
    }

    public CircuitBreakerUrl(String forwardUrl, long timeout, String circuitBreakerKey) {
        this(forwardUrl, timeout, CurcuitBreakerPool.getBreaker(circuitBreakerKey));
    }
    
    @VisibleForTesting
    public CircuitBreakerUrl(String forwardUrl, long timeout, CircuitBreaker circuitBreaker) {
        this.proxyUrl = forwardUrl;
        this.timeout = timeout;
        this.circuitBreaker = circuitBreaker;
    }

    public void doOut(HttpServletResponse response)
                    throws IOException, CircuitBreakerOpenException {
        try (OutputStream out = response.getOutputStream()) {
            doOut(out);
        }
    }
    
    public String doString() throws CircuitBreakerOpenException, IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()){
            doOut(out);
            return new String(out.toByteArray());
        }
    }
    public void doOut(OutputStream outer) throws CircuitBreakerOpenException, IOException {
        try (OutputStream out = outer) {
            Failsafe.with(circuitBreaker)
                .onSuccess(connection -> Logger.info(this, "Connected to " + proxyUrl))
                .onFailure(failure -> Logger.error(this,
                                "Connection attempts failed " + failure.getMessage()))
                .run(ctx -> {
                    try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
                        final HttpGet get = new HttpGet(proxyUrl);
                        Timer timer = new Timer();
                        timer.schedule(new TimerTask() {
                            public void run() {
                                if(get != null) {
                                    get.abort();
                                }
                            }
                        }, timeout);

      
                        HttpResponse response = httpclient.execute(get);
                        IOUtils.copy(response.getEntity().getContent(), out);
                    }
                });
        } catch (FailsafeException ee) {
            Logger.debug(this.getClass(), ee.getMessage() + " " + toString());
        }
    }


    @Override
    public String toString() {
        return "CircuitBreakerUrl [proxyUrl=" + proxyUrl + ", timeout=" + timeout
                        + ", circuitBreaker=" + circuitBreaker + "]";
    }



}
