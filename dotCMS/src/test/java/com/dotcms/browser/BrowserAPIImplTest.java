package com.dotcms.browser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.dotmarketing.portlets.contentlet.model.Contentlet;
import java.util.List;
import java.util.Set;

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

    // --- collectWarmUpUserIds (issue #37186, FR-001 warm-up set) ---------------------------
    //
    // These are pure in-memory tests: they build plain Contentlet objects (no DB, no
    // APILocator) and assert on the distinct id set the warm-up pass would resolve before
    // hydrateContentletsInParallel runs. They do NOT prove the thundering-herd race is fixed —
    // that requires a real cache and real concurrency, which is what the dotcms-integration
    // test (BrowserAPITest) covers.

    private static Contentlet contentletWith(final String modUser, final String owner) {
        final Contentlet c = new Contentlet();
        if (modUser != null) {
            c.setModUser(modUser);
        }
        if (owner != null) {
            c.setOwner(owner);
        }
        return c;
    }

    /** Two rows authored by the same user collapse to one id — this is the whole point of warming up before the parallel fan-out, not once per row. */
    @Test
    public void collectWarmUpUserIds_dedupesRepeatedModUser() {
        final List<Contentlet> page = List.of(
                contentletWith("user-a", "user-a"),
                contentletWith("user-a", "user-a"));
        final Set<String> ids = BrowserAPIImpl.collectWarmUpUserIds(page);
        assertEquals(Set.of("user-a"), ids);
    }

    /** modUser and owner are independent fields; both must be collected when they differ. */
    @Test
    public void collectWarmUpUserIds_collectsDistinctModUserAndOwner() {
        final List<Contentlet> page = List.of(contentletWith("author-1", "owner-1"));
        final Set<String> ids = BrowserAPIImpl.collectWarmUpUserIds(page);
        assertEquals(Set.of("author-1", "owner-1"), ids);
    }

    /** A page with N distinct authors across many rows yields exactly N ids — the number SC-001's DB-lookup count must match. */
    @Test
    public void collectWarmUpUserIds_manyRowsFewAuthors_yieldsOneIdPerAuthor() {
        final List<Contentlet> page = List.of(
                contentletWith("author-1", "author-1"),
                contentletWith("author-1", "author-1"),
                contentletWith("author-2", "author-2"),
                contentletWith("author-1", "author-1"),
                contentletWith("author-3", "author-3"));
        final Set<String> ids = BrowserAPIImpl.collectWarmUpUserIds(page);
        assertEquals(Set.of("author-1", "author-2", "author-3"), ids);
    }

    /** An empty page needs no warm-up at all. */
    @Test
    public void collectWarmUpUserIds_emptyPage_yieldsEmptySet() {
        assertTrue(BrowserAPIImpl.collectWarmUpUserIds(List.of()).isEmpty());
    }

    /**
     * locked-by is deliberately excluded from the warm-up set (plan.md Legacy Impact carry-forward
     * note 1: resolving it costs a real per-contentlet {@code getLockedBy} call, not a free field
     * read, so pulling it into the sequential warm-up would add new serial work per row instead of
     * per distinct author). This test only documents the id sources actually read
     * ({@code modUser}/{@code owner}); it cannot assert an absence of locked-by handling since
     * {@code collectWarmUpUserIds} never touches locking at all by construction.
     */
    @Test
    public void collectWarmUpUserIds_ignoresLockStateEntirely() {
        final Contentlet locked = contentletWith("author-1", "author-1");
        locked.setInode("some-inode"); // locking is keyed off inode/versionable state, not read here
        final Set<String> ids = BrowserAPIImpl.collectWarmUpUserIds(List.of(locked));
        assertEquals(Set.of("author-1"), ids);
    }
}
