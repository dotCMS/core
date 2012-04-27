package com.dotmarketing.portlets.structure.action;

import static com.dotmarketing.business.PermissionAPI.PERMISSION_PUBLISH;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.cache.VirtualLinksCache;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portal.struts.DotPortletAction;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.structure.business.FieldAPI;
import com.dotmarketing.portlets.structure.factories.FieldFactory;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.structure.struts.FieldForm;
import com.dotmarketing.services.ContentletMapServices;
import com.dotmarketing.services.ContentletServices;
import com.dotmarketing.services.StructureServices;
import com.dotmarketing.util.ActivityLogger;
import com.dotmarketing.util.AdminLogger;
import com.dotmarketing.util.HostUtil;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.RegEX;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.Validator;
import com.dotmarketing.util.VelocityUtil;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import com.liferay.portal.util.Constants;
import com.liferay.portlet.ActionRequestImpl;
import com.liferay.util.StringUtil;
import com.liferay.util.servlet.SessionMessages;

public class EditFieldAction extends DotPortletAction {

	private CategoryAPI categoryAPI = APILocator.getCategoryAPI();
	private ContentletAPI conAPI = APILocator.getContentletAPI();
	private FieldAPI fAPI = APILocator.getFieldAPI();

	public CategoryAPI getCategoryAPI() {
		return categoryAPI;
	}

	public void setCategoryAPI(CategoryAPI categoryAPI) {
		this.categoryAPI = categoryAPI;
	}

	public void processAction(ActionMapping mapping, ActionForm form, PortletConfig config, ActionRequest req,
			ActionResponse res) throws Exception {

		User user = _getUser(req);

		String cmd = req.getParameter(Constants.CMD);
		String referer = req.getParameter("referer");

		if ((referer != null) && (referer.length() != 0)) {
			referer = URLDecoder.decode(referer, "UTF-8");
		}

		// Retrieve the field in the request
		if ((cmd == null) || !cmd.equals("reorder")) {
			_retrieveField(form, req, res);
		}
		HibernateUtil.startTransaction();

		/*
		 * saving the field
		 */
		if ((cmd != null) && cmd.equals(Constants.ADD)) {
			try {
				Logger.debug(this, "Calling Add/Edit Method");

				Field field = (Field) req.getAttribute(WebKeys.Field.FIELD);
				if (InodeUtils.isSet(field.getInode())) {
					if (field.isFixed()
							|| (field.getFieldType().equals(Field.FieldType.LINE_DIVIDER.toString())
									|| field.getFieldType().equals(Field.FieldType.TAB_DIVIDER.toString())
									|| field.getFieldType().equals(Field.FieldType.CATEGORIES_TAB.toString())
									|| field.getFieldType().equals(Field.FieldType.PERMISSIONS_TAB.toString())
									|| field.getFieldType().equals(Field.FieldType.RELATIONSHIPS_TAB.toString())
									|| field.getFieldContentlet().equals(FieldAPI.ELEMENT_CONSTANT) || field
									.getFieldType().equals(Field.FieldType.HIDDEN.toString()))) {
						FieldForm fieldForm = (FieldForm) form;
						field.setFieldName(fieldForm.getFieldName());

						// This is what you can change on a fixed field
						if (field.isFixed()) {
							field.setHint(fieldForm.getHint());
							field.setDefaultValue(fieldForm.getDefaultValue());
							field.setSearchable(fieldForm.isSearchable());
							field.setListed(fieldForm.isListed());
							// field.setFieldName(fieldForm.getFieldName());
						}

						Structure structure = StructureCache.getStructureByInode(field.getStructureInode());

						if (((structure.getStructureType() == Structure.STRUCTURE_TYPE_CONTENT) && !fAPI
								.isElementConstant(field))
								|| ((structure.getStructureType() == Structure.STRUCTURE_TYPE_WIDGET) && fAPI
										.isElementConstant(field))
								|| ((structure.getStructureType() == Structure.STRUCTURE_TYPE_FILEASSET) && fAPI
										.isElementConstant(field))
								|| ((structure.getStructureType() == Structure.STRUCTURE_TYPE_FORM) && fAPI
										.isElementConstant(field))) {
							field.setValues(fieldForm.getValues());
						}
						BeanUtils.copyProperties(fieldForm, field);
					}
				}
				if (Validator.validate(req, form, mapping)) {
					if (_saveField(form, req, res, user)) {
						_sendToReferral(req, res, referer);
						return;
					}
				}

			} catch (Exception ae) {
				_handleException(ae, req);
				return;
			}

		}
		/*
		 * If we are deleting the field, run the delete action and return to the
		 * list
		 *
		 */
		else if ((cmd != null) && cmd.equals(Constants.DELETE)) {
			try {
				Logger.debug(this, "Calling Delete Method");
				_deleteField(form, req, res);
			} catch (Exception ae) {
				_handleException(ae, req);
				return;
			}
			_sendToReferral(req, res, referer);
		} else if ((cmd != null) && cmd.equals("reorder")) {
			try {
				Logger.debug(this, "Calling reorder Method");
				_reorderFields(form, req, res);
			} catch (Exception ae) {
				_handleException(ae, req);
				return;
			}
			_sendToReferral(req, res, referer);
		}
		HibernateUtil.commitTransaction();
		_loadForm(form, req, res);
		setForward(req, "portlet.ext.structure.edit_field");
	}

