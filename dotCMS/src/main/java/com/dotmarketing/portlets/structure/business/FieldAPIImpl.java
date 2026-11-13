/**
 * 
 */
package com.dotmarketing.portlets.structure.business;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.contenttype.business.UniqueFieldValueDuplicatedException;
import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.transform.field.FieldVariableTransformer;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.business.PermissionLevel;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.FieldVariable;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

/**
 * @author Jason Tesser
 * @deprecated As of dotCMS 4.1.0, this API has been deprecated. From now on,
 *             please use the {@link com.dotcms.contenttype.business.FieldAPI}
 *             class via {@link APILocator#getContentTypeFieldAPI()} in order to
 *             interact with Content Type fields.
 */
@Deprecated
public class FieldAPIImpl implements FieldAPI, Serializable {

	/* (non-Javadoc)
	 * @see com.dotmarketing.portlets.structure.business.FieldAPI#valueSettable(com.dotmarketing.portlets.structure.model.Field)
	 */
	public boolean valueSettable(Field field) {
		if(field.getFieldType().equals(Field.FieldType.LINE_DIVIDER.toString()) || field.getFieldType().equals(Field.FieldType.TAB_DIVIDER.toString())
				|| field.getFieldType().equals(Field.FieldType.CATEGORIES_TAB.toString()) || field.getFieldType().equals(Field.FieldType.PERMISSIONS_TAB.toString())
						|| field.getFieldType().equals(Field.FieldType.RELATIONSHIPS_TAB.toString()) || field.getFieldType().equals(Field.FieldType.CATEGORY.toString())){
			return false;
		}
		return true;
	}
	public boolean isNumeric(Field field){
		if(field.getFieldContentlet().startsWith("integer") || field.getFieldContentlet().startsWith("float")){
			return true;
		}
		return false;
	}

	public boolean isString(Field field) {
		if(field.getFieldContentlet().startsWith("text")){
			return true;
		}
		return false;
	}
	
	public boolean isElementConstant(Field field) {
		return FieldAPI.ELEMENT_CONSTANT.equals(field.getFieldType()) || FieldAPI.ELEMENT_CONSTANT.equals(field.getFieldContentlet());
	}

	public boolean isElementHidden(final Field field) {
		return FieldAPI.ELEMENT_HIDDEN.equals(field.getFieldType()) || FieldAPI.ELEMENT_HIDDEN.equals(field.getFieldContentlet());
	}
	
	public boolean isElementDivider(Field field) {
		if(Field.FieldType.LINE_DIVIDER.toString().equals(field.getFieldType()) || Field.FieldType.TAB_DIVIDER.toString().equals(field.getFieldType())){
			return true;
		}
		return false;
	}
	
	public boolean isElementdotCMSTab(Field field) {
		if(Field.FieldType.CATEGORIES_TAB.toString().equals(field.getFieldType()) || Field.FieldType.PERMISSIONS_TAB.toString().equals(field.getFieldType()) 
				|| Field.FieldType.RELATIONSHIPS_TAB.toString().equals(field.getFieldType())){
			return true;
		}
		return false;
	}
	public boolean isAnalyze(Field field) {
		if (field.getFieldType().equals("checkbox") || field.getFieldType().equals("multi_select")) { 
			return true;
		} else if(field.getFieldContentlet().startsWith("text") || field.getFieldContentlet().startsWith("integer")){
			return true;
		}
		return false;
	}
	
	@CloseDBIfOpened
	public Field find(String id, User user, boolean respectFrontendRoles){
		return FieldsCache.getField(id);
	}

	/**
	 * Deletes a field variable as the given user. The variable is looked up by its id, and the
	 * user needs the same rights on its content type as
	 * {@link com.dotcms.contenttype.business.FieldAPI#save(com.dotcms.contenttype.model.field.FieldVariable, User)}
	 * asks for to save one.
	 *
	 * @param fieldVar             the variable to delete; only its id is used
	 * @param user                 the user deleting it
	 * @param respectFrontendRoles whether the user's front-end roles count
	 * @throws DotSecurityException if the user cannot change the variables of that content type
	 */
	@WrapInTransaction
	public void deleteFieldVariable(final FieldVariable fieldVar, final User user,
			final boolean respectFrontendRoles) throws DotDataException,
			DotSecurityException {
		final com.dotcms.contenttype.model.field.FieldVariable stored =
				FactoryLocator.getFieldFactory().loadVariable(fieldVar.getId());
		checkCanChangeVariablesOf(stored.fieldId(), user, respectFrontendRoles);
		try {
			APILocator.getContentTypeFieldAPI().delete(stored);
		} catch (final UniqueFieldValueDuplicatedException e) {
			throw new DotDataException(e.getMessage(), e);
		}
		FieldsCache.removeFieldVariables(find(stored.fieldId(), user, respectFrontendRoles));
	}

