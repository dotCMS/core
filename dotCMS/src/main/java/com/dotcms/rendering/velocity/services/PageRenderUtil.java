package com.dotcms.rendering.velocity.services;

import static com.dotmarketing.business.PermissionAPI.PERMISSION_CAN_ADD_CHILDREN;
import static com.dotmarketing.business.PermissionAPI.PERMISSION_PUBLISH;
import static com.dotmarketing.business.PermissionAPI.PERMISSION_READ;
import static com.dotmarketing.business.PermissionAPI.PERMISSION_WRITE;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.publisher.endpoint.bean.PublishingEndPoint;
import com.dotcms.rendering.velocity.directive.ParseContainer;
import com.dotcms.rendering.velocity.viewtools.DotTemplateTool;
import com.dotcms.rendering.velocity.viewtools.content.util.ContentUtils;
import com.dotcms.repackage.com.google.common.collect.Lists;
import com.dotcms.rest.api.v1.page.PageResource;
import com.dotcms.util.TimeMachineUtil;
import com.dotcms.variant.VariantAPI;
import com.dotcms.visitor.domain.Visitor;
import com.dotmarketing.beans.ContainerStructure;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.MultiTreeAPI;
import com.dotmarketing.factories.PersonalizedContentlet;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.transform.DotContentletTransformer;
import com.dotmarketing.portlets.contentlet.transform.DotTransformerBuilder;
import com.dotmarketing.portlets.htmlpageasset.business.render.ContainerRaw;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.personas.model.IPersona;
import com.dotmarketing.portlets.personas.model.Persona;
import com.dotmarketing.portlets.templates.design.bean.ContainerUUID;
import com.dotmarketing.portlets.templates.design.bean.TemplateLayout;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.tag.business.TagAPI;
import com.dotmarketing.tag.model.Tag;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.Maps;
import com.google.common.collect.Table;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import io.vavr.control.Try;
import java.io.Serializable;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.apache.velocity.context.Context;

/**
 * Utility class that provides commonly used methods for rendering HTML Pages in dotCMS.
 *
 * @author Freddy Rodriguez
 * @since Feb 22nd, 2019
 */
public class PageRenderUtil implements Serializable {

    public static String CONTAINER_UUID_PREFIX = "uuid-";
    private static final long serialVersionUID = 1L;

    private final PermissionAPI permissionAPI = APILocator.getPermissionAPI();
    private final MultiTreeAPI multiTreeAPI = APILocator.getMultiTreeAPI();
    private final ContentletAPI contentletAPI = APILocator.getContentletAPI();
    private final TagAPI tagAPI = APILocator.getTagAPI();
    private final IdentifierAPI identifierAPI = APILocator.getIdentifierAPI();

    final IHTMLPage htmlPage;
    final User user;
    final Map<String, Object> contextMap; // this is the velocity runtime context
    final PageMode mode;
    final List<Tag> pageFoundTags;
    final StringBuilder widgetPreExecute;
    final List<ContainerRaw> containersRaw;
    final long languageId;
    final Host site;
    final TemplateLayout templateLayout;



    /**
     * Creates an instance of this class for a given HTML Page.
     *
     * @param htmlPage   The {@link IHTMLPage} that is being rendered.
     * @param user       The {@link User} accessing the page.
     * @param mode       The {@link PageMode} that this page is being rendered in.
     * @param languageId The ID of the language that the page is being rendered in.
     * @param site       The {@link Host} that the page lives in.
     *
     * @throws DotSecurityException The user does not have the specified permissions to perform
     *                              this action.
     * @throws DotDataException     An error occurred when accessing the data source.
     */
    public PageRenderUtil(
            final IHTMLPage htmlPage,
            final User user,
            final PageMode mode,
            final long languageId,
            final Host site) throws DotSecurityException, DotDataException {

        if (site == null) {
            this.site = APILocator.getHostAPI().findDefaultHost(user, mode.respectAnonPerms);
        } else {
            this.site = site;
        }

        this.pageFoundTags = Lists.newArrayList();
        this.htmlPage = htmlPage;
        this.user = user;
        this.mode = mode;
        this.languageId = languageId;
        this.widgetPreExecute = new StringBuilder();
        final Template template = APILocator.getHTMLPageAssetAPI().getTemplate(htmlPage, !mode.showLive);
        this.templateLayout = template != null && template.isDrawed() ? DotTemplateTool.themeLayout(template.getInode()) : null;
        this.contextMap = populateContext();
        this.containersRaw = populateContainers();
    }

