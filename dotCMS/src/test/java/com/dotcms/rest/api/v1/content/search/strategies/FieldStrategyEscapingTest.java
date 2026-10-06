package com.dotcms.rest.api.v1.content.search.strategies;

import com.dotcms.rest.api.v1.content.search.handlers.FieldContext;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Fast, dependency-free unit tests for the escaping of Lucene query-syntax characters in the
 * per-field search strategies. A user term containing a hyphen, colon, slash, etc. must be escaped
 * so it can't break query parsing (which previously errored the whole search); the {@code *}
 * wildcards the strategy adds stay outside the escaped term. A clean term is unchanged.
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
    public void textEscapesTheFullLuceneOperatorSet() {
        // Not just hyphens: slash, parentheses, colon (and the rest of the Lucene set) are escaped
        // too. (`+`, `,`, `|` and whitespace are token delimiters, so they never reach escaping.)
        assertEquals("+(SSS.text:*a\\/\\(b\\)\\:c* SSS.text_dotraw:*a\\/\\(b\\)\\:c*)",
                new TextFieldStrategy().generateQuery(ctx("SSS.text", "a/(b):c")));
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
}
