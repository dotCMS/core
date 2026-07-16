package com.dotcms.rest;

import com.dotcms.rest.annotation.SwaggerCompliant;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import com.dotcms.rest.exception.ForbiddenException;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


@SwaggerCompliant(value = "Core authentication and user management APIs", batch = 1)
@Tag(name = "Roles")
@Path("/role")
public class RoleResource {

    private final WebResource webResource = new WebResource();

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
	 * @throws DotDataException
	 * @throws JSONException
	 */

	@Operation(
		operationId = "loadRoleChildrenLegacy",
		summary = "Load role children (deprecated)",
		description = "Returns role hierarchy with first-level children for lazy-loading role tree in admin UI. If no ID provided, returns root roles. This endpoint is deprecated.",
		deprecated = true
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", 
					description = "Role children loaded successfully",
					content = @Content(mediaType = "application/json",
									  schema = @Schema(type = "object", description = "Role hierarchy tree with child roles containing id, name, locked, and children properties"))),
		@ApiResponse(responseCode = "401", 
					description = "Unauthorized - backend user authentication required",
					content = @Content(mediaType = "application/json")),
		@ApiResponse(responseCode = "403", 
					description = "Forbidden - insufficient permissions",
					content = @Content(mediaType = "application/json")),
		@ApiResponse(responseCode = "500", 
					description = "Internal server error",
					content = @Content(mediaType = "application/json"))
	})
	@GET
	@Path("/loadchildren/{params:.*}")
	@Produces("application/json")
	public Response loadChildren(@Context HttpServletRequest request, @Context final HttpServletResponse response, 
		@Parameter(description = "URL parameters including role ID (id=roleId or empty for root roles)", required = true) @PathParam("params") String params)
			throws DotDataException, JSONException {

		final InitDataObject initData = new WebResource.InitBuilder(webResource)
				.requiredBackendUser(true)
				.requiredFrontendUser(false)
				.params(params)
				.requestAndResponse(request, response)
				.rejectWhenNoUser(true)
				.init();

        //Creating an utility response object
        ResourceResponse responseResource = new ResourceResponse( initData.getParamsMap() );

		Map<String, String> paramsMap = initData.getParamsMap();
		String roleId = paramsMap.get("id");

		try {
			RoleAPI roleAPI = APILocator.getRoleAPI();
			final List<Role> workflowRoles = roleAPI.findWorkflowSpecialRoles();
			CacheControl cc = new CacheControl();
			cc.setNoCache(true);

			if (!UtilMethods.isSet(roleId) || roleId.equals("root")) {  // Loads Root Roles
				JSONArray jsonRoles = new JSONArray();
				JSONObject jsonRoleObject = new JSONObject();
				jsonRoleObject.put("id", "root");
				jsonRoleObject.put("name", "Roles");
				jsonRoleObject.put("top", "true");

				List<Role> rootRoles = roleAPI.findRootRoles();
				JSONArray jsonChildren = new JSONArray();

				for (Role r : rootRoles) {
					if (!workflowRoles.contains(r)) {
						JSONObject jsonRoleChildObject = new JSONObject();
						jsonRoleChildObject.put("id", r.getId());
						jsonRoleChildObject.put("$ref", r.getId());
						jsonRoleChildObject.put("name", r.getName());
						jsonRoleChildObject.put("locked", r.isLocked());
						jsonRoleChildObject.put("children", true);

						jsonChildren.add(jsonRoleChildObject);
					}
				}
				//In order to add a JsonArray to a JsonObject
				//we need to specify that is an object (API bug)
				jsonRoleObject.put("children", (Object) jsonChildren);
				jsonRoles.add(jsonRoleObject);

				return responseResource.response(jsonRoles.toString(), cc);

			} else {  // Loads Children Roles of given Role ID
				Role role = roleAPI.loadRoleById(roleId);

				JSONObject jsonRoleObject = new JSONObject();
				jsonRoleObject.put("id", role.getId());
				jsonRoleObject.put("name", role.getName());
				jsonRoleObject.put("locked", role.isLocked());

				JSONArray jsonChildren = new JSONArray();

				List<String> children = role.getRoleChildren();
				if (children != null) {
					for (String childId : children) {
						Role r = roleAPI.loadRoleById(childId);
						if (!workflowRoles.contains(r)) {
							JSONObject jsonRoleChildObject = new JSONObject();
							jsonRoleChildObject.put("id", r.getId());
							jsonRoleChildObject.put("$ref", r.getId());
							jsonRoleChildObject.put("name", r.getName());
							jsonRoleChildObject.put("locked", r.isLocked());
							jsonRoleChildObject.put("children", true);

							jsonChildren.add(jsonRoleChildObject);
						}
					}
				}
				//In order to add a JsonArray to a JsonObject
				//we need to specify that is an object (API bug)
				jsonRoleObject.put("children", (Object) jsonChildren);

				return responseResource.response(jsonRoleObject.toString(), cc);
			}
		} catch (DotSecurityException e) {
			throw new ForbiddenException(e);
		}
    }

	/**
	 * <p>Returns a JSON representation of the Role with the given id.
	 * <br>The resulting role node contains the following fields:
	 * <br>DBFQN, FQN, description, editLayouts, editPermissions, editUsers,
	 * id, locked, name, parent, roleKey, system. See {@link Role}.
	 *
	 * <p>This is used to load all the info of a role when clicked on the Tree (UI) in the Role Manager
	 * of dotCMS Admin
	 *
	 * <p>Usage: /api/role/loadbyid/id/{id}
	 * <br>Example usage: /api/role/loadbyid/id/2adccac3-a56b-4078-be40-94e343f20712
	 *
	 * @param request
	 * @param params a string containing the URL parameters
	 * @return
	 * @throws DotDataException
	 * @throws JSONException
	 */

	@Operation(
		operationId = "loadRoleByIdLegacy",
		summary = "Load role by ID (deprecated)",
		description = "Returns detailed role information including all role properties. Used for loading complete role details in admin UI. This endpoint is deprecated.",
		deprecated = true
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", 
					description = "Role loaded successfully",
					content = @Content(mediaType = "application/json",
									  schema = @Schema(type = "object", description = "Role details including DBFQN, FQN, description, permissions, id, name, and other role properties"))),
		@ApiResponse(responseCode = "401", 
					description = "Unauthorized - backend user authentication required",
					content = @Content(mediaType = "application/json")),
		@ApiResponse(responseCode = "500", 
					description = "Internal server error",
					content = @Content(mediaType = "application/json"))
	})
	@GET
	@Path("/loadbyid/{params:.*}")
	@Produces("application/json")
	public Response loadById(@Context HttpServletRequest request, @Context final HttpServletResponse response, 
		@Parameter(description = "URL parameters including role ID (id=roleId)", required = true) @PathParam("params") String params) throws DotDataException, JSONException {

		final InitDataObject initData = new WebResource.InitBuilder(webResource)
				.requiredBackendUser(true)
				.requiredFrontendUser(false)
				.params(params)
				.requestAndResponse(request, response)
				.rejectWhenNoUser(true)
				.init();

        //Creating an utility response object
        ResourceResponse responseResource = new ResourceResponse( initData.getParamsMap() );

		Map<String, String> paramsMap = initData.getParamsMap();
		String roleId = paramsMap.get("id");

		if(!UtilMethods.isSet(roleId) || roleId.equalsIgnoreCase("root")) {
			JSONObject jsonRoleObject = new JSONObject();
			jsonRoleObject.put("id", 0);
			jsonRoleObject.put("name", "Root Role");

            return responseResource.response(jsonRoleObject.toString());
		}

		RoleAPI roleAPI = APILocator.getRoleAPI();
		Role role = roleAPI.loadRoleById(roleId);

		JSONObject jsonRoleObject = new JSONObject();
		jsonRoleObject.put("DBFQN", UtilMethods.javaScriptify(role.getDBFQN()));
		jsonRoleObject.put("FQN", UtilMethods.javaScriptify(role.getFQN()));
		jsonRoleObject.put("children", (Object)new JSONArray());
		jsonRoleObject.put("description", role.getDescription());
		jsonRoleObject.put("editLayouts", role.isEditLayouts());
		jsonRoleObject.put("editPermissions", role.isEditPermissions());
		jsonRoleObject.put("editUsers", role.isEditUsers());
		jsonRoleObject.put("id", role.getId());
		jsonRoleObject.put("locked", role.isLocked());
		jsonRoleObject.put("name", role.getName());
		jsonRoleObject.put("parent", role.getParent());
		jsonRoleObject.put("roleKey", role.getRoleKey()!=null?role.getRoleKey():"");
		jsonRoleObject.put("system", role.isSystem());

        return responseResource.response(jsonRoleObject.toString());
    }

	/**
	 * Returns a JSON tree structure whose leaves names contain the given "name" parameter.
	 * Each node contains the fields: id, name, locked, children.
	 * - id: id of the child role
	 * - name: name of the child role
	 * - locked: boolean that indicates if the child role is locked
	 * - children: list of the role's first level children, if any.
	 *
	 * This is used to feed the resulting Tree (UI) in the Role Manager of dotCMS Admin when using
	 * the filter functionality
	 *
	 * Usage: /api/role/loadbyname/name/<id>
	 * Example usage: /api/role/loadbyid/id/2adccac3-a56b-4078-be40-94e343f20712
	 *
	 *
	 * @param request
	 * @param params
	 * @return
	 * @throws DotDataException
	 * @throws JSONException
	 */
	@Operation(
		operationId = "loadRolesByNameLegacy",
		summary = "Load roles by name filter (deprecated)",
		description = "Returns a filtered role tree structure where leaf nodes contain the specified name. Used for role filtering in admin UI. This endpoint is deprecated.",
		deprecated = true
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", 
					description = "Filtered roles loaded successfully",
					content = @Content(mediaType = "application/json",
									  schema = @Schema(type = "object", description = "Filtered role tree structure with identifier, label, and items containing matching roles"))),
		@ApiResponse(responseCode = "401", 
					description = "Unauthorized - backend user authentication required",
					content = @Content(mediaType = "application/json")),
		@ApiResponse(responseCode = "500", 
					description = "Internal server error",
					content = @Content(mediaType = "application/json"))
	})
	@GET
	@Path("/loadbyname/{params:.*}")
	@Produces("application/json")
	@SuppressWarnings("unchecked")
	public Response loadByName(@Context HttpServletRequest request, @Context final HttpServletResponse response, 
		@Parameter(description = "URL parameters including name filter (name=filterText)", required = true) @PathParam("params") String params) throws DotDataException, JSONException {

		final InitDataObject initData = new WebResource.InitBuilder(webResource)
				.requiredBackendUser(true)
				.requiredFrontendUser(false)
				.params(params)
				.requestAndResponse(request, response)
				.rejectWhenNoUser(true)
				.init();

        //Creating an utility response object
        ResourceResponse responseResource = new ResourceResponse( initData.getParamsMap() );

		Map<String, String> paramsMap = initData.getParamsMap();
		String name = paramsMap.get("name");

		if(!UtilMethods.isSet(name)) {
            responseResource.response( "" );//FIXME: Should return a proper error....
        }

		RoleAPI roleAPI = APILocator.getRoleAPI();
		Role userRole = roleAPI.loadRoleByKey(RoleAPI.USERS_ROOT_ROLE_KEY);
		List<Role> roles = roleAPI.findRolesByNameFilter(name, -1, -1);

		LinkedHashMap<String, Object> resultTree = new LinkedHashMap<>();

		for (Role r : roles) {

			String DBFQN =  r.getDBFQN();

			if(DBFQN.contains(userRole.getId())) {
				continue;
			}

			String node = DBFQN.split(" --> ")[0];
			int offset = DBFQN.indexOf(" --> ");

			if(offset>0) {
				String nodes = DBFQN.substring(offset+5, DBFQN.length());
				// check if it already exists
				LinkedHashMap<String, Object> existingMap = (LinkedHashMap<String, Object>) resultTree.get(node);

				if(existingMap!=null) {
					buildTree(existingMap, nodes); // if exists past the existing HashMap to continue looking for children
				} else {
					resultTree.put(node,  buildTree(new LinkedHashMap<>(), nodes)); // if does not exist put the key and continue building recursively
				}

			} else {
				resultTree.put(node,  null);
			}

		}
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("identifier", "id");
		jsonObject.put("label", "name");

		JSONArray jsonItems = new JSONArray();

		JSONObject jsonItemsObject = new JSONObject();
		jsonItemsObject.put("id", "root");
		jsonItemsObject.put("name", "Roles");
		jsonItemsObject.put("top", true);
		jsonItemsObject.put("children", (Object)buildFilteredJsonTree(resultTree));

		jsonItems.add(jsonItemsObject);

		jsonObject.put("items", (Object)jsonItems);

        return responseResource.response(jsonObject.toString());
	}

	@SuppressWarnings("unchecked")
	private LinkedHashMap<String, Object> buildTree(LinkedHashMap<String, Object> map, String nodes) {

		String node = nodes.split(" --> ")[0];
		int offset = nodes.indexOf(" --> ");

		if(offset>0) {
			String subNodes = nodes.substring(offset+5, nodes.length());

			LinkedHashMap<String, Object> existingMap = (LinkedHashMap<String, Object>) map.get(node);

			if(existingMap!=null) {
				buildTree(existingMap, subNodes); // if exists pass the existing HashMap to continue looking for children
			} else {
				map.put(node,  buildTree(new LinkedHashMap<>(), subNodes)); // if does not exist put the key and continue building recursively
			}

		} else {
			map.put(node,  null);
		}

		return map;

	}

	@SuppressWarnings("unchecked")
	private JSONArray buildFilteredJsonTree(LinkedHashMap<String, Object> map) throws DotDataException, JSONException {
		JSONArray jsonChildren = new JSONArray();

		RoleAPI roleAPI = APILocator.getRoleAPI();

		if(map != null) {
			for (String key : map.keySet()) {
				Role r = roleAPI.loadRoleById(key);

				JSONObject jsonObject = new JSONObject();
				jsonObject.put("id", r.getId().replace('-', '_'));
				jsonObject.put("name", r.getName());
				jsonObject.put("locked", r.isLocked());

				LinkedHashMap<String, Object> children = (LinkedHashMap<String, Object>) map.get(key);

				jsonObject.put("children", (Object)buildFilteredJsonTree(children));
				jsonChildren.add(jsonObject);
			}
		}
		return jsonChildren;
	}



}
