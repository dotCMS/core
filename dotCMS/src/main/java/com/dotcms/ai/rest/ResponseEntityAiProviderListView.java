package com.dotcms.ai.rest;

import com.dotcms.ai.client.langchain4j.ProviderMetadata;
import com.dotcms.rest.ResponseEntityView;

import java.util.List;

/**
 * Entity View wrapping the dotAI provider configuration metadata list response.
 */
public class ResponseEntityAiProviderListView extends ResponseEntityView<List<ProviderMetadata>> {
    public ResponseEntityAiProviderListView(final List<ProviderMetadata> entity) {
        super(entity);
    }
}
