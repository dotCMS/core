package com.dotcms.tiptap;

import com.dotcms.UnitTestBase;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for the #36002 save-path discriminators added to {@link TiptapMarkdown}:
 * {@link TiptapMarkdown#isTiptapDoc(String)} (AC: "already valid Tiptap JSON is detected and
 * stored unchanged") and {@link TiptapMarkdown#isMarkdownRepresentable(String)} (the
 * whitelist that powers the rich-content overwrite guard).
 *
 * @author hassandotcms
 */
public class TiptapMarkdownDocDetectionTest extends UnitTestBase {

    // ---- isTiptapDoc ----------------------------------------------------

    @Test
    public void isTiptapDoc_true_for_doc_with_content() {
        assertTrue(TiptapMarkdown.isTiptapDoc(
                "{\"type\":\"doc\",\"content\":[{\"type\":\"paragraph\"}]}"));
    }

    @Test
    public void isTiptapDoc_true_for_empty_doc() {
        // The editor serializes an empty Story Block as a doc with an empty content array.
        assertTrue(TiptapMarkdown.isTiptapDoc("{\"type\":\"doc\",\"content\":[]}"));
    }

    @Test
    public void isTiptapDoc_true_when_leading_whitespace() {
        assertTrue(TiptapMarkdown.isTiptapDoc("  \n {\"type\":\"doc\",\"content\":[]}"));
    }

    @Test
    public void isTiptapDoc_false_for_doc_without_content_array() {
        // type=doc but no content array -> not a usable document, must not pass through as one.
        assertFalse(TiptapMarkdown.isTiptapDoc("{\"type\":\"doc\"}"));
        assertFalse(TiptapMarkdown.isTiptapDoc("{\"type\":\"doc\",\"content\":\"x\"}"));
    }

    @Test
    public void isTiptapDoc_false_for_arbitrary_or_non_doc_json() {
        assertFalse(TiptapMarkdown.isTiptapDoc("{\"foo\":\"bar\"}"));
        assertFalse(TiptapMarkdown.isTiptapDoc("{\"type\":\"paragraph\",\"content\":[]}"));
    }

    @Test
    public void isTiptapDoc_false_for_markdown_html_and_blanks() {
        assertFalse(TiptapMarkdown.isTiptapDoc("# Heading\n\nHello **world**."));
        assertFalse(TiptapMarkdown.isTiptapDoc("<h1>Heading</h1>"));
        assertFalse(TiptapMarkdown.isTiptapDoc(""));
        assertFalse(TiptapMarkdown.isTiptapDoc("   "));
        assertFalse(TiptapMarkdown.isTiptapDoc(null));
    }

    @Test
    public void isTiptapDoc_false_for_malformed_json() {
        assertFalse(TiptapMarkdown.isTiptapDoc("{\"type\":\"doc\",\"content\":["));
    }

    // ---- isMarkdownRepresentable ---------------------------------------

    @Test
    public void representable_true_for_primitive_blocks() {
        final String doc = "{\"type\":\"doc\",\"content\":["
                + "{\"type\":\"heading\",\"attrs\":{\"level\":1},\"content\":[{\"type\":\"text\",\"text\":\"H\"}]},"
                + "{\"type\":\"bulletList\",\"content\":[{\"type\":\"listItem\",\"content\":["
                + "{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"x\"}]}]}]},"
                + "{\"type\":\"dotImage\",\"attrs\":{\"src\":\"/a.png\"}}]}";
        assertTrue(TiptapMarkdown.isMarkdownRepresentable(doc));
    }

    @Test
    public void representable_true_when_only_marks_are_unsupported() {
        // Underline has no Markdown, but it's a mark, not a block — losing it is acceptable,
        // so it must NOT trip the overwrite guard.
        final String doc = "{\"type\":\"doc\",\"content\":[{\"type\":\"paragraph\",\"content\":["
                + "{\"type\":\"text\",\"text\":\"x\",\"marks\":[{\"type\":\"underline\"}]}]}]}";
        assertTrue(TiptapMarkdown.isMarkdownRepresentable(doc));
    }

    @Test
    public void representable_false_for_embedded_contentlet() {
        final String doc = "{\"type\":\"doc\",\"content\":["
                + "{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"x\"}]},"
                + "{\"type\":\"dotContent\",\"attrs\":{\"data\":{\"title\":\"t\"}}}]}";
        assertFalse(TiptapMarkdown.isMarkdownRepresentable(doc));
    }

    @Test
    public void representable_false_for_rich_blocks() {
        // youtube is deliberately NOT in this list: its READABLE emission is a plain link that
        // keeps the reference, so it is markdown-representable (whitelist bugfix, #36658).
        for (final String richType : new String[]{"dotVideo", "gridBlock", "aiContent"}) {
            final String doc = "{\"type\":\"doc\",\"content\":[{\"type\":\"" + richType + "\"}]}";
            assertFalse("'" + richType + "' must be flagged as not representable",
                    TiptapMarkdown.isMarkdownRepresentable(doc));
        }
    }

    @Test
    public void representable_false_when_rich_block_is_nested() {
        // A rich block buried inside a list item must still be detected.
        final String doc = "{\"type\":\"doc\",\"content\":[{\"type\":\"bulletList\",\"content\":["
                + "{\"type\":\"listItem\",\"content\":[{\"type\":\"dotVideo\"}]}]}]}";
        assertFalse(TiptapMarkdown.isMarkdownRepresentable(doc));
    }

    @Test
    public void representable_true_for_null_empty_and_non_json() {
        // Nothing structured to protect.
        assertTrue(TiptapMarkdown.isMarkdownRepresentable(null));
        assertTrue(TiptapMarkdown.isMarkdownRepresentable(""));
        assertTrue(TiptapMarkdown.isMarkdownRepresentable("   "));
        assertTrue(TiptapMarkdown.isMarkdownRepresentable("just some legacy text"));
    }
}
