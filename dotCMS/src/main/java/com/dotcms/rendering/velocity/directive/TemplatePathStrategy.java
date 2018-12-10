package com.dotcms.rendering.velocity.directive;

import org.apache.velocity.context.Context;

/**
 * Encapsulates the Template Path Strategy to generate template path
 * @author jsanca
 */
public interface TemplatePathStrategy {

    /**
     * Test if the Strategy could applies for the arguments
     * @param context
     * @param params
     * @param arguments
     * @return boolean
     */
    boolean test(final Context context, final RenderParams params, final String... arguments);

    /**
     * Applies the strategy
     * @param context
     * @param params
     * @param arguments
     * @return String
     */
    String apply(final Context context, final RenderParams params, final String... arguments);
}
