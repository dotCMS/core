package com.dotcms.tiptap;

import com.dotcms.UnitTestBase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Byte-parity lock on the default {@link TiptapMarkdown#toMarkdown(String)} output.
 *
 * <p>Three shipped consumers embed this exact output and must never see it change
 * (see #36658 §3.4): dotAI embeddings ({@code ContentToStringUtil}), the
 * {@code $markdownTool.blockToMarkdown} viewtool, and
 * {@code $contentlet.storyBlock.toMd} ({@code StoryBlockMap}). Every expected string in
 * this class was captured from the converter as it shipped <b>before</b> the ROUNDTRIP
 * flavor work and is intentionally byte-exact — including escaping, blank-line policy,
 * table padding and fence escalation.
 *
 * <p>If a change makes one of these tests fail, the change breaks the READABLE contract:
 * fix the change, do not re-capture the snapshot.
 *
 * @author hassandotcms
 */
public class TiptapMarkdownReadableParityTest extends UnitTestBase {

    private static void assertParity(final String tiptapJson, final String expectedMarkdown) {
        assertEquals(expectedMarkdown, TiptapMarkdown.toMarkdown(tiptapJson));
    }

    @Test
    public void headings_and_marks() {
        assertParity(
                "{\"type\":\"doc\",\"content\":["
                + "{\"type\":\"heading\",\"attrs\":{\"level\":1},\"content\":[{\"type\":\"text\",\"text\":\"Title\"}]},"
                + "{\"type\":\"heading\",\"attrs\":{\"level\":3},\"content\":[{\"type\":\"text\",\"text\":\"Sub\"}]},"
                + "{\"type\":\"paragraph\",\"content\":["
                + "{\"type\":\"text\",\"text\":\"plain \"},"
                + "{\"type\":\"text\",\"marks\":[{\"type\":\"bold\"}],\"text\":\"bold\"},"
                + "{\"type\":\"text\",\"text\":\" \"},"
                + "{\"type\":\"text\",\"marks\":[{\"type\":\"italic\"}],\"text\":\"italic\"},"
                + "{\"type\":\"text\",\"text\":\" \"},"
                + "{\"type\":\"text\",\"marks\":[{\"type\":\"strike\"}],\"text\":\"gone\"},"
                + "{\"type\":\"text\",\"text\":\" \"},"
                + "{\"type\":\"text\",\"marks\":[{\"type\":\"code\"}],\"text\":\"x=1\"},"
                + "{\"type\":\"text\",\"text\":\" \"},"
                + "{\"type\":\"text\",\"marks\":[{\"type\":\"link\",\"attrs\":{\"href\":\"https://dotcms.com\",\"title\":\"dot\"}}],\"text\":\"site\"}"
                + "]}]}",
                "# Title\n"
                + "\n"
                + "### Sub\n"
                + "\n"
                + "plain **bold** *italic* ~~gone~~ `x=1` [site](https://dotcms.com \"dot\")");
    }

    @Test
    public void escaping_of_markdown_significant_characters() {
        assertParity(
                "{\"type\":\"doc\",\"content\":[{\"type\":\"paragraph\",\"content\":["
                + "{\"type\":\"text\",\"text\":\"a*b _c_ [d] (e) #f +g -h !i |j <k> {l} \\\\m `n`\"}]}]}",
                "a\\*b \\_c\\_ \\[d\\] \\(e\\) \\#f \\+g \\-h \\!i \\|j \\<k\\> \\{l\\} \\\\m \\`n\\`");
    }

    @Test
    public void nested_lists_and_multi_block_items() {
        assertParity(
                "{\"type\":\"doc\",\"content\":["
                + "{\"type\":\"bulletList\",\"content\":["
                + "{\"type\":\"listItem\",\"content\":[{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"one\"}]}]},"
                + "{\"type\":\"listItem\",\"content\":["
                + "{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"two\"}]},"
                + "{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"two-b\"}]},"
                + "{\"type\":\"bulletList\",\"content\":[{\"type\":\"listItem\",\"content\":[{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"nested\"}]}]}]}"
                + "]}]},"
                + "{\"type\":\"orderedList\",\"attrs\":{\"start\":3},\"content\":["
                + "{\"type\":\"listItem\",\"content\":[{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"three\"}]}]},"
                + "{\"type\":\"listItem\",\"content\":[{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"four\"}]}]}"
                + "]}]}",
                "- one\n"
                + "- two\n"
                + "\n"
                + "  two\\-b\n"
                + "\n"
                + "  - nested\n"
                + "\n"
                + "3. three\n"
                + "4. four");
    }

    @Test
    public void blockquote_multi_paragraph() {
        assertParity(
                "{\"type\":\"doc\",\"content\":[{\"type\":\"blockquote\",\"content\":["
                + "{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"quoted one\"}]},"
                + "{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"quoted two\"}]}]}]}",
                "> quoted one\n"
                + "\n"
                + "> quoted two");
    }

    @Test
    public void code_block_fence_escalates_past_backtick_runs() {
        assertParity(
                "{\"type\":\"doc\",\"content\":[{\"type\":\"codeBlock\",\"attrs\":{\"language\":\"java\"},"
                + "\"content\":[{\"type\":\"text\",\"text\":\"String s = \\\"```\\\";\\nint i = 0;\"}]}]}",
                "````java\n"
                + "String s = \"```\";\n"
                + "int i = 0;\n"
                + "````");
    }

    @Test
    public void horizontal_rule_and_hard_break() {
        assertParity(
                "{\"type\":\"doc\",\"content\":["
                + "{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"before\"},{\"type\":\"hardBreak\"},{\"type\":\"text\",\"text\":\"after\"}]},"
                + "{\"type\":\"horizontalRule\"},"
                + "{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"end\"}]}]}",
                "before  \n"
                + "after\n"
                + "\n"
                + "---\n"
                + "\n"
                + "end");
    }

    /**
     * The decorated dotImage (asset binding, href/target, wrap/align) collapses to plain
     * {@code ![alt](src "title")} in READABLE — the bindings are dropped by design here;
     * the ROUNDTRIP flavor is the lossless surface.
     */
    @Test
    public void images_plain_and_decorated_emit_plain_markdown() {
        assertParity(
                "{\"type\":\"doc\",\"content\":["
                + "{\"type\":\"image\",\"attrs\":{\"src\":\"https://x/y.png\",\"alt\":\"generic\",\"title\":\"t\"}},"
                + "{\"type\":\"dotImage\",\"attrs\":{\"src\":\"/dA/abc/img.png\",\"alt\":\"plain\",\"title\":null,\"href\":null,\"data\":null,\"target\":null,\"textWrap\":null,\"textAlign\":null}},"
                + "{\"type\":\"dotImage\",\"attrs\":{\"src\":\"/dA/def/img2.png\",\"alt\":\"bound\",\"title\":\"cap\",\"href\":\"https://dotcms.com\",\"target\":\"_blank\",\"textWrap\":\"wrap-left\",\"textAlign\":\"center\","
                + "\"data\":{\"identifier\":\"11111111-2222-3333-4444-555555555555\",\"languageId\":1,\"title\":\"Asset Title\",\"contentType\":\"Image\",\"fileName\":\"img2.png\"}}}]}",
                "![generic](https://x/y.png \"t\")\n"
                + "\n"
                + "![plain](/dA/abc/img.png)\n"
                + "\n"
                + "![bound](/dA/def/img2.png \"cap\")");
    }

    @Test
    public void table_with_padding_and_multi_paragraph_cell() {
        assertParity(
                "{\"type\":\"doc\",\"content\":[{\"type\":\"table\",\"content\":["
                + "{\"type\":\"tableRow\",\"content\":["
                + "{\"type\":\"tableHeader\",\"attrs\":{\"colspan\":1,\"rowspan\":1,\"colwidth\":null},\"content\":[{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"H1\"}]}]},"
                + "{\"type\":\"tableHeader\",\"attrs\":{\"colspan\":1,\"rowspan\":1,\"colwidth\":null},\"content\":[{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"H2\"}]}]}]},"
                + "{\"type\":\"tableRow\",\"content\":["
                + "{\"type\":\"tableCell\",\"attrs\":{\"colspan\":1,\"rowspan\":1,\"colwidth\":null},\"content\":[{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"a\"}]},{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"a2\"}]}]},"
                + "{\"type\":\"tableCell\",\"attrs\":{\"colspan\":1,\"rowspan\":1,\"colwidth\":null},\"content\":[{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"marks\":[{\"type\":\"bold\"}],\"text\":\"b\"}]}]}]}]}]}",
                "| H1      | H2    |\n"
                + "| ------- | ----- |\n"
                + "| a<br>a2 | **b** |");
    }

    @Test
    public void youtube_emits_plain_link_block_and_inline() {
        assertParity(
                "{\"type\":\"doc\",\"content\":["
                + "{\"type\":\"youtube\",\"attrs\":{\"src\":\"https://www.youtube.com/watch?v=abc123\",\"start\":30,\"width\":640,\"height\":480}},"
                + "{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"watch \"},"
                + "{\"type\":\"youtube\",\"attrs\":{\"src\":\"https://youtu.be/xyz\"}},{\"type\":\"text\",\"text\":\" now\"}]}]}",
                "[https://www.youtube.com/watch?v=abc123](https://www.youtube.com/watch?v=abc123)\n"
                + "\n"
                + "watch [https://youtu.be/xyz](https://youtu.be/xyz) now");
    }

    /** A fat hydrated dotContent contributes exactly its escaped title, nothing else. */
    @Test
    public void dotcontent_fat_hydrated_emits_escaped_title_only() {
        assertParity(
                "{\"type\":\"doc\",\"content\":["
                + "{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"intro\"}]},"
                + "{\"type\":\"dotContent\",\"attrs\":{\"data\":{"
                + "\"identifier\":\"2d5d1c4c-3557-4bc2-a067-fca9a3f4a2a5\",\"languageId\":1,"
                + "\"title\":\"Q3 *Report* [final]\",\"contentType\":\"Blog\",\"inode\":\"aaa\",\"hostName\":\"demo\","
                + "\"modDate\":\"2026-01-01\",\"publishDate\":\"2026-01-02\",\"baseType\":\"CONTENT\",\"working\":true,"
                + "\"live\":true,\"owner\":\"admin\",\"archived\":false,\"url\":\"/blog/q3\",\"titleImage\":\"image\","
                + "\"hasLiveVersion\":true,\"folder\":\"SYSTEM_FOLDER\",\"sortOrder\":0,\"modUser\":\"admin\"}}}]}",
                "intro\n"
                + "\n"
                + "Q3 \\*Report\\* \\[final\\]");
    }

    @Test
    public void dotcontent_without_title_emits_nothing() {
        assertParity(
                "{\"type\":\"doc\",\"content\":["
                + "{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"before\"}]},"
                + "{\"type\":\"dotContent\",\"attrs\":{\"data\":{\"identifier\":\"99999999-8888-7777-6666-555555555555\",\"languageId\":2}}},"
                + "{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"after\"}]}]}",
                "before\n"
                + "\n"
                + "after");
    }

    @Test
    public void dotvideo_is_dropped_entirely() {
        assertParity(
                "{\"type\":\"doc\",\"content\":["
                + "{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"before video\"}]},"
                + "{\"type\":\"dotVideo\",\"attrs\":{\"src\":\"/dA/abc/v.mp4\",\"mimeType\":\"video/mp4\",\"width\":640,\"height\":360,\"orientation\":\"horizontal\","
                + "\"data\":{\"identifier\":\"aaaabbbb-cccc-dddd-eeee-ffff00001111\",\"languageId\":1,\"title\":\"clip\"}}},"
                + "{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"after video\"}]}]}",
                "before video\n"
                + "\n"
                + "after video");
    }

    @Test
    public void aicontent_is_dropped_entirely() {
        assertParity(
                "{\"type\":\"doc\",\"content\":["
                + "{\"type\":\"aiContent\",\"attrs\":{\"content\":\"generated words\"}},"
                + "{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"human words\"}]}]}",
                "human words");
    }

    @Test
    public void gridblock_children_are_flattened() {
        assertParity(
                "{\"type\":\"doc\",\"content\":[{\"type\":\"gridBlock\",\"attrs\":{\"columns\":[4,8]},\"content\":["
                + "{\"type\":\"gridColumn\",\"content\":[{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"left col\"}]}]},"
                + "{\"type\":\"gridColumn\",\"content\":[{\"type\":\"heading\",\"attrs\":{\"level\":2},\"content\":[{\"type\":\"text\",\"text\":\"right col\"}]}]}]}]}",
                "left col\n"
                + "\n"
                + "## right col");
    }

    @Test
    public void unknown_nodes_flatten_and_unknown_marks_drop() {
        assertParity(
                "{\"type\":\"doc\",\"content\":["
                + "{\"type\":\"myCustomBlock\",\"attrs\":{\"foo\":\"bar\"},\"content\":[{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"inside custom\"}]}]},"
                + "{\"type\":\"paragraph\",\"content\":["
                + "{\"type\":\"text\",\"marks\":[{\"type\":\"underline\"}],\"text\":\"underlined\"},"
                + "{\"type\":\"text\",\"text\":\" \"},"
                + "{\"type\":\"text\",\"marks\":[{\"type\":\"superscript\"}],\"text\":\"sup\"},"
                + "{\"type\":\"text\",\"text\":\" \"},"
                + "{\"type\":\"text\",\"marks\":[{\"type\":\"myCustomMark\"}],\"text\":\"custom-marked\"}]},"
                + "{\"type\":\"paragraph\",\"attrs\":{\"textAlign\":\"center\"},\"content\":[{\"type\":\"text\",\"text\":\"centered attr ignored\"}]}]}",
                "inside custom\n"
                + "\n"
                + "underlined sup custom\\-marked\n"
                + "\n"
                + "centered attr ignored");
    }
}
