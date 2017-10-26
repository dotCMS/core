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
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.liferay.portal.model.User;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Provides different methods to access information about HTML Pages in dotCMS. For example,
 * users of this end-point can get the metadata of an HTML Page (i.e., information about the
 * different data structures that make up a page), the final render of a page, etc.
 *
 * @author Jose Castro
 * @version 4.2
 * @since Oct 6, 2017
 */
@Path("/v1/page")
public class PageResource {

    private final PageResourceHelper pageResourceHelper;
    private final WebResource webResource;

    /**
     * Creates an instance of this REST end-point.
     */
    public PageResource() {
        this(PageResourceHelper.getInstance(), new WebResource());
    }

    @VisibleForTesting
    protected PageResource(final PageResourceHelper pageResourceHelper, final WebResource
            webResource) {
        this.pageResourceHelper = pageResourceHelper;
        this.webResource = webResource;
    }

    /**
     * Returns the metadata in JSON format of the objects that make up an HTML Page in the system.
     * <pre>
     * Format:
     * http://localhost:8080/api/v1/page/json/{page-url}
     * <br/>
     * Example:
     * http://localhost:8080/api/v1/page/json/about-us/locations/index
     * </pre>
     *
     * @param request  The {@link HttpServletRequest} object.
     * @param response The {@link HttpServletResponse} object.
     * @param uri      The path to the HTML Page whose information will be retrieved.
     * @return All the objects on an associated HTML Page.
     */
    @NoCache
    @GET
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Path("/json/{uri: .*}")
    public Response loadJson(@Context final HttpServletRequest request, @Context final
    HttpServletResponse response, @PathParam("uri") final String uri) {
        // Force authentication
        final InitDataObject auth = webResource.init(false, request, true);
        final User user = auth.getUser();
        Response res = null;
        try {
            final PageView pageView = this.pageResourceHelper.getPageMetadata(request, response,
                    user, uri);
            final String json = this.pageResourceHelper.asJson(pageView);
            final Response.ResponseBuilder responseBuilder = Response.ok(json);
            responseBuilder.header("Access-Control-Expose-Headers", "Authorization");
            responseBuilder.header("Access-Control-Allow-Headers", "Origin, X-Requested-With, " +
                    "Content-Type, " + "Accept, Authorization");
            res = responseBuilder.build();
        } catch (JsonProcessingException e) {
            final String errorMsg = "An error occurred when generating the JSON response (" + e
                    .getMessage() + ")";
            Logger.error(this, e.getMessage(), e);
            res = ExceptionMapperUtil.createResponse(null, errorMsg);
        } catch (DotSecurityException e) {
            final String errorMsg = "The user does not have the required permissions (" + e
                    .getMessage() + ")";
            Logger.error(this, errorMsg, e);
            res = ExceptionMapperUtil.createResponse(e, Response.Status.UNAUTHORIZED);
        } catch (DotDataException e) {
            final String errorMsg = "An error occurred when accessing the page information " +
                    "permissions (" + e.getMessage() + ")";
            Logger.error(this, e.getMessage(), e);
            res = ExceptionMapperUtil.createResponse(null, errorMsg);
        } catch (Exception e) {
            final String errorMsg = "An internal error occurred (" + e.getMessage() + ")";
            Logger.error(this, errorMsg, e);
            res = ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
        }
        return res;
    }

    /**
     * Returns the JSON representation of the specified HTML Page, i.e., the source code of the
     * rendered page.
     * <p>
     * <pre>
     * Format:
     * http://localhost:8080/api/v1/page/render/{page-url}
     * <br/>
     * Example:
     * http://localhost:8080/api/v1/page/render/about-us/locations/index
     * </pre>
     *
     * @param request  The {@link HttpServletRequest} object.
     * @param response The {@link HttpServletResponse} object.
     * @param uri      The path to the HTML Page whose information will be retrieved.
     * @return All the <b>rendered</b> objects on an associated HTML Page.
     */
    @NoCache
    @GET
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Path("/render/{uri: .*}")
    public Response renderPage(@Context final HttpServletRequest request, @Context final
    HttpServletResponse response, @PathParam("uri") final String uri) {
        // Force authentication
        final InitDataObject auth = webResource.init(false, request, true);
        final User user = auth.getUser();
        Response res = null;
        try {
            final PageView pageView = this.pageResourceHelper.getPageMetadataRendered(request,
                    response, user, uri);
            final String json = this.pageResourceHelper.asJson(pageView);
            final Response.ResponseBuilder responseBuilder = Response.ok(json);
            responseBuilder.header("Access-Control-Expose-Headers", "Authorization");
            responseBuilder.header("Access-Control-Allow-Headers", "Origin, X-Requested-With, " +
                    "Content-Type, " + "Accept, Authorization");
            res = responseBuilder.build();
        } catch (JsonProcessingException e) {
            final String errorMsg = "An error occurred when generating the JSON response (" + e
                    .getMessage() + ")";
            Logger.error(this, e.getMessage(), e);
            res = ExceptionMapperUtil.createResponse(null, errorMsg);
        } catch (DotSecurityException e) {
            final String errorMsg = "The user does not have the required permissions (" + e
                    .getMessage() + ")";
            Logger.error(this, errorMsg, e);
            res = ExceptionMapperUtil.createResponse(e, Response.Status.UNAUTHORIZED);
        } catch (DotDataException e) {
            final String errorMsg = "An error occurred when accessing the page information (" + e
                    .getMessage() + ")";
            Logger.error(this, e.getMessage(), e);
            res = ExceptionMapperUtil.createResponse(null, errorMsg);
        } catch (Exception e) {
            final String errorMsg = "An internal error occurred (" + e.getMessage() + ")";
            Logger.error(this, errorMsg, e);
            res = ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
        }
        return res;
    }

}
