package com.dotcms.http;

import java.util.Map;

import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;

import com.dotcms.http.CircuitBreakerUrl.Method;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.util.Config;
import com.google.common.collect.Maps;

import net.jodah.failsafe.CircuitBreaker;


public class CircuitBreakerUrlBuilder {
    String proxyUrl = null;
    long timeout = Config.getIntProperty("URL_CONNECTION_TIMEOUT", 2000);
    CircuitBreaker circuitBreaker = null;
    Map<String, String> params = Maps.newHashMap();
    Map<String, String> headers = Maps.newHashMap();
    Method method = Method.GET;
    boolean verbose = false;
    int failAfter = CurcuitBreakerPool.FAIL_AFTER;
    int tryAgainAttempts = CurcuitBreakerPool.TRY_AGAIN_ATTEMPTS;
    int tryAgainAfterDelay = CurcuitBreakerPool.TRY_AGAIN_DELAY_SEC;
    String rawData = null;

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
            this.circuitBreaker = CurcuitBreakerPool.getBreaker(this.proxyUrl + this.timeout, failAfter, tryAgainAttempts, tryAgainAfterDelay);
        }
        HttpRequestBase request;
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
            default:
                request = new HttpGet(proxyUrl);
                break;
        }

        return new CircuitBreakerUrl(this.proxyUrl, this.timeout, this.circuitBreaker, request, this.params, this.headers, this.verbose, this.rawData);


    }


}

