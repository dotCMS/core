package com.dotmarketing.portlets.htmlpageasset.business.render;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.experiments.business.ConfigExperimentUtil;
import com.dotcms.experiments.business.web.ExperimentWebAPI;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.rendering.velocity.services.PageLoader;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.variant.business.web.VariantWebAPI.RenderContext;
import com.dotcms.visitor.domain.Visitor;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.PermissionLevel;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.business.web.HostWebAPI;
import com.dotmarketing.business.web.LanguageWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.cms.urlmap.URLMapAPIImpl;
import com.dotmarketing.cms.urlmap.URLMapInfo;
import com.dotmarketing.cms.urlmap.UrlMapContextBuilder;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.filters.Constants;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI;
import com.dotmarketing.portlets.htmlpageasset.business.render.page.HTMLPageAssetRendered;
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
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UUIDUtil;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import io.vavr.control.Try;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Optional;
import java.util.Set;

/**
 * Implementation class for the {@link HTMLPageAssetRenderedAPI}.
 *
 * @author Freddy Rodriguez
 * @since Apr 12th, 2018
 */
public class HTMLPageAssetRenderedAPIImpl implements HTMLPageAssetRenderedAPI {

    public static final String HTML_HEAD = "<head>";
    private final HostWebAPI hostWebAPI;
    private final HTMLPageAssetAPI htmlPageAssetAPI;
    private final LanguageAPI languageAPI;
    private final PermissionAPI permissionAPI;
    private final UserAPI userAPI;
    private final URLMapAPIImpl urlMapAPIImpl;
    private final LanguageWebAPI languageWebAPI;
    private final ExperimentWebAPI experimentWebAPI;

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

        this.experimentWebAPI = WebAPILocator.getExperimentWebAPI();
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

    @Override
    public PageView getPageMetadata(
            final PageContext context,
            final HttpServletRequest request,
            final HttpServletResponse response)
            throws DotSecurityException, DotDataException {

        final Host host = this.hostWebAPI.getCurrentHost(request, context.getUser());
        final HTMLPageUrl htmlPageUrl = getHtmlPageAsset(context, host, request);

        fireRulesOnPage(htmlPageUrl.getHTMLPage(), request, response);

        final HTMLPageAssetRenderedBuilder htmlPageAssetRenderedBuilder = new HTMLPageAssetRenderedBuilder()
                .setHtmlPageAsset(htmlPageUrl.getHTMLPage())
                .setUser(context.getUser())
                .setRequest(request)
                .setResponse(response)
                .setSite(host)
                .setURLMapper(htmlPageUrl.getPageUrlMapper())
                .setLive(htmlPageUrl.hasLive())
                .setVanityUrl(context.getVanityUrl());
        if (ConfigExperimentUtil.INSTANCE.isExperimentEnabled()) {
            APILocator.getExperimentsAPI()
                    .getRunningExperimentPerPage(htmlPageUrl.getHTMLPage().getIdentifier())
                    .ifPresent(htmlPageAssetRenderedBuilder::setRunningExperiment);
        }

        return htmlPageAssetRenderedBuilder.build(false, context.getPageMode());
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

        final HTMLPageAssetRenderedBuilder htmlPageAssetRenderedBuilder = new HTMLPageAssetRenderedBuilder()
                .setHtmlPageAsset(htmlPageUrl.getHTMLPage())
                .setUser(context.getUser())
                .setRequest(request)
                .setResponse(response)
                .setSite(host)
                .setURLMapper(htmlPageUrl.getPageUrlMapper())
                .setLive(htmlPageUrl.hasLive())
                .setParseJSON(context.isParseJSON())
                .setVanityUrl(context.getVanityUrl());
        if (ConfigExperimentUtil.INSTANCE.isExperimentEnabled()) {
            APILocator.getExperimentsAPI()
                    .getRunningExperimentPerPage(htmlPageUrl.getHTMLPage().getIdentifier())
                    .ifPresent(htmlPageAssetRenderedBuilder::setRunningExperiment);
        }

        return htmlPageAssetRenderedBuilder.build(true, mode);
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
        final String currentVariantId = WebAPILocator.getVariantWebAPI().currentVariantId();
        final Set<String> pagePersonalizationSet  = APILocator.getMultiTreeAPI()
                .getPersonalizationsForPage(htmlpage, currentVariantId);
        final IPersona persona     = this.getCurrentPersona(request);
        final boolean personalized = this.isPersonalized(persona, pagePersonalizationSet);
        final Contentlet device = APILocator.getDeviceAPI().getCurrentDevice(request, user)
                .orElse(null);

        return new ViewAsPageStatus.Builder()
                .setVisitor(getVisitor(request))
                .setLanguage(WebAPILocator.getLanguageWebAPI().getLanguage(request))
                .setDevice(device)
                .setPageMode(pageMode)
                .setPersonalized(personalized)
                .setVariant(currentVariantId)
                .build();
    }

