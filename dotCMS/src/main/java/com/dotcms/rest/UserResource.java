package com.dotcms.rest;

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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * 
 * @deprecated This Jersey end-point is deprecated. Please use new
 *             {@link com.dotcms.rest.api.v1.user.UserResource} end-point.
 */
@Deprecated
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

	@GET
	@Path("/getloggedinuser/{params:.*}")
	@Produces("application/json")
	@Deprecated
	public Response getLoggedInUser(@Context HttpServletRequest request, @Context final HttpServletResponse response, @PathParam("params") String params) throws DotDataException,
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
