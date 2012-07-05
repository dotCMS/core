/**
 * 
 */
package com.dotmarketing.business;

import java.lang.reflect.InvocationTargetException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import org.apache.commons.beanutils.BeanUtils;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.AdminLogger;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.VelocityUtil;
import com.liferay.portal.model.User;

/**
 * @author Jason Tesser
 *
 */
public class RoleFactoryImpl extends RoleFactory {

	private RoleCache rc = CacheLocator.getCmsRoleCache(); 
	
	@Override
	protected List<Role> findAllAssignableRoles(boolean showSystemRoles) throws DotDataException {
		HibernateUtil hu = new HibernateUtil(Role.class);
		String query = "from com.dotmarketing.business.Role where edit_permissions = ?";
		if(!showSystemRoles){
			query = query + " and system = ?";
		}
		
		hu.setQuery(query);
		hu.setParam(true);
		if(!showSystemRoles){
			hu.setParam(false);
		}
		return hu.list();
	}
	
	@Override
	protected Role getRoleById(String roleId) throws DotDataException {
		return getRoleById(roleId, true);
	}
	
	protected Role getRoleById(String roleId, boolean translateFQN) throws DotDataException {
		Role r = null;
		r = rc.get(roleId);
		if(r == null){
			HibernateUtil hu = new HibernateUtil(Role.class);
			hu.setQuery("from com.dotmarketing.business.Role where id = ?");
			hu.setParam(roleId);
			r = (Role)hu.load();
			if(r == null)
				return null;
			rc.add(r);
			try {
				if(r != null && InodeUtils.isSet(r.getId())){
					List<Role> roles = new ArrayList<Role>();
					roles.add(r);
					populatChildrenForRoles(roles);
					if(translateFQN) {
						for (Role role : roles) {
							translateFQNFromDB(role);
						}
					}
					r = roles.get(0);
				}else{
					return null;
				}
			} catch (Exception e) {
				Logger.error(this, e.getMessage(), e);
				throw new DotDataException(e.getMessage(), e);
			}
			rc.add(r);
			HibernateUtil.evict(r);
		}
		return r;
	}
	
	@Override
	protected List<Role> loadRolesForUser(String userId) throws DotDataException {
		try {
			List<Role> roles = new ArrayList<Role>();
			List<String> usersRoleIds = rc.getRoleIdsForUser(userId);
			if(usersRoleIds != null){
				for (String roleId : usersRoleIds) {
					Role r = getRoleById(roleId);
					if(r != null){
						roles.add(r);
					}
				}
				return roles;
			}
			StringBuffer buffy = new StringBuffer();
			buffy.append("select {cms_role.*} ");
			buffy.append("from cms_role, users_cms_roles ");
			buffy.append("where users_cms_roles.role_id = cms_role.id ");
			buffy.append("and users_cms_roles.user_id  = ? ");
			HibernateUtil hu = new HibernateUtil(Role.class);
			hu.setSQLQuery(buffy.toString());
			hu.setParam(userId);
			roles = hu.list();
			populatChildrenForRoles(roles);
			for (Role role : roles) {
				if(role == null) continue;
				translateFQNFromDB(role);
				HibernateUtil.evict(role);
				rc.addRoleToUser(userId, role.getId());
				rc.add(role);
			}
			return roles;
		} catch (Exception e) {
			throw new DotDataException(e.getMessage(), e);
		}
	}

	
	@Override
	protected List<Role> getRolesByName(String filter, int start, int limit) throws DotDataException {
		if(filter !=null)filter ="";
		filter = "%" + filter.toLowerCase() + "%";
		return getRolesByNameFiltered(filter, start, limit);
	}
	
	
	@Override
	protected List<Role> getRolesByNameFiltered(String filter, int start, int limit) throws DotDataException {
		HibernateUtil hu = new HibernateUtil(Role.class);
		hu.setQuery("from " + Role.class.getName() + " where lower(role_name) like ? order by role_name");
		if(filter ==null)filter ="";
		filter = filter.toLowerCase();
		hu.setParam(filter);
		hu.setFirstResult(start);
		hu.setMaxResults(limit);
		List<Role> roles = (List<Role>)hu.list();
		try {
			populatChildrenForRoles(roles);
			for (Role role : roles) {
				HibernateUtil.evict(role);
				translateFQNFromDB(role);
			}
		} catch (Exception e) {
			Logger.error(this, e.getMessage(), e);
			throw new DotDataException(e.getMessage(), e);
		}
		if(roles != null){
			for (Role role : roles) {
				rc.add(role);
			}
		}
		return roles;
	}
	
