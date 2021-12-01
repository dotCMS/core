package com.dotcms.contenttype.model.field;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import com.google.common.collect.ImmutableMap;

/**
 * This class provides a useful data mapping between the legacy fields and new
 * fields. It allows you to obtain the type of the legacy fields as well as the
 * new type, represented by its respective class now. This will also assist in
 * the process of removing the remaining legacy code as new versions of the
 * application are released.
 * 
 * @author Will Ezell
 * @version 4.1
 * @since Oct 17, 2016
 *
 */
public enum LegacyFieldTypes {

	CHECKBOX("checkbox",com.dotcms.contenttype.model.field.CheckboxField.class),
	DATE("date",com.dotcms.contenttype.model.field.DateField.class),
	TIME("time",com.dotcms.contenttype.model.field.TimeField.class),
	DATE_TIME("date_time",com.dotcms.contenttype.model.field.DateTimeField.class),
	RADIO("radio",com.dotcms.contenttype.model.field.RadioField.class),
	SELECT("select",com.dotcms.contenttype.model.field.SelectField.class),
	MULTI_SELECT("multi_select",com.dotcms.contenttype.model.field.MultiSelectField.class),
	TEXT("text",com.dotcms.contenttype.model.field.TextField.class),
	TEXT_AREA("textarea",com.dotcms.contenttype.model.field.TextAreaField.class),
	WYSIWYG("wysiwyg",com.dotcms.contenttype.model.field.WysiwygField.class),
	FILE("file",com.dotcms.contenttype.model.field.FileField.class),
	IMAGE("image",com.dotcms.contenttype.model.field.ImageField.class),
	TAG("tag",com.dotcms.contenttype.model.field.TagField.class),
	CONSTANT("constant",com.dotcms.contenttype.model.field.ConstantField.class),
	CATEGORY("category",com.dotcms.contenttype.model.field.CategoryField.class),
	RELATIONSHIP("relationship",com.dotcms.contenttype.model.field.RelationshipField.class),
	LINE_DIVIDER("line_divider",com.dotcms.contenttype.model.field.LineDividerField.class),
	TAB_DIVIDER("tab_divider",com.dotcms.contenttype.model.field.TabDividerField.class),
	PERMISSIONS_TAB("permissions_tab",com.dotcms.contenttype.model.field.PermissionTabField.class),
	RELATIONSHIPS_TAB("relationships_tab",com.dotcms.contenttype.model.field.RelationshipsTabField.class),
	HIDDEN("hidden",com.dotcms.contenttype.model.field.HiddenField.class),
	BINARY("binary",com.dotcms.contenttype.model.field.BinaryField.class), 
	CUSTOM_FIELD("custom_field",com.dotcms.contenttype.model.field.CustomField.class),
	HOST_OR_FOLDER("host or folder",com.dotcms.contenttype.model.field.HostFolderField.class),
	KEY_VALUE("key_value",com.dotcms.contenttype.model.field.KeyValueField.class),
	ROW_FIELD("row",com.dotcms.contenttype.model.field.RowField.class),
	COLUMN_FIELD("column",com.dotcms.contenttype.model.field.ColumnField.class),
	STORY_BLOCK_FIELD("story_block_field",com.dotcms.contenttype.model.field.StoryBlockField.class);

    final static private Map<String, String> oldFieldMap;
    static {
      Map<String,String> map = new HashMap<>();
    
      for(LegacyFieldTypes fieldType : LegacyFieldTypes.values()){
        map.put(fieldType.implClass.getCanonicalName(),fieldType.legacyValue() );
      }
      oldFieldMap = ImmutableMap.copyOf(map);
    }
  
	private String legacyValue;
	private Class<? extends Field> implClass;
	private static final Pattern immutablePattern = Pattern.compile(".Immutable");
	private static final Map<String, String> classToLegacyClassMap = new ConcurrentHashMap<> ();

	/**
	 * Default constructor where the association between the legacy and new
	 * field types is created.
	 * 
	 * @param legacyValue
	 *            - The legacy field type.
	 * @param implClass
	 *            - The new field type.
	 */
	LegacyFieldTypes (String legacyValue, Class<? extends Field> implClass) {
		this.legacyValue = legacyValue;
		this.implClass = implClass;
	}

	/**
	 * Returns the legacy field type of the current Enum.
	 */
	public String toString () {
		return this.legacyValue;
	}

	/**
	 * Returns the legacy field type of the current Enum.
	 * 
	 * @return The legacy field type.
	 */
	public String legacyValue () {
		return this.legacyValue;
	}

	/**
	 * Returns the new field type of the current Enum.
	 * 
	 * @return The new field type, i.e., the field class.
	 */
	public Class<? extends Field> implClass (){
		return this.implClass;
	}

	/**
	 * Returns the new field class associated to the specified legacy field
	 * type.
	 * 
	 * @param legacyValue
	 *            - The legacy field type.
	 * @return The class of the new field.
	 */
	public static Class getImplClass (String legacyValue) {
		String className =  immutablePattern.matcher(legacyValue).replaceFirst(".");
		
		for(LegacyFieldTypes fieldType : LegacyFieldTypes.values()){
			if(fieldType.legacyValue.equals(legacyValue) || fieldType.implClass.getCanonicalName().equals(className)){
				return fieldType.implClass;
			}
		}
		try {
			return Class.forName(legacyValue);
		} catch (ClassNotFoundException e) {
			return null;
		}
	}

	/**
	 * Returns the legacy field type associated to the specified new field
	 * class.
	 * 
	 * @param clazz
	 *            - The new field class.
	 * @return The legacy field type.
	 */
	public static String getLegacyName (Class<? extends Field> clazz) {
		return getLegacyName(clazz.getCanonicalName());
	}

	/**
	 * Returns the legacy field type associated to the specified new field
	 * implementation class.
	 * 
	 * @param clazz
	 *            - The new field implementation class.
	 * @return The legacy field type.
	 */
	public static String getLegacyName (String clazz) {

	    if (!classToLegacyClassMap.containsKey(clazz)) {
	        classToLegacyClassMap.put(clazz, immutablePattern.matcher(clazz).replaceFirst("."));
	    }
	    
	    return oldFieldMap.get(classToLegacyClassMap.get(clazz));
	}

}
