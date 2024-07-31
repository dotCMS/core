package com.dotcms.ai.util;

import com.dotcms.ai.AiKeys;
import com.dotcms.ai.app.AIModel;
import com.dotcms.ai.app.AppConfig;
import com.dotcms.ai.app.AppKeys;
import com.dotcms.ai.app.ConfigService;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.json.JSONObject;
import io.vavr.control.Try;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.*;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The OpenAIRequest class is a utility class that handles HTTP requests to the OpenAI API.
 * It provides methods for sending GET, POST, PUT, DELETE, and PATCH requests.
 * This class also manages rate limiting for the OpenAI API by keeping track of the last time a request was made.
 *
 * This class is implemented as a singleton, meaning that only one instance of the class is created throughout the execution of the program.
 */
public class OpenAIRequest {

    private static final ConcurrentHashMap<AIModel, Long> lastRestCall = new ConcurrentHashMap<>();

    private OpenAIRequest() {}

    /**
     * Sends a request to the specified URL with the specified method, OpenAI API key, and JSON payload.
     * The response from the request is written to the provided OutputStream.
     * This method also manages rate limiting for the OpenAI API by keeping track of the last time a request was made.
     *
     * @param urlIn the URL to send the request to
     * @param method the HTTP method to use for the request
     * @param appConfig the AppConfig object containing the OpenAI API key and models
     * @param payload the JSON payload to send with the request
     * @param out the OutputStream to write the response to
     */
    public static void doRequest(final String urlIn,
                                 final String method,
                                 final AppConfig appConfig,
                                 final JSONObject payload,
                                 final OutputStream out) {

        final JSONObject json = Optional.ofNullable(payload).orElse(new JSONObject());

        if (appConfig.getConfigBoolean(AppKeys.DEBUG_LOGGING)) {
            Logger.debug(OpenAIRequest.class, "posting: " + json);
        }

        final AIModel model = appConfig.resolveModelOrThrow(json.optString(AiKeys.MODEL));
        final long sleep = lastRestCall.computeIfAbsent(model, m -> 0L)
                + model.minIntervalBetweenCalls()
                - System.currentTimeMillis();
        if (sleep > 0) {
            Logger.info(
                    OpenAIRequest.class,
                    "Rate limit:"
                            + model.getApiPerMinute()
                            + "/minute, or 1 every "
                            + model.minIntervalBetweenCalls()
                            + "ms. Sleeping:"
                            + sleep);
            Try.run(() -> Thread.sleep(sleep));
        }

        lastRestCall.put(model, System.currentTimeMillis());

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            final StringEntity jsonEntity = new StringEntity(json.toString(), ContentType.APPLICATION_JSON);
            final HttpUriRequest httpRequest = resolveMethod(method, urlIn);
            httpRequest.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
            httpRequest.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + appConfig.getApiKey());

            if (!json.getAsMap().isEmpty()) {
                Try.run(() -> ((HttpEntityEnclosingRequestBase) httpRequest).setEntity(jsonEntity));
            }

            try (CloseableHttpResponse response = httpClient.execute(httpRequest)) {
                final BufferedInputStream in = new BufferedInputStream(response.getEntity().getContent());
                final byte[] buffer = new byte[1024];
                int len;
                while ((len = in.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                    out.flush();
                }
            }
        } catch (Exception e) {
            if (ConfigService.INSTANCE.config().getConfigBoolean(AppKeys.DEBUG_LOGGING)){
                Logger.warn(OpenAIRequest.class, "INVALID REQUEST: " + e.getMessage(), e);
            } else {
                Logger.warn(OpenAIRequest.class, "INVALID REQUEST: " + e.getMessage());
            }

            Logger.warn(OpenAIRequest.class, " -  " + method + " : " +json);

            throw new DotRuntimeException(e);
        }
    }

    /**
     * Sends a request to the specified URL with the specified method, OpenAI API key, and JSON payload.
     * The response from the request is returned as a string.
     *
     * @param url the URL to send the request to
     * @param method the HTTP method to use for the request
     * @param appConfig the AppConfig object containing the OpenAI API key and models
     * @param payload the JSON payload to send with the request
     * @return the response from the request as a string
     */
    public static String doRequest(final String url,
                                   final String method,
                                   final AppConfig appConfig,
                                   final JSONObject payload)  {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        doRequest(url, method, appConfig, payload, out);

        return out.toString();
    }

    /**
     * Sends a POST request to the specified URL with the specified OpenAI API key and JSON payload.
     * The response from the request is written to the provided OutputStream.
     *
     * @param urlIn the URL to send the request to
     * @param appConfig the AppConfig object containing the OpenAI API key and models
     * @param payload the JSON payload to send with the request
     * @param out the OutputStream to write the response to
     */
    public static void doPost(final String urlIn,
                              final AppConfig appConfig,
                              final JSONObject payload,
                              final OutputStream out) {
       doRequest(urlIn, HttpMethod.POST, appConfig, payload, out);
    }

    /**
     * Sends a GET request to the specified URL with the specified OpenAI API key and JSON payload.
     * The response from the request is written to the provided OutputStream.
     *
     * @param urlIn the URL to send the request to
     * @param appConfig the AppConfig object containing the OpenAI API key and models
     * @param payload the JSON payload to send with the request
     * @param out the OutputStream to write the response to
     */
    public static void doGet(final String urlIn,
                             final AppConfig appConfig,
                             final JSONObject payload,
                             final OutputStream out) {
        doRequest(urlIn, HttpMethod.GET, appConfig, payload, out);
    }

    private static HttpUriRequest resolveMethod(final String method, final String urlIn) {
        switch(method) {
            case HttpMethod.POST:
                return new HttpPost(urlIn);
            case HttpMethod.PUT:
                return new HttpPut(urlIn);
            case HttpMethod.DELETE:
                return new HttpDelete(urlIn);
            case "patch":
                return new HttpPatch(urlIn);
            case HttpMethod.GET:
            default:
                return new HttpGet(urlIn);
        }
    }

}
