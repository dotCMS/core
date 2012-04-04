package com.dotmarketing.business;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.cactus.ServletTestCase;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.portlets.contentlet.business.Contentlet;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.factories.FolderFactory;
import com.dotmarketing.portlets.folders.model.Folder;
import com.liferay.portal.model.User;

/**
 * 
 * This is the first version of this test class and only contains a few of methods to be tested for dotConnect
 * @author David Torres
 * @version 1.8
 * @since 1.8
 *
 */
public class PermissionAPITest extends ServletTestCase {

	Host hostToTest;
	Folder folder1ToTest;
	Folder folder2ToTest;
	User systemUser;
	User testUser;
	
	Role testRole;
	Role lockedRole;
	Role cmsOwner;
	Role cmsAdminRole;

	HostAPI hostAPI;
	FolderAPI folderAPI;
	RoleAPI roleAPI;
	UserAPI userAPI;
	PermissionAPI perAPI;
	
	@Override
	public void setUp () throws DotDataException, DotSecurityException {

		hostAPI = APILocator.getHostAPI();
		folderAPI = APILocator.getFolderAPI();
		roleAPI = APILocator.getRoleAPI();
		userAPI = APILocator.getUserAPI();
		perAPI = APILocator.getPermissionAPI();

		testUser = userAPI.createUser(null, "unittest@dotcms.org");
		
		cmsOwner = roleAPI.loadCMSOwnerRole();
		cmsAdminRole = roleAPI.loadCMSAdminRole();
		systemUser = userAPI.getSystemUser();
		testRole = new Role();
		testRole.setName("test role");
		testRole = roleAPI.save(testRole);
		lockedRole = new Role();
		lockedRole.setName("locked test role");
		lockedRole = roleAPI.save(lockedRole);
		hostToTest = hostAPI.findDefaultHost(systemUser, false);

		roleAPI.addRoleToUser(testRole, testUser);
		
		folder1ToTest = new Folder();
		folder1ToTest.setName("test_folder");
		folder1ToTest.setOwner(systemUser.getUserId());
		folder1ToTest.setTitle("Test Folder");
		//folder1ToTest.setPath("/test_folder");
		folder1ToTest.setHostId(hostToTest.getIdentifier());
		
		folderAPI.save(folder1ToTest,userAPI.getSystemUser(),false);

		folder2ToTest = new Folder();
		folder2ToTest.setName("test_folder2");
		//folder2ToTest.setPath("/test_folder/test_folder2");
		folder2ToTest.setOwner(testUser.getUserId());
		folder2ToTest.setTitle("Test Folder 2");
		folder2ToTest.setHostId(hostToTest.getIdentifier());
		
		folderAPI.save(folder2ToTest,userAPI.getSystemUser(),false);
	}
	
	@Override
	protected void tearDown() throws Exception {
		perAPI.removePermissions(folder1ToTest);
		InodeFactory.deleteInode(folder1ToTest);
		perAPI.removePermissions(folder2ToTest);
		InodeFactory.deleteInode(folder2ToTest);
		roleAPI.delete(testRole);
		roleAPI.delete(lockedRole);
		userAPI.delete(testUser, systemUser, false);
	}
	
