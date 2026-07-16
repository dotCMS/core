package com.dotmarketing.portlets.htmlpageasset.business.render.page;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.enterprise.license.LicenseManager;
import com.dotcms.experiments.model.Experiment;
import com.dotcms.featureflag.FeatureFlagName;
import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.dotcms.rest.api.v1.page.PageResourceHelper;
import com.dotmarketing.portlets.htmlpageasset.business.render.page.PageView.Builder;
import com.fasterxml.jackson.databind.JsonNode;
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
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
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

    public static final String SDK_EDITOR_SCRIPT_SOURCE = "<script src=\"/ext/uve/dot-uve.js\"></script>";

    private static final String UVE_SCRIPTS_TEMPLATE =
            "<script>" +
            "function initDotUVE() {" +
            "if (typeof dotUVE !== 'undefined' && dotUVE.registerStyleEditorSchemas) {" +
            "dotUVE.registerStyleEditorSchemas(%s);" +
            "} else {" +
            "console.error('dotUVE is not available');" +
            "}" +
            "}" +
            "</script>" +
            "<script src=\"/ext/uve/dot-uve.js\" onload=\"initDotUVE()\"></script>";

    /**
     * Prefix of the inline init function — referenced by {@code UVE_SCRIPT_BLOCK_PATTERN} in HTMLPageAssetRenderedAPIImpl.
     */
    public static final String UVE_INIT_FUNCTION_PREFIX = "<script>function initDotUVE()";

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
            applyStyleEditorSchemas(resolveStyleEditorSchemas(mode, containers), mode, pageViewBuilder);

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
            final String rawHTML = this.getPageHTML(mode);
            // Compute schemas once here so both the UVE script block and the PageView builder
            // consume the same result without a redundant DB traversal.
            final List<JsonNode> styleEditorSchemas = resolveStyleEditorSchemas(mode, containers);
            final String pageHTML;
            if (mode != PageMode.LIVE) {
                Logger.debug(this, () -> String.format(
                        "Injecting UVE script for page '%s' in mode %s",
                        htmlPageAsset.getPageUrl(), mode));
                pageHTML = injectUVEScript(rawHTML, styleEditorSchemas);
            } else {
                Logger.debug(this, () -> String.format(
                        "Skipping UVE script injection for page '%s' (LIVE mode)",
                        htmlPageAsset.getPageUrl()));
                pageHTML = rawHTML;
            }

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
            applyStyleEditorSchemas(styleEditorSchemas, mode, pageViewBuilder);

            return pageViewBuilder.build();
        }
    }

    /**
     * Returns Style Editor schemas for all distinct ContentTypes on the page when the
     * {@code FEATURE_FLAG_UVE_STYLE_EDITOR} feature flag is enabled and the page is not in
     * {@link PageMode#LIVE} mode; returns an empty list otherwise.
     * <p>
     * This is the single computation point — call it once per request and share the result with
     * both the UVE script-injection block and the {@link PageView.Builder}. The UVE script
     * injection uses schemas for all non-LIVE modes; the REST/GQL response only exposes them in
     * {@link PageMode#EDIT_MODE} (enforced by {@link #applyStyleEditorSchemas}).
     *
     * @param mode       The {@link PageMode} the page is being rendered in.
     * @param containers The containers whose contentlets are inspected for schemas.
     * @return A (possibly empty) list of JSON schema nodes.
     */
    private static List<JsonNode> resolveStyleEditorSchemas(final PageMode mode,
            final Collection<? extends ContainerRaw> containers) {
        if (mode == PageMode.LIVE || !ConfigUtils.isFeatureFlagOn(
                FeatureFlagName.FEATURE_FLAG_UVE_STYLE_EDITOR)) {
            return Collections.emptyList();
        }
        final List<Contentlet> pageContentlets = containers.stream()
                .flatMap(c -> c.getContentlets().values().stream())
                .flatMap(List::stream)
                .collect(Collectors.toList());
        return PageResourceHelper.getStyleEditorSchemas(pageContentlets);
    }

    /**
     * Conditionally populates the {@link PageView.Builder} with pre-resolved Style Editor schemas.
     * <p>
     * Schemas are only written to the REST/GQL response in {@link PageMode#EDIT_MODE}; other
     * non-LIVE modes receive schemas only in the injected UVE script block, not in the page JSON.
     *
     * @param schemas         Pre-resolved schemas from {@link #resolveStyleEditorSchemas}.
     * @param mode            The current {@link PageMode}; schemas are applied only in EDIT_MODE.
     * @param pageViewBuilder The builder that will receive the resolved schemas.
     */
    private static void applyStyleEditorSchemas(final List<JsonNode> schemas,
            final PageMode mode, final Builder pageViewBuilder) {
        if (mode == PageMode.EDIT_MODE && !schemas.isEmpty()) {
            pageViewBuilder.styleEditorSchemas(schemas);
        }
    }

    /**
     * Returns the URL contentlet associated with the page's URL content map, if one exists.
     *
     * @param request The current HTTP request, used to read URL contentlet attributes.
     * @param mode    The {@link PageMode} the page is being rendered in.
     * @return An {@link Optional} containing the URL contentlet, or empty if none is found.
     * @throws DotDataException     An error occurred when accessing the data source.
     * @throws DotSecurityException The user does not have the required permissions.
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

    /**
     * Injects UVE scripts before the closing {@code </body>} tag in the given HTML string. If
     * pre-resolved schemas are non-empty, the full {@code UVE_SCRIPTS_TEMPLATE} is injected
     * (init function + {@code <script src>} with onload). Otherwise, the plain
     * {@code SDK_EDITOR_SCRIPT_SOURCE} tag is injected. If no {@code </body>} tag is found, the
     * scripts are appended at the end.
     *
     * @param html    The rendered HTML content of the page.
     * @param schemas Pre-resolved style editor schemas (from {@link #resolveStyleEditorSchemas}).
     * @return The HTML content with the UVE scripts injected.
     */
    private String injectUVEScript(final String html, final List<JsonNode> schemas) {
        if (!UtilMethods.isSet(html)) {
            Logger.debug(this, "Skipping UVE script injection: rendered HTML is empty or null");
            return html;
        }
        final Optional<String> styleEditorScript = buildUVEStyleEditorScripts(schemas);
        final String scripts = styleEditorScript.orElse(SDK_EDITOR_SCRIPT_SOURCE);
        Logger.debug(this, () -> styleEditorScript.isPresent()
                ? "Injecting UVE script with style editor schemas"
                : "Injecting plain UVE script (no style editor schemas found)");
        final int closingBodyIndex = html.toLowerCase().lastIndexOf("</body>");
        if (closingBodyIndex != -1) {
            return html.substring(0, closingBodyIndex) + scripts + html.substring(closingBodyIndex);
        }
        Logger.warn(this, "No </body> tag found in page HTML, appending UVE script at end");
        return html + scripts;
    }

    /**
     * Formats a UVE script block from pre-resolved schemas: an inline {@code initDotUVE()}
     * function that calls {@code dotUVE.registerStyleEditorSchemas(schemas)}, followed by a
     * {@code <script src>} tag that triggers it on load.
     *
     * @param schemas Pre-resolved schemas (from {@link #resolveStyleEditorSchemas}).
     * @return An {@link Optional} with the formatted script block, or empty if schemas is empty.
     */
    private Optional<String> buildUVEStyleEditorScripts(final List<JsonNode> schemas) {
        if (schemas.isEmpty()) {
            return Optional.empty();
        }

        final Optional<String> schemasJson = Try.of(() -> Optional.of(
                DotObjectMapperProvider.getInstance()
                        .getDefaultObjectMapper()
                        .writeValueAsString(schemas)
                        .replace("</script>", "<\\/script>")))
                .onFailure(e -> Logger.error(HTMLPageAssetRenderedBuilder.class,
                        "Failed to serialize DOT_STYLE_EDITOR_SCHEMA, falling back to plain script tag", e))
                .getOrElse(Optional.empty());

        return schemasJson.map(json -> String.format(UVE_SCRIPTS_TEMPLATE, json));
    }

}
