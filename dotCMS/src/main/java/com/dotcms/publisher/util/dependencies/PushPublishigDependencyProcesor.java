package com.dotcms.publisher.util.dependencies;

import com.dotcms.contenttype.business.StoryBlockAPI;
import com.dotcms.contenttype.transform.contenttype.StructureTransformer;
import com.dotcms.languagevariable.business.LanguageVariableAPI;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publisher.util.PusheableAsset;
import com.dotcms.publishing.DotBundleException;
import com.dotcms.publishing.PublisherConfig.Operation;
import com.dotcms.publishing.PublisherFilter;
import com.dotcms.publishing.manifest.ManifestItem;
import com.dotcms.publishing.manifest.ManifestReason;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.containers.model.FileAssetContainer;
import com.dotmarketing.portlets.containers.model.SystemContainer;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.rules.model.Rule;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.templates.model.FileAssetTemplate;
import com.dotmarketing.portlets.templates.model.SystemTemplate;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import io.vavr.Lazy;
import io.vavr.control.Try;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Implementation class for the {@link DependencyProcessor} interface.
 * <p>This processor takes a set of dotCMS objects that are being Push Published and determines the list of data
 * objects that they're related to. Depending on the Push Publishing Filter that was selected for the bundle's
 * generation, such dependent objects will be automatically added to the bundle as well.</p>
 * <p>It's worth noting that this is an automated process. This means that the final result cannot be easily determined
 * by a person if a high number of assets are being pushed, or if a high number of dependent objects are found.</p>
 *
 * @author Freddy Rodriguez
 * @since June 22nd, 2021
 */
public class PushPublishigDependencyProcesor implements DependencyProcessor {

    private final PublisherFilter publisherFilter;
    private final PushPublisherConfig config;
    private final User user;
    private final ConcurrentDependencyProcessor dependencyProcessor;
    private final PushPublishigDependencyProvider pushPublishigDependencyProvider;

    private final DependencyModDateUtil dependencyModDateUtil;
    private final PushedAssetUtil pushedAssetUtil;
    private final Lazy<StoryBlockAPI> storyBlockAPI = Lazy.of(APILocator::getStoryBlockAPI);
    private final Lazy<ContentletAPI> contentletAPI = Lazy.of(APILocator::getContentletAPI);

    /**
     * Creates an instance of the Push Publishing Dependency Processor mechanism, and initializes all the different data
     * structures that calculate dependencies based on the objects that are being added to the bundle.
     *
     * @param user            The {@link User} performing this action.
     * @param config          The bundle's {@link PushPublisherConfig} object, containing its configuration.
     * @param publisherFilter The {@link PublisherFilter} that was selected when the bundle wasa generated.
     */
    public PushPublishigDependencyProcesor(final User user, final PushPublisherConfig config,
            final PublisherFilter publisherFilter) {

        this.publisherFilter = publisherFilter;
        this.config = config;
        this.user = user;

        pushedAssetUtil = new PushedAssetUtil(config);
        dependencyModDateUtil = new DependencyModDateUtil(config);

        pushPublishigDependencyProvider = new PushPublishigDependencyProvider(user);

        dependencyProcessor = new ConcurrentDependencyProcessor();

        dependencyProcessor.addProcessor(PusheableAsset.SITE, (site) ->
                proccessSiteDependency((Host) site));

        dependencyProcessor.addProcessor(PusheableAsset.FOLDER, (folder) ->
                processFolderDependency((Folder) folder));

        dependencyProcessor.addProcessor(PusheableAsset.TEMPLATE,
                (template) -> processTemplateDependencies((Template) template));

        dependencyProcessor.addProcessor(PusheableAsset.CONTAINER,
                (container) -> processContainerDependency((Container) container));

        dependencyProcessor.addProcessor(PusheableAsset.CONTENT_TYPE,
                (contentType) -> processContentTypeDependency((Structure) contentType));

        dependencyProcessor.addProcessor(PusheableAsset.LINK,
                (link) -> processLinkDependency((Link) link));

        dependencyProcessor.addProcessor(PusheableAsset.CONTENTLET, (content) -> {
            try {
                processContentDependency((Contentlet) content);
            } catch (DotBundleException e) {
                Logger.error(DependencyManager.class, e.getMessage());
                throw new DotRuntimeException(e);
            }
        });
        dependencyProcessor.addProcessor(PusheableAsset.RULE, (rule) -> setRuleDependencies((Rule) rule));
        dependencyProcessor.addProcessor(PusheableAsset.LANGUAGE, (lang) -> {
            try {
                processLanguage((Language) lang);
            } catch (DotBundleException e) {
                Logger.error(DependencyManager.class, e.getMessage());
                throw new DotRuntimeException(e);
            }
        });
        dependencyProcessor.addProcessor(PusheableAsset.RELATIONSHIP,
                (relationship) -> processRelationshipDependencies((Relationship) relationship));

        if (publisherFilter.isDependencies() && !config.justIncludesUsers() && !config.justIncludesCategories()) {
            setLanguageVariables();
        }
    }

