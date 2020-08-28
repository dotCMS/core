package com.dotmarketing.portlets.htmlpageasset.business.render;

import static com.dotcms.rendering.velocity.services.PageRenderUtil.CONTAINER_UUID_PREFIX;
import static com.dotmarketing.business.PermissionAPI.PERMISSION_READ;
import static com.dotmarketing.business.PermissionAPI.PERMISSION_WRITE;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.rendering.velocity.directive.ParseContainer;
import com.dotcms.rendering.velocity.viewtools.DotTemplateTool;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.com.google.common.collect.Lists;
import com.dotcms.visitor.domain.Visitor;
import com.dotmarketing.beans.ContainerStructure;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.business.*;
import com.dotmarketing.business.web.HostWebAPI;
import com.dotmarketing.business.web.LanguageWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.cms.urlmap.URLMapAPIImpl;
import com.dotmarketing.cms.urlmap.URLMapInfo;
import com.dotmarketing.cms.urlmap.UrlMapContextBuilder;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.PersonalizedContentlet;
import com.dotmarketing.filters.Constants;
import com.dotmarketing.portlets.containers.business.ContainerExceptionNotifier;
import com.dotmarketing.portlets.containers.business.ContainerFinderByIdOrPathStrategy;
import com.dotmarketing.portlets.containers.business.FileAssetContainerUtil;
import com.dotmarketing.portlets.containers.business.LiveContainerFinderByIdOrPathStrategyResolver;
import com.dotmarketing.portlets.containers.business.WorkingContainerFinderByIdOrPathStrategyResolver;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.containers.model.FileAssetContainer;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.util.ContentletUtil;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI;
import com.dotmarketing.portlets.htmlpageasset.business.render.page.HTMLPageAssetRenderedBuilder;
import com.dotmarketing.portlets.htmlpageasset.business.render.page.PageView;
import com.dotmarketing.portlets.htmlpageasset.business.render.page.ViewAsPageStatus;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.personas.model.IPersona;
import com.dotmarketing.portlets.personas.model.Persona;
import com.dotmarketing.portlets.rules.business.RulesEngine;
import com.dotmarketing.portlets.rules.model.Rule.FireOn;
import com.dotmarketing.portlets.templates.design.bean.ContainerUUID;
import com.dotmarketing.portlets.templates.design.bean.TemplateLayout;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.tag.model.Tag;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UUIDUtil;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.google.common.collect.Maps;
import com.google.common.collect.Table;
import com.liferay.portal.model.User;

import com.liferay.util.StringPool;
import com.rainerhahnekamp.sneakythrow.Sneaky;
import io.vavr.control.Try;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * {@link HTMLPageAssetRenderedAPI} implementation
 */
public class HTMLPageAssetRenderedAPIImpl implements HTMLPageAssetRenderedAPI {

    private final HostWebAPI hostWebAPI;
    private final HTMLPageAssetAPI htmlPageAssetAPI;
    private final LanguageAPI languageAPI;
    private final PermissionAPI permissionAPI;
    private final UserAPI userAPI;
    private final URLMapAPIImpl urlMapAPIImpl;
    private final LanguageWebAPI languageWebAPI;

    public HTMLPageAssetRenderedAPIImpl(){
        this(
            APILocator.getPermissionAPI(),
            APILocator.getUserAPI(),
            WebAPILocator.getHostWebAPI(),
            APILocator.getLanguageAPI(),
            APILocator.getHTMLPageAssetAPI(),
            APILocator.getURLMapAPI(),
            WebAPILocator.getLanguageWebAPI()
        );
    }

    @VisibleForTesting
    public HTMLPageAssetRenderedAPIImpl(
            final PermissionAPI permissionAPI,
            final UserAPI userAPI,
            final HostWebAPI hostWebAPI,
            final LanguageAPI languageAPI,
            final HTMLPageAssetAPI htmlPageAssetAPI,
            final URLMapAPIImpl urlMapAPIImpl,
            final LanguageWebAPI languageWebAPI
    ){

        this.permissionAPI = permissionAPI;
        this.userAPI = userAPI;
        this.hostWebAPI = hostWebAPI;
        this.languageAPI = languageAPI;
        this.htmlPageAssetAPI = htmlPageAssetAPI;
        this.urlMapAPIImpl = urlMapAPIImpl;
        this.languageWebAPI = languageWebAPI;
    }

