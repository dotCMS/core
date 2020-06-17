/**
 * 
 */
package com.dotmarketing.portlets.structure.business;

import java.util.List;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.structure.factories.FieldFactory;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.FieldVariable;
import com.liferay.portal.model.User;

/**
 * @author Jason Tesser
 * @deprecated As of dotCMS 4.1.0, this API has been deprecated. From now on,
 *             please use the {@link com.dotcms.contenttype.business.FieldAPI}
 *             class via {@link APILocator#getContentTypeFieldAPI()} in order to
 *             interact with Content Type fields.
 */
@Deprecated
public class FieldAPIImpl implements FieldAPI {

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

	public void deleteFieldVariable(FieldVariable fieldVar, User user,
			boolean respectFrontendRoles) throws DotDataException,
			DotSecurityException {
		Field field = APILocator.getFieldAPI().find(fieldVar.getFieldId(), APILocator.getUserAPI().getSystemUser(), true);
		FieldFactory.deleteFieldVariable(fieldVar);
		FieldsCache.removeFieldVariables(field);
	}

	@WrapInTransaction
	public FieldVariable findFieldVariable(String id, User user,
			boolean respectFrontendRoles) throws DotDataException,
			DotSecurityException {		
		return FieldFactory.getFieldVariable(id);		
	}

	@CloseDBIfOpened
	public List<FieldVariable> getFieldVariablesForField(String fieldId, User user,
			boolean respectFrontendRoles) throws DotDataException,
			DotSecurityException {		
		return FieldFactory.getFieldVariablesForField(fieldId);
	}

	@WrapInTransaction
	public void saveFieldVariable(FieldVariable object, User user,
			boolean respectFrontendRoles) throws DotDataException,
			DotSecurityException {
		FieldFactory.saveFieldVariable(object);
	}

    @Override
    public List<FieldVariable> getAllFieldVariables(User user, boolean respectFrontendRoles)
            throws DotDataException, DotSecurityException {
        return null;
    }
	
}
