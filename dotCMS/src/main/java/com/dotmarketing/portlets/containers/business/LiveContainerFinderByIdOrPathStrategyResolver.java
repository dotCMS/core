package com.dotmarketing.portlets.containers.business;

import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Source;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.model.Container;
import com.google.common.collect.ImmutableList;
import com.liferay.portal.model.User;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Subscribe Strategies and get the strategy for a set of arguments if applies
 * @author jsanca
 */
public class LiveContainerFinderByIdOrPathStrategyResolver {

    private volatile ContainerFinderByIdOrPathStrategy       defaultOne = null;
    private volatile List<ContainerFinderByIdOrPathStrategy> strategies = this.getDefaultStrategies();

    private List<ContainerFinderByIdOrPathStrategy> getDefaultStrategies() {

        final ImmutableList.Builder<ContainerFinderByIdOrPathStrategy> builder =
                new ImmutableList.Builder<>();

        final IdentifierContainerFinderByIdOrPathStrategyImpl identifierContainerFinderStrategy = new IdentifierContainerFinderByIdOrPathStrategyImpl();

        builder.add(identifierContainerFinderStrategy);
        builder.add(new PathContainerFinderByIdOrPathStrategyImpl());

        this.defaultOne = identifierContainerFinderStrategy;

        return builder.build();
    }

    public ContainerFinderByIdOrPathStrategy getDefaultStrategy () {

        return defaultOne;
    }

    public synchronized void setDefaultStrategy (final ContainerFinderByIdOrPathStrategy strategy) {

        if (null != strategy) {

            this.defaultOne = strategy;
        }
    }


    private static class SingletonHolder {
        private static final LiveContainerFinderByIdOrPathStrategyResolver INSTANCE = new LiveContainerFinderByIdOrPathStrategyResolver();
    }
    /**
     * Get the instance.
     * @return WorkingContainerFinderByIdOrPathStrategyResolver
     */
    public static LiveContainerFinderByIdOrPathStrategyResolver getInstance() {

        return LiveContainerFinderByIdOrPathStrategyResolver.SingletonHolder.INSTANCE;
    } // getInstance.

    /**
     * Adds a new strategy
     * @param strategy
     */
    public synchronized void subscribe (final ContainerFinderByIdOrPathStrategy strategy) {

        if (null != strategy) {

            final ImmutableList.Builder<ContainerFinderByIdOrPathStrategy> builder =
                    new ImmutableList.Builder<>();

            builder.addAll(this.strategies);
            builder.add(strategy);

            this.strategies = builder.build();
        }
    }

    /**
     * Get a strategy if applies
     * @param containerIdOrPath {@link String} container id, or relative/absolute (with host) container file asset path
     * @return Optional ContainerFinderStrategy
     */
    public Optional<ContainerFinderByIdOrPathStrategy> get(final String containerIdOrPath) {

        for (int i = 0; i < this.strategies.size(); ++i) {

            final ContainerFinderByIdOrPathStrategy strategy = this.strategies.get(i);
            if (strategy.test(containerIdOrPath)) {

                return Optional.of(strategy);
            }
        }

        return Optional.empty();
    }


    ///////////
    private class IdentifierContainerFinderByIdOrPathStrategyImpl implements ContainerFinderByIdOrPathStrategy {

        @Override
        public boolean test(final String containerIdOrPath) {
            return FileAssetContainerUtil.getInstance().getContainerSourceFromContainerIdOrPath(containerIdOrPath) == Source.DB;
        }

        @Override
        public Container apply(final String containerIdOrPath, final User user, final boolean respectFrontEndPermissions, final Supplier<Host> resourceHost) throws NotFoundInDbException {

            try {
                return APILocator.getContainerAPI().getLiveContainerById(containerIdOrPath, user, false);
            } catch (DotDataException | DotSecurityException e) {
                throw new DotRuntimeException(e);
            }
        }
    } // IdentifierContainerFinderByIdOrPathStrategyImpl

    private class PathContainerFinderByIdOrPathStrategyImpl implements ContainerFinderByIdOrPathStrategy {


        @Override
        public boolean test(final String containerIdOrPath) {
            return FileAssetContainerUtil.getInstance().getContainerSourceFromContainerIdOrPath(containerIdOrPath) == Source.FILE;
        }

        @Override
        public Container apply(final String containerIdOrPath, final User user, final boolean respectFrontEndPermissions, final Supplier<Host> resourceHost) throws NotFoundInDbException {

            try {

                return APILocator.getContainerAPI().getLiveContainerByFolderPath
                        (containerIdOrPath, user, false,
                        resourceHost);
            } catch (DotDataException | DotSecurityException e) {
                throw new DotRuntimeException(e);
            }
        }
    } // PathContainerFinderByIdOrPathStrategyImpl.

} // E:O:F:WorkingContainerFinderByIdOrPathStrategyResolver.
