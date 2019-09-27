package com.dotcms.rest.api.v1.site;

import static com.dotcms.util.CollectionsUtils.map;

import com.dotcms.exception.ExceptionUtil;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.exception.ForbiddenException;
import com.dotcms.rest.exception.mapper.ExceptionMapperUtil;
import com.dotcms.util.I18NUtil;
import com.dotcms.util.PaginationUtil;
import com.dotcms.util.pagination.SitePaginator;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import java.io.Serializable;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.commons.lang.StringUtils;
import org.glassfish.jersey.server.JSONP;

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
} // E:O:F:SiteBrowserResource.