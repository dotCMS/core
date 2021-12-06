package com.dotcms.rendering.velocity.viewtools.content.util;

import com.google.common.collect.ImmutableSet;

public class GenericRenderableFactory {

    final static String DEFAULT_TEMPLATE_STOCK_BLOCK_PATH = "static/storyblock/";

    public com.dotcms.rendering.velocity.viewtools.content.Readable create (final Object item, final String type) {

        final GenericRenderableItem defaultRenderableItem =
                new GenericRenderableItem(DEFAULT_TEMPLATE_STOCK_BLOCK_PATH, "default.vtl",
                        ImmutableSet.of("heading1","heading2","heading3","paragraph","dotContent","bulletList","orderedList"),
                        item, type);

        return defaultRenderableItem;
    }
}
