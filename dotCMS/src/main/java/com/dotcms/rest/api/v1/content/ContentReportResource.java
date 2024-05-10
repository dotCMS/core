package com.dotcms.rest.api.v1.content;

import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.api.v1.site.ResponseSiteVariablesEntityView;
import com.dotcms.util.PaginationUtil;
import com.dotcms.util.pagination.ContentReportPaginator;
import com.dotcms.util.pagination.FolderContentReportPaginator;
import com.dotcms.util.pagination.OrderDirection;
import com.dotcms.util.pagination.SiteContentReportPaginator;
import com.dotmarketing.util.UUIDUtil;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang3.math.NumberUtils;
import org.glassfish.jersey.server.JSONP;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

import static com.dotcms.util.DotPreconditions.checkNotEmpty;

/**
 * This REST Endpoint provides a way to generate a report of the different Content Types and the
 * number of contents for each of them that live under or are associated to a given dotCMS object --
 * i.e., a Site, a Folder, etc.
 * <p>For instance, this report can be used by data deletion routines where Users can identify how
 * many contents and what Content Types will be erased, should they decide to continue with it. This
 * will help them make an informed decision and obtain more information on what is actually being
 * deleted.</p>
 *
 * @author Jose Castro
 * @since Mar 7th, 2024
 */
@Path("/v1/contentreport")
@Tag(name = "Content Report")
public class ContentReportResource {

    private final WebResource webResource;

    @SuppressWarnings("unused")
    public ContentReportResource() {
        this(new WebResource());
    }

    @VisibleForTesting
    public ContentReportResource(final WebResource webResource) {
        this.webResource = webResource;
    }