    /**
     * Creates an instance of this class for a given HTML Page.
     *
     * @param htmlPage   The {@link IHTMLPage} that is being rendered.
     * @param user       The {@link User} accessing the page.
     * @param mode       The {@link PageMode} that this page is being rendered in.
     *
     * @throws DotSecurityException The user does not have the specified permissions to perform
     *                              this action.
     * @throws DotDataException     An error occurred when accessing the data source.
     */
    public PageRenderUtil(final HTMLPageAsset htmlPage, final User user, final PageMode mode)
            throws DotSecurityException, DotDataException {
        this(htmlPage, user, mode, htmlPage.getLanguageId(), APILocator.getHostAPI().find(htmlPage.getHost(), user, false));
    }

    /**
     * Creates a Context Data Map with useful information about the HTML Page that is being rendered. This can be passed
     * down to the Velocity Engine to use such information as required.
     *
     * @return The {@link Map} with page data.
     *
     * @throws DotDataException     An error occurred when accessing the data source.
     * @throws DotSecurityException The {@link User} accessing the  page doesn't have the required permissions to
     *                              perform this action.
     */
    private Map<String, Object> populateContext() throws DotDataException, DotSecurityException {
        final Map<String, Object> ctxMap = Maps.newHashMap();
        ctxMap.put(mode.name(), Boolean.TRUE);

        // set the page cache var
        if (htmlPage.getCacheTTL() > 0 && LicenseUtil.getLevel() >= LicenseLevel.COMMUNITY.level) {
            ctxMap.put("dotPageCacheDate", new java.util.Date());
            ctxMap.put("dotPageCacheTTL", htmlPage.getCacheTTL());
        }

        final String templateId = htmlPage.getTemplateId();

        final Identifier pageIdent = APILocator.getIdentifierAPI().find(htmlPage.getIdentifier());

        // gets pageChannel for this path
        final java.util.StringTokenizer st = new java.util.StringTokenizer(String.valueOf(pageIdent.getURI()), StringPool.SLASH);
        String pageChannel = null;
        if (st.hasMoreTokens()) {
            pageChannel = st.nextToken();
        }

        final User systemUser = APILocator.getUserAPI().getSystemUser();
        final Template template = mode.showLive ?
                APILocator.getTemplateAPI().findLiveTemplate(templateId,systemUser, false) :
                APILocator.getTemplateAPI().findWorkingTemplate(templateId,systemUser, false);

        // to check user has permission to write on this page
        final List<PublishingEndPoint> receivingEndpoints = APILocator.getPublisherEndPointAPI().getReceivingEndPoints();
        final boolean hasAddChildrenPermOverHTMLPage = permissionAPI.doesUserHavePermission(htmlPage, PERMISSION_CAN_ADD_CHILDREN, user);
        final boolean hasWritePermOverHTMLPage = permissionAPI.doesUserHavePermission(htmlPage, PERMISSION_WRITE, user);
        final boolean hasPublishPermOverHTMLPage = permissionAPI.doesUserHavePermission(htmlPage, PERMISSION_PUBLISH, user);
        final boolean hasRemotePublishPermOverHTMLPage =
                hasPublishPermOverHTMLPage && LicenseUtil.getLevel() >= LicenseLevel.STANDARD.level;
        final boolean hasEndPoints = UtilMethods.isSet(receivingEndpoints) && !receivingEndpoints.isEmpty();
        final boolean canUserWriteOnTemplate = permissionAPI.doesUserHavePermission(template, PERMISSION_WRITE, user)
                && APILocator.getPortletAPI().hasTemplateManagerRights(user);

        ctxMap.put("dotPageMode", mode.name());

        ctxMap.put("ADD_CHILDREN_HTMLPAGE_PERMISSION", hasAddChildrenPermOverHTMLPage);
        ctxMap.put("EDIT_HTMLPAGE_PERMISSION", hasWritePermOverHTMLPage);
        ctxMap.put("PUBLISH_HTMLPAGE_PERMISSION", hasPublishPermOverHTMLPage);
        ctxMap.put("REMOTE_PUBLISH_HTMLPAGE_PERMISSION", hasRemotePublishPermOverHTMLPage);
        ctxMap.put("REMOTE_PUBLISH_END_POINTS", hasEndPoints);
        ctxMap.put("canAddForm", LicenseUtil.getLevel() >= LicenseLevel.STANDARD.level);
        ctxMap.put("canViewDiff", LicenseUtil.getLevel() >= LicenseLevel.STANDARD.level);
        ctxMap.put("pageChannel", pageChannel);
        ctxMap.put("HTMLPAGE_ASSET_STRUCTURE_TYPE", true);
        ctxMap.put("HTMLPAGE_IS_CONTENT", true);


        ctxMap.put("EDIT_TEMPLATE_PERMISSION", canUserWriteOnTemplate);


        ctxMap.put(ContainerLoader.SHOW_PRE_POST_LOOP, true);
        ctxMap.put("HTMLPAGE_INODE", htmlPage.getInode());
        ctxMap.put("HTMLPAGE_IDENTIFIER", htmlPage.getIdentifier());
        ctxMap.put("HTMLPAGE_FRIENDLY_NAME", UtilMethods.espaceForVelocity(htmlPage.getFriendlyName()));
        ctxMap.put("HTMLPAGE_TITLE", UtilMethods.espaceForVelocity(htmlPage.getTitle()));
        ctxMap.put("TEMPLATE_INODE", templateId);
        ctxMap.put("HTMLPAGE_META", UtilMethods.espaceForVelocity(htmlPage.getMetadata()));
        ctxMap.put("HTMLPAGE_DESCRIPTION", UtilMethods.espaceForVelocity(htmlPage.getSeoDescription()));
        ctxMap.put("HTMLPAGE_KEYWORDS", UtilMethods.espaceForVelocity(htmlPage.getSeoKeywords()));
        ctxMap.put("HTMLPAGE_SECURE", htmlPage.isHttpsRequired());
        ctxMap.put("VTLSERVLET_URI", UtilMethods.encodeURIComponent(pageIdent.getURI()));
        ctxMap.put("HTMLPAGE_REDIRECT", UtilMethods.espaceForVelocity(htmlPage.getRedirect()));
        ctxMap.put("pageTitle", UtilMethods.espaceForVelocity(htmlPage.getTitle()));
        ctxMap.put("friendlyName", UtilMethods.espaceForVelocity(htmlPage.getFriendlyName()));
        ctxMap.put("HTML_PAGE_LAST_MOD_DATE", UtilMethods.espaceForVelocity(htmlPage.getFriendlyName()));
        ctxMap.put("HTMLPAGE_MOD_DATE", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(htmlPage.getModDate()));


        return ctxMap;
    }

