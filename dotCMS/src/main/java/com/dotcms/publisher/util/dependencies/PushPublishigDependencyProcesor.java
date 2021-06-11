package com.dotcms.publisher.util.dependencies;

import com.dotcms.contenttype.transform.contenttype.StructureTransformer;
import com.dotcms.languagevariable.business.LanguageVariableAPI;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publisher.util.PusheableAsset;
import com.dotcms.publishing.DotBundleException;
import com.dotcms.publishing.PublisherConfig.Operation;
import com.dotcms.publishing.PublisherFilter;
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
                    pushPublishigDependencyProvider.getTemplatesByHost(host));

            // Container dependencies
            tryToAddAllAndProcessDependencies(PusheableAsset.CONTAINER,
                    pushPublishigDependencyProvider.getContainersByHost(host));
            pushPublishigDependencyProvider.getFileContainersByHost(host).stream()
                    .forEach(fileContainer -> dependencyProcessor.addAsset(fileContainer.getIdentifier(),
                            PusheableAsset.CONTAINER));

            // Content dependencies
            tryToAddAllAndProcessDependencies(PusheableAsset.CONTENTLET,
                    pushPublishigDependencyProvider.getContentletByLuceneQuery(
                            "+conHost:" + host.getIdentifier()));

            // Structure dependencies
            tryToAddAllAndProcessDependencies(PusheableAsset.CONTENT_TYPE,
                    pushPublishigDependencyProvider.getContentTypeByHost(host));

            // Folder dependencies
            tryToAddAllAndProcessDependencies(PusheableAsset.FOLDER,
                    pushPublishigDependencyProvider.getFoldersByHost(host));

            // Rule dependencies
            tryToAddAllAndProcessDependencies(PusheableAsset.RULE,
                    pushPublishigDependencyProvider.getRulesByHost(host));

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
                tryToAddSilently(PusheableAsset.FOLDER, parentFolder);
            }

            tryToAddSilently(PusheableAsset.SITE,
                    pushPublishigDependencyProvider.getHostById(folder.getHostId()));

            // Content dependencies
            tryToAddAllAndProcessDependencies(PusheableAsset.CONTENTLET,
                    pushPublishigDependencyProvider.getContentletByLuceneQuery("+conFolder:" + folder.getInode()));

            // Menu Link dependencies
            tryToAddAllAndProcessDependencies(PusheableAsset.LINK,
                    pushPublishigDependencyProvider.getLinksByFolder(folder));

            // Structure dependencies
            tryToAddAllAndProcessDependencies(PusheableAsset.CONTENT_TYPE,
                    pushPublishigDependencyProvider.getContentTypeByFolder(folder));

            //Add the default structure of this folder
            tryToAddAndProcessDependencies(PusheableAsset.CONTENT_TYPE,
                    CacheLocator.getContentTypeCache()
                            .getStructureByInode(folder.getDefaultFileType()));

            // SubFolders
            tryToAddAllAndProcessDependencies(PusheableAsset.FOLDER,
                    APILocator.getFolderAPI().findSubFolders(folder, user, false));
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
                    pushPublishigDependencyProvider.getHostByTemplate(workingTemplate));

            addContainerByTemplate(workingTemplate);
            addContainerByTemplate(liveTemplate);

            tryToAddAndProcessDependencies(PusheableAsset.FOLDER,
                    pushPublishigDependencyProvider.getThemeByTemplate(workingTemplate));

            if(workingTemplate instanceof FileAssetTemplate){
                //Process FileAssetTemplate
                tryToAddAndProcessDependencies(PusheableAsset.FOLDER,
                        pushPublishigDependencyProvider.getFileAssetTemplateRootFolder(FileAssetTemplate.class.cast(workingTemplate)));
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
                        .collect(Collectors.toList())
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
                    pushPublishigDependencyProvider.getHostByContainer(containerById));

            // Content Type Dependencies
            tryToAddAllAndProcessDependencies(PusheableAsset.CONTENT_TYPE,
                    pushPublishigDependencyProvider.getContentTypeByWorkingContainer(containerId));

            tryToAddAllAndProcessDependencies(PusheableAsset.CONTENT_TYPE,
                    pushPublishigDependencyProvider.getContentTypeByLiveContainer(containerId));

            if (containerById instanceof FileAssetContainer) {
                // Process FileAssetContainer
                tryToAddAndProcessDependencies(PusheableAsset.FOLDER,
                        pushPublishigDependencyProvider.getFileAssetContainerRootFolder(FileAssetContainer.class.cast(containerById)));
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
                    pushPublishigDependencyProvider.getHostById(structure.getHost()));

            // Folder Dependencies
            tryToAddSilently(PusheableAsset.FOLDER,
                    pushPublishigDependencyProvider.getFolderById(structure.getFolder()));

            // Workflows Dependencies
            tryToAddAll(PusheableAsset.WORKFLOW,
                    pushPublishigDependencyProvider.getWorkflowSchemasByContentType(structure));

            // Categories Dependencies
            tryToAddAll(PusheableAsset.CATEGORY, APILocator.getCategoryAPI()
                    .findCategories(new StructureTransformer(structure).from(), user));

            // Related structures
            tryToAddAllAndProcessDependencies(PusheableAsset.RELATIONSHIP,
                    APILocator.getRelationshipAPI().byContentType(structure));

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
                    pushPublishigDependencyProvider.getFolderByParentIdentifier(ident));

            // Host Dependencies
            tryToAddSilently(PusheableAsset.SITE,
                    pushPublishigDependencyProvider.getHostById(ident.getHostId()));

            // Content Dependencies
            tryToAddAllAndProcessDependencies(PusheableAsset.CONTENTLET,
                    pushPublishigDependencyProvider.getContentletsByLink(linkId));
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
                        pushPublishigDependencyProvider.getHostById(contentletVersion.getHost()));

                contentsToProcess.add(contentletVersion);

                // Relationships Dependencies
                final Map<Relationship, List<Contentlet>> contentRelationships = APILocator
                        .getContentletAPI().findContentRelationships(contentletVersion, user);

                tryToAddAllAndProcessDependencies(PusheableAsset.RELATIONSHIP, contentRelationships.keySet());

                if(publisherFilter.isRelationships()) {
                    contentRelationships.values().stream()
                            .forEach(contentlets -> contentsToProcess.addAll(contentlets));
                }

            }

            for (final Contentlet contentletToProcess : contentsToProcess) {
                // Host Dependency
                tryToAddSilently(PusheableAsset.SITE,
                        pushPublishigDependencyProvider.getHostById(contentletToProcess.getHost()));

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
                        pushPublishigDependencyProvider.getHostById(contentletWithDependenciesToProcess.getHost()));

                // Content Dependency
                tryToAddSilently(PusheableAsset.CONTENTLET, contentletWithDependenciesToProcess);

                // Folder Dependency
                tryToAddSilently(PusheableAsset.FOLDER,
                        pushPublishigDependencyProvider.getFolderById(contentletWithDependenciesToProcess.getFolder()));

                // Language Dependency
                tryToAddAndProcessDependencies(PusheableAsset.LANGUAGE,
                        APILocator.getLanguageAPI().getLanguage(contentletWithDependenciesToProcess.getLanguageId())
                );

                try {
                    if (Config.getBooleanProperty("PUSH_PUBLISHING_PUSH_ALL_FOLDER_PAGES", false)
                            && contentletWithDependenciesToProcess.getStructure().getStructureType() == Structure.STRUCTURE_TYPE_HTMLPAGE) {

                        final Folder contFolder=APILocator.getFolderAPI()
                                .find(contentletWithDependenciesToProcess.getFolder(), user, false);

                        tryToAddAllAndProcessDependencies(PusheableAsset.CONTENTLET,
                                pushPublishigDependencyProvider.getHTMLPages(contFolder));
                    }
                } catch (Exception e) {
                    Logger.debug(this, e.toString());
                }

                if(Config.getBooleanProperty("PUSH_PUBLISHING_PUSH_STRUCTURES", true)) {
                    tryToAddAndProcessDependencies(PusheableAsset.CONTENT_TYPE,
                            CacheLocator.getContentTypeCache()
                                    .getStructureByInode(contentletWithDependenciesToProcess.getStructureInode())
                    );

                }

                // Evaluate all the categories from this  to include as dependency.
                tryToAddAll(PusheableAsset.CATEGORY, APILocator.getCategoryAPI()
                                .getParents(contentletWithDependenciesToProcess, APILocator.systemUser(), false)
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
                            hostAPI.find(rule.getParent(), this.user, false));
                } else if (parent.isHTMLPage()) {
                    tryToAddSilently(PusheableAsset.CONTENTLET, parent);
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
            tryToAddAll(PusheableAsset.CONTENTLET, listKeyValueLang);

            final String contentTypeId = listKeyValueLang.get(0).getContentTypeId();
            tryToAddSilently(PusheableAsset.CONTENT_TYPE,
                    CacheLocator.getContentTypeCache().getStructureByInode(contentTypeId));

        } catch (Exception e) {
            throw new DotBundleException(this.getClass().getName() + " : " + "generate()"
                    + e.getMessage() + ": Unable to pull content", e);
        }
    }

    public void processRelationshipDependencies(final Relationship relationship) {

        try {
            tryToAddAndProcessDependencies(PusheableAsset.CONTENT_TYPE,
                    CacheLocator.getContentTypeCache()
                            .getStructureByInode(relationship.getChildStructureInode()));

            tryToAddAndProcessDependencies(PusheableAsset.CONTENT_TYPE, CacheLocator.getContentTypeCache()
                            .getStructureByInode(relationship.getParentStructureInode()));
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
                    pushPublishigDependencyProvider.getHostById(identifier.getHostId()));

            // Folder dependencies
            tryToAddSilently(PusheableAsset.FOLDER,
                    pushPublishigDependencyProvider.getFolderByParentIdentifier(identifier));

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
                    tryToAddAndProcessDependencies(PusheableAsset.TEMPLATE, workingTemplateWP);
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
                    tryToAddAndProcessDependencies(PusheableAsset.TEMPLATE, liveTemplateLP);
                } else {
                    dependencyProcessor.addAsset(liveTemplateLP, PusheableAsset.TEMPLATE);
                }
            }

            // Contents dependencies
            tryToAddAllAndProcessDependencies(PusheableAsset.CONTENTLET,
                    pushPublishigDependencyProvider.getContentletsByPage(workingPage));


            // Rule dependencies
            tryToAddAllAndProcessDependencies(PusheableAsset.RULE,
                    pushPublishigDependencyProvider.getRuleByPage(workingPage));
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

    private <T> boolean add(final PusheableAsset pusheableAsset, final T asset) {
        final boolean isAdded = config.add(asset, pusheableAsset);

        if (isAdded) {
            pushedAssetUtil.savePushedAssetForAllEnv(asset, pusheableAsset);
        }

        return isAdded;
    }

    private <T> void tryToAddAllAndProcessDependencies(
            final PusheableAsset pusheableAsset, final Collection<T> assets)
            throws DotDataException, DotSecurityException {

        assets.stream().forEach(asset -> {
            try {
                tryToAdd(pusheableAsset, asset);
                dependencyProcessor.addAsset(asset, pusheableAsset);
            } catch (AssetExcludeByFilterException e) {
                //ignore
            } catch (AssetExcludeException e) {
                dependencyProcessor.addAsset(asset, pusheableAsset);
            }
        });
    }

    private <T> Collection<T> tryToAddAll(
            final PusheableAsset pusheableAsset, final Collection<T> assets)
            throws DotDataException, DotSecurityException {

        if (assets != null) {
            return assets.stream()
                    .filter(asset -> {
                        try {
                            return tryToAdd(pusheableAsset, asset);
                        } catch (AssetExcludeException e) {
                            return false;
                        }
                    })
                    .collect(Collectors.toSet());
        } else {
            return Collections.emptySet();
        }
    }

    private <T> void tryToAddAndProcessDependencies(final PusheableAsset pusheableAsset, final T asset)
            throws DotDataException, DotSecurityException {
        try {
            tryToAdd(pusheableAsset, asset);
            config.addWithDependencies(asset, pusheableAsset);
        } catch (AssetExcludeByFilterException e) {
            //ignore
        } catch (AssetExcludeException e) {
            dependencyProcessor.addAsset(asset, pusheableAsset);
        }
    }

    private <T> boolean tryToAddSilently (
            final PusheableAsset pusheableAsset, final T asset)
            throws DotDataException, DotSecurityException{
        try {
            return tryToAdd(pusheableAsset, asset);
        } catch (AssetExcludeException e) {
            return false;
        }
    }

    private synchronized <T> boolean tryToAdd(final PusheableAsset pusheableAsset, final T asset)
            throws AssetExcludeException {

        if (isExcludeByFilter(pusheableAsset)) {
            throw new AssetExcludeByFilterException(String.format("Exclude by Operation %s",
                    config.getOperation()));
        }

        if ( config.getOperation() != Operation.PUBLISH ) {
            this.pushedAssetUtil.removePushedAssetForAllEnv(asset, pusheableAsset);
            throw new AssetExcludeException(String.format("Exclude by Operation %s",
                    config.getOperation()));
        }

        if (Contentlet.class.isInstance(asset) && !Contentlet.class.cast(asset).isHost() &&
                publisherFilter.doesExcludeDependencyQueryContainsContentletId(
                        ((Contentlet) asset).getIdentifier())) {

            throw new AssetExcludeByFilterException(String.format("Exclude by Contentlet Id Filter: %s",
                    ((Contentlet) asset).getIdentifier()));
        }

        if (!shouldCheckModDate(asset) ||
                !dependencyModDateUtil.excludeByModDate(asset, pusheableAsset)) {

            return add(pusheableAsset, asset);
        } else {
            throw new AssetExcludeException(String.format("Exclude by Moddate"));
        }
    }

    private <T> boolean isExcludeByFilter(final PusheableAsset pusheableAsset) {

        return (PusheableAsset.RELATIONSHIP == pusheableAsset && !publisherFilter.isRelationships()) ||
                publisherFilter.doesExcludeDependencyClassesContainsType(pusheableAsset.getType());
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

            tryToAddAllAndProcessDependencies(PusheableAsset.CONTENTLET, langVariables);
        }catch (Exception e){
            Logger.error(this, e.getMessage(),e);
        }
    }
}
