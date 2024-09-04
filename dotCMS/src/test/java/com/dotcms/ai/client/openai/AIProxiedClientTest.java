package com.dotcms.ai.client.openai;

import com.dotcms.ai.client.AIClient;
import com.dotcms.ai.client.AIClientStrategy;
import com.dotcms.ai.client.AIProxiedClient;
import com.dotcms.ai.client.AIProxyStrategy;
import com.dotcms.ai.client.AIRequest;
import com.dotcms.ai.domain.AIResponse;
import com.dotcms.ai.client.AIResponseEvaluator;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.Serializable;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the AIProxiedClient class.
 *
 * @author vico
 */
public class AIProxiedClientTest {

    private AIClient mockClient;
    private AIProxyStrategy mockProxyStrategy;
    private AIClientStrategy mockClientStrategy;
    private AIResponseEvaluator mockResponseEvaluator;
    private AIProxiedClient proxiedClient;

    @Before
    public void setUp() {
        mockClient = mock(AIClient.class);
        mockProxyStrategy = mock(AIProxyStrategy.class);
        mockClientStrategy = mock(AIClientStrategy.class);
        when(mockProxyStrategy.getStrategy()).thenReturn(mockClientStrategy);
        mockResponseEvaluator = mock(AIResponseEvaluator.class);
        proxiedClient = AIProxiedClient.of(mockClient, mockProxyStrategy, mockResponseEvaluator);
    }

    /**
     * Scenario: Sending a valid AI request
     * Given a valid AI request
     * When the request is sent to the AI service
     * Then the strategy should be applied
     * And the response should be written to the output stream
     */
    @Test
    public void testSendToAI_withValidRequest() {
        AIRequest<Serializable> request = mock(AIRequest.class);
        OutputStream output = mock(OutputStream.class);

        AIResponse response = proxiedClient.sendToAI(request, output);

        verify(mockClientStrategy).applyStrategy(mockClient, mockResponseEvaluator, request, output);
        assertEquals(AIResponse.EMPTY, response);
    }

    /**
     * Scenario: Sending an AI request with null output stream
     * Given a valid AI request and a null output stream
     * When the request is sent to the AI service
     * Then the strategy should be applied
     * And the response should be returned as a string
     */
    @Test
    public void testSendToAI_withNullOutput() {
        AIRequest<Serializable> request = mock(AIRequest.class);
        AIResponse response = proxiedClient.sendToAI(request, null);

        verify(mockClientStrategy).applyStrategy(
                eq(mockClient),
                eq(mockResponseEvaluator),
                eq(request),
                any(OutputStream.class));
        assertEquals("", response.getResponse());
    }

    /**
     * Scenario: Sending an AI request with NOOP client
     * Given a valid AI request and a NOOP client
     * When the request is sent to the AI service
     * Then no operations should be performed
     * And the response should be empty
     */
    @Test
    public void testSendToAI_withNoopClient() {
        proxiedClient = AIProxiedClient.NOOP;
        AIRequest<Serializable> request = AIRequest.builder().build();
        OutputStream output = new ByteArrayOutputStream();

        AIResponse response = proxiedClient.sendToAI(request, output);

        assertEquals(AIResponse.EMPTY, response);
    }
}