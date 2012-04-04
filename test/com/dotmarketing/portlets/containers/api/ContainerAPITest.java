package com.dotmarketing.portlets.containers.api;

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
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.business.Versionable;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.IdentifierFactory;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.factories.TreeFactory;
import com.dotmarketing.portlets.containers.business.ContainerAPI;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.entities.model.Entity;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.templates.factories.TemplateFactory;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;

/**
 * 
 * This test assumes you are running again a fresh dotcms installation based on
 * the starter site
 * 
 * @author David Torres
 * @version 1.9
 * @since 1.9
 * 
 */
public class ContainerAPITest extends ServletTestCase {
	private static UserAPI userAPI;
	private static HostAPI hostAPI;
	private static RoleAPI roleAPI;
	private static PermissionAPI permissionAPI;
	private static ContainerAPI containerAPI;
	private static IdentifierAPI identifierAPI;
	
	private static Container testContainer;
	private static String code = "$Body";
	private static boolean deleted = false;
	private static String friendlyName = "JUnit Test Container Friendly Name";
	private static Date iDate = new Date();
	private static boolean live = false;
	private static boolean locked = true;
	private static String luceneQuery = "lucene - query";
	private static int maxContentlets = 1;
	private static Date modDate = new Date();
	private static String modUser;
	private static String notes = "Test Container";
	private static String owner;
	private static String postLoop = "post loop code";
	private static String preLoop = "pre loop code";
	private static boolean showOnMenu = true;
	private static String sortContentletsBy = "";
	private static int sortOrder = 2;
	private static boolean staticify = true;
	private static String title = "JUnit Test Container";
	private static String type = "containers";
	private static boolean useDiv = true;
	private static boolean working = true;
	
	private static Structure testStructure;
	private static Host testHost1;
	private static Host testHost2;
	
	private static List<Permission> permissionList;
	
	protected void setUp() throws Exception {
		userAPI = APILocator.getUserAPI();
		hostAPI = APILocator.getHostAPI();
		roleAPI = APILocator.getRoleAPI();
		permissionAPI = APILocator.getPermissionAPI();
		containerAPI = APILocator.getContainerAPI();
		identifierAPI = APILocator.getIdentifierAPI();
		
		createJUnitTestContainer();
	}
	
	protected void tearDown() throws Exception {
		deleteJUnitTestContainer();
	}
	
	private void createJUnitTestContainer() throws Exception {
		testContainer = new Container();
		testContainer.setCode(code);
		testContainer.setDeleted(deleted);
		testContainer.setFriendlyName(friendlyName);
		testContainer.setIDate(iDate);
		testContainer.setLive(live);
		testContainer.setLocked(locked);
		testContainer.setLuceneQuery(luceneQuery);
		testContainer.setMaxContentlets(maxContentlets);
		testContainer.setModDate(modDate);
		
		User user = userAPI.getSystemUser();
		modUser = user.getUserId();
		testContainer.setModUser(modUser);
		
		testContainer.setNotes(notes);
		
		owner = user.getUserId();
		testContainer.setOwner(owner);
		
		testContainer.setPostLoop(postLoop);
		testContainer.setPreLoop(preLoop);
		testContainer.setShowOnMenu(showOnMenu);
		testContainer.setSortContentletsBy(sortContentletsBy);
		testContainer.setSortOrder(sortOrder);
		testContainer.setStaticify(staticify);
		testContainer.setTitle(title);
		testContainer.setType(type);
		testContainer.setUseDiv(useDiv);
		testContainer.setWorking(working);
		
		testHost1 = new Host();
		testHost1.setHostname("dotcms_junit_test_host_1");
		testHost1.setModDate(new Date());
		testHost1.setModUser(user.getUserId());
		testHost1.setOwner(user.getUserId());
		testHost1.setProperty("theme", "default");
		testHost1 = hostAPI.save(testHost1, user, false);
		
		testHost2 = new Host();
		testHost2.setHostname("dotcms_junit_test_host_2");
		testHost2.setModDate(new Date());
		testHost2.setModUser(user.getUserId());
		testHost2.setOwner(user.getUserId());
		testHost2.setProperty("theme", "default");
		testHost2 = hostAPI.save(testHost2, user, false);
		
		testStructure = new Structure();
		testStructure.setDefaultStructure(false);
		testStructure.setDescription("JUnit Test Structure Description.");
		testStructure.setFixed(false);
		testStructure.setIDate(new Date());
		testStructure.setName("JUnit Test Structure.");
		testStructure.setOwner(user.getUserId());
		testStructure.setDetailPage("");
		testStructure.setStructureType(Structure.STRUCTURE_TYPE_CONTENT);
		testStructure.setSystem(true);
		testStructure.setType("structure");
		testStructure.setVelocityVarName("junit_test_structure");
		StructureFactory.saveStructure(testStructure);
		Entity entity = new Entity();
		entity.setEntityName(testStructure.getName());
		HibernateUtil.saveOrUpdate(entity);
		TreeFactory.saveTree(new Tree(entity.getInode(), testStructure.getInode(), WebKeys.Structure.STRUCTURE_ENTITY, 0));
		
		testContainer = containerAPI.save(testContainer, testStructure, testHost1, user, false);
		
		permissionList = new ArrayList<Permission>();
		permissionList.add(new Permission("", roleAPI.loadCMSAnonymousRole().getId(), PermissionAPI.PERMISSION_READ));
		
		Permission newPermission;
		for (Permission permission: permissionList) {
			newPermission = new Permission(testContainer.getPermissionId(), permission.getRoleId(), permission.getPermission(), true);
			permissionAPI.save(newPermission, testContainer, user, false);
		}
	}
	
