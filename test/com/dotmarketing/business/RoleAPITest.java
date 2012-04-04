package com.dotmarketing.business;

import java.util.ArrayList;
import java.util.List;

import org.apache.cactus.ServletTestCase;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.model.User;

/**
 * 
 * This is the first version of this test class and only contains a few of methods to be tested for dotConnect
 * @author David Torres
 * @author Jason Tesser
 * @version 1.9
 * @since 1.9
 *
 */
public class RoleAPITest extends ServletTestCase {

	List<Role> roles; 
	RoleAPI roleAPI;
	String roleId = "";
	UserAPI uAPI;
	User testUser;
	
	@Override
	protected void setUp() throws Exception {
		try{
			HibernateUtil.startTransaction();
			roleAPI = APILocator.getRoleAPI();
			uAPI = APILocator.getUserAPI();
			roles = new ArrayList<Role>(); 
			APILocator.getRoleAPI();
			
			try{
				testUser = uAPI.createUser("JUNIT TEST", "junitdev@dotcms.org");
				testUser.setFirstName("JUNITFIRSTTESTER");
				testUser.setLastName("JUNITLASTTESTER");
				uAPI.save(testUser, APILocator.getUserAPI().getSystemUser(), false);
			}catch (Exception e) {
				testUser = uAPI.loadUserById("JUNIT TEST", uAPI.getSystemUser(), true);
			}
			
			String roleName = "testrole";
			int count = 1000;
			while(count > 0){
				Role role = new Role();
				role.setDescription(roleName + count +  " description");
				role.setName(roleName + new Integer(count).toString());
				role.setSystem(false);
				try{
					roleAPI.save(role);
				}catch (Exception e) {
					System.out.print("Error saving role with name " + role.getName());
				}
				roleId = role.getId();
				count = count -1;
				roles.add(role);
			}
			roleName = "subrole";
			count = 1000;
			while(count > 0){
				Role role = new Role();
				role.setDescription(roleName + count +  " description");
				role.setName(roleName + count);
				role.setSystem(false);
				role.setParent(roleId);
				try{
					roleAPI.save(role);
				}catch (Exception e) {
					System.out.print("Error saving role with name " + role.getName());
				}
				count = count - 1;
				roles.add(role);
			}
		}catch (Exception e) {
			HibernateUtil.rollbackTransaction();
			throw new Exception(e);
		}finally{
			HibernateUtil.commitTransaction();
		}
	}
	
	@Override
	protected void tearDown() throws Exception {
//		uAPI.delete(testUser, uAPI.getSystemUser(), false);
		DotConnect dc = new DotConnect();
		dc.setSQL("delete from users_cms_roles where user_id like 'junit test'");
		dc.loadResult();
		dc.setSQL("delete from cms_role where role_name like 'subrole%'");
		dc.loadResult();
		dc.setSQL("delete from cms_role where role_name like 'testrole%'");
		dc.loadResult();
	}
	
	public void testLoadRoleById() throws Exception{
		Role r = roleAPI.loadRoleById(roleId);
		assertNotNull("Role is null Not loaded right", r);
		assertNotNull("Role name is null Not loaded right", r.getName());
		assertNotSame("Role Name is empty Not loaded right", r.getName(), "");
	}
	
	public void testAddLoadRolesForUser() throws Exception{
		for(Role role: roles){
			roleAPI.addRoleToUser(role, testUser);
		}
		List<String> urs = roleAPI.findUserIdsForRole(roleAPI.loadRoleById(roleId));
		assertTrue("Userid not set on role",urs.get(0).equals(testUser.getUserId()));
		List<Role> uroles = roleAPI.loadRolesForUser(testUser.getUserId());
		assertTrue("The number of users roles are not 2000", uroles.size() == 2000);
	}
	
	public void testFindRoleByName() throws Exception{
		Role r = roleAPI.findRoleByName("testrole1", null);
		assertNotNull("Role is null Not loaded right it is null", r);
		assertNotNull("Role name is null Not loaded right name is null", r.getName());
		assertEquals("Role Name is empty Not loaded right", r.getName(), "testrole1");
	}
	
	public void testFindRoleByNameFilter() throws Exception{
		List<Role> rs = roleAPI.findRolesByNameFilter("testrole1", 0, 20);
		assertTrue("Didn't return proper number for roles", rs.size() == 20);
	}
	
	public void testFindRootRoles() throws Exception{
		List<Role> rs = roleAPI.findRootRoles();
		assertTrue("Didn't return proper number for roles", rs.size() == 1000);
	}
	
	public void testDeleteRole() throws DotDataException, DotStateException, DotSecurityException{
		Role r = roleAPI.findRoleByName("subrole1", roleAPI.loadRoleById(roleId));
		roleAPI.delete(r);
		r.setId("");
		roleAPI.save(r);
	}
	
	public void testRoleExistsByName() throws DotDataException{
		assertTrue("The role doesn't exist by name : testrole5",roleAPI.roleExistsByName("testrole5", null));
	}
	
	public void testLayoutManagmentOnRole() throws Exception{
		LayoutAPI lAPI = APILocator.getLayoutAPI();
		Layout layout = new Layout();
		layout.setDescription("Test Description");
		layout.setName("Test Name");
		layout.setTabOrder(1);
		lAPI.saveLayout(layout);
		
		Layout layout1 = new Layout();
		layout1.setDescription("Test Description1");
		layout1.setName("Test Name1");
		layout1.setTabOrder(1);
		lAPI.saveLayout(layout1);
		
		Role r = roleAPI.findRoleByName("testrole1", null);
		roleAPI.addLayoutToRole(layout, r);
		roleAPI.addLayoutToRole(layout1, r);
		
		List<String> lIDs = roleAPI.loadLayoutIdsForRole(r);
		assertEquals("Added 2 layouts to role but 2 wasn't returned",lIDs.size(),2);
		
		roleAPI.removeLayoutFromRole(layout1, r);
		lIDs = roleAPI.loadLayoutIdsForRole(r);
		assertEquals("Should only have one layout on role",lIDs.size(),1);
		
		assertEquals("Layout id on role not set right",layout.getId(), lIDs.get(0));
		
		roleAPI.removeLayoutFromRole(layout, r);
		
		lAPI.removeLayout(layout);
		lAPI.removeLayout(layout1);
	}
}
