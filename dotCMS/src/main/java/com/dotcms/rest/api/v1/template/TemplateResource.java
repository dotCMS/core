package com.dotcms.rest.api.v1.template;

import com.dotcms.business.WrapInTransaction;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.api.BulkResultView;
import com.dotcms.rest.api.FailedResultView;
import com.dotcms.util.PaginationUtil;
import com.dotcms.util.pagination.OrderDirection;
import com.dotcms.util.pagination.TemplatePaginator;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Theme;
import com.dotmarketing.business.web.HostWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.business.ContainerAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.templates.business.TemplateAPI;
import com.dotmarketing.portlets.templates.business.TemplateSaveParameters;
import com.dotmarketing.portlets.templates.design.bean.TemplateLayout;
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
import com.liferay.util.StringPool;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.vavr.Lazy;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * CRUD of Templates
 * @author jsanca
 */
@Path("/v1/templates")
@Tag(name = "Templates", description = "Endpoints for managing page templates and layouts")
public class TemplateResource {

    private static final String ARCHIVE_PARAM = "archive";
    public static final String USER = "User ";
    private final PaginationUtil paginationUtil;
    private final WebResource    webResource;
    private final TemplateAPI    templateAPI;
    private final HostWebAPI     hostWebAPI;
    private final TemplateHelper templateHelper;

    public TemplateResource() {
        this(new WebResource(),
                new PaginationUtil(new TemplatePaginator(APILocator.getTemplateAPI(),
                        new TemplateHelper(APILocator.getContainerAPI()))),
                APILocator.getTemplateAPI(),
                WebAPILocator.getHostWebAPI(),
                APILocator.getContainerAPI());
    }

    @VisibleForTesting
    public TemplateResource(final WebResource     webResource,
                             final PaginationUtil templatePaginator,
                             final TemplateAPI    templateAPI,
                             final HostWebAPI     hostWebAPI,
                             final ContainerAPI   containerAPI) {

        this.webResource    = webResource;
        this.templateAPI    = templateAPI;
        this.hostWebAPI     = hostWebAPI;
        this.templateHelper = new TemplateHelper(containerAPI);
        this.paginationUtil = templatePaginator;
    }

    /**
     * Return a list of {@link com.dotmarketing.portlets.templates.model.Template} which the user has READ permissions. Each of the templates
     * is the current working version.
     *
     * Url syntax:
     * api/v1/templates?filter=filter-string&page=page-number&per_page=per-page&orderby=order-field-name&direction=order-direction&host=host-id&archive=true|false
     * where:
     * @param filter template title or identifier must content this pattern
     * @param page page to return
     * @param perPage limit of items
     * @param orderBy field to order the items. Default value is by mod_date
     * @param direction order direction (ASC for ascending or DESC for descending). Default value is DESC
     * @param hostId filter by site (where the template lives).
     * @param archive if true will return the templates that are archived. Default value is false
     *
     * @return a paginated list of templates that the user has READ permissions and comply with the params provided.
     */
    @GET
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response list(@Context final HttpServletRequest httpRequest,
                                        @Context final HttpServletResponse httpResponse,
                                        @QueryParam(PaginationUtil.FILTER)   final String filter,
                                        @QueryParam(PaginationUtil.PAGE)     final int page,
                                        @DefaultValue("40") @QueryParam(PaginationUtil.PER_PAGE) final int perPage,
                                        @DefaultValue("mod_date") @QueryParam(PaginationUtil.ORDER_BY) final String orderBy,
                                        @DefaultValue("DESC") @QueryParam(PaginationUtil.DIRECTION)  final String direction,
                                        @QueryParam(TemplatePaginator.HOST_PARAMETER_ID)           final String hostId,
                                        @QueryParam(ARCHIVE_PARAM)                                  final boolean archive) {

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requestAndResponse(httpRequest, httpResponse).rejectWhenNoUser(true).init();
        final User user = initData.getUser();


        Logger.debug(this, ()-> "Getting the List of templates");

        final Map<String, Object> extraParams = new HashMap<>();
        extraParams.put(ARCHIVE_PARAM, archive);

        //In case we need to get the list of templates across multiple sites, we don't set the TemplatePaginator.HOST_PARAMETER_ID
        if (null == hostId || !StringPool.STAR.equals(hostId)) {
            final Lazy<String> lazyCurrentHost = Lazy.of(() -> Try.of(() -> Host.class.cast(
                            httpRequest.getSession().getAttribute(WebKeys.CURRENT_HOST)).getIdentifier())
                    .getOrNull());
            final Optional<String> checkedHostId = Optional.ofNullable(
                    Try.of(() -> APILocator.getHostAPI()
                                    .find(hostId, user, false).getIdentifier())
                            .getOrElse(lazyCurrentHost.get()));
            checkedHostId.ifPresent(
                    checkedHostIdentifier -> extraParams.put(TemplatePaginator.HOST_PARAMETER_ID,
                            checkedHostIdentifier));
        }
        return this.paginationUtil.getPage(httpRequest, user, filter, page, perPage, orderBy, OrderDirection.valueOf(direction),
                extraParams);
    }

