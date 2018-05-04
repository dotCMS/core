package com.dotmarketing.portlets.htmlpageasset.business.render.page;

import com.dotcms.business.CloseDB;
import com.dotcms.rendering.velocity.servlet.VelocityModeHandler;
import com.dotcms.rendering.velocity.viewtools.DotTemplateTool;
import com.dotcms.visitor.domain.Visitor;
import com.dotmarketing.beans.ContainerStructure;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.*;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.business.ContainerAPI;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.DotLockException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.htmlpageasset.business.render.ContainerRendered;
import com.dotmarketing.portlets.htmlpageasset.business.render.ContainerRenderedBuilder;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.personas.model.IPersona;
import com.dotmarketing.portlets.personas.model.Persona;
import com.dotmarketing.portlets.templates.business.TemplateAPI;
import com.dotmarketing.portlets.templates.design.bean.TemplateLayout;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.VelocityUtil;
import com.dotmarketing.util.WebKeys;
import com.google.common.collect.ImmutableMap;
import com.liferay.portal.model.User;
import org.apache.velocity.context.Context;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Builder of {@link HTMLPageAssetRendered}
 */
public class HTMLPageAssetRenderedBuilder {
    private HTMLPageAsset htmlPageAsset;
    private User user;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private Host site;

    private final PermissionAPI permissionAPI;
    private final UserAPI userAPI;
    private final TemplateAPI templateAPI;
    private final ContentletAPI contentletAPI;
    private final LayoutAPI layoutAPI;
    private final VersionableAPI versionableAPI = APILocator.getVersionableAPI();

    private final ContainerRenderedBuilder containerRenderedBuilder;

    public HTMLPageAssetRenderedBuilder() {
        permissionAPI = APILocator.getPermissionAPI();
        userAPI = APILocator.getUserAPI();
        templateAPI = APILocator.getTemplateAPI();
        contentletAPI = APILocator.getContentletAPI();
        layoutAPI = APILocator.getLayoutAPI();
        containerRenderedBuilder = new ContainerRenderedBuilder();
    }

    public HTMLPageAssetRenderedBuilder setHtmlPageAsset(HTMLPageAsset htmlPageAsset) {
        this.htmlPageAsset = htmlPageAsset;
        return this;
    }

    public HTMLPageAssetRenderedBuilder setUser(User user) {
        this.user = user;
        return this;
    }

    public HTMLPageAssetRenderedBuilder setRequest(HttpServletRequest request) {
        this.request = request;
        return this;
    }

    public HTMLPageAssetRenderedBuilder setResponse(HttpServletResponse response) {
        this.response = response;
        return this;
    }

    public HTMLPageAssetRenderedBuilder setSite(Host site) {
        this.site = site;
        return this;
    }

    public PageView build(final boolean rendered) throws DotDataException, DotSecurityException {
        final ContentletVersionInfo info = APILocator.getVersionableAPI().
                getContentletVersionInfo(htmlPageAsset.getIdentifier(), htmlPageAsset.getLanguageId());

        final HTMLPageAssetInfo htmlPageAssetInfo = getHTMLPageAssetInfo(info);
        final Template template = getTemplate();
        final TemplateLayout layout = template.isDrawed() ? DotTemplateTool.themeLayout(template.getInode()) : null;

        if (!rendered) {
            final List<ContainerRendered> containers = this.containerRenderedBuilder.getContainers(template);
            return new PageView(site, template, containers, htmlPageAssetInfo, layout);
        } else {
            final PageMode mode = PageMode.get(request);
            final Context velocityContext = VelocityUtil.getWebContext(request, response);

            final List<ContainerRendered> containers = this.containerRenderedBuilder.getContainersRendered(template,
                    velocityContext, mode);
            final boolean canCreateTemplates = layoutAPI.doesUserHaveAccessToPortlet("templates", user);
            final String pageHTML = this.getPageHTML();
            final boolean canEditTemplate = this.permissionAPI.doesUserHavePermission(template, PermissionLevel.EDIT.getType(), user);

            return new HTMLPageAssetRendered(site, template, containers, htmlPageAssetInfo, layout, pageHTML,
                    canCreateTemplates, canEditTemplate, this.getViewAsStatus()
            );
        }
    }

