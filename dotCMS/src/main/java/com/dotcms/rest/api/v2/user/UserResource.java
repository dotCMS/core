package com.dotcms.rest.api.v2.user;

import com.dotcms.exception.ExceptionUtil;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.exception.ForbiddenException;
import com.dotcms.rest.exception.mapper.ExceptionMapperUtil;
import com.dotcms.util.PaginationUtil;
import com.dotcms.util.pagination.UserPaginator;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.server.JSONP;

/**
 * This end-point provides access to information associated to dotCMS users.
 */
@Path("/v2/users")
public class UserResource {

    private final WebResource webResource;
    private final PaginationUtil paginationUtil;

    public UserResource() {
        this( new WebResource(), new PaginationUtil( new UserPaginator() ) );
    }

    @VisibleForTesting
    public UserResource(final WebResource webresource, PaginationUtil paginationUtil) {

        this.webResource = webresource;
        this.paginationUtil = paginationUtil;
    }

    /**
     * Returns all the users (without the anonymous and default users) that can
     * be impersonated.
     *
     * @return The list of users that can be impersonated.
     */
    @GET
    @Path("/loginAsData")
    @JSONP
    @NoCache
    @Produces({ MediaType.APPLICATION_JSON, "application/javascript" })
    public final Response loginAsData(@Context final HttpServletRequest httpServletRequest,
                                      @Context final HttpServletResponse httpServletResponse,
                                      @QueryParam(PaginationUtil.FILTER)   final String filter,
                                      @QueryParam(PaginationUtil.PAGE) final int page,
                                      @QueryParam(PaginationUtil.PER_PAGE) final int perPage) {

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(httpServletRequest, httpServletResponse)
                .rejectWhenNoUser(true).init();

        Response response = null;
        final User user = initData.getUser();

        try {
            response = this.paginationUtil.getPage( httpServletRequest, user, filter, page, perPage );
        } catch (Exception e) {
            if (ExceptionUtil.causedBy(e, DotSecurityException.class)) {
                throw new ForbiddenException(e);
            }
            response = ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
            Logger.error(this, e.getMessage(), e);
        }

        return response;
    }
}