    /**
     * Collects the different dependent objects that are required for pushing
     * {@link Host} objects. The required dependencies of a site are:
     * <ul>
     * <li>Templates.</li>
     * <li>Containers.</li>
     * <li>Contentlets.</li>
     * <li>Content Types.</li>
     * <li>Folders.</li>
     * <li>Rules.</li>
     * </ul>
     */
    private void proccessSiteDependency(final Host site) {
        try {
            // Template dependencies
            tryToAddAllAndProcessDependencies(PusheableAsset.TEMPLATE,
                    pushPublishigDependencyProvider.getTemplatesByHost(site),
                    ManifestReason.INCLUDE_DEPENDENCY_FROM.getMessage(site));

            // Container dependencies
            tryToAddAllAndProcessDependencies(PusheableAsset.CONTAINER,
                    pushPublishigDependencyProvider.getContainersByHost(site),
                    ManifestReason.INCLUDE_DEPENDENCY_FROM.getMessage(site));

            pushPublishigDependencyProvider.getFileContainersByHost(site).stream()
                    .forEach(fileContainer -> dependencyProcessor.addAsset(fileContainer,
                            PusheableAsset.CONTAINER));

            // Content dependencies
            tryToAddAllAndProcessDependencies(PusheableAsset.CONTENTLET,
                    pushPublishigDependencyProvider.getContentletByLuceneQuery(
                            "+conHost:" + site.getIdentifier()),
                    ManifestReason.INCLUDE_DEPENDENCY_FROM.getMessage(site));

            // Structure dependencies
            tryToAddAllAndProcessDependencies(PusheableAsset.CONTENT_TYPE,
                    pushPublishigDependencyProvider.getContentTypeByHost(site),
                    ManifestReason.INCLUDE_DEPENDENCY_FROM.getMessage(site));

            // Folder dependencies
            tryToAddAllAndProcessDependencies(PusheableAsset.FOLDER,
                    pushPublishigDependencyProvider.getFoldersByHost(site),
                    ManifestReason.INCLUDE_DEPENDENCY_FROM.getMessage(site));

            // Rule dependencies
            tryToAddAllAndProcessDependencies(PusheableAsset.RULE,
                    pushPublishigDependencyProvider.getRulesByHost(site),
                    ManifestReason.INCLUDE_DEPENDENCY_FROM.getMessage(site));

        } catch (final DotSecurityException | DotDataException e) {
            Logger.error(this, String.format("An error occurred when processing dependencies on Site '%s' [%s]: %s",
                    site, site.getIdentifier(), e.getMessage()), e);
        }
    }

    /**
     * For given Folders adds its dependencies:
     * <ul>
     * <li>Hosts</li>
     * <li>Contentlets</li>
     * <li>Links</li>
     * <li>Structures</li>
     * <li>HTMLPages</li>
     * </ul>
     */
    private void processFolderDependency(final Folder folder) {
        try {

            final Folder parentFolder = pushPublishigDependencyProvider.getParentFolder(folder);

            if(UtilMethods.isSet(parentFolder)) {
                tryToAddSilently(PusheableAsset.FOLDER, parentFolder,
                        ManifestReason.INCLUDE_DEPENDENCY_FROM.getMessage(folder));
            }

            tryToAddSilently(PusheableAsset.SITE,
                    pushPublishigDependencyProvider.getHostById(folder.getHostId()),
                    ManifestReason.INCLUDE_DEPENDENCY_FROM.getMessage(folder));

            // Content dependencies
            tryToAddAllAndProcessDependencies(PusheableAsset.CONTENTLET,
                    pushPublishigDependencyProvider.getContentletByLuceneQuery("+conFolder:" + folder.getInode()),
                    ManifestReason.INCLUDE_DEPENDENCY_FROM.getMessage(folder));

            // Menu Link dependencies
            tryToAddAllAndProcessDependencies(PusheableAsset.LINK,
                    pushPublishigDependencyProvider.getLinksByFolder(folder),
                    ManifestReason.INCLUDE_DEPENDENCY_FROM.getMessage(folder));

            // Structure dependencies
            tryToAddAllAndProcessDependencies(PusheableAsset.CONTENT_TYPE,
                    pushPublishigDependencyProvider.getContentTypeByFolder(folder),
                    ManifestReason.INCLUDE_DEPENDENCY_FROM.getMessage(folder));

            //Add the default structure of this folder
            tryToAddAsDependency(PusheableAsset.CONTENT_TYPE,
                    CacheLocator.getContentTypeCache()
                            .getStructureByInode(folder.getDefaultFileType()), folder);

            // SubFolders
            tryToAddAllAndProcessDependencies(PusheableAsset.FOLDER,
                    APILocator.getFolderAPI().findSubFolders(folder, user, false),
                    ManifestReason.INCLUDE_DEPENDENCY_FROM.getMessage(folder));
        } catch (final DotSecurityException | DotDataException e) {
            Logger.error(this, String.format("An error occurred when processing dependencies on Folder '%s' [%s]: %s",
                    folder.getPath(), folder.getIdentifier(), e.getMessage()), e);
        }
    }

    /**
     * Analyzes the specified Template and retrieves the appropriate data dependencies that must be added to the Push
     * Publishing bundle.
     *
     * @param template The {@link Template} object that is being pushed.
     */
    private void processTemplateDependencies(final Template template) {

        try {

            final Template workingTemplate = template.isWorking() ? template
                    : APILocator.getTemplateAPI().findWorkingTemplate(template.getIdentifier(), user, false);
            final Template liveTemplate = template.isLive() ? template :
                    APILocator.getTemplateAPI().findLiveTemplate(template.getIdentifier(), user, false);

            // Host dependency
            tryToAddSilently(PusheableAsset.SITE,
                    pushPublishigDependencyProvider.getHostByTemplate(workingTemplate),
                    ManifestReason.INCLUDE_DEPENDENCY_FROM.getMessage(template));

            addContainerByTemplate(workingTemplate);

            if (UtilMethods.isSet(liveTemplate)) {
                addContainerByTemplate(liveTemplate);
            }

            if (UtilMethods.isSet(workingTemplate.getTheme())) {
                tryToAddAsDependency(PusheableAsset.FOLDER,
                        pushPublishigDependencyProvider.getThemeByTemplate(workingTemplate), template);
            }

            if(workingTemplate instanceof FileAssetTemplate){
                //Process FileAssetTemplate
                tryToAddAsDependency(PusheableAsset.FOLDER,
                        pushPublishigDependencyProvider.getFileAssetTemplateRootFolder(FileAssetTemplate.class.cast(workingTemplate)),
                        template);
            }
        } catch (final DotSecurityException | DotDataException e) {
            Logger.error(this, String.format("An error occurred when processing dependencies on Template '%s' [%s]: %s",
                    template.getName(), template.getIdentifier(), e.getMessage()), e);
        }
    }

