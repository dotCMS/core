package com.dotmarketing.portlets.htmlpages.business;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.cactus.ServletTestCase;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.beans.Tree;
import com.dotmarketing.beans.WebAsset;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.business.Versionable;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.cache.IdentifierCache;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.IdentifierFactory;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.factories.MultiTreeFactory;
import com.dotmarketing.factories.TreeFactory;
import com.dotmarketing.portlets.containers.business.ContainerAPI;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.entities.model.Entity;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.factories.FolderFactory;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.portlets.structure.factories.FieldFactory;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.structure.model.Field.DataType;
import com.dotmarketing.portlets.structure.model.Field.FieldType;
import com.dotmarketing.portlets.templates.business.TemplateAPI;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.services.StructureServices;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;

/**
 * 
 * This test is assuming that you are running on top a clean dotCMS starter installation
 * 
 * @author davidtorresv
 *
 */
public class HTMLPageAPITest extends ServletTestCase {
	private static UserAPI userAPI;
	private static HostAPI hostAPI;
	private static ContentletAPI contentletAPI;
	private static ContainerAPI containerAPI;
	private static TemplateAPI templateAPI;
	private static FolderAPI folderAPI;
	private static HTMLPageAPI htmlPageAPI;
	private static RoleAPI roleAPI;
	private static PermissionAPI permissionAPI;
	private static IdentifierAPI identifierAPI;
	
	private static Structure testStructure;
	private static Contentlet testContentlet1;
	private static Contentlet testContentlet2;
	private static Host testHost;
	private static Container testContainer1;
	private static Container testContainer2;
	private static Template testTemplate;
	private static Folder testFolder1;
	private static Folder testFolder2;
	private static HTMLPage testHTMLPage;
	
	private static boolean deleted = false;
	private static Date endDate = new Date();
	private static String friendlyName = "JUnit HTML Page Test Friendly Name";
	private static boolean httpsRequired = true;
	private static Date iDate = new Date();
	private static boolean live = false;
	private static boolean locked = true;
	private static String metadata = "";
	private static Date modDate = new Date();
	private static String modUser;
	private static String owner;
	private static String pageUrl = "junit_htmlpage_test.dot";
	private static String redirect = "";
	private static boolean showOnMenu = true;
	private static int sortOrder = 2;
	private static Date startDate = new Date();
	private static String title = "JUnit HTML Page Test";
	private static String type = "htmlpage";
	private static String webEndDate = "";
	private static String webStartDate = "";
	private static boolean working = true;
	
	private static List<Permission> permissionList;
	
	protected void setUp() throws Exception {
		userAPI = APILocator.getUserAPI();
		hostAPI = APILocator.getHostAPI();
		contentletAPI = APILocator.getContentletAPI();
		containerAPI = APILocator.getContainerAPI();
		templateAPI = APILocator.getTemplateAPI();
		folderAPI = APILocator.getFolderAPI();
		htmlPageAPI = APILocator.getHTMLPageAPI();
		roleAPI = APILocator.getRoleAPI();
		permissionAPI = APILocator.getPermissionAPI();
		identifierAPI = APILocator.getIdentifierAPI();
		
		createJUnitTestHTMLPage();
	}
	
	protected void tearDown() throws Exception {
		deleteJUnitTestHTMLPage();
	}
	
