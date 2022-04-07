package com.dotmarketing.portlets.containers.business;

import com.dotcms.contenttype.model.event.ContentTypeDeletedEvent;
import com.dotcms.contenttype.model.event.ContentTypeSavedEvent;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.system.event.local.model.Subscriber;
import com.dotmarketing.beans.ContainerStructure;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.cache.ContentTypeCache;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.containers.model.FileAssetContainer;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDUtil;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.liferay.util.StringPool;
import io.vavr.control.Try;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * There are different ways of creating Containers in dotCMS. For example:
 * <ul>
 *     <li>Default Containers -- added via the <b>Containers</b> portlet in the back-end.</li>
 *     <li>Containers as Files.</li>
 * </ul>
 * Because of that, the application must provide a mechanism that can easily retrieve and resolve the appropriate
 * Container-to-Content Type relationship based on their specific nature. Information from Default Containers is located
 * in the database, whereas Containers as Files provide all of its information from text files, usually in the form of
 * JSON or VTL files.
 * <p>
 * Therefore, based on the way a Container is referenced in a Template, this {@code ContainerStructureFinderStrategyResolver}
 * will be able to correctly determine the best way of reading, validating, and retrieving what types of Contentlets can
 * be added to a given Container. For more information, please refer to: {@link ContainerStructureFinderStrategy}.
 * </p>
 *
 * @author jsanca
 */
public class ContainerStructureFinderStrategyResolver {

    private volatile ContainerStructureFinderStrategy       defaultOne = null;
    private volatile List<ContainerStructureFinderStrategy> strategies = this.getDefaultStrategies();

    /**
     * Utility method used to load the list of default strategies for resolving the correct associations between
     * Containers and one or more Content Types.
     *
     * @return The list of {@link ContainerStructureFinderStrategy} objects.
     */
    private List<ContainerStructureFinderStrategy> getDefaultStrategies() {

        final ImmutableList.Builder<ContainerStructureFinderStrategy> builder =
                new ImmutableList.Builder<>();

        final IdentifierContainerStructureFinderStrategyImpl identifierContainerFinderStrategy = new IdentifierContainerStructureFinderStrategyImpl();

        builder.add(identifierContainerFinderStrategy);
        builder.add(new PathContainerStructureFinderStrategyImpl());

        this.defaultOne = identifierContainerFinderStrategy;

        return builder.build();
    }

    /**
     * Returns the default Strategy that dotCMS uses to find the appropriate relationships between a Container and one
     * or more Content Types. Initially, the default Strategy is the one that uses ID of the Container to resolve its
     * associated Content Types, but it can be changed if necessary.
     *
     * @return The default {@link ContainerStructureFinderStrategy} instance.
     */
    public ContainerStructureFinderStrategy getDefaultStrategy () {

        return defaultOne;
    }

    /**
     * Sets the default Strategy that will be used by dotCMS to retrieve the relationships between a Container and its
     * Content Types.
     *
     * @param strategy The {@link ContainerStructureFinderStrategy} that will be used by default.
     */
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

    @Subscriber()
    public void onSaveContentType (final ContentTypeSavedEvent contentTypeSavedEvent) {

        CacheLocator.getContentTypeCache().clearContainerStructures();
    }

    @Subscriber()
    public void onSaveContentType (final ContentTypeDeletedEvent contentTypeDeletedEvent) {

        CacheLocator.getContentTypeCache().clearContainerStructures();
    }

    /**
     * Finds the appropriate Container->Content Type Finder Strategy for a given Container. Each Finder Strategy must be
     * responsible for correctly determining whether it has the ability to find the information for a Container or not.
     *
     * @param container The {@link Container} whose Finder Strategy must be determined.
     *
     * @return The valid {@link ContainerStructureFinderStrategy} for the specified Container, or an empty {@link
     * Optional} if it could not be found.
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

    /**
     * Determines whether the specified Container is a Container as File or not.
     *
     * @param container The {@link Container} that is being checked.
     * @return If it is a Container as File, returns {@code true}. Otherwise, returns {@code false}.
     */
    private boolean isFolderFileAsset (final Container container) {

        return null != container && container instanceof FileAssetContainer;
    }

    /**
     * This is the Identifier-based implementation of the {@link ContainerStructureFinderStrategy}.
     * <p>
     * It allows you to find the metadata of a Container that is being referenced in a Template via its identifier.
     * These types of Containers are the ones living in the database. This means that all of their information must be
     * retrieved via SQL queries -- unless they're memory-only objects, such as the System Container.</p>
     */
    private class IdentifierContainerStructureFinderStrategyImpl implements ContainerStructureFinderStrategy {

