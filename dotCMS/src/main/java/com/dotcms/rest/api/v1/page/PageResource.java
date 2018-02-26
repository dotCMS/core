package com.dotcms.rest.api.v1.page;


import com.dotcms.business.WrapInTransaction;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.javax.ws.rs.Consumes;
import com.dotcms.repackage.javax.ws.rs.DefaultValue;
import com.dotcms.repackage.javax.ws.rs.GET;
import com.dotcms.repackage.javax.ws.rs.POST;
import com.dotcms.repackage.javax.ws.rs.Path;
import com.dotcms.repackage.javax.ws.rs.PathParam;
import com.dotcms.repackage.javax.ws.rs.Produces;
import com.dotcms.repackage.javax.ws.rs.QueryParam;
import com.dotcms.repackage.javax.ws.rs.core.Context;
import com.dotcms.repackage.javax.ws.rs.core.MediaType;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.org.glassfish.jersey.server.JSONP;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.api.v1.page.PageContainerForm.ContainerEntry;
import com.dotcms.rest.exception.BadRequestException;
import com.dotcms.rest.exception.ForbiddenException;
import com.dotcms.rest.exception.NotFoundException;
import com.dotcms.rest.exception.mapper.ExceptionMapperUtil;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.PermissionLevel;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.MultiTreeFactory;
import com.dotmarketing.portlets.contentlet.business.DotLockException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.templates.business.TemplateAPI;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UUIDUtil;
import com.dotmarketing.util.WebKeys;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.liferay.portal.model.User;
import org.jetbrains.annotations.NotNull;

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
    private final TemplateAPI templateAPI;
    private final PermissionAPI permissionAPI;

    /**
     * Creates an instance of this REST end-point.
     */
    public PageResource() {
        this(PageResourceHelper.getInstance(), new WebResource(), APILocator.getTemplateAPI(),
                APILocator.getPermissionAPI());
    }

    @VisibleForTesting
    protected PageResource(final PageResourceHelper pageResourceHelper, final WebResource webResource,
                           TemplateAPI templateAPI, PermissionAPI permissionAPI) {
        this.pageResourceHelper = pageResourceHelper;
        this.webResource = webResource;
        this.templateAPI = templateAPI;
        this.permissionAPI = permissionAPI;
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
    public Response loadJson(@Context final HttpServletRequest request, @Context final HttpServletResponse response,
                             @PathParam("uri") final String uri,
                             @QueryParam("live") @DefaultValue("true")  final boolean live) {
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
            throw new ForbiddenException(e);
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
            throw new ForbiddenException(e);
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
    public Response renderHTMLOnly(@Context final HttpServletRequest request,
                                   @Context final HttpServletResponse response,
                                   @PathParam("uri") final String uri,
                                   @QueryParam("mode") @DefaultValue("LIVE_ADMIN") final String modeStr) {

        Logger.debug(this, String.format("Rendering page: uri -> %s mode-> %s", uri, modeStr));

        // Force authentication
        final InitDataObject auth = webResource.init(false, request, true);
        final User user = auth.getUser();
        Response res = null;

        final PageMode mode = PageMode.get(modeStr);
        PageMode.setPageMode(request, mode);
        try {

            final HTMLPageAsset page = (UUIDUtil.isUUID(uri)) ?
                    (HTMLPageAsset) APILocator.getHTMLPageAssetAPI().findPage(uri, user, mode.respectAnonPerms) :
                    this.pageResourceHelper.getPage(request, user, uri, mode);

            final ContentletVersionInfo info = APILocator.getVersionableAPI().getContentletVersionInfo(page.getIdentifier(), page.getLanguageId());
            final Builder<String, Object> pageMapBuilder = ImmutableMap.builder();

            final Host host = APILocator.getHostAPI().find(page.getHost(), user, mode.respectAnonPerms);
            
            request.setAttribute(WebKeys.CURRENT_HOST, host);
            request.getSession().setAttribute(WebKeys.CURRENT_HOST, host);
            final String html = this.pageResourceHelper.getPageRendered(page, request, response, user, mode);

            pageMapBuilder.put("render",html )
                .put("workingInode", info.getWorkingInode())
                .put("shortyWorking", APILocator.getShortyAPI().shortify(info.getWorkingInode()))
                .put("canEdit", this.permissionAPI.doesUserHavePermission(page, PermissionLevel.EDIT.getType(), user));

            final Map<String, Object> pageMap = new HashMap(page.getMap());
            final String templateIdentifier = (String) pageMap.get(HTMLPageAssetAPI.TEMPLATE_FIELD);
            pageMap.remove(HTMLPageAssetAPI.TEMPLATE_FIELD);

            pageMapBuilder.putAll(pageMap);
            pageMapBuilder.put("template", getTemplateAtrributes(user, page, templateIdentifier));
            pageMapBuilder.putAll(getLockMap(user, page, info));

            if(info.getLiveInode()!=null) {
                pageMapBuilder.put("liveInode", info.getLiveInode())
                .put("shortyLive", APILocator.getShortyAPI().shortify(info.getLiveInode()));
            }
            final Response.ResponseBuilder responseBuilder = Response.ok(pageMapBuilder.build());
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
            throw new ForbiddenException(e);
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

    @NotNull
    private Map<String, Object> getLockMap(User user, HTMLPageAsset page, ContentletVersionInfo info) throws DotDataException, DotSecurityException {
        final Builder<String, Object> lockMap = ImmutableMap.builder();
        final boolean canLock  = canLock(user, page);
        final String lockedBy= (info.getLockedBy()!=null)  ? info.getLockedBy() : null;
        final String lockedUserName = (info.getLockedBy()!=null)  ? APILocator.getUserAPI().loadUserById(info.getLockedBy()).getFullName() : null;

        lockMap.put("canLock", canLock);

        if(lockedBy!=null) {
            lockMap.put("lockedOn", info.getLockedOn())
                .put("lockedBy", lockedBy)
                .put("lockedByName", lockedUserName);
        }
        return lockMap.build();
    }

    private ImmutableMap<Object, Object> getTemplateAtrributes(User user, HTMLPageAsset page, String templateIdentifier)
            throws DotDataException, DotSecurityException {

        Template template = this.templateAPI.findWorkingTemplate(page.getTemplateId(), APILocator.getUserAPI().getSystemUser(), false);
        return ImmutableMap.builder().put("drawed", template.isDrawed())
                .put("canEdit", this.permissionAPI.doesUserHavePermission(template, PermissionLevel.EDIT.getType(), user))
                .put("id", templateIdentifier)
                .build();
    }

    private boolean canLock(User user, HTMLPageAsset page)  {
        try {
            APILocator.getContentletAPI().canLock(page, user);
            return true;
        } catch (DotLockException e) {
            return false;
        }
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
    public Response saveLayout(@Context final HttpServletRequest request,
                               @Context final HttpServletResponse response,
                               @PathParam("pageId") final String pageId,
                               final PageForm form) {

        final InitDataObject auth = webResource.init(false, request, true);
        final User user = auth.getUser();

        Response res = null;

        try {
            IHTMLPage page = this.pageResourceHelper.getPage(user, pageId);
            this.pageResourceHelper.saveTemplate(user, page, form);

            final PageView pageView = this.pageResourceHelper.getPageMetadataRendered(request, response, user,
                    page.getURI(), false);
            final String json = this.pageResourceHelper.asJson(pageView);

            res = Response.ok(new ResponseEntityView(json)).build();

        } catch (DotSecurityException e) {
            final String errorMsg = String.format("DotSecurityException on PageResource.saveLayout, parameters:  %s, %s %s: ",
                    request, pageId, form);
            Logger.error(this, errorMsg, e);
            throw new ForbiddenException(e);
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
            throw new ForbiddenException(e);
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

    /**
     * Update all the content's page receive a json object with the follow format:
     *
     * {
     *     container_1_id: [contentlet_1_id, contentlet_2_id,...,contentlet_n_id],
     *     container_2_id: [contentlet_1_id, contentlet_2_id,...,contentlet_n_id],
     *     ...
     *     container_n_id: [contentlet_1_id, contentlet_2_id,...,contentlet_n_id],
     * }
     *
     * where:
     *
     * - container_1_id, container_2_id,..., container_n_id: Each container's identifier
     * - contentlet_1_id, contentlet_2_id,...,contentlet_n_id: each contentlet identifier
     *
     * @param req
     * @param pageId
     * @param pageContainerForm
     * @return
     */
    @POST
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{pageId}/content")
    public final Response addContent(@Context final HttpServletRequest req, @PathParam("pageId") final String pageId,
                                     final PageContainerForm pageContainerForm) {

        Logger.debug(this, String.format("Saving page's content: %s", pageContainerForm.getRequestJson()));

        final InitDataObject initData = webResource.init(true, req, true);
        final User user = initData.getUser();
        Response res = null;

        try {
            final IHTMLPage page = pageResourceHelper.getPage(user, pageId);
            if (page == null) {
                return ExceptionMapperUtil.createResponse(Response.Status.BAD_REQUEST);
            }

            APILocator.getPermissionAPI().checkPermission(page, PermissionLevel.EDIT, user);
            pageResourceHelper.saveContent(pageId, pageContainerForm.getContainerEntries());

            res = Response.ok(new ResponseEntityView("ok")).build();
        } catch (DotSecurityException e) {
            Logger.error(this, e.getMessage(), e);
            throw new ForbiddenException(e);
        } catch (DotDataException e) {
            res = ExceptionMapperUtil.createResponse(e, Response.Status.BAD_REQUEST);
            Logger.error(this, e.getMessage(), e);
        }

        return res;
    }
}
