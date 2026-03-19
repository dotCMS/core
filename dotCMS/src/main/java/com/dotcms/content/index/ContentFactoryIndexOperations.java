package com.dotcms.content.index;

import com.dotcms.content.index.domain.SearchHits;
import com.dotcms.content.model.annotation.IndexLibraryIndependent;
import com.dotmarketing.common.model.ContentletSearch;
import com.dotmarketing.util.PaginatedArrayList;
import com.liferay.portal.model.User;
import java.util.List;


/**
 * Service interface that abstracts index operations for content factory functionality.
 * This interface provides a clean abstraction layer that can be implemented by different
 * search engine providers (Elasticsearch, OpenSearch, etc.) without exposing provider-specific types.
 *
 * @author Fabrizio Araya
 */
@IndexLibraryIndependent
public interface ContentFactoryIndexOperations {

    /**
     * Given a query this method resolves what index should be used to execute it against.
     * This method analyzes the query to determine whether it should target the live or working index
     * based on the presence of specific query parameters like "+live:true" or "+deleted:true".
     *
     * @param query a Lucene query string to analyze
     * @return the index name to execute the query against, or null if no suitable index is found
     */
    String inferIndexToHit(final String query);

    /**
     * This will return a count of the number of elements matching the query.
     * The count operation is optimized for performance and may use caching if enabled.
     *
     * @param query a Lucene query string to count matches for
     * @return the number of items matching the query, or -1 if an error occurs
     */
    long indexCount(final String query);

    /**
     * Returns a representation of search hits matching the given query.
     * This method provides access to search results with pagination and sorting capabilities
     * while abstracting away the underlying search engine implementation details.
     *
     * @param query a Lucene query string to search for
     * @param limit maximum number of items to return (0 means no limit)
     * @param offset start-at position for pagination (0-based)
     * @param sortBy sort criteria (e.g., "score desc", "moddate asc", "title desc")
     * @return SearchHits representation containing the search results and metadata
     */
    SearchHits searchHits(String query, int limit, int offset, String sortBy);

    /**
     * This returns a list of inodes matching the given query.
     * This is a simplified search method that only returns the inode identifiers
     * of matching documents, useful for lightweight operations that only need document IDs.
     *
     * @param query a Lucene query string to search for
     * @param limit maximum number of items to return
     * @param offset start-at position for pagination (0-based)
     * @return list of matching inodes as strings
     */
    List<String> search(String query, int limit, int offset);

    /**
     * Performs a scroll-based search operation to efficiently retrieve large result sets.
     * This method uses the search engine's scroll API to handle datasets that exceed
     * the maximum result window limits. The scroll operation loads all matching results
     * and returns them as a paginated list.
     *
     * @param query a Lucene query string to search for
     * @param sortBy sort criteria for the results (e.g., "title asc", "moddate desc")
     * @param scrollBatchSize number of results to retrieve per scroll batch for optimal memory usage
     * @return PaginatedArrayList containing all matching ContentletSearch results with total count
     */
    PaginatedArrayList<ContentletSearch> indexSearchScroll(final String query, String sortBy, int scrollBatchSize);

    /**
     * Creates an ESContentletScroll instance for scroll-based queries.
     * <p>
     * The Scroll API is designed for efficiently retrieving large result sets that exceed
     * ElasticSearch's max_result_window limit. Use this when you need to iterate through
     * thousands of results.
     * </p>
     * <p>
     * <strong>IMPORTANT:</strong> Always use try-with-resources to ensure scroll contexts
     * are properly cleaned up:
     * </p>
     * <pre>
     * try (ESContentletScroll scroll = factory.createScrollQuery(query, user, false, 100)) {
     *     List&lt;ContentletSearch&gt; batch = scroll.initialize();
     *     while (!batch.isEmpty()) {
     *         // process batch
     *         batch = scroll.nextBatch();
     *     }
     * }
     * </pre>
     *
     * @param luceneQuery Lucene query string to search for contentlets
     * @param user User for permission checking
     * @param respectFrontendRoles Whether to respect frontend roles
     * @param batchSize Number of results to retrieve per batch (page size)
     * @param sortBy Sort criteria (e.g., "title asc", "moddate desc")
     * @return ESContentletScroll instance for iterating through results
     */
    IndexContentletScroll createScrollQuery(final String luceneQuery, final User user,
            final boolean respectFrontendRoles, final int batchSize,
            final String sortBy);

    /**
     * Creates an ESContentletScroll instance for scroll-based queries with default sorting.
     * This is a convenience method that uses "title asc" as the default sort criteria.
     *
     * @param luceneQuery Lucene query string to search for contentlets
     * @param user User for permission checking
     * @param respectFrontendRoles Whether to respect frontend roles
     * @param batchSize Number of results to retrieve per batch (page size)
     * @return ESContentletScroll instance for iterating through results
     * @see #createScrollQuery(String, User, boolean, int, String)
     */
    IndexContentletScroll createScrollQuery(final String luceneQuery, final User user,
            final boolean respectFrontendRoles, final int batchSize);
}