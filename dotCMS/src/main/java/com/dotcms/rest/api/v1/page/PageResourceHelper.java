package com.dotcms.rest.api.v1.page;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.exception.ExceptionUtil;
import com.dotcms.mock.request.CachedParameterDecorator;
import com.dotcms.mock.request.HttpServletRequestParameterDecoratorWrapper;
import com.dotcms.mock.request.LanguageIdParameterDecorator;
import com.dotcms.mock.request.ParameterDecorator;
import com.dotcms.rendering.velocity.directive.ParseContainer;
import com.dotcms.rendering.velocity.services.ContentletLoader;
import com.dotcms.rendering.velocity.viewtools.DotTemplateTool;
import com.dotcms.rest.ErrorEntity;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.exception.BadRequestException;
import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.DotPreconditions;
import com.dotcms.vanityurl.business.VanityUrlAPI;
import com.dotcms.vanityurl.model.CachedVanityUrl;
import com.dotcms.variant.VariantAPI;
import com.dotcms.variant.business.web.VariantWebAPI.RenderContext;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.PermissionLevel;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.business.web.HostWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.db.FlushCacheRunnable;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.MultiTreeAPI;
import com.dotmarketing.portlets.containers.business.FileAssetContainerUtil;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI;
import com.dotmarketing.portlets.htmlpageasset.business.render.HTMLPageAssetNotFoundException;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.personas.model.Persona;
import com.dotmarketing.portlets.templates.business.TemplateAPI;
import com.dotmarketing.portlets.templates.business.TemplateSaveParameters;
import com.dotmarketing.portlets.templates.design.bean.ContainerUUID;
import com.dotmarketing.portlets.templates.design.bean.TemplateLayout;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.ImmutableList;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.jetbrains.annotations.NotNull;

import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.dotcms.util.CollectionsUtils.list;

/**
 * Provides the utility methods that interact with HTML Pages in dotCMS. These methods are used by
 * the Page REST end-point.
 *
 * @author Will Ezell
 * @author Jose Castro
 * @version 4.2
 * @since Oct 6, 2017
 */
public class PageResourceHelper implements Serializable {

    private static final long serialVersionUID = 296763857542258211L;

    private final HostWebAPI hostWebAPI = WebAPILocator.getHostWebAPI();
    private final HTMLPageAssetAPI htmlPageAssetAPI = APILocator.getHTMLPageAssetAPI();
    private final TemplateAPI templateAPI = APILocator.getTemplateAPI();
    private final ContentletAPI contentletAPI = APILocator.getContentletAPI();
    private final HostAPI hostAPI = APILocator.getHostAPI();
    private final MultiTreeAPI multiTreeAPI = APILocator.getMultiTreeAPI();
    private final UserAPI userAPI = APILocator.getUserAPI();
    private final PermissionAPI permissionAPI = APILocator.getPermissionAPI();
    private final transient LanguageAPI languageAPI = APILocator.getLanguageAPI();
    private final transient VanityUrlAPI vanityUrlAPI = APILocator.getVanityUrlAPI();

    /**
     * Private constructor
     */
    private PageResourceHelper() {

    }

    /**
     * Provides a singleton instance of the {@link PageResourceHelper}
     */
    private static class SingletonHolder {
        private static final PageResourceHelper INSTANCE = new PageResourceHelper();
    }

    /**
     * Returns a singleton instance of this class.
     *
     * @return A single instance of this class.
     */
    public static PageResourceHelper getInstance() {
        return PageResourceHelper.SingletonHolder.INSTANCE;
    }

    private static final ParameterDecorator LANGUAGE_PARAMETER_DECORATOR = new LanguageIdParameterDecorator();


    public HttpServletRequest decorateRequest(final HttpServletRequest request) {

        final HttpServletRequest wrapRequest = new HttpServletRequestParameterDecoratorWrapper(request,
                new CachedParameterDecorator(LANGUAGE_PARAMETER_DECORATOR));
        HttpServletRequestThreadLocal.INSTANCE.setRequest(wrapRequest);
        return wrapRequest;
    }

