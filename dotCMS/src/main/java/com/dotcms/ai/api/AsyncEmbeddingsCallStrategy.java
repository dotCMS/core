package com.dotcms.ai.api;

import com.dotcms.ai.rest.forms.EmbeddingsForm;
import com.dotcms.ai.util.OpenAIThreadPool;
import com.dotmarketing.portlets.contentlet.model.Contentlet;

import java.util.List;

/**
 * The AsyncEmbeddingsCallStrategy class is responsible for embedding contentlets in an asynchronous manner.
 *
 * @author vico
 */
public class AsyncEmbeddingsCallStrategy implements EmbeddingsCallStrategy {

    @Override
    public void bulkEmbed(final List<String> inodes, final EmbeddingsForm embeddingsForm) {
        OpenAIThreadPool.submit(new BulkEmbeddingsRunner(inodes, embeddingsForm));
    }

    @Override
    public void embed(final EmbeddingsAPIImpl embeddingsAPI,
                      final Contentlet contentlet,
                      final String content,
                      final String indexName) {
        OpenAIThreadPool.submit(new EmbeddingsRunner(embeddingsAPI, contentlet, content, indexName));
    }

}
