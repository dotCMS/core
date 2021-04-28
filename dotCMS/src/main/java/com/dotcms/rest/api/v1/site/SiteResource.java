package com.dotcms.rest.api.v1.site;

import static com.dotcms.util.CollectionsUtils.map;

import com.dotcms.business.WrapInTransaction;
import com.dotcms.enterprise.HostAssetsJobProxy;
import com.dotcms.exception.ExceptionUtil;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.api.v1.temp.DotTempFile;
import com.dotcms.rest.api.v1.temp.TempFileAPI;
import com.dotcms.rest.exception.ForbiddenException;
import com.dotcms.rest.exception.mapper.ExceptionMapperUtil;
import com.dotcms.util.DotLambdas;
import com.dotcms.util.I18NUtil;
import com.dotcms.util.PaginationUtil;
import com.dotcms.util.pagination.SitePaginator;
import com.dotcms.workflow.helper.WorkflowHelper;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.business.util.HostNameComparator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.quartz.QuartzUtils;
import com.dotmarketing.quartz.SimpleScheduledTask;
import com.dotmarketing.quartz.job.HostCopyOptions;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.User;

import java.io.File;
import java.io.Serializable;
import java.text.ParseException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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

import io.vavr.control.Try;
import org.apache.commons.lang.StringUtils;
import org.glassfish.jersey.server.JSONP;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;

/**
 * This resource provides all the different end-points associated to information
 * and actions that the front-end can perform on the Site Browser page.
 *
 * @author jsanca
 */
