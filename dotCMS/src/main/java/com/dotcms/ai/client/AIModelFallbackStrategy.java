package com.dotcms.ai.client;

import com.dotcms.ai.AiKeys;
import com.dotcms.ai.app.AIModel;
import com.dotcms.ai.app.AppConfig;
import com.dotcms.ai.domain.AIRequest;
import com.dotcms.ai.domain.AIResponseData;
import com.dotcms.ai.domain.JSONObjectAIRequest;
import com.dotcms.ai.domain.Model;
import com.dotcms.ai.exception.DotAIAllModelsExhaustedException;
import com.dotcms.ai.validator.AIAppValidator;
import com.dotmarketing.exception.DotRuntimeException;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Implementation of the {@link AIClientStrategy} interface that provides a fallback mechanism
 * for handling AI client requests.
 *
 * <p>
 * This class attempts to send a request using a primary AI model and, if the request fails,
 * it falls back to alternative models until a successful response is obtained or all models
 * are exhausted.
 * </p>
 *
 * <p>
 * The fallback strategy ensures that the AI client can continue to function even if some models
 * are not operational or fail to process the request.
 * </p>
 *
 * @author vico
 */
public class AIModelFallbackStrategy implements AIClientStrategy {

    /**
     * Applies the fallback strategy to the given AI client request and handles the response.
     *
     * <p>
     * This method first attempts to send the request using the primary model. If the request
     * fails, it falls back to alternative models until a successful response is obtained or
     * all models are exhausted.
     * </p>
     *
     * @param client the AI client to which the request is sent
     * @param handler the response evaluator to handle the response
     * @param request the AI request to be processed
     * @param output the output stream to which the response will be written
     * @throws DotAIAllModelsExhaustedException if all models are exhausted and no successful response is obtained
     */
    @Override
    public void applyStrategy(final AIClient client,
                              final AIResponseEvaluator handler,
                              final AIRequest<? extends Serializable> request,
                              final OutputStream output) {
        final JSONObjectAIRequest jsonRequest = AIClient.useRequestOrThrow(request);
        final Tuple2<AIModel, Model> modelTuple = resolveModel(jsonRequest);

        final AIResponseData firstAttempt = sendAttempt(client, handler, jsonRequest, output, modelTuple);
        if (firstAttempt.isSuccess()) {
            return;
        }

        runFallbacks(client, handler, jsonRequest, output, modelTuple);
    }

    private static Tuple2<AIModel, Model> resolveModel(final JSONObjectAIRequest request) {
        final String modelName = request.getPayload().optString(AiKeys.MODEL);
        return request.getConfig().resolveModelOrThrow(modelName, request.getType());
    }

    private static boolean isSameAsFirst(final Model firstAttempt, final Model model) {
        if (firstAttempt.equals(model)) {
            AppConfig.debugLogger(
                    AIModelFallbackStrategy.class,
                    () -> String.format(
                            "Model [%s] is the same as the current one [%s].",
                            model.getName(),
                            firstAttempt.getName()));
            return true;
        }

        return false;
    }

    private static boolean isOperational(final Model model) {
        if (!model.isOperational()) {
            AppConfig.debugLogger(
                    AIModelFallbackStrategy.class,
                    () -> String.format("Model [%s] is not operational. Skipping.", model.getName()));
            return false;
        }

        return true;
    }

    private static AIResponseData doSend(final AIClient client, final AIRequest<? extends Serializable> request) {
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        client.sendRequest(request, output);

        final AIResponseData responseData = new AIResponseData();
        responseData.setResponse(output.toString());
        IOUtils.closeQuietly(output);

        return responseData;
    }

    private static void redirectOutput(final OutputStream output, final String response) {
        try (final InputStream input = new ByteArrayInputStream(response.getBytes(StandardCharsets.UTF_8))) {
            IOUtils.copy(input, output);
        } catch (IOException e) {
            throw new DotRuntimeException(e);
        }
    }

