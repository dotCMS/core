package com.dotcms.rendering.velocity.viewtools.content.util;

import com.dotcms.rendering.velocity.viewtools.content.Renderable;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;
import com.dotmarketing.util.json.JSONArray;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.velocity.context.Context;

/**
 * Velocity-facing helper that lets the recursive Story Block render macro
 * ({@code #renderContentBlock} in {@code static/storyblock/render.vtl}) resolve
 * custom contentlet renders for blocks nested inside container blocks such as
 * {@code gridBlock}.
 * <p>
 * Without this helper, nested {@code dotContent} nodes are rendered straight
 * through the generic {@code dotContent.vtl} template (title only), bypassing the
 * {@code dotContent-{ContentType}.vtl} custom-render lookup that top-level blocks
 * get via {@link GenericRenderableImpl#toHtml(String)}. An instance is exposed in
 * the Velocity context under {@link #CONTEXT_KEY} whenever a Story Block is
 * rendered with a base template path.
 *
 * @author dotCMS
 */
public class StoryBlockRenderHelper {

    /** Key under which an instance is exposed in the Velocity context. */
    public static final String CONTEXT_KEY = "dotStoryBlockRenderHelper";

    private static final RenderableFactory renderableFactory = new RenderableFactory();

    private static final String TYPE = "type";
    private static final String MARKS = "marks";
    private static final String ATTRS = "attrs";
    private static final String LINK = "link";
    private static final String EMOJI = "emoji";
    private static final String NAME = "name";
    private static final String TEXT = "text";

    /** Link attributes that make two links the same link. */
    private static final String[] LINK_ATTRS = {"href", "target", "rel", "title", "aria-label"};

    private final String baseTemplatePath;
    private final Context context;

    /**
     * Shortcodes already warned about, scoped to this helper — i.e. to one Story Block field
     * render, including nested blocks, since {@code GenericRenderableImpl} creates at most one
     * helper per {@code toHtml} call and shares it across the recursion. Bounded without any
     * request-scoped state.
     */
    private final Set<String> warnedEmoji = new HashSet<>();

    public StoryBlockRenderHelper(final String baseTemplatePath, final Context context) {
        this.baseTemplatePath = baseTemplatePath;
        this.context = context;
    }

    /**
     * Renders a single Story Block node honoring custom render templates located
     * under the base template path. Falls back to the default template when no
     * custom one exists, mirroring the top-level rendering behavior in
     * {@link com.dotcms.rendering.velocity.viewtools.content.StoryBlockMap}.
     *
     * @param node the Story Block JSON node (e.g. a {@code dotContent} block)
     * @return the rendered HTML
     */
    public String render(final JSONObject node) {
        try {
            final Renderable renderable = renderableFactory.create(node, NodeTypes.typeKey(node), this.context);
            return renderable.toHtml(this.baseTemplatePath);
        } catch (final JSONException e) {
            Logger.error(this, e.getMessage(), e);
            throw new DotRuntimeException(e);
        }
    }

    /**
     * Resolves a Story Block {@code emoji} node to the text the macro should output (#37340).
     * <p>
     * Precedence: the generated shortcode map, then the node's own {@code attrs.text}, then the
     * literal {@code :name:}. It never returns empty — silently dropping the character is the
     * defect this fixes.
     * <p>
     * <strong>The caller must escape the result.</strong> {@code attrs.name} arrives from the
     * Contentlet REST API, which applies no Story Block schema validation, so it is
     * user-controlled; the macro wraps this in {@code $esc.html}.
     *
     * @param node the {@code emoji} node
     * @return the text to render, never {@code null} and never empty
     */
    public String emoji(final JSONObject node) {
        final String name = attrString(node, NAME);

        final String resolved = EmojiShortcodes.resolve(name);

        if (null != resolved) {
            return resolved;
        }

        final String carried = attrString(node, TEXT);

        if (null != carried && !carried.isEmpty()) {
            return carried;
        }

        if (this.warnedEmoji.add(String.valueOf(name))) {
            Logger.warn(this, "[dotCMS Block Editor]: Emoji " + name + " is not supported");
        }

        return ":" + name + ":";
    }

