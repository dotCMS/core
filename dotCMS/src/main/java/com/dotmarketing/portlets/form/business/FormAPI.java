package com.dotmarketing.portlets.form.business;

import java.util.List;

import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
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
		public static final String FORM_TITLE_FIELD_VELOCITY_VAR_NAME = "formTitle";
		public static final String FORM_HOST_FIELD_NAME = "Form Host";
		
		public static final String FORM_WIDGET_CODE_FIELD_NAME = "Widget Code";
		public static final String FORM_WIDGET_CODE_VELOCITY_VAR_NAME = "widgetCode";
		public static final String FORM_WIDGET_FORM_ID_FIELD_NAME = "Form ID";
		public static final String FORM_WIDGET_FORM_ID_FIELD_VELOCITY_VAR_NAME = "formId";
		public static final String FORM_WIDGET_STRUCTURE_NAME_FIELD_NAME = "Forms";
		public static final String FORM_WIDGET_STRUCTURE_NAME_VELOCITY_VAR_NAME = "forms";
		public static final String FORM_WIDGET_TITLE_FIELD_NAME = "Widget Title";
        public static final String FORM_WIDGET_TITLE_VELOCITY_VAR_NAME = "widgetTitle";
		
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

		/**
		 * Finds the widget a form is placed on pages with, read as the system user.
		 *
		 * @param formId the form's content type id
		 * @return the widget, or {@code null} when the form has none
		 * @deprecated use {@link #getFormContent(String, User)}, which reads with the caller's permissions
		 */
		@Deprecated
		public Contentlet getFormContent(String formId);

		/**
		 * Finds the widget a form is placed on pages with, among the widgets the given user can read.
		 *
		 * @param formId the form's content type id or variable
		 * @param user   the user the widget is read for
		 * @return the widget, or {@code null} when no content type has that id or variable, or the form
		 *         has none the user can read
		 * @throws DotDataException     if the widgets cannot be searched
		 * @throws DotSecurityException if the user cannot run the search
		 */
		public Contentlet getFormContent(String formId, User user) throws DotDataException, DotSecurityException;

		/**
		 * Creates and publishes the widget a form is placed on pages with, as the system user.
		 *
		 * @param formId the form's content type id
		 * @return the new widget
		 * @throws DotDataException     if the widget cannot be saved
		 * @throws DotSecurityException if the widget cannot be saved
		 * @deprecated use {@link #createDefaultFormContent(String, User)}, which saves and publishes as
		 *             the caller
		 */
		@Deprecated
		public Contentlet createDefaultFormContent(String formId) throws DotDataException, DotSecurityException;

		/**
		 * Creates the widget a form is placed on pages with, and publishes it, as the given user, so the
		 * user needs permission to create and publish widget content. Visitors can read the new widget,
		 * and every other role may read, edit and publish it as far as it may do so with the form.
		 *
		 * @param formId the form's content type id
		 * @param user   the user the widget is created and published as
		 * @return the new, live widget
		 * @throws DotDataException     if the widget cannot be saved
		 * @throws DotSecurityException if the user may not create or publish widget content
		 */
		public Contentlet createDefaultFormContent(String formId, User user) throws DotDataException, DotSecurityException;
}