    /**
     * Reads the Containers that are present in an HTML Page and loads all the Contentlets in them. This information can
     * be used, for example, by the UI layer to display the contents in a page and allow Users to edit it.
     *
     * @return The list of {@link ContainerRaw} objects with their respective Contentlets.
     *
     * @throws DotDataException     An error occurred when accessing the data source.
     * @throws DotSecurityException The {@link User} accessing the  page doesn't have the required permissions to
     *                              perform this action.
     */
    private List<ContainerRaw> populateContainers() throws DotDataException, DotSecurityException {

        final HttpServletRequest request = HttpServletRequestThreadLocal.INSTANCE.getRequest();
        final Date timeMachineDate    = timeMachineDate(request).orElseGet(()->null);
        final boolean live            = isLive();
        final String currentVariantId = WebAPILocator.getVariantWebAPI().currentVariantId();
        final Table<String, String, Set<PersonalizedContentlet>> pageContents = this.multiTreeAPI
                .getPageMultiTrees(htmlPage, currentVariantId, live);
        final Set<String> personalizationsForPage = multiTreeAPI.getPersonalizationsForPage(htmlPage, currentVariantId);
        final List<ContainerRaw> rawContainers  = Lists.newArrayList();
        final String includeContentFor = this.getPersonaTagToIncludeContent(request, personalizationsForPage);

        for (final String containerId : pageContents.rowKeySet()) {

            final Container container = this.getContainer(live, containerId);

            if (container == null) {
                continue;
            }

            final List<ContainerStructure> containerStructures = APILocator.getContainerAPI().getContainerStructures(container);
            this.addPermissions(container);

            final Map<String, List<Contentlet>> contentMaps = Maps.newLinkedHashMap();
            final ContainerUUIDPersona containerUuidPersona = new ContainerUUIDPersona();

            for (final String uniqueId : pageContents.row(containerId).keySet()) {

                final String uniqueUUIDForRender = getUniqueUUIDForRender(uniqueId, container);

                if (ContainerUUID.UUID_DEFAULT_VALUE.equals(uniqueId)) {
                    continue;
                }

                final Collection<PersonalizedContentlet> personalizedContentletSet = pageContents.get(containerId, uniqueId);
                final List<Contentlet> personalizedContentletMap          = Lists.newArrayList();

                for (final PersonalizedContentlet personalizedContentlet : personalizedContentletSet) {

                    final Contentlet nonHydratedContentlet = getContentletByVariantFallback(currentVariantId, personalizedContentlet, timeMachineDate);

                    if (nonHydratedContentlet == null) {
                        continue;
                    }

                    final DotContentletTransformer transformer = new DotTransformerBuilder()
                            .defaultOptions().content(nonHydratedContentlet).build();
                    final Contentlet contentlet = transformer.hydrate().get(0);
                    this.addContentletPageReferenceCount(contentlet);

                    final long contentsSize = containerUuidPersona
                            .getSize(container, uniqueUUIDForRender, personalizedContentlet);

                    if (container.getMaxContentlets() < contentsSize) {

                        Logger.debug(this, ()-> "Contentlet: "          + contentlet.getIdentifier()
                                + ", has been skipped. Max contentlet capacity: " + container.getMaxContentlets()
                                + ", has been exceeded for container: " + containerId);
                        continue;
                    }

                    containerUuidPersona.add(container, uniqueUUIDForRender, personalizedContentlet);


                    contextMap.put("EDIT_CONTENT_PERMISSION" + contentlet.getIdentifier(),
                            permissionAPI.doesUserHavePermission(contentlet, PERMISSION_WRITE, user));

                    this.widgetPreExecute(contentlet);
                    this.addAccrueTags(contentlet);
                    this.addRelationships(contentlet);

                    if (personalizedContentlet.getPersonalization().equals(includeContentFor)) {

                        contentlet.getMap().put("contentType", contentlet.getContentType().variable());
                        personalizedContentletMap.add(contentlet);
                    }
                }

                contentMaps.put(CONTAINER_UUID_PREFIX + uniqueUUIDForRender, personalizedContentletMap);
            }

            for (final Map.Entry<String, List<String>> entry : containerUuidPersona.entrySet()) {

                contextMap.put("contentletList" + entry.getKey(), entry.getValue());
                contextMap.put("totalSize"      + entry.getKey(), entry.getValue().size());
            }

            rawContainers.add(new ContainerRaw(container, containerStructures, contentMaps));
        }

        return rawContainers;
        }