    @CloseDB
    public String getPageHTML() throws DotSecurityException, DotDataException {

        final PageMode mode = PageMode.get(request);

        if(mode.isAdmin ) {
            APILocator.getPermissionAPI().checkPermission(htmlPageAsset, PermissionLevel.READ, user);
        }

        return VelocityModeHandler.modeHandler(mode, request, response, htmlPageAsset.getURI(), site).eval();
    }

    private Template getTemplate() throws DotDataException {
        try {
            final PageMode mode = PageMode.get(request);
            final User systemUser = APILocator.getUserAPI().getSystemUser();

            return mode.showLive ?
                    (Template) this.versionableAPI.findLiveVersion(htmlPageAsset.getTemplateId(), systemUser, mode.respectAnonPerms) :
                    (Template) this.versionableAPI.findWorkingVersion(htmlPageAsset.getTemplateId(), systemUser, mode.respectAnonPerms);
        } catch (DotSecurityException e) {
            return null;
        }
    }

    private HTMLPageAssetInfo getHTMLPageAssetInfo(final ContentletVersionInfo info) throws DotDataException {
        HTMLPageAssetInfo htmlPageAssetInfo = new HTMLPageAssetInfo()
            .setPage(this.htmlPageAsset)
            .setWorkingInode(info.getWorkingInode())
            .setShortyWorking(APILocator.getShortyAPI().shortify(info.getWorkingInode()))
            .setCanEdit(this.permissionAPI.doesUserHavePermission(htmlPageAsset, PermissionLevel.EDIT.getType(), user))
            .setLiveInode(info.getLiveInode())
            .setShortyLive(APILocator.getShortyAPI().shortify(info.getLiveInode()))
            .setCanLock(this.canLock());

        final String lockedBy= (info.getLockedBy()!=null)  ? info.getLockedBy() : null;

        if(lockedBy!=null) {
            htmlPageAssetInfo.setLockedOn(info.getLockedOn())
                .setLockedBy(lockedBy)
                .setLockedByName(getLockedByUserName(info));
        }

        return htmlPageAssetInfo;
    }

    private String getLockedByUserName(final ContentletVersionInfo info) throws DotDataException {
        try {
            return userAPI.loadUserById(info.getLockedBy()).getFullName();
        } catch (DotSecurityException e) {
            return null;
        }
    }

    private boolean canLock()  {
        try {
            APILocator.getContentletAPI().canLock(htmlPageAsset, user);
            return true;
        } catch (DotLockException e) {
            return false;
        }
    }

    private ViewAsPageStatus getViewAsStatus()
            throws DotDataException {

        return new ViewAsPageStatus()
            .setPersona(this.getCurrentPersona())
            .setLanguage(WebAPILocator.getLanguageWebAPI().getLanguage(request))
            .setDevice(getCurrentDevice());
    }

    private IPersona getCurrentPersona() {
        final Optional<Visitor> visitor = APILocator.getVisitorAPI().getVisitor(request);
        return visitor.isPresent() && visitor.get().getPersona() != null ? visitor.get().getPersona() : null;
    }

    private Contentlet getCurrentDevice() throws DotDataException {
        final String deviceInode = (String) request.getSession().getAttribute(WebKeys.CURRENT_DEVICE);
        Contentlet currentDevice = null;

        try {

            final String currentDeviceId = deviceInode == null ?
                    (String) request.getSession().getAttribute(WebKeys.CURRENT_DEVICE)
                    : deviceInode;

            if (currentDeviceId != null) {
                currentDevice = contentletAPI.find(currentDeviceId, user, false);

                if (currentDevice == null) {
                    request.getSession().removeAttribute(WebKeys.CURRENT_DEVICE);
                }
            }
        } catch (DotSecurityException e) {
            Logger.debug(this.getClass(),
                    "Exception on createViewAsMap exception message: " + e.getMessage(), e);
        }

        return currentDevice;
    }
}