        @Override
        public boolean test(final Container container) {
            // If the Container is NOT a file-based Container, then this strategy can be applied
            return !ContainerStructureFinderStrategyResolver.this.isFolderFileAsset(container);
        }

        @Override
        public List<ContainerStructure> apply(final Container container) {

            if (null == container || UtilMethods.isNotSet(container.getIdentifier())) {

                return Collections.emptyList();
            }
            //Gets the list from cache.
            List<ContainerStructure> containerStructures = CacheLocator.getContentTypeCache().getContainerStructures(container.getIdentifier(), container.getInode());

            //If there is not cache data for that container, go to the DB.
            if(containerStructures == null) {

                final ImmutableList.Builder<ContainerStructure> builder =
                        new ImmutableList.Builder<>();

                try {
                    if (Container.SYSTEM_CONTAINER.equals(container.getIdentifier())) {
                        addAllContentTypesToSystemContainer(builder);
                        containerStructures = builder.build();
                    } else {
                        final HibernateUtil dh = new HibernateUtil(ContainerStructure.class);
                        dh.setSQLQuery("select {container_structures.*} from container_structures " +
                                "where container_structures.container_id = ? " +
                                "and container_structures.container_inode = ?");
                        dh.setParam(container.getIdentifier());
                        dh.setParam(container.getInode());
                        builder.addAll(dh.list());

                        //Add the list to cache.
                        containerStructures = builder.build();
                        containerStructures = containerStructures.stream().map((cs) -> {
                            if (cs.getCode() == null) {
                                cs.setCode("");
                            }
                            return cs;
                        }).collect(Collectors.toList());
                    }
                    CacheLocator.getContentTypeCache().addContainerStructures(containerStructures, container.getIdentifier(), container.getInode());
                } catch (final DotHibernateException e) {
                    final String errorMsg = String.format(
                            "An error occurred when retrieving Content Types associated to Container '%s' [%s]: %s",
                            container.getName(), container.getIdentifier(), e.getMessage());
                    Logger.warn(this, errorMsg);
                    throw new DotStateException(errorMsg);
                }
            }

            return containerStructures;
        }

        /**
         * This method allows the System Container to be able to reference every single Content Type in the current
         * repository. Keep in mind that the System Container s meant to be able to hold Contentlets of any type, and
         * display them on the page based on its specified source code.
         *
         * @param builder The list of Container-to-Content Type references that the system will use to allow Users to
         *                add any Contentlet to the {@link com.dotmarketing.portlets.containers.model.SystemContainer}.
         */
        private void addAllContentTypesToSystemContainer(final ImmutableList.Builder<ContainerStructure> builder) {
            final List<ContentType> contentTypes =
                    Try.of(() -> APILocator.getContentTypeAPI(APILocator.systemUser())
                            .findAll()).getOrElse(Collections.emptyList());
            for (final ContentType contentType : contentTypes) {
                final ContainerStructure containerStructure =
                        new ContainerStructure(APILocator.getContainerAPI().systemContainer(), contentType);
                builder.add(containerStructure);
            }
        }

    } // IdentifierContainerStructureFinderStrategyImpl

    /**
     * This is the Container as File-based implementation of the {@link ContainerStructureFinderStrategy}.
     * <p>
     * It allows you to find the metadata of a Container that is being referenced in a Template via File Assets inside
     * the content repository. These types of Containers are the ones living in the {@code /application/containers/}
     * folder. This means that all of their metadata and configuration behavior must be set via VTL files.</p>
     */
    @VisibleForTesting
    class PathContainerStructureFinderStrategyImpl implements ContainerStructureFinderStrategy {

        private static final String USE_DEFAULT_LAYOUT = "useDefaultLayout";
        private final String FILE_EXTENSION = ".vtl";

        @Override
        public boolean test(final Container container) {
            // If the Container IS a file-based Container, then this strategy can be applied
            return ContainerStructureFinderStrategyResolver.this.isFolderFileAsset(container);
        }

