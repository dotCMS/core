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
import javax.ws.rs.QueryParam;
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

	@GET
	@Path("/{path:.*}")
	@Produces("application/json")
	public String getRootRoles(@Context HttpServletRequest request, @Context HttpServletResponse response, @PathParam("path") String path, @QueryParam("name") String name) throws DotStateException, DotDataException, DotSecurityException {
		Map<String, String> params = parsePath(path);
		Boolean excludeUserRoles = params.get("excludeUserRoles")!=null;
		Boolean onlyUserAssignableRoles = params.get("onlyUserAssignableRoles")!=null;
		String roleId = params.get("id");
		String method = params.get("method");

		if(UtilMethods.isSet(method) && method.equals("full")) {
			return getRolesTree();  // Loads all the Roles for the Parent Filtering Select
		} else if(UtilMethods.isSet(method) && method.equals("loadRole")) {
			String roleMap = getRole(roleId);
			return roleMap; // Loads all the data for a given Role ID
		} else if(UtilMethods.isSet(method) && method.equals("filter")) {
			String rolesMap = getRolesByQuery(params.get("query"));
			return rolesMap; // Loads all the data for a given Role ID
		}

		RoleAPI roleAPI = APILocator.getRoleAPI();
		StringBuilder json = new StringBuilder();

		if(!UtilMethods.isSet(roleId)) {  // Loads Root Roles
			json.append("[ { id: 'root', name: 'Roles', top: true, children: ").append("[");
			int rolesCounter = 0;
			List<Role> rootRoles = roleAPI.findRootRoles();

			for(Role r : rootRoles) {

				if(onlyUserAssignableRoles) {

					//If the role has no children and is not user assignable then we don't include it
					if(!r.isEditUsers() && (r.getRoleChildren() == null || r.getRoleChildren().size() == 0))
						continue;
					//Special case the users roles branch should be entirely hidden
					if(r.getRoleKey() != null && r.getRoleKey().equals(RoleAPI.USERS_ROOT_ROLE_KEY))
						continue;
				}

				if(excludeUserRoles) {
					if(r.getRoleKey() != null && r.getRoleKey().equals(RoleAPI.USERS_ROOT_ROLE_KEY))
						continue;
				}

				json.append("{id: '").append(r.getId()).append("', $ref: '").append(r.getId()).append("', name: '").append(r.getName()).append("', children:true}");

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

					if(onlyUserAssignableRoles) {

						//If the role has no children and is not user assignable then we don't include it
						if(!r.isEditUsers() && (r.getRoleChildren() == null || r.getRoleChildren().size() == 0))
							continue;
						//Special case the users roles branch should be entirely hidden
						if(r.getRoleKey() != null && r.getRoleKey().equals(RoleAPI.USERS_ROOT_ROLE_KEY))
							continue;
					}

					if(excludeUserRoles) {
						if(r.getRoleKey() != null && r.getRoleKey().equals(RoleAPI.USERS_ROOT_ROLE_KEY))
							continue;
					}

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

	private String getRolesTree () throws DotDataException {
		StringBuilder toReturn = new StringBuilder();
		RoleAPI roleAPI = APILocator.getRoleAPI();
		List<Role> rootRoles = roleAPI.findRootRoles();

		for(Role r : rootRoles) {
			toReturn.append(constructRoleMap(r, 0));
		}

		String toReturnStr = toReturn.toString();
		String finalStr = "[{id: '0', name: 'Root Role'}, " + toReturnStr.substring(0, toReturnStr.length()-2) + "]";

		return finalStr;
	}

	private String constructRoleMap(Role role, int level) throws DotDataException {
		RoleAPI roleAPI = APILocator.getRoleAPI();
		StringBuilder roleMap = new StringBuilder();
		String depth = "";

		if(role!=null){
			 for(int i=0; i<level; i++) {
				 depth+= "-->";
			 }
        	 roleMap.append("{name: '").append(depth).append(role.getName()).append("', id: '").append(role.getId()).append("'}, ");
        }

		if(role!=null && role.getRoleChildren() != null) {

			level++;

			for(String id : role.getRoleChildren()) {
				Role childRole = roleAPI.loadRoleById(id);
				roleMap.append(constructRoleMap(childRole, level));
			}
		}

		return roleMap.toString();
	}

	private String getRole(String roleId) throws DotDataException {
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

	@SuppressWarnings("unchecked")
	private String getRolesByQuery(String query) throws DotDataException {

		if(!UtilMethods.isSet(query))
			return "";

		RoleAPI roleAPI = APILocator.getRoleAPI();
		List<Role> roles = roleAPI.findRolesByNameFilter(query, -1, -1);

		LinkedHashMap<String, Object> resultTree = new LinkedHashMap<String, Object>();


		for (Role r : roles) {
			String DBFQN =  r.getDBFQN();
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
				buildTree(existingMap, subNodes); // if exists past the existing HashMap to continue looking for children
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
		return jsonStr.length()>0?jsonStr.substring(0, jsonStr.length()-1):jsonStr;
	}




}
