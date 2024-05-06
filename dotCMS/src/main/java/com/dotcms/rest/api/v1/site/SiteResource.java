package com.dotcms.rest.api.v1.site;

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
import com.dotcms.util.PaginationUtil;
import com.dotcms.util.pagination.SitePaginator;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.NoSuchUserException;
import com.dotmarketing.business.util.HostNameComparator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.AlreadyExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.hostvariable.model.HostVariable;
import com.dotmarketing.quartz.QuartzUtils;
import com.dotmarketing.quartz.job.HostCopyOptions;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.PortletID;
import com.dotmarketing.util.RegEX;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.vavr.control.Try;
import org.apache.commons.lang.StringUtils;
import org.glassfish.jersey.server.JSONP;
import org.quartz.SchedulerException;

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
import java.io.File;
import java.io.Serializable;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static com.dotcms.rest.api.v1.site.SiteHelper.toView;

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
    public static final String RUN_DASHBOARD = "runDashboard";
    public static final String KEYWORDS = "keywords";
    public static final String DESCRIPTION = "description";
    public static final String GOOGLE_MAP = "googleMap";
    public static final String GOOGLE_ANALYTICS = "googleAnalytics";
    public static final String ADD_THIS = "addThis";
    public static final String PROXY_EDIT_MODE_URL = "proxyEditModeUrl";
    public static final String EMBEDDED_DASHBOARD = "embeddedDashboard";
    private static final String SITE_DOESNT_EXIST_ERR_MSG = "Site '%s' does not exist";

    private final WebResource webResource;
    private final SiteHelper siteHelper;
    private final PaginationUtil paginationUtil;

    @SuppressWarnings("unused")
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
            response = Response.ok( new ResponseEntityView<>(currentSite) ).build();
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
     * Returns the default site
     * @param httpServletRequest
     * @param httpServletResponse
     * @return
     */
    @GET
    @Path ("/defaultSite")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response defaultSite(@Context final HttpServletRequest httpServletRequest,
                                      @Context final HttpServletResponse httpServletResponse) {
        Response response = null;

        try {
            final User user = new WebResource.InitBuilder(this.webResource)
                    .requestAndResponse(httpServletRequest, httpServletResponse)
                    .requiredBackendUser(true)
                    .requiredFrontendUser(true)
                    .init().getUser();

            final Host currentSite = APILocator.getHostAPI().findDefaultHost(user, PageMode.get(httpServletRequest).respectAnonPerms);
            response = Response.ok(
                        new ResponseEntityView<>(currentSite)
                    ).build();
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

            final Map<String, Object>  extraParams = new HashMap<>();
            extraParams.put(SitePaginator.ARCHIVED_PARAMETER_NAME, showArchived);
            extraParams.put(SitePaginator.LIVE_PARAMETER_NAME, showLive);
            extraParams.put(SitePaginator.SYSTEM_PARAMETER_NAME, showSystem);

            response = paginationUtil.getPage(httpServletRequest, user, sanitizedFilter, page, perPage, extraParams);
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

            final Map<String, Object> resultMap = new HashMap<>();
            resultMap.put("hostSwitched", switchDone);

            response = (switchDone) ?
                    Response.ok(new ResponseEntityView(resultMap)).build(): // 200
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
            return Response.ok(new ResponseEntityView<>(host)).build();

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
                .init().getUser();

        final ContentletAPI contentletAPI = APILocator.getContentletAPI();
        final PageMode      pageMode      = PageMode.get(httpServletRequest);
        final boolean respectFrontend     = pageMode.respectAnonPerms;
        final List<Host> hosts            = this.siteHelper.findAll(user, respectFrontend);
        final HostNameComparator hostNameComparator = new HostNameComparator();

        Logger.debug(this, ()-> "Finding all site thumbnails...");

        return Response.ok(new ResponseEntityView<>(hosts.stream().filter(DotLambdas.not(Host::isSystemHost))
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
     * Publishes a Site.
     *
     * @param httpServletRequest  The current instance of the {@link HttpServletRequest} object.
     * @param httpServletResponse The current instance of the {@link HttpServletResponse} object.
     * @param siteId              The identifier of the Site to be published.
     *
     * @return The {@link Response} object containing the result of the operation.
     *
     * @throws DotDataException     An error occurred when publishing the Site.
     * @throws DotSecurityException The logged-in User does not have the required permissions to
     *                              perform this action.
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
                .requiredPortlet(PortletID.SITES.toString())
                .init().getUser();

        Logger.debug(this, ()-> "Publishing site: " + siteId);

        final PageMode      pageMode      = PageMode.get(httpServletRequest);
        final Host site = pageMode.respectAnonPerms? this.siteHelper.getSite(user, siteId):
                this.siteHelper.getSiteNoFrontEndRoles(user, siteId);
        if (null == site) {
            throw new IllegalArgumentException(String.format(SITE_DOESNT_EXIST_ERR_MSG, siteId));
        }
        this.siteHelper.publish(site, user, pageMode.respectAnonPerms);
        return Response.ok(new ResponseEntityView<>(toView(site, user))).build();
    }

    /**
     * Un-publishes a Site.
     *
     * @param httpServletRequest  The current instance of the {@link HttpServletRequest} object.
     * @param httpServletResponse The current instance of the {@link HttpServletResponse} object.
     * @param siteId              The identifier of the Site to be un-published.
     *
     * @return The {@link Response} object containing the result of the operation.
     *
     * @throws DotDataException     An error occurred when un-publishing the Site.
     * @throws DotSecurityException The logged-in User does not have the required permissions to
     *                              perform this action.
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
                .requiredPortlet(PortletID.SITES.toString())
                .init().getUser();

        Logger.debug(this, ()-> "Unpublishing site: " + siteId);

        final PageMode      pageMode      = PageMode.get(httpServletRequest);
        final Host site = pageMode.respectAnonPerms? this.siteHelper.getSite(user, siteId):
                this.siteHelper.getSiteNoFrontEndRoles(user, siteId);

        if (null == site) {
            throw new NotFoundException(String.format(SITE_DOESNT_EXIST_ERR_MSG, siteId));
        }

        this.siteHelper.unpublish(site, user, pageMode.respectAnonPerms);
        return Response.ok(new ResponseEntityView<>(toView(site, user))).build();
    }

    /**
     * Archives a Site.
     *
     * @param httpServletRequest  The current instance of the {@link HttpServletRequest} object.
     * @param httpServletResponse The current instance of the {@link HttpServletResponse} object.
     * @param siteId              The identifier of the Site to be archived.
     *
     * @return The {@link Response} object containing the result of the operation.
     *
     * @throws DotDataException     An error occurred when archiving the Site.
     * @throws DotSecurityException The logged-in User does not have the required permissions to
     *                              perform this action.
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
                .requiredPortlet(PortletID.SITES.toString())
                .init().getUser();

        Logger.debug(this, "Archiving site: " + siteId);

        final PageMode      pageMode      = PageMode.get(httpServletRequest);
        final Host site = pageMode.respectAnonPerms? this.siteHelper.getSite(user, siteId):
                this.siteHelper.getSiteNoFrontEndRoles(user, siteId);

        if (null == site) {
            throw new NotFoundException(String.format(SITE_DOESNT_EXIST_ERR_MSG, siteId));
        }

        if(site.isDefault()) {
            throw new DotStateException(String.format("Site '%s' is the default site. It can't be archived", site));
        }

        return this.archive(user, pageMode, site);
    }

    @WrapInTransaction
    private Response archive(final User user, final PageMode pageMode,
                         final Host site) throws DotDataException, DotSecurityException {

        if(site.isLocked()) {

            this.siteHelper.unlock(site, user, pageMode.respectAnonPerms);
        }

        this.siteHelper.archive(site, user, pageMode.respectAnonPerms);
        return Response.ok(new ResponseEntityView<>(toView(site, user))).build();
    }

    /**
     * Un-archives a Site.
     *
     * @param httpServletRequest  The current instance of the {@link HttpServletRequest} object.
     * @param httpServletResponse The current instance of the {@link HttpServletResponse} object.
     * @param siteId              The identifier of the Site to be un-archived.
     *
     * @return The {@link Response} object containing the result of the operation.
     *
     * @throws DotDataException     An error occurred when un-archiving the Site.
     * @throws DotSecurityException The logged-in User does not have the required permissions to
     *                              perform this action.
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
                .requiredPortlet(PortletID.SITES.toString())
                .init().getUser();

        Logger.debug(this, ()-> "unarchiving site: " + siteId);

        final PageMode      pageMode      = PageMode.get(httpServletRequest);
        final Host site = pageMode.respectAnonPerms? this.siteHelper.getSite(user, siteId):
                this.siteHelper.getSiteNoFrontEndRoles(user, siteId);

        if (null == site) {
            throw new IllegalArgumentException(String.format(SITE_DOESNT_EXIST_ERR_MSG, siteId));
        }

        this.siteHelper.unarchive(site, user, pageMode.respectAnonPerms);
        return Response.ok(new ResponseEntityView<>(toView(site))).build();
    }

    /**
     * Deletes a Site. It's worth noting that the Default Site cannot be deleted, so you need to
     * mark another Site as "default" before doing this.
     *
     * @param httpServletRequest  The current instance of the {@link HttpServletRequest} object.
     * @param httpServletResponse The current instance of the {@link HttpServletResponse} object.
     * @param siteId              The identifier of the Site to be deleted.
     *
     * @throws DotDataException     An error occurred when deleting the Site.
     * @throws DotSecurityException The logged-in User does not have the required permissions to
     *                              perform this action.
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
                .requiredPortlet(PortletID.SITES.toString())
                .init().getUser();

        Logger.debug(this, ()-> "deleting the site: " + siteId);

        final PageMode      pageMode      = PageMode.get(httpServletRequest);
        final boolean respectFrontendRoles = pageMode.respectAnonPerms;
        final Host site = pageMode.respectAnonPerms? this.siteHelper.getSite(user, siteId):
                this.siteHelper.getSiteNoFrontEndRoles(user, siteId);

        if (null == site) {
            throw new IllegalArgumentException(String.format(SITE_DOESNT_EXIST_ERR_MSG, siteId));
        }

        if(site.isDefault()) {
            throw new DotStateException(String.format("Site '%s' is the default site. It can't be deleted", site));
        }

        final Future<Boolean> deleteHostResult = this.siteHelper.delete(site, user, respectFrontendRoles);
        if (null == deleteHostResult) {
            throw new DotStateException(String.format("Site '%s' couldn't be deleted", siteId));
        } else {

            try {
                asyncResponse.resume(new ResponseEntityView<>(deleteHostResult.get()));
            } catch (final Exception e) {
                asyncResponse.resume(ResponseUtil.mapExceptionResponse(e));
            }
        }
    }


    /**
     * Marks a Site as "default".
     *
     * @param httpServletRequest  The current instance of the {@link HttpServletRequest} object.
     * @param httpServletResponse The current instance of the {@link HttpServletResponse} object.
     * @param siteId              The identifier of the Site to be marked as "default"..
     *
     * @return The {@link Response} object containing the result of the operation.
     *
     * @throws DotDataException     An error occurred when marking the Site as "default".
     * @throws DotSecurityException The logged-in User does not have the required permissions to
     *                              perform this action.
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
                .requiredPortlet(PortletID.SITES.toString())
                .init().getUser();

        Logger.debug(this, ()-> "making the site: " + siteId + " as a default");

        final PageMode      pageMode      = PageMode.get(httpServletRequest);
        final boolean respectFrontendRoles = pageMode.respectAnonPerms;
        final Host site = pageMode.respectAnonPerms? this.siteHelper.getSite(user, siteId):
                this.siteHelper.getSiteNoFrontEndRoles(user, siteId);

        if (null == site) {
            throw new IllegalArgumentException(String.format(SITE_DOESNT_EXIST_ERR_MSG, siteId));
        }

        return Response.ok(new ResponseEntityView<>(
                this.siteHelper.makeDefault(site, user, respectFrontendRoles))).build();
    }

    /**
     * Returns the site setup progress when the site assets are being copied in the background.
     *
     * @param httpServletRequest  The current instance of the {@link HttpServletRequest} object.
     * @param httpServletResponse The current instance of the {@link HttpServletResponse} object.
     * @param siteId              The identifier of the Site that the setup process belongs to.
     *
     * @return The {@link Response} object containing the result of the operation.
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
                .requiredPortlet(PortletID.SITES.toString())
                .init().getUser();

        Logger.debug(this, ()-> "Getting the site : " + siteId + " as a default");

        return Response.ok(new ResponseEntityView<>(
                QuartzUtils.getTaskProgress("setup-host-" + siteId, "setup-host-group"))).build();
    }

    /**
     * Retrieves a Site by its Identifier.
     *
     * @param httpServletRequest  The current instance of the {@link HttpServletRequest} object.
     * @param httpServletResponse The current instance of the {@link HttpServletResponse} object.
     * @param siteId              The identifier of the Site to be retrieved.
     *
     * @return The {@link Response} object containing the Site.
     *
     * @throws DotDataException     An error occurred when retrieving the Site.
     * @throws DotSecurityException The logged-in User does not have the required permissions to
     *                              perform this action.
     */
    @GET
    @Path("/{siteId}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response findHostByIdentifier(@Context final HttpServletRequest httpServletRequest,
                                         @Context final HttpServletResponse httpServletResponse,
                                         @PathParam("siteId")  final String siteId) throws DotDataException, DotSecurityException {

        final User user = new WebResource.InitBuilder(this.webResource)
                .requestAndResponse(httpServletRequest, httpServletResponse)
                .requiredBackendUser(true)
                .rejectWhenNoUser(true)
                .init().getUser();

        Logger.debug(this, ()-> "Finding the site: " + siteId);

        final PageMode      pageMode      = PageMode.get(httpServletRequest);
        final Host site = pageMode.respectAnonPerms? this.siteHelper.getSite(user, siteId):
                this.siteHelper.getSiteNoFrontEndRoles(user, siteId);

        if (null == site) {
            throw new NotFoundException(String.format(SITE_DOESNT_EXIST_ERR_MSG, siteId));
        }

        return Response.ok(new ResponseEntityView<>(toView(site,user))).build();
    }

    /**
     * Finds a site by its name. The site name is sent via POST in order to avoid escaped url
     * issues.
     *
     * @param httpServletRequest   The current instance of the {@link HttpServletRequest} object.
     * @param httpServletResponse  The current instance of the {@link HttpServletResponse} object.
     * @param searchSiteByNameForm The form containing the site name to be searched.
     *
     * @return The {@link Response} object containing the Site.
     *
     * @throws DotDataException     An error occurred when retrieving the Site.
     * @throws DotSecurityException The logged-in User does not have the required permissions to
     *                              perform this action.
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
                .init().getUser();

        final String hostname = searchSiteByNameForm.getSiteName();

        if (null == hostname) {
            throw new IllegalArgumentException("Sitename can not be null");
        }

        Logger.debug(this, ()-> "Finding the site by name: " + hostname);

        final PageMode      pageMode      = PageMode.get(httpServletRequest);
        final Host site = pageMode.respectAnonPerms? this.siteHelper.getSiteByName(user, hostname):
                this.siteHelper.getSiteByNameNoFrontEndRoles(user, hostname);

        if (null == site) {
            throw new NotFoundException(String.format(SITE_DOESNT_EXIST_ERR_MSG, hostname));
        }

        return Response.ok(new ResponseEntityView<>(toView(site,user))).build();
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
            throws DotDataException, DotSecurityException, AlreadyExistException, LanguageException {

        final User user = new WebResource.InitBuilder(this.webResource)
                .requestAndResponse(httpServletRequest, httpServletResponse)
                .requiredBackendUser(true)
                .rejectWhenNoUser(true)
                .requiredPortlet(PortletID.SITES.toString())
                .init().getUser();
        final PageMode      pageMode      = PageMode.get(httpServletRequest);
        final Host newSite = new Host();
        final TempFileAPI tempFileAPI = APILocator.getTempFileAPI();

        if (UtilMethods.isNotSet(newSiteForm.getSiteName())) {

            throw new IllegalArgumentException("siteName can not be Null");
        }

        Logger.debug(this, "Creating the site: " + newSiteForm);
        newSite.setHostname(newSiteForm.getSiteName());
        if (UtilMethods.isSet(newSiteForm.getSiteThumbnail())) {

            final Optional<DotTempFile> dotTempFileOpt = tempFileAPI.getTempFile(httpServletRequest, newSiteForm.getSiteThumbnail());
            dotTempFileOpt.ifPresent(dotTempFile -> newSite.setHostThumbnail(dotTempFile.file));
        }

        newSite.setIdentifier(newSiteForm.getIdentifier());
        newSite.setInode(newSiteForm.getInode());
        copySitePropertiesFromForm(newSiteForm, newSite);

        return Response.ok(new ResponseEntityView<>(
                this.siteHelper.save(
                        newSite, newSiteForm.getVariables(), user, pageMode.respectAnonPerms
                )
        )).build();
    }

    /**
     * Copy the most common properties from the REST form into the Site object.
     * <p>It's very important to note that copying properties such as the Identifier, the Inode or
     * the Site Name is NOT part of this method as they are very specific to what you may need to
     * do; i.e., creating or updating a Site. So, make sure you handle those properties
     * appropriately before or after calling this method.</p>
     *
     * @param siteForm The REST {@link SiteForm} object containing the information to be copied.
     * @param site     The application {@link Host} that will contain the properties above.
     */
    private void copySitePropertiesFromForm(final SiteForm siteForm, final Host site) {
        if (UtilMethods.isSet(siteForm.getAliases())) {
            site.setAliases(siteForm.getAliases());
        }

        if (UtilMethods.isSet(siteForm.getTagStorage())) {
            final Host tagStorageSite =
                    Try.of(() -> this.siteHelper.getSite(APILocator.systemUser(), siteForm.getTagStorage())).getOrNull();
            if (null == tagStorageSite) {
                throw new IllegalArgumentException(String.format("Tag Storage Site '%s' was not found", siteForm.getTagStorage()));
            }
            site.setTagStorage(tagStorageSite.getIdentifier());
        } else {
            site.setTagStorage(Host.SYSTEM_HOST);
        }

        site.setProperty(RUN_DASHBOARD, siteForm.isRunDashboard());
        if (UtilMethods.isSet(siteForm.getKeywords())) {
            site.setProperty(KEYWORDS, siteForm.getKeywords());
        }

        if (UtilMethods.isSet(siteForm.getDescription())) {
            site.setProperty(DESCRIPTION, siteForm.getDescription());
        }

        if (UtilMethods.isSet(siteForm.getGoogleMap())) {
            site.setProperty(GOOGLE_MAP, siteForm.getGoogleMap());
        }

        if (UtilMethods.isSet(siteForm.getGoogleAnalytics())) {
            site.setProperty(GOOGLE_ANALYTICS, siteForm.getGoogleAnalytics());
        }

        if (UtilMethods.isSet(siteForm.getAddThis())) {
            site.setProperty(ADD_THIS, siteForm.getAddThis());
        }

        if (UtilMethods.isSet(siteForm.getProxyUrlForEditMode())) {
            site.setProperty(PROXY_EDIT_MODE_URL, siteForm.getProxyUrlForEditMode());
        }

        if (UtilMethods.isSet(siteForm.getEmbeddedDashboard())) {
            site.setProperty(EMBEDDED_DASHBOARD, siteForm.getEmbeddedDashboard());
        }

        // Property needed to mark the site as default, only set it if the site is marked as default
        //  to avoid changing the existing default behavior.
        if (siteForm.isDefault()) {
            site.setDefault(siteForm.isDefault());
        }

        final long languageId = 0 == siteForm.getLanguageId()?
                APILocator.getLanguageAPI().getDefaultLanguage().getId(): site.getLanguageId();

        site.setLanguageId(languageId);
    }

    /**
     * Saves a Site Variable in the specified Site.
     *
     * @param httpServletRequest  The current instance of the {@link HttpServletRequest} object.
     * @param httpServletResponse The current instance of the {@link HttpServletResponse} object.
     * @param siteVariableForm    The form containing the Site Variable to be saved.
     *
     * @return The {@link Response} object containing the Site Variable.
     *
     * @throws DotDataException     An error occurred when saving the Site Variable.
     * @throws DotSecurityException The logged-in User does not have the required permissions to
     *                              perform this action.
     * @throws LanguageException    An error occurred when retrieving the default Language.
     */
    @PUT
    @Path("/variable")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Operation(summary = "Save a Site Variable",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseHostVariableEntityView.class))),
                    @ApiResponse(responseCode = "400", description = "When a required value is not sent")})
    public Response saveSiteVariable(@Context final HttpServletRequest httpServletRequest,
                               @Context final HttpServletResponse httpServletResponse,
                              final SiteVariableForm siteVariableForm)
            throws DotDataException, DotSecurityException, LanguageException {

        final User user = new WebResource.InitBuilder(this.webResource)
                .requestAndResponse(httpServletRequest, httpServletResponse)
                .requiredBackendUser(true)
                .rejectWhenNoUser(true)
                .requiredPortlet(PortletID.SITES.toString())
                .init().getUser();
        final PageMode pageMode = PageMode.get(httpServletRequest);

        Logger.debug(this, ()-> "Saving the site variable: " + siteVariableForm);

        verifySite(siteVariableForm);

        final String id     = siteVariableForm.getId();
        final String key    = siteVariableForm.getKey().trim();
        final String value  = UtilMethods.escapeDoubleQuotes(siteVariableForm.getValue().trim());
        final String name   = UtilMethods.escapeDoubleQuotes(siteVariableForm.getName().trim());
        final String siteId = siteVariableForm.getSiteId();

        // Getting all the existing variables for the host
        final List<HostVariable> existingVariables = APILocator.getHostVariableAPI().
                getVariablesForHost(siteId, user, pageMode.respectAnonPerms);

        HostVariable siteVariable = null;

        // Verify if the variable already exists by id
        if (UtilMethods.isSet(id)) {
            for (final HostVariable next : existingVariables) {
                if (next.getId().equals(id)) {
                    siteVariable = next;
                    break;
                }
            }
        } else {
            // Verify if the variable already exists by key
            for (final HostVariable next : existingVariables) {
                if (UtilMethods.isSet(key) && next.getKey().equalsIgnoreCase(key)) {
                    siteVariable = next;
                    break;
                }
            }
        }

        if (null == siteVariable) {
            siteVariable = new HostVariable();
            siteVariable.setId(id);
        }

        siteVariable.setHostId(siteId);
        siteVariable.setName(name);
        siteVariable.setKey(key);
        siteVariable.setValue(value);
        siteVariable.setLastModifierId(user.getUserId());
        siteVariable.setLastModDate(new Date());

        // Validate the Site Variable
        siteHelper.validateVariable(siteVariable, user);
        siteHelper.validateVariableAlreadyExist(siteVariable, existingVariables, user);

        // Saving the Site Variable
        APILocator.getHostVariableAPI().save(siteVariable, user, pageMode.respectAnonPerms);

        return Response.ok(new ResponseHostVariableEntityView(siteVariable)).build();
    }

    private void verifySite(SiteVariableForm siteVariableForm) {

        if (!UtilMethods.isSet(siteVariableForm)) {

            throw new IllegalArgumentException("Body with the Site Variable is required");
        }

        if (!UtilMethods.isSet(siteVariableForm.getSiteId())) {

            throw new IllegalArgumentException("The siteId is required");
        }

        if (!UtilMethods.isSet(siteVariableForm.getName())) {

            throw new IllegalArgumentException("The Name is required");
        }

        if (!UtilMethods.isSet(siteVariableForm.getKey())) {

            throw new IllegalArgumentException("The Key is required");
        }

        if (!UtilMethods.isSet(siteVariableForm.getValue())) {

            throw new IllegalArgumentException("The Value is required");
        }
    }

    /**
     * Returns the complete list of Site Variables associated to the specified Site ID.
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
    @GET
    @Path("/variable/{siteId}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Operation(summary = "Retrieve the Site Variables for a site",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseSiteVariablesEntityView.class))),
                    @ApiResponse(responseCode = "404", description = "When the site id does not exists")})
    public Response getSiteVariables(@Context final HttpServletRequest httpServletRequest,
                                     @Context final HttpServletResponse httpServletResponse,
                                     @PathParam("siteId")  final String siteId)
            throws DotDataException, DotSecurityException, LanguageException {

        final User loggedInUser = new WebResource.InitBuilder(this.webResource)
                .requestAndResponse(httpServletRequest, httpServletResponse)
                .requiredBackendUser(true)
                .rejectWhenNoUser(true)
                .init().getUser();
        final PageMode pageMode = PageMode.get(httpServletRequest);
        final boolean respectFrontendRoles = pageMode.respectAnonPerms;

        Logger.debug(this, () -> "Getting the site variables, for site: " + siteId);

        final Host host = this.siteHelper.getSite(loggedInUser, siteId);
        if (null == host || !InodeUtils.isSet(host.getIdentifier())) {

            throw new NotFoundException("The site id: " + siteId + " does not exists");
        }

        final List<SiteVariableView> resultList = new ArrayList<>();
        final List<HostVariable> siteVariableList = APILocator.getHostVariableAPI().getVariablesForHost(siteId, loggedInUser, respectFrontendRoles);
        for (final HostVariable variable : siteVariableList) {

            String lastModifierFullName = "Unknown";

            try {
                final User variableLastModifier = WebAPILocator.getUserWebAPI().loadUserById(variable.getLastModifierId(),
                        APILocator.systemUser(), false);
                if (null != variableLastModifier) {

                    lastModifierFullName = variableLastModifier.getFullName();
                }
            } catch (final NoSuchUserException e) {
                // The modifier user does not exist anymore. So just default its name to "Unknown"
            }

            resultList.add(new SiteVariableView(variable.getId(), variable.getHostId(), variable.getName(), variable.getKey(),
                    variable.getValue(), variable.getLastModifierId(), variable.getLastModDate(), lastModifierFullName));
        }

        return Response.ok(new ResponseSiteVariablesEntityView(resultList)).build();
    }
    /**
     * Updates an existing Site in dotCMS. In order to do this, the User calling this method must
     * have access to the {@code Sites} portlet.
     *
     * @param httpServletRequest  The current instance of the {@link HttpServletRequest}.
     * @param httpServletResponse The current instance of the {@link HttpServletResponse}.
     * @param newSiteForm         The {@link SiteForm} containing the information of the updated
     *                            Site.
     *
     * @return The {@link Response} containing the updated Site.
     *
     * @throws DotDataException      An error occurred when persisting the Site's information.
     * @throws DotSecurityException  The user calling this method does not have the required
     *                               permissions to create a Site.
     */
    @PUT
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response updateSite(@Context final HttpServletRequest httpServletRequest,
                                  @Context final HttpServletResponse httpServletResponse,
                                  @QueryParam("id") final String  siteIdentifier,
                                  final SiteForm newSiteForm)
            throws DotDataException, DotSecurityException, LanguageException {

        final User user = new WebResource.InitBuilder(this.webResource)
                .requestAndResponse(httpServletRequest, httpServletResponse)
                .requiredBackendUser(true)
                .rejectWhenNoUser(true)
                .requiredPortlet(PortletID.SITES.toString())
                .init().getUser();

        if (!UtilMethods.isSet(siteIdentifier)) {
            throw new IllegalArgumentException("The id query string parameter can not be null");
        }

        final Host originalSite = siteHelper.getSite(user, siteIdentifier);
        if (null == originalSite) {
            throw new NotFoundException(String.format(SITE_DOESNT_EXIST_ERR_MSG, siteIdentifier));
        }
        final Host site = new Host(APILocator.getContentletAPI().find(originalSite.getInode()
                , user, false));
        // we need to clean up mostly the null properties when retrieving the Site by identifier
        site.cleanup();

        final PageMode      pageMode      = PageMode.get(httpServletRequest);
        final TempFileAPI tempFileAPI = APILocator.getTempFileAPI();

        if (UtilMethods.isNotSet(newSiteForm.getSiteName())) {
            throw new IllegalArgumentException("siteName can not be Null");
        }

        Logger.debug(this, "Updating the site: " + siteIdentifier +
                ", with: " + newSiteForm);

        //Property need to update the siteName
        site.setProperty("forceExecution",newSiteForm.isForceExecution());
        site.setHostname(newSiteForm.getSiteName());

        if (UtilMethods.isSet(newSiteForm.getSiteThumbnail())) {

            final Optional<DotTempFile> dotTempFileOpt = tempFileAPI.getTempFile(httpServletRequest, newSiteForm.getSiteThumbnail());
            dotTempFileOpt.ifPresent(dotTempFile -> site.setHostThumbnail(dotTempFile.file));
        }

        copySitePropertiesFromForm(newSiteForm, site);

        return Response.ok(new ResponseEntityView<>(
                this.siteHelper.update(
                        site, newSiteForm.getVariables(), user, pageMode.respectAnonPerms
                )
        )).build();
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
                .requiredPortlet(PortletID.SITES.toString())
                .init().getUser();

        final String siteId = copySiteForm.getCopyFromSiteId();
        final PageMode      pageMode      = PageMode.get(httpServletRequest);
        final Host sourceSite = pageMode.respectAnonPerms? this.siteHelper.getSite(user, siteId):
                this.siteHelper.getSiteNoFrontEndRoles(user, siteId);

        if (null == sourceSite) {
            throw new NotFoundException(String.format(SITE_DOESNT_EXIST_ERR_MSG, siteId));
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
                            copySiteForm.isCopySiteVariables(), copySiteForm.isCopyContentTypes());

        HostAssetsJobProxy.fireJob(newSite.getIdentifier(), sourceSite.getIdentifier(), hostCopyOptions, user.getUserId());
        return Response.ok(new ResponseEntityView<>(newSite)).build();
    }

} // E:O:F:SiteBrowserResource.