    private String getUniqueUUIDForRender(String uniqueId, Container container) {
        if (needParseContainerPrefix(container, uniqueId)) {
            return ParseContainer.getDotParserContainerUUID(uniqueId);
        } else {
            return uniqueId.equals(ContainerUUID.UUID_LEGACY_VALUE) ? ContainerUUID.UUID_START_VALUE : uniqueId;
        }
    }

    /**
     * Retrieves the Time Machine Date from the current HTTP Request, if available.
     * @param request
     * @return
     */
    private Optional<Date> timeMachineDate(final HttpServletRequest request) {
        // EDIT mode should always override time machine date
        // EDIT mode should continue to work as it does now, without the time machine date
        // And guaranty that we will only get working content in EDIT mode
        // therefore EDIT mode nullifies the time machine date
        if (request == null || this.mode == PageMode.EDIT_MODE) {
            return Optional.empty();
        }

        Optional<Object> millis = Optional.empty();
        final HttpSession session = request.getSession(false);
        if (session != null) {
            millis = Optional.ofNullable (session.getAttribute(PageResource.TM_DATE));
        }

        if (millis.isEmpty()) {
            millis = Optional.ofNullable(request.getAttribute(PageResource.TM_DATE));
        }

        if (millis.isEmpty()) {
            return Optional.empty();
        }
        final Object object = millis.get();
        try {
            final long milliseconds =  object instanceof Number ? (Long) object : Long.parseLong(object.toString());
            return milliseconds > 0
                    ? Optional.of(Date.from(Instant.ofEpochMilli(milliseconds)))
                    : Optional.empty();
        } catch (NumberFormatException e) {
            Logger.error(this, "Invalid timestamp format: " + object, e);
            return Optional.empty();
        }

    }

