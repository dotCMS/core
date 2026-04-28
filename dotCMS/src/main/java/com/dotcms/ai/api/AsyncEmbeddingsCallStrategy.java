package com.dotcms.ai.api;

import com.dotcms.ai.rest.forms.EmbeddingsForm;
import com.dotcms.concurrent.DotConcurrentFactory;
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
        DotConcurrentFactory.getInstance().getSubmitter(OPEN_AI_THREAD_POOL_KEY).submit(new BulkEmbeddingsRunner(inodes, embeddingsForm));
    }

    @Override
    public void embed(final EmbeddingsAPIImpl embeddingsAPI,
                      final Contentlet contentlet,
                      final String content,
                      final String indexName) {
        DotConcurrentFactory.getInstance().getSubmitter(OPEN_AI_THREAD_POOL_KEY).submit(new EmbeddingsRunner(embeddingsAPI, contentlet, content, indexName));
    }

}
