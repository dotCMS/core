package com.dotcms.ai.api;

import com.dotcms.ai.rest.forms.EmbeddingsForm;
import com.dotmarketing.portlets.contentlet.model.Contentlet;

import java.util.List;

/**
 * The SyncEmbeddingsCallStrategy class is responsible for embedding contentlets in a synchronous manner.
 *
 * @author vico
 */
public class SyncEmbeddingsCallStrategy implements EmbeddingsCallStrategy {

    @Override
    public void bulkEmbed(final List<String> inodes, final EmbeddingsForm embeddingsForm) {
        new BulkEmbeddingsRunner(inodes, embeddingsForm).run();
    }

    @Override
    public void embed(final EmbeddingsAPIImpl embeddingsAPI,
                      final Contentlet contentlet,
                      final String content,
                      final String indexName) {
        new EmbeddingsRunner(embeddingsAPI, contentlet, content, indexName).run();
    }
}