	private void createJUnitTestHTMLPage() throws DotDataException, DotSecurityException {
		User user = userAPI.getSystemUser();
		
		testStructure = new Structure();
		testStructure.setDefaultStructure(false);
		testStructure.setDescription("JUnit Test Structure Description.");
		testStructure.setFixed(false);
		testStructure.setIDate(new Date());
		testStructure.setName("JUnit Test Structure.");
		testStructure.setOwner(user.getUserId());
		testStructure.setDetailPage("");
		testStructure.setStructureType(Structure.STRUCTURE_TYPE_CONTENT);
		testStructure.setSystem(false);
		testStructure.setType("structure");
		testStructure.setVelocityVarName("junit_test_structure");
		StructureFactory.saveStructure(testStructure);
		Entity entity = new Entity();
		entity.setEntityName(testStructure.getName());
		HibernateUtil.saveOrUpdate(entity);
		TreeFactory.saveTree(new Tree(entity.getInode(), testStructure.getInode(), WebKeys.Structure.STRUCTURE_ENTITY, 0));
		
		Field field1 = new Field("Title", FieldType.TEXT, DataType.TEXT, testStructure, false, true, false, 1, false, false, false);
		FieldFactory.saveField(field1);
		
		Field field2 = new Field("Body", FieldType.TEXT_AREA, DataType.LONG_TEXT, testStructure, false, false, false, 2, false, false, false);
		FieldFactory.saveField(field2);
		
		testHost = new Host();
		testHost.setHostname("dotcms_junit_test_host");
		testHost.setModDate(new Date());
		testHost.setModUser(user.getUserId());
		testHost.setOwner(user.getUserId());
		testHost.setProperty("theme", "default");
		testHost = hostAPI.save(testHost, user, false);
		
		testContentlet1 = new Contentlet();
		testContentlet1.setLive(false);
		testContentlet1.setWorking(true);
		testContentlet1.setStructureInode(testStructure.getInode());
		testContentlet1.setHost(testHost.getIdentifier());
		testContentlet1.setFolder(FolderAPI.SYSTEM_FOLDER);
		
		contentletAPI.setContentletProperty(testContentlet1, field1, "JUnit Test Content 1");
		contentletAPI.setContentletProperty(testContentlet1, field2, "JUnit Test Content 1 Body");
		
		testContentlet1 = contentletAPI.checkin(testContentlet1, user, false);
		
		testContentlet2 = new Contentlet();
		testContentlet2.setLive(false);
		testContentlet2.setWorking(true);
		testContentlet2.setStructureInode(testStructure.getInode());
		testContentlet2.setHost(testHost.getIdentifier());
		testContentlet2.setFolder(FolderAPI.SYSTEM_FOLDER);
		
		contentletAPI.setContentletProperty(testContentlet2, field1, "JUnit Test Content 2");
		contentletAPI.setContentletProperty(testContentlet2, field2, "JUnit Test Content 2 Body");
		
		testContentlet2 = contentletAPI.checkin(testContentlet2, user, false);
		
		testContainer1 = new Container();
		testContainer1.setCode("$!{body}");
		testContainer1.setDeleted(false);
		testContainer1.setFriendlyName("JUnit Test Container 1 Friendly Name");
		testContainer1.setIDate(new Date());
		testContainer1.setLive(false);
		testContainer1.setLocked(true);
		testContainer1.setLuceneQuery("");
		testContainer1.setMaxContentlets(1);
		testContainer1.setModDate(new Date());
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
		
		testContainer1 = containerAPI.save(testContainer1, testStructure, testHost, user, false);
		
		testContainer2 = new Container();
		testContainer2.setCode("$!{body}");
		testContainer2.setDeleted(false);
		testContainer2.setFriendlyName("JUnit Test Container 2 Friendly Name");
		testContainer2.setIDate(new Date());
		testContainer2.setLive(false);
		testContainer2.setLocked(true);
		testContainer2.setLuceneQuery("");
		testContainer2.setMaxContentlets(1);
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
		
		testContainer2 = containerAPI.save(testContainer2, testStructure, testHost, user, false);
		
		testTemplate = new Template();
		
		String body = "<html>\n<head>\n</head>\n<body>\n</body>\n#parseContainer('" + testContainer1.getIdentifier() + "')\n<br>\n<br>\n#parseContainer('" + testContainer2.getIdentifier() + "')\n</html>";
		testTemplate.setBody(body);
		testTemplate.setDeleted(false);
		testTemplate.setFooter("");
		testTemplate.setFriendlyName("JUnit Test Template Friendly Name");
		testTemplate.setHeader("");
		testTemplate.setIDate(new Date());
		testTemplate.setImage("");
		testTemplate.setLive(false);
		testTemplate.setLocked(true);
		testTemplate.setModDate(new Date());
		testTemplate.setModUser(user.getUserId());
		testTemplate.setOwner(user.getUserId());
		testTemplate.setSelectedimage("");
		testTemplate.setShowOnMenu(true);
		testTemplate.setSortOrder(2);
		testTemplate.setTitle("JUnit Test Template");
		testTemplate.setType("template");
		testTemplate.setWorking(true);
		
		testTemplate = templateAPI.saveTemplate(testTemplate, testHost, user, false);
		
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
		
		folderAPI.save(testFolder1,user,false);
	
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
	
		testHTMLPage = new HTMLPage();
		testHTMLPage.setDeleted(deleted);
		testHTMLPage.setEndDate(endDate);
		testHTMLPage.setFriendlyName(friendlyName);
		testHTMLPage.setHttpsRequired(httpsRequired);
		testHTMLPage.setIDate(iDate);
		testHTMLPage.setLive(live);
		testHTMLPage.setLocked(locked);
		testHTMLPage.setMetadata(metadata);
		testHTMLPage.setModDate(modDate);
		
		modUser = user.getUserId();
		testHTMLPage.setModUser(modUser);
		
		owner = user.getUserId();
		testHTMLPage.setOwner(owner);
		
		testHTMLPage.setPageUrl(pageUrl);
		testHTMLPage.setRedirect(redirect);
		testHTMLPage.setShowOnMenu(showOnMenu);
		testHTMLPage.setSortOrder(sortOrder);
		testHTMLPage.setStartDate(startDate);
		testHTMLPage.setTitle(title);
		testHTMLPage.setType(type);
		testHTMLPage.setWebEndDate(webEndDate);
		testHTMLPage.setWebStartDate(webStartDate);
		testHTMLPage.setWorking(working);
		
		testHTMLPage = htmlPageAPI.saveHTMLPage(testHTMLPage, testTemplate, testFolder1, user, false);
		
		permissionList = new ArrayList<Permission>();
		permissionList.add(new Permission("", roleAPI.loadCMSAnonymousRole().getId(), PermissionAPI.PERMISSION_READ));
		
		Permission newPermission;
		for (Permission permission: permissionList) {
			newPermission = new Permission(testHTMLPage.getPermissionId(), permission.getRoleId(), permission.getPermission(), true);
			permissionAPI.save(newPermission, testHTMLPage, user, false);
		}
		
		MultiTreeFactory.saveMultiTree(new MultiTree(testHTMLPage.getIdentifier(), testContainer1.getIdentifier(), testContentlet1.getIdentifier()));
		MultiTreeFactory.saveMultiTree(new MultiTree(testHTMLPage.getIdentifier(), testContainer2.getIdentifier(), testContentlet2.getIdentifier()));
	}
	
	private void deleteJUnitTestHTMLPage() throws DotDataException, DotSecurityException, Exception {
		User user = userAPI.getSystemUser();
		
		htmlPageAPI.delete(testHTMLPage, user, false);
		
		contentletAPI.delete(testContentlet1, user, false);
		contentletAPI.delete(testContentlet2, user, false);
		
		containerAPI.delete(testContainer1, user, false);
		containerAPI.delete(testContainer2, user, false);
		
		templateAPI.delete(testTemplate, user, false);
		
		testStructure = StructureCache.getStructureByInode(testStructure.getInode());
		List<Field> fields = FieldsCache.getFieldsByStructureInode(testStructure.getInode());
		for (Field field: fields) {
			FieldFactory.deleteField(field);
		}
		
		FieldsCache.removeFields(testStructure);
		
		StructureFactory.deleteStructure(testStructure);
		
		StructureCache.removeStructure(testStructure);
		StructureServices.removeStructureFile(testStructure);
		
		folderAPI.delete(testFolder1,user,false);
		folderAPI.delete(testFolder2,user,false);
		
		hostAPI.delete(testHost, user, false);
	}
	
	public void testFindWorkingHTMLPages () throws Exception {
		User user = userAPI.getSystemUser();
		Host host = hostAPI.findDefaultHost(user, false);
		Folder f = folderAPI.findFolderByPath("/calendar",host,user,false);
		List<HTMLPage> pages = htmlPageAPI.findWorkingHTMLPages(f);
		assertEquals(5, pages.size());
		assertTrue(containsPageByURI(pages, "/calendar/add_event.dot"));
		assertTrue(containsPageByURI(pages, "/calendar/calendar_rss.dot"));
		assertTrue(containsPageByURI(pages, "/calendar/help.dot"));
		assertTrue(containsPageByURI(pages, "/calendar/index.dot"));
		
	}

