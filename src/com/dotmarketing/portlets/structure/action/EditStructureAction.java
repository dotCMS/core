package com.dotmarketing.portlets.structure.action;

import static com.dotmarketing.business.PermissionAPI.PERMISSION_PUBLISH;

import java.net.URLDecoder;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletConfig;
import javax.portlet.WindowState;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.PageContext;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.struts.Globals;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portal.struts.DotPortletAction;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.form.business.FormAPI;
import com.dotmarketing.portlets.htmlpages.factories.HTMLPageFactory;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.portlets.structure.factories.FieldFactory;
import com.dotmarketing.portlets.structure.factories.RelationshipFactory;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.structure.struts.StructureForm;
import com.dotmarketing.portlets.widget.business.WidgetAPI;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
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
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import com.liferay.portal.struts.ActionException;
import com.liferay.portal.util.Constants;
import com.liferay.portlet.ActionRequestImpl;
import com.liferay.util.servlet.SessionMessages;

public class EditStructureAction extends DotPortletAction {

	private ContentletAPI conAPI = APILocator.getContentletAPI();

	private WidgetAPI wAPI = APILocator.getWidgetAPI();

	private FormAPI fAPI = APILocator.getFormAPI();

	private HostAPI hostAPI = APILocator.getHostAPI();

	private FolderAPI folderAPI = APILocator.getFolderAPI();

	private PermissionAPI perAPI = APILocator.getPermissionAPI();
	private final String[] reservedStructureNames = { "Host", "Folder", "File", "HTML Page", "Menu Link", "Virtual Link", "Container", "Template", "User" };

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

		HibernateUtil.commitTransaction();
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
	private Structure _loadStructure(ActionForm form, ActionRequest req, ActionResponse res) throws ActionException, DotDataException {

		User user = _getUser(req);
		Structure structure = new Structure();
		String inodeString = req.getParameter("inode");
		if (InodeUtils.isSet(inodeString)) {
			/*
			 * long inode = Long.parseLong(inodeString); if (inode != 0) {
			 * structure = StructureFactory.getStructureByInode(inode); }
			 */

			if (InodeUtils.isSet(inodeString)) {
				structure = StructureFactory.getStructureByInode(inodeString);
			}
		}
		req.setAttribute(WebKeys.Structure.STRUCTURE, structure);

		boolean searchable = false;

		List<Field> fields = structure.getFields();
		for (Field f : fields) {
			if (f.isIndexed()) {
				searchable = true;
				break;
			}
		}

		if (!searchable && InodeUtils.isSet(structure.getInode())) {
			String message = "warning.structure.notsearchable";
			SessionMessages.add(req, "message", message);

		}

		if (structure.isFixed()) {
			String message = "warning.object.isfixed";
			SessionMessages.add(req, "message", message);
		}

		// Checking permissions
		_checkUserPermissions(structure, user, PermissionAPI.PERMISSION_READ);

		return structure;
	}

