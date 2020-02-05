package com.dotcms.rendering.velocity.services;

import com.google.common.collect.ImmutableList;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.business.ContainerAPI;
import com.dotmarketing.portlets.containers.business.FileAssetContainerUtil;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.liferay.util.StringPool;

import java.util.List;
import java.util.Optional;

/**
 * Subscribe Strategies and get the strategy for a set of arguments if applies
 * @author jsanca
 */
public class ContainerFinderStrategyResolver {

    private static final String HOST_INDICATOR     = "///";

    private volatile ContainerFinderStrategy       defaultOne = null;
    private volatile List<ContainerFinderStrategy> strategies = this.getDefaultStrategies();

    private List<ContainerFinderStrategy> getDefaultStrategies() {

        final ImmutableList.Builder<ContainerFinderStrategy> builder =
                new ImmutableList.Builder<>();

        final IdentifierContainerFinderStrategyImpl identifierContainerFinderStrategy
                = new IdentifierContainerFinderStrategyImpl();

        builder.add(identifierContainerFinderStrategy);
        builder.add(new PathContainerFinderStrategyImpl());

        this.defaultOne = identifierContainerFinderStrategy;

        return builder.build();
    }

    public ContainerFinderStrategy getDefaultStrategy () {

        return defaultOne;
    }

    public synchronized void setDefaultStrategy (final ContainerFinderStrategy strategy) {

        if (null != strategy) {

            this.defaultOne = strategy;
        }
    }


    private static class SingletonHolder {
        private static final ContainerFinderStrategyResolver INSTANCE = new ContainerFinderStrategyResolver();
    }
    /**
     * Get the instance.
     * @return ContainerFinderStrategyResolver
     */
    public static ContainerFinderStrategyResolver getInstance() {

        return ContainerFinderStrategyResolver.SingletonHolder.INSTANCE;
    } // getInstance.

    /**
     * Adds a new strategy
     * @param strategy
     */
    public synchronized void subscribe (final ContainerFinderStrategy strategy) {

        if (null != strategy) {

            final ImmutableList.Builder<ContainerFinderStrategy> builder =
                    new ImmutableList.Builder<>();

            builder.addAll(this.strategies);
            builder.add(strategy);

            this.strategies = builder.build();
        }
    }

    /**
     * Get a strategy if applies
     * @param key
     * @return Optional ContainerFinderStrategy
     */
    public Optional<ContainerFinderStrategy> get(final VelocityResourceKey key) {

        for (int i = 0; i < this.strategies.size(); ++i) {

            final ContainerFinderStrategy strategy = this.strategies.get(i);
            if (strategy.test(key)) {

                return Optional.of(strategy);
            }
        }

        return Optional.empty();
    }

    /////////////
    private class IdentifierContainerFinderStrategyImpl  implements ContainerFinderStrategy {

        private final ContainerAPI   containerAPI   = APILocator.getContainerAPI();

        @Override
        public boolean test(final VelocityResourceKey key) {

            return FileAssetContainerUtil.getInstance().isDataBaseContainerId(key.id1);
        }

        @Override
        public Container apply(final VelocityResourceKey key) {

            Container container = null;

            try {

                container = (key.mode.showLive)?
                    this.containerAPI.getLiveContainerById   (key.id1, APILocator.systemUser(), true):
                    this.containerAPI.getWorkingContainerById(key.id1, APILocator.systemUser(), true);
            } catch (DotDataException | DotSecurityException e) {
                throw new DotStateException("cannot find container for : " +  key, e);
            }

            return container;
        }
    }

    /////////////
    private class PathContainerFinderStrategyImpl implements ContainerFinderStrategy {

        private final HostAPI hostAPI            = APILocator.getHostAPI();
        private final FolderAPI folderAPI        = APILocator.getFolderAPI();
        private final ContainerAPI containerAPI  = APILocator.getContainerAPI();

        @Override
        public boolean test(final VelocityResourceKey key) {

            return FileAssetContainerUtil.getInstance().isFolderAssetContainerId(key.id1);
        }

        @Override
        public Container apply(final VelocityResourceKey key) {

            Container container = null;

            try {

                final Host site                = this.getHost(key.path);
                final String baseContainerPath = this.getContainerPath (key.path, site.getHostname());
                final Folder folder            = this.folderAPI.findFolderByPath(baseContainerPath, site, APILocator.systemUser(), false);
                container                      = containerAPI.getContainerByFolder(folder, site, APILocator.systemUser(), key.mode.showLive);
            } catch (DotDataException | DotSecurityException e) {
                throw new DotStateException("cannot find container for : " +  key, e);
            }

            return container;
        }

        private String getContainerPath(final String path, final String hostName) {

            String pathHostName = hostName;
            int hostIndexOf     = path.indexOf(hostName);
            final int lastSlash = path.lastIndexOf(StringPool.FORWARD_SLASH);

            if (hostIndexOf == -1) {

                // there are cases where the host does not exists but the host used is the default, so we need to keep the host name in the path.
                pathHostName = FileAssetContainerUtil.getInstance().getHostName(path);
                hostIndexOf  = path.indexOf(pathHostName);
            }

            return path.substring(hostIndexOf + pathHostName.length(), lastSlash);
        }

        // /EDIT_MODE///demo.dotcms.com/application/containers/test?languageid=1/LEGACY_RELATION_TYPE.container
        // /EDIT_MODE///demo.dotcms.com/application/containers/test/LEGACY_RELATION_TYPE.container

        private Host getHost (final String path) {

            try {

                return FileAssetContainerUtil.getInstance().getHost(path);
            } catch (Exception e) {

                return APILocator.systemHost();
            }
        }

    } // PathContainerFinderStrategyImpl.
}
