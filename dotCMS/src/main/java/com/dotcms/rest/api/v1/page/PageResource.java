package com.dotcms.rest.api.v1.page;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.javax.ws.rs.GET;
import com.dotcms.repackage.javax.ws.rs.Path;
import com.dotcms.repackage.javax.ws.rs.PathParam;
import com.dotcms.repackage.javax.ws.rs.Produces;
import com.dotcms.repackage.javax.ws.rs.core.Context;
import com.dotcms.repackage.javax.ws.rs.core.MediaType;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.exception.mapper.ExceptionMapperUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.business.VersionableAPI;
import com.dotmarketing.business.web.HostWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.business.ContainerAPI;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.templates.business.TemplateAPI;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.liferay.portal.model.User;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Path("/v1/page")
public class PageResource {

    private final PageResourceHelper pageResourceHelper;
    private final WebResource webResource;

    private final HostWebAPI hostWebAPI = WebAPILocator.getHostWebAPI();
    private final IdentifierAPI identifierAPI = APILocator.getIdentifierAPI();
    private final HTMLPageAssetAPI htmlPageAssetAPI = APILocator.getHTMLPageAssetAPI();
    private final LanguageAPI languageAPI = APILocator.getLanguageAPI();
    private final VersionableAPI versionableAPI = APILocator.getVersionableAPI();
    private final TemplateAPI templateAPI = APILocator.getTemplateAPI();
    private final ContainerAPI containerAPI = APILocator.getContainerAPI();

    /**
     *
     */
    public PageResource() {
        this(PageResourceHelper.getInstance(), new WebResource());
    }

    @VisibleForTesting
    protected PageResource(PageResourceHelper pageResourceHelper, WebResource webResource) {
        this.pageResourceHelper = pageResourceHelper;
        this.webResource = webResource;
    }

    /**
     * <p>Returns a JSON representation of a page
     * <p/>
     * Usage: /page/{hostOrFolderIdentifier}
     */
    @NoCache
    @GET
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Path("/json/{uri: .*}")
    public Response loadJson(@Context HttpServletRequest request, @Context HttpServletResponse
            response, @PathParam("uri") String uri) {
        // Force authentication
        final InitDataObject auth = webResource.init(false, request, false);
        final User user = auth.getUser();
        Response res = null;
        try {
            final PageView pageView = this.pageResourceHelper.getPageData(request, response,
                    user, uri);
            final String json = this.pageResourceHelper.asJson(pageView);
            final Response.ResponseBuilder responseBuilder = Response.ok(json);
            responseBuilder.header("Access-Control-Expose-Headers", "Authorization");
            responseBuilder.header("Access-Control-Allow-Headers", "Origin, X-Requested-With, " +
                    "Content-Type, " + "Accept, Authorization");
            res = responseBuilder.build();
        } catch (JsonProcessingException e) {
            res = ExceptionMapperUtil.createResponse(null, "An error occurred when generating " +
                    "the JSON response (" + e.getMessage() + ")");
        } catch (DotSecurityException e) {
            res = ExceptionMapperUtil.createResponse(null, "The user does not have the required"
                    + " permissions (" + e.getMessage() + ")");
        } catch (DotDataException e) {
            res = ExceptionMapperUtil.createResponse(null, "An error occurred when accessing " +
                    "the page information permissions (" + e.getMessage() + ")");
        } catch (Exception e) {
            res = ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
        }
        return res;
    }

    @NoCache
    @GET
    @Path("/render/{uri: .*}")
    public Response renderPage(@Context HttpServletRequest request, @Context HttpServletResponse
            response, @PathParam("uri") String uri) throws Exception {
        // Force authentication
        final InitDataObject auth = webResource.init(false, request, false);
        final User user = auth.getUser();
        Response res = null;
        try {
            final PageView pageView = this.pageResourceHelper.getPageDataRendered(request,
                    response, user, uri);
            final String json = this.pageResourceHelper.asJson(pageView);
            final Response.ResponseBuilder responseBuilder = Response.ok(json);
            responseBuilder.header("Access-Control-Expose-Headers", "Authorization");
            responseBuilder.header("Access-Control-Allow-Headers", "Origin, X-Requested-With, " +
                    "Content-Type, " + "Accept, Authorization");
            res = responseBuilder.build();
        } catch (JsonProcessingException e) {
            res = ExceptionMapperUtil.createResponse(null, "An error occurred when generating " +
                    "the JSON response (" + e.getMessage() + ")");
        } catch (DotSecurityException e) {
            res = ExceptionMapperUtil.createResponse(null, "The user does not have the required"
                    + " permissions (" + e.getMessage() + ")");
        } catch (DotDataException e) {
            res = ExceptionMapperUtil.createResponse(null, "An error occurred when accessing " +
                    "the page information permissions (" + e.getMessage() + ")");
        } catch (Exception e) {
            res = ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
        }
        return res;
    }

}