	private void deleteJUnitTestContainer() throws Exception {
		User user = userAPI.getSystemUser();
		
		containerAPI.delete(testContainer, user, false);
		testStructure = StructureCache.getStructureByInode(testStructure.getInode());
		StructureFactory.deleteStructure(testStructure);
		
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
	
	public void testCopy() throws Exception {
		User user = userAPI.getSystemUser();
		Container testContainerCopy = containerAPI.copy(testContainer, testHost1, user, false);
		
		try {
			assertFalse("Invalid \"identifier\" in copy container.", testContainerCopy.getIdentifier().equals(testContainer.getIdentifier()));
			assertFalse("Invalid \"inode\" in copy container.", testContainerCopy.getInode().equals(testContainer.getInode()));
			assertEquals("Invalid \"code\" in copy container.", code, testContainerCopy.getCode());
			assertFalse("Invalid \"friendlyName\" in copy container.", testContainerCopy.getFriendlyName().equals(friendlyName));
			assertEquals("Invalid \"luceneQuery\" in copy container.", luceneQuery, testContainerCopy.getLuceneQuery());
			assertEquals("Invalid \"maxContentlets\" in copy container.", maxContentlets, testContainerCopy.getMaxContentlets());
			assertEquals("Invalid \"modUser\" in copy container.", modUser, testContainerCopy.getModUser());
			assertEquals("Invalid \"notes\" in copy container.", notes, testContainerCopy.getNotes());
			assertEquals("Invalid \"postLoop\" in copy container.", postLoop, testContainerCopy.getPostLoop());
			assertEquals("Invalid \"preLoop\" in copy container.", preLoop, testContainerCopy.getPreLoop());
			assertEquals("Invalid \"sortContentletsBy\" in copy container.", sortContentletsBy, testContainerCopy.getSortContentletsBy());
			assertEquals("Invalid \"sortOrder\" in copy container.", sortOrder, testContainerCopy.getSortOrder());
			assertEquals("Invalid \"showOnMenu\" in copy container.", showOnMenu, testContainerCopy.isShowOnMenu());
			assertEquals("Invalid \"staticify\" in copy container.", staticify, testContainerCopy.isStaticify());
			
			Structure structure = (Structure) InodeFactory.getInode(testContainerCopy.getStructureInode(), Structure.class);
			assertTrue("Invalid \"structure\" in copy container.", structure.getInode().equals(testStructure.getInode()));
			
			assertEquals("Invalid \"useDiv\" in copy container.", useDiv, testContainerCopy.isUseDiv());
			assertFalse("Invalid \"title\" in copy container.", testContainerCopy.getTitle().equals(title));
			assertEquals("Invalid \"type\" in copy container.", type, testContainerCopy.getType());
			assertTrue("Invalid permissions assigned to copy container.", checkPermission(testContainerCopy));
		} finally {
			containerAPI.delete(testContainerCopy, user, false);
		}
	}
	
	public void testCopyToAnotherHosts() throws Exception {
		User user = userAPI.getSystemUser();
		Container testContainerCopy1 = containerAPI.copy(testContainer, testHost2, user, false);
		Container testContainerCopy2 = containerAPI.copy(testContainer, testHost2, user, false);
		
		try {
			assertFalse("Invalid \"identifier\" in first copy container.", testContainerCopy1.getIdentifier().equals(testContainer.getIdentifier()));
			assertFalse("Invalid \"inode\" in first copy container.", testContainerCopy1.getInode().equals(testContainer.getInode()));
			assertEquals("Invalid \"code\" in first copy container.", code, testContainerCopy1.getCode());
			assertTrue("Invalid \"friendlyName\" in first copy container.", testContainerCopy1.getFriendlyName().equals(friendlyName));
			assertEquals("Invalid \"luceneQuery\" in first copy container.", luceneQuery, testContainerCopy1.getLuceneQuery());
			assertEquals("Invalid \"maxContentlets\" in first copy container.", maxContentlets, testContainerCopy1.getMaxContentlets());
			assertEquals("Invalid \"modUser\" in first copy container.", modUser, testContainerCopy1.getModUser());
			assertEquals("Invalid \"notes\" in first copy container.", notes, testContainerCopy1.getNotes());
			assertEquals("Invalid \"postLoop\" in first copy container.", postLoop, testContainerCopy1.getPostLoop());
			assertEquals("Invalid \"preLoop\" in first copy container.", preLoop, testContainerCopy1.getPreLoop());
			assertEquals("Invalid \"sortContentletsBy\" in first copy container.", sortContentletsBy, testContainerCopy1.getSortContentletsBy());
			assertEquals("Invalid \"sortOrder\" in first copy container.", sortOrder, testContainerCopy1.getSortOrder());
			assertEquals("Invalid \"showOnMenu\" in first copy container.", showOnMenu, testContainerCopy1.isShowOnMenu());
			assertEquals("Invalid \"staticify\" in first copy container.", staticify, testContainerCopy1.isStaticify());
			
			Structure structure = (Structure) InodeFactory.getInode(testContainerCopy1.getStructureInode(), Structure.class);
			assertTrue("Invalid \"structure\" in first copy container.", structure.getInode().equals(testStructure.getInode()));
			
			assertEquals("Invalid \"useDiv\" in first copy container.", useDiv, testContainerCopy1.isUseDiv());
			assertTrue("Invalid \"title\" in first copy container.", testContainerCopy1.getTitle().equals(title));
			assertEquals("Invalid \"type\" in first copy container.", type, testContainerCopy1.getType());
			assertTrue("Invalid permissions assigned to first copy container.", checkPermission(testContainerCopy1));
			
			Identifier identifier = identifierAPI.findFromInode(testContainerCopy1.getIdentifier());
			List<Versionable> allVersions = APILocator.getVersionableAPI().findAllVersions(identifier);
			assertTrue("Invalid number of version in first copy container: " + allVersions.size(), (allVersions.size() == 1));
			
			assertFalse("Invalid \"identifier\" in second copy container.", testContainerCopy2.getIdentifier().equals(testContainer.getIdentifier()));
			assertFalse("Invalid \"inode\" in second copy container.", testContainerCopy2.getInode().equals(testContainer.getInode()));
			assertEquals("Invalid \"code\" in second copy container.", code, testContainerCopy2.getCode());
			assertFalse("Invalid \"friendlyName\" in second copy container.", testContainerCopy2.getFriendlyName().equals(friendlyName));
			assertEquals("Invalid \"luceneQuery\" in second copy container.", luceneQuery, testContainerCopy2.getLuceneQuery());
			assertEquals("Invalid \"maxContentlets\" in second copy container.", maxContentlets, testContainerCopy2.getMaxContentlets());
			assertEquals("Invalid \"modUser\" in second copy container.", modUser, testContainerCopy2.getModUser());
			assertEquals("Invalid \"notes\" in second copy container.", notes, testContainerCopy2.getNotes());
			assertEquals("Invalid \"postLoop\" in second copy container.", postLoop, testContainerCopy2.getPostLoop());
			assertEquals("Invalid \"preLoop\" in second copy container.", preLoop, testContainerCopy2.getPreLoop());
			assertEquals("Invalid \"sortContentletsBy\" in second copy container.", sortContentletsBy, testContainerCopy2.getSortContentletsBy());
			assertEquals("Invalid \"sortOrder\" in second copy container.", sortOrder, testContainerCopy2.getSortOrder());
			assertEquals("Invalid \"showOnMenu\" in second copy container.", showOnMenu, testContainerCopy2.isShowOnMenu());
			assertEquals("Invalid \"staticify\" in second copy container.", staticify, testContainerCopy2.isStaticify());
			
			structure = (Structure) InodeFactory.getInode(testContainerCopy2.getStructureInode(), Structure.class);
			assertTrue("Invalid \"structure\" in copy second container.", structure.getInode().equals(testStructure.getInode()));
			
			assertEquals("Invalid \"useDiv\" in second copy container.", useDiv, testContainerCopy2.isUseDiv());
			assertFalse("Invalid \"title\" in second copy container.", testContainerCopy2.getTitle().equals(title));
			assertEquals("Invalid \"type\" in second copy container.", type, testContainerCopy2.getType());
			assertTrue("Invalid permissions assigned to second copy container.", checkPermission(testContainerCopy2));
			
			identifier = identifierAPI.findFromInode(testContainerCopy2.getIdentifier());
			allVersions =  APILocator.getVersionableAPI().findAllVersions(identifier);
			assertTrue("Invalid number of version in second copy container: " + allVersions.size(), (allVersions.size() == 1));
			
			List<Container> containers = containerAPI.findContainersUnder(testHost2);
			
			assertTrue("Invalid number of containers created: " + containers.size(), (containers.size() == 2));
		} finally {
			containerAPI.delete(testContainerCopy1, user, false);
			containerAPI.delete(testContainerCopy2, user, false);
		}
	}
	
	public void testGetContainersInTemplate() throws DotStateException, DotHibernateException, DotDataException, DotSecurityException {
		List<Container> containers = APILocator.getContainerAPI().getContainersInTemplate(
				TemplateFactory.getTemplateByCondition("title = 'dotCMS Starter - Blank Template'").get(0));
		assertEquals(1, containers.size());
		assertEquals("Starter - Body Container 1", containers.get(0).getTitle().trim());

	}
	
	public boolean containsContainerByTitle (String title, List<Container> containers) {
		
		for (Container cont: containers) {
			if (cont.getTitle().trim().equals(title)) return true;
		}
		return false;
		
	}
	
	public void testFindContainersUnder() throws DotDataException {
		
		Host defaultHost;
		try {
			defaultHost = APILocator.getHostAPI().findDefaultHost(APILocator.getUserAPI().getSystemUser(), false);
		} catch (DotDataException e) {
			throw new DotRuntimeException(e.getMessage(), e);
		} catch (DotSecurityException e) {
			throw new DotRuntimeException(e.getMessage(), e);
		}
		List<Container> containers = APILocator.getContainerAPI().findContainersUnder(defaultHost);
		assertEquals(11, containers.size());
		assertTrue(containsContainerByTitle("Starter - Body Container 1", containers));
		assertTrue(containsContainerByTitle("Starter - Body Container 2", containers));
		assertTrue(containsContainerByTitle("Starter - Body Container 3", containers));
		assertTrue(containsContainerByTitle("Starter - Body Container 4", containers));
		assertTrue(containsContainerByTitle("Starter - Body Container 5", containers));
		assertTrue(containsContainerByTitle("Starter - Document Header", containers));
		assertTrue(containsContainerByTitle("Starter - JQuery Accordion", containers));
		assertTrue(containsContainerByTitle("Starter - Promotion", containers));
		
	}
}