package com.dotmarketing.portlets.structure.action;

import static com.dotmarketing.business.PermissionAPI.PERMISSION_PUBLISH;

import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.model.type.SimpleContentType;
import com.dotcms.contenttype.transform.contenttype.StructureTransformer;
import com.dotcms.rendering.velocity.services.ContentTypeLoader;
import com.dotcms.repackage.javax.portlet.ActionRequest;
import com.dotcms.repackage.javax.portlet.ActionResponse;
import com.dotcms.repackage.javax.portlet.PortletConfig;
import com.dotcms.repackage.javax.portlet.WindowState;
import com.dotcms.repackage.org.apache.struts.action.ActionForm;
import com.dotcms.repackage.org.apache.struts.action.ActionMapping;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portal.struts.DotPortletAction;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.form.business.FormAPI;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.structure.struts.StructureForm;
import com.dotmarketing.portlets.widget.business.WidgetAPI;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.util.ActivityLogger;
import com.dotmarketing.util.HostUtil;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.Validator;
import com.dotmarketing.util.VelocityUtil;
import com.dotmarketing.util.WebKeys;
import com.google.common.collect.ImmutableList;
import com.liferay.portal.model.User;
import com.liferay.portal.struts.ActionException;
import com.liferay.portal.util.Constants;
import com.liferay.portlet.ActionRequestImpl;
import com.liferay.util.servlet.SessionMessages;
import java.net.URLDecoder;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.beanutils.BeanUtils;

public class EditStructureAction extends DotPortletAction {

	/**
	 * API objects 
	 */
	private ContentletAPI conAPI = APILocator.getContentletAPI();
	private WidgetAPI wAPI = APILocator.getWidgetAPI();
	private FormAPI fAPI = APILocator.getFormAPI();
	private HostAPI hostAPI = APILocator.getHostAPI();
	private FolderAPI folderAPI = APILocator.getFolderAPI();
	private PermissionAPI perAPI = APILocator.getPermissionAPI();
	

	public void processAction(ActionMapping mapping, ActionForm form, PortletConfig config, ActionRequest req, ActionResponse res) throws Exception {

		String cmd = req.getParameter(Constants.CMD);
		String referer = req.getParameter("referer");

		if ((referer != null) && (referer.length() != 0)) {
			referer = URLDecoder.decode(referer, "UTF-8");
		}

		// Load the structure in the request
		try {
			Logger.debug(this, "Calling Retrieve method");
			_loadStructure(form, req, res);
		} catch (Exception ae) {
			_handleException(ae, req);
			return;
		}

		HibernateUtil.startTransaction();
		boolean returnToList = false;
		/*
		 * If we are updating the workflow message, copy the information from
		 * the struts bean to the hbm inode and run the update action and return
		 * to the list
		 */
		if ((cmd != null) && cmd.equals(Constants.ADD)) {
			try {
				Logger.debug(this, "Calling Add/Edit Method");
				if (Validator.validate(req, form, mapping)) {

					if (UtilMethods.isSet(((StructureForm) form).getInode())) {
						returnToList = true;
					}

					_saveStructure(form, req, res);

				}

			} catch (Exception ae) {
				_handleException(ae, req);
				return;
			}

		}
		/*
		 * If we are deleteing the structure, run the delete action and return
		 * to the list
		 */
		else if ((cmd != null) && cmd.equals(Constants.DELETE)) {
			try {
				Logger.debug(this, "Calling Delete Method");
				_deleteStructure(form, req, res);
			} catch (Exception ae) {
				_handleException(ae, req);
				return;
			}
			_sendToReferral(req, res, referer);
			return;
		} else if ((cmd != null) && cmd.equals(WebKeys.Structure.SET_DEFAULT)) {
			try {
				_defaultStructure(form, req, res);
			} catch (Exception ae) {
				_handleException(ae, req);
				return;
			}
			_sendToReferral(req, res, referer);
			return;
		} else if ((cmd != null) && cmd.equals(com.dotmarketing.util.Constants.RESET)) {
			try {
				_resetIntervals(form, req, res);
			} catch (Exception ae) {
				_handleException(ae, req);
				return;
			}
			_sendToReferral(req, res, referer);
			return;
		}

		HibernateUtil.closeAndCommitTransaction();
		_loadForm(form, req, res);
		if (returnToList) {
			if (!UtilMethods.isSet(referer)) {
				java.util.Map<String, String[]> params = new java.util.HashMap<String, String[]>();

				if (((StructureForm) form).getStructureType() == Structure.STRUCTURE_TYPE_FORM) {
					params.put("struts_action", new String[] { "/ext/formhandler/view_form" });
				} else {
					params.put("struts_action", new String[] { "/ext/structure/view_structure" });
				}

				referer = com.dotmarketing.util.PortletURLUtil.getActionURL(req, WindowState.MAXIMIZED.toString(), params);
			}
			_sendToReferral(req, res, referer);
		} else {
			setForward(req, "portlet.ext.structure.edit_structure");
		}
	}