	private void _retrieveField(ActionForm form, ActionRequest req, ActionResponse res) {
		Field field = new Field();
		String inodeString = req.getParameter("inode");
		if (InodeUtils.isSet(inodeString)) {
			/*
			 * long inode = Long.parseLong(inodeString); if (inode != 0) { field =
			 * FieldFactory.getFieldByInode(inode); }
			 */
			if (InodeUtils.isSet(inodeString)) {
				field = FieldFactory.getFieldByInode(inodeString);
			} else {
				String structureInode = req.getParameter("structureInode");
				field.setStructureInode(structureInode);
			}
		} else {
			String structureInode = req.getParameter("structureInode");
			field.setStructureInode(structureInode);
		}

		if (field.isFixed()) {

			String message = "warning.object.isfixed";
			SessionMessages.add(req, "message", message);

		}

		req.setAttribute(WebKeys.Field.FIELD, field);
	}

	private boolean _saveField(ActionForm form, ActionRequest req, ActionResponse res, User user) {
		try {
			FieldForm fieldForm = (FieldForm) form;
			Field field = (Field) req.getAttribute(WebKeys.Field.FIELD);
			Structure structure = StructureFactory.getStructureByInode(field.getStructureInode());
			boolean isNew = false;
			boolean wasIndexed = field.isIndexed();


			//http://jira.dotmarketing.net/browse/DOTCMS-5918
			HttpServletRequest httpReq = ((ActionRequestImpl) req).getHttpServletRequest();
			try {
			    _checkUserPermissions(structure, user, PERMISSION_PUBLISH);
			} catch (Exception ae) {
				if (ae.getMessage().equals(WebKeys.USER_PERMISSIONS_EXCEPTION)) {
					SessionMessages.add(httpReq, "message", "message.insufficient.permissions.to.save");
				}
				throw ae;
			}



			String dataType = fieldForm.getDataType();

			if (fieldForm.isSearchable()) {
				fieldForm.setIndexed(true);
			}

			if (fieldForm.isUnique()) {
				fieldForm.setRequired(true);
				fieldForm.setIndexed(true);
			}

			BeanUtils.copyProperties(field, fieldForm);


			//To validate values entered for decimal/number type check box field
			//http://jira.dotmarketing.net/browse/DOTCMS-5516
			if (field.getFieldType().equals(Field.FieldType.CHECKBOX.toString())&&
					 (dataType.equals(Field.DataType.FLOAT.toString())||dataType.equals(Field.DataType.INTEGER.toString()))) {
                		String values = fieldForm.getValues();
	                  String temp = values.replaceAll("\r\n","|");
        	            String[] tempVals = StringUtil.split(temp.trim(), "|");
	                  for(int i=0;i<tempVals.length;i++){
        	        	  try {
					Float.parseFloat(tempVals[i]);
				  }catch (Exception e) {
			          String message = "message.structure.invaliddata";
				    SessionMessages.add(req, "error", message);
				    return false;
				 }
                       }
                  }

			// check if is a new field to add at the botton of the structure
			// field list
			if (!InodeUtils.isSet(fieldForm.getInode())) {
				isNew = true;
				int sortOrder = 0;
				List<Field> fields = FieldsCache.getFieldsByStructureInode(structure.getInode());
				for (Field f : fields) {
					// http://jira.dotmarketing.net/browse/DOTCMS-3232
					if (f.getFieldType().equalsIgnoreCase(fieldForm.getFieldType())
							&& f.getFieldType().equalsIgnoreCase(Field.FieldType.HOST_OR_FOLDER.toString())) {
						SessionMessages.add(req, "error", "message.structure.duplicate.host_or_folder.field");
						return false;
					}
					if (f.getSortOrder() > sortOrder)
						sortOrder = f.getSortOrder();
				}
				field.setSortOrder(sortOrder + 1);
				field.setFixed(false);
				field.setReadOnly(false);

				String fieldVelocityName = VelocityUtil.convertToVelocityVariable(fieldForm.getFieldName(), false);
				int found = 0;
				if (VelocityUtil.isNotAllowedVelocityVariableName(fieldVelocityName)) {
					found++;
				}

				String velvar;
				for (Field f : fields) {
					velvar = f.getVelocityVarName();
					if (velvar != null) {
						if (fieldVelocityName.equals(velvar)) {
							found++;
						} else if (velvar.contains(fieldVelocityName)) {
							String number = velvar.substring(fieldVelocityName.length());
							if (RegEX.contains(number, "^[0-9]+$")) {
								found++;
							}
						}
					}
				}
				if (found > 0) {
					fieldVelocityName = fieldVelocityName + Integer.toString(found);
				}

				//http://jira.dotmarketing.net/browse/DOTCMS-5616
				if(!validateInternalFieldVelocityVarName(fieldVelocityName)){
					fieldVelocityName+="1";
				}

				field.setVelocityVarName(fieldVelocityName);
			}

			if (!field.isFixed() && !field.isReadOnly()) {
				// gets the data type from the contentlet: bool, date, text, etc

				String prevDataType = (field.getFieldContentlet() != null) ? field.getFieldContentlet().replaceAll(
						"[0-9]*", "") : "";

				if (field.getFieldType().equals("categories_tab") || field.getFieldType().equals("permissions_tab")
						|| field.getFieldType().equals("relationships_tab")) {

					List<Field> structureFields = FieldsCache.getFieldsByStructureInode(structure.getInode());
					for (Field f : structureFields) {
						if (f.getFieldType().equals("categories_tab") && field.getFieldType().equals("categories_tab")
								&& !f.getInode().equals(field.getInode())) {
							String message = "message.structure.duplicate.categories_tab";
							SessionMessages.add(req, "error", message);
							return false;

						} else if (f.getFieldType().equals("permissions_tab")
								&& field.getFieldType().equals("permissions_tab")
								&& !f.getInode().equals(field.getInode())) {
							String message = "message.structure.duplicate.permissions_tab";
							SessionMessages.add(req, "error", message);
							return false;

						} else if (f.getFieldType().equals("relationships_tab")
								&& field.getFieldType().equals("relationships_tab")
								&& !f.getInode().equals(field.getInode())) {
							String message = "message.structure.duplicate.relationships_tab";
							SessionMessages.add(req, "error", message);
							return false;

						}
					}

				}

				if (!(field.getFieldType().equals("host or folder") || field.getFieldType().equals("line_divider") || field.getFieldType().equals("tab_divider")
						|| field.getFieldType().equals("categories_tab")
						|| field.getFieldType().equals("permissions_tab") || field.getFieldType().equals(
						"relationships_tab"))
						&& !UtilMethods.isSet(fieldForm.getDataType())) {
					// it's either an image, file or link so there is no
					// datatype
					field.setFieldContentlet("");
				}

				if (!UtilMethods.isSet(fieldForm.getDataType())) {
					// it's either an image, file or link so there is no
					// datatype
					if (!field.getFieldType().equals("host or folder")){
							field.setFieldContentlet("");
						}

				} else if (!prevDataType.equals(fieldForm.getDataType())) {
					String fieldContentlet = FieldFactory.getNextAvaliableFieldNumber(dataType, field.getInode(), field
							.getStructureInode());
					if (fieldContentlet == null) {
						// didn't find any empty ones, so im throwing an error
						// to the user to select a new one
						String message = "message.structure.nodatatype";
						SessionMessages.add(req, "error", message);
						return false;
					}
					field.setFieldContentlet(fieldContentlet);
				}

				if (field.getFieldType().equalsIgnoreCase(Field.FieldType.CATEGORY.toString())) {
					field.setValues(req.getParameter("categories"));
					field.setIndexed(true);
					if (UtilMethods.isSet(fieldForm.getDefaultValue())) {
						List<Category> selectedCategoriesList = new ArrayList<Category>();
						String[] selectedCategories = fieldForm.getDefaultValue().trim().split("\\|");
						for (String cat : selectedCategories) {
							selectedCategoriesList.add(categoryAPI.findByName(cat, user, false));
						}
						Category category = categoryAPI.find(req.getParameter("categories"), user, false);
						List<Category> childrenCategories = categoryAPI.getChildren(category, user, false);
						if (!childrenCategories.containsAll(selectedCategoriesList)) {
							String message = "error.invalid.child.category";
							SessionMessages.add(req, "error", message);
							return false;
						}
					}

				}

				if (field.getFieldType().equalsIgnoreCase(Field.FieldType.TAG.toString()) || field.isSearchable()) {
					field.setIndexed(true);
				}
			}

			if (fieldForm.getElement().equals(FieldAPI.ELEMENT_CONSTANT) || fieldForm.getFieldType().equals(FieldAPI.ELEMENT_CONSTANT)) {
				field.setFieldContentlet(FieldAPI.ELEMENT_CONSTANT);
				field.setValues(fieldForm.getValues());
			}


			// saves this field
			FieldFactory.saveField(field);
			
			ActivityLogger.logInfo(ActivityLogger.class, "Save Field Action", "User " + _getUser(req).getUserId() + "/" + _getUser(req).getFirstName() + " added field " + field.getFieldName() + " to " + structure.getName()
				    + " Structure.", HostUtil.hostNameUtil(req, _getUser(req)));

			FieldsCache.removeFields(structure);
			StructureCache.removeStructure(structure);
			StructureServices.removeStructureFile(structure);
			StructureFactory.saveStructure(structure);

			FieldsCache.addFields(structure, structure.getFields());

			//Refreshing permissions
			PermissionAPI perAPI = APILocator.getPermissionAPI();
			if(field.getFieldType().equals("host or folder")) {
				perAPI.resetChildrenPermissionReferences(structure);
			}

		    //http://jira.dotmarketing.net/browse/DOTCMS-5178
			if(!isNew && ((!wasIndexed && fieldForm.isIndexed()) || (wasIndexed && !fieldForm.isIndexed()))){
			  // rebuild contentlets indexes
			  conAPI.reindex(structure);
			}

			if (fAPI.isElementConstant(field)) {
				ContentletServices.removeContentletFile(structure);
				ContentletMapServices.removeContentletMapFile(structure);
				conAPI.refresh(structure);
			}

			String message = "message.structure.savefield";
			SessionMessages.add(req, "message", message);
			AdminLogger.log(EditFieldAction.class, "_saveField","Added field " + field.getFieldName() + " to " + structure.getName() + " Structure.", user);
			return true;
		} catch (Exception ex) {
			Logger.debug(EditFieldAction.class, ex.toString());
		}
		return false;
	}

