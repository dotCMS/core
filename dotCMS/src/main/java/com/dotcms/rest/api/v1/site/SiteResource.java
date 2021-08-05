package com.dotcms.rest.api.v1.site;

import static com.dotcms.util.CollectionsUtils.map;

import com.dotcms.business.WrapInTransaction;
import com.dotcms.enterprise.HostAssetsJobProxy;
import com.dotcms.exception.ExceptionUtil;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.api.v1.authentication.ResponseUtil;
import com.dotcms.rest.api.v1.temp.DotTempFile;
import com.dotcms.rest.api.v1.temp.TempFileAPI;
import com.dotcms.rest.exception.ForbiddenException;
import com.dotcms.rest.exception.NotFoundException;
import com.dotcms.rest.exception.mapper.ExceptionMapperUtil;
import com.dotcms.util.DotLambdas;
import com.dotcms.util.I18NUtil;
import com.dotcms.util.PaginationUtil;
import com.dotcms.util.pagination.SitePaginator;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.business.util.HostNameComparator;
import com.dotmarketing.exception.AlreadyExistException;
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
import com.liferay.util.StringPool;
import io.vavr.control.Try;
import java.io.File;
import java.io.Serializable;
import java.text.ParseException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.commons.lang.StringUtils;
import org.glassfish.jersey.server.JSONP;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;

/**
 * This resource provides all the different end-points associated to information
 * and actions that the front-end can perform on the Sites page.
 *
 * @author jsanca
 */
