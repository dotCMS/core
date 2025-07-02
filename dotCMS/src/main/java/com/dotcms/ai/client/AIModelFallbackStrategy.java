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
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
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
     * @param incoming the output stream to which the response will be written
     * @return response data object
     * @throws DotAIAllModelsExhaustedException if all models are exhausted and no successful response is obtained
     */
    @Override
    public AIResponseData applyStrategy(final AIClient client,
                                        final AIResponseEvaluator handler,
                                        final AIRequest<? extends Serializable> request,
                                        final OutputStream incoming) {
        final JSONObjectAIRequest jsonRequest = AIClient.useRequestOrThrow(request);
        final Tuple2<AIModel, Model> modelTuple = resolveModel(jsonRequest);

        final AIResponseData firstAttempt = sendRequest(client, handler, jsonRequest, incoming, modelTuple);
        if (firstAttempt.isSuccess()) {
            return firstAttempt;
        }

        return runFallbacks(client, handler, jsonRequest, incoming, modelTuple);
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
        return firstAttempt.equals(model);
    }

    private static boolean isOperational(final Model model, final AppConfig config) {
        if (!model.isOperational()) {
            config.debugLogger(
                    AIModelFallbackStrategy.class,
                    () -> String.format("Model [%s] is not operational. Skipping.", model.getName()));
            return false;
        }

        return true;
    }

    private static AIResponseData doSend(final AIClient client,
                                         final JSONObjectAIRequest request,
                                         final OutputStream incoming,
                                         final boolean isStream) {
        final OutputStream output = Optional.ofNullable(incoming).orElseGet(ByteArrayOutputStream::new);
        client.sendRequest(request, output);

        return AIClientStrategy.response(output, isStream);
    }

    private static void notifyFailure(final AIModel aiModel, final JSONObjectAIRequest request) {
        AIAppValidator.get().validateModelsUsage(aiModel, request);
    }

    private static void handleResponse(final Tuple2<AIModel, Model> modelTuple,
                                       final JSONObjectAIRequest request,
                                       final AIResponseData responseData) {
        if (responseData.isSuccess()) {
            request.getConfig().debugLogger(
                    AIModelFallbackStrategy.class,
                    () -> String.format("Model [%s] succeeded. No need to fallback.", modelTuple._2.getName()));
            return;
        }

        handleFailure(modelTuple, request, responseData);
    }

    private static void handleFailure(final Tuple2<AIModel, Model> modelTuple,
                                      final JSONObjectAIRequest request,
                                      final AIResponseData responseData) {
        logFailure(modelTuple, request, responseData);

        if (responseData.getStatus().doesNeedToThrow()) {
            throw responseData.getException();
        }

        final AIModel aiModel = modelTuple._1;
        final Model model = modelTuple._2;
        final AppConfig appConfig = request.getConfig();

        appConfig.debugLogger(
                AIModelFallbackStrategy.class,
                () -> String.format(
                        "Model [%s] failed then setting its status to [%s].",
                        model.getName(),
                        responseData.getStatus()));
        model.setStatus(responseData.getStatus());

        if (model.getIndex() == aiModel.getModels().size() - 1) {
            aiModel.setCurrentModelIndex(AIModel.NOOP_INDEX);
            appConfig.debugLogger(
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

    private static AIResponseData sendRequest(final AIClient client,
                                              final AIResponseEvaluator evaluator,
                                              final JSONObjectAIRequest request,
                                              final OutputStream output,
                                              final Tuple2<AIModel, Model> modelTuple) {
        final boolean isStream = AIClientStrategy.isStream(request) && output != null;
        final AIResponseData responseData = Try
                .of(() -> doSend(client, request, output, isStream))
                .getOrElseGet(exception -> fromException(evaluator, exception));

        try {
            if (responseData.isSuccess()) {
                evaluator.fromResponse(responseData.getResponse(), responseData, !isStream);
            } else {
                handleException(request, modelTuple, responseData);
            }

            handleResponse(modelTuple, request, responseData);

            if (responseData.isSuccess()) {
                return responseData;
            }
        } catch (DotRuntimeException e) {
            Logger.error(AIModelFallbackStrategy.class,
                    "Something went wrong while trying to process the request with the AI service." + e.getMessage());
            throw e;
        } finally {
            if (!isStream) {
                IOUtils.closeQuietly(responseData.getOutput());
            }
        }

        return responseData;
    }

    private static void handleException(final JSONObjectAIRequest request,
                                        final Tuple2<AIModel, Model> modelTuple,
                                        final AIResponseData responseData) {
        if (!modelTuple._1.isOperational()) {
            request.getConfig().debugLogger(
                    AIModelFallbackStrategy.class,
                    () -> String.format(
                            "All models from type [%s] are not operational. Throwing exception.",
                            modelTuple._1.getType()));
            notifyFailure(modelTuple._1, request);
        }

        if (responseData.getStatus().doesNeedToThrow()) {
            throw responseData.getException();
        }
    }

    private static void logFailure(final Tuple2<AIModel, Model> modelTuple,
                                   final JSONObjectAIRequest request,
                                   final AIResponseData responseData) {
        Optional
                .ofNullable(responseData.getResponse())
                .ifPresentOrElse(
                        response -> request.getConfig().debugLogger(
                                AIModelFallbackStrategy.class,
                                () -> String.format(
                                        "Model [%s] failed with response:%s%sTrying next model.",
                                        modelTuple._2.getName(),
                                        System.lineSeparator(),
                                        response)),
                        () -> request.getConfig().debugLogger(
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

    private static AIResponseData runFallbacks(final AIClient client,
                                               final AIResponseEvaluator evaluator,
                                               final JSONObjectAIRequest request,
                                               final OutputStream output,
                                               final Tuple2<AIModel, Model> modelTuple) {
        for(final Model model : modelTuple._1.getModels()) {
            if (isSameAsFirst(modelTuple._2, model) || !isOperational(model, request.getConfig())) {
                continue;
            }

            request.getPayload().put(AiKeys.MODEL, model.getName());
            final AIResponseData responseData = sendRequest(
                    client,
                    evaluator,
                    request,
                    output,
                    Tuple.of(modelTuple._1, model));
            if (responseData.isSuccess()) {
                return responseData;
            }
        }

        return null;
    }

}
