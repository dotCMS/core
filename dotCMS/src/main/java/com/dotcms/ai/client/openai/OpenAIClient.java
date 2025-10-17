package com.dotcms.ai.client.openai;

import com.dotcms.ai.AiKeys;
import com.dotcms.ai.app.AIModel;
import com.dotcms.ai.app.AIModels;
import com.dotcms.ai.app.AiAppConfig;
import com.dotcms.ai.app.AppKeys;
import com.dotcms.ai.client.AIClient;
import com.dotcms.ai.client.AIRequest;
import com.dotcms.ai.client.JSONObjectAIRequest;
import com.dotcms.ai.domain.AIProvider;
import com.dotcms.ai.domain.Model;
import com.dotcms.ai.exception.DotAIAppConfigDisabledException;
import com.dotcms.ai.exception.DotAIClientConnectException;
import com.dotcms.ai.exception.DotAIModelNotFoundException;
import com.dotcms.ai.exception.DotAIModelNotOperationalException;
import com.dotcms.http.CircuitBreakerUrl;
import com.dotcms.rest.exception.GenericHttpStatusCodeException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.json.JSONObject;
import io.vavr.Lazy;
import io.vavr.Tuple2;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.Response;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Optional;

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

    private static final String AI_OPEN_AI_TIMEOUT_KEY = "AI_OPEN_AI_TIMEOUT_KEY";
    private static final String AI_OPEN_AI_ATTEMPTS_KEY = "AI_OPEN_AI_ATTEMPTS_KEY";
    private static final Lazy<OpenAIClient> INSTANCE = Lazy.of(OpenAIClient::new);

    public static OpenAIClient get() {
        return INSTANCE.get();
    }

    private static long resolveTimeout() {
        return Config.getLongProperty(AI_OPEN_AI_TIMEOUT_KEY, 120 * 1000L);
    }

    private static int resolveAttempts() {
        return Config.getIntProperty(AI_OPEN_AI_ATTEMPTS_KEY, 3);
    }

    private static CircuitBreakerUrl.Method resolveMethod(final JSONObjectAIRequest request) {
        switch(request.getMethod().toUpperCase()) {
            case HttpMethod.POST:
                return CircuitBreakerUrl.Method.POST;
            case HttpMethod.PUT:
                return CircuitBreakerUrl.Method.PUT;
            case HttpMethod.DELETE:
                return CircuitBreakerUrl.Method.DELETE;
            case HttpMethod.PATCH:
                return CircuitBreakerUrl.Method.PATCH;
            case HttpMethod.GET:
            default:
                return CircuitBreakerUrl.Method.GET;
        }
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
        final AiAppConfig appConfig = jsonRequest.getConfig();

        appConfig.debugLogger(
                OpenAIClient.class,
                () -> String.format(
                        "Posting to [%s] with method [%s]%s  with app config:%s%s  the payload: %s",
                        jsonRequest.getUrl(),
                        jsonRequest.getMethod(),
                        System.lineSeparator(),
                        appConfig,
                        System.lineSeparator(),
                        jsonRequest.payloadToString()));

        if (!appConfig.isEnabled()) {
            appConfig.debugLogger(OpenAIClient.class, () -> "App dotAI is not enabled and will not send request.");
            throw new DotAIAppConfigDisabledException("App dotAI config without API urls or API key");
        }

        final JSONObject payload = jsonRequest.getPayload();
        final String modelName = Optional
                .ofNullable(payload.optString(AiKeys.MODEL))
                .orElseThrow(() -> new DotAIModelNotFoundException("Model is not present in the request"));
        final Tuple2<AIModel, Model> modelTuple = appConfig.resolveModelOrThrow(modelName, jsonRequest.getType());
        //final AIModel aiModel = modelTuple._1;

        if (!modelTuple._2.isOperational()) {
            appConfig.debugLogger(
                    getClass(),
                    () -> String.format("Resolved model [%s] is not operational, avoiding its usage", modelName));
            throw new DotAIModelNotOperationalException(String.format("Model [%s] is not operational", modelName));
        }

        try {
            CircuitBreakerUrl.builder()
                    .setMethod(resolveMethod(jsonRequest))
                    .setAuthHeaders(AIModels.BEARER + appConfig.getApiKey())
                    .setUrl(jsonRequest.getUrl())
                    .setRawData(payload.toString())
                    .setTimeout(resolveTimeout())
                    .setTryAgainAttempts(resolveAttempts())
                    .setOverrideException(statusCode -> resolveException(jsonRequest, modelName, statusCode))
                    .setRaiseFailsafe(true)
                    .build()
                    .doOut(output);
        } catch (DotAIModelNotFoundException e) {
            throw e;
        } catch (Exception e) {
            if (appConfig.getConfigBoolean(AppKeys.DEBUG_LOGGING)) {
                Logger.warn(this, "INVALID REQUEST: " + e.getMessage(), e);
            } else {
                Logger.warn(this, "INVALID REQUEST: " + e.getMessage());
            }

            Logger.warn(this, " -  " + jsonRequest.getMethod() + " : " + payload);

            throw new DotAIClientConnectException("Error while sending request to OpenAI", e);
        }
    }

    private Exception resolveException(final JSONObjectAIRequest jsonRequest,
                                       final String modelName,
                                       final int statusCode) {
        return statusCode == 404
                ? new DotAIModelNotFoundException(String.format("Model [%s] not found", modelName))
                : new GenericHttpStatusCodeException(
                        String.format(
                                "Got invalid response for url: [%s] response: [%d]",
                                jsonRequest.getUrl(),
                                statusCode),
                        Response.Status.fromStatusCode(statusCode));
    }
}
