package com.dotcms.rest.api.v1.container;


import com.beust.jcommander.internal.Maps;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.rendering.velocity.services.ContainerLoader;
import com.dotcms.rendering.velocity.services.VelocityResourceKey;
import com.dotcms.rendering.velocity.util.VelocityUtil;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.dotmarketing.util.*;
import org.glassfish.jersey.server.JSONP;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.exception.ForbiddenException;
import com.dotcms.rest.exception.mapper.ExceptionMapperUtil;
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
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.business.ContainerAPI;
import com.dotmarketing.portlets.containers.business.FileAssetContainerUtil;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.form.business.FormAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.liferay.portal.model.User;
import org.apache.commons.io.IOUtils;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ResourceNotFoundException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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
     * Url sintax:
     * api/v1/container?filter=filter-string&page=page-number&per_page=per-page&ordeby=order-field-name&direction=order-direction&host=host-id
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
            @QueryParam(ContainerPaginator.HOST_PARAMETER_ID)           final String hostId) {

        final InitDataObject initData = webResource.init(null, httpRequest, httpResponse, true, null);
        final User user = initData.getUser();
        final Optional<String> checkedHostId = this.checkHost(httpRequest, hostId, user);

        try {

            final Map<String, Object> extraParams = Maps.newHashMap();
            if (checkedHostId.isPresent()) {
                extraParams.put(ContainerPaginator.HOST_PARAMETER_ID, checkedHostId.get());
            }
            return this.paginationUtil.getPage(httpRequest, user, filter, page, perPage, orderBy, OrderDirection.valueOf(direction),
                    extraParams);
        } catch (Exception e) {
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

    @GET
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Path("/{containerId}/content/{contentletId}")
    public final Response containerContent(@Context final HttpServletRequest req,
                                           @Context final HttpServletResponse res,
                                           @PathParam("containerId")  final String containerId,
                                           @PathParam("contentletId") final String contentletId)
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

            final String html = this.getHTML(req, res, containerId, user, contentlet);

            final Map<String, String> response = ImmutableMap.<String, String> builder().put("render", html).build();

            return Response.ok(new ResponseEntityView(response)).build();
        } catch (DotSecurityException e) {
            throw new ForbiddenException(e);
        }
    }

    /**
     * This method is pretty much the same of {@link #containerContent(HttpServletRequest, HttpServletResponse, String, String)}
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
                                           @PathParam("contentletId") final String contentletId)
            throws DotDataException, DotSecurityException {

        return this.containerContent(req, res, containerId, contentletId);
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

        final PageMode mode = PageMode.EDIT_MODE;
        final Container container = getContainer(containerId, user, WebAPILocator.getHostWebAPI().getHost(req));
        ContainerResourceHelper.getInstance().setContainerLanguage(container, req);

        final org.apache.velocity.context.Context context = velocityUtil.getContext(req, res);

        context.put(ContainerLoader.SHOW_PRE_POST_LOOP, false);
        context.put("contentletList" + container.getIdentifier() + Container.LEGACY_RELATION_TYPE,
                Lists.newArrayList(contentlet.getIdentifier()));
        context.put(mode.name(), Boolean.TRUE);

        final VelocityResourceKey key = new VelocityResourceKey(container, Container.LEGACY_RELATION_TYPE, mode);

        return velocityUtil.merge(key.path, context);
    }


    private Container getContainer(final String containerId, final User user, final Host host) throws DotDataException, DotSecurityException {

        final PageMode mode = PageMode.EDIT_MODE; // todo: ask for this, does not make sense ask for mode.showLive

        if (FileAssetContainerUtil.getInstance().isFolderAssetContainerId(containerId)) {

            final Optional<Host> hostOpt = HostUtil.getHostFromPathOrCurrentHost(containerId, Constants.CONTAINER_FOLDER_PATH);
            final Host   containerHost   = hostOpt.isPresent()? hostOpt.get():host;
            final String relativePath    = FileAssetContainerUtil.getInstance().getPathFromFullPath(containerHost.getHostname(), containerId);
            try {
                
                return mode.showLive ?
                        this.containerAPI.getLiveContainerByFolderPath(relativePath, containerHost, user, mode.respectAnonPerms) :
                        this.containerAPI.getWorkingContainerByFolderPath(relativePath, containerHost, user, mode.respectAnonPerms);
            } catch (NotFoundInDbException e) {

                // if does not found in the host path or current host, tries the default one if it is not the same
                final Host defaultHost = WebAPILocator.getHostWebAPI().findDefaultHost(user, false);
                if (!defaultHost.getIdentifier().equals(containerHost.getIdentifier())) {

                    return  this.containerAPI.getWorkingContainerByFolderPath(relativePath,
                            defaultHost, APILocator.getUserAPI().getSystemUser(), false);
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


}