    /**
     * Generates the Content Report for a Site based on the specified filtering criteria. You can
     * generate the report like this:
     * <pre>{@code
     * GET http://localhost:8080/api/v1/contentreport/site/<SITE-ID-OR-KEY>?per_page=40&&direction=DESC
     * }</pre>
     * <p>If you want results composed of 10 items per page and you want the third page, and you
     * don't have the Site's Identifier, you can call this URL:</p>
     * <pre>{@code
     * GET http://localhost:8080/api/v1/contenttype?sites=demo.dotcms.com&page=3&per_page=10
     * }</pre>
     *
     * @param httpServletRequest  The current instance of the {@link HttpServletRequest} object.
     * @param httpServletResponse The current instance of the {@link HttpServletResponse} object.
     * @param site                The ID/Key of the Site to generate the report for.
     * @param page                The page number to retrieve, for pagination purposes.
     * @param perPage             The number of items to retrieve per page, for pagination
     *                            purposes.
     * @param orderBy             The field to order the results by.
     * @param direction           The direction to order the results by: ASC or DESC.
     *
     * @return The {@link ContentReportView} for the specified Site.
     */
    @GET
    @Path("/site/{site}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Operation(summary = "Generates a report of the different Content Types living under a Site, " +
            "and the number of content items for each type",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation =
                                            ResponseSiteVariablesEntityView.class)),
                            description = "Content Report for the specified Site ID/Key, or an " +
                                    "empty list if either the Site doesn't exist, or no content " +
                                    "is found.")
            })
    public Response getSiteContentReport(@Context final HttpServletRequest httpServletRequest,
                                         @Context final HttpServletResponse httpServletResponse,
                                         @PathParam(ContentReportPaginator.SITE_PARAM) final String site,
                                         @QueryParam(PaginationUtil.PAGE) final int page,
                                         @QueryParam(PaginationUtil.PER_PAGE) final int perPage,
                                         @DefaultValue("upper(name)") @QueryParam(PaginationUtil.ORDER_BY) final String orderBy,
                                         @DefaultValue("ASC") @QueryParam(PaginationUtil.DIRECTION) final String direction) {
        final User user = new WebResource.InitBuilder(this.webResource)
                .requestAndResponse(httpServletRequest, httpServletResponse)
                .requiredBackendUser(true)
                .rejectWhenNoUser(true)
                .init().getUser();
        checkNotEmpty(site, IllegalArgumentException.class, "'site' parameter cannot be empty");
        final Map<String, Object> extraParams = new HashMap<>();
        extraParams.put(ContentReportPaginator.SITE_PARAM, site);
        final PaginationUtil paginationUtil =
                new PaginationUtil(new SiteContentReportPaginator(user));
        return paginationUtil.getPage(httpServletRequest, user, null, page, perPage, orderBy,
                OrderDirection.valueOf(direction), extraParams);
    }

    /**
     * Generates the Content Report for a Folder based on the specified filtering criteria. You can
     * generate the report like this:
     * <pre>{@code
     * GET http://localhost:8080/api/v1/contentreport/folder/<FOLDER-ID>?per_page=40&&direction=DESC
     * }</pre>
     * <p>If you want to pass down the Folder's path, you must pass down the Site ID as well, like
     * this:</p>
     * <pre>{@code
     * GET http://localhost:8080/api/v1/contentreport/folder/store/outdoor?site=demo.dotcms.com
     * }</pre>
     *
     * @param httpServletRequest  The current instance of the {@link HttpServletRequest} object.
     * @param httpServletResponse The current instance of the {@link HttpServletResponse} object.
     * @param folder              The ID of the Folder to generate the report for, or its path.
     * @param site                The ID/Key of the Site that the Folder lives in. It's only
     *                            required if you pass down a Folder path.
     * @param page                The page number to retrieve, for pagination purposes.
     * @param perPage             The number of items to retrieve per page, for pagination
     *                            purposes.
     * @param orderBy             The field to order the results by.
     * @param direction           The direction to order the results by: ASC or DESC.
     *
     * @return The {@link ContentReportView} for the specified Folder.
     */
    @GET
    @Path("/folder/{folder: .*}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Operation(summary = "Generates a report of the different Content Types living under a " +
            "Folder, and the number of content items for each type",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ContentReportView.class)),
                            description = "Content Report for the specified Folder ID/path, or " +
                                    "an empty list if either the Folder doesn't exist, or no " +
                                    "content is found.")
            })
    public Response getFolderContentReport(@Context final HttpServletRequest httpServletRequest,
                                           @Context final HttpServletResponse httpServletResponse,
                                           @PathParam(ContentReportPaginator.FOLDER_PARAM) final String folder,
                                           @QueryParam(ContentReportPaginator.SITE_PARAM) final String site,
                                           @QueryParam(PaginationUtil.PAGE) final int page,
                                           @QueryParam(PaginationUtil.PER_PAGE) final int perPage,
                                           @DefaultValue("upper(name)") @QueryParam(PaginationUtil.ORDER_BY) final String orderBy,
                                           @DefaultValue("ASC") @QueryParam(PaginationUtil.DIRECTION) final String direction) {
        final User user = new WebResource.InitBuilder(this.webResource)
                .requestAndResponse(httpServletRequest, httpServletResponse)
                .requiredBackendUser(true)
                .rejectWhenNoUser(true)
                .init().getUser();
        checkNotEmpty(folder, IllegalArgumentException.class, "'folder' parameter cannot be empty");
        final Map<String, Object> extraParams = new HashMap<>();
        extraParams.put(ContentReportPaginator.FOLDER_PARAM, folder);
        if (!UUIDUtil.isUUID(folder) && !NumberUtils.isParsable(folder) && UtilMethods.isNotSet(site)) {
            checkNotEmpty(site, IllegalArgumentException.class, "'site' parameter cannot be empty when a Folder path is used");
        }
        if (UtilMethods.isSet(site)) {
            extraParams.put(ContentReportPaginator.SITE_PARAM, site);
        }
        final PaginationUtil paginationUtil =
                new PaginationUtil(new FolderContentReportPaginator(user));
        return paginationUtil.getPage(httpServletRequest, user, null, page, perPage, orderBy,
                OrderDirection.valueOf(direction), extraParams);
    }

}
