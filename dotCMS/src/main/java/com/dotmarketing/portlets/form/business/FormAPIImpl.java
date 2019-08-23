package com.dotmarketing.portlets.form.business;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.rendering.velocity.services.ContentTypeLoader;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.*;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.ajax.ContentletAjax;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicyProvider;
import com.dotmarketing.portlets.structure.business.FieldAPI;
import com.dotmarketing.portlets.structure.factories.FieldFactory;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.StringUtils;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author Oswaldo
 *
 */
public class FormAPIImpl implements FormAPI {

	public final PermissionAPI perAPI;
	public final ContentletAPI conAPI;
	public final UserAPI userAPI;

	public FormAPIImpl () {
		this(APILocator.getPermissionAPI(), APILocator.getContentletAPI(), APILocator.getUserAPI());
	}

	@VisibleForTesting
	public FormAPIImpl (final PermissionAPI perAPI, final ContentletAPI conAPI, final UserAPI userAPI) {
		this.perAPI = perAPI;
		this.conAPI = conAPI;
		this.userAPI = userAPI;
	}

	@WrapInTransaction
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

	@CloseDBIfOpened
	public List<Structure> findAll(User user, boolean respectFrontEndPermissions) throws DotDataException, DotSecurityException {
        List<Structure> sts = StructureFactory.getAllStructuresByType(Structure.STRUCTURE_TYPE_FORM);
        List<Structure> forms = new ArrayList<Structure>();
        for (Structure structure : sts) {
            if (perAPI.doesUserHavePermission(structure, PermissionAPI.PERMISSION_READ, user, respectFrontEndPermissions)) {
                forms.add(structure);
            }
        }
        return forms;
	}

	@WrapInTransaction
	public void createFormWidgetInstanceStructure() throws DotDataException,DotStateException{

		//try {
		User user = APILocator.getUserAPI().getSystemUser();
		Structure structure = new Structure();
		structure.setName(FORM_WIDGET_STRUCTURE_NAME_FIELD_NAME);
		structure.setDescription(FORM_WIDGET_STRUCTURE_NAME_FIELD_NAME);
		structure.setStructureType(Structure.STRUCTURE_TYPE_WIDGET);
		structure.setVelocityVarName(StringUtils.camelCaseLower(FORM_WIDGET_STRUCTURE_NAME_FIELD_NAME));
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
		CacheLocator.getContentTypeCache().remove(structure);
		CacheLocator.getContentTypeCache().add(structure);
		new ContentTypeLoader().invalidate(structure);

		/*Adding Widget Fields*/
		Field formIdField = new Field(FORM_WIDGET_FORM_ID_FIELD_NAME,Field.FieldType.TEXT,Field.DataType.TEXT,structure,true,true,true,1,"", "", "", true, false, true);
		FieldFactory.saveField(formIdField);

		Field codeField = new Field(FORM_WIDGET_CODE_FIELD_NAME,Field.FieldType.TEXT_AREA,Field.DataType.TEXT,structure,true,false,false,3,"", "", "", true, false, true);
		codeField.setDefaultValue("#submitContent(\"${formId}\")");
		FieldFactory.saveField(codeField);
		FieldsCache.clearCache();

	}

	@Override
	public Contentlet getFormContent(final String formId) {
		try {
			final User systemUser = userAPI.getSystemUser();
			final String luceneQuery = "+structureName:" + FormAPI.FORM_WIDGET_STRUCTURE_NAME_VELOCITY_VAR_NAME + " +" +
					FormAPI.FORM_WIDGET_STRUCTURE_NAME_VELOCITY_VAR_NAME + "." + FormAPI.FORM_WIDGET_FORM_ID_FIELD_VELOCITY_VAR_NAME + ":" + formId;

			final List<Contentlet> listContentlet = conAPI.search(luceneQuery, 1, 0, null,
					systemUser, false);

			return listContentlet.isEmpty() ? null : listContentlet.get(0);
		} catch(final DotDataException | DotSecurityException e) {
			throw new DotRuntimeException(e);
		}
	}