    /**
     * Groups a node list into link runs so the macro emits ONE {@code <a>} per logical link
     * (#37340).
     * <p>
     * {@code #renderMarks} opens and closes an anchor per text node, so stored JSON with several
     * text nodes sharing a link produced several anchors: duplicate tab stops, duplicate
     * screen-reader entries, a fragmented announcement — WCAG 2.2 Level A failures under 1.3.1,
     * 2.4.4 and 4.1.2.
     * <p>
     * The reported payload is harder than plain adjacency: the two linked text nodes are not
     * adjacent, because the mark-less {@code emoji} node the old editor created sits between
     * them. A run therefore continues across an intervening <strong>unmarked {@code emoji}
     * node</strong>, which is absorbed into the anchor. Only an {@code emoji} node — a
     * {@code hardBreak} or any other atom breaks the run, so nothing unrelated is ever pulled
     * inside a link.
     * <p>
     * Doing this in Java rather than in the macro keeps the interpreted template additive and
     * makes the attribute-equality logic unit-testable. The list is returned unchanged when
     * nothing coalesces, so the common path allocates one wrapper and no more.
     *
     * @param content the inline nodes of a block
     * @return one entry per run; each has a nullable {@code link} mark and its nodes
     */
    public List<LinkRun> linkRuns(final JSONArray content) {
        final List<LinkRun> runs = new ArrayList<>();

        if (null == content) {
            return runs;
        }

        for (int i = 0; i < content.size(); i++) {
            final Object raw = content.get(i);

            if (!(raw instanceof JSONObject)) {
                continue;
            }

            final JSONObject node = (JSONObject) raw;
            final JSONObject link = linkMark(node);
            final LinkRun previous = runs.isEmpty() ? null : runs.get(runs.size() - 1);

            if (null != link && null != previous && sameLink(previous.getLink(), link)) {
                previous.add(node);
                continue;
            }

            // An unmarked emoji only joins an OPEN run, so a standalone one is never
            // speculatively wrapped in an anchor.
            if (isAbsorbableEmoji(node) && null != previous && null != previous.getLink()) {
                previous.add(node);
                continue;
            }

            runs.add(new LinkRun(link, node));
        }

        return runs;
    }

    private static String attrString(final JSONObject node, final String key) {
        final JSONObject attrs = node.optJSONObject(ATTRS);

        return null == attrs ? null : attrs.optString(key, null);
    }

    private static JSONObject linkMark(final JSONObject node) {
        final JSONArray marks = node.optJSONArray(MARKS);

        if (null == marks) {
            return null;
        }

        for (int i = 0; i < marks.size(); i++) {
            final Object raw = marks.get(i);

            if (raw instanceof JSONObject
                    && LINK.equals(((JSONObject) raw).optString(TYPE, null))) {
                return (JSONObject) raw;
            }
        }

        return null;
    }

    /**
     * Two links are the same only when every attribute matches. Comparing {@code href} alone
     * would merge links that differ in {@code target}, {@code rel}, {@code title} or
     * {@code aria-label} — changing where a link opens, or what a screen reader announces.
     */
    private static boolean sameLink(final JSONObject a, final JSONObject b) {
        if (null == a || null == b) {
            return false;
        }

        final JSONObject attrsA = a.optJSONObject(ATTRS);
        final JSONObject attrsB = b.optJSONObject(ATTRS);

        for (final String key : LINK_ATTRS) {
            final String valueA = null == attrsA ? null : attrsA.optString(key, null);
            final String valueB = null == attrsB ? null : attrsB.optString(key, null);

            if (null == valueA ? null != valueB : !valueA.equals(valueB)) {
                return false;
            }
        }

        return true;
    }

    private static boolean isAbsorbableEmoji(final JSONObject node) {
        final JSONArray marks = node.optJSONArray(MARKS);

        return EMOJI.equals(node.optString(TYPE, null)) && (null == marks || marks.size() == 0);
    }

    /** One link run: an optional {@code link} mark and the nodes it wraps. */
    public static final class LinkRun {

        private final JSONObject link;
        private final List<JSONObject> nodes = new ArrayList<>();

        LinkRun(final JSONObject link, final JSONObject first) {
            this.link = link;
            this.nodes.add(first);
        }

        void add(final JSONObject node) {
            this.nodes.add(node);
        }

        /** @return the shared {@code link} mark, or {@code null} when the run is unlinked */
        public JSONObject getLink() {
            return this.link;
        }

        public List<JSONObject> getNodes() {
            return this.nodes;
        }

        /** Velocity-friendly alias so the macro can write {@code $run.linked}. */
        public boolean isLinked() {
            return null != this.link;
        }
    }
}
