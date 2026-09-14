package com.dotcms.rest.api.v1.content.search.strategies;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.dotcms.rest.api.v1.content.search.handlers.FieldContext;
import org.junit.Test;

/**
 * Tests for {@link GlobalSearchAttributeStrategy} covering issue #37532: a search term must be
 * matched as <b>literal text</b>, never as query syntax.
 *
 * <p>The defect these tests pin down is an asymmetry. The strategy escapes only its final clause,
 * so the mandatory gate — the clause that decides whether a document matches at all — is built from
 * the raw user input. A title containing {@code :} or {@code (} therefore produces a query
 * Elasticsearch cannot parse, the failure is swallowed, and the author is told their content does
 * not exist. See customer ticket 39185.</p>
 *
 * <p>Today's (broken) output is pinned separately in
 * {@link GlobalSearchAttributeStrategyBaselineTest}, which is what makes the change visible in
 * review rather than implicit.</p>
 *
 * @see <a href="https://github.com/dotCMS/core/issues/37532">#37532</a>
 */
public class GlobalSearchAttributeStrategyTest {

    /** The Lucene {@code query_string} reserved set, as documented on {@code LuceneQueryUtils}. */
    private static final char[] RESERVED = {
            '\\', '+', '-', '!', '(', ')', ':', '^', '[', ']', '"', '{', '}', '~', '*', '?', '|',
            '&', '/'
    };

    private static String query(final String value) {
        return new GlobalSearchAttributeStrategy().generateQuery(
                new FieldContext.Builder().withFieldName("title").withFieldValue(value).build());
    }

    /** The mandatory gate — everything up to the first {@code )} — is where matching is decided. */
    private static String gateOf(final String query) {
        return query.substring(0, query.indexOf(')') + 1);
    }

    // ---------------------------------------------------------------------------------------
    // FR-027 — every clause is escaped, not only the last one.
    // ---------------------------------------------------------------------------------------

    /**
     * The defect in one assertion: the gate must not carry a raw reserved character. This is what
     * makes the ticket 39185 headline unfindable today.
     */
    @Test
    public void mandatoryGate_escapesReservedCharacters() {
        final String gate = gateOf(query("angular-cms"));
        assertTrue("The mandatory gate must carry the ESCAPED term, not the raw one: " + gate,
                gate.contains("angular\\-cms"));
        assertFalse("The gate must not contain the unescaped hyphen: " + gate,
                gate.contains("catchall:angular-cms"));
    }

    /**
     * Every character of the reserved set must be escaped, in every clause. A single unescaped
     * occurrence anywhere is enough to break parsing of the whole query.
     */
    @Test
    public void everyReservedCharacter_isEscapedEverywhere() {
        for (final char c : RESERVED) {
            final String term = "a" + c + "b";
            final String result = query(term);
            final String unescaped = "a" + c + "b";
            // The escaped form is what must appear; the bare form must not survive anywhere
            // except as part of the escaped sequence.
            assertTrue("Reserved character '" + c + "' must be escaped somewhere in: " + result,
                    result.contains("a\\" + c + "b"));
            assertFalse("Reserved character '" + c + "' left unescaped in the gate: " + result,
                    gateOf(result).contains("catchall:" + unescaped));
        }
    }

    /**
     * A forward slash is reserved by the {@code query_string} syntax but is absent from the
     * strategy's historical private escape set — #37532's fifth acceptance criterion, stated as a
     * test of its own because it is the one character the old set silently omitted.
     */
    @Test
    public void forwardSlash_isEscaped() {
        final String result = query("a/b");
        assertTrue("A forward slash must be escaped: " + result, result.contains("a\\/b"));
        assertFalse("A raw forward slash must not survive: " + result, result.contains("a/b"));
    }

    /**
     * The {@code *} wildcards the strategy appends are syntax it adds itself, so they must sit
     * OUTSIDE the escaped token. Escaping them would turn a prefix search into a literal search
     * for an asterisk.
     */
    @Test
    public void appendedWildcards_areNotThemselvesEscaped() {
        final String result = query("pricing");
        assertTrue("The catchall prefix wildcard must remain live syntax: " + result,
                result.contains("catchall:pricing*"));
        assertFalse("The appended wildcard must not be escaped: " + result,
                result.contains("pricing\\*"));
    }

    // ---------------------------------------------------------------------------------------
    // FR-028 — consecutive separators must not emit empty clauses.
    // ---------------------------------------------------------------------------------------

    /** {@code "a  b"} must not produce a term-less {@code title:^5} clause. */
    @Test
    public void consecutiveSeparators_emitNoEmptyClause() {
        final String result = query("a  b");
        assertFalse("An empty token produced a term-less clause: " + result,
                result.contains("title:^5"));
    }

    /** A term made only of separators yields no boost clauses at all rather than empty ones. */
    @Test
    public void separatorsOnlyTerm_emitsNoEmptyClause() {
        final String result = query("  ,  ");
        assertFalse("A separators-only term produced a term-less clause: " + result,
                result.contains("title:^5"));
    }

    // ---------------------------------------------------------------------------------------
    // FR-009 — terms with no reserved character must be untouched by this change.
    // ---------------------------------------------------------------------------------------

    /**
     * The carve-out has a hard edge: an ordinary term must produce the byte-identical query it
     * produced before the fix. This is the assertion that keeps "no regression" honest.
     */
    @Test
    public void ordinaryTerm_isByteIdenticalToTheBaseline() {
        assertEquals(
                "+(catchall:pricing*^10 OR title_dotraw:*pricing*^2) "
                        + "title:'pricing'^15 "
                        + "title:pricing*",
                query("pricing"));
    }

    /** Multi-word ordinary terms keep their per-token boosts exactly as before. */
    @Test
    public void ordinaryMultiWordTerm_isByteIdenticalToTheBaseline() {
        assertEquals(
                "+(catchall:hello world*^10 OR title_dotraw:*hello world*^2) "
                        + "title:'hello world'^15 "
                        + "title:hello^5 title:world^5 "
                        + "title:hello world*",
                query("hello world"));
    }
}
