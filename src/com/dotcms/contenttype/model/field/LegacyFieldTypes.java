package com.dotcms.contenttype.model.field;





public enum LegacyFieldTypes {

	BUTTON("button",com.dotcms.contenttype.model.field.ButtonField.class),
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
	CATEGORIES_TAB("categories_tab",com.dotcms.contenttype.model.field.CategoriesTabField.class),
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
		return legacyValue;
	}



	public static Class getImplClass (String legacyValue) {
		for(LegacyFieldTypes fieldType : LegacyFieldTypes.values()){
			if(fieldType.legacyValue.equals(legacyValue)){
				return fieldType.implClass;
			}
		}
		try {
			return Class.forName(legacyValue);
		} catch (ClassNotFoundException e) {
			return null;
		}
	}

}


