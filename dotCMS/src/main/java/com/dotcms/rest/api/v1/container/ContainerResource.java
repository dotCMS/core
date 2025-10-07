package com.dotcms.rest.api.v1.container;

import com.dotcms.business.WrapInTransaction;
import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.rendering.velocity.services.ContainerLoader;
import com.dotcms.rendering.velocity.services.VelocityResourceKey;
import com.dotcms.rendering.velocity.util.VelocityUtil;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.api.BulkResultView;
import com.dotcms.rest.api.FailedResultView;
import com.dotcms.rest.exception.ForbiddenException;
import com.dotcms.rest.exception.mapper.ExceptionMapperUtil;
import com.dotcms.util.DotPreconditions;
import com.dotcms.util.JsonUtil;
import com.dotcms.util.PaginationUtil;
import com.dotcms.util.pagination.ContainerPaginator;
import com.dotcms.util.pagination.OrderDirection;
import com.dotcms.uuid.shorty.ShortType;
import com.dotcms.uuid.shorty.ShortyId;
import com.dotcms.uuid.shorty.ShortyIdAPI;
import com.dotmarketing.beans.ContainerStructure;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionLevel;
import com.dotmarketing.business.VersionableAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.business.ContainerAPI;
import com.dotmarketing.portlets.containers.business.FileAssetContainerUtil;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.containers.model.ContainerView;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.form.business.FormAPI;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.ActivityLogger;
import com.dotmarketing.util.Constants;
import com.dotmarketing.util.HostUtil;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.commons.io.IOUtils;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.glassfish.jersey.server.JSONP;

/**
 * This resource provides all the different end-points associated to information and actions that
 * the front-end can perform on the {@link com.dotmarketing.portlets.containers.model.Container}.
 *
 */
@Path("/v1/containers")
@Tag(name = "Containers", description = "Endpoints for managing Container objects and their content")
public class ContainerResource implements Serializable {

    private static final long serialVersionUID = 1L;

    private final PaginationUtil paginationUtil;
    private final WebResource    webResource;
    private final FormAPI        formAPI;
    private final ContainerAPI   containerAPI;
    private final VelocityUtil   velocityUtil;
    private final ShortyIdAPI    shortyAPI;
    private final ContentletAPI  contentletAPI;

    public ContainerResource() {
        this(new WebResource(),
                new PaginationUtil(new ContainerPaginator()),
                APILocator.getFormAPI(),
                APILocator.getContainerAPI(),
                APILocator.getVersionableAPI(),
                VelocityUtil.getInstance(),
                APILocator.getShortyAPI(),
                APILocator.getContentletAPI());
    }

    @VisibleForTesting
    public ContainerResource(final WebResource    webResource,
                             final PaginationUtil paginationUtil,
                             final FormAPI        formAPI,
                             final ContainerAPI   containerAPI,
                             final VersionableAPI versionableAPI,
                             final VelocityUtil   velocityUtil,
                             final ShortyIdAPI    shortyAPI,
                             final ContentletAPI  contentletAPI) {

        this.webResource    = webResource;
        this.paginationUtil = paginationUtil;
        this.formAPI        = formAPI;
        this.containerAPI   = containerAPI;
        this.velocityUtil   = velocityUtil;
        this.shortyAPI      = shortyAPI;
        this.contentletAPI  = contentletAPI;
    }

