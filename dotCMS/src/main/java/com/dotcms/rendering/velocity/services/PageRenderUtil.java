package com.dotcms.rendering.velocity.services;


import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.publisher.endpoint.bean.PublishingEndPoint;
import com.dotcms.rendering.velocity.directive.ParseContainer;
import com.dotcms.rendering.velocity.viewtools.DotTemplateTool;
import com.dotcms.repackage.com.google.common.collect.Lists;
import com.dotcms.repackage.com.ibm.icu.text.SimpleDateFormat;
import com.dotcms.visitor.domain.Visitor;
import com.dotmarketing.beans.ContainerStructure;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.MultiTreeAPI;
import com.dotmarketing.factories.PersonalizedContentlet;
import com.dotmarketing.portlets.containers.business.*;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.containers.model.FileAssetContainer;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.util.ContentletUtil;
import com.dotmarketing.portlets.htmlpageasset.business.render.ContainerRaw;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.personas.business.PersonaAPI;
import com.dotmarketing.portlets.personas.model.IPersona;
import com.dotmarketing.portlets.personas.model.Persona;
import com.dotmarketing.portlets.templates.design.bean.ContainerUUID;
import com.dotmarketing.portlets.templates.design.bean.TemplateLayout;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.tag.business.TagAPI;
import com.dotmarketing.tag.model.Tag;
import com.dotmarketing.util.*;
import com.google.common.collect.Maps;
import com.google.common.collect.Table;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import com.rainerhahnekamp.sneakythrow.Sneaky;
import io.vavr.control.Try;
import org.apache.velocity.context.Context;
import org.jetbrains.annotations.Nullable;

import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.*;
import java.util.function.Supplier;

import static com.dotmarketing.business.PermissionAPI.*;

/**
 * Util class for rendering a Page
 */
public class PageRenderUtil implements Serializable {

    public static String CONTAINER_UUID_PREFIX = "uuid-";
    private static final long serialVersionUID = 1L;

    private final PermissionAPI permissionAPI = APILocator.getPermissionAPI();
    private final MultiTreeAPI multiTreeAPI = APILocator.getMultiTreeAPI();
    private final ContentletAPI contentletAPI = APILocator.getContentletAPI();
    private final TagAPI tagAPI = APILocator.getTagAPI();
    private final PersonaAPI personaAPI = APILocator.getPersonaAPI();

    final IHTMLPage htmlPage;
    final User user;
    final Map<String, Object> contextMap; // this is the velocity runtime context
    final PageMode mode;
    final List<Tag> pageFoundTags;
    final StringBuilder widgetPreExecute;
    final static String WIDGET_PRE_EXECUTE = "WIDGET_PRE_EXECUTE";
    final List<ContainerRaw> containersRaw;
    final long languageId;
    final Host site;
    final TemplateLayout templateLayout;


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

    public PageRenderUtil(final HTMLPageAsset htmlPage, final User user, final PageMode mode)
            throws DotSecurityException, DotDataException {
        this(htmlPage, user, mode, htmlPage.getLanguageId(), APILocator.getHostAPI().find(htmlPage.getHost(), user, false));
    }

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
                (Template) APILocator.getVersionableAPI().findLiveVersion(templateId, systemUser, false)
                : (Template) APILocator.getVersionableAPI().findWorkingVersion(templateId, systemUser, false);

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
        ctxMap.put("canAddForm", LicenseUtil.getLevel() >= LicenseLevel.STANDARD.level ? true : false);
        ctxMap.put("canViewDiff", LicenseUtil.getLevel() >= LicenseLevel.STANDARD.level ? true : false);
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

    private Container getLiveContainerById(final String containerId) throws DotSecurityException, DotDataException {

        final LiveContainerFinderByIdOrPathStrategyResolver strategyResolver =
                LiveContainerFinderByIdOrPathStrategyResolver.getInstance();
        final Optional<ContainerFinderByIdOrPathStrategy> strategy = strategyResolver.get(containerId);
        final ContainerFinderByIdOrPathStrategy liveStrategy = strategy.isPresent() ? strategy.get() : strategyResolver.getDefaultStrategy();
        final Supplier<Host> resourceHostSupplier = () -> this.site;


        Container container = null;
        try {

            container =
                    liveStrategy.apply(containerId, APILocator.systemUser(), false, resourceHostSupplier);
        } catch (NotFoundInDbException e) {

            container = null;
        }

        return container;
    }

    private Container getLiveContainerById(final String containerIdOrPath, final User user, final Template template) throws NotFoundInDbException {

        final LiveContainerFinderByIdOrPathStrategyResolver strategyResolver =
                LiveContainerFinderByIdOrPathStrategyResolver.getInstance();
        final Optional<ContainerFinderByIdOrPathStrategy> strategy = strategyResolver.get(containerIdOrPath);

        return this.geContainerById(containerIdOrPath, user, template, strategy, strategyResolver.getDefaultStrategy());
    }