        @Override
        public List<ContainerStructure> apply(final Container container) {

            if (null == container) {

                return Collections.emptyList();
            }

            final ContentTypeCache contentTypeCache = CacheLocator.getContentTypeCache();
            List<ContainerStructure> containerStructures = contentTypeCache
                    .getContainerStructures(container.getIdentifier(), container.getInode());
            if(containerStructures == null) {

                final Set<String> contentTypesIncludedSet = new HashSet<>();
                final ImmutableList.Builder<ContainerStructure> builder =
                        new ImmutableList.Builder<>();
                final List<FileAsset> assets =
                        FileAssetContainer.class.cast(container).getContainerStructuresAssets();

                for (final FileAsset asset : assets) {

                    if (this.isValidFileAsset(asset)) {

                        final String velocityVarName = this.getVelocityVarName(asset);
                        if (UtilMethods.isSet(velocityVarName)) {

                            final Optional<ContentType> contentType =
                                    this.findContentTypeByVelocityVarName(velocityVarName);
                            if (contentType.isPresent()) {

                                final ContainerStructure containerStructure =
                                        new ContainerStructure();

                                containerStructure.setContainerId(container.getIdentifier());
                                containerStructure.setContainerInode(container.getInode());
                                containerStructure.setId(asset.getIdentifier());
                                containerStructure.setCode(wrapIntoDotParseDirective(asset));
                                containerStructure.setStructureId(contentType.get().id());
                                builder.add(containerStructure);
                                contentTypesIncludedSet.add(velocityVarName);
                            }
                        } else {
                            Logger.debug(this,
                                    String.format("Could not find a Velocity Var for File Asset '%s'", asset));
                        }
                    } else {
                        Logger.debug(this, String.format("File asset '%s' does not exist or cannot be read.", asset));
                    }
                }

                this.processDefaultContainerLayout(FileAssetContainer.class.cast(container),
                        builder, contentTypesIncludedSet);

                containerStructures = builder.build();
                contentTypeCache.addContainerStructures(containerStructures, container.getIdentifier(), container.getInode());
            }

            return containerStructures;
        }

        /**
         * If the {@code useDefaultLayout} property in the Container's configuration is present, then all the Content
         * Types it references will use the {@code default_container.vtl} file to render their contents. The way the
         * {@code useDefaultLayout} property can reference Content Types is to pass down:
         * <ul>
         *     <li>A start ( {@code *} ) -- which will reference ALL Content Types in the repository.</li>
         *     <li>A comma-separated list of specific Velocity Variable Names for each allowed Content Type.</li>
         * </ul>
         *
         * @param fileAssetContainer      The Container as File.
         * @param builder                 The list of relationships between the Container and its respective Content
         *                                Types, in the form of {@link ContainerStructure} objects.
         * @param contentTypesIncludedSet The list of Content Types that have already been added to the list, so none of
         *                                them can be duplicated or overwritten.
         */
        private void processDefaultContainerLayout(final FileAssetContainer fileAssetContainer,
                                                   final ImmutableList.Builder<ContainerStructure> builder,
                                                   final Set<String> contentTypesIncludedSet) {

            final Map<String, Object> metaDataMap = fileAssetContainer.getMetaDataMap();
            if (UtilMethods.isSet(metaDataMap) && metaDataMap.containsKey(USE_DEFAULT_LAYOUT)
                                    && null != metaDataMap.get(USE_DEFAULT_LAYOUT)
                                    && this.isValidFileAsset(fileAssetContainer.getDefaultContainerLayoutAsset())) {

                final String useDefaultLayout = metaDataMap.get(USE_DEFAULT_LAYOUT).toString().trim();
                final String code = this.wrapIntoDotParseDirective(fileAssetContainer.getDefaultContainerLayoutAsset());

                if (StringPool.STAR.equals(useDefaultLayout)) {

                    final List<ContentType> contentTypes =
                            Try.of(()->APILocator.getContentTypeAPI(APILocator.systemUser())
                                    .findAll()).getOrElse(Collections.emptyList());
                    for (final ContentType contentType : contentTypes) {

                        if (!contentTypesIncludedSet.contains(contentType.variable().trim())) {

                            this.addContainerStructure(builder, fileAssetContainer, code, contentType);
                        }
                    }
                } else {

                    for (final String contentTypeVariable : useDefaultLayout.split(StringPool.COMMA)) {

                        if (!contentTypesIncludedSet.contains(contentTypeVariable.trim())) {

                            final Optional<ContentType> contentType = this.findContentTypeByVelocityVarName(contentTypeVariable.trim());
                            contentType.ifPresent(ct -> this.addContainerStructure(builder, fileAssetContainer, code, ct));
                        }
                    }
                }
            }
        }

        private void addContainerStructure (final ImmutableList.Builder<ContainerStructure> builder,
                                            final FileAssetContainer fileAssetContainer,
                                            final String code, final ContentType contentType) {

            final ContainerStructure containerStructure = new ContainerStructure();
            containerStructure.setContainerId(fileAssetContainer.getIdentifier());
            containerStructure.setContainerInode(fileAssetContainer.getInode());
            containerStructure.setId(UUIDUtil.uuid());
            containerStructure.setCode(code);
            containerStructure.setStructureId(contentType.id());
            builder.add(containerStructure);
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

            ContentType contentType;

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