    @Override
    public PageView getPageMetadata(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final User user,
            final String uri,
            final PageMode mode)
                throws DotSecurityException, DotDataException {

        return this.getPageMetadata(
                PageContextBuilder.builder()
                        .setUser(user)
                        .setPageUri(uri)
                        .setPageMode(mode)
                        .build(),
                request,
                response
        );
    }

    /**
     * @param context    The {@link PageContext} object.
     * @return The rendered page, i.e., the HTML source code that will be rendered by the browser.
     * @throws DotSecurityException The user does not have the specified permissions to perform
     *                              this action.
     * @throws DotDataException     An error occurred when accessing the data source.
     */
    @Override
    public PageView getPageMetadata(
            final PageContext context,
            final HttpServletRequest request,
            final HttpServletResponse response)
                throws DotSecurityException, DotDataException {

        final Host host = this.hostWebAPI.getCurrentHost(request, context.getUser());
        final HTMLPageUrl htmlPageUrl = getHtmlPageAsset(context, host, request);

        fireRulesOnPage(htmlPageUrl.getHTMLPage(), request, response);

        return new HTMLPageAssetRenderedBuilder()
                .setHtmlPageAsset(htmlPageUrl.getHTMLPage())
                .setUser(context.getUser())
                .setRequest(request)
                .setResponse(response)
                .setSite(host)
                .setURLMapper(htmlPageUrl.getPageUrlMapper())
                .setLive(htmlPageUrl.hasLive())
                .build(false, context.getPageMode());
    }

    @Override
    public HTMLPageUrl getHtmlPageAsset(final PageContext context,
            final HttpServletRequest request) throws DotSecurityException, DotDataException {
        final Host host = this.hostWebAPI.getCurrentHost(request, context.getUser());
        return getHtmlPageAsset(context, host, request);
    }

    @Override
    public PageView getPageRendered(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final User user,
            final String pageUri,
            final PageMode pageMode) throws DotDataException, DotSecurityException {

        return this.getPageRendered(
                PageContextBuilder.builder()
                        .setUser(user)
                        .setPageUri(pageUri)
                        .setPageMode(pageMode)
                        .build(),
                request,
                response
        );
    }

    @Override
    public PageView getPageRendered(
            final PageContext context,
            final HttpServletRequest request,
            final HttpServletResponse response)
                throws DotDataException, DotSecurityException {

        final PageMode mode = context.getPageMode();

        PageMode.setPageMode(request, mode);

        final Host host = this.hostWebAPI.getCurrentHost(request, context.getUser());
        final HTMLPageUrl htmlPageUrl = context.getPage() != null
                ? new HTMLPageUrl(context.getPage())
                : getHtmlPageAsset(context, host, request);

        fireRulesOnPage(htmlPageUrl.getHTMLPage(), request, response);
                
        return new HTMLPageAssetRenderedBuilder()
                .setHtmlPageAsset(htmlPageUrl.getHTMLPage())
                .setUser(context.getUser())
                .setRequest(request)
                .setResponse(response)
                .setSite(host)
                .setURLMapper(htmlPageUrl.getPageUrlMapper())
                .setLive(htmlPageUrl.hasLive())
                .build(true, mode);
    }

    @Override
    public String getPageHtml(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final User user,
            final String uri,
            final PageMode mode) throws DotSecurityException, DotDataException {

        return this.getPageHtml(
                PageContextBuilder.builder()
                        .setUser(user)
                        .setPageUri(uri)
                        .setPageMode(mode)
                        .build(),
                request,
                response
        );
    }

    public PageMode getDefaultEditPageMode(
            final User user,
            final HttpServletRequest request,
            final String pageUri) {
        try {
            final User systemUser = userAPI.getSystemUser();

            final Host host = this.hostWebAPI.getCurrentHost(request, systemUser);

            final IHTMLPage htmlPageAsset = this.getHtmlPageAsset(
                    PageContextBuilder.builder()
                        .setPageMode(PageMode.PREVIEW_MODE)
                        .setPageUri(pageUri)
                        .setUser(systemUser)
                        .build(),
                    host,
                    request
            ).getHTMLPage();

            return this.permissionAPI.doesUserHavePermission(htmlPageAsset, PermissionLevel.READ.getType(), user, false)
                    ? PageMode.PREVIEW_MODE : PageMode.ADMIN_MODE;
        } catch (DotDataException | DotSecurityException e) {
            throw new DotRuntimeException(e);
        }
    }