@Path("/v1/site")
public class SiteResource implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final String NO_FILTER = "*";

    private final UserAPI userAPI;
    private final WebResource webResource;
    private final SiteHelper siteHelper;
    private final I18NUtil i18NUtil;
    private final PaginationUtil paginationUtil;

    public SiteResource() {
        this(new WebResource(),
                SiteHelper.getInstance(),
                I18NUtil.INSTANCE, APILocator.getUserAPI(),
                new PaginationUtil(new SitePaginator()));
    }

    @VisibleForTesting
    public SiteResource(final WebResource webResource,
                        final SiteHelper siteHelper,
                        final I18NUtil i18NUtil,
                        final UserAPI userAPI,
                        final PaginationUtil paginationUtil) {
        this.webResource = webResource;
        this.siteHelper  = siteHelper;
        this.i18NUtil    = i18NUtil;
        this.userAPI = userAPI;
        this.paginationUtil = paginationUtil;
    }

    /**
     * Returns the list of Sites that the currently logged-in user has access
     * to. In the front-end, this list is displayed in the Site Selector
     * component. Its contents will also be refreshed when performing the "Login
     * As".
     * <p>
     * The site that will be selected in the UI component will be retrieved from
     * the HTTP session. If such a site does not exist in the list of sites, the
     * first site in it will be selected.
     *
     * @param httpServletRequest
     *            - The {@link HttpServletRequest} object.
     * @return The {@link Response} containing the list of Sites.
     */
    @GET
    @Path ("/currentSite")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response currentSite(@Context final HttpServletRequest httpServletRequest,
                                      @Context final HttpServletResponse httpServletResponse) {
        Response response = null;


        try {
          final User user = new WebResource.InitBuilder(this.webResource)
              .requestAndResponse(httpServletRequest, httpServletResponse)
              .requiredBackendUser(true)
              .requiredFrontendUser(true)
              .init().getUser();
          
            Host currentSite = siteHelper.getCurrentSite(httpServletRequest, user);
            response = Response.ok( new ResponseEntityView(currentSite) ).build();
        } catch (Exception e) {
            if (ExceptionUtil.causedBy(e, DotSecurityException.class)) {
                throw new ForbiddenException(e);
            }
            // Unknown error, so we report it as a 500
            response = ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
        }
        return response;
    }

    /**
     * Return the list of sites paginated
     * @param httpServletRequest
     * @param httpServletResponse
     * @param filterParam
     * @param showArchived
     * @param showLive
     * @param showSystem
     * @param page
     * @param perPage
     * @return
     */
    @GET
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response sites(
            @Context final HttpServletRequest httpServletRequest,
            @Context final HttpServletResponse httpServletResponse,
            @QueryParam(PaginationUtil.FILTER)   final String filterParam,
            @QueryParam(SitePaginator.ARCHIVED_PARAMETER_NAME) final Boolean showArchived,
            @QueryParam(SitePaginator.LIVE_PARAMETER_NAME) final Boolean showLive,
            @QueryParam(SitePaginator.SYSTEM_PARAMETER_NAME) final Boolean showSystem,
            @QueryParam(PaginationUtil.PAGE) final int page,
            @QueryParam(PaginationUtil.PER_PAGE) final int perPage
    ) {

        Response response = null;
        final User user = new WebResource.InitBuilder(this.webResource)
            .requestAndResponse(httpServletRequest, httpServletResponse)
            .requiredBackendUser(true)
            .rejectWhenNoUser(true)
            .init().getUser();

        String filter = (null != filterParam && filterParam.endsWith(NO_FILTER))?
                filterParam.substring(0, filterParam.length() - 1):
                (null != filterParam)? filterParam: StringUtils.EMPTY;
        final String sanitizedFilter = !"all".equals(filter) ? filter : StringUtils.EMPTY;

        try {
            response = paginationUtil.getPage(httpServletRequest, user, sanitizedFilter, page, perPage,
                    map(SitePaginator.ARCHIVED_PARAMETER_NAME, showArchived, SitePaginator.LIVE_PARAMETER_NAME, showLive,
                            SitePaginator.SYSTEM_PARAMETER_NAME, showSystem));
        } catch (Exception e) { // this is an unknown error, so we report as a 500.
            if (ExceptionUtil.causedBy(e, DotSecurityException.class)) {
                throw new ForbiddenException(e);
            }
            response = ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
        }

        return response;
    } // sites.

    /**
     * Switch to a site
     * @param httpServletRequest
     * @param httpServletResponse
     * @param hostId
     * @return
     */
    @PUT
    @Path ("/switch/{id}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response switchSite(
            @Context final HttpServletRequest httpServletRequest,
            @Context final HttpServletResponse httpServletResponse,
            @PathParam("id")   final String hostId
    ) {

        Response response = null;
        final User user = new WebResource.InitBuilder(this.webResource)
            .requestAndResponse(httpServletRequest, httpServletResponse)
            .requiredBackendUser(true)
            .rejectWhenNoUser(true)
            .init().getUser();
        boolean switchDone = false;
        Host hostFound = null;

        try {

            if (UtilMethods.isSet(hostId)) {

                // we verified if the host id pass by parameter is one of the user's hosts
                hostFound = siteHelper.getSite( user, hostId);

                if (hostFound != null) {
                    siteHelper.switchSite(httpServletRequest, hostId);
                    switchDone = true;
                }
            }

            response = (switchDone) ?
                    Response.ok(new ResponseEntityView(map("hostSwitched",
                            switchDone))).build(): // 200
                    Response.status(Response.Status.NOT_FOUND).build();

        } catch (Exception e) { // this is an unknown error, so we report as a 500.
            if (ExceptionUtil.causedBy(e, DotSecurityException.class)) {
                throw new ForbiddenException(e);
            }
            response = ExceptionMapperUtil.createResponse(e,
                    Response.Status.INTERNAL_SERVER_ERROR);
        }

        return response;
    } // sites.

    /**
     * Swicth to the user's default site
     *
     * @param request
     * @return
     */
    @PUT
    @Path ("/switch")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response switchSite(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response
    ) {

      final User user = new WebResource.InitBuilder(this.webResource)
          .requestAndResponse(request, response)
          .requiredBackendUser(true)
          .rejectWhenNoUser(true)
          .init().getUser();

        Logger.debug(this, "Switching to default host for user: " + user.getUserId());

        try {
            final Host host = siteHelper.switchToDefaultHost(request, user);
            return Response.ok(new ResponseEntityView(host)).build();

        } catch (DotSecurityException e) {
            Logger.error(this.getClass(), "Exception on switch site exception message: " + e.getMessage(), e);
            throw new ForbiddenException(e);
        } catch (Exception e) {
            Logger.error(this.getClass(), "Exception on switch site exception message: " + e.getMessage(), e);
            return ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
        }

    }

    /**
     * Retrieve the host thumbnails
     * @param httpServletRequest
     * @param httpServletResponse
     * @return
     * @throws PortalException
     * @throws SystemException
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @GET
    @Path("/thumbnails")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response findAllHostThumbnails(@Context final HttpServletRequest httpServletRequest,
                                @Context final HttpServletResponse httpServletResponse) throws PortalException, SystemException, DotDataException, DotSecurityException {

        final User user = new WebResource.InitBuilder(this.webResource)
                .requestAndResponse(httpServletRequest, httpServletResponse)
                .requiredBackendUser(true)
                .rejectWhenNoUser(true)
                .init().getUser();

        final ContentletAPI contentletAPI = APILocator.getContentletAPI();
        final PageMode      pageMode      = PageMode.get(httpServletRequest);
        final boolean respectFrontend     = pageMode.respectAnonPerms;
        final List<Host> hosts            = this.siteHelper.findAll(user, respectFrontend);
        final HostNameComparator hostNameComparator = new HostNameComparator();

        Logger.debug(this, ()-> "Finding all host thumbnails...");

        return Response.ok(new ResponseEntityView(hosts.stream().filter(DotLambdas.not(Host::isSystemHost))
                .sorted(hostNameComparator).map(host -> this.toHostMap(user, contentletAPI, host))
                .collect(Collectors.toList()))).build();
    }

    private Map<String, Object> toHostMap(final User user,
                                          final ContentletAPI contentletAPI, final Host host) {

        final Map<String, Object> thumbInfo = new HashMap<>();
        thumbInfo.put("hostId",    host.getIdentifier());
        thumbInfo.put("hostInode", host.getInode());
        thumbInfo.put("hostName",  host.getHostname());
        final File hostThumbnail   = Try.of(()-> contentletAPI.getBinaryFile(host.getInode(), Host.HOST_THUMB_KEY, user)).getOrNull();
        final boolean hasThumbnail = hostThumbnail != null;
        thumbInfo.put("hasThumbnail", hasThumbnail);
        thumbInfo.put("tagStorage",   host.getMap().get("tagStorage"));
        return thumbInfo;
    }

    /**
     * Publish a host
     * @param httpServletRequest
     * @param httpServletResponse
     * @param hostId
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     * @throws PortalException
     * @throws SystemException
     */
    @PUT
    @Path("/publish")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response publishHost(@Context final HttpServletRequest httpServletRequest,
                            @Context final HttpServletResponse httpServletResponse,
                            final String hostId) throws DotDataException, DotSecurityException, PortalException, SystemException {

        final User user = new WebResource.InitBuilder(this.webResource)
                .requestAndResponse(httpServletRequest, httpServletResponse)
                .requiredBackendUser(true)
                .rejectWhenNoUser(true)
                .init().getUser();

        Logger.debug(this, ()-> "Publishing host: " + hostId);

        final PageMode      pageMode      = PageMode.get(httpServletRequest);
        final Host host = pageMode.respectAnonPerms? this.siteHelper.getSite(user, hostId):
                this.siteHelper.getSiteNoFrontEndRoles(user, hostId);
        this.siteHelper.publish(host, user, pageMode.respectAnonPerms);
        return Response.ok(new ResponseEntityView(host)).build();
    }

    /**
     * Unpublish a host
     * @param httpServletRequest
     * @param httpServletResponse
     * @param hostId
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     * @throws PortalException
     * @throws SystemException
     */
    @PUT
    @Path("/unpublish")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response unpublishHost(@Context final HttpServletRequest httpServletRequest,
                                @Context final HttpServletResponse httpServletResponse,
                                final String hostId) throws DotDataException, DotSecurityException, PortalException, SystemException {

        final User user = new WebResource.InitBuilder(this.webResource)
                .requestAndResponse(httpServletRequest, httpServletResponse)
                .requiredBackendUser(true)
                .rejectWhenNoUser(true)
                .init().getUser();

        Logger.debug(this, ()-> "Unpublishing host: " + hostId);

        final PageMode      pageMode      = PageMode.get(httpServletRequest);
        final Host host = pageMode.respectAnonPerms? this.siteHelper.getSite(user, hostId):
                this.siteHelper.getSiteNoFrontEndRoles(user, hostId);
        this.siteHelper.unpublish(host, user, pageMode.respectAnonPerms);
        return Response.ok(new ResponseEntityView(host)).build();
    }

    /**
     * Archive a host
     * @param httpServletRequest
     * @param httpServletResponse
     * @param hostId
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     * @throws PortalException
     * @throws SystemException
     */
    @PUT
    @Path("/archive")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response archiveHost(@Context final HttpServletRequest httpServletRequest,
                            @Context final HttpServletResponse httpServletResponse,
                            final String hostId) throws DotDataException, DotSecurityException, PortalException, SystemException {

        final User user = new WebResource.InitBuilder(this.webResource)
                .requestAndResponse(httpServletRequest, httpServletResponse)
                .requiredBackendUser(true)
                .rejectWhenNoUser(true)
                .init().getUser();

        Logger.debug(this, ()-> "Archiving host: " + hostId);

        final PageMode      pageMode      = PageMode.get(httpServletRequest);
        final Host host = pageMode.respectAnonPerms? this.siteHelper.getSite(user, hostId):
                this.siteHelper.getSiteNoFrontEndRoles(user, hostId);
        if(host.isDefault()) {

            throw new DotStateException("the default host can't be archived");
        }

        this.archive(user, pageMode, host);
        return Response.ok(new ResponseEntityView(host)).build();
    }

    @WrapInTransaction
    private Response archive(final User user, final PageMode pageMode,
                         final Host host) throws DotDataException, DotSecurityException {

        if(host.isLocked()) {

            this.siteHelper.unlock(host, user, pageMode.respectAnonPerms);
        }

        this.siteHelper.archive(host, user, pageMode.respectAnonPerms);
        return Response.ok(new ResponseEntityView(host)).build();
    }

    /**
     * Unarchive a host
     * @param httpServletRequest
     * @param httpServletResponse
     * @param hostId
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     * @throws PortalException
     * @throws SystemException
     */
    @PUT
    @Path("/unarchive")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response unarchiveHost(@Context final HttpServletRequest httpServletRequest,
                              @Context final HttpServletResponse httpServletResponse,
                              final String hostId) throws DotDataException, DotSecurityException, PortalException, SystemException {

        final User user = new WebResource.InitBuilder(this.webResource)
                .requestAndResponse(httpServletRequest, httpServletResponse)
                .requiredBackendUser(true)
                .rejectWhenNoUser(true)
                .init().getUser();

        Logger.debug(this, ()-> "unarchiving host: " + hostId);

        final PageMode      pageMode      = PageMode.get(httpServletRequest);
        final Host host = pageMode.respectAnonPerms? this.siteHelper.getSite(user, hostId):
                this.siteHelper.getSiteNoFrontEndRoles(user, hostId);

        this.siteHelper.unarchive(host, user, pageMode.respectAnonPerms);
        return Response.ok(new ResponseEntityView(host)).build();
    }

    /**
     * Creates a new host
     * @param httpServletRequest
     * @param httpServletResponse
     * @param newHostForm
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     * @throws PortalException
     * @throws SystemException
     * @throws ParseException
     * @throws SchedulerException
     * @throws ClassNotFoundException
     */
    @POST
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response createNewHost(@Context final HttpServletRequest httpServletRequest,
                             @Context final HttpServletResponse httpServletResponse,
                             final HostForm newHostForm)
            throws DotDataException, DotSecurityException, PortalException, SystemException, ParseException, SchedulerException, ClassNotFoundException {

        final User user = new WebResource.InitBuilder(this.webResource)
                .requestAndResponse(httpServletRequest, httpServletResponse)
                .requiredBackendUser(true)
                .rejectWhenNoUser(true)
                .requireLicense(true)
                .init().getUser();
        final PageMode      pageMode      = PageMode.get(httpServletRequest);
        final Host newHost = new Host();
        final TempFileAPI tempFileAPI = APILocator.getTempFileAPI();

        if (UtilMethods.isNotSet(newHostForm.getHostName())) {

            throw new IllegalArgumentException("Hostname can not be Null");
        }

        Logger.debug(this, ()->"Creating the host: " + newHostForm);
        newHost.setHostname(newHostForm.getHostName());
        if (UtilMethods.isSet(newHostForm.getHostThumbnail())) {

            final Optional<DotTempFile> dotTempFileOpt = tempFileAPI.getTempFile(httpServletRequest, newHostForm.getHostThumbnail());
            if (dotTempFileOpt.isPresent()) {
                newHost.setHostThumbnail(dotTempFileOpt.get().file);
            }
        }

        if (UtilMethods.isSet(newHostForm.getAliases())) {
            newHost.setAliases(newHostForm.getAliases());
        }

        if (UtilMethods.isSet(newHostForm.getTagStorage())) {
            newHost.setTagStorage(newHostForm.getTagStorage());
        }

        newHost.setProperty("runDashboard", newHostForm.isRunDashboard());
        if (UtilMethods.isSet(newHostForm.getKeywords())) {
            newHost.setProperty("keywords", newHostForm.getKeywords());
        }

        if (UtilMethods.isSet(newHostForm.getDescription())) {
            newHost.setProperty("description", newHostForm.getDescription());
        }

        if (UtilMethods.isSet(newHostForm.getGoogleMap())) {
            newHost.setProperty("googleMap", newHostForm.getGoogleMap());
        }

        if (UtilMethods.isSet(newHostForm.getGoogleAnalytics())) {
            newHost.setProperty("googleAnalytics", newHostForm.getGoogleAnalytics());
        }

        if (UtilMethods.isSet(newHostForm.getAddThis())) {
            newHost.setProperty("addThis", newHostForm.getAddThis());
        }

        if (UtilMethods.isSet(newHostForm.getProxyUrlForEditMode())) {
            newHost.setProperty("proxyEditModeUrl", newHostForm.getProxyUrlForEditMode());
        }

        if (UtilMethods.isSet(newHostForm.getEmbeddedDashboard())) {
            newHost.setProperty("embeddedDashboard", newHostForm.getEmbeddedDashboard());
        }

        final long languageId = 0 == newHostForm.getLanguageId()?
                APILocator.getLanguageAPI().getDefaultLanguage().getId(): newHost.getLanguageId();

        newHost.setLanguageId(languageId);

        Logger.debug(this, ()-> "Creating new Host: " + newHostForm);

        return Response.ok(new ResponseEntityView(this.siteHelper.save(newHost, user, pageMode.respectAnonPerms))).build();
    }

    @PUT
    @Path("/copy")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response copyHost(@Context final HttpServletRequest httpServletRequest,
                                  @Context final HttpServletResponse httpServletResponse,
                                  final CopyHostForm copyHostForm)
            throws DotDataException, DotSecurityException, PortalException, SystemException, ParseException, SchedulerException, ClassNotFoundException {

        final User user = new WebResource.InitBuilder(this.webResource)
                .requestAndResponse(httpServletRequest, httpServletResponse)
                .requiredBackendUser(true)
                .rejectWhenNoUser(true)
                .requireLicense(true)
                .init().getUser();

        final String hostId = copyHostForm.getCopyFromHostId();
        final PageMode      pageMode      = PageMode.get(httpServletRequest);
        final Host sourceHost = pageMode.respectAnonPerms? this.siteHelper.getSite(user, hostId):
                this.siteHelper.getSiteNoFrontEndRoles(user, hostId);
        final Response response  = this.createNewHost(httpServletRequest, httpServletResponse, copyHostForm.getHost());
        final Host newHost       = (Host)ResponseEntityView.class.cast(response.getEntity()).getEntity();

        Logger.debug(this, ()-> "copying host from: " + hostId);
        Logger.debug(this, ()-> "copying host with values: " + copyHostForm);

        final HostCopyOptions hostCopyOptions = copyHostForm.isCopyAll()?
                    new HostCopyOptions(true):
                    new HostCopyOptions(copyHostForm.isCopyTemplatesContainers(),
                            copyHostForm.isCopyFolders(), copyHostForm.isCopyLinks(),
                            copyHostForm.isCopyContentOnPages(), copyHostForm.isCopyContentOnHost(),
                            copyHostForm.isCopyHostVariables());

        final Map<String, Object> parameters = new HashMap<>();
        parameters.put("sourceHostId",      sourceHost.getIdentifier());
        parameters.put("destinationHostId", newHost.getIdentifier());
        parameters.put("copyOptions",       hostCopyOptions);

        // We make sure we schedule the copy only once even if the
        // browser for any reason sends the request twice
        if (!QuartzUtils.isJobSequentiallyScheduled("setup-host-" + newHost.getIdentifier(), "setup-host-group")) {
            Calendar startTime = Calendar.getInstance();
            SimpleScheduledTask task = new SimpleScheduledTask("setup-host-" + newHost.getIdentifier(), "setup-host-group", "Setups host "
                    + newHost.getIdentifier() + " from host " + sourceHost.getIdentifier(), HostAssetsJobProxy.class.getCanonicalName(), false,
                    "setup-host-" + newHost.getIdentifier() + "-trigger", "setup-host-trigger-group", startTime.getTime(), null,
                    SimpleTrigger.MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_REMAINING_COUNT, 5, true, parameters, 0, 0);
            QuartzUtils.scheduleTask(task);
        }

        return Response.ok(new ResponseEntityView(newHost)).build();
    }

} // E:O:F:SiteBrowserResource.
