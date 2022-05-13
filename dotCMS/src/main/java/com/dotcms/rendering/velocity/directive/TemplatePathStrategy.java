package com.dotcms.rendering.velocity.directive;

import org.apache.velocity.context.Context;

/**
 * This Path Strategy allows developers to correctly determine the best way of retrieving information about how a
 * given Template and its associated components can be added to a page in dotCMS. There are different ways in which
 * Templates can reference Containers in dotCMS. For example:
 * <ul>
 *     <li>Using their respective ID.</li>
 *     <li>Using a path in case they're Containers as Files.</li>
 * </ul>
 * <p>This means that the metadata for each of them comes from different data sources. In that case, specific strategies
 * must be implemented in order to correctly validate and retrieve such information so that dotCMS can seamlessly work
 * with Templates and its Containers, regardless the approach that Developers or Content Authors followed to create
 * them.</p>
 *
 * @author jsanca
 * @since Mar 12th, 2018
 */
public interface TemplatePathStrategy {

    /**
     * Verifies whether this Template Path Strategy can be applied to the specified Template.
     *
     * @param context   The {@link Context} of the Velocity Engine.
     * @param params    Optional {@link RenderParams} instance holding additional parameters that can be used for
     *                  testing this Path Strategy.
     * @param arguments Parameters set via the Velocity directive that indicate how and what pieces of data are being
     *                  rendered in the Template.
     *
     * @return boolean If the current Path Strategy can be applied to the Template, returns {@code true}. Otherwise, *
     * returns {@code false}.
     */
    boolean test(final Context context, final RenderParams params, final String... arguments);

    /**
     * Executes this Template Path Strategy on the specified Template. The result of this process is the correct way of
     * displaying Containers inside the Template, which are retrieved correctly based on their nature. Keep in mind
     * that the correct approach is to:
     * <ol>
     *     <li>Call the {@link #test(Context, RenderParams, String...)} to double-check that the Template data can
     *     be retrieved with this Path Strategy.</li>
     *     <li>If it can, then call this method to get the appropriate information.</li>
     * </ol>
     *
     * @param context   The {@link Context} of the Velocity Engine.
     * @param params    Optional {@link RenderParams} instance holding additional parameters that can be used for
     *                  testing this Path Strategy.
     * @param arguments Parameters set via the Velocity directive that indicate how and what pieces of data are being
     *                  rendered in the Template.
     *
     * @return String The correct path that will render every Container inside the specified Template.
     */
    String apply(final Context context, final RenderParams params, final String... arguments);

}
