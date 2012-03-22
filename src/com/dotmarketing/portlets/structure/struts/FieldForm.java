package com.dotmarketing.portlets.structure.struts;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.Globals;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.validator.ValidatorForm;

import com.dotmarketing.portlets.structure.business.FieldAPI;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.UtilMethods;

public class FieldForm extends ValidatorForm {
	
	
	private static final long serialVersionUID = 1L;
	private String inode;
	private String structureInode;
	private String fieldName;
	private String fieldType;
	private String fieldRelationType;
	private String fieldContentlet;
	private boolean required;
	private String velocityVarName;
	private int sortOrder;
	private String values;
	private String regexCheck;
	private List freeContentletFieldsName;
	private List freeContentletFieldsValue;
	private String validation;
	private String hint;
	private String defaultValue;
	private String dataType;
	private boolean indexed = false;
    private boolean listed = false;
    private boolean readOnly = false;
    private boolean fixed = false;
    private boolean searchable = false;
    private boolean unique = false;
    private String element;
    		
	public boolean isSearchable() {
		return searchable;
	}

	public void setSearchable(boolean searchable) {
		this.searchable = searchable;
	}

	public String getFieldContentlet() {
		return fieldContentlet;
	}

	public void setFieldContentlet(String fieldContentlet) {
		this.fieldContentlet = fieldContentlet;
	}

	public String getFieldName() {
		return fieldName;
	}

	public void setFieldName(String fieldName) {
		this.fieldName = fieldName;
	}

	public String getFieldRelationType() {
		return fieldRelationType;
	}

	public void setFieldRelationType(String fieldRelationType) {
		this.fieldRelationType = fieldRelationType;
	}

	public String getFieldType() {
		return fieldType;
	}

	public void setFieldType(String fieldType) {
		this.fieldType = fieldType;
	}

	public String getInode() {
		if(InodeUtils.isSet(inode))
			return inode;
		
		return "";
	}

	public void setInode(String inode) {
		this.inode = inode;
	}

	public String getRegexCheck() {
		return regexCheck;
	}

	public void setRegexCheck(String regexCheck) {
		this.regexCheck = regexCheck;
	}

	public boolean isRequired() {
		return required;
	}
	
	public void setRequired(boolean required) {
		this.required = required;
	}

	public int getSortOrder() {
		return sortOrder;
	}

	public void setSortOrder(int sortOrder) {
		this.sortOrder = sortOrder;
	}

	public String getStructureInode() {
		return structureInode;
	}

	public void setStructureInode(String structureInode) {
		this.structureInode = structureInode;
	}

	public String getValues() {
		return values;
	}

	public void setValues(String values) {
		this.values = values;
	}

	public String getVelocityVarName() {
		return velocityVarName;
	}

	public void setVelocityVarName(String velocityVarName) {
		this.velocityVarName = velocityVarName;
	}
	
	public List getFreeContentletFieldsName() {
		return freeContentletFieldsName;
	}

	public void setFreeContentletFieldsName(List freeContentletFieldsName) {
		this.freeContentletFieldsName = freeContentletFieldsName;
	}

	public List getFreeContentletFieldsValue() {
		return freeContentletFieldsValue;
	}

	public void setFreeContentletFieldsValue(List freeContentletFieldsvalue) {
		this.freeContentletFieldsValue = freeContentletFieldsvalue;
	}
	
	/**
	 * @return Returns the defaultValue.
	 */
	public String getDefaultValue() {
		return defaultValue;
	}

	/**
	 * @param defaultValue The defaultValue to set.
	 */
	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}

	/**
	 * @return Returns the hint.
	 */
	public String getHint() {
		return hint;
	}

	/**
	 * @param hint The hint to set.
	 */
	public void setHint(String hint) {
		this.hint = hint;
	}

	/**
	 * @return Returns the dataType.
	 */
	public String getDataType() {
		return dataType;
	}

	/**
	 * @param dataType The dataType to set.
	 */
	public void setDataType(String dataType) {
		this.dataType = dataType;
	}

	/**
	 * @return Returns the validation.
	 */
	public String getValidation() {
		if (!UtilMethods.isSet(validation)) {
			return regexCheck;
		}
		return validation;
	}

	/**
	 * @param validation The validation to set.
	 */
	public void setValidation(String validation) {
		this.validation = validation;
	}

	public boolean isIndexed() {
		return indexed;
	}

	public void setIndexed(boolean indexed) {
		this.indexed = indexed;
	}

    public boolean isListed() {
        return listed;
    }

    public void setListed(boolean listed) {
        this.listed = listed;
    }
    
    
	public boolean isReadOnly() {
		return readOnly;
	}

	public void setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
	}

	public boolean isFixed() {
		return fixed;
	}

	public void setFixed(boolean fixed) {
		this.fixed = fixed;
	}
	
	public boolean isUnique() {
		return unique;
	}

	public void setUnique(boolean unique) {
		this.unique = unique;
	}

    public ActionErrors validate(ActionMapping arg0, HttpServletRequest arg1) {
		ActionErrors ae = new ActionErrors();	
		ae = super.validate(arg0,arg1);
		if(!isFixed() && !isReadOnly() && !(fieldType == null) && !(fieldType.equals("line_divider") || fieldType.equals("tab_divider"))){
		if(fieldType.equals("select") || fieldType.equals("radio") || fieldType.equals("checkbox") || fieldType.equals("javascript"))
		{
			if (!UtilMethods.isSet(values))
			{
				ae.add(Globals.ERROR_KEY,new ActionMessage("message.field.values"));
			}
		}
		if( !fieldType.equals("host or folder" )&& !fieldType.equals("relationships_tab") && !fieldType.equals("permissions_tab") && !fieldType.equals("categories_tab") && !fieldType.equals("image") && !fieldType.equals("link") && !fieldType.equals("file") && !element.equals(FieldAPI.ELEMENT_CONSTANT) && !fieldType.equals("hidden"))
		{
			if (!UtilMethods.isSet(dataType))
			{
				ae.add(Globals.ERROR_KEY,new ActionMessage("message.field.dataType"));
			}
		}
		}
		return ae;
	}

	public String getElement() {
		return element;
	}

	public void setElement(String element) {
		this.element = element;
	}
	
}
