package com.dotmarketing.portlets.links.business;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.cactus.ServletTestCase;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.beans.Tree;
import com.dotmarketing.beans.WebAsset;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.business.Versionable;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.factories.IdentifierFactory;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.factories.TreeFactory;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.factories.FolderFactory;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.links.model.Link.LinkType;
import com.liferay.portal.model.User;

public class MenuLinkAPITest extends ServletTestCase {
	private static UserAPI userAPI;
	private static HostAPI hostAPI;
	private static RoleAPI roleAPI;
	private static FolderAPI folderAPI;
	private static PermissionAPI permissionAPI;
	private static MenuLinkAPI menuLinkAPI;
	private static IdentifierAPI identifierAPI;
	
	private static Link testMenuLink;
	private static boolean deleted = false;
	private static String friendlyName = "JUnit Test Menu Link";
	private static Date iDate = new Date();
	private static boolean internal = false;
	private static String internalLinkIdentifier = "";
	private static String linkCode = "";
	private static String linkType = LinkType.EXTERNAL.toString();
	private static boolean live = false;
	private static boolean locked = true;
	private static Date modDate = new Date();
	private static String modUser;
	private static String owner;
	private static String protocal = "https://";
	private static boolean showOnMenu = true;
	private static int sortOrder = 2;
	private static String target = "_blank";
	private static String title = "JUnit MenuLink Test";
	private static String type = "links";
	private static String url = "www.dotcms.org";
	private static boolean working = true;
	
	private static Host testHost;
	private static Folder testFolder1;
	private static Folder testFolder2;
	
	private static List<Permission> permissionList;
	
	protected void setUp() throws Exception {
		userAPI = APILocator.getUserAPI();
		hostAPI = APILocator.getHostAPI();
		roleAPI = APILocator.getRoleAPI();
		folderAPI = APILocator.getFolderAPI();
		permissionAPI = APILocator.getPermissionAPI();
		menuLinkAPI = APILocator.getMenuLinkAPI();
		identifierAPI = APILocator.getIdentifierAPI();
		
		createJUnitTestMenuLink();
	}
	
	protected void tearDown() throws Exception {
		deleteJUnitTestMenuLink();
	}
	
	private void createJUnitTestMenuLink() throws Exception {
		testMenuLink = new Link();
		testMenuLink.setDeleted(deleted);
		testMenuLink.setFriendlyName(friendlyName);
		testMenuLink.setIDate(iDate);
		testMenuLink.setInternal(internal);
		testMenuLink.setInternalLinkIdentifier(internalLinkIdentifier);
		testMenuLink.setLinkCode(linkCode);
		testMenuLink.setLinkType(linkType);
		testMenuLink.setLive(live);
		testMenuLink.setLocked(locked);
		testMenuLink.setModDate(modDate);
		
		User user = userAPI.getSystemUser();
		modUser = user.getUserId();
		testMenuLink.setModUser(modUser);
		
		owner = user.getUserId();
		testMenuLink.setOwner(owner);
		
		testMenuLink.setProtocal(protocal);
		testMenuLink.setShowOnMenu(showOnMenu);
		testMenuLink.setSortOrder(sortOrder);
		testMenuLink.setTarget(target);
		testMenuLink.setTitle(title);
		testMenuLink.setType(type);
		testMenuLink.setUrl(url);
		testMenuLink.setWorking(working);
		
		testHost = new Host();
		testHost.setHostname("dotcms_junit_test_host");
		testHost.setModDate(new Date());
		testHost.setModUser(user.getUserId());
		testHost.setOwner(user.getUserId());
		testHost.setProperty("theme", "default");
		testHost = hostAPI.save(testHost, user, false);
		
		testFolder1 = (Folder) InodeFactory.getInode(null, Folder.class);
		testFolder1.setFilesMasks("");
		testFolder1.setIDate(new Date());
		testFolder1.setName("dotcms_junit_test_folder_1");
		testFolder1.setOwner(user.getUserId());
		testFolder1.setShowOnMenu(false);
		testFolder1.setSortOrder(0);
		testFolder1.setTitle("dotcms_junit_test_folder_1");
		testFolder1.setType("folder");
		testFolder1.setHostId(testHost.getIdentifier());
				
		permissionList = new ArrayList<Permission>();
		permissionList.add(new Permission("", roleAPI.loadCMSAnonymousRole().getId(), PermissionAPI.PERMISSION_READ));
		
		folderAPI.save(testFolder1,user,false);
		Permission newPermission;
		for (Permission permission: permissionList) {
			newPermission = new Permission(testFolder1.getInode(), permission.getRoleId(), permission.getPermission());
			permissionAPI.save(newPermission, testFolder1, user, false);
		}
		testFolder2 = (Folder) InodeFactory.getInode(null, Folder.class);
		testFolder2.setFilesMasks("");
		testFolder2.setIDate(new Date());
		testFolder2.setName("dotcms_junit_test_folder_2");
		testFolder2.setOwner(user.getUserId());
		testFolder2.setShowOnMenu(false);
		testFolder2.setSortOrder(0);
		testFolder2.setTitle("dotcms_junit_test_folder_2");
		testFolder2.setType("folder");
		testFolder2.setHostId(testHost.getIdentifier());
		
		folderAPI.save(testFolder2,user,false);
		for (Permission permission: permissionList) {
			newPermission = new Permission(testFolder2.getInode(), permission.getRoleId(), permission.getPermission());
			permissionAPI.save(newPermission, testFolder2, user, false);
		}
		menuLinkAPI.save(testMenuLink, testFolder1, user, false);
		permissionAPI.copyPermissions(testFolder1, testMenuLink);
	}
	