    private void addContainerByTemplate(Template workingTemplate) throws DotDataException, DotSecurityException {
        final List<Container> containerByTemplate = pushPublishigDependencyProvider.getContainerByTemplate(workingTemplate);

        tryToAddAllAndProcessDependencies(PusheableAsset.CONTAINER,
                containerByTemplate.stream()
                        .filter(container -> !FileAssetContainer.class.isInstance(container))
                        .collect(Collectors.toList()),
                ManifestReason.INCLUDE_DEPENDENCY_FROM.getMessage(workingTemplate)
        );

        containerByTemplate.stream()
                .filter(FileAssetContainer.class::isInstance)
                .forEach(container -> dependencyProcessor.addAsset(container,
                        PusheableAsset.CONTAINER));
    }

    /**
     * Analyzes the specified Container and retrieves the appropriate data dependencies that must be added to the Push
     * Publishing bundle.
     *
     * @param container The {@link Container} object that is being pushed.
     */
    private void processContainerDependency(final Container container)  {

        try {
            final String containerId = container.getIdentifier();
            final Container containerById = APILocator.getContainerAPI()
                    .getWorkingContainerById(containerId, user, false);

            // Site Dependency
            tryToAddSilently(PusheableAsset.SITE,
                    pushPublishigDependencyProvider.getHostByContainer(containerById),
                    ManifestReason.INCLUDE_DEPENDENCY_FROM.getMessage(container));

            // Content Type Dependencies
            tryToAddAllAndProcessDependencies(PusheableAsset.CONTENT_TYPE,
                    pushPublishigDependencyProvider.getContentTypeByWorkingContainer(containerId),
                    ManifestReason.INCLUDE_DEPENDENCY_FROM.getMessage(container));

            tryToAddAllAndProcessDependencies(PusheableAsset.CONTENT_TYPE,
                    pushPublishigDependencyProvider.getContentTypeByLiveContainer(containerId),
                    ManifestReason.INCLUDE_DEPENDENCY_FROM.getMessage(container));

            if (containerById instanceof FileAssetContainer) {
                // Process FileAssetContainer
                tryToAddAsDependency(PusheableAsset.FOLDER,
                        pushPublishigDependencyProvider.getFileAssetContainerRootFolder(FileAssetContainer.class.cast(containerById)),
                        container);
            }
        } catch (final DotSecurityException | DotDataException e) {
            Logger.error(this,
                    String.format("An error occurred when processing dependencies on Container '%s' [%s]: %s",
                            container.getName(), container.getIdentifier(), e.getMessage()), e);
        }
    }

    private void processContentTypeDependency(final Structure contentType) {
        try {
            // Site Dependency
            tryToAddSilently(PusheableAsset.SITE,
                    pushPublishigDependencyProvider.getHostById(contentType.getHost()),
                    ManifestReason.INCLUDE_DEPENDENCY_FROM.getMessage(contentType));

            // Folder Dependencies
            tryToAddSilently(PusheableAsset.FOLDER,
                    pushPublishigDependencyProvider.getFolderById(contentType.getFolder()),
                    ManifestReason.INCLUDE_DEPENDENCY_FROM.getMessage(contentType));

            // Workflows Dependencies
            tryToAddAll(PusheableAsset.WORKFLOW,
                    pushPublishigDependencyProvider.getWorkflowSchemasByContentType(contentType),
                    ManifestReason.INCLUDE_DEPENDENCY_FROM.getMessage(contentType));

            // Categories Dependencies
            tryToAddAll(PusheableAsset.CATEGORY, APILocator.getCategoryAPI()
                    .findCategories(new StructureTransformer(contentType).from(), user),
                    ManifestReason.INCLUDE_DEPENDENCY_FROM.getMessage(contentType));

            // Related structures
            tryToAddAllAndProcessDependencies(PusheableAsset.RELATIONSHIP,
                    APILocator.getRelationshipAPI().byContentType(contentType),
                    ManifestReason.INCLUDE_DEPENDENCY_FROM.getMessage(contentType));

        } catch (final DotDataException | DotSecurityException e) {
            Logger.error(this,
                    String.format("An error occurred when processing dependencies on Content Type '%s' [%s]: %s",
                            contentType.getName(), contentType.getIdentifier(), e.getMessage()), e);
        }
    }

    /**
     * For given Links adds its dependencies:
     * <ul>
     * <li>Hosts</li>
     * <li>Folders</li>
     * </ul>
     */
    private void processLinkDependency(final Link link)  {
        final String linkId = link.getIdentifier();

        try {
            Identifier ident = APILocator.getIdentifierAPI().find(linkId);

            // Folder Dependencies
            tryToAddSilently(PusheableAsset.FOLDER,
                    pushPublishigDependencyProvider.getFolderByParentIdentifier(ident),
                    ManifestReason.INCLUDE_DEPENDENCY_FROM.getMessage(link));

            // Host Dependencies
            tryToAddSilently(PusheableAsset.SITE,
                    pushPublishigDependencyProvider.getHostById(ident.getHostId()),
                    ManifestReason.INCLUDE_DEPENDENCY_FROM.getMessage(link));

            // Content Dependencies
            tryToAddAllAndProcessDependencies(PusheableAsset.CONTENTLET,
                    pushPublishigDependencyProvider.getContentletsByLink(linkId),
                    ManifestReason.INCLUDE_DEPENDENCY_FROM.getMessage(link));
        } catch (final Exception e) {
            Logger.error(this, String.format("An error occurred when processing dependencies on Link '%s' [%s]: %s",
                    link.getName(), link.getIdentifier(), e.getMessage()), e);
        }
    }