    /**
     * Saves the list of Containers and their respective Contentlet IDs for a given HTML Page.
     *
     * @param pageId           The Identifier of the HTML Page whose contents are being updated.
     * @param containerEntries The list of Containers and Contentlets in the form of
     *                         {@link ContainerEntry} objects.
     * @param language         The {@link Language} of the Contentlets for this page.
     * @throws DotDataException An error occurred when interacting with the data source.
     * @param variantName      The variant name
     * @return The list of saved Contentlets with the containerId, contentletId, uuid and styleProperties.
     * @throws BadRequestException if style validation fails
     */
    @WrapInTransaction
    public List<ContentView> saveContent(final String pageId,
            final List<ContainerEntry> containerEntries,
            final Language language, String variantName) throws DotDataException {

        final Map<String, List<MultiTree>> multiTreesMap = new HashMap<>();

        for (final ContainerEntry containerEntry : containerEntries) {
            int i = 0;
            final List<String> contentIds = containerEntry.getContentIds();
            final String personalization = UtilMethods.isSet(containerEntry.getPersonaTag()) ?
                    Persona.DOT_PERSONA_PREFIX_SCHEME + StringPool.COLON + containerEntry.getPersonaTag() :
                    MultiTree.DOT_PERSONALIZATION_DEFAULT;
            final Map<String, Map<String, Object>> stylePropertiesMap = containerEntry.getStylePropertiesMap();

            // Validate style properties during the saving process
            stylePropertiesValidation(stylePropertiesMap, contentIds,
                    containerEntry.getContainerId(), containerEntry.getContainerUUID());

            if (UtilMethods.isSet(contentIds)) {
                for (final String contentletId : contentIds) {
                    final Map<String, Object> styleProperties = stylePropertiesMap.get(contentletId);

                    final MultiTree multiTree = new MultiTree().setContainer(containerEntry.getContainerId())
                            .setContentlet(contentletId)
                            .setInstanceId(containerEntry.getContainerUUID())
                            .setTreeOrder(i++)
                            .setHtmlPage(pageId)
                            .setVariantId(variantName)
                            .setStyleProperties(styleProperties);

                    CollectionsUtils.computeSubValueIfAbsent(
                            multiTreesMap, personalization,
                            MultiTree.personalized(multiTree, personalization),
                            CollectionsUtils::add,
                            (String key, MultiTree multitree) -> list(multitree));

                    HibernateUtil.addCommitListener(new FlushCacheRunnable() {

                        @Override
                        public void run() {
                            try {
                                Contentlet contentlet =
                                        contentletAPI.findContentletByIdentifierAnyLanguage(contentletId, variantName);

                                if (contentlet == null && !VariantAPI.DEFAULT_VARIANT.equals(variantName)) {
                                    contentlet = contentletAPI.findContentletByIdentifierAnyLanguage(contentletId,
                                            VariantAPI.DEFAULT_VARIANT.name());
                                }

                                new ContentletLoader().invalidate(contentlet, PageMode.EDIT_MODE);
                            } catch (final DotDataException e) {
                                Logger.warn(this, String.format("Contentlet with ID '%s' could not be invalidated " +
                                                                        "from cache: %s", contentletId,
                                        e.getMessage()));
                            }
                        }

                    });
                }
            } else {
                multiTreesMap.computeIfAbsent(personalization, key -> new ArrayList<>());
            }
        }
        for (final String personalization : multiTreesMap.keySet()) {
            multiTreeAPI.overridesMultitreesByPersonalization(pageId, personalization,
                    multiTreesMap.get(personalization), Optional.of(language.getId()),
                    variantName);
        }

        // MultiTrees as a flattened list
        final List<MultiTree> savedMultiTrees = multiTreesMap.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());

