/**
 * 
 */
package com.dotmarketing.business;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.transform.contenttype.StructureTransformer;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.templates.design.bean.TemplateLayout;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

/**
 * @author Jason Tesser
 * The purpose of this class is to pull things like Templates, Pages, and Files with their respecting permissions
 * This is needed because if you have a limited user in a system with many object and permissions it can be slow to pull all 
 * WebAssets and then loop over each one filtering out based on permissions. This class provides methods which get what is needed 
 * in 1-2 DB pulls per call.  This can be faster in the large systems. 
 */

public class PermissionedWebAssetUtil {

	/**
	 * THIS METHOD WILL HIT DB AT LEAST ONCE. It loads the templatesIds to load then trys to load from Cache or go back
	 * to DB using Hibernate to load the ones not in cache with a single Hibernate query (if there are more then 500 it will actually go back to 
	 * db for each 500 as it does an IN (..)).  
	 * NOTE: Returns working templates 
	 * @param searchString - Can be null or empty. Can be used as a filter. Will Search Template TITLE and Host (if searchHost is True)
	 * @param dbColSort - Name of the DB Column to sort by. If left null or empty will sort by template name by default
	 * @param offset
	 * @param limit
	 * @param permission - The required Permission needed to pull template
	 * @param user
	 * @param respectFrontEndPermissions
	 * @return
	 * @throws DotSecurityException 
	 * @throws DotDataException 
	 */
	public static List<Template> findTemplatesForLimitedUser(String searchString, String hostName, boolean searchHost,String dbColSort ,int offset, int limit,int permission, User user, boolean respectFrontEndPermissions) throws DotDataException, DotSecurityException{
		//		PermissionAPI perAPI = APILocator.getPermissionAPI();
		//		UserAPI uAPI = APILocator.getUserAPI();
		//		if(uAPI.isCMSAdmin(user)){
		//			if(UtilMethods.isSet(searchString)){
		//				return InodeFactory.getInodesOfClassByConditionAndOrderBy(Template.class, "lower(title) LIKE '%" + templateName.toLowerCase() + "%'", dbColSort,limit, offset);
		//			}else{
		//				return InodeFactory.getInodesOfClass(Template.class, dbColSort,limit, offset);
		//			}
		//		}else{
		offset = offset<0?0:offset;
		String hostQuery = null;
		if(searchHost){
			Structure st = CacheLocator.getContentTypeCache().getStructureByVelocityVarName("Host");
			Field hostNameField = st.getFieldVar("hostName");
			List<Contentlet> list = null;
			try {
                String query = "+structureInode:" + st.getInode() + " +working:true";
                if(UtilMethods.isSet(hostName)){
                    query += " +" + hostNameField.getFieldContentlet() + ":" + hostName;
                }
				list = APILocator.getContentletAPI().search(query, 0, 0, null, user, respectFrontEndPermissions);
			} catch (Exception e) {
				Logger.error(PermissionedWebAssetUtil.class,e.getMessage(),e);
			}
			if(list!=null){
				if(list.size()>0){
					hostQuery = "identifier.host_inode IN (";
				}
				boolean first = true;
				for (Contentlet contentlet : list) {
					if(!first){
						hostQuery+=",";
					}
					hostQuery+="'" + contentlet.getIdentifier() + "'";
					first = false;
				}
				if(hostQuery != null){
					hostQuery+=")";
				}
			}
		}
		ArrayList<ColumnItem> columnsToOrderBy = new ArrayList<ColumnItem>();
		ColumnItem templateTitle = new ColumnItem("title", "template", null, true, OrderDir.ASC);
		//ColumnItem hostName = new ColumnItem("title", "contentlet", "host_name", true, OrderDir.ASC);
		//columnsToOrderBy.add(hostName);
		columnsToOrderBy.add(templateTitle);

		List<String> tIds = queryForAssetIds("template, identifier, inode, template_version_info ",
		        new String[] {Template.class.getCanonicalName(), TemplateLayout.class.getCanonicalName()},
		        "template.inode", 
		        "identifier.id",
		        "template.identifier = identifier.id and inode.inode = template.inode and " +
		            "identifier.id=template_version_info.identifier and template_version_info.working_inode=template.inode and " +
		            "template_version_info.deleted="+DbConnectionFactory.getDBFalse() 
				+ (
						UtilMethods.isSet(searchString) ? 
						" and (lower(template.title) LIKE '%" + searchString.toLowerCase() + "%'" 
						+(UtilMethods.isSet(hostQuery)? " AND (" + hostQuery + ")":"") + ")":
						(UtilMethods.isSet(hostQuery)? " AND (" + hostQuery + ")":"")
			      ) , columnsToOrderBy, offset, limit, permission, respectFrontEndPermissions, user);

		//		CacheLocator.getTemplateCache().
		if(tIds != null && tIds.size()>0){
			StringBuilder bob = new StringBuilder();
			for (String s : tIds) {
				bob.append("'").append(s).append("',");
			}

			return InodeFactory.getInodesOfClassByConditionAndOrderBy(Template.class, "inode in (" + bob.toString().subSequence(0, bob.toString().length()-1) + ")", dbColSort);
		}else{
			return new ArrayList<Template>();
		}
		//	}
	}
	
	
	/**
	 * THIS METHOD WILL HIT DB AT LEAST ONCE. It loads the templatesIds to load then trys to load from Cache or go back
	 * to DB using Hibernate to load the ones not in cache with a single Hibernate query (if there are more then 500 it will actually go back to 
	 * db for each 500 as it does an IN (..)).  
	 * NOTE: Returns working templates 
	 * @param searchString - Can be null or empty. Can be used as a filter. Will Search Template TITLE and Host (if searchHost is True)
	 * @param dbColSort - Name of the DB Column to sort by. If left null or empty will sort by template name by default
	 * @param offset
	 * @param limit
	 * @param permission - The required Permission needed to pull template
	 * @param user
	 * @param respectFrontEndPermissions
	 * @return
	 * @throws DotSecurityException 
	 * @throws DotDataException 
	 */
	public static List<Structure> findStructuresForLimitedUser(String searchString, Integer structureType ,String dbColSort ,int offset, 
			int limit,int permission, User user, boolean respectFrontEndPermissions) throws DotDataException, DotSecurityException{
		BaseContentType baseType = BaseContentType.getBaseContentType(structureType);
		List<ContentType> listContentTypes = APILocator.getContentTypeAPI(user).search(searchString,baseType, dbColSort, limit, offset);
		return new StructureTransformer(listContentTypes).asStructureList();
	}
	
