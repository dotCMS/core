package com.dotmarketing.util;

import com.liferay.util.StringPool;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.search.*;
import org.apache.lucene.index.Term;
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


    /**
     * Uses Lucene APIs to deeply analyze the parsed query structure
     * to determine if it represents an intentional Lucene query
     */
    public static boolean isLuceneQuery(String query) {
        if (query == null || query.trim().isEmpty()) {
            return false;
        }

        String trimmed = query.trim();

        // Lista positiva de características de Lucene
        return trimmed.contains(":") ||                           // field:value
                trimmed.matches(".*\\b(AND|OR|NOT)\\b.*") ||       // boolean operators
                trimmed.contains("*") ||                           // wildcards
                trimmed.contains("?") ||                           // single char wildcard
                trimmed.startsWith("\"") && trimmed.endsWith("\"") || // phrase query
                trimmed.contains("~") ||                           // fuzzy/proximity
                trimmed.matches(".*\\^\\d+(\\.\\d+)?") ||          // boost
                trimmed.matches(".*[\\[\\{].*TO.*[\\]\\}].*") ||   // range queries
                trimmed.contains("(") && trimmed.contains(")") ||   // grouping
                trimmed.matches(".*\\+\\w+.*") ||                  // required terms
                trimmed.matches(".*-\\w+.*") && !isSimpleIdentifier(trimmed); // prohibited terms
    }

    private static boolean isSimpleIdentifier(String query) {
        // Exclude common identifier patterns from being treated as Lucene queries
        return query.matches("^\\w+[\\-_]\\w+$") ||        // word-word, word_word
                query.matches("^\\w+\\-\\d+$") ||            // word-123
                query.matches("^\\d+\\-\\d+$") ||            // 123-456
                query.matches("^[a-zA-Z0-9\\-_]+$");         // simple alphanumeric with hyphens
    }


}