        // Response with container, contentlet and styleProperties
        return buildSaveContentResponse(savedMultiTrees);
    }

    /**
     * Validates the style properties for a given contentlet and container during the saving process.
     * @param stylePropertiesMap The map of style properties.
     * @param contentIds The list of contentlet ids.
     * @param containerId The id of the container.
     * @param containerUUID The uuid of the container.
     * @throws BadRequestException if the style properties are invalid.
     */
    private void stylePropertiesValidation(
            Map<String, Map<String, Object>> stylePropertiesMap,
            List<String> contentIds, String containerId, String containerUUID
    ) {
        if (!stylePropertiesMap.isEmpty()) {
            final List<ErrorEntity> errors = new ArrayList<>();

            stylePropertiesMap.forEach((contentletId, styleProps) -> {
                if (!contentIds.contains(contentletId)) {
                    errors.add(new ContentletStylingErrorEntity(
                            "INVALID_CONTENTLET_REFERENCE",
                            "Could not define Style Properties for non-existing contentlet",
                            contentletId,
                            containerId,
                            containerUUID
                    ));
                }
            });

            if (!errors.isEmpty()) {
                throw new BadRequestException(null, new ResponseEntityView<>(errors),
                        "Invalid Style Properties configuration");
            }
        }
    }

    /**
     * Returns a list of saved Contentlets and Style Properites.
     * @param savedMultiTrees The list of saved MultiTrees.
     * @return A list of the saved Contentlets with the containerId, uuid, contentletId and
     * styleProperties.
     */
    private List<ContentView> buildSaveContentResponse(List<MultiTree> savedMultiTrees) {
        return savedMultiTrees.stream()
                .map(multiTree -> {
                    ContentView.Builder builder = ContentView.builder()
                            .containerId(multiTree.getContainer())
                            .contentletId(multiTree.getContentlet())
                            .uuid(multiTree.getRelationType());

                    // Include style properties in the response if Style Editor FF is enabled
                    final boolean isStyleEditorEnabled = Config.getBooleanProperty("FEATURE_FLAG_UVE_STYLE_EDITOR", false);
                    if (isStyleEditorEnabled) {
                        final Map<String, Object> styleProperties = multiTree.getStyleProperties();
                        builder.putAllStyleProperties(styleProperties != null ? styleProperties : new HashMap<>());
                    }

                    return builder.build();
                })
                .collect(Collectors.toList());
    }

    public void saveMultiTree(final String containerId,
                              final String contentletId,
                              final int order,
                              final String uid,
                              final Contentlet page) throws DotDataException {

        final MultiTree multiTree = new MultiTree().setContainer(containerId)
                .setContentlet(contentletId)
                .setRelationType(uid)
                .setTreeOrder(order)
                .setHtmlPage(page.getIdentifier());

        multiTreeAPI.saveMultiTreeAndReorder(multiTree);
    }

    /**
     * Do a copy page including the multi tree
     * @param page
     * @param user
     * @param pageMode
     * @param language
     * @return returns only the page contentlet
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @WrapInTransaction
    public Contentlet copyPage(final IHTMLPage page, final User user,
                               final PageMode pageMode, final Language language) throws DotDataException, DotSecurityException {

        if (page instanceof HTMLPageAsset) {

            final Contentlet newPage = this.contentletAPI.copyContentlet(
                    HTMLPageAsset.class.cast(page), user, pageMode.respectAnonPerms);

            Logger.debug(this, ()-> "New page from: " + page.getIdentifier() + " has been already created");

            final List<MultiTree> multiTrees = this.multiTreeAPI.getMultiTrees(page.getIdentifier());
            for (final MultiTree multiTree : multiTrees) {

                Logger.debug(this, ()-> "Making a copy of: " + multiTree.getContentlet());
                this.copyContentlet(new CopyContentletForm.Builder()
                                .pageId(page.getIdentifier())
                                .containerId(multiTree.getContainer())
                                .relationType(multiTree.getRelationType())
                                .contentId(multiTree.getContentlet())
                                .personalization(multiTree.getPersonalization())
                                .treeOrder(multiTree.getTreeOrder())
                                .variantId(multiTree.getVariantId())
                                .styleProperties(multiTree.getStyleProperties())
                                .build()
                        , user, pageMode, language);
            }

            return newPage;
        }

        throw new IllegalArgumentException("The page: " + page.getIdentifier() + " is not a valid page");
    }

    @WrapInTransaction
    public HTMLPageAsset saveTemplate(final User user, final HTMLPageAsset htmlPageAsset, final PageForm pageForm)

            throws BadRequestException, DotDataException, DotSecurityException {

        try {
            final Template templateSaved = this.saveTemplate(htmlPageAsset, user, pageForm);

            final String templateId = htmlPageAsset.getTemplateId();

            Contentlet contentlet = htmlPageAsset;

            if (!templateId.equals( templateSaved.getIdentifier() )) {
                contentlet = this.contentletAPI.checkout(htmlPageAsset.getInode(), user, false);
                contentlet.setStringProperty(HTMLPageAssetAPI.TEMPLATE_FIELD, templateSaved.getIdentifier());
                contentlet = this.contentletAPI.checkin(contentlet, user, false);
            }

            return contentlet instanceof  HTMLPageAsset ?
                    (HTMLPageAsset) contentlet :
                    this.htmlPageAssetAPI.fromContentlet(contentlet);
        } catch (BadRequestException | DotDataException | DotSecurityException e) {
            throw new DotRuntimeException(e);
        }
    }

    @NotNull
    public IHTMLPage getPage(final User user, final String pageId, final HttpServletRequest request)
            throws DotDataException, DotSecurityException {

        try {
            final PageMode mode = PageMode.get(request);
            final Language currentLanguage = WebAPILocator.getLanguageWebAPI().getLanguage(request);

            final RenderContext renderContext = WebAPILocator.getVariantWebAPI()
                    .getRenderContext(currentLanguage.getId(), pageId, mode, user);

            final ContentletVersionInfo contentletVersionInfo = APILocator.getVersionableAPI()
                    .getContentletVersionInfo(pageId, renderContext.getCurrentLanguageId(),
                            renderContext.getCurrentVariantKey())
                    .orElseThrow(() -> new HTMLPageAssetNotFoundException(pageId));

            final String pageInode = mode.showLive ? contentletVersionInfo.getLiveInode() :
                    contentletVersionInfo.getWorkingInode();

            IHTMLPage page = this.htmlPageAssetAPI.findPage(pageInode, user,
                            mode.respectAnonPerms);

            if (page == null) {
                throw new HTMLPageAssetNotFoundException(pageId);
            }

            final String currentVariantId = WebAPILocator.getVariantWebAPI().currentVariantId();

            if (!contentletVersionInfo.getVariant().equals(currentVariantId)) {
                page = createNewVersion(user, pageInode, currentVariantId);
            }

            return page;
        }catch (DotContentletStateException | ResourceNotFoundException e) {
            throw new HTMLPageAssetNotFoundException(pageId, e);
        }

    }

    private IHTMLPage createNewVersion(final User user, final String pageInode,
            final String currentVariantId) throws DotDataException, DotSecurityException {

        final Contentlet checkout = APILocator.getContentletAPI()
                .checkout(pageInode, user, false);
        checkout.setVariantId(currentVariantId);
        checkout.setIndexPolicy(IndexPolicy.FORCE);
        final Contentlet checkin = APILocator.getContentletAPI()
                .checkin(checkout, user, false);

        return APILocator.getHTMLPageAssetAPI().fromContentlet(checkin);
    }

    /**
     * Saves the HTML Page layout -- i.e., the Template -- and its associated Containers and
     * Contentlets.
     *
     * @param page     The {@link IHTMLPage} object whose layout will be saved.
     * @param user     The {@link User} performing this action.
     * @param pageForm The {@link PageForm} object containing the new Template's layout
     *                 information.
     *
     * @return The {@link Template} object that was saved.
     *
     * @throws DotDataException     An error occurred when persisting the changes.
     * @throws DotSecurityException The specified User does not have the required permission to
     *                              execute this action.
     */
    @WrapInTransaction
    public Template saveTemplate(final IHTMLPage page, final User user, final PageForm pageForm)
            throws DotDataException, DotSecurityException {

        final Host site = getSite(pageForm.getSiteId(), user);
        final User systemUser = userAPI.getSystemUser();
        final Template template = checkoutTemplate(page, systemUser, pageForm);

        try {
            final boolean hasPermission = template.isAnonymous() ?
                    permissionAPI.doesUserHavePermission(page, PermissionLevel.EDIT.getType(), user) :
                    permissionAPI.doesUserHavePermission(template, PermissionLevel.EDIT.getType(), user);

            if (!hasPermission) {
                throw new DotSecurityException(String.format("User '%s' doesn't have permission to edit Template " +
                                                                     "'%s'", user.getUserId(), page.getTemplateId()));
            }

            template.setDrawed(true);

            template.setModDate(new Date());
            // permissions have been updated above
            final Template oldTemplate = this.templateAPI.findWorkingTemplate(page.getTemplateId(), user, false);
            final TemplateLayout oldTemplateLayout = DotTemplateTool.getTemplateLayout(oldTemplate.getDrawedBody());

            final TemplateSaveParameters templateSaveParameters = new TemplateSaveParameters.Builder()
                    .setNewTemplate(template)
                    .setOldTemplateLayout(oldTemplateLayout)
                    .setPageIds(list(page.getIdentifier()))
                    .setNewLayout(pageForm.getLayout())
                    .setSite(site)
                    .build();

            return this.templateAPI.saveAndUpdateLayout(templateSaveParameters, APILocator.systemUser(), false);
        } catch (final DotDataException | DotSecurityException e) {
            final String errorMsg = String.format("An error occurred when saving Template '%s' [ %s ]: %s",
                    template.getTitle(), template.getIdentifier(), ExceptionUtil.getErrorMessage(e));
            throw new DotRuntimeException(errorMsg, e);
        }
    }

    public Template saveTemplate(final User user, final PageForm pageForm)
            throws BadRequestException, DotDataException, DotSecurityException {
        return this.saveTemplate(null, user, pageForm);
    }

    private Template checkoutTemplate(final IHTMLPage page, final User user, final PageForm form)
            throws DotDataException, DotSecurityException {

        final Template oldTemplate = this.templateAPI.findWorkingTemplate(page.getTemplateId(), user, false);

        final Template saveTemplate;

        final boolean isAnonymousTemplate = oldTemplate.isAnonymous();

        if (isAnonymousTemplate) {
            // Template is already a custom page layout, modify
            saveTemplate = oldTemplate;
        } else {
            saveTemplate = new Template();
            saveTemplate.setTitle(form.getTitle());
        }

        saveTemplate.setInode(null);
        saveTemplate.setTheme((form.getThemeId()==null) ? oldTemplate.getTheme() : form.getThemeId());
        saveTemplate.setDrawed(true);
        
        return saveTemplate;
    }

    /**
     * Returns the {@link Host} object for the specified Site Identifier.
     *
     * @param siteId The Identifier of the Site whose {@link Host} object will be returned.
     * @param user   The {@link User} performing this action.
     *
     * @return The {@link Host} object for the specified Site Identifier.
     */
    private Host getSite(final String siteId, final User user) {
        try {
            return UtilMethods.isSet(siteId) ? hostAPI.find(siteId, user, false) :
                        hostWebAPI.getCurrentHost(HttpServletRequestThreadLocal.INSTANCE.getRequest());
        } catch (final DotDataException | DotSecurityException | PortalException | SystemException e) {
            throw new DotRuntimeException(String.format("Could not find Site with ID '%s'", siteId), e);
        }
    }


    @WrapInTransaction
    protected Contentlet copyContentlet(final CopyContentletForm copyContentletForm, final User user,
                                      final PageMode pageMode, final Language language)
            throws DotDataException, DotSecurityException {

        final Tuple2<Contentlet, Contentlet> tuple2 = this.copyContent(copyContentletForm, user, pageMode, language.getId());

        final Contentlet copiedContentlet   = tuple2._1();
        final Contentlet originalContentlet = tuple2._2();
        final String htmlPage   = copyContentletForm.getPageId();
        String container        = copyContentletForm.getContainerId();
        final String contentId  = copyContentletForm.getContentId();
        final String instanceId = copyContentletForm.getRelationType();
        final String variant    = copyContentletForm.getVariantId();
        final int treeOrder     = copyContentletForm.getTreeOrder();
        final String personalization = copyContentletForm.getPersonalization();
        final Map<String, Object> styleProperties = copyContentletForm.getStyleProperties();

        if (FileAssetContainerUtil.getInstance().isFolderAssetContainerId(container)) {

            final Container containerObject = APILocator.getContainerAPI().getLiveContainerByFolderPath(container, user, pageMode.respectAnonPerms,
                    ()-> Try.of(()->APILocator.getHostAPI().findDefaultHost(user, pageMode.respectAnonPerms)).getOrNull());
            if (null != containerObject) {
                container =containerObject.getIdentifier();
            }
        }

        Logger.debug(this, ()-> "Deleting current contentlet multi tree: " + copyContentletForm);
        final MultiTree currentMultitree = getMultiTree(htmlPage, container, contentId, instanceId, personalization, variant);

        if (null == currentMultitree) {

            throw new DoesNotExistException(
                    "Can not copied the contentlet in the page, because the record is not part of the page, multitree: " + copyContentletForm);
        }

        APILocator.getMultiTreeAPI().deleteMultiTree(currentMultitree);

        final MultiTree newMultitree = new MultiTree(htmlPage, container,
                copiedContentlet.getIdentifier(), instanceId, treeOrder,
                null == personalization ? MultiTree.DOT_PERSONALIZATION_DEFAULT : personalization,
                null == variant ? VariantAPI.DEFAULT_VARIANT.name() : variant,
                styleProperties);
        Logger.debug(this, ()-> "Saving current contentlet multi tree: " + currentMultitree);
        APILocator.getMultiTreeAPI().saveMultiTree(newMultitree);

        if (null != originalContentlet) {
            HibernateUtil.addCommitListener(()->
                    new ContentletLoader().invalidate(originalContentlet, PageMode.EDIT_MODE));
        }


        return copiedContentlet;
    }

    private static MultiTree getMultiTree(String htmlPage, String container, String contentId, String instanceId, String personalization, String variant) throws DotDataException {
        MultiTree currentMultitree = APILocator.getMultiTreeAPI().getMultiTree(htmlPage, container, contentId, instanceId,
                null == personalization ? MultiTree.DOT_PERSONALIZATION_DEFAULT: personalization, null == variant ? VariantAPI.DEFAULT_VARIANT.name(): variant);

        if (null == currentMultitree &&
                (ContainerUUID.UUID_START_VALUE.equals(instanceId) ||
                        (ParseContainer.PARSE_CONTAINER_UUID_PREFIX + ContainerUUID.UUID_START_VALUE).equals(instanceId))) {
            currentMultitree = APILocator.getMultiTreeAPI().getMultiTree(htmlPage, container, contentId, ContainerUUID.UUID_LEGACY_VALUE,
                    null == personalization ? MultiTree.DOT_PERSONALIZATION_DEFAULT: personalization, null == variant ? VariantAPI.DEFAULT_VARIANT.name(): variant);
        }
        return currentMultitree;
    }

    /**
     * Takes the information related to an existing piece of Content -- which is already associated to a Container
     * inside an HTML Page -- and creates an exact copy of it. This is particularly useful when a User wants to:
     * <ul>
     *     <li>Update a Contentlet that is referenced in several pages, but only want to update the one in a specific
     *     page.</li>
     *     <li>Perform a deep copy of a page; i.e., create an exact cpy of an HTML Page and all its contents.</li>
     * </ul>
     *
     * @param copyContentletForm The {@link CopyContentletForm} object which contains all the Multi-Tree information of
     *                           the Contentlet being copied.
     * @param user               The {@link User} performing this action.
     * @param pageMode           The currently selected {@link PageMode}, which allows to get information such as the
     *                          "Respect Anonymous Permissions" flag.
     * @param languageId         The language ID in which the Contentlet copy will be created.
     *
     * @return The fresh copy of the specified Contentlet.
     *
     * @throws DotDataException     An error occurred when interacting with the data source.
     * @throws DotSecurityException The specified User does not have the required permissions to execute this action.
     */
    private Tuple2<Contentlet, Contentlet> copyContent(final CopyContentletForm copyContentletForm, final User user,
                                   final PageMode pageMode, final long languageId) throws DotDataException, DotSecurityException {
        Logger.debug(this, ()-> "Copying existing contentlet: " + copyContentletForm.getContentId());

        Contentlet currentContentlet = this.contentletAPI.findContentletByIdentifier(
                copyContentletForm.getContentId(), pageMode.showLive, languageId, user, pageMode.respectAnonPerms);
        if (null == currentContentlet || UtilMethods.isNotSet(currentContentlet.getIdentifier())) {
            Logger.debug(this, () -> String.format("Contentlet '%s' is not available in language ID '%s'.",
                    copyContentletForm.getContentId(), languageId));
            currentContentlet =
                    this.contentletAPI.findContentletByIdentifierAnyLanguage(copyContentletForm.getContentId(), false);
        }
        if (null == currentContentlet || UtilMethods.isNotSet(currentContentlet.getIdentifier())) {
            throw new DoesNotExistException(
                    "The Contentlet being copied does not exist. Content id: " + copyContentletForm.getContentId());
        }
        final Contentlet copiedContentlet  = this.contentletAPI.copyContentlet(currentContentlet, user, pageMode.respectAnonPerms);
        Logger.debug(this, ()-> "Contentlet: " + copiedContentlet.getIdentifier() + " has been copied");

        return Tuple.of(copiedContentlet, currentContentlet);
    }

    /**
     * Returns a list of ALL languages in dotCMS and, for each of them, adds a boolean indicating
     * whether the specified HTML Page Identifier is available in such a language or not. This is
     * particularly useful for the UI layer to be able to easily check what languages a page is
     * available on, and what languages it is not.
     *
     * @param pageId The Identifier of the HTML Page whose languages are being checked.
     * @param user   The {@link User} performing this action.
     *
     * @return The list of languages and the flag indicating whether the page is available in such a
     * language or not.
     *
     * @throws DotDataException An error occurred when interacting with the database.
     *
     * @deprecated This method is deprecated and will be removed in future versions.
     */
    @Deprecated(since = "Nov 7th, 24", forRemoval = true)
    public List<ExistingLanguagesForPageView> getExistingLanguagesForPage(final String pageId, final User user) throws DotDataException {
        DotPreconditions.checkNotNull(pageId, "Page ID cannot be null");
        DotPreconditions.checkNotNull(user, "User cannot be null");
        final ImmutableList.Builder<ExistingLanguagesForPageView> languagesForPage = new ImmutableList.Builder<>();
        final Set<Long> existingPageLanguages = APILocator.getVersionableAPI().findContentletVersionInfos(pageId).stream()
                .map(ContentletVersionInfo::getLang)
                .collect(Collectors.toSet());
        final List<Language> allLanguages = this.languageAPI.getLanguages();
        allLanguages.forEach(language -> languagesForPage.add(new ExistingLanguagesForPageView(language,
                existingPageLanguages.contains(language.getId()))));
        return languagesForPage.build();
    }

    /**
     * Verifies whether the incoming URI matches a Vanity URL or not.
     *
     * @param request    The current instance of the {@link HttpServletRequest}.
     * @param uri        The incoming URI.
     * @param languageId The language ID in which the URI is being requested.
     *
     * @return The Optional {@link CachedVanityUrl} object if the URI matches a Vanity URL.
     */
    public Optional<CachedVanityUrl> resolveVanityUrlIfPresent(final HttpServletRequest request,
                                                               final String uri,
                                                               final String languageId) {
        final Host site = this.hostWebAPI.getCurrentHostNoThrow(request);
        final Language language = UtilMethods.isSet(languageId) ?
                this.languageAPI.getLanguage(languageId) : this.languageAPI.getDefaultLanguage();
        final String correctedUri = !uri.startsWith(StringPool.SLASH) ? StringPool.SLASH + uri :
                uri;
        final Optional<CachedVanityUrl> vanityUrlOpt =
                this.vanityUrlAPI.resolveVanityUrl(correctedUri, site, language);
        if (vanityUrlOpt.isPresent()) {
            if (this.vanityUrlAPI.isSelfReferenced(vanityUrlOpt.get(), correctedUri)){
                return Optional.empty();
            }
            return vanityUrlOpt;
        }
        return Optional.empty();
    }

}
