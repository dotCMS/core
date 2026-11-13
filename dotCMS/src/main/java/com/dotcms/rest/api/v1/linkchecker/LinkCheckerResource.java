package com.dotcms.rest.api.v1.linkchecker;

import com.dotcms.rest.ResponseEntityStringView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.annotation.SwaggerCompliant;
import com.dotcms.rest.exception.BadRequestException;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.ApiProvider;
import com.dotmarketing.business.LayoutAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.PortletID;
import com.dotmarketing.util.SecurityLogger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.Serializable;

/**
 * REST resource for the Link Checker tool: lists the broken links found in the contentlets of a
 * content type and queues an on-demand run of the link-check job.
 * <p>
 * Both operations require an authenticated back-end user with access to the {@code link-checker}
 * tool (CMS Administrators included). A request with no user or from a front-end user answers
 * 401; a back-end user without the tool answers 403 and the refusal is recorded in the security log.
 * The list only returns the rows whose contentlet the caller can read.
 */
@Path("/v1/link-checker")
@Tag(name = "Administration")
@SwaggerCompliant(value = "Link Checker broken links and on-demand run", batch = 1)
public class LinkCheckerResource implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final int DEFAULT_OFFSET = 0;
    private static final int DEFAULT_LIMIT = 25;

    private final WebResource webResource;
    private final LayoutAPI layoutApi;
    private final LinkCheckerHelper helper;

    /**
     * Default constructor used by the JAX-RS runtime.
     */
    public LinkCheckerResource() {
        this(new WebResource(new ApiProvider()), APILocator.getLayoutAPI(), new LinkCheckerHelper());
    }

    /**
     * Constructor with explicit collaborators, for tests.
     *
     * @param webResource authenticates the caller
     * @param layoutApi   checks access to the Link Checker tool
     * @param helper      reads broken links and queues the link-check job
     */
    @VisibleForTesting
    LinkCheckerResource(final WebResource webResource, final LayoutAPI layoutApi,
                        final LinkCheckerHelper helper) {
        this.webResource = webResource;
        this.layoutApi = layoutApi;
        this.helper = helper;
    }

    /**
     * Lists, one page at a time, the broken links found in the contentlets of a content type that
     * the caller can read.
     *
     * @param request     the current request
     * @param response    the current response
     * @param contentType id of the content type whose broken links are listed; an id that matches
     *                    no content type answers an empty list
     * @param offset      zero-based index of the first row; defaults to 0
     * @param limit       page size, 1 to 100; defaults to 25
     * @return the page of rows plus the total of rows the caller can read
     * @throws DotDataException     if access to the tool cannot be checked
     * @throws DotSecurityException if the caller has no access to the Link Checker tool
     */
    @Operation(
            operationId = "listBrokenLinks",
            summary = "List broken links of a content type",
            description = "Returns one page of the broken links the link-check job found in the contentlets of the "
                    + "given content type. Only the rows whose contentlet the caller can read are returned, and "
                    + "total counts those same rows. Paging uses offset (0 or greater) and limit (1 to 100). The "
                    + "last editor appears by full name only. An unknown content type, or one with no broken "
                    + "links, answers an empty list with total 0. Requires an authenticated back-end user with "
                    + "access to the link-checker tool; CMS Administrators included."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Broken links retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ResponseEntityBrokenLinksView.class))),
            @ApiResponse(responseCode = "400",
                    description = "Bad request - blank content type, offset not a whole number of 0 or more, or "
                            + "limit not a whole number between 1 and 100",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "401",
                    description = "Unauthorized - authentication required, or the caller is not a back-end user",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "403",
                    description = "Forbidden - the caller has no access to the link-checker tool",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500",
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @GET
    @Path("/broken-links")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Response list(@Context final HttpServletRequest request,
                         @Context final HttpServletResponse response,
                         @Parameter(description = "Id of the content type whose broken links are listed; an id "
                                 + "that matches no content type answers an empty list",
                                 required = true, example = "2a3e91e4-fbbf-4876-8c5b-2233c1739b05")
                         @QueryParam("contentType") final String contentType,
                         @Parameter(description = "Zero-based index of the first row to return",
                                 example = "0", schema = @Schema(type = "integer", defaultValue = "0", minimum = "0"))
                         @QueryParam("offset") final String offset,
                         @Parameter(description = "Maximum number of rows to return, 1 to 100",
                                 example = "25", schema = @Schema(type = "integer", defaultValue = "25",
                                 minimum = "1", maximum = "100"))
                         @QueryParam("limit") final String limit) throws DotDataException, DotSecurityException {

        final User user = initTool(request, response);
        final int offsetValue = parseInt(offset, DEFAULT_OFFSET, "offset");
        final int limitValue = parseInt(limit, DEFAULT_LIMIT, "limit");
        return Response.ok(new ResponseEntityBrokenLinksView(
                helper.findBrokenLinks(contentType, offsetValue, limitValue, user))).build();
    }

    /**
     * Queues one run of the link-check job, which refreshes the broken links of every contentlet.
     *
     * @param request  the current request
     * @param response the current response
     * @return {@code "queued"} once the run is queued
     * @throws DotDataException     if access to the tool cannot be checked
     * @throws DotSecurityException if the caller has no access to the Link Checker tool
     */
    @Operation(
            operationId = "runLinkCheck",
            summary = "Run the link check now",
            description = "Queues one run of the link-check job, which refreshes the broken links stored for the "
                    + "contentlets. Nothing is queued when the caller is refused. Requires an authenticated back-end "
                    + "user with access to the link-checker tool; CMS Administrators included."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Link check queued",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ResponseEntityStringView.class))),
            @ApiResponse(responseCode = "401",
                    description = "Unauthorized - authentication required, or the caller is not a back-end user",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "403",
                    description = "Forbidden - the caller has no access to the link-checker tool",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500",
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @POST
    @Path("/_run")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Response run(@Context final HttpServletRequest request,
                        @Context final HttpServletResponse response) throws DotDataException, DotSecurityException {

        initTool(request, response);
        helper.runCheckNow();
        return Response.ok(new ResponseEntityStringView("queued")).build();
    }

    /**
     * Authenticates a back-end user and checks that they have access to the Link Checker tool.
     *
     * @param request  the current request
     * @param response the current response
     * @return the authenticated user
     * @throws DotDataException     if access to the tool cannot be checked
     * @throws DotSecurityException if the user has no access to the Link Checker tool
     */
    private User initTool(final HttpServletRequest request, final HttpServletResponse response)
            throws DotDataException, DotSecurityException {

        final User user = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .rejectWhenNoUser(true)
                .requestAndResponse(request, response)
                .init().getUser();

        if (!layoutApi.doesUserHaveAccessToPortlet(PortletID.LINK_CHECKER.toString(), user)) {
            SecurityLogger.logInfo(LinkCheckerResource.class, "unauthorized access to the link checker by user "
                    + user.getUserId() + " from " + request.getRemoteHost());
            throw new DotSecurityException("User is not authorized to use the link checker");
        }
        return user;
    }

    /**
     * Parses an optional whole-number query parameter.
     *
     * @param value    the raw parameter value, possibly null or blank
     * @param fallback the value used when the parameter is absent
     * @param name     the parameter name, used in the error message
     * @return the parsed value or the fallback
     * @throws BadRequestException if the value is not a whole number
     */
    private static int parseInt(final String value, final int fallback, final String name) {
        if (!UtilMethods.isSet(value) || value.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (final NumberFormatException e) {
            throw new BadRequestException("The " + name + " parameter must be a whole number");
        }
    }
}