    /**
     * Takes the specified {@link Contentlet} object and analyzes the different object dependencies it may have.
     */
    private void processContentDependency(final Contentlet contentlet)
            throws  DotBundleException {

        try {
            final String contentId = contentlet.getIdentifier();
            final Identifier ident = APILocator.getIdentifierAPI().find(contentId);
            final List<Contentlet> contentList =
                    APILocator.getContentletAPI().findAllVersions(ident, false, user, false);

            final Set<Contentlet> contentsToProcess = new HashSet<>();
            final Set<Contentlet> contentsWithDependenciesToProcess = new HashSet<>();

            for (final Contentlet contentletVersion : contentList) {

                if (contentletVersion.isHTMLPage()) {
                    processHTMLPagesDependency(contentletVersion.getIdentifier());
                }
                processStoryBockDependencies(contentletVersion);

                // Site Dependency
                tryToAddSilently(PusheableAsset.SITE,
                        pushPublishigDependencyProvider.getHostById(contentletVersion.getHost()),
                        ManifestReason.INCLUDE_DEPENDENCY_FROM.getMessage(contentletVersion));

                contentsToProcess.add(contentletVersion);

                // Relationships Dependencies
                final Map<Relationship, List<Contentlet>> contentRelationships = APILocator
                        .getContentletAPI().findContentRelationships(contentletVersion, user);
                
                tryToAddAllAndProcessDependencies(PusheableAsset.RELATIONSHIP,
                        contentRelationships.keySet(), ManifestReason.INCLUDE_DEPENDENCY_FROM.getMessage(contentlet));

                if(publisherFilter.isRelationships() && publisherFilter.isDependencies()) {
                    for (Entry<Relationship, List<Contentlet>> relationshipListEntry : contentRelationships
                            .entrySet()) {
                        contentsToProcess.addAll(relationshipListEntry.getValue());

                        tryToAddAll(PusheableAsset.CONTENTLET, relationshipListEntry.getValue(),
                                ManifestReason.INCLUDE_DEPENDENCY_FROM.getMessage(relationshipListEntry.getKey()));
                    }
                }

            }

            for (final Contentlet contentletToProcess : contentsToProcess) {
                // Host Dependency
                tryToAddSilently(PusheableAsset.SITE,
                        pushPublishigDependencyProvider.getHostById(contentletToProcess.getHost()),
                        ManifestReason.INCLUDE_DEPENDENCY_FROM.getMessage(contentlet));

                contentsWithDependenciesToProcess.add(contentletToProcess);
                //Copy asset files to bundle folder keeping original folders structure
                final List<Field> fields= FieldsCache.getFieldsByStructureInode(contentletToProcess.getContentTypeId());

                for(final Field field : fields) {
                    if (field.getFieldType().equals(Field.FieldType.IMAGE.toString())
                            || field.getFieldType().equals(Field.FieldType.FILE.toString())) {
                        String value = StringPool.BLANK;
                        try {
                            if(UtilMethods.isSet(APILocator.getContentletAPI().getFieldValue(contentletToProcess, field))){
                                value = APILocator.getContentletAPI().getFieldValue(contentletToProcess, field).toString();
                            }
                            final Identifier id = APILocator.getIdentifierAPI().find(value);
                            if (InodeUtils.isSet(id.getId()) && id.getAssetType().equals("contentlet")) {
                                final List<Contentlet> fileAssets = APILocator.getContentletAPI()
                                        .findAllVersions(id, false, user, false);

                                for (final Contentlet fileAsset : fileAssets) {
                                    final boolean added = tryToAddSilently(PusheableAsset.CONTENTLET, fileAsset,
                                            ManifestReason.INCLUDE_DEPENDENCY_FROM
                                                    .getMessage(contentletToProcess));

                                    if (added) {
                                        contentsWithDependenciesToProcess.addAll(fileAssets);
                                    }
                                }
                            }
                        } catch (final Exception ex) {
                            final String errorMsg = String.format(
                                    "An error occurred when processing value '%s' of field '%s' in Contentlet '%s': %s",
                                    value, field.getVelocityVarName(), contentlet.getIdentifier(), ex.getMessage());
                            Logger.debug(this, errorMsg);
                            throw new DotStateException(errorMsg, ex );
                        }
                    }

                }
            }

            // Adding the Contents (including related) and adding filesAsContent
            for (final Contentlet contentletWithDependenciesToProcess : contentsWithDependenciesToProcess) {
                // Site Dependency
                tryToAddSilently(PusheableAsset.SITE,
                        pushPublishigDependencyProvider.getHostById(contentletWithDependenciesToProcess.getHost()),
                        ManifestReason.INCLUDE_DEPENDENCY_FROM.getMessage(contentletWithDependenciesToProcess));


                // Folder Dependency
                tryToAddSilently(PusheableAsset.FOLDER,
                        pushPublishigDependencyProvider.getFolderById(contentletWithDependenciesToProcess.getFolder()),
                        ManifestReason.INCLUDE_DEPENDENCY_FROM.getMessage(contentletWithDependenciesToProcess));

                // Language Dependency
                tryToAddAsDependency(PusheableAsset.LANGUAGE,
                        APILocator.getLanguageAPI().getLanguage(contentletWithDependenciesToProcess.getLanguageId()),
                        contentletWithDependenciesToProcess
                );

                try {
                    if (Config.getBooleanProperty("PUSH_PUBLISHING_PUSH_ALL_FOLDER_PAGES", false)
                            && contentletWithDependenciesToProcess.getStructure().getStructureType() == Structure.STRUCTURE_TYPE_HTMLPAGE) {

                        final Folder contFolder=APILocator.getFolderAPI()
                                .find(contentletWithDependenciesToProcess.getFolder(), user, false);

                        tryToAddAllAndProcessDependencies(PusheableAsset.CONTENTLET,
                                pushPublishigDependencyProvider.getHTMLPages(contFolder),
                                ManifestReason.INCLUDE_DEPENDENCY_FROM.getMessage(contentletWithDependenciesToProcess));
                    }
                } catch (Exception e) {
                    Logger.debug(this, e.toString());
                }

                if(Config.getBooleanProperty("PUSH_PUBLISHING_PUSH_STRUCTURES", true)) {
                    tryToAddAsDependency(PusheableAsset.CONTENT_TYPE,
                            CacheLocator.getContentTypeCache()
                                    .getStructureByInode(contentletWithDependenciesToProcess.getStructureInode()),
                            contentletWithDependenciesToProcess
                    );

                }

                // Evaluate all the categories from this  to include as dependency.
                tryToAddAll(PusheableAsset.CATEGORY, APILocator.getCategoryAPI()
                                .getParents(contentletWithDependenciesToProcess, APILocator.systemUser(), false),
                        ManifestReason.INCLUDE_DEPENDENCY_FROM.getMessage(contentletWithDependenciesToProcess)
                );

            }
        } catch (final Exception e) {
            final String errorMsg =
                    String.format("An error occurred when processing dependencies on Contentlet '%s': %s",
                            contentlet.getIdentifier(), e.getMessage());
            Logger.error(this, errorMsg, e);
            throw new DotBundleException(errorMsg, e);
        }
    }

