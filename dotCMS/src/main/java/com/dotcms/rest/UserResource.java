package com.dotcms.rest;

import com.dotcms.repackage.javax.ws.rs.GET;
import com.dotcms.repackage.javax.ws.rs.Path;
import com.dotcms.repackage.javax.ws.rs.PathParam;
import com.dotcms.repackage.javax.ws.rs.Produces;
import com.dotcms.repackage.javax.ws.rs.core.Context;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.User;
import javax.servlet.http.HttpServletRequest;


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
	public Response getLoggedInUser(@Context HttpServletRequest request, @PathParam("params") String params) throws DotDataException, DotSecurityException,
			DotRuntimeException, PortalException, SystemException, JSONException {

        InitDataObject initData = webResource.init(params, true, request, true, null);

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
