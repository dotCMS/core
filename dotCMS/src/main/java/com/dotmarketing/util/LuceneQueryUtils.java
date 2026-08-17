package com.dotmarketing.util;

import com.liferay.util.StringPool;


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
     * The set of characters that are part of the Lucene {@code query_string} syntax and must be
     * backslash-escaped to be treated as literals: {@code \ + - ! ( ) : ^ [ ] " { } ~ * ? | & /}.
     * This is a stable contract of the {@code query_string} syntax that both Elasticsearch and
     * OpenSearch honor identically.
     */
    private static final String LUCENE_SPECIAL_CHARS = "\\+-!():^[]\"{}~*?|&/";

    /**
     * Backslash-escapes the Lucene {@code query_string} special characters in the given term so it
     * is treated as a literal rather than as query syntax (e.g. a hyphen, colon or slash in a user
     * value can't break query parsing). The {@code *} wildcards a caller wraps around the term must
     * be added <em>after</em> escaping so they are not themselves escaped.
     *
     * <p>This is a vendor-neutral reimplementation of {@code QueryParser.escape(String)} — a pure
     * string transform over a fixed, stable character set — so the REST-layer field strategies do
     * not depend on the Lucene {@code queryparser} artifact (which is only on the classpath
     * transitively via the Elasticsearch client and disappears with the ES→OS migration).</p>
     *
     * @param term The raw term to escape (must not be {@code null}).
     *
     * @return The term with all Lucene special characters backslash-escaped.
     */
    public static String escape(final String term) {
        return escape(term, LUCENE_SPECIAL_CHARS);
    }

    /**
     * The same set as {@link #LUCENE_SPECIAL_CHARS} minus the forward slash — see
     * {@link #escapeForWildcardTerm(String)} for why that one character has to be left alone.
     */
    private static final String WILDCARD_TERM_SPECIAL_CHARS = "\\+-!():^[]\"{}~*?|&";

    /**
     * Backslash-escapes the Lucene {@code query_string} special characters <b>except the forward
     * slash</b>, for a term that the caller wraps in {@code *} wildcards.
     *
     * <p>An escaped slash cannot match a stored one. Inside a wildcard term the {@code \} is matched
     * as a literal character, so {@code *\/store\/index*} looks for a value containing a backslash
     * and finds nothing, while {@code *&#47;store&#47;index*} matches {@code /store/index} exactly. Escaping
     * it made every filter on a URL-like value unusable: the term matched neither the analyzed field
     * nor its {@code _dotraw} keyword, and the OR of two failing clauses degenerated into matching on
     * individual path segments instead.</p>
     *
     * <p>Leaving it unescaped is safe <em>for a wildcard term specifically</em>. A {@code
     * query_string} regex term is delimited by slashes and must <b>start</b> with one, and a term
     * built for this method always starts with the {@code *} the caller prepends, so the slash can
     * never open a regex. Every other special character is still escaped, so a crafted value cannot
     * break out of the wildcard into query operators. Verified against a live index: {@code *&#47;*},
     * {@code *&#47;&#47;*}, a full {@code https://} URL and an injection-flavoured {@code *x/")OR*} all parse
     * and match correctly.</p>
     *
     * <p>Use {@link #escape(String)} for a term that is <b>not</b> wrapped in wildcards, where a
     * leading slash could legitimately be read as a regex delimiter.</p>
     *
     * @param term The raw term to escape (must not be {@code null}).
     *
     * @return The term with all Lucene special characters except {@code /} backslash-escaped.
     */
    public static String escapeForWildcardTerm(final String term) {
        return escape(term, WILDCARD_TERM_SPECIAL_CHARS);
    }

    /**
     * Backslash-escapes every character of the given term that appears in the given special-character
     * set.
     *
     * @param term         The raw term to escape.
     * @param specialChars The characters to escape.
     *
     * @return The escaped term.
     */
    private static String escape(final String term, final String specialChars) {
        final StringBuilder sb = new StringBuilder(term.length() + 8);
        for (int i = 0; i < term.length(); i++) {
            final char c = term.charAt(i);
            if (specialChars.indexOf(c) >= 0) {
                sb.append('\\');
            }
            sb.append(c);
        }
        return sb.toString();
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
