package com.dotcms.browser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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

    // ---------------------------------------------------------------------------------------
    // Title scope query building (issue #37479).
    //
    // The title field is indexed with the standard tokenizer, which treats punctuation as word
    // separators — "COVID-19" is stored as the tokens "covid" and "19" (verified against the
    // Lucene 8.7.0 StandardAnalyzer this index uses). The expected clauses below are pinned to
    // that fact.
    // ---------------------------------------------------------------------------------------

    /**
     * A hyphenated term must become one mandatory clause per WORD, not one fused word: stripping
     * the separator turned "COVID-19" into "COVID19" — a word no document contains — so a title
     * findable in All Fields vanished from Title scope. The analyzer stores "covid" and "19", so
     * the prefix queries must be split the same way.
     *
     * <p>Only the first word carries {@code title_dotraw} — see
     * {@code buildTitleScopedQuery_multiWordTerm_onlyFirstWordCarriesTitleDotraw}.</p>
     */
    @Test
    public void buildTitleScopedQuery_hyphenatedTerm_splitsIntoMandatoryWordPrefixes() {
        assertEquals(
                "+(title:COVID* title_dotraw:COVID*) +title:19*",
                BrowserAPIImpl.buildTitleScopedQuery("COVID-19"));
    }

    /** Same mechanism, other analyzer separators: the words on both sides stay reachable. */
    @Test
    public void buildTitleScopedQuery_slashedTerm_splitsIntoMandatoryWordPrefixes() {
        assertEquals(
                "+(title:input* title_dotraw:input*) +title:output*",
                BrowserAPIImpl.buildTitleScopedQuery("input/output"));
    }

    /**
     * Punctuation at a token's edges splits exactly like it stripped (the empty side is dropped),
     * so the punctuated-paste cases keep working: "(XETRA:" reaches the analyzer's "xetra" token.
     */
    @Test
    public void buildTitleScopedQuery_punctuatedToken_reachesTheStoredWord() {
        assertEquals(
                "+(title:XETRA* title_dotraw:XETRA*)",
                BrowserAPIImpl.buildTitleScopedQuery("(XETRA:"));
    }

    /**
     * Wildcards are query intent, not word separators: dropped, keeping the term as one token.
     * Splitting them would leave fragments like ".txt" that no analyzed token starts with and
     * that, being mandatory, would sink the whole search.
     */
    @Test
    public void buildTitleScopedQuery_wildcardsAreDropped_notSplit() {
        assertEquals(
                "+(title:file.txt* title_dotraw:file.txt*)",
                BrowserAPIImpl.buildTitleScopedQuery("file*.txt"));
    }

    /** Underscore and dot are not query syntax: a dotted file title survives as one token. */
    @Test
    public void buildTitleScopedQuery_dottedFileName_staysOneToken() {
        assertEquals(
                "+(title:IMG_1004.jpeg* title_dotraw:IMG_1004.jpeg*)",
                BrowserAPIImpl.buildTitleScopedQuery("IMG_1004.jpeg"));
    }

    /**
     * A term that is ONLY query syntax carries no usable word. It must match nothing — returning
     * no clause at all would drop the text constraint and return the whole folder, the opposite
     * of All Fields, which matches nothing for the same input.
     */
    @Test
    public void buildTitleScopedQuery_termOfOnlyQuerySyntax_matchesNothing() {
        assertEquals("+title:* -title:*", BrowserAPIImpl.buildTitleScopedQuery("***"));
        assertEquals("+title:* -title:*", BrowserAPIImpl.buildTitleScopedQuery("/"));
    }

    /** A multi-word term keeps one mandatory clause per word — the injection-shaped terms too. */
    @Test
    public void buildTitleScopedQuery_multiWordTerm_oneMandatoryClausePerWord() {
        assertEquals(
                "+(title:mixed* title_dotraw:mixed*) +title:case*",
                BrowserAPIImpl.buildTitleScopedQuery("mixed case"));
    }

    /**
     * {@code title_dotraw} is the WHOLE raw title as one keyword term, so a prefix match against
     * it can only ever succeed for the very first word of the search term — no word after it can
     * be a prefix of the full title string. Carrying it on every word (the pre-#37554-review
     * shape) paid the cost of a prefix search over a near-one-term-per-document keyword
     * dictionary on every word, for a clause that could only ever contribute on the first.
     */
    @Test
    public void buildTitleScopedQuery_multiWordTerm_onlyFirstWordCarriesTitleDotraw() {
        assertEquals(
                "+(title:three* title_dotraw:three*) +title:word* +title:title*",
                BrowserAPIImpl.buildTitleScopedQuery("three word title"));
    }

    /**
     * Prefix-only is a deliberate, signed-off trade-off (see buildTitleScopedQuery's Javadoc,
     * "No leading wildcard"), and searching a file EXTENSION is the case where it bites hardest:
     * "." is not query syntax, so ".css" stays one token, and the clause becomes a mandatory
     * prefix search for a title token starting with ".css" literally. "plugin.css" is indexed as
     * ONE token (a single "." between letters does not split under the standard analyzer's
     * word-break rules), and that token does not start with ".css" — it starts with "plugin". All
     * Fields still finds it, via {@code title_dotraw:*.css*}, the leading-wildcard substring
     * clause Title scope exists specifically to avoid paying for.
     */
    @Test
    public void buildTitleScopedQuery_fileExtensionTerm_isPrefixOnly_doesNotSubstringMatch() {
        assertEquals(
                "+(title:.css* title_dotraw:.css*)",
                BrowserAPIImpl.buildTitleScopedQuery(".css"));
    }

    /**
     * {@code >}, {@code <} and {@code =} are reserved by the {@code query_string} RANGE syntax
     * ({@code field:>value}) but are not in {@code LuceneQueryUtils.LUCENE_SPECIAL_CHARS}, so
     * before this fix they survived a split untouched and {@code title:>2024*} was parsed by
     * Elasticsearch as a range query instead of the intended prefix search. They must now split
     * like any other separator rather than fuse onto the adjacent word.
     */
    @Test
    public void buildTitleScopedQuery_rangeOperatorChars_splitAsSeparators() {
        assertEquals(
                "+(title:Sales* title_dotraw:Sales*) +title:2024*",
                BrowserAPIImpl.buildTitleScopedQuery("Sales > 2024"));
        assertEquals(
                "+(title:a* title_dotraw:a*) +title:b*",
                BrowserAPIImpl.buildTitleScopedQuery("a<b"));
        assertEquals(
                "+(title:x* title_dotraw:x*) +title:y*",
                BrowserAPIImpl.buildTitleScopedQuery("x=y"));
    }

    // ---------------------------------------------------------------------------------------
    // All Fields scope query building (issue #37532, customer ticket 39185).
    //
    // Forked from GlobalSearchAttributeStrategy rather than built on top of it — see
    // buildAllFieldsScopedQuery's Javadoc — so the Search portlet's and the Relationships
    // dialog's existing wildcard-aware behavior stays untouched.
    // GlobalSearchAttributeStrategyBaselineTest pins that unchanged behavior on the other side
    // of the fork.
    // ---------------------------------------------------------------------------------------

    /** The Lucene {@code query_string} reserved set, as documented on {@code LuceneQueryUtils}. */
    private static final char[] RESERVED_LUCENE_CHARS = {
            '\\', '+', '-', '!', '(', ')', ':', '^', '[', ']', '"', '{', '}', '~', '*', '?', '|',
            '&', '/'
    };

    /** The mandatory gate — everything up to the first {@code )} — is where matching is decided. */
    private static String gateOf(final String query) {
        return query.substring(0, query.indexOf(')') + 1);
    }

    /**
     * The defect in one assertion: the gate must not carry a raw reserved character. This is what
     * made the ticket 39185 headline unfindable in Content Drive's All Fields scope.
     */
    @Test
    public void buildAllFieldsScopedQuery_mandatoryGate_escapesReservedCharacters() {
        final String gate = gateOf(BrowserAPIImpl.buildAllFieldsScopedQuery("angular-cms"));
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
    public void buildAllFieldsScopedQuery_everyReservedCharacter_isEscapedEverywhere() {
        for (final char c : RESERVED_LUCENE_CHARS) {
            final String term = "a" + c + "b";
            final String result = BrowserAPIImpl.buildAllFieldsScopedQuery(term);
            assertTrue("Reserved character '" + c + "' must be escaped somewhere in: " + result,
                    result.contains("a\\" + c + "b"));
            assertFalse("Reserved character '" + c + "' left unescaped in the gate: " + result,
                    gateOf(result).contains("catchall:" + term));
        }
    }

    /**
     * A forward slash is reserved by the {@code query_string} syntax but is absent from
     * {@code GlobalSearchAttributeStrategy}'s legacy private escape set — #37532's fifth
     * acceptance criterion, stated as a test of its own because it is the one character that set
     * silently omits.
     */
    @Test
    public void buildAllFieldsScopedQuery_forwardSlash_isEscaped() {
        final String result = BrowserAPIImpl.buildAllFieldsScopedQuery("a/b");
        assertTrue("A forward slash must be escaped: " + result, result.contains("a\\/b"));
        assertFalse("A raw forward slash must not survive: " + result, result.contains("a/b"));
    }

    /**
     * The {@code *} wildcard this method appends is syntax it adds itself, so it must sit OUTSIDE
     * the escaped token. Escaping it would turn a prefix search into a literal search for an
     * asterisk.
     */
    @Test
    public void buildAllFieldsScopedQuery_appendedWildcard_isNotItselfEscaped() {
        final String result = BrowserAPIImpl.buildAllFieldsScopedQuery("pricing");
        assertTrue("The catchall prefix wildcard must remain live syntax: " + result,
                result.contains("catchall:pricing*"));
        assertFalse("The appended wildcard must not be escaped: " + result,
                result.contains("pricing\\*"));
    }

    /** {@code "a  b"} must not produce a term-less {@code title:^5} clause. */
    @Test
    public void buildAllFieldsScopedQuery_consecutiveSeparators_emitNoEmptyClause() {
        final String result = BrowserAPIImpl.buildAllFieldsScopedQuery("a  b");
        assertFalse("An empty token produced a term-less clause: " + result,
                result.contains("title:^5"));
    }

    /** A term made only of separators yields no boost clauses at all rather than empty ones. */
    @Test
    public void buildAllFieldsScopedQuery_separatorsOnlyTerm_emitsNoEmptyClause() {
        final String result = BrowserAPIImpl.buildAllFieldsScopedQuery("  ,  ");
        assertFalse("A separators-only term produced a term-less clause: " + result,
                result.contains("title:^5"));
    }

    /** An ordinary term produces the same shape All Fields search has always used. */
    @Test
    public void buildAllFieldsScopedQuery_ordinaryTerm_matchesExpectedShape() {
        assertEquals(
                "+(catchall:pricing*^10 OR title_dotraw:*pricing*^2) "
                        + "title:'pricing'^15 "
                        + "title:pricing*",
                BrowserAPIImpl.buildAllFieldsScopedQuery("pricing"));
    }

    /** Multi-word ordinary terms keep one boost clause per token. */
    @Test
    public void buildAllFieldsScopedQuery_ordinaryMultiWordTerm_matchesExpectedShape() {
        assertEquals(
                "+(catchall:hello world*^10 OR title_dotraw:*hello world*^2) "
                        + "title:'hello world'^15 "
                        + "title:hello^5 title:world^5 "
                        + "title:hello world*",
                BrowserAPIImpl.buildAllFieldsScopedQuery("hello world"));
    }
}
