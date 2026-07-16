package com.dotcms.rendering.velocity.viewtools.content.util;

import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;
import com.liferay.util.StringPool;

/**
 * Single source of truth for mapping a Story Block JSON node to the render
 * "type key" used to resolve its template.
 * <p>
 * {@code heading} is a composite of the type plus its level (e.g.
 * {@code heading1}); every other node uses its raw {@code type}. This logic is
 * shared by {@link com.dotcms.rendering.velocity.viewtools.content.StoryBlockMap}
 * (top-level dispatch) and {@link StoryBlockRenderHelper} (nested dispatch) so
 * the two render paths can never drift apart.
 *
 * @author dotCMS
 */
public final class NodeTypes {

    private NodeTypes() {
        // utility class
    }

    /**
     * Computes the render type key for a Story Block node.
     *
     * @param node the Story Block JSON node
     * @return the type key (e.g. {@code paragraph}, {@code dotContent}, {@code heading2})
     */
    public static String typeKey(final JSONObject node) throws JSONException {
        final String type = node.get("type").toString();
        return type + ("heading".equalsIgnoreCase(type)
                ? node.getJSONObject("attrs").get("level").toString()
                : StringPool.BLANK);
    }
}
