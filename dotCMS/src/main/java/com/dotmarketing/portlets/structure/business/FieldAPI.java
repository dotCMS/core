package com.dotmarketing.portlets.structure.business;

import java.util.List;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.FieldVariable;
import com.liferay.portal.model.User;

/**
 * 
 * @author Jason Tesser
 * @since 1.6
 */
public interface FieldAPI {

	public static final String ELEMENT_FIELD = "field";
	public static final String ELEMENT_DIVIDER = "divider";
	public static final String ELEMENT_TAB = "tab";
	public static final String ELEMENT_CONSTANT = "constant";
	
	public boolean valueSettable(Field field);
	
	/**
	 * Use to determine if the field has a numeric value.
	 * @param field
	 * @return
	 */
	public boolean isNumeric(Field field);
	
	/**
	 * Use to determine if the field has a numeric value.
	 * @param field
	 * @return
	 */
	public boolean isString(Field field);
	
	/**
	 * A field that is a divider element like Line or tab divider
	 * @param field
	 * @return
	 */
	public boolean isElementDivider(Field field);
	
	/**
	 * A field that is a constant
	 * @param field
	 * @return
	 */
	public boolean isElementConstant(Field field);
	
	/**
	 * A field that is a divider element like Line or tab divider
	 * @param field
	 * @return
	 */
	public boolean isElementdotCMSTab(Field field);
	
	/**
	 * Should the field be analyzed in the index
	 * @param field
	 * @return
	 */
	public boolean isAnalyze(Field field);
	
	/**
	 * Retrieves a Field given its id
	 */
	public Field find(String id, User user, boolean respectFrontendRoles) throws DotDataException;
	
	public void deleteFieldVariable(FieldVariable object, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException ;

	public FieldVariable findFieldVariable(String id, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException ;
	
	public List<FieldVariable > getAllFieldVariables(User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;
	
	public List<FieldVariable > getFieldVariablesForField(String fieldId,User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException; 
	
	public void saveFieldVariable(FieldVariable object, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;	
	
}
