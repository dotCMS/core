package com.dotcms.rest;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.UtilMethods;


@Path("/bundle")
public class BundleResource extends WebResource {

	/**
	 * <p>Returns a JSON representation of the Role with the given id, including its first level children.
	 * <br>The role node contains: id, name, locked, children.
	 * <br>- id: id of the role
	 * <br>- name: name of the role
	 * <br>- locked: boolean that indicates if the role is locked
	 * <br>- children: a list of the role's first level children
	 *
	 * <br><p>Each child node contains: id, name, locked, children.
	 * <br>- id: id of the child role
	 * <br>- name: name of the child role
	 * <br>- locked: boolean that indicates if the child role is locked
	 * <br>- children: boolean that indicates if the child role has children
	 *
	 *
	 * <br><p>If no id is given, returns the root node (not a role) and its first level children (root roles)
	 *
	 * <br><p>This is used to lazy-load the Tree (UI) of roles in the Role Manager of dotCMS Admin
	 *
	 * Usage: /loadchildren/id/{id}
	 * Example usage 1: /loadchildren/id/2adccac3-a56b-4078-be40-94e343f20712
	 * Example usage 2 (Root Roles): /loadchildren/
	 *
	 * @param request
	 * @param params a string containing the URL parameters
	 * @return
	 * @throws DotStateException
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */

	@GET
	@Path("/loadbundles/{params:.*}")
	@Produces("application/json")
	public Response loadBundles(@Context HttpServletRequest request, @PathParam("params") String params) throws DotStateException, DotDataException, DotSecurityException {
		InitDataObject initData = init(params, true, request, true);

		StringBuilder json = new StringBuilder();

		json.append("[ { id: 'root', name: 'Bundles', top: true, children: ").append("[");
//		int bundlesCounter = 0;
//		List<Bundle> bundles = bundleAPI.findBundles();
//
//
//		for(Bundle bundle : bundles) {
//
//			json.append("{id: '").append(r.getId()).append("', ");
//			json.append("$ref: '").append(r.getId()).append("', ");
//			json.append("name: '").append(r.getName()).append("', ");
//			json.append("locked: '").append(r.isLocked()).append("', ");
//			json.append(" children:true}");
//
//			if(rolesCounter+1 < rootRoles.size()) {
//				json.append(", ");
//			}
//
//			rolesCounter++;
//		}

		json.append("]").append(" } ]");


		json.append("]").append(" }");


		CacheControl cc = new CacheControl();
		cc.setNoCache(true);

		ResponseBuilder builder = Response.ok(json.toString(), "application/json");
		return builder.cacheControl(cc).build();

	}

}