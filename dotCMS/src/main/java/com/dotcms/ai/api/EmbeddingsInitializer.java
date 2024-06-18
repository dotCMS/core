package com.dotcms.ai.api;

import com.dotcms.ai.listener.EmbeddingContentListener;
import com.dotcms.config.DotInitializer;
import com.dotmarketing.business.APILocator;

/**
 *  Initializes the {@link EmbeddingContentListener}.
 */
public class EmbeddingsInitializer implements DotInitializer {

    private static final EmbeddingContentListener LISTENER = new EmbeddingContentListener();

    @Override
    public void init() {
        APILocator.getLocalSystemEventsAPI().subscribe(LISTENER);
    }

}