	private void _loadForm(ActionForm form, ActionRequest req, ActionResponse res) {
		try {
			FieldForm fieldForm = (FieldForm) form;
			Field field = (Field) req.getAttribute(WebKeys.Field.FIELD);

			String structureInode = field.getStructureInode();
			structureInode = (InodeUtils.isSet(structureInode) ? structureInode : req.getParameter("structureInode"));

			field.setStructureInode(structureInode);
			BeanUtils.copyProperties(fieldForm, field);

			if (fAPI.isElementDivider(field)) {
				fieldForm.setElement(FieldAPI.ELEMENT_DIVIDER);
			} else if (fAPI.isElementdotCMSTab(field)) {
				fieldForm.setElement(FieldAPI.ELEMENT_TAB);
			} else if (fAPI.isElementConstant(field)) {
				fieldForm.setElement(FieldAPI.ELEMENT_CONSTANT);
			} else {
				fieldForm.setElement(FieldAPI.ELEMENT_FIELD);
			}

			List<String> values = new ArrayList<String>();
			List<String> names = new ArrayList<String>();
			fieldForm.setDataType(field.getFieldContentlet().replaceAll("[0-9]*", ""));
			fieldForm.setFreeContentletFieldsValue(values);
			fieldForm.setFreeContentletFieldsName(names);
		} catch (Exception ex) {
			Logger.debug(EditFieldAction.class, ex.toString());
		}
	}

