package com.dotmarketing.business;

import com.dotcms.IntegrationTestBase;
import com.dotcms.LicenseTestUtil;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.notifications.bean.Notification;
import com.dotcms.rest.api.v1.authentication.DotInvalidTokenException;
import com.dotcms.rest.api.v1.authentication.ResetPasswordTokenUtil;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.util.TimeUtil;
import com.dotmarketing.beans.*;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.common.model.ContentletSearch;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.exception.WebAssetException;
import com.dotmarketing.factories.PublishFactory;
import com.dotmarketing.factories.WebAssetFactory;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.containers.business.ContainerAPI;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.hostvariable.bussiness.HostVariableAPI;
import com.dotmarketing.portlets.hostvariable.model.HostVariable;
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
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.ejb.UserTestUtil;
import com.liferay.portal.ejb.UserUtil;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Optional;
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
	protected static HostVariableAPI hostVariableAPI;

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
		hostVariableAPI = APILocator.getHostVariableAPI();

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

		final long langId =APILocator.getLanguageAPI().getDefaultLanguage().getId();
		final String timeString = String.valueOf( new Date().getTime());

		/**
		 * Add host
		 */
		Host host = new Host();
		host.setHostname("test"+timeString+".demo.dotcms.com");
		host.setIndexPolicy(IndexPolicy.FORCE);
		host=hostAPI.save(host, systemUser, false);
		/**
		 * Add role
		 */
		Role newRole = new Role();
		String newRoleName = "role"+timeString;
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
		List<Permission> permissionsToSave = new ArrayList<>();
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

		List<Permission> permissions = new ArrayList<>();
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
			if(l.getName().equals("Site") || l.getName().equals("Content") || l.getName().contains("Types")){
				roleAPI.addLayoutToRole(l, newRole);
			}
		}

		/**
		 * Add users with role
		 */
		String userToDeleteId = "user"+timeString;
		User userToDelete = userAPI.createUser( userToDeleteId + "@test.com", userToDeleteId + "@test.com" );
		userToDelete.setFirstName( userToDeleteId );
		userToDelete.setLastName( "TestUser" );
		userAPI.save( userToDelete, systemUser, false );

		roleAPI.addRoleToUser(newRole, userToDelete);
		roleAPI.addRoleToUser(roleAPI.loadBackEndUserRole(), userToDelete);
		Role newUserUserRole = roleAPI.loadRoleByKey(userToDelete.getUserId());

		String replacementUserName = "replacementuser"+timeString;
		User replacementUser = userAPI.createUser( replacementUserName + "@test.com", replacementUserName + "@test.com" );
		replacementUser.setFirstName( replacementUserName );
		replacementUser.setLastName( "TestUser" );
		userAPI.save( replacementUser, systemUser, false );

		roleAPI.addRoleToUser(newRole, replacementUser);
        roleAPI.addRoleToUser(roleAPI.loadBackEndUserRole(), replacementUser);
		Role replacementUserUserRole = roleAPI.loadRoleByKey(replacementUser.getUserId());

		/**
		 * Add folder
		 */
		Folder testFolder = folderAPI.createFolders("/folderTest"+timeString, host, userToDelete, false);
		testFolder.setOwner(userToDelete.getUserId());
		folderAPI.save(testFolder, userToDelete, false);

		/**
		 * Create workflow scheme
		 */
		String schemeName = "workflow-"+timeString;

		WorkflowScheme newScheme = new WorkflowScheme();
		newScheme.setName(schemeName);
		newScheme.setArchived(false);
		workflowAPI.saveScheme(newScheme, systemUser);

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
		workflowAPI.saveStep(workflowStep1, systemUser);

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
		workflowAPI.saveStep(workflowStep2, systemUser);

		steps = workflowAPI.findSteps(ws);
		Assert.assertTrue(steps.size()==2);
		workflowStep2 = steps.get(1);

		/**
		 * Add action to scheme step1
		 */
		WorkflowAction newAction = new WorkflowAction();
		newAction.setSchemeId(ws.getId());
		newAction.setName("Edit");
		newAction.setAssignable(true);
		newAction.setCommentable(true);
		newAction.setIcon("workflowIcon");
		newAction.setNextStep(workflowStep2.getId());
		newAction.setCondition("");
		newAction.setRequiresCheckout(false);
		newAction.setRoleHierarchyForAssign(false);
		newAction.setNextAssign(newUserUserRole.getId());

		List<Permission> permissionsNewAction = new ArrayList<>();
		permissionsNewAction.add(new Permission( newAction.getId(), newRole.getId(), PermissionAPI.PERMISSION_USE ));

		workflowAPI.saveAction(newAction, permissionsNewAction, systemUser);
		workflowAPI.saveAction(newAction.getId(), workflowStep1.getId(), systemUser);

		List<WorkflowAction> actions1= workflowAPI.findActions(workflowStep1, systemUser);
		Assert.assertTrue(actions1.size()==1);
		WorkflowAction action1 = actions1.get(0);

		/**
		 * Add action to scheme step2
		 */
		WorkflowAction newAction2 = new WorkflowAction();
		newAction2.setSchemeId(ws.getId());
		newAction2.setName("Publish");
		newAction2.setAssignable(true);
		newAction2.setCommentable(true);
		newAction2.setIcon("workflowIcon");
		newAction2.setNextStep(workflowStep2.getId());
		newAction2.setCondition("");
		newAction2.setRequiresCheckout(false);
		newAction2.setRoleHierarchyForAssign(false);
		newAction2.setNextAssign(newUserUserRole.getId());

		List<Permission> permissionsNewAction2 = new ArrayList<>();
		permissionsNewAction2.add(new Permission( newAction2.getId(), newRole.getId(), PermissionAPI.PERMISSION_USE ));

		workflowAPI.saveAction(newAction2, permissionsNewAction2, systemUser);
		workflowAPI.saveAction(newAction2.getId(), workflowStep2.getId(), systemUser);

		List<WorkflowAction> actions2= workflowAPI.findActions(workflowStep2, systemUser);
		Assert.assertTrue(actions2.size()==1);
		WorkflowAction action2 = actions2.get(0);

		/**
		 * Add structure
		 */
		Structure st = new Structure();
		st.setHost(host.getIdentifier());
		st.setFolder(testFolder.getInode());
		st.setName("structure"+timeString);
		st.setStructureType(Structure.STRUCTURE_TYPE_CONTENT);
		st.setOwner(userToDelete.getUserId());
		st.setVelocityVarName("structure"+timeString);
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

		List<WorkflowScheme> schemes = new ArrayList<>();
		schemes.add(ws);
		workflowAPI.saveSchemesForStruct(st, schemes);

		/**
		 * Add container
		 */
		Container container = new Container();
		String containerName="container"+timeString;
		container.setFriendlyName(containerName);
		container.setTitle(containerName);
		container.setOwner(userToDelete.getUserId());
		container.setMaxContentlets(5);
		container.setPreLoop("preloop code");
		container.setCode("<div><h3>content $!{title}</h3><p>$!{body}</p></div>");
		container.setPostLoop("postloop code");

		List<ContainerStructure> csList = new ArrayList<>();
		ContainerStructure cs = new ContainerStructure();
		cs.setStructureId(st.getInode());
		cs.setCode(container.getCode());
		csList.add(cs);
		container = containerAPI.save(container, csList, host, userToDelete, false);
		PublishFactory.publishAsset(container,userToDelete, false, false);

		/**
		 * Add template
		 */
		String templateBody="<html><body> #parseContainer('"+container.getIdentifier()+"') </body></html>";
		String templateTitle="template"+timeString;

		Template template=new Template();
		template.setTitle(templateTitle);
		template.setBody(templateBody);
		template.setOwner(userToDelete.getUserId());
		template.setDrawedBody(templateBody);
		template = templateAPI.saveTemplate(template, host, userToDelete, false);
		PublishFactory.publishAsset(template, userToDelete, false, false);

		/**
		 * Add page
		 */
		String page0Str ="page"+timeString;

		Contentlet contentAsset=new Contentlet();
		contentAsset.setStructureInode(HTMLPageAssetAPIImpl.DEFAULT_HTMLPAGE_ASSET_STRUCTURE_INODE);
		contentAsset.setHost(host.getIdentifier());
		contentAsset.setProperty(HTMLPageAssetAPIImpl.FRIENDLY_NAME_FIELD, page0Str);
		contentAsset.setProperty(HTMLPageAssetAPIImpl.URL_FIELD, page0Str);
		contentAsset.setProperty(HTMLPageAssetAPIImpl.TITLE_FIELD, page0Str);
		contentAsset.setProperty(HTMLPageAssetAPIImpl.CACHE_TTL_FIELD, "0");
		contentAsset.setProperty(HTMLPageAssetAPIImpl.TEMPLATE_FIELD, template.getIdentifier());
		contentAsset.setLanguageId(langId);
		contentAsset.setFolder(testFolder.getInode());
		contentAsset.setIndexPolicy(IndexPolicy.FORCE);
		contentAsset=conAPI.checkin(contentAsset, userToDelete, false);
		contentAsset.setIndexPolicy(IndexPolicy.FORCE);
		conAPI.publish(contentAsset, userToDelete, false);

		/**
		 * Add content
		 */
		String title ="content"+timeString;
		Contentlet contentAsset2=new Contentlet();
		contentAsset2.setStructureInode(st.getInode());
		contentAsset2.setHost(host.getIdentifier());
		contentAsset2.setProperty("title", title);
		contentAsset2.setLanguageId(langId);
		contentAsset2.setProperty("body", title);
		contentAsset2.setFolder(testFolder.getInode());
		contentAsset2.setIndexPolicy(IndexPolicy.FORCE);
		contentAsset2=conAPI.checkin(contentAsset2, userToDelete, false);
		contentAsset.setIndexPolicy(IndexPolicy.FORCE);
		conAPI.publish(contentAsset2, userToDelete, false);

		/**
		 * Test that delete is not possible for step2
		 * while has associated step or content
		 */
		contentAsset2.setStringProperty("wfActionId", action1.getId());
		contentAsset2.setStringProperty("wfActionComments", "step1");
		contentAsset2.setStringProperty("wfActionAssign", newUserUserRole.getId());
		workflowAPI.fireWorkflowNoCheckin(contentAsset2, userToDelete);

		contentAsset2.setStringProperty("wfActionId", action2.getId());
		contentAsset2.setStringProperty("wfActionComments", "step2");
		contentAsset2.setStringProperty("wfActionAssign", newUserUserRole.getId());
		workflowAPI.fireWorkflowNoCheckin(contentAsset2, userToDelete);

		WorkflowStep  currentStep = workflowAPI.findStepByContentlet(contentAsset2);
		assertNotNull(currentStep);

		assertTrue(currentStep.getId().equals(workflowStep2.getId()));

		/**
		 * Relate content to page
		 */
		MultiTree m = new MultiTree(contentAsset.getIdentifier(), container.getIdentifier(), contentAsset2.getIdentifier());
		APILocator.getMultiTreeAPI().saveMultiTree(m);

		/**
		 * Add menu link
		 */
		String linkStr="link"+timeString;
		Link link = new Link();
		link.setTitle(linkStr);
		link.setFriendlyName(linkStr);
		link.setParent(testFolder.getInode());
		link.setTarget("_blank");
		link.setOwner(userToDelete.getUserId());
		link.setModUser(userToDelete.getUserId());
		IHTMLPage page =htmlPageAssetAPI.getPageByPath(testFolder.getPath()+page0Str, host, langId, true);

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
		WebAssetFactory.createAsset(link, userToDelete.getUserId(), testFolder);
		versionableAPI.setLive(link);

		/**
		 * Add HostVariable
		 */
		final String hostVariableString = "hostVariable"+timeString;
		HostVariable hostVariable = new HostVariable();
		hostVariable.setHostId(host.getIdentifier());
		hostVariable.setName(hostVariableString);
		hostVariable.setKey(hostVariableString);
		hostVariable.setValue(hostVariableString);
		hostVariable.setLastModifierId(userToDelete.getUserId());
		hostVariable.setLastModDate(new Date());
		hostVariableAPI.save(hostVariable,userToDelete,false);

		/**
		 * validation of current user and references
		 */
		assertTrue(userAPI.userExistsWithEmail(userToDelete.getEmailAddress()));
		assertNotNull(roleAPI.loadRoleByKey(userToDelete.getUserId()));

		assertTrue(link.getOwner().equals(userToDelete.getUserId()));
		assertTrue(link.getModUser().equals(userToDelete.getUserId()));

		assertTrue(contentAsset2.getOwner().equals(userToDelete.getUserId()));
		assertTrue(contentAsset2.getModUser().equals(userToDelete.getUserId()));

		assertTrue(contentAsset.getOwner().equals(userToDelete.getUserId()));
		assertTrue(contentAsset.getModUser().equals(userToDelete.getUserId()));

		WorkflowTask task = workflowAPI.findTaskByContentlet(contentAsset2);
		assertTrue(task.getAssignedTo().equals(newUserUserRole.getId()));
		Logger.info(this,"******* userToDelete: " + userToDelete);
		Logger.info(this,"******* task.getCreatedBy(): " + task.getCreatedBy());
		Logger.info(this,"******* newUserUserRole.getId(): " + newUserUserRole.getId());
		assertTrue(task.getCreatedBy().equals(userToDelete.getUserId()));

		WorkflowStep  step =  workflowAPI.findStepByContentlet(contentAsset2);
		assertNotNull(step);
		WorkflowAction action =  workflowAPI.findActions(step, systemUser).get(0);
		assertTrue(action.getNextAssign().equals(newUserUserRole.getId()));

		List<WorkflowComment> comments = workflowAPI.findWorkFlowComments(task);
		for(WorkflowComment comm : comments){
			assertTrue(comm.getPostedBy().equals(userToDelete.getUserId()));
		}

		assertTrue(container.getOwner().equals(userToDelete.getUserId()));
		assertTrue(container.getModUser().equals(userToDelete.getUserId()));

		assertTrue(template.getOwner().equals(userToDelete.getUserId()));
		assertTrue(template.getModUser().equals(userToDelete.getUserId()));

		assertTrue(testFolder.getOwner().equals(userToDelete.getUserId()));

		hostVariable = hostVariableAPI.getVariablesForHost(host.getIdentifier(),userToDelete,false).get(0);
		assertTrue(hostVariable.getLastModifierId().equals(userToDelete.getUserId()));

		//Verify we have the proper user set in the HTMLPage
		page = htmlPageAssetAPI.getPageByPath(testFolder.getPath() + page0Str, host, langId, true);
		assertTrue(page.getOwner().equals(userToDelete.getUserId()));
		assertTrue(page.getModUser().equals(userToDelete.getUserId()));

		/*
		 * delete user and replace its references with the replacement user, this delete method
		 * does a lot of things, after the delete lets wait a bit in order to allow the reindex
		 * of the modified contentlets to finish processing.
		 */

		final StringBuilder luceneQuery = new StringBuilder("working:true +modUser:").append(userToDelete.getUserId());
		final int limit = 0;
		final int offset = -1;
		final List<ContentletSearch> contentlets = APILocator.getContentletAPI().searchIndex
			(luceneQuery.toString(), limit, offset, null, systemUser, false);

		Logger.info(this, "ES query: " + luceneQuery.toString());
		Logger.info(this, "contentlets.size: " + contentlets.size());

		APILocator.getNotificationAPI().deleteNotifications(systemUser.getUserId());

		userAPI.delete(userToDelete, replacementUser, systemUser,false);

		waitForDeleteCompletedNotification(userToDelete);
		/*
		 * Validate that the user was deleted and if its references were updated
		 */
		try {
			assertNull(userAPI.loadByUserByEmail(userToDelete.getEmailAddress(),systemUser,false));
		}catch(com.dotmarketing.business.NoSuchUserException e){
			//enter here if the user doesn't exist as is expected
		}
		assertNull(roleAPI.loadRoleByKey(userToDelete.getUserId()));

		assertNotNull(userAPI.loadByUserByEmail(replacementUser.getEmailAddress(),systemUser,false));

		link = APILocator.getMenuLinkAPI().find(link.getInode(), systemUser, false);
		assertEquals(replacementUser.getUserId(), link.getOwner());
		assertEquals(replacementUser.getUserId(), link.getModUser());

		page =htmlPageAssetAPI.getPageByPath(testFolder.getPath()+page0Str, host, langId, true);
		Logger.info(this, "Page inode:" + page.getInode());
		Logger.info(this, "Page identifier:" + page.getIdentifier());
		assertEquals("Page Owner " + page.getOwner(), replacementUser.getUserId(), page.getOwner());
		assertEquals(replacementUser.getUserId(), page.getModUser());

		List<Contentlet> contentAssets = conAPI.findByStructure(st, systemUser, false, 100,0);
		for(Contentlet content: contentAssets){
			assertEquals(replacementUser.getUserId(), content.getOwner());
			assertEquals(replacementUser.getUserId(), content.getModUser());

			task = workflowAPI.findTaskByContentlet(content);
			assertEquals(replacementUserUserRole.getId(), task.getAssignedTo());
			Logger.info(this, "task.getCreatedBy() = " + task.getCreatedBy());
			Logger.info(this, "replacementUserUserRole.getId() = " + replacementUserUserRole.getId());
			assertEquals(replacementUser.getUserId(), task.getCreatedBy());

			step = workflowAPI.findStepByContentlet(content);
			assertNotNull(step);
			action =  workflowAPI.findActions(step, systemUser).get(0);
			assertEquals(replacementUserUserRole.getId(), action.getNextAssign());

			comments = workflowAPI.findWorkFlowComments(task);
			for(WorkflowComment comm : comments){
				assertEquals(replacementUser.getUserId(), comm.getPostedBy());
			}
		}

		container = containerAPI.getLiveContainerById(container.getIdentifier(), systemUser, false);
		assertEquals(replacementUser.getUserId(), container.getOwner());
		assertEquals(replacementUser.getUserId(), container.getModUser());

		template = templateAPI.find(template.getInode(), systemUser, false);
		assertEquals(replacementUser.getUserId(), template.getOwner());
		assertEquals(replacementUser.getUserId(), template.getModUser());

		CacheLocator.getFolderCache().removeFolder(testFolder, identifierAPI.find(testFolder.getIdentifier()));
		testFolder = folderAPI.findFolderByPath(testFolder.getPath(), host, systemUser, false);
		assertEquals(replacementUser.getUserId(), testFolder.getOwner());

		hostVariable = hostVariableAPI.getVariablesForHost(host.getIdentifier(),replacementUser,false).get(0);
		assertEquals(replacementUser.getUserId(), hostVariable.getLastModifierId());


	}

	private void waitForDeleteCompletedNotification(User userToDelete) throws DotDataException, InterruptedException {

		final int MAX_TIME = 20000;
		final int WAIT_TIME = 1000;

		TimeUtil.waitFor(WAIT_TIME, MAX_TIME, () -> {

			boolean isReindexFinished = false;
			List<Notification> notifications = null;

			try {
				notifications = APILocator.getNotificationAPI().getAllNotifications(systemUser.getUserId());
			} catch (DotDataException e) {
				Logger.error(this, "Unable to get notifications. ", e);
			}

			for (final Notification notification : notifications) {
				final String notificationKey = notification.getMessage().getKey();
				final String expectedKey = "notification.contentapi.reindex.related.content.success";
				final Object[] notificationArgs = notification.getNotificationData().getMessage().getArguments();
                final Object[] expectedArgs = {userToDelete.getUserId() + "/" + userToDelete.getFullName()};
                final String notificationArg = UtilMethods.isSet(notificationArgs)?(String)notificationArgs[0]:"notArg";
                final String expectedArg = UtilMethods.isSet(expectedArgs)?(String)expectedArgs[0]:"expectedArg";

				if (notificationKey.equals(expectedKey) && notificationArg.equals(expectedArg)) {
					isReindexFinished = true;
					break;
				}
			}

			Logger.info(this, "waitForDeleteCompletedNotification isReindexFinished:" + isReindexFinished);
			return isReindexFinished;

		});
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


		}

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


	}

    @Test
    public void testFindAllUsers() throws DotDataException, DotSecurityException {
        String id;
        String userName;
        UserAPI userAPI;

        id = String.valueOf(new Date().getTime());
        userAPI = APILocator.getUserAPI();
        userName = "user" + id;

        final User user = UserTestUtil.getUser(userName, false, true);

        final List<User> result = userAPI.findAllUsers();

        assertNotNull(result);
        assertTrue(result.stream().anyMatch(currentUser -> currentUser.getUserId().equals(user.getUserId())));
        assertTrue(result.stream().noneMatch(currentUser -> currentUser.getCompanyId().equals(User.DEFAULT)));
    }

    @Test
    public void testGetCountUsersByName() throws DotDataException, DotSecurityException {
        String id;
        String userName;
        UserAPI userAPI;

        id = String.valueOf(new Date().getTime());
        userAPI = APILocator.getUserAPI();
        userName = "user" + id;

        UserTestUtil.getUser(userName, false, true);

        long count = userAPI.getCountUsersByName(userName);

        assertTrue(count == 1);
    }

	@Test
	public void testGetCountUsersByNameOrEmail() throws DotDataException, DotSecurityException {
		String id;
		String userName;
		UserAPI userAPI;

		id = String.valueOf(new Date().getTime());
		userAPI = APILocator.getUserAPI();
		userName = "user" + id;

		UserTestUtil.getUser(userName, false, true);

		long count = userAPI.getCountUsersByNameOrEmail(userName + "@fake.org");

		assertTrue(count == 1);



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


	}

	@Test
	public void testGetUsersIdsByCreationDate() throws DotDataException, DotSecurityException {

		UserAPI userAPI = APILocator.getUserAPI();

		List<String> users = userAPI.getUsersIdsByCreationDate(null, 0, 40);
		assertNotNull(users);
		assertTrue(users.size() > 0);

	}
	
	
	
	static List<User> frontEndUsers = null;
  static List<User> backEndUsers = null;
  static String uniqueUserKey = UUIDGenerator.shorty();
	
	private void loadEndUsers() throws DotStateException, DotDataException {
	  if(frontEndUsers!=null) {
	    return;
	  }
    UserAPI userAPI = APILocator.getUserAPI();
    String unique = uniqueUserKey;
    List<User> userList = new ArrayList<>();
    
    for (int i = 0; i < 10; i++) {
      User user = new UserDataGen().firstName("frontend" + unique + i).nextPersisted();
      roleAPI.addRoleToUser(roleAPI.loadFrontEndUserRole(), user);
      userList.add(user);
    }
    frontEndUsers = userList;
    userList = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      User user = new UserDataGen().firstName("backend" + unique + i).nextPersisted();
      roleAPI.addRoleToUser(roleAPI.loadBackEndUserRole(), user);
      userList.add(user);
    }
    backEndUsers=userList;
	  
	  
	}
	
	/***
	 * this method tests that the count of users we get when searching users
	 * by a text filter and role membership
	 * returns proper values
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
  @Test
  public void testUserApiGetCount() throws DotDataException, DotSecurityException {

    loadEndUsers();

    long frontEndCount =
        userAPI.getCountUsersByNameOrEmailOrUserID("frontend" + uniqueUserKey, false, false, roleAPI.loadFrontEndUserRole().getId());
    long backEndCount =
        userAPI.getCountUsersByNameOrEmailOrUserID("backend" + uniqueUserKey, false, false, roleAPI.loadBackEndUserRole().getId());

    assertTrue("should have 10 frontend users, got " + frontEndCount, frontEndCount == 10);

    assertTrue("should have 10 backend users, got " + backEndCount, backEndCount == 10);
    
    long uniqueBackEndUser =
        userAPI.getCountUsersByNameOrEmailOrUserID("backend" + uniqueUserKey + "5", false, false, roleAPI.loadBackEndUserRole().getId());

    assertTrue("should have 1 matching user, got " + uniqueBackEndUser, uniqueBackEndUser == 1);
    
    
  }
	
	
  /***
   * this method tests that the list of users we get when searching users
   * by a text filter and role membership is correct - based on filter and the the role
   * id passed in
   * returns proper values
   * @throws DotDataException
   * @throws DotSecurityException
   */
  @Test
  public void testUserApiFilterUsersByNameAndRole() throws DotDataException, DotSecurityException {

    loadEndUsers();

    List<User> frontEndUsers =
        userAPI.getUsersByNameOrEmailOrUserID("frontend" + uniqueUserKey, 0,20,false, false, roleAPI.loadFrontEndUserRole().getId());
    
    List<User> backEndUsers =
        userAPI.getUsersByNameOrEmailOrUserID("backend" + uniqueUserKey,  0,20,false, false, roleAPI.loadBackEndUserRole().getId());

    assertTrue("should have 10 frontend users, got " + frontEndUsers.size(), frontEndUsers.size() == 10);

    assertTrue("should have 10 backend users, got " + backEndUsers.size(), backEndUsers.size() == 10);
    
    List<User>  uniqueBackEndUser =
        userAPI.getUsersByNameOrEmailOrUserID("backend" + uniqueUserKey + "5",  0,20, false, false, roleAPI.loadBackEndUserRole().getId());

    assertTrue("should have 1 matching user, got " + uniqueBackEndUser.size(), uniqueBackEndUser.size() == 1);
    
    
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


	}

	@Test
	public void testFrontendAndBackendUser()throws DotDataException, DotSecurityException {

		final User backendUser = new UserDataGen().roles(TestUserUtils.getBackendRole()).nextPersisted();

        assertTrue(backendUser.isBackendUser());
		assertFalse(backendUser.isFrontendUser());
		final Role frontEndRole = TestUserUtils.getFrontendRole();
		final Role backendRole = TestUserUtils.getBackendRole();

		final User frontendUser = new UserDataGen().roles(frontEndRole).nextPersisted();

		assertTrue(frontendUser.isFrontendUser());
		assertFalse(frontendUser.isBackendUser());

		final User frontendAndBackendUser = new UserDataGen().roles(frontEndRole).nextPersisted();

		assertTrue(frontendAndBackendUser.isFrontendUser());
		assertFalse(frontendAndBackendUser.isBackendUser());

        APILocator.getRoleAPI().removeRoleFromUser(backendRole,backendUser);
		assertFalse(backendUser.isBackendUser());

		APILocator.getRoleAPI().removeRoleFromUser(frontEndRole,frontendUser);
		assertFalse(frontendUser.isFrontendUser());

	}

	/**
	 * Method to test: {@link User#getUserRole()}
	 * Given Scenario: When creating a new user an UserRole must be created, get that UserRole.
	 * ExpectedResult: UserRole created for the new user.
	 *
	 */
	@Test
	public void test_getUserRole_success() throws Exception{
  		final User newUser = new UserDataGen().roles(TestUserUtils.getFrontendRole(), TestUserUtils.getBackendRole()).nextPersisted();

  		final Role userRole = newUser.getUserRole();

  		assertNotNull(userRole);
		assertEquals(newUser.getUserId(),userRole.getRoleKey());
	}

	/**
	 * Method to test: {@link User#getUserRole()}
	 * Given Scenario: Try to get the UserRole of an user that does not exist.
	 * ExpectedResult: UserRole must be null
	 *
	 */
	@Test
	public void test_getUserRole_null() throws Exception{
		final User newUser = new UserDataGen().roles(TestUserUtils.getFrontendRole(), TestUserUtils.getBackendRole()).next();

		final Role userRole = newUser.getUserRole();

		assertNull(userRole);
	}

	/**
	 * Method to test: {@link UserAPI#getUserIdByToken(String)}
	 * Given Scenario: Create a new user and set it the icqId (token when forgot password).
	 * 					Find the userId associated to the icqId.
	 * ExpectedResult: UserId of the created user.
	 *
	 */
	@Test
	public void test_getUserIdByIcqId_success() throws Exception{
		final User newUser = new UserDataGen().roles(TestUserUtils.getFrontendRole(), TestUserUtils.getBackendRole()).nextPersisted();
		final String icqId = ResetPasswordTokenUtil.createToken();
		newUser.setIcqId(icqId);
		UserUtil.update(newUser);

		final Optional<String> userId = userAPI.getUserIdByToken(icqId);
		assertTrue(userId.isPresent());
		assertEquals(newUser.getUserId(),userId.get());
	}

	/**
	 * Method to test: {@link UserAPI#getUserIdByToken(String)}
	 * Given Scenario: Try to get the userId of an empty icqId
	 * ExpectedResult: DotInvalidTokenException
	 *
	 */
	@Test(expected = DotInvalidTokenException.class)
	public void test_getUserIdByIcqId_icqIdEmpty_throwDotInvalidTokenException() throws Exception{
		userAPI.getUserIdByToken("");
	}

	/**
	 * Method to test: {@link UserAPI#getUserIdByToken(String)}
	 * Given Scenario: Try to get the userId of a null icqId
	 * ExpectedResult: DotInvalidTokenException
	 *
	 */
	@Test(expected = DotInvalidTokenException.class)
	public void test_getUserIdByIcqId_icqIdNull_throwDotInvalidTokenException() throws Exception{
		userAPI.getUserIdByToken(null);
	}

	/**
	 * Method to test: {@link UserAPI#getUserIdByToken(String)}
	 * Given Scenario: Find the userId associated to the icqId.
	 * ExpectedResult: UserId is not present since the icqId was not associated to any user.
	 *
	 */
	@Test
	public void test_getUserIdByIcqId_icqIdDoesNotBelongToAnyUser_returnEmptyOptional() throws Exception{
		final String icqId = ResetPasswordTokenUtil.createToken();

		final Optional<String> userId = userAPI.getUserIdByToken(icqId);
		assertFalse(userId.isPresent());
	}

    /**
     * Method to test: {@link UserAPI#save(User, User, boolean)}
     * Given Scenario: A new user is saved with its additional info
     * ExpectedResult: The new user is persisted with its additional info
     *
     */
	@Test
	public void test_saveNewUser_withAdditionalInfo() throws DotDataException, DotSecurityException {

        final User newUser = new UserDataGen().firstName("backendUser" + System.currentTimeMillis())
                .additionalInfo("suffix", "MySuffix")
                .additionalInfo("title", "MyTitle")
                .additionalInfo("facebookId", "MyFacebookId").nextPersisted();

        final User savedUser = userAPI.loadUserById(newUser.getUserId());

        assertNotNull(savedUser.getAdditionalInfo());
        assertEquals("MySuffix", savedUser.getAdditionalInfo().get("suffix"));
        assertEquals("MyTitle", savedUser.getAdditionalInfo().get("title"));
        assertEquals("MyFacebookId", savedUser.getAdditionalInfo().get("facebookId"));
    }

    /**
     * Method to test: {@link UserAPI#save(User, User, boolean)}
     * Given Scenario: The additional info of an existing user is modified
     * ExpectedResult: The user's additional info is updated in DB correctly
     *
     */
    @Test
    public void test_updateUser_withAdditionalInfo() throws DotDataException, DotSecurityException {

        final User newUser = new UserDataGen().firstName("backendUser" + System.currentTimeMillis())
                .additionalInfo("suffix", "MySuffix")
                .additionalInfo("title", "MyTitle")
                .additionalInfo("facebookId", "MyFacebookId").nextPersisted();

        newUser.getAdditionalInfo().put("suffix", "EditedSuffix");
        newUser.getAdditionalInfo().put("twitterId", "MyTwitterId");
        
        userAPI.save(newUser, systemUser, false);

        final User savedUser = userAPI.loadUserById(newUser.getUserId());

        assertNotNull(savedUser.getAdditionalInfo());
        assertEquals("EditedSuffix", savedUser.getAdditionalInfo().get("suffix"));
        assertEquals("MyTitle", savedUser.getAdditionalInfo().get("title"));
        assertEquals("MyFacebookId", savedUser.getAdditionalInfo().get("facebookId"));
        assertEquals("MyTwitterId", savedUser.getAdditionalInfo().get("twitterId"));
    }

	/**
	 * Method to test: {@link UserAPI#loadByUserByEmail}
	 * Given Scenario: Given limited user
	 * ExpectedResult: Limited user should be able to load self-user
	 *
	 */
	@Test
	public void test_loadByUserByEmail_selfUser() throws DotDataException, DotSecurityException {

		final User newUser = new UserDataGen().firstName("limitedUser" + System.currentTimeMillis())
			.nextPersisted();

		userAPI.save(newUser, systemUser, false);

		final User ownUser = userAPI.loadByUserByEmail(newUser.getEmailAddress(),
				newUser, false);

		assertNotNull(ownUser.getAdditionalInfo());
		assertEquals(newUser.getUserId(), ownUser.getUserId());
	}

	/**
	 * Method to test: {@link UserAPI#loadUserById(String, User, boolean)}
	 * Given Scenario: Given limited user
	 * ExpectedResult: Limited user should be able to load self-user
	 *
	 */
	@Test
	public void test_loadByUserById_selfUser() throws DotDataException, DotSecurityException {

		final User newUser = new UserDataGen().firstName("limitedUser" + System.currentTimeMillis())
				.nextPersisted();

		userAPI.save(newUser, systemUser, false);

		final User ownUser = userAPI.loadUserById(newUser.getUserId(),
				newUser, false);

		assertEquals(newUser.getUserId(), ownUser.getUserId());
	}

	/**
	 * Method to test: {@link UserAPI#save(User, User, boolean)}
	 * Given Scenario: Given limited user
	 * ExpectedResult: Limited user should be able to save self-user
	 *
	 */
	@Test
	public void test_save_selfUser() throws DotDataException, DotSecurityException {

		final User newUser = new UserDataGen().firstName("limitedUser" + System.currentTimeMillis())
				.nextPersisted();

		final String userId = newUser.getUserId();

		userAPI.save(newUser, systemUser, false);

		final User ownUser = userAPI.loadUserById(userId,
				APILocator.systemUser(), false);

		// let's modify the user and save it using the self-user
		ownUser.setFirstName("modifiedLimitedUser"+ + System.currentTimeMillis());
		userAPI.save(ownUser, ownUser, false);

		final User reloadedUser = userAPI.loadUserById(userId,
				APILocator.systemUser(), false);

		assertTrue(reloadedUser.getFirstName().contains("modifiedLimitedUser"));
	}

	/**
	 * Method to test: {@link User#toMap()}
	 * Given Scenario: Any user that lacks email address should have a blank gravatar
	 * ExpectedResult: While the user has an email the gravatar prop should be different from null if the user does not have an email gravatar should come back as an empty string
	 * @throws Exception
	 */
	@Test
	public void Test_User_Returns_No_Gravatar_When_No_Email_Is_Available_And_OtherWise()
			throws Exception {
		final User user = new UserDataGen().firstName("noEmailUser" + System.currentTimeMillis())
				.emailAddress(String.format("any%d@dotCMS.com", System.currentTimeMillis()))
				.nextPersisted();
		assertNotNull(user.getEmailAddress());
		final Map<String, Object> userMapWithEmail = user.toMap();
		assertNotNull(userMapWithEmail.get("emailAddress"));
		assertNotNull(userMapWithEmail.get("gravitar"));

		//Wipe out email
		user.setEmailAddress(null);
		userAPI.save(user, APILocator.systemUser(), false);
		//Verify it is gone
		assertNull(user.getEmailAddress());
        //Test that gravatar comes back as blank
		final Map<String, Object> userMapNoEmail = user.toMap();
		assertEquals(userMapNoEmail.get("emailAddress"), StringPool.BLANK);
		assertEquals(userMapNoEmail.get("gravitar"), StringPool.BLANK);
	}


	/**
	 * Method to test: {@link UserAPI#save(User, User, boolean)}
	 * Given Scenario: A new user is saved
	 * ExpectedResult: The new user is persisted successfully
	 *
	 */
	@Test
	public void test_saveNewUser_withFirstName() throws DotDataException, DotSecurityException {
		String firstname = "Test User";
		final User newUser = new UserDataGen().firstName(firstname).nextPersisted();

        assertEquals("Test User", newUser.getFirstName());

	}


	/**
	 * Method to test: {@link UserAPI#save(User, User, boolean)}
	 * Given Scenario: A new user creation with user first name being created exceeding maximum characters allowed
	 * ExpectedResult: The new user creation should throw an error
	 *
	 */
	@Test
	public void test_saveNewUser_withFirstNameExceeding() throws DotSecurityException {
        String firstName ="xzYWuASxGQZemNRBdEjouKEUOZvOoERBFgCxJHuqQAvpjywvZuARWRThKTONozXwBXUOkXnwhzbTtovqFBxYNzNQDJxfhecGTRfOAAA";
		final User newUser = new UserDataGen().firstName("test").nextPersisted();

		try {
			newUser.setFirstName(firstName);
			// Call the save method with the user object
			userAPI.save(newUser, APILocator.systemUser(), false, false);
			fail("Expected DotDataException to be thrown");
		} catch (DotDataException e) {
			// Verify that the exception message matches the expected message
			assertEquals("Length of First Name provided exceeds the maximum limit 100", e.getMessage());
		}
	}

	/**
	 * Method to test: {@link UserAPI#save(User, User, boolean)}
	 * Given Scenario: A new user creation with user lastname being created exceeding maximum characters allowed
	 * ExpectedResult: The new user creation should throw an error
	 *
	 */
	@Test
	public void test_saveNewUser_withLastNameExceeding() throws  DotSecurityException {
		String lastName ="xzYWuASxGQZemNRBdEjouKEUOZvOoERBFgCxJHuqQAvpjywvZuARWRThKTONozXwBXUOkXnwhzbTtovqFBxYNzNQDJxfhecGTRfOAAA";
		final User newUser = new UserDataGen().firstName("test").nextPersisted();

		try {
			newUser.setLastName(lastName);
			// Call the save method with the user object
			userAPI.save(newUser, APILocator.systemUser(), false, false);
			fail("Expected DotDataException to be thrown");
		} catch (DotDataException e) {
			// Verify that the exception message matches the expected message
			assertEquals("Length of Last Name provided exceeds the maximum limit 100", e.getMessage());
		}
	}

	/**
	 * Method to test: {@link UserAPI#save(User, User, boolean)}
	 * Given Scenario: A new user creation with user email being created exceeding maximum characters allowed
	 * ExpectedResult: The new user creation should throw an error
	 *
	 */
	@Test
	public void test_saveNewUser_withEmailNameExceeding() throws  DotSecurityException {
		String email ="xzYWuASxGQZemNRBdEjouKEUOZvOoERBFgCxJHuqQAvpjywvZuARWRThKTONozXwBXUOkXnwhzbTtovqFBxYNzNQDJxfhecGTRf@dotcms.com";
		final User newUser = new UserDataGen().firstName("test").nextPersisted();

		try {
			newUser.setEmailAddress(email);
			// Call the save method with the user object
			userAPI.save(newUser, APILocator.systemUser(), false, false);
			fail("Expected DotDataException to be thrown");
		} catch (DotDataException e) {
			// Verify that the exception message matches the expected message
			assertEquals("Length of Email Address provided exceeds the maximum limit 100", e.getMessage());
		}
	}
}