package com.dotcms.rest.api.v1.template;

import com.beust.jcommander.internal.Maps;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.exception.ValidationException;
import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.PaginationUtil;
import com.dotcms.util.pagination.ContainerPaginator;
import com.dotcms.util.pagination.OrderDirection;
import com.dotcms.util.pagination.TemplatePaginator;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.business.VersionableAPI;
import com.dotmarketing.business.web.HostWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.WebAssetFactory;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.templates.business.TemplateAPI;
import com.dotmarketing.portlets.templates.business.TemplateConstants;
import com.dotmarketing.portlets.templates.design.util.DesignTemplateUtil;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.ActivityLogger;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import com.liferay.util.servlet.SessionDialogMessage;
import io.vavr.control.Try;
import org.glassfish.jersey.server.JSONP;

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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.dotcms.util.CollectionsUtils.map;
import static com.dotmarketing.business.PermissionAPI.PERMISSION_READ;
import static com.dotmarketing.business.PermissionAPI.PERMISSION_WRITE;

/**
 * CRUD of Templates
 * @author jsanca
 */
@Path("/v1/templates")
public class TemplateResource {

    private static final String ARCHIVE_PARAM = "archive";
    private final PaginationUtil paginationUtil;
    private final WebResource    webResource;
    private final TemplateAPI    templateAPI;
    private final VersionableAPI versionableAPI;
    private final FolderAPI      folderAPI;
    private final HostWebAPI     hostWebAPI;
    private final PermissionAPI  permissionAPI;
    private final RoleAPI        roleAPI;
    private final HTMLPageAssetAPI htmlPageAssetAPI;
    private final TemplateHelper templateHelper;


    public TemplateResource() {

        this(new WebResource(),
                new PaginationUtil(new TemplatePaginator(APILocator.getTemplateAPI(),
                        new TemplateHelper(APILocator.getPermissionAPI(), APILocator.getRoleAPI()))),
                APILocator.getTemplateAPI(),
                APILocator.getVersionableAPI(),
                APILocator.getFolderAPI(),
                APILocator.getPermissionAPI(),
                WebAPILocator.getHostWebAPI(),
                APILocator.getRoleAPI(),
                APILocator.getHTMLPageAssetAPI());
    }

    @VisibleForTesting
    public TemplateResource(final WebResource     webResource,
                             final PaginationUtil templatePaginator,
                             final TemplateAPI    templateAPI,
                             final VersionableAPI versionableAPI,
                             final FolderAPI      folderAPI,
                             final PermissionAPI  permissionAPI,
                             final HostWebAPI     hostWebAPI,
                             final RoleAPI        roleAPI,
                             final HTMLPageAssetAPI htmlPageAssetAPI) {

        this.webResource    = webResource;
        this.templateAPI    = templateAPI;
        this.versionableAPI = versionableAPI;
        this.folderAPI      = folderAPI;
        this.permissionAPI  = permissionAPI;
        this.hostWebAPI     = hostWebAPI;
        this.roleAPI        = roleAPI;
        this.htmlPageAssetAPI = htmlPageAssetAPI;
        this.templateHelper = new TemplateHelper(permissionAPI, roleAPI);
        this.paginationUtil = templatePaginator;
    }

