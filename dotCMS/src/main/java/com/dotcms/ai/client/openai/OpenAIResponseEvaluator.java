package com.dotcms.ai.client.openai;

import com.dotcms.ai.AiKeys;
import com.dotcms.ai.client.AIResponseEvaluator;
import com.dotcms.ai.domain.AIResponseData;
import com.dotcms.ai.domain.ModelStatus;
import com.dotcms.ai.exception.DotAIModelNotFoundException;
import com.dotcms.ai.exception.DotAIModelNotOperationalException;
import com.dotcms.rest.exception.GenericHttpStatusCodeException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.json.JSONObject;
import io.vavr.Lazy;

import javax.ws.rs.core.Response;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Evaluates AI responses from OpenAI and updates the provided metadata.
 * This class implements the singleton pattern and provides methods to process responses and exceptions.
 *
 * <p>Methods:</p>
 * <ul>
 *   <li>\fromResponse\ - Processes a response string and updates the metadata.</li>
 *   <li>\fromThrowable\ - Processes an exception and updates the metadata.</li>
 * </ul>
 *
 * @author vico
 */
public class OpenAIResponseEvaluator implements AIResponseEvaluator {

    private static final String JSON_ERROR_FIELD = "\"error\":";
    private static final Lazy<OpenAIResponseEvaluator> INSTANCE = Lazy.of(OpenAIResponseEvaluator::new);

    public static OpenAIResponseEvaluator get() {
        return INSTANCE.get();
    }

    private OpenAIResponseEvaluator() {}

    /**
     * {@inheritDoc}
     */
    @Override
    public void fromResponse(final String response, final AIResponseData metadata, final boolean jsonExpected) {
        Optional.ofNullable(response)
                .ifPresent(resp -> {
                    if (jsonExpected || resp.contains(JSON_ERROR_FIELD)) {
                        final JSONObject jsonResponse = new JSONObject(resp);
                        if (jsonResponse.has(AiKeys.ERROR)) {
                            final JSONObject error = jsonResponse.getJSONObject(AiKeys.ERROR);
                            final String message = error.getString(AiKeys.MESSAGE);
                            metadata.setError(message);
                            metadata.setStatus(resolveStatus(message));
                        }
                    }
                });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void fromException(final Throwable exception, final AIResponseData metadata) {
        metadata.setError(exception.getMessage());
        metadata.setStatus(resolveStatus(exception));
        metadata.setException(exception instanceof DotRuntimeException
                ? (DotRuntimeException) exception
                : new DotRuntimeException(exception));
    }

    private ModelStatus resolveStatus(final String error) {
        if (error.contains("has been deprecated")) {
            return ModelStatus.DECOMMISSIONED;
        } else if (error.contains("does not exist or you do not have access to it")) {
            return ModelStatus.INVALID;
        } else {
            return ModelStatus.UNKNOWN;
        }
    }

    private ModelStatus resolveStatus(final Throwable throwable) {
        if (Stream
                .of(DotAIModelNotFoundException.class, DotAIModelNotOperationalException.class)
                .anyMatch(exception -> exception.isInstance(throwable))) {
            return ModelStatus.INVALID;
        }


        if (throwable instanceof GenericHttpStatusCodeException) {
            final GenericHttpStatusCodeException statusCodeException = (GenericHttpStatusCodeException) throwable;
            final Response.Status status = Response.Status.fromStatusCode(statusCodeException.getResponse().getStatus());
            if (status == Response.Status.NOT_FOUND) {
                return ModelStatus.INVALID;
            }
        }

        return ModelStatus.UNKNOWN;
    }
}