    public String getPageHtml(
            final PageContext context,
            final HttpServletRequest request,
            final HttpServletResponse response)
            throws DotSecurityException, DotDataException {
        Logger.debug(this, "HTMLPageAssetRenderedAPIImpl_getPageHtml Getting the HTML for the context: " + context.toString());

        final Host host = this.hostWebAPI.getCurrentHost(request, context.getUser());
        Logger.debug(this, "HTMLPageAssetRenderedAPIImpl_getPageHtml Host object retrieved from the request is: " + host.getIdentifier());
        final HTMLPageUrl htmlPageUrl = getHtmlPageAsset(context, host, request);
        final HTMLPageAsset page = htmlPageUrl.getHTMLPage();
        Logger.debug(this, "HTMLPageAssetRenderedAPIImpl_getPageHtml HTMLPageUrl: " + htmlPageUrl.toString());

        final String pageHTML = new HTMLPageAssetRenderedBuilder()
                .setHtmlPageAsset(page)
                .setUser(context.getUser())
                .setRequest(request)
                .setResponse(response)
                .setSite(host)
                .setURLMapper(htmlPageUrl.getPageUrlMapper())
                .setLive(htmlPageUrl.hasLive())
                .getPageHTML(context.getPageMode());

        if (context.getPageMode() == PageMode.LIVE && ConfigExperimentUtil.INSTANCE.isExperimentAutoJsInjection()) {
            Logger.debug(this, "HTMLPageAssetRenderedAPIImpl_getPageHtml experiments is running");
            return experimentWebAPI.getCode(host, request)
                    .map(jsCodeToBeInjected -> injectJSCode(pageHTML, jsCodeToBeInjected))
                    .orElse(pageHTML);
        } else {
            Logger.debug(this, "HTMLPageAssetRenderedAPIImpl_getPageHtml Page HTML: " + pageHTML);
            return pageHTML;
        }
    }

    private String injectJSCode(final String pageHTML, final String JsCode) {

        if (StringUtils.containsIgnoreCase(pageHTML, HTML_HEAD)) {
            final int indexOf = pageHTML.toLowerCase().indexOf(HTML_HEAD);
            return pageHTML.substring(0, indexOf + HTML_HEAD.length()) + JsCode + pageHTML.substring(indexOf + 6);
        } else {
            return JsCode + "\n" + pageHTML;
        }
    }

    /**
     * Returns the information of the HTML Page set in the Page Context object.
     *
     * @param context The {@link PageContext} indicating the HTML Page that is being requested.
     * @param host    The Site that the page lives in.
     * @param request The current {@link HttpServletRequest} instance.
     *
     * @return The {@link HTMLPageUrl} containing the HTML Page as a {@link HTMLPageAsset} object.
     *
     * @throws DotDataException               An error occurred when interacting with the data source.
     * @throws DotSecurityException           The User accessing the APIs does not have the required permissions to
     *                                        perform this action.
     * @throws HTMLPageAssetNotFoundException The HTML Page specified in the Page Context object was not found.
     */
    private HTMLPageUrl getHtmlPageAsset(final PageContext context, final Host host, final HttpServletRequest request)
            throws DotDataException, DotSecurityException {
        Logger.debug(this, "--HTMLPageAssetRenderedAPIImpl_getHtmlPageAsset--");

        Optional<HTMLPageUrl> htmlPageUrlOptional = findPageByContext(host, context);

        if (htmlPageUrlOptional.isEmpty()) {
            Logger.debug(this, "HTMLPageAssetRenderedAPIImpl_getHtmlPageAsset htmlPageUrlOptional is Empty trying to find by URL Map");
            htmlPageUrlOptional = findByURLMap(context, host, request);
        }

        if(htmlPageUrlOptional.isEmpty()){
            Logger.debug(this, "HTMLPageAssetRenderedAPIImpl_getHtmlPageAsset htmlPageUrlOptional is Empty throwing HTMLPageAssetNotFoundException");
            throw new HTMLPageAssetNotFoundException(context.getPageUri());
        }

        final HTMLPageUrl htmlPageUrl = htmlPageUrlOptional.get();
        Logger.debug(this, "HTMLPageAssetRenderedAPIImpl_getHtmlPageAsset htmlPageUrl: " + htmlPageUrl.htmlPage.toString());
        checkPagePermission(context, htmlPageUrl.htmlPage);
        Logger.debug(this, "HTMLPageAssetRenderedAPIImpl_getHtmlPageAsset User has permission to access the page");

        return htmlPageUrl;
    }