	@SuppressWarnings("deprecation")
	private Structure _loadStructure(ActionForm form, ActionRequest req, ActionResponse res) throws ActionException, DotDataException, DotSecurityException {

		User user = _getUser(req);
		String inodeString = req.getParameter("inode");
		ContentType type;
		Structure struc = new Structure();
		try{
			if(!UtilMethods.isSet(inodeString)) {
				type= ContentTypeBuilder.instanceOf(SimpleContentType.class);
			} else {
				type = APILocator.getContentTypeAPI(user).find(inodeString);
				struc = new StructureTransformer(type).asStructure();
			}
		} catch(NotFoundInDbException nodb){
			type= ContentTypeBuilder.instanceOf(SimpleContentType.class);
		}

		if(!type.fixed()){//GIT-780
			if(type.baseType() == BaseContentType.WIDGET
					&& type.variable().equalsIgnoreCase(FormAPI.FORM_WIDGET_STRUCTURE_NAME_VELOCITY_VAR_NAME)){
						type = ContentTypeBuilder.builder(type).fixed(true).build();
						APILocator.getContentTypeAPI(user).save(type);
			}
		}

		
		req.setAttribute(WebKeys.Structure.STRUCTURE, struc);

		boolean searchable = false;


		List<com.dotcms.contenttype.model.field.Field> fields = type.fields();
		for (com.dotcms.contenttype.model.field.Field f : fields) {
			if (f.indexed()) {
				searchable = true;
				break;
			}
		}

		if (!searchable && InodeUtils.isSet(type.inode())) {
			String message = "warning.structure.notsearchable";
			SessionMessages.add(req, "message", message);

		}

		if (type.fixed()) {
			String message = "warning.object.isfixed";
			SessionMessages.add(req, "message", message);
		}


		return new StructureTransformer(type).asStructure();
	}

