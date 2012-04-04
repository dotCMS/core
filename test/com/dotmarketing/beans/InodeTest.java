package com.dotmarketing.beans;

import java.util.Collection;
import java.util.Map;

import org.apache.cactus.ServletTestCase;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;

/**
 * 
 * @author David H Torres
 * @version 1.8
 * @since 1.8
 *
 */
public class InodeTest extends ServletTestCase {

	private Map<String, Integer> permissionTypes;
	private PermissionAPI perAPI;
	
	@Override
	protected void setUp() throws Exception {
		perAPI = APILocator.getPermissionAPI();
		permissionTypes = perAPI.getPermissionTypes();
		perAPI = APILocator.getPermissionAPI();
	}
	
	public void testInodePermissionableInterface () {
		
		Inode in = new Inode();
		assertTrue(in.acceptedPermissions() == null || in.acceptedPermissions().size() == 0);
		Collection<Integer> values = permissionTypes.values();
		for(Integer value: values) {
			assertTrue(in.permissionDependencies(value) == null || in.permissionDependencies(value).size() == 0);
		}
		
		
	}
	

	
	
}