    /**
     * Verifies whether the {@link User} accessing the HTML Page has permissions to read it or not.
     *
     * @param context       The {@link PageContext} indicating the HTML Page that is being requested.
     * @param htmlPageAsset The {@link IHTMLPage} object.
     *
     * @throws DotDataException     An error occurred when interacting with the data source.
     * @throws DotSecurityException The User accessing the APIs does not have the required permissions to
     *                              perform this action.
     */
    private void checkPagePermission(final PageContext context, final IHTMLPage htmlPageAsset)
            throws DotDataException, DotSecurityException {
        Logger.debug(this, "--HTMLPageAssetRenderedAPIImpl_checkPagePermission--");

        final boolean doesUserHavePermission = this.permissionAPI.doesUserHavePermission(
                htmlPageAsset,
                PermissionLevel.READ.getType(),
                context.getUser(),
                context.getPageMode().respectAnonPerms);

        if (!doesUserHavePermission) {
            final String message = String.format("User: %s does not have permissions %s for page %s",
                    context.getUser(),
                    PermissionLevel.READ, htmlPageAsset.getURI());
            Logger.debug(this, "HTMLPageAssetRenderedAPIImpl_checkPagePermission " + message);
            throw new DotSecurityException(message);
        }
    }

    /**
     * Finds the HTML Page specified in the Page Context object.
     *
     * @param host    The Site that the HTML Page lives in.
     * @param context The {@link PageContext} indicating the HTML Page that is being requested.
     *
     * @return An Optional with the {@link HTMLPageUrl} containing the HTML Page as a {@link HTMLPageAsset} object.
     *
     * @throws DotDataException     An error occurred when interacting with the data source.
     * @throws DotSecurityException The User accessing the APIs does not have the required permissions to perform
     *                              this action.
     */
    private Optional<HTMLPageUrl> findPageByContext(final Host host, final PageContext context)
            throws DotDataException, DotSecurityException {

        final User user = context.getUser();
        final String uri = context.getPageUri();
        final PageMode mode = context.getPageMode();
        final String pageUri = (UUIDUtil.isUUID(uri) ||( uri.length()>0 && '/' == uri.charAt(0))) ? uri : ("/" + uri);
        Logger.debug(this, "HTMLPageAssetRenderedAPIImpl_findPageByContext user: " + user + " uri: " + uri + " mode: " + mode + " host: " + host + " pageUri: " + pageUri);
        final HTMLPageAsset htmlPageAsset = (HTMLPageAsset) (UUIDUtil.isUUID(pageUri) ?
                this.htmlPageAssetAPI.findPage(pageUri, user, mode.respectAnonPerms) :
                getPageByUri(mode, host, pageUri));

        Logger.debug(this, "HTMLPageAssetRenderedAPIImpl_findPageByContext htmlPageAsset: " + (htmlPageAsset == null ? "Not Found" : htmlPageAsset.toString()));
        return Optional.ofNullable(htmlPageAsset == null ? null : new HTMLPageUrl(htmlPageAsset));
    }

    /**
     * Finds the HTML Page specified in the Page Context object when it's being requested via URL Map.
     *
     * @param context The {@link PageContext} indicating the HTML Page that is being requested.
     * @param host    The Site that the page lives in.
     * @param request The current {@link HttpServletRequest} instance.
     *
     * @return An Optional with the {@link HTMLPageUrl} containing the HTML Page as a {@link HTMLPageAsset} object.
     *
     * @throws DotDataException     An error occurred when interacting with the data source.
     * @throws DotSecurityException The User accessing the APIs does not have the required permissions to perform
     * this action.
     */
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
                        .setGraphQL(context.isGraphQL())
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
        Logger.debug(this, "HTMLPageAssetRenderedAPIImpl_getPageByUri pageUri: " + pageUri + " host: " + host + " language: " + language + " mode: " + mode);

        IHTMLPage htmlPage = this.htmlPageAssetAPI.getPageByPath(pageUri, host, language.getId(),
                mode.showLive);

        Logger.debug(this, "HTMLPageAssetRenderedAPIImpl_getPageByUri page found: " + htmlPage);

        if (htmlPage == null && !defaultLanguage.equals(language)
                && APILocator.getLanguageAPI().canDefaultPageToDefaultLanguage()) {
            Logger.debug(this, "HTMLPageAssetRenderedAPIImpl_getPageByUri page not found trying to get the default language");

            htmlPage = this.htmlPageAssetAPI.getPageByPath(pageUri, host, defaultLanguage.getId(),
                    mode.showLive);
        }