	public void testSave() throws DotDataException,	DotSecurityException {
		
		Permission p1 = new Permission(folder1ToTest.getInode(), testRole.getId(), PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_WRITE | PermissionAPI.PERMISSION_CAN_ADD_CHILDREN | PermissionAPI.PERMISSION_EDIT_PERMISSIONS, true);
		perAPI.save(p1, folder1ToTest, systemUser, false);

		List<Permission> permissions = perAPI.getPermissions(folder1ToTest, true);
		
		assertEquals(permissions.size(), 1);
		Permission permissionSaved = permissions.get(0);
		assertEquals(folder1ToTest.getInode(), permissionSaved.getInode());
		assertEquals(testRole.getId(), permissionSaved.getRoleId());
		assertEquals(PermissionAPI.INDIVIDUAL_PERMISSION_TYPE, permissionSaved.getType());
		assertEquals(PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_WRITE | 
				PermissionAPI.PERMISSION_CAN_ADD_CHILDREN | PermissionAPI.PERMISSION_EDIT_PERMISSIONS, 
				permissionSaved.getPermission());
		
		p1 = new Permission(Contentlet.class.getCanonicalName(), folder1ToTest.getInode(), testRole.getId(), PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_WRITE | PermissionAPI.PERMISSION_CAN_ADD_CHILDREN | PermissionAPI.PERMISSION_EDIT_PERMISSIONS, true);
		perAPI.save(p1, folder1ToTest, systemUser, false);
		
		permissions = perAPI.getInheritablePermissions(folder1ToTest, true);
		
		assertEquals(permissions.size(), 1);
		permissionSaved = permissions.get(0);
		assertEquals(folder1ToTest.getInode(), permissionSaved.getInode());
		assertEquals(testRole.getId(), permissionSaved.getRoleId());
		assertEquals(Contentlet.class.getCanonicalName(), permissionSaved.getType());
		assertEquals(PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_WRITE | 
				PermissionAPI.PERMISSION_CAN_ADD_CHILDREN | PermissionAPI.PERMISSION_EDIT_PERMISSIONS, 
				permissionSaved.getPermission());

	}

	public void testCopyPermissions() throws DotDataException, DotSecurityException {

		Permission p1 = new Permission(folder1ToTest.getInode(), testRole.getId(), PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_WRITE | PermissionAPI.PERMISSION_CAN_ADD_CHILDREN | PermissionAPI.PERMISSION_EDIT_PERMISSIONS, true);
		perAPI.save(p1, folder1ToTest, systemUser, false);
		
		perAPI.copyPermissions(folder1ToTest, folder2ToTest);
		
		List<Permission> destFolderPermissions = perAPI.getPermissions(folder2ToTest, true, true);
		
		assertEquals(destFolderPermissions.size(), 1);
		Permission permCopied = destFolderPermissions.get(0);
		assertEquals(folder2ToTest.getInode(), permCopied.getInode());
		assertEquals(testRole.getId(), permCopied.getRoleId());
		assertEquals(PermissionAPI.INDIVIDUAL_PERMISSION_TYPE, permCopied.getType());
		assertEquals(PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_WRITE | 
				PermissionAPI.PERMISSION_CAN_ADD_CHILDREN | PermissionAPI.PERMISSION_EDIT_PERMISSIONS, 
				permCopied.getPermission());
		
		perAPI.removePermissions(folder1ToTest);
		
		//Copying inheritable permissions
		p1 = new Permission(Contentlet.class.getCanonicalName(), folder1ToTest.getInode(), testRole.getId(), PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_WRITE | PermissionAPI.PERMISSION_CAN_ADD_CHILDREN | PermissionAPI.PERMISSION_EDIT_PERMISSIONS, true);
		perAPI.save(p1, folder1ToTest, systemUser, false);
		
		perAPI.copyPermissions(folder1ToTest, folder2ToTest);
		
		destFolderPermissions = perAPI.getInheritablePermissions(folder2ToTest, true);
		
		assertEquals(destFolderPermissions.size(), 1);
		permCopied = destFolderPermissions.get(0);
		assertEquals(folder2ToTest.getInode(), permCopied.getInode());
		assertEquals(testRole.getId(), permCopied.getRoleId());
		assertEquals(Contentlet.class.getCanonicalName(), permCopied.getType());
		assertEquals(PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_WRITE | 
				PermissionAPI.PERMISSION_CAN_ADD_CHILDREN | PermissionAPI.PERMISSION_EDIT_PERMISSIONS, 
				permCopied.getPermission());

	}

