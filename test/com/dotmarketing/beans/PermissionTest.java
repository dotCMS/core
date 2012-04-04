package com.dotmarketing.beans;

import org.apache.cactus.ServletTestCase;

import com.dotmarketing.business.PermissionAPI;

/**
 * 
 * @author David Torres
 * @version 1.8
 * @since 1.8
 *
 */
public class PermissionTest extends ServletTestCase {

	public void testPermissionConstructors () {
		Permission p = new Permission();
		assertEquals(p.getId(), 0);
		assertEquals(p.getInode(), null);
		assertEquals(p.getPermission(), 0);
		assertEquals(p.getRoleId(), null);
		assertEquals(p.isBitPermission(), false);
		
		p = new Permission("124312", "roleasdf", 43532);
		assertEquals(p.getId(), 0);
		assertEquals(p.getInode(), "124312");
		assertEquals(p.getPermission(), 43532);
		assertEquals(p.getRoleId(), "roleasdf");
		assertEquals(p.isBitPermission(), false);
		
		p = new Permission("123421", "roleagfsd", 4325, true);
		assertEquals(p.getId(), 0);
		assertEquals(p.getInode(), "123421");
		assertEquals(p.getPermission(), 4325);
		assertEquals(p.getRoleId(), "roleagfsd");
		assertEquals(p.isBitPermission(), true);
		
	}
	
	public void testPermissionGettersAndSetters() {
		Permission p = new Permission();
		p.setId(123421);
		assertEquals(p.getId(), 123421);
		p.setInode("234645");
		assertEquals(p.getInode(), "234645");
		p.setPermission(6543);
		assertEquals(p.getPermission(), 6543);
		p.setRoleId("hwer");
		assertEquals(p.getRoleId(), "hwer");
		assertEquals(p.isBitPermission(), false);
		p.setBitPermission(true);
		assertEquals(p.isBitPermission(), true);
	}
	
	public void testPermissionEquals() {
		Permission p1 = new Permission("1", "1", 1);
		Permission p2 = new Permission("2", "2", 2);
		Permission p3 = new Permission("1", "1", 1);
		
		assertTrue(p1.equals(p3));
		assertFalse(p1.equals(p2));
		assertFalse(p3.equals(p2));
	}
	
	public void testMatchesPermissionType() {
		Permission p = new Permission();
		p.setBitPermission(false);
		p.setPermission(PermissionAPI.PERMISSION_PUBLISH);
		assertEquals(p.matchesPermission(PermissionAPI.PERMISSION_PUBLISH), true);
		
		p = new Permission();
		p.setBitPermission(true);
		p.setPermission(PermissionAPI.PERMISSION_PUBLISH | PermissionAPI.PERMISSION_READ);
		assertEquals(p.matchesPermission(PermissionAPI.PERMISSION_PUBLISH), true);
		assertFalse(p.matchesPermission(PermissionAPI.PERMISSION_WRITE));
	}
	
	
}