    /**
     * When <b>in Edit Mode only</b>, we can determine the number of Containers in any HTML Page in
     * the repository that are referencing a given Contentlet. This piece of information will be
     * added to the Contentlet's data map so that the UI can provide the editing User two choices:
     * <ol>
     *     <li>Edit that specific Contentlet: This will create a brand new copy of such a
     *     Contentlet that the User will modify. This means that any other page referencing it
     *     WILL NOT have the new changes.</li>
     *     <li>Edit the "global" Content: The User will be editing the unique instance of this
     *     Contentlet. This means that any change made to it will be reflected on all other HTML
     *     Pages that are displaying it.</li>
     * </ol>
     * This new property is specified in {@link Contentlet#ON_NUMBER_OF_PAGES}.
     *
     * @param contentlet The {@link Contentlet} whose HTML Page references will be counted.
     */
    private void addContentletPageReferenceCount(final Contentlet contentlet) {
        if (this.mode.isEditMode()) {
            final Optional<Integer> pageReferences =
                    Try.of(() -> this.contentletAPI.getAllContentletReferencesCount(contentlet.getIdentifier())).getOrElse(Optional.empty());
            pageReferences.ifPresent(integer -> contentlet.getMap().put(Contentlet.ON_NUMBER_OF_PAGES, integer));
        }
    }

    private void addRelationships(final Contentlet contentlet) {

        ContentUtils.addRelationships(contentlet, user, mode, languageId);
    }

    private Contentlet getContentletByVariantFallback(final String currentVariantId,
            final PersonalizedContentlet personalizedContentlet, final Date timeMachineDate)
            throws DotSecurityException {

        final String contentletId = personalizedContentlet.getContentletId();

        // Try current variant first
        Contentlet contentlet = getContentlet(contentletId, currentVariantId, timeMachineDate);

        // Fallback to default variant if not found and not already using default
        if (contentlet == null && !VariantAPI.DEFAULT_VARIANT.name().equals(currentVariantId)) {
            contentlet = getContentlet(contentletId, VariantAPI.DEFAULT_VARIANT.name(), timeMachineDate);
        }

        return contentlet;
    }

    /**
     * Checks if live content must be returned based on information in the current HTTP Request. So, if the
     * {@code "tm_date"} Session attribute is present -- a Time Machine request -- then working content must always be
     * returned. Otherwise, the result will be given by the value provided by {@link PageMode#showLive}.
     * @return If live content must be displayed, returned {@code true}.
     */
    private boolean isLive() {
        return TimeMachineUtil.isNotRunning() && mode.showLive;
    }

    /**
     * Retrieves the specified Container ID from the dotCMS repository.
     *
     * @param live        If the live version of the Container must be loaded, set this to {@code true}.
     * @param containerId The Container ID being retrieved.
     *
     * @return The {@link Container} object.
     *
     * @throws DotSecurityException The specified User does not have the required permissions to perform this action.
     * @throws DotDataException     An error occurred when interacting with the data source.
     */
    private Container getContainer(final boolean live, final String containerId) throws DotSecurityException, DotDataException {
        final Optional<Container> optionalContainer =
                APILocator.getContainerAPI().findContainer(containerId, APILocator.systemUser(), live, false);
        return optionalContainer.orElse(null);
    }

    /**
     * Adds the values of several Container-specific permissions to the HTML Page's Context Map.
     *
     * @param container The {@link Container} whose permissions are being checked.
     *
     * @throws DotDataException An error occurred when interacting with the data source.
     */
    private void addPermissions(final Container container) throws DotDataException {

        final boolean hasWritePermissionOnContainer = permissionAPI.doesUserHavePermission(container, PERMISSION_WRITE, user, false)
                && APILocator.getPortletAPI().hasContainerManagerRights(user);
        final boolean hasReadPermissionOnContainer  = permissionAPI.doesUserHavePermission(container, PERMISSION_READ,  user, false);
        contextMap.put("EDIT_CONTAINER_PERMISSION" + container.getIdentifier(), hasWritePermissionOnContainer);

        if (Config.getBooleanProperty("SIMPLE_PAGE_CONTENT_PERMISSIONING", true)) {

            contextMap.put("USE_CONTAINER_PERMISSION" + container.getIdentifier(), true);
        } else {

            contextMap.put("USE_CONTAINER_PERMISSION" + container.getIdentifier(), hasReadPermissionOnContainer);
        }
    }