    /**
     * Return live version {@link com.dotmarketing.portlets.templates.model.Template} based on the id
     *
     * @param httpRequest
     * @param httpResponse
     * @param templateId template identifier to get the live version.
     * @return
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @GET
    @Path("/{templateId}/live")
    @JSONP
    @NoCache
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

            throw new DoesNotExistException("Live Version of the Template with Id: " + templateId + " does not exist");
        }

        return Response.ok(new ResponseEntityView(this.templateHelper.toTemplateView(template, user))).build();
    }

    /**
     * Return working version {@link com.dotmarketing.portlets.templates.model.Template} based on the id
     *
     * @param httpRequest
     * @param httpResponse
     * @param templateId template identifier to get the working version.
     * @return
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @GET
    @Path("/{templateId}/working")
    @JSONP
    @NoCache
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

            throw new DoesNotExistException("Working Version of the Template with Id: " + templateId + " does not exist");
        }

        return Response.ok(new ResponseEntityView(this.templateHelper.toTemplateView(template, user))).build();
    }

    /**
     * Saves a new working version of a template.
     *
     * @param request
     * @param response
     * @param templateForm
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
                                final TemplateForm templateForm) throws DotDataException, DotSecurityException {

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requestAndResponse(request, response).rejectWhenNoUser(true).init();
        final User user         = initData.getUser();
        final Host host         = this.templateHelper.getHost(templateForm.getSiteId(), ()->this.hostWebAPI.getCurrentHostNoThrow(request));
        final PageMode pageMode = PageMode.get(request);

        return Response.ok(new ResponseEntityView(this.templateHelper.toTemplateView(
                this.fillAndSaveTemplate(templateForm, user, host, pageMode, new Template()), user))).build();
    }



    /**
     * Saves a new working version of an existing template. The templateForm must contain the identifier of the template.
     *
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
        final Host host         = this.templateHelper.getHost(templateForm.getSiteId(), ()->this.hostWebAPI.getCurrentHostNoThrow(request));
        final PageMode pageMode = PageMode.get(request);
        final Template currentTemplate = this.templateAPI.findWorkingTemplate(templateForm.getIdentifier(),user,pageMode.respectAnonPerms);

        if (null == currentTemplate) {
            throw new DoesNotExistException("Template with Id: " + templateForm.getIdentifier() + " does not exist");
        }

        final Template newVersionTemplate = new Template();
        newVersionTemplate.setIdentifier(currentTemplate.getIdentifier());

        return Response.ok(new ResponseEntityView(this.templateHelper.toTemplateView(
                this.fillAndSaveTemplate(templateForm, user, host, pageMode, newVersionTemplate), user))).build();
    }

    /**
     * Saves a new draft version of an existing template. The templateForm must contain the identifier of the template.
     *
     * @param request       {@link HttpServletRequest}
     * @param response      {@link HttpServletResponse}
     * @param templateForm  {@link TemplateForm}
     * @return Response
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @PUT
    @Path("/draft")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response saveDraft(@Context final HttpServletRequest  request,
                               @Context final HttpServletResponse response,
                               final TemplateForm templateForm) throws DotDataException, DotSecurityException {

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requestAndResponse(request, response).rejectWhenNoUser(true).init();
        final User user         = initData.getUser();
        final Host host         = this.templateHelper.getHost(templateForm.getSiteId(), ()->this.hostWebAPI.getCurrentHostNoThrow(request));
        final PageMode pageMode = PageMode.get(request);
        final Template currentTemplate = this.templateAPI.findWorkingTemplate(templateForm.getIdentifier(),
                user, pageMode.respectAnonPerms);

        if (null == currentTemplate) {
            throw new DoesNotExistException("Template with Id: " + templateForm.getIdentifier() + " does not exist");
        }

        final Template newVersionTemplate = new Template();
        newVersionTemplate.setIdentifier(currentTemplate.getIdentifier());

        return Response.ok(new ResponseEntityView<>(this.templateHelper.toTemplateView(
                this.fillAndSaveDraftTemplate(templateForm, user, host, pageMode, newVersionTemplate), user))).build();
    }

    @WrapInTransaction
    private Template fillAndSaveTemplate(final TemplateForm templateForm,
                                         final User user,
                                         final Host host,
                                         final PageMode pageMode,
                                         final Template template) throws DotSecurityException, DotDataException {

        template.setInode(StringPool.BLANK);
        fillTemplate(templateForm, user, host, pageMode.respectAnonPerms, template);

        if (null != templateForm.getLayout()) {
            final TemplateLayout templateLayout = this.templateHelper.toTemplateLayout(templateForm.getLayout());
            template.setDrawedBody(templateLayout);
            template.setDrawed(true);

            final TemplateSaveParameters parameters = new TemplateSaveParameters.Builder()
                    .setNewTemplate(template)
                    .setNewLayout(templateLayout)
                    .setSite(host)
                    .build();

            this.templateAPI.saveAndUpdateLayout(parameters, user, pageMode.respectAnonPerms);
        } else {
            template.setDrawedBody(templateForm.getDrawedBody());
            this.templateAPI.saveTemplate(template, host, user, pageMode.respectAnonPerms);
        }

        ActivityLogger.logInfo(this.getClass(), "Saved Template", USER + user.getPrimaryKey()
                + "Template: " + template.getTitle(), host.getTitle() != null? host.getTitle():"default");

        return template;
    }

    /**
     * Takes the Template information submitted to this REST Endpoint and saves it to dotCMS the database.
     *
     * @param templateForm The {@link TemplateForm} object with the Template's data.
     * @param user         The {@link User} that is saving this Template.
     * @param site         The {@link Host} that this Template belongs to.
     * @param pageMode     The {@link PageMode} object used to determine whether anonymous permissions must be respected
     *                     or not.
     * @param template     The {@link Template} object that will hold the incoming data.
     * @return The new Template.
     * @throws DotSecurityException The specified User does not have permission to save this Template.
     * @throws DotDataException     An error occurred when interacting with the data source.
     */
    @WrapInTransaction
    private Template fillAndSaveDraftTemplate(final TemplateForm templateForm,
            final User user,
            final Host site,
            final PageMode pageMode,
            final Template template) throws DotSecurityException, DotDataException {

        template.setInode(templateForm.getInode());
        fillTemplate(templateForm, user, site, pageMode.respectAnonPerms, template);

        this.templateAPI.saveDraftTemplate(template, site, user,pageMode.respectAnonPerms);

        ActivityLogger.logInfo(this.getClass(), "Saved Template", USER + user.getPrimaryKey()
                + "Template: " + template.getTitle(), site.getTitle() != null? site.getTitle():"default");

        return template;
    }