	private void _saveStructure(ActionForm form, ActionRequest req, ActionResponse res) {
		try {
			StructureForm structureForm = (StructureForm) form;
			boolean newStructure = !UtilMethods.isSet(structureForm.getInode());
			boolean publishChanged = false;
			boolean expireChanged = false;
			
			
			Structure structure = (Structure) req.getAttribute(WebKeys.Structure.STRUCTURE);

			User user = _getUser(req);
			HttpServletRequest httpReq = ((ActionRequestImpl) req).getHttpServletRequest();


			if (UtilMethods.isSet(structure.getVelocityVarName())) {
				structureForm.setVelocityVarName(structure.getVelocityVarName());
			}
			/** moved to api
			// If the structure is fixed the name cannot be changed
			if (structure.isFixed()) {
				structureForm.setName(structure.getName());
			}

		
			// if I'm editing a structure the structureType couldn't not be
			// change
			if (UtilMethods.isSet(structure.getInode()) && InodeUtils.isSet(structure.getInode())) {
				// reset the structure type to it's original value
				structureForm.setStructureType(structure.getStructureType());
			}
			if (UtilMethods.isSet(structure.getVelocityVarName())) {
				structureForm.setVelocityVarName(structure.getVelocityVarName());
			}
			**/
			/**
			if (UtilMethods.isSet(structureForm.getHost())) {
				if (!structureForm.getHost().equals(Host.SYSTEM_HOST) && hostAPI.findSystemHost().getIdentifier().equals(structureForm.getHost())) {
					structureForm.setHost(Host.SYSTEM_HOST);
				}
				structureForm.setFolder("SYSTEM_FOLDER");
			} else if (UtilMethods.isSet(structureForm.getFolder())) {
				structureForm.setHost(folderAPI.find(structureForm.getFolder(), user, false).getHostId());
			}
			**/
			/** 
			 * moved to ContentTypeAPI
			 * 
			if (UtilMethods.isSet(structureForm.getHost()) && (!UtilMethods.isSet(structureForm.getFolder()) || structureForm.getFolder().equals("SYSTEM_FOLDER"))) {
				Host host = hostAPI.find(structureForm.getHost(), user, false);
				if (host != null) {
					if (structure.getStructureType() == Structure.STRUCTURE_TYPE_FORM) {
						if (!perAPI.doesUserHavePermissions(host, "PARENT:" + PermissionAPI.PERMISSION_CAN_ADD_CHILDREN + ", STRUCTURES:" + PermissionAPI.PERMISSION_PUBLISH, user)) {
							throw new DotDataException(LanguageUtil.get(user, "User-does-not-have-add-children-permission-on-host-folder"));
						}
					} else {
						if (!perAPI.doesUserHavePermission(host, PermissionAPI.PERMISSION_CAN_ADD_CHILDREN, user)) {
							throw new DotDataException(LanguageUtil.get(user, "User-does-not-have-add-children-permission-on-host-folder"));
						}
					}
				}
			}

			if (UtilMethods.isSet(structureForm.getFolder()) && !structureForm.getFolder().equals("SYSTEM_FOLDER")) {
				Folder folder = folderAPI.find(structureForm.getFolder(), user, false);
				if (folder != null) {
					if (structure.getStructureType() == Structure.STRUCTURE_TYPE_FORM) {
						if (!perAPI.doesUserHavePermissions(folder, "PARENT:" + PermissionAPI.PERMISSION_CAN_ADD_CHILDREN + ", STRUCTURES:" + PermissionAPI.PERMISSION_PUBLISH,
								user)) {
							throw new DotDataException(LanguageUtil.get(user, "User-does-not-have-add-children-permission-on-host-folder"));
						}
					} else {
						if (!perAPI.doesUserHavePermission(folder, PermissionAPI.PERMISSION_CAN_ADD_CHILDREN, user)) {
							throw new DotDataException(LanguageUtil.get(user, "User-does-not-have-add-children-permission-on-host-folder"));
						}
					}
				}
			}
			
			//Checks if Publish was updated
			if (UtilMethods.isSet(structure.getPublishDateVar()) &&
					UtilMethods.isSet(structureForm.getPublishDateVar())){
				
				if (!structure.getPublishDateVar().equals(structureForm.getPublishDateVar())){
					publishChanged = true;
				}
			}else{
				if(structure.getPublishDateVar() != null ||
						structureForm.getPublishDateVar() != null)
					
					publishChanged = true;
			}
			//Checks if Expire was updated
			if (UtilMethods.isSet(structure.getExpireDateVar()) &&
					UtilMethods.isSet(structureForm.getExpireDateVar())){
				
				if (!structure.getExpireDateVar().equals(structureForm.getExpireDateVar())){
					expireChanged = true;
				}
			}else{
				if(structure.getExpireDateVar() != null ||
						structureForm.getExpireDateVar() != null)
					
					expireChanged = true;
			}
			if(!newStructure && (publishChanged || expireChanged)){
				 List<Contentlet> results = conAPI.findByStructure(structure, user, true, 0, 0);
					  for (Contentlet con : results) {
						if( UtilMethods.isSet(structureForm.getExpireDateVar())){
							Date pub = (Date)con.getMap().get(structureForm.getPublishDateVar());
							Date exp = (Date) con.getMap().get(structureForm.getExpireDateVar());
							if(UtilMethods.isSet(pub) && UtilMethods.isSet(exp)){	
								if(exp.before(new Date())){
									throw new PublishStateException("'"+con.getTitle()+"'" + LanguageUtil.get(user, "found-expired-content-please-check-before-continue"));
								}else if(exp.before(pub)){
									throw new PublishStateException("'"+con.getTitle()+"'" + LanguageUtil.get(user, "expire-date-should-not-be-less-than-Publish-date-please-check-before-continue"));
								}else if(con.isLive()  && pub.after(new Date()) && exp.after(new Date())){
									conAPI.unpublish(con, user, true);  
								}
							} 
						}
					  }
				}
			***/
			
			
			
			BeanUtils.copyProperties(structure, structureForm);
			structure.setHost(structureForm.getHost());

			if (newStructure) {
				String structureVelocityName = VelocityUtil.convertToVelocityVariable(structure.getName(), true);
				structureVelocityName = APILocator.getContentTypeAPI(APILocator.systemUser()).suggestVelocityVar(structureVelocityName);
				structure.setVelocityVarName(structureVelocityName);
			}


			// If there is no default structure this would be it
			Structure defaultStructure = new StructureTransformer(APILocator.getContentTypeAPI(user).findDefault()).asStructure();
			if (!InodeUtils.isSet(defaultStructure.getInode())) {
				structure.setDefaultStructure(true);
			}
			if (newStructure) {
				structure.setFixed(false);
				structure.setOwner(user.getUserId());
			}

			StructureFactory.saveStructure(structure);
			
			//ContentType type = APILocator.getContentTypeAPI2().save(new StructureTransformer(structure).from(), user);
			//structureForm.setInode(type.inode());
			structureForm.setUrlMapPattern(structure.getUrlMapPattern());

			final ImmutableList.Builder<WorkflowScheme> schemes = new ImmutableList.Builder<>();
			String[] schemeIds = req.getParameterValues("workflowScheme");

			if(UtilMethods.isSet(schemeIds)) {
				for (String schemeId : schemeIds) {
					if (UtilMethods.isSet(schemeId)) {
						WorkflowScheme scheme = APILocator.getWorkflowAPI().findScheme(schemeId);
						schemes.add(scheme);
					}
				}
			}

			APILocator.getWorkflowAPI().saveSchemesForStruct(structure, schemes.build());

			/**
			 * 
			 * Moved to API

			// if the structure is a widget we need to add the base fields.
			if (newStructure && structureForm.getStructureType() == Structure.STRUCTURE_TYPE_WIDGET) {
				wAPI.createBaseWidgetFields(structure);
			}

			// if the structure is a form we need to add the base fields.
			if (newStructure && structureForm.getStructureType() == Structure.STRUCTURE_TYPE_FORM) {
				fAPI.createBaseFormFields(structure);
			}

			// if the structure is a file we need to add the base fields.
			if (newStructure && structureForm.getStructureType() == Structure.STRUCTURE_TYPE_FILEASSET) {
				APILocator.getFileAssetAPI().createBaseFileAssetFields(structure);
			}
			
			// if the structure is a page we need to add the base fields.
			if(newStructure && structureForm.getStructureType() == Structure.STRUCTURE_TYPE_HTMLPAGE) {
			    APILocator.getHTMLPageAssetAPI().createHTMLPageAssetBaseFields(structure);
			}
			
			// if the structure is a persona we need to add the base fields.
			if(newStructure && structureForm.getStructureType() == Structure.STRUCTURE_TYPE_PERSONA) {
			    APILocator.getPersonaAPI().createPersonaBaseFields(structure);
			}
			
			if (!newStructure) {
				perAPI.resetPermissionReferences(structure);
			}
			*/

			// Saving the structure in cache
			CacheLocator.getContentTypeCache().remove(structure);
			CacheLocator.getContentTypeCache().add(structure);
			new ContentTypeLoader().invalidate(structure);

			String message = "message.structure.savestructure";
			if (structure.getStructureType() == 3) {
				message = "message.form.saveform";
			}
			SessionMessages.add(req, "message", message);
		
		} catch (Exception ex) {
			Logger.error(this.getClass(), ex.toString(),ex);
			String message = ex.getMessage();
			SessionMessages.add(req, "error", message);
		}
	}

