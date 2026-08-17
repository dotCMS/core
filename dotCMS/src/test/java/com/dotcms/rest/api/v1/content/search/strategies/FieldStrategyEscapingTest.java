package com.dotcms.rest.api.v1.content.search.strategies;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ImmutableFileAssetContentType;
import com.dotcms.contenttype.model.type.ImmutablePageContentType;
import com.dotcms.contenttype.model.type.ImmutableSimpleContentType;
import com.dotcms.rest.api.v1.content.search.handlers.FieldContext;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Fast, dependency-free unit tests for the escaping of Lucene query-syntax characters in the
 * per-field search strategies. A user term containing a hyphen, colon, parenthesis, etc. must be
 * escaped so it can't break query parsing (which previously errored the whole search); the
 * {@code *} wildcards the strategy adds stay outside the escaped term. A clean term is unchanged.
 * The forward slash is the deliberate exception -- inside a wildcard term an escaped slash is
 * matched literally and so could never match a stored one.
 *
 * <p>{@link FieldContext} is built with a null Content Type, so the strategies take their plain
 * (non-URL-map) branch — no DB or index needed.</p>
 */
public class FieldStrategyEscapingTest {

    private FieldContext ctx(final String fieldName, final String value) {
        return new FieldContext.Builder().withFieldName(fieldName).withFieldValue(value).build();
    }

    // ---- TextFieldStrategy (Text / Textarea / WYSIWYG / Select / Radio / Multi-Select / Checkbox
    //      / JSON / Story Block / Custom all route here) ----

    @Test
    public void textCleanTermUnchanged() {
        assertEquals("+(SSS.text:*value* SSS.text_dotraw:*value*)",
                new TextFieldStrategy().generateQuery(ctx("SSS.text", "value")));
    }

    @Test
    public void textHyphenTermIsEscaped() {
        assertEquals("+(SSS.text:*quarterly\\-report* SSS.text_dotraw:*quarterly\\-report*)",
                new TextFieldStrategy().generateQuery(ctx("SSS.text", "quarterly-report")));
    }

    @Test
    public void textColonTermIsEscaped() {
        assertEquals("+(SSS.text:*12\\:30* SSS.text_dotraw:*12\\:30*)",
                new TextFieldStrategy().generateQuery(ctx("SSS.text", "12:30")));
    }

    @Test
    public void textEscapesTheFullLuceneOperatorSetExceptTheSlash() {
        // Not just hyphens: parentheses, colon (and the rest of the Lucene set) are escaped too.
        // (`+`, `,`, `|` and whitespace are token delimiters, so they never reach escaping.)
        // The slash is the one exception — see textSlashIsNotEscapedSoItCanMatchAStoredSlash.
        assertEquals("+(SSS.text:*a/\\(b\\)\\:c* SSS.text_dotraw:*a/\\(b\\)\\:c*)",
                new TextFieldStrategy().generateQuery(ctx("SSS.text", "a/(b):c")));
    }

    @Test
    public void textSlashIsNotEscapedSoItCanMatchAStoredSlash() {
        // Inside a wildcard term an escaped slash is matched as a literal backslash, so `*\/a\/b*`
        // can never match a value containing "/a/b". Escaping it made every filter on a URL-like
        // value unusable: the term matched neither the analyzed field nor its `_dotraw` keyword, and
        // the OR of two failing clauses degenerated into matching individual path segments.
        assertEquals("+(SSS.text:*/store/index* SSS.text_dotraw:*/store/index*)",
                new TextFieldStrategy().generateQuery(ctx("SSS.text", "/store/index")));
    }

    @Test
    public void textSlashInAnInjectionTermIsStillNeutralized() {
        // Leaving the slash unescaped does not weaken the guard: a wildcard term always starts with
        // the `*` we prepend, so the slash cannot open a regex, and every operator stays escaped.
        assertEquals("+(SSS.text:*x/\\\"\\)OR* SSS.text_dotraw:*x/\\\"\\)OR*)",
                new TextFieldStrategy().generateQuery(ctx("SSS.text", "x/\")OR")));
    }

    @Test
    public void textNeutralizesLuceneInjection() {
        // A query-injection-flavored term is escaped to a literal — it can't break out of the
        // wildcard into query operators.
        assertEquals("+(SSS.text:*x\\\"\\)OR* SSS.text_dotraw:*x\\\"\\)OR*)",
                new TextFieldStrategy().generateQuery(ctx("SSS.text", "x\")OR")));
    }

    @Test
    public void textQuotedPhraseIsNotEscaped() {
        // An explicit quoted phrase keeps its exact-phrase behavior (delimiter is ", not *).
        assertEquals("+(SSS.text:\"a-b\" SSS.text_dotraw:\"a-b\")",
                new TextFieldStrategy().generateQuery(ctx("SSS.text", "\"a-b\"")));
    }

    // ---- BinaryFieldStrategy (analyzed file-name + _dotraw keyword, like Text) ----

    @Test
    public void binaryCleanTermUnchanged() {
        assertEquals("+(SSS.file:*value* SSS.file_dotraw:*value*)",
                new BinaryFieldStrategy().generateQuery(ctx("SSS.file", "value")));
    }

    @Test
    public void binaryHyphenTermIsEscapedAndHitsDotraw() {
        // The analyzed field tokenizes the file name on the hyphen, so a hyphenated term can only
        // match the escaped term against the `_dotraw` keyword that stores the whole file name.
        assertEquals("+(SSS.file:*quarterly\\-report* SSS.file_dotraw:*quarterly\\-report*)",
                new BinaryFieldStrategy().generateQuery(ctx("SSS.file", "quarterly-report")));
    }