	@Override
	protected Role findRoleByName(String rolename, Role parent) throws DotDataException {
		HibernateUtil hu = new HibernateUtil(Role.class);
		if(parent == null){
			hu.setQuery("from " + Role.class.getName() + " as r where r.name = ? and r.parent = r.id");
		}else{
			hu.setQuery("from " + Role.class.getName() + " as r where r.name = ? and r.parent = ? and r.parent <> r.id");
		}
		hu.setParam(rolename);
		if(parent != null){
			hu.setParam(parent.getId());
		}
		List<Role> roles = (List<Role>)hu.list();
		try {
			populatChildrenForRoles(roles);
			for (Role role : roles) {
				translateFQNFromDB(role);
			}
		} catch (Exception e) {
			Logger.error(this, e.getMessage(), e);
			throw new DotDataException(e.getMessage(), e);
		}
		if(roles != null){
			for (Role role : roles) {
				HibernateUtil.evict(role);
				rc.add(role);
			}
		}
		Role role = null;
		if(roles != null && roles.size()>0){
			role = roles.get(0);
			if(roles.size()>1){
				Logger.error(this, "Found more then one role with the same name : " + rolename != null ? rolename : "");
			}
		}
		return role;
	}
	
	@Override
	protected void addRoleToUser(Role role, User user) throws DotDataException {
		UsersRoles ur = new UsersRoles();
		ur.setRoleId(role.getId());
		ur.setUserId(user.getUserId());
		HibernateUtil.save(ur);
		rc.addRoleToUser(user.getUserId(), role.getId());
	}
	
	@Override
	protected void removeRoleFromUser(Role role, User user)	throws DotDataException {
		DotConnect dc = new DotConnect();
		dc.setSQL("delete from users_cms_roles where user_id like ? and role_id like ?");
		dc.addParam(user.getUserId());
		dc.addParam(role.getId());
		dc.loadResult();
		if(UtilMethods.isSet(role.getRoleKey())&& role.getRoleKey().equalsIgnoreCase(user.getUserId())){
			dc.setSQL("delete from permission where roleid like ?");
			dc.addParam(role.getId());
			dc.loadResult();
			dc.setSQL("delete from cms_role where role_key like ?");
			dc.addParam(user.getUserId());
			dc.loadResult();
			CacheLocator.getPermissionCache().clearCache();
		}
		rc.remove(user.getUserId());
	}
	
