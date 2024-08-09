package com.dotcms.ai.client;

import com.dotcms.ai.AiKeys;
import com.dotcms.ai.app.AIModel;
import com.dotcms.ai.app.AppConfig;
import com.dotcms.ai.app.AppKeys;
import com.dotcms.ai.domain.AIProvider;
import com.dotcms.ai.domain.AIRequest;
import com.dotcms.ai.domain.JSONObjectAIRequest;
import com.dotcms.ai.util.OpenAIRequest;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.json.JSONObject;
import io.vavr.Lazy;
import io.vavr.control.Try;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import javax.ws.rs.core.MediaType;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;

public class OpenAIClient implements AIClient {

    private static final Lazy<OpenAIClient> INSTANCE = Lazy.of(OpenAIClient::new);

    private final ConcurrentHashMap<AIModel, Long> lastRestCall;

    public static OpenAIClient get() {
        return INSTANCE.get();
    }

    private OpenAIClient() {
        lastRestCall = new ConcurrentHashMap<>();
    }

    @Override
    public AIProvider getProvider() {
        return AIProvider.OPEN_AI;
    }

    @Override
    public OutputStream sendRequest(final AIRequest<? extends Serializable> request) {
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        sendRequest(request, output);
        return output;
    }

    private OutputStream sendRequest(final AIRequest<? extends Serializable> request, final OutputStream output) {
        final AppConfig config = request.getConfig();
        if (!config.isEnabled()) {
            Logger.debug(OpenAIRequest.class, "OpenAI is not enabled and will not send request.");
            throw new IllegalStateException("OpenAI is not enabled");
        }

        // When we get rid of JSONObject usage, we can remove this check
        if (!(request instanceof JSONObjectAIRequest)) {
            throw new UnsupportedOperationException("Only JsonAIRequest (JSONObject) is supported");
        }

        final JSONObject json = ((JSONObjectAIRequest) request).getPayload();
        final AIModel model = config.resolveModelOrThrow(json.optString(AiKeys.MODEL));

        if (config.getConfigBoolean(AppKeys.DEBUG_LOGGING)) {
            Logger.debug(OpenAIRequest.class, "posting: " + json);
        }

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
            final HttpUriRequest httpRequest = AIClient.resolveMethod(request.getMethod(), request.getUrl());
            httpRequest.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
            httpRequest.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + config.getApiKey());

            if (!json.getAsMap().isEmpty()) {
                Try.run(() -> ((HttpEntityEnclosingRequestBase) httpRequest).setEntity(jsonEntity));
            }

            try (CloseableHttpResponse response = httpClient.execute(httpRequest)) {
                final BufferedInputStream in = new BufferedInputStream(response.getEntity().getContent());
                final byte[] buffer = new byte[1024];
                int len;
                while ((len = in.read(buffer)) != -1) {
                    output.write(buffer, 0, len);
                    output.flush();
                }
            }
        } catch (Exception e) {
            if (config.getConfigBoolean(AppKeys.DEBUG_LOGGING)){
                Logger.warn(OpenAIRequest.class, "INVALID REQUEST: " + e.getMessage(), e);
            } else {
                Logger.warn(OpenAIRequest.class, "INVALID REQUEST: " + e.getMessage());
            }

            Logger.warn(OpenAIRequest.class, " -  " + request.getMethod() + " : " + json);

            throw new DotRuntimeException(e);
        }

        return output;
    }

}
