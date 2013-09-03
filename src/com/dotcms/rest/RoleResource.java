package com.dotcms.rest;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.UtilMethods;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


@Path("/role")
public class RoleResource extends WebResource {

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
	@Path("/loadchildren/{params:.*}")
	@Produces("application/json")
	public Response loadChildren(@Context HttpServletRequest request, @PathParam("params") String params) throws DotStateException, DotDataException, DotSecurityException {

        InitDataObject initData = init(params, true, request, true);

        //Creating an utility response object
        ResourceResponse responseResource = new ResourceResponse( initData.getParamsMap() );

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
				json.append("locked: '").append(r.isLocked()).append("', ");
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

			json.append("{ id: '").append(role.getId()).append("', name: '").append(role.getName()).append("', locked: '")
			.append(role.isLocked()).append("', children: ").append("[");

			if(children!=null) {

				int childCounter = 0;
				for(String childId : children) {
					Role r = roleAPI.loadRoleById(childId);

					json.append("{id: '").append(r.getId()).append("', $ref: '").append(r.getId()).append("', name: '")
					.append(r.getName()).append("', locked: '").append(r.isLocked()).append("', children:true}");

					if(childCounter+1 < children.size()) {
						json.append(", ");
					}

					childCounter++;
				}
			}

			json.append("]").append(" }");

		}

        CacheControl cc = new CacheControl();
        cc.setNoCache( true );
        return responseResource.response( json.toString(), cc );
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
	 */

	@GET
	@Path("/loadbyid/{params:.*}")
	@Produces("application/json")
	public Response loadById(@Context HttpServletRequest request, @PathParam("params") String params) throws DotDataException {
		InitDataObject initData = init(params, true, request, true);

        //Creating an utility response object
        ResourceResponse responseResource = new ResourceResponse( initData.getParamsMap() );

		Map<String, String> paramsMap = initData.getParamsMap();
		String roleId = paramsMap.get("id");

		if(!UtilMethods.isSet(roleId) || roleId.equalsIgnoreCase("root")) {
            return responseResource.response( "{id:'0', name: 'Root Role'}" );
		}

		RoleAPI roleAPI = APILocator.getRoleAPI();
		Role role = roleAPI.loadRoleById(roleId);

		StringBuilder node = new StringBuilder();
		node.append("{");
		node.append("DBFQN: '").append(role.getDBFQN()).append("',");
		node.append("FQN: '").append(role.getFQN()).append("',");
		node.append("children: [],");
		node.append("description: '").append(role.getDescription()).append("',");
		node.append("editLayouts: ").append(role.isEditLayouts()).append(",");
		node.append("editPermissions: ").append(role.isEditPermissions()).append(",");
		node.append("editUsers: ").append(role.isEditUsers()).append(",");
		node.append("id: '").append(role.getId()).append("',");
		node.append("locked: '").append(role.isLocked()).append("',");
		node.append("name: '").append(role.getName()).append("',");
		node.append("parent: '").append(role.getParent()).append("',");
		node.append("roleKey: '").append(role.getRoleKey()!=null?role.getRoleKey():"").append("',");
		node.append("system: '").append(role.isSystem()).append("'");
		node.append("}");

        return responseResource.response( node.toString() );
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
	 */

	@GET
	@Path("/loadbyname/{params:.*}")
	@Produces("application/json")
	@SuppressWarnings("unchecked")
	public Response loadByName(@Context HttpServletRequest request, @PathParam("params") String params) throws DotDataException {
		InitDataObject initData = init(params, true, request, true);

        //Creating an utility response object
        ResourceResponse responseResource = new ResourceResponse( initData.getParamsMap() );

		Map<String, String> paramsMap = initData.getParamsMap();
		String name = paramsMap.get("name");

		if(!UtilMethods.isSet(name)) {
            //return "";
            responseResource.response( "" );//FIXME: Should return a proper error....
        }

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
        return responseResource.response( "{ identifier: 'id', label: 'name', items: [ { id: 'root', name: 'Roles', top: true, " +
                "children: [" + json + "] } ] }" );
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
			json.append("{ id: '").append(r.getId().replace('-', '_')).append("', name: '").append(r.getName())
			.append("', locked: ").append(r.isLocked()).append(", children: ").append("[");

			LinkedHashMap<String, Object> children = (LinkedHashMap<String, Object>) map.get(key);
			json.append(buildFilteredJsonTree(children));

			json.append("]},");
		}

		String jsonStr = json.toString();
		// removing comma after last item
		return jsonStr.length()>0?jsonStr.substring(0, jsonStr.length()-1):jsonStr;
	}


}