    /**
     * Checks if we want to accrue the tags associated to each contentlet on this page.
     *
     * @param contentlet The {@link Contentlet} with potential Tags in it.
     *
     * @throws DotDataException An error occurred when interacting with the data source.
     */
    private void addAccrueTags(final Contentlet contentlet) throws DotDataException {

        if (Config.getBooleanProperty("ACCRUE_TAGS_IN_CONTENTS_ON_PAGE", false)) {

            // Search for the tags associated to this contentlet inode
            final List<Tag> contentletFoundTags = this.tagAPI.getTagsByInode(contentlet.getInode());
            if (contentletFoundTags != null) {
                this.pageFoundTags.addAll(contentletFoundTags);
            }
        }
    }

    /**
     * Sets the Widget Pre-Execute code for the specified Contentlet.
     *
     * @param contentlet The {@link Contentlet} object.
     */
    private void widgetPreExecute(final Contentlet contentlet) {

        final ContentType type = contentlet.getContentType();
        if (type.baseType() == BaseContentType.WIDGET) {

            final com.dotcms.contenttype.model.field.Field field = type.fieldMap().get("widgetPreexecute");
            if (field != null && UtilMethods.isSet(field.values())) {

                widgetPreExecute.append(field.values());
            }
        }
    }

    private boolean needParseContainerPrefix(final Container container, final String uniqueId) {
        return !ParseContainer.isParserContainerUUID(uniqueId) &&
                (templateLayout == null || !templateLayout.existsContainer(container, uniqueId));
    }

    /**
     * Returns the Contentlet specified in the {@link PersonalizedContentlet} object, which comes
     * from the {@code multi_tree} table that determines how HTML Pages, Containers, Contentlets,
     * and Personalization are associated. Depending on the current configuration in your dotCMS
     * instance, the {@link Contentlet} being returned will be either the one in the current
     * language, or the version in the default language.
     * <p>Now, for HTML Page editing purposes ONLY, if the current User does not have {@code READ}
     * permission on the specified Contentlet, the Anonymous User will be used to retrieve it. This
     * way, limited Users can still open and edit the HTML Page without any problems.</p>
     *
     * @param contentletIdentifier The contentletIdentifier.
     * @param variantName            The name of the Variant that the Contentlet is associated
     *                               with.
     *
     * @return An instance of the {@link Contentlet} represented by the Identifier in the
     * Personalized Contentlet object.
     */
    private Contentlet getContentlet(final String contentletIdentifier,
            final String variantName, final Date timeMachineDate) throws DotSecurityException {

        try {
            return Config.getBooleanProperty("DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE", false) ?
                    getContentletOrFallback(contentletIdentifier, variantName, timeMachineDate) :
                    getSpecificContentlet(contentletIdentifier, variantName, timeMachineDate);
        } catch (final DotSecurityException se) {
            if (this.mode == PageMode.EDIT_MODE || this.mode == PageMode.PREVIEW_MODE) {
                // In Edit Mode, allow Users who cannot edit a specific piece of content to be able to edit the HTML
                // Page that is holding it without any problems
                return limitedUserPermissionFallback(contentletIdentifier);
            }
            throw se;
        }
    }

    /**
     * Checks if the CMS Anonymous User has {@code READ} permission on the specified Contentlet ID.
     *
     * @param contentletId The Identifier of the Contentlet whose permission will be checked.
     *
     * @return If the User has the expected {@code READ} permission, the {@link Contentlet} will be returned. If not, a
     * {@code null} will be returned.
     */
    private Contentlet limitedUserPermissionFallback(final String contentletId) {
        final long resolvedLangId = this.resolveLanguageId();
        try {
            final User anonymousUser = APILocator.getUserAPI().getAnonymousUser();
            return this.contentletAPI.findContentletByIdentifier(contentletId, this.mode.showLive, resolvedLangId,
                            anonymousUser, true);
        } catch (final Exception e) {
            Logger.debug(this,
                    String.format("User '%s' does not have access to Contentlet '%s' in Edit Mode. Just move on",
                            this.user.getUserId(), contentletId));
            return null;
        }
    }

