package com.dotcms.rendering.velocity.viewtools.content.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.rendering.velocity.viewtools.content.util.StoryBlockRenderHelper.LinkRun;
import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;
import java.util.List;
import org.junit.Test;

/**
 * Unit coverage for the Story Block link-run grouping and emoji resolution added for #37340.
 * <p>
 * This logic lives in Java precisely so it can be asserted in isolation: the Velocity macro it
 * feeds renders every Story Block field on every VTL-rendered page, for every customer, so a
 * defect here would reach far more content than the bug being fixed.
 */
public class StoryBlockRenderHelperTest {

    private static final String HREF = "https://dotcms.com";

    private final StoryBlockRenderHelper helper = new StoryBlockRenderHelper(null, null);

    private static JSONObject link(final String href) throws JSONException {
        return new JSONObject().put("type", "link").put("attrs", new JSONObject().put("href", href));
    }

    private static JSONObject text(final String value, final JSONObject... marks)
            throws JSONException {
        final JSONObject node = new JSONObject().put("type", "text").put("text", value);

        if (marks.length > 0) {
            final JSONArray array = new JSONArray();

            for (final JSONObject mark : marks) {
                array.add(mark);
            }

            node.put("marks", array);
        }

        return node;
    }

    private static JSONObject emoji(final String name, final JSONObject... marks)
            throws JSONException {
        final JSONObject node = new JSONObject().put("type", "emoji")
                .put("attrs", new JSONObject().put("name", name));

        if (marks.length > 0) {
            final JSONArray array = new JSONArray();

            for (final JSONObject mark : marks) {
                array.add(mark);
            }

            node.put("marks", array);
        }

        return node;
    }

    private static JSONArray nodes(final JSONObject... items) {
        final JSONArray array = new JSONArray();

        for (final JSONObject item : items) {
            array.add(item);
        }

        return array;
    }

    private static int anchors(final List<LinkRun> runs) {
        return (int) runs.stream().filter(LinkRun::isLinked).count();
    }

    /**
     * AC-015 — the reported payload. The two linked text nodes are NOT adjacent: the mark-less
     * emoji node sits between them, so plain adjacency merging would still emit two anchors.
     */
    @Test
    public void shape1_absorbsUnmarkedEmojiBetweenIdenticalLinks() throws JSONException {
        final List<LinkRun> runs = helper.linkRuns(nodes(
                text("dotCMS Copyright ", link(HREF)),
                emoji("copyright"),
                text("All rights reserved", link(HREF))));

        assertEquals("the whole run is one anchor", 1, runs.size());
        assertEquals(1, anchors(runs));
        assertEquals("the emoji is inside the anchor", 3, runs.get(0).getNodes().size());
    }

    /** Shape 2 — a link applied over an existing emoji marks the emoji too; already one anchor. */
    @Test
    public void shape2_emojiCarryingTheLinkStaysInOneRun() throws JSONException {
        final List<LinkRun> runs = helper.linkRuns(nodes(
                text("dotCMS ", link(HREF)),
                emoji("copyright", link(HREF)),
                text(" 2026", link(HREF))));

        assertEquals(1, runs.size());
        assertEquals(1, anchors(runs));
    }

    /** AC-014 — plain adjacent text nodes sharing a link. */
    @Test
    public void mergesAdjacentTextNodesSharingALink() throws JSONException {
        assertEquals(1, helper.linkRuns(nodes(
                text("one", link(HREF)),
                text("two", link(HREF)))).size());
    }

    /** AC-016 — absorption is narrow: only an emoji, never any other unmarked atom. */
    @Test
    public void nonEmojiAtomBreaksTheRun() throws JSONException {
        final List<LinkRun> runs = helper.linkRuns(nodes(
                text("one", link(HREF)),
                new JSONObject().put("type", "hardBreak"),
                text("two", link(HREF))));

        assertEquals("hardBreak must not be swallowed into the link", 2, anchors(runs));
    }

    /** AC-017 — a differing href keeps the anchors separate. */
    @Test
    public void differingHrefKeepsAnchorsSeparate() throws JSONException {
        assertEquals(2, anchors(helper.linkRuns(nodes(
                text("one", link(HREF)),
                text("two", link("https://example.com"))))));
    }

    /**
     * AC-017 — comparing href alone would merge links that differ in where they open or what a
     * screen reader announces.
     */
    @Test
    public void differingSecondaryAttributesKeepAnchorsSeparate() throws JSONException {
        for (final String attr : new String[] {"target", "rel", "title", "aria-label"}) {
            final JSONObject other = link(HREF);
            other.getJSONObject("attrs").put(attr, "x");

            assertEquals("differing " + attr + " must not merge", 2, anchors(helper.linkRuns(
                    nodes(text("one", link(HREF)), text("two", other)))));
        }
    }

    /** A standalone unmarked emoji is never speculatively wrapped in an anchor. */
    @Test
    public void standaloneEmojiIsNotWrappedInAnAnchor() throws JSONException {
        final List<LinkRun> runs = helper.linkRuns(nodes(emoji("copyright")));

        assertEquals(1, runs.size());
        assertFalse(runs.get(0).isLinked());
    }

    /** The no-op path: this helper sits on every Story Block render, so it must stay cheap. */
    @Test
    public void unlinkedContentIsReturnedUnchanged() throws JSONException {
        final JSONArray content = nodes(text("no links here"));
        final List<LinkRun> runs = helper.linkRuns(content);

        assertEquals(1, runs.size());
        assertFalse(runs.get(0).isLinked());
        assertEquals(1, runs.get(0).getNodes().size());
    }

    @Test
    public void nullContentYieldsNoRuns() {
        assertTrue(helper.linkRuns(null).isEmpty());
    }

    // ---------------------------------------------------------------- emoji resolution

    /** AC-009 — the character the VTL renderer used to drop entirely. */
    @Test
    public void resolvesTheReportedSymbols() throws JSONException {
        assertEquals("©", helper.emoji(emoji("copyright")));
        assertEquals("®", helper.emoji(emoji("registered")));
        assertEquals("™", helper.emoji(emoji("tm")));
    }

    /** The generated map must actually be on the classpath, or every emoji degrades. */
    @Test
    public void theGeneratedMapIsOnTheClasspath() {
        assertTrue("expected the generated emoji map to load", EmojiShortcodes.size() > 1000);
        assertNotNull(EmojiShortcodes.resolve("rocket"));
    }

    /** AC-013 — precedence step 2: the node's own text wins over the shortcode fallback. */
    @Test
    public void prefersCarriedTextOverTheShortcodeFallback() throws JSONException {
        final JSONObject node = emoji("unknown-name");
        node.getJSONObject("attrs").put("text", "★");

        assertEquals("★", helper.emoji(node));
    }

    /** AC-013 — never empty. Silently dropping the character is the defect being fixed. */
    @Test
    public void unresolvedNameFallsBackToTheShortcode() throws JSONException {
        assertEquals(":definitely-not-real:", helper.emoji(emoji("definitely-not-real")));
    }

    /**
     * The fallback echoes {@code attrs.name}, which arrives from the Contentlet REST API with no
     * Story Block schema validation. This asserts the helper returns it as inert data; the macro
     * is responsible for {@code $esc.html}.
     */
    @Test
    public void unresolvedNameIsReturnedAsInertText() throws JSONException {
        assertEquals(":<img src=x onerror=alert(1)>:",
                helper.emoji(emoji("<img src=x onerror=alert(1)>")));
    }
}
