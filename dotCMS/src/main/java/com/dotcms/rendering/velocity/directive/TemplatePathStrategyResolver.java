package com.dotcms.rendering.velocity.directive;

import com.dotcms.rendering.velocity.services.VelocityType;
import com.dotcms.uuid.shorty.ShortyId;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.portlets.containers.business.ContainerStructureFinderStrategy;
import com.dotmarketing.portlets.containers.business.FileAssetContainerUtil;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.ImmutableList;
import org.apache.velocity.context.Context;

import java.util.List;
import java.util.Optional;

import static com.dotcms.rendering.velocity.services.ContainerLoader.*;
import static com.dotmarketing.util.StringUtils.builder;
import static com.liferay.util.StringPool.FORWARD_SLASH;
import static com.liferay.util.StringPool.PERIOD;

/**
 * There are different ways in which Templates can reference Containers in dotCMS. For example:
 * <ul>
 *     <li>Using their respective ID.</li>
 *     <li>Using a path in case they're Containers as Files.</li>
 * </ul>
 * Because of that, the application must provide a mechanism that can easily retrieve and resolve the appropriate
 * source of information about Templates based on their specific nature.
 * <p>
 * Therefore, based on the way a Container in a Template is referenced in a page, this
 * {@code TemplatePathStrategyResolver} will be able to correctly determine the best way of reading, validating, and
 * retrieving Containers in it. For more information, please refer to: {@link TemplatePathStrategy}.
 * </p>
 *
 * @author jsanca
 * @since Mar 5th, 2018
 */
public class TemplatePathStrategyResolver {

    public  static final String DEFAULT_UUID_VALUE = MultiTree.LEGACY_RELATION_TYPE;

    private volatile TemplatePathStrategy       defaultOne = null;
    private volatile List<TemplatePathStrategy> strategies = this.getDefaultStrategies();

    /**
     * Utility method used to load the list of default strategies for resolving the correct way of referencing
     * Containers inside a Template.
     *
     * @return The list of {@link TemplatePathStrategy} objects.
     */
    private List<TemplatePathStrategy> getDefaultStrategies() {

        final ImmutableList.Builder<TemplatePathStrategy> builder =
                new ImmutableList.Builder<>();

        final IdentifierTemplatePathStrategyImpl identifierTemplatePathStrategy = new IdentifierTemplatePathStrategyImpl();

        builder.add(identifierTemplatePathStrategy);
        builder.add(new PathTemplatePathStrategyImpl());

        this.defaultOne = identifierTemplatePathStrategy;

        return builder.build();
    }

    /**
     * Returns the default Strategy that dotCMS uses to find the appropriate way in which a Template is going to
     * reference its Containers. Initially, the default Strategy is the one that uses ID of the Containers to resolve
     * its reference in a Template, but it can be changed if necessary.
     *
     * @return The default {@link TemplatePathStrategy} instance.
     */
    public TemplatePathStrategy getDefaultStrategy () {

        return defaultOne;
    }

    /**
     * Sets the default Strategy that will be used by dotCMS to retrieve the Containers added to a Template.
     *
     * @param strategy The {@link TemplatePathStrategy} that will be used by default.
     */
    public synchronized void setDefaultStrategy (final TemplatePathStrategy strategy) {

        if (null != strategy) {

            this.defaultOne = strategy;
        }
    }

    private static class SingletonHolder {
        private static final TemplatePathStrategyResolver INSTANCE = new TemplatePathStrategyResolver();
    }
    /**
     * Get the instance.
     * @return TemplatePathStrategyResolver
     */
    public static TemplatePathStrategyResolver getInstance() {

        return TemplatePathStrategyResolver.SingletonHolder.INSTANCE;
    } // getInstance.

    /**
     * Adds a new strategy
     * @param strategy
     */
    public synchronized void subscribe (final TemplatePathStrategy strategy) {

        if (null != strategy) {

            final ImmutableList.Builder<TemplatePathStrategy> builder =
                    new ImmutableList.Builder<>();

            builder.addAll(this.strategies);
            builder.add(strategy);

            this.strategies = builder.build();
        }
    }

    /**
     * Finds the appropriate Path Strategy for a given Template. Each Path Strategy must be responsible for correctly
     * determining the way a Container is being added to a Template.
     *
     * @param context   The {@link Context} of the Velocity Engine.
     * @param params    The {@link RenderParams} instance holding additional parameters that can be used for testing
     *                  this Path Strategy.
     * @param arguments Parameters set via the Velocity directive that indicate how and what pieces of data are being
     *                  rendered in the Template.
     *
     * @return The valid {@link TemplatePathStrategy} for the specified Template, or an empty {@link * Optional} if it
     * could not be found.
     */
    public Optional<TemplatePathStrategy> get(final Context context, final RenderParams params, final String[] arguments) {

        for (int i = 0; i < this.strategies.size(); ++i) {

            final TemplatePathStrategy strategy = this.strategies.get(i);
            if (strategy.test(context, params, arguments)) {

                return Optional.of(strategy);
            }
        }

        return Optional.empty();
    }

