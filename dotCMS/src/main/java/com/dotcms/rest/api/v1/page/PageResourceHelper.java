package com.dotcms.rest.api.v1.page;

import com.dotmarketing.beans.ContainerStructure;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.VersionableAPI;
import com.dotmarketing.business.web.HostWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.business.ContainerAPI;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.templates.business.TemplateAPI;
import com.dotmarketing.portlets.templates.design.bean.TemplateLayout;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.VelocityUtil;
import com.dotmarketing.viewtools.DotTemplateTool;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.liferay.portal.model.User;
import org.apache.velocity.context.Context;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Provides the utility methods that interact with HTML Pages in dotCMS. These methods are used by
 * the Page REST end-point.
 *
 * @author Will Ezell
 * @author Jose Castro
 * @version 4.2
 * @since Oct 6, 2017
 */
public class PageResourceHelper implements Serializable {

    private static final long serialVersionUID = 296763857542258211L;

    private final HostWebAPI hostWebAPI = WebAPILocator.getHostWebAPI();
    private final HTMLPageAssetAPI htmlPageAssetAPI = APILocator.getHTMLPageAssetAPI();
    private final LanguageAPI languageAPI = APILocator.getLanguageAPI();
    private final VersionableAPI versionableAPI = APILocator.getVersionableAPI();
    private final TemplateAPI templateAPI = APILocator.getTemplateAPI();
    private final ContainerAPI containerAPI = APILocator.getContainerAPI();

    private static final boolean RESPECT_FE_ROLES = Boolean.TRUE;
    private static final boolean RESPECT_ANON_PERMISSIONS = Boolean.TRUE;

    /**
     * Private constructor
     */
    private PageResourceHelper() {

    }

    /**
     * Provides a singleton instance of the {@link PageResourceHelper}
     */
    private static class SingletonHolder {
        private static final PageResourceHelper INSTANCE = new PageResourceHelper();
    }

    /**
     * Returns a singleton instance of this class.
     *
     * @return A single instance of this class.
     */
    public static PageResourceHelper getInstance() {
        return PageResourceHelper.SingletonHolder.INSTANCE;
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
    public PageView getPageMetadata(final HttpServletRequest request, final HttpServletResponse
            response, final User user, final String uri) throws DotSecurityException,
            DotDataException {
        return getPageMetadata(request, response, user, uri, false);
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
    public PageView getPageMetadataRendered(final HttpServletRequest request, final
    HttpServletResponse response, final User user, final String uri) throws DotSecurityException,
            DotDataException {
        return getPageMetadata(request, response, user, uri, true);
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
    private PageView getPageMetadata(final HttpServletRequest request, final HttpServletResponse
            response, final User user, final String uri, final boolean isRendered) throws
            DotSecurityException, DotDataException {
        final Context velocityContext = VelocityUtil.getWebContext(request, response);
        final String pageUri = ('/' == uri.charAt(0)) ? uri : ("/" + uri);
        final String siteName = null == request.getParameter(Host.HOST_VELOCITY_VAR_NAME) ?
                request.getServerName() : request.getParameter(Host.HOST_VELOCITY_VAR_NAME);
        final Host site = this.hostWebAPI.resolveHostName(siteName, user, RESPECT_FE_ROLES);

        final HTMLPageAsset page = (HTMLPageAsset) this.htmlPageAssetAPI.getPageByPath(pageUri,
                site, this.languageAPI.getDefaultLanguage().getId(), true);
        if (isRendered) {
            try {
                page.setProperty("rendered", VelocityUtil.mergeTemplate("/live/" + page
                        .getIdentifier() + "_" + page.getLanguageId() + ".dotpage",
                        velocityContext));
            } catch (Exception e) {
                throw new DotDataException(String.format("Page '%s' could not be rendered via " +
                        "Velocity.", pageUri), e);
            }
        }
        final Template template = (Template) this.versionableAPI.findLiveVersion(page
                .getTemplateId(), user, RESPECT_ANON_PERMISSIONS);
        final List<Container> templateContainers = this.templateAPI.getContainersInTemplate
                (template, user, RESPECT_FE_ROLES);
        final Map<String, ContainerView> mappedContainers = new LinkedHashMap<>();
        for (final Container container : templateContainers) {
            final List<ContainerStructure> containerStructures = this.containerAPI
                    .getContainerStructures(container);
            String rendered = null;
            if (isRendered) {
                try {
                    rendered = VelocityUtil.mergeTemplate("/live/" + container.getIdentifier() +
                            ".container", velocityContext);
                } catch (Exception e) {
                    throw new DotDataException(String.format("Container '%s' could not be " +
                            "rendered via " + "Velocity.", container.getIdentifier()), e);
                }
            }
            mappedContainers.put(container.getIdentifier(), new ContainerView(container,
                    containerStructures, rendered));
        }
        TemplateLayout layout = null;
        if (null != template.getTheme()) {
            final DotTemplateTool dotTemplateTool = new DotTemplateTool();
            dotTemplateTool.init(velocityContext);
            layout = DotTemplateTool.themeLayout(template.getInode());
        }
        return new PageView(site, template, mappedContainers, page, layout);
    }

    /**
     * Converts the specified {@link PageView} object to JSON format.
     *
     * @param pageView The representation of an HTML Page and it associated objects.
     * @return The JSON representation of the {@link PageView}.
     * @throws JsonProcessingException An error occurred when generating the JSON data.
     */
    public String asJson(final PageView pageView) throws JsonProcessingException {
        final ObjectWriter objectWriter = JsonMapper.mapper.writer().withDefaultPrettyPrinter();
        return objectWriter.writeValueAsString(pageView);
    }

}
