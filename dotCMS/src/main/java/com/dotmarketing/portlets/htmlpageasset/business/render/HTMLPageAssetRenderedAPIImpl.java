package com.dotmarketing.portlets.htmlpageasset.business.render;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.visitor.domain.Visitor;
import com.dotmarketing.beans.Host;
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
import com.dotmarketing.filters.Constants;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
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
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UUIDUtil;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;

import com.liferay.util.StringPool;
import io.vavr.control.Try;

import java.util.Optional;
import java.util.Set;
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