	private void _deleteField(ActionForm form, ActionRequest req, ActionResponse res) {
		Field field = (Field) req.getAttribute(WebKeys.Field.FIELD);
		Structure structure = StructureFactory.getStructureByInode(field.getStructureInode());
		// clean contentlet field in db
		try {
			String type = field.getFieldType();
			if (!fAPI.isElementConstant(field) && !Field.FieldType.LINE_DIVIDER.toString().equals(type)
					&& !Field.FieldType.TAB_DIVIDER.toString().equals(type)
					&& !Field.FieldType.RELATIONSHIPS_TAB.toString().equals(type)
					&& !Field.FieldType.CATEGORIES_TAB.toString().equals(type)
					&& !Field.FieldType.PERMISSIONS_TAB.toString().equals(type)
					&& !Field.FieldType.HOST_OR_FOLDER.toString().equals(type)) {
				conAPI.cleanField(structure, field, APILocator.getUserAPI().getSystemUser(), false);
			}
			FieldFactory.deleteField(field);
			// Call the commit method to avoid a deadlock
			HibernateUtil.commitTransaction();
			FieldsCache.removeFields(structure);
			
			ActivityLogger.logInfo(ActivityLogger.class, "Delete Field Action", "User " + _getUser(req).getUserId() + "/" + _getUser(req).getFirstName() + " deleted field " + field.getFieldName() + " to " + structure.getName()
				    + " Structure.", HostUtil.hostNameUtil(req, _getUser(req)));
			
			StructureCache.removeStructure(structure);
			StructureServices.removeStructureFile(structure);

			//Refreshing permissions
			PermissionAPI perAPI = APILocator.getPermissionAPI();
			if(field.getFieldType().equals("host or folder")) {
				conAPI.cleanHostField(structure, APILocator.getUserAPI().getSystemUser(), false);
				perAPI.resetChildrenPermissionReferences(structure);
			}

			// rebuild contentlets indexes
			conAPI.reindex(structure);
			// remove the file from the cache
			ContentletServices.removeContentletFile(structure);
			ContentletMapServices.removeContentletMapFile(structure);
			String message = "message.structure.deletefield";
			SessionMessages.add(req, "message", message);
		} catch (Exception e) {
			Logger.error(EditFieldAction.class, "Falied trying to delete field.", e);
			String message = "message.structure.deletefield.error";
			SessionMessages.add(req, "error", message);
		}

	}

