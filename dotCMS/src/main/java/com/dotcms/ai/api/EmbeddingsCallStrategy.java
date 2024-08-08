package com.dotcms.ai.api;

import com.dotcms.ai.rest.forms.EmbeddingsForm;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.ConfigUtils;

import java.util.List;

/**
 * The EmbeddingsCallStrategy interface defines the contract for embedding strategies.
 * Implementations of this interface will provide different ways to handle the embedding of contentlets.
 *
 * The embed method takes a list of inodes and an EmbeddingsForm object, and performs the embedding operation.
 * The specifics of how the embedding is done depends on the implementation.
 *
 * @author Your Name
 */
public interface EmbeddingsCallStrategy {

    String OPEN_AI_THREAD_POOL_KEY = "OpenAIThreadPool";
    /**
     * Embeds contentlets based on the provided inodes and form data.
     *
     * @param inodes the list of inodes representing the contentlets to be embedded
     * @param embeddingsForm the form data containing the details for the embedding operation
     */
    void bulkEmbed(List<String> inodes, EmbeddingsForm embeddingsForm);

    /**
     * Embeds the content of a contentlet.
     *
     * @param embeddingsAPI the EmbeddingsAPIImpl instance to use
     * @param contentlet the contentlet to embed
     * @param content the content to embed
     * @param indexName the index name to use
     */
    void embed(EmbeddingsAPIImpl embeddingsAPI, Contentlet contentlet, String content, String indexName);

    /**
     * Resolves the appropriate embedding strategy based on the current environment.
     *
     * @return the EmbeddingsCallStrategy implementation to use
     */
    static EmbeddingsCallStrategy resolveStrategy() {
        return ConfigUtils.isDevMode() ? new SyncEmbeddingsCallStrategy() : new AsyncEmbeddingsCallStrategy();
    }

}
