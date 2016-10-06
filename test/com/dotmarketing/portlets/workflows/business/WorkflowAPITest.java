package com.dotmarketing.portlets.workflows.business;

import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.dotcms.TestBase;
import com.dotcms.repackage.org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.structure.factories.FieldFactory;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.portlets.workflows.model.WorkflowStep;
import com.dotmarketing.servlets.test.ServletTestRunner;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

/**
 * This class test the workflow API issues
 * @author oswaldogallango
 *
 */

@Ignore("Temporarily ignore this. https://github.com/dotCMS/core/issues/9785")
public class WorkflowAPITest extends TestBase{

	/**
	 * This Test validate that a workflow step could not be deleted if depends of another step or
	 * has a contentlet related
	 * @throws DotDataException
	 * @throws IOException
	 * @throws DotSecurityException
	 */
	@Test
	public void issue5197() throws DotDataException, IOException, DotSecurityException{
		HttpServletRequest req=ServletTestRunner.localRequest.get();
		User systemUser = APILocator.getUserAPI().getSystemUser();
		PermissionAPI perAPI = APILocator.getPermissionAPI();
		RoleAPI roleAPI = APILocator.getRoleAPI();
		Host host = APILocator.getHostAPI().findDefaultHost(systemUser, true);

		User adminUser = APILocator.getUserAPI().loadByUserByEmail("admin@dotcms.com", systemUser, false);
		Role role = roleAPI.getUserRole(adminUser);
		/*
		 * Create workflow scheme
		 */
		String schemeName = "issue5197-"+UtilMethods.dateToHTMLDate(new Date(), "MM-dd-yyyy-HHmmss");
		String baseURL = "http://"+req.getServerName()+":"+req.getServerPort()+"/DotAjaxDirector/com.dotmarketing.portlets.workflows.business.TestableWfSchemeAjax?cmd=save&schemeId=&schemeName="+schemeName;
		URL testUrl = new URL(baseURL);
		IOUtils.toString(testUrl.openStream(),"UTF-8");
		WorkflowAPI wapi = APILocator.getWorkflowAPI();
		WorkflowScheme ws = wapi.findSchemeByName(schemeName);
		Assert.assertTrue(UtilMethods.isSet(ws));

		/*
		 * Create scheme step1
		 */
		baseURL = "http://"+req.getServerName()+":"+req.getServerPort()+"/DotAjaxDirector/com.dotmarketing.portlets.workflows.business.TestableWfStepAjax?cmd=add&stepName=Edit&schemeId=" +  ws.getId();
		testUrl = new URL(baseURL);
		IOUtils.toString(testUrl.openStream(),"UTF-8");
		List<WorkflowStep> steps = wapi.findSteps(ws);
		Assert.assertTrue(steps.size()==1);
		WorkflowStep step1 = steps.get(0);

		/*
		 * Create scheme step2
		 */
		baseURL = "http://"+req.getServerName()+":"+req.getServerPort()+"/DotAjaxDirector/com.dotmarketing.portlets.workflows.business.TestableWfStepAjax?cmd=add&stepName=Publish&schemeId=" +  ws.getId();
		testUrl = new URL(baseURL);
		IOUtils.toString(testUrl.openStream(),"UTF-8");
		steps = wapi.findSteps(ws);
		Assert.assertTrue(steps.size()==2);
		WorkflowStep step2 = steps.get(1);

		/*
		 * Add action to scheme step1
		 */
		baseURL = "http://"+req.getServerName()+":"+req.getServerPort()+"/DotAjaxDirector/com.dotmarketing.portlets.workflows.business.TestableWfActionAjax?cmd=save&stepId="+step1.getId()+"&schemeId="+UtilMethods.webifyString(ws.getId())+"&actionName=Edit&whoCanUse=";
		baseURL+=role.getId()+",&actionIconSelect=workflowIcon&actionAssignable=true&actionCommentable=true&actionRequiresCheckout=false&actionRoleHierarchyForAssign=false";
		baseURL+="&actionAssignToSelect="+role.getId()+"&actionNextStep="+step2.getId()+"&actionCondition=";
		testUrl = new URL(baseURL);
		IOUtils.toString(testUrl.openStream(),"UTF-8");
		List<WorkflowAction> actions1= wapi.findActions(step1, systemUser);
		Assert.assertTrue(actions1.size()==1);
		WorkflowAction action1 = actions1.get(0);

		/*
		 * Add action to scheme step2
		 */
		baseURL = "http://"+req.getServerName()+":"+req.getServerPort()+"/DotAjaxDirector/com.dotmarketing.portlets.workflows.business.TestableWfActionAjax?cmd=save&stepId="+step2.getId()+"&schemeId="+UtilMethods.webifyString(ws.getId())+"&actionName=Publish&whoCanUse=";
		baseURL+=role.getId()+",&actionIconSelect=workflowIcon&actionAssignable=true&actionCommentable=true&actionRequiresCheckout=false&actionRoleHierarchyForAssign=false";
		baseURL+="&actionAssignToSelect="+role.getId()+"&actionNextStep="+step2.getId()+"&actionCondition=";

		testUrl = new URL(baseURL);
		IOUtils.toString(testUrl.openStream(),"UTF-8");
		List<WorkflowAction> actions2= wapi.findActions(step2, systemUser);
		Assert.assertTrue(actions2.size()==1);
		WorkflowAction action2 = actions2.get(0);

		/*
		 * Create structure and add workflow scheme
		 */
		Structure st = CacheLocator.getContentTypeCache().getStructureByVelocityVarName("Issue5197Structure");
		if(!UtilMethods.isSet(st) || !UtilMethods.isSet(st.getInode())){
			st = new Structure();
			st.setHost(host.getIdentifier());
			st.setDescription("Testing issue 5197");
			st.setName("Issue5197Structure");
			st.setVelocityVarName("Issue5197Structure");
			st.setStructureType(Structure.STRUCTURE_TYPE_CONTENT);
			st.setFixed(false);
			st.setOwner(systemUser.getUserId());
			st.setExpireDateVar("");
			st.setPublishDateVar("");
			StructureFactory.saveStructure(st);

			Permission p = new Permission();
			p.setInode(st.getPermissionId());
			p.setRoleId(roleAPI.loadCMSAnonymousRole().getId());
			p.setPermission(PermissionAPI.PERMISSION_READ);
			perAPI.save(p, st, systemUser, true);

			p = new Permission();
			p.setInode(st.getPermissionId());
			p.setRoleId(roleAPI.loadCMSAnonymousRole().getId());
			p.setPermission(PermissionAPI.PERMISSION_EDIT);
			perAPI.save(p, st, systemUser, true);

			p = new Permission();
			p.setInode(st.getPermissionId());
			p.setRoleId(roleAPI.loadCMSAnonymousRole().getId());
			p.setPermission(PermissionAPI.PERMISSION_PUBLISH);
			perAPI.save(p, st, systemUser, true);	

			Field title = new Field("Title",Field.FieldType.TEXT,Field.DataType.TEXT,st,true,true,true,1,"", "", "", false, false, true);
			title.setVelocityVarName("title");
			FieldFactory.saveField(title);
			FieldsCache.addField(title);
		}
		APILocator.getWorkflowAPI().saveSchemeForStruct(st, ws);
		/*
		 * Create test content and set it up in scheme step
		 */
		Contentlet contentlet1 = new Contentlet();
		contentlet1.setStructureInode(st.getInode());
		contentlet1.setHost(host.getIdentifier());
		contentlet1.setLanguageId(APILocator.getLanguageAPI().getDefaultLanguage().getId());
		contentlet1.setStringProperty("title", "test5197-1"+UtilMethods.dateToHTMLDate(new Date(), "MM-dd-yyyy-HHmmss"));
		contentlet1.setHost(host.getIdentifier());

		contentlet1 = APILocator.getContentletAPI().checkin(contentlet1, systemUser,false);
		if(perAPI.doesUserHavePermission(contentlet1, PermissionAPI.PERMISSION_PUBLISH, systemUser))
			APILocator.getVersionableAPI().setLive(contentlet1);

		/*
		 * Test that delete is not possible for step2
		 * while has associated step or content
		 */
		contentlet1.setStringProperty("wfActionId", action1.getId());
		contentlet1.setStringProperty("wfActionComments", "step1");
		contentlet1.setStringProperty("wfActionAssign", role.getId());
		wapi.fireWorkflowNoCheckin(contentlet1, systemUser);

		contentlet1.setStringProperty("wfActionId", action2.getId());
		contentlet1.setStringProperty("wfActionComments", "step2");
		contentlet1.setStringProperty("wfActionAssign", role.getId());
		wapi.fireWorkflowNoCheckin(contentlet1, systemUser);

		WorkflowStep  currentStep = wapi.findStepByContentlet(contentlet1);
		Assert.assertTrue(currentStep.getId().equals(step2.getId()));

		/*
		 * Validate that step2 could not be deleted
		 */
		try{
			wapi.deleteStep(step2);
		}catch(Exception e){
			/*
			 * Should enter here with this exception
			 * </br> <b> Step : 'Publish' is being referenced by </b> </br></br> Step : 'Edit' ->  Action : 'Edit' </br></br>
			 */
		}
		Assert.assertTrue(UtilMethods.isSet(wapi.findStep(step2.getId())));
		/*
		 * Validate correct deletion of step1
		 */
		wapi.deleteStep(step1);
		
		/*
		 * Validate that the step 1 was deleted from the scheme
		 */
		steps = wapi.findSteps(ws);
		Assert.assertTrue(steps.size()==1);
		Assert.assertTrue(steps.get(0).getId().equals(step2.getId()));

		/*
		 * Validate that step2 could not be deleted
		 */
		try{
			wapi.deleteStep(step2);
		}catch(Exception e){
			/*
			 * Should enter here with this exception
			 * </br> <b> Step : 'Publish' is being referenced by: X Contentlet(s) </br></br>
			 */
		}
		currentStep = wapi.findStepByContentlet(contentlet1);
		Assert.assertTrue(currentStep.getId().equals(step2.getId()));
		
		/*
		 * Validate that step2 is not deleted
		 */
		steps = wapi.findSteps(ws);
		Assert.assertTrue(steps.size()==1);
		Assert.assertTrue(steps.get(0).getId().equals(step2.getId()));
		
		/*
		 * Clean test
		 */
		APILocator.getContentletAPI().delete(contentlet1, systemUser, false);
		APILocator.getStructureAPI().delete(st, systemUser);
		wapi.deleteStep(step2);
		wapi.deleteScheme(ws);
	}

}
