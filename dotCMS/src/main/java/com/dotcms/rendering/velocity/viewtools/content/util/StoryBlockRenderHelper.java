package com.dotcms.rendering.velocity.viewtools.content.util;

import com.dotcms.rendering.velocity.viewtools.content.Renderable;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;
import com.liferay.util.StringPool;
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

    private final String baseTemplatePath;
    private final Context context;
    private final RenderableFactory renderableFactory = new RenderableFactory();

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
            final Renderable renderable = this.renderableFactory.create(node, this.processType(node), this.context);
            return renderable.toHtml(this.baseTemplatePath);
        } catch (final JSONException e) {
            Logger.error(this, e.getMessage(), e);
            throw new DotRuntimeException(e);
        }
    }

    /**
     * Computes the render type for a node. Mirrors the logic in
     * {@code StoryBlockMap#processType}: {@code heading} is a composite of the
     * type plus its level (e.g. {@code heading1}).
     */
    private String processType(final JSONObject node) throws JSONException {
        final String type = node.get("type").toString();
        return type + ("heading".equalsIgnoreCase(type)
                ? node.getJSONObject("attrs").get("level").toString()
                : StringPool.BLANK);
    }
}