    /**
     * Return a list of {@link com.dotmarketing.portlets.templates.model.Template}, entity
     * response syntax:.
     *
     *
     * Url sintax:
     * api/v1/templates?filter=filter-string&page=page-number&per_page=per-page&ordeby=order-field-name&direction=order-direction&host=host-id
     *
     * where:
     *
     * <ul>
     * <li>filter-string: just return Template who content this pattern into its title</li>
     * <li>page: page to return</li>
     * <li>per_page: limit of items to return</li>
     * <li>ordeby: field to order by</li>
     * <li>direction: asc for upward order and desc for downward order</li>
     * <li>host: filter by host's id</li>
     * </ul>
     *
     * Url example: v1/templates?filter=test&page=2&orderby=title
     *
     * @param httpRequest
     * @return
     */
    @GET
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response list(@Context final HttpServletRequest httpRequest,
                                        @Context final HttpServletResponse httpResponse,
                                        @QueryParam(PaginationUtil.FILTER)   final String filter,
                                        @QueryParam(PaginationUtil.PAGE)     final int page,
                                        @DefaultValue("40") @QueryParam(PaginationUtil.PER_PAGE) final int perPage,
                                        @DefaultValue("title") @QueryParam(PaginationUtil.ORDER_BY) final String orderBy,
                                        @DefaultValue("ASC") @QueryParam(PaginationUtil.DIRECTION)  final String direction,
                                        @QueryParam(ContainerPaginator.HOST_PARAMETER_ID)           final String hostId,
                                        @QueryParam(ARCHIVE_PARAM)                                  final boolean archive) {

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requestAndResponse(httpRequest, httpResponse).rejectWhenNoUser(true).init();
        final User user = initData.getUser();
        final Optional<String> checkedHostId = Optional.ofNullable(Try.of(()-> APILocator.getHostAPI()
                .find(hostId, user, false).getIdentifier()).getOrNull());

        Logger.debug(this, ()-> "Getting the List of templates");

        final Map<String, Object> extraParams = Maps.newHashMap();
        extraParams.put(ARCHIVE_PARAM, archive);
        checkedHostId.ifPresent(checkedHostIdentifier -> extraParams.put(ContainerPaginator.HOST_PARAMETER_ID, checkedHostIdentifier));
        return this.paginationUtil.getPage(httpRequest, user, filter, page, perPage, orderBy, OrderDirection.valueOf(direction),
                extraParams);
    }

    /**
     * Return a {@link com.dotmarketing.portlets.templates.model.Template} based on the inode
     *
     * @return Response
     */
    @GET
    @Path("/{templateInode}")
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response getByInode(@Context final HttpServletRequest  httpRequest,
                               @Context final HttpServletResponse httpResponse,
                               @PathParam("templateInode") final String templateInode) throws DotSecurityException, DotDataException {

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requestAndResponse(httpRequest, httpResponse).rejectWhenNoUser(true).init();
        final User user     = initData.getUser();
        final PageMode mode = PageMode.get(httpRequest);
        Logger.debug(this, ()-> "Getting the template by inode: " + templateInode);

        final Template template = this.templateAPI.find(templateInode, user, mode.respectAnonPerms);

        if (null == template || UtilMethods.isNotSet(template.getIdentifier())) {

            throw new DoesNotExistException("The template inode: " + templateInode + " does not exists");
        }

        return Response.ok(new ResponseEntityView(this.templateHelper.toTemplateView(template, user))).build();
    }

    /**
     * Return a live version {@link com.dotmarketing.portlets.templates.model.Template} based on the id
     *
     * @return Response
     */
    @GET
    @Path("/{templateId}/live")
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response getLiveById(@Context final HttpServletRequest  httpRequest,
                               @Context final HttpServletResponse httpResponse,
                               @PathParam("templateId") final String templateId) throws DotSecurityException, DotDataException {

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requestAndResponse(httpRequest, httpResponse).rejectWhenNoUser(true).init();
        final User user     = initData.getUser();
        final PageMode mode = PageMode.get(httpRequest);
        Logger.debug(this, ()-> "Getting the live template by id: " + templateId);

        final Template template = this.templateAPI.findLiveTemplate(templateId, user, mode.respectAnonPerms);

        if (null == template || UtilMethods.isNotSet(template.getIdentifier())) {

            throw new DoesNotExistException("The live template id: " + templateId + " does not exists");
        }

        return Response.ok(new ResponseEntityView(this.templateHelper.toTemplateView(template, user))).build();
    }

    /**
     * Return a working version {@link com.dotmarketing.portlets.templates.model.Template} based on the id
     *
     * @return Response
     */
    @GET
    @Path("/{templateId}/working")
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response getWorkingById(@Context final HttpServletRequest  httpRequest,
                                         @Context final HttpServletResponse httpResponse,
                                         @PathParam("templateId") final String templateId) throws DotSecurityException, DotDataException {

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requestAndResponse(httpRequest, httpResponse).rejectWhenNoUser(true).init();
        final User user     = initData.getUser();
        final PageMode mode = PageMode.get(httpRequest);
        Logger.debug(this, ()-> "Getting the working template by id: " + templateId);

        final Template template = this.templateAPI.findWorkingTemplate(templateId, user, mode.respectAnonPerms);

        if (null == template || UtilMethods.isNotSet(template.getIdentifier())) {

            throw new DoesNotExistException("The working template id: " + templateId + " does not exists");
        }

        return Response.ok(new ResponseEntityView(this.templateHelper.toTemplateView(template, user))).build();
    }

