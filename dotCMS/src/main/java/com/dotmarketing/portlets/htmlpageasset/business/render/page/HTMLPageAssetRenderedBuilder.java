package com.dotmarketing.portlets.htmlpageasset.business.render.page;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.enterprise.license.LicenseManager;
import com.dotcms.experiments.model.Experiment;
import com.dotcms.rendering.velocity.directive.RenderParams;
import com.dotcms.rendering.velocity.services.PageRenderUtil;
import com.dotcms.rendering.velocity.servlet.VelocityModeHandler;
import com.dotcms.rendering.velocity.viewtools.DotTemplateTool;
import com.dotcms.util.TimeMachineUtil;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.LayoutAPI;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.PermissionLevel;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.hostvariable.model.HostVariable;
import com.dotmarketing.portlets.htmlpageasset.business.render.ContainerRaw;
import com.dotmarketing.portlets.htmlpageasset.business.render.ContainerRenderedBuilder;
import com.dotmarketing.portlets.htmlpageasset.business.render.HTMLPageAssetNotFoundException;
import com.dotmarketing.portlets.htmlpageasset.business.render.HTMLPageAssetRenderedAPI;
import com.dotmarketing.portlets.htmlpageasset.business.render.VanityURLView;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.templates.design.bean.ContainerUUID;
import com.dotmarketing.portlets.templates.design.bean.TemplateLayout;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.VelocityUtil;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import io.vavr.control.Try;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.velocity.context.Context;

/**
 * This class will be in charge of building the Metadata object for an HTML Page.
 *
 * @author Freddy Rodriguez
 * @since Apr 12th, 2018
 */
public class HTMLPageAssetRenderedBuilder {

    private HTMLPageAsset htmlPageAsset;
    private User user;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private Host site;

    private final PermissionAPI  permissionAPI;
    private final ContentletAPI  contentletAPI;
    private final LayoutAPI      layoutAPI;
    private final HTMLPageAssetRenderedAPI htmlPageAssetRenderedAPI;
    private String pageUrlMapper;
    private boolean live;
    private boolean parseJSON;
    private Experiment runningExperiment;
    private VanityURLView vanityUrl;

