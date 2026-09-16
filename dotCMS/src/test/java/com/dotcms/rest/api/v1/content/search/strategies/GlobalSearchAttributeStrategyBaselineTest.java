package com.dotcms.rest.api.v1.content.search.strategies;

import static org.junit.Assert.assertEquals;

import com.dotcms.rest.api.v1.content.search.handlers.FieldContext;
import org.junit.Test;

/**
 * Characterization tests pinning the query {@link GlobalSearchAttributeStrategy} produces, so that
 * the Search portlet's and the Relationships dialog's global search stay byte-for-byte unchanged
 * going forward.
 *
 * <p>Issue #37532 reported that a term containing a Lucene reserved character (a colon, a
 * parenthesis, a slash) produces a query Elasticsearch cannot parse, so the mandatory gate fails
 * silently and the user is told their content does not exist. That defect is real in the output
 * pinned below — see the carve-out cases — but it is <b>deliberately not fixed in this class</b>:
 * fixing it here would also change the Search portlet's and the Relationships dialog's existing
 * wildcard-aware search behavior, which product asked to leave untouched. The literal-text fix for
 * #37532 instead lives in Content Drive's own {@code BrowserAPIImpl#buildAllFieldsScopedQuery},
 * forked from this class rather than built on top of it — see that method's Javadoc and
 * {@code BrowserAPIImplTest} for its coverage.</p>
 *
 * <p>This class exists to keep that boundary honest: if someone edits
 * {@link GlobalSearchAttributeStrategy} later — to fix this same defect here, say — these
 * assertions fail and make the Search-portlet/Relationships-dialog impact visible in review rather
 * than discovered by a customer.</p>
 *
 * <ul>
 *   <li><b>Invariant cases</b> — a term with no reserved character. Byte-identical query, and
 *       should stay that way indefinitely.</li>
 *   <li><b>Carve-out cases</b> — a term with a reserved character or consecutive separators,
 *       pinning the known-malformed output described above. These are expected to keep failing to
 *       parse in the Search portlet and Relationships dialog; that is the tracked, accepted
 *       trade-off, not a regression.</li>
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
    // Invariant: no reserved characters. Must stay byte-identical indefinitely.
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
    // Carve-out: reserved characters. These record the known-malformed output issue #37532
    // reports for the Search portlet / Relationships dialog, deliberately left as-is here.
    // ---------------------------------------------------------------------------------------

    /**
     * A hyphen is reserved. The asymmetry is the whole defect: the mandatory gate — the clause
     * that decides whether a document matches at all — carries the RAW {@code angular-cms}, and
     * only the trailing clause is escaped to {@code angular\-cms}. Elasticsearch cannot parse a
     * bare hyphen in {@code query_string} position, so the gate fails and the search returns
     * nothing for a title that exists.
     */
    @Test
    public void baseline_reservedCharacter_hyphen_onlyTrailingClauseIsEscaped() {
        assertEquals(
                "+(catchall:angular-cms*^10 OR title_dotraw:*angular-cms*^2) "
                        + "title:'angular-cms'^15 "
                        + "title:angular\\-cms*",
                query("angular-cms"));
    }

    /**
     * A forward slash is reserved by the {@code query_string} syntax but is absent from this
     * strategy's private escape set, so it is not escaped <b>anywhere</b> — not even in the
     * trailing clause. This is #37532's fifth acceptance criterion, reproduced.
     */
    @Test
    public void baseline_forwardSlash_isEscapedNowhere() {
        assertEquals(
                "+(catchall:a/b*^10 OR title_dotraw:*a/b*^2) "
                        + "title:'a/b'^15 "
                        + "title:a/b*",
                query("a/b"));
    }

    /**
     * Consecutive separators split into an empty token, emitting a term-less {@code title:^5}
     * clause that cannot parse (the FR-028 defect, also reproduced here).
     */
    @Test
    public void baseline_consecutiveSpaces_emitsTermlessClause() {
        assertEquals(
                "+(catchall:a  b*^10 OR title_dotraw:*a  b*^2) "
                        + "title:'a  b'^15 "
                        + "title:a^5 title:^5 title:b^5 "
                        + "title:a  b*",
                query("a  b"));
    }
}