    private static void fillTemplate(TemplateForm templateForm, User user, Host site, boolean respectAnonPerms, Template template) throws DotSecurityException, DotDataException {
        template.setTheme(UtilMethods.isSet(templateForm.getTheme()) ? templateForm.getTheme() : Theme.SYSTEM_THEME);
        template.setBody(templateForm.getBody());
        template.setCountContainers(templateForm.getCountAddContainer());
        template.setCountAddContainer(templateForm.getCountAddContainer());
        template.setSortOrder(templateForm.getSortOrder());
        template.setTitle(templateForm.getTitle());
        template.setModUser(user.getUserId());
        template.setModDate(new Date());
        template.setFooter(templateForm.getFooter());
        template.setFriendlyName(templateForm.getFriendlyName());
        template.setHeadCode(templateForm.getHeadCode());
        template.setImage(templateForm.getImage());
        template.setSelectedimage(templateForm.getSelectedimage());
        template.setHeader(templateForm.getHeader());

        if (templateForm.isDrawed()) {
            final String themeHostId = APILocator.getFolderAPI().find(templateForm.getTheme(), user, respectAnonPerms).getHostId();
            final String themePath   = themeHostId.equals(site.getInode())?
                    Template.THEMES_PATH + template.getThemeName() + "/":
                    "//" + APILocator.getHostAPI().find(themeHostId, user, respectAnonPerms).getHostname()
                            + Template.THEMES_PATH + template.getThemeName() + "/";

            final StringBuffer endBody = DesignTemplateUtil.getBody(template.getBody(), template.getHeadCode(),
                    themePath, templateForm.isHeaderCheck(), templateForm.isFooterCheck());
            template.setBody(endBody.toString());
        }
    }

