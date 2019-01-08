package com.dotmarketing.portlets.htmlpageasset.business.render;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.cms.login.LoginServiceAPI;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.*;
import com.dotmarketing.business.web.HostWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI;
import com.dotmarketing.portlets.htmlpageasset.business.render.page.HTMLPageAssetRenderedBuilder;
import com.dotmarketing.portlets.htmlpageasset.business.render.page.PageView;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UUIDUtil;
import com.liferay.portal.model.User;

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

    public HTMLPageAssetRenderedAPIImpl(){
        this(APILocator.getPermissionAPI(), APILocator.getUserAPI(), WebAPILocator.getHostWebAPI(),
                APILocator.getLanguageAPI(), APILocator.getHTMLPageAssetAPI(), APILocator.getVersionableAPI(),
                APILocator.getHostAPI());
    }

    @VisibleForTesting
    public HTMLPageAssetRenderedAPIImpl(final PermissionAPI permissionAPI, final UserAPI userAPI,
                                        final HostWebAPI hostWebAPI, final LanguageAPI languageAPI,
                                        final HTMLPageAssetAPI htmlPageAssetAPI, final VersionableAPI versionableAPI,
                                        final HostAPI hostAPI){

        this.permissionAPI = permissionAPI;
        this.userAPI = userAPI;
        this.hostWebAPI = hostWebAPI;
        this.languageAPI = languageAPI;
        this.htmlPageAssetAPI = htmlPageAssetAPI;
        this.versionableAPI = versionableAPI;
        this.hostAPI = hostAPI;
    }

    /**
     * @param request    The {@link HttpServletRequest} object.
     * @param response   The {@link HttpServletResponse} object.
     * @param user       The {@link User} performing this action.
     * @param uri        The path to the HTML Page whose information will be retrieved.
     * @return The rendered page, i.e., the HTML source code that will be rendered by the browser.
     * @throws DotSecurityException The user does not have the specified permissions to perform
     *                              this action.
     * @throws DotDataException     An error occurred when accessing the data source.
     */
    @Override
    public PageView getPageMetadata(final HttpServletRequest request, final HttpServletResponse response,
                                     final User user, final String uri, final PageMode mode)
            throws DotSecurityException, DotDataException {
        final Host host = resolveSite(request, user, mode);
        final HTMLPageAsset page = getHtmlPageAsset(user, uri, mode, host);

        return new HTMLPageAssetRenderedBuilder()
                .setHtmlPageAsset(page)
                .setUser(user)
                .setRequest(request)
                .setResponse(response)
                .setSite(host)
                .build(false, mode);
    }

    @Override
    public PageView getPageRendered(final HttpServletRequest request, final HttpServletResponse response,
                                    final User user, final String pageUri, final PageMode mode)
            throws DotDataException, DotSecurityException {

        final PageMode pageMode = mode != null ? mode : this.getDefaultEditPageMode(user, request, pageUri);
        PageMode.setPageMode(request, pageMode);

        final Host host = resolveSite(request, user, pageMode);
        final HTMLPageAsset page = getHtmlPageAsset(user, pageUri, pageMode, host);

        return new HTMLPageAssetRenderedBuilder()
                .setHtmlPageAsset(page)
                .setUser(user)
                .setRequest(request)
                .setResponse(response)
                .setSite(host)
                .build(true, pageMode);
    }

    public PageMode getDefaultEditPageMode(final User user, final HttpServletRequest request, final String pageUri) {
        try {
            final User systemUser = userAPI.getSystemUser();

            final Host host = this.resolveSite(request, systemUser, PageMode.PREVIEW_MODE);
            final HTMLPageAsset htmlPageAsset = getHtmlPageAsset(systemUser, pageUri, PageMode.PREVIEW_MODE, host);

            final ContentletVersionInfo info = this.versionableAPI.
                    getContentletVersionInfo(htmlPageAsset.getIdentifier(), htmlPageAsset.getLanguageId());

            return user.getUserId().equals(info.getLockedBy()) ? PageMode.EDIT_MODE
                    : getNotLockDefaultPageMode(htmlPageAsset, user);
        } catch (DotDataException | DotSecurityException e) {
            throw new DotRuntimeException(e);
        }
    }

    @Override
    public PageView getPageRendered(final HttpServletRequest request, final HttpServletResponse response,
                                    final User user, final HTMLPageAsset page, PageMode pageMode)
            throws DotDataException, DotSecurityException {

        final Host host = resolveSite(request, user, pageMode);

        return new HTMLPageAssetRenderedBuilder()
                .setHtmlPageAsset(page)
                .setUser(user)
                .setRequest(request)
                .setResponse(response)
                .setSite(host)
                .build(true, pageMode);
    }

    public String getPageHtml(final HttpServletRequest request, final HttpServletResponse response, final User user,
                              final String uri, final PageMode mode) throws DotSecurityException, DotDataException {
        final Host host = resolveSite(request, user, mode);
        final HTMLPageAsset page = getHtmlPageAsset(user, uri, mode, host);

        return new HTMLPageAssetRenderedBuilder()
                .setHtmlPageAsset(page)
                .setUser(user)
                .setRequest(request)
                .setResponse(response)
                .setSite(host)
                .getPageHTML();
    }

    private PageMode getNotLockDefaultPageMode(final HTMLPageAsset htmlPageAsset, final User user) throws DotDataException {
        return this.permissionAPI.doesUserHavePermission(htmlPageAsset, PermissionLevel.READ.getType(), user, false)
                ? PageMode.PREVIEW_MODE : PageMode.ADMIN_MODE;
    }

    private HTMLPageAsset getHtmlPageAsset(final User user, final String uri, final PageMode mode, final Host host)
            throws DotDataException, DotSecurityException {
        final String pageUri = (UUIDUtil.isUUID(uri) ||( uri.length()>0 && '/' == uri.charAt(0))) ? uri : ("/" + uri);

        final HTMLPageAsset htmlPageAsset = UUIDUtil.isUUID(pageUri) ?
                (HTMLPageAsset) this.htmlPageAssetAPI.findPage(pageUri, user, mode.respectAnonPerms) :
                (HTMLPageAsset) getPageByUri(mode, host, pageUri);

        if (htmlPageAsset == null){
            throw new HTMLPageAssetNotFoundException(uri);
        } else  {
            final boolean doesUserHavePermission = this.permissionAPI.doesUserHavePermission(htmlPageAsset, PermissionLevel.READ.getType(),
                    user, mode.respectAnonPerms);

            if (!doesUserHavePermission) {
                final String message = String.format("User: %s does not have permissions %s for object %s", user,
                        PermissionLevel.READ, htmlPageAsset);
                throw new DotSecurityException(message);
            }
        }

        return htmlPageAsset;
    }

    private IHTMLPage getPageByUri(final PageMode mode, final Host host, final String pageUri) throws DotDataException, DotSecurityException {
        final HttpServletRequest request = HttpServletRequestThreadLocal.INSTANCE.getRequest();
        final Language defaultLanguage = this.languageAPI.getDefaultLanguage();
        final Language language = request != null ?
                WebAPILocator.getLanguageWebAPI().getLanguage(request) : defaultLanguage;

        final IHTMLPage htmlPage = this.htmlPageAssetAPI.getPageByPath(pageUri, host, language.getId(), mode.showLive);

        return htmlPage != null || defaultLanguage.equals(language)? htmlPage :
                (APILocator.getLanguageAPI().canDefaultPageToDefaultLanguage())?
                        this.htmlPageAssetAPI.getPageByPath(pageUri, host, defaultLanguage.getId(), mode.showLive):
                        htmlPage;
    }

    private Host resolveSite(final HttpServletRequest request, final User user, final PageMode mode) throws DotDataException, DotSecurityException {
        
        
        final String hostId = request.getParameter("host_id");
        if (null != hostId) {
            return this.hostAPI.find(hostId, user, mode.respectAnonPerms);
        }
        
        final String siteName = (null == request.getParameter(Host.HOST_VELOCITY_VAR_NAME)) ?
                request.getServerName() : request.getParameter(Host.HOST_VELOCITY_VAR_NAME);
        Host site = this.hostWebAPI.resolveHostName(siteName, user, mode.respectAnonPerms);

        if(mode.isAdmin && request.getSession().getAttribute( com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID )!=null) {
            site = this.hostAPI.find(request.getSession().getAttribute( com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID ).toString(), user, mode.respectAnonPerms);
        }
        return site;

    }
}
