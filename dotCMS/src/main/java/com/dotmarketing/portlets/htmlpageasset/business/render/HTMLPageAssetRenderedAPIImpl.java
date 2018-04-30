package com.dotmarketing.portlets.htmlpageasset.business.render;

import com.dotcms.rendering.velocity.services.PageContextBuilder;
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
    private final VersionableAPI versionableAPI = APILocator.getVersionableAPI();
    private final HostAPI hostAPI = APILocator.getHostAPI();
    private final PermissionAPI permissionAPI = APILocator.getPermissionAPI();
    private final UserAPI userAPI = APILocator.getUserAPI();
    private final ContainerRenderedBuilder containerRenderedBuilder = new ContainerRenderedBuilder();

    @Override
    public HTMLPageAssetRendered getPageRendered(HttpServletRequest request, HttpServletResponse response, User user,
                                               String pageUri, PageMode pageMode)
            throws DotDataException, DotSecurityException, IOException {

        final HTMLPageAsset page = this.getPage(request, user, pageUri, pageMode);

        if (page == null) {
            throw new HTMLPageAssetNotFoundException(pageUri);
        }

        return this.getPageRendered(request, response, user, page, pageMode);
    }

    @Override
    public HTMLPageAssetRendered getPageRendered(final HttpServletRequest request, final HttpServletResponse response,
                                                 final User user, final HTMLPageAsset page, PageMode pageMode)
            throws DotDataException, DotSecurityException, IOException {

        return new HTMLPageAssetRenderedBuilder()
                .setHtmlPageAsset(page)
                .setUser(user)
                .setRequest(request)
                .setResponse(response)
                .setSite(resolveSite(request, user))
                .build();
    }

    /**
     * Returns the metadata of an HTML Page in the system and its associated data structures.
     *
     * @param request  The {@link HttpServletRequest} object.
     * @param response The {@link HttpServletResponse} object.
     * @param user     The {@link User} performing this action.
     * @param uri      The path to the HTML Page whose information will be retrieved.
     * @return The {@link PageView} object containing the metadata of the different objects that
     * make up an HTML Page.
     * @throws DotSecurityException The user does not have the specified permissions to perform
     *                              this action.
     * @throws DotDataException     An error occurred when accessing the data source.
     */
    @Override
    public PageView getPageMetadata(final HttpServletRequest request, final HttpServletResponse
            response, final User user, final String uri, boolean live) throws DotSecurityException,
            DotDataException {

        return getPageMetadata(request, response, user, uri, false, PageMode.get(request));
    }

    /**
     * Returns the rendered version of an HTML Page, i.e., the HTML code that will be rendered in
     * the browser.
     *
     * @param request  The {@link HttpServletRequest} object.
     * @param response The {@link HttpServletResponse} object.
     * @param user     The {@link User} performing this action.
     * @param uri      The path to the HTML Page whose information will be retrieved.
     * @return The {@link PageView} object containing the metadata of the different objects that
     * make up an HTML Page.
     * @throws DotSecurityException The user does not have the specified permissions to perform
     *                              this action.
     * @throws DotDataException     An error occurred when accessing the data source.
     */
    @Override
    public PageView getPageMetadataRendered(final HttpServletRequest request, final HttpServletResponse response,
                                            final User user, final String uri, boolean live) throws DotSecurityException,
            DotDataException {
        return getPageMetadata(request, response, user, uri, true, PageMode.get(request));
    }

    private HTMLPageAsset getPage(final HttpServletRequest request, final User user,
                                 final String uri, final PageMode mode) throws DotSecurityException, DotDataException {

        final Host site = resolveSite(request, user);
        final String pageUri = URLUtils.addSlashIfNeeded(uri);
        final HTMLPageAsset page = (HTMLPageAsset) this.htmlPageAssetAPI.getPageByPath(pageUri,
                site, this.languageAPI.getDefaultLanguage().getId(), mode.respectAnonPerms);

        if (page != null) {
            final boolean doesUserHavePermission = this.permissionAPI.doesUserHavePermission(page, PermissionLevel.READ.getType(),
                    user, false);

            if (!doesUserHavePermission) {
                final String message = String.format("User: %s does not have permissions %s for object %s", user,
                        PermissionLevel.READ, page);
                throw new DotSecurityException(message);
            }
        }

        return page;
    }

    /**
     * @param request    The {@link HttpServletRequest} object.
     * @param response   The {@link HttpServletResponse} object.
     * @param user       The {@link User} performing this action.
     * @param uri        The path to the HTML Page whose information will be retrieved.
     * @param isRendered If the response must include the final render of the page and its
     *                   containers, set to {@code true}. Otherwise, set to {@code false}.
     * @return The rendered page, i.e., the HTML source code that will be rendered by the browser.
     * @throws DotSecurityException The user does not have the specified permissions to perform
     *                              this action.
     * @throws DotDataException     An error occurred when accessing the data source.
     */
    private PageView getPageMetadata(final HttpServletRequest request, final HttpServletResponse response,
                                     final User user, final String uri, final boolean isRendered, PageMode mode)
            throws DotSecurityException, DotDataException {


        final Host site =resolveSite(request, user);
        final String pageUri = (UUIDUtil.isUUID(uri) ||( uri.length()>0 && '/' == uri.charAt(0))) ? uri : ("/" + uri);


        final HTMLPageAsset page = (UUIDUtil.isUUID(pageUri)) ?
                (HTMLPageAsset) this.htmlPageAssetAPI.findPage(pageUri, user, mode.respectAnonPerms) :
                (HTMLPageAsset) this.htmlPageAssetAPI.getPageByPath(pageUri, site, this.languageAPI.getDefaultLanguage().getId(), mode.showLive);

        TemplateLayout layout = null;
        Template template = null;

        final User systemUser = userAPI.getSystemUser();
        template = mode.showLive ?
                (Template) this.versionableAPI.findLiveVersion(page.getTemplateId(), systemUser, mode.respectAnonPerms) :
                (Template) this.versionableAPI.findWorkingVersion(page.getTemplateId(), systemUser, mode.respectAnonPerms);

        if (template.isDrawed()) {
            layout = DotTemplateTool.themeLayout(template.getInode());
        }

        final List<ContainerRendered> containers = this.containerRenderedBuilder.getContainers(template);
        Map<String, ContainerRendered> containersNotRendered = containers.stream()
                .collect(Collectors.toMap(
                        containerRendered -> containerRendered.getContainer().getIdentifier(),
                        containerRendered -> containerRendered,
                        (c1,c2) ->c1
                ));
        
        Map<String, String> containersRendered = new HashMap<>();
        
        if (isRendered) {
            final Context velocityContext  = new PageContextBuilder(page, systemUser, mode).addAll(VelocityUtil.getWebContext(request, response));
            final List<ContainerRendered> containersToRender = this.containerRenderedBuilder.getContainersRendered(page, velocityContext, mode);
            containersRendered = containersToRender
                .stream()
                .collect(Collectors.toMap(
                        containerToRender -> containerToRender.getContainer().getIdentifier() + "_" +containerToRender.getUuid() ,
                        containerToRender -> containerToRender.getRendered())
                );
        } 


        return new PageView(site, template, containersNotRendered,containersRendered, page, layout);
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
