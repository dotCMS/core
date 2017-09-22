package com.dotmarketing.business;

import com.dotcms.IntegrationTestBase;
import com.dotcms.LicenseTestUtil;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.*;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.exception.WebAssetException;
import com.dotmarketing.factories.MultiTreeFactory;
import com.dotmarketing.factories.PublishFactory;
import com.dotmarketing.factories.WebAssetFactory;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.containers.business.ContainerAPI;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPIImpl;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.rules.model.Rule;
import com.dotmarketing.portlets.structure.factories.FieldFactory;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.templates.business.TemplateAPI;
import com.dotmarketing.portlets.templates.design.bean.TemplateLayout;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.*;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.ejb.UserTestUtil;
import com.liferay.portal.model.User;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;

/**
 * 
 * @author Oswaldo Gallango
 *
 */

public class UserAPITest extends IntegrationTestBase {

	protected static UserAPI userAPI;
	protected static RoleAPI roleAPI;
	protected static PermissionAPI perAPI;
	protected static HostAPI hostAPI;
	protected static TemplateAPI templateAPI;
	protected static ContainerAPI containerAPI;
	protected static ContentletAPI conAPI;
	protected static LayoutAPI layoutAPI;
	protected static VersionableAPI versionableAPI;
	protected static HTMLPageAssetAPI htmlPageAssetAPI;
	protected static FolderAPI folderAPI;
	protected static IdentifierAPI identifierAPI;
	protected static WorkflowAPI workflowAPI;

	private static User systemUser;

	@BeforeClass
	public static void prepare () throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();

		LicenseTestUtil.getLicense();

		userAPI = APILocator.getUserAPI();
		roleAPI = APILocator.getRoleAPI();
		perAPI = APILocator.getPermissionAPI();
		hostAPI = APILocator.getHostAPI();
		templateAPI = APILocator.getTemplateAPI();
		containerAPI = APILocator.getContainerAPI();
		conAPI = APILocator.getContentletAPI();
		layoutAPI = APILocator.getLayoutAPI();
		versionableAPI = APILocator.getVersionableAPI();
		htmlPageAssetAPI = APILocator.getHTMLPageAssetAPI();
		folderAPI = APILocator.getFolderAPI();
		identifierAPI = APILocator.getIdentifierAPI();
		workflowAPI = APILocator.getWorkflowAPI();

