package com.dotmarketing.portlets.form.business;

import java.util.List;

import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.structure.model.Structure;
import com.liferay.portal.model.User;

/**
 * 
 * @author Oswaldo
 *
 */
public interface FormAPI {

		public static final String FORM_RETURN_PAGE_FIELD_NAME = "Form Return Page";
		public static final String FORM_EMAIL_FIELD_NAME = "Form Email";
		public static final String FORM_TITLE_FIELD_NAME = "Form Title";
		public static final String FORM_HOST_FIELD_NAME = "Form Host";
		
		public static final String FORM_WIDGET_CODE_FIELD_NAME = "Widget Code";
		public static final String FORM_WIDGET_CODE_VELOCITY_VAR_NAME = "widgetCode";
		public static final String FORM_WIDGET_FORM_ID_FIELD_NAME = "Form ID";
		public static final String FORM_WIDGET_FORM_ID_FIELD_VELOCITY_VAR_NAME = "formId";
		public static final String FORM_WIDGET_STRUCTURE_NAME_FIELD_NAME = "Forms";
		public static final String FORM_WIDGET_STRUCTURE_NAME_VELOCITY_VAR_NAME = "forms";
		
		/**
		 * Will create the base form fields for a specific structure.
		 * @param structure
		 * @throws DotDataException
		 * @throws DotStateException
		 */
		public void createBaseFormFields(Structure structure) throws DotDataException, DotStateException;
		
		/**
		 * Will return all forms
		 * @param user
		 * @param respectFrontEndPermissions
		 * @return
		 * @throws DotSecurityException 
		 * @throws DotDataException 
		 */
		public List<Structure> findAll(User user, boolean respectFrontEndPermissions) throws DotDataException, DotSecurityException;

		
		/**
		 * Will create the form widget structure to use in the addForm on edit mode pages.
		 * @throws DotDataException
		 * @throws DotStateException
		 */
		public void createFormWidgetInstanceStructure()throws DotDataException, DotStateException;
}