	private void _saveStructure(ActionForm form, ActionRequest req, ActionResponse res) {
		try {
			boolean newStructure = false;
			StructureForm structureForm = (StructureForm) form;
			Structure structure = (Structure) req.getAttribute(WebKeys.Structure.STRUCTURE);

			User user = _getUser(req);
			HttpServletRequest httpReq = ((ActionRequestImpl) req).getHttpServletRequest();

			if (!UtilMethods.isSet(structureForm.getHost()) && (!UtilMethods.isSet(structureForm.getFolder()) || structureForm.getFolder().equals("SYSTEM_FOLDER"))) {
				throw new DotDataException(LanguageUtil.get(user, "Host-or-folder-is-required"));
			}

			// Checking permissions
			_checkWritePermissions(structure, user, httpReq);

			// Check if another structure with the same name exist
			String auxStructureName = structureForm.getName();
			auxStructureName = (auxStructureName != null ? auxStructureName.trim() : "");

			@SuppressWarnings("deprecation")
			Structure auxStructure = StructureCache.getStructureByType(auxStructureName);

			if (InodeUtils.isSet(auxStructure.getInode()) && !auxStructure.getInode().equalsIgnoreCase(structure.getInode())) {
				throw new DotDataException(LanguageUtil.format(user.getLocale(), "structure-name-already-exist",new String[]{auxStructureName},false));
			}

			Arrays.sort(reservedStructureNames);
			if (!InodeUtils.isSet(structureForm.getInode()) && (Arrays.binarySearch(reservedStructureNames, auxStructureName) >= 0)) {
				throw new DotDataException("Invalid Reserved Structure Name : " + auxStructureName);
			}

			// Validate if is a new structure and if the name hasn't change
			if (!InodeUtils.isSet(structure.getInode())) {
				newStructure = true;
			} else {
				String structureName = structure.getName();
				String structureFormName = structureForm.getName();
				if (UtilMethods.isSet(structureName) && UtilMethods.isSet(structureFormName) && !structureName.equals(structureFormName) && !structure.isFixed()) {

					StructureCache.removeStructure(structure);

				}
			}

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
			if (UtilMethods.isSet(structureForm.getHost())) {
				if (!structureForm.getHost().equals(Host.SYSTEM_HOST) && hostAPI.findSystemHost().getIdentifier().equals(structureForm.getHost())) {
					structureForm.setHost(Host.SYSTEM_HOST);
				}
				structureForm.setFolder("SYSTEM_FOLDER");
			} else if (UtilMethods.isSet(structureForm.getFolder())) {
				structureForm.setHost(folderAPI.find(structureForm.getFolder(), user, false).getHostId());
			}

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

			BeanUtils.copyProperties(structure, structureForm);

			// if htmlpage doesn't exist page id should be an identifier. Should
			// be refactored once we get identifierAPI/HTMLPage API done
			String pageDetail = structureForm.getDetailPage();

			if (newStructure) {
				String structureVelocityName = VelocityUtil.convertToVelocityVariable(structure.getName(), true);
				List<String> velocityvarnames = StructureFactory.getAllVelocityVariablesNames();
				int found = 0;
				if (VelocityUtil.isNotAllowedVelocityVariableName(structureVelocityName)) {
					found++;
				}

				for (String velvar : velocityvarnames) {
					if (velvar != null) {
						if (structureVelocityName.equalsIgnoreCase(velvar)) {
							found++;
						} else if (velvar.toLowerCase().contains(structureVelocityName.toLowerCase())) {
							String number = velvar.substring(structureVelocityName.length());
							if (RegEX.contains(number, "^[0-9]+$")) {
								found++;
							}
						}
					}
				}
				if (found > 0) {
					structureVelocityName = structureVelocityName + Integer.toString(found);
				}
				structure.setVelocityVarName(structureVelocityName);
			}

			if (UtilMethods.isSet(pageDetail)) {
				structure.setDetailPage(pageDetail);
			}

			// Saving interval review properties
			if (structureForm.isReviewContent()) {
				structure.setReviewInterval(structureForm.getReviewIntervalNum() + structureForm.getReviewIntervalSelect());
			} else {
				structure.setReviewInterval(null);
				structure.setReviewerRole(null);
			}

			// If there is no default structure this would be
			Structure defaultStructure = StructureFactory.getDefaultStructure();
			if (!InodeUtils.isSet(defaultStructure.getInode())) {
				structure.setDefaultStructure(true);
			}
			if (newStructure) {
				structure.setFixed(false);
				structure.setOwner(user.getUserId());
			}
			// validate iit is a form structure set it as system by default
			if (structureForm.getStructureType() == Structure.STRUCTURE_TYPE_FORM) {
				structure.setSystem(true);
			}
			StructureFactory.saveStructure(structure);
			structureForm.setUrlMapPattern(structure.getUrlMapPattern());

			WorkflowScheme scheme = APILocator.getWorkflowAPI().findSchemeForStruct(structure);

			String schemeId = req.getParameter("workflowScheme");

			if (scheme != null && UtilMethods.isSet(schemeId) && !schemeId.equals(scheme.getId())) {
				scheme = APILocator.getWorkflowAPI().findScheme(schemeId);
				APILocator.getWorkflowAPI().saveSchemeForStruct(structure, scheme);
			}

			// if the structure is a widget we need to add the base fields.
			if (newStructure && structureForm.getStructureType() == Structure.STRUCTURE_TYPE_WIDGET) {
				wAPI.createBaseWidgetFields(structure);
			}

			// if the structure is a form we need to add the base fields.
			if (newStructure && structureForm.getStructureType() == Structure.STRUCTURE_TYPE_FORM) {
				fAPI.createBaseFormFields(structure);
			}

			// if the structure is a form we need to add the base fields.
			if (newStructure && structureForm.getStructureType() == Structure.STRUCTURE_TYPE_FILEASSET) {
				APILocator.getFileAssetAPI().createBaseFileAssetFields(structure);
			}
			if (!newStructure) {
				perAPI.resetPermissionReferences(structure);
			}
			System.out.println("L'host settato all'interno dellastructure è: " + structure.getHost());
			ActivityLogger.logInfo(ActivityLogger.class, "Save Structure Action", "User " + _getUser(req).getUserId() + "/" + _getUser(req).getFirstName() + " added structure "
					+ structure.getName() + ".", HostUtil.hostNameUtil(req, _getUser(req)));

			// Saving the structure in cache
			StructureCache.removeStructure(structure);
			StructureCache.addStructure(structure);
			StructureServices.removeStructureFile(structure);

			String message = "message.structure.savestructure";
			if (structure.getStructureType() == 3) {
				message = "message.form.saveform";
			}
			SessionMessages.add(req, "message", message);
			AdminLogger.log(EditStructureAction.class, "_saveStructure", "Structure saved : " + structure.getName(), user);
		} catch (Exception ex) {
			Logger.error(this.getClass(), ex.toString());
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
				HTMLPage page = HTMLPageFactory.getLiveHTMLPageByIdentifier(ident);
				if (InodeUtils.isSet(page.getInode())) {
					structureForm.setDetailPage(page.getIdentifier());
				}
			}

		} catch (Exception ex) {
			Logger.debug(EditStructureAction.class, ex.toString());
		}
	}

	private void _deleteStructure(ActionForm form, ActionRequest req, ActionResponse res) throws Exception {

		try {
			Structure structure = (Structure) req.getAttribute(WebKeys.Structure.STRUCTURE);

			User user = _getUser(req);
			HttpServletRequest httpReq = ((ActionRequestImpl) req).getHttpServletRequest();

			// Checking permissions
			_checkDeletePermissions(structure, user, httpReq);

			// checking if there is containers using this structure
			List<Container> containers = APILocator.getContainerAPI().findContainersForStructure(structure.getInode());
			if (containers.size() > 0) {
				StringBuilder names = new StringBuilder();
				for (int i = 0; i < containers.size(); i++)
					names.append(containers.get(i).getFriendlyName()).append(", ");
				Logger.warn(EditStructureAction.class, "Structure " + structure.getName() + " can't be deleted because the following containers are using it: " + names);
				SessionMessages.add(req, "message", "message.structure.notdeletestructure.container");
				return;
			}

			if (!structure.isDefaultStructure()) {

				@SuppressWarnings("rawtypes")
				List fields = FieldFactory.getFieldsByStructure(structure.getInode());

				@SuppressWarnings("rawtypes")
				Iterator fieldsIter = fields.iterator();

				while (fieldsIter.hasNext()) {
					Field field = (Field) fieldsIter.next();
					FieldFactory.deleteField(field);
				}

				int limit = 200;
				int offset = 0;
				List<Contentlet> contentlets = conAPI.findByStructure(structure, user, false, limit, offset);
				int size = contentlets.size();
				while (size > 0) {
					conAPI.delete(contentlets, user, false);
					contentlets = conAPI.findByStructure(structure, user, false, limit, offset);
					size = contentlets.size();
				}

				if (structure.getStructureType() == Structure.STRUCTURE_TYPE_FORM) {

					@SuppressWarnings({ "deprecation", "static-access" })
					Structure st = StructureCache.getStructureByName(fAPI.FORM_WIDGET_STRUCTURE_NAME_FIELD_NAME);

					if (UtilMethods.isSet(st) && UtilMethods.isSet(st.getInode())) {

						@SuppressWarnings({ "deprecation", "static-access" })
						Field field = st.getField(fAPI.FORM_WIDGET_FORM_ID_FIELD_NAME);

						List<Contentlet> widgetresults = conAPI.search("+structureInode:" + st.getInode() + " +" + field.getFieldContentlet() + ":" + structure.getInode(), 0, 0,
								"", user, false);
						if (widgetresults.size() > 0) {
							conAPI.delete(widgetresults, user, false);
						}
					}
				}

				// http://jira.dotmarketing.net/browse/DOTCMS-6435
				if (structure.getStructureType() == Structure.STRUCTURE_TYPE_FILEASSET) {
					StructureFactory.updateFolderFileAssetReferences(structure);
				}

				List<Relationship> relationships = RelationshipFactory.getRelationshipsByParent(structure);
				for (Relationship rel : relationships) {
					RelationshipFactory.deleteRelationship(rel);
				}
				relationships = RelationshipFactory.getRelationshipsByChild(structure);
				for (Relationship rel : relationships) {
					RelationshipFactory.deleteRelationship(rel);
				}

				PermissionAPI perAPI = APILocator.getPermissionAPI();
				perAPI.removePermissions(structure);

				StructureFactory.deleteStructure(structure);
				
				ActivityLogger.logInfo(ActivityLogger.class, "Delete Structure Action", "User " + _getUser(req).getUserId() + "/" + _getUser(req).getFirstName() + " deleted structure "
						+ structure.getName() + " Structure.", HostUtil.hostNameUtil(req, _getUser(req)));

				// Removing the structure from cache
				FieldsCache.removeFields(structure);
				StructureCache.removeStructure(structure);
				StructureServices.removeStructureFile(structure);

				SessionMessages.add(req, "message", "message.structure.deletestructure");
			} else {
				SessionMessages.add(req, "message", "message.structure.notdeletestructure");
			}
		} catch (Exception ex) {
			Logger.debug(EditStructureAction.class, ex.toString());
			throw ex;
		}
	}

	private void _defaultStructure(ActionForm form, ActionRequest req, ActionResponse res) {
		try {

			Structure structure = (Structure) req.getAttribute(WebKeys.Structure.STRUCTURE);

			User user = _getUser(req);
			HttpServletRequest httpReq = ((ActionRequestImpl) req).getHttpServletRequest();
			_checkWritePermissions(structure, user, httpReq);

			StructureFactory.disableDefault();
			structure.setDefaultStructure(true);
			StructureFactory.saveStructure(structure);
			String message = "message.structure.defaultstructure";
			SessionMessages.add(req, "message", message);
		} catch (Exception ex) {
			Logger.debug(EditStructureAction.class, ex.toString());
		}

	}

	private void _checkWritePermissions(Structure structure, User user, HttpServletRequest httpReq) throws Exception {
		try {
			_checkUserPermissions(structure, user, PERMISSION_PUBLISH);

		} catch (Exception ae) {
			if (ae.getMessage().equals(WebKeys.USER_PERMISSIONS_EXCEPTION)) {
				SessionMessages.add(httpReq, "message", "message.insufficient.permissions.to.save");
			}
			throw ae;
		}
	}

}
