package com.dotcms.rest.api.v1.container;

import static com.dotcms.util.CollectionsUtils.map;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.javax.ws.rs.Consumes;
import com.dotcms.repackage.javax.ws.rs.DefaultValue;
import com.dotcms.repackage.javax.ws.rs.GET;
import com.dotcms.repackage.javax.ws.rs.Path;
import com.dotcms.repackage.javax.ws.rs.Produces;
import com.dotcms.repackage.javax.ws.rs.QueryParam;
import com.dotcms.repackage.javax.ws.rs.core.Context;
import com.dotcms.repackage.javax.ws.rs.core.MediaType;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.org.glassfish.jersey.server.JSONP;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.exception.mapper.ExceptionMapperUtil;
import com.dotcms.util.PaginationUtil;
import com.dotcms.util.pagination.ContainerPaginator;
import com.dotcms.util.pagination.OrderDirection;

import com.dotmarketing.util.Logger;

import java.io.Serializable;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.liferay.portal.model.User;

/**
 * This resource provides all the different end-points associated to information
 * and actions that the front-end can perform on the {@link com.dotmarketing.portlets.containers.model.Container}.
 *
 */
@Path("/v1/containers")
public class ContainerResource implements Serializable {

    private static final long serialVersionUID = 1L;

    private final PaginationUtil paginationUtil;
    private final WebResource webResource;

    public ContainerResource() {
        this(new WebResource(),
                new PaginationUtil(new ContainerPaginator()));
    }

    @VisibleForTesting
    public ContainerResource(final WebResource webResource,
                             final PaginationUtil paginationUtil) {
        this.webResource = webResource;
        this.paginationUtil = paginationUtil;
    }

    /**
     * Return a list of {@link com.dotmarketing.portlets.containers.model.Container}, entity response syntax:.
     *
     * <code>
     *  {
     *      contentTypes: array of Container
     *      total: total number of Containers
     *  }
     * <code/>
     *
     * Url sintax: api/v1/container?filter=filter-string&page=page-number&per_page=per-page&ordeby=order-field-name&direction=order-direction&host=host-id
     *
     * where:
     *
     * <ul>
     *     <li>filter-string: just return Container who content this pattern into its title</li>
     *     <li>page: page to return</li>
     *     <li>per_page: limit of items to return</li>
     *     <li>ordeby: field to order by</li>
     *     <li>direction: asc for upward order and desc for downward order</li>
     *     <li>host: filter by host's id</li>
     * </ul>
     *
     * Url example: v1/container?filter=test&page=2&orderby=title
     *
     * @param request
     * @return
     */
    @GET
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response getContainers(@Context final HttpServletRequest request,
                                          @QueryParam(PaginationUtil.FILTER)   final String filter,
                                          @QueryParam(PaginationUtil.PAGE) final int page,
                                          @QueryParam(PaginationUtil.PER_PAGE) final int perPage,
                                          @DefaultValue("title") @QueryParam(PaginationUtil.ORDER_BY) final String orderBy,
                                          @DefaultValue("ASC") @QueryParam(PaginationUtil.DIRECTION) final String direction,
                                          @QueryParam(ContainerPaginator.HOST_PARAMETER_ID) final String hostId) {

        final InitDataObject initData = webResource.init(null, true, request, true, null);
        final User user = initData.getUser();

        try {
            final Map<String, Object> extraParams = map(ContainerPaginator.HOST_PARAMETER_ID, hostId);
            return this.paginationUtil.getPage(request, user, filter, page, perPage, orderBy,
                    OrderDirection.valueOf(direction), extraParams);
        } catch (Exception e) {
            Logger.error(this, e.getMessage(), e);
            return ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);

        }
    }
}