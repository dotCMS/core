package com.dotcms.ai.client.openai;

import com.dotcms.ai.domain.AIResponseData;
import com.dotcms.ai.domain.ModelStatus;
import com.dotcms.ai.exception.DotAIModelNotFoundException;
import com.dotcms.rest.exception.GenericHttpStatusCodeException;
import com.dotmarketing.exception.DotRuntimeException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.Response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Tests for the OpenAIResponseEvaluator class.
 *
 * @author vico
 */
public class OpenAIResponseEvaluatorTest {

    private OpenAIResponseEvaluator evaluator;

    @Before
    public void setUp() {
        evaluator = OpenAIResponseEvaluator.get();
    }

    /**
     * Scenario: Processing a response with an error
     * Given a response with an error message "Model has been deprecated"
     * When the response is processed
     * Then the metadata should contain the error message "Model has been deprecated"
     * And the status should be set to DECOMMISSIONED
     */
    @Test
    public void testFromResponse_withError() {
        final String response = new JSONObject()
                .put("error", new JSONObject().put("message", "Model has been deprecated"))
                .toString();
        final AIResponseData metadata = new AIResponseData();

        evaluator.fromResponse(response, metadata, true);

        assertEquals("Model has been deprecated", metadata.getError());
        assertEquals(ModelStatus.DECOMMISSIONED, metadata.getStatus());
    }

    /**
     * Scenario: Processing a response with an error
     * Given a response with an error message "Model has been deprecated"
     * When the response is processed as no JSON
     * Then the metadata should contain the error message "Model has been deprecated"
     * And the status should be set to DECOMMISSIONED
     */
    @Test
    public void testFromResponse_withErrorNoJson() {
        final String response = new JSONObject()
                .put("error", new JSONObject().put("message", "Model has been deprecated"))
                .toString();
        final AIResponseData metadata = new AIResponseData();

        evaluator.fromResponse(response, metadata, false);

        assertEquals("Model has been deprecated", metadata.getError());
        assertEquals(ModelStatus.DECOMMISSIONED, metadata.getStatus());
    }

    /**
     * Scenario: Processing a response with an error
     * Given a response with an error message "Model has been deprecated"
     * When the response is processed as no JSON
     * Then the metadata should contain the error message "Model has been deprecated"
     * And the status should be set to DECOMMISSIONED
     */
    @Test
    public void testFromResponse_withoutErrorNoJson() {
        final String response = "not a json response";
        final AIResponseData metadata = new AIResponseData();

        evaluator.fromResponse(response, metadata, false);

        assertNull(metadata.getError());
        assertNull(metadata.getStatus());
    }

    /**
     * Scenario: Processing a response without an error
     * Given a response without an error message
     * When the response is processed
     * Then the metadata should not contain any error message
     * And the status should be null
     */
    @Test
    public void testFromResponse_withoutError() {
        final String response = new JSONObject().put("data", "some data").toString();
        final AIResponseData metadata = new AIResponseData();

        evaluator.fromResponse(response, metadata, true);

        assertNull(metadata.getError());
        assertNull(metadata.getStatus());
    }

    /**
     * Scenario: Processing an exception of type DotRuntimeException
     * Given an exception of type DotAIModelNotFoundException with message "Model not found"
     * When the exception is processed
     * Then the metadata should contain the error message "Model not found"
     * And the status should be set to INVALID
     * And the exception should be set to the given DotRuntimeException
     */
    @Test
    public void testFromException_withDotRuntimeException() {
        final DotRuntimeException exception = new DotAIModelNotFoundException("Model not found");
        final AIResponseData metadata = new AIResponseData();

        evaluator.fromException(exception, metadata);

        assertEquals("Model not found", metadata.getError());
        assertEquals(ModelStatus.INVALID, metadata.getStatus());
        assertEquals(exception, metadata.getException());
    }

    @Test
    public void testFromException_withGenericHttpStatusCodeException_notFound() {
        final GenericHttpStatusCodeException exception = new GenericHttpStatusCodeException(
                "Not found",
                Response.Status.NOT_FOUND);
        final AIResponseData metadata = new AIResponseData();

        evaluator.fromException(exception, metadata);

        assertEquals("HTTP 404 Not Found", metadata.getError());
        assertEquals(ModelStatus.INVALID, metadata.getStatus());
        assertEquals(exception, metadata.getException().getCause());
    }

    @Test
    public void testFromException_withGenericHttpStatusCodeException() {
        final GenericHttpStatusCodeException exception = new GenericHttpStatusCodeException(
                "Not found",
                Response.Status.BAD_REQUEST);
        final AIResponseData metadata = new AIResponseData();

        evaluator.fromException(exception, metadata);

        assertEquals("HTTP 400 Bad Request", metadata.getError());
        assertEquals(ModelStatus.UNKNOWN, metadata.getStatus());
        assertEquals(exception, metadata.getException().getCause());
    }

    /**
     * Scenario: Processing a general exception
     * Given a general exception with message "General error"
     * When the exception is processed
     * Then the metadata should contain the error message "General error"
     * And the status should be set to UNKNOWN
     * And the exception should be wrapped in a DotRuntimeException
     */
    @Test
    public void testFromException_withOtherException() {
        Exception exception = new Exception("General error");
        AIResponseData metadata = new AIResponseData();

        evaluator.fromException(exception, metadata);

        assertEquals("General error", metadata.getError());
        assertEquals(ModelStatus.UNKNOWN, metadata.getStatus());
        assertEquals(DotRuntimeException.class, metadata.getException().getClass());
    }
}