	private void deleteJUnitTestMenuLink() throws Exception {
		User user = userAPI.getSystemUser();
		menuLinkAPI.delete(testMenuLink, user, false);
		
		testFolder1 = (Folder) InodeFactory.getInode(testFolder1.getInode(), Folder.class);
		InodeFactory.deleteInode(testFolder1);
		
		testFolder2 = (Folder) InodeFactory.getInode(testFolder2.getInode(), Folder.class);
		InodeFactory.deleteInode(testFolder2);
		
		hostAPI.delete(testHost, user, false);
	}
	
	private boolean checkPermission(WebAsset asset) throws Exception {
		List<Permission> permissions = permissionAPI.getPermissions(asset);
		
		if (permissions.size() != permissionList.size())
			return false;
		
		for (Permission permission1: permissions) {
			for (Permission permission2: permissionList) {
				if ((permission1.getPermission() != permission2.getPermission()) || !permission1.getRoleId().equals(permission2.getRoleId())) {
					return false;
				}
			}
		}
		
		return true;
	}
	
	public void testCopy() throws Exception {
		User user = userAPI.getSystemUser();
		Link testMenuLinkCopy = menuLinkAPI.copy(testMenuLink, testFolder2, user, false);
		
		try {
			assertFalse("Invalid \"identifier\" in copy menu link.", testMenuLinkCopy.getIdentifier().equals(testMenuLink.getIdentifier()));
			assertFalse("Invalid \"inode\" in copy menu link.", testMenuLinkCopy.getInode().equals(testMenuLink.getInode()));
			assertEquals("Invalid \"friendlyName\" in copy menu link.", friendlyName, testMenuLinkCopy.getFriendlyName());
			assertEquals("Invalid \"internalLinkIdentifier\" in copy menu link.", internalLinkIdentifier, testMenuLinkCopy.getInternalLinkIdentifier());
			assertEquals("Invalid \"linkCode\" in copy menu link.", linkCode, testMenuLinkCopy.getLinkCode());
			assertEquals("Invalid \"linkType\" in copy menu link.", linkType, testMenuLinkCopy.getLinkType());
			assertEquals("Invalid \"modUser\" in copy menu link.", modUser, testMenuLinkCopy.getModUser());
			assertEquals("Invalid \"protocal\" in copy menu link.", protocal, testMenuLinkCopy.getProtocal());
			assertEquals("Invalid \"internal\" in copy menu link.", internal, testMenuLinkCopy.isInternal());
			assertEquals("Invalid \"showOnMenu\" in copy menu link.", showOnMenu, testMenuLinkCopy.isShowOnMenu());
			assertEquals("Invalid \"sortOrder\" in copy menu link.", sortOrder, testMenuLinkCopy.getSortOrder());
			assertEquals("Invalid \"target\" in copy menu link.", target, testMenuLinkCopy.getTarget());
			assertEquals("Invalid \"title\" in copy menu link.", title, testMenuLinkCopy.getTitle());
			assertEquals("Invalid \"type\" in copy menu link.", type, testMenuLinkCopy.getType());
			assertEquals("Invalid \"url\" in copy menu link.", url, testMenuLinkCopy.getUrl());
			assertTrue("Invalid permissions assigned to copy menu link.", checkPermission(testMenuLinkCopy));
		} finally {
			menuLinkAPI.delete(testMenuLinkCopy, user, false);
		}
	}
	
