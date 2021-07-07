package com.dotcms.publisher.util.dependencies;

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
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import io.vavr.control.Try;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;


public class PushPublishigDependencyProcesor implements DependencyProcessor{

    private PublisherFilter publisherFilter;
    private PushPublisherConfig config;
    private User user;
    private ConcurrentDependencyProcessor dependencyProcessor;
    private PushPublishigDependencyProvider pushPublishigDependencyProvider;

    private DependencyModDateUtil dependencyModDateUtil;
    private PushedAssetUtil pushedAssetUtil;

    public PushPublishigDependencyProcesor(final User user, final PushPublisherConfig config,
            final PublisherFilter publisherFilter) {

        this.publisherFilter = publisherFilter;
        this.config = config;
        this.user = user;

        pushedAssetUtil = new PushedAssetUtil(config);
        dependencyModDateUtil = new DependencyModDateUtil(config);

        pushPublishigDependencyProvider = new PushPublishigDependencyProvider(user);

        dependencyProcessor = new ConcurrentDependencyProcessor();

        dependencyProcessor.addProcessor(PusheableAsset.SITE, (host) ->
                proccessHostDependency((Host) host));

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

        if (publisherFilter.isDependencies()) {
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
    private void proccessHostDependency (final Host host) {
        try {
            // Template dependencies
            tryToAddAllAndProcessDependencies(PusheableAsset.TEMPLATE,
                    pushPublishigDependencyProvider.getTemplatesByHost(host),
                    ManifestReason.INCLUDE_DEPENDENCY_FROM.getMessage(host));

            // Container dependencies
            tryToAddAllAndProcessDependencies(PusheableAsset.CONTAINER,
                    pushPublishigDependencyProvider.getContainersByHost(host),
                    ManifestReason.INCLUDE_DEPENDENCY_FROM.getMessage(host));

            pushPublishigDependencyProvider.getFileContainersByHost(host).stream()
                    .forEach(fileContainer -> dependencyProcessor.addAsset(fileContainer,
                            PusheableAsset.CONTAINER));

            // Content dependencies
            tryToAddAllAndProcessDependencies(PusheableAsset.CONTENTLET,
                    pushPublishigDependencyProvider.getContentletByLuceneQuery(
                            "+conHost:" + host.getIdentifier()),
                    ManifestReason.INCLUDE_DEPENDENCY_FROM.getMessage(host));

            // Structure dependencies
            tryToAddAllAndProcessDependencies(PusheableAsset.CONTENT_TYPE,
                    pushPublishigDependencyProvider.getContentTypeByHost(host),
                    ManifestReason.INCLUDE_DEPENDENCY_FROM.getMessage(host));

            // Folder dependencies
            tryToAddAllAndProcessDependencies(PusheableAsset.FOLDER,
                    pushPublishigDependencyProvider.getFoldersByHost(host),
                    ManifestReason.INCLUDE_DEPENDENCY_FROM.getMessage(host));

            // Rule dependencies
            tryToAddAllAndProcessDependencies(PusheableAsset.RULE,
                    pushPublishigDependencyProvider.getRulesByHost(host),
                    ManifestReason.INCLUDE_DEPENDENCY_FROM.getMessage(host));

        } catch (DotSecurityException | DotDataException e) {
            Logger.error(this, e.getMessage(),e);
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
        } catch (DotSecurityException | DotDataException e) {
            Logger.error(this, e.getMessage(),e);
        }
    }

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
        } catch (DotSecurityException | DotDataException e) {

            Logger.error(this, e.getMessage(),e);
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
     * For given Containers adds its dependencies:
     * <ul>
     * <li>Hosts</li>
     * <li>Structures</li>
     * </ul>
     */
    private void processContainerDependency(final Container container)  {

        try {
            final String containerId = container.getIdentifier();
            final Container containerById = APILocator.getContainerAPI()
                    .getWorkingContainerById(containerId, user, false);

            // Host Dependency
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

        } catch (DotSecurityException e) {

            Logger.error(this, e.getMessage(),e);
        } catch (DotDataException e) {
            Logger.error(this, e.getMessage(),e);
        }

    }

    /**
     *
     * @throws DotDataException
     * @throws DotSecurityException
     */
    private void processContentTypeDependency(final Structure structure) {
        try{
            // Host Dependency
            tryToAddSilently(PusheableAsset.SITE,
                    pushPublishigDependencyProvider.getHostById(structure.getHost()),
                    ManifestReason.INCLUDE_DEPENDENCY_FROM.getMessage(structure));

            // Folder Dependencies
            tryToAddSilently(PusheableAsset.FOLDER,
                    pushPublishigDependencyProvider.getFolderById(structure.getFolder()),
                    ManifestReason.INCLUDE_DEPENDENCY_FROM.getMessage(structure));

            // Workflows Dependencies
            tryToAddAll(PusheableAsset.WORKFLOW,
                    pushPublishigDependencyProvider.getWorkflowSchemasByContentType(structure),
                    ManifestReason.INCLUDE_DEPENDENCY_FROM.getMessage(structure));

            // Categories Dependencies
            tryToAddAll(PusheableAsset.CATEGORY, APILocator.getCategoryAPI()
                    .findCategories(new StructureTransformer(structure).from(), user),
                    ManifestReason.INCLUDE_DEPENDENCY_FROM.getMessage(structure));

            // Related structures
            tryToAddAllAndProcessDependencies(PusheableAsset.RELATIONSHIP,
                    APILocator.getRelationshipAPI().byContentType(structure),
                    ManifestReason.INCLUDE_DEPENDENCY_FROM.getMessage(structure));

        } catch (DotDataException | DotSecurityException e) {
            Logger.error(this, e.getMessage(),e);
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
        } catch (Exception e) {
            Logger.error(this, "can't load menuLink deps "+linkId,e);
        }
    }

    /**
     *
     * @throws DotDataException
     * @throws DotSecurityException
     */
    private void processContentDependency(final Contentlet contentlet)
            throws  DotBundleException {

        try {
            final String contentId = contentlet.getIdentifier();
            final Identifier ident = APILocator.getIdentifierAPI().find(contentId);
            final List<Contentlet> contentList =
                    APILocator.getContentletAPI().findAllVersions(ident, false, user, false);

            final Set<Contentlet> contentsToProcess = new HashSet<Contentlet>();
            final Set<Contentlet> contentsWithDependenciesToProcess = new HashSet<Contentlet>();

            for (final Contentlet contentletVersion : contentList) {

                if (contentletVersion.isHTMLPage()) {
                    processHTMLPagesDependency(contentletVersion.getIdentifier());
                }

                // Host Dependency
                tryToAddSilently(PusheableAsset.SITE,
                        pushPublishigDependencyProvider.getHostById(contentletVersion.getHost()),
                        ManifestReason.INCLUDE_DEPENDENCY_FROM.getMessage(contentlet));

                contentsToProcess.add(contentletVersion);

                // Relationships Dependencies
                final Map<Relationship, List<Contentlet>> contentRelationships = APILocator
                        .getContentletAPI().findContentRelationships(contentletVersion, user);

                tryToAddAllAndProcessDependencies(PusheableAsset.RELATIONSHIP,
                        contentRelationships.keySet(), ManifestReason.INCLUDE_DEPENDENCY_FROM.getMessage(contentlet));

                if(publisherFilter.isRelationships()) {
                    contentRelationships.values().stream()
                            .forEach(contentlets -> contentsToProcess.addAll(contentlets));
                }

            }

            for (final Contentlet contentletToProcess : contentsToProcess) {
                // Host Dependency
                tryToAddSilently(PusheableAsset.SITE,
                        pushPublishigDependencyProvider.getHostById(contentletToProcess.getHost()),
                        ManifestReason.INCLUDE_DEPENDENCY_FROM.getMessage(contentlet));

                contentsWithDependenciesToProcess.add(contentletToProcess);
                //Copy asset files to bundle folder keeping original folders structure
                final List<Field> fields= FieldsCache.getFieldsByStructureInode(contentletToProcess.getStructureInode());

                for(final Field field : fields) {
                    if (field.getFieldType().equals(Field.FieldType.IMAGE.toString())
                            || field.getFieldType().equals(Field.FieldType.FILE.toString())) {

                        try {
                            String value = "";
                            if(UtilMethods.isSet(APILocator.getContentletAPI().getFieldValue(contentletToProcess, field))){
                                value = APILocator.getContentletAPI().getFieldValue(contentletToProcess, field).toString();
                            }
                            final Identifier id = APILocator.getIdentifierAPI().find(value);
                            if (InodeUtils.isSet(id.getInode()) && id.getAssetType().equals("contentlet")) {
                                contentsWithDependenciesToProcess.addAll(APILocator.getContentletAPI().findAllVersions(id, false, user, false));
                            }
                        } catch (Exception ex) {
                            Logger.debug(this, ex.toString());
                            throw new DotStateException("Problem occured while publishing file:" +ex.getMessage(), ex );
                        }
                    }

                }
            }

            // Adding the Contents (including related) and adding filesAsContent
            for (final Contentlet contentletWithDependenciesToProcess : contentsWithDependenciesToProcess) {
                // Host Dependency
                tryToAddSilently(PusheableAsset.SITE,
                        pushPublishigDependencyProvider.getHostById(contentletWithDependenciesToProcess.getHost()),
                        ManifestReason.INCLUDE_DEPENDENCY_FROM.getMessage(contentlet));

                // Content Dependency
                tryToAddSilently(PusheableAsset.CONTENTLET, contentletWithDependenciesToProcess,
                        ManifestReason.INCLUDE_DEPENDENCY_FROM.getMessage(contentlet));

                // Folder Dependency
                tryToAddSilently(PusheableAsset.FOLDER,
                        pushPublishigDependencyProvider.getFolderById(contentletWithDependenciesToProcess.getFolder()),
                        ManifestReason.INCLUDE_DEPENDENCY_FROM.getMessage(contentlet));

                // Language Dependency
                tryToAddAsDependency(PusheableAsset.LANGUAGE,
                        APILocator.getLanguageAPI().getLanguage(contentletWithDependenciesToProcess.getLanguageId()),
                        contentlet
                );

                try {
                    if (Config.getBooleanProperty("PUSH_PUBLISHING_PUSH_ALL_FOLDER_PAGES", false)
                            && contentletWithDependenciesToProcess.getStructure().getStructureType() == Structure.STRUCTURE_TYPE_HTMLPAGE) {

                        final Folder contFolder=APILocator.getFolderAPI()
                                .find(contentletWithDependenciesToProcess.getFolder(), user, false);

                        tryToAddAllAndProcessDependencies(PusheableAsset.CONTENTLET,
                                pushPublishigDependencyProvider.getHTMLPages(contFolder),
                                ManifestReason.INCLUDE_DEPENDENCY_FROM.getMessage(contentlet));
                    }
                } catch (Exception e) {
                    Logger.debug(this, e.toString());
                }

                if(Config.getBooleanProperty("PUSH_PUBLISHING_PUSH_STRUCTURES", true)) {
                    tryToAddAsDependency(PusheableAsset.CONTENT_TYPE,
                            CacheLocator.getContentTypeCache()
                                    .getStructureByInode(contentletWithDependenciesToProcess.getStructureInode()),
                            contentlet
                    );

                }

                // Evaluate all the categories from this  to include as dependency.
                tryToAddAll(PusheableAsset.CATEGORY, APILocator.getCategoryAPI()
                                .getParents(contentletWithDependenciesToProcess, APILocator.systemUser(), false),
                        ManifestReason.INCLUDE_DEPENDENCY_FROM.getMessage(contentlet)
                );

            }
        } catch (Exception e) {
            throw new DotBundleException(this.getClass().getName() + " : " + "generate()"
                    + e.getMessage() + ": Unable to pull content", e);
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
        String ruleToProcess = "";
        final HostAPI hostAPI = APILocator.getHostAPI();
        final ContentletAPI contentletAPI = APILocator.getContentletAPI();

        try {
            ruleToProcess = rule.getId();
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
                    throw new DotDataException("The parent ID [" + parent.getIdentifier() + "] is a non-valid parent.");
                }
            } else {
                throw new DotDataException("The parent ID [" + rule.getParent() + "] cannot be found for Rule [" + rule.getId() + "]");
            }
        } catch (DotDataException e) {
            Logger.error(this, "Dependencies for rule [" + ruleToProcess + "] could not be set: " + e.getMessage(), e);
        } catch (DotSecurityException e) {
            Logger.error(this, "Dependencies for rule [" + ruleToProcess + "] could not be set: " + e.getMessage(), e);
        }
    }

    private void processLanguage(final Language language) throws DotBundleException {
        try{
            final long lang = language.getId();
            final String keyValueQuery = "+contentType:" + LanguageVariableAPI.LANGUAGEVARIABLE + " +languageId:" + lang;
            final List<Contentlet> listKeyValueLang = APILocator.getContentletAPI()
                    .search(keyValueQuery,0, -1, StringPool.BLANK, user, false);
            tryToAddAll(PusheableAsset.CONTENTLET, listKeyValueLang,
                    ManifestReason.INCLUDE_DEPENDENCY_FROM.getMessage(language));

            final String contentTypeId = listKeyValueLang.get(0).getContentTypeId();
            tryToAddSilently(PusheableAsset.CONTENT_TYPE,
                    CacheLocator.getContentTypeCache().getStructureByInode(contentTypeId),
                    ManifestReason.INCLUDE_DEPENDENCY_FROM.getMessage(language));

        } catch (Exception e) {
            throw new DotBundleException(this.getClass().getName() + " : " + "generate()"
                    + e.getMessage() + ": Unable to pull content", e);
        }
    }

    public void processRelationshipDependencies(final Relationship relationship) {

        try {
            tryToAddAsDependency(PusheableAsset.CONTENT_TYPE,
                    CacheLocator.getContentTypeCache()
                            .getStructureByInode(relationship.getChildStructureInode()), relationship);

            tryToAddAsDependency(PusheableAsset.CONTENT_TYPE, CacheLocator.getContentTypeCache()
                            .getStructureByInode(relationship.getParentStructureInode()), relationship);
        } catch (DotDataException | DotSecurityException e) {
            Logger.error(this, e.getMessage(),e);
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

            // Host dependency
            tryToAddSilently(PusheableAsset.SITE,
                    pushPublishigDependencyProvider.getHostById(identifier.getHostId()),
                    ManifestReason.INCLUDE_DEPENDENCY_FROM.getMessage(pageId));

            // Folder dependencies
            tryToAddSilently(PusheableAsset.FOLDER,
                    pushPublishigDependencyProvider.getFolderByParentIdentifier(identifier),
                    ManifestReason.INCLUDE_DEPENDENCY_FROM.getMessage(pageId));

            // looking for working version (must exists)
            final IHTMLPage workingPage = Try.of(
                    ()->APILocator.getHTMLPageAssetAPI().findByIdLanguageFallback(
                            identifier,
                            APILocator.getLanguageAPI().getDefaultLanguage().getId(),
                            false,
                            user,
                            false)
            ).onFailure(e->Logger.warnAndDebug(DependencyManager.class, e)).getOrNull();

            if(workingPage==null) {
                return;
            }

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
                    ManifestReason.INCLUDE_DEPENDENCY_FROM.getMessage(pageId));


            // Rule dependencies
            tryToAddAllAndProcessDependencies(PusheableAsset.RULE,
                    pushPublishigDependencyProvider.getRuleByPage(workingPage),
                    ManifestReason.INCLUDE_DEPENDENCY_FROM.getMessage(pageId));
        } catch (DotSecurityException | DotDataException e) {
            Logger.error(this, e.getMessage(),e);
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

    private <T> boolean tryToAddSilently (
            final PusheableAsset pusheableAsset, final T asset, final String reason)
            throws DotDataException, DotSecurityException{
        try {
            final TryToAddResult tryToAddResult = tryToAdd(pusheableAsset, asset, reason);
            return TryToAddResult.Result.INCLUDE == tryToAddResult.result;
        } catch (AssetExcludeException e) {
            return false;
        }
    }

    private synchronized <T> TryToAddResult tryToAdd(final PusheableAsset pusheableAsset, final T asset,
            final String reason)
            throws AssetExcludeException {

        if (config.contains(asset, pusheableAsset)) {
            return new TryToAddResult(TryToAddResult.Result.ALREADY_INCLUDE);
        }

        if (!UtilMethods.isSet(asset)) {
            return new TryToAddResult(TryToAddResult.Result.ALREADY_INCLUDE);
        }

        if (isHostFolderSystem(asset)) {
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

        return (
            !publisherFilter.isDependencies() ||
            PusheableAsset.RELATIONSHIP == pusheableAsset && !publisherFilter.isRelationships()) ||
            publisherFilter.doesExcludeDependencyClassesContainsType(pusheableAsset.getType()
        );
    }


    private <T> boolean shouldCheckModDate(T asset) {
        return !Language.class.isInstance(asset);
    }

    private void setLanguageVariables() {
        final ContentletAPI contentletAPI = APILocator.getContentletAPI();
        final Date date = new Date();
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

    private static boolean isHostFolderSystem(Object dependency) {

        try {
            final Host  systemHost = APILocator.getHostAPI().findSystemHost();
            final Folder systemFolder = APILocator.getFolderAPI().findSystemFolder();

            if (Contentlet.class.isInstance(dependency)){
                return ((Contentlet) dependency).getIdentifier().equals(systemHost.getIdentifier());
            } else  if (Folder.class.isInstance(dependency)){
                return ((Folder) dependency).getIdentifier().equals(systemFolder.getIdentifier());
            } else {
                return false;
            }
        } catch (DotDataException e) {
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