    @POST
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response saveNew(@Context final HttpServletRequest  request,
                                @Context final HttpServletResponse response,
                                final TemplateForm templateForm) throws DotDataException, DotSecurityException {

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requestAndResponse(request, response).rejectWhenNoUser(true).init();
        final User user         = initData.getUser();
        final Host host         = this.hostWebAPI.getCurrentHostNoThrow(request);
        final PageMode pageMode = PageMode.get(request);

        if(!permissionAPI.doesUserHavePermission(host, PermissionAPI.PERMISSION_CAN_ADD_CHILDREN, user, pageMode.respectAnonPerms)) {

            Logger.error(this, "The user: " + user.getUserId() + " does not have permission to add a template");
            throw new DotSecurityException(WebKeys.USER_PERMISSIONS_EXCEPTION);
        }

        return Response.ok(new ResponseEntityView(this.templateHelper.toTemplateView(
                this.fillAndSaveTemplate(templateForm, user, host, pageMode, new Template()), user))).build();
    }

    /**
     * Save a single template
     * @param request       {@link HttpServletRequest}
     * @param response      {@link HttpServletResponse}
     * @param templateForm  {@link TemplateForm}
     * @return Response
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @PUT
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response save(@Context final HttpServletRequest  request,
                            @Context final HttpServletResponse response,
                            final TemplateForm templateForm) throws DotDataException, DotSecurityException {

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requestAndResponse(request, response).rejectWhenNoUser(true).init();
        final User user         = initData.getUser();
        final Host host         = this.hostWebAPI.getCurrentHostNoThrow(request);
        final PageMode pageMode = PageMode.get(request);
        final Template currentTemplate = this.templateAPI.find(templateForm.getInode(), user, pageMode.respectAnonPerms);

        if (null == currentTemplate || UtilMethods.isNotSet(currentTemplate.getIdentifier())
                || !InodeUtils.isSet(currentTemplate.getInode())) {

            throw new DoesNotExistException("The working template inode: " + templateForm.getInode() + " does not exists");
        }

        this.templateHelper.checkPermission(user, currentTemplate, PERMISSION_WRITE);

        return Response.ok(new ResponseEntityView(this.templateHelper.toTemplateView(
                this.fillAndSaveTemplate(templateForm, user, host, pageMode, currentTemplate), user))).build();
    }

    /**
     * Publish a list of template inodes
     * Return the list of a success published and failed
     * @param request            {@link HttpServletRequest}
     * @param response           {@link HttpServletResponse}
     * @param templatesToPublish {@link List} list of template inodes to publish
     * @return Response
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @PUT
    @Path("/_publish")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response PUBLISH(@Context final HttpServletRequest  request,
                               @Context final HttpServletResponse response,
                               final List<String> templatesToPublish) throws DotDataException, DotSecurityException {

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requestAndResponse(request, response).rejectWhenNoUser(true).init();
        final User user         = initData.getUser();
        final Host host         = this.hostWebAPI.getCurrentHostNoThrow(request);
        final PageMode pageMode = PageMode.get(request);
        final List<String> publishedInodes = new ArrayList<>();
        final List<String> failedInodes    = new ArrayList<>();

        if (!UtilMethods.isSet(templatesToPublish)) {

            throw new IllegalArgumentException("The body must send a collection of template inodes such as: " +
                    "[\"dd60695c-9e0f-4a2e-9fd8-ce2a4ac5c27d\",\"cc59390c-9a0f-4e7a-9fd8-ca7e4ec0c77d\"]");
        }

        for (final String templateInode : templatesToPublish) {

            final Template template = this.templateAPI.find(templateInode, user, pageMode.respectAnonPerms);

            if (null != template && InodeUtils.isSet(template.getInode())) {

                try {

                    if (this.templateAPI.publishTemplate(template, user, pageMode.respectAnonPerms)) {

                        ActivityLogger.logInfo(this.getClass(), "Publish Template action", "User " +
                                user.getPrimaryKey() + " publishing template" + template.getTitle(), host.getTitle() != null ? host.getTitle() : "default");
                        publishedInodes.add(templateInode);
                    } else {

                        failedInodes.add(templateInode);
                    }
                } catch(Exception e) {

                    Logger.error(this, e.getMessage(), e);
                    failedInodes.add(templateInode);
                }
            } else {

                failedInodes.add(templateInode);
            }
        }

        return Response.ok(new ResponseEntityView(
                CollectionsUtils.map(
                "publishedInodes", publishedInodes,
                "failedInodes",   failedInodes
                ))).build();
    }

    /**
     * Unpublish a list of template inodes
     * Return the list of a success unpublished and failed
     * @param request            {@link HttpServletRequest}
     * @param response           {@link HttpServletResponse}
     * @param templatesToUnpublish {@link List} list of template inodes to unpublish
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
                                  final List<String> templatesToUnpublish) throws DotDataException, DotSecurityException {

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requestAndResponse(request, response).rejectWhenNoUser(true).init();
        final User user         = initData.getUser();
        final Host host         = this.hostWebAPI.getCurrentHostNoThrow(request);
        final PageMode pageMode = PageMode.get(request);
        final List<String> publishedInodes = new ArrayList<>();
        final List<String> failedInodes    = new ArrayList<>();

        if (!UtilMethods.isSet(templatesToUnpublish)) {

            throw new IllegalArgumentException("The body must send a collection of template inodes such as: " +
                    "[\"dd60695c-9e0f-4a2e-9fd8-ce2a4ac5c27d\",\"cc59390c-9a0f-4e7a-9fd8-ca7e4ec0c77d\"]");
        }

        for (final String templateInode : templatesToUnpublish) {

            final Template template = this.templateAPI.find(templateInode, user, pageMode.respectAnonPerms);

            if (null != template && InodeUtils.isSet(template.getInode())) {

                try {

                    if (this.templateAPI.unpublishTemplate(template, user, pageMode.respectAnonPerms)) {

                        ActivityLogger.logInfo(this.getClass(), "UnPublish Template action", "User " +
                                user.getPrimaryKey() + " unpublishing template" + template.getTitle(), host.getTitle() != null ? host.getTitle() : "default");
                        publishedInodes.add(templateInode);
                    } else {
                        failedInodes.add(templateInode);
                    }
                } catch(Exception wax) {

                    Logger.error(this, wax.getMessage(), wax);
                    failedInodes.add(templateInode);
                }
            } else {

                failedInodes.add(templateInode);
            }
        }

        return Response.ok(new ResponseEntityView(
                CollectionsUtils.map(
                        "unpublishedInodes", publishedInodes,
                        "failedInodes",   failedInodes
                ))).build();
    }

    /**
     * Copy a template
     * @param request            {@link HttpServletRequest}
     * @param response           {@link HttpServletResponse}
     * @param templateInode      {@link String} template inode to copy
     * @return Response
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @PUT
    @Path("/{templateInode}/_copy")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response copy(@Context final HttpServletRequest  request,
                               @Context final HttpServletResponse response,
                               @PathParam("templateInode") final String templateInode) throws DotDataException, DotSecurityException {

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requestAndResponse(request, response).rejectWhenNoUser(true).init();
        final User user         = initData.getUser();
        final Host host         = this.hostWebAPI.getCurrentHostNoThrow(request);
        final PageMode pageMode = PageMode.get(request);

        Logger.debug(this, ()->"Copying the Template: " + templateInode);

        final Template template = this.templateAPI.find(templateInode, user, pageMode.respectAnonPerms);

        if (null == template || !InodeUtils.isSet(template.getInode())) {

            throw new DoesNotExistException("The  template inode: " + templateInode + " does not exists");
        }

        this.templateHelper.checkPermission(user, template, PERMISSION_WRITE);
        final Response responseRest = Response.ok(new ResponseEntityView(
                this.templateHelper.toTemplateView(this.templateAPI.copy(template, user), user))).build();

        ActivityLogger.logInfo(this.getClass(), "Copied Template", "User " +
                user.getPrimaryKey() + " copied template" + template.getTitle(), host.getTitle() != null ? host.getTitle() : "default");

        return responseRest;
    }

    /**
     * Unlock a template
     * @param request            {@link HttpServletRequest}
     * @param response           {@link HttpServletResponse}
     * @param templateInode      {@link String} template inode to unlock
     * @return Response
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @PUT
    @Path("/{templateInode}/_unlock")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response unlock(@Context final HttpServletRequest  request,
                               @Context final HttpServletResponse response,
                               @PathParam("templateInode") final String templateInode) throws DotDataException, DotSecurityException {

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requestAndResponse(request, response).rejectWhenNoUser(true).init();
        final User user         = initData.getUser();
        final PageMode pageMode = PageMode.get(request);

        Logger.debug(this, ()->"Unlocking the Template: " + templateInode);

        final Template template = this.templateAPI.find(templateInode, user, pageMode.respectAnonPerms);

        if (null == template || !InodeUtils.isSet(template.getInode())) {

            throw new DoesNotExistException("The  template inode: " + templateInode + " does not exists");
        }

        this.templateHelper.checkPermission(user, template, PERMISSION_READ);
        this.templateAPI.unlock(template, user);

        Logger.debug(this, "Unlocked template: " + templateInode);
        return Response.ok(new ResponseEntityView(true)).build();
    }

    /**
     * Archive a template
     * @param request            {@link HttpServletRequest}
     * @param response           {@link HttpServletResponse}
     * @param templateInode      {@link String} template inode to unlock
     * @return Response
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @PUT
    @Path("/{templateInode}/_archive")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response archive(@Context final HttpServletRequest  request,
                                 @Context final HttpServletResponse response,
                                 @PathParam("templateInode") final String templateInode) throws DotDataException, DotSecurityException {

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requestAndResponse(request, response).rejectWhenNoUser(true).init();
        final User user         = initData.getUser();
        final PageMode pageMode = PageMode.get(request);

        Logger.debug(this, ()->"Doing archive of the Template: " + templateInode);

        final Template template = this.templateAPI.find(templateInode, user, pageMode.respectAnonPerms);

        if (null == template || !InodeUtils.isSet(template.getInode())) {

            throw new DoesNotExistException("The  template inode: " + templateInode + " does not exists");
        }

        this.templateHelper.checkPermission(user, template, PERMISSION_WRITE);
        final boolean result = this.templateAPI.archive(template, user, pageMode.respectAnonPerms);

        Logger.debug(this, "Archive done template: " + templateInode);
        return Response.ok(new ResponseEntityView(result)).build();
    }

    /**
     * Unarchive a template
     * @param request            {@link HttpServletRequest}
     * @param response           {@link HttpServletResponse}
     * @param templateInode      {@link String} template inode to unlock
     * @return Response
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @PUT
    @Path("/{templateInode}/_unarchive")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response unarchive(@Context final HttpServletRequest  request,
                                  @Context final HttpServletResponse response,
                                  @PathParam("templateInode") final String templateInode) throws DotDataException, DotSecurityException {

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requestAndResponse(request, response).rejectWhenNoUser(true).init();
        final User user         = initData.getUser();
        final PageMode pageMode = PageMode.get(request);

        Logger.debug(this, ()->"Doing unarchive of the Template: " + templateInode);

        final Template template = this.templateAPI.find(templateInode, user, pageMode.respectAnonPerms);

        if (null == template || !InodeUtils.isSet(template.getInode())) {

            throw new DoesNotExistException("The  template inode: " + templateInode + " does not exists");
        }

        this.templateHelper.checkPermission(user, template, PERMISSION_WRITE);
        this.templateAPI.unarchive(template);

        Logger.debug(this, "Unarchive done template: " + templateInode);
        return Response.ok(new ResponseEntityView(true)).build();
    }

    /**
     * Deletes a template
     * Pre: template must not has dependencies
     * @param request            {@link HttpServletRequest}
     * @param response           {@link HttpServletResponse}
     * @param templateInode      {@link String} template inode to look for the template and then, delete it
     * @return Response
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @DELETE
    @Path("/{templateInode}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response delete(@Context final HttpServletRequest  request,
                                    @Context final HttpServletResponse response,
                                    @PathParam("templateInode") final String templateInode) throws Exception {

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requestAndResponse(request, response).rejectWhenNoUser(true).init();
        final User user         = initData.getUser();
        final PageMode pageMode = PageMode.get(request);

        Logger.debug(this, ()->"Deleting the Template: " + templateInode);

        final Template template = this.templateAPI.find(templateInode, user, pageMode.respectAnonPerms);

        if (null == template || !InodeUtils.isSet(template.getInode())) {

            throw new DoesNotExistException("The  template inode: " + templateInode + " does not exists");
        }

        final SessionDialogMessage error = new SessionDialogMessage(
                LanguageUtil.get(user, "Delete-Template"),
                LanguageUtil.get(user, TemplateConstants.TEMPLATE_DELETE_ERROR),
                LanguageUtil.get(user.getLocale(), "message.template.dependencies.top", TemplateConstants.TEMPLATE_DEPENDENCY_SEARCH_LIMIT) +
                        "<br>" + LanguageUtil.get(user.getLocale(), "message.template.dependencies.query",
                        "<br>+baseType:" + BaseContentType.HTMLPAGE.getType() +
                                " +catchall:" + template.getIdentifier()
                ));

        return this.canTemplateBeDeleted(template, user, error)?
             Response.ok(new ResponseEntityView(this.templateAPI.deleteTemplate(template,user, pageMode.respectAnonPerms))).build():
             Response.status(Response.Status.BAD_REQUEST).entity(map("message", error)).build();
    }

    @WrapInTransaction
    private Template fillAndSaveTemplate(final TemplateForm templateForm,
                                         final User user,
                                         final Host host,
                                         final PageMode pageMode,
                                         final Template template) throws DotSecurityException, DotDataException {

        if(UtilMethods.isSet(templateForm.getTheme())) {

            template.setThemeName(this.folderAPI.find(templateForm.getTheme(), user, pageMode.respectAnonPerms).getName());
        }

        template.setBody(templateForm.getBody());
        template.setCountContainers(templateForm.getCountAddContainer());
        template.setCountAddContainer(templateForm.getCountAddContainer());
        template.setSortOrder(templateForm.getSortOrder());
        template.setTitle(templateForm.getTitle());
        template.setModUser(user.getUserId());
        template.setOwner(user.getUserId());
        template.setModDate(new Date());
        template.setDrawedBody(templateForm.getDrawedBody());
        template.setFooter(templateForm.getFooter());
        template.setFriendlyName(templateForm.getFriendlyName());
        template.setHeadCode(templateForm.getHeadCode());
        template.setImage(templateForm.getImage());
        template.setSelectedimage(templateForm.getSelectedimage());
        template.setHeader(templateForm.getHeader());

        if (templateForm.isDrawed()) {

            final String themeHostId = APILocator.getFolderAPI().find(templateForm.getTheme(), user, pageMode.respectAnonPerms).getHostId();
            final String themePath   = themeHostId.equals(host.getInode())?
                    Template.THEMES_PATH + template.getThemeName() + "/":
                    "//" + APILocator.getHostAPI().find(themeHostId, user, pageMode.respectAnonPerms).getHostname()
                            + Template.THEMES_PATH + template.getThemeName() + "/";

            final StringBuffer endBody = DesignTemplateUtil.getBody(template.getBody(), template.getHeadCode(),
                    themePath, templateForm.isHeaderCheck(), templateForm.isFooterCheck());

            // set the drawedBody for future edit
            template.setDrawedBody(template.getBody());
            // set the real body
            template.setBody(endBody.toString());
        }

        this.versionableAPI.setLocked(
                this.templateAPI.saveTemplate(template, host, user, pageMode.respectAnonPerms), false, user);

        ActivityLogger.logInfo(this.getClass(), "Saved Template", "User " + user.getPrimaryKey()
                + "Template: " + template.getTitle(), host.getTitle() != null? host.getTitle():"default");

        return template;
    }

    /**
     * Returns true if a template is not being used by any html pages and can be deleted, false otherwise
     * @param template
     * @param user
     * @return true if template can be deleted
     */
    private boolean canTemplateBeDeleted (final Template template, final User user, final SessionDialogMessage errorMessage) {

        final Map<String, String> resultMap = this.templateAPI.checkPageDependencies(template, user, false);

        if (resultMap != null && !resultMap.isEmpty()) {

            for (final Map.Entry<String, String> entry: resultMap.entrySet()) {

                errorMessage.addMessage(entry.getKey(),entry.getValue());
            }

            return false;
        }

        return true;
    }
}