	private void _resetIntervals(ActionForm form, ActionRequest req, ActionResponse res) {
		try {
			Structure structure = (Structure) req.getAttribute(WebKeys.Structure.STRUCTURE);

			int limit = 200;
			int offset = 0;
			List<Contentlet> contents = conAPI.findByStructure(structure, _getUser(req), false, limit, offset);
			int size = contents.size();
			while (size > 0) {
				for (Contentlet cont : contents) {
					cont.setReviewInterval(structure.getReviewInterval());
				}
				offset += limit;
				contents = conAPI.findByStructure(structure, _getUser(req), false, limit, offset);
				size = contents.size();
			}
		} catch (Exception ex) {
			Logger.debug(EditStructureAction.class, ex.toString());
		}
	}

	@SuppressWarnings("deprecation")
	private void _loadForm(ActionForm form, ActionRequest req, ActionResponse res) {
		try {
			StructureForm structureForm = (StructureForm) form;
			Structure structure = (Structure) req.getAttribute(WebKeys.Structure.STRUCTURE);
			BeanUtils.copyProperties(structureForm, structure);
			structureForm.setFields(structure.getFields());

			if (structure.getReviewInterval() != null) {
				String interval = structure.getReviewInterval();
				Pattern p = Pattern.compile("(\\d+)([dmy])");
				Matcher m = p.matcher(interval);
				boolean b = m.matches();
				if (b) {
					structureForm.setReviewContent(true);
					String g1 = m.group(1);
					String g2 = m.group(2);
					structureForm.setReviewIntervalNum(g1);
					structureForm.setReviewIntervalSelect(g2);
				}
			}
			if (UtilMethods.isSet(structure.getDetailPage())) {
				Identifier ident = APILocator.getIdentifierAPI().find(structure.getDetailPage());
				HTMLPageAsset page = (HTMLPageAsset) APILocator.getVersionableAPI().findLiveVersion(ident, APILocator.getUserAPI().getSystemUser(), false);
				if (InodeUtils.isSet(page.getInode())) {
					structureForm.setDetailPage(ident.getId());
				}
			}

		} catch (Exception ex) {
			Logger.debug(EditStructureAction.class, ex.toString());
		}
	}

