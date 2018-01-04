package com.dotcms.rest.api.v1.page;


import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.javax.ws.rs.*;
import com.dotcms.repackage.javax.ws.rs.core.Context;
import com.dotcms.repackage.javax.ws.rs.core.MediaType;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.org.glassfish.jersey.server.JSONP;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.exception.BadRequestException;
import com.dotcms.rest.exception.NotFoundException;
import com.dotcms.rest.exception.mapper.ExceptionMapperUtil;
import com.dotcms.uuid.shorty.ShortType;
import com.dotcms.uuid.shorty.ShortyId;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionLevel;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.MultiTreeFactory;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableMap;
import com.liferay.portal.model.User;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

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
     * @param live     If it is false look for live and working page version, otherwise just look for live version,
     *                 true is the default value
     * @return All the objects on an associated HTML Page.
     */
    @NoCache
    @GET
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Path("/json/{uri: .*}")
    public Response loadJson(@Context final HttpServletRequest request, @Context final
    HttpServletResponse response, @PathParam("uri") final String uri, @QueryParam("live") @DefaultValue("true")  final boolean live) {
        // Force authentication
        final InitDataObject auth = webResource.init(false, request, true);
        final User user = auth.getUser();
        Response res = null;
        try {
            final PageView pageView = this.pageResourceHelper.getPageMetadata(request, response,
                    user, uri, live);
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
     * @param live     If it is false look for live and working page version, otherwise just look for live version,
     *                 true is the default value
     * @return All the <b>rendered</b> objects on an associated HTML Page.
     */
    @NoCache
    @GET
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Path("/render/{uri: .*}")
    public Response renderPageObject(@Context final HttpServletRequest request, @Context final
    HttpServletResponse response, @PathParam("uri") final String uri, @QueryParam("live") @DefaultValue("true")  final boolean live) {
        // Force authentication
        final InitDataObject auth = webResource.init(false, request, true);
        final User user = auth.getUser();
        Response res = null;
        try {
            final PageView pageView = this.pageResourceHelper.getPageMetadataRendered(request,
                    response, user, uri, live);
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
    

    @NoCache
    @GET
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Path("/renderHTML/{uri: .*}")
    public Response renderHTMLOnly(@Context final HttpServletRequest request, @Context final
    HttpServletResponse response, @PathParam("uri") final String uri, @QueryParam("mode") @DefaultValue("LIVE_ADMIN") String modeStr) {
        // Force authentication
        final InitDataObject auth = webResource.init(false, request, true);
        final User user = auth.getUser();
        Response res = null;

        PageMode mode = PageMode.get(modeStr);
        PageMode.setPageMode(request, mode);
        try {

            HTMLPageAsset page = this.pageResourceHelper.getPage(request, user, uri, mode);
            final String html = this.pageResourceHelper.getPageRendered(page, request, response, user, mode);
            final Response.ResponseBuilder responseBuilder = Response.ok(ImmutableMap.of("render",html, "identifier",
                    page.getIdentifier(), "inode", page.getInode()));
            responseBuilder.header("Access-Control-Expose-Headers", "Authorization");
            responseBuilder.header("Access-Control-Allow-Headers", "Origin, X-Requested-With, " +
                    "Content-Type, " + "Accept, Authorization");
            res = responseBuilder.build();
        } catch (JsonProcessingException e) {
            final String errorMsg = "An error occurred when generating the JSON response (" + e.getMessage() + ")";
            Logger.error(this, e.getMessage(), e);
            res = ExceptionMapperUtil.createResponse(null, errorMsg);
        } catch (DotSecurityException e) {
            PageMode.setPageMode(request, PageMode.ADMIN_MODE);
            final String errorMsg = "The user does not have the required permissions (" + e.getMessage() + ")";
            Logger.error(this, errorMsg, e);
            res = ExceptionMapperUtil.createResponse(e, Response.Status.UNAUTHORIZED);
        } catch (DotDataException e) {
            final String errorMsg = "An error occurred when accessing the page information (" + e.getMessage() + ")";
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
     * Save a template and link it with a page, If the page already has a anonymous template linked then it is updated,
     * otherwise a new template is created and the old link template remains unchanged
     *
     * @see Template#isAnonymous()
     *
     * @param request The {@link HttpServletRequest} object.
     * @param pageId page's Id to link the template
     * @param form The {@link PageForm}
     * @return
     */
    @NoCache
    @POST
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Path("/{pageId}/layout")
    public Response saveLayout(@Context final HttpServletRequest request, @PathParam("pageId") final String pageId,
                               final PageForm form) {

        final InitDataObject auth = webResource.init(false, request, true);
        final User user = auth.getUser();

        Response res = null;

        try {

            final Template templateSaved = this.pageResourceHelper.saveTemplate(user, pageId, form);
            res = Response.ok(new ResponseEntityView(templateSaved)).build();

        } catch (DotSecurityException e) {
            final String errorMsg = String.format("DotSecurityException on PageResource.saveLayout, parameters:  %s, %s %s: ",
                    request, pageId, form);
            Logger.error(this, errorMsg, e);
            res = ExceptionMapperUtil.createResponse(e, Response.Status.UNAUTHORIZED);
        } catch(NotFoundException e) {
            final String errorMsg = String.format("NotFoundException on PageResource.saveLayout, parameters:  %s, %s %s: ",
                    request, pageId, form);
            Logger.error(this, errorMsg, e);
            res = ExceptionMapperUtil.createResponse(e, Response.Status.NOT_FOUND);
        } catch (BadRequestException | DotDataException e) {
            final String errorMsg = String.format("%s on PageResource.saveLayout, parameters:  %s, %s %s: ",
                    e.getClass().getCanonicalName(), request, pageId, form);
            Logger.error(this, errorMsg, e);
            res = ExceptionMapperUtil.createResponse(e, Response.Status.BAD_REQUEST);
        } catch (IOException e) {
            final String errorMsg = String.format("IOException on PageResource.saveLayout, parameters:  %s, %s %s: ",
                    request, pageId, form);
            Logger.error(this, errorMsg, e);
            res = ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
        }

        return res;
    }

    /**
     * Save a template.
     *
     * @see Template#isAnonymous()
     *
     * @param request The {@link HttpServletRequest} object.
     * @param form The {@link PageForm}
     * @return
     */
    @NoCache
    @POST
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Path("/layout")
    public Response saveLayout(@Context final HttpServletRequest request, final PageForm form) {
        final InitDataObject auth = webResource.init(false, request, true);
        final User user = auth.getUser();

        Response res = null;

        try {

            final Template templateSaved = this.pageResourceHelper.saveTemplate(user, form);
            res = Response.ok(new ResponseEntityView(templateSaved)).build();

        } catch (DotSecurityException e) {
            final String errorMsg = String.format("DotSecurityException on PageResource.saveLayout, parameters:  %s, %s: ",
                    request, form);
            Logger.error(this, errorMsg, e);
            res = ExceptionMapperUtil.createResponse(e, Response.Status.UNAUTHORIZED);
        } catch (BadRequestException | DotDataException e) {
            final String errorMsg = String.format("%s on PageResource.saveLayout, parameters:  %s, %s: ",
                    e.getClass().getCanonicalName(), request, form);
            Logger.error(this, errorMsg, e);
            res = ExceptionMapperUtil.createResponse(e, Response.Status.BAD_REQUEST);
        } catch (IOException e) {
            final String errorMsg = String.format("IOException on PageResource.saveLayout, parameters:  %s, %s: ",
                    request, form);
            Logger.error(this, errorMsg, e);
            res = ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
        }

        return res;
    }

    @POST
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Path("{pageId}/add/container/{containerId}/content/{contentletId}/uid/{uid}/order/{order}")
    public final Response addContentToContainer(@Context final HttpServletRequest req, @Context final HttpServletResponse res,
                                                @PathParam("containerId") final String containerId, @PathParam("contentletId") final String contentletId,
                                                @PathParam("order") final int order, @PathParam("uid") final String uid,
                                                @PathParam("pageId") final String pageId) throws DotDataException,
            DotSecurityException, ParseErrorException, MethodInvocationException, ResourceNotFoundException, IOException,
            IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException {


        final InitDataObject initData = webResource.init(true, req, true);
        final User user = initData.getUser();
        final PageMode mode = PageMode.get(req);
        final Language id = WebAPILocator.getLanguageWebAPI()
                .getLanguage(req);

        final Contentlet contentlet = pageResourceHelper.getContentlet(user, mode, id, contentletId);
        Container container = pageResourceHelper.getContainer(containerId, user, mode);
        Contentlet page = pageResourceHelper.getPage(user, pageId);

        if (page == null || contentlet == null || container == null) {
            return ExceptionMapperUtil.createResponse(Response.Status.BAD_REQUEST);
        }

        pageResourceHelper.checkPagePermission(user, page);
        pageResourceHelper.checkPermission(user, contentlet, container);

        MultiTree mt = new MultiTree().setContainer(containerId)
                .setContentlet(contentletId)
                .setRelationType(uid)
                .setTreeOrder(order)
                .setHtmlPage(page.getIdentifier());

        MultiTreeFactory.saveMultiTree(mt);


        return Response.ok("ok")
                .build();
    }

    @POST
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Path("{pageId}/content")
    public final Response addContent(@Context final HttpServletRequest req, @PathParam("pageId") final String pageId,
                                     PageContainerForm pageContainerForm) {

        final InitDataObject initData = webResource.init(true, req, true);
        final User user = initData.getUser();
        Response res = null;

        try {
            Contentlet page = pageResourceHelper.getPage(user, pageId);
            if (page == null) {
                return ExceptionMapperUtil.createResponse(Response.Status.BAD_REQUEST);
            }

            pageResourceHelper.checkPagePermission(user, page);
            pageResourceHelper.saveContent(pageId, pageContainerForm.getContainerEntries());

            res = Response.ok("ok").build();
        } catch (DotSecurityException e) {
            res = ExceptionMapperUtil.createResponse(e, Response.Status.UNAUTHORIZED);
        } catch (DotDataException e) {
            res = ExceptionMapperUtil.createResponse(e, Response.Status.BAD_REQUEST);
        }

        return res;
    }
}