    /**
     * Saves and publish a template. The templateForm must contain the identifier of the template.
     *
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
                               final TemplateForm templateForm) throws DotDataException, DotSecurityException {

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requestAndResponse(request, response).rejectWhenNoUser(true).init();
        final User user         = initData.getUser();
        final Host host         = this.hostWebAPI.getCurrentHostNoThrow(request);
        final PageMode pageMode = PageMode.get(request);
        final Template currentTemplate = UtilMethods.isSet(templateForm.getIdentifier())?
                this.templateAPI.findWorkingTemplate(templateForm.getIdentifier(),user,pageMode.respectAnonPerms):null;
        Template newVersionTemplate = new Template();

        if (null != currentTemplate) {

            newVersionTemplate = currentTemplate;
        }

        Logger.debug(this, ()-> "Saving & publishing the template: " + templateForm.getIdentifier());

        final Template templateSaved = this.saveAndPublishTemplate(templateForm, user, host, pageMode, newVersionTemplate);

        return Response.ok(new ResponseEntityView(this.templateHelper.toTemplateView(templateSaved, user))).build();

    }

    @WrapInTransaction
    private Template saveAndPublishTemplate(final TemplateForm templateForm,
                                            final User user,
                                            final Host host,
                                            final PageMode pageMode, Template newVersionTemplate) {

        try {

            final Template templateSaved =
                    this.fillAndSaveTemplate(templateForm, user, host, pageMode, newVersionTemplate);

            Logger.debug(this, () -> "Saved the template: " + templateSaved.getIdentifier());

            this.templateAPI.publishTemplate(templateSaved, user, pageMode.respectAnonPerms);

            Logger.debug(this, () -> "Published the template: " + templateSaved.getIdentifier());

            return templateSaved;
        } catch (Exception e) {

            Logger.error(this, e.getMessage(), e);
            throw new RuntimeException(e);
        }
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
                    ActivityLogger.logInfo(this.getClass(), "Publish Template Action", USER +
                            user.getPrimaryKey() + " published template: " + template.getIdentifier());
                    publishedTemplatesCount++;
                } else {
                    Logger.error(this, "Template with Id: " + templateId + " does not exist");
                    failedToPublish.add(new FailedResultView(templateId,"Template Does not exist"));
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
                    ActivityLogger.logInfo(this.getClass(), "Unpublish Template Action", USER +
                            user.getPrimaryKey() + " unpublished template: " + template.getIdentifier());
                    unpublishedTemplatesCount++;
                } else {
                    Logger.error(this, "Template with Id: " + templateId + " does not exist");
                    failedToUnpublish.add(new FailedResultView(templateId,"Template Does not exist"));
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
     * @param templateId      {@link String} template identifier to copy
     * @return Response
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @PUT
    @Path("/{templateId}/_copy")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response copy(@Context final HttpServletRequest  request,
                               @Context final HttpServletResponse response,
                               @PathParam("templateId") final String templateId) throws DotDataException, DotSecurityException {

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requestAndResponse(request, response).rejectWhenNoUser(true).init();
        final User user         = initData.getUser();
        final PageMode pageMode = PageMode.get(request);

        Logger.debug(this, ()->"Copying the Template: " + templateId);

        final Template template = this.templateAPI.findWorkingTemplate(templateId,user,pageMode.respectAnonPerms);

        if (null == template || !InodeUtils.isSet(template.getInode())) {

            throw new DoesNotExistException("Template with Id: " + templateId + " does not exist");
        }

        return Response.ok(new ResponseEntityView(
                this.templateHelper.toTemplateView(this.templateAPI.copy(template, user), user))).build();
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
                    ActivityLogger.logInfo(this.getClass(), "Archive Template Action", USER +
                            user.getPrimaryKey() + " archived template: " + template.getIdentifier());
                    archivedTemplatesCount++;
                } else {
                    Logger.error(this, "Template with Id: " + templateId + " does not exist");
                    failedToArchive.add(new FailedResultView(templateId,"Template Does not exist"));
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
                    ActivityLogger.logInfo(this.getClass(), "Unarchive Template Action", USER +
                            user.getPrimaryKey() + " unarchived template: " + template.getIdentifier());
                    unarchivedTemplatesCount++;
                } else {
                    Logger.error(this, "Template with Id: " + templateId + " does not exist");
                    failedToUnarchive.add(new FailedResultView(templateId,"Template Does not exist"));
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
                    ActivityLogger.logInfo(this.getClass(), "Delete Template Action", USER +
                            user.getPrimaryKey() + " deleted template: " + template.getIdentifier());
                    deletedTemplatesCount++;
                } else {
                    Logger.error(this, "Template with Id: " + templateId + " does not exist");
                    failedToDelete.add(new FailedResultView(templateId,"Template Does not exist"));
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

    /**
     * Return the image contentlet of a template (if exists, otherwise return 404)
     *
     * @param httpRequest
     * @param httpResponse
     * @param templateId template identifier to get the live version.
     * @return
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @POST
    @Path("/image")
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Map<String, Object> fetchTemplateImage(@Context final HttpServletRequest  httpRequest,
                                                  @Context final HttpServletResponse httpResponse,
                                                  final TemplateImageForm templateImageForm) throws DotDataException, DotSecurityException {

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requestAndResponse(httpRequest, httpResponse).rejectWhenNoUser(true).init();
        final User user     = initData.getUser();
        final PageMode mode = PageMode.get(httpRequest);
        final String templateId = templateImageForm.getTemplateId();

        Logger.debug(this, ()-> "Getting the image working template by id: " + templateId);
        final Template template = this.templateAPI.findWorkingTemplate(templateId, user, mode.respectAnonPerms);
        if (null != template && UtilMethods.isSet(template.getIdentifier())) {

            final Identifier imageIdentifier = APILocator.getIdentifierAPI().find(template.getImage());
            if (UtilMethods.isSet(imageIdentifier.getAssetType()) && imageIdentifier.getAssetType().equals("contentlet")) {

                final Optional<Contentlet> imageContentletOpt = templateAPI.getImageContentlet(template);
                if (imageContentletOpt.isPresent()) {

                    final Contentlet imageContentlet = imageContentletOpt.get();
                    final Map<String, Object> toReturn =  new HashMap<>();
                    toReturn.put("inode", imageContentlet.getInode());
                    toReturn.put("name", imageContentlet.getTitle());
                    toReturn.put("identifier", imageContentlet.getIdentifier());
                    toReturn.put("extension", UtilMethods.getFileExtension(imageContentlet.getTitle()));
                    return toReturn;
                }
            }
        }

        throw new DoesNotExistException("Working Version of the Template with Id: " + templateId + " does not exist");
    }
}