	private void _deleteStructure(ActionForm form, ActionRequest req, ActionResponse res) throws Exception {
	
		Structure structure = (Structure) req.getAttribute(WebKeys.Structure.STRUCTURE);
		User user = _getUser(req);
		HttpServletRequest httpReq = ((ActionRequestImpl) req).getHttpServletRequest();
		
		try {
            _checkUserPermissions(structure,user, PERMISSION_PUBLISH);
        } catch (Exception ae) {
            if (ae.getMessage().equals(WebKeys.USER_PERMISSIONS_EXCEPTION)) {
            	String message = "message.insufficient.permissions.to.delete";
            	SessionMessages.add(req, "error", message);
            	return;
            }
        }
		
		try {
		    APILocator.getStructureAPI().delete(structure, user);
		    
		    ActivityLogger.logInfo(ActivityLogger.class, "Delete Structure Action", "User " + _getUser(req).getUserId() + "/" + _getUser(req).getFirstName() + " deleted structure "
                    + structure.getName() + " Structure.", HostUtil.hostNameUtil(req, _getUser(req)));
		    
		    SessionMessages.add(req, "message", "message.structure.deletestructure");
		}
		catch(DotStateException ex) {
		    if(ex.getMessage().contains("containers")){
		        SessionMessages.add(req, "message", "message.structure.notdeletestructure.container");
		    	SessionMessages.add(req, "message", ex.getMessage());
		    } else if(ex.getMessage().contains("default"))
		        SessionMessages.add(req, "message", "message.structure.notdeletestructure");
		    
		}
		catch(DotSecurityException ex) {
		    SessionMessages.add(httpReq, "message", "message.insufficient.permissions.to.delete");
		}
	
	}

	private void _defaultStructure(ActionForm form, ActionRequest req, ActionResponse res) {
		try {

			Structure structure = (Structure) req.getAttribute(WebKeys.Structure.STRUCTURE);

			User user = _getUser(req);
			APILocator.getContentTypeAPI(user).setAsDefault(new StructureTransformer(structure).from());
			String message = "message.structure.defaultstructure";
			SessionMessages.add(req, "message", message);
		} catch (Exception ex) {
			Logger.debug(EditStructureAction.class, ex.toString());
		}

	}


}
