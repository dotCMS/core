package com.dotmarketing.portlets.templates.business;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.cactus.ServletTestCase;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.beans.WebAsset;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.business.Versionable;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.IdentifierFactory;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.portlets.containers.business.ContainerAPI;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.templates.model.Template;
import com.liferay.portal.model.User;

public class TemplateAPITest extends ServletTestCase {
	private static UserAPI userAPI;
	private static HostAPI hostAPI;
	private static ContainerAPI containerAPI;
	private static RoleAPI roleAPI;
	private static PermissionAPI permissionAPI;
	private static TemplateAPI templateAPI;
	private static IdentifierAPI identifierAPI;
	
	public static Template testTemplate;
	public static String body;
	public static boolean deleted = false;
	public static String footer = "JUnit Test Template Footer";
	public static String friendlyName = "JUnit Test Template Friendly Name";
	public static String header = "JUnit Test Template Header";
	public static Date iDate = new Date();
	public static String image = "";
	public static boolean live = false;
	public static boolean locked = true;
	public static Date modDate = new Date();
	public static String modUser;
	public static String owner;
	public static String selectedimage = "";
	public static boolean showOnMenu = false;
	public static int sortOrder = 2;
	public static String title = "JUnit Test Template";
	public static String type = "template";
	public static boolean working = true;
	
	public static Container testContainer1;
	public static Container testContainer2;
	
	public static Host testHost1;
	public static Host testHost2;
	
	private static Structure testStructure;
	
	private static List<Permission> permissionList;
	
	public void setUp() throws Exception {
		userAPI = APILocator.getUserAPI();
		hostAPI = APILocator.getHostAPI();
		containerAPI = APILocator.getContainerAPI();
		roleAPI = APILocator.getRoleAPI();
		permissionAPI = APILocator.getPermissionAPI();
		templateAPI = APILocator.getTemplateAPI();
		identifierAPI = APILocator.getIdentifierAPI();
		
		createJUnitTestTemplate();
	}
	
	protected void tearDown() throws Exception {
		deleteJUnitTestTemplate();
	}
	
