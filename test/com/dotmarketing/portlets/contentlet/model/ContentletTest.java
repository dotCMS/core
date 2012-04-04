package com.dotmarketing.portlets.contentlet.model;

import java.util.List;

import org.apache.cactus.ServletTestCase;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.PermissionSummary;
import com.dotmarketing.business.RelatedPermissionableGroup;

public class ContentletTest extends ServletTestCase {

	public void testContentletPermissions () {
		
		Contentlet c = new Contentlet();
		List<PermissionSummary> accepted =  c.acceptedPermissions();
		
		PermissionSummary ps1 = new PermissionSummary("view", "view-permission-description", PermissionAPI.PERMISSION_READ);
		PermissionSummary ps2 = new PermissionSummary("edit", "edit-permission-description", PermissionAPI.PERMISSION_WRITE);
		PermissionSummary ps3 = new PermissionSummary("publish", "publish-permission-description", PermissionAPI.PERMISSION_PUBLISH);
		PermissionSummary ps4 = new PermissionSummary("edit-permissions", "edit-permissions-permission-description", PermissionAPI.PERMISSION_EDIT_PERMISSIONS);
		assertTrue(accepted.contains(ps1));
		assertTrue(accepted.contains(ps2));
		assertTrue(accepted.contains(ps3));
		assertTrue(accepted.contains(ps4));
		
		PermissionAPI papi = APILocator.getPermissionAPI();
		for(Object perm : papi.getPermissionTypes().values()) {
			List<RelatedPermissionableGroup> related = c.permissionDependencies((Integer)perm);
			assertTrue(related == null || related.size() == 0);
		}
		
	}
}
