package com.dotmarketing.portlets.htmlpageasset.business.render.page;
import com.dotmarketing.factories.MultiTreeAPI;
import com.dotmarketing.portlets.htmlpageasset.business.render.HTMLPageAssetRenderedAPI;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;

import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.language.LanguageUtil;
import io.vavr.control.Try;
import java.util.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.dotmarketing.portlets.personas.model.Persona;
import com.liferay.util.StringPool;
import org.apache.velocity.context.Context;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.enterprise.license.LicenseManager;
import com.dotcms.rendering.velocity.directive.RenderParams;
import com.dotcms.rendering.velocity.services.PageRenderUtil;
import com.dotcms.rendering.velocity.servlet.VelocityModeHandler;
import com.dotcms.rendering.velocity.viewtools.DotTemplateTool;
import com.dotcms.visitor.domain.Visitor;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.*;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.DotLockException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.htmlpageasset.business.render.ContainerRaw;
import com.dotmarketing.portlets.htmlpageasset.business.render.ContainerRenderedBuilder;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.personas.model.IPersona;
import com.dotmarketing.portlets.templates.design.bean.TemplateLayout;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.VelocityUtil;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import java.util.Collection;
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

    private final PermissionAPI  permissionAPI;
    private final UserAPI        userAPI;
    private final ContentletAPI  contentletAPI;
    private final LayoutAPI      layoutAPI;
    private final VersionableAPI versionableAPI;
    private final MultiTreeAPI   multiTreeAPI;
    private final HTMLPageAssetRenderedAPI htmlPageAssetRenderedAPI;
    private String pageUrlMapper;
    private boolean live;

    public HTMLPageAssetRenderedBuilder() {

        this.permissionAPI  = APILocator.getPermissionAPI();
        this.userAPI        = APILocator.getUserAPI();
        this.contentletAPI  = APILocator.getContentletAPI();
        this.layoutAPI      = APILocator.getLayoutAPI();
        this.versionableAPI = APILocator.getVersionableAPI();
        this.multiTreeAPI   = APILocator.getMultiTreeAPI();
        this.htmlPageAssetRenderedAPI   = APILocator.getHTMLPageAssetRenderedAPI();
    }

    public HTMLPageAssetRenderedBuilder setLive(final boolean live) {
        this.live = live;
        return this;
    }

    public HTMLPageAssetRenderedBuilder setHtmlPageAsset(final HTMLPageAsset htmlPageAsset) {
        this.htmlPageAsset = htmlPageAsset;
        return this;
    }

    public HTMLPageAssetRenderedBuilder setUser(final User user) {
        this.user = user;
        return this;
    }

    public HTMLPageAssetRenderedBuilder setRequest(final HttpServletRequest request) {
        this.request = request;
        return this;
    }

    public HTMLPageAssetRenderedBuilder setResponse(final HttpServletResponse response) {
        this.response = response;
        return this;
    }

    public HTMLPageAssetRenderedBuilder setSite(final Host site) {
        this.site = site;
        return this;
    }

    public HTMLPageAssetRenderedBuilder setURLMapper(final String pageUrlMapper) {
        this.pageUrlMapper = pageUrlMapper;
        return this;
    }

    @CloseDBIfOpened
    public PageView build(final boolean rendered, final PageMode mode) throws DotDataException, DotSecurityException {
        final Template template = getTemplate(mode);
        if(!UtilMethods.isSet(template) && mode.equals(PageMode.ADMIN_MODE)){
            throw new DotStateException(
                    Try.of(() -> LanguageUtil.get(user.getLocale(), "template.archived.page.live.mode.error"))
                            .getOrElse("This page cannot be viewed on Live Mode because the template is unpublished or archived"));
        }
        final Language language = WebAPILocator.getLanguageWebAPI().getLanguage(request);

        final TemplateLayout layout = template != null && template.isDrawed() && !LicenseManager.getInstance().isCommunity()
                ? DotTemplateTool.themeLayout(template.getInode()) : null;

        // this forces all velocity dotParses to use the site for the given page 
        // (unless host is specified in the dotParse) github 14624
        final RenderParams params=new RenderParams(user,language, site, mode);
        request.setAttribute(RenderParams.RENDER_PARAMS_ATTRIBUTE, params);
        final User systemUser = APILocator.getUserAPI().getSystemUser();
        final boolean canEditTemplate = this.permissionAPI.doesUserHavePermission(template, PermissionLevel.EDIT.getType(), user);
        final boolean canCreateTemplates = layoutAPI.doesUserHaveAccessToPortlet("templates", user);

        final PageRenderUtil pageRenderUtil = new PageRenderUtil(
                this.htmlPageAsset, systemUser, mode, language.getId(), this.site);

        final Optional<Contentlet> urlContentletOpt = this.findUrlContentlet (request);

        if (!rendered) {

            final Collection<? extends ContainerRaw> containers =  pageRenderUtil.getContainersRaw();
            final PageView.Builder pageViewBuilder = new PageView.Builder().site(site).template(template).containers(containers)
                    .page(this.htmlPageAsset).layout(layout).canCreateTemplate(canCreateTemplates)
                    .canEditTemplate(canEditTemplate).viewAs(
                            this.htmlPageAssetRenderedAPI.getViewAsStatus(request,
                                    mode, this.htmlPageAsset, user))
                    .pageUrlMapper(pageUrlMapper).live(live);
            urlContentletOpt.ifPresent(pageViewBuilder::urlContent);

            return pageViewBuilder.build();
        } else {

            final Context velocityContext  = pageRenderUtil
                    .addAll(VelocityUtil.getInstance().getContext(request, response));
            final Collection<? extends ContainerRaw> containers = new ContainerRenderedBuilder(
                    pageRenderUtil.getContainersRaw(), velocityContext, mode).build();
            final String pageHTML = this.getPageHTML();

            final HTMLPageAssetRendered.RenderedBuilder pageViewBuilder = new HTMLPageAssetRendered.RenderedBuilder().html(pageHTML);
            pageViewBuilder.site(site).template(template).containers(containers)
                    .page(this.htmlPageAsset).layout(layout).canCreateTemplate(canCreateTemplates)
                    .canEditTemplate(canEditTemplate).viewAs(
                    this.htmlPageAssetRenderedAPI.getViewAsStatus(request,
                            mode, this.htmlPageAsset, user))
                    .pageUrlMapper(pageUrlMapper).live(live);
            urlContentletOpt.ifPresent(pageViewBuilder::urlContent);

            return pageViewBuilder.build();
        }
    }

    private Optional<Contentlet> findUrlContentlet(final HttpServletRequest request) throws DotDataException, DotSecurityException {

        Contentlet contentlet = null;

        if (null != request.getAttribute(WebKeys.WIKI_CONTENTLET_INODE)) {

            final String inode = (String)request.getAttribute(WebKeys.WIKI_CONTENTLET_INODE);
            contentlet         = this.contentletAPI.find(inode, user, false);
        } else if (null != request.getAttribute(WebKeys.WIKI_CONTENTLET)) {

            final String id    = (String)request.getAttribute(WebKeys.WIKI_CONTENTLET);
            contentlet         = this.contentletAPI.findContentletByIdentifierAnyLanguage(id);
        }

        return Optional.ofNullable(contentlet);
    }

    public String getPageHTML() throws DotSecurityException {

        final PageMode mode = PageMode.get(request);

        return getPageHTML(mode);
    }

    @CloseDBIfOpened
    public String getPageHTML(final PageMode pageMode) throws DotSecurityException {

        if(pageMode.isAdmin ) {
            APILocator.getPermissionAPI().checkPermission(htmlPageAsset, PermissionLevel.READ, user);
        }

        return VelocityModeHandler.modeHandler(htmlPageAsset, pageMode, request, response, site).eval();
    }

    private Template getTemplate(final PageMode mode) throws DotDataException {
        try {
            final User systemUser = APILocator.getUserAPI().getSystemUser();

            return mode.showLive ?
                    (Template) this.versionableAPI.findLiveVersion(htmlPageAsset.getTemplateId(), systemUser, mode.respectAnonPerms) :
                    (Template) this.versionableAPI.findWorkingVersion(htmlPageAsset.getTemplateId(), systemUser, mode.respectAnonPerms);
        } catch (DotSecurityException e) {
            return null;
        }
    }
}
