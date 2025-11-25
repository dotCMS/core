package com.dotcms.ai.client;

import com.dotcms.ai.domain.AIProvider;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;

import javax.ws.rs.HttpMethod;
import java.io.OutputStream;
import java.io.Serializable;

/**
 * Interface representing an AI client capable of sending requests to an AI service.
 *
 * <p>
 * This interface defines methods for obtaining the AI provider and sending requests
 * to the AI service. Implementations of this interface should handle the specifics
 * of interacting with the AI service, including request formatting and response handling.
 * </p>
 *
 * <p>
 * The interface also provides a NOOP implementation that throws an
 * {@link UnsupportedOperationException} for all operations.
 * </p>
 *
 * @author vico
 */
public interface AIClient {

    AIClient NOOP = new AIClient() {
        @Override
        public AIProvider getProvider() {
            return AIProvider.NONE;
        }

        @Override
        public <T extends Serializable> void sendRequest(final AIRequest<T> request, final OutputStream output) {
            throwUnsupported();
        }

        private void throwUnsupported() {
            throw new UnsupportedOperationException("Noop client does not support sending requests");
        }
    };

    /**
     * Resolves the appropriate HTTP method for the given method name and URL.
     *
     * @param method the HTTP method name (e.g., "GET", "POST", "PUT", "DELETE", "patch")
     * @param url the URL to which the request will be sent
     * @return the corresponding {@link HttpUriRequest} for the given method and URL
     */
    static HttpUriRequest resolveMethod(final String method, final String url) {
        switch(method) {
            case HttpMethod.POST:
                return new HttpPost(url);
            case HttpMethod.PUT:
                return new HttpPut(url);
            case HttpMethod.DELETE:
                return new HttpDelete(url);
            case "patch":
                return new HttpPatch(url);
            case HttpMethod.GET:
            default:
                return new HttpGet(url);
        }
    }

    /**
     * Validates and casts the given AI request to a {@link JSONObjectAIRequest}.
     *
     * @param <T> the type of the request payload
     * @param request the AI request to be validated and cast
     * @return the validated and cast {@link JSONObjectAIRequest}
     * @throws UnsupportedOperationException if the request is not an instance of {@link JSONObjectAIRequest}
     */
    static <T extends Serializable> JSONObjectAIRequest useRequestOrThrow(final AIRequest<T> request) {
        // When we get rid of JSONObject usage, we can remove this check
        if (request instanceof JSONObjectAIRequest) {
            return (JSONObjectAIRequest) request;
        }

        throw new UnsupportedOperationException("Only JSONObjectAIRequest (JSONObject) is supported");
    }

    /**
     * Returns the AI provider associated with this client.
     *
     * @return the AI provider
     */
    AIProvider getProvider();

    /**
     * Sends the given AI request to the AI service and writes the response to the provided output stream.
     *
     * @param <T> the type of the request payload
     * @param request the AI request to be sent
     * @param output the output stream to which the response will be written
     * @throws Exception if any error occurs during the request execution
     */
    <T extends Serializable> void sendRequest(AIRequest<T> request, OutputStream output);

}