	private void createJUnitTestTemplate() throws Exception {
		testContainer1 = new Container();
		testContainer1.setCode("##JUnit Test Container 1\n$Body");
		testContainer1.setDeleted(false);
		testContainer1.setFriendlyName("JUnit Test Container 1 Friendly Name");
		testContainer1.setIDate(new Date());
		testContainer1.setLive(false);
		testContainer1.setLocked(true);
		testContainer1.setLuceneQuery("");
		testContainer1.setMaxContentlets(0);
		testContainer1.setModDate(new Date());
		
		User user = userAPI.getSystemUser();
		testContainer1.setModUser(user.getUserId());
		
		testContainer1.setNotes("JUnit Test Container 1 Note");
		testContainer1.setOwner(user.getUserId());
		
		testContainer1.setPostLoop("");
		testContainer1.setPreLoop("");
		testContainer1.setShowOnMenu(true);
		testContainer1.setSortContentletsBy("");
		testContainer1.setSortOrder(2);
		testContainer1.setStaticify(true);
		testContainer1.setTitle("JUnit Test Container 1");
		testContainer1.setType("containers");
		testContainer1.setUseDiv(true);
		testContainer1.setWorking(true);
		
		testContainer2 = new Container();
		testContainer2.setCode("##JUnit Test Container 2\n$Body");
		testContainer2.setDeleted(false);
		testContainer2.setFriendlyName("JUnit Test Container 2 Friendly Name");
		testContainer2.setIDate(new Date());
		testContainer2.setLive(false);
		testContainer2.setLocked(true);
		testContainer2.setLuceneQuery("");
		testContainer2.setMaxContentlets(0);
		testContainer2.setModDate(new Date());
		testContainer2.setModUser(user.getUserId());
		testContainer2.setNotes("JUnit Test Container 2 Note");
		testContainer2.setOwner(user.getUserId());
		testContainer2.setPostLoop("");
		testContainer2.setPreLoop("");
		testContainer2.setShowOnMenu(true);
		testContainer2.setSortContentletsBy("");
		testContainer2.setSortOrder(2);
		testContainer2.setStaticify(true);
		testContainer2.setTitle("JUnit Test Container 2");
		testContainer2.setType("containers");
		testContainer2.setUseDiv(true);
		testContainer2.setWorking(true);
		
		testHost1 = new Host();
		testHost1.setHostname("dotcms_junit_test_host_1");
		testHost1.setModDate(new Date());
		testHost1.setModUser(user.getUserId());
		testHost1.setOwner(user.getUserId());
		testHost1.setProperty("theme", "default");
		testHost1 = hostAPI.save(testHost1, user, false);
		
		testHost2 = new Host();
		testHost2.setProperty("theme", "default");
		testHost2.setHostname("dotcms_junit_test_host_2");
		testHost2.setModDate(new Date());
		testHost2.setModUser(user.getUserId());
		testHost2.setOwner(user.getUserId());
		testHost2 = hostAPI.save(testHost2, user, false);
		
		testStructure = null;
		
		testContainer1 = containerAPI.save(testContainer1, testStructure, testHost1, user, false);
		testContainer2 = containerAPI.save(testContainer2, testStructure, testHost1, user, false);
		
		testTemplate = new Template();
		
		body = "<html>\n<head>\n</head>\n<body>\n</body>\n#parseContainer('" + testContainer1.getIdentifier() + "')\n#parseContainer('" + testContainer2.getIdentifier() + "')\n</html>";
		testTemplate.setBody(body);
		testTemplate.setDeleted(deleted);
		testTemplate.setFooter(footer);
		testTemplate.setFriendlyName(friendlyName);
		testTemplate.setHeader(header);
		testTemplate.setIDate(iDate);
		testTemplate.setImage(image);
		testTemplate.setLive(live);
		testTemplate.setLocked(locked);
		testTemplate.setModDate(modDate);
		
		modUser = user.getUserId();
		testTemplate.setModUser(modUser);
		
		owner = user.getUserId();
		testTemplate.setOwner(owner);
		
		testTemplate.setSelectedimage(selectedimage);
		testTemplate.setShowOnMenu(showOnMenu);
		testTemplate.setSortOrder(sortOrder);
		testTemplate.setTitle(title);
		testTemplate.setType(type);
		testTemplate.setWorking(working);
		
		testTemplate = templateAPI.saveTemplate(testTemplate, testHost1, user, false);
		
		permissionList = new ArrayList<Permission>();
		permissionList.add(new Permission("", roleAPI.loadCMSAnonymousRole().getId(), PermissionAPI.PERMISSION_READ));
		
		Permission newPermission;
		for (Permission permission: permissionList) {
			newPermission = new Permission(testTemplate.getPermissionId(), permission.getRoleId(), permission.getPermission(), true);
			permissionAPI.save(newPermission, testTemplate, user, false);
		}
	}
	
