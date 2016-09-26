package com.dotcms.contenttype.model.field;





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
	LINE_DIVIDER("line_divider",com.dotcms.contenttype.model.field.LineDividerField.class),
	TAB_DIVIDER("tab_divider",com.dotcms.contenttype.model.field.TabDividerField.class),
	PERMISSIONS_TAB("permissions_tab",com.dotcms.contenttype.model.field.PermissionTabField.class),
	RELATIONSHIPS_TAB("relationships_tab",com.dotcms.contenttype.model.field.RelationshipsTabField.class),
	HIDDEN("hidden",com.dotcms.contenttype.model.field.HiddenField.class),
	BINARY("binary",com.dotcms.contenttype.model.field.BinaryField.class), 
	CUSTOM_FIELD("custom_field",com.dotcms.contenttype.model.field.CustomField.class),
	HOST_OR_FOLDER("host or folder",com.dotcms.contenttype.model.field.HostFolderField.class),
	KEY_VALUE("key_value",com.dotcms.contenttype.model.field.KeyValueField.class);

	private String legacyValue;
	private Class implClass;
	
	LegacyFieldTypes (String legacyValue, Class implClass) {
		this.legacyValue = legacyValue;
		this.implClass = implClass;
		}

	public String toString () {
		return this.legacyValue;
	}
	
	public String legacyValue () {
		return this.legacyValue;
	}
	public Class implClass (){
		return this.implClass;
	}


	public static Class getImplClass (String legacyValue) {
		String className = legacyValue.replace("Immutable", "");
		
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
	
	public static String getLegacyName (Class clazz) {
		return getLegacyName(clazz.getCanonicalName());
	}
	
	public static String getLegacyName (String clazz) {
		clazz=clazz.replace(".Immutable", ".");
		for(LegacyFieldTypes fieldType : LegacyFieldTypes.values()){
			if(fieldType.implClass.getCanonicalName().equals(clazz)){
				return fieldType.legacyValue();
			}
		}
		return clazz;
	}
}


