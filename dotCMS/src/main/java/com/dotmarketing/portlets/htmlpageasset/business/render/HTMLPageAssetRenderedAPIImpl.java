package com.dotmarketing.portlets.htmlpageasset.business.render;

import com.dotcms.api.web.HttpServletRequestThreadLocal;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
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
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI;
import com.dotmarketing.portlets.htmlpageasset.business.render.page.HTMLPageAssetRenderedBuilder;
import com.dotmarketing.portlets.htmlpageasset.business.render.page.PageView;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.rules.business.RulesEngine;
import com.dotmarketing.portlets.rules.model.Rule.FireOn;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UUIDUtil;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;

import io.vavr.control.Try;

import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * {@link HTMLPageAssetRenderedAPI} implementation
 */
public class HTMLPageAssetRenderedAPIImpl implements HTMLPageAssetRenderedAPI {

    private final HostWebAPI hostWebAPI;
    private final HTMLPageAssetAPI htmlPageAssetAPI;
    private final LanguageAPI languageAPI;
    private final HostAPI hostAPI;
    private final PermissionAPI permissionAPI;
    private final UserAPI userAPI;
    private final VersionableAPI versionableAPI;
    private final URLMapAPIImpl urlMapAPIImpl;
    private final LanguageWebAPI languageWebAPI;