	public void testDoesRoleHavePermission() throws DotDataException, DotSecurityException {
		Permission p1 = new Permission(folder1ToTest.getInode(), testRole.getId(), PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_WRITE | PermissionAPI.PERMISSION_CAN_ADD_CHILDREN | PermissionAPI.PERMISSION_EDIT_PERMISSIONS, true);
		perAPI.save(p1, folder1ToTest, systemUser, false);
		assertTrue(perAPI.doesRoleHavePermission(folder1ToTest, PermissionAPI.PERMISSION_CAN_ADD_CHILDREN, testRole));
		assertFalse(perAPI.doesRoleHavePermission(folder1ToTest, PermissionAPI.PERMISSION_PUBLISH, testRole));
		//Testing through inheritance
		assertFalse(perAPI.doesRoleHavePermission(folder2ToTest, PermissionAPI.PERMISSION_WRITE, testRole));
		p1 = new Permission(Folder.class.getCanonicalName(), folder1ToTest.getInode(), testRole.getId(), PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_WRITE, true);
		perAPI.save(p1, folder1ToTest, systemUser, false);
		assertTrue(perAPI.doesRoleHavePermission(folder2ToTest, PermissionAPI.PERMISSION_WRITE, testRole));
	}


	public void testDoesUserHavePermission() throws DotDataException, DotSecurityException {
		Permission p1 = new Permission(folder1ToTest.getInode(), testRole.getId(), PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_WRITE | PermissionAPI.PERMISSION_CAN_ADD_CHILDREN | PermissionAPI.PERMISSION_EDIT_PERMISSIONS, true);
		perAPI.save(p1, folder1ToTest, systemUser, false);
		assertTrue(perAPI.doesUserHavePermission(folder1ToTest, PermissionAPI.PERMISSION_CAN_ADD_CHILDREN, testUser));
		assertFalse(perAPI.doesUserHavePermission(folder1ToTest, PermissionAPI.PERMISSION_PUBLISH, testUser));
		//Testing through inheritance
		assertFalse(perAPI.doesUserHavePermission(folder2ToTest, PermissionAPI.PERMISSION_WRITE, testUser));
		p1 = new Permission(Folder.class.getCanonicalName(), folder1ToTest.getInode(), testRole.getId(), PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_WRITE, true);
		perAPI.save(p1, folder1ToTest, systemUser, false);
		assertTrue(perAPI.doesUserHavePermission(folder2ToTest, PermissionAPI.PERMISSION_WRITE, testUser));
	}

	public void testDoesUserOwn() throws DotDataException {
		assertFalse(perAPI.doesUserOwn(folder1ToTest, testUser));
		assertTrue(perAPI.doesUserOwn(folder2ToTest, testUser));
	}

	public void testFilterCollection() throws DotDataException,	DotSecurityException {
		
		List<Permissionable> permissionables = new ArrayList<Permissionable>();
		permissionables.add(folder1ToTest);
		permissionables.add(folder2ToTest);
		
		Permission p1 = new Permission(folder1ToTest.getInode(), testRole.getId(), PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_WRITE | PermissionAPI.PERMISSION_CAN_ADD_CHILDREN | PermissionAPI.PERMISSION_EDIT_PERMISSIONS, true);
		perAPI.save(p1, folder1ToTest, systemUser, false);
		
		List<Permissionable> resultList = perAPI.filterCollection(permissionables, PermissionAPI.PERMISSION_READ, false, testUser);
		assertEquals(1, resultList.size());
		assertEquals(folder1ToTest.getInode(), resultList.get(0).getPermissionId());

		p1 = new Permission(Folder.class.getCanonicalName(), folder1ToTest.getInode(), testRole.getId(), PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_WRITE, true);
		perAPI.save(p1, folder1ToTest, systemUser, false);

		resultList = perAPI.filterCollection(permissionables, PermissionAPI.PERMISSION_READ, false, testUser);
		
		assertEquals(2, resultList.size());
		assertTrue(resultList.contains(folder1ToTest));
		assertTrue(resultList.contains(folder2ToTest));
		
	}