	/**
	 * Returns the list of {@link Container} objects that the current user has
	 * access to. This will retrieve a final list of results with a single
	 * query, instead of programmatically traversing the total list of
	 * containers and then filtering it to get the permissioned objects from it.
	 * 
	 * @param searchString
	 *            - Can be null or empty. It is used as a filter to search for
	 *            container's "title".
	 * @param hostName
	 *            - Can be null or empty. It is used as a filter to search for
	 *            containers in a specific site. The "searchHost" must be set to
	 *            {@code true}.
	 * @param searchHost
	 *            - If {@code true}, and if "hostName" is not empty, the method
	 *            will only get the containers from the specified site.
	 * @param dbColSort
	 *            - The column name of the DB table used to order the results.
	 * @param offset
	 *            - The offset of records to include in the result (used for
	 *            pagination purposes).
	 * @param limit
	 *            - The limit of records to retrieve (used for pagination
	 *            purposes).
	 * @param permission
	 *            - The required Permission needed to pull the containers
	 *            information.
	 * @param user
	 *            - The {@link User} that performs the operation.
	 * @param respectFrontEndPermissions
	 * @return The {@code List} of {@link Container} objects that the specified
	 *         user has access to.
	 * @throws DotDataException
	 *             - If an error occurred when retrieving information from the
	 *             database.
	 * @throws DotSecurityException
	 *             - If the current user does not have permission to perform the
	 *             required operation.
	 */
	public static List<Container> findContainersForLimitedUser(
			String searchString, String hostName, boolean searchHost,
			String dbColSort, int offset, int limit, int permission, User user,
			boolean respectFrontEndPermissions) throws DotDataException,
			DotSecurityException {
		offset = offset < 0 ? 0 : offset;
		String hostQuery = null;
		if (searchHost) {
			Structure st = CacheLocator.getContentTypeCache().getStructureByVelocityVarName("Host");
			Field hostNameField = st.getFieldVar("hostName");
			List<Contentlet> hostList = null;
			try {
				String query = "+structureInode:" + st.getInode()
						+ " +working:true";
				if (UtilMethods.isSet(hostName)) {
					query += " +" + hostNameField.getFieldContentlet() + ":"
							+ hostName;
				}
				hostList = APILocator.getContentletAPI().search(query, 0, 0,
						null, user, respectFrontEndPermissions);
			} catch (Exception e) {
				Logger.error(PermissionedWebAssetUtil.class, e.getMessage(), e);
			}
			if (hostList != null) {
				if (hostList.size() > 0) {
					hostQuery = "identifier.host_inode IN (";
				}
				boolean first = true;
				for (Contentlet contentlet : hostList) {
					if (!first) {
						hostQuery += ",";
					}
					hostQuery += "'" + contentlet.getIdentifier() + "'";
					first = false;
				}
				if (hostQuery != null) {
					hostQuery += ")";
				}
			}
		}
		ArrayList<ColumnItem> columnsToOrderBy = new ArrayList<ColumnItem>();
   		ColumnItem templateTitle = new ColumnItem("title", Inode.Type.CONTAINERS.getTableName(), null,
				true, OrderDir.ASC);
		columnsToOrderBy.add(templateTitle);
		List<String> containerIds = queryForAssetIds(
			    Inode.Type.CONTAINERS.getTableName() + " , identifier, inode, " + Inode.Type.CONTAINERS.getVersionTableName(),
				new String[] { Container.class.getCanonicalName() },
			    Inode.Type.CONTAINERS.getTableName() + ".inode",
				"identifier.id",
			    Inode.Type.CONTAINERS.getTableName() + ".identifier = identifier.id and inode.inode = " + Inode.Type.CONTAINERS.getTableName() + ".inode and "
						+ "identifier.id=container_version_info.identifier and container_version_info.working_inode=" + Inode.Type.CONTAINERS.getTableName() + ".inode and "
						+ "container_version_info.deleted="
						+ DbConnectionFactory.getDBFalse()
						+ (UtilMethods.isSet(searchString) ? " and (lower(" + Inode.Type.CONTAINERS.getTableName() + ".title) LIKE '%"
								+ searchString.toLowerCase()
								+ "%'"
								+ (UtilMethods.isSet(hostQuery) ? " AND ("
										+ hostQuery + ")" : "") + ")"
								: (UtilMethods.isSet(hostQuery) ? " AND ("
										+ hostQuery + ")" : "")),
				columnsToOrderBy, offset, limit, permission,
				respectFrontEndPermissions, user);
		if (containerIds != null && containerIds.size() > 0) {
			StringBuilder identifiers = new StringBuilder();
			for (String id : containerIds) {
				identifiers.append("'").append(id).append("',");
			}
			return InodeFactory.getInodesOfClassByConditionAndOrderBy(
					Container.class,
					"inode in ("
							+ identifiers.toString().subSequence(0,
									identifiers.toString().length() - 1) + ")",
					dbColSort);
		} else {
			return new ArrayList<Container>();
		}
	}

