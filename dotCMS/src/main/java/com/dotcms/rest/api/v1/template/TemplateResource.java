package com.dotcms.rest.api.v1.template;

import com.dotcms.business.WrapInTransaction;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.api.BulkResultView;
import com.dotcms.rest.api.FailedResultView;
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
import com.dotmarketing.exception.WebAssetException;
import com.dotmarketing.portlets.containers.business.ContainerAPI;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.templates.business.TemplateAPI;
import com.dotmarketing.portlets.templates.design.util.DesignTemplateUtil;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.ActivityLogger;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;
import io.vavr.control.Try;
import org.glassfish.jersey.server.JSONP;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.BadRequestException;
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
import java.util.HashMap;
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
    private final TemplateHelper templateHelper;


    public TemplateResource() {

        this(new WebResource(),
                new PaginationUtil(new TemplatePaginator(APILocator.getTemplateAPI(),
                        new TemplateHelper(APILocator.getPermissionAPI(),
                                APILocator.getRoleAPI(),
                                APILocator.getContainerAPI()))),
                APILocator.getTemplateAPI(),
                APILocator.getVersionableAPI(),
                APILocator.getFolderAPI(),
                APILocator.getPermissionAPI(),
                WebAPILocator.getHostWebAPI(),
                APILocator.getRoleAPI(),
                APILocator.getContainerAPI());
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
                             final ContainerAPI   containerAPI) {

        this.webResource    = webResource;
        this.templateAPI    = templateAPI;
        this.versionableAPI = versionableAPI;
        this.folderAPI      = folderAPI;
        this.permissionAPI  = permissionAPI;
        this.hostWebAPI     = hostWebAPI;
        this.templateHelper = new TemplateHelper(permissionAPI, roleAPI, containerAPI);
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

        final Map<String, Object> extraParams = new HashMap<>();
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
    @Path("/live")
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response getLiveById(@Context final HttpServletRequest  httpRequest,
                               @Context final HttpServletResponse httpResponse,
                               @QueryParam("templateId") final String templateId) throws DotSecurityException, DotDataException {

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
    @Path("/working")
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response getWorkingById(@Context final HttpServletRequest  httpRequest,
                                         @Context final HttpServletResponse httpResponse,
                                         @QueryParam("templateId") final String templateId) throws DotSecurityException, DotDataException {

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
     * Save an existing template
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
        final Template currentTemplate = this.findTemplateByForm(templateForm, user, pageMode);

        if (null == currentTemplate) {

            throw new DoesNotExistException("The working template inode: " + templateForm.getInode()
                    + " or id: " + templateForm.getIdentifier() + " does not exists");
        }

        this.templateHelper.checkPermission(user, currentTemplate, PERMISSION_WRITE);

        return Response.ok(new ResponseEntityView(this.templateHelper.toTemplateView(
                this.fillAndSaveTemplate(templateForm, user, host, pageMode, currentTemplate), user))).build();
    }

    private Template findTemplateByForm (final TemplateForm templateForm, final User user, final PageMode pageMode) throws DotDataException, DotSecurityException {

        return this.findTemplateBy(templateForm.getInode(), templateForm.getIdentifier(), user, pageMode);
    }

    private Template findTemplateBy (final String templateInode, final String templateIdentifier,
                                         final User user, final PageMode pageMode) throws DotDataException, DotSecurityException {

        Template template = null;
        try {
            if (UtilMethods.isSet(templateInode)) {
                template = this.templateAPI.find(templateInode, user, pageMode.respectAnonPerms);
            }
        } catch (Exception e) {

            Logger.debug(this, e.getMessage(), e);
            // if does not work by inode see if can by id
        }

        if ((null == template || InodeUtils.isSet(template.getInode())) && UtilMethods.isSet(templateIdentifier)) {

            return pageMode.showLive?
                    this.templateAPI.findLiveTemplate(templateIdentifier, user, pageMode.respectAnonPerms):
                    this.templateAPI.findWorkingTemplate(templateIdentifier, user, pageMode.respectAnonPerms);
        }

        throw new DoesNotExistException("Can not find the template");
    }


    /**
     * Save and publish a new template
     * @param request        {@link HttpServletResponse}
     * @param response       {@link HttpServletResponse}
     * @param templateForm   {@link TemplateForm}
     * @return Response
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @POST
    @Path("/_savepublish")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response saveNewAndPublish(@Context final HttpServletRequest  request,
                                  @Context final HttpServletResponse response,
                                  final TemplateForm templateForm)
            throws DotDataException, DotSecurityException, WebAssetException {

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requestAndResponse(request, response).rejectWhenNoUser(true).init();
        final User user         = initData.getUser();
        final Host host         = this.hostWebAPI.getCurrentHostNoThrow(request);
        final PageMode pageMode = PageMode.get(request);

        Logger.debug(this, ()-> "Saving and Publishing new template: " + templateForm.getTitle());
        if(!permissionAPI.doesUserHavePermission(host, PermissionAPI.PERMISSION_CAN_ADD_CHILDREN, user, pageMode.respectAnonPerms)) {

            Logger.error(this, "The user: " + user.getUserId() + " does not have permission to add a template");
            throw new DotSecurityException(WebKeys.USER_PERMISSIONS_EXCEPTION);
        }

        return Response.ok(new ResponseEntityView(this.templateHelper.toTemplateView(
                this.saveAndPublish(templateForm, user, host, pageMode, new Template()), user))).build();
    }

    /**
     * Save and publish an existing template
     * @param request       {@link HttpServletRequest}
     * @param response      {@link HttpServletResponse}
     * @param templateForm  {@link TemplateForm}
     * @return Response
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @PUT
    @Path("/_savepublish")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response saveAndPublish(@Context final HttpServletRequest  request,
                               @Context final HttpServletResponse response,
                               final TemplateForm templateForm)
            throws DotDataException, DotSecurityException, WebAssetException {

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requestAndResponse(request, response).rejectWhenNoUser(true).init();
        final User user         = initData.getUser();
        final Host host         = this.hostWebAPI.getCurrentHostNoThrow(request);
        final PageMode pageMode = PageMode.get(request);
        final Template currentTemplate = this.findTemplateByForm(templateForm, user, pageMode);

        if (null == currentTemplate) {

            throw new DoesNotExistException("The working template inode: " + templateForm.getInode()
                    + " or id: " + templateForm.getIdentifier() + " does not exists");
        }

        this.templateHelper.checkPermission(user, currentTemplate, PERMISSION_WRITE);

        return Response.ok(new ResponseEntityView(this.templateHelper.toTemplateView(
                this.saveAndPublish(templateForm, user, host, pageMode, currentTemplate), user))).build();
    }

    @WrapInTransaction
    private Template saveAndPublish (final TemplateForm templateForm,
                                     final User user,
                                     final Host host,
                                     final PageMode pageMode,
                                     final Template template)
            throws DotDataException, DotSecurityException, WebAssetException {

        final Template savedTemplate = this.fillAndSaveTemplate(templateForm, user, host, pageMode, template);
        this.templateAPI.publishTemplate(savedTemplate, user, pageMode.respectAnonPerms);
        return savedTemplate;
    }

    @WrapInTransaction
    private Template fillAndSaveTemplate(final TemplateForm templateForm,
            final User user,
            final Host host,
            final PageMode pageMode,
            final Template template) throws DotSecurityException, DotDataException {

        if(UtilMethods.isSet(templateForm.getTheme())) {

            final Folder themeFolder = this.folderAPI.find(templateForm.getTheme(), user, pageMode.respectAnonPerms);

            if (null != themeFolder && InodeUtils.isSet(themeFolder.getInode())) {
                template.setThemeName(this.folderAPI.find(templateForm.getTheme(), user, pageMode.respectAnonPerms).getName());
            } else {

                throw new BadRequestException("Invalid theme: " + templateForm.getTheme());
            }
            template.setTheme(templateForm.getTheme());
        }

        template.setBody(templateForm.getBody());
        template.setCountContainers(templateForm.getCountAddContainer());
        template.setCountAddContainer(templateForm.getCountAddContainer());
        template.setSortOrder(templateForm.getSortOrder());
        template.setTitle(templateForm.getTitle());
        template.setModUser(user.getUserId());
        template.setOwner(user.getUserId());
        template.setModDate(new Date());
        if (null != templateForm.getLayout()) {
            template.setDrawedBody(this.templateHelper.toTemplateLayout(templateForm.getLayout()));
            template.setDrawed(true);
        } else {
            template.setDrawedBody(templateForm.getDrawedBody());
        }
        template.setFooter(templateForm.getFooter());
        template.setFriendlyName(templateForm.getFriendlyName());
        template.setHeadCode(templateForm.getHeadCode());
        template.setImage(templateForm.getImage());
        template.setSelectedimage(templateForm.getSelectedimage());
        template.setHeader(templateForm.getHeader());

        if (templateForm.isDrawed()) { // todo: not sure if this needed

            final String themeHostId = APILocator.getFolderAPI().find(templateForm.getTheme(), user, pageMode.respectAnonPerms).getHostId();
            final String themePath   = themeHostId.equals(host.getInode())?
                    Template.THEMES_PATH + template.getThemeName() + "/":
                    "//" + APILocator.getHostAPI().find(themeHostId, user, pageMode.respectAnonPerms).getHostname()
                            + Template.THEMES_PATH + template.getThemeName() + "/";

            final StringBuffer endBody = DesignTemplateUtil.getBody(template.getBody(), template.getHeadCode(),
                    themePath, templateForm.isHeaderCheck(), templateForm.isFooterCheck());

            // set the drawedBody for future edit
            //template.setDrawedBody(template.getBody());
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
     * Publishes Template(s)
     *
     * This method receives a list of identifiers and publishes the templates.
     * To publish a template successfully the user needs to have Publish Permissions and the template
     * can not be archived.
     *
     * @param request            {@link HttpServletRequest}
     * @param response           {@link HttpServletResponse}
     * @param templatesToPublish {@link List} list of template ids to publish
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
                               final List<String> templatesToPublish){

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requestAndResponse(request, response).rejectWhenNoUser(true).init();
        final User user         = initData.getUser();
        final PageMode pageMode = PageMode.get(request);
        Long publishedTemplatesCount = 0L;
        final List<FailedResultView> failedToPublish    = new ArrayList<>();

        if (!UtilMethods.isSet(templatesToPublish)) {

            throw new IllegalArgumentException("The body must send a collection of template identifiers such as: " +
                    "[\"dd60695c-9e0f-4a2e-9fd8-ce2a4ac5c27d\",\"cc59390c-9a0f-4e7a-9fd8-ca7e4ec0c77d\"]");
        }

        for (final String templateId : templatesToPublish) {
            try{
                final Template template = this.templateAPI.findWorkingTemplate(templateId,user,pageMode.respectAnonPerms);
                if (null != template && InodeUtils.isSet(template.getInode())){
                    this.templateAPI.publishTemplate(template, user, pageMode.respectAnonPerms);
                    ActivityLogger.logInfo(this.getClass(), "Publish Template Action", "User " +
                            user.getPrimaryKey() + " published template: " + template.getIdentifier());
                    publishedTemplatesCount++;
                } else {
                    Logger.error(this, "Template with Id: " + templateId + " does not exists");
                    failedToPublish.add(new FailedResultView(templateId,"Template Does Not Exists"));
                }
            } catch(Exception e) {
                Logger.debug(this, e.getMessage(), e);
                failedToPublish.add(new FailedResultView(templateId,e.getMessage()));
            }
        }

        return Response.ok(new ResponseEntityView(
                new BulkResultView(publishedTemplatesCount,0L,failedToPublish)))
                .build();
    }

    /**
     * Unpublishes Template(s)
     *
     * This method receives a list of identifiers and unpublishes the templates.
     * To publish a template successfully the user needs to have Publish Permissions and the template
     * can not be archived.
     *
     * @param request            {@link HttpServletRequest}
     * @param response           {@link HttpServletResponse}
     * @param templatesToUnpublish {@link List} list of template ids to unpublish
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
                                  final List<String> templatesToUnpublish) {

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requestAndResponse(request, response).rejectWhenNoUser(true).init();
        final User user         = initData.getUser();
        final PageMode pageMode = PageMode.get(request);
        Long unpublishedTemplatesCount = 0L;
        final List<FailedResultView> failedToUnpublish    = new ArrayList<>();

        if (!UtilMethods.isSet(templatesToUnpublish)) {

            throw new IllegalArgumentException("The body must send a collection of template identifiers such as: " +
                    "[\"dd60695c-9e0f-4a2e-9fd8-ce2a4ac5c27d\",\"cc59390c-9a0f-4e7a-9fd8-ca7e4ec0c77d\"]");
        }

        for (final String templateId : templatesToUnpublish) {
            try{
                final Template template = this.templateAPI.findWorkingTemplate(templateId,user,pageMode.respectAnonPerms);
                if (null != template && InodeUtils.isSet(template.getInode())){
                    this.templateAPI.unpublishTemplate(template, user, pageMode.respectAnonPerms);
                    ActivityLogger.logInfo(this.getClass(), "Unpublish Template Action", "User " +
                            user.getPrimaryKey() + " unpublished template: " + template.getIdentifier());
                    unpublishedTemplatesCount++;
                } else {
                    Logger.error(this, "Template with Id: " + templateId + " does not exists");
                    failedToUnpublish.add(new FailedResultView(templateId,"Template Does Not Exists"));
                }
            } catch(Exception e) {
                Logger.debug(this, e.getMessage(), e);
                failedToUnpublish.add(new FailedResultView(templateId,e.getMessage()));
            }
        }

        return Response.ok(new ResponseEntityView(
                new BulkResultView(unpublishedTemplatesCount,0L,failedToUnpublish)))
                .build();
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

        final Template template = this.findTemplateBy(templateInode, templateInode, user, pageMode);

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
     * Lock a template
     * @param request            {@link HttpServletRequest}
     * @param response           {@link HttpServletResponse}
     * @param templateId      {@link String} template identifier to lock
     * @return Response
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @PUT
    @Path("/{templateId}/_lock")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response lock(@Context final HttpServletRequest  request,
            @Context final HttpServletResponse response,
            @PathParam("templateId") final String templateId) throws DotDataException, DotSecurityException {

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requestAndResponse(request, response).rejectWhenNoUser(true).init();
        final User user         = initData.getUser();
        final PageMode pageMode = PageMode.get(request);

        final Template template = this.templateAPI.findWorkingTemplate(templateId,user,pageMode.respectAnonPerms);

        if (null == template || !InodeUtils.isSet(template.getInode())) {
            throw new DoesNotExistException("Template with Id: " + templateId + " does not exists");
        }

        this.templateAPI.lock(template, user);

        ActivityLogger.logInfo(this.getClass(), "Locked Template Action", "User " +
                user.getPrimaryKey() + " locked template: " + template.getIdentifier());

        return Response.ok(new ResponseEntityView("Template Locked Successfully")).build();
    }

    /**
     * Unlock a template
     * @param request            {@link HttpServletRequest}
     * @param response           {@link HttpServletResponse}
     * @param templateId      {@link String} template identifier to unlock
     * @return Response
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @PUT
    @Path("/{templateId}/_unlock")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response unlock(@Context final HttpServletRequest  request,
                               @Context final HttpServletResponse response,
                               @PathParam("templateId") final String templateId) throws DotDataException, DotSecurityException {

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requestAndResponse(request, response).rejectWhenNoUser(true).init();
        final User user         = initData.getUser();
        final PageMode pageMode = PageMode.get(request);

        final Template template = this.templateAPI.findWorkingTemplate(templateId,user,pageMode.respectAnonPerms);

        if (null == template || !InodeUtils.isSet(template.getInode())) {
            throw new DoesNotExistException("Template with Id: " + templateId + " does not exists");
        }

        this.templateAPI.unlock(template, user);

        ActivityLogger.logInfo(this.getClass(), "Unlocked Template Action", "User " +
                user.getPrimaryKey() + " unlocked template: " + template.getIdentifier());
        return Response.ok(new ResponseEntityView("Template Unlocked Successfully")).build();
    }

    /**
     * Archives template(s).
     *
     * This method receives a list of identifiers and archives the templates.
     * To archive a template successfully the user needs to have Edit Permissions.
     *
     * @param request            {@link HttpServletRequest}
     * @param response           {@link HttpServletResponse}
     * @param templatesToArchive {@link List} templates identifier to archive.
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
                                 final List<String> templatesToArchive) {

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requestAndResponse(request, response).rejectWhenNoUser(true).init();
        final User user         = initData.getUser();
        final PageMode pageMode = PageMode.get(request);
        Long archivedTemplatesCount = 0L;
        final List<FailedResultView> failedToArchive    = new ArrayList<>();

        if (!UtilMethods.isSet(templatesToArchive)) {

            throw new IllegalArgumentException("The body must send a collection of template identifier such as: " +
                    "[\"dd60695c-9e0f-4a2e-9fd8-ce2a4ac5c27d\",\"cc59390c-9a0f-4e7a-9fd8-ca7e4ec0c77d\"]");
        }

        for(final String templateId : templatesToArchive){
            try{
                final Template template = this.templateAPI.findWorkingTemplate(templateId,user,pageMode.respectAnonPerms);
                if (null != template && InodeUtils.isSet(template.getInode())){
                    this.templateAPI.archive(template, user, pageMode.respectAnonPerms);
                    ActivityLogger.logInfo(this.getClass(), "Archive Template Action", "User " +
                            user.getPrimaryKey() + " archived template: " + template.getIdentifier());
                    archivedTemplatesCount++;
                } else {
                    Logger.error(this, "Template with Id: " + templateId + " does not exists");
                    failedToArchive.add(new FailedResultView(templateId,"Template Does Not Exists"));
                }
            } catch(Exception e) {
                Logger.debug(this,e.getMessage(),e);
                failedToArchive.add(new FailedResultView(templateId,e.getMessage()));
            }
        }

        return Response.ok(new ResponseEntityView(
                new BulkResultView(archivedTemplatesCount,0L,failedToArchive)))
                .build();
    }

    /**
     * Unarchives template(s).
     *
     * This method receives a list of identifiers and unarchives the templates.
     * To unarchive a template successfully the user needs to have Edit Permissions and the template
     * needs to be archived.
     *
     * @param request            {@link HttpServletRequest}
     * @param response           {@link HttpServletResponse}
     * @param templatesToUnarchive {@link List} templates identifier to unarchive.
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
            final List<String> templatesToUnarchive){

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requestAndResponse(request, response).rejectWhenNoUser(true).init();
        final User user         = initData.getUser();
        final PageMode pageMode = PageMode.get(request);
        Long unarchivedTemplatesCount = 0L;
        final List<FailedResultView> failedToUnarchive    = new ArrayList<>();

        if (!UtilMethods.isSet(templatesToUnarchive)) {

            throw new IllegalArgumentException("The body must send a collection of template identifier such as: " +
                    "[\"dd60695c-9e0f-4a2e-9fd8-ce2a4ac5c27d\",\"cc59390c-9a0f-4e7a-9fd8-ca7e4ec0c77d\"]");
        }

        for(final String templateId : templatesToUnarchive){
            try{
                final Template template = this.templateAPI.findWorkingTemplate(templateId,user,pageMode.respectAnonPerms);
                if (null != template && InodeUtils.isSet(template.getInode())){
                    this.templateAPI.unarchive(template, user);
                    ActivityLogger.logInfo(this.getClass(), "Unarchive Template Action", "User " +
                            user.getPrimaryKey() + " unarchived template: " + template.getIdentifier());
                    unarchivedTemplatesCount++;
                } else {
                    Logger.error(this, "Template with Id: " + templateId + " does not exists");
                    failedToUnarchive.add(new FailedResultView(templateId,"Template Does Not Exists"));
                }
            } catch(Exception e) {
                Logger.debug(this, e.getMessage(), e);
                failedToUnarchive.add(new FailedResultView(templateId,e.getMessage()));
            }
        }

        return Response.ok(new ResponseEntityView(
                new BulkResultView(unarchivedTemplatesCount,0L,failedToUnarchive)))
                .build();
    }

    /**
     * Deletes Template(s).
     *
     * This method receives a list of identifiers and deletes the templates.
     * To delete a template successfully the user needs to have Edit Permissions over it and
     * it must be archived and no dependencies (pages referencing the template).
     * @param request            {@link HttpServletRequest}
     * @param response           {@link HttpServletResponse}
     * @param templatesToDelete {@link String} template identifier to look for and then delete it
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
                                 final List<String> templatesToDelete) {

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requestAndResponse(request, response).rejectWhenNoUser(true).init();
        final User user         = initData.getUser();
        final PageMode pageMode = PageMode.get(request);
        Long deletedTemplatesCount  = 0L;
        final List<FailedResultView> failedToDelete  = new ArrayList<>();

        if (!UtilMethods.isSet(templatesToDelete)) {

            throw new IllegalArgumentException("The body must send a collection of template identifier such as: " +
                    "[\"dd60695c-9e0f-4a2e-9fd8-ce2a4ac5c27d\",\"cc59390c-9a0f-4e7a-9fd8-ca7e4ec0c77d\"]");
        }

        for(final String templateId : templatesToDelete){
            try{
                final Template template = this.templateAPI.findWorkingTemplate(templateId,user,pageMode.respectAnonPerms);
                if (null != template && InodeUtils.isSet(template.getInode())){
                    this.templateAPI.deleteTemplate(template, user, pageMode.respectAnonPerms);
                    ActivityLogger.logInfo(this.getClass(), "Delete Template Action", "User " +
                            user.getPrimaryKey() + " deleted template: " + template.getIdentifier());
                    deletedTemplatesCount++;
                } else {
                    Logger.error(this, "Template with Id: " + templateId + " does not exists");
                    failedToDelete.add(new FailedResultView(templateId,"Template Does Not Exists"));
                }
            } catch(Exception e){
                Logger.debug(this,e.getMessage(),e);
                failedToDelete.add(new FailedResultView(templateId,e.getMessage()));
            }
        }

        return Response.ok(new ResponseEntityView(
                new BulkResultView(deletedTemplatesCount,0L,failedToDelete)))
                .build();
    }
}