    private Container getWorkingContainerById(final String containerIdOrPath, final User user, final Template template) throws NotFoundInDbException {

        final WorkingContainerFinderByIdOrPathStrategyResolver strategyResolver =
                WorkingContainerFinderByIdOrPathStrategyResolver.getInstance();
        final Optional<ContainerFinderByIdOrPathStrategy> strategy = strategyResolver.get(containerIdOrPath);

        return this.geContainerById(containerIdOrPath, user, template, strategy, strategyResolver.getDefaultStrategy());
    }

    private Container geContainerById(final String containerIdOrPath, final User user, final Template template,
                                      final Optional<ContainerFinderByIdOrPathStrategy> strategy,
                                      final ContainerFinderByIdOrPathStrategy defaultContainerFinderByIdOrPathStrategy) throws NotFoundInDbException {

        final Supplier<Host> resourceHostSupplier = Sneaky.sneaked(() -> APILocator.getTemplateAPI().getTemplateHost(template));

        return strategy.isPresent() ?
                strategy.get().apply(containerIdOrPath, user, false, resourceHostSupplier) :
                defaultContainerFinderByIdOrPathStrategy.apply(containerIdOrPath, user, false, resourceHostSupplier);
    }

    private List<ContainerRaw> populateContainers() throws DotDataException, DotSecurityException {

        final HttpServletRequest request = HttpServletRequestThreadLocal.INSTANCE.getRequest();
        final boolean live =
                request != null && request.getSession(false) != null && request.getSession().getAttribute("tm_date") != null ?
                        false :
                        mode.showLive;
        final Table<String, String, Set<PersonalizedContentlet>> pageContents = this.multiTreeAPI.getPageMultiTrees(htmlPage, live);
        final Set<String> personalizationsForPage = this.multiTreeAPI.getPersonalizationsForPage(htmlPage);
        final List<ContainerRaw> raws = Lists.newArrayList();
        final String includeContentFor = this.getPersonaTagToIncludeContent(request, personalizationsForPage);

        for (final String containerId : pageContents.rowKeySet()) {

            Container container = null;
            final WorkingContainerFinderByIdOrPathStrategyResolver strategyResolver =
                    WorkingContainerFinderByIdOrPathStrategyResolver.getInstance();
            final Optional<ContainerFinderByIdOrPathStrategy> strategy = strategyResolver.get(containerId);
            final ContainerFinderByIdOrPathStrategy workingStrategy = strategy.isPresent() ? strategy.get() : strategyResolver.getDefaultStrategy();
            final Supplier<Host> resourceHostSupplier = () -> this.site;

            try {
                if (live) {

                    container = this.getLiveContainerById(containerId);
                    if (null == container) {
                        container = workingStrategy.apply(containerId, APILocator.systemUser(), false, resourceHostSupplier);
                    }
                } else {
                    container = workingStrategy.apply(containerId, APILocator.systemUser(), false, resourceHostSupplier);
                }
            } catch (NotFoundInDbException | DotRuntimeException e) {

                new ContainerExceptionNotifier(e, containerId).notifyUser();
                container = null;
            }

            if (container == null) {
                continue;
            }

            final List<ContainerStructure> containerStructures = APILocator.getContainerAPI().getContainerStructures(container);
            final boolean hasWritePermissionOnContainer = permissionAPI.doesUserHavePermission(container, PERMISSION_WRITE, user, false)
                    && APILocator.getPortletAPI().hasContainerManagerRights(user);
            final boolean hasReadPermissionOnContainer = permissionAPI.doesUserHavePermission(container, PERMISSION_READ, user, false);
            contextMap.put("EDIT_CONTAINER_PERMISSION" + container.getIdentifier(), hasWritePermissionOnContainer);
            if (Config.getBooleanProperty("SIMPLE_PAGE_CONTENT_PERMISSIONING", true)) {
                contextMap.put("USE_CONTAINER_PERMISSION" + container.getIdentifier(), true);
            } else {
                contextMap.put("USE_CONTAINER_PERMISSION" + container.getIdentifier(), hasReadPermissionOnContainer);
            }

            final Map<String, List<Map<String, Object>>> contentMaps = Maps.newLinkedHashMap();
            final Map<String, List<String>> containerUuidPersona = Maps.newHashMap();
            for (final String uniqueId : pageContents.row(containerId).keySet()) {

                final String uniqueUUIDForRender = needParseContainerPrefix(container, uniqueId) ?
                        ParseContainer.getDotParserContainerUUID(uniqueId) :
                        uniqueId;

                if (ContainerUUID.UUID_DEFAULT_VALUE.equals(uniqueId)) {
                    continue;
                }

                final Collection<PersonalizedContentlet> personalizedContentletSet = pageContents.get(containerId, uniqueId);
                final List<Map<String, Object>> personalizedContentletMap = Lists.newArrayList();

                for (final PersonalizedContentlet personalizedContentlet : personalizedContentletSet) {
                    final Contentlet contentlet = getContentlet(personalizedContentlet);

                    if (contentlet == null) {
                        continue;
                    }

                    containerUuidPersona
                            .computeIfAbsent(containerId + uniqueUUIDForRender + personalizedContentlet.getPersonalization(), k -> Lists.newArrayList())
                            .add(personalizedContentlet.getContentletId());
                    contextMap.put("EDIT_CONTENT_PERMISSION" + contentlet.getIdentifier(),
                            permissionAPI.doesUserHavePermission(contentlet, PERMISSION_WRITE, user));

                    final ContentType type = contentlet.getContentType();
                    if (type.baseType() == BaseContentType.WIDGET) {
                        final com.dotcms.contenttype.model.field.Field field = type.fieldMap().get("widgetPreexecute");
                        if (field != null && UtilMethods.isSet(field.values())) {
                            widgetPreExecute.append(field.values());
                        }
                    }

                    // Check if we want to accrue the tags associated to each contentlet on
                    // this page
                    if (Config.getBooleanProperty("ACCRUE_TAGS_IN_CONTENTS_ON_PAGE", false)) {

                        // Search for the tags associated to this contentlet inode
                        final List<Tag> contentletFoundTags = this.tagAPI.getTagsByInode(contentlet.getInode());
                        if (contentletFoundTags != null) {
                            this.pageFoundTags.addAll(contentletFoundTags);
                        }
                    }

                    if (personalizedContentlet.getPersonalization().equals(includeContentFor)) {
                        final Map<String, Object> contentPrintableMap = Try.of(() -> ContentletUtil.getContentPrintableMap(user, contentlet)).onFailure(f -> Logger.warn(this.getClass(), f.getMessage())).getOrNull();
                        if (contentPrintableMap == null) continue;
                        contentPrintableMap.put("contentType", contentlet.getContentType().variable());
                        personalizedContentletMap.add(contentPrintableMap);
                    }
                }

                contentMaps.put(CONTAINER_UUID_PREFIX + uniqueUUIDForRender, personalizedContentletMap);
            }
            for (Map.Entry<String, List<String>> entry : containerUuidPersona.entrySet()) {
                contextMap.put("contentletList" + entry.getKey(), entry.getValue());
                contextMap.put("totalSize" + entry.getKey(), entry.getValue().size());
            }


            raws.add(new ContainerRaw(container, containerStructures, contentMaps));
        }

        return raws;
    }

