package com.dotcms.content.elasticsearch.business;

import com.dotmarketing.common.model.ContentletSearch;
import com.dotmarketing.exception.DotDataException;

import java.util.List;

/**
 * API for performing scroll-based queries on contentlets in ElasticSearch.
 * <p>
 * The Scroll API is designed for efficiently retrieving large result sets that exceed
 * ElasticSearch's max_result_window limit. Unlike offset-based pagination, scroll maintains
 * a search context and retrieves results in batches.
 * </p>
 * <p>
 * The scroll context is initialized automatically upon instantiation, and the first batch
 * is fetched and cached. Use {@link #nextBatch()} to retrieve batches sequentially.
 * </p>
 * <p>
 * <strong>IMPORTANT:</strong> Always close the scroll context when done to free server resources.
 * Use try-with-resources pattern:
 * </p>
 * <pre>
 * try (ESContentletScroll scroll = factory.createScrollQuery(query, user, false, 100)) {
 *     List&lt;ContentletSearch&gt; batch;
 *     while ((batch = scroll.nextBatch()) != null && !batch.isEmpty()) {
 *         // process batch
 *     }
 * }
 * </pre>
 *
 * @see ContentletFactory#createScrollQuery(String, com.liferay.portal.model.User, boolean, int)
 */
public interface ESContentletScroll extends AutoCloseable {

    /**
     * Retrieves the next batch of results from the scroll context.
     * On the first call, returns the initial batch fetched during construction.
     * Subsequent calls fetch and return the next batch.
     * Returns an empty list when there are no more results.
     *
     * @return List of ContentletSearch objects from the next batch (empty if no more results)
     * @throws DotDataException if fetching the next batch fails
     */
    List<ContentletSearch> nextBatch() throws DotDataException;

    /**
     * Returns the total number of hits for the query.
     * Available immediately after construction.
     *
     * @return Total number of matching documents
     */
    long getTotalHits();

    /**
     * Checks if there are more results available.
     *
     * @return true if more results are available, false otherwise
     */
    boolean hasMoreResults();

    /**
     * Clears the scroll context and frees server resources.
     * This is called automatically when using try-with-resources.
     * Safe to call multiple times.
     */
    @Override
    void close();
}
