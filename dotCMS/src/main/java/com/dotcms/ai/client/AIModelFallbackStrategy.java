package com.dotcms.ai.client;

import com.dotcms.ai.AiKeys;
import com.dotcms.ai.app.AIModel;
import com.dotcms.ai.app.AIModels;
import com.dotcms.ai.app.AppConfig;
import com.dotcms.ai.domain.AIResponseData;
import com.dotcms.ai.domain.Model;
import com.dotcms.ai.exception.DotAIAllModelsExhaustedException;
import com.dotcms.ai.validator.AIAppValidator;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.UtilMethods;
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
        final AppConfig appConfig = request.getConfig();
        final String modelName = request.getPayload().optString(AiKeys.MODEL);
        if (UtilMethods.isSet(modelName)) {
            return appConfig.resolveModelOrThrow(modelName, request.getType());
        }

        final Optional<AIModel> aiModelOpt = AIModels.get().findModel(appConfig.getHost(), request.getType());
        if (aiModelOpt.isPresent()) {
            final AIModel aiModel = aiModelOpt.get();
            if (aiModel.isOperational()) {
                aiModel.repairCurrentIndexIfNeeded();
                return appConfig.resolveModelOrThrow(aiModel.getCurrentModel(), aiModel.getType());
            }

            notifyFailure(aiModel, request);
        }

        throw new DotAIAllModelsExhaustedException(String.format("No models found for type [%s]", request.getType()));
    }

    private static boolean isSameAsFirst(final Model firstAttempt, final Model model) {
        if (firstAttempt.equals(model)) {
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
            AppConfig.debugLogger(
                    AIModelFallbackStrategy.class,
                    () -> String.format(
                            "Model [%s] failed then setting its status to [%s].",
                            model.getName(),
                            responseData.getStatus()));
            model.setStatus(responseData.getStatus());
        }

        if (model.getIndex() == aiModel.getModels().size() - 1) {
            aiModel.setCurrentModelIndex(AIModel.NOOP_INDEX);
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
                if (!modelTuple._1.isOperational()) {
                    AppConfig.debugLogger(
                            AIModelFallbackStrategy.class,
                            () -> String.format(
                                    "All models from type [%s] are not operational. Throwing exception.",
                                    modelTuple._1.getType()));
                    notifyFailure(modelTuple._1, request);
                }
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
                                        "Model [%s] failed with response:%s%sTrying next model.",
                                        modelTuple._2.getName(),
                                        System.lineSeparator(),
                                        response)),
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
