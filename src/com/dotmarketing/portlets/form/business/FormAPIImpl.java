package com.dotmarketing.portlets.form.business;

import java.util.ArrayList;
import java.util.List;

import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DataAccessException;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.structure.business.FieldAPI;
import com.dotmarketing.portlets.structure.factories.FieldFactory;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.services.StructureServices;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.VelocityUtil;
import com.liferay.portal.model.User;

/**
 * 
 * @author Oswaldo
 *
 */
public class FormAPIImpl implements FormAPI {

	public PermissionAPI perAPI = APILocator.getPermissionAPI();
	public ContentletAPI conAPI = APILocator.getContentletAPI();

	public void createBaseFormFields(Structure structure) throws DotDataException,DotStateException {
		if(!InodeUtils.isSet(structure.getInode())){
			throw new DotStateException("Cannot create base forms fields on a structure that doesn't exist");
		}

		Field titleField = new Field(FORM_TITLE_FIELD_NAME,Field.FieldType.HIDDEN,Field.DataType.TEXT,structure,false,false,false,1,"", "", "", true, true, true);
		titleField.setFieldContentlet(FieldAPI.ELEMENT_CONSTANT);
		FieldFactory.saveField(titleField);

		Field emailField = new Field(FORM_EMAIL_FIELD_NAME,Field.FieldType.HIDDEN,Field.DataType.TEXT,structure,false,false,false,2,"", "", "", true, true, true);
		emailField.setFieldContentlet(FieldAPI.ELEMENT_CONSTANT);
		FieldFactory.saveField(emailField);

		Field returnpageField = new Field(FORM_RETURN_PAGE_FIELD_NAME,Field.FieldType.HIDDEN,Field.DataType.TEXT,structure,false,false,false,3,"", "", "", true, true, true);
		returnpageField.setFieldContentlet(FieldAPI.ELEMENT_CONSTANT);
		FieldFactory.saveField(returnpageField);
		
		Field hostField = new Field(FORM_HOST_FIELD_NAME,Field.FieldType.HOST_OR_FOLDER,Field.DataType.TEXT,structure,false,false,true,4,"", "", "", true, true, true);
		FieldFactory.saveField(hostField);

		FieldsCache.clearCache();
	}

	public List<Structure> findAll(User user, boolean respectFrontEndPermissions) throws DotDataException, DotSecurityException {
		List<Structure> sts = StructureFactory.getStructures();
		List<Structure> forms = new ArrayList<Structure>();
		for (Structure structure : sts) {
			if(structure.getStructureType() == Structure.STRUCTURE_TYPE_FORM){
				forms.add(structure);
			}
		}
		forms = perAPI.filterCollection(forms, PermissionAPI.PERMISSION_READ, respectFrontEndPermissions, user);
		return forms;
	}

	public void createFormWidgetInstanceStructure() throws DotDataException,DotStateException{

		//try {
		User user = APILocator.getUserAPI().getSystemUser();
		Structure structure = new Structure();
		structure.setName(FORM_WIDGET_STRUCTURE_NAME_FIELD_NAME);
		structure.setDescription(FORM_WIDGET_STRUCTURE_NAME_FIELD_NAME);
		structure.setStructureType(Structure.STRUCTURE_TYPE_WIDGET);
		structure.setVelocityVarName(VelocityUtil.convertToVelocityVariable(FORM_WIDGET_STRUCTURE_NAME_FIELD_NAME));
		structure.setSystem(true);
		structure.setDefaultStructure(false);
		structure.setFixed(false);
		structure.setReviewInterval(null);
		structure.setOwner(user.getUserId());
		StructureFactory.saveStructure(structure);



		/*Saving Structure Permission*/
		perAPI.setDefaultCMSAdminPermissions(structure);

		User systemUser = APILocator.getUserAPI().getSystemUser();
		
		/*Permission for cmsanonymous*/
		Permission p = new Permission(structure.getPermissionId(), APILocator.getRoleAPI().loadCMSAnonymousRole().getId(), PermissionAPI.PERMISSION_READ + PermissionAPI.PERMISSION_EDIT + PermissionAPI.PERMISSION_PUBLISH, true);		
		try{
			perAPI.save(p, structure, systemUser, false);
		}catch(Exception e){
			Logger.debug(FormAPIImpl.class, "Permission with Inode" + p + " cannot be saved over this asset: " + structure);
		}
				
		/*Permission for form editor*/		
		p = new Permission();
		p.setRoleId(APILocator.getRoleAPI().loadRoleByKey("Form Editor").getId());
		p.setPermission(PermissionAPI.PERMISSION_READ);
		p.setInode(structure.getPermissionId());
		try {
			perAPI.save(p, structure, systemUser, false);
		} catch (DataAccessException e) {
			Logger.debug(FormAPIImpl.class, "Permission with Inode" + p + " cannot be saved over this asset: " + structure);
		} catch (DotSecurityException e) {
			Logger.debug(FormAPIImpl.class, "Permission with Inode" + p + " cannot be saved over this asset: " + structure);
		}
		
		p = new Permission();
		p.setRoleId(APILocator.getRoleAPI().loadRoleByKey("Form Editor").getId());
		p.setPermission(PermissionAPI.PERMISSION_WRITE);
		p.setInode(structure.getPermissionId());
		try {
			perAPI.save(p, structure, systemUser, false);
		} catch (DataAccessException e) {
			Logger.debug(FormAPIImpl.class, "Permission with Inode" + p + " cannot be saved over this asset: " + structure);
		} catch (DotSecurityException e) {
			Logger.debug(FormAPIImpl.class, "Permission with Inode" + p + " cannot be saved over this asset: " + structure);
		}
		
		p = new Permission();
		p.setRoleId(APILocator.getRoleAPI().loadRoleByKey("Form Editor").getId());
		p.setPermission(PermissionAPI.PERMISSION_PUBLISH);
		p.setInode(structure.getPermissionId());
		try {
			perAPI.save(p, structure, systemUser, false);
		} catch (DataAccessException e) {
			Logger.debug(FormAPIImpl.class, "Permission with Inode" + p + " cannot be saved over this asset: " + structure);
		} catch (DotSecurityException e) {
			Logger.debug(FormAPIImpl.class, "Permission with Inode" + p + " cannot be saved over this asset: " + structure);
		}
		
		/*Saving the structure in cache*/
		StructureCache.removeStructure(structure);
		StructureCache.addStructure(structure);
		StructureServices.removeStructureFile(structure);

		/*Adding Widget Fields*/
		Field formIdField = new Field(FORM_WIDGET_FORM_ID_FIELD_NAME,Field.FieldType.TEXT,Field.DataType.TEXT,structure,true,true,true,1,"", "", "", true, false, true);
		FieldFactory.saveField(formIdField);

		Field codeField = new Field(FORM_WIDGET_CODE_FIELD_NAME,Field.FieldType.TEXT_AREA,Field.DataType.TEXT,structure,true,false,false,3,"", "", "", true, false, true);
		codeField.setDefaultValue("#submitContent(\"${formId}\")");
		FieldFactory.saveField(codeField);
		FieldsCache.clearCache();

	}

}
