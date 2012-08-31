package com.dotmarketing.viewtools;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.tools.ViewTool;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.FieldVariable;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.viewtools.StructuresWebAPI;
import com.liferay.portal.model.User;

/**
 * Allow user to get field attributes
 * @author Oswaldo
 *
 */
public class SubmitContentWebAPI implements ViewTool{

	private HttpServletRequest request;
	private UserWebAPI userAPI;
	private User user = null;
	
	public void init(Object obj) {
		ViewContext context = (ViewContext) obj;
		this.request = context.getRequest();
		userAPI = WebAPILocator.getUserWebAPI();
		try {
			user = userAPI.getLoggedInFrontendUser(request);
		} catch (Exception e) {
			Logger.error(this, "Error finding the logged in user", e);
		}
	}
	
	/**
	 * Retrieves the field variables
	 * @param field Field
	 * @return List<FieldVariable> 
	 */
	public List<FieldVariable> getFieldVariables(Field field){
		try {
			return APILocator.getFieldAPI().getFieldVariablesForField(field.getInode(), user, true);
		} catch (DotDataException e) {
			Logger.error(StructuresWebAPI.class,e.getMessage(),e);
		} catch (DotSecurityException e) {
			Logger.error(StructuresWebAPI.class,e.getMessage(),e);
		}
		return new ArrayList<FieldVariable>();
	}
	
	/**
	 * Validate if a FieldVariable is present in a FieldVariable List
	 * @param list List<FieldVariable>
	 * @param fieldVariableName Variable Name
	 * @return boolean
	 */
	public boolean containsFieldVariable(List<FieldVariable> list, String fieldVariableName){
		boolean containsVariable =false;
		if(!UtilMethods.isSet(fieldVariableName)){
			return false;
		}
		for(FieldVariable fv : list){
			if(fv.getKey().equals(fieldVariableName)){
				containsVariable= true;
				break;
			}
		}		
		return containsVariable;
	}
	
	/**
	 * Retrieves the field variable value based on its Field variables and variable name
	 * @param fieldVariableList List of field variables
	 * @param fieldVariableName Field variable name
	 * @return
	 */
	public String getFieldVariableValue(List<FieldVariable> fieldVariableList, String fieldVariableName){
		if(!UtilMethods.isSet(fieldVariableName)){
			return null;
		}
		for(FieldVariable fv : fieldVariableList){
			if(fv.getKey().equals(fieldVariableName)){
				return fv.getValue();
			}
		}		
		return null;
	}
	
	/**
	 * Retrieves the field variable value based on its Field and variable name
	 * @param field Field
	 * @param fieldVariableName Field variable name
	 * @return String
	 */
	public String getFieldVariableValue(Field field, String fieldVariableName){
		List<FieldVariable> fieldVariableList = getFieldVariables(field);
		if(!UtilMethods.isSet(fieldVariableName)){
			return null;
		}
		for(FieldVariable fv : fieldVariableList){
			if(fv.getKey().equals(fieldVariableName)){
				return fv.getValue();
			}
		}		
		return null;
	}
	
	/**
	 * Retrieves the field variable value based on its structure name, field name and variable name
	 * @param structureVarName Structure velocity name
	 * @param fieldVarName Field velocity name
	 * @param fieldVariable Field variable name
	 * @param respectFrontEndRoles
	 * @return String
	 */
	public String getFieldVariableValue(String structureVarName, String fieldVarName, String fieldVariable){
		Structure st = StructureCache.getStructureByVelocityVarName(structureVarName);
		Field field = findFieldByVarName(st, fieldVarName);				
		return getFieldVariableValue(field,fieldVariable);
	}
	
	/**
	 * Retrieves a field from a structure based on its field inode
	 * @param st The structure
	 * @param fieldVariableName The variable name
	 * @return
	 */
	public Field findFieldByVarName(Structure st, String fieldVariableName) {
		List<Field> fields = FieldsCache.getFieldsByStructureInode(st.getInode());
		for(Field f : fields) {
			if(f.getVelocityVarName().equals(fieldVariableName))
				return f;
		}
		return null;
	}

}
