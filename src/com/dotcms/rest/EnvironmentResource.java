package com.dotcms.rest;

import com.dotcms.publisher.environment.bean.Environment;
import com.dotcms.repackage.javax.ws.rs.GET;
import com.dotcms.repackage.javax.ws.rs.Path;
import com.dotcms.repackage.javax.ws.rs.PathParam;
import com.dotcms.repackage.javax.ws.rs.Produces;
import com.dotcms.repackage.javax.ws.rs.core.CacheControl;
import com.dotcms.repackage.javax.ws.rs.core.Context;
import com.dotcms.repackage.javax.ws.rs.core.MediaType;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.org.apache.commons.lang.StringEscapeUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.Role;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.model.User;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;


@Path("/environment")
public class EnvironmentResource {

    private final WebResource webResource = new WebResource();

    /**
	 * <p>Returns a JSON representation of the environments (with servers) that the Role with the given roleid can push to
	 * <br>Each Environment node contains: id, name.
	 *
	 * Usage: /loadenvironments/{roleid}
	 * @throws JSONException
	 *
	 */

	@GET
	@Path("/loadenvironments/{params:.*}")
	@Produces("application/json")
	public Response loadEnvironments(@Context HttpServletRequest request, @PathParam("params") String params) throws DotStateException, DotDataException, DotSecurityException, LanguageException, JSONException {


        InitDataObject initData = webResource.init(params, true, request, true, null);

        //Creating an utility response object
        ResourceResponse responseResource = new ResourceResponse( initData.getParamsMap() );

		String roleId = initData.getParamsMap().get("roleid");

		//Using JsonArray instead of manually creating the json object
		JSONArray jsonEnvironments = new JSONArray();

		//First objects is expected to be blank
		JSONObject jsonEnvironmentFirst = new JSONObject();
		jsonEnvironmentFirst.put( "id", "0" );
		jsonEnvironmentFirst.put( "name", "");

		jsonEnvironments.add(jsonEnvironmentFirst);

		Role role = APILocator.getRoleAPI().loadRoleById(roleId);
		User user = APILocator.getUserAPI().loadUserById(role.getRoleKey());
		boolean isAdmin = APILocator.getUserAPI().isCMSAdmin(user);

		List<Role> roles = APILocator.getRoleAPI().loadRolesForUser(user.getUserId(),true);
		Set<Environment> environments = new HashSet<Environment>();
		if(isAdmin){
			List<Environment> app = APILocator.getEnvironmentAPI().findEnvironmentsWithServers();
			for(Environment e:app)
				environments.add(e);
		}
		else
			for(Role r: roles)
				environments.addAll(APILocator.getEnvironmentAPI().findEnvironmentsByRole(r.getId()));

		//For each env, create one json and add it to the array
		for(Environment e : environments) {

			JSONObject environmentBundle = new JSONObject();
			environmentBundle.put( "id", e.getId() );
			//Escape name for cases like: dotcms's
			environmentBundle.put( "name", StringEscapeUtils.unescapeJava( e.getName() ));

			jsonEnvironments.add(environmentBundle);
		}

		CacheControl cc = new CacheControl();
		cc.setNoCache(true);
		return Response.ok(jsonEnvironments.toString(), MediaType.APPLICATION_JSON_TYPE).cacheControl(cc).build();
	}

}
