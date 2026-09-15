package com.dotcms.rest.api.v1.content.search.strategies;

import static org.junit.Assert.assertEquals;

import com.dotcms.rest.api.v1.content.search.handlers.FieldContext;
import org.junit.Test;

/**
 * Characterization tests pinning the query {@link GlobalSearchAttributeStrategy} produces
 * <b>before</b> the escaping fix of issue #37532, so that "All Fields results are unchanged" can be
 * proved rather than asserted.
 *
 * <p>The spec's FR-009 promises no regression for All Fields search, with <b>exactly one</b>
 * written carve-out: terms containing Lucene reserved characters or consecutive separators, whose
 * results change by design because today's behavior for them is a malformed query. This class
 * draws that line concretely:</p>
 *
 * <ul>
 *   <li><b>Invariant cases</b> — a term with no reserved character must produce a byte-identical
 *       query after the fix. These assertions must never be edited.</li>
 *   <li><b>Carve-out cases</b> — a term with a reserved character produced a malformed query
 *       before the fix. These assertions were <b>updated on 2026-09-14</b> when the fix landed, each
 *       carrying its before/after in a comment. They are the complete list of pre-existing test
 *       expectations this work changed (SC-002).</li>
 * </ul>
 *
 * @see <a href="https://github.com/dotCMS/core/issues/37532">#37532</a>
 */
public class GlobalSearchAttributeStrategyBaselineTest {

    private static String query(final String value) {
        return new GlobalSearchAttributeStrategy().generateQuery(
                new FieldContext.Builder().withFieldName("title").withFieldValue(value).build());
    }

    // ---------------------------------------------------------------------------------------
    // Invariant: no reserved characters. These MUST stay byte-identical after the fix (FR-009).
    // ---------------------------------------------------------------------------------------

    /** A single plain word — the most common search there is. */
    @Test
    public void baseline_plainWord() {
        assertEquals(
                "+(catchall:pricing*^10 OR title_dotraw:*pricing*^2) "
                        + "title:'pricing'^15 "
                        + "title:pricing*",
                query("pricing"));
    }

    /** Multiple words: a space is not a Lucene reserved character, so this is invariant too. */
    @Test
    public void baseline_multipleWords() {
        assertEquals(
                "+(catchall:hello world*^10 OR title_dotraw:*hello world*^2) "
                        + "title:'hello world'^15 "
                        + "title:hello^5 title:world^5 "
                        + "title:hello world*",
                query("hello world"));
    }

    // ---------------------------------------------------------------------------------------
    // Carve-out: reserved characters. These record TODAY'S BROKEN OUTPUT and are expected to
    // change when #37532 is fixed. Each one is a query Elasticsearch cannot parse.
    // ---------------------------------------------------------------------------------------

    /**
     * A hyphen is reserved. Note the asymmetry that is the whole defect: the mandatory gate carries
     * the RAW {@code angular-cms} while only the trailing clause is escaped to
     * {@code angular\-cms}. After the fix every clause must be escaped.
     */
    @Test
    public void baseline_reservedCharacter_hyphen_isEscapedInEveryClause() {
        // CHANGED by the #37532 fix. Before, the gate carried the RAW term and only the trailing
        // clause was escaped:
        //   +(catchall:angular-cms*^10 OR title_dotraw:*angular-cms*^2) title:'angular-cms'^15 ...
        assertEquals(
                "+(catchall:angular\\-cms*^10 OR title_dotraw:*angular\\-cms*^2) "
                        + "title:'angular\\-cms'^15 "
                        + "title:angular\\-cms*",
                query("angular-cms"));
    }

    /**
     * A forward slash is reserved by the {@code query_string} syntax but is absent from this
     * strategy's private escape set, so it is not escaped <b>anywhere</b> — not even in the final
     * clause. This is #37532's fifth acceptance criterion, reproduced.
     */
    @Test
    public void baseline_forwardSlash_isEscaped() {
        // CHANGED by the #37532 fix. Before, "/" was absent from the private escape set so it was
        // escaped NOWHERE — the issue's fifth acceptance criterion:
        //   +(catchall:a/b*^10 OR title_dotraw:*a/b*^2) title:'a/b'^15 title:a/b*
        assertEquals(
                "+(catchall:a\\/b*^10 OR title_dotraw:*a\\/b*^2) "
                        + "title:'a\\/b'^15 "
                        + "title:a\\/b*",
                query("a/b"));
    }

    /**
     * Consecutive separators split into an empty token, emitting a term-less {@code title:^5}
     * clause that cannot parse (FR-028).
     */
    @Test
    public void baseline_consecutiveSpaces_emitNoTermlessClause() {
        // CHANGED by the #37532 fix (FR-028). Before, the empty token between the two spaces
        // produced a term-less clause that could not parse:
        //   ... title:a^5 title:^5 title:b^5 ...
        assertEquals(
                "+(catchall:a  b*^10 OR title_dotraw:*a  b*^2) "
                        + "title:'a  b'^15 "
                        + "title:a^5 title:b^5 "
                        + "title:a  b*",
                query("a  b"));
    }
}