    /**
     * Analyzes the specified {@link Contentlet} and determines whether it has at least one Story Block field. If it
     * does, then its contents are retrieved in order to determine if any other Contentlets are being referenced in it
     * or not. And if they are, the Dependency Processor needs to determine whether they need to be added to the Bundle
     * or not.
     *
     * @param contentlet The {@link Contentlet} whose Story Block fields will be analyzed.
     */
    private void processStoryBockDependencies(final Contentlet contentlet) {
        if (contentlet.getContentType().hasStoryBlockFields()) {
            this.storyBlockAPI.get().getDependencies(contentlet).forEach(contentletId -> {
                Contentlet contentInStoryBlock = new Contentlet();
                try {
                    contentInStoryBlock = this.contentletAPI.get().findContentletByIdentifier(contentletId,
                            contentlet.isLive(), contentlet.getLanguageId(), APILocator.systemUser(), false);
                    tryToAddAndProcessDependencies(PusheableAsset.CONTENTLET, contentInStoryBlock,
                            ManifestReason.INCLUDE_DEPENDENCY_FROM.getMessage(contentlet));
                } catch (final DotDataException | DotSecurityException e) {
                    Logger.warn(this, String.format("Could not analyze dependent Contentlet '%s' referenced in Story "
                                                            + "Block field from Contentlet Inode " + "'%s': %s",
                            contentInStoryBlock, contentlet.getInode(), e.getMessage()));
                }
            });
        }
    }

    /**
     * Collects the different dependent objects that are required for pushing
     * {@link Rule} objects. The required dependency of a rule is either:
     * <ol>
     * <li>The Site (host) they were created in.</li>
     * <li>Or the Content Page they were created in.</li>
     * </ol>
     */
    private void setRuleDependencies(final Rule rule)  {
        final HostAPI hostAPI = APILocator.getHostAPI();
        final ContentletAPI contentletAPI = APILocator.getContentletAPI();

        try {
            final List<Contentlet> contentlets = contentletAPI.searchByIdentifier(
                    "+identifier:" + rule.getParent(), 1, 0, null, this.user, false,
                    PermissionAPI.PERMISSION_READ, true);

            if (contentlets != null && contentlets.size() > 0) {
                final Contentlet parent = contentlets.get(0);

                if (parent.isHost()) {
                    tryToAddSilently(PusheableAsset.SITE,
                            hostAPI.find(rule.getParent(), this.user, false),
                            ManifestReason.INCLUDE_DEPENDENCY_FROM.getMessage(rule));
                } else if (parent.isHTMLPage()) {
                    tryToAddSilently(PusheableAsset.CONTENTLET, parent,
                            ManifestReason.INCLUDE_DEPENDENCY_FROM.getMessage(rule));
                } else {
                    throw new DotDataException(String.format(
                            "For Rule '%s', the parent ID '%s' is invalid as it must be either an HTML Page or a Site.",
                            rule.getName(), parent.getIdentifier()));
                }
            } else {
                throw new DotDataException(
                        String.format("For Rule '%s', no parent with ID '%s' could be found", rule.getName(),
                                rule.getParent()));
            }
        } catch (final DotDataException | DotSecurityException e) {
            Logger.error(this, String.format("An error occurred when processing dependencies on Rule '%s' [%s]: %s",
                    rule.getName(), rule.getId(), e.getMessage()), e);
        }
    }

    private void processLanguage(final Language language) throws DotBundleException {
        try{
            final long lang = language.getId();
            final String keyValueQuery = "+contentType:" + LanguageVariableAPI.LANGUAGEVARIABLE + " +languageId:" + lang;
            final List<Contentlet> listKeyValueLang = APILocator.getContentletAPI()
                    .search(keyValueQuery,0, -1, StringPool.BLANK, user, false);

            if (UtilMethods.isSet(listKeyValueLang)) {
                tryToAddAll(PusheableAsset.CONTENTLET, listKeyValueLang,
                        ManifestReason.INCLUDE_DEPENDENCY_FROM.getMessage(language));

                final String contentTypeId = listKeyValueLang.get(0).getContentTypeId();
                tryToAddSilently(PusheableAsset.CONTENT_TYPE,
                        CacheLocator.getContentTypeCache().getStructureByInode(contentTypeId),
                        ManifestReason.INCLUDE_DEPENDENCY_FROM.getMessage(language));
            }
        } catch (final Exception e) {
            final String errorMsg =
                    String.format("An error occurred when processing dependencies on Language '%s': %s", language,
                            e.getMessage());
            Logger.error(this, errorMsg, e);
            throw new DotBundleException(errorMsg, e);
        }
    }

