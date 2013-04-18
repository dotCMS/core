package com.dotcms.rest;

import static org.junit.Assert.assertTrue;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.junit.Before;
import org.junit.Test;

import com.dotcms.TestBase;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.servlets.test.ServletTestRunner;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;

public class RoleResourceTest extends TestBase  {

	private Client client;
	private WebResource webResource;
	private HttpServletRequest request;
	private String serverName;
	private Integer serverPort;

	@Before
	public void init() {
		client = Client.create();
		request = ServletTestRunner.localRequest.get();
		serverName = request.getServerName();
		serverPort = request.getServerPort();
		webResource = client.resource("http://"+serverName+":"+serverPort+"/api/role");
	}

	@Test
	public void testWellFormedJSONloadChildren() {

		// loadchildren - root roles
		String response = webResource.path("/loadchildren/user/admin@dotcms.com/password/admin").get(String.class);
		assertTrue(isValidJSONArray(response));

		// loadchildren - role with children
		Role roleWithChildren = null;
		List<Role> rootRoles = null;
		try {
			rootRoles = APILocator.getRoleAPI().findRootRoles();
		} catch (DotDataException e) {
			Logger.warn(this.getClass(), "Could not validate well-formed JSON in api/role/loadchildren. Error loading root roles", e);
		}

		if(rootRoles!=null) {
			for (Role role : rootRoles) {
				if(role.getRoleChildren()!=null && !role.getRoleChildren().isEmpty()) {
					roleWithChildren = role;
					break;
				}
			}
			response = webResource.path("/loadchildren/user/admin@dotcms.com/password/admin/id/"+roleWithChildren.getId()).get(String.class);
			assertTrue(isValidJSONObject(response));
		}

	}

	@Test
	public void testWellFormedJSONloadById() {
		Role intranet = null;

		try {
			intranet = APILocator.getRoleAPI().findRoleByName("Intranet", null);
		} catch (DotDataException e) {
			Logger.warn(this.getClass(), "Could not validate well-formed JSON in api/role/loadbyid. Error loading role", e);
		}

		String response = webResource.path("/loadbyid/user/admin@dotcms.com/password/admin/id/"+intranet.getId()).get(String.class);
		assertTrue(isValidJSONObject(response));
	}

	@Test
	public void testWellFormedJSONloadByName() {
		String response = webResource.path("/loadbyname/user/admin@dotcms.com/password/admin/name/admin").get(String.class);
		assertTrue(isValidJSONObject(response));
	}

	private boolean isValidJSONArray(String json) {
		try {
			new JSONArray(json);
		} catch (JSONException e) {
			Logger.error(this.getClass(), "Not Valid JSON Array");
			return false;
		}

		return true;
	}

	private boolean isValidJSONObject(String json) {
		try {
			new JSONObject(json);
		} catch (JSONException e) {
			Logger.error(this.getClass(), "Not Valid JSON Array");
			return false;
		}

		return true;
	}

}
