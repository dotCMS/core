package com.dotcms.rest.api.v1.page;

import com.dotmarketing.beans.ContainerStructure;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.IdentifierAPI;
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
 * @author Will Ezell
 * @author Jose Castro
 * @version 4.2
 * @since Oct 6, 2017
 */
public class PageResourceHelper implements Serializable {

    private final HostWebAPI hostWebAPI = WebAPILocator.getHostWebAPI();
    private final IdentifierAPI identifierAPI = APILocator.getIdentifierAPI();
    private final HTMLPageAssetAPI htmlPageAssetAPI = APILocator.getHTMLPageAssetAPI();
    private final LanguageAPI languageAPI = APILocator.getLanguageAPI();
    private final VersionableAPI versionableAPI = APILocator.getVersionableAPI();
    private final TemplateAPI templateAPI = APILocator.getTemplateAPI();
    private final ContainerAPI containerAPI = APILocator.getContainerAPI();

    private static final boolean RESPECT_FE_ROLES = Boolean.TRUE;
    private static final boolean RESPECT_ANON_PERMISSIONS = Boolean.TRUE;

    /**
     *
     */
    private static class SingletonHolder {
        private static final PageResourceHelper INSTANCE = new PageResourceHelper();
    }

    /**
     * @return
     */
    public static PageResourceHelper getInstance() {
        return PageResourceHelper.SingletonHolder.INSTANCE;
    }

    /**
     * @param request
     * @param response
     * @param user
     * @param uri
     * @return
     * @throws DotSecurityException
     * @throws DotDataException
     */
    public PageView getPageData(final HttpServletRequest request, final HttpServletResponse
            response, final User user, final String uri) throws DotSecurityException,
            DotDataException {
        return getPageData(request, response, user, uri, Boolean.FALSE);
    }

    /**
     * @param request
     * @param response
     * @param user
     * @param uri
     * @return
     * @throws DotSecurityException
     * @throws DotDataException
     */
    public PageView getPageDataRendered(final HttpServletRequest request, final
    HttpServletResponse response, final User user, final String uri) throws DotSecurityException,
            DotDataException {
        return getPageData(request, response, user, uri, Boolean.TRUE);
    }

    /**
     * @param request
     * @param response
     * @param user
     * @param uri
     * @param isRendered
     * @return
     * @throws DotSecurityException
     * @throws DotDataException
     */
    private PageView getPageData(final HttpServletRequest request, final HttpServletResponse
            response, final User user, final String uri, final boolean isRendered) throws
            DotSecurityException, DotDataException {
        final Context velocityContext = VelocityUtil.getWebContext(request, response);
        final String pageUri = (uri.startsWith("/")) ? uri : ("/" + uri);
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
        for (Container container : templateContainers) {
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
            layout = dotTemplateTool.themeLayout(template.getInode());
        }

        final PageView pageView = new PageView(site, template, mappedContainers, page, layout);
        return pageView;
    }

    /**
     *
     * @param pageView
     * @return
     * @throws JsonProcessingException
     */
    public String asJson(final PageView pageView) throws JsonProcessingException {
        final ObjectWriter objectWriter = JsonMapper.mapper.writer().withDefaultPrettyPrinter();
        final String json = objectWriter.writeValueAsString(pageView);
        return json;
    }

}