	/**
	 * Will execute the query to get assetIds with required permission
	 * @param tablesToJoin - This would be the tables to include in where ie... "template,inode,indentifier"
	 * @param assetWhereClause - ie... "inode.identifier = identifier.inode and inode.inode = template.inode and template.title LIKE '%mytitle%'"
	 * @param colToSelect ie.. template.inode
	 * @param colToJoinAssetIdTo The column and table to joing the asset_id or inode_id from permission tables to ie.. identifier.inode
	 * @param orderByClause - ie... template.name ASC   make sure to capitalize the ASC or DESC
	 * @param offset
	 * @param limit
	 * @param requiredTypePermission
	 * @param respectFrontendRoles
	 * @param user
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	private static List<String> queryForAssetIds(String tablesToJoin,String[] permissionType ,String colToSelect, String colToJoinAssetIdTo, String assetWhereClause, List<ColumnItem> columnsToOrderBy, int offset, int limit, int requiredTypePermission, boolean respectFrontendRoles, User user) throws DotDataException,DotSecurityException {
		Role anonRole;
		Role frontEndUserRole;
		Role adminRole;
		boolean userIsAdmin = false;
		try {
			adminRole = APILocator.getRoleAPI().loadCMSAdminRole();
			anonRole = APILocator.getRoleAPI().loadCMSAnonymousRole();
			frontEndUserRole = APILocator.getRoleAPI().loadLoggedinSiteRole();
		} catch (DotDataException e1) {
			Logger.error(PermissionedWebAssetUtil.class, e1.getMessage(), e1);
			throw new DotRuntimeException(e1.getMessage(), e1);
		}	

		List<String> roleIds = new ArrayList<String>();
		if(respectFrontendRoles){
			// add anonRole and frontEndUser roles
			roleIds.add(anonRole.getId());
			if(user != null ){
				roleIds.add("'"+frontEndUserRole.getId()+"'");
			}
		}

		//If user is null and roleIds are empty return empty list
		if(roleIds.isEmpty() && user==null){
			return new ArrayList<String>();
		}

		List<Role> roles;
		try {
			roles = APILocator.getRoleAPI().loadRolesForUser(user.getUserId());
		} catch (DotDataException e1) {
			Logger.error(PermissionedWebAssetUtil.class, e1.getMessage(), e1);
			throw new DotRuntimeException(e1.getMessage(), e1);
		}	
		for (Role role : roles) {
			try{
				String roleId = role.getId();
				roleIds.add("'"+roleId+"'");
				if(roleId.equals(adminRole.getId())){
					userIsAdmin = true;
				}
			}catch (Exception e) {
				Logger.error(PermissionedWebAssetUtil.class, "Roleid should be a long : ",e);
			}
		}

		StringBuilder permissionRefSQL = new StringBuilder();
		String extraSQLForOffset = "";
		
		String orderByClause = "";
		String orderByClauseWithAlias = "";
		String orderBySelect = "";
		int count  = 0;
		for(ColumnItem item : columnsToOrderBy){
			if(DbConnectionFactory.isPostgres()
					|| DbConnectionFactory.isMySql()){
				item.setIsString(false);
			}
			orderByClause += item.getOrderClause(true) + (count<columnsToOrderBy.size()-1?", ":"");
			orderByClauseWithAlias += item.getAliasOrderClause() + (count<columnsToOrderBy.size()-1?", ":"");
			orderBySelect += item.getSelectClause(true) + (count<columnsToOrderBy.size()-1?", ":"");
			count++;
		}

		if(DbConnectionFactory.isOracle()){
			extraSQLForOffset = "ROW_NUMBER() OVER(ORDER BY "+orderByClause+") LINENUM, ";
		}else if(DbConnectionFactory.isMsSql()){
			extraSQLForOffset = "ROW_NUMBER() OVER (ORDER BY "+orderByClause+") AS LINENUM, ";
		}
		permissionRefSQL.append("SELECT * FROM (");
		permissionRefSQL.append("SELECT ").append(extraSQLForOffset).append(colToSelect).append(" as asset_id,").append(orderBySelect).append(" ");
		permissionRefSQL.append("FROM ");
		if(!userIsAdmin){
			permissionRefSQL.append("permission_reference, permission, ");
		}
		permissionRefSQL.append(tablesToJoin).append(" ");
		permissionRefSQL.append("WHERE ");
		if(!userIsAdmin){
			permissionRefSQL.append("permission_reference.reference_id = permission.inode_id ");
			permissionRefSQL.append("AND permission.permission_type = permission_reference.permission_type ");
			permissionRefSQL.append("AND permission_reference.asset_id = ").append(colToJoinAssetIdTo).append(" AND ");
		}
		permissionRefSQL.append(assetWhereClause).append(" ");
		if(!userIsAdmin){
		    if(permissionType.length==1) {
		        permissionRefSQL.append("AND permission.permission_type = '").append(permissionType[0]).append("' ");
		    }
		    else {
		        permissionRefSQL.append(" AND (");
		        boolean first=true;
		        for(String type : permissionType) {
		            if(first) {
		                first=false;
		            }
		            else {
		                permissionRefSQL.append(" OR ");
		            }
		            permissionRefSQL.append(" permission.permission_type = '").append(type).append("' ");
		        }
		        permissionRefSQL.append(") ");
		    }
			
			permissionRefSQL.append("AND permission.roleid in( ");
		}
		StringBuilder individualPermissionSQL = new StringBuilder();
		if(!userIsAdmin){
			individualPermissionSQL.append("select ").append(extraSQLForOffset).append(colToSelect).append(" as asset_id, ").append(orderBySelect).append(" ");
			individualPermissionSQL.append("FROM ");
			individualPermissionSQL.append("permission,");

			individualPermissionSQL.append(tablesToJoin).append(" WHERE ");
			individualPermissionSQL.append("permission_type = 'individual' ");
			individualPermissionSQL.append(" and permission.inode_id=").append(colToJoinAssetIdTo).append(" AND ");

			individualPermissionSQL.append(assetWhereClause).append(" ");
			individualPermissionSQL.append(" and roleid in( ");
			int roleIdCount = 0;
			for(String roleId : roleIds){
				permissionRefSQL.append(roleId);
				individualPermissionSQL.append(roleId);
				if(roleIdCount<roleIds.size()-1){
					permissionRefSQL.append(", ");
					individualPermissionSQL.append(", ");
				}
				roleIdCount++;
			}
			if(DbConnectionFactory.isOracle() || DbConnectionFactory.isH2()){
				permissionRefSQL.append(") and bitand(permission.permission, ").append(requiredTypePermission).append(") > 0 ");
				individualPermissionSQL.append(") and bitand(permission, ").append(requiredTypePermission).append(") > 0 ");
			}else{
				permissionRefSQL.append(") and (permission.permission & ").append(requiredTypePermission).append(") > 0 ");
				individualPermissionSQL.append(") and (permission & ").append(requiredTypePermission).append(") > 0 ");
			}
		}
		permissionRefSQL.append(" group by ").append(colToSelect).append(" ");
		if(UtilMethods.isSet(individualPermissionSQL.toString())){
			individualPermissionSQL.append(" group by ").append(colToSelect).append(" ");
		}
		for(ColumnItem item : columnsToOrderBy){
			if(DbConnectionFactory.isPostgres() 
					|| DbConnectionFactory.isMySql()){
				item.setIsString(false);
			}

			orderBySelect = item.getSelectClause(true);
			permissionRefSQL.append(", ").append(orderBySelect).append(" ");
			if(UtilMethods.isSet(individualPermissionSQL.toString())){
				individualPermissionSQL.append(", ").append(orderBySelect).append(" ");
			}
		}
		List<String> idsToReturn = new ArrayList<String>();
		String sql = "";
		DotConnect dc = new DotConnect();
		String limitOffsetSQL = null;
		boolean limitResults = limit > 0;
		if(DbConnectionFactory.isOracle()){
			limitOffsetSQL = limitResults ? "WHERE LINENUM BETWEEN " + (offset<=0?offset:offset+1) + " AND " + (offset + limit) : "";
			sql = permissionRefSQL.toString() + (UtilMethods.isSet(individualPermissionSQL.toString())?" UNION " +individualPermissionSQL.toString():"") +" ) " + limitOffsetSQL + " ORDER BY " + orderByClauseWithAlias ;
		}else if(DbConnectionFactory.isMsSql()){
			limitOffsetSQL = limitResults ? "AS MyDerivedTable WHERE MyDerivedTable.LINENUM BETWEEN " + (offset<=0?offset:offset+1)  + " AND " + (offset + limit) : "";
			sql = permissionRefSQL.toString() + (UtilMethods.isSet(individualPermissionSQL.toString())?" UNION " +individualPermissionSQL.toString():"") +" ) " + limitOffsetSQL + " ORDER BY " + orderByClauseWithAlias;
		}else{
			limitOffsetSQL = limitResults ? " LIMIT " +  limit + " OFFSET " + offset : "";
			sql = permissionRefSQL.toString() + (UtilMethods.isSet(individualPermissionSQL.toString())?" UNION " +individualPermissionSQL.toString():"") +" ) " +  " as t1 ORDER BY "+ orderByClauseWithAlias + " " + limitOffsetSQL;
		}

		dc.setSQL(sql);
		List<Map<String, Object>> results = (ArrayList<Map<String, Object>>)dc.loadResults();
		for (int i = 0; i < results.size(); i++) {
			Map<String, Object> hash = (Map<String, Object>) results.get(i);
			if(!hash.isEmpty()){
				idsToReturn.add((String) hash.get("asset_id"));
			}
		}

		return idsToReturn;
	}


	private enum OrderDir{
		ASC("ASC"),
		DESC("DESC");

		private String value;

		OrderDir (String value) {
			this.value = value;
		}

		public String toString () {
			return value;
		}	

	};


	private static class ColumnItem{

		private String columnName;

		private String tableName;

		private String alias;

		private OrderDir orderDir;

		private boolean isStringColumn;
		
		public void setIsString(boolean isStringColumn){
			this.isStringColumn = isStringColumn;
		}
		
		
		public ColumnItem(String columnName, String tableName, String alias, boolean isStringColumn, OrderDir orderDir){
			this.columnName = columnName;
			this.tableName = tableName;
			this.alias = alias;
			this.isStringColumn=isStringColumn;
			this.orderDir = orderDir!=null?orderDir:OrderDir.ASC;
		}
		
		
		public String getOrderClause(boolean includeTableName){
			String ret = "";
			if(this.isStringColumn){
				ret = "lower("+ (includeTableName?this.tableName+".":"") + this.columnName  +") " + orderDir.toString();
			}else{
				ret = (includeTableName?this.tableName+".":"") + this.columnName +" " + orderDir.toString();
			}
			
			return ret;
		}
		
		public String getAliasOrderClause(){
			String ret = "";
			if(this.isStringColumn){
				ret = "lower("+ (UtilMethods.isSet(this.alias)?this.alias:this.columnName)  +") " + orderDir.toString();
			}else{
				ret = (UtilMethods.isSet(this.alias)?this.alias:this.columnName) +" " + orderDir.toString();
			}
			
			return ret;
		}
		
		public String getSelectClause(boolean includeTableName){
		   String ret = (includeTableName?this.tableName+".":"")+this.columnName + (UtilMethods.isSet(this.alias)?" as " + this.alias:"");
		   return ret;
			
		}


	}

}
