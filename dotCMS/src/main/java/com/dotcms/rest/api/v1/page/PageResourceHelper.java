package com.dotcms.rest.api.v1.page;

import static com.dotcms.util.CollectionsUtils.list;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotNull;

import org.apache.velocity.exception.ResourceNotFoundException;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.exception.ExceptionUtil;
import com.dotcms.mock.request.CachedParameterDecorator;
import com.dotcms.mock.request.HttpServletRequestParameterDecoratorWrapper;
import com.dotcms.mock.request.LanguageIdParameterDecorator;
import com.dotcms.mock.request.ParameterDecorator;
import com.dotcms.rendering.velocity.directive.ParseContainer;
import com.dotcms.rendering.velocity.services.ContentletLoader;
import com.dotcms.rendering.velocity.viewtools.DotTemplateTool;
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
import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FileField;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.mock.request.FakeHttpRequest;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotmarketing.beans.Source;
import com.dotmarketing.business.Theme;
import com.dotmarketing.portlets.containers.business.FileAssetContainerUtil;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.containers.model.FileAssetContainer;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.cms.urlmap.URLMapInfo;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI;
import com.dotmarketing.portlets.htmlpageasset.business.render.ContainerRaw;
import com.dotmarketing.portlets.htmlpageasset.business.render.HTMLPageAssetNotFoundException;
import com.dotmarketing.portlets.htmlpageasset.business.render.HTMLPageAssetRenderedAPI;
import com.dotmarketing.portlets.htmlpageasset.business.render.HTMLPageAssetRenderedAPIImpl;
import com.dotmarketing.portlets.htmlpageasset.business.render.PageContextBuilder;
import com.dotmarketing.portlets.htmlpageasset.business.render.page.PageView;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.personas.business.PersonaAPI;
import com.dotmarketing.portlets.personas.model.Persona;
import com.dotmarketing.portlets.templates.business.TemplateAPI;
import com.dotmarketing.portlets.templates.business.TemplateSaveParameters;
import com.dotmarketing.portlets.templates.design.bean.ContainerUUID;
import com.dotmarketing.portlets.templates.design.bean.TemplateLayout;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.Constants;
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
     * @param variantName      The variant name
     * @return The list of saved Contentlets with the containerId, contentletId, uuid and styleProperties.
     * @throws DotDataException An error occurred when interacting with the data source.
     * @throws BadRequestException if style validation fails
     */
    @WrapInTransaction
    public List<ContentView> saveContent(final String pageId,
            final List<ContainerEntry> containerEntries,
            final Language language, String variantName, User user) throws DotDataException {

        final Map<String, List<MultiTree>> multiTreesMap = new HashMap<>();
        final List<ContentView> responseViews = new ArrayList<>();

        for (final ContainerEntry containerEntry : containerEntries) {
            int i = 0;
            final List<String> contentIds = containerEntry.getContentIds();
            final String personalization = UtilMethods.isSet(containerEntry.getPersonaTag()) ?
                    Persona.DOT_PERSONA_PREFIX_SCHEME + StringPool.COLON + containerEntry.getPersonaTag() :
                    MultiTree.DOT_PERSONALIZATION_DEFAULT;

            if (UtilMethods.isSet(contentIds)) {
                for (final String contentletId : contentIds) {
                    final MultiTree multiTree = new MultiTree()
                            .setContainer(castToOriginalContainerId(containerEntry.getContainerId(), user, false))
                            .setContentlet(contentletId)
                            .setInstanceId(containerEntry.getContainerUUID())
                            .setTreeOrder(i++)
                            .setHtmlPage(pageId)
                            .setVariantId(variantName);

                    CollectionsUtils.computeSubValueIfAbsent(
                            multiTreesMap, personalization,
                            MultiTree.personalized(multiTree, personalization),
                            CollectionsUtils::add,
                            (String key, MultiTree multitree) -> list(multitree));

                    // Invalidate contentlet cache for immediate visual updates
                    invalidateContentletCache(contentletId, variantName);

                    // Build response view for this contentlet
                    final ContentView responseView = ContentView.builder()
                            .containerId(containerEntry.getContainerId())
                            .uuid(containerEntry.getContainerUUID())
                            .contentletId(contentletId)
                            .build();

                    responseViews.add(responseView);
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

        return responseViews;
    }

    /**
     * Invalidates the cache for contentlets that had their styles updated. This ensures that the
     * rendered HTML reflects the style changes immediately.
     *
     * @param contentletId Contentlet identifier that was updated
     * @param variantId    The variant identifier for the contentlets
     */
    private void invalidateContentletCache(final String contentletId, final String variantId) {
        HibernateUtil.addCommitListenerNoThrow(new FlushCacheRunnable() {
            @Override
            public void run() {
                try {
                    // Try to get contentlet with current variant first
                    Contentlet contentlet =
                            contentletAPI.findContentletByIdentifierAnyLanguage(contentletId, variantId);

                    // Fallback to DEFAULT variant if not found in current variant
                    if (contentlet == null && !VariantAPI.DEFAULT_VARIANT.name().equals(variantId)) {
                        contentlet = contentletAPI.findContentletByIdentifierAnyLanguage(
                                contentletId, VariantAPI.DEFAULT_VARIANT.name());
                    }

                    // Invalidate cache for the contentlet in edit mode
                    if (contentlet != null) {
                        new ContentletLoader().invalidate(contentlet, PageMode.EDIT_MODE);
                        Logger.debug(PageResourceHelper.this,
                                String.format("Cache invalidated for contentlet: %s", contentletId));
                    } else {
                        Logger.warn(PageResourceHelper.this,
                                String.format("Contentlet not found for cache invalidation: %s", contentletId));
                    }
                } catch (final DotDataException e) {
                    Logger.warn(PageResourceHelper.this, String.format(
                            "Could not invalidate cache for contentlet '%s': %s", contentletId, e.getMessage()));
                }
            }
        });
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

        final String htmlPage   = copyContentletForm.getPageId();
        final String container  = castToOriginalContainerId(copyContentletForm.getContainerId(), user, pageMode.respectAnonPerms);
        final String contentId  = copyContentletForm.getContentId();
        final String instanceId = copyContentletForm.getRelationType();
        final String variant    = UtilMethods.isSet(copyContentletForm.getVariantId()) ? copyContentletForm.getVariantId() : VariantAPI.DEFAULT_VARIANT.name();
        final int treeOrder     = copyContentletForm.getTreeOrder();
        final String personalization = copyContentletForm.getPersonalization();
        final Map<String, Object> styleProperties = copyContentletForm.getStyleProperties();

        final MultiTree currentMultitree = getMultiTree(htmlPage, container, contentId, instanceId, personalization, variant);

        if (null == currentMultitree) {
            throw new DoesNotExistException(
                    "Can not copied the contentlet in the page, because the record is not part of the page, multitree: " + copyContentletForm);
        }

        final Tuple2<Contentlet, Contentlet> tuple2 = this.copyContent(copyContentletForm, user, pageMode, language.getId());
        final Contentlet copiedContentlet   = tuple2._1();
        final Contentlet originalContentlet = tuple2._2();

        Logger.debug(this, ()-> "Deleting current contentlet multi tree: " + copyContentletForm);
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
     * Converts a folder asset container ID to its actual container identifier.
     * If the container ID is a folder path (e.g., "/application/containers/mycontainer/"),
     * this method retrieves the actual container object and returns its identifier.
     * If it's already a regular container ID, it returns it unchanged.
     *
     * @param containerId The container ID (could be folder path or actual identifier)
     * @param user The user performing the action
     * @param respectAnonPerms Whether to respect anonymous permissions
     * @return The actual container identifier
     */
    private String castToOriginalContainerId(final String containerId, final User user, final boolean respectAnonPerms) {
        if (FileAssetContainerUtil.getInstance().isFolderAssetContainerId(containerId)) {
            try {
                final Container containerObject = APILocator.getContainerAPI().getLiveContainerByFolderPath(containerId, user, respectAnonPerms,
                        () -> Try.of(() -> APILocator.getHostAPI().findDefaultHost(user, respectAnonPerms)).getOrNull());
                if (null != containerObject) {
                    return containerObject.getIdentifier();
                }
            } catch (DotDataException | DotSecurityException e) {
                Logger.warn(this, String.format(
                        "Could not resolve folder asset container ID '%s': %s", containerId,
                        e.getMessage()));
            }
        }
        return containerId;
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

    /**
     * Updates style properties for contentlets within containers on a specific page.
     * This method only updates the styleProperties field in existing MultiTree entries
     * without modifying the page structure (containers, contentlets, order).
     *
     * @param pageId The identifier of the HTML Page whose contentlet styles are being updated.
     * @param containerEntries The list of container entries with contentlet style updates.
     *                        Must be already validated and reduced (deduplicated).
     * @param user The user performing the action
     * @return A list of ContentView objects representing the updated contentlets with their new styles.
     * @throws DotDataException If there's an error accessing or updating the database.
     */
    @WrapInTransaction
    public List<ContentView> saveContentletStyles(final String pageId,
            final List<ContainerEntry> containerEntries, final User user) throws DotDataException {

        // Fetch all existing MultiTree entries for this page from database
        final List<MultiTree> existingMultiTrees = multiTreeAPI.getMultiTrees(pageId);

        if (existingMultiTrees.isEmpty()) {
            String message = String.format(
                    "There is no Content in the Page: %s to associate the style entries", pageId);
            ContentletStylingErrorEntity.throwSingleError("CONTENT_NOT_FOUND", message, null);
        }

        // Create a lookup map for O(1) access
        // Key = unique identifier (container|uuid|contentlet|personalization|variant)
        // Value = MultiTree object
        final Map<String, MultiTree> multiTreeLookup = existingMultiTrees.stream()
                .collect(Collectors.toMap(
                    mt -> MultiTree.buildMultiTreeKey(
                        mt.getRelationType(),
                        mt.getPersonalization(),
                        mt.getContainerAsID(),
                        mt.getContentlet(),
                        mt.getVariantId()
                    ),
                    mt -> mt,
                    // In case of duplicate keys (shouldn't happen), keep the first one
                    (existing, duplicate) -> existing
                ));

        // Get the current variant from the request context (fallback to DEFAULT if unavailable)
        final String currentVariantId = Try.of(
                        () -> WebAPILocator.getVariantWebAPI().currentVariantId())
                .getOrElse(VariantAPI.DEFAULT_VARIANT.name());

        final List<MultiTree> multiTreesToUpdate = new ArrayList<>();
        final List<ContentView> responseViews = new ArrayList<>();

        // Process each container entry from the request
        for (final ContainerEntry containerEntry : containerEntries) {

            final String containerId = containerEntry.getContainerId();
            final String containerUuid = containerEntry.getContainerUUID();
            final String personalization = UtilMethods.isSet(containerEntry.getPersonaTag())
                    ? Persona.DOT_PERSONA_PREFIX_SCHEME + StringPool.COLON + containerEntry.getPersonaTag()
                    : MultiTree.DOT_PERSONALIZATION_DEFAULT;
            final Map<String, Map<String, Object>> contentletStylesMap = containerEntry.getStylePropertiesMap();

            // For each contentlet in this container, update its styles
            for (final String contentletId : containerEntry.getContentIds()) {
                this.updateContentletStyles(pageId, contentletId, containerId, containerUuid,
                        personalization, currentVariantId, multiTreeLookup, contentletStylesMap,
                        multiTreesToUpdate, responseViews, user);
            }
        }

        // Use updateStyleProperties instead of saveMultiTrees to preserve treeOrder.
        multiTreeAPI.updateStyleProperties(multiTreesToUpdate);
        Logger.info(this, String.format(
                "Successfully updated styles for %d contentlets on page %s (variant: %s)",
                multiTreesToUpdate.size(), pageId, currentVariantId));

        return responseViews;
    }

    /**
     * Updates the style properties for a contentlet in a specific container on a specific page.
     *
     * @param pageId The identifier of the HTML Page whose contentlet styles are being updated.
     * @param contentletId The identifier of the contentlet whose styles are being updated.
     * @param containerId The identifier of the container in which the contentlet is located.
     * @param containerUuid The UUID of the container.
     * @param personalization The personalization tag (persona) of the contentlet.
     * @param currentVariantId The current variant identifier.
     * @param multiTreeLookup The lookup map for the MultiTree entries.
     * @param contentletStylesMap The map of contentlet styles.
     * @param multiTreesToUpdate The list of MultiTree entries to update.
     * @param responseViews The list of ContentView objects representing the updated contentlets with their new styles.
     * @param user The user performing the action
     */
    private void updateContentletStyles(final String pageId, final String contentletId, final String containerId,
            final String containerUuid, final String personalization, final String currentVariantId,
            final Map<String, MultiTree> multiTreeLookup,
            final Map<String, Map<String, Object>> contentletStylesMap,
            final List<MultiTree> multiTreesToUpdate, final List<ContentView> responseViews, final User user) {

        // Find the existing MultiTree entry searching in the multiTreeLookup map
        final MultiTree existingMultiTree = multiTreeLookup.get(
                buildMultiTreeLookupKey(containerId, containerUuid, contentletId, personalization, currentVariantId, user));

        if (existingMultiTree == null) {
            String message = String.format(
                    "Contentlet: %s not found for page=%s, container=%s, uuid=%s, personalization=%s, variant=%s.",
                    contentletId, pageId, containerId, containerUuid, personalization,
                    currentVariantId);
            ContentletStylingErrorEntity.throwSingleError("CONTENT_NOT_FOUND", message,
                    null, contentletId, containerId, containerUuid);
        }

        // Get the new style properties for this contentlet (could be empty to clear styles)
        final Map<String, Object> newStyleProperties = contentletStylesMap.get(contentletId);

        // Update the MultiTree with new style properties
        existingMultiTree.setStyleProperties(newStyleProperties);
        multiTreesToUpdate.add(existingMultiTree);

        // Invalidate contentlet cache for immediate visual updates
        invalidateContentletCache(contentletId, currentVariantId);

        // Build response view for this contentlet
        final ContentView responseView = ContentView.builder()
                .containerId(containerId)
                .uuid(containerUuid)
                .contentletId(contentletId)
                .styleProperties(newStyleProperties)
                .build();

        responseViews.add(responseView);
    }

    /**
     * Builds a unique lookup key for a MultiTree entry.
     * This key includes all fields necessary to uniquely identify a MultiTree record,
     * including personalization and variant to support personas and A/B testing.
     *
     * @param container       The container identifier
     * @param instanceId      The container instance UUID
     * @param contentlet      The contentlet identifier
     * @param personalization The personalization tag (persona)
     * @param variantId       The variant identifier
     * @param user            The user performing the action
     * @return A unique string key combining all parameters
     */
    private String buildMultiTreeLookupKey(final String container, final String instanceId,
            final String contentlet, final String personalization, final String variantId, final User user) {

        // Resolve folder asset container IDs to actual identifiers for consistent lookups
        final String originalContainerID = castToOriginalContainerId(container, user, false);

        return MultiTree.buildMultiTreeKey(
                instanceId,
                personalization != null ? personalization : MultiTree.DOT_PERSONALIZATION_DEFAULT,
                originalContainerID,
                contentlet,
                variantId != null ? variantId : VariantAPI.DEFAULT_VARIANT.name());
    }

    /**
     * Returns the parsed {@code DOT_STYLE_EDITOR_SCHEMA} entries for every distinct content type
     * present on the given page. Contentlets are loaded from the page's multi-tree relationships,
     * deduplicated by content type variable, and then filtered to those whose content type carries
     * a {@code DOT_STYLE_EDITOR_SCHEMA} metadata entry.
     * <p>
     * Returns an empty list when the page has no contentlets or none of the content types define
     * a style editor schema.
     *
     * @param pageId Identifier of the HTML Page to inspect.
     * @return Parsed schema nodes, one per distinct content type that has a schema.
     * @throws DotDataException If a database error occurs while retrieving the multi-tree data.
     */
    public List<JsonNode> getStyleEditorSchemasInPage(final String pageId) throws DotDataException {
        final List<MultiTree> multiTrees = APILocator.getMultiTreeAPI().getMultiTreesByPage(pageId);

        if (multiTrees == null || multiTrees.isEmpty()) {
            return Collections.emptyList();
        }

        // gets the contentlets present in the page without duplicates
        final List<Contentlet> contentlets = multiTrees.stream()
                .map(MultiTree::getContentlet)
                .filter(UtilMethods::isSet)
                .distinct()
                .map(id -> Try.of(() -> APILocator.getContentletAPI()
                                .findContentletByIdentifierAnyLanguageAnyVariant(id))
                        .onFailure(e -> Logger.warn(this, "Could not load contentlet '" + id
                                + "' for page '" + pageId + "': " + e.getMessage()))
                        .getOrNull())
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return getStyleEditorSchemas(contentlets);
    }

    /**
     * Extracts and parses the {@code DOT_STYLE_EDITOR_SCHEMA} metadata entry from each distinct
     * content type found in the given contentlet list. Content types are deduplicated by variable
     * name, and those without a {@code DOT_STYLE_EDITOR_SCHEMA} entry are excluded from the result.
     * Individual parse failures are logged as warnings and skipped.
     * <p>
     * This method is shared by {@link PageResource} (REST response) and
     * {@link com.dotmarketing.portlets.htmlpageasset.business.render.page.HTMLPageAssetRenderedBuilder}
     * (UVE script injection).
     *
     * @param contentlets Contentlets whose content types will be inspected for style editor schemas.
     * @return Parsed schema nodes, one per distinct content type that defines a schema; never {@code null}, may be empty.
     */
    public static List<JsonNode> getStyleEditorSchemas(final List<Contentlet> contentlets) {
        final ObjectMapper mapper = DotObjectMapperProvider.getInstance().getDefaultObjectMapper();
        return contentlets.stream()
                .map(contentlet -> Try.of(contentlet::getContentType).getOrNull())
                .filter(contentType -> contentType != null && UtilMethods.isSet(contentType.variable()))
                .collect(Collectors.toMap(ContentType::variable, ct -> ct,
                        (existing, replacement) -> existing))
                .values().stream()
                .map(ct -> Optional.ofNullable(ct.metadata())
                        .map(meta -> {
                            final String schemaStr = (String) meta.get("DOT_STYLE_EDITOR_SCHEMA");
                            if (!UtilMethods.isSet(schemaStr)) {
                                return null;
                            }
                            return Try.of(() -> mapper.readTree(schemaStr))
                                    .onFailure(e -> Logger.warn(PageResourceHelper.class,
                                            "Could not parse DOT_STYLE_EDITOR_SCHEMA for content type '"
                                                    + ct.variable() + "': " + e.getMessage()))
                                    .getOrNull();
                        })
                        .orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    // -----------------------------------------------------------------------
    // _render-sources support
    // -----------------------------------------------------------------------

    /**
     * Builds a {@link PageRenderSourcesView} for the given page path. All parameters except
     * {@code path} are optional; see individual parameter docs for resolution rules.
     *
     * @param path        Required. Qualified ({@code //host/uri}) or plain ({@code /uri}) page path.
     * @param hostId      Optional. Explicit host identifier (ignored when {@code path} is qualified).
     * @param languageId  Optional. Language identifier; defaults to the default language.
     * @param personaId   Optional. Persona contentlet identifier whose key-tag is used for
     *                    personalization lookup (maps to WebKeys.CMS_PERSONA_PARAMETER semantics).
     * @param variantName Optional. Variant name; defaults to {@link VariantAPI#DEFAULT_VARIANT}.
     * @param pageMode    Optional string that maps to a {@link PageMode}; defaults to PREVIEW_MODE.
     * @param user        The user performing the request.
     * @param renderedAPI The {@link HTMLPageAssetRenderedAPI} used to obtain the {@link PageView}.
     * @return A fully-populated {@link PageRenderSourcesView}.
     * @throws DotDataException     Data access error.
     * @throws DotSecurityException User does not have READ permission on the page.
     */
    public PageRenderSourcesView getRenderSources(
            final String path,
            final String hostId,
            final String languageId,
            final String personaId,
            final String variantName,
            final String pageMode,
            final User user,
            final HTMLPageAssetRenderedAPI renderedAPI)
            throws DotDataException, DotSecurityException {

        // ---- 1. Resolve host -----------------------------------------------
        final Host host = resolveHostForRenderSources(path, hostId, user);

        // ---- 2. Resolve language -------------------------------------------
        final Language language = resolveLanguageForRenderSources(languageId);

        // ---- 3. Resolve URI ------------------------------------------------
        final String uri = UtilMethods.isSet(path) ? path : "/";

        // ---- 4. Resolve PageMode -------------------------------------------
        final PageMode mode = UtilMethods.isSet(pageMode)
                ? PageMode.get(pageMode)
                : PageMode.PREVIEW_MODE;

        // ---- 5. Build mock request — needed for page resolution -------------
        final javax.servlet.http.HttpServletRequest mockReq =
                buildSourceLookupRequest(uri, host, language);

        // ---- 6. Resolve page via renderedAPI.getHtmlPageAsset ---------------
        // This mirrors the render endpoint exactly: it handles both direct page paths
        // AND URL-mapped paths (e.g. /blog/post/{urlTitle} → detail page) through
        // the same HTMLPageAssetRenderedAPIImpl.findByURLMap code path.
        // Calling processURLMap directly from the helper would not work because
        // findByURLMap relies on request attributes (host, language) set on the
        // mock request; routing through getHtmlPageAsset ensures the same resolution
        // logic is applied with the properly initialised request context.
        final HTMLPageAssetRenderedAPIImpl.HTMLPageUrl htmlPageUrl = renderedAPI.getHtmlPageAsset(
                PageContextBuilder.builder()
                        .setUser(user)
                        .setPageUri(uri)
                        .setPageMode(mode)
                        .build(),
                mockReq);

        final IHTMLPage page = htmlPageUrl.getHTMLPage();
        final URLMapInfo urlMapInfo = htmlPageUrl.getUrlMapInfo();

        // ---- 7. Check READ permission -------------------------------------
        final boolean canRead = permissionAPI.doesUserHavePermission(
                page, PermissionAPI.PERMISSION_READ, user, mode.respectAnonPerms);
        if (!canRead) {
            throw new DotSecurityException(
                    "User " + user.getUserId() + " does not have READ permission on page: " + uri);
        }

        // ---- 8. Obtain PageView via getPageMetadata -----------------------
        // Use the detail page URI for rendering (for URL-mapped pages this is the
        // configured detail page URI, not the original mapped URI).
        final String pageUri = page.getURI();
        final javax.servlet.http.HttpServletRequest metaReq =
                buildSourceLookupRequest(pageUri, host, language);
        final javax.servlet.http.HttpServletResponse mockResp = new MockHttpResponse();

        final PageView pageView = renderedAPI.getPageMetadata(
                PageContextBuilder.builder()
                        .setUser(user)
                        .setPageUri(pageUri)
                        .setPageMode(mode)
                        .setParseJSON(true)
                        .build(),
                metaReq, mockResp);

        // ---- 9. Resolve personalization key --------------------------------
        final String personalization = resolvePersonalization(personaId, user);

        // ---- 10. Resolve variant -------------------------------------------
        final String resolvedVariant = UtilMethods.isSet(variantName)
                ? variantName
                : VariantAPI.DEFAULT_VARIANT.name();

        // ---- 11. Build onPage lookup (containerId+contentTypeVar → true) ---
        // Fetch the page's placed-content tree once and share it with the widget pass below.
        // A failure here would silently empty both the container content-type filter and the
        // widget list, so it is logged at error level (with the page id) rather than swallowed —
        // an empty tree is otherwise indistinguishable from a genuinely empty page.
        final List<MultiTree> pageTrees = Try.of(() ->
                        multiTreeAPI.getMultiTreesByPersonalizedPage(
                                page.getIdentifier(), personalization, resolvedVariant))
                .onFailure(e -> Logger.error(this,
                        "Could not load MultiTrees for page '" + page.getIdentifier()
                                + "'; container content-types and widgets will be empty", e))
                .getOrElse(Collections.emptyList());
        final Set<String> onPageKeys = buildOnPageKeys(pageTrees);

        // ---- 12. Theme -----------------------------------------------------
        final Template template = templateAPI.findWorkingTemplate(
                page.getTemplateId(), user, false);
        final ThemeSourceView themeView = buildThemeView(template, host, user);

        // ---- 13. Containers ------------------------------------------------
        final Collection<? extends ContainerRaw> containerRaws = pageView.getContainers();
        final Map<String, ContainerSourceView> containerViews =
                buildContainerViews(containerRaws, onPageKeys, host);

        // ---- 14. Widgets ---------------------------------------------------
        final List<WidgetSourceView> widgetViews =
                buildWidgetViews(pageTrees, resolvedVariant, language.getId(),
                        mode.showLive, host, user);

        // ---- 15. URL content map view (only for URL-mapped pages) ----------
        final UrlContentMapView urlContentMapView = urlMapInfo != null
                ? buildUrlContentMapView(urlMapInfo, language.getId(), resolvedVariant,
                        mode.showLive, user)
                : null;

        // ---- 16. Assemble --------------------------------------------------
        // page.uri = the originally requested URI (host-qualified), not the detail page URI.
        // When resolved via URL map, page.identifier is the detail page identifier.
        final String responseUri = urlMapInfo != null
                ? buildHostQualifiedPath(uri, host)
                : buildHostQualifiedPath(page.getURI(), host);
        final PageSourceRefView pageRef = new PageSourceRefView(
                page.getIdentifier(),
                responseUri,
                language.getId());

        return new PageRenderSourcesView(pageRef, themeView, containerViews, widgetViews,
                urlContentMapView);
    }

    // -----------------------------------------------------------------------
    // private helpers
    // -----------------------------------------------------------------------

    private Host resolveHostForRenderSources(final String path, final String hostId,
            final User user) throws DotDataException, DotSecurityException {
        // Host resolution: host_id query param → default host.
        // The //hostname/path form is not supported — dotCMS's NormalizationFilter
        // rejects any URI containing "//" before it reaches this code.
        if (UtilMethods.isSet(hostId)) {
            final Host h = hostAPI.find(hostId, user, false);
            if (h == null || !UtilMethods.isSet(h.getIdentifier())) {
                throw new com.dotmarketing.exception.DoesNotExistException(
                        "Host not found for id: " + hostId);
            }
            return h;
        }
        return hostAPI.findDefaultHost(user, false);
    }

    private Language resolveLanguageForRenderSources(final String languageId) {
        if (UtilMethods.isSet(languageId)) {
            final Language lang = languageAPI.getLanguage(languageId);
            if (lang != null && lang.getId() > 0) {
                return lang;
            }
        }
        return languageAPI.getDefaultLanguage();
    }

    /**
     * Builds a mock request used to drive page resolution and {@code getPageMetadata} for the
     * render-sources lookup. Host resolution inside {@code renderedAPI} goes through
     * {@code HostWebAPIImpl.getCurrentHostFromRequest}, which checks, in priority order:
     * <ol>
     *   <li>{@code getParameter("host_id")} when {@code user.isBackendUser()} → most reliable</li>
     *   <li>{@code getParameter/getAttribute(Host.HOST_VELOCITY_VAR_NAME)}</li>
     *   <li>{@code getAttribute(WebKeys.CURRENT_HOST)}</li>
     *   <li>{@code resolveHostName(request.getServerName())} → fragile fallback</li>
     * </ol>
     * {@link FakeHttpRequest} parses query params from the URI string, so embedding
     * {@code "?host_id=<identifier>"} guarantees option 1 fires for backend users and prevents the
     * fragile serverName fallback (option 4) from choosing the wrong site when processing
     * URL-mapped pages. Options 2 and 3 are set as belt-and-suspenders for non-backend (frontend)
     * users who skip the host_id branch.
     */
    private javax.servlet.http.HttpServletRequest buildSourceLookupRequest(
            final String uri, final Host host, final Language language) {
        final String uriWithHostParam = uri + "?host_id=" + host.getIdentifier();
        final javax.servlet.http.HttpServletRequest request =
                new MockHeaderRequest(
                        new MockSessionRequest(
                                new MockAttributeRequest(
                                        new FakeHttpRequest(host.getHostname(),
                                                uriWithHostParam).request())));
        request.setAttribute(com.dotmarketing.util.WebKeys.CURRENT_HOST, host);
        request.setAttribute(Host.HOST_VELOCITY_VAR_NAME, host.getIdentifier());
        request.setAttribute(com.dotmarketing.util.WebKeys.HTMLPAGE_LANGUAGE,
                String.valueOf(language.getId()));
        return request;
    }

    private String resolvePersonalization(final String personaId, final User user) {
        if (!UtilMethods.isSet(personaId)) {
            return MultiTree.DOT_PERSONALIZATION_DEFAULT;
        }
        try {
            final Contentlet personaContentlet =
                    contentletAPI.findContentletByIdentifierAnyLanguage(personaId);
            if (personaContentlet != null) {
                final Persona persona = APILocator.getPersonaAPI()
                        .fromContentlet(personaContentlet);
                if (persona != null && UtilMethods.isSet(persona.getKeyTag())) {
                    return Persona.DOT_PERSONA_PREFIX_SCHEME + StringPool.COLON
                            + persona.getKeyTag();
                }
            }
        } catch (final DotDataException | DotSecurityException e) {
            Logger.warn(this, "Could not resolve persona for id '" + personaId + "': "
                    + e.getMessage());
        }
        return MultiTree.DOT_PERSONALIZATION_DEFAULT;
    }

    /**
     * Builds a set of {@code "containerId|contentTypeVar"} keys for the contentlets placed on the
     * page, from the page's pre-fetched personalized {@link MultiTree} list.
     */
    private Set<String> buildOnPageKeys(final List<MultiTree> trees) {
        final Set<String> keys = new HashSet<>();
        final FileAssetContainerUtil fileContainerUtil = FileAssetContainerUtil.getInstance();
        for (final MultiTree mt : trees) {
            final String contentletId = mt.getContentlet();
            if (!UtilMethods.isSet(contentletId)) {
                continue;
            }
            final Contentlet c = Try.of(
                    () -> contentletAPI.findContentletByIdentifierAnyLanguage(contentletId))
                    .getOrNull();
            if (c == null) {
                continue;
            }
            final ContentType ct = Try.of(c::getContentType).getOrNull();
            if (ct == null) {
                continue;
            }
            // Normalize containerId: FILE containers store a path in MultiTree
            final String rawContainer = mt.getContainer();
            final String containerId =
                    fileContainerUtil.isFolderAssetContainerId(rawContainer)
                            ? Try.of(() -> fileContainerUtil.getContainerIdFromPath(rawContainer))
                                    .getOrElse(rawContainer)
                            : rawContainer;
            keys.add(containerId + "|" + ct.variable());
        }
        return keys;
    }

    private ThemeSourceView buildThemeView(final Template template, final Host host,
            final User user) {
        if (template == null || !UtilMethods.isSet(template.getTheme())) {
            return null;
        }
        try {
            final Theme theme = APILocator.getThemeAPI()
                    .findThemeById(template.getTheme(), user, false);
            if (theme == null || !UtilMethods.isSet(theme.getIdentifier())) {
                return null;
            }
            final String rawPath = theme.getPath();
            final String folderPath = buildHostQualifiedPath(rawPath, host);

            // Themes organise VTLs into sub-folders (e.g. navigation/, header/), so walk the
            // theme folder recursively rather than listing only the root level.
            final List<Folder> folders = new ArrayList<>();
            folders.add(theme);
            folders.addAll(APILocator.getFolderAPI()
                    .findSubFoldersRecursively(theme, user, false));

            final List<VtlFileRefView> vtls = folders.stream()
                    .flatMap(folder -> Try.of(() -> APILocator.getFileAssetAPI()
                                    .findFileAssetsByFolder(folder, null, false, user, false))
                            .getOrElse(Collections.emptyList()).stream())
                    .filter(f -> UtilMethods.isSet(f.getFileName())
                            && f.getFileName().endsWith(Constants.VELOCITY_FILE_EXTENSION))
                    .map(f -> new VtlFileRefView(
                            buildHostQualifiedPath(f.getPath() + f.getFileName(), host),
                            f.getIdentifier()))
                    .collect(Collectors.toList());

            return new ThemeSourceView(theme.getIdentifier(), theme.getName(), folderPath, vtls);
        } catch (final Exception e) {
            Logger.warn(this, "Could not build theme view: " + e.getMessage());
            return null;
        }
    }

    /**
     * Returns a {@link LinkedHashMap} keyed by the container reference (UUID for DB containers,
     * host-qualified path for FILE containers).  Each value is a {@link ContainerSourceView}
     * whose {@code contentTypes} list contains only the types that are actually placed on the page
     * under the resolved persona and variant — types that are allowed but not placed are omitted.
     * Containers themselves always appear even if their filtered list is empty.
     */
    private Map<String, ContainerSourceView> buildContainerViews(
            final Collection<? extends ContainerRaw> containerRaws,
            final Set<String> onPageKeys,
            final Host host) {
        if (containerRaws == null) {
            return Collections.emptyMap();
        }
        final Map<String, ContainerSourceView> result = new LinkedHashMap<>();
        for (final ContainerRaw cr : containerRaws) {
            final Container c = cr.getContainer();
            if (c instanceof FileAssetContainer) {
                final FileAssetContainer fac = (FileAssetContainer) c;
                final String key = FileAssetContainerUtil.getInstance().getFullPath(fac);
                result.put(key, buildFileContainerView(fac, cr, onPageKeys, host));
            } else {
                result.put(c.getIdentifier(), buildDbContainerView(c, cr, onPageKeys));
            }
        }
        return result;
    }

    /**
     * Builds a DB container value: only content types that appear in {@code onPageKeys} are
     * included.
     */
    private ContainerSourceView buildDbContainerView(final Container c, final ContainerRaw cr,
            final Set<String> onPageKeys) {
        final String containerId = c.getIdentifier();
        final List<ContentTypeEntryView> contentTypes = cr.getContainerStructures().stream()
                .map(cs -> {
                    final String var = cs.getContentTypeVar();
                    if (!UtilMethods.isSet(var)) {
                        return null;
                    }
                    // Include only if placed on page
                    if (!onPageKeys.contains(containerId + "|" + var)) {
                        return null;
                    }
                    return new ContentTypeEntryView(var);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return new ContainerSourceView(Source.DB.name(), contentTypes);
    }

    /**
     * Builds a FILE container value: only content types that appear in {@code onPageKeys} are
     * included.
     *
     * <p>The canonical content type variable name comes from {@link ContainerRaw#getContainerStructures()}
     * (which resolves the real {@code velocity_var_name} from the DB, regardless of VTL filename
     * casing). The path and identifier of the backing VTL file are resolved by matching each
     * content type variable case-insensitively against the file assets returned by
     * {@link FileAssetContainer#getContainerStructuresAssets()}.</p>
     */
    private ContainerSourceView buildFileContainerView(final FileAssetContainer fac,
            final ContainerRaw cr, final Set<String> onPageKeys, final Host host) {
        final String containerId = fac.getIdentifier();

        // Build a case-insensitive lookup: lowercase(contentTypeVar) → FileAsset
        // The VTL filenames may differ in case from the content type's velocity_var_name
        // (e.g. "activity.vtl" vs ContentType variable "Activity").
        final Map<String, FileAsset> vtlByVarLower = new HashMap<>();
        for (final FileAsset fa : fac.getContainerStructuresAssets()) {
            final String fileName = fa.getFileName();
            if (UtilMethods.isSet(fileName)
                    && fileName.endsWith(Constants.VELOCITY_FILE_EXTENSION)) {
                // strip the .vtl extension → raw name from the file, then lowercase for lookup
                final String rawVar = fileName.substring(0,
                        fileName.length() - Constants.VELOCITY_FILE_EXTENSION.length());
                vtlByVarLower.put(rawVar.toLowerCase(), fa);
            }
        }

        // Iterate ContainerStructures to get canonical (properly-cased) variable names,
        // then filter to only those placed on the page.
        final List<ContentTypeEntryView> contentTypes = cr.getContainerStructures().stream()
                .map(cs -> {
                    final String contentTypeVar = cs.getContentTypeVar();
                    if (!UtilMethods.isSet(contentTypeVar)) {
                        return null;
                    }
                    // Include only if placed on page (key uses canonical variable name)
                    if (!onPageKeys.contains(containerId + "|" + contentTypeVar)) {
                        return null;
                    }
                    // Look up the backing VTL file by case-insensitive var name
                    final FileAsset fa = vtlByVarLower.get(contentTypeVar.toLowerCase());
                    if (fa == null) {
                        // No dedicated VTL file — include the entry without path/identifier
                        return new ContentTypeEntryView(contentTypeVar);
                    }
                    final String vtlPath = buildHostQualifiedPath(
                            fa.getPath() + fa.getFileName(), host);
                    return new ContentTypeEntryView(contentTypeVar, vtlPath, fa.getIdentifier());
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return new ContainerSourceView(Source.FILE.name(), contentTypes);
    }

    /**
     * Builds widget view entries for every Widget contentlet placed on the page under the given
     * personalization and variant.
     *
     * <p>Each contentlet is resolved version-aware: the overload
     * {@code findContentletByIdentifier(id, live, languageId, variantId, user, false)} is tried
     * first so that {@code contentletInode} reflects the exact language/variant version requested.
     * When that version does not exist (e.g. the widget was not translated into the requested
     * language) the call falls back to {@code findContentletByIdentifierAnyLanguage} so the widget
     * still appears in the response.</p>
     */
    private List<WidgetSourceView> buildWidgetViews(final List<MultiTree> trees,
            final String variant, final long languageId, final boolean live,
            final Host host, final User user) {
        final List<WidgetSourceView> result = new ArrayList<>();
        final Set<String> seenContentlets = new HashSet<>();
        for (final MultiTree mt : trees) {
            final String contentletId = mt.getContentlet();
            if (!UtilMethods.isSet(contentletId) || !seenContentlets.add(contentletId)) {
                continue;
            }

            // Attempt version-aware resolution first (language + variant specific inode).
            Contentlet c = null;
            try {
                c = contentletAPI.findContentletByIdentifier(
                        contentletId, live, languageId, variant, user, false);
            } catch (final Exception e) {
                Logger.debug(this, () -> "Version-aware lookup failed for widget contentlet '"
                        + contentletId + "', falling back to any-language: " + e.getMessage());
            }
            // Fall back to any-language/any-variant if the specific version is absent.
            if (c == null || !UtilMethods.isSet(c.getInode())) {
                c = Try.of(() -> contentletAPI
                        .findContentletByIdentifierAnyLanguage(contentletId)).getOrNull();
            }
            if (c == null) {
                continue;
            }

            final ContentType ct = Try.of(c::getContentType).getOrNull();
            if (ct == null || ct.baseType() != BaseContentType.WIDGET) {
                continue;
            }

            final String widgetContentletId   = c.getIdentifier();
            final String widgetContentletInode = c.getInode();
            final Optional<VtlFileRefView> fileRef = resolveWidgetFileRef(c, ct, host, user);

            if (fileRef.isPresent()) {
                // FILE-backed widget: path and VTL file identifier are known
                result.add(new WidgetSourceView(ct.variable(), c.getTitle(),
                        widgetContentletId, widgetContentletInode,
                        fileRef.get().getPath(), fileRef.get().getIdentifier()));
            } else {
                // CODE widget: Velocity lives in widgetCode / contentlet fields
                result.add(new WidgetSourceView(ct.variable(), c.getTitle(),
                        widgetContentletId, widgetContentletInode));
            }
        }
        return result;
    }

    /**
     * Returns a {@link VtlFileRefView} (path + identifier) for the file backing a FILE-type widget,
     * or {@link Optional#empty()} if the contentlet has no file-typed field resolving to a file
     * asset.
     *
     * <p>A widget content type may declare more than one file field (e.g. a banner image alongside
     * the Velocity template), so the field order is not a reliable signal. This prefers the first
     * field whose file asset is a {@code .vtl}; only when no field resolves to a {@code .vtl} does
     * it fall back to the first file asset found.</p>
     */
    private Optional<VtlFileRefView> resolveWidgetFileRef(final Contentlet contentlet,
            final ContentType ct, final Host host, final User user) {
        // Try FileField first, then BinaryField
        final List<Field> fileFields = new ArrayList<>(ct.fields(FileField.class));
        fileFields.addAll(ct.fields(BinaryField.class));

        FileAsset firstAsset = null;
        for (final Field field : fileFields) {
            try {
                final Object val = contentlet.get(field.variable());
                if (val == null) {
                    continue;
                }
                // FileField stores a file identifier; BinaryField stores the actual file
                final String fileIdentifier = val.toString();
                if (!UtilMethods.isSet(fileIdentifier)) {
                    continue;
                }
                final Contentlet fileCon = Try.of(
                        () -> contentletAPI.findContentletByIdentifierAnyLanguage(fileIdentifier))
                        .getOrNull();
                if (fileCon != null && APILocator.getFileAssetAPI().isFileAsset(fileCon)) {
                    final FileAsset fa = APILocator.getFileAssetAPI().fromContentlet(fileCon);
                    if (UtilMethods.isSet(fa.getFileName())
                            && fa.getFileName().endsWith(Constants.VELOCITY_FILE_EXTENSION)) {
                        return Optional.of(toVtlFileRef(fa, host));
                    }
                    if (firstAsset == null) {
                        firstAsset = fa;
                    }
                }
            } catch (final Exception e) {
                Logger.debug(this, "Could not resolve file field '" + field.variable()
                        + "' on contentlet '" + contentlet.getIdentifier() + "': "
                        + e.getMessage());
            }
        }
        return Optional.ofNullable(firstAsset).map(fa -> toVtlFileRef(fa, host));
    }

    private VtlFileRefView toVtlFileRef(final FileAsset fa, final Host host) {
        return new VtlFileRefView(
                buildHostQualifiedPath(fa.getPath() + fa.getFileName(), host),
                fa.getIdentifier());
    }

    /**
     * Builds a {@link UrlContentMapView} for the URL-mapped contentlet resolved by
     * {@code urlMapInfo}. Resolves the contentlet version under the requested language and variant
     * (falls back to any-language if no exact match), mirroring the widget version-resolution
     * pattern.
     */
    private UrlContentMapView buildUrlContentMapView(final URLMapInfo urlMapInfo,
            final long languageId, final String variantName, final boolean live, final User user) {
        final Contentlet raw = urlMapInfo.getContentlet();
        if (raw == null) {
            return null;
        }
        final String contentletId = raw.getIdentifier();
        final ContentType ct = Try.of(raw::getContentType).getOrNull();
        final String contentTypeVar = ct != null ? ct.variable() : null;

        // Resolve version-specific inode (language + variant aware)
        Contentlet versioned = null;
        try {
            versioned = contentletAPI.findContentletByIdentifier(
                    contentletId, live, languageId, variantName, user, false);
        } catch (final Exception e) {
            Logger.debug(this, "URL map contentlet version lookup failed for '" + contentletId
                    + "': " + e.getMessage());
        }
        if (versioned == null || !UtilMethods.isSet(versioned.getInode())) {
            versioned = Try.of(
                    () -> contentletAPI.findContentletByIdentifierAnyLanguage(contentletId))
                    .getOrNull();
        }

        final String inode = versioned != null ? versioned.getInode() : raw.getInode();
        final String title = versioned != null ? versioned.getTitle() : raw.getTitle();
        return new UrlContentMapView(contentTypeVar, title, contentletId, inode);
    }

    /**
     * Prepends the host qualifier ({@code //hostname}) to a plain path if not already present.
     */
    private String buildHostQualifiedPath(final String rawPath, final Host host) {
        if (UtilMethods.isSet(rawPath) && rawPath.startsWith("//")) {
            return rawPath;
        }
        final String hostname = (host != null && UtilMethods.isSet(host.getHostname()))
                ? host.getHostname()
                : "localhost";
        final String normalised = UtilMethods.isSet(rawPath) ? rawPath : "/";
        return "//" + hostname + normalised;
    }

}
