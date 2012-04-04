package com.dotmarketing.business;

import org.apache.cactus.ServletTestCase;

import com.dotmarketing.portlets.containers.business.ContainerAPI;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.business.HostAPIImpl;
import com.dotmarketing.portlets.files.business.FileAPI;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.htmlpages.business.HTMLPageAPI;
import com.dotmarketing.portlets.links.business.MenuLinkAPI;
import com.dotmarketing.portlets.templates.business.TemplateAPI;
import com.dotmarketing.portlets.templates.business.TemplateAPIImpl;
import com.dotmarketing.portlets.virtuallinks.business.VirtualLinkAPI;

/**
 * 
 * This is the first version of this test class and only contains a few of methods to be tested for dotConnect
 * @author David Torres
 * @version 1.8
 * @since 1.8
 *
 */
public class APILocatorTest extends ServletTestCase {

	public void testLocatePermissionsAPI() {
		PermissionAPI api = APILocator.getPermissionAPI();
		assertNotNull(api);
		assertTrue(api instanceof PermissionBitAPIImpl);
	}
	
	public void testLocateTemplateAPI() {
		TemplateAPI api = APILocator.getTemplateAPI();
		assertNotNull(api);
		assertTrue(api instanceof TemplateAPIImpl);
	}

	public void testLocateFolderAPI() {
		FolderAPI api = APILocator.getFolderAPI();
		assertNotNull(api);
		assertTrue(api instanceof FolderAPI);
	}

	public void testLocateHostAPI() {
		HostAPI api = APILocator.getHostAPI();
		assertNotNull(api);
		assertTrue(api instanceof HostAPIImpl);
	}

	public void testLocateContainerAPI() {
		ContainerAPI api = APILocator.getContainerAPI();
		assertNotNull(api);
		assertTrue(api instanceof ContainerAPI);
	}
	
	public void testLocateFileAPI() {
		FileAPI api = APILocator.getFileAPI();
		assertNotNull(api);
		assertTrue(api instanceof FileAPI);
	}
	
	public void testLocateMenuLinkAPI() {
		MenuLinkAPI api = APILocator.getMenuLinkAPI();
		assertNotNull(api);
		assertTrue(api instanceof MenuLinkAPI);
	}
	
	public void testLocateContentletAPI() {
		ContentletAPI api = APILocator.getContentletAPI();
		assertNotNull(api);
		assertTrue(api instanceof ContentletAPI);
	}
	
	public void testLocateHTMLPageAPI() {
		HTMLPageAPI api = APILocator.getHTMLPageAPI();
		assertNotNull(api);
		assertTrue(api instanceof HTMLPageAPI);
	}
	
	public void testLocateVirtualLinkAPI() {
		VirtualLinkAPI api = APILocator.getVirtualLinkAPI();
		assertNotNull(api);
		assertTrue(api instanceof VirtualLinkAPI);
	}
}