@Path("/v1/site")
public class SiteResource implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final String NO_FILTER = "*";

    private final WebResource webResource;
    private final SiteHelper siteHelper;
    private final PaginationUtil paginationUtil;

    public SiteResource() {
        this(new WebResource(),
                SiteHelper.getInstance(),
                new PaginationUtil(new SitePaginator()));
    }

    @VisibleForTesting
    public SiteResource(final WebResource webResource,
                        final SiteHelper siteHelper,
                        final PaginationUtil paginationUtil) {
        this.webResource = webResource;
        this.siteHelper  = siteHelper;
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
            .requiredPortlet("sites")
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
          .requiredPortlet("sites")
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
    public Response findAllSiteThumbnails(@Context final HttpServletRequest httpServletRequest,
                                @Context final HttpServletResponse httpServletResponse) throws DotDataException, DotSecurityException {

        final User user = new WebResource.InitBuilder(this.webResource)
                .requestAndResponse(httpServletRequest, httpServletResponse)
                .requiredBackendUser(true)
                .rejectWhenNoUser(true)
                .requiredPortlet("sites")
                .init().getUser();

        final ContentletAPI contentletAPI = APILocator.getContentletAPI();
        final PageMode      pageMode      = PageMode.get(httpServletRequest);
        final boolean respectFrontend     = pageMode.respectAnonPerms;
        final List<Host> hosts            = this.siteHelper.findAll(user, respectFrontend);
        final HostNameComparator hostNameComparator = new HostNameComparator();

        Logger.debug(this, ()-> "Finding all site thumbnails...");

        return Response.ok(new ResponseEntityView(hosts.stream().filter(DotLambdas.not(Host::isSystemHost))
                .sorted(hostNameComparator).map(host -> this.toSiteMap(user, contentletAPI, host))
                .collect(Collectors.toList()))).build();
    }

    private Map<String, Object> toSiteMap(final User user,
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
     * Publish a Site
     * @param httpServletRequest
     * @param httpServletResponse
     * @param siteId
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     * @throws PortalException
     * @throws SystemException
     */
    @PUT
    @Path("/{siteId}/_publish")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response publishSite(@Context final HttpServletRequest httpServletRequest,
                                @Context final HttpServletResponse httpServletResponse,
                                @PathParam("siteId") final String siteId) throws DotDataException, DotSecurityException {

        final User user = new WebResource.InitBuilder(this.webResource)
                .requestAndResponse(httpServletRequest, httpServletResponse)
                .requiredBackendUser(true)
                .rejectWhenNoUser(true)
                .requiredPortlet("sites")
                .init().getUser();

        Logger.debug(this, ()-> "Publishing site: " + siteId);

        final PageMode      pageMode      = PageMode.get(httpServletRequest);
        final Host host = pageMode.respectAnonPerms? this.siteHelper.getSite(user, siteId):
                this.siteHelper.getSiteNoFrontEndRoles(user, siteId);
        if (null == host) {
            throw new IllegalArgumentException("Site: " + siteId + " does not exists");
        }
        this.siteHelper.publish(host, user, pageMode.respectAnonPerms);
        return Response.ok(new ResponseEntityView(this.toView(host))).build();
    }

    /**
     * Unpublish a site
     * @param httpServletRequest
     * @param httpServletResponse
     * @param siteId
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     * @throws PortalException
     * @throws SystemException
     */
    @PUT
    @Path("/{siteId}/_unpublish")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response unpublishSite(@Context final HttpServletRequest httpServletRequest,
                                  @Context final HttpServletResponse httpServletResponse,
                                  @PathParam("siteId") final String siteId) throws DotDataException, DotSecurityException {

        final User user = new WebResource.InitBuilder(this.webResource)
                .requestAndResponse(httpServletRequest, httpServletResponse)
                .requiredBackendUser(true)
                .rejectWhenNoUser(true)
                .requiredPortlet("sites")
                .init().getUser();

        Logger.debug(this, ()-> "Unpublishing site: " + siteId);

        final PageMode      pageMode      = PageMode.get(httpServletRequest);
        final Host site = pageMode.respectAnonPerms? this.siteHelper.getSite(user, siteId):
                this.siteHelper.getSiteNoFrontEndRoles(user, siteId);

        if (null == site) {
            throw new NotFoundException("Site: " + siteId + " does not exists");
        }

        this.siteHelper.unpublish(site, user, pageMode.respectAnonPerms);
        return Response.ok(new ResponseEntityView(this.toView(site))).build();
    }

    /**
     * Archive a site
     * @param httpServletRequest
     * @param httpServletResponse
     * @param siteId
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     * @throws PortalException
     * @throws SystemException
     */
    @PUT
    @Path("/{siteId}/_archive")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response archiveSite(@Context final HttpServletRequest httpServletRequest,
                                @Context final HttpServletResponse httpServletResponse,
                                @PathParam("siteId")  final String siteId) throws DotDataException, DotSecurityException{

        final User user = new WebResource.InitBuilder(this.webResource)
                .requestAndResponse(httpServletRequest, httpServletResponse)
                .requiredBackendUser(true)
                .rejectWhenNoUser(true)
                .requiredPortlet("sites")
                .init().getUser();

        Logger.debug(this, ()-> "Archiving site: " + siteId);

        final PageMode      pageMode      = PageMode.get(httpServletRequest);
        final Host site = pageMode.respectAnonPerms? this.siteHelper.getSite(user, siteId):
                this.siteHelper.getSiteNoFrontEndRoles(user, siteId);

        if (null == site) {
            throw new NotFoundException("Site: " + siteId + " does not exists");
        }

        if(site.isDefault()) {

            throw new DotStateException("the default site can't be archived");
        }

        this.archive(user, pageMode, site);
        return Response.ok(new ResponseEntityView(this.toView(site))).build();
    }

    @WrapInTransaction
    private Response archive(final User user, final PageMode pageMode,
                         final Host site) throws DotDataException, DotSecurityException {

        if(site.isLocked()) {

            this.siteHelper.unlock(site, user, pageMode.respectAnonPerms);
        }

        this.siteHelper.archive(site, user, pageMode.respectAnonPerms);
        return Response.ok(new ResponseEntityView(this.toView(site))).build();
    }

    /**
     * Unarchive a site
     * @param httpServletRequest
     * @param httpServletResponse
     * @param siteId
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     * @throws PortalException
     * @throws SystemException
     */
    @PUT
    @Path("/{siteId}/_unarchive")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response unarchiveSite(@Context final HttpServletRequest httpServletRequest,
                                  @Context final HttpServletResponse httpServletResponse,
                                  @PathParam("siteId")  final String siteId) throws DotDataException, DotSecurityException {

        final User user = new WebResource.InitBuilder(this.webResource)
                .requestAndResponse(httpServletRequest, httpServletResponse)
                .requiredBackendUser(true)
                .rejectWhenNoUser(true)
                .requiredPortlet("sites")
                .init().getUser();

        Logger.debug(this, ()-> "unarchiving site: " + siteId);

        final PageMode      pageMode      = PageMode.get(httpServletRequest);
        final Host host = pageMode.respectAnonPerms? this.siteHelper.getSite(user, siteId):
                this.siteHelper.getSiteNoFrontEndRoles(user, siteId);

        if (null == host) {
            throw new IllegalArgumentException("Site: " + siteId + " does not exists");
        }

        this.siteHelper.unarchive(host, user, pageMode.respectAnonPerms);
        return Response.ok(new ResponseEntityView(this.toView(host))).build();
    }

    /**
     * Delete a site
     * Default site can not be deleted
     * @param httpServletRequest
     * @param httpServletResponse
     * @param siteId
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     * @throws PortalException
     * @throws SystemException
     */
    @DELETE
    @Path("/{siteId}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public void deleteSite(@Context final HttpServletRequest httpServletRequest,
                                @Context final HttpServletResponse httpServletResponse,
                                @Suspended final AsyncResponse asyncResponse,
                                @PathParam("siteId")  final String siteId) throws DotDataException, DotSecurityException {

        final User user = new WebResource.InitBuilder(this.webResource)
                .requestAndResponse(httpServletRequest, httpServletResponse)
                .requiredBackendUser(true)
                .rejectWhenNoUser(true)
                .requiredPortlet("sites")
                .init().getUser();

        Logger.debug(this, ()-> "deleting the site: " + siteId);

        final PageMode      pageMode      = PageMode.get(httpServletRequest);
        final boolean respectFrontendRoles = pageMode.respectAnonPerms;
        final Host host = pageMode.respectAnonPerms? this.siteHelper.getSite(user, siteId):
                this.siteHelper.getSiteNoFrontEndRoles(user, siteId);

        if (null == host) {
            throw new IllegalArgumentException("Site: " + siteId + " does not exists");
        }

        if(host.isDefault()) {

            throw new DotStateException("the default site can't be deleted");
        }

        final Future<Boolean> deleteHostResult = this.siteHelper.delete(host, user, respectFrontendRoles);
        if (null == deleteHostResult) {

            throw new DotStateException("the Site: " + siteId + " couldn't be deleted");
        } else {

            try {
                asyncResponse.resume(new ResponseEntityView(deleteHostResult.get()));
            } catch (Exception e) {
                asyncResponse.resume(ResponseUtil.mapExceptionResponse(e));
            }
        }
    }


    /**
     * Make a site as a default
     * @param httpServletRequest
     * @param httpServletResponse
     * @param siteId
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     * @throws PortalException
     * @throws SystemException
     */
    @PUT
    @Path("/{siteId}/_makedefault")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response makeDefault(@Context final HttpServletRequest httpServletRequest,
                           @Context final HttpServletResponse httpServletResponse,
                           @PathParam("siteId")  final String siteId) throws DotDataException, DotSecurityException {

        final User user = new WebResource.InitBuilder(this.webResource)
                .requestAndResponse(httpServletRequest, httpServletResponse)
                .requiredBackendUser(true)
                .rejectWhenNoUser(true)
                .requiredPortlet("sites")
                .init().getUser();

        Logger.debug(this, ()-> "making the site: " + siteId + " as a default");

        final PageMode      pageMode      = PageMode.get(httpServletRequest);
        final boolean respectFrontendRoles = pageMode.respectAnonPerms;
        final Host host = pageMode.respectAnonPerms? this.siteHelper.getSite(user, siteId):
                this.siteHelper.getSiteNoFrontEndRoles(user, siteId);

        if (null == host) {
            throw new IllegalArgumentException("Site: " + siteId + " does not exists");
        }

        return Response.ok(new ResponseEntityView(
                this.siteHelper.makeDefault(host, user, respectFrontendRoles))).build();
    }

    /**
     * Get the site setup progress when the site assets are being copied on background
     * @param httpServletRequest
     * @param httpServletResponse
     * @param siteId
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     * @throws PortalException
     * @throws SystemException
     */
    @GET
    @Path("/{siteId}/setup_progress")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response getSiteSetupProgress(@Context final HttpServletRequest httpServletRequest,
                                @Context final HttpServletResponse httpServletResponse,
                                @PathParam("siteId")  final String siteId){

        new WebResource.InitBuilder(this.webResource)
                .requestAndResponse(httpServletRequest, httpServletResponse)
                .requiredBackendUser(true)
                .rejectWhenNoUser(true)
                .requiredPortlet("sites")
                .init().getUser();

        Logger.debug(this, ()-> "Getting the site : " + siteId + " as a default");

        return Response.ok(new ResponseEntityView(
                QuartzUtils.getTaskProgress("setup-host-" + siteId, "setup-host-group"))).build();
    }

    /**
     * Finds the site by identifier
     * @param httpServletRequest
     * @param httpServletResponse
     * @param siteId
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     * @throws PortalException
     * @throws SystemException
     */
    @GET
    @Path("/{siteId}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response findHostByIdentifier(@Context final HttpServletRequest httpServletRequest,
                                         @Context final HttpServletResponse httpServletResponse,
                                         @PathParam("siteId")  final String siteId) throws DotDataException, DotSecurityException, PortalException, SystemException {

        final User user = new WebResource.InitBuilder(this.webResource)
                .requestAndResponse(httpServletRequest, httpServletResponse)
                .requiredBackendUser(true)
                .rejectWhenNoUser(true)
                .requiredPortlet("sites")
                .init().getUser();

        Logger.debug(this, ()-> "Finding the site: " + siteId);

        final PageMode      pageMode      = PageMode.get(httpServletRequest);
        final Host site = pageMode.respectAnonPerms? this.siteHelper.getSite(user, siteId):
                this.siteHelper.getSiteNoFrontEndRoles(user, siteId);

        if (null == site) {
            throw new NotFoundException("Site: " + siteId + " does not exists");
        }

        return Response.ok(new ResponseEntityView(this.toView(site))).build();
    }

    /**
     * Finds a site by name
     * The site name is sent by post to avoid escape url issues.
     * @param httpServletRequest
     * @param httpServletResponse
     * @param searchSiteByNameForm
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     * @throws PortalException
     * @throws SystemException
     */
    @POST
    @Path("/_byname")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response findHostByName(@Context final HttpServletRequest httpServletRequest,
                                 @Context final HttpServletResponse httpServletResponse,
                                 final SearchSiteByNameForm searchSiteByNameForm) throws DotDataException, DotSecurityException {

        final User user = new WebResource.InitBuilder(this.webResource)
                .requestAndResponse(httpServletRequest, httpServletResponse)
                .requiredBackendUser(true)
                .rejectWhenNoUser(true)
                .requiredPortlet("sites")
                .init().getUser();

        final String hostname = searchSiteByNameForm.getSiteName();

        if (null == hostname) {
            throw new IllegalArgumentException("Sitename can not be null");
        }

        Logger.debug(this, ()-> "Finding the site by name: " + hostname);

        final PageMode      pageMode      = PageMode.get(httpServletRequest);
        final Host host = pageMode.respectAnonPerms? this.siteHelper.getSiteByName(user, hostname):
                this.siteHelper.getSiteByNameNoFrontEndRoles(user, hostname);

        if (null == host) {
            throw new NotFoundException("Site: " + hostname + " does not exists");
        }

        return Response.ok(new ResponseEntityView(this.toView(host))).build();
    }

    /**
     * Creates a new site
     * @param httpServletRequest
     * @param httpServletResponse
     * @param newSiteForm
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
    public Response createNewSite(@Context final HttpServletRequest httpServletRequest,
                                  @Context final HttpServletResponse httpServletResponse,
                                  final SiteForm newSiteForm)
            throws DotDataException, DotSecurityException, AlreadyExistException {

        final User user = new WebResource.InitBuilder(this.webResource)
                .requestAndResponse(httpServletRequest, httpServletResponse)
                .requiredBackendUser(true)
                .rejectWhenNoUser(true)
                .requiredPortlet("sites")
                .init().getUser();
        final PageMode      pageMode      = PageMode.get(httpServletRequest);
        final Host newSite = new Host();
        final TempFileAPI tempFileAPI = APILocator.getTempFileAPI();

        if (UtilMethods.isNotSet(newSiteForm.getSiteName())) {

            throw new IllegalArgumentException("siteName can not be Null");
        }

        Logger.debug(this, ()->"Creating the site: " + newSiteForm);
        newSite.setHostname(newSiteForm.getSiteName());
        if (UtilMethods.isSet(newSiteForm.getSiteThumbnail())) {

            final Optional<DotTempFile> dotTempFileOpt = tempFileAPI.getTempFile(httpServletRequest, newSiteForm.getSiteThumbnail());
            if (dotTempFileOpt.isPresent()) {
                newSite.setHostThumbnail(dotTempFileOpt.get().file);
            }
        }

        newSite.setIdentifier(newSiteForm.getIdentifier());
        newSite.setInode(newSiteForm.getInode());

        if (UtilMethods.isSet(newSiteForm.getAliases())) {
            newSite.setAliases(newSiteForm.getAliases());
        }

        if (UtilMethods.isSet(newSiteForm.getTagStorage())) {
            newSite.setTagStorage(newSiteForm.getTagStorage());
        }

        newSite.setProperty("runDashboard", newSiteForm.isRunDashboard());
        if (UtilMethods.isSet(newSiteForm.getKeywords())) {
            newSite.setProperty("keywords", newSiteForm.getKeywords());
        }

        if (UtilMethods.isSet(newSiteForm.getDescription())) {
            newSite.setProperty("description", newSiteForm.getDescription());
        }

        if (UtilMethods.isSet(newSiteForm.getGoogleMap())) {
            newSite.setProperty("googleMap", newSiteForm.getGoogleMap());
        }

        if (UtilMethods.isSet(newSiteForm.getGoogleAnalytics())) {
            newSite.setProperty("googleAnalytics", newSiteForm.getGoogleAnalytics());
        }

        if (UtilMethods.isSet(newSiteForm.getAddThis())) {
            newSite.setProperty("addThis", newSiteForm.getAddThis());
        }

        if (UtilMethods.isSet(newSiteForm.getProxyUrlForEditMode())) {
            newSite.setProperty("proxyEditModeUrl", newSiteForm.getProxyUrlForEditMode());
        }

        if (UtilMethods.isSet(newSiteForm.getEmbeddedDashboard())) {
            newSite.setProperty("embeddedDashboard", newSiteForm.getEmbeddedDashboard());
        }

        final long languageId = 0 == newSiteForm.getLanguageId()?
                APILocator.getLanguageAPI().getDefaultLanguage().getId(): newSite.getLanguageId();

        newSite.setLanguageId(languageId);

        Logger.debug(this, ()-> "Creating new Host: " + newSiteForm);

        return Response.ok(new ResponseEntityView(
                this.toView(this.siteHelper.save(newSite, user, pageMode.respectAnonPerms)))).build();
    }

    /**
     * Updates a site
     * @param httpServletRequest
     * @param httpServletResponse
     * @param newSiteForm
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     * @throws PortalException
     * @throws SystemException
     * @throws ParseException
     * @throws SchedulerException
     * @throws ClassNotFoundException
     */
    @PUT
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response updateSite(@Context final HttpServletRequest httpServletRequest,
                                  @Context final HttpServletResponse httpServletResponse,
                                  @QueryParam("id") final String  siteIdentifier,
                                  final SiteForm newSiteForm)
            throws DotDataException, DotSecurityException {

        final User user = new WebResource.InitBuilder(this.webResource)
                .requestAndResponse(httpServletRequest, httpServletResponse)
                .requiredBackendUser(true)
                .rejectWhenNoUser(true)
                .requiredPortlet("sites")
                .init().getUser();

        if (!UtilMethods.isSet(siteIdentifier)) {

            throw new IllegalArgumentException("The id query string parameter can not be null");
        }

        final Host site = siteHelper.getSite(user, siteIdentifier);

        if (null == site) {

            throw new NotFoundException("Site: " + siteIdentifier + " does not exists");
        }

        // we need to clean up mostly the null properties when recovery the by identifier
        site.cleanup();

        final PageMode      pageMode      = PageMode.get(httpServletRequest);
        final TempFileAPI tempFileAPI = APILocator.getTempFileAPI();

        if (UtilMethods.isNotSet(newSiteForm.getSiteName())) {

            throw new IllegalArgumentException("siteName can not be Null");
        }

        Logger.debug(this, ()->"Updating the site: " + siteIdentifier +
                ", with: " + newSiteForm);
        site.setHostname(newSiteForm.getSiteName());
        if (UtilMethods.isSet(newSiteForm.getSiteThumbnail())) {

            final Optional<DotTempFile> dotTempFileOpt = tempFileAPI.getTempFile(httpServletRequest, newSiteForm.getSiteThumbnail());
            if (dotTempFileOpt.isPresent()) {
                site.setHostThumbnail(dotTempFileOpt.get().file);
            }
        }


        if (UtilMethods.isSet(newSiteForm.getAliases())) {
            site.setAliases(newSiteForm.getAliases());
        }

        if (UtilMethods.isSet(newSiteForm.getTagStorage())) {
            site.setTagStorage(newSiteForm.getTagStorage());
        }

        site.setProperty("runDashboard", newSiteForm.isRunDashboard());
        if (UtilMethods.isSet(newSiteForm.getKeywords())) {
            site.setProperty("keywords", newSiteForm.getKeywords());
        }

        if (UtilMethods.isSet(newSiteForm.getDescription())) {
            site.setProperty("description", newSiteForm.getDescription());
        }

        if (UtilMethods.isSet(newSiteForm.getGoogleMap())) {
            site.setProperty("googleMap", newSiteForm.getGoogleMap());
        }

        if (UtilMethods.isSet(newSiteForm.getGoogleAnalytics())) {
            site.setProperty("googleAnalytics", newSiteForm.getGoogleAnalytics());
        }

        if (UtilMethods.isSet(newSiteForm.getAddThis())) {
            site.setProperty("addThis", newSiteForm.getAddThis());
        }

        if (UtilMethods.isSet(newSiteForm.getProxyUrlForEditMode())) {
            site.setProperty("proxyEditModeUrl", newSiteForm.getProxyUrlForEditMode());
        }

        if (UtilMethods.isSet(newSiteForm.getEmbeddedDashboard())) {
            site.setProperty("embeddedDashboard", newSiteForm.getEmbeddedDashboard());
        }

        final long languageId = 0 == newSiteForm.getLanguageId()?
                APILocator.getLanguageAPI().getDefaultLanguage().getId(): site.getLanguageId();

        site.setLanguageId(languageId);

        Logger.debug(this, ()-> "Creating new Host: " + newSiteForm);

        return Response.ok(new ResponseEntityView(
                this.toView(this.siteHelper.update(site, user, pageMode.respectAnonPerms)))).build();
    }

    /**
     * Copy a site
     * - Creates a new site,
     * - Copies the assets based on the copy options
     * @param httpServletRequest
     * @param httpServletResponse
     * @param copySiteForm
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     * @throws PortalException
     * @throws SystemException
     * @throws ParseException
     * @throws SchedulerException
     * @throws ClassNotFoundException
     */
    @PUT
    @Path("/_copy")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response copySite(@Context final HttpServletRequest httpServletRequest,
                                  @Context final HttpServletResponse httpServletResponse,
                                  final CopySiteForm copySiteForm)
            throws DotDataException, DotSecurityException, PortalException, SystemException, ParseException, SchedulerException, ClassNotFoundException, AlreadyExistException {

        final User user = new WebResource.InitBuilder(this.webResource)
                .requestAndResponse(httpServletRequest, httpServletResponse)
                .requiredBackendUser(true)
                .rejectWhenNoUser(true)
                .requireLicense(true)
                .requiredPortlet("sites")
                .init().getUser();

        final String siteId = copySiteForm.getCopyFromSiteId();
        final PageMode      pageMode      = PageMode.get(httpServletRequest);
        final Host sourceHost = pageMode.respectAnonPerms? this.siteHelper.getSite(user, siteId):
                this.siteHelper.getSiteNoFrontEndRoles(user, siteId);

        if (null == sourceHost) {
            throw new NotFoundException("Site: " + siteId + " does not exists");
        }

        final Response response  = this.createNewSite(httpServletRequest, httpServletResponse, copySiteForm.getSite());
        final SiteView newSite   = (SiteView)ResponseEntityView.class.cast(response.getEntity()).getEntity();

        Logger.debug(this, ()-> "copying site from: " + siteId);
        Logger.debug(this, ()-> "copying site with values: " + copySiteForm);

        final HostCopyOptions hostCopyOptions = copySiteForm.isCopyAll()?
                    new HostCopyOptions(true):
                    new HostCopyOptions(copySiteForm.isCopyTemplatesContainers(),
                            copySiteForm.isCopyFolders(), copySiteForm.isCopyLinks(),
                            copySiteForm.isCopyContentOnPages(), copySiteForm.isCopyContentOnSite(),
                            copySiteForm.isCopySiteVariables());

        final Map<String, Object> parameters = new HashMap<>();
        parameters.put("sourceHostId",      sourceHost.getIdentifier());
        parameters.put("destinationHostId", newSite.getIdentifier());
        parameters.put("copyOptions",       hostCopyOptions);

        // We make sure we schedule the copy only once even if the
        // browser for any reason sends the request twice
        if (!QuartzUtils.isJobSequentiallyScheduled("setup-host-" + newSite.getIdentifier(), "setup-host-group")) {
            Calendar startTime = Calendar.getInstance();
            SimpleScheduledTask task = new SimpleScheduledTask("setup-host-" + newSite.getIdentifier(), "setup-host-group", "Setups host "
                    + newSite.getIdentifier() + " from host " + sourceHost.getIdentifier(), HostAssetsJobProxy.class.getCanonicalName(), false,
                    "setup-host-" + newSite.getIdentifier() + "-trigger", "setup-host-trigger-group", startTime.getTime(), null,
                    SimpleTrigger.MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_REMAINING_COUNT, 5, true, parameters, 0, 0);
            QuartzUtils.scheduleTask(task);
        }

        return Response.ok(new ResponseEntityView(newSite)).build();
    }

    private SiteView toView (final Host host) throws DotStateException, DotDataException, DotSecurityException {

        return new SiteView(host.getIdentifier(), host.getInode(), host.getAliases(), host.getHostname(), host.getTagStorage(),
                null != host.getHostThumbnail()? host.getHostThumbnail().getName(): StringPool.BLANK,
                host.getBoolProperty("runDashboard"), host.getStringProperty("keywords"), host.getStringProperty("description"),
                host.getStringProperty("googleMap"), host.getStringProperty("googleAnalytics"), host.getStringProperty("addThis"),
                host.getStringProperty("proxyEditModeUrl"), host.getStringProperty("embeddedDashboard"), host.getLanguageId(),
                host.isSystemHost(), host.isDefault(), host.isArchived(), host.isLive(), host.isLocked(), host.isWorking(), host.getModDate(), host.getModUser()
        );
    }
} // E:O:F:SiteBrowserResource.
