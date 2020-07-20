package com.dotmarketing.portlets.containers.business;

import com.dotcms.contenttype.model.type.ContentType;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.dotmarketing.beans.ContainerStructure;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.containers.model.FileAssetContainer;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;

/**
 * Subscribe Strategies and get the strategy for a set of arguments if applies
 * @author jsanca
 */
public class ContainerStructureFinderStrategyResolver {

    private volatile ContainerStructureFinderStrategy       defaultOne = null;
    private volatile List<ContainerStructureFinderStrategy> strategies = this.getDefaultStrategies();

    private List<ContainerStructureFinderStrategy> getDefaultStrategies() {

        final ImmutableList.Builder<ContainerStructureFinderStrategy> builder =
                new ImmutableList.Builder<>();

        final IdentifierContainerStructureFinderStrategyImpl identifierContainerFinderStrategy = new IdentifierContainerStructureFinderStrategyImpl();

        builder.add(identifierContainerFinderStrategy);
        builder.add(new PathContainerStructureFinderStrategyImpl());

        this.defaultOne = identifierContainerFinderStrategy;

        return builder.build();
    }

    public ContainerStructureFinderStrategy getDefaultStrategy () {

        return defaultOne;
    }

    public synchronized void setDefaultStrategy (final ContainerStructureFinderStrategy strategy) {

        if (null != strategy) {

            this.defaultOne = strategy;
        }
    }


    private static class SingletonHolder {
        private static final ContainerStructureFinderStrategyResolver INSTANCE = new ContainerStructureFinderStrategyResolver();
    }
    /**
     * Get the instance.
     * @return ContainerStructureFinderStrategyResolver
     */
    public static ContainerStructureFinderStrategyResolver getInstance() {

        return ContainerStructureFinderStrategyResolver.SingletonHolder.INSTANCE;
    } // getInstance.

    /**
     * Adds a new strategy
     * @param strategy
     */
    public synchronized void subscribe (final ContainerStructureFinderStrategy strategy) {

        if (null != strategy) {

            final ImmutableList.Builder<ContainerStructureFinderStrategy> builder =
                    new ImmutableList.Builder<>();

            builder.addAll(this.strategies);
            builder.add(strategy);

            this.strategies = builder.build();
        }
    }

    /**
     * Get a strategy if applies
     * @param container
     * @return Optional ContainerFinderStrategy
     */
    public Optional<ContainerStructureFinderStrategy> get(final Container container) {

        for (int i = 0; i < this.strategies.size(); ++i) {

            final ContainerStructureFinderStrategy strategy = this.strategies.get(i);
            if (strategy.test(container)) {

                return Optional.of(strategy);
            }
        }

        return Optional.empty();
    }

    private boolean isFolderFileAsset (final Container container) {

        return null != container && container instanceof FileAssetContainer;
    }

    ///////////
    private class IdentifierContainerStructureFinderStrategyImpl implements ContainerStructureFinderStrategy {

        @Override
        public boolean test(final Container container) {
            return !ContainerStructureFinderStrategyResolver.this.isFolderFileAsset(container);
        }

        @Override
        public List<ContainerStructure> apply(final Container container) {

            if (null == container) {

                return Collections.emptyList();
            }
            //Gets the list from cache.
            List<ContainerStructure> containerStructures = CacheLocator.getContentTypeCache().getContainerStructures(container.getIdentifier(), container.getInode());

            //If there is not cache data for that container, go to the DB.
            if(containerStructures == null) {

                final ImmutableList.Builder<ContainerStructure> builder =
                        new ImmutableList.Builder<>();

                try {

                    final HibernateUtil dh = new HibernateUtil(ContainerStructure.class);
                    dh.setSQLQuery("select {container_structures.*} from container_structures " +
                            "where container_structures.container_id = ? " +
                            "and container_structures.container_inode = ?");
                    dh.setParam(container.getIdentifier());
                    dh.setParam(container.getInode());
                    builder.addAll(dh.list());

                    //Add the list to cache.
                    containerStructures = builder.build();
                    CacheLocator.getContentTypeCache().addContainerStructures(containerStructures, container.getIdentifier(), container.getInode());
                } catch (DotHibernateException e) {
                    throw new DotStateException("cannot find container structures for : " + container);
                }
            }

            return containerStructures;
        }
    } // IdentifierContainerStructureFinderStrategyImpl

    class PathContainerStructureFinderStrategyImpl implements ContainerStructureFinderStrategy {

        private final String FILE_EXTENSION = ".vtl";

        @Override
        public boolean test(final Container container) {
            return ContainerStructureFinderStrategyResolver.this.isFolderFileAsset(container);
        }

        @Override
        public List<ContainerStructure> apply(final Container container) {

            if (null == container) {

                return Collections.emptyList();
            }

            final ImmutableList.Builder<ContainerStructure> builder =
                    new ImmutableList.Builder<>();
            final List<FileAsset> assets =
                    FileAssetContainer.class.cast(container).getContainerStructuresAssets();

            for (final FileAsset asset: assets) {

                if (this.isValidFileAsset(asset)) {

                    final String velocityVarName = this.getVelocityVarName(asset);
                    if (UtilMethods.isSet(velocityVarName)) {

                        final Optional<ContentType> contentType =
                                this.findContentTypeByVelocityVarName(velocityVarName);
                        if (contentType.isPresent()) {

                            final ContainerStructure containerStructure =
                                    new ContainerStructure();

                            containerStructure.setContainerId(container.getIdentifier());
                            containerStructure.setContainerInode(asset.getInode());
                            containerStructure.setId(asset.getIdentifier());
                            containerStructure.setCode(wrapIntoDotParseDirective(asset));
                            containerStructure.setStructureId(contentType.get().id());
                            builder.add(containerStructure);
                        }
                    } else {

                        Logger.debug(this, "Could find a velocity var for the asset: " + asset);
                    }
                }else {

                    Logger.debug(this, "The asset: " + asset + ", does not exists or can not read");
                }
            }

            return builder.build();
        }

        private boolean isValidFileAsset(final FileAsset asset) {

            File file      = null;
            boolean exists = false;

            if (null != asset) {

                file   = asset.getFileAsset();
                exists = null != file && file.exists() && file.canRead();
            }

            return exists;
        }

        String getVelocityVarName(final FileAsset asset) {

            final String name = asset.getFileName();

            return StringUtils.remove(name, FILE_EXTENSION);
        }

        private String wrapIntoDotParseDirective (final FileAsset fileAsset) {

            return FileAssetContainerUtil.getInstance().wrapIntoDotParseDirective(fileAsset);
        }

        private Optional<ContentType> findContentTypeByVelocityVarName (final String velocityVarName) {

            ContentType contentType = null;

            try {

                contentType = APILocator.getContentTypeAPI
                        (APILocator.systemUser()).find(velocityVarName);
            } catch (DotSecurityException | DotDataException e) {

                Logger.debug(this, "cannot find the content type for the velocity var: " +  velocityVarName);
                return Optional.empty();
            }

            return Optional.of(contentType);
        }
    } // PathContainerStructureFinderStrategyImpl




} // E:O:F:ContainerStructureFinderStrategyResolver.