    @Override
    public ViewAsPageStatus getViewAsStatus(final HttpServletRequest request,
            final PageMode pageMode, final HTMLPageAsset htmlpage, final User user)
            throws DotDataException {
        final Set<String> pagePersonalizationSet  = APILocator.getMultiTreeAPI()
                .getPersonalizationsForPage(htmlpage);
        final IPersona persona     = this.getCurrentPersona(request);
        final boolean personalized = this.isPersonalized(persona, pagePersonalizationSet);
        final Contentlet device = APILocator.getDeviceAPI().getCurrentDevice(request, user)
                .orElse(null);

        return new ViewAsPageStatus(
                getVisitor(request),
                WebAPILocator.getLanguageWebAPI().getLanguage(request),
                device,
                pageMode,
                personalized );
    }

    @Override
    public List<ContainerRaw> getPageContainers(final HttpServletRequest request,
            final HTMLPageAsset htmlPage, final PageMode mode, final User user)
        throws DotDataException, DotSecurityException {
        final boolean live               = this.isLive(request, mode);
        final Table<String, String, Set<PersonalizedContentlet>> pageContents =
                APILocator.getMultiTreeAPI().getPageMultiTrees(htmlPage, live);
        final Set<String> personalizationsForPage = APILocator.getMultiTreeAPI()
                .getPersonalizationsForPage(htmlPage);
        final List<ContainerRaw> raws  = Lists.newArrayList();
        final String includeContentFor = getPersonaTagToIncludeContent(request,
                personalizationsForPage);

        final Template template = APILocator.getHTMLPageAssetAPI()
                .getTemplate(htmlPage, !mode.showLive);

        final TemplateLayout templateLayout = template != null && template.isDrawed()
                ? DotTemplateTool.themeLayout(template.getInode()) : null;

        for (final String containerId : pageContents.rowKeySet()) {

            final Container container = this.getContainer(live, containerId,
                    APILocator.getHostAPI().find(htmlPage.getHost(), user, false));

            if (container == null) {
                continue;
            }

            final List<ContainerStructure> containerStructures =
                    APILocator.getContainerAPI().getContainerStructures(container);
            this.addPermissions(container, user);

            final Map<String, List<Map<String, Object>>> contentMaps = Maps.newLinkedHashMap();
            final Map<String, List<String>> containerUuidPersona     = Maps.newHashMap();
            for (final String uniqueId : pageContents.row(containerId).keySet()) {

                final String uniqueUUIDForRender = needParseContainerPrefix(container, uniqueId,
                        templateLayout) ?
                        ParseContainer.getDotParserContainerUUID(uniqueId) : uniqueId;

                if (ContainerUUID.UUID_DEFAULT_VALUE.equals(uniqueId)) {
                    continue;
                }

                final Collection<PersonalizedContentlet> personalizedContentletSet =
                        pageContents.get(containerId, uniqueId);
                final List<Map<String, Object>> personalizedContentletMap= Lists.newArrayList();
                int   contentletIncludedCount = 1;

                for (final PersonalizedContentlet personalizedContentlet : personalizedContentletSet) {

                    final Contentlet contentlet = this.getContentlet(personalizedContentlet, mode,
                            htmlPage.getLanguageId(), user);

                    if (contentlet == null) {

                        continue;
                    }

                    if (container.getMaxContentlets() < contentletIncludedCount) {

                        Logger.debug(this, ()-> "Contentlet: "          + contentlet.getIdentifier()
                                + ", has been skipped. Max contentlet: "    + container.getMaxContentlets()
                                + ", has been overcome for the container: " + containerId);
                        continue;
                    }

                    containerUuidPersona
                            .computeIfAbsent(
                                    containerId + uniqueUUIDForRender + personalizedContentlet.getPersonalization(),
                                    k -> Lists.newArrayList())
                            .add(personalizedContentlet.getContentletId());
//                    contextMap.put("EDIT_CONTENT_PERMISSION" + contentlet.getIdentifier(),
//                            permissionAPI.doesUserHavePermission(contentlet, PERMISSION_WRITE, user));

                    // todo check if needed
//                    this.widgetPreExecute(contentlet);
//                    this.addAccrueTags(contentlet);

                    if (personalizedContentlet.getPersonalization().equals(includeContentFor)) {

                        final Map<String, Object> contentPrintableMap = Try
                                .of(() -> ContentletUtil.getContentPrintableMap(user, contentlet))
                                .onFailure(f -> Logger.warn(this.getClass(), f.getMessage())).getOrNull();
                        if (contentPrintableMap == null) {

                            continue;
                        }

                        contentPrintableMap.put("contentType", contentlet.getContentType().variable());
                        personalizedContentletMap.add(contentPrintableMap);
                        contentletIncludedCount++;
                    }
                }

                contentMaps.put(CONTAINER_UUID_PREFIX + uniqueUUIDForRender, personalizedContentletMap);
            }

//            for (final Map.Entry<String, List<String>> entry : containerUuidPersona.entrySet()) {
//
//                contextMap.put("contentletList" + entry.getKey(), entry.getValue());
//                contextMap.put("totalSize"      + entry.getKey(), entry.getValue().size());
//            }

            raws.add(new ContainerRaw(container, containerStructures, contentMaps));
        }

        return raws;
    }