    public void processRelationshipDependencies(final Relationship relationship) {

        try {
            tryToAddAsDependency(PusheableAsset.CONTENT_TYPE,
                    CacheLocator.getContentTypeCache()
                            .getStructureByInode(relationship.getChildStructureInode()), relationship);

            tryToAddAsDependency(PusheableAsset.CONTENT_TYPE, CacheLocator.getContentTypeCache()
                            .getStructureByInode(relationship.getParentStructureInode()), relationship);
        } catch (final DotDataException | DotSecurityException e) {
            Logger.error(this,
                    String.format("An error occurred when processing dependencies on Relationship '%s' [%s]: %s",
                            relationship.getTitle(), relationship.getIdentifier(), e.getMessage()), e);
        }
    }

    /**
     * Collects the different dependent objects that are required for pushing
     * {@link IHTMLPage} objects. The required dependencies of a page are:
     * <ul>
     * <li>Host.</li>
     * <li>Template.</li>
     * <li>Containers.</li>
     * <li>Content Types.</li>
     * <li>Contentlets.</li>
     * <li>Rules.</li>
     * </ul>
     *
     */
    private void processHTMLPagesDependency(final String pageId) {
        try {

            final IdentifierAPI idenAPI = APILocator.getIdentifierAPI();
            final Identifier identifier = idenAPI.find(pageId);

            if(identifier==null || UtilMethods.isEmpty(identifier.getId())) {
                Logger.warn(this.getClass(), "Unable to find page for identifier, moving on.  Id: " + identifier );
                return;
            }

            // looking for working version (must exists)
            final IHTMLPage workingPage = Try.of(
                    ()->APILocator.getHTMLPageAssetAPI().findByIdLanguageFallback(
                            identifier,
                            APILocator.getLanguageAPI().getDefaultLanguage().getId(),
                            false,
                            user,
                            false)
            ).onFailure(e->Logger.warnAndDebug(DependencyManager.class, e)).getOrNull();

            if(workingPage == null) {
                return;
            }

            // Site dependency
            tryToAddSilently(PusheableAsset.SITE,
                    pushPublishigDependencyProvider.getHostById(identifier.getHostId()),
                    ManifestReason.INCLUDE_DEPENDENCY_FROM.getMessage(workingPage));

            // Folder dependencies
            tryToAddSilently(PusheableAsset.FOLDER,
                    pushPublishigDependencyProvider.getFolderByParentIdentifier(identifier),
                    ManifestReason.INCLUDE_DEPENDENCY_FROM.getMessage(workingPage));

            final IHTMLPage livePage = workingPage.isLive()
                    ? workingPage
                    : Try.of(()->
                            APILocator.getHTMLPageAssetAPI().findByIdLanguageFallback(identifier, APILocator.getLanguageAPI().getDefaultLanguage().getId(), true, user, false))
                            .onFailure(e->Logger.warnAndDebug(DependencyManager.class, e)).getOrNull();

            // working template working page
            final Template workingTemplateWP = workingPage != null ?
                    APILocator.getTemplateAPI()
                            .findWorkingTemplate(workingPage.getTemplateId(), user, false) : null;

            if (workingTemplateWP != null) {

                // Templates dependencies
                if(!(workingTemplateWP instanceof FileAssetTemplate)) {
                    tryToAddAsDependency(PusheableAsset.TEMPLATE, workingTemplateWP, workingPage);
                } else {
                    dependencyProcessor.addAsset(workingTemplateWP, PusheableAsset.TEMPLATE);
                }
            }

            final Template liveTemplateLP = livePage != null ?
                    APILocator.getTemplateAPI()
                            .findLiveTemplate(livePage.getTemplateId(), user, false) : null;

            // Templates dependencies
            if (liveTemplateLP != null ) {
                if(!(liveTemplateLP instanceof FileAssetTemplate)) {
                    tryToAddAsDependency(PusheableAsset.TEMPLATE, liveTemplateLP, workingPage);
                } else {
                    dependencyProcessor.addAsset(liveTemplateLP, PusheableAsset.TEMPLATE);
                }
            }

            // Contents dependencies
            tryToAddAllAndProcessDependencies(PusheableAsset.CONTENTLET,
                    pushPublishigDependencyProvider.getContentletsByPage(workingPage),
                    ManifestReason.INCLUDE_DEPENDENCY_FROM.getMessage(workingPage));


            // Rule dependencies
            tryToAddAllAndProcessDependencies(PusheableAsset.RULE,
                    pushPublishigDependencyProvider.getRuleByPage(workingPage),
                    ManifestReason.INCLUDE_DEPENDENCY_FROM.getMessage(workingPage));
        } catch (final DotSecurityException | DotDataException e) {
            Logger.error(this,
                    String.format("An error occurred when processing dependencies on HTML Page with ID '%s': %s", pageId,
                            e.getMessage()), e);
        }
    }

    @Override
    public void addAsset(Object asset, PusheableAsset pusheableAsset) {
        dependencyProcessor.addAsset(asset, pusheableAsset);
    }

    @Override
    public void waitUntilResolveAllDependencies() throws ExecutionException {
        dependencyProcessor.waitUntilResolveAllDependencies();
    }

    private <T> boolean add(final PusheableAsset pusheableAsset, final T asset, final String reason) {
        final boolean isAdded = config.add(asset, pusheableAsset, reason);

        if (isAdded) {
            pushedAssetUtil.savePushedAssetForAllEnv(asset, pusheableAsset);
        }

        return isAdded;
    }

