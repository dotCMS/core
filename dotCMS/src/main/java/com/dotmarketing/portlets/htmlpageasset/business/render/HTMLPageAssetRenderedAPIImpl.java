package com.dotmarketing.portlets.htmlpageasset.business.render;

import com.dotcms.rendering.velocity.services.VelocityResourceKey;
import com.dotcms.rendering.velocity.viewtools.DotTemplateTool;
import com.dotmarketing.beans.ContainerStructure;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.*;
import com.dotmarketing.business.web.HostWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.business.ContainerAPI;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI;
import com.dotmarketing.portlets.htmlpageasset.business.render.page.HTMLPageAssetRendered;
import com.dotmarketing.portlets.htmlpageasset.business.render.page.HTMLPageAssetRenderedBuilder;
import com.dotmarketing.portlets.htmlpageasset.business.render.page.PageView;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
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
                                     final User user, final String uri, PageMode mode)
            throws DotSecurityException, DotDataException {

        final Host host = resolveSite(request, user);
        final HTMLPageAsset page = getHtmlPageAsset(user, uri, mode, host);

        return new HTMLPageAssetRenderedBuilder()
                .setHtmlPageAsset(page)
                .setUser(user)
                .setRequest(request)
                .setResponse(response)
                .setSite(host)
                .build(false);
    }

    @Override
    public PageView getPageRendered(final HttpServletRequest request, final HttpServletResponse response,
                                    final User user, final String pageUri, final PageMode pageMode)
            throws DotDataException, DotSecurityException {

        final Host host = resolveSite(request, user);
        final HTMLPageAsset page = getHtmlPageAsset(user, pageUri, pageMode, host);

        return new HTMLPageAssetRenderedBuilder()
                .setHtmlPageAsset(page)
                .setUser(user)
                .setRequest(request)
                .setResponse(response)
                .setSite(host)
                .build(true);
    }

    @Override
    public PageView getPageRendered(final HttpServletRequest request, final HttpServletResponse response,
                                    final User user, final HTMLPageAsset page, PageMode pageMode)
            throws DotDataException, DotSecurityException {

        final Host host = resolveSite(request, user);

        return new HTMLPageAssetRenderedBuilder()
                .setHtmlPageAsset(page)
                .setUser(user)
                .setRequest(request)
                .setResponse(response)
                .setSite(host)
                .build(true);
    }

    private HTMLPageAsset getHtmlPageAsset(User user, String uri, PageMode mode, Host host) throws DotDataException, DotSecurityException {
        final String pageUri = (UUIDUtil.isUUID(uri) ||( uri.length()>0 && '/' == uri.charAt(0))) ? uri : ("/" + uri);

        HTMLPageAsset htmlPageAsset = (UUIDUtil.isUUID(pageUri)) ?
                (HTMLPageAsset) this.htmlPageAssetAPI.findPage(pageUri, user, mode.respectAnonPerms) :
                (HTMLPageAsset) this.htmlPageAssetAPI.getPageByPath(pageUri, host, this.languageAPI.getDefaultLanguage().getId(), mode.showLive);

        if (htmlPageAsset == null){
            throw new HTMLPageAssetNotFoundException(pageUri);
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

    private Host resolveSite(final HttpServletRequest request, final User user) throws DotDataException, DotSecurityException {
        final PageMode mode = PageMode.get(request);
        final String siteName = null == request.getParameter(Host.HOST_VELOCITY_VAR_NAME) ?
                request.getServerName() : request.getParameter(Host.HOST_VELOCITY_VAR_NAME);
        Host site = this.hostWebAPI.resolveHostName(siteName, user, mode.respectAnonPerms);

        if(mode.isAdmin && request.getSession().getAttribute( com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID )!=null) {
            site = this.hostAPI.find(request.getSession().getAttribute( com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID ).toString(), user, mode.respectAnonPerms);
        }
        return site;

    }
}
