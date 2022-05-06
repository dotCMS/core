package com.dotcms.rendering.velocity.viewtools.content.util;

import com.dotcms.rendering.velocity.viewtools.content.Renderable;
import com.google.common.collect.ImmutableSet;

/**
 * Renderable Factoruy
 * @author jsanca
 */
public class RenderableFactory {

    final static String DEFAULT_TEMPLATE_STOCK_BLOCK_PATH = "static/storyblock/";

    public Renderable create (final Object item, final String type) {

        final GenericRenderableImpl defaultRenderableItem =
                new GenericRenderableImpl(DEFAULT_TEMPLATE_STOCK_BLOCK_PATH, "default.vtl",
                        ImmutableSet.of("heading1","heading2","heading3","paragraph","dotContent","bulletList","orderedList","dotImage"),
                        item, type);

        return defaultRenderableItem;
    }
}