	private boolean containsPageByURI (List<HTMLPage> pages, String uri) throws Exception {
		for(HTMLPage page : pages) {
			if(page.getURI().equals(uri))
				return true;
		}
		return false;
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
		HTMLPage testHTMLPageCopy1 = htmlPageAPI.copy(testHTMLPage, testFolder2, false, false, HTMLPageAPI.CopyMode.BLANK_HTMLPAGE, user, false);
		HTMLPage testHTMLPageCopy2 = htmlPageAPI.copy(testHTMLPage, testFolder2, false, false, HTMLPageAPI.CopyMode.USE_SOURCE_CONTENT, user, false);
		HTMLPage testHTMLPageCopy3 = null;
		
		try {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); 
			
			assertFalse("Invalid \"identifier\" in first copy htmlpage.", testHTMLPageCopy1.getIdentifier().equals(testHTMLPage.getIdentifier()));
			assertFalse("Invalid \"inode\" in first copy htmlpage.", testHTMLPageCopy1.getInode().equals(testHTMLPage.getInode()));
			assertEquals("Invalid \"endDate\" in first copy htmlpage.", sdf.format(endDate), sdf.format(testHTMLPageCopy1.getEndDate()));
			assertFalse("Invalid \"friendlyName\" in first copy htmlpage.", testHTMLPageCopy1.getFriendlyName().equals(friendlyName));
			assertEquals("Invalid \"httpsRequired\" in first copy htmlpage.", httpsRequired, testHTMLPageCopy1.isHttpsRequired());
			assertEquals("Invalid \"metadata\" in first copy htmlpage.", metadata, testHTMLPageCopy1.getMetadata());
			assertEquals("Invalid \"modUser\" in first copy htmlpage.", modUser, testHTMLPageCopy1.getModUser());
			assertEquals("Invalid \"pageUrl\" in first copy htmlpage.", pageUrl, testHTMLPageCopy1.getPageUrl());
			assertEquals("Invalid \"redirect\" in first copy htmlpage.", redirect, testHTMLPageCopy1.getRedirect());
			assertEquals("Invalid \"showOnMenu\" in first copy htmlpage.", showOnMenu, testHTMLPageCopy1.isShowOnMenu());
			assertEquals("Invalid \"sortOrder\" in first copy htmlpage.", sortOrder, testHTMLPageCopy1.getSortOrder());
			assertEquals("Invalid \"startDate\" in first copy htmlpage.", sdf.format(startDate), sdf.format(testHTMLPageCopy1.getStartDate()));
			assertEquals("Invalid \"title\" in first copy htmlpage.", title, testHTMLPageCopy1.getTitle());
			assertEquals("Invalid \"type\" in first copy htmlpage.", type, testHTMLPageCopy1.getType());
			assertFalse("Invalid \"uri\" in first copy htmlpage.", testHTMLPage.getURI().equals(testHTMLPageCopy1.getURI()));
			assertTrue("Invalid permissions assigned to first copy htmlpage.", checkPermission(testHTMLPageCopy1));
			
			Identifier tempIdentifier = APILocator.getIdentifierAPI().find(testHTMLPageCopy1.getTemplateId());
			List<Versionable> templates =  APILocator.getVersionableAPI().findAllVersions(tempIdentifier);
			Template tempTemplate = null;
			for (Versionable version: templates) {
				Template template =(Template)version;
				if (template.getIdentifier().equals(tempIdentifier.getInode()) && template.isWorking()) {
					tempTemplate = template;
					break;
				}
			}
			
			assertEquals("Invalid \"template\" in first copy htmlpage.", testTemplate.getIdentifier(), tempTemplate.getIdentifier());
			assertEquals("Another version of \"template\" was assigned in first copy htmlpage.", testTemplate.getInode(), tempTemplate.getInode());
			
			List<Container> containers = containerAPI.getContainersInTemplate(tempTemplate);
			assertEquals("Invalid number of containers used in the template in first copy htmlpage.", 2, containers.size());
			
			Container tempContainer = containers.get(0);
			if (testContainer1.getIdentifier().equals(tempContainer.getIdentifier())) {
				assertEquals("Invalid Container 1 \"inode\" in first copy htmlpage.", testContainer1.getInode(), tempContainer.getInode());
			} else if (testContainer2.getIdentifier().equals(tempContainer.getIdentifier())) {
				assertEquals("Invalid Container 2 \"inode\" in first copy htmlpage.", testContainer2.getInode(), tempContainer.getInode());
			} else {
				assertTrue("A new container was created", false);
			}
			
			tempContainer = containers.get(1);
			if (testContainer1.getIdentifier().equals(tempContainer.getIdentifier())) {
				assertEquals("Invalid Container 1 \"inode\" in first copy htmlpage.", testContainer1.getInode(), tempContainer.getInode());
			} else if (testContainer2.getIdentifier().equals(tempContainer.getIdentifier())) {
				assertEquals("Invalid Container 2 \"inode\" in first copy htmlpage.", testContainer2.getInode(), tempContainer.getInode());
			} else {
				assertTrue("A new container was created", false);
			}
			
			List<Contentlet> contents = contentletAPI.findByStructure(testStructure, user, false, 0, 0);
			assertEquals("Invalid number of contents created in first copy htmlpage.", 2, contents.size());
			
			List<Identifier> contentIds = MultiTreeFactory.getChildrenClassByCondition(testHTMLPageCopy1.getIdentifier(), testContainer1.getIdentifier(), Identifier.class, "1=1");
			assertEquals("Invalid number of contents assigned to Container 1 in first copy htmlpage.", 0, contentIds.size());
			
			contentIds = MultiTreeFactory.getChildrenClassByCondition(testHTMLPageCopy1.getIdentifier(), testContainer2.getIdentifier(), Identifier.class, "1=1");
			assertEquals("Invalid number of contents assigned to Container 2 in first copy htmlpage.", 0, contentIds.size());
			
			
			
			
			
			
			assertFalse("Invalid \"identifier\" in second copy htmlpage.", testHTMLPageCopy2.getIdentifier().equals(testHTMLPage.getIdentifier()));
			assertFalse("Invalid \"inode\" in second copy htmlpage.", testHTMLPageCopy2.getInode().equals(testHTMLPage.getInode()));
			assertEquals("Invalid \"endDate\" in second copy htmlpage.", sdf.format(endDate), sdf.format(testHTMLPageCopy2.getEndDate()));
			assertFalse("Invalid \"friendlyName\" in second copy htmlpage.", testHTMLPageCopy2.getFriendlyName().equals(friendlyName));
			assertEquals("Invalid \"httpsRequired\" in second copy htmlpage.", httpsRequired, testHTMLPageCopy2.isHttpsRequired());
			assertEquals("Invalid \"metadata\" in second copy htmlpage.", metadata, testHTMLPageCopy2.getMetadata());
			assertEquals("Invalid \"modUser\" in second copy htmlpage.", modUser, testHTMLPageCopy2.getModUser());
			assertFalse("Invalid \"pageUrl\" in second copy htmlpage.", pageUrl.equals(testHTMLPageCopy2.getPageUrl()));
			assertEquals("Invalid \"redirect\" in second copy htmlpage.", redirect, testHTMLPageCopy2.getRedirect());
			assertEquals("Invalid \"showOnMenu\" in second copy htmlpage.", showOnMenu, testHTMLPageCopy2.isShowOnMenu());
			assertEquals("Invalid \"sortOrder\" in second copy htmlpage.", sortOrder, testHTMLPageCopy2.getSortOrder());
			assertEquals("Invalid \"startDate\" in second copy htmlpage.", sdf.format(startDate), sdf.format(testHTMLPageCopy2.getStartDate()));
			assertEquals("Invalid \"title\" in second copy htmlpage.", title, testHTMLPageCopy2.getTitle());
			assertEquals("Invalid \"type\" in second copy htmlpage.", type, testHTMLPageCopy2.getType());
			assertFalse("Invalid \"uri\" in second copy htmlpage.", testHTMLPage.getURI().equals(testHTMLPageCopy2.getURI()));
			assertTrue("Invalid permissions assigned to second copy htmlpage.", checkPermission(testHTMLPageCopy2));
			
			tempIdentifier = APILocator.getIdentifierAPI().find(testHTMLPageCopy2.getTemplateId());
			templates = APILocator.getVersionableAPI().findAllVersions(tempIdentifier);
			tempTemplate = null;
			for (Versionable version: templates) {
				Template template = (Template)version;
				if (template.getIdentifier().equals(tempIdentifier.getInode()) && template.isWorking()) {
					tempTemplate = template;
					break;
				}
			}
			
			assertEquals("Invalid \"template\" in second copy htmlpage.", testTemplate.getIdentifier(), tempTemplate.getIdentifier());
			assertEquals("Another version of \"template\" was assigned in second copy htmlpage.", testTemplate.getInode(), tempTemplate.getInode());
			
			containers = containerAPI.getContainersInTemplate(tempTemplate);
			assertEquals("Invalid number of containers used in the template in second copy htmlpage.", 2, containers.size());
			
			tempContainer = containers.get(0);
			if (testContainer1.getIdentifier().equals(tempContainer.getIdentifier())) {
				assertEquals("Invalid Container 1 \"inode\" in second copy htmlpage.", testContainer1.getInode(), tempContainer.getInode());
			} else if (testContainer2.getIdentifier().equals(tempContainer.getIdentifier())) {
				assertEquals("Invalid Container 2 \"inode\" in second copy htmlpage.", testContainer2.getInode(), tempContainer.getInode());
			} else {
				assertTrue("A new container was created", false);
			}
			
			tempContainer = containers.get(1);
			if (testContainer1.getIdentifier().equals(tempContainer.getIdentifier())) {
				assertEquals("Invalid Container 1 \"inode\" in second copy htmlpage.", testContainer1.getInode(), tempContainer.getInode());
			} else if (testContainer2.getIdentifier().equals(tempContainer.getIdentifier())) {
				assertEquals("Invalid Container 2 \"inode\" in second copy htmlpage.", testContainer2.getInode(), tempContainer.getInode());
			} else {
				assertTrue("A new container was created", false);
			}
			
			contents = contentletAPI.findByStructure(testStructure, user, false, 0, 0);
			assertEquals("Invalid number of contents created in second copy htmlpage.", 2, contents.size());
			
			contentIds = MultiTreeFactory.getChildrenClassByCondition(testHTMLPageCopy2.getIdentifier(), testContainer1.getIdentifier(), Identifier.class, "1=1");
			assertEquals("Invalid number of contents assigned to Container 1 in second copy htmlpage.", 1, contentIds.size());
			Contentlet tempContent = null;
			for (Contentlet content: contents) {
				if (content.getIdentifier().equals(contentIds.get(0).getInode()) && content.isWorking()) {
					tempContent = content;
					break;
				}
			}
			assertEquals("Content 1 was copied in second copy htmlpage.", testContentlet1.getIdentifier(), tempContent.getIdentifier());
			assertEquals("Content 1 was overwritten in second second htmlpage.", testContentlet1.getInode(), tempContent.getInode());
			
			contentIds = MultiTreeFactory.getChildrenClassByCondition(testHTMLPageCopy2.getIdentifier(), testContainer2.getIdentifier(), Identifier.class, "1=1");
			assertEquals("Invalid number of contents assigned to Container 2 in second copy htmlpage.", 1, contentIds.size());
			tempContent = null;
			for (Contentlet content: contents) {
				if (content.getIdentifier().equals(contentIds.get(0).getInode()) && content.isWorking()) {
					tempContent = content;
					break;
				}
			}
			assertEquals("Content 2 was copied in second copy htmlpage.", testContentlet2.getIdentifier(), tempContent.getIdentifier());
			assertEquals("Content 2 was overwritten in second copy htmlpage.", testContentlet2.getInode(), tempContent.getInode());
			
			testHTMLPageCopy3 = htmlPageAPI.copy(testHTMLPage, testFolder2, false, false, HTMLPageAPI.CopyMode.COPY_SOURCE_CONTENT, user, false);
			
			assertFalse("Invalid \"identifier\" in third copy htmlpage.", testHTMLPageCopy3.getIdentifier().equals(testHTMLPage.getIdentifier()));
			assertFalse("Invalid \"inode\" in third copy htmlpage.", testHTMLPageCopy3.getInode().equals(testHTMLPage.getInode()));
			assertEquals("Invalid \"endDate\" in third copy htmlpage.", sdf.format(endDate), sdf.format(testHTMLPageCopy3.getEndDate()));
			assertFalse("Invalid \"friendlyName\" in third copy htmlpage.", testHTMLPageCopy3.getFriendlyName().equals(friendlyName));
			assertEquals("Invalid \"httpsRequired\" in third copy htmlpage.", httpsRequired, testHTMLPageCopy3.isHttpsRequired());
			assertEquals("Invalid \"metadata\" in third copy htmlpage.", metadata, testHTMLPageCopy3.getMetadata());
			assertEquals("Invalid \"modUser\" in third copy htmlpage.", modUser, testHTMLPageCopy3.getModUser());
			assertFalse("Invalid \"pageUrl\" in third copy htmlpage.", pageUrl.equals(testHTMLPageCopy3.getPageUrl()));
			assertEquals("Invalid \"redirect\" in third copy htmlpage.", redirect, testHTMLPageCopy3.getRedirect());
			assertEquals("Invalid \"showOnMenu\" in third copy htmlpage.", showOnMenu, testHTMLPageCopy3.isShowOnMenu());
			assertEquals("Invalid \"sortOrder\" in third copy htmlpage.", sortOrder, testHTMLPageCopy3.getSortOrder());
			assertEquals("Invalid \"startDate\" in third copy htmlpage.", sdf.format(startDate), sdf.format(testHTMLPageCopy3.getStartDate()));
			assertEquals("Invalid \"title\" in third copy htmlpage.", title, testHTMLPageCopy3.getTitle());
			assertEquals("Invalid \"type\" in third copy htmlpage.", type, testHTMLPageCopy3.getType());
			assertFalse("Invalid \"uri\" in third copy htmlpage.", testHTMLPage.getURI().equals(testHTMLPageCopy3.getURI()));
			assertTrue("Invalid permissions assigned to third copy htmlpage.", checkPermission(testHTMLPageCopy3));
			
			tempIdentifier = APILocator.getIdentifierAPI().find(testHTMLPageCopy3.getTemplateId());
			templates = APILocator.getVersionableAPI().findAllVersions(tempIdentifier);
			tempTemplate = null;
			for (Versionable version: templates) {
				Template template = (Template)version; 
				if (template.getIdentifier().equals(tempIdentifier.getInode()) && template.isWorking()) {
					tempTemplate = template;
					break;
				}
			}
			
			assertEquals("Invalid \"template\" in third copy htmlpage.", testTemplate.getIdentifier(), tempTemplate.getIdentifier());
			assertEquals("Another version of \"template\" was assigned in third copy htmlpage.", testTemplate.getInode(), tempTemplate.getInode());
			
			containers = containerAPI.getContainersInTemplate(tempTemplate);
			assertEquals("Invalid number of containers used in the template in third copy htmlpage.", 2, containers.size());
			
			tempContainer = containers.get(0);
			if (testContainer1.getIdentifier().equals(tempContainer.getIdentifier())) {
				assertEquals("Invalid Container 1 \"inode\" in third copy htmlpage.", testContainer1.getInode(), tempContainer.getInode());
			} else if (testContainer2.getIdentifier().equals(tempContainer.getIdentifier())) {
				assertEquals("Invalid Container 2 \"inode\" in third copy htmlpage.", testContainer2.getInode(), tempContainer.getInode());
			} else {
				assertTrue("A new container was created", false);
			}
			
			tempContainer = containers.get(1);
			if (testContainer1.getIdentifier().equals(tempContainer.getIdentifier())) {
				assertEquals("Invalid Container 1 \"inode\" in third copy htmlpage.", testContainer1.getInode(), tempContainer.getInode());
			} else if (testContainer2.getIdentifier().equals(tempContainer.getIdentifier())) {
				assertEquals("Invalid Container 2 \"inode\" in third copy htmlpage.", testContainer2.getInode(), tempContainer.getInode());
			} else {
				assertTrue("A new container was created", false);
			}
			
			contents = contentletAPI.findByStructure(testStructure, user, false, 0, 0);
			assertEquals("Invalid number of contents created in third copy htmlpage.", 4, contents.size());
			
			contentIds = MultiTreeFactory.getChildrenClassByCondition(testHTMLPageCopy3.getIdentifier(), testContainer1.getIdentifier(), Identifier.class, "1=1");
			assertEquals("Invalid number of contents assigned to Container 1 in third copy htmlpage.", 1, contentIds.size());
			tempContent = null;
			for (Contentlet content: contents) {
				if (content.getIdentifier().equals(contentIds.get(0).getInode()) && content.isWorking()) {
					tempContent = content;
					break;
				}
			}
			assertFalse("Content 1 was copied in third copy htmlpage.", testContentlet1.getIdentifier().equals(tempContent.getIdentifier()));
			assertFalse("Content 1 was overwritten in third third htmlpage.", testContentlet1.getInode().equals(tempContent.getInode()));
			
			contentIds = MultiTreeFactory.getChildrenClassByCondition(testHTMLPageCopy3.getIdentifier(), testContainer2.getIdentifier(), Identifier.class, "1=1");
			assertEquals("Invalid number of contents assigned to Container 2 in third copy htmlpage.", 1, contentIds.size());
			tempContent = null;
			for (Contentlet content: contents) {
				if (content.getIdentifier().equals(contentIds.get(0).getInode()) && content.isWorking()) {
					tempContent = content;
					break;
				}
			}
			assertFalse("Content 2 was copied in third copy htmlpage.", testContentlet2.getIdentifier().equals(tempContent.getIdentifier()));
			assertFalse("Content 2 was overwritten in third copy htmlpage.", testContentlet2.getInode().equals(tempContent.getInode()));
			
			List<HTMLPage> htmlPages = IdentifierFactory.getChildrenClassByCondition(testFolder2, HTMLPage.class, "working=" + DbConnectionFactory.getDBTrue());
			assertEquals("Invalid number of html pages created.", 3, (htmlPages.size()));
		} finally {
			htmlPageAPI.delete(testHTMLPageCopy1, user, false);
			htmlPageAPI.delete(testHTMLPageCopy2, user, false);
			if (testHTMLPageCopy3 != null) {
				htmlPageAPI.delete(testHTMLPageCopy3, user, false);
				
				List<Contentlet> contents = contentletAPI.findByStructure(testStructure, user, false, 0, 0);
				for (Contentlet content: contents) {
					if (!content.getIdentifier().equals(testContentlet1.getIdentifier()) && !content.getIdentifier().equals(testContentlet2.getIdentifier())) {
						contentletAPI.delete(content, user, false);
					}
				}
			}
		}
	}
	
	
	public void testCopyOverwrite() throws Exception {
		User user = userAPI.getSystemUser();
		HTMLPage testHTMLPageCopy1 = htmlPageAPI.copy(testHTMLPage, testFolder2, true, false, HTMLPageAPI.CopyMode.BLANK_HTMLPAGE, user, false);
		
		HTMLPage testHTMLPageCopy2 = null;
		Template testTemplateCopy2 = null;
		Container testContainerCopy2a = null;
		Container testContainerCopy2b = null;
		
		HTMLPage testHTMLPageCopy3 = null;
		Template testTemplateCopy3 = null;
		Container testContainerCopy3a = null;
		Container testContainerCopy3b = null;
		
		try {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); 
			
			assertFalse("Invalid \"identifier\" in first copy htmlpage.", testHTMLPageCopy1.getIdentifier().equals(testHTMLPage.getIdentifier()));
			assertFalse("Invalid \"inode\" in first copy htmlpage.", testHTMLPageCopy1.getInode().equals(testHTMLPage.getInode()));
			assertEquals("Invalid \"endDate\" in first copy htmlpage.", sdf.format(endDate), sdf.format(testHTMLPageCopy1.getEndDate()));
			assertFalse("Invalid \"friendlyName\" in first copy htmlpage.", testHTMLPageCopy1.getFriendlyName().equals(friendlyName));
			assertEquals("Invalid \"httpsRequired\" in first copy htmlpage.", httpsRequired, testHTMLPageCopy1.isHttpsRequired());
			assertEquals("Invalid \"metadata\" in first copy htmlpage.", metadata, testHTMLPageCopy1.getMetadata());
			assertEquals("Invalid \"modUser\" in first copy htmlpage.", modUser, testHTMLPageCopy1.getModUser());
			assertEquals("Invalid \"pageUrl\" in first copy htmlpage.", pageUrl, testHTMLPageCopy1.getPageUrl());
			assertEquals("Invalid \"redirect\" in first copy htmlpage.", redirect, testHTMLPageCopy1.getRedirect());
			assertEquals("Invalid \"showOnMenu\" in first copy htmlpage.", showOnMenu, testHTMLPageCopy1.isShowOnMenu());
			assertEquals("Invalid \"sortOrder\" in first copy htmlpage.", sortOrder, testHTMLPageCopy1.getSortOrder());
			assertEquals("Invalid \"startDate\" in first copy htmlpage.", sdf.format(startDate), sdf.format(testHTMLPageCopy1.getStartDate()));
			assertEquals("Invalid \"title\" in first copy htmlpage.", title, testHTMLPageCopy1.getTitle());
			assertEquals("Invalid \"type\" in first copy htmlpage.", type, testHTMLPageCopy1.getType());
			assertFalse("Invalid \"uri\" in first copy htmlpage.", testHTMLPage.getURI().equals(testHTMLPageCopy1.getURI()));
			assertTrue("Invalid permissions assigned to first copy htmlpage.", checkPermission(testHTMLPageCopy1));
			
			Identifier tempIdentifier = APILocator.getIdentifierAPI().find(testHTMLPageCopy1.getTemplateId());
			List<Versionable> templates = APILocator.getVersionableAPI().findAllVersions(tempIdentifier);
			Template tempTemplate = null;
			for (Versionable version: templates) {
				Template template = (Template)version;
				if (template.getIdentifier().equals(tempIdentifier.getInode()) && template.isWorking()) {
					tempTemplate = template;
					break;
				}
			}
			
			assertEquals("Invalid \"template\" in first copy htmlpage.", testTemplate.getIdentifier(), tempTemplate.getIdentifier());
			assertEquals("Another version of \"template\" was assigned in first copy htmlpage.", testTemplate.getInode(), tempTemplate.getInode());
			
			List<Container> containers = containerAPI.getContainersInTemplate(tempTemplate);
			assertEquals("Invalid number of containers used in the template in first copy htmlpage.", 2, containers.size());
			
			Container tempContainer = containers.get(0);
			if (testContainer1.getIdentifier().equals(tempContainer.getIdentifier())) {
				assertEquals("Invalid Container 1 \"inode\" in first copy htmlpage.", testContainer1.getInode(), tempContainer.getInode());
			} else if (testContainer2.getIdentifier().equals(tempContainer.getIdentifier())) {
				assertEquals("Invalid Container 2 \"inode\" in first copy htmlpage.", testContainer2.getInode(), tempContainer.getInode());
			} else {
				assertTrue("A new container was created", false);
			}
			
			tempContainer = containers.get(1);
			if (testContainer1.getIdentifier().equals(tempContainer.getIdentifier())) {
				assertEquals("Invalid Container 1 \"inode\" in first copy htmlpage.", testContainer1.getInode(), tempContainer.getInode());
			} else if (testContainer2.getIdentifier().equals(tempContainer.getIdentifier())) {
				assertEquals("Invalid Container 2 \"inode\" in first copy htmlpage.", testContainer2.getInode(), tempContainer.getInode());
			} else {
				assertTrue("A new container was created", false);
			}
			
			List<Contentlet> contents = contentletAPI.findByStructure(testStructure, user, false, 0, 0);
			assertEquals("Invalid number of contents created in first copy htmlpage.", 2, contents.size());
			
			List<Identifier> contentIds = MultiTreeFactory.getChildrenClassByCondition(testHTMLPageCopy1.getIdentifier(), testContainer1.getIdentifier(), Identifier.class, "1=1");
			assertEquals("Invalid number of contents assigned to Container 1 in first copy htmlpage.", 0, contentIds.size());
			
			contentIds = MultiTreeFactory.getChildrenClassByCondition(testHTMLPageCopy1.getIdentifier(), testContainer2.getIdentifier(), Identifier.class, "1=1");
			assertEquals("Invalid number of contents assigned to Container 2 in first copy htmlpage.", 0, contentIds.size());
			
			
			
			
			
			testHTMLPageCopy2 = htmlPageAPI.copy(testHTMLPage, testFolder2, true, true, HTMLPageAPI.CopyMode.COPY_SOURCE_CONTENT, user, false);
			
			tempIdentifier = APILocator.getIdentifierAPI().find(testHTMLPageCopy2.getTemplateId());
			templates = APILocator.getVersionableAPI().findAllVersions(tempIdentifier);
			tempTemplate = null;
			for (Versionable version: templates) {
				Template template = (Template)version;
				if (template.getIdentifier().equals(tempIdentifier.getInode()) && template.isWorking()) {
					tempTemplate = template;
					testTemplateCopy2 = template;
					break;
				}
			}
			
			containers = containerAPI.getContainersInTemplate(tempTemplate);
			testContainerCopy2a = containers.get(0);
			testContainerCopy2b = containers.get(1);
			
			assertFalse("Invalid \"identifier\" in second copy htmlpage.", testHTMLPageCopy2.getIdentifier().equals(testHTMLPage.getIdentifier()));
			assertFalse("Invalid \"inode\" in second copy htmlpage.", testHTMLPageCopy2.getInode().equals(testHTMLPage.getInode()));
			assertEquals("Invalid \"endDate\" in second copy htmlpage.", sdf.format(endDate), sdf.format(testHTMLPageCopy2.getEndDate()));
			assertFalse("Invalid \"friendlyName\" in second copy htmlpage.", testHTMLPageCopy2.getFriendlyName().equals(friendlyName));
			assertEquals("Invalid \"httpsRequired\" in second copy htmlpage.", httpsRequired, testHTMLPageCopy2.isHttpsRequired());
			assertEquals("Invalid \"metadata\" in second copy htmlpage.", metadata, testHTMLPageCopy2.getMetadata());
			assertEquals("Invalid \"modUser\" in second copy htmlpage.", modUser, testHTMLPageCopy2.getModUser());
			assertEquals("Invalid \"pageUrl\" in second copy htmlpage.", pageUrl, testHTMLPageCopy2.getPageUrl());
			assertEquals("Invalid \"redirect\" in second copy htmlpage.", redirect, testHTMLPageCopy2.getRedirect());
			assertEquals("Invalid \"showOnMenu\" in second copy htmlpage.", showOnMenu, testHTMLPageCopy2.isShowOnMenu());
			assertEquals("Invalid \"sortOrder\" in second copy htmlpage.", sortOrder, testHTMLPageCopy2.getSortOrder());
			assertEquals("Invalid \"startDate\" in second copy htmlpage.", sdf.format(startDate), sdf.format(testHTMLPageCopy2.getStartDate()));
			assertEquals("Invalid \"title\" in second copy htmlpage.", title, testHTMLPageCopy2.getTitle());
			assertEquals("Invalid \"type\" in second copy htmlpage.", type, testHTMLPageCopy2.getType());
			assertFalse("Invalid \"uri\" in second copy htmlpage.", testHTMLPage.getURI().equals(testHTMLPageCopy2.getURI()));
			assertTrue("Invalid permissions assigned to second copy htmlpage.", checkPermission(testHTMLPageCopy2));
			
			assertFalse("Invalid \"template\" in second copy htmlpage.", testTemplate.getIdentifier().equals(tempTemplate.getIdentifier()));
			assertFalse("Another version of \"template\" was assigned in second copy htmlpage.", testTemplate.getInode().equals(tempTemplate.getInode()));
			
			assertEquals("Invalid number of containers used in the template in second copy htmlpage.", 2, containers.size());
			
			tempContainer = containers.get(0);
			if (testContainer1.getIdentifier().equals(tempContainer.getIdentifier()) || testContainer2.getIdentifier().equals(tempContainer.getIdentifier())) {
				assertTrue("Container 1 was not copy in second copy htmlpage.", false);
			}
			
			tempContainer = containers.get(1);
			if (testContainer1.getIdentifier().equals(tempContainer.getIdentifier()) || testContainer2.getIdentifier().equals(tempContainer.getIdentifier())) {
				assertTrue("Container 2 was not copy in second copy htmlpage.", false);
			}
			
			contents = contentletAPI.findByStructure(testStructure, user, false, 0, 0);
			assertEquals("Invalid number of contents created in second copy htmlpage.", 2, contents.size());
			
			contentIds = MultiTreeFactory.getChildrenClassByCondition(testHTMLPageCopy2.getIdentifier(), testContainerCopy2a.getIdentifier(), Identifier.class, "1=1");
			assertEquals("Invalid number of contents assigned to Container 1 in second copy htmlpage.", 1, contentIds.size());
			Contentlet tempContent = null;
			for (Contentlet content: contents) {
				if (content.getIdentifier().equals(contentIds.get(0).getInode()) && content.isWorking()) {
					tempContent = content;
					break;
				}
			}
			if (testContentlet1.getIdentifier().equals(tempContent.getIdentifier())) {
				assertEquals("Content 1 was copied in second copy htmlpage.", testContentlet1.getIdentifier(), tempContent.getIdentifier());
				assertEquals("Content 1 was overwritten in second second htmlpage.", testContentlet1.getInode(), tempContent.getInode());
			} else {
				assertEquals("Content 2 was copied in second copy htmlpage.", testContentlet2.getIdentifier(), tempContent.getIdentifier());
				assertEquals("Content 2 was overwritten in second copy htmlpage.", testContentlet2.getInode(), tempContent.getInode());
			}
			
			contentIds = MultiTreeFactory.getChildrenClassByCondition(testHTMLPageCopy2.getIdentifier(), testContainerCopy2b.getIdentifier(), Identifier.class, "1=1");
			assertEquals("Invalid number of contents assigned to Container 2 in second copy htmlpage.", 1, contentIds.size());
			tempContent = null;
			for (Contentlet content: contents) {
				if (content.getIdentifier().equals(contentIds.get(0).getInode()) && content.isWorking()) {
					tempContent = content;
					break;
				}
			}
			if (testContentlet1.getIdentifier().equals(tempContent.getIdentifier())) {
				assertEquals("Content 1 was copied in second copy htmlpage.", testContentlet1.getIdentifier(), tempContent.getIdentifier());
				assertEquals("Content 1 was overwritten in second second htmlpage.", testContentlet1.getInode(), tempContent.getInode());
			} else {
				assertEquals("Content 2 was copied in second copy htmlpage.", testContentlet2.getIdentifier(), tempContent.getIdentifier());
				assertEquals("Content 2 was overwritten in second copy htmlpage.", testContentlet2.getInode(), tempContent.getInode());
			}
			testHTMLPageCopy3 = htmlPageAPI.copy(testHTMLPage, testFolder2, true, true, HTMLPageAPI.CopyMode.COPY_SOURCE_CONTENT, user, false);
			
			tempIdentifier = APILocator.getIdentifierAPI().find(testHTMLPageCopy3.getTemplateId());
			templates = APILocator.getVersionableAPI().findAllVersions(tempIdentifier);
			tempTemplate = null;
			for (Versionable version : templates) {
				Template template = (Template)version;
				if (template.getIdentifier().equals(tempIdentifier.getInode()) && template.isWorking()) {
					tempTemplate = template;
					testTemplateCopy3 = template;
					break;
				}
			}
			
			containers = containerAPI.getContainersInTemplate(tempTemplate);
			testContainerCopy3a = containers.get(0);
			testContainerCopy3b = containers.get(1);
			
			assertFalse("Invalid \"identifier\" in third copy htmlpage.", testHTMLPageCopy3.getIdentifier().equals(testHTMLPage.getIdentifier()));
			assertFalse("Invalid \"inode\" in third copy htmlpage.", testHTMLPageCopy3.getInode().equals(testHTMLPage.getInode()));
			assertEquals("Invalid \"endDate\" in third copy htmlpage.", sdf.format(endDate), sdf.format(testHTMLPageCopy3.getEndDate()));
			assertFalse("Invalid \"friendlyName\" in third copy htmlpage.", testHTMLPageCopy3.getFriendlyName().equals(friendlyName));
			assertEquals("Invalid \"httpsRequired\" in third copy htmlpage.", httpsRequired, testHTMLPageCopy3.isHttpsRequired());
			assertEquals("Invalid \"metadata\" in third copy htmlpage.", metadata, testHTMLPageCopy3.getMetadata());
			assertEquals("Invalid \"modUser\" in third copy htmlpage.", modUser, testHTMLPageCopy3.getModUser());
			assertEquals("Invalid \"pageUrl\" in third copy htmlpage.", pageUrl, testHTMLPageCopy3.getPageUrl());
			assertEquals("Invalid \"redirect\" in third copy htmlpage.", redirect, testHTMLPageCopy3.getRedirect());
			assertEquals("Invalid \"showOnMenu\" in third copy htmlpage.", showOnMenu, testHTMLPageCopy3.isShowOnMenu());
			assertEquals("Invalid \"sortOrder\" in third copy htmlpage.", sortOrder, testHTMLPageCopy3.getSortOrder());
			assertEquals("Invalid \"startDate\" in third copy htmlpage.", sdf.format(startDate), sdf.format(testHTMLPageCopy3.getStartDate()));
			assertEquals("Invalid \"title\" in third copy htmlpage.", title, testHTMLPageCopy3.getTitle());
			assertEquals("Invalid \"type\" in third copy htmlpage.", type, testHTMLPageCopy3.getType());
			assertFalse("Invalid \"uri\" in third copy htmlpage.", testHTMLPage.getURI().equals(testHTMLPageCopy3.getURI()));
			assertTrue("Invalid permissions assigned to third copy htmlpage.", checkPermission(testHTMLPageCopy3));
			
			assertFalse("Invalid \"template\" in third copy htmlpage.", testTemplate.getIdentifier().equals(tempTemplate.getIdentifier()));
			assertFalse("Another version of \"template\" was assigned in third copy htmlpage.", testTemplate.getInode().equals(tempTemplate.getInode()));
			
			assertEquals("Invalid number of containers used in the template in third copy htmlpage.", 2, containers.size());
			
			tempContainer = containers.get(0);
			if (testContainer1.getIdentifier().equals(tempContainer.getIdentifier()) || testContainer2.getIdentifier().equals(tempContainer.getIdentifier())) {
				assertTrue("Container 1 was not copy in third copy htmlpage.", false);
			}
			
			tempContainer = containers.get(1);
			if (testContainer1.getIdentifier().equals(tempContainer.getIdentifier()) || testContainer2.getIdentifier().equals(tempContainer.getIdentifier())) {
				assertTrue("Container 2 was not copy in third copy htmlpage.", false);
			}
			
			contents = contentletAPI.findByStructure(testStructure, user, false, 0, 0);
			assertEquals("Invalid number of contents created in third copy htmlpage.", 4, contents.size());
			
			contentIds = MultiTreeFactory.getChildrenClassByCondition(testHTMLPageCopy3.getIdentifier(), testContainerCopy3a.getIdentifier(), Identifier.class, "1=1");
			assertEquals("Invalid number of contents assigned to Container 1 in third copy htmlpage.", 1, contentIds.size());
			tempContent = null;
			for (Contentlet content: contents) {
				if (content.getIdentifier().equals(contentIds.get(0).getInode()) && content.isWorking()) {
					tempContent = content;
					break;
				}
			}
			assertFalse("Content 1 was copied in third copy htmlpage.", testContentlet1.getIdentifier().equals(tempContent.getIdentifier()));
			assertFalse("Content 1 was overwritten in third third htmlpage.", testContentlet1.getInode().equals(tempContent.getInode()));
			assertFalse("Content 1 was copied in third copy htmlpage.", testContentlet2.getIdentifier().equals(tempContent.getIdentifier()));
			assertFalse("Content 1 was overwritten in third copy htmlpage.", testContentlet2.getInode().equals(tempContent.getInode()));
			
			contentIds = MultiTreeFactory.getChildrenClassByCondition(testHTMLPageCopy3.getIdentifier(), testContainerCopy3b.getIdentifier(), Identifier.class, "1=1");
			assertEquals("Invalid number of contents assigned to Container 2 in third copy htmlpage.", 1, contentIds.size());
			tempContent = null;
			for (Contentlet content: contents) {
				if (content.getIdentifier().equals(contentIds.get(0).getInode()) && content.isWorking()) {
					tempContent = content;
					break;
				}
			}
			assertFalse("Content 2 was copied in third copy htmlpage.", testContentlet1.getIdentifier().equals(tempContent.getIdentifier()));
			assertFalse("Content 2 was overwritten in third third htmlpage.", testContentlet1.getInode().equals(tempContent.getInode()));
			assertFalse("Content 2 was copied in third copy htmlpage.", testContentlet2.getIdentifier().equals(tempContent.getIdentifier()));
			assertFalse("Content 2 was overwritten in third copy htmlpage.", testContentlet2.getInode().equals(tempContent.getInode()));
			
			List<HTMLPage> htmlPages = IdentifierFactory.getChildrenClassByCondition(testFolder2, HTMLPage.class, "working=" + DbConnectionFactory.getDBTrue());
			assertEquals("Invalid number of html pages created.", 1, (htmlPages.size()));
			
			Identifier identifier = identifierAPI.findFromInode(testHTMLPageCopy3.getIdentifier());
			List<Versionable> allVersions =  APILocator.getVersionableAPI().findAllVersions(identifier);
			assertTrue("Invalid number of version: " + allVersions.size(), (allVersions.size() == 3));
		} finally {
			boolean pageDeleted = false;
			if (testHTMLPageCopy3 != null) {
				htmlPageAPI.delete(testHTMLPageCopy3, user, false);
				pageDeleted = true;
				
				if (testTemplateCopy3 != null)
					templateAPI.delete(testTemplateCopy3, user, false);
				
				if (testContainerCopy3a != null)
					containerAPI.delete(testContainerCopy3a, user, false);
				
				if (testContainerCopy3b != null)
					containerAPI.delete(testContainerCopy3b, user, false);
				
				List<Contentlet> contents = contentletAPI.findByStructure(testStructure, user, false, 0, 0);
				for (Contentlet content: contents) {
					if (!content.getIdentifier().equals(testContentlet1.getIdentifier()) && !content.getIdentifier().equals(testContentlet2.getIdentifier())) {
						contentletAPI.delete(content, user, false);
					}
				}
			}
			
			if (testHTMLPageCopy2 != null) {
				if (!pageDeleted) {
					htmlPageAPI.delete(testHTMLPageCopy2, user, false);
					pageDeleted = true;
				}
				
				if (testTemplateCopy2 != null)
					templateAPI.delete(testTemplateCopy2, user, false);
				
				if (testContainerCopy2a != null)
					containerAPI.delete(testContainerCopy2a, user, false);
				
				if (testContainerCopy2b != null)
					containerAPI.delete(testContainerCopy2b, user, false);
			}
			
			if (!pageDeleted) {
				htmlPageAPI.delete(testHTMLPageCopy1, user, false);
			}
		}
	}
}