        return htmlPage;
    }

    private IHTMLPage getPageById(final PageMode mode, final String id)
            throws DotDataException, DotSecurityException {

        final HttpServletRequest request = HttpServletRequestThreadLocal.INSTANCE.getRequest();
        final Language language = request != null ? this.getCurrentLanguage(request) : this.languageAPI.getDefaultLanguage();

        final RenderContext renderContext = WebAPILocator.getVariantWebAPI()
                .getRenderContextForceLangFallback(language.getId(), id, mode, userAPI.getSystemUser());

        final ContentletVersionInfo contentletVersionInfo = APILocator.getVersionableAPI()
                .getContentletVersionInfo(id, renderContext.getCurrentLanguageId(), renderContext.getCurrentVariantKey())
                .orElseThrow();

        final String inode = mode.showLive ? contentletVersionInfo.getLiveInode() : contentletVersionInfo.getWorkingInode();
        return this.htmlPageAssetAPI.findPage(inode, userAPI.getSystemUser(), false);
    }

    private Language getCurrentLanguage(final HttpServletRequest request) {
        return request != null ? this.languageWebAPI.getLanguage(request) : this.languageAPI.getDefaultLanguage();
    }

    /**
     * Contains information of a given HTML Page and its related URL Map data, if required.
     */
    public static class HTMLPageUrl {
        private final URLMapInfo urlMapInfo;
        private final HTMLPageAsset htmlPage;

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

        //Create toString method
        @Override
        public String toString() {
            return "HTMLPageUrl{" +
                    "urlMapInfo=" + (urlMapInfo != null ? urlMapInfo.toString() : "null") +
                    ", htmlPage=" + (htmlPage != null ? htmlPage.toString() : "null") +
                    ", hasLive=" + hasLive() +
                    '}';
        }

    }

    /**
     * Checks for the existence of e request parameter called {@code fireRules}. If it exists and is {@code true}, the
     * Rules Engine will analyze the incoming HTML Page and will fire its associated Rules as expected.
     *
     * @param page     The {@link IHTMLPage} object.
     * @param request  The current {@link HttpServletRequest} instance.
     * @param response The current {@link HttpServletResponse} instance.
     */
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

    @Override
    public PageLivePreviewVersionBean getPageRenderedLivePreviewVersion (final String  pageId,
                                                                         final User user,
                                                                         final long languageId,
                                                                         final HttpServletRequest  request,
                                                                         final HttpServletResponse response)
            throws DotSecurityException, DotDataException {

        final HTMLPageAsset page   = (HTMLPageAsset) this.htmlPageAssetAPI.
                findByIdLanguageFallback(pageId, languageId, false, user, false);

        if (!this.permissionAPI.doesUserHavePermission(page, PermissionAPI.PERMISSION_EDIT, user)) {

            throw new DotSecurityException("User " + user.getFullName() + " id " + user.getUserId()
                    + " does not have EDIT perms on page :" + page.getIdentifier());
        }

        Logger.debug(this, ()-> "Getting the html for the live and preview version of the page: " + pageId);

        final String pageURI = page.getURI();
        final Identifier pageIdentifier = APILocator.getIdentifierAPI().find(page.getIdentifier());
        new PageLoader().invalidate(page, PageMode.EDIT_MODE, PageMode.PREVIEW_MODE);
        final Host host = this.hostWebAPI.find(pageIdentifier.getHostId(), user, false);

        final HttpServletRequest wrapRequestLiveMode = new DiffMockRequest(request);
        wrapRequestLiveMode.setAttribute(WebKeys.PAGE_MODE_PARAMETER, PageMode.LIVE);

        final String renderLive    = HTMLPageAssetRendered.class.cast(new HTMLPageAssetRenderedBuilder()
                .setHtmlPageAsset(page).setUser(user)
                .setRequest(wrapRequestLiveMode).setResponse(response)
                .setSite(host).setURLMapper(pageURI)
                .setLive(true).build(true, PageMode.LIVE)).getHtml();

        final HttpServletRequest wrapRequestPreviewMode = new DiffMockRequest(request);
        wrapRequestPreviewMode.setAttribute(WebKeys.PAGE_MODE_PARAMETER, PageMode.PREVIEW_MODE);

        final String renderWorking =  HTMLPageAssetRendered.class.cast(new HTMLPageAssetRenderedBuilder()
                .setHtmlPageAsset(page).setUser(user)
                .setRequest(wrapRequestPreviewMode).setResponse(response)
                .setSite(host).setURLMapper(pageURI)
                .setLive(false).build(true, PageMode.PREVIEW_MODE)).getHtml();

        return new PageLivePreviewVersionBean(renderLive, renderWorking);
    }

    private static class DiffMockRequest extends MockAttributeRequest {
        public DiffMockRequest(HttpServletRequest request) {
            super(request);
        }

        @Override
        public String getParameter(String name) {
            if (WebKeys.PAGE_MODE_PARAMETER.equals(name)) {
                return null;
            }

            return super.getParameter(name);
        }


    }
}
