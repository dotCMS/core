package com.dotcms.rest;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.User;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;


@Path("/user")
public class UserResource extends WebResource {

	/**
	 * <p>Returns a JSON representation of the logged in User object
	 * <br>The user node contains: userId, firstName, lastName, roleId.
	 *
	 * Usage: /getloggedinuser
	 *
	 */

	@GET
	@Path("/getloggedinuser/{params:.*}")
	@Produces("application/json")
	public Response getLoggedInUser(@Context HttpServletRequest request, @PathParam("params") String params) throws DotDataException, DotSecurityException,
			DotRuntimeException, PortalException, SystemException {

        InitDataObject initData = init( params, true, request, true );

        //Creating an utility response object
        ResourceResponse responseResource = new ResourceResponse( initData.getParamsMap() );

		User user = WebAPILocator.getUserWebAPI().getLoggedInUser(request);

        if ( user == null ) {
            return responseResource.response( "{}" );
        }

		Role myRole  = APILocator.getRoleAPI().getUserRole(user);

		StringBuilder node = new StringBuilder();
		node.append("{");
		node.append("userId: '").append(user.getUserId()).append("',");
		node.append("firstName: '").append(user.getFirstName()).append("',");
		node.append("lastName: '").append(user.getLastName()).append("',");
		node.append("roleId: '").append(myRole.getId()).append("'");
		node.append("}");

        return responseResource.response( node.toString() );
	}

}
