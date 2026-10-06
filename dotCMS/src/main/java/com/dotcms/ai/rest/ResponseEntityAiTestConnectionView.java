package com.dotcms.ai.rest;

import com.dotcms.ai.client.langchain4j.TestConnectionResult;
import com.dotcms.rest.ResponseEntityView;

/**
 * Entity View wrapping the dotAI provider connection test result response.
 */
public class ResponseEntityAiTestConnectionView extends ResponseEntityView<TestConnectionResult> {
    public ResponseEntityAiTestConnectionView(final TestConnectionResult entity) {
        super(entity);
    }
}
