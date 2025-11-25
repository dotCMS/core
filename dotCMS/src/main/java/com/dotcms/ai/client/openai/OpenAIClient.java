package com.dotcms.ai.client.openai;

import com.dotcms.ai.AiKeys;
import com.dotcms.ai.app.AIModel;
import com.dotcms.ai.app.AppConfig;
import com.dotcms.ai.app.AppKeys;
import com.dotcms.ai.client.AIClient;
import com.dotcms.ai.domain.AIProvider;
import com.dotcms.ai.client.AIRequest;
import com.dotcms.ai.client.JSONObjectAIRequest;
import com.dotcms.ai.domain.Model;
import com.dotcms.ai.exception.DotAIAppConfigDisabledException;
import com.dotcms.ai.exception.DotAIClientConnectException;
import com.dotcms.ai.exception.DotAIModelNotFoundException;
import com.dotcms.ai.exception.DotAIModelNotOperationalException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.json.JSONObject;
import io.vavr.Lazy;
import io.vavr.Tuple;
import io.vavr.Tuple2;
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
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of the {@link AIClient} interface for interacting with the OpenAI service.
 *
 * <p>
 * This class provides methods to send requests to the OpenAI service and handle responses.
 * It includes functionality to manage rate limiting and ensure that models are operational
 * before sending requests.
 * </p>
 *
 * <p>
 * The class uses a singleton pattern to ensure a single instance of the client is used
 * throughout the application. It also maintains a record of the last REST call for each
 * model to enforce rate limiting.
 * </p>
 *
 * @auhor vico
 */
public class OpenAIClient implements AIClient {

    private static final Lazy<OpenAIClient> INSTANCE = Lazy.of(OpenAIClient::new);

    private final ConcurrentHashMap<AIModel, Long> lastRestCall;

    public static OpenAIClient get() {
        return INSTANCE.get();
    }

    private OpenAIClient() {
        lastRestCall = new ConcurrentHashMap<>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AIProvider getProvider() {
        return AIProvider.OPEN_AI;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends Serializable> void sendRequest(final AIRequest<T> request, final OutputStream output) {
        final JSONObjectAIRequest jsonRequest = AIClient.useRequestOrThrow(request);
        final AppConfig appConfig = jsonRequest.getConfig();

        AppConfig.debugLogger(
                OpenAIClient.class,
                () -> String.format(
                        "Posting to [%s] with method [%s]%s  with app config:%s%s  the payload: %s",
                        jsonRequest.getUrl(),
                        jsonRequest.getMethod(),
                        System.lineSeparator(),
                        appConfig.toString(),
                        System.lineSeparator(),
                        jsonRequest.payloadToString()));

        if (!appConfig.isEnabled()) {
            AppConfig.debugLogger(OpenAIClient.class, () -> "App dotAI is not enabled and will not send request.");
            throw new DotAIAppConfigDisabledException("App dotAI config without API urls or API key");
        }

        final JSONObject payload = jsonRequest.getPayload();
        final String modelName = Optional
                .ofNullable(payload.optString(AiKeys.MODEL))
                .orElseThrow(() -> new DotAIModelNotFoundException("Model is not present in the request"));
        final Tuple2<AIModel, Model> modelTuple = appConfig.resolveModelOrThrow(modelName, jsonRequest.getType());
        final AIModel aiModel = modelTuple._1;

        if (!modelTuple._2.isOperational()) {
            AppConfig.debugLogger(
                    getClass(),
                    () -> String.format("Resolved model [%s] is not operational, avoiding its usage", modelName));
            throw new DotAIModelNotOperationalException(String.format("Model [%s] is not operational", modelName));
        }

        final long sleep = lastRestCall.computeIfAbsent(aiModel, m -> 0L)
                + aiModel.minIntervalBetweenCalls()
                - System.currentTimeMillis();
        if (sleep > 0) {
            Logger.info(
                    this,
                    "Rate limit:"
                            + aiModel.getApiPerMinute()
                            + "/minute, or 1 every "
                            + aiModel.minIntervalBetweenCalls()
                            + "ms. Sleeping:"
                            + sleep);
            Try.run(() -> Thread.sleep(sleep));
        }

        lastRestCall.put(aiModel, System.currentTimeMillis());

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            final StringEntity jsonEntity = new StringEntity(payload.toString(), ContentType.APPLICATION_JSON);
            final HttpUriRequest httpRequest = AIClient.resolveMethod(jsonRequest.getMethod(), jsonRequest.getUrl());
            httpRequest.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
            httpRequest.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + appConfig.getApiKey());

            if (!payload.getAsMap().isEmpty()) {
                Try.run(() -> HttpEntityEnclosingRequestBase.class.cast(httpRequest).setEntity(jsonEntity));
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
            if (appConfig.getConfigBoolean(AppKeys.DEBUG_LOGGING)){
                Logger.warn(this, "INVALID REQUEST: " + e.getMessage(), e);
            } else {
                Logger.warn(this, "INVALID REQUEST: " + e.getMessage());
            }

            Logger.warn(this, " -  " + jsonRequest.getMethod() + " : " + payload);

            throw new DotAIClientConnectException("Error while sending request to OpenAI", e);
        }
    }

}
