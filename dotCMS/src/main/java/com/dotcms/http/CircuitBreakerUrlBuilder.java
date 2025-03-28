package com.dotcms.http;

import java.util.Map;
import java.util.function.Function;

import com.google.common.collect.ImmutableMap;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;

import com.dotcms.http.CircuitBreakerUrl.Method;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.util.Config;
import com.google.common.collect.Maps;

import net.jodah.failsafe.CircuitBreaker;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;


public class CircuitBreakerUrlBuilder {
    String proxyUrl = null;
    long timeout = Config.getIntProperty("URL_CONNECTION_TIMEOUT", 2000);
    CircuitBreaker circuitBreaker = null;
    Map<String, String> params = Maps.newHashMap();
    Map<String, String> headers = Maps.newHashMap();
    Method method = Method.GET;
    boolean verbose = false;
    int failAfter = CircuitBreakerPool.FAIL_AFTER;
    int tryAgainAttempts = CircuitBreakerPool.TRY_AGAIN_ATTEMPTS;
    int tryAgainAfterDelay = CircuitBreakerPool.TRY_AGAIN_DELAY_SEC;
    String rawData = null;
    boolean allowRedirects=Config.getBooleanProperty("REMOTE_CALL_ALLOW_REDIRECTS", false);
    boolean throwWhenError = true;
    Function<Integer, Exception> overrideException;
    boolean raiseFailsafe = false;

    public CircuitBreakerUrlBuilder setUrl(String proxyUrl) {
        this.proxyUrl = proxyUrl;
        return this;
    }

    public CircuitBreakerUrlBuilder setFailAfter(int failAfter) {
        this.failAfter = failAfter;
        return this;
    }

    public CircuitBreakerUrlBuilder setTryAgainAfterDelaySeconds(int tryAgainAfter) {
        this.tryAgainAfterDelay = tryAgainAfter;
        return this;
    }

    public CircuitBreakerUrlBuilder setTryAgainAttempts(int tryAgainAttempts) {
      this.tryAgainAttempts = tryAgainAttempts;
      return this;
    }

    public CircuitBreakerUrlBuilder setRawData(String rawData) {
        this.rawData = rawData;
        return this;
    }

    public CircuitBreakerUrlBuilder setTimeout(long timeout) {
        this.timeout = timeout;
        return this;
    }

    public CircuitBreakerUrlBuilder setAllowRedirects(boolean allowRedirects) {
        this.allowRedirects = allowRedirects;
        return this;
    }

    public CircuitBreakerUrlBuilder setThrowWhenError(boolean throwWhenError) {
        this.throwWhenError = throwWhenError;
        return this;
    }

    public CircuitBreakerUrlBuilder setRaiseFailsafe(final boolean raiseFailsafe) {
        this.raiseFailsafe = raiseFailsafe;
        return this;
    }
    
    public CircuitBreakerUrlBuilder setCircuitBreaker(CircuitBreaker circuitBreaker) {
        this.circuitBreaker = circuitBreaker;
        return this;
    }

    public CircuitBreakerUrlBuilder setParams(Map<String, String> params) {
        if (params == null) {
            params = Maps.newHashMap();
        }
        this.params = params;
        return this;
    }

    public CircuitBreakerUrlBuilder setHeaders(Map<String, String> headers) {
        if (headers == null) {
            headers = Maps.newHashMap();
        }
        this.headers = headers;
        return this;
    }

    public CircuitBreakerUrlBuilder setAuthHeaders(final String token) {
        return setHeaders(ImmutableMap.<String, String>builder()
                .put(HttpHeaders.AUTHORIZATION, token)
                .put(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON)
                .put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .build());
    }

    public CircuitBreakerUrlBuilder setOverrideException(final Function<Integer, Exception> overrideException) {
        this.overrideException = overrideException;
        return this;
    }

    public CircuitBreakerUrlBuilder setMethod(Method method) {
        this.method = method;
        return this;
    }

    public CircuitBreakerUrlBuilder setVerbose(boolean verbose) {
        this.verbose = verbose;
        return this;
    }

    public CircuitBreakerUrl build() {
        if (proxyUrl == null) {
            throw new DotStateException("A URL must be set to use CircuitBreakerUrl");
        }
        if (this.circuitBreaker == null) {
            this.circuitBreaker = CircuitBreakerPool.getBreaker(this.proxyUrl + this.timeout, failAfter, tryAgainAttempts, tryAgainAfterDelay);
        }
        final HttpRequestBase request;
        switch (this.method) {
            case POST:
                request = new HttpPost(proxyUrl);
                break;
            case PUT:
                request = new HttpPut(proxyUrl);
                break;
            case DELETE:
                request = new HttpDelete(proxyUrl);
                break;
            case HEAD:
                request = new HttpHead(proxyUrl);
                break;
            default:
                request = new HttpGet(proxyUrl);
                break;
        } 

        return new CircuitBreakerUrl(
            this.proxyUrl,
            this.timeout,
            this.circuitBreaker,
            request,
            this.params,
            this.headers,
            this.verbose,
            this.rawData,
            this.allowRedirects,
            this.throwWhenError,
            this.overrideException,
            this.raiseFailsafe);
    }

    /**
     * Set the config to do a ping
     * @return
     */
    public CircuitBreakerUrlBuilder doPing() {
        this.method = Method.HEAD;
        return this;
    }
}