    private <T> void tryToAddAllAndProcessDependencies(
            final PusheableAsset pusheableAsset, final Collection<T> assets, final String reason)
            throws DotDataException, DotSecurityException {

        if (UtilMethods.isSet(assets)) {
            assets.stream().forEach(asset -> {
                try {
                    final TryToAddResult tryToAddResult = tryToAdd(pusheableAsset, asset, reason);

                    if (TryToAddResult.Result.INCLUDE == tryToAddResult.result ||
                            ManifestReason.EXCLUDE_BY_FILTER != tryToAddResult.excludeReason) {
                        dependencyProcessor.addAsset(asset, pusheableAsset);
                    }
                } catch (AssetExcludeException e) {
                    dependencyProcessor.addAsset(asset, pusheableAsset);
                }
            });
        }
    }

    private <T> Collection<T> tryToAddAll(final PusheableAsset pusheableAsset,
            final Collection<T> assets, final String reason)
            throws DotDataException, DotSecurityException {

        if (assets != null) {
            return assets.stream()
                    .filter(asset -> {
                        try {
                            final TryToAddResult tryToAddResult = tryToAdd(pusheableAsset, asset,
                                    reason);
                            return TryToAddResult.Result.INCLUDE == tryToAddResult.result;
                        } catch (AssetExcludeException e) {
                            return false;
                        }
                    })
                    .collect(Collectors.toSet());
        } else {
            return Collections.emptySet();
        }
    }

    private void tryToAddAsDependency(final PusheableAsset pusheableAsset,
            final ManifestItem dependency, final ManifestItem from)
            throws DotDataException, DotSecurityException {

        tryToAddAndProcessDependencies(pusheableAsset, dependency,
                ManifestReason.INCLUDE_DEPENDENCY_FROM.getMessage(from));
    }

    /**
     * This method tries to add the specified dotCMS asset by dependency. If it cannot be added to the bundle, then a
     * new entry will be added to the bundle's MANIFEST file indicating the reason why.
     * <p>Additionally, the very same asset will be added to the Dependency Processor queue in order to determine what
     * other dependent assets might need to be added to the bundle as well.</p>
     *
     * @param pusheableAsset The type of asset that is being added to the bundle.
     * @param asset          The actual dotCMS object that is being added.
     * @param reason         The reason why this asset is being added to the bundle. Refer to {@link ManifestReason} for
     *                       more details.
     */
    private <T> void tryToAddAndProcessDependencies(final PusheableAsset pusheableAsset,
            final T asset, final String reason) {
        if (UtilMethods.isSet(asset)) {
            try {
                final TryToAddResult tryToAddResult = tryToAdd(pusheableAsset, asset, reason);

                if (TryToAddResult.Result.INCLUDE == tryToAddResult.result ||
                        ManifestReason.EXCLUDE_BY_FILTER != tryToAddResult.excludeReason) {
                    dependencyProcessor.addAsset(asset, pusheableAsset);
                }
            } catch (AssetExcludeException e) {
                dependencyProcessor.addAsset(asset, pusheableAsset);
            }
        }
    }

    /**
     * This method tries to add the specified dotCMS asset by dependency. If it cannot be added to the bundle, then a
     * new entry will be added to the bundle's MANIFEST file indicating the reason why.
     *
     * @param pusheableAsset The type of asset that is being added to the bundle.
     * @param asset          The actual dotCMS object that is being added.
     * @param reason         The reason why this asset is being added to the bundle. Refer to {@link ManifestReason} for
     *                       more details.
     *
     * @return If the asset was added to the bundle, {@code true} will be returned. Otherwise, {@code false} will be
     * returned.
     */
    private <T> boolean tryToAddSilently (
            final PusheableAsset pusheableAsset, final T asset, final String reason) {
        if (null == asset) {
            return false;
        }
        try {
            final TryToAddResult tryToAddResult = tryToAdd(pusheableAsset, asset, reason);
            return TryToAddResult.Result.INCLUDE == tryToAddResult.result;
        } catch (final AssetExcludeException e) {
            Logger.debug(PushPublishigDependencyProcesor.class,
                    () -> String.format("Asset [ %s ] has been excluded from bundle: %s", asset, e.getMessage()));
            return false;
        }
    }

