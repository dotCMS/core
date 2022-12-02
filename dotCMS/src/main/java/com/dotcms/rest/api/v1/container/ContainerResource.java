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
public class ContainerResource implements Serializable {

    private static final long serialVersionUID = 1L;

    private final PaginationUtil paginationUtil;
    private final WebResource    webResource;
    private final FormAPI        formAPI;
    private final ContainerAPI   containerAPI;
    private final VersionableAPI versionableAPI;
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
        this.versionableAPI = versionableAPI;
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
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response getContainers(@Context final HttpServletRequest httpRequest,
            @Context final HttpServletResponse  httpResponse,
            @QueryParam(PaginationUtil.FILTER)   final String filter,
            @QueryParam(PaginationUtil.PAGE)     final int page,
            @QueryParam(PaginationUtil.PER_PAGE) final int perPage,
            @DefaultValue("title") @QueryParam(PaginationUtil.ORDER_BY) final String orderBy,
            @DefaultValue("ASC") @QueryParam(PaginationUtil.DIRECTION)  final String direction,
            @QueryParam(ContainerPaginator.HOST_PARAMETER_ID)           final String hostId,
            @QueryParam(ContainerPaginator.SYSTEM_PARAMETER_NAME)       final Boolean showSystemContainer,
            @QueryParam(ContainerPaginator.ARCHIVE_PARAMETER_NAME)       final Boolean showArchiveContainer) {

        final InitDataObject initData = webResource.init(null, httpRequest, httpResponse, true, null);
        final User user = initData.getUser();
        final Optional<String> checkedHostId = this.checkHost(httpRequest, hostId, user);

        try {

            final Map<String, Object> extraParams = Maps.newHashMap();
            if (checkedHostId.isPresent()) {
                extraParams.put(ContainerPaginator.HOST_PARAMETER_ID, checkedHostId.get());
            }
            extraParams.put(ContainerPaginator.SYSTEM_PARAMETER_NAME, showSystemContainer);
            extraParams.put(ContainerPaginator.ARCHIVE_PARAMETER_NAME, showArchiveContainer);
            return this.paginationUtil.getPage(httpRequest, user, filter, page, perPage, orderBy, OrderDirection.valueOf(direction),
                    extraParams);
        } catch (final Exception e) {
            Logger.error(this, e.getMessage(), e);
            return ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    private Optional<String> checkHost(final HttpServletRequest request, final String hostId, final User user) {

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
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Path("/{containerId}/content/{contentletId}")
    public final Response containerContent(@Context final HttpServletRequest req,
                                           @Context final HttpServletResponse res,
                                           @PathParam("containerId")  final String containerId,
                                           @PathParam("contentletId") final String contentletId,
                                           @QueryParam("pageInode") final String pageInode)
            throws DotDataException, DotSecurityException {

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
                            this.contentletAPI.findContentletByIdentifierAnyLanguage(contentShorty.longId):
                            this.contentletAPI.find(contentShorty.longId, user, mode.respectAnonPerms);

            final String html = this.getHTML(req, res, containerId, user, contentlet, pageInode);

            final Map<String, String> response = ImmutableMap.<String, String> builder().put("render", html).build();

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
    @Consumes(MediaType.APPLICATION_JSON)
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
    @Consumes(MediaType.APPLICATION_JSON)
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
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Path("/{containerId}/form/{formId}")
    public final Response containerForm(@Context final HttpServletRequest req,
                                        @Context final HttpServletResponse res,
                                        @PathParam("containerId") final String containerId,
                                        @PathParam("formId") final String formId)
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
            .put("render", html)
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
    public final Response removeContentletFromContainer(@Context final HttpServletRequest req,
            @Context final HttpServletResponse res, @PathParam("containerId") final String containerId,
            @PathParam("contentletId") final String contentletId, @QueryParam("order") final long order,
            @PathParam("uid") final String uid) throws DotDataException,
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
            response.put("render", outputWriter.toString());

            return Response.ok(response).build();
        } catch (DotSecurityException e) {
            throw new ForbiddenException(e);
        }
    }

    ///////

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
    public final Response saveNew(@Context final HttpServletRequest  request,
                                  @Context final HttpServletResponse response,
                                  final ContainerForm containerForm) throws DotDataException, DotSecurityException {

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requestAndResponse(request, response).requiredBackendUser(true).rejectWhenNoUser(true).init();
        final User user         = initData.getUser();
        final Host host         = WebAPILocator.getHostWebAPI().getCurrentHostNoThrow(request);
        final PageMode pageMode = PageMode.get(request);
        Container container     = new Container();


        Logger.debug(this,
                () -> "Adding container. Request payload is : " + JsonUtil.getJsonStringFromObject(containerForm));

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

        if(containerForm.getMaxContentlets() == 0){
            container.setCode(containerForm.getCode());
        }

        this.containerAPI.save(container, containerForm.getContainerStructures(), host, user, pageMode.respectAnonPerms);

        ActivityLogger.logInfo(this.getClass(), "Save Container",
                "User " + user.getPrimaryKey() + " saved " + container.getTitle(), host.getHostname());

        Logger.debug(this, ()-> "The container: " + container.getIdentifier() + " has been saved");

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
    public final Response update(@Context final HttpServletRequest  request,
                                  @Context final HttpServletResponse response,
                                  final ContainerForm containerForm) throws DotDataException, DotSecurityException {

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requestAndResponse(request, response).requiredBackendUser(true).rejectWhenNoUser(true).init();
        final User user         = initData.getUser();
        final Host host         = WebAPILocator.getHostWebAPI().getCurrentHostNoThrow(request);
        final PageMode pageMode = PageMode.get(request);
        final Container container = this.getContainerWorking(containerForm.getIdentifier(), user, host);

        if (null == container || !InodeUtils.isSet(container.getInode())) {

            Logger.error(this, "The container: " + containerForm.getIdentifier() + ", does not exists");
            new DoesNotExistException("The container: " + containerForm.getIdentifier() + " does not exists");
        }

        ActivityLogger.logInfo(this.getClass(), "Upate Container: " + containerForm.getIdentifier(),
                "User " + user.getPrimaryKey() + " saved " + container.getTitle(), host.getHostname());

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

        if(containerForm.getMaxContentlets() == 0){
            container.setCode(containerForm.getCode());
        }

        this.containerAPI.save(container, containerForm.getContainerStructures(), host, user, pageMode.respectAnonPerms);

        Logger.error(this, "The container: " + container.getIdentifier() + " has been updated");

        return Response.ok(new ResponseEntityView(new ContainerView(container))).build();
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
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response getLiveById(@Context final HttpServletRequest  httpRequest,
                                      @Context final HttpServletResponse httpResponse,
                                      @QueryParam("containerId")  final String containerId,
                                      @QueryParam("includeContentType")  final boolean includeContentType) throws DotSecurityException, DotDataException {

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requestAndResponse(httpRequest, httpResponse).rejectWhenNoUser(true).init();
        final User user     = initData.getUser();
        Logger.debug(this, ()-> "Getting the live container by id: " + containerId);

        final Container container = this.getContainerLive(containerId, user, WebAPILocator.getHostWebAPI().getHost(httpRequest));

        if (null == container || UtilMethods.isNotSet(container.getIdentifier())) {

            Logger.error(this, "Live Version of the Container with Id: " + containerId + " does not exist");
            throw new DoesNotExistException("Live Version of the Container with Id: " + containerId + " does not exist");
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
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response getWorkingById(@Context final HttpServletRequest  request,
                                         @Context final HttpServletResponse httpResponse,
                                         @QueryParam("containerId") final String containerId,
                                         @QueryParam("includeContentType")  final boolean includeContentType) throws DotSecurityException, DotDataException {

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requestAndResponse(request, httpResponse).rejectWhenNoUser(true).init();
        final User user     = initData.getUser();
        Logger.debug(this, ()-> "Getting the working container by id: " + containerId);

        final Host      host      =  WebAPILocator.getHostWebAPI().getHost(request);
        final Container container = this.getContainerWorking(containerId, user, host);

        if (null == container || UtilMethods.isNotSet(container.getIdentifier())) {

            Logger.error(this, "Working Version of the Container with Id: " + containerId + " does not exist");
            throw new DoesNotExistException("Working Version of the Container with Id: " + containerId + " does not exist");
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
    public final Response publish(@Context final HttpServletRequest  request,
                                    @Context final HttpServletResponse response,
                                    @QueryParam("containerId") final String containerId) throws DotSecurityException, DotDataException {

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requestAndResponse(request, response).requiredBackendUser(true).rejectWhenNoUser(true).init();
        final User user         = initData.getUser();
        final PageMode pageMode = PageMode.get(request);

        if (!UtilMethods.isSet(containerId)) {

            Logger.error(this, "The container id is required");
            throw new IllegalArgumentException("The container id is required");
        }

        Logger.debug(this, ()-> "Publishing the container: " + containerId);

        final Host      host      = WebAPILocator.getHostWebAPI().getHost(request);
        final Container container = this.getContainerWorking(containerId, user, host);

        if (null != container && InodeUtils.isSet(container.getInode())) {

            this.containerAPI.publish(container, user, pageMode.respectAnonPerms);
            ActivityLogger.logInfo(this.getClass(), "Publish Container", "User " +
                    user.getPrimaryKey() + " Published container: " + container.getIdentifier());
        } else {

            Logger.error(this, "The Container with Id: " + containerId + " does not exist");
            throw new DoesNotExistException("The Container with Id: " + containerId + " does not exist");
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
    public final Response unpublish(@Context final HttpServletRequest  request,
                                  @Context final HttpServletResponse response,
                                  @QueryParam("containerId") final String containerId) throws DotSecurityException, DotDataException {

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requestAndResponse(request, response).requiredBackendUser(true).rejectWhenNoUser(true).init();
        final User user         = initData.getUser();
        final PageMode pageMode = PageMode.get(request);

        if (!UtilMethods.isSet(containerId)) {

            Logger.error(this, "The container id is required");
            throw new IllegalArgumentException("The container id is required");
        }

        Logger.debug(this, ()-> "UnPublishing the container: " + containerId);

        final Host      host      = WebAPILocator.getHostWebAPI().getHost(request);
        final Container container = this.getContainerWorking(containerId, user, host);

        if (null != container && InodeUtils.isSet(container.getInode())){
            this.containerAPI.unpublish(container, user, pageMode.respectAnonPerms);
            ActivityLogger.logInfo(this.getClass(), "Unpublish Container", "User " +
                    user.getPrimaryKey() + " unpublished container: " + container.getIdentifier());
        } else {

            Logger.error(this, "The Container with Id: " + containerId + " does not exist");
            throw new DoesNotExistException("The Container with Id: " + containerId + " does not exist");
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

            Logger.error(this, "The container id is required");
            throw new IllegalArgumentException("The container id is required");
        }

        Logger.debug(this, ()-> "Archive the container: " + containerId);

        final Host      host      = WebAPILocator.getHostWebAPI().getHost(request);
        final Container container = this.getContainerWorking(containerId, user, host);
        if (null != container && InodeUtils.isSet(container.getInode())) {

            this.containerAPI.archive(container, user, pageMode.respectAnonPerms);
            ActivityLogger.logInfo(this.getClass(), "Doing Archive Container Action", "User " +
                    user.getPrimaryKey() + " archived container: " + container.getIdentifier());
        } else {

            Logger.error(this, "Container with Id: " + containerId + " does not exist");
            throw new DoesNotExistException("Container with Id: " + containerId + " does not exist");
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

            Logger.error(this, "The container id is required");
            throw new IllegalArgumentException("The container id is required");
        }

        Logger.debug(this, ()-> "Unarchive the container: " + containerId);

        final Host      host      = WebAPILocator.getHostWebAPI().getHost(request);
        final Container container = this.getContainerArchiveWorking(containerId, user, host);
        if (null != container && InodeUtils.isSet(container.getInode())) {

            this.containerAPI.unarchive(container, user, pageMode.respectAnonPerms);
            ActivityLogger.logInfo(this.getClass(), "Doing Archive Container Action", "User " +
                    user.getPrimaryKey() + " archived container: " + container.getIdentifier());
        } else {

            Logger.error(this, "Container with Id: " + containerId + " does not exist");
            throw new DoesNotExistException("Container with Id: " + containerId + " does not exist");
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

            Logger.error(this, "The container id is required");
            throw new IllegalArgumentException("The container id is required");
        }

        final Container container = this.getContainerWorking(containerId, user,
                WebAPILocator.getHostWebAPI().getHost(request));
        if (null != container && InodeUtils.isSet(container.getInode())) {

            Logger.debug(this,()->"Calling Delete Container");

            if(this.containerAPI.delete(container, user, pageMode.respectAnonPerms)) {

                ActivityLogger.logInfo(this.getClass(), "Done Delete Container", "User " +
                        user.getPrimaryKey() + " deleted container: " + container.getIdentifier());
                return Response.ok(new ResponseEntityView(true)).build();
            }

            ActivityLogger.logInfo(this.getClass(), "Can not Delete Container", "User " +
                    user.getPrimaryKey() + " container: " + container.getIdentifier());
            return Response.ok(new ResponseEntityView(false)).build();
        } else {

            Logger.error(this, "Container with Id: " + containerId + " does not exist");
            throw new DoesNotExistException("Container with Id: " + containerId + " does not exist");
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
                    "The container id is required");

        final Container sourceContainer = this.getContainerWorking(id, user,
                WebAPILocator.getHostWebAPI().getHost(request));

        if (null != sourceContainer && InodeUtils.isSet(sourceContainer.getInode())) {

            ActivityLogger.logInfo(this.getClass(), "Copy Container",
                    "User " + user.getPrimaryKey() + " saved " + sourceContainer.getTitle(),
                    host.getHostname());

            Container copiedContainer = this.containerAPI.copy(sourceContainer, host, user,
                    pageMode.respectAnonPerms);

            Logger.debug(this,
                    () -> "The container: " + sourceContainer.getIdentifier() + " has been copied");

            return new ResponseEntityContainerView(Collections.singletonList(copiedContainer));
        } else {

            Logger.error(this, "Container with Id: " + id + " does not exist");
            throw new DoesNotExistException("Container with Id: " + id + " does not exist");
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
                    ActivityLogger.logInfo(this.getClass(), "Delete Container Action", "User " +
                            user.getPrimaryKey() + " deleted template: " + container.getIdentifier());
                    deletedContainersCount++;
                } else {
                    Logger.error(this, "Container with Id: " + containerId + " does not exist");
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
                    ActivityLogger.logInfo(this.getClass(), "Publish Container Action", "User " +
                            user.getPrimaryKey() + " published container: " + container.getIdentifier());
                    publishedContainersCount++;
                } else {
                    Logger.error(this, "Container with Id: " + containerId + " does not exist");
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
                    ActivityLogger.logInfo(this.getClass(), "Unpublish Container Action", "User " +
                            user.getPrimaryKey() + " unpublished container: " + container.getIdentifier());
                    unpublishedContainersCount++;
                } else {
                    Logger.error(this, "Container with Id: " + containerId + " does not exist");
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
                    ActivityLogger.logInfo(this.getClass(), "Archive Container Action", "User " +
                            user.getPrimaryKey() + " archived container: " + container.getIdentifier());
                    archivedContainersCount++;
                } else {
                    Logger.error(this, "Container with Id: " + containerId + " does not exist");
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
                    ActivityLogger.logInfo(this.getClass(), "Unarchive Container Action", "User " +
                            user.getPrimaryKey() + " unarchived container: " + container.getIdentifier());
                    unarchivedContainersCount++;
                } else {
                    Logger.error(this, "Container with Id: " + containerId + " does not exist");
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
}