	public void testGetInheritablePermissions() throws DotDataException, DotSecurityException {

		Permission p1 = new Permission(folder1ToTest.getInode(), testRole.getId(), PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_WRITE | PermissionAPI.PERMISSION_CAN_ADD_CHILDREN | PermissionAPI.PERMISSION_EDIT_PERMISSIONS, true);
		perAPI.save(p1, folder1ToTest, systemUser, false);

		List<Permission> list = perAPI.getInheritablePermissions(folder1ToTest, true);
		assertEquals(0, list.size());

		list = perAPI.getInheritablePermissions(folder1ToTest);
		assertEquals(0, list.size());

		p1 = new Permission(Folder.class.getCanonicalName(), folder1ToTest.getInode(), testRole.getId(), PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_WRITE, true);
		perAPI.save(p1, folder1ToTest, systemUser, false);

		list = perAPI.getInheritablePermissions(folder1ToTest, true);
		assertEquals(1, list.size());
		assertTrue(list.get(0).getType().equals(Folder.class.getCanonicalName()));

		list = perAPI.getInheritablePermissions(folder1ToTest);
		assertEquals(2, list.size());
		assertTrue(list.get(0).getType().equals(Folder.class.getCanonicalName()));
		assertTrue(list.get(0).getInode().equals(folder1ToTest.getInode()));
		assertTrue(list.get(1).getType().equals(Folder.class.getCanonicalName()));
		assertTrue(list.get(1).getInode().equals(folder1ToTest.getInode()));

	}
	
	public void testGetPermissionIdsFromRoles() throws DotDataException, DotSecurityException {
		
		List<Integer> ids = perAPI.getPermissionIdsFromRoles(folder1ToTest, new Role[] { testRole }, null);
		assertEquals(0, ids.size());
		
		Permission p1 = new Permission(folder1ToTest.getInode(), testRole.getId(), PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_WRITE | PermissionAPI.PERMISSION_CAN_ADD_CHILDREN | PermissionAPI.PERMISSION_EDIT_PERMISSIONS, true);
		perAPI.save(p1, folder1ToTest, systemUser, false);

		ids = perAPI.getPermissionIdsFromRoles(folder1ToTest, new Role[] { testRole }, null);
		assertEquals(4, ids.size());
		assertTrue(ids.contains(PermissionAPI.PERMISSION_READ));
		assertTrue(ids.contains(PermissionAPI.PERMISSION_WRITE));
		assertTrue(ids.contains(PermissionAPI.PERMISSION_CAN_ADD_CHILDREN));
		assertTrue(ids.contains(PermissionAPI.PERMISSION_EDIT_PERMISSIONS));

		ids = perAPI.getPermissionIdsFromRoles(folder2ToTest, new Role[] { testRole }, null);
		assertEquals(0, ids.size());

		p1 = new Permission(Folder.class.getCanonicalName(), folder1ToTest.getInode(), testRole.getId(), PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_WRITE, true);
		perAPI.save(p1, folder1ToTest, systemUser, false);

		ids = perAPI.getPermissionIdsFromRoles(folder2ToTest, new Role[] { testRole }, null);
		assertEquals(2, ids.size());
		assertTrue(ids.contains(PermissionAPI.PERMISSION_READ));
		assertTrue(ids.contains(PermissionAPI.PERMISSION_WRITE));

		//Testing cms owner perms
		p1 = new Permission(Folder.class.getCanonicalName(), folder1ToTest.getInode(), cmsOwner.getId(), PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_WRITE, true);
		perAPI.save(p1, folder1ToTest, systemUser, false);

		ids = perAPI.getPermissionIdsFromRoles(folder2ToTest, new Role[] { }, testUser);
		assertEquals(2, ids.size());
		assertTrue(ids.contains(PermissionAPI.PERMISSION_READ));
		assertTrue(ids.contains(PermissionAPI.PERMISSION_WRITE));

	}

	public void testGetPermissionTypes() {
		Map<String, Integer> types = perAPI.getPermissionTypes();
		assertNotNull(types.get("PERMISSION_READ"));
		assertNotNull(types.get("PERMISSION_USE"));
		assertNotNull(types.get("PERMISSION_WRITE"));
		assertNotNull(types.get("PERMISSION_EDIT"));
		assertNotNull(types.get("PERMISSION_PUBLISH"));
		assertNotNull(types.get("PERMISSION_EDIT_PERMISSIONS"));
		assertNotNull(types.get("PERMISSION_CAN_ADD_CHILDREN"));
		assertNotNull(types.get("PERMISSION_CREATE_VIRTUAL_LINKS"));
		assertEquals(new Integer(1), (Integer)types.get("PERMISSION_READ"));
		assertEquals(new Integer(1), (Integer)types.get("PERMISSION_USE"));
		assertEquals(new Integer(2), (Integer)types.get("PERMISSION_WRITE"));
		assertEquals(new Integer(2), (Integer)types.get("PERMISSION_EDIT"));
		assertEquals(new Integer(4), (Integer)types.get("PERMISSION_PUBLISH"));
		assertEquals(new Integer(8), (Integer)types.get("PERMISSION_EDIT_PERMISSIONS"));
		assertEquals(new Integer(16), (Integer)types.get("PERMISSION_CAN_ADD_CHILDREN"));
		assertEquals(new Integer(32), (Integer)types.get("PERMISSION_CREATE_VIRTUAL_LINKS"));
	}

