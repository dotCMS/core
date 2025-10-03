package com.dotmarketing.portlets.structure.model;

import com.dotcms.contenttype.model.field.FieldIf;
import com.dotcms.contenttype.model.field.LegacyFieldTypes;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.portlets.structure.factories.FieldFactory;
import com.dotmarketing.util.UtilMethods;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
/**
 * 
 * @deprecated use {@link com.dotcms.contenttype.model.field.Field}
 *
 */
@Deprecated
public class Field extends Inode implements  FieldIf
{
  /**
   * 
   * @deprecated
   *
   */
  @Deprecated
	public enum FieldType {

		BUTTON("button"),
		CHECKBOX("checkbox"),
		DATE("date"),
		TIME("time"),
		DATE_TIME("date_time"),
		RADIO("radio"),
		SELECT("select"),
		MULTI_SELECT("multi_select"),
		TEXT("text"),
		TEXT_AREA("textarea"),
		WYSIWYG("wysiwyg"),
		FILE("file"),
		IMAGE("image"),
		TAG("tag"),
		CONSTANT("constant"),
		CATEGORY("category"),
		LINE_DIVIDER("line_divider"),
		TAB_DIVIDER("tab_divider"),
    	CATEGORIES_TAB("categories_tab"),
    	PERMISSIONS_TAB("permissions_tab"),
	    RELATIONSHIP("relationship"),
    	RELATIONSHIPS_TAB("relationships_tab"),
    	HIDDEN("hidden"),
    	BINARY("binary"), // http://jira.dotmarketing.net/browse/DOTCMS-1073
		CUSTOM_FIELD("custom_field"), // http://jira.dotmarketing.net/browse/DOTCMS-2869
		HOST_OR_FOLDER("host or folder"),// http://jira.dotmarketing.net/browse/DOTCMS-3232
		KEY_VALUE("key_value"),
	  	ROW("row"),
	  	COLUMN("column"),
	  	STORY_BLOCK_FIELD("story_block_field"),
	    JSON_FIELD("json_field");

		private String value;

		FieldType (String value) {
			this.value = value;
		}

		public String toString () {
			return value;
		}

		public static FieldType getFieldType (String value) {
			FieldType[] types = FieldType.values();
			for (FieldType type : types) {
				if (type.value.equals(value))
					return type;
			}
			return null;
		}

	}

	/**
	 * 
	 * @deprecated
	 *
	 */
	@Deprecated
	public enum DataType {

		BOOL("bool"),
		DATE("date"),
		FLOAT("float"),
		INTEGER("integer"),
		TEXT("text"),
		LONG_TEXT("text_area"),
		SECTION_DIVIDER("section_divider"),
		BINARY("binary"),
		SYSTEM("system_field");

		private String value;

		DataType (String value) {
			this.value = value;
		}

		public String toString () {
			return value;
		}

		public static DataType getDataType (String value) {
			DataType[] types = DataType.values();
			for (DataType type : types) {
				if (type.value.equals(value))
					return type;
			}
			return null;
		}

	}

	private static final long serialVersionUID = 1L;

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
	private String hint;
	private String defaultValue;
	private boolean indexed;
    private boolean listed;
	private boolean fixed;
	private boolean readOnly;
    private boolean searchable;
    private boolean unique;
    private Date modDate;

    public Field(){
    	super.setType("field");
    	setFieldName("");
    	setFieldType("");
    	setFieldContentlet("");
    	setVelocityVarName("");
    	setValues("");
    	setRegexCheck("");
    	setHint("");
    	setDefaultValue("");
    	modDate = new Date();
    }

    /**
     * 
     * @param fieldName
     * @param fieldType
     * @param dataType
     * @param structure
     * @param required
     * @param listed
     * @param indexed
     * @param sortOrder
     * @param fixed
     * @param readOnly
     * @param searchable
     */
	public Field (String fieldName, FieldType fieldType, DataType dataType, Structure structure, boolean required, boolean listed, boolean indexed, int sortOrder,boolean fixed, boolean readOnly, boolean searchable) {
		this(fieldName, fieldType, dataType, structure, required, listed, indexed, sortOrder, "", "", "",fixed, readOnly, searchable);
	}

