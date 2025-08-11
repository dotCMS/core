package com.dotcms.rest;

import com.dotcms.rest.annotation.SwaggerCompliant;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * 
 * @deprecated This Jersey end-point is deprecated. Please use new
 *             {@link com.dotcms.rest.api.v1.user.UserResource} end-point.
 */
@Deprecated
@SwaggerCompliant(value = "Core authentication and user management APIs", batch = 1)
@Tag(name = "Users")
@Path("/user")
public class UserResource {

    private final WebResource webResource = new WebResource();

    /**
	 * <p>Returns a JSON representation of the logged in User object
	 * <br>The user node contains: userId, firstName, lastName, roleId.
	 *
	 * Usage: /getloggedinuser
	 * @throws JSONException 
	 *
	 */
    @Operation(
        operationId = "getLoggedInUserLegacy",
        summary = "Get logged in user (deprecated)",
        description = "Returns a JSON representation of the currently logged in user including userId, emailAddress, firstName, lastName, and roleId. This endpoint is deprecated - use v1 UserResource instead.",
        deprecated = true
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "User information retrieved successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(type = "object", description = "User information containing userId, emailAddress, firstName, lastName, and roleId"))),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - backend user authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
	@GET
	@Path("/getloggedinuser/{params:.*}")
	@Produces("application/json")
	@Deprecated
	public Response getLoggedInUser(@Context HttpServletRequest request, @Context final HttpServletResponse response, 
		@Parameter(description = "URL parameters for the request", required = true) @PathParam("params") String params) throws DotDataException,
			DotRuntimeException, PortalException, SystemException, JSONException {

		final InitDataObject initData = new WebResource.InitBuilder(webResource)
				.requiredBackendUser(true)
				.requiredFrontendUser(false)
				.params(params)
				.requestAndResponse(request, response)
				.rejectWhenNoUser(true).init();

        //Creating an utility response object
        ResourceResponse responseResource = new ResourceResponse( initData.getParamsMap() );

		User user = WebAPILocator.getUserWebAPI().getLoggedInUser(request);

		//Using JSONObject instead of manually creating the json object
		JSONObject jsonLoggedUserObject = new JSONObject();
		
		if ( user == null ) {
            //return responseResource.response( "{}" );
			return responseResource.response(jsonLoggedUserObject.toString());
        }

		Role myRole  = APILocator.getRoleAPI().getUserRole(user);

		//Adding logged user information to the object
		jsonLoggedUserObject.put("userId", user.getUserId());
		jsonLoggedUserObject.put("emailAddress", user.getEmailAddress());
		jsonLoggedUserObject.put("firstName", UtilMethods.escapeSingleQuotes(user.getFirstName()));
		jsonLoggedUserObject.put("lastName", UtilMethods.escapeSingleQuotes(user.getLastName()));
		jsonLoggedUserObject.put("roleId", myRole.getId());

        return responseResource.response(jsonLoggedUserObject.toString());
	}

}