    /**
     * This method tries to add the specified dotCMS asset by dependency. If it cannot be added to the bundle, then a
     * new entry will be added to the bundle's MANIFEST file indicating the reason why.
     *
     * @param pusheableAsset The type of asset that is being added to the bundle.
     * @param asset          The actual dotCMS object that is being added.
     * @param reason         The reason why this asset is being added to the bundle. Refer to {@link ManifestReason} for
     *                       more details.
     *
     * @return An instance of the {@link TryToAddResult} class indicating if the asset was included, excluded, and the
     * reason why.
     *
     * @throws AssetExcludeException
     */
    private synchronized <T> TryToAddResult tryToAdd(final PusheableAsset pusheableAsset, final T asset,
            final String reason)
            throws AssetExcludeException {
        if (config.contains(asset, pusheableAsset)) {
            return new TryToAddResult(TryToAddResult.Result.ALREADY_INCLUDE);
        }

        if (!UtilMethods.isSet(asset)) {
            return new TryToAddResult(TryToAddResult.Result.ALREADY_INCLUDE);
        }

        if (isSystemObject(asset)) {
            config.exclude(asset, pusheableAsset, ManifestReason.EXCLUDE_SYSTEM_OBJECT.getMessage());
            return new TryToAddResult(TryToAddResult.Result.EXCLUDE, ManifestReason.EXCLUDE_SYSTEM_OBJECT);
        }

        if (isExcludeByFilter(pusheableAsset)) {
            config.exclude(asset, pusheableAsset, ManifestReason.EXCLUDE_BY_FILTER.getMessage());
            return new TryToAddResult(TryToAddResult.Result.EXCLUDE, ManifestReason.EXCLUDE_BY_FILTER);
        }

        if ( config.getOperation() != Operation.PUBLISH ) {
            config.exclude(asset, pusheableAsset, ManifestReason.EXCLUDE_BY_OPERATION.getMessage(config.getOperation()));
            return new TryToAddResult(TryToAddResult.Result.EXCLUDE, ManifestReason.EXCLUDE_BY_OPERATION);
        }

        if (Contentlet.class.isInstance(asset) && !Contentlet.class.cast(asset).isHost() &&
                publisherFilter.doesExcludeDependencyQueryContainsContentletId(
                        ((Contentlet) asset).getIdentifier())) {
            config.exclude(asset, pusheableAsset, ManifestReason.EXCLUDE_BY_FILTER.getMessage());
            return new TryToAddResult(TryToAddResult.Result.EXCLUDE, ManifestReason.EXCLUDE_BY_FILTER);
        }

        if (!shouldCheckModDate(asset) ||
                !dependencyModDateUtil.excludeByModDate(asset, pusheableAsset)) {
            add(pusheableAsset, asset, reason);
            return new TryToAddResult(TryToAddResult.Result.INCLUDE);
        } else {
            config.exclude(asset, pusheableAsset,
                    ManifestReason.EXCLUDE_BY_MOD_DATE.getMessage(asset.getClass()));
            return new TryToAddResult(TryToAddResult.Result.EXCLUDE, ManifestReason.EXCLUDE_BY_MOD_DATE);
        }
    }

    private <T> boolean isExcludeByFilter(final PusheableAsset pusheableAsset) {
        return !isRelationshipObject(pusheableAsset) && (!publisherFilter.isDependencies() ||
            publisherFilter.doesExcludeDependencyClassesContainsType(pusheableAsset.getType()));
    }

    /**
     * Checks whether the specified Pusheable Asset represents a {@link Relationship} object or not. Unless the
     * {@code excludeDependencyClasses} attribute in the Filter Descriptor YAML file includes the {@link Relationship}
     * class, we should <b>ALWAYS</b> send the Relationship object when a Contentlet with a relationship field is being
     * pushed. Otherwise, shallow pushing a Contentlet with related content and then shallow pushing such a related
     * content will result in the parent content not having its relationship set as expected.
     *
     * @param pusheableAsset The {@link PusheableAsset} that is being analyzed.
     *
     * @return If the Pusheable Asset is a {@link Relationship} object, return {@code true}.
     */
    private boolean isRelationshipObject(final PusheableAsset pusheableAsset) {
        return (PusheableAsset.RELATIONSHIP == pusheableAsset && !publisherFilter.doesExcludeDependencyClassesContainsType(pusheableAsset.getType()));
    }

    private <T> boolean shouldCheckModDate(T asset) {
        return !Language.class.isInstance(asset);
    }

    private void setLanguageVariables() {
        final ContentletAPI contentletAPI = APILocator.getContentletAPI();
        try{
            //We're no longer filtering by language here..
            //The reason is We're simply collecting all available lang variables so we can infer additional languages used. see #15359
            final String langVarsQuery = "+contentType:" + LanguageVariableAPI.LANGUAGEVARIABLE ;
            final List<Contentlet> langVariables = contentletAPI.search(langVarsQuery, 0, -1, StringPool.BLANK, user, false);

            tryToAddAllAndProcessDependencies(PusheableAsset.CONTENTLET, langVariables,
                    ManifestReason.INCLUDE_AUTOMATIC_BY_DOTCMS.getMessage());
        }catch (Exception e){
            Logger.error(this, e.getMessage(),e);
        }
    }

    /**
     * Determines if the specified Push Publishing dependency represents one of the internal system objects:
     * <ol>
     *     <li>System Host.</li>
     *     <li>System Folder.</li>
     *     <li>System Template.</li>
     *     <li>System Container.</li>
     * </ol>
     * If it does, then it must NOT be added to any bundle at all.
     *
     * @param dependency The object being added to the bundle by dependency.
     *
     * @return If the dependency represents any of the internal system objects, returns {@code true}. Otherwise, returns
     * {@code false}.
     */
    private static boolean isSystemObject(final Object dependency) {
        try {
            final Host  systemHost = APILocator.getHostAPI().findSystemHost();
            final Folder systemFolder = APILocator.getFolderAPI().findSystemFolder();

            if (Contentlet.class.isInstance(dependency)){
                return  Contentlet.class.cast(dependency).getIdentifier().equals(systemHost.getIdentifier());
            } else  if (Folder.class.isInstance(dependency)){
                return Folder.class.cast(dependency).getIdentifier().equals(systemFolder.getIdentifier());
            } else if (SystemContainer.class.isInstance(dependency)) {
                return Container.SYSTEM_CONTAINER.equals(SystemContainer.class.cast(dependency).getIdentifier());
            } else if (SystemTemplate.class.isInstance(dependency)) {
                return Template.SYSTEM_TEMPLATE.equals(SystemTemplate.class.cast(dependency).getIdentifier());
            } else {
                return false;
            }
        } catch (final DotDataException e) {
            Logger.debug(PushPublishigDependencyProcesor.class, () -> e.getMessage());
            return false;
        }
    }

    private static class TryToAddResult {
        enum Result {
            INCLUDE, EXCLUDE, ALREADY_INCLUDE;
        }

        Result result;
        ManifestReason excludeReason;

        public TryToAddResult(final Result result) {
            this(result, null);
        }

        public TryToAddResult(final Result result, final ManifestReason excludeReason) {
            this.result = result;
            this.excludeReason = excludeReason;
        }
    }

}