    private static void notifyFailure(final AIModel aiModel, final AIRequest<? extends Serializable> request) {
        AIAppValidator.get().validateModelsUsage(aiModel, request.getUserId());
    }

    private static void handleFailure(final Tuple2<AIModel, Model> modelTuple,
                                      final AIRequest<? extends Serializable> request,
                                      final AIResponseData responseData) {
        final AIModel aiModel = modelTuple._1;
        final Model model = modelTuple._2;

        if (!responseData.getStatus().doesNeedToThrow()) {
            model.setStatus(responseData.getStatus());
        }

        if (model.getIndex() == aiModel.getModels().size() - 1) {
            aiModel.setCurrentModelIndex(-1);
            AppConfig.debugLogger(
                    AIModelFallbackStrategy.class,
                    () -> String.format(
                            "Model [%s] is the last one. Cannot fallback anymore.",
                            model.getName()));

            notifyFailure(aiModel, request);

            throw new DotAIAllModelsExhaustedException(
                    String.format("All models for type [%s] has been exhausted.", aiModel.getType()));
        } else {
            aiModel.setCurrentModelIndex(model.getIndex() + 1);
        }
    }

    private static AIResponseData sendAttempt(final AIClient client,
                                              final AIResponseEvaluator evaluator,
                                              final JSONObjectAIRequest request,
                                              final OutputStream output,
                                              final Tuple2<AIModel, Model> modelTuple) {

        final AIResponseData responseData = Try
                .of(() -> doSend(client, request))
                .getOrElseGet(exception -> fromException(evaluator, exception));

        if (!responseData.isSuccess()) {
            if (responseData.getStatus().doesNeedToThrow()) {
                throw responseData.getException();
            }
        } else {
            evaluator.fromResponse(responseData.getResponse(), responseData, output instanceof ByteArrayOutputStream);
        }

        if (responseData.isSuccess()) {
            AppConfig.debugLogger(
                    AIModelFallbackStrategy.class,
                    () -> String.format("Model [%s] succeeded. No need to fallback.", modelTuple._2.getName()));
            redirectOutput(output, responseData.getResponse());
        } else {
            logFailure(modelTuple, responseData);

            handleFailure(modelTuple, request, responseData);
        }

        return responseData;
    }

    private static void logFailure(final Tuple2<AIModel, Model> modelTuple, final AIResponseData responseData) {
        Optional
                .ofNullable(responseData.getResponse())
                .ifPresentOrElse(
                        response -> AppConfig.debugLogger(
                                AIModelFallbackStrategy.class,
                                () -> String.format(
                                        "Model [%s] failed with response:%s%s%s. Trying next model.",
                                        modelTuple._2.getName(),
                                        System.lineSeparator(),
                                        response,
                                        System.lineSeparator())),
                        () -> AppConfig.debugLogger(
                                AIModelFallbackStrategy.class,
                                () -> String.format(
                                        "Model [%s] failed with error: [%s]. Trying next model.",
                                        modelTuple._2.getName(),
                                        responseData.getError())));
    }

    private static AIResponseData fromException(final AIResponseEvaluator evaluator, final Throwable exception) {
        final AIResponseData metadata = new AIResponseData();
        evaluator.fromException(exception, metadata);
        return metadata;
    }

    private static void runFallbacks(final AIClient client,
                                     final AIResponseEvaluator evaluator,
                                     final JSONObjectAIRequest request,
                                     final OutputStream output,
                                     final Tuple2<AIModel, Model> modelTuple) {
        for(final Model model : modelTuple._1.getModels()) {
            if (isSameAsFirst(modelTuple._2, model) || !isOperational(model)) {
                continue;
            }

            request.getPayload().put(AiKeys.MODEL, model.getName());
            final AIResponseData responseData = sendAttempt(
                    client,
                    evaluator,
                    request,
                    output,
                    Tuple.of(modelTuple._1, model));
            if (responseData.isSuccess()) {
                return;
            }
        }
    }

}