    private boolean isLive(final HttpServletRequest request, final PageMode mode) {
        return (request == null || request.getSession(false) == null
                || request.getSession().getAttribute("tm_date") == null) && mode.showLive;
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

    private Container getContainer(final boolean live, final String containerId, final Host site) throws DotSecurityException, DotDataException {
        Container container;
        final WorkingContainerFinderByIdOrPathStrategyResolver strategyResolver =
                WorkingContainerFinderByIdOrPathStrategyResolver.getInstance();
        final Optional<ContainerFinderByIdOrPathStrategy> strategy = strategyResolver.get(containerId);
        final ContainerFinderByIdOrPathStrategy workingStrategy = strategy.isPresent() ? strategy.get() : strategyResolver.getDefaultStrategy();
        final Supplier<Host> resourceHostSupplier = () -> site;

        try {
            if (live) {

                container = this.getLiveContainerById(containerId, site);
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
        return container;
    }

    private void addPermissions(final Container container, final User user) throws DotDataException {

        final boolean hasWritePermissionOnContainer = permissionAPI.doesUserHavePermission(container, PERMISSION_WRITE, user, false)
                && APILocator.getPortletAPI().hasContainerManagerRights(user);
        final boolean hasReadPermissionOnContainer  = permissionAPI.doesUserHavePermission(container, PERMISSION_READ,  user, false);
//        contextMap.put("EDIT_CONTAINER_PERMISSION" + container.getIdentifier(), hasWritePermissionOnContainer);

        if (Config.getBooleanProperty("SIMPLE_PAGE_CONTENT_PERMISSIONING", true)) {

//            contextMap.put("USE_CONTAINER_PERMISSION" + container.getIdentifier(), true);
        } else {

//            contextMap.put("USE_CONTAINER_PERMISSION" + container.getIdentifier(), hasReadPermissionOnContainer);
        }
    }

    /* Check if we want to accrue the tags associated to each contentlet on
     * this page
     */
//    private void addAccrueTags(final Contentlet contentlet) throws DotDataException {
//
//        if (Config.getBooleanProperty("ACCRUE_TAGS_IN_CONTENTS_ON_PAGE", false)) {
//
//            // Search for the tags associated to this contentlet inode
//            final List<Tag> contentletFoundTags = APILocator.getTagAPI().getTagsByInode(contentlet.getInode());
//            if (contentletFoundTags != null) {
//                this.pageFoundTags.addAll(contentletFoundTags);
//            }
//        }
//    }

    private boolean needParseContainerPrefix(final Container container, final String uniqueId,
            final TemplateLayout templateLayout) {
        String containerIdOrPath = null;

        if (FileAssetContainerUtil.getInstance().isFileAssetContainer(container)) {
            containerIdOrPath = FileAssetContainerUtil.getInstance().getFullPath((FileAssetContainer) container);
        } else {
            containerIdOrPath = container.getIdentifier();
        }

        return !ParseContainer.isParserContainerUUID(uniqueId) &&
                (templateLayout == null || !templateLayout.existsContainer(containerIdOrPath, uniqueId));
    }

    private Contentlet getContentlet(final PersonalizedContentlet personalizedContentlet,
            final PageMode mode, final long languageId, final User user) {

        return Config.getBooleanProperty("DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE", false)?
                getContentletOrFallback(personalizedContentlet, mode, languageId, user)
                : getSpecificContentlet(personalizedContentlet, mode, languageId, user);
    }

    private Contentlet getSpecificContentlet(final PersonalizedContentlet personalizedContentlet,
            final PageMode mode, final long requestedLanguage, final User user) {
        try {

            long languageId = this.resolveLanguageId().isPresent() ? this.resolveLanguageId().get()
                    : requestedLanguage;

            return APILocator.getContentletAPI().findContentletByIdentifier
                    (personalizedContentlet.getContentletId(), mode.showLive, languageId,
                            user, mode.respectAnonPerms);
        } catch (final DotContentletStateException e) {
            // Expected behavior, DotContentletState Exception is used for flow control
            return null;
        } catch (Exception e) {
            throw new DotStateException(e);
        }
    }

    private Contentlet getContentletOrFallback(final PersonalizedContentlet personalizedContentlet,
            final PageMode mode, final long languageId, final User user) {
        try {

            final Optional<Contentlet> contentletOpt = APILocator.getContentletAPI().findContentletByIdentifierOrFallback
                    (personalizedContentlet.getContentletId(), mode.showLive, languageId, user, mode.respectAnonPerms);

            final Contentlet contentlet = contentletOpt.isPresent()
                    ? contentletOpt.get() : APILocator.getContentletAPI()
                    .findContentletByIdentifierAnyLanguage(personalizedContentlet.getContentletId());

            return contentlet;
        } catch (final DotContentletStateException e) {
            // Expected behavior, DotContentletState Exception is used for flow control
            return null;
        } catch (Exception e) {
            throw new DotStateException(e);
        }
    }

    private Optional<Long> resolveLanguageId () {

        final HttpServletRequest request = HttpServletRequestThreadLocal.INSTANCE.getRequest();
        if (null != request) {

            final Language currentLanguage =
                    WebAPILocator.getLanguageWebAPI().getLanguage(request);
            if (null != currentLanguage) {

                return Optional.of(currentLanguage.getId());
            }
        }

        return Optional.empty();
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

    private Container getLiveContainerById(final String containerId, final Host site) throws DotSecurityException, DotDataException {

        final LiveContainerFinderByIdOrPathStrategyResolver strategyResolver =
                LiveContainerFinderByIdOrPathStrategyResolver.getInstance();
        final Optional<ContainerFinderByIdOrPathStrategy> strategy = strategyResolver.get(containerId);
        final ContainerFinderByIdOrPathStrategy liveStrategy = strategy.isPresent() ? strategy.get() : strategyResolver.getDefaultStrategy();
        final Supplier<Host> resourceHostSupplier = () -> site;


        Container container = null;
        try {

            container =
                    liveStrategy.apply(containerId, APILocator.systemUser(), false, resourceHostSupplier);
        } catch (NotFoundInDbException e) {

            container = null;
        }

        return container;
    }

    private Container geContainerById(final String containerIdOrPath, final User user, final Template template,
            final Optional<ContainerFinderByIdOrPathStrategy> strategy,
            final ContainerFinderByIdOrPathStrategy defaultContainerFinderByIdOrPathStrategy) throws NotFoundInDbException {

        final Supplier<Host> resourceHostSupplier = Sneaky
                .sneaked(() -> APILocator.getTemplateAPI().getTemplateHost(template));

        return strategy.isPresent() ?
                strategy.get().apply(containerIdOrPath, user, false, resourceHostSupplier) :
                defaultContainerFinderByIdOrPathStrategy.apply(containerIdOrPath, user, false, resourceHostSupplier);
    }

    public String getPageHtml(
            final PageContext context,
            final HttpServletRequest request,
            final HttpServletResponse response)
                throws DotSecurityException, DotDataException {

        final Host host = this.hostWebAPI.getCurrentHost(request, context.getUser());
        final HTMLPageUrl htmlPageUrl = getHtmlPageAsset(context, host, request);
        final HTMLPageAsset page = htmlPageUrl.getHTMLPage();

        return new HTMLPageAssetRenderedBuilder()
                .setHtmlPageAsset(page)
                .setUser(context.getUser())
                .setRequest(request)
                .setResponse(response)
                .setSite(host)
                .setURLMapper(htmlPageUrl.getPageUrlMapper())
                .setLive(htmlPageUrl.hasLive())
                .getPageHTML(context.getPageMode());
    }

    private HTMLPageUrl getHtmlPageAsset(final PageContext context, final Host host, final HttpServletRequest request)
            throws DotDataException, DotSecurityException {

        Optional<HTMLPageUrl> htmlPageUrlOptional = findPageByContext(host, context);

        if (!htmlPageUrlOptional.isPresent()) {
            htmlPageUrlOptional = findByURLMap(context, host, request);
        }

        if(!htmlPageUrlOptional.isPresent()){
            throw new HTMLPageAssetNotFoundException(context.getPageUri());
        }

        final HTMLPageUrl htmlPageUrl = htmlPageUrlOptional.get();
        checkPagePermission(context, htmlPageUrl.htmlPage);

        return htmlPageUrl;
    }

    private void checkPagePermission(final PageContext context, final IHTMLPage htmlPageAsset)
            throws DotDataException, DotSecurityException {

        final boolean doesUserHavePermission = this.permissionAPI.doesUserHavePermission(
                htmlPageAsset,
                PermissionLevel.READ.getType(),
                context.getUser(),
                context.getPageMode().respectAnonPerms);

        if (!doesUserHavePermission) {
            final String message = String.format("User: %s does not have permissions %s for object %s",
                    context.getUser(),
                    PermissionLevel.READ, htmlPageAsset);
            throw new DotSecurityException(message);
        }
    }

    private Optional<HTMLPageUrl> findPageByContext(final Host host, final PageContext context)
            throws DotDataException, DotSecurityException {

        final User user = context.getUser();
        final String uri = context.getPageUri();
        final PageMode mode = context.getPageMode();
        final String pageUri = (UUIDUtil.isUUID(uri) ||( uri.length()>0 && '/' == uri.charAt(0))) ? uri : ("/" + uri);
        final HTMLPageAsset htmlPageAsset = (HTMLPageAsset) (UUIDUtil.isUUID(pageUri) ?
                this.htmlPageAssetAPI.findPage(pageUri, user, mode.respectAnonPerms) :
                getPageByUri(mode, host, pageUri));

        return Optional.ofNullable(htmlPageAsset == null ? null : new HTMLPageUrl(htmlPageAsset));
    }

    private Optional<HTMLPageUrl> findByURLMap(
            final PageContext context,
            final Host host,
            final HttpServletRequest request)
                throws DotSecurityException, DotDataException {

        final Language language = this.getCurrentLanguage(request);

        final Optional<URLMapInfo> urlMapInfoOptional = this.urlMapAPIImpl.processURLMap(
                UrlMapContextBuilder.builder()
                    .setHost(host)
                    .setLanguageId(language.getId())
                    .setMode(context.getPageMode())
                    .setUri(context.getPageUri())
                    .setUser(context.getUser())
                    .build()
        );

        if (urlMapInfoOptional.isPresent()) {
            final URLMapInfo urlMapInfo = urlMapInfoOptional.get();
            request.setAttribute(WebKeys.WIKI_CONTENTLET, urlMapInfo.getContentlet().getIdentifier());
            request.setAttribute(WebKeys.WIKI_CONTENTLET_INODE, urlMapInfo.getContentlet().getInode());
            request.setAttribute(WebKeys.WIKI_CONTENTLET_URL, context.getPageUri());
            request.setAttribute(WebKeys.CLICKSTREAM_IDENTIFIER_OVERRIDE, urlMapInfo.getContentlet().getIdentifier());
            request.setAttribute(Constants.CMS_FILTER_URI_OVERRIDE, context.getPageUri());

            return Optional.of(new HTMLPageUrl(
                    (HTMLPageAsset) getPageById(context.getPageMode(), urlMapInfo.getIdentifier().getId()),
                    urlMapInfo

            ));
        } else {
            return Optional.empty();
        }
    }

    private IHTMLPage getPageByUri(final PageMode mode, final Host host, final String pageUri)
            throws DotDataException, DotSecurityException {

        final HttpServletRequest request = HttpServletRequestThreadLocal.INSTANCE.getRequest();
        final Language defaultLanguage = this.languageAPI.getDefaultLanguage();
        final Language language = this.getCurrentLanguage(request);

        IHTMLPage htmlPage = this.htmlPageAssetAPI.getPageByPath(pageUri, host, language.getId(),
                mode.showLive);

        if (htmlPage == null && !defaultLanguage.equals(language)
                && APILocator.getLanguageAPI().canDefaultPageToDefaultLanguage()) {

            htmlPage = this.htmlPageAssetAPI.getPageByPath(pageUri, host, defaultLanguage.getId(),
                    mode.showLive);
        }

        return htmlPage;
    }

    private IHTMLPage getPageById(final PageMode mode, final String id)
            throws DotDataException, DotSecurityException {

        final HttpServletRequest request = HttpServletRequestThreadLocal.INSTANCE.getRequest();
        final Language language = request != null ? this.getCurrentLanguage(request) : this.languageAPI.getDefaultLanguage();

        return this.htmlPageAssetAPI.findByIdLanguageFallback(id, language.getId(), mode.showLive, userAPI.getSystemUser(),
                mode.respectAnonPerms);
    }

    private Language getCurrentLanguage(final HttpServletRequest request) {
        return request != null ? this.languageWebAPI.getLanguage(request) : this.languageAPI.getDefaultLanguage();
    }

    public static class HTMLPageUrl {
        private URLMapInfo urlMapInfo;
        private HTMLPageAsset htmlPage;

        private HTMLPageUrl(final HTMLPageAsset htmlPage, final URLMapInfo urlMapInfo) {
            this.htmlPage = htmlPage;
            this.urlMapInfo = urlMapInfo;
        }

        private HTMLPageUrl(final HTMLPageAsset htmlPage) {
            this(htmlPage, null);
        }

        public boolean hasLive() {
            try {
                return urlMapInfo != null ? urlMapInfo.getContentlet().isLive() : this.htmlPage.hasLiveVersion();
            } catch(DotDataException | DotSecurityException e) {
                throw new DotRuntimeException(e);
            }
        }

        public URLMapInfo getUrlMapInfo() {
            return urlMapInfo;
        }

        public String getPageUrl() {
            return htmlPage.getPageUrl();
        }

        public String getPageUrlMapper() throws DotDataException {
            return urlMapInfo != null ? urlMapInfo.getUrlMapped() : htmlPage.getURI();
        }

        public HTMLPageAsset getHTMLPage() {
            return htmlPage;
        }

    }
    
    private void fireRulesOnPage(IHTMLPage page,  HttpServletRequest request, HttpServletResponse response) {
      final boolean fireRules =Try.of(()->Boolean.valueOf(request.getParameter("fireRules"))).getOrElse(false);
      
      if(fireRules) {
        RulesEngine.fireRules(request,  response, page, FireOn.EVERY_PAGE);
      }
    }

    private Visitor getVisitor(final HttpServletRequest request) {
        final Optional<Visitor> visitor = APILocator.getVisitorAPI().getVisitor(request, false);
        return visitor.orElse(null);
    }

    private IPersona getCurrentPersona(final HttpServletRequest request) {
        final Optional<Visitor> visitor = APILocator.getVisitorAPI().getVisitor(request);
        return visitor.isPresent() && visitor.get().getPersona() != null ? visitor.get().getPersona() : null;
    }

    private boolean isPersonalized (final IPersona persona, final Set<String> pagePersonalizationSet) {

        return null != persona && pagePersonalizationSet.contains
                (Persona.DOT_PERSONA_PREFIX_SCHEME + StringPool.COLON + persona.getKeyTag());
    }
}