		//Setting the test user
		systemUser = APILocator.getUserAPI().getSystemUser();
		//setDebugMode(true);
	}

	/*@AfterClass
	public static void cleanup() throws DotDataException, DotSecurityException {

		cleanupDebug(UserAPITest.class);
	}*/

	/**
	 * Testing {@link UserAPI#delete(User, User, User, boolean)}
	 *
	 * @throws DotDataException If the user to delete or the replacement user are not set
	 * @throws DotSecurityException If the user requesting the delete doesn't have permission
	 * @throws SystemException 
	 * @throws PortalException 
	 * @throws WebAssetException 
	 * @throws IOException If the url is malformed or if there is an issue opening the connection stream 
	 */
	@Test
	public void delete() throws Exception {

		long langId =APILocator.getLanguageAPI().getDefaultLanguage().getId();
		String id = String.valueOf( new Date().getTime());

		/**
		 * Add host
		 */
		Host host = new Host();
		host.setHostname("test"+id+".demo.dotcms.com");
		host=hostAPI.save(host, systemUser, false);

		/**
		 * Add role
		 */
		Role newRole = new Role();
		String newRoleName = "role"+id;
		newRole.setName(newRoleName);
		newRole.setRoleKey(newRoleName);
		newRole.setEditLayouts(true);
		newRole.setEditPermissions(true);
		newRole.setEditUsers(true);
		newRole.setSystem(false);
		newRole = roleAPI.save(newRole);

		/**
		 * Set permission to role
		 */
		List<Permission> permissionsToSave = new ArrayList<Permission>();
		permissionsToSave.add(new Permission(PermissionAPI.INDIVIDUAL_PERMISSION_TYPE, host.getPermissionId(), newRole.getId(), (PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_WRITE | PermissionAPI.PERMISSION_CAN_ADD_CHILDREN | PermissionAPI.PERMISSION_EDIT_PERMISSIONS), true));
		permissionsToSave.add(new Permission(Host.class.getCanonicalName(), host.getPermissionId(), newRole.getId(), 0, true));
		permissionsToSave.add(new Permission(Folder.class.getCanonicalName(), host.getPermissionId(), newRole.getId(), (PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_WRITE | PermissionAPI.PERMISSION_CAN_ADD_CHILDREN), true));
		permissionsToSave.add(new Permission(Container.class.getCanonicalName(), host.getPermissionId(), newRole.getId(), (PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_WRITE | PermissionAPI.PERMISSION_PUBLISH), true));
		permissionsToSave.add(new Permission(Template.class.getCanonicalName(), host.getPermissionId(), newRole.getId(), (PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_WRITE | PermissionAPI.PERMISSION_PUBLISH), true));
		permissionsToSave.add(new Permission(TemplateLayout.class.getCanonicalName(), host.getPermissionId(), newRole.getId(), (PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_WRITE | PermissionAPI.PERMISSION_PUBLISH), true));
		permissionsToSave.add(new Permission(Link.class.getCanonicalName(), host.getPermissionId(), newRole.getId(), (PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_WRITE | PermissionAPI.PERMISSION_PUBLISH), true));
		permissionsToSave.add(new Permission(Contentlet.class.getCanonicalName(), host.getPermissionId(), newRole.getId(), (PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_WRITE | PermissionAPI.PERMISSION_PUBLISH), true));
		permissionsToSave.add(new Permission(IHTMLPage.class.getCanonicalName(), host.getPermissionId(), newRole.getId(), (PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_CAN_ADD_CHILDREN | PermissionAPI.PERMISSION_WRITE | PermissionAPI.PERMISSION_PUBLISH), true));
		permissionsToSave.add(new Permission(Structure.class.getCanonicalName(), host.getPermissionId(), newRole.getId(), (PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_WRITE | PermissionAPI.PERMISSION_PUBLISH), true));
		permissionsToSave.add(new Permission(Category.class.getCanonicalName(), host.getPermissionId(), newRole.getId(), 0, true));
		permissionsToSave.add(new Permission(Rule.class.getCanonicalName(), host.getPermissionId(), newRole.getId(), 0, true));

		if ( APILocator.getPermissionAPI().isInheritingPermissions(host) ) {
			Permissionable parentPermissionable = APILocator.getPermissionAPI().findParentPermissionable(host);
			APILocator.getPermissionAPI().permissionIndividually(parentPermissionable, host, systemUser);
		}

		// NOTE: Method "assignPermissions" is deprecated in favor of "save", which has subtle functional differences. Please take these differences into consideration if planning to replace this method with the "save"
		APILocator.getPermissionAPI().assignPermissions(permissionsToSave, host, systemUser, false);

		/**
		 * Add role permission over htmlpages
		 */
		Structure pageStructure = StructureFactory.getStructureByVelocityVarName(HTMLPageAssetAPIImpl.DEFAULT_HTMLPAGE_ASSET_STRUCTURE_VARNAME);
		perAPI.permissionIndividually(host, pageStructure, systemUser);

		List<Permission> permissions = new ArrayList<Permission>();
		Permission p1=new Permission();
		p1.setPermission(PermissionAPI.PERMISSION_READ);
		p1.setRoleId(newRole.getId());
		p1.setInode(pageStructure.getInode());
		permissions.add(p1);

		p1=new Permission();
		p1.setPermission(PermissionAPI.PERMISSION_WRITE);
		p1.setRoleId(newRole.getId());
		p1.setInode(pageStructure.getInode());
		permissions.add(p1);

		p1=new Permission();
		p1.setPermission(PermissionAPI.PERMISSION_PUBLISH);
		p1.setRoleId(newRole.getId());
		p1.setInode(pageStructure.getInode());
		permissions.add(p1);

		// NOTE: Method "assignPermissions" is deprecated in favor of "save", which has subtle functional differences. Please take these differences into consideration if planning to replace this method with the "save"
		perAPI.assignPermissions(permissions, pageStructure, systemUser, false);

		/**
		 * Add layout
		 */
		List<Layout> layouts = layoutAPI.findAllLayouts();
		for(Layout l : layouts){
			if(l.getName().equals("Site Browser") || l.getName().equals("Content") || l.getName().equals("Content Types")){
				roleAPI.addLayoutToRole(l, newRole);
			}
		}

		/**
		 * Add users with role
		 */
		String newUserName = "user"+id;
		User newUser = userAPI.createUser( newUserName + "@test.com", newUserName + "@test.com" );
		newUser.setFirstName( newUserName );
		newUser.setLastName( "TestUser" );
		userAPI.save( newUser, systemUser, false );

		roleAPI.addRoleToUser(newRole, newUser);

		Role newUserUserRole = roleAPI.loadRoleByKey(newUser.getUserId());

		String replacementUserName = "replacementuser"+id;
		User replacementUser = userAPI.createUser( replacementUserName + "@test.com", replacementUserName + "@test.com" );
		replacementUser.setFirstName( replacementUserName );
		replacementUser.setLastName( "TestUser" );
		userAPI.save( replacementUser, systemUser, false );

		roleAPI.addRoleToUser(newRole, replacementUser);

		Role replacementUserUserRole = roleAPI.loadRoleByKey(replacementUser.getUserId());

		/**
		 * Add folder
		 */
		Folder ftest = folderAPI.createFolders("/folderTest"+id, host, newUser, false);
		ftest.setOwner(newUser.getUserId());
		folderAPI.save(ftest, newUser, false);

		/**
		 * Create workflow scheme
		 */
		String schemeName = "workflow-"+id;

		WorkflowScheme newScheme = new WorkflowScheme();
		newScheme.setName(schemeName);
		newScheme.setArchived(false);
		newScheme.setMandatory(false);
		workflowAPI.saveScheme(newScheme);

		WorkflowScheme ws = workflowAPI.findSchemeByName(schemeName);
		Assert.assertTrue(UtilMethods.isSet(ws));

		/**
		 * Create scheme step1
		 */
		WorkflowStep workflowStep1 = new WorkflowStep();
		workflowStep1.setMyOrder(1);
		workflowStep1.setName("Edit");
		workflowStep1.setSchemeId(ws.getId());
		workflowStep1.setResolved(false);
		workflowAPI.saveStep(workflowStep1);

		List<WorkflowStep> steps = workflowAPI.findSteps(ws);
		Assert.assertTrue(steps.size()==1);
		workflowStep1 = steps.get(0);

		/**
		 * Create scheme step2
		 */
		WorkflowStep workflowStep2 = new WorkflowStep();
		workflowStep2.setMyOrder(2);
		workflowStep2.setName("Publish");
		workflowStep2.setSchemeId(ws.getId());
		workflowStep2.setResolved(false);
		workflowAPI.saveStep(workflowStep2);

		steps = workflowAPI.findSteps(ws);
		Assert.assertTrue(steps.size()==2);
		workflowStep2 = steps.get(1);

		/**
		 * Add action to scheme step1
		 */
		WorkflowAction newAction = new WorkflowAction();
		newAction.setName("Edit");
		newAction.setAssignable(true);
		newAction.setCommentable(true);
		newAction.setIcon("workflowIcon");
		newAction.setNextStep(workflowStep2.getId());
		newAction.setStepId(workflowStep1.getId());
		newAction.setCondition("");
		newAction.setRequiresCheckout(false);
		newAction.setRoleHierarchyForAssign(false);
		newAction.setNextAssign(newUserUserRole.getId());

		List<Permission> permissionsNewAction = new ArrayList<Permission>();
		permissionsNewAction.add(new Permission( newAction.getId(), newRole.getId(), PermissionAPI.PERMISSION_USE ));

		workflowAPI.saveAction(newAction, permissionsNewAction);
		
		List<WorkflowAction> actions1= workflowAPI.findActions(workflowStep1, systemUser);
		Assert.assertTrue(actions1.size()==1);
		WorkflowAction action1 = actions1.get(0);

		/**
		 * Add action to scheme step2
		 */
		WorkflowAction newAction2 = new WorkflowAction();
		newAction2.setName("Publish");
		newAction2.setAssignable(true);
		newAction2.setCommentable(true);
		newAction2.setIcon("workflowIcon");
		newAction2.setNextStep(workflowStep2.getId());
		newAction2.setStepId(workflowStep2.getId());
		newAction2.setCondition("");
		newAction2.setRequiresCheckout(false);
		newAction2.setRoleHierarchyForAssign(false);
		newAction2.setNextAssign(newUserUserRole.getId());

		List<Permission> permissionsNewAction2 = new ArrayList<Permission>();
		permissionsNewAction2.add(new Permission( newAction2.getId(), newRole.getId(), PermissionAPI.PERMISSION_USE ));

		workflowAPI.saveAction(newAction2, permissionsNewAction2);

		List<WorkflowAction> actions2= workflowAPI.findActions(workflowStep2, systemUser);
		Assert.assertTrue(actions2.size()==1);
		WorkflowAction action2 = actions2.get(0);

		/**
		 * Add structure
		 */
		Structure st = new Structure();
		st.setHost(host.getIdentifier());
		st.setFolder(ftest.getInode());
		st.setName("structure"+id);
		st.setStructureType(Structure.STRUCTURE_TYPE_CONTENT);
		st.setOwner(newUser.getUserId());
		st.setVelocityVarName("structure"+id);
		StructureFactory.saveStructure(st);
		CacheLocator.getContentTypeCache().add(st);

		Field field1 = new Field("title", Field.FieldType.TEXT, Field.DataType.TEXT, st,true, true, true, 3, "", "", "", true, false, true);
		field1.setVelocityVarName("title");
		field1.setListed(true);
		FieldFactory.saveField(field1);
		FieldsCache.addField(field1);

		Field field2 = new Field("body", Field.FieldType.TEXT_AREA, Field.DataType.LONG_TEXT, st,true, true, true, 4, "", "", "", true, false, true);
		field2.setVelocityVarName("body");
		FieldFactory.saveField(field2);
		FieldsCache.addField(field2);

		workflowAPI.saveSchemeForStruct(st, ws);

		/**
		 * Add container
		 */
		Container container = new Container();
		String containerName="container"+id;
		container.setFriendlyName(containerName);
		container.setTitle(containerName);
		container.setOwner(newUser.getUserId());
		container.setMaxContentlets(5);
		container.setPreLoop("preloop code");
		container.setCode("<div><h3>content $!{title}</h3><p>$!{body}</p></div>");
		container.setPostLoop("postloop code");

		List<ContainerStructure> csList = new ArrayList<ContainerStructure>();
		ContainerStructure cs = new ContainerStructure();
		cs.setStructureId(st.getInode());
		cs.setCode(container.getCode());
		csList.add(cs);
		container = containerAPI.save(container, csList, host, newUser, false);
		PublishFactory.publishAsset(container,newUser, false, false);

		/**
		 * Add template
		 */
		String templateBody="<html><body> #parseContainer('"+container.getIdentifier()+"') </body></html>";
		String templateTitle="template"+id;

		Template template=new Template();
		template.setTitle(templateTitle);
		template.setBody(templateBody);
		template.setOwner(newUser.getUserId());
		template = templateAPI.saveTemplate(template, host, newUser, false);
		PublishFactory.publishAsset(template, newUser, false, false);

		/**
		 * Add page
		 */
		String page0Str ="page"+id;

		Contentlet contentAsset=new Contentlet();
		contentAsset.setStructureInode(HTMLPageAssetAPIImpl.DEFAULT_HTMLPAGE_ASSET_STRUCTURE_INODE);
		contentAsset.setHost(host.getIdentifier());
		contentAsset.setProperty(HTMLPageAssetAPIImpl.FRIENDLY_NAME_FIELD, page0Str);
		contentAsset.setProperty(HTMLPageAssetAPIImpl.URL_FIELD, page0Str);
		contentAsset.setProperty(HTMLPageAssetAPIImpl.TITLE_FIELD, page0Str);
		contentAsset.setProperty(HTMLPageAssetAPIImpl.CACHE_TTL_FIELD, "0");
		contentAsset.setProperty(HTMLPageAssetAPIImpl.TEMPLATE_FIELD, template.getIdentifier());
		contentAsset.setLanguageId(langId);
		contentAsset.setFolder(ftest.getInode());
		contentAsset=conAPI.checkin(contentAsset, newUser, false);
		conAPI.publish(contentAsset, newUser, false);

		/**
		 * Add content
		 */
		String title ="content"+id;
		Contentlet contentAsset2=new Contentlet();
		contentAsset2.setStructureInode(st.getInode());
		contentAsset2.setHost(host.getIdentifier());
		contentAsset2.setProperty("title", title);
		contentAsset2.setLanguageId(langId);
		contentAsset2.setProperty("body", title);
		contentAsset2.setFolder(ftest.getInode());
		contentAsset2=conAPI.checkin(contentAsset2, newUser, false);
		conAPI.publish(contentAsset2, newUser, false);

		/**
		 * Test that delete is not possible for step2
		 * while has associated step or content
		 */
		contentAsset2.setStringProperty("wfActionId", action1.getId());
		contentAsset2.setStringProperty("wfActionComments", "step1");
		contentAsset2.setStringProperty("wfActionAssign", newUserUserRole.getId());
		workflowAPI.fireWorkflowNoCheckin(contentAsset2, newUser);

		contentAsset2.setStringProperty("wfActionId", action2.getId());
		contentAsset2.setStringProperty("wfActionComments", "step2");
		contentAsset2.setStringProperty("wfActionAssign", newUserUserRole.getId());
		workflowAPI.fireWorkflowNoCheckin(contentAsset2, newUser);

		WorkflowStep  currentStep = workflowAPI.findStepByContentlet(contentAsset2);

		assertTrue(currentStep.getId().equals(workflowStep2.getId()));

		/**
		 * Relate content to page
		 */
		MultiTree m = new MultiTree(contentAsset.getIdentifier(), container.getIdentifier(), contentAsset2.getIdentifier());
		MultiTreeFactory.saveMultiTree(m);

		/**
		 * Add menu link
		 */
		String linkStr="link"+id;
		Link link = new Link();
		link.setTitle(linkStr);
		link.setFriendlyName(linkStr);
		link.setParent(ftest.getInode());
		link.setTarget("_blank");
		link.setOwner(newUser.getUserId());
		link.setModUser(newUser.getUserId());
		IHTMLPage page =htmlPageAssetAPI.getPageByPath(ftest.getPath()+page0Str, host, langId, true);

		Identifier internalLinkIdentifier = identifierAPI.findFromInode(page.getIdentifier());
		link.setLinkType(Link.LinkType.INTERNAL.toString());
		link.setInternalLinkIdentifier(internalLinkIdentifier.getId());
		link.setProtocal("http://");

		StringBuffer myURL = new StringBuffer();
		if(InodeUtils.isSet(internalLinkIdentifier.getHostId())) {
			myURL.append(host.getHostname());
		}
		myURL.append(internalLinkIdentifier.getURI());
		link.setUrl(myURL.toString());
		WebAssetFactory.createAsset(link, newUser.getUserId(), ftest);
		versionableAPI.setLive(link);

		/**
		 * validation of current user and references
		 */
		assertTrue(userAPI.userExistsWithEmail(newUser.getEmailAddress()));
		assertNotNull(roleAPI.loadRoleByKey(newUser.getUserId()));

		assertTrue(link.getOwner().equals(newUser.getUserId()));
		assertTrue(link.getModUser().equals(newUser.getUserId()));

		assertTrue(contentAsset2.getOwner().equals(newUser.getUserId()));
		assertTrue(contentAsset2.getModUser().equals(newUser.getUserId()));

		assertTrue(contentAsset.getOwner().equals(newUser.getUserId()));
		assertTrue(contentAsset.getModUser().equals(newUser.getUserId()));

		WorkflowTask task = workflowAPI.findTaskByContentlet(contentAsset2);
		assertTrue(task.getAssignedTo().equals(newUserUserRole.getId()));
		assertTrue(task.getCreatedBy().equals(newUserUserRole.getId()));

		WorkflowStep step = workflowAPI.findStepByContentlet(contentAsset2);
		WorkflowAction action =  workflowAPI.findActions(step, systemUser).get(0);
		assertTrue(action.getNextAssign().equals(newUserUserRole.getId()));

		List<WorkflowComment> comments = workflowAPI.findWorkFlowComments(task);
		for(WorkflowComment comm : comments){
			assertTrue(comm.getPostedBy().equals(newUserUserRole.getId()));
		}

		assertTrue(container.getOwner().equals(newUser.getUserId()));
		assertTrue(container.getModUser().equals(newUser.getUserId()));

		assertTrue(template.getOwner().equals(newUser.getUserId()));
		assertTrue(template.getModUser().equals(newUser.getUserId()));

		assertTrue(ftest.getOwner().equals(newUser.getUserId()));

		//Verify we have the proper user set in the HTMLPage
		page = htmlPageAssetAPI.getPageByPath(ftest.getPath() + page0Str, host, langId, true);
		assertTrue(page.getOwner().equals(newUser.getUserId()));
		assertTrue(page.getModUser().equals(newUser.getUserId()));

		/*
		 * delete user and replace its references with the replacement user, this delete method
		 * does a lot of things, after the delete lets wait a bit in order to allow the reindex
		 * of the modified contentlets to finish processing.
		 */
		userAPI.delete(newUser, replacementUser, systemUser,false);
		APILocator.getContentletAPI().isInodeIndexed(page.getInode(), true);
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			//Do nothing...
		}

		/*
		 * Validate that the user was deleted and if its references were updated
		 */
		try {
			assertNull(userAPI.loadByUserByEmail(newUser.getEmailAddress(),systemUser,false));			
		}catch(com.dotmarketing.business.NoSuchUserException e){
			//enter here if the user doesn't exist as is expected
		}
		assertNull(roleAPI.loadRoleByKey(newUser.getUserId()));

		assertNotNull(userAPI.loadByUserByEmail(replacementUser.getEmailAddress(),systemUser,false));

		link = APILocator.getMenuLinkAPI().find(link.getInode(), systemUser, false);
		assertTrue(link.getOwner().equals(replacementUser.getUserId()));
		assertTrue(link.getModUser().equals(replacementUser.getUserId()));

		page =htmlPageAssetAPI.getPageByPath(ftest.getPath()+page0Str, host, langId, true);
		assertTrue(page.getOwner().equals(replacementUser.getUserId()));
		assertTrue(page.getModUser().equals(replacementUser.getUserId()));

		List<Contentlet> contentAssets = conAPI.findByStructure(st, systemUser, false, 100,0);
		for(Contentlet content: contentAssets){
			assertTrue(content.getOwner().equals(replacementUser.getUserId()));
			assertTrue(content.getModUser().equals(replacementUser.getUserId()));

			task = workflowAPI.findTaskByContentlet(content);
			assertTrue(task.getAssignedTo().equals(replacementUserUserRole.getId()));
			assertTrue(task.getCreatedBy().equals(replacementUserUserRole.getId()));

			step = workflowAPI.findStepByContentlet(content);
			action =  workflowAPI.findActions(step, systemUser).get(0);
			assertTrue(action.getNextAssign().equals(replacementUserUserRole.getId()));

			comments = workflowAPI.findWorkFlowComments(task);
			for(WorkflowComment comm : comments){
				assertTrue(comm.getPostedBy().equals(replacementUserUserRole.getId()));
			}
		}

		container = containerAPI.getLiveContainerById(container.getIdentifier(), systemUser, false);
		assertTrue(container.getOwner().equals(replacementUser.getUserId()));
		assertTrue(container.getModUser().equals(replacementUser.getUserId()));

		template = templateAPI.find(template.getInode(), systemUser, false);
		assertTrue(template.getOwner().equals(replacementUser.getUserId()));
		assertTrue(template.getModUser().equals(replacementUser.getUserId()));

		CacheLocator.getFolderCache().removeFolder(ftest, identifierAPI.find(ftest.getIdentifier()));
		ftest = folderAPI.findFolderByPath(ftest.getPath(), host, systemUser, false);
		assertTrue(ftest.getOwner().equals(replacementUser.getUserId()));
	}

	/**
	 * Validates rule to forbid anonymous user delete
	 * as in other restrictions an exception is returned
	 * @throws DotDataException
	 */
	@Test(expected=DotDataException.class)
	public void deleteAnonymous() throws DotDataException, DotSecurityException {

		UserAPI userAPI = APILocator.getUserAPI();
		User anonymousUser = null;

		anonymousUser = userAPI.getAnonymousUser();
		userAPI.delete(anonymousUser, systemUser, false);
	}

	@Test
	public void testGetUsersByNameOrEmailOrUserID() throws DotDataException {
		UserAPI userAPI = APILocator.getUserAPI();
		List<User> users = userAPI.getUsersByNameOrEmailOrUserID(null, 0, 40, false);
		assertNotNull(users);
		assertTrue(users.size() > 0);
	}

	@Test
	public void testGetUsersByNameOrEmailOrUserIDWithFilter() throws DotDataException, DotSecurityException {
		String id;
		String userName;
		User user;
		UserAPI userAPI;

		id = String.valueOf(new Date().getTime());
		userAPI = APILocator.getUserAPI();
		userName = "user" + id;

		user = UserTestUtil.getUser(userName, false, true);

		List<User> users = userAPI.getUsersByNameOrEmailOrUserID(userName + "@fake.org", 0, 40, false);
		assertNotNull(users);
		assertTrue(users.size() == 1);

		userAPI.delete(user, userAPI.getDefaultUser(), systemUser, false);}

	@Test
	public void testGetUsersByNameOrEmailOrUserIDDeleted() throws DotDataException, DotSecurityException {
		String id;
		String userName;
		User user;
		UserAPI userAPI;

		id = String.valueOf(new Date().getTime());
		userAPI = APILocator.getUserAPI();
		userName = "user" + id;

		user = UserTestUtil.getUser(userName, true, true);

		List<User> users = userAPI.getUsersByNameOrEmailOrUserID(userName + "@fake.org", 0, 40, false);
		assertNotNull(users);
		assertTrue(users.size() == 0);

		userAPI.delete(user, userAPI.getDefaultUser(), userAPI.getSystemUser(), false);
	}

	@Test
	public void testGetUnDeletedUsers() throws DotDataException, DotSecurityException {

		UserAPI userAPI = APILocator.getUserAPI();

		//Getting the current list
		List<User> currentUsers = userAPI.getUnDeletedUsers();
		int currentUsersCount = 0;
		if (null != currentUsers) {
			currentUsersCount = currentUsers.size();
		}

		/*
		 * Add user
		 */
		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.HOUR, -48);
		String newUserName = "user" + System.currentTimeMillis();

		User newUser = UserTestUtil.getUser(newUserName, true, true, calendar.getTime());

		List<User> foundUsers = userAPI.getUnDeletedUsers();

		assertNotNull(foundUsers);
		assertEquals(foundUsers.size(), currentUsersCount + 1);

		Boolean found = false;
		for (User user : foundUsers) {
			if (newUserName.equals(user.getFirstName())) {
				assertTrue(user.getDeleteInProgress());
				assertNotNull(user.getDeleteDate());
				assertEquals(user.getFirstName(), newUserName);
				found = true;
			}
		}

		if (!found) {
			fail("The user saved was not found in the retrieved list.");
		}

		//Clean up the created user
		userAPI.delete(newUser, userAPI.getDefaultUser(), userAPI.getSystemUser(), false);
	}

	@Test
	public void testGetUsersByName() throws DotDataException, DotSecurityException {

		String id;
		String userName;
		User user;
		UserAPI userAPI;

		id = String.valueOf(new Date().getTime());
		userAPI = APILocator.getUserAPI();
		userName = "user" + id;

		user = UserTestUtil.getUser(userName, false, true);

		List<User> users = userAPI.getUsersByName(userName, 0, 40, systemUser, false);
		assertNotNull(users);
		assertTrue(users.size() == 1);

		userAPI.delete(user, userAPI.getDefaultUser(), userAPI.getSystemUser(), false);
	}

	@Test
	public void testGetUsersByNameDeleted() throws DotDataException, DotSecurityException {

		String id;
		String userName;
		User user;
		UserAPI userAPI;

		id = String.valueOf(new Date().getTime());
		userAPI = APILocator.getUserAPI();
		userName = "user" + id;

		user = UserTestUtil.getUser(userName, true, true);

		List<User> users = userAPI.getUsersByName(userName, 0, 40, systemUser, false);
		assertNotNull(users);
		assertTrue(users.size() == 0);

		userAPI.delete(user, userAPI.getDefaultUser(), userAPI.getSystemUser(), false);
	}

	@Test
	public void testGetUsersByNameOrEmail() throws DotDataException, DotSecurityException {

		String id;
		String userName;
		User user;
		UserAPI userAPI;

		id = String.valueOf(new Date().getTime());
		userAPI = APILocator.getUserAPI();
		userName = "user" + id;

		user = UserTestUtil.getUser(userName, false, true);

		List<User> users = userAPI.getUsersByNameOrEmail(userName, 0, 40);
		assertNotNull(users);
		assertTrue(users.size() == 1);

		userAPI.delete(user, userAPI.getDefaultUser(), userAPI.getSystemUser(), false);
	}

	@Test
	public void testGetUsersByNameOrEmailDeleted() throws DotDataException, DotSecurityException {

		String id;
		String userName;
		User user;
		UserAPI userAPI;

		id = String.valueOf(new Date().getTime());
		userAPI = APILocator.getUserAPI();
		userName = "user" + id;

		user = UserTestUtil.getUser(userName, true, true);

		List<User> users = userAPI.getUsersByNameOrEmail(userName, 0, 40);
		assertNotNull(users);
		assertTrue(users.size() == 0);

		userAPI.delete(user, userAPI.getDefaultUser(), userAPI.getSystemUser(), false);
	}

	@Test
	public void testGetCountUsersByNameOrEmail() throws DotDataException, DotSecurityException {
		String id;
		String userName;
		User user;
		UserAPI userAPI;

		id = String.valueOf(new Date().getTime());
		userAPI = APILocator.getUserAPI();
		userName = "user" + id;

		user = UserTestUtil.getUser(userName, false, true);

		long count = userAPI.getCountUsersByNameOrEmail(userName + "@fake.org");

		assertTrue(count == 1);

		userAPI.delete(user, userAPI.getDefaultUser(), userAPI.getSystemUser(), false);

	}

	@Test
	public void testGetCountUsersByNameOrEmailDeleted() throws DotDataException, DotSecurityException {
		String id;
		String userName;
		User user;
		UserAPI userAPI;

		id = String.valueOf(new Date().getTime());
		userAPI = APILocator.getUserAPI();
		userName = "user" + id;

		user = UserTestUtil.getUser(userName, true, true);

		long count = userAPI.getCountUsersByNameOrEmail(userName + "@fake.org");

		assertTrue(count == 0);

		userAPI.delete(user, userAPI.getDefaultUser(), userAPI.getSystemUser(), false);
	}

	@Test
	public void testGetCountUsersByNameOrEmailOrUserID() throws DotDataException, DotSecurityException {
		String id;
		String userName;
		User user;
		UserAPI userAPI;

		id = String.valueOf(new Date().getTime());
		userAPI = APILocator.getUserAPI();
		userName = "user" + id;

		user = UserTestUtil.getUser(userName, false, true);

		long count = userAPI.getCountUsersByNameOrEmailOrUserID(userName + "@fake.org");

		assertTrue(count == 1);

		userAPI.delete(user, userAPI.getDefaultUser(), userAPI.getSystemUser(), false);
	}

	@Test
	public void testGetCountUsersByNameOrEmailOrUserIDDeleted() throws DotDataException, DotSecurityException {
		String id;
		String userName;
		User user;
		UserAPI userAPI;

		id = String.valueOf(new Date().getTime());
		userAPI = APILocator.getUserAPI();
		userName = "user" + id;

		user = UserTestUtil.getUser(userName, true, true);

		long count = userAPI.getCountUsersByNameOrEmailOrUserID(userName + "@fake.org");

		assertTrue(count == 0);

		userAPI.delete(user, userAPI.getDefaultUser(), userAPI.getSystemUser(), false);
	}

	@Test
	public void testGetUsersIdsByCreationDate() throws DotDataException, DotSecurityException {

		UserAPI userAPI = APILocator.getUserAPI();

		List<String> users = userAPI.getUsersIdsByCreationDate(null, 0, 40);
		assertNotNull(users);
		assertTrue(users.size() > 0);

	}

	@Test
	public void testGetUsersIdsByCreationDateDeleted() throws DotDataException, DotSecurityException {

		String id;
		String userName;
		User user;
		UserAPI userAPI;
		Calendar calendar;

		id = String.valueOf(new Date().getTime());
		userAPI = APILocator.getUserAPI();
		userName = "user" + id;

		user = UserTestUtil.getUser(userName, true, true);

		calendar = Calendar.getInstance();
		calendar.add(Calendar.HOUR, -24);

		List<String> users = userAPI.getUsersIdsByCreationDate(calendar.getTime(), 0, 40);
		assertNotNull(users);
		assertTrue(!users.contains(user.getUserId()));

		userAPI.delete(user, userAPI.getDefaultUser(), userAPI.getSystemUser(), false);
	}
}