	@Override
	@WrapInTransaction
	public Contentlet createDefaultFormContent(final String formId) throws DotDataException {
		final User systemUser = userAPI.getSystemUser();


		Structure formWidget = CacheLocator.getContentTypeCache().getStructureByVelocityVarName(FormAPI.FORM_WIDGET_STRUCTURE_NAME_VELOCITY_VAR_NAME);
		if (!UtilMethods.isSet(formWidget.getInode())) {
			APILocator.getFormAPI().createFormWidgetInstanceStructure();
			formWidget = CacheLocator.getContentTypeCache().getStructureByVelocityVarName(FormAPI.FORM_WIDGET_STRUCTURE_NAME_VELOCITY_VAR_NAME);
		}

		try {
			final List<Role> roles = perAPI.getRoles(formId, PermissionAPI.PERMISSION_READ + PermissionAPI.PERMISSION_EDIT + PermissionAPI.PERMISSION_PUBLISH, "", 0, 10, true);

			final ContentType formStructure = APILocator.getContentTypeAPI(systemUser).find(formId);
			final String formStructureTitle = formStructure.name();
			final String formTitleFieldValue = formStructureTitle;
			Contentlet formInstance = new Contentlet();
			formInstance.setStructureInode(formWidget.getInode());
			formInstance.setProperty(FormAPI.FORM_WIDGET_FORM_ID_FIELD_VELOCITY_VAR_NAME, formId);
			final Field codeField = formWidget.getFieldVar(FormAPI.FORM_WIDGET_CODE_VELOCITY_VAR_NAME);
			formInstance.setProperty(FormAPI.FORM_WIDGET_CODE_VELOCITY_VAR_NAME, codeField.getDefaultValue());
			formInstance.setStringProperty(FormAPI.FORM_WIDGET_TITLE_VELOCITY_VAR_NAME, (UtilMethods.isSet(formTitleFieldValue)) ? formTitleFieldValue : formStructureTitle);

			formInstance.setLanguageId(APILocator.getLanguageAPI().getDefaultLanguage().getId());
			formInstance.setOwner(systemUser.getUserId());
			formInstance.setModUser(systemUser.getUserId());
			formInstance.setModDate(new java.util.Date());
			formInstance.setIndexPolicy(IndexPolicyProvider.getInstance().forSingleContent());

			HibernateUtil.startTransaction();
			formInstance = conAPI.checkin(formInstance, systemUser, true);
				/*Permission for cmsanonymous*/
			final Permission p = new Permission(formInstance.getPermissionId(), APILocator.getRoleAPI().loadCMSAnonymousRole().getId(), PermissionAPI.PERMISSION_READ + PermissionAPI.PERMISSION_EDIT + PermissionAPI.PERMISSION_PUBLISH, true);
			HibernateUtil.commitTransaction();
			try {
				perAPI.save(p, formInstance, APILocator.getUserAPI().getSystemUser(), false);
			} catch (Exception e) {
				Logger.error(ContentletAjax.class, "Permission with Inode" + p + " cannot be saved over this asset: " + formInstance);
			}

			if (roles.size() > 0) {
				for (final Role role : roles) {
					final String id = role.getId();
					final Permission per = new Permission(formInstance.getPermissionId(), id, PermissionAPI.PERMISSION_READ + PermissionAPI.PERMISSION_EDIT + PermissionAPI.PERMISSION_PUBLISH, true);
					try {
						perAPI.save(per, formInstance, APILocator.getUserAPI().getSystemUser(), false);
					} catch (Exception e) {
						Logger.error(ContentletAjax.class, "Permission with Inode" + per + " cannot be saved over this asset: " + formInstance);
					}
				}
			}


			APILocator.getVersionableAPI().setLive(formInstance);
			APILocator.getVersionableAPI().setWorking(formInstance);

			return formInstance;
		} catch (DotSecurityException e) {
			throw new DotRuntimeException(e);
		}

	}

}
