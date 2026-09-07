package com.dotcms.browser;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

/**
 * Unit tests for {@link BrowserAPIImpl#jsonEscape(String)} — the escaping that lets a per-field
 * strategy's Lucene query be safely embedded as the string value of the ES {@code query_string}
 * request body (a hand-built JSON template).
 *
 * <p>The field strategies escape Lucene special characters with a backslash (e.g. a hyphen becomes
 * {@code angular\-cms}). A raw backslash — or a double quote from a quoted phrase — is an invalid
 * JSON escape, so without this step the whole Elasticsearch request body is malformed and the
 * search silently returns nothing. A clean term (no backslash/quote) must pass through
 * unchanged.</p>
 */
public class BrowserAPIImplTest {

    /** A term without a hyphen produces no backslash, so the JSON escaping is a no-op. */
    @Test
    public void jsonEscape_queryWithoutHyphen_isUnchanged() {
        final String query = "+(SSS.topic:*angular* SSS.topic_dotraw:*angular*)";
        assertEquals(query, BrowserAPIImpl.jsonEscape(query));
    }

    /**
     * A term with a hyphen reaches this method already Lucene-escaped ({@code angular\-cms}); the
     * single backslash must become a double backslash so the JSON request body is valid and ES
     * receives the intended {@code \-} literal.
     */
    @Test
    public void jsonEscape_queryWithHyphen_backslashIsDoubled() {
        final String luceneEscaped = "+(SSS.topic:*angular\\-cms* SSS.topic_dotraw:*angular\\-cms*)";
        final String expected = "+(SSS.topic:*angular\\\\-cms* SSS.topic_dotraw:*angular\\\\-cms*)";
        assertEquals(expected, BrowserAPIImpl.jsonEscape(luceneEscaped));
    }

    /** A double quote (from a quoted-phrase term) must also be JSON-escaped. */
    @Test
    public void jsonEscape_quoteIsEscaped() {
        assertEquals("SSS.topic:\\\"a b\\\"", BrowserAPIImpl.jsonEscape("SSS.topic:\"a b\""));
    }

    /**
     * A plain option value needs no escaping, so the clause is the bare wildcard pair. Guards against
     * an over-eager escape that would corrupt ordinary values.
     */
    @Test
    public void buildMultiValueOrClause_plainValue_isNotEscaped() {
        assertEquals("+(SSS.sections:*news* SSS.sections_dotraw:*news*)",
                BrowserAPIImpl.buildMultiValueOrClause("SSS.sections", List.of("news")));
    }

    /**
     * An option value carrying {@code query_string} syntax must be Lucene-escaped, or the unescaped
     * character fails the WHOLE query — these searches are not lenient — and the filter returns an
     * empty result set with no error at all. {@code Yes/No} is a realistic Multi-Select option: the
     * {@code /} opens a regex.
     *
     * <p>The {@code *} wildcards must stay OUTSIDE the escaped token, otherwise they are escaped
     * themselves and the contains match becomes a literal search for an asterisk.</p>
     */
    @Test
    public void buildMultiValueOrClause_valueWithLuceneSyntax_isEscaped() {
        assertEquals("+(SSS.answer:*Yes\\/No* SSS.answer_dotraw:*Yes\\/No*)",
                BrowserAPIImpl.buildMultiValueOrClause("SSS.answer", List.of("Yes/No")));
    }

    /** A colon would otherwise re-parse as {@code field:value} and break the clause. */
    @Test
    public void buildMultiValueOrClause_valueWithColon_isEscaped() {
        assertEquals("+(SSS.level:*Level\\:1* SSS.level_dotraw:*Level\\:1*)",
                BrowserAPIImpl.buildMultiValueOrClause("SSS.level", List.of("Level:1")));
    }

    /** Several values OR together inside one mandatory group, each escaped independently. */
    @Test
    public void buildMultiValueOrClause_multipleValues_eachEscapedAndOred() {
        assertEquals(
                "+(SSS.f:*N\\/A* SSS.f_dotraw:*N\\/A* SSS.f:*ok* SSS.f_dotraw:*ok*)",
                BrowserAPIImpl.buildMultiValueOrClause("SSS.f", List.of("N/A", "ok")));
    }

    /** Blank and empty values are skipped, and an all-blank list produces no clause at all. */
    @Test
    public void buildMultiValueOrClause_blankValues_produceNoClause() {
        assertEquals("", BrowserAPIImpl.buildMultiValueOrClause("SSS.f", List.of("", "   ")));
    }
}
