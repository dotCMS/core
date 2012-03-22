package com.dotmarketing.portlets.structure.action;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletConfig;
import javax.portlet.WindowState;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;

import com.dotmarketing.beans.Tree;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.factories.TreeFactory;
import com.dotmarketing.portal.struts.DotPortletAction;
import com.dotmarketing.portlets.structure.factories.RelationshipFactory;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.structure.struts.RelationshipForm;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PortletURLUtil;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.Validator;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.util.Constants;
import com.liferay.util.servlet.SessionMessages;

public class EditRelationshipAction extends DotPortletAction {
	
	public void processAction(
			ActionMapping mapping, ActionForm form, PortletConfig config,
			ActionRequest req, ActionResponse res)
	throws Exception {
		
		String cmd = req.getParameter(Constants.CMD);
		String referer = req.getParameter("referer");		
		if (!UtilMethods.isSet(referer)) {
			Map<String, String[]> params = new HashMap<String, String[]> ();
			params.put("struts_action", new String[] {"/ext/structure/view_relationships"});
			referer = PortletURLUtil.getActionURL(req, WindowState.MAXIMIZED.toString(), params);
		}
		
		//Retrive the field in the request
		_retrieveRelationship(form,req,res);

		HibernateUtil.startTransaction();		       
		
		/*
		 * saving the field
		 */
		if ((cmd != null) && cmd.equals(Constants.ADD)) {
			try 
			{
				Logger.debug(this, "Calling Add/Edit Method");
				if (Validator.validate(req, form, mapping)) {
					if (_saveRelationship(form,req,res)) {
						_sendToReferral(req,res,referer);
						return;
					}
				} 
			} 
			catch (Exception ae) 
			{
				_handleException(ae, req);
				return;
			}			
		}
		/*
		 * If we are deleting the field,
		 * run the delete action and return to the list
		 *
		 */
		else if ((cmd != null) && cmd.equals(Constants.DELETE)) 
		{
			try 
			{
				Logger.debug(this, "Calling Delete Method");
				_deleteRelationship(form,req,res);
			} 
			catch (Exception ae) 
			{
				_handleException(ae, req);
				return;
			}
			if (UtilMethods.isSet(referer))
				_sendToReferral(req,res,referer);
			else
				setForward(req, "portlet.ext.structure.view_relationships");
			return;
		} 
		HibernateUtil.commitTransaction();
		
		//otherwise edit field
		_loadForm(form, req, res);
		setForward(req, "portlet.ext.structure.edit_relationship");		
	}
	
	private void _retrieveRelationship(ActionForm form,ActionRequest req, ActionResponse res)
	{
		Relationship relationship = new Relationship();
		String inodeString = req.getParameter("inode");
		if(InodeUtils.isSet(inodeString))
		{
			relationship = RelationshipFactory.getRelationshipByInode(inodeString);
		}
		else
		{
			relationship = new Relationship ();
		}
		
		if(relationship.isFixed()){
			String message = "warning.object.isfixed";
			SessionMessages.add(req, "message", message);
		}
		
		req.setAttribute(WebKeys.Relationship.RELATIONSHIP_EDIT,relationship);
	}
	
	private void _loadForm(ActionForm form,ActionRequest req, ActionResponse res)
	{
		try
		{
			String cmd = req.getParameter(Constants.CMD);
			if ((cmd == null) || !cmd.equals(Constants.ADD)) {
				RelationshipForm relationshipForm = (RelationshipForm) form;
				Relationship relationship = (Relationship) req.getAttribute(WebKeys.Relationship.RELATIONSHIP_EDIT);
						
				//Copy properties to the form
				BeanUtils.copyProperties(relationshipForm, relationship);
			}
			
			List structures = StructureFactory.getStructures();
			req.setAttribute(WebKeys.Relationship.STRUCTURES_LIST, structures);
		}
		catch(Exception ex)
		{
			Logger.error(EditRelationshipAction.class,ex.toString(), ex);
		}
	}
	
	private boolean _saveRelationship(ActionForm form,ActionRequest req, ActionResponse res)
	{
		try
		{
			RelationshipForm relationshipForm = (RelationshipForm) form;
			Relationship relationship = (Relationship) req.getAttribute(WebKeys.Relationship.RELATIONSHIP_EDIT);
			Structure parentStructure = null;
			Structure childStructure  = null;
			if(InodeUtils.isSet(relationshipForm.getParentStructureInode()) || 
					InodeUtils.isSet(relationshipForm.getChildStructureInode())){
				parentStructure = StructureCache.getStructureByInode(relationshipForm.getParentStructureInode());
				childStructure  = StructureCache.getStructureByInode(relationshipForm.getChildStructureInode());
			}
			if(parentStructure!=null && childStructure!=null){

				if(!relationship.isFixed()){

					String relationshipTypeValue = relationshipForm.getParentRelationName().replaceAll("\\s", "_").replaceAll("[^a-zA-Z0-9\\_]", "") +
					"-" + relationshipForm.getChildRelationName().replaceAll("\\s", "_").replaceAll("[^a-zA-Z0-9\\_]", "");

					String lastRelationshipTypeValue = relationship.getRelationTypeValue();

					//Preserve old tree relationship if the relation name be changed
					if (InodeUtils.isSet(relationship.getInode()) && !relationshipTypeValue.equals(lastRelationshipTypeValue)) {
						DotConnect dc = new DotConnect ();
						dc.setSQL("update tree set relation_type = '" + relationshipTypeValue + 
								"' where relation_type = '" + lastRelationshipTypeValue + "'");
						dc.getResult();
					}

					//Copy properties from the form
					BeanUtils.copyProperties(relationship,relationshipForm);

					if (!relationshipTypeValue.equals(relationship.getRelationTypeValue())) {
						Relationship oRel = RelationshipFactory.getRelationshipByRelationTypeValue(relationshipTypeValue);
						if (InodeUtils.isSet(oRel.getInode()) && !oRel.getInode().equalsIgnoreCase(relationship.getInode())) {
							String message = "error.relationship.same.relation.exist";
							SessionMessages.add(req, "error",message);
							return false;
						}

						List<Tree> treesToUpdate = TreeFactory.getTreesByRelationType(relationshipForm.getRelationTypeValue());
						for (Tree theTree : treesToUpdate) {
							theTree.setRelationType(relationshipTypeValue);
						}
						relationship.setRelationTypeValue(relationshipTypeValue);
					}

					//saves this relationship
					RelationshipFactory.saveRelationship(relationship);

					String message = "message.relationship.saved";
					SessionMessages.add(req, "message",message);
					return true;
				}else{
					throw new DotRuntimeException("Error: cannot save a fixed relationship");
				}	
			}else{
				String message = parentStructure!=null?"message.relationship.childstructureinodemsg":childStructure!=null?"message.relationship.parentstructureinodemsg":"";
				SessionMessages.add(req, "error", message);
				return false;
			}
		}
		catch(Exception ex)
		{
			Logger.debug(EditRelationshipAction.class,ex.toString());
		}
		return false;
	}
	
	private void _deleteRelationship(ActionForm form,ActionRequest req, ActionResponse res) throws DotHibernateException
	{
		Relationship relationship = (Relationship) req.getAttribute(WebKeys.Relationship.RELATIONSHIP_EDIT);
		
		TreeFactory.deleteTreesByRelationType(relationship.getRelationTypeValue());
		
		RelationshipFactory.deleteRelationship(relationship);
		
		String message = "message.relationship.deleted";
		SessionMessages.add(req, "message",message);				
	}
	
}
