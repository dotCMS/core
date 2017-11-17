package com.dotcms.rest.api.v1.page;

import com.dotcms.contenttype.transform.JsonTransformer;
import com.dotcms.repackage.groovy.json.JsonException;
import com.dotcms.rest.exception.BadRequestException;
import com.dotcms.rest.exception.NotFoundException;
import com.dotmarketing.beans.ContainerStructure;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.VersionableAPI;
import com.dotmarketing.business.web.HostWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.business.ContainerAPI;
import com.dotmarketing.portlets.containers.model.Container;

import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.business.web.ContentletWebAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.templates.business.TemplateAPI;
import com.dotmarketing.portlets.templates.design.bean.TemplateLayout;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.VelocityUtil;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;
import com.dotmarketing.viewtools.DotTemplateTool;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.User;
import org.apache.velocity.context.Context;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.dotcms.api.web.HttpServletRequestThreadLocal;

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

    private static final String REQUEST_LAYOUT_PROPERTY_NAME = "layout";
    private static final String REQUEST_TITLE_PROPERTY_NAME = "title";
    private static final String REQUEST_THEME_PROPERTY_NAME = "theme";
    private static final String REQUEST_HOST_ID_PROPERTY_NAME = "hostId";

    private final HostWebAPI hostWebAPI = WebAPILocator.getHostWebAPI();
    private final HTMLPageAssetAPI htmlPageAssetAPI = APILocator.getHTMLPageAssetAPI();
    private final LanguageAPI languageAPI = APILocator.getLanguageAPI();
    private final VersionableAPI versionableAPI = APILocator.getVersionableAPI();
    private final TemplateAPI templateAPI = APILocator.getTemplateAPI();
    private final ContainerAPI containerAPI = APILocator.getContainerAPI();
    private final ContentletAPI contentletAPI = APILocator.getContentletAPI();
    private final HostAPI hostAPI = APILocator.getHostAPI();
    private final LanguageAPI langAPI = APILocator.getLanguageAPI();

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
            response, final User user, final String uri, boolean live) throws DotSecurityException,
            DotDataException {
        return getPageMetadata(request, response, user, uri, false, live);
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
    HttpServletResponse response, final User user, final String uri, boolean live) throws DotSecurityException,
            DotDataException {
        return getPageMetadata(request, response, user, uri, true, live);
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
            response, final User user, final String uri, final boolean isRendered, boolean live) throws
            DotSecurityException, DotDataException {
        final Context velocityContext = VelocityUtil.getWebContext(request, response);

        final String siteName = null == request.getParameter(Host.HOST_VELOCITY_VAR_NAME) ?
                request.getServerName() : request.getParameter(Host.HOST_VELOCITY_VAR_NAME);
        final Host site = this.hostWebAPI.resolveHostName(siteName, user, RESPECT_FE_ROLES);

        final String pageUri = ('/' == uri.charAt(0)) ? uri : ("/" + uri);
        final HTMLPageAsset page =  (HTMLPageAsset) this.htmlPageAssetAPI.getPageByPath(pageUri,
                site, this.languageAPI.getDefaultLanguage().getId(), false);
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

        Template template = live ? (Template) this.versionableAPI.findLiveVersion(page.getTemplateId(), user, RESPECT_ANON_PERMISSIONS) :
                (Template) this.versionableAPI.findWorkingVersion(page.getTemplateId(), user, RESPECT_ANON_PERMISSIONS);

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

    public Template saveTemplate(final User user, String pageId, final String requestJson)
            throws BadRequestException, DotDataException, DotSecurityException, IOException {

        final Contentlet page = this.contentletAPI.findContentletByIdentifier(pageId, false,
                langAPI.getDefaultLanguage().getId(), user, false);

        if (page == null) {
            throw new NotFoundException("An error occurred when proccessing the JSON request");
        }

        try {
            Template templateSaved = this.saveTemplate(page, user, requestJson);

            String templateId = page.getStringProperty(HTMLPageAssetAPI.TEMPLATE_FIELD);

            if (!templateId.equals( templateSaved.getIdentifier() )) {
                page.setStringProperty(Contentlet.INODE_KEY, null);
                page.setStringProperty(HTMLPageAssetAPI.TEMPLATE_FIELD, templateSaved.getIdentifier());
                this.contentletAPI.checkin(page, user, false);
            }

            return templateSaved;
        } catch (Exception e) {
            throw new DotRuntimeException(e);
        }
    }

    public Template saveTemplate(Contentlet page, final User user, final String requestJson)
            throws BadRequestException, DotDataException, DotSecurityException, IOException {

        try {
            final JSONObject  requestJsonObject = new JSONObject(requestJson);

            Host host = getHost(requestJsonObject, user);

            validateLayoutJson(requestJsonObject);
            String newTemplateName = requestJsonObject.has(REQUEST_TITLE_PROPERTY_NAME) ?
                    requestJsonObject.get(REQUEST_TITLE_PROPERTY_NAME).toString() : null;

            Template template = getTemplate(page, user, newTemplateName);
            setTemplatePropertiesFromJson(template, requestJsonObject);

            return this.templateAPI.saveTemplate(template, host, user, false);
        } catch (JSONException e) {
            throw new BadRequestException(e, "An error occurred when proccessing the JSON request");
        } catch (Exception e) {
            throw new DotRuntimeException(e);
        }
    }

    public Template saveTemplate(final User user, final String requestJson)
            throws BadRequestException, DotDataException, DotSecurityException, IOException {
        return this.saveTemplate(null, user, requestJson);
    }

    private Template getTemplate(Contentlet page, User user, String newTemplateName) throws DotDataException, DotSecurityException {

        Template result = null;
        String templateId = page != null ? page.getStringProperty(HTMLPageAssetAPI.TEMPLATE_FIELD) : null;

        if (UtilMethods.isSet(templateId)) {
            result = this.templateAPI.findWorkingTemplate(templateId, user, false);

            if (!UtilMethods.isSet(newTemplateName) && !result.isAnonymous()) {
                result = new Template();
            }
        } else {
            result = new Template();
        }

        return result;
    }

    private Host getHost(JSONObject requestJsonObject, User user) {
        try {
            if (requestJsonObject.has(REQUEST_HOST_ID_PROPERTY_NAME)) {
                String hostId = requestJsonObject.remove(REQUEST_HOST_ID_PROPERTY_NAME).toString();

                return hostAPI.find(hostId, user, false);
            } else {
                return hostWebAPI.getCurrentHost(HttpServletRequestThreadLocal.INSTANCE.getRequest());
            }
        } catch (DotDataException | DotSecurityException | PortalException | SystemException e) {
            throw new DotRuntimeException(e);
        }
    }

    private void setTemplatePropertiesFromJson(Template template, JSONObject requestJsonObject)  {
        try {
            String jsonLayout = requestJsonObject.get(REQUEST_LAYOUT_PROPERTY_NAME).toString();

            if (jsonLayout == null) {
                throw new BadRequestException("An error occurred when proccessing the JSON request");
            }

            if (requestJsonObject.has(REQUEST_TITLE_PROPERTY_NAME)) {
                template.setTitle(requestJsonObject.get(REQUEST_TITLE_PROPERTY_NAME).toString());
            }

            if (requestJsonObject.has(REQUEST_THEME_PROPERTY_NAME)) {
                template.setTheme(requestJsonObject.get(REQUEST_THEME_PROPERTY_NAME).toString());
            }

            template.setDrawedBody(jsonLayout);
        } catch ( JSONException e) {
            throw new BadRequestException(e, "An error occurred when proccessing the JSON request");
        }
    }

    private String validateLayoutJson(JSONObject requestJsonObject) throws BadRequestException, IOException {

        try {

            if (!requestJsonObject.has(REQUEST_LAYOUT_PROPERTY_NAME) || requestJsonObject.has("body")
                    || requestJsonObject.has("drawedBody")) {

                throw new BadRequestException("An error occurred when proccessing the JSON request");
            }

            String jsonLayout = requestJsonObject.get(REQUEST_LAYOUT_PROPERTY_NAME).toString();
            JsonTransformer.mapper.readValue(jsonLayout, TemplateLayout.class);
            return jsonLayout;
        } catch (JSONException e) {
            throw new BadRequestException(e, "An error occurred when proccessing the JSON request");
        }
    }
}