	public void testGetPermissions() throws DotDataException, DotSecurityException {
		
		List<Permission> perms = perAPI.getPermissions(folder1ToTest, true, true);
		assertEquals(0, perms.size());
		
		Permission p1 = new Permission(folder1ToTest.getInode(), testRole.getId(), PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_WRITE | PermissionAPI.PERMISSION_CAN_ADD_CHILDREN | PermissionAPI.PERMISSION_EDIT_PERMISSIONS, true);
		perAPI.save(p1, folder1ToTest, systemUser, false);


		perms = perAPI.getPermissions(folder1ToTest, true);
		assertEquals(1, perms.size());
		assertEquals(p1.getId(), perms.get(0).getId());
		assertEquals(PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_WRITE | PermissionAPI.PERMISSION_CAN_ADD_CHILDREN | PermissionAPI.PERMISSION_EDIT_PERMISSIONS, perms.get(0).getPermission());
		assertEquals(PermissionAPI.INDIVIDUAL_PERMISSION_TYPE, p1.getType());
		
		//Testing through inheritance
		Permission p2 = new Permission(Folder.class.getCanonicalName(), folder1ToTest.getInode(), testRole.getId(), PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_WRITE, true);
		perAPI.save(p2, folder1ToTest, systemUser, false);

		perms = perAPI.getPermissions(folder2ToTest, true);
		assertEquals(1, perms.size());
		assertEquals(p2.getId(), perms.get(0).getId());
		assertEquals(PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_WRITE, perms.get(0).getPermission());
		assertEquals(Folder.class.getCanonicalName(), perms.get(0).getType());

		//Testing fixed role permissions
		Permission p3 = new Permission(Folder.class.getCanonicalName(), folder1ToTest.getInode(), lockedRole.getId(), PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_WRITE | PermissionAPI.PERMISSION_CAN_ADD_CHILDREN, true);
		perAPI.save(p3, folder1ToTest, systemUser, false);
		perms = perAPI.getPermissions(folder2ToTest, true);
		assertEquals(2, perms.size());
		assertTrue(perms.contains(p2));
		assertTrue(perms.contains(p3));
		
	}

	public void testGetPermissionsByRole() throws DotDataException, DotSecurityException {
		
		List<Permission> permissions = perAPI.getPermissionsByRole(testRole, false);
		assertEquals(0, permissions.size());
		
		Permission p1 = new Permission(folder1ToTest.getInode(), testRole.getId(), PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_WRITE | PermissionAPI.PERMISSION_CAN_ADD_CHILDREN | PermissionAPI.PERMISSION_EDIT_PERMISSIONS, true);
		perAPI.save(p1, folder1ToTest, systemUser, false);

		permissions = perAPI.getPermissionsByRole(testRole, false, true);
		assertEquals(1, permissions.size());
		assertEquals(p1.getId(), permissions.get(0).getId());

	}

	public void testGetRoles() throws DotDataException, DotSecurityException {
		
		Permission p1 = new Permission(folder1ToTest.getInode(), testRole.getId(), PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_WRITE | PermissionAPI.PERMISSION_CAN_ADD_CHILDREN | PermissionAPI.PERMISSION_EDIT_PERMISSIONS, true);
		perAPI.save(p1, folder1ToTest, systemUser, false);

		Set<Role> roles = perAPI.getPublishRoles(folder1ToTest);
		assertEquals(0, roles.size());
		roles = perAPI.getReadRoles(folder1ToTest);
		assertEquals(1, roles.size());
		assertTrue(roles.contains(testRole));
		roles = perAPI.getWriteRoles(folder1ToTest);
		assertEquals(1, roles.size());
		assertTrue(roles.contains(testRole));
		
		roles = perAPI.getRolesWithPermission(folder1ToTest, PermissionAPI.PERMISSION_CAN_ADD_CHILDREN);
		assertEquals(1, roles.size());
		assertTrue(roles.contains(testRole));

	}