	/**
	 * 
	 * @param fieldName
	 * @param fieldType
	 * @param dataType
	 * @param structure
	 * @param required
	 * @param listed
	 * @param indexed
	 * @param sortOrder
	 * @param values
	 * @param defaultValue
	 * @param checkRegex
	 * @param fixed
	 * @param readOnly
	 * @param searchable
	 */
	public Field (String fieldName, FieldType fieldType, DataType dataType, Structure structure, boolean required, boolean listed, boolean indexed, int sortOrder, String values, String defaultValue, String checkRegex, boolean fixed, boolean readOnly, boolean searchable) {
		this();
		this.setFieldContentlet(FieldFactory.getNextAvaliableFieldNumber(dataType.toString(), "", structure.getInode()));
		this.setFieldName(fieldName);
		String fieldNameCC = UtilMethods.toCamelCase(fieldName);
		this.setFieldRelationType(structure.getName() + ":" + UtilMethods.toCamelCase(fieldName));
		this.setFieldType(fieldType.toString());
		this.setStructureInode(structure.getInode());
		this.setVelocityVarName(fieldNameCC);
		this.setSortOrder(sortOrder);
		this.setRequired(required);
		this.setListed(listed);
		if(searchable){
		    this.setIndexed(true);
		}else{
			this.setIndexed(indexed);
		}
		this.searchable = searchable;
		this.setDefaultValue(defaultValue);
		this.setRegexCheck(checkRegex);
		this.setValues(values);
		this.setReadOnly(readOnly);
		this.setFixed(fixed);
	}

    /**
     * Database name of the field (text1, text2, ..., date1, ...)
     * @return
     */
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
		if(fieldType.contains(".")){
			String x = LegacyFieldTypes.getLegacyName(fieldType);
			return LegacyFieldTypes.getLegacyName(fieldType);
		}
		return fieldType;
	}
	public void setFieldType(String fieldType) {
		this.fieldType = fieldType;
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
	public String getRegexCheck() {
		return regexCheck;
	}
	public void setRegexCheck(String regexCheck) {
		this.regexCheck = regexCheck;
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
	public void delete() throws DotHibernateException
	{
		FieldFactory.deleteField(this);
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


	public boolean isFixed() {
		return fixed;
	}

	public void setFixed(boolean fixed) {
		this.fixed = fixed;
	}

	public boolean isReadOnly() {
		return readOnly;
	}

	public void setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
	}

    public Map<String, Object> getMap () {
        Map<String, Object> oMap = new HashMap<> ();
        oMap.put("defaultValue", this.getDefaultValue());
        oMap.put("fieldContentlet", this.getFieldContentlet());
        oMap.put("fieldName", this.getFieldName());
        oMap.put("fieldRelationType", this.getFieldRelationType());
        oMap.put("fieldFieldType", this.getFieldType());
        oMap.put("fieldHint", this.getHint());
        oMap.put("fieldRegexCheck", this.getRegexCheck());
        oMap.put("fieldValues", this.getValues()!=null ? this.getValues() : "");
        oMap.put("fieldVelocityVarName", this.getVelocityVarName());
        oMap.put("fieldSortOrder", this.getSortOrder());
        oMap.put("fieldStructureInode", this.getStructureInode());
        oMap.put("fieldStructureVarName", CacheLocator.getContentTypeCache().getStructureByInode(this.getStructureInode()).getVelocityVarName());
        oMap.put("fieldRequired", this.isRequired());
        oMap.put("fieldIndexed", this.isIndexed());
        oMap.put("fieldListed", this.isListed());
        oMap.put("fieldSearchable", this.isSearchable());
		oMap.put("fieldFixed", this.isFixed());
		oMap.put("fieldReadOnly", this.isReadOnly());
		oMap.put("fieldUnique", this.isUnique());
        oMap.put("inode", this.getInode());
        return oMap;
    }

	public boolean isSearchable() {
		return searchable;
	}

	public void setSearchable(boolean searchable) {
		this.searchable = searchable;
	}

	public boolean isUnique() {
		return unique;
	}

	public void setUnique(boolean unique) {
		this.unique = unique;
	}

	public Date getModDate() {
		return modDate;
	}

	public void setModDate(Date modDate) {
		this.modDate = modDate;
	}

    @Override
    public String toString() {
        return "Field{" +
                "structureInode='" + structureInode + '\'' +
                ", fieldName='" + fieldName + '\'' +
                ", fieldType='" + fieldType + '\'' +
                ", fieldRelationType='" + fieldRelationType + '\'' +
                ", fieldContentlet='" + fieldContentlet + '\'' +
                ", required=" + required +
                ", velocityVarName='" + velocityVarName + '\'' +
                ", sortOrder=" + sortOrder +
                ", values='" + values + '\'' +
                ", regexCheck='" + regexCheck + '\'' +
                ", hint='" + hint + '\'' +
                ", defaultValue='" + defaultValue + '\'' +
                ", indexed=" + indexed +
                ", listed=" + listed +
                ", fixed=" + fixed +
                ", readOnly=" + readOnly +
                ", searchable=" + searchable +
                ", unique=" + unique +
                ", modDate=" + modDate +
                '}';
    }

    public String getDataType() {
		return this.getFieldContentlet().replaceAll("[0-9]*", "");
	}
}
