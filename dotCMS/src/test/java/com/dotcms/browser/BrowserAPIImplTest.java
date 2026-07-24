package com.dotcms.browser;

import static org.junit.Assert.assertEquals;

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
}