	/**
	 * Finds a field variable by its id, as the given user, who needs READ on the content type of
	 * its field.
	 *
	 * @param id                   the variable id
	 * @param user                 the user reading it
	 * @param respectFrontendRoles whether the user's front-end roles count
	 * @return the variable
	 * @throws DotSecurityException if the user cannot read that content type
	 */
	@CloseDBIfOpened
	public FieldVariable findFieldVariable(final String id, final User user,
			final boolean respectFrontendRoles) throws DotDataException,
			DotSecurityException {

		Logger.debug(this, ()-> "Finding the field variable: " + id);
		final com.dotcms.contenttype.model.field.FieldVariable stored =
				FactoryLocator.getFieldFactory().loadVariable(id);
		checkCanRead(stored.fieldId(), user, respectFrontendRoles);
		return new FieldVariableTransformer(stored).oldField();
	}

	/**
	 * Returns the variables of a field, as the given user, who needs READ on the field's content
	 * type.
	 *
	 * @param fieldId              the field id
	 * @param user                 the user reading them
	 * @param respectFrontendRoles whether the user's front-end roles count
	 * @return the field's variables
	 * @throws DotSecurityException if the user cannot read that content type
	 */
	@CloseDBIfOpened
	public List<FieldVariable> getFieldVariablesForField(final String fieldId, final User user,
			final boolean respectFrontendRoles) throws DotDataException,
			DotSecurityException {
		final com.dotcms.contenttype.model.field.Field field = checkCanRead(fieldId, user,
				respectFrontendRoles);
		return new FieldVariableTransformer(field.fieldVariables()).oldFieldList();
	}

	/**
	 * Saves a field variable as the given user, through
	 * {@link com.dotcms.contenttype.business.FieldAPI#save(com.dotcms.contenttype.model.field.FieldVariable, User)},
	 * which checks the user's rights on the field's content type. When the variable already
	 * exists, the user also needs those rights on the content type it belongs to now.
	 *
	 * @param object               the variable to save
	 * @param user                 the user saving it
	 * @param respectFrontendRoles whether the user's front-end roles count
	 * @throws DotSecurityException if the user cannot change the variables of that content type
	 */
	@WrapInTransaction
	public void saveFieldVariable(final FieldVariable object, final User user,
			final boolean respectFrontendRoles) throws DotDataException,
			DotSecurityException {
		if (UtilMethods.isSet(object.getId())) {
			final Optional<com.dotcms.contenttype.model.field.FieldVariable> stored =
					loadVariableIfExists(object.getId());
			if (stored.isPresent()) {
				checkCanChangeVariablesOf(stored.get().fieldId(), user, respectFrontendRoles);
			}
		}
		APILocator.getContentTypeFieldAPI()
				.save(new FieldVariableTransformer(object).newfield(), user);
	}

	/**
	 * Checks that the user can read the content type of the field, and returns the field.
	 */
	private com.dotcms.contenttype.model.field.Field checkCanRead(final String fieldId,
			final User user, final boolean respectFrontendRoles)
			throws DotDataException, DotSecurityException {
		final com.dotcms.contenttype.model.field.Field field =
				APILocator.getContentTypeFieldAPI().find(fieldId);
		APILocator.getContentTypeAPI(user, respectFrontendRoles).find(field.contentTypeId());
		return field;
	}

	/**
	 * Checks that the user can change the variables of the field's content type: READ on the
	 * type and EDIT_PERMISSIONS on it, as the content type field API asks for.
	 */
	private void checkCanChangeVariablesOf(final String fieldId, final User user,
			final boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
		final com.dotcms.contenttype.model.field.Field field =
				APILocator.getContentTypeFieldAPI().find(fieldId);
		final ContentType type = APILocator.getContentTypeAPI(user, respectFrontendRoles)
				.find(field.contentTypeId());
		APILocator.getPermissionAPI().checkPermission(type, PermissionLevel.EDIT_PERMISSIONS, user);
	}

	private Optional<com.dotcms.contenttype.model.field.FieldVariable> loadVariableIfExists(
			final String id) throws DotDataException {
		try {
			return Optional.of(FactoryLocator.getFieldFactory().loadVariable(id));
		} catch (final NotFoundInDbException e) {
			return Optional.empty();
		}
	}

    @Override
    public List<FieldVariable> getAllFieldVariables(User user, boolean respectFrontendRoles)
            throws DotDataException, DotSecurityException {
        return null;
    }
	
}