    /**
     * This is the path-based implementation of the {@link TemplatePathStrategy}.
     * <p>
     * It allows you to correctly reference Containers as Files inside a Template. These types of Containers are the
     * ones living in the {@code /application/containers/} folder.</p>
     */
    private class PathTemplatePathStrategyImpl implements TemplatePathStrategy {

        @Override
        public boolean test(final Context context, final RenderParams params, final String... arguments) {

            final  String templatePath = arguments[0];
            return this.isPath(templatePath);
        }

        @Override
        public String apply(final Context context, final RenderParams params, final String... arguments) {

            final String templatePath = arguments[0];
            final String uid          = (arguments.length > 1 && UtilMethods.isSet(arguments[1])) ? arguments[1] :  DEFAULT_UUID_VALUE;
            return this.getPath(params, templatePath, uid);
        }

        private boolean isPath (final String templatePath) {

            return FileAssetContainerUtil.getInstance().isFolderAssetContainerId(templatePath);
        }

        private String getPath(final RenderParams params, final String path, final String uid) {
            try {
                String fileContainerPathToVelocityPath = null;

                if (FileAssetContainerUtil.getInstance().isFullPath(path)) {
                    fileContainerPathToVelocityPath =
                            path.replaceAll(FORWARD_SLASH, FILE_CONTAINER_PATH_SEPARATOR_IN_VELOCITY_KEY)
                                    .replaceAll("\\" + PERIOD, HOST_NAME_SEPARATOR_IN_VELOCITY_KEY);
                } else {
                    final String fullPath = FileAssetContainerUtil.getInstance().getFullPath(path);
                    fileContainerPathToVelocityPath = RESOLVE_RELATIVE + FILE_CONTAINER_PATH_SEPARATOR_IN_VELOCITY_KEY +
                            fullPath.replaceAll(FORWARD_SLASH, FILE_CONTAINER_PATH_SEPARATOR_IN_VELOCITY_KEY)
                                    .replaceAll("\\" + PERIOD, HOST_NAME_SEPARATOR_IN_VELOCITY_KEY);
                }

                return builder(FORWARD_SLASH, params.mode.name(), FORWARD_SLASH, fileContainerPathToVelocityPath,
                        FORWARD_SLASH, uid, PERIOD, VelocityType.CONTAINER.fileExtension).toString();
            } catch (Exception e) {

                Logger.warn(this.getClass(), " - unable to resolve " + path + " getting this: "+ e.getMessage() );
                if(e.getStackTrace().length>0) {
                    Logger.warn(this.getClass(), " - at " + e.getStackTrace()[0]);
                }
                throw new DotStateException(e);
            }
        }
    } // PathTemplatePathStrategyImpl.

    /**
     * This is the Identifier-based implementation of the {@link TemplatePathStrategy}.
     * <p>
     * It allows you to correctly reference Containers form database inside a Template. This also include memory-only
     * objects, such as the System Container.</p>
     */
    private class IdentifierTemplatePathStrategyImpl implements TemplatePathStrategy {

        @Override
        public boolean test(final Context context, final RenderParams params, final String... arguments) {

            final String id = arguments[0];
            return this.isIdentifier(id);
        }

        @Override
        public String apply(final Context context, final RenderParams params, final String... arguments) {

            final String id   = arguments[0];
            final String uid  = (arguments.length > 1 && UtilMethods.isSet(arguments[1])) ? arguments[1] :  DEFAULT_UUID_VALUE;

            return builder(FORWARD_SLASH, params.mode.name(), FORWARD_SLASH, this.getIdentifier(id),
                    FORWARD_SLASH, uid, PERIOD, VelocityType.CONTAINER.fileExtension).toString();
        }

        /**
         * Returns the shorty version of the specified Container ID.
         *
         * @param identifier The Container's ID.
         *
         * @return The shorty ID, or the original ID if the short version was not/could not be generated.
         */
        private String getIdentifier(final String identifier) {
            if (Container.SYSTEM_CONTAINER.equals(identifier)) {
                return identifier;
            }
            final Optional<ShortyId> shortyIdOptional =
                    APILocator.getShortyAPI().getShorty(identifier);

            return shortyIdOptional.isPresent()?
                    shortyIdOptional.get().longId:identifier;
        }

        /**
         * Determines whether the specified ID belongs to a Container from database or not.
         *
         * @param identifier The Container's ID.
         *
         * @return If the Container comes from the database or if it is the System Container, returns {@code true}.
         * Otherwise, returns {@code false}.
         */
        private boolean isIdentifier (final String identifier) {

           return Container.SYSTEM_CONTAINER.equals(identifier) || FileAssetContainerUtil.getInstance().isDataBaseContainerId(identifier);
        }

    } // IdentifierTemplatePathStrategyImpl.

} // E:O:F:TemplatePathStrategyResolver.
