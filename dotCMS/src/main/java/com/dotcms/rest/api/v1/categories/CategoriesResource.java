package com.dotcms.rest.api.v1.categories;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.javax.ws.rs.*;
import com.dotcms.repackage.javax.ws.rs.core.Context;
import com.dotcms.repackage.javax.ws.rs.core.MediaType;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.org.glassfish.jersey.server.JSONP;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.exception.mapper.ExceptionMapperUtil;
import com.dotcms.util.PaginationUtil;
import com.dotcms.util.pagination.CategoriesPaginator;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;

import javax.servlet.http.HttpServletRequest;

/**
 * This resource provides all the different end-points associated to information
 * and actions that the front-end can perform on the Categories.
 */
@Path("/v1/categories")
public class CategoriesResource {

    private final WebResource webResource;
    private final PaginationUtil paginationUtil;

    public CategoriesResource() {
        this(new WebResource(), new PaginationUtil( new CategoriesPaginator() ) );
    }

    @VisibleForTesting
    public CategoriesResource(final WebResource webresource, final PaginationUtil paginationUtil) {
        this.webResource = webresource;
        this.paginationUtil = paginationUtil;
    }

    @GET
    @JSONP
    @NoCache
    @Produces({ MediaType.APPLICATION_JSON, "application/javascript" })
    public final Response getCategories(@Context final HttpServletRequest request,
                                      @QueryParam(PaginationUtil.FILTER)   final String filter,
                                      @QueryParam(PaginationUtil.PAGE) final int page,
                                      @QueryParam(PaginationUtil.PER_PAGE) final int perPage) {

        final InitDataObject initData = webResource.init(null, true, request, true, null);

        Response response = null;
        final User user = initData.getUser();

        try {
            response = this.paginationUtil.getPage( request, user, filter, page, perPage );
        } catch (Exception e) {

            response = ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
            Logger.error(this, e.getMessage(), e);
        }

        return response;
    }
}