    public HTMLPageAssetRenderedAPIImpl(){
        this(
            APILocator.getPermissionAPI(),
            APILocator.getUserAPI(),
            WebAPILocator.getHostWebAPI(),
            APILocator.getLanguageAPI(),
            APILocator.getHTMLPageAssetAPI(),
            APILocator.getVersionableAPI(),
            APILocator.getHostAPI(),
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
            final VersionableAPI versionableAPI,
            final HostAPI hostAPI,
            final URLMapAPIImpl urlMapAPIImpl,
            final LanguageWebAPI languageWebAPI
    ){

        this.permissionAPI = permissionAPI;
        this.userAPI = userAPI;
        this.hostWebAPI = hostWebAPI;
        this.languageAPI = languageAPI;
        this.htmlPageAssetAPI = htmlPageAssetAPI;
        this.versionableAPI = versionableAPI;
        this.hostAPI = hostAPI;
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

        final Host host = resolveSite(context, request);
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

        final Host host = resolveSite(context, request);
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
                .setURLMapper(htmlPageUrl.pageUrlMapper)
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

            final Host host = this.resolveSite(
                    PageContextBuilder.builder()
                            .setUser(systemUser)
                            .setPageMode(PageMode.PREVIEW_MODE)
                            .build(),
                    request);

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

    public String getPageHtml(
            final PageContext context,
            final HttpServletRequest request,
            final HttpServletResponse response)
                throws DotSecurityException, DotDataException {

        final Host host = resolveSite(context, request);
        final IHTMLPage page = getHtmlPageAsset(context, host, request).getHTMLPage();

        return new HTMLPageAssetRenderedBuilder()
                .setHtmlPageAsset(page)
                .setUser(context.getUser())
                .setRequest(request)
                .setResponse(response)
                .setSite(host)
                .getPageHTML();
    }

    private HTMLPageUrl getHtmlPageAsset(
            final PageContext context,
            final Host host,
            final HttpServletRequest request)
                throws DotDataException, DotSecurityException {

        HTMLPageUrl htmlPageUrl = null;
        IHTMLPage htmlPageAsset = findPageByContext(host, context);

        if (htmlPageAsset == null){
            htmlPageUrl   = findByURLMap(context, host, request);
            htmlPageAsset = getPageByUri(context.getPageMode(), host, htmlPageUrl.getPageUrl());
        } else {
            htmlPageUrl   = new HTMLPageUrl(htmlPageAsset);
        }

        htmlPageUrl.setHTMLPage((HTMLPageAsset) htmlPageAsset);

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

        return htmlPageUrl;
    }

    private IHTMLPage findPageByContext(final Host host, final PageContext context)
            throws DotDataException, DotSecurityException {

        final User user = context.getUser();
        final String uri = context.getPageUri();
        final PageMode mode = context.getPageMode();
        final String pageUri = (UUIDUtil.isUUID(uri) ||( uri.length()>0 && '/' == uri.charAt(0))) ? uri : ("/" + uri);

        return UUIDUtil.isUUID(pageUri) ?
                this.htmlPageAssetAPI.findPage(pageUri, user, mode.respectAnonPerms) :
                getPageByUri(mode, host, pageUri);
    }

    private HTMLPageUrl findByURLMap(
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

        if (!urlMapInfoOptional.isPresent()) {
            throw new HTMLPageAssetNotFoundException(context.getPageUri());
        } else {
            final URLMapInfo urlMapInfo = urlMapInfoOptional.get();
            request.setAttribute(WebKeys.WIKI_CONTENTLET, urlMapInfo.getContentlet().getIdentifier());
            request.setAttribute(WebKeys.WIKI_CONTENTLET_INODE, urlMapInfo.getContentlet().getInode());
            request.setAttribute(WebKeys.WIKI_CONTENTLET_URL, context.getPageUri());
            request.setAttribute(WebKeys.CLICKSTREAM_IDENTIFIER_OVERRIDE, urlMapInfo.getContentlet().getIdentifier());
            request.setAttribute(Constants.CMS_FILTER_URI_OVERRIDE, urlMapInfo.getIdentifier().getURI());

            return new HTMLPageUrl (urlMapInfo.getIdentifier().getURI(), context.getPageUri(), urlMapInfo.getContentlet().isLive());
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

    private Language getCurrentLanguage(final HttpServletRequest request) {
        return request != null ? this.languageWebAPI.getLanguage(request) : this.languageAPI.getDefaultLanguage();
    }

    private Host resolveSite(final PageContext context, final HttpServletRequest request)
            throws DotDataException, DotSecurityException {

        final User user = context.getUser();
        final PageMode mode = context.getPageMode();

        Optional<Host> optionalSite = this.getHostFromRequest(request, user, mode);

        if (!optionalSite.isPresent()) {
            optionalSite = this.getHostFromSession(request, user, mode);
        }

        return optionalSite.isPresent() ? optionalSite.get()
                : this.hostAPI.resolveHostName(request.getServerName(), user, mode.respectAnonPerms) ;

    }

    private Optional<Host> getHostFromSession(final HttpServletRequest request, final User user, final PageMode mode)
            throws DotSecurityException, DotDataException {
        final Object hostId = request.getSession().getAttribute(WebKeys.CMS_SELECTED_HOST_ID);

        if(mode.isAdmin && hostId !=null) {
            final Host host = this.hostAPI.find(hostId.toString(), user, mode.respectAnonPerms);
            return Optional.ofNullable(host);
        } else {
            return Optional.empty();
        }
    }

    private Optional<Host> getHostFromRequest(final HttpServletRequest request, final User user, final PageMode mode)
            throws DotSecurityException, DotDataException {

        final String hostId = request.getParameter("host_id");

        if (null != hostId) {
            return Optional.ofNullable(this.hostAPI.find(hostId, user, mode.respectAnonPerms));
        }

        final String hostName = request.getParameter(Host.HOST_VELOCITY_VAR_NAME);

        if (null != hostName) {
            return this.hostWebAPI.resolveHostNameWithoutDefault(hostName, user, mode.respectAnonPerms);
        }

        return Optional.empty();
    }

    public class HTMLPageUrl {
        private String pageUrl;
        private String pageUrlMapper;
        private HTMLPageAsset htmlPage;
        private Boolean hasLive = null;

        public HTMLPageUrl(final String pageUrl, final String pageUrlMapper, final Boolean hasLive) {
            this.pageUrl = pageUrl;
            this.pageUrlMapper = pageUrlMapper;
            this.hasLive = hasLive;
        }

        public HTMLPageUrl(final IHTMLPage htmlPage) {
            this(htmlPage.getPageUrl(), null, null);
            this.setHTMLPage((HTMLPageAsset) htmlPage);
        }

        public boolean hasLive() {
            try {
                return hasLive == null ? this.htmlPage.hasLiveVersion() : this.hasLive;
            } catch(DotDataException e) {
                throw new DotRuntimeException(e);
            }
        }

        public String getPageUrl() {
            return pageUrl;
        }

        public String getPageUrlMapper() {
            return pageUrlMapper;
        }

        public IHTMLPage getHTMLPage() {
            return htmlPage;
        }

        public void setHTMLPage(final HTMLPageAsset htmlPage) {
            this.htmlPage = htmlPage;
        }
    }
    
    private void fireRulesOnPage(IHTMLPage page,  HttpServletRequest request, HttpServletResponse response) {
      final boolean fireRules =Try.of(()->Boolean.valueOf(request.getParameter("fireRules"))).getOrElse(false);
      
      if(fireRules) {
        RulesEngine.fireRules(request,  response, page, FireOn.EVERY_PAGE);
      }
    }
}
