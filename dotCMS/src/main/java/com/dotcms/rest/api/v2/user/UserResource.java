package com.dotcms.rest.api.v2.user;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.javax.ws.rs.*;
import com.dotcms.repackage.javax.ws.rs.core.Context;
import com.dotcms.repackage.javax.ws.rs.core.MediaType;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.org.glassfish.jersey.server.JSONP;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.api.v1.contenttype.ContentTypeHelper;
import com.dotcms.rest.exception.mapper.ExceptionMapperUtil;
import com.dotcms.util.PaginationUtil;
import com.dotcms.util.pagination.ContentTypesPaginator;
import com.dotcms.util.pagination.UserPaginator;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.SecurityLogger;
import com.liferay.portal.model.User;

import javax.servlet.http.HttpServletRequest;

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
    public final Response loginAsData(@Context final HttpServletRequest request,
                                      @QueryParam(PaginationUtil.FILTER)   final String filter,
                                      @QueryParam(PaginationUtil.PAGE) final int page,
                                      @QueryParam(PaginationUtil.PER_PAGE) final int perPage) {

        final InitDataObject initData = webResource.init(null, true, request, true, null);

        Response response = null;
        final User user = initData.getUser();

        try {
            response = this.paginationUtil.getPage( request, user, filter, false, page, perPage );
        } catch (Exception e) {

            response = ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
            Logger.error(this, e.getMessage(), e);
        }

        return response;
    }
}