    // ---- Path-derived URL fields: a Page's `url` and a File Asset's `fileName` ----

    private FieldContext pathCtx(final ContentType type, final String fieldName, final String value) {
        return new FieldContext.Builder().withContentType(type).withFieldName(fieldName)
                .withFieldValue(value).build();
    }

    private ContentType pageType() {
        return ImmutablePageContentType.builder().name("Test Page").build();
    }

    private ContentType fileAssetType() {
        return ImmutableFileAssetContentType.builder().name("Test File").build();
    }

    @Test
    public void pageUrlWithoutSlashIsUnchanged() {
        // No slash means the term can still match the field itself, so the query must stay exactly
        // as it is today — the `path` clause is only for terms the field can never match.
        assertEquals("+(htmlpageasset.url:*index* htmlpageasset.url_dotraw:*index*)",
                new TextFieldStrategy().generateQuery(
                        pathCtx(pageType(), "htmlpageasset.url", "index")));
    }

    @Test
    public void pageUrlWithLeadingSlashQueriesPathInstead() {
        // `htmlpageasset.url` indexes only the page's own last path segment ("index"), never the
        // folder path the grid displays. The field clauses are dropped rather than OR-ed: the analyzed
        // one is analyzed into the segments and BROADENS, which returned every page named "index" for
        // a term of "/store/index". The term keeps its slashes: `path` holds the whole path as a
        // single term, so a contains wildcard over it matches exactly what was typed.
        assertEquals("+path:*/store*",
                new TextFieldStrategy().generateQuery(
                        pathCtx(pageType(), "htmlpageasset.url", "/store")));
    }

    @Test
    public void pageUrlKeepsTheSlashInThePathTerm() {
        assertEquals("+path:*store/index*",
                new TextFieldStrategy().generateQuery(
                        pathCtx(pageType(), "htmlpageasset.url", "store/index")));
    }

    @Test
    public void pageUrlOperatorsStayEscapedInThePathClause() {
        // The slash goes in as-is; every other Lucene operator is still escaped, so the clause
        // can't be broken out of.
        assertEquals("+path:*about\\-us/index*",
                new TextFieldStrategy().generateQuery(
                        pathCtx(pageType(), "htmlpageasset.url", "about-us/index")));
    }

    @Test
    public void pageUrlOfBareSlashMatchesAnyPath() {
        // Every path contains a slash, so a lone-slash term matches all of them.
        assertEquals("+path:*/*",
                new TextFieldStrategy().generateQuery(
                        pathCtx(pageType(), "htmlpageasset.url", "/")));
    }

    @Test
    public void fileAssetFileNameWithSlashQueriesPathInstead() {
        // Same defect, same shape: `fileName` indexes "logo.png", not the folders above it.
        assertEquals("+path:*/images*",
                new TextFieldStrategy().generateQuery(
                        pathCtx(fileAssetType(), "fileAsset.fileName", "/images")));
    }

    @Test
    public void otherFieldOnAPageTypeIsUnchanged() {
        // The carve-out is the URL field specifically, not every field on a page type.
        assertEquals("+(htmlpageasset.friendlyName:*a/b* htmlpageasset.friendlyName_dotraw:*a/b*)",
                new TextFieldStrategy().generateQuery(
                        pathCtx(pageType(), "htmlpageasset.friendlyName", "a/b")));
    }

    @Test
    public void urlFieldOnAContentTypeIsUnchanged() {
        // A regular Content Type that happens to have a `url` field stores the real string, slashes
        // included, so it needs no help and must keep today's behavior.
        assertEquals("+(Blog.url:*a/b* Blog.url_dotraw:*a/b*)",
                new TextFieldStrategy().generateQuery(
                        pathCtx(ImmutableSimpleContentType.builder().name("Blog").build(),
                                "Blog.url", "a/b")));
    }

    // ---- KeyValueFieldStrategy (regular, colon-less path — what Content Drive sends) ----

    @Test
    public void keyValueCleanTermUnchanged() {
        assertEquals("+SSS.props.key_value:*color_red*",
                new KeyValueFieldStrategy().generateQuery(ctx("SSS.props", "color_red")));
    }

    @Test
    public void keyValueHyphenTermIsEscaped() {
        assertEquals("+SSS.props.key_value:*color_blue\\-green*",
                new KeyValueFieldStrategy().generateQuery(ctx("SSS.props", "color_blue-green")));
    }

    @Test
    public void keyValueSlashIsNotEscapedSoAMimeTypeCanMatch() {
        // Key/Value payloads routinely hold slashes: a File Asset's metadata stores
        // `contenttype_image/jpeg`. Escaped, the term matched nothing at all.
        assertEquals("+SSS.props.key_value:*contenttype_image/jpeg*",
                new KeyValueFieldStrategy().generateQuery(
                        ctx("SSS.props", "contenttype_image/jpeg")));
    }

    @Test
    public void binarySlashIsNotEscapedSoItStopsFalsePositives() {
        // A file name can never contain a slash, so this term should match nothing. Escaped, it did
        // not merely miss: the analyzed clause broadened and returned unrelated files.
        assertEquals("+(SSS.file:*/logo.png* SSS.file_dotraw:*/logo.png*)",
                new BinaryFieldStrategy().generateQuery(ctx("SSS.file", "/logo.png")));
    }
}
