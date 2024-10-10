package com.dotcms.ai.listener;

import com.dotcms.config.DotInitializer;
import com.dotmarketing.business.APILocator;

/**
 *  Initializes the {@link EmbeddingContentListener}.
 */
public class AIVisionInitializer implements DotInitializer {

    private static final OpenAIImageTaggingContentListener LISTENER = new OpenAIImageTaggingContentListener();

    @Override
    public void init() {
        APILocator.getLocalSystemEventsAPI().subscribe(LISTENER);
    }

}