	private void _reorderFields(ActionForm form, ActionRequest req, ActionResponse res) {
		try {
			Enumeration enumeration = req.getParameterNames();
			while (enumeration.hasMoreElements()) {
				String parameterName = (String) enumeration.nextElement();
				if (parameterName.indexOf("order_") != -1) {
					String parameterValue = req.getParameter(parameterName);
					String fieldInode = parameterName.substring(parameterName.indexOf("_") + 1);
					Field field = FieldFactory.getFieldByInode(fieldInode);
					field.setSortOrder(Integer.parseInt(parameterValue));
					FieldFactory.saveField(field);

				}
			}
			FieldsCache.clearCache();
			//VirtualLinksCache.clearCache();
			String message = "message.structure.reorderfield";
			SessionMessages.add(req, "message", message);
			
			//AdminLogger.log(EditFieldAction.class, "_saveField", "Added field " + field.getFieldName() + " to " + structure.getName() + " Structure.", user);
		    
		    
		} catch (Exception ex) {
			Logger.error(EditFieldAction.class, ex.toString());
		}
	}

	private boolean validateInternalFieldVelocityVarName(String fieldVelVarName){

	    if(fieldVelVarName.equals(Contentlet.INODE_KEY)||
	    		fieldVelVarName.equals(Contentlet.LANGUAGEID_KEY)||
	    		fieldVelVarName.equals(Contentlet.STRUCTURE_INODE_KEY)||
	    		fieldVelVarName.equals(Contentlet.LAST_REVIEW_KEY)||
	    		fieldVelVarName.equals(Contentlet.NEXT_REVIEW_KEY)||
	    		fieldVelVarName.equals(Contentlet.REVIEW_INTERNAL_KEY)||
	    		fieldVelVarName.equals(Contentlet.DISABLED_WYSIWYG_KEY)||
	    		fieldVelVarName.equals(Contentlet.LOCKED_KEY)||
	    		fieldVelVarName.equals(Contentlet.ARCHIVED_KEY)||
	    		fieldVelVarName.equals(Contentlet.LIVE_KEY)||
	    		fieldVelVarName.equals(Contentlet.WORKING_KEY)||
	    		fieldVelVarName.equals(Contentlet.MOD_DATE_KEY)||
	    		fieldVelVarName.equals(Contentlet.MOD_USER_KEY)||
	    		fieldVelVarName.equals(Contentlet.OWNER_KEY)||
	    		fieldVelVarName.equals(Contentlet.IDENTIFIER_KEY)||
	    		fieldVelVarName.equals(Contentlet.SORT_ORDER_KEY)||
	    		fieldVelVarName.equals(Contentlet.HOST_KEY)||
	    		fieldVelVarName.equals(Contentlet.FOLDER_KEY)){
	    	return false;
	    }

	    return true;

	}
	
	public String hostNameUtil(ActionRequest req) {

		ActionRequestImpl reqImpl = (ActionRequestImpl) req;
		HttpServletRequest httpReq = reqImpl.getHttpServletRequest();
		HttpSession session = httpReq.getSession();

		String hostId = (String) session.getAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID);

		Host h = null;
		try {
			h = APILocator.getHostAPI().find(hostId, _getUser(req), false);
		} catch (DotDataException e) {
			_handleException(e, req);
		} catch (DotSecurityException e) {
			_handleException(e, req);
		}

		return h.getTitle()!=null?h.getTitle():"default";

	}

}
