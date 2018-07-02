package com.dotmarketing.portlets.htmlpageasset.business.render;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.cms.login.LoginServiceAPI;
import com.dotcms.rendering.velocity.services.VelocityResourceKey;
import com.dotcms.rendering.velocity.viewtools.DotTemplateTool;
import com.dotmarketing.beans.ContainerStructure;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.*;
import com.dotmarketing.business.web.HostWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.business.ContainerAPI;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI;
import com.dotmarketing.portlets.htmlpageasset.business.render.page.HTMLPageAssetInfo;
import com.dotmarketing.portlets.htmlpageasset.business.render.page.HTMLPageAssetRendered;
import com.dotmarketing.portlets.htmlpageasset.business.render.page.HTMLPageAssetRenderedBuilder;
import com.dotmarketing.portlets.htmlpageasset.business.render.page.PageView;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.templates.business.TemplateAPI;
import com.dotmarketing.portlets.templates.design.bean.TemplateLayout;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.*;
import com.liferay.portal.model.User;
import org.apache.velocity.context.Context;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * {@link HTMLPageAssetRenderedAPI} implementation
 */
public class HTMLPageAssetRenderedAPIImpl implements HTMLPageAssetRenderedAPI {

    private final HostWebAPI hostWebAPI = WebAPILocator.getHostWebAPI();
    private final HTMLPageAssetAPI htmlPageAssetAPI = APILocator.getHTMLPageAssetAPI();
    private final LanguageAPI languageAPI = APILocator.getLanguageAPI();
    private final HostAPI hostAPI = APILocator.getHostAPI();
    private final PermissionAPI permissionAPI = APILocator.getPermissionAPI();
    private final UserAPI userAPI = APILocator.getUserAPI();
    private final  LoginServiceAPI loginServiceAPI = APILocator.getLoginServiceAPI();

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

        final PageMode pageMode = mode != null ? mode : this.getDefaultPageMode(request, pageUri);
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

    private PageMode getDefaultPageMode(final HttpServletRequest request, final String pageUri) {
        try {
            User systemUser = userAPI.getSystemUser();

            final PageMode mode = PageMode.PREVIEW_MODE;
            Host host = this.resolveSite(request, systemUser, mode);
            final HTMLPageAsset htmlPageAsset = getHtmlPageAsset(systemUser, mode, host, pageUri);

            final ContentletVersionInfo info = APILocator.getVersionableAPI().
                    getContentletVersionInfo(htmlPageAsset.getIdentifier(), htmlPageAsset.getLanguageId());

            return loginServiceAPI.getLoggedInUser().getUserId().equals(info.getLockedBy())
                    ? PageMode.EDIT_MODE : mode;
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

    private HTMLPageAsset getHtmlPageAsset(final User user, final String uri, final PageMode mode, final Host host)
            throws DotDataException, DotSecurityException {
        final HTMLPageAsset htmlPageAsset = getHtmlPageAsset(user, mode, host, uri);

        if (htmlPageAsset == null){
            throw new HTMLPageAssetNotFoundException(uri);
        } else  {
            final boolean doesUserHavePermission = this.permissionAPI.doesUserHavePermission(htmlPageAsset, PermissionLevel.READ.getType(),
                    user, false);

            if (!doesUserHavePermission) {
                final String message = String.format("User: %s does not have permissions %s for object %s", user,
                        PermissionLevel.READ, htmlPageAsset);
                throw new DotSecurityException(message);
            }
        }

        return htmlPageAsset;
    }

    private HTMLPageAsset getHtmlPageAsset(User user, PageMode mode, Host host, String uri) throws DotDataException, DotSecurityException {
        final String pageUri = (UUIDUtil.isUUID(uri) ||( uri.length()>0 && '/' == uri.charAt(0))) ? uri : ("/" + uri);

        return (UUIDUtil.isUUID(pageUri)) ?
                (HTMLPageAsset) this.htmlPageAssetAPI.findPage(pageUri, user, mode.respectAnonPerms) :
                (HTMLPageAsset) getPageByUri(mode, host, pageUri);
    }

    private IHTMLPage getPageByUri(final PageMode mode, final Host host, final String pageUri) throws DotDataException, DotSecurityException {
        final HttpServletRequest request = HttpServletRequestThreadLocal.INSTANCE.getRequest();
        final Language defaultLanguage = this.languageAPI.getDefaultLanguage();
        final Language language = request != null ?
                WebAPILocator.getLanguageWebAPI().getLanguage(request) : defaultLanguage;

        final IHTMLPage htmlPage = this.htmlPageAssetAPI.getPageByPath(pageUri, host, language.getId(), mode.showLive);

        return htmlPage != null || defaultLanguage.equals(language)? htmlPage :
                this.htmlPageAssetAPI.getPageByPath(pageUri, host, defaultLanguage.getId(), mode.showLive);
    }

    private Host resolveSite(final HttpServletRequest request, final User user, final PageMode mode) throws DotDataException, DotSecurityException {
        final String siteName = null == request.getParameter(Host.HOST_VELOCITY_VAR_NAME) ?
                request.getServerName() : request.getParameter(Host.HOST_VELOCITY_VAR_NAME);
        Host site = this.hostWebAPI.resolveHostName(siteName, user, mode.respectAnonPerms);

        if(mode.isAdmin && request.getSession().getAttribute( com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID )!=null) {
            site = this.hostAPI.find(request.getSession().getAttribute( com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID ).toString(), user, mode.respectAnonPerms);
        }
        return site;

    }
}
