package com.dotcms.rest.api.v1.portlet;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.rest.ResponseEntityMapStringObjectView;
import com.dotcms.rest.ResponseEntityMapView;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.ApiProvider;
import com.dotmarketing.business.Layout;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.dotcms.rest.annotation.SwaggerCompliant;
import org.glassfish.jersey.server.JSONP;

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
import java.io.Serializable;
import java.util.Map;

/**
 * This Resource allows you to manage toolgroups
 */
@SwaggerCompliant(value = "Legacy and utility APIs", batch = 8)
@Path("/v1/toolgroups")
@Tag(name = "Tool Groups")
@SuppressWarnings("serial")
public class ToolGroupResource implements Serializable {

    private final WebResource webResource;


    /**
     * Default class constructor.
     */
    public ToolGroupResource() {
        this(new WebResource(new ApiProvider()));
    }

    @VisibleForTesting
    public ToolGroupResource(WebResource webResource) {
        this.webResource = webResource;

    }


    @Operation(
        summary = "Remove toolgroup from user",
        description = "Removes a toolgroup from the specified user using the layout ID as key. If the layoutId is 'gettingStarted' it will remove the getting started layout. If no user ID is provided, the operation is performed on the current user."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Toolgroup removed from user successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityMapStringObjectView.class))),
        @ApiResponse(responseCode = "400", 
                    description = "Bad request - invalid layout ID",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", 
                    description = "Layout or user not found",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @PUT
    @Path("/{layoutId}/_removefromuser")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON})
    public final Response deleteToolGroupFromUser(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @Parameter(description = "Layout ID or 'gettingStarted' for getting started layout", required = true) @PathParam("layoutId") final String layoutId, 
            @Parameter(description = "User ID (optional, defaults to current user)") @QueryParam("userid") final String userid)
            throws DotDataException, DotSecurityException {

        User user = null;

        final User loggedInUser = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init()
                .getUser();

        if (null != userid){
            user = APILocator.getUserAPI().loadUserById(userid, loggedInUser, true);
        }

        Layout layoutToRemove = getLayout(layoutId);

        APILocator.getRoleAPI().removeLayoutFromRole(layoutToRemove,
                null == userid ? loggedInUser.getUserRole() : user.getUserRole());
        return Response.ok(new ResponseEntityView<>(Map.of("message",
                        layoutId + " removed from " + (null == userid ? loggedInUser.getUserId()
                                : userid))))
                .build();

    }


    @Operation(
        summary = "Add toolgroup to user",
        description = "Adds a toolgroup to the specified user using the layout ID as key. If the layoutId is 'gettingStarted' it will add the getting started layout. If no user ID is provided, the operation is performed on the current user."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Toolgroup added to user successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityMapStringObjectView.class))),
        @ApiResponse(responseCode = "400", 
                    description = "Bad request - invalid layout ID",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", 
                    description = "Layout or user not found",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @PUT
    @Path("/{layoutId}/_addtouser")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON})
    public final Response addToolGroupToUser(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @Parameter(description = "Layout ID or 'gettingStarted' for getting started layout", required = true) @PathParam("layoutId") final String layoutId, 
            @Parameter(description = "User ID (optional, defaults to current user)") @QueryParam("userid") final String userid)
            throws DotDataException, DotSecurityException {

        User user = null;

        final User loggedInUser = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init()
                .getUser();

        if (null != userid){
            user = APILocator.getUserAPI().loadUserById(userid, loggedInUser, true);
        }

        final Layout layoutToAdd = getLayout(layoutId);

        APILocator.getLayoutAPI()
                .addLayoutForUser(layoutToAdd, null == userid ? loggedInUser : user);
        return Response.ok(new ResponseEntityView<>(Map.of("message",
                layoutId + " added to " + (null == userid ? loggedInUser.getUserId()
                        : userid))))
                .build();
    }

    @Operation(
        summary = "Check if user has layout",
        description = "Returns a boolean indicating if the user's role has the specified layout associated. If no user ID is provided, the operation is performed on the current user."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Layout check completed successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityMapStringObjectView.class))),
        @ApiResponse(responseCode = "400", 
                    description = "Bad request - invalid layout ID",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", 
                    description = "Layout or user not found",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @GET
    @Path("/{layoutId}/_userHasLayout")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON})
    public final Response userHasLayout(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @Parameter(description = "Layout ID or 'gettingStarted' for getting started layout", required = true) @PathParam("layoutId") final String layoutId, 
            @Parameter(description = "User ID (optional, defaults to current user)") @QueryParam("userid") final String userid)
            throws DotDataException, DotSecurityException {

        User user = null;

        final User loggedInUser = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init()
                .getUser();

        if (null != userid){
            user = APILocator.getUserAPI().loadUserById(userid, loggedInUser, true);
        }

        final Layout layout = getLayout(layoutId);

        return Response.ok(new ResponseEntityView<>(Map.of("message", APILocator.getRoleAPI()
                .roleHasLayout(layout,
                        null == userid ? loggedInUser.getUserRole() : user.getUserRole()))))
                .build();
    }

    private Layout getLayout(final String layoutId) throws DotDataException {
        final Layout layoutToAdd;
        if (layoutId.equalsIgnoreCase("gettingstarted")) {
            layoutToAdd = APILocator.getLayoutAPI().findGettingStartedLayout();
        } else {
            layoutToAdd = APILocator.getLayoutAPI().findLayout(layoutId);
        }
        return layoutToAdd;
    }

}