	private void deleteJUnitTestTemplate() throws Exception {
		User user = userAPI.getSystemUser();
		
		templateAPI.delete(testTemplate, user, false);
		
		containerAPI.delete(testContainer1, user, false);
		containerAPI.delete(testContainer2, user, false);
		
		hostAPI.delete(testHost1, user, false);
		hostAPI.delete(testHost2, user, false);
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
	
	public void testCopyNoOverwriteReuseContainers() throws Exception {
		User user = userAPI.getSystemUser();
		Template testTemplateCopy = templateAPI.copy(testTemplate, testHost1, false, false, user, false);
		
		try {
			assertFalse("Invalid \"identifier\" in copy template.", testTemplateCopy.getIdentifier().equals(testTemplate.getIdentifier()));
			assertFalse("Invalid \"inode\" in copy template.", testTemplateCopy.getInode().equals(testTemplate.getInode()));
			assertEquals("Invalid \"body\" in copy template. Body must be the same", body, testTemplateCopy.getBody());
			assertEquals("Invalid \"footer\" in copy template.", footer, testTemplateCopy.getFooter());
			assertFalse("Invalid \"friendlyName\" in copy template.", testTemplateCopy.getFriendlyName().equals(friendlyName));
			assertEquals("Invalid \"header\" in copy template.", header, testTemplateCopy.getHeader());
			assertEquals("Invalid \"modUser\" in copy template.", modUser, testTemplateCopy.getModUser());
			assertEquals("Invalid \"sortOrder\" in copy template.", sortOrder, testTemplateCopy.getSortOrder());
			assertEquals("Invalid \"showOnMenu\" in copy template.", showOnMenu, testTemplateCopy.isShowOnMenu());
			assertFalse("Invalid \"title\" in copy template.", testTemplateCopy.getTitle().equals(title));
			assertEquals("Invalid \"type\" in copy template.", type, testTemplateCopy.getType());
			assertTrue("Invalid permissions assigned to copy template.", checkPermission(testTemplateCopy));
			
			List<Container> containers = containerAPI.getContainersInTemplate(testTemplateCopy);
			assertTrue("Invalid number of containers copy", containers.size() == 2);
		} finally {
			templateAPI.delete(testTemplateCopy, user, false);
		}
	}
	
	public void testCopyOverwriteReuseContainers() throws Exception {
		User user = userAPI.getSystemUser();
		Template testTemplateCopy = templateAPI.copy(testTemplate, testHost2, true, false, user, false);
		
		try {
			assertFalse("Invalid \"identifier\" in copy template.", testTemplateCopy.getIdentifier().equals(testTemplate.getIdentifier()));
			assertFalse("Invalid \"inode\" in copy template.", testTemplateCopy.getInode().equals(testTemplate.getInode()));
			assertEquals("Invalid \"body\" in copy template. Body must be the same", body, testTemplateCopy.getBody());
			assertEquals("Invalid \"footer\" in copy template.", footer, testTemplateCopy.getFooter());
			assertFalse("Invalid \"friendlyName\" in copy template.", testTemplateCopy.getFriendlyName().equals(friendlyName));
			assertEquals("Invalid \"header\" in copy template.", header, testTemplateCopy.getHeader());
			assertEquals("Invalid \"modUser\" in copy template.", modUser, testTemplateCopy.getModUser());
			assertEquals("Invalid \"sortOrder\" in copy template.", sortOrder, testTemplateCopy.getSortOrder());
			assertEquals("Invalid \"showOnMenu\" in copy template.", showOnMenu, testTemplateCopy.isShowOnMenu());
			assertEquals("Invalid \"title\" in copy template.", title, testTemplateCopy.getTitle());
			assertEquals("Invalid \"type\" in copy template.", type, testTemplateCopy.getType());
			assertTrue("Invalid permissions assigned to copy template.", checkPermission(testTemplateCopy));
			
			List<Container> containers = containerAPI.getContainersInTemplate(testTemplateCopy);
			assertTrue("Invalid number of containers copy", containers.size() == 2);
			
			String otherTemplateBody = "This is other template";
			testTemplateCopy.setBody(otherTemplateBody);
			String otherTemplateFooter = "This is other footers";
			testTemplateCopy.setFooter(otherTemplateFooter);
			String otherTemplateHeader = "This is other header";
			testTemplateCopy.setHeader(otherTemplateHeader);
			testTemplateCopy = templateAPI.copy(testTemplateCopy, testHost2, true, false, user, false);
			
			assertFalse("Invalid \"identifier\" in copy template.", testTemplateCopy.getIdentifier().equals(testTemplate.getIdentifier()));
			assertFalse("Invalid \"inode\" in copy template.", testTemplateCopy.getInode().equals(testTemplate.getInode()));
			assertEquals("Invalid \"body\" in copy template. Body must be the same", otherTemplateBody, testTemplateCopy.getBody());
			assertEquals("Invalid \"footer\" in copy template.", otherTemplateFooter, testTemplateCopy.getFooter());
			assertFalse("Invalid \"friendlyName\" in copy template.", testTemplateCopy.getFriendlyName().equals(friendlyName));
			assertEquals("Invalid \"header\" in copy template.", otherTemplateHeader, testTemplateCopy.getHeader());
			assertEquals("Invalid \"modUser\" in copy template.", modUser, testTemplateCopy.getModUser());
			assertEquals("Invalid \"sortOrder\" in copy template.", sortOrder, testTemplateCopy.getSortOrder());
			assertEquals("Invalid \"showOnMenu\" in copy template.", showOnMenu, testTemplateCopy.isShowOnMenu());
			assertEquals("Invalid \"title\" in copy template.", title, testTemplateCopy.getTitle());
			assertEquals("Invalid \"type\" in copy template.", type, testTemplateCopy.getType());
			
			containers = containerAPI.getContainersInTemplate(testTemplateCopy);
			assertTrue("Invalid number of containers copy", containers.size() == 0);
			
			testTemplateCopy = templateAPI.copy(testTemplate, testHost2, true, false, user, false);
			
			assertFalse("Invalid \"identifier\" in copy template.", testTemplateCopy.getIdentifier().equals(testTemplate.getIdentifier()));
			assertFalse("Invalid \"inode\" in copy template.", testTemplateCopy.getInode().equals(testTemplate.getInode()));
			assertEquals("Invalid \"body\" in copy template. Body must be the same", body, testTemplateCopy.getBody());
			assertEquals("Invalid \"footer\" in copy template.", footer, testTemplateCopy.getFooter());
			assertFalse("Invalid \"friendlyName\" in copy template.", testTemplateCopy.getFriendlyName().equals(friendlyName));
			assertEquals("Invalid \"header\" in copy template.", header, testTemplateCopy.getHeader());
			assertEquals("Invalid \"modUser\" in copy template.", modUser, testTemplateCopy.getModUser());
			assertEquals("Invalid \"sortOrder\" in copy template.", sortOrder, testTemplateCopy.getSortOrder());
			assertEquals("Invalid \"showOnMenu\" in copy template.", showOnMenu, testTemplateCopy.isShowOnMenu());
			assertEquals("Invalid \"title\" in copy template.", title, testTemplateCopy.getTitle());
			assertEquals("Invalid \"type\" in copy template.", type, testTemplateCopy.getType());
			assertTrue("Invalid permissions assigned to copy template.", checkPermission(testTemplateCopy));
			
			containers = containerAPI.getContainersInTemplate(testTemplateCopy);
			assertTrue("Invalid number of containers copy", containers.size() == 2);
			
			Identifier identifier = identifierAPI.findFromInode(testTemplateCopy.getIdentifier());
			List<Versionable> allVersions = APILocator.getVersionableAPI().findAllVersions(identifier);
			assertTrue("Invalid number of version: " + allVersions.size(), (allVersions.size() == 3));
			
			List<Template> templates = templateAPI.findTemplatesAssignedTo(testHost2);
			assertTrue("Invalid number of template created: " + templates.size(), (templates.size() == 1));
		} finally {
			templateAPI.delete(testTemplateCopy, user, false);
		}
	}
	
	public void testFindTemplatesUnder() throws Exception {

		FolderAPI folderAPI = APILocator.getFolderAPI();
		User user = userAPI.getSystemUser();
		Host host = hostAPI.findDefaultHost(user, false);
		Folder f = folderAPI.findFolderByPath("/calendar",host,user,false);
		
		List<Template> templates = templateAPI.findTemplatesUnder(f);
		
		assertEquals(3, templates.size());
		assertTrue(containsTemplateByTitle(templates, "dotCMS Core - Template 1"));
		assertTrue(containsTemplateByTitle(templates, "dotCMS Core - Template 4"));
		assertTrue(containsTemplateByTitle(templates, "dotCMS Core - Blank Template"));
		
	}
	
	public void testFindTemplatesAssignedTo() throws DotDataException {
		
		Host defaultHost;
		try {
			defaultHost = APILocator.getHostAPI().findDefaultHost(APILocator.getUserAPI().getSystemUser(), false);
		} catch (DotDataException e) {
			throw new DotRuntimeException(e.getMessage(), e);
		} catch (DotSecurityException e) {
			throw new DotRuntimeException(e.getMessage(), e);
		}

		List<Template> templates = templateAPI.findTemplatesAssignedTo(defaultHost);
		assertEquals(8, templates.size());
		assertTrue(containsTemplateByTitle(templates, "dotCMS Core - Template 2"));
		assertTrue(containsTemplateByTitle(templates, "dotCMS Core - Template 4"));
		assertTrue(containsTemplateByTitle(templates, "dotCMS Core - Blank Template"));
		assertTrue(containsTemplateByTitle(templates, "dotCMS Core - Flash Banner"));
		assertTrue(containsTemplateByTitle(templates, "dotCMS Core - Template 5"));
		assertTrue(containsTemplateByTitle(templates, "dotCMS Core - Template 6"));
		assertTrue(containsTemplateByTitle(templates, "dotCMS Core - Template 1"));
		
	}
	
	private boolean containsTemplateByTitle (List<Template> templates, String title) {
		for(Template t : templates) {
			if(t.getTitle().trim().equals(title.trim())) return true;
		}
		return false;
	}
	
}