    private boolean needParseContainerPrefix(final Container container, final String uniqueId) {
        String containerIdOrPath = null;

        if (FileAssetContainerUtil.getInstance().isFileAssetContainer(container)) {
            containerIdOrPath = getRelativePathFromSite((FileAssetContainer) container);
        } else {
            containerIdOrPath = container.getIdentifier();
        }

        return !ParseContainer.isParserContainerUUID(uniqueId) &&
                (templateLayout == null || !templateLayout.existsContainer(containerIdOrPath, uniqueId));
    }

    /**
     * If the container's Host is equals to {@link PageRenderUtil#site} then return the relative path, but if the Host
     * are different then it return the full path.
     *
     * @param container
     * @return
     * @throws DotSecurityException
     * @throws DotDataException
     */
    private String getRelativePathFromSite(final FileAssetContainer container) {
        return this.site.getIdentifier().equals(container.getHost().getIdentifier()) ?
                container.getPath() :
                FileAssetContainerUtil.getInstance().getFullPath(container);
    }

    private Contentlet getContentlet(final PersonalizedContentlet personalizedContentlet) {

        return (Config.getBooleanProperty("DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE", false)) ?
                getContentletOrFallback(personalizedContentlet) : getSpecificContentlet(personalizedContentlet);
    }

    private Contentlet getSpecificContentlet(final PersonalizedContentlet personalizedContentlet) {
        try {

            final Contentlet contentlet = contentletAPI.findContentletByIdentifier
                    (personalizedContentlet.getContentletId(), mode.showLive, this.resolveLanguageId(), user, mode.respectAnonPerms);

            return contentlet;
        } catch (final DotContentletStateException e) {
            // Expected behavior, DotContentletState Exception is used for flow control
            return null;
        } catch (Exception e) {
            throw new DotStateException(e);
        }
    }

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

    private Contentlet getContentletOrFallback(final PersonalizedContentlet personalizedContentlet) {
        try {

            final Optional<Contentlet> contentletOpt = contentletAPI.findContentletByIdentifierOrFallback
                    (personalizedContentlet.getContentletId(), mode.showLive, languageId, user, mode.respectAnonPerms);

            final Contentlet contentlet = contentletOpt.isPresent()
                    ? contentletOpt.get() : contentletAPI.findContentletByIdentifierAnyLanguage(personalizedContentlet.getContentletId());

            return contentlet;
        } catch (final DotContentletStateException e) {
            // Expected behavior, DotContentletState Exception is used for flow control
            return null;
        } catch (Exception e) {
            throw new DotStateException(e);
        }
    }


    public List<Tag> getPageFoundTags() {
        return this.pageFoundTags;
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
}
