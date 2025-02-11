/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.priv;

import java.util.List;
import java.util.Map;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.enterprise.FormAJAXProxy;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.ajax.ContentletAjax;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.form.business.FormAPI;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

public class FormAJAX {
	
	public static Map<String, Object> searchFormWidget(String formStructureInode) throws DotDataException, DotSecurityException {
		ContentletAPI conAPI = APILocator.getContentletAPI();
		
		User systemUser = APILocator.getUserAPI().getSystemUser();
		PermissionAPI pAPI;
		pAPI = APILocator.getPermissionAPI();
		List<Role> roles = pAPI.getRoles(formStructureInode, PermissionAPI.PERMISSION_READ + PermissionAPI.PERMISSION_EDIT + PermissionAPI.PERMISSION_PUBLISH, "", 0, 10, true);

		if(!InodeUtils.isSet(formStructureInode)) {
			Logger.error(FormAJAXProxy.class, "An invalid form structure inode =  \"" + formStructureInode + "\" was passed");
			throw new DotRuntimeException("A valid form structure inode need to be passed");
		}

		List<Contentlet> widgetList = conAPI.search("+structureName:" + FormAPI.FORM_WIDGET_STRUCTURE_NAME_VELOCITY_VAR_NAME + " +" + 
				FormAPI.FORM_WIDGET_STRUCTURE_NAME_VELOCITY_VAR_NAME + "." + FormAPI.FORM_WIDGET_FORM_ID_FIELD_VELOCITY_VAR_NAME + ":" + formStructureInode, 
				1, 0, null, systemUser, false);

		if(widgetList.size() == 0) {
						
			Structure formWidget = CacheLocator.getContentTypeCache().getStructureByVelocityVarName(FormAPI.FORM_WIDGET_STRUCTURE_NAME_VELOCITY_VAR_NAME);
			if(!UtilMethods.isSet(formWidget.getInode())){
				APILocator.getFormAPI().createFormWidgetInstanceStructure();
				formWidget = CacheLocator.getContentTypeCache().getStructureByVelocityVarName(FormAPI.FORM_WIDGET_STRUCTURE_NAME_VELOCITY_VAR_NAME);
			}
			ContentType formStructure = APILocator.getContentTypeAPI(systemUser).find(formStructureInode);
			String formStructureTitle = formStructure.name();
			String formTitleFieldValue = APILocator.getContentTypeFieldAPI().byContentTypeAndVar(formStructure, FormAPI.FORM_TITLE_FIELD_VELOCITY_VAR_NAME).values();
			Contentlet formInstance = new Contentlet();
			formInstance.setStructureInode(formWidget.getInode());
			formInstance.setProperty(FormAPI.FORM_WIDGET_FORM_ID_FIELD_VELOCITY_VAR_NAME, formStructureInode);
			Field codeField = formWidget.getFieldVar(FormAPI.FORM_WIDGET_CODE_VELOCITY_VAR_NAME);
			formInstance.setProperty(FormAPI.FORM_WIDGET_CODE_VELOCITY_VAR_NAME, codeField.getDefaultValue());
			formInstance.setStringProperty(FormAPI.FORM_WIDGET_TITLE_VELOCITY_VAR_NAME, (UtilMethods.isSet(formTitleFieldValue))?formTitleFieldValue:formStructureTitle);
			
			formInstance.setLanguageId(APILocator.getLanguageAPI().getDefaultLanguage().getId());			
			formInstance.setOwner(systemUser.getUserId());
			formInstance.setModUser(systemUser.getUserId());
			formInstance.setModDate(new java.util.Date());

			try {

				HibernateUtil.startTransaction();
				formInstance = conAPI.checkin(formInstance ,systemUser, true);
				/*Permission for cmsanonymous*/
				Permission p = new Permission(formInstance.getPermissionId(), APILocator.getRoleAPI().loadCMSAnonymousRole().getId(), PermissionAPI.PERMISSION_READ + PermissionAPI.PERMISSION_EDIT + PermissionAPI.PERMISSION_PUBLISH, true);
				HibernateUtil.commitTransaction();
				try{
					pAPI.save(p, formInstance, APILocator.getUserAPI().getSystemUser(), false);
				}catch(Exception e){
					Logger.error(ContentletAjax.class, "Permission with Inode" + p + " cannot be saved over this asset: " + formInstance);
				}
				if(roles.size() > 0){
					for (Role role : roles) {
						String id = role.getId();
						Permission per = new Permission(formInstance.getPermissionId(), id, PermissionAPI.PERMISSION_READ + PermissionAPI.PERMISSION_EDIT + PermissionAPI.PERMISSION_PUBLISH, true);						
						try{
							pAPI.save(per, formInstance, APILocator.getUserAPI().getSystemUser(), false);
						}catch(Exception e){
							Logger.error(ContentletAjax.class, "Permission with Inode" + per + " cannot be saved over this asset: " + formInstance);
						}
					}
				}
			}catch (DotStateException dse) {
				Logger.error(FormAJAXProxy.class, dse.getMessage(), dse);
				HibernateUtil.rollbackTransaction();
			}catch (Exception e) {
				Logger.error(FormAJAXProxy.class, e.getMessage(), e);
				HibernateUtil.rollbackTransaction();
			} finally {
				HibernateUtil.closeSessionSilently();
			}

			APILocator.getVersionableAPI().setLive(formInstance);
			APILocator.getVersionableAPI().setWorking(formInstance);
			if(!conAPI.isInodeIndexed(formInstance.getInode())){
				Logger.error(FormAJAXProxy.class, "Timed out waiting for index to return");
			}
			
			return formInstance.getMap();

		}
		if(roles.size() > 0){
			for (Role r : roles) {
				String id = r.getId();
				Permission per = new Permission(widgetList.get(0).getPermissionId(), id, PermissionAPI.PERMISSION_READ + PermissionAPI.PERMISSION_EDIT + PermissionAPI.PERMISSION_PUBLISH, true);				
				try{
					pAPI.save(per, widgetList.get(0), APILocator.getUserAPI().getSystemUser(), false);
				}catch(Exception e){
					Logger.error(ContentletAjax.class, "Permission with Inode" + per + " cannot be saved over this asset: " + widgetList.get(0));
				}
			}
		}
		return widgetList.get(0).getMap();

	}
	
}