	public void testCopySameFolder() throws Exception {
		User user = userAPI.getSystemUser();
		Link testMenuLinkCopy = menuLinkAPI.copy(testMenuLink, testFolder1, user, false);
		
		try {
			assertFalse("Invalid \"identifier\" in copy menu link.", testMenuLinkCopy.getIdentifier().equals(testMenuLink.getIdentifier()));
			assertFalse("Invalid \"inode\" in copy menu link.", testMenuLinkCopy.getInode().equals(testMenuLink.getInode()));
			assertEquals("Invalid \"friendlyName\" in copy menu link.", friendlyName, testMenuLinkCopy.getFriendlyName());
			assertEquals("Invalid \"internalLinkIdentifier\" in copy menu link.", internalLinkIdentifier, testMenuLinkCopy.getInternalLinkIdentifier());
			assertEquals("Invalid \"linkCode\" in copy menu link.", linkCode, testMenuLinkCopy.getLinkCode());
			assertEquals("Invalid \"linkType\" in copy menu link.", linkType, testMenuLinkCopy.getLinkType());
			assertEquals("Invalid \"modUser\" in copy menu link.", modUser, testMenuLinkCopy.getModUser());
			assertEquals("Invalid \"protocal\" in copy menu link.", protocal, testMenuLinkCopy.getProtocal());
			assertEquals("Invalid \"internal\" in copy menu link.", internal, testMenuLinkCopy.isInternal());
			assertEquals("Invalid \"showOnMenu\" in copy menu link.", showOnMenu, testMenuLinkCopy.isShowOnMenu());
			assertEquals("Invalid \"sortOrder\" in copy menu link.", sortOrder, testMenuLinkCopy.getSortOrder());
			assertEquals("Invalid \"target\" in copy menu link.", target, testMenuLinkCopy.getTarget());
			assertEquals("Invalid \"title\" in copy menu link.", title, testMenuLinkCopy.getTitle());
			assertEquals("Invalid \"type\" in copy menu link.", type, testMenuLinkCopy.getType());
			assertEquals("Invalid \"url\" in copy menu link.", url, testMenuLinkCopy.getUrl());
			assertTrue("Invalid permissions assigned to copy menu link.", checkPermission(testMenuLinkCopy));
			
			Identifier identifier = identifierAPI.findFromInode(testMenuLink.getIdentifier());
			List<Versionable> allVersions = APILocator.getVersionableAPI().findAllVersions(identifier);
			
			assertTrue("Invalid number of version: " + allVersions.size(), (allVersions.size() == 1));
			
			List<Link> menuLinks = IdentifierFactory.getChildrenClassByCondition(testFolder1, Link.class, "working=" + DbConnectionFactory.getDBTrue());
			assertTrue("Invalid number of menu links created: " + menuLinks.size(), (menuLinks.size() == 2));
		} finally {
			menuLinkAPI.delete(testMenuLinkCopy, user, false);
		}
	}
}