    /**
     * Creates an instance of this Builder, along with all the required dotCMS APIs.
     */
    public HTMLPageAssetRenderedBuilder() {
        this.permissionAPI  = APILocator.getPermissionAPI();
        this.contentletAPI  = APILocator.getContentletAPI();
        this.layoutAPI      = APILocator.getLayoutAPI();
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

    public HTMLPageAssetRenderedBuilder setParseJSON(final boolean parseJSON) {
        this.parseJSON = parseJSON;
        return this;
    }

    public HTMLPageAssetRenderedBuilder setVanityUrl(final VanityURLView vanityUrl) {
        this.vanityUrl = vanityUrl;
        return this;
    }

    /**
     * Generates the metadata of the specified HTML Page. All the internal data structures that make up the page will be
     * retrieved from the content repository and will be returned to the User in the form of a {@link PageView} object.
     *
     * @param rendered If the {@link PageView} object needs to contain the rendered version of the HTML Page, i.e., the
     *                 resulting HTML code, set this to {@code true}.
     * @param mode     The {@link PageMode} used to get the HTML Page metadata.
     *
     * @return The {@link PageView} object with the HTML Page metadata.
     *
     * @throws DotDataException     An error occurred when accessing the data source.
     * @throws DotSecurityException The user does not have the specified permissions to perform
     *                              this action.
     */
    @CloseDBIfOpened
    public PageView build(final boolean rendered, final PageMode mode) throws DotDataException, DotSecurityException {
        final Template template = this.getTemplate(mode);
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
        final boolean canEditTemplate = this.permissionAPI.doesUserHavePermission(template, PermissionLevel.EDIT.getType(), user);
        final boolean canCreateTemplates = layoutAPI.doesUserHaveAccessToPortlet("templates", user);

        final PageRenderUtil pageRenderUtil = new PageRenderUtil(
                this.htmlPageAsset, user, mode, language.getId(), this.site);

        // Here we get the URL contentlet, if it exists.
        // If it exists, we check if the page is live or not.
        final Optional<Contentlet> urlContentletOpt = findUrlMapContentlet(request, mode);
        if (!rendered) {
            final Collection<? extends ContainerRaw> containers =  pageRenderUtil.getContainersRaw();

            transformLegacyContainerUUIDs(layout);

            final PageView.Builder pageViewBuilder = new PageView.Builder().site(site).template(template).containers(containers)
                    .page(this.htmlPageAsset).layout(layout).canCreateTemplate(canCreateTemplates)
                    .canEditTemplate(canEditTemplate).viewAs(
                            this.htmlPageAssetRenderedAPI.getViewAsStatus(request,
                                    mode, this.htmlPageAsset, user))
                    .pageUrlMapper(pageUrlMapper).live(live)
                    .runningExperiment(runningExperiment)
                    .vanityUrl(this.vanityUrl);
            urlContentletOpt.ifPresent(pageViewBuilder::urlContent);

            return pageViewBuilder.build();
        } else {
            final Context velocityContext  = pageRenderUtil
                    .addAll(VelocityUtil.getInstance().getContext(request, response));
            velocityContext.put("parseJSON", parseJSON);
            final List<HostVariable> hostVariables = APILocator.getHostVariableAPI().getVariablesForHost(this.site.getIdentifier(), user, false);
            velocityContext.put("host_variable", null != hostVariables?
                    hostVariables.stream().collect(Collectors.toMap(HostVariable::getKey, HostVariable::getValue)) : Collections.emptyMap());
            final Collection<? extends ContainerRaw> containers = new ContainerRenderedBuilder(
                    pageRenderUtil.getContainersRaw(), velocityContext, mode)
                    .build();
            final int contentletsKey = containers.stream()
                    .flatMap(container -> container.getContentlets().values().stream())
                    .flatMap(Collection::stream)
                    .map(Contentlet::getIdentifier)
                    .collect(Collectors.joining(","))
                    .hashCode();
            request.setAttribute("contentletsKey", String.valueOf(contentletsKey));
            final String pageHTML = this.getPageHTML(mode);

            transformLegacyContainerUUIDs(layout);

            final HTMLPageAssetRendered.RenderedBuilder pageViewBuilder = new HTMLPageAssetRendered.RenderedBuilder().html(pageHTML);
            pageViewBuilder.site(site).template(template).containers(containers)
                    .page(this.htmlPageAsset).layout(layout).canCreateTemplate(canCreateTemplates)
                    .canEditTemplate(canEditTemplate).viewAs(
                    this.htmlPageAssetRenderedAPI.getViewAsStatus(request,
                            mode, this.htmlPageAsset, user))
                    .pageUrlMapper(pageUrlMapper).live(live)
                    .runningExperiment(runningExperiment)
                    .vanityUrl(this.vanityUrl);
            urlContentletOpt.ifPresent(pageViewBuilder::urlContent);

            return pageViewBuilder.build();
        }
    }

    /**
     * Returns the URL contentlet if it exists. This is used to get the contentlet that is associated with the URL of the page like a urlMapContent
     * @param request
     * @param mode
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    private Optional<Contentlet> findUrlMapContentlet(final HttpServletRequest request, final PageMode mode)
            throws DotDataException, DotSecurityException {

        Contentlet contentlet = null;

        if (null != request.getAttribute(WebKeys.WIKI_CONTENTLET_INODE)) {
            final String inode = (String)request.getAttribute(WebKeys.WIKI_CONTENTLET_INODE);
            contentlet = this.contentletAPI.find(inode, user, false);
        } else if (null != request.getAttribute(WebKeys.WIKI_CONTENTLET)) {
            final String id = (String)request.getAttribute(WebKeys.WIKI_CONTENTLET);
            contentlet = this.contentletAPI.findContentletByIdentifierAnyLanguage(id);
        }

        final Optional<Date> timeMachineDate = TimeMachineUtil.getTimeMachineDateAsDate();
        if(null != contentlet && timeMachineDate.isPresent()) {
            final Contentlet contentletForTimeMachine = getContentletForTimeMachine(contentlet, timeMachineDate.get());
            return Optional.of(contentletForTimeMachine);
        } else {
            if (null != contentlet && PageMode.LIVE == mode && !contentlet.isLive()) {
                throw new HTMLPageAssetNotFoundException(pageUrlMapper);
            }
            return Optional.ofNullable(contentlet);
        }
    }

    /**
     * Finds the appropriate contentlet version for Time Machine functionality.
     * If a Time Machine date is present and the contentlet is not null, this method
     * attempts to find a version of the contentlet that existed at the specified Time Machine date.
     *
     * @param contentlet The original contentlet to find a time machine version for. Can be null.
     * @param timeMachineDate The date to find the contentlet version for. If null, the original contentlet is returned.
     * @return The time machine version of the contentlet if found, otherwise the original contentlet.
     *         Returns null if the input contentlet was null.
     * @throws DotDataException If there is an error in the underlying data layer
     * @throws DotSecurityException If the current user doesn't have permission to access the contentlet
     */
    private Contentlet getContentletForTimeMachine(final Contentlet contentlet, final Date timeMachineDate)
            throws DotDataException, DotSecurityException {

        // Early return if the contentlet is null or no Time Machine date is configured
        if (contentlet == null ) {
            throw new IllegalArgumentException("Contentlet cannot be null");
        }

        // Attempt to find the version of the contentlet at the Time Machine date
        final Contentlet future = contentletAPI.findContentletByIdentifier(
                contentlet.getIdentifier(),
                contentlet.getLanguageId(),
                WebAPILocator.getVariantWebAPI().currentVariantId(),
                timeMachineDate,
                user,
                false
        );

        // Return the future version if found, otherwise return the original contentlet
        return future != null ? future : contentlet;
    }



    @CloseDBIfOpened
    public String getPageHTML(final PageMode pageMode) throws DotSecurityException {

        if(pageMode.isAdmin ) {
            APILocator.getPermissionAPI().checkPermission(htmlPageAsset, PermissionLevel.READ, user);
        }

        return VelocityModeHandler.modeHandler(htmlPageAsset, pageMode, request, response, site).eval();
    }

    /**
     * Returns the Template used by the current HTML Page being rendered.
     *
     * @param mode The {@link PageMode} that the HTML Page is being returned in. If the Live version is required, then
     *             the Live version of the Template must always be returned. Otherwise, the Working version is returned.
     *
     * @return The {@link Template} used by the HTML Page.
     *
     * @throws DotDataException An error occurred when accessing the data source.
     */
    private Template getTemplate(final PageMode mode) throws DotDataException {
        try {
            final User systemUser = APILocator.getUserAPI().getSystemUser();

            return mode.showLive ?
                    APILocator.getTemplateAPI().findLiveTemplate(htmlPageAsset.getTemplateId(),systemUser, mode.respectAnonPerms) :
                    APILocator.getTemplateAPI().findWorkingTemplate(htmlPageAsset.getTemplateId(),systemUser, mode.respectAnonPerms);
        } catch (DotSecurityException e) {
            return null;
        }
    }

    public void setRunningExperiment(Experiment experiment) {
        this.runningExperiment = experiment;
    }

    /**
     * Transforms legacy container UUIDs (LEGACY_RELATION_TYPE) to "1" throughout the template layout
     * to ensure consistency between layout and rendered container fields in the API response.
     * 
     * @param layout The template layout to transform
     */
    private void transformLegacyContainerUUIDs(TemplateLayout layout) {
        if (layout == null) {
            return;
        }


        if (layout.getBody() != null && layout.getBody().getRows() != null) {
            layout.getBody().getRows().forEach(row -> {
                if (row.getColumns() != null) {
                    row.getColumns().forEach(column -> {
                        if (column.getContainers() != null) {
                            column.getContainers().stream()
                                    .filter(container -> ContainerUUID.UUID_LEGACY_VALUE.equals(container.getUUID()))
                                    .forEach(container -> container.setUuid(ContainerUUID.UUID_START_VALUE));
                        }
                    });
                }
            });
        }


        if (layout.getSidebar() != null && layout.getSidebar().getContainers() != null) {
            layout.getSidebar().getContainers().stream()
                    .filter(container -> ContainerUUID.UUID_LEGACY_VALUE.equals(container.getUUID()))
                    .forEach(container -> container.setUuid(ContainerUUID.UUID_START_VALUE));
        }
    }

}