    /**
     * Returns the Contentlet specified in the {@link PersonalizedContentlet} object, which comes
     * from the {@code multi_tree} table that determines how HTML Pages, Containers, Contentlets,
     * and Personalization are associated.
     *
     * @param contentletIdentifier The contentletIdentifier.
     * @param variantName            The name of the Variant that the Contentlet is associated with
     *
     * @return An instance of the {@link Contentlet} represented by the Identifier in the
     * Personalized Contentlet object.
     *
     * @throws DotSecurityException The specified User does not have {@code READ} permission on the
     *                              specified Content
     */
    private Contentlet getSpecificContentlet(final String contentletIdentifier,
            final String variantName, final Date timeMachineDate) throws
            DotSecurityException {
        final long resolveLanguageId = this.resolveLanguageId();
        try {
            if(null != timeMachineDate && hasPublishOrExpireDateSet(contentletIdentifier)){
                // if a time machine date is provided we need to return regardless of the result.
                Logger.debug(this, "Trying to find contentlet with Time Machine date");
                return contentletAPI.findContentletByIdentifier(
                        contentletIdentifier,
                        resolveLanguageId,
                        variantName, timeMachineDate, user, mode.respectAnonPerms
                );
            }
            //If no time machine date is provided, we will return the contentlet based on the mode.showLive
            return contentletAPI.findContentletByIdentifier(
                        contentletIdentifier, mode.showLive, resolveLanguageId,
                        variantName, user, mode.respectAnonPerms
            );
        } catch (final DotContentletStateException e) {
            // Expected behavior, DotContentletState Exception is used for flow control
            return null;
        } catch (final DotSecurityException e) {
            // Expected behavior. The User might not have permissions on a given Contentlet
            throw e;
        } catch (final Exception e) {
            throw new DotStateException(e);
        }
    }

    /**
     * Checks if the specified Contentlet has a publish-date set in the system.
     * @param identifier The identifier of the Contentlet to check.
     * @return {@code true} if the Contentlet has a publish-date set, {@code false} otherwise.
     */
    boolean hasPublishOrExpireDateSet(final String identifier) {
        try {
            final Identifier found = identifierAPI.find(identifier);
            if (found != null && (found.getSysPublishDate() != null || found.getSysExpireDate() != null)) {
                return true;
            }
        } catch (DotDataException e) {
            Logger.error(this, "Error finding identifier: " + identifier, e);
        }
        return false;
    }

    /**
     * Resolves the currently selected Language ID from the Thread Local object, or the current HTTP Request.
     *
     * @return The selected Language ID.
     */
    private long resolveLanguageId () {

        final HttpServletRequest request = HttpServletRequestThreadLocal.INSTANCE.getRequest();
        if (null != request) {

            final Language currentLanguage =
                    WebAPILocator.getLanguageWebAPI().getLanguage(request);
            if (null != currentLanguage) {

                return currentLanguage.getId();
            }
        }

        return this.languageId;
    }