	@Override
	protected Role save(Role role) throws DotDataException {
//		role.setRoleKey(UUIDGenerator.generateUuid());
		HibernateUtil hu = new HibernateUtil(Role.class);

		Role r = null;
		if(InodeUtils.isSet(role.getId())) {
			hu.setQuery("from com.dotmarketing.business.Role where id = ?");
			hu.setParam(role.getId());
			r = (Role)hu.load();
			rc.remove(r.getId());
			if(UtilMethods.isSet(r.getParent())) {
				rc.remove(r.getParent());
			}
			try {
				BeanUtils.copyProperties(r, role);
			} catch (IllegalAccessException e) {
				Logger.error(this, "Error populating role to save", e);
				throw new DotDataException("Error populating role to save", e);
			} catch (InvocationTargetException e) {
				Logger.error(this, "Error populating role to save", e);
				throw new DotDataException("Error populating role to save", e);
			}
		} else {
			
			r = role;
			if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MSSQL)){
				String roleKey= VelocityUtil.convertToVelocityVariable(r.getName());
				DotConnect dc = new DotConnect();
				dc.setSQL("select count(*) as total from cms_role where role_key =?");
				dc.addParam(roleKey);
				int total= dc.getInt("total");
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd H:mm:ss.S");
				String date= sdf.format(new java.util.Date());
				if(total>0){
					
					roleKey= UtilMethods.toCamelCase(r.getName())+date;
				}
				r.setRoleKey(roleKey);
				r.setFQN(UUIDGenerator.generateUuid());
			}
			HibernateUtil.save(r);
		}
		
		//We need to update the role FQN and well as the role children, grand-children, etc as well
		List<Role> rolesToUpdate = new ArrayList<Role>();
		Queue<String> roleIdsToProcess = new LinkedList<String>();
		roleIdsToProcess.add(r.getId());
		while(roleIdsToProcess.size()>0) {
			String parentId = roleIdsToProcess.poll();
			hu = new HibernateUtil(Role.class);
			hu.setQuery("from com.dotmarketing.business.Role where id = ?");
			hu.setParam(parentId);
			Role parentRole = (Role)hu.load();
			rolesToUpdate.add(parentRole);
			List<Role> toPopulate = new ArrayList<Role>();
			toPopulate.add(parentRole);
			try {
				populatChildrenForRoles(toPopulate);
			} catch (Exception e) {
				throw new DotDataException(e.getMessage(), e);
			}
			if(parentRole.getRoleChildren() != null)
				roleIdsToProcess.addAll(parentRole.getRoleChildren());
		}
		
		for(Role roleToUpdate : rolesToUpdate) {
			setFQNForDB(roleToUpdate);
			HibernateUtil.save(roleToUpdate);
		}
		
		translateFQNFromDB(r);
				
		if(r.getParent() != null){
			rc.remove(r.getParent());
			Role parent = getRoleById(r.getParent());
			rc.remove(parent.getRoleKey());
		}
		
		List<Role> singleRole = new ArrayList<Role>();
		singleRole.add(r);
		try {
			populatChildrenForRoles(singleRole);
		} catch (Exception e) {
			Logger.error(this, "Error populating role children", e);
			throw new DotDataException("Error populating role children", e);
		}
		rc.add(r);
		HibernateUtil.evict(r);
		AdminLogger.log(RoleFactoryImpl.class, "save", "Role saved Id :"+r.getId());
		
		return r;
	}
	
	@Override
	protected void delete(Role role) throws DotDataException {
		DotConnect dc = new DotConnect();
		dc.setSQL("delete from users_cms_roles where role_id = ?");
		dc.addParam(role.getId());
		dc.loadResult();
		HibernateUtil hu = new HibernateUtil(Role.class);
		hu.setQuery("from com.dotmarketing.business.Role where id = ?");
		hu.setParam(role.getId());
		Role r = (Role)hu.load();
		HibernateUtil.delete(r);
		rc.remove(r.getId());
		rc.remove(r.getRoleKey());
		rc.clearRoleCache();
		
		AdminLogger.log(RoleFactoryImpl.class, "delete", "Role deleted Id :"+r.getId());
		
	}
	
	@Override
	protected List<Role> findRootRoles() throws DotDataException {
		HibernateUtil hu = new HibernateUtil(Role.class);
		hu.setQuery("from " + Role.class.getName() + " where parent = id order by role_name");
		List<Role> roles = (List<Role>)hu.list();
		try {
			populatChildrenForRoles(roles);
			for (Role role : roles) {
				translateFQNFromDB(role);	
			}
		} catch (Exception e) {
			Logger.error(this, e.getMessage(), e);
			throw new DotDataException(e.getMessage(), e);
		}
		if(roles != null){
			for (Role role : roles) {
				HibernateUtil.evict(role);
				rc.add(role);
			}
		}
		return roles;
	}
	
	@Override
	protected List<String> findUserIdsForRole(Role role) throws DotDataException {
		HibernateUtil hu = new HibernateUtil(Role.class);
		hu.setQuery("from " + UsersRoles.class.getName() + " where role_id = ?");
		hu.setParam(role.getId());
		List<UsersRoles> urs = (List<UsersRoles>)hu.list();
		List<String> ret = null;
		if(urs != null){
			ret = new ArrayList<String>();
			for (UsersRoles ur : urs) {
				ret.add(ur.getUserId());
			}
		}
		return ret;
	}
	
	protected void fillChildrensRecursive(List<String> list, List<String> ids) throws DotDataException {
	    DotConnect dc=new DotConnect();
	    StringBuilder sb=new StringBuilder();
	    sb.append("SELECT id FROM cms_role WHERE parent in (");
	    boolean first=true;
	    for(String id : ids) { 
	        if(first) first=false;
	        else sb.append(',');
	        sb.append('\'').append(id).append('\'');
	    }
	    sb.append(") AND parent<>id");
	    dc.setSQL(sb.toString());
	    List<Map<String,String>> childs=dc.loadResults();
	    List<String> newchilds=new ArrayList<String>();
	    for(Map<String,String> cc : childs) {
	        final String cid=cc.get("id");
	        if(!list.contains(cid)) {
	            list.add(cid); newchilds.add(cid);
	        }
	    }
	    if(newchilds.size()>0)
	        fillChildrensRecursive(list, newchilds);
	}
	
	@Override
	protected boolean doesUserHaveRole(User user, Role role) throws DotDataException {
		
		if(user == null || role ==null) {
			Logger.debug(this, "User or Role was Null");
			return false;
		}
		
		List<String> roles = rc.getRoleIdsForUser(user.getUserId());
		if(roles == null){
		    List<Role> rolelist=loadRolesForUser(user.getUserId());
		    roles=new ArrayList<String>();
		    List<String> rolesToRecurse=new ArrayList<String>();
		    for(Role rr : rolelist) {
		        roles.add(rr.getId());
		        if(rr.getRoleChildren()!=null)
		            rolesToRecurse.addAll(rr.getRoleChildren());
		    }
		    roles.addAll(rolesToRecurse);
		    if(!rolesToRecurse.isEmpty())
		        fillChildrensRecursive(roles, rolesToRecurse);
		    rc.addRoleListForUser(roles, user.getUserId());
		}
		if(roles != null && roles.contains(role.getId())){
			return true;
		}else{
			Logger.debug(this,"User ("+user.getUserId()+") does not have the role ("+role.getId()+")");
			Logger.debug(this, "User ("+user.getUserId()+") has roles: "+roles);
			return false;
		}
	}
	
	@Override
	protected List<String> loadLayoutIdsForRole(Role role) throws DotDataException {
		List<String> layouts = rc.getLayoutsForRole(role.getId());
		if(layouts == null){
			layouts = new ArrayList<String>();
			HibernateUtil hu = new HibernateUtil(Role.class);
			hu.setQuery("from " + LayoutsRoles.class.getName() + " where role_id = ?");
			hu.setParam(role.getId());
			List<LayoutsRoles> urs = (List<LayoutsRoles>)hu.list();
			if(urs != null){
				for (LayoutsRoles ur : urs) {
					layouts.add(ur.getLayoutId());
				}
			}
			rc.addLayoutsToRole(layouts, role.getId());
		}
		return layouts;
	}
	
	@Override
	protected void addLayoutToRole(Layout layout, Role role) throws DotDataException {
		LayoutsRoles lr = new LayoutsRoles();
		lr.setLayoutId(layout.getId());
		lr.setRoleId(role.getId());
		HibernateUtil.save(lr);
		rc.removeLayoutsOnRole(role.getId());
	}
	
	@Override
	protected void removeLayoutFromRole(Layout layout, Role role) throws DotDataException {
		DotConnect dc = new DotConnect();
		dc.setSQL("delete from layouts_cms_roles where role_id = ? and layout_id = ?");
		dc.addParam(role.getId());
		dc.addParam(layout.getId());
		dc.loadResult();
		rc.removeLayoutsOnRole(role.getId());
	}
	
	@Override
	protected Role findRoleByFQN(String FQN) throws DotDataException {
		if(FQN == null){
			throw new DotDataException("FQN is null");
		}
		Role r = null;
		if(!FQN.contains("-->")){
			r = findRoleByName(FQN, null);
		}else{
			Role parent = null;
			String rFQN = "";
			String[] rids = FQN.split(" --> ");
			String parentId = null;
			for (String rid : rids) {
				if(parentId == null){
					parent = findRoleByName(rid, null);
					parentId = parent.getId();
					rFQN = parent.getId();
				}else{
					parent = findRoleByName(rid, parent);
					parentId = parent.getId();
					rFQN = rFQN + " --> " + parentId;
				}
			}
			
			HibernateUtil hu = new HibernateUtil(Role.class);
			hu.setQuery("from " + Role.class.getName() + " where db_fqn like ?");
			hu.setParam(rFQN);
			r = (Role)hu.load();
			translateFQNFromDB(r);
			rc.add(r);
			HibernateUtil.evict(r);
		}
		return r;
	}
	
	@Override
	protected Role loadRoleByKey(String key) throws DotDataException {
		Role r = null;
		r = rc.get(key);
		if(r == null){
			HibernateUtil hu = new HibernateUtil(Role.class);
			hu.setQuery("from com.dotmarketing.business.Role where role_key = ?");
			hu.setParam(key);
			r = (Role)hu.load();
			try {
				if(r != null && InodeUtils.isSet(r.getId())){
					List<Role> roles = new ArrayList<Role>();
					roles.add(r);
					populatChildrenForRoles(roles);
					for (Role role : roles) {
						translateFQNFromDB(role);
					}
					r = roles.get(0);
				}else{
					return null;
				}
			} catch (Exception e) {
				Logger.error(this, e.getMessage(), e);
				throw new DotDataException(e.getMessage(), e);
			}
			rc.add(r);
			HibernateUtil.evict(r);
		}
		return r;
	}
	
	private void populatChildrenForRoles(List<Role> roles) throws Exception{
		Map<String,Role> roleMap = UtilMethods.convertListToHashMap(roles, "getId", String.class);
		String sql = "select cr1.id as childId, cr2.id as parentId  from cms_role cr1, cms_role cr2 where cr1.parent in (:param1) and cr1.parent = cr2.id " +
				"and cr1.parent != cr1.id";
		DotConnect dc = new DotConnect();
		List<Map<String,String>> sqlResults = new ArrayList<Map<String,String>>();
		String ids = "";
		int count = 0;
		for (Role role : roles) {
			if(role ==null) continue;
			if(count > 200){
				dc.setSQL(sql.replace(":param1", ids));
				sqlResults = dc.loadResults();
				populatChildrenForRolesHelper(roleMap,sqlResults);
				count = 0;
				ids = "";
			}
			if(ids.length() < 1){
				ids += "'" + role.getId() + "'";
			}else{
				ids += ", '" + role.getId() + "'";
			}
			count++;
		}
		if(ids.length() > 0){
			dc.setSQL(sql.replace(":param1", ids));
			sqlResults = dc.loadResults();
			populatChildrenForRolesHelper(roleMap,sqlResults);
		}
		
	}
	
	private void populatChildrenForRolesHelper(Map<String,Role> roleMap, List<Map<String,String>> sqlResults){
		for (Map<String, String> row : sqlResults) {
			List<String> childrenList = roleMap.get(row.get("parentid")) != null?
					roleMap.get(row.get("parentid")).getRoleChildren(): null;
			if(childrenList == null){
				childrenList = new ArrayList<String>();
			}
			if(!childrenList.contains(row.get("childid")))
				childrenList.add(row.get("childid"));
			if(roleMap.get(row.get("parentid")) != null)
				roleMap.get(row.get("parentid")).setRoleChildren(childrenList);
		}
	}
	
	private void setFQNForDB(Role role) throws DotDataException{
		if(role.getParent().equals(role.getId())){
			role.setDBFQN(role.getId());
		}else{
			String fqn = role.getId();
			Role current = role;
			do{
				Role parent = getRoleById(current.getParent(), false);
				fqn = parent.getId() + " --> "+ fqn;
				current = parent;
			} while (!current.getParent().equals(current.getId()));
			role.setDBFQN(fqn);
		}
	}
	
	private void translateFQNFromDB(Role role) throws DotDataException{
		String fqn = role.getDBFQN();
		if(!fqn.contains("-->")){
			role.setFQN(role.getName());
		}else{
			String[] rids = fqn.split(" --> ");
			boolean first = true;
			for (String rid : rids) {
				if(first){
					if(!rid.equals(role.getId()))
						fqn = getRoleById(rid).getName();
					else
						fqn = role.getName();
				}else{
					if(!rid.equals(role.getId()))
						fqn += " --> " + getRoleById(rid).getName();
					else
						fqn += " --> " + role.getName();
				}
				first = false;
			}
			role.setFQN(fqn);
		}
	}

	@Override
	protected Role addUserRole(User user) throws DotDataException {
		Role parent = loadRoleByKey(RoleAPI.USERS_ROOT_ROLE_KEY);

		DotConnect dc = new DotConnect();
		dc.setSQL("insert into cms_role (id, role_name, description, role_key, db_fqn, parent, edit_permissions, edit_users, edit_layouts, locked, system) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
		String roleUUID = UUIDGenerator.generateUuid();
		dc.addParam(roleUUID);
		dc.addParam(user.getFullName());
		dc.addParam("");
		dc.addParam(user.getUserId());
		dc.addParam(parent.getId() + " --> " + roleUUID);
		dc.addParam(parent.getId());
		dc.addParam(true);
		dc.addParam(false);
		dc.addParam(true);
		dc.addParam(false);
		dc.addParam(true);
		dc.loadResult();

		dc.setSQL("insert into users_cms_roles (id, user_id, role_id) values (?, ?, ?)");
		String uuid = UUIDGenerator.generateUuid();
		dc.addParam(uuid);
		dc.addParam(user.getUserId());
		dc.addParam(roleUUID);
		dc.loadResult();
		
		rc.remove(user.getUserId());
		
		return loadRoleByKey(user.getUserId());

	}
	
}
