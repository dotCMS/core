package com.dotmarketing.util;

import com.liferay.util.StringPool;
import org.apache.lucene.queryparser.classic.ParseException;

/**
 * Utility class in charge of dealing with Lucene Queries that require a specific formatting, validation, or addition of
 * default parameters.
 *
 * @author Fabrizzio Araya
 * @since Jun 21, 2018
 */
public class LuceneQueryUtils {

    /**
     * Removes the {@code "query_"} prefix that might be included in the Lucene query.
     *
     * @param luceneQuery The Lucene query.
     *
     * @return The Lucene query without the {@code "query_"} prefix.
     */
    private static String removeQueryPrefix(final String luceneQuery) {
        return (luceneQuery.startsWith("query_") ? luceneQuery.replace("query_", StringPool.BLANK)
                : luceneQuery
        );
    }

    /**
     * This method basically does two things:
     * <ol>
     *     <li>Gets rid of the 'query_' prefix.</li>
     *     <li>Adds an additional condition to ensure we exclude all content of type {@code host} since access to
     *     Content Type Host is limited.</li>
     * </ol>
     *
     * @param luceneQuery The Lucene query that will be treated.
     *
     * @return The sanitized query.
     */
    public static String sanitizeBulkActionsQuery(final String luceneQuery) {
        return removeQueryPrefix(luceneQuery) + " -contentType:host";
    }

}
