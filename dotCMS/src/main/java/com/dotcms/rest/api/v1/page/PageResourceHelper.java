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
import com.dotcms.rendering.velocity.services.PageLoader;
import com.dotcms.rest.api.v1.page.PageContainerForm.ContainerEntry;
import com.dotcms.rest.exception.BadRequestException;
import com.dotcms.util.CollectionsUtils;
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
import com.dotmarketing.factories.PersonalizedContentlet;
import com.dotmarketing.portlets.containers.business.FileAssetContainerUtil;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.containers.model.FileAssetContainer;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI;
import com.dotmarketing.portlets.htmlpageasset.business.render.HTMLPageAssetNotFoundException;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.personas.model.Persona;
import com.dotmarketing.portlets.templates.business.TemplateAPI;
import com.dotmarketing.portlets.templates.design.bean.ContainerUUID;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.Table;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import io.vavr.Lazy;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import java.util.Date;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.jetbrains.annotations.NotNull;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
    private static final Lazy<Boolean> DELETE_ORPHANED_CONTENTS_FROM_CONTAINER =
            Lazy.of(() -> Config.getBooleanProperty("DELETE_ORPHANED_CONTENTS_FROM_CONTAINER", true));

    private final HostWebAPI hostWebAPI = WebAPILocator.getHostWebAPI();
    private final HTMLPageAssetAPI htmlPageAssetAPI = APILocator.getHTMLPageAssetAPI();
    private final TemplateAPI templateAPI = APILocator.getTemplateAPI();
    private final ContentletAPI contentletAPI = APILocator.getContentletAPI();
    private final HostAPI hostAPI = APILocator.getHostAPI();
    private final MultiTreeAPI multiTreeAPI = APILocator.getMultiTreeAPI();
    private final UserAPI userAPI = APILocator.getUserAPI();
    private final PermissionAPI permissionAPI = APILocator.getPermissionAPI();

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
     *                         {@link PageContainerForm.ContainerEntry} objects.
     * @param language         The {@link Language} of the Contentlets for this page.
     *
     * @throws DotDataException An error occurred when interacting with the data source.
     */
    @WrapInTransaction
    public void saveContent(final String pageId,
            final List<ContainerEntry> containerEntries,

            final Language language, String variantName) throws DotDataException {
        final Map<String, List<MultiTree>> multiTreesMap = new HashMap<>();

        for (final PageContainerForm.ContainerEntry containerEntry : containerEntries) {
            int i = 0;
            final List<String> contentIds = containerEntry.getContentIds();
            final String personalization = UtilMethods.isSet(containerEntry.getPersonaTag()) ?
                    Persona.DOT_PERSONA_PREFIX_SCHEME + StringPool.COLON + containerEntry.getPersonaTag() :
                    MultiTree.DOT_PERSONALIZATION_DEFAULT;

            if (UtilMethods.isSet(contentIds)) {
                for (final String contentletId : contentIds) {
                    final MultiTree multiTree = new MultiTree().setContainer(containerEntry.getContainerId())
                            .setContentlet(contentletId)
                            .setInstanceId(containerEntry.getContainerUUID())
                            .setTreeOrder(i++)
                            .setHtmlPage(pageId)
                            .setVariantId(variantName);

                    CollectionsUtils.computeSubValueIfAbsent(
                            multiTreesMap, personalization, MultiTree.personalized(multiTree, personalization),
                            CollectionsUtils::add,
                            (String key, MultiTree multitree) -> CollectionsUtils.list(multitree));

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
        final Contentlet checkin = APILocator.getContentletAPI()
                .checkin(checkout, user, false);

        return APILocator.getHTMLPageAssetAPI().fromContentlet(checkin);
    }

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
            updateMultiTrees(page, pageForm);

            template.setModDate(new Date());
            // permissions have been updated above
            return this.templateAPI.saveTemplate(template, site, APILocator.systemUser(), false);
        } catch (final DotDataException | DotSecurityException e) {
            final String errorMsg = String.format("An error occurred when saving Template '%s' [ %s ]: %s",
                    template.getTitle(), template.getIdentifier(), ExceptionUtil.getErrorMessage(e));
            throw new DotRuntimeException(errorMsg, e);
        }
    }

    @WrapInTransaction
    protected void updateMultiTrees(final IHTMLPage page, final PageForm pageForm) throws DotDataException, DotSecurityException {

        final String currentVariantId = WebAPILocator.getVariantWebAPI().currentVariantId();
        final Table<String, String, Set<PersonalizedContentlet>> pageContents = multiTreeAPI
                .getPageMultiTrees(page, currentVariantId, false);

        final String pageIdentifier = page.getIdentifier();
        APILocator.getMultiTreeAPI().deleteMultiTree(pageIdentifier, currentVariantId);

        final List<MultiTree> multiTrees = new ArrayList<>();
        final boolean deleteOrphanedContents = DELETE_ORPHANED_CONTENTS_FROM_CONTAINER.get();

        for (final String containerId : pageContents.rowKeySet()) {
            int treeOrder = 0;

            for (final String uniqueId : pageContents.row(containerId).keySet()) {
                final Map<String, Set<PersonalizedContentlet>> row = pageContents.row(containerId);
                final Set<PersonalizedContentlet> contents         = row.get(uniqueId);

                if (!contents.isEmpty()) {
                    final String newContainerLayoutID = getNewContainerLayoutID(pageForm, containerId, uniqueId);
                    // Adding multi-tre records is skipped if (1) deleting orphaned records is enabled
                    // and (2) the container instance ID equals -1
                    if (!deleteOrphanedContents || (!ContainerUUID.UUID_DEFAULT_VALUE.equals(newContainerLayoutID))) {
                        for (final PersonalizedContentlet identifierPersonalization : contents) {
                            final MultiTree multiTree = MultiTree.personalized(
                                    new MultiTree().setContainer(containerId)
                                            .setContentlet(identifierPersonalization.getContentletId())
                                            .setInstanceId(newContainerLayoutID)
                                            .setTreeOrder(treeOrder++)
                                            .setHtmlPage(pageIdentifier)
                                            .setVariantId(currentVariantId),
                                    identifierPersonalization.getPersonalization());

                            multiTrees.add(multiTree);
                        }
                    }
                }
            }
        }

        multiTreeAPI.saveMultiTrees(pageIdentifier, currentVariantId, multiTrees);
    }

    /**
     * Returns the existing instance ID of the specified Container in the HTML Page Layout, or a new
     * instance ID in case it was changed because one or more Containers before or after it were
     * added, moved or removed.
     *
     * <p>The object {@link ContainerUUIDChanged} in the {@link PageForm} parameter indicates
     * whether a Container's position was changed, or removed altogether. If that's the case, an
     * instance ID of -1 will be returned. Containers whose instance ID start with
     * {@link ParseContainer#PARSE_CONTAINER_UUID_PREFIX} will always keep its ID.</p>
     *
     * <p>Additionally, if the Container instance ID cannot be found in the internal Map that holds
     * the updated position of Containers, it means such a Container doesn't exist anymore, so a -1
     * will be returned.</p>
     *
     * @param pageForm    The {@link PageForm} object containing the Template's new layout
     *                    information.
     * @param containerId The ID of the Container being processed.
     * @param uniqueId    The currently assigned instance ID of the Container
     *
     * @return The new instance ID of the Container, or -1 in case it was deleted from the Template.
     *
     * @throws DotDataException     An error occurred when persisting the changes.
     * @throws DotSecurityException A user permission error has occurred.
     */
    private String getNewContainerLayoutID(final PageForm pageForm, final String containerId,
                                           final String uniqueId)
            throws DotDataException, DotSecurityException {

        String containerPath = null;
        final Container foundContainer = APILocator.getContainerAPI()
                .getWorkingContainerById(containerId, userAPI.getSystemUser(), false);
        if (foundContainer instanceof FileAssetContainer) {
            // If we have a FileAssetContainer, we may need to search by path instead
            containerPath = FileAssetContainerUtil.getInstance().getFullPath((FileAssetContainer) foundContainer);
        }

        if (ContainerUUID.UUID_DEFAULT_VALUE.equals(uniqueId)) {
            String newContainerInstanceID = pageForm.getNewlyContainerUUID(containerId);
            if (newContainerInstanceID == null && containerPath != null) {
                // Searching also by Container path -- i.e., Container as File -- if not found
                newContainerInstanceID = pageForm.getNewlyContainerUUID(containerPath);
            }
            return newContainerInstanceID != null ? newContainerInstanceID
                    : ContainerUUID.UUID_DEFAULT_VALUE;
        } if (ParseContainer.isParserContainerUUID(uniqueId)) {
            return uniqueId;
        } else {
            ContainerUUIDChanged change = pageForm.getChangeInContainerInstanceIDs(containerId, uniqueId);
            if (change == null && containerPath != null) {
                // Searching also by Container path -- i.e., Container as File -- if not found
                change = pageForm.getChangeInContainerInstanceIDs(containerPath, uniqueId);
            }
            return change != null ? change.getNewInfo().getUUID() : ContainerUUID.UUID_DEFAULT_VALUE;
        }
    }

    public Template saveTemplate(final User user, final PageForm pageForm)
            throws BadRequestException, DotDataException, DotSecurityException, IOException {
        return this.saveTemplate(null, user, pageForm);
    }

    private Template checkoutTemplate(final IHTMLPage page, final User user, final PageForm form)
            throws DotDataException, DotSecurityException {

        final Template oldTemplate = this.templateAPI.findWorkingTemplate(page.getTemplateId(), user, false);
        final Template saveTemplate;
        final boolean useByAnotherPage = this.templateAPI.getPages(page.getTemplateId()).stream()
                .anyMatch(pageVersion -> !page.getIdentifier().equals(pageVersion.getIdentifier()) ||
                        !((HTMLPageAsset) page).getVariantId().equals(pageVersion.getVariantName()));

        if (!useByAnotherPage && !oldTemplate.getIdentifier().equals(Template.SYSTEM_TEMPLATE)) {
            saveTemplate = oldTemplate;
        } else {
            saveTemplate = new Template();
            saveTemplate.setTitle(form.getTitle());
        }

        saveTemplate.setInode(null);
        saveTemplate.setTheme((form.getThemeId()==null) ? oldTemplate.getTheme() : form.getThemeId());
        saveTemplate.setDrawedBody(form.getLayout());
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

        if (FileAssetContainerUtil.getInstance().isFolderAssetContainerId(container)) {

            final Container containerObject = APILocator.getContainerAPI().getLiveContainerByFolderPath(container, user, pageMode.respectAnonPerms,
                    ()-> Try.of(()->APILocator.getHostAPI().findDefaultHost(user, pageMode.respectAnonPerms)).getOrNull());
            if (null != containerObject) {
                container =containerObject.getIdentifier();
            }
        }

        Logger.debug(this, ()-> "Deleting current contentlet multi tree: " + copyContentletForm);
        final MultiTree currentMultitree = APILocator.getMultiTreeAPI().getMultiTree(htmlPage, container, contentId, instanceId,
                null == personalization? MultiTree.DOT_PERSONALIZATION_DEFAULT: personalization, null == variant? VariantAPI.DEFAULT_VARIANT.name(): variant);

        if (null == currentMultitree) {

            throw new DoesNotExistException(
                    "Can not copied the contentlet in the page, because the record is not part of the page, multitree: " + copyContentletForm);
        }

        APILocator.getMultiTreeAPI().deleteMultiTree(currentMultitree);

        final MultiTree newMultitree = new MultiTree(htmlPage, container, copiedContentlet.getIdentifier(),
                instanceId, treeOrder, null == personalization? MultiTree.DOT_PERSONALIZATION_DEFAULT: personalization,
                null == variant? VariantAPI.DEFAULT_VARIANT.name(): variant);
        Logger.debug(this, ()-> "Saving current contentlet multi tree: " + currentMultitree);
        APILocator.getMultiTreeAPI().saveMultiTree(newMultitree);

        if (null != originalContentlet) {
            HibernateUtil.addCommitListener(()->
                    new ContentletLoader().invalidate(originalContentlet, PageMode.EDIT_MODE));
        }


        return copiedContentlet;
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

}
