package com.dotmarketing.portlets.folders.model;

import java.util.List;

import org.apache.cactus.ServletTestCase;

import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.PermissionSummary;

/**
 * 
 * This test assumes you are running it against a clean dotCMS starter based installation.
 * @author davidtorresv
 *
 */
public class FolderTest extends ServletTestCase {

	@SuppressWarnings("unchecked")
	public void testFolderPermissionable () {

		Folder f = new Folder();
		List<PermissionSummary> accepted =  f.acceptedPermissions();
		
		PermissionSummary ps1 = new PermissionSummary("view", "view-permission-description", PermissionAPI.PERMISSION_READ);
		PermissionSummary ps2 = new PermissionSummary("edit", "edit-permission-description", PermissionAPI.PERMISSION_WRITE);
		PermissionSummary ps3 = new PermissionSummary("publish", "publish-permission-description", PermissionAPI.PERMISSION_PUBLISH);
		PermissionSummary ps4 = new PermissionSummary("edit-permissions", "edit-permissions-permission-description", PermissionAPI.PERMISSION_EDIT_PERMISSIONS);
		assertTrue(accepted.contains(ps1));
		assertTrue(accepted.contains(ps2));
		assertTrue(accepted.contains(ps3));
		assertTrue(accepted.contains(ps4));
			

	}
	
}