	public void testGetUsers() throws DotDataException, DotSecurityException {
		
		Permission p1 = new Permission(folder1ToTest.getInode(), testRole.getId(), PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_WRITE | PermissionAPI.PERMISSION_CAN_ADD_CHILDREN | PermissionAPI.PERMISSION_EDIT_PERMISSIONS, true);
		perAPI.save(p1, folder1ToTest, systemUser, false);

		Set<User> users = perAPI.getUsersWithPermission(folder1ToTest, PermissionAPI.PERMISSION_CAN_ADD_CHILDREN);
		assertEquals(1, users.size());
		assertTrue(users.contains(testUser));

		users = perAPI.getUsersWithPermission(folder1ToTest, PermissionAPI.PERMISSION_PUBLISH);
		assertEquals(0, users.size());

		users = perAPI.getReadUsers(folder1ToTest);
		assertEquals(1, users.size());
		assertTrue(users.contains(testUser));

		users = perAPI.getWriteUsers(folder1ToTest);
		assertEquals(1, users.size());
		assertTrue(users.contains(testUser));

	}
	
	public void testMaskOfAllPermissions() {
		int mask = perAPI.maskOfAllPermissions();
		assertEquals(PermissionAPI.PERMISSION_READ |
				PermissionAPI.PERMISSION_USE |
				PermissionAPI.PERMISSION_WRITE |
				PermissionAPI.PERMISSION_EDIT |
				PermissionAPI.PERMISSION_PUBLISH |
				PermissionAPI.PERMISSION_EDIT_PERMISSIONS |
				PermissionAPI.PERMISSION_CAN_ADD_CHILDREN |
				PermissionAPI.PERMISSION_CREATE_VIRTUAL_LINKS , mask);		
		
	}
	
	public void testRemovePermissions() throws DotDataException, DotSecurityException {

		Permission p1 = new Permission(folder1ToTest.getInode(), testRole.getId(), PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_WRITE | PermissionAPI.PERMISSION_CAN_ADD_CHILDREN | PermissionAPI.PERMISSION_EDIT_PERMISSIONS, true);
		perAPI.save(p1, folder1ToTest, systemUser, false);
		
		List<Permission> permissions = perAPI.getPermissions(folder1ToTest, true, true);
		assertEquals(1, permissions.size());
		
		perAPI.removePermissions(folder1ToTest);

		permissions = perAPI.getPermissions(folder1ToTest, true, true);
		assertEquals(0, permissions.size());

		p1 = new Permission(folder1ToTest.getInode(), testRole.getId(), PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_WRITE | PermissionAPI.PERMISSION_CAN_ADD_CHILDREN | PermissionAPI.PERMISSION_EDIT_PERMISSIONS, true);
		perAPI.save(p1, folder1ToTest, systemUser, false);
		
		permissions = perAPI.getPermissions(folder1ToTest, true, true);
		assertEquals(1, permissions.size());

		perAPI.removePermissionsByRole(testRole.getId());

		permissions = perAPI.getPermissions(folder1ToTest, true, true);
		assertEquals(0, permissions.size());
		

	}
	
	public void testResetPermissionsUnder() throws DotDataException, DotSecurityException {
		
		Permission p1 = new Permission(folder2ToTest.getInode(), testRole.getId(), PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_WRITE | PermissionAPI.PERMISSION_CAN_ADD_CHILDREN | PermissionAPI.PERMISSION_EDIT_PERMISSIONS, true);
		perAPI.save(p1, folder2ToTest, systemUser, false);

		List<Permission> permissions = perAPI.getPermissions(folder2ToTest, true, true);
		assertEquals(1, permissions.size());

		perAPI.resetPermissionsUnder(folder1ToTest);

		permissions = perAPI.getPermissions(folder2ToTest, true, true);
		assertEquals(0, permissions.size());

	}

}