    /**
     * Return a list of {@link com.dotmarketing.portlets.containers.model.Container}, entity
     * response syntax:.
     *
     * <code> { contentTypes: array of Container total: total number of Containers } <code/>
     *
     * Url syntax:
     * api/v1/container?filter=filter-string&page=page-number&per_page=per-page&ordeby=order-field-name&direction=order-direction&host=host-id&system=true|false
     *
     * where:
     *
     * <ul>
     * <li>filter-string: just return Container who content this pattern into its title</li>
     * <li>page: page to return</li>
     * <li>per_page: limit of items to return</li>
     * <li>ordeby: field to order by</li>
     * <li>direction: asc for upward order and desc for downward order</li>
     * <li>host: filter by host's id</li>
     * <li>system: If the System Container object must be returned, set to {@code true}. Otherwise, set to {@code false}.</li>
     * </ul>
     *
     * Url example: v1/container?filter=test&page=2&orderby=title
     *
     * @param httpRequest
     * @return
     */
    @GET
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Operation(
            operationId = "getContainers",
            summary = "Retrieves a paginated list of Containers",
            description = "Returns a list of Container objects based on filtering and pagination parameters. " +
                    "Containers are layout components that define how content is displayed on pages.",
            tags = {"Containers"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Containers retrieved successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityView.class))),
                    @ApiResponse(responseCode = "400", description = "Bad request - Invalid parameters"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized - User not authenticated"),
                    @ApiResponse(responseCode = "403", description = "Forbidden - User lacks required permissions"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public final Response getContainers(@Context final HttpServletRequest httpRequest,
            @Context final HttpServletResponse  httpResponse,
            @Parameter(description = "Filter containers by title pattern") @QueryParam(PaginationUtil.FILTER) final String filter,
            @Parameter(description = "Page number for pagination (starting from 1)") @QueryParam(PaginationUtil.PAGE) final int page,
            @Parameter(description = "Number of items per page") @QueryParam(PaginationUtil.PER_PAGE) final int perPage,
            @Parameter(description = "Field to order results by") @DefaultValue("title") @QueryParam(PaginationUtil.ORDER_BY) final String orderBy,
            @Parameter(description = "Sort direction: ASC or DESC") @DefaultValue("ASC") @QueryParam(PaginationUtil.DIRECTION) final String direction,
            @Parameter(description = "Filter containers by host ID") @QueryParam(ContainerPaginator.HOST_PARAMETER_ID) final String hostId,
            @Parameter(description = "Include system containers in results") @QueryParam(ContainerPaginator.SYSTEM_PARAMETER_NAME) final Boolean showSystemContainer,
            @Parameter(description = "Include archived containers in results") @QueryParam(ContainerPaginator.ARCHIVE_PARAMETER_NAME) final Boolean showArchiveContainer,
            @Parameter(description = "Filter containers by content type ID or variable name") @QueryParam(ContainerPaginator.CONTENT_TYPE) final String contentTypeIdOrVar) {

        final InitDataObject initData = webResource.init(null, httpRequest, httpResponse, true, null);
        final User user = initData.getUser();
        final Optional<String> checkedHostId = this.checkHost(hostId, user);

        try {
            final Map<String, Object> extraParams = Maps.newHashMap();
            if (checkedHostId.isPresent()) {
                extraParams.put(ContainerPaginator.HOST_PARAMETER_ID, checkedHostId.get());
            }
            extraParams.put(ContainerPaginator.SYSTEM_PARAMETER_NAME, showSystemContainer);
            extraParams.put(ContainerPaginator.ARCHIVE_PARAMETER_NAME, showArchiveContainer);
            extraParams.put(ContainerPaginator.CONTENT_TYPE, contentTypeIdOrVar);

            return this.paginationUtil.getPage(httpRequest, user, filter, page, perPage, orderBy, OrderDirection.valueOf(direction),
                    extraParams);

        } catch (final Exception e) {
            Logger.error(this, e.getMessage(), e);
            return ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    private Optional<String> checkHost(final String hostId, final User user) {

        String checkedHostId = null;
        try {
            if (UtilMethods.isSet(hostId) && null != APILocator.getHostAPI().find(hostId, user, false)) {

                checkedHostId = hostId;
            }
        } catch (DotDataException | DotSecurityException e) {

            checkedHostId = null;
        }

        return (null == checkedHostId)?
                Optional.empty():
                Optional.of(checkedHostId);
    }

    /**
     * Generates the HTML for a container with a particular contentlet
     *
     * @param req
     * @param res
     * @param containerId
     * @param contentletId
     * @param pageInode
     * @return Response
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @GET
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Path("/{containerId}/content/{contentletId}")
    public final Response containerContent(@Context final HttpServletRequest req,
                                           @Context final HttpServletResponse res,
                                           @PathParam("containerId")  final String containerId,
                                           @PathParam("contentletId") final String contentletId,
                                           @QueryParam("pageInode") final String pageInode)
            throws DotDataException {

        final InitDataObject initData = this.webResource.init(req, res, true);
        final User user               = initData.getUser();
        final PageMode mode           = PageMode.EDIT_MODE;

        PageMode.setPageMode(req, PageMode.EDIT_MODE);

        final ShortyId contentShorty = this.shortyAPI
            .getShorty(contentletId)
            .orElseGet(() -> {
                throw new ResourceNotFoundException("Can't find contentlet:" + contentletId);
            });

        try {

            final Contentlet contentlet =
                    (contentShorty.type == ShortType.IDENTIFIER) ?
                            this.contentletAPI.findContentletByIdentifierAnyLanguageAnyVariant(contentShorty.longId):
                            this.contentletAPI.find(contentShorty.longId, user, mode.respectAnonPerms);

            final String html = this.getHTML(req, res, containerId, user, contentlet, pageInode);

            final Map<String, String> response = ImmutableMap.<String, String> builder().put(MessageConstants.RENDER, html).build();

            return Response.ok(new ResponseEntityView(response)).build();
        } catch (DotSecurityException e) {
            throw new ForbiddenException(e);
        }
    }

    /**
     * This method is pretty much the same of {@link #containerContent(HttpServletRequest, HttpServletResponse, String, String, String)}
     * But there is a limitation on the vanity url for the rest call since the container id path parameter is a path itself
     * (for {@link com.dotmarketing.portlets.containers.model.FileAssetContainer)} so we need to pass it by query string
     *
     * <i>Example:</i>
     * <code>
     *     /api/v1/containers/content/27108d63-969e-4086-a405-86777be16230?containerId=/application/containers/large-column
     * </code>
     * @param req {@link HttpServletRequest}
     * @param res {@link HttpServletResponse}
     * @param containerId {@link String}   query string with the container id, could be uuid or path
     * @param contentletId {@link String}  path parameter with the contentlet id
     * @return Response
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @GET
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Path("/content/{contentletId}")
    public final Response containerContentByQueryParam(@Context final HttpServletRequest req,
                                           @Context final HttpServletResponse res,
                                           @QueryParam("containerId") final String containerId,
                                           @QueryParam("pageInode") final String pageInode,
                                           @PathParam("contentletId") final String contentletId)
            throws DotDataException, DotSecurityException {

        return this.containerContent(req, res, containerId, contentletId, pageInode);
    }


    /**
     * This method is pretty much the same of {@link #containerForm(HttpServletRequest, HttpServletResponse, String, String)}
     * But there is a limitation on the vanity url for the rest call since the container id path parameter is a path itself
     * (for {@link com.dotmarketing.portlets.containers.model.FileAssetContainer)} so we need to pass it by query string
     *
     * <i>Example:</i>
     * <code>
     *     /api/v1/containers/form/27108d63-969e-4086-a405-86777be16230?containerId=/application/containers/large-column
     * </code>
     * @param req
     * @param res
     * @param containerId
     * @param formId
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @GET
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Path("/form/{formId}")
    public final Response containerFormByQueryParam(@Context final HttpServletRequest req,
                                                       @Context final HttpServletResponse res,
                                                       @QueryParam("containerId") final String containerId,
                                                       @PathParam("formId") final String formId)
            throws DotDataException, DotSecurityException {

        return this.containerForm(req, res, containerId, formId);
    }

    /**
     * Return a form render into a specific container
     *
     * @param req
     * @param res
     * @param containerId
     * @param formId
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @GET
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Path("/{containerId}/form/{formId}")
    @Operation(
            operationId = "containerForm",
            summary = "Renders a form within a container",
            description = "Returns HTML content for a form rendered within the specified container. " +
                    "This is used to display forms with container styling and layout.",
            tags = {"Containers"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Form rendered successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityView.class))),
                    @ApiResponse(responseCode = "400", description = "Bad request - Invalid container or form ID"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized - User not authenticated"),
                    @ApiResponse(responseCode = "403", description = "Forbidden - User lacks required permissions"),
                    @ApiResponse(responseCode = "404", description = "Container or form not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public final Response containerForm(@Context final HttpServletRequest req,
                                        @Context final HttpServletResponse res,
                                        @Parameter(description = "Container identifier (UUID or path)") @PathParam("containerId") final String containerId,
                                        @Parameter(description = "Form identifier") @PathParam("formId") final String formId)
            throws DotDataException, DotSecurityException {

        final InitDataObject initData = webResource.init(req, res, true);
        final User user = initData.getUser();

        PageMode.setPageMode(req, PageMode.EDIT_MODE);
        Contentlet formContent = formAPI.getFormContent(formId);

        if (formContent == null) {
            formContent = formAPI.createDefaultFormContent(formId);
        }

        final String html = getHTML(req, res, containerId, user, formContent);

        final Map<String, Object> response = ImmutableMap.<String, Object> builder()
            .put(MessageConstants.RENDER, html)
            .put("content", formContent.getMap())
            .build();

        return Response.ok(new ResponseEntityView(response))
                .build();

    }

    private String getHTML(final HttpServletRequest req,
            final HttpServletResponse res,
            final String containerId,
            final User user,
            final Contentlet contentlet) throws DotDataException, DotSecurityException {

        return getHTML(req, res, containerId, user, contentlet, null);

    }

    private String getHTML(final HttpServletRequest req,
                           final HttpServletResponse res,
                           final String containerId,
                           final User user,
                           final Contentlet contentlet,
                           final String pageInode) throws DotDataException, DotSecurityException {

        final PageMode mode = PageMode.EDIT_MODE;
        final Container container = getContainerWorking(containerId, user, WebAPILocator.getHostWebAPI().getHost(req));
        ContainerResourceHelper.getInstance().setContainerLanguage(container, req);

        final org.apache.velocity.context.Context context = velocityUtil.getContext(req, res);

        if (UtilMethods.isSet(pageInode)) {
            loadPageContext(user, pageInode, mode, context);
        } else {
            context.put("dotPageContent", Boolean.TRUE);
        }

        context.put(ContainerLoader.SHOW_PRE_POST_LOOP, false);
        context.put("contentletList" + container.getIdentifier() + Container.LEGACY_RELATION_TYPE,
                Lists.newArrayList(contentlet.getIdentifier()));
        context.put(mode.name(), Boolean.TRUE);

        final VelocityResourceKey key = new VelocityResourceKey(container, Container.LEGACY_RELATION_TYPE, mode);
        return velocityUtil.merge(key.path, context);
    }

    private void loadPageContext(User user, String pageInode, PageMode mode,
            org.apache.velocity.context.Context context)
            throws DotDataException, DotSecurityException {
        final IHTMLPage htmlPage =  APILocator.getHTMLPageAssetAPI().findPage(pageInode, user, false);
        final long languageId = htmlPage.getLanguageId();

        final VelocityResourceKey pageKey = new VelocityResourceKey((HTMLPageAsset) htmlPage, mode, languageId);
        velocityUtil.merge(pageKey.path, context);
    }

    private Container getContainerWorking(final String containerId, final User user, final Host host) throws DotDataException, DotSecurityException {

        final PageMode mode = PageMode.EDIT_MODE;
        return this.getContainer(containerId, user, host, mode);
    }

    private Container getContainerArchiveWorking(final String containerId, final User user, final Host host) throws DotDataException, DotSecurityException {

        final PageMode mode = PageMode.EDIT_MODE;
        return this.getContainer(containerId, user, host, mode, true);
    }

    private Container getContainerLive(final String containerId, final User user, final Host host) throws DotDataException, DotSecurityException {

        final PageMode mode = PageMode.LIVE;
        return this.getContainer(containerId, user, host, mode);
    }

    private Container getContainer(final String containerId, final User user, final Host host, final PageMode mode) throws DotDataException, DotSecurityException {

        return  this.getContainer(containerId, user, host, mode, false);
    }

    /**
     * Returns the Container that matches the specified ID.
     *
     * @param containerId The Identifier of the Container being returned.
     * @param user        The {@link User} performing this action.
     * @param site        The {@link Host} object representing the Site that the Container lives in.
     *
     * @return The {@link Container} matching the ID.
     *
     * @throws DotDataException     An error occurred when interacting with the data source.
     * @throws DotSecurityException The specified user does not have the required permissions to perform this action.
     */
    private Container getContainer(final String containerId, final User user, final Host host, final PageMode mode, final boolean archive) throws DotDataException, DotSecurityException {

        if (Container.SYSTEM_CONTAINER.equals(containerId)) {
            return this.containerAPI.systemContainer();
        }

        if (FileAssetContainerUtil.getInstance().isFolderAssetContainerId(containerId)) {

            final Optional<Host> hostOpt = HostUtil.getHostFromPathOrCurrentHost(containerId, Constants.CONTAINER_FOLDER_PATH);
            final Host   containerHost   = hostOpt.isPresent()? hostOpt.get():host;
            final String relativePath    = FileAssetContainerUtil.getInstance().getPathFromFullPath(containerHost.getHostname(), containerId);
            try {

                return mode.showLive ?
                        this.containerAPI.getLiveContainerByFolderPath(relativePath, containerHost, user, mode.respectAnonPerms) :
                        (archive?
                                this.containerAPI.getWorkingArchiveContainerByFolderPath(relativePath, containerHost, user, mode.respectAnonPerms):
                                this.containerAPI.getWorkingContainerByFolderPath(relativePath, containerHost, user, mode.respectAnonPerms));
            } catch (NotFoundInDbException e) {

                // if does not found in the host path or current host, tries the default one if it is not the same
                final Host defaultHost = WebAPILocator.getHostWebAPI().findDefaultHost(user, false);
                if (!defaultHost.getIdentifier().equals(containerHost.getIdentifier())) {

                    return  mode.showLive ?
                            this.containerAPI.getLiveContainerByFolderPath(relativePath, defaultHost, APILocator.getUserAPI().getSystemUser(), false) :
                            (archive?
                                    this.containerAPI.getWorkingArchiveContainerByFolderPath(relativePath, defaultHost, APILocator.getUserAPI().getSystemUser(), false):
                                    this.containerAPI.getWorkingContainerByFolderPath(relativePath, defaultHost, APILocator.getUserAPI().getSystemUser(), false));
                }
            }
        }

        final ShortyId containerShorty = this.shortyAPI.getShorty(containerId)
                .orElseGet(() -> {
                    throw new ResourceNotFoundException("Can't find Container:" + containerId);
                });

        return (containerShorty.type != ShortType.IDENTIFIER)
                ? this.containerAPI.find(containerId, user, mode.showLive)
                : (mode.showLive) ?
                this.containerAPI.getLiveContainerById(containerShorty.longId, user, mode.respectAnonPerms) :
                this.containerAPI.getWorkingContainerById(containerShorty.longId, user, mode.respectAnonPerms);
    }

    @DELETE
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Path("delete/{containerId}/content/{contentletId}/uid/{uid}")
    @Operation(
            operationId = "removeContentletFromContainer",
            summary = "Removes content from a container",
            description = "Removes a specific contentlet from a container at a particular position. " +
                    "This affects the container's content layout and rendering.",
            tags = {"Containers"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Content removed successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityView.class))),
                    @ApiResponse(responseCode = "400", description = "Bad request - Invalid parameters"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized - User not authenticated"),
                    @ApiResponse(responseCode = "403", description = "Forbidden - User lacks required permissions"),
                    @ApiResponse(responseCode = "404", description = "Container, content, or UID not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public final Response removeContentletFromContainer(@Context final HttpServletRequest req,
            @Context final HttpServletResponse res, 
            @Parameter(description = "Container identifier") @PathParam("containerId") final String containerId,
            @Parameter(description = "Contentlet identifier") @PathParam("contentletId") final String contentletId, 
            @Parameter(description = "Order position of the content") @QueryParam("order") final long order,
            @Parameter(description = "Unique identifier for the content instance") @PathParam("uid") final String uid) throws DotDataException,
            MethodInvocationException, ResourceNotFoundException, IOException, IllegalAccessException, InstantiationException,
            InvocationTargetException, NoSuchMethodException {

        final InitDataObject initData = webResource.init(req, res, true);
        final User user = initData.getUser();
        final PageMode mode = PageMode.get(req);
        try {
            final Language id = WebAPILocator.getLanguageWebAPI()
                .getLanguage(req);

            return removeContentletFromContainer(id, containerId, contentletId, uid, user, mode);
        } catch (DotSecurityException e) {
            throw new ForbiddenException(e);
        }
    }

    @WrapInTransaction
    private Response removeContentletFromContainer(final Language id, final String containerId,
                                                   final String contentletId, final String uid,
                                                   final User user, final PageMode mode) throws DotDataException, DotSecurityException {


        final ShortyId contentShorty = APILocator.getShortyAPI()
                .getShorty(contentletId)
                .orElseGet(() -> {
                    throw new ResourceNotFoundException(
                            "Can't find contentlet:" + contentletId);
                });

        final Contentlet contentlet =
                (contentShorty.subType == ShortType.CONTENTLET) ? APILocator.getContentletAPI()
                        .find(contentShorty.longId, user, !mode.isAdmin)
                        : APILocator.getContentletAPI()
                                .findContentletByIdentifier(contentShorty.longId, mode.showLive,
                                        id.getId(), user, !mode.isAdmin);

        final ShortyId containerShorty = APILocator.getShortyAPI()
                .getShorty(containerId)
                .orElseGet(() -> {
                    throw new ResourceNotFoundException("Can't find Container:" + containerId);
                });

        final Container container =
                (containerShorty.subType == ShortType.CONTAINER) ? this.containerAPI
                        .find(containerId, user, !mode.isAdmin)
                        : (mode.showLive) ?
                                this.containerAPI.getLiveContainerById(containerShorty.longId, user, !mode.isAdmin) :
                                this.containerAPI.getWorkingContainerById(containerShorty.longId, user, !mode.isAdmin);

        APILocator.getPermissionAPI()
                .checkPermission(contentlet, PermissionLevel.READ, user);
        APILocator.getPermissionAPI()
                .checkPermission(container, PermissionLevel.EDIT, user);

        final MultiTree mt = new MultiTree().setContainer(containerId)
                .setContentlet(contentletId)
                .setRelationType(uid);

        APILocator.getMultiTreeAPI().deleteMultiTree(mt);

        return Response.ok("ok").build();
    }


    @Path("/containerContent/{params:.*}")
    public final Response containerContents(@Context final HttpServletRequest req, @Context final HttpServletResponse res,
            @QueryParam("containerId") final String containerId, @QueryParam("contentInode") final String contentInode)
            throws DotDataException, IOException {

        final InitDataObject initData = webResource.init(req, res, true);
        final User user = initData.getUser();

        try {

            final PageMode mode = PageMode.get(req);
            final Container container = APILocator.getContainerAPI()
                    .find(containerId, user, !mode.isAdmin);

            final org.apache.velocity.context.Context context = VelocityUtil.getWebContext(req, res);
            final Contentlet contentlet = APILocator.getContentletAPI()
                    .find(contentInode, user, !mode.isAdmin);

            final StringWriter inputWriter  = new StringWriter();
            final StringWriter outputWriter = new StringWriter();
            inputWriter.append("#set ($contentletList")
                    .append(container.getIdentifier())
                    .append(" = [")
                    .append(contentlet.getIdentifier())
                    .append("] )")
                    .append("#set ($totalSize")
                    .append(container.getIdentifier())
                    .append("=")
                    .append("1")
                    .append(")")
                    .append("#parseContainer(\"")
                    .append(container.getIdentifier())
                    .append("\")");

            VelocityUtil.getEngine()
                    .evaluate(context, outputWriter, this.getClass()
                            .getName(), IOUtils.toInputStream(inputWriter.toString()));

            final Map<String, String> response = new HashMap<>();
            response.put(MessageConstants.RENDER, outputWriter.toString());

            return Response.ok(response).build();
        } catch (DotSecurityException e) {
            throw new ForbiddenException(e);
        }
    }

    /**
     * Saves a new working version of a container.
     *
     * @param request
     * @param response
     * @param containerForm
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @POST
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Operation(
            operationId = "saveContainer",
            summary = "Creates a new container",
            description = "Creates and publishes a new container with the provided configuration. " +
                    "The container will be saved as both working and live versions.",
            tags = {"Containers"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Container created successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityView.class))),
                    @ApiResponse(responseCode = "400", description = "Bad request - Invalid container data"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized - User not authenticated"),
                    @ApiResponse(responseCode = "403", description = "Forbidden - User lacks create permissions"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public final Response saveNew(@Context final HttpServletRequest  request,
                                  @Context final HttpServletResponse response,
                                  @RequestBody(description = "Container configuration data including title, code, content type structures, and display settings",
                                          required = true,
                                          content = @Content(schema = @Schema(implementation = ContainerForm.class)))
                                  final ContainerForm containerForm) throws DotDataException, DotSecurityException {

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requestAndResponse(request, response).requiredBackendUser(true)
                .rejectWhenNoUser(true).init();
        final User user = initData.getUser();
        final Host host = WebAPILocator.getHostWebAPI().getCurrentHostNoThrow(request);
        final PageMode pageMode = PageMode.get(request);

        return saveNewAndPublish(containerForm, user, host, pageMode);
    }

    @WrapInTransaction
    private Response saveNewAndPublish(final ContainerForm containerForm, final User user, final Host host, final PageMode pageMode)
            throws DotDataException, DotSecurityException {
        Container container = new Container();

        Logger.debug(this,
                () -> "Adding container. Request payload is : " + JsonUtil.getJsonStringFromObject(
                        containerForm));

        container.setCode(containerForm.getCode());
        container.setMaxContentlets(containerForm.getMaxContentlets());
        container.setNotes(containerForm.getNotes());
        container.setPreLoop(containerForm.getPreLoop());
        container.setPostLoop(containerForm.getPostLoop());
        container.setSortContentletsBy(containerForm.getSortContentletsBy());
        container.setStaticify(containerForm.isStaticify());
        container.setUseDiv(containerForm.isUseDiv());
        container.setFriendlyName(containerForm.getFriendlyName());
        container.setModDate(new Date());
        container.setModUser(user.getUserId());
        container.setOwner(user.getUserId());
        container.setShowOnMenu(containerForm.isShowOnMenu());
        container.setTitle(containerForm.getTitle());

        if (containerForm.getMaxContentlets() == 0) {
            container.setCode(containerForm.getCode());
        }

        this.containerAPI.save(container, containerForm.getContainerStructures(), host, user,
                pageMode.respectAnonPerms);

        ActivityLogger.logInfo(this.getClass(),
                "Save Container",
                getInfoMessage(user, MessageConstants.SAVED + container.getTitle()),
                host.getHostname());

        Logger.debug(this, () -> MessageConstants.CONTAINER + container.getIdentifier() + " has been saved");

        Logger.debug(this, () -> "Publishing the container: " + container.getIdentifier());

        this.containerAPI.publish(container, user, pageMode.respectAnonPerms);
        ActivityLogger.logInfo(this.getClass(),
                "Publish Container",
                getInfoMessage(user, MessageConstants.PUBLISHED + container.getIdentifier()));

        return Response.ok(new ResponseEntityView(new ContainerView(container))).build();
    }

    /**
     * Updates a new working version of a container.
     *
     * @param request
     * @param response
     * @param containerForm
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @PUT
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Operation(
            operationId = "updateContainer",
            summary = "Updates an existing container",
            description = "Updates a container's working version with the provided configuration. " +
                    "The container must exist and the user must have edit permissions.",
            tags = {"Containers"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Container updated successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityView.class))),
                    @ApiResponse(responseCode = "400", description = "Bad request - Invalid container data"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized - User not authenticated"),
                    @ApiResponse(responseCode = "403", description = "Forbidden - User lacks edit permissions"),
                    @ApiResponse(responseCode = "404", description = "Container not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public final Response update(@Context final HttpServletRequest  request,
                                  @Context final HttpServletResponse response,
                                  @RequestBody(description = "Updated container configuration data including identifier and modified properties",
                                          required = true,
                                          content = @Content(schema = @Schema(implementation = ContainerForm.class)))
                                  final ContainerForm containerForm) throws DotDataException, DotSecurityException {

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requestAndResponse(request, response).requiredBackendUser(true).rejectWhenNoUser(true).init();
        final User user         = initData.getUser();
        final Host host         = WebAPILocator.getHostWebAPI().getCurrentHostNoThrow(request);
        final PageMode pageMode = PageMode.get(request);
        final Container container = this.getContainerWorking(containerForm.getIdentifier(), user, host);

        if (null == container || !InodeUtils.isSet(container.getInode())) {

            Logger.error(this, MessageConstants.CONTAINER + containerForm.getIdentifier() + ", does not exists");
            throw new DoesNotExistException(MessageConstants.CONTAINER + containerForm.getIdentifier() + " does not exists");
        }
            Logger.debug(this,
                () -> "Updating container. Request payload is : " + JsonUtil.getJsonStringFromObject(
                        containerForm));

        Container newContainerVersion = new Container();
        newContainerVersion.setIdentifier(containerForm.getIdentifier());
        newContainerVersion.setCode(containerForm.getCode());
        newContainerVersion.setMaxContentlets(containerForm.getMaxContentlets());
        newContainerVersion.setNotes(containerForm.getNotes());
        newContainerVersion.setPreLoop(containerForm.getPreLoop());
        newContainerVersion.setPostLoop(containerForm.getPostLoop());
        newContainerVersion.setSortContentletsBy(containerForm.getSortContentletsBy());
        newContainerVersion.setStaticify(containerForm.isStaticify());
        newContainerVersion.setUseDiv(containerForm.isUseDiv());
        newContainerVersion.setFriendlyName(containerForm.getFriendlyName());
        newContainerVersion.setModDate(new Date());
        newContainerVersion.setModUser(user.getUserId());
        newContainerVersion.setOwner(user.getUserId());
        newContainerVersion.setShowOnMenu(containerForm.isShowOnMenu());
        newContainerVersion.setTitle(containerForm.getTitle());

        if (containerForm.getMaxContentlets() == 0) {
            newContainerVersion.setCode(containerForm.getCode());
        }

        newContainerVersion.setLuceneQuery(container.getLuceneQuery());

        this.containerAPI.save(newContainerVersion, containerForm.getContainerStructures(), host, user,
                pageMode.respectAnonPerms);

        ActivityLogger.logInfo(this.getClass(),
                "Update Container: " + containerForm.getIdentifier(),
                getInfoMessage(user,
                        MessageConstants.SAVED + newContainerVersion.getTitle()),
                host.getHostname());

        return Response.ok(new ResponseEntityView(new ContainerView(newContainerVersion))).build();
    }

    /**
     * Return live version {@link com.dotmarketing.portlets.containers.model.Container} based on the id
     *
     * @param httpRequest
     * @param httpResponse
     * @param containerId container identifier to get the live version.
     * @return
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @GET
    @Path("/live")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Operation(
            operationId = "getLiveContainer",
            summary = "Retrieves a live container by ID",
            description = "Returns the live (published) version of a container. Optionally includes associated content type information.",
            tags = {"Containers"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Live container retrieved successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityView.class))),
                    @ApiResponse(responseCode = "400", description = "Bad request - Invalid container ID"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized - User not authenticated"),
                    @ApiResponse(responseCode = "403", description = "Forbidden - User lacks required permissions"),
                    @ApiResponse(responseCode = "404", description = "Live container not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public final Response getLiveById(@Context final HttpServletRequest  httpRequest,
                                      @Context final HttpServletResponse httpResponse,
                                      @Parameter(description = "Container identifier") @QueryParam("containerId") final String containerId,
                                      @Parameter(description = "Include associated content type information") @QueryParam("includeContentType") final boolean includeContentType) throws DotSecurityException, DotDataException {

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requestAndResponse(httpRequest, httpResponse).rejectWhenNoUser(true).init();
        final User user     = initData.getUser();
        Logger.debug(this, ()-> "Getting the live container by id: " + containerId);

        final Container container = this.getContainerLive(containerId, user, WebAPILocator.getHostWebAPI().getHost(httpRequest));

        if (null == container || UtilMethods.isNotSet(container.getIdentifier())) {

            Logger.error(this, "Live Version of the Container with Id: " + containerId + MessageConstants.DOES_NOT_EXIST);
            throw new DoesNotExistException("Live Version of the Container with Id: " + containerId + MessageConstants.DOES_NOT_EXIST);
        }

        if(includeContentType){
            List<ContainerStructure> structures = this.containerAPI.getContainerStructures(container);
            return Response.ok(new ResponseEntityView(ContainerResourceHelper.getInstance().toResponseEntityContainerWithContentTypesView(container, structures))).build();
        }

        return Response.ok(new ResponseEntityView(new ContainerView(container))).build();
    }

    /**
     * Return working version {@link com.dotmarketing.portlets.containers.model.Container} based on the id
     *
     * @param request
     * @param httpResponse
     * @param containerId container identifier to get the working version.
     * @return
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @GET
    @Path("/working")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Operation(
            operationId = "getWorkingContainer",
            summary = "Retrieves a working container by ID",
            description = "Returns the working (draft) version of a container. Optionally includes associated content type information.",
            tags = {"Containers"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Working container retrieved successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityView.class))),
                    @ApiResponse(responseCode = "400", description = "Bad request - Invalid container ID"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized - User not authenticated"),
                    @ApiResponse(responseCode = "403", description = "Forbidden - User lacks required permissions"),
                    @ApiResponse(responseCode = "404", description = "Working container not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public final Response getWorkingById(@Context final HttpServletRequest  request,
                                         @Context final HttpServletResponse httpResponse,
                                         @Parameter(description = "Container identifier") @QueryParam("containerId") final String containerId,
                                         @Parameter(description = "Include associated content type information") @QueryParam("includeContentType") final boolean includeContentType) throws DotSecurityException, DotDataException {

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requestAndResponse(request, httpResponse).rejectWhenNoUser(true).init();
        final User user     = initData.getUser();
        Logger.debug(this, ()-> "Getting the working container by id: " + containerId);

        final Host      host      =  WebAPILocator.getHostWebAPI().getHost(request);
        final Container container = this.getContainerWorking(containerId, user, host);

        if (null == container || UtilMethods.isNotSet(container.getIdentifier())) {

            Logger.error(this, "Working Version of the Container with Id: " + containerId + MessageConstants.DOES_NOT_EXIST);
            throw new DoesNotExistException("Working Version of the Container with Id: " + containerId + MessageConstants.DOES_NOT_EXIST);
        }

        if(includeContentType){
            List<ContainerStructure> structures = this.containerAPI.getContainerStructures(container);
            return Response.ok(new ResponseEntityView(ContainerResourceHelper.getInstance().toResponseEntityContainerWithContentTypesView(container, structures))).build();
        }

        return Response.ok(new ResponseEntityView(new ContainerView(container))).build();
    }

    /**
     * Publishes a Container
     *
     * This method receives an identifier and publish it
     * To publish a container successfully the user needs to have Publish Permissions and the container
     * can not be archived.
     *
     * @param request            {@link HttpServletRequest}
     * @param response           {@link HttpServletResponse}
     * @param containerId        {@link Integer} container id to publish
     * @return Response
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @PUT
    @Path("/_publish")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Operation(
            operationId = "publishContainer",
            summary = "Publishes a container",
            description = "Makes a container live by publishing it. The container must exist and the user must have publish permissions.",
            tags = {"Containers"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Container published successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityView.class))),
                    @ApiResponse(responseCode = "400", description = "Bad request - Container ID is required"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized - User not authenticated"),
                    @ApiResponse(responseCode = "403", description = "Forbidden - User lacks publish permissions"),
                    @ApiResponse(responseCode = "404", description = "Container not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public final Response publish(@Context final HttpServletRequest  request,
                                    @Context final HttpServletResponse response,
                                    @Parameter(description = "Container identifier to publish") @QueryParam("containerId") final String containerId) throws DotSecurityException, DotDataException {

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requestAndResponse(request, response).requiredBackendUser(true).rejectWhenNoUser(true).init();
        final User user         = initData.getUser();
        final PageMode pageMode = PageMode.get(request);

        if (!UtilMethods.isSet(containerId)) {

            Logger.error(this, MessageConstants.CONTAINER_ID_IS_REQUIRED);
            throw new IllegalArgumentException(MessageConstants.CONTAINER_ID_IS_REQUIRED);
        }

        Logger.debug(this, ()-> "Publishing the container: " + containerId);

        final Host      host      = WebAPILocator.getHostWebAPI().getHost(request);
        final Container container = this.getContainerWorking(containerId, user, host);

        if (null != container && InodeUtils.isSet(container.getInode())) {

            this.containerAPI.publish(container, user, pageMode.respectAnonPerms);
            ActivityLogger.logInfo(this.getClass(),
                    "Publish Container",
                    getInfoMessage(user, MessageConstants.PUBLISHED + container.getIdentifier()));
        } else {

            Logger.error(this, MessageConstants.CONTAINER_ID_WITH + containerId + MessageConstants.DOES_NOT_EXIST);
            throw new DoesNotExistException(MessageConstants.CONTAINER_ID_WITH + containerId + MessageConstants.DOES_NOT_EXIST);
        }

        return Response.ok(new ResponseEntityView(new ContainerView(
                this.getContainerLive(containerId, user, host)))).build();
    }

    /**
     * UnPublishes a Container
     *
     * This method receives an identifier and unpublish it
     * To unpublish a container successfully the user needs to have Write Permissions and the container
     * can not be archived.
     *
     * @param request            {@link HttpServletRequest}
     * @param response           {@link HttpServletResponse}
     * @param containerId        {@link Integer} container id to unpublish
     * @return Response
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @PUT
    @Path("/_unpublish")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Operation(
            operationId = "unpublishContainer",
            summary = "Unpublishes a container",
            description = "Removes a container from live status by unpublishing it. The container must exist and the user must have unpublish permissions.",
            tags = {"Containers"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Container unpublished successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityView.class))),
                    @ApiResponse(responseCode = "400", description = "Bad request - Container ID is required"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized - User not authenticated"),
                    @ApiResponse(responseCode = "403", description = "Forbidden - User lacks unpublish permissions"),
                    @ApiResponse(responseCode = "404", description = "Container not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public final Response unpublish(@Context final HttpServletRequest  request,
                                  @Context final HttpServletResponse response,
                                  @Parameter(description = "Container identifier to unpublish") @QueryParam("containerId") final String containerId) throws DotSecurityException, DotDataException {

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requestAndResponse(request, response).requiredBackendUser(true).rejectWhenNoUser(true).init();
        final User user         = initData.getUser();
        final PageMode pageMode = PageMode.get(request);

        if (!UtilMethods.isSet(containerId)) {

            Logger.error(this, MessageConstants.CONTAINER_ID_IS_REQUIRED);
            throw new IllegalArgumentException(MessageConstants.CONTAINER_ID_IS_REQUIRED);
        }

        Logger.debug(this, ()-> "UnPublishing the container: " + containerId);

        final Host      host      = WebAPILocator.getHostWebAPI().getHost(request);
        final Container container = this.getContainerWorking(containerId, user, host);

        if (null != container && InodeUtils.isSet(container.getInode())){
            this.containerAPI.unpublish(container, user, pageMode.respectAnonPerms);
            ActivityLogger.logInfo(this.getClass(),
                    "Unpublish Container",
                    getInfoMessage(user, MessageConstants.UNPUBLISHED + container.getIdentifier()));
        } else {

            Logger.error(this, MessageConstants.CONTAINER_ID_WITH + containerId + MessageConstants.DOES_NOT_EXIST);
            throw new DoesNotExistException(MessageConstants.CONTAINER_ID_WITH + containerId + MessageConstants.DOES_NOT_EXIST);
        }

        return Response.ok(new ResponseEntityView(
                this.getContainerWorking(containerId, user, host))).build();
    }

    /**
     * Archives container
     *
     * This method receives a container id and archives it.
     * To archive a container successfully the user needs to have Edit Permissions.
     *
     * @param request            {@link HttpServletRequest}
     * @param response           {@link HttpServletResponse}
     * @param containerId       containerId identifier to archive.
     * @return Response
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @PUT
    @Path("/_archive")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response archive(@Context final HttpServletRequest  request,
                                  @Context final HttpServletResponse response,
                                  @QueryParam("containerId") final String containerId) throws DotSecurityException, DotDataException {

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requestAndResponse(request, response).rejectWhenNoUser(true).init();
        final User user         = initData.getUser();
        final PageMode pageMode = PageMode.get(request);

        if (!UtilMethods.isSet(containerId)) {

            Logger.error(this, MessageConstants.CONTAINER_ID_IS_REQUIRED);
            throw new IllegalArgumentException(MessageConstants.CONTAINER_ID_IS_REQUIRED);
        }

        Logger.debug(this, ()-> "Archive the container: " + containerId);

        final Host      host      = WebAPILocator.getHostWebAPI().getHost(request);
        final Container container = this.getContainerWorking(containerId, user, host);
        if (null != container && InodeUtils.isSet(container.getInode())) {

            this.containerAPI.archive(container, user, pageMode.respectAnonPerms);
            ActivityLogger.logInfo(this.getClass(),
                    "Doing Archive Container Action",
                    getInfoMessage(user, MessageConstants.ARCHIVED + container.getIdentifier()));
        } else {

            Logger.error(this, MessageConstants.CONTAINER_ID_WITH + containerId + MessageConstants.DOES_NOT_EXIST);
            throw new DoesNotExistException(MessageConstants.CONTAINER_ID_WITH + containerId + MessageConstants.DOES_NOT_EXIST);
        }

        return Response.ok(new ResponseEntityView(new ContainerView(this.getContainerArchiveWorking(
                containerId, user, host)))).build();
    }

    /**
     * UnArchives container
     *
     * This method receives a container id and archives it.
     * To archive a container successfully the user needs to have Edit Permissions.
     *
     * @param request            {@link HttpServletRequest}
     * @param response           {@link HttpServletResponse}
     * @param containerId       containerId identifier to archive.
     * @return Response
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @PUT
    @Path("/_unarchive")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response unarchive(@Context final HttpServletRequest  request,
                                  @Context final HttpServletResponse response,
                                  @QueryParam("containerId") final String containerId) throws DotSecurityException, DotDataException {

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requestAndResponse(request, response).rejectWhenNoUser(true).init();
        final User user         = initData.getUser();
        final PageMode pageMode = PageMode.get(request);

        if (!UtilMethods.isSet(containerId)) {

            Logger.error(this, MessageConstants.CONTAINER_ID_IS_REQUIRED);
            throw new IllegalArgumentException(MessageConstants.CONTAINER_ID_IS_REQUIRED);
        }

        Logger.debug(this, ()-> "Unarchive the container: " + containerId);

        final Host      host      = WebAPILocator.getHostWebAPI().getHost(request);
        final Container container = this.getContainerArchiveWorking(containerId, user, host);
        if (null != container && InodeUtils.isSet(container.getInode())) {

            this.containerAPI.unarchive(container, user, pageMode.respectAnonPerms);
            ActivityLogger.logInfo(this.getClass(),
                    "Doing Archive Container Action",
                    getInfoMessage(user, MessageConstants.ARCHIVED + container.getIdentifier()));
        } else {

            Logger.error(this, MessageConstants.CONTAINER_ID_WITH + containerId + MessageConstants.DOES_NOT_EXIST);
            throw new DoesNotExistException(MessageConstants.CONTAINER_ID_WITH + containerId + MessageConstants.DOES_NOT_EXIST);
        }

        return Response.ok(new ResponseEntityView(new ContainerView(this.getContainerWorking(
                containerId, user, host)))).build();
    }

    /**
     * UnArchives container
     *
     * This method receives a container id and archives it.
     * To archive a container successfully the user needs to have Edit Permissions.
     *
     * @param request            {@link HttpServletRequest}
     * @param response           {@link HttpServletResponse}
     * @param containerId       containerId identifier to archive.
     * @return Response
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @DELETE
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response delete(@Context final HttpServletRequest  request,
                                    @Context final HttpServletResponse response,
                                    @QueryParam("containerId") final String containerId) throws Exception {

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requestAndResponse(request, response).rejectWhenNoUser(true).init();
        final User user         = initData.getUser();
        final PageMode pageMode = PageMode.get(request);

        if (!UtilMethods.isSet(containerId)) {

            Logger.error(this, MessageConstants.CONTAINER_ID_IS_REQUIRED);
            throw new IllegalArgumentException(MessageConstants.CONTAINER_ID_IS_REQUIRED);
        }

        final Container container = this.getContainerWorking(containerId, user,
                WebAPILocator.getHostWebAPI().getHost(request));
        if (null != container && InodeUtils.isSet(container.getInode())) {

            Logger.debug(this,()->"Calling Delete Container");

            if(this.containerAPI.delete(container, user, pageMode.respectAnonPerms)) {

                ActivityLogger.logInfo(this.getClass(),
                        "Done Delete Container",
                        getInfoMessage(user, MessageConstants.DELETED + container.getIdentifier()));
                return Response.ok(new ResponseEntityView(true)).build();
            }

            ActivityLogger.logInfo(this.getClass(),
                    "Can not Delete Container",
                    getInfoMessage(user, MessageConstants.CANNOT_DELETE + container.getIdentifier()));
            return Response.ok(new ResponseEntityView(false)).build();
        } else {

            Logger.error(this, MessageConstants.CONTAINER_ID_WITH + containerId + MessageConstants.DOES_NOT_EXIST);
            throw new DoesNotExistException(MessageConstants.CONTAINER_ID_WITH + containerId + MessageConstants.DOES_NOT_EXIST);
        }
    }

    /**
     * Copies container to the specified host
     *
     * @param request
     * @param response
     * @param id       id identifier to copy.
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @POST
    @Path("/{id}/_copy")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ResponseEntityContainerView copy(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @PathParam("id") final String id) throws DotDataException, DotSecurityException {

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requestAndResponse(request, response).requiredBackendUser(true)
                .rejectWhenNoUser(true).init();
        final User user = initData.getUser();
        final Host host = WebAPILocator.getHostWebAPI().getCurrentHostNoThrow(request);
        final PageMode pageMode = PageMode.get(request);

        DotPreconditions.checkArgument(UtilMethods.isSet(id),
                MessageConstants.CONTAINER_ID_IS_REQUIRED);

        final Container sourceContainer = this.getContainerWorking(id, user,
                WebAPILocator.getHostWebAPI().getHost(request));

        if (null != sourceContainer && InodeUtils.isSet(sourceContainer.getInode())) {

            ActivityLogger.logInfo(this.getClass(),
                    "Copy Container",
                    getInfoMessage(user, MessageConstants.COPY + sourceContainer.getIdentifier()),
                    host.getHostname());

            Container copiedContainer = this.containerAPI.copy(sourceContainer, host, user,
                    pageMode.respectAnonPerms);

            Logger.debug(this,
                    () -> "The container: " + sourceContainer.getIdentifier() + " has been copied");

            return new ResponseEntityContainerView(Collections.singletonList(copiedContainer));
        } else {

            Logger.error(this, MessageConstants.CONTAINER_ID_WITH + id + MessageConstants.DOES_NOT_EXIST);
            throw new DoesNotExistException(MessageConstants.CONTAINER_ID_WITH + id + MessageConstants.DOES_NOT_EXIST);
        }
    }

    /**
     * Deletes Container(s).
     *
     * This method receives a list of identifiers and deletes the containers.
     * To delete a container successfully the user needs to have Edit Permissions over it.
     * @param request            {@link HttpServletRequest}
     * @param response           {@link HttpServletResponse}
     * @param containersToDelete {@link String} container identifier to look for and then delete it
     * @return Response
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @DELETE
    @Path("_bulkdelete")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response bulkDelete(@Context final HttpServletRequest  request,
            @Context final HttpServletResponse response,
            final List<String> containersToDelete) {

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requestAndResponse(request, response).rejectWhenNoUser(true).init();
        final User user         = initData.getUser();
        final PageMode pageMode = PageMode.get(request);
        Long deletedContainersCount  = 0L;
        final List<FailedResultView> failedToDelete  = new ArrayList<>();

        Logger.debug(this,
                () -> "Deleting containers in bulk. Request payload is : {" + String.join(",", containersToDelete) + "}");

        DotPreconditions.checkArgument(UtilMethods.isSet(containersToDelete),
                "The body must send a collection of container identifier such as: " +
                        "[\"dd60695c-9e0f-4a2e-9fd8-ce2a4ac5c27d\",\"cc59390c-9a0f-4e7a-9fd8-ca7e4ec0c77d\"]");

        for(final String containerId : containersToDelete){
            try{
                final Container container = this.getContainerWorking(containerId, user,
                        WebAPILocator.getHostWebAPI().getHost(request));

                if (null != container && InodeUtils.isSet(container.getInode())){
                    this.containerAPI.delete(container, user, pageMode.respectAnonPerms);
                    ActivityLogger.logInfo(this.getClass(),
                            "Delete Container Action",
                            getInfoMessage(user,MessageConstants.DELETED_TEMPLATE + container.getIdentifier()));
                    deletedContainersCount++;
                } else {
                    Logger.error(this, MessageConstants.CONTAINER_ID_WITH + containerId + MessageConstants.DOES_NOT_EXIST);
                    failedToDelete.add(new FailedResultView(containerId,"Container does not exist"));
                }
            } catch(Exception e){
                Logger.debug(this,e.getMessage(),e);
                failedToDelete.add(new FailedResultView(containerId,e.getMessage()));
            }
        }

        return Response.ok(new ResponseEntityView(
                        new BulkResultView(deletedContainersCount,0L,failedToDelete)))
                .build();
    }


    /**
     * Publishes Container(s)
     *
     * This method receives a list of identifiers and publishes the containers.
     * To publish a container successfully the user needs to have Publish Permissions
     *
     * @param request            {@link HttpServletRequest}
     * @param response           {@link HttpServletResponse}
     * @param containersToPublish {@link List} list of container ids to publish
     * @return Response
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @PUT
    @Path("/_bulkpublish")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response bulkPublish(@Context final HttpServletRequest  request,
            @Context final HttpServletResponse response,
            final List<String> containersToPublish){

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requestAndResponse(request, response).rejectWhenNoUser(true).init();
        final User user         = initData.getUser();
        final PageMode pageMode = PageMode.get(request);
        Long publishedContainersCount = 0L;
        final List<FailedResultView> failedToPublish    = new ArrayList<>();

        Logger.debug(this,
                () -> "Publishing containers in bulk. Request payload is : {" + String.join(",", containersToPublish) + "}");

        DotPreconditions.checkArgument(UtilMethods.isSet(containersToPublish),
                "The body must send a collection of container identifier such as: " +
                        "[\"dd60695c-9e0f-4a2e-9fd8-ce2a4ac5c27d\",\"cc59390c-9a0f-4e7a-9fd8-ca7e4ec0c77d\"]");

        for (final String containerId : containersToPublish) {
            try{
                final Container container =  this.getContainerWorking(containerId, user,
                        WebAPILocator.getHostWebAPI().getHost(request));
                if (null != container && InodeUtils.isSet(container.getInode())){
                    this.containerAPI.publish(container, user, pageMode.respectAnonPerms);
                    ActivityLogger.logInfo(this.getClass(),
                            "Publish Container Action",
                            getInfoMessage(user, MessageConstants.PUBLISHED + container.getIdentifier()));
                    publishedContainersCount++;
                } else {
                    Logger.error(this, MessageConstants.CONTAINER_ID_WITH + containerId + MessageConstants.DOES_NOT_EXIST);
                    failedToPublish.add(new FailedResultView(containerId,"Container does not exist"));
                }
            } catch(Exception e) {
                Logger.debug(this, e.getMessage(), e);
                failedToPublish.add(new FailedResultView(containerId,e.getMessage()));
            }
        }

        return Response.ok(new ResponseEntityView(
                        new BulkResultView(publishedContainersCount,0L,failedToPublish)))
                .build();
    }

    /**
     * Unpublishes Container(s)
     *
     * This method receives a list of identifiers and unpublishes the containers.
     * To publish a container successfully the user needs to have Publish Permissions.
     *
     * @param request            {@link HttpServletRequest}
     * @param response           {@link HttpServletResponse}
     * @param containersToUnpublish {@link List} list of container ids to unpublish
     * @return Response
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @PUT
    @Path("/_bulkunpublish")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response bulkUnpublish(@Context final HttpServletRequest  request,
            @Context final HttpServletResponse response,
            final List<String> containersToUnpublish) {

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requestAndResponse(request, response).rejectWhenNoUser(true).init();
        final User user         = initData.getUser();
        final PageMode pageMode = PageMode.get(request);
        Long unpublishedContainersCount = 0L;
        final List<FailedResultView> failedToUnpublish    = new ArrayList<>();

        Logger.debug(this,
                () -> "Unpublishing containers in bulk. Request payload is : {" + String.join(",", containersToUnpublish) + "}");

        DotPreconditions.checkArgument(UtilMethods.isSet(containersToUnpublish),
                "The body must send a collection of container identifier such as: " +
                        "[\"dd60695c-9e0f-4a2e-9fd8-ce2a4ac5c27d\",\"cc59390c-9a0f-4e7a-9fd8-ca7e4ec0c77d\"]");

        for (final String containerId : containersToUnpublish) {
            try{
                final Container container =  this.getContainerWorking(containerId, user,
                        WebAPILocator.getHostWebAPI().getHost(request));
                if (null != container && InodeUtils.isSet(container.getInode())){
                    this.containerAPI.unpublish(container, user, pageMode.respectAnonPerms);
                    ActivityLogger.logInfo(this.getClass(),
                            "Unpublish Container Action",
                            getInfoMessage(user, MessageConstants.UNPUBLISHED + container.getIdentifier()));
                    unpublishedContainersCount++;
                } else {
                    Logger.error(this, MessageConstants.CONTAINER_ID_WITH + containerId + MessageConstants.DOES_NOT_EXIST);
                    failedToUnpublish.add(new FailedResultView(containerId,"Container does not exist"));
                }
            } catch(Exception e) {
                Logger.debug(this, e.getMessage(), e);
                failedToUnpublish.add(new FailedResultView(containerId,e.getMessage()));
            }
        }

        return Response.ok(new ResponseEntityView(
                        new BulkResultView(unpublishedContainersCount,0L,failedToUnpublish)))
                .build();
    }

    /**
     * Archives container(s).
     *
     * This method receives a list of identifiers and archives the containers.
     * To archive a container successfully the user needs to have Edit Permissions.
     *
     * @param request            {@link HttpServletRequest}
     * @param response           {@link HttpServletResponse}
     * @param containersToArchive {@link List} containers identifier to archive.
     * @return Response
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @PUT
    @Path("/_bulkarchive")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response bulkArchive(@Context final HttpServletRequest  request,
            @Context final HttpServletResponse response,
            final List<String> containersToArchive) {

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requestAndResponse(request, response).rejectWhenNoUser(true).init();
        final User user         = initData.getUser();
        final PageMode pageMode = PageMode.get(request);
        Long archivedContainersCount = 0L;
        final List<FailedResultView> failedToArchive    = new ArrayList<>();

        Logger.debug(this,
                () -> "Archiving containers in bulk. Request payload is : {" + String.join(",", containersToArchive) + "}");

        DotPreconditions.checkArgument(UtilMethods.isSet(containersToArchive),
                "The body must send a collection of container identifier such as: " +
                        "[\"dd60695c-9e0f-4a2e-9fd8-ce2a4ac5c27d\",\"cc59390c-9a0f-4e7a-9fd8-ca7e4ec0c77d\"]");


        for(final String containerId : containersToArchive){
            try{
                final Container container =  this.getContainerWorking(containerId, user,
                        WebAPILocator.getHostWebAPI().getHost(request));
                if (null != container && InodeUtils.isSet(container.getInode())){
                    this.containerAPI.archive(container, user, pageMode.respectAnonPerms);
                    ActivityLogger.logInfo(this.getClass(),
                            "Archive Container Action",
                            getInfoMessage(user, MessageConstants.ARCHIVED + container.getIdentifier()));
                    archivedContainersCount++;
                } else {
                    Logger.error(this, MessageConstants.CONTAINER_ID_WITH + containerId + MessageConstants.DOES_NOT_EXIST);
                    failedToArchive.add(new FailedResultView(containerId,"Container does not exist"));
                }
            } catch(Exception e) {
                Logger.debug(this,e.getMessage(),e);
                failedToArchive.add(new FailedResultView(containerId,e.getMessage()));
            }
        }

        return Response.ok(new ResponseEntityView(
                        new BulkResultView(archivedContainersCount,0L,failedToArchive)))
                .build();
    }

    /**
     * Unarchives container(s).
     *
     * This method receives a list of identifiers and unarchives the containers.
     * To unarchive a container successfully the user needs to have Edit Permissions and the container
     * needs to be archived.
     *
     * @param request            {@link HttpServletRequest}
     * @param response           {@link HttpServletResponse}
     * @param containersToUnarchive {@link List} containers identifier to unarchive.
     * @return Response
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @PUT
    @Path("/_bulkunarchive")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response bulkUnarchive(@Context final HttpServletRequest  request,
            @Context final HttpServletResponse response,
            final List<String> containersToUnarchive){

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requestAndResponse(request, response).rejectWhenNoUser(true).init();
        final User user         = initData.getUser();
        final PageMode pageMode = PageMode.get(request);
        Long unarchivedContainersCount = 0L;
        final List<FailedResultView> failedToUnarchive    = new ArrayList<>();

        Logger.debug(this,
                () -> "Unarchiving containers in bulk. Request payload is : {" + String.join(",", containersToUnarchive) + "}");

        DotPreconditions.checkArgument(UtilMethods.isSet(containersToUnarchive),
                "The body must send a collection of container identifier such as: " +
                        "[\"dd60695c-9e0f-4a2e-9fd8-ce2a4ac5c27d\",\"cc59390c-9a0f-4e7a-9fd8-ca7e4ec0c77d\"]");

        for(final String containerId : containersToUnarchive){
            try{
                final Container container =  this.getContainerWorking(containerId, user,
                        WebAPILocator.getHostWebAPI().getHost(request));
                if (null != container && InodeUtils.isSet(container.getInode())){
                    this.containerAPI.unarchive(container, user, pageMode.respectAnonPerms);
                    ActivityLogger.logInfo(this.getClass(),
                            "Unarchive Container Action",
                            getInfoMessage(user, MessageConstants.UNARCHIVED + container.getIdentifier()));
                    unarchivedContainersCount++;
                } else {
                    Logger.error(this, MessageConstants.CONTAINER_ID_WITH + containerId + MessageConstants.DOES_NOT_EXIST);
                    failedToUnarchive.add(new FailedResultView(containerId,"Container does not exist"));
                }
            } catch(Exception e) {
                Logger.debug(this, e.getMessage(), e);
                failedToUnarchive.add(new FailedResultView(containerId,e.getMessage()));
            }
        }

        return Response.ok(new ResponseEntityView(
                        new BulkResultView(unarchivedContainersCount,0L,failedToUnarchive)))
                .build();
    }

    private String getInfoMessage(User user, String message){
        return String.format("User %s %s", user.getPrimaryKey(), message);
    }
}