    /**
     * Returns the Contentlet specified in the {@link PersonalizedContentlet} object, which comes
     * from the {@code multi_tree} table that determines how HTML Pages, Containers, Contentlets,
     * and Personalization are associated. If the {@link Contentlet} being returned is NOT available
     * in the current language, the version in the default language will be returned instead.
     * <p>Now, for HTML Page editing purposes ONLY, if the current User does not have {@code READ}
     * permission on the specified Contentlet, the Anonymous User will be used to retrieve it. This
     * way, limited Users can still open and edit the HTML Page without any problems.</p>
     *
     * @param contentletIdentifier   The contentletIdentifier
     * @param variantName            The name of the Variant that the Contentlet is associated
     *                               with.
     *
     * @return An instance of the {@link Contentlet} represented by the Identifier in the
     * Personalized Contentlet object.
     */
    private Contentlet getContentletOrFallback(final String contentletIdentifier,
            final String variantName, final Date timeMachineDate) {
        try {
            // Apply the Time Machine date if it is provided and the Contentlet has a publish-date set
            //This helps to avoid applying time that are not set to be published in the future
            if(null != timeMachineDate && hasPublishOrExpireDateSet(contentletIdentifier)) {
                //This code shouldn't return contentlets that have a sys-publish date unless they match the tm date
                Logger.debug(this, "Trying to find contentlet with Fallback and Time Machine date");
                final Optional<Contentlet> contentlet = contentletAPI.findContentletByIdentifierOrFallback(
                        contentletIdentifier, languageId, variantName,
                        timeMachineDate, user,
                        true);
                if(contentlet.isPresent()) {
                     return contentlet.get();
                }
            }
            // No need to apply the Time Machine date, just return the contentlet based on the mode.showLive
            final Optional<Contentlet> contentletOpt = contentletAPI.findContentletByIdentifierOrFallback(
                    contentletIdentifier, mode.showLive, languageId,
                    user, true);

            return contentletOpt.isPresent()
                    ? contentletOpt.get() : contentletAPI.findContentletByIdentifierAnyLanguage(
                    contentletIdentifier,
                    variantName);

        } catch (final DotContentletStateException e) {
            // Expected behavior, DotContentletState Exception is used for flow control
            return null;
        } catch (final Exception e) {
            throw new DotStateException(e);
        }
    }

    final String getWidgetPreExecute() {
        return this.widgetPreExecute.toString();
    }


    public Context addAll(Context incoming) {

        for (String key : this.contextMap.keySet()) {
            incoming.put(key, this.contextMap.get(key));
        }

        return incoming;
    }

    /**
     * Return the Velocity code to set the Page's variables as: contentletList, contentType,
     * totalSize, etc.
     *
     * @return
     */
    public String asString() {

        final StringWriter s = new StringWriter();
        for (String key : this.contextMap.keySet()) {
            s.append("#set($").append(key.replace(":", "")).append("=").append(new StringifyObject(this.contextMap.get(key)).from()).append(')');
        }

        return s.toString();

    }

    /**
     * Return the Page's {@link ContainerRaw}
     *
     * @return
     */
    public List<ContainerRaw> getContainersRaw() {
        return this.containersRaw;
    }

    private String getPersonaTagToIncludeContent(final HttpServletRequest request, final Set<String> personalizationsForPage) {
        IPersona iPersona = null;

        if (request != null) {
            final Optional<Visitor> visitor = APILocator.getVisitorAPI().getVisitor(request);
            iPersona = visitor.isPresent() && visitor.get().getPersona() != null ? visitor.get().getPersona() : null;
        }

        final String currentPersonaTag = iPersona == null ? MultiTree.DOT_PERSONALIZATION_DEFAULT
                : Persona.DOT_PERSONA_PREFIX_SCHEME + StringPool.COLON + iPersona.getKeyTag();

        final boolean hasPersonalizations = personalizationsForPage.contains(currentPersonaTag);

        return hasPersonalizations ? currentPersonaTag : MultiTree.DOT_PERSONALIZATION_DEFAULT;
    }

    /**
     * Util class to sort the {@link Contentlet} by {@link Persona} and {@link Container}
     */
    private static class ContainerUUIDPersona {
        Map<String, List<String>> contents = Maps.newHashMap();

        public void add(
                final Container container,
                final String uniqueUUIDForRender,
                final PersonalizedContentlet personalizedContentlet) {

            get(container, uniqueUUIDForRender, personalizedContentlet)
                    .add(personalizedContentlet.getContentletId());
        }

        private List<String> get(Container container, String uniqueUUIDForRender, PersonalizedContentlet personalizedContentlet) {
            return contents
                    .computeIfAbsent(
                            getKey(container, uniqueUUIDForRender, personalizedContentlet),
                            k -> Lists.newArrayList()
                    );
        }

        private String getKey(Container container, String uniqueUUIDForRender, PersonalizedContentlet personalizedContentlet) {
            return container.getIdentifier() + uniqueUUIDForRender + personalizedContentlet.getPersonalization();
        }

        public long getSize(
                final Container container,
                final String uniqueUUIDForRender,
                final PersonalizedContentlet personalizedContentlet) {
            return get(container, uniqueUUIDForRender, personalizedContentlet).size();
        }

        public Set<? extends Map.Entry<String, List<String>>> entrySet() {
            return contents.entrySet();
        }
    }

}
