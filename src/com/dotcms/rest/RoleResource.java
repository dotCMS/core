package com.dotcms.rest;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.UtilMethods;


@Path("/role")
public class RoleResource extends WebResource {

	/**
	 * Returns a JSON representation of Roles in the System.
	 * To load a role, use:/api/role/id/{id}
	 * To retrieve the children of a given role, use:/api/role/children/id/{id}
	 * To get roles by name, use /api/role/name/{name}
	 *
	 * @param request
	 * @param response
	 * @param params
	 * @param name
	 * @return
	 * @throws DotStateException
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */

	@GET
	@Path("/children/{params:.*}")
	@Produces("application/json")
	public String getRoleChildren(@Context HttpServletRequest request, @Context HttpServletResponse response, @PathParam("params") String params) throws DotStateException, DotDataException, DotSecurityException {
		InitDataObject initData = init(params, AuthType.PARAMS_OR_SESSION, request, true);

		Map<String, String> paramsMap = initData.getParamsMap();
		String roleId = paramsMap.get("id");

		RoleAPI roleAPI = APILocator.getRoleAPI();
		StringBuilder json = new StringBuilder();

		if(!UtilMethods.isSet(roleId) || roleId.equals("root")) {  // Loads Root Roles
			json.append("[ { id: 'root', name: 'Roles', top: true, children: ").append("[");
			int rolesCounter = 0;
			List<Role> rootRoles = roleAPI.findRootRoles();


			for(Role r : rootRoles) {
				json.append("{id: '").append(r.getId()).append("', ");
				json.append("$ref: '").append(r.getId()).append("', ");
				json.append("name: '").append(r.getName()).append("', ");
				json.append(" children:true}");

				if(rolesCounter+1 < rootRoles.size()) {
					json.append(", ");
				}

				rolesCounter++;
			}

			json.append("]").append(" } ]");

		} else {  // Loads Children Roles of given Role ID
			Role role = roleAPI.loadRoleById(roleId);
			List<String> children = role.getRoleChildren();

			json.append("{ id: '").append(role.getId()).append("', name: '").append(role.getName()).append("', children: ").append("[");

			if(children!=null) {

				int childCounter = 0;
				for(String childId : children) {
					Role r = roleAPI.loadRoleById(childId);

					json.append("{id: '").append(r.getId()).append("', $ref: '").append(r.getId()).append("', name: '").append(r.getName()).append("', children:true}");

					if(childCounter+1 < children.size()) {
						json.append(", ");
					}

					childCounter++;
				}
			}

			json.append("]").append(" }");

		}

		return json.toString();
	}

	@GET
	@Path("/id/{id}/{params:.*}")
	@Produces("application/json")
	public String loadRole(@PathParam("id") String roleId) throws DotDataException {
		if(roleId.equalsIgnoreCase("root")) {
			return "{id:'0', name: 'Root Role'}";
		}

		RoleAPI roleAPI = APILocator.getRoleAPI();
		Role role = roleAPI.loadRoleById(roleId);

		StringBuilder node = new StringBuilder();
		node.append("{");
		node.append("DBFQN: '").append(role.getDBFQN()).append("',");
		node.append("FQN: '").append(role.getFQN()).append("',");
		node.append("children: [],");
		node.append("description: '").append(role.getDescription()).append("',");
		node.append("editLayouts: '").append(role.isEditLayouts()).append("',");
		node.append("editPermissions: '").append(role.isEditPermissions()).append("',");
		node.append("editUsers: '").append(role.isEditUsers()).append("',");
		node.append("id: '").append(role.getId()).append("',");
		node.append("locked: '").append(role.isLocked()).append("',");
		node.append("name: '").append(role.getName()).append("',");
		node.append("parent: '").append(role.getParent()).append("',");
		node.append("roleKey: '").append(role.getRoleKey()!=null?role.getRoleKey():"").append("',");
		node.append("system: '").append(role.isSystem()).append("'");
		node.append("}");

		return node.toString();
	}

	@GET
	@Path("/name/{name}/{params:.*}")
	@Produces("application/json")
	@SuppressWarnings("unchecked")
	public String getRolesByQuery(@PathParam("name") String name) throws DotDataException {

		if(!UtilMethods.isSet(name))
			return "";

		RoleAPI roleAPI = APILocator.getRoleAPI();
		Role userRole = roleAPI.loadRoleByKey(RoleAPI.USERS_ROOT_ROLE_KEY);
		List<Role> roles = roleAPI.findRolesByNameFilter(name, -1, -1);

		LinkedHashMap<String, Object> resultTree = new LinkedHashMap<String, Object>();


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
					resultTree.put(node,  buildTree(new LinkedHashMap<String, Object>(), nodes)); // if does not exist put the key and continue building recursively
				}

			} else {
				resultTree.put(node,  null);
			}

		}

		// build the resulting Json Tree
		String json = buildFilteredJsonTree(resultTree);
		return "{ identifier: 'id', label: 'name', items: [ { id: 'root', name: 'Roles', top: true, " +
            "children: [" + json + "] } ] }";

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
				map.put(node,  buildTree(new LinkedHashMap<String, Object>(), subNodes)); // if does not exist put the key and continue building recursively
			}

		} else {
			map.put(node,  null);
		}

		return map;

	}

	@SuppressWarnings("unchecked")
	private String buildFilteredJsonTree(LinkedHashMap<String, Object> map) throws DotDataException {
		StringBuilder json = new StringBuilder();
		RoleAPI roleAPI = APILocator.getRoleAPI();

		if(map==null) {
			return "";
		}
		for (String key : map.keySet()) {
			Role r = roleAPI.loadRoleById(key);
			json.append("{ id: '").append(r.getId().replace('-', '_')).append("', name: '").append(r.getName()).append("', children: ").append("[");

			LinkedHashMap<String, Object> children = (LinkedHashMap<String, Object>) map.get(key);
			json.append(buildFilteredJsonTree(children));

			json.append("]},");
		}

		String jsonStr = json.toString();
		// removing comma after last item
		return jsonStr.length()>0?jsonStr.substring(0, jsonStr.length()-1):jsonStr;
	}


}
