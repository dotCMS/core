package com.dotcms.contenttype.transform;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.elasticsearch.common.Nullable;

import com.dotcms.contenttype.model.decorator.FieldDecorator;
import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldTypes;
import com.dotcms.contenttype.model.field.ImmutableBinaryField;
import com.dotcms.contenttype.model.field.ImmutableButtonField;
import com.dotcms.contenttype.model.field.ImmutableCategoriesTabField;
import com.dotcms.contenttype.model.field.ImmutableCategoryField;
import com.dotcms.contenttype.model.field.ImmutableCheckboxField;
import com.dotcms.contenttype.model.field.ImmutableConstantField;
import com.dotcms.contenttype.model.field.ImmutableCustomField;
import com.dotcms.contenttype.model.field.ImmutableDateField;
import com.dotcms.contenttype.model.field.ImmutableDateTimeField;
import com.dotcms.contenttype.model.field.ImmutableFileField;
import com.dotcms.contenttype.model.field.ImmutableHiddenField;
import com.dotcms.contenttype.model.field.ImmutableHostFolderField;
import com.dotcms.contenttype.model.field.ImmutableImageField;
import com.dotcms.contenttype.model.field.ImmutableKeyValueField;
import com.dotcms.contenttype.model.field.ImmutableLineDividerField;
import com.dotcms.contenttype.model.field.ImmutableMultiSelectField;
import com.dotcms.contenttype.model.field.ImmutablePermissionTabField;
import com.dotcms.contenttype.model.field.ImmutableRadioField;
import com.dotcms.contenttype.model.field.ImmutableRelationshipsTabField;
import com.dotcms.contenttype.model.field.ImmutableSelectField;
import com.dotcms.contenttype.model.field.ImmutableTabDividerField;
import com.dotcms.contenttype.model.field.ImmutableTagField;
import com.dotcms.contenttype.model.field.ImmutableTextAreaField;
import com.dotcms.contenttype.model.field.ImmutableTextField;
import com.dotcms.contenttype.model.field.ImmutableTimeField;
import com.dotcms.contenttype.model.field.ImmutableWysiwygField;
import com.dotcms.repackage.com.google.common.collect.ImmutableList;
import com.dotmarketing.exception.DotDataException;

public class DbFieldTransformer {

	public static Field transform(final Map<String, Object> map) throws DotDataException {

		String fieldType = (String) map.get("field_type");
		final FieldTypes TYPE = FieldTypes.getFieldType(fieldType);
		final Field field = new Field() {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			/**
			 * structure_inode field_type field_relation_type field_contentlet
			 * required indexed listed sort_order default_value fixed read_only
			 * searchable unique_ inode idate type
			 */
			@Override
			public String variable() {
				return (String) map.get("velocity_var_name");
			}

			@Override
			public String values() {
				return (String) map.get("field_values");
			}

			@Override
			public String type() {
				return (String) map.get("field_type");
			}
			
			@Override
			public FieldTypes fieldType() {
				return TYPE;
			}

			@Override
			@Nullable
			public String relationType() {
				return (String) map.get("field_relation_type");
			}

			
			
			
			@Override
			public String contentTypeId() {
				return (String) map.get("structure_inode");
			}

			@Override
			public String regexCheck() {
				return (String) map.get("regex_check");
			}

			@Override
			public String owner() {
				return (String) map.get("owner");
			}

			@Override
			public String name() {
				return (String) map.get("field_name");
			}

			@Override
			public String inode() {
				return (String) map.get("inode");
			}

			@Override
			public String hint() {
				return (String) map.get("hint");
			}

			@Override
			public String defaultValue() {
				return (String) map.get("default_value");
			}

			@Override
			public DataTypes dataType() {
				String dbType = map.get("field_contentlet").toString().replaceAll("[0-9]", "");
				return DataTypes.getDataType(dbType);
			}

			@Override
			public String dbColumn() {
				return (String) map.get("field_contentlet");

			}

			@Override
			public Date modDate() {
				return new Date(((Date) map.get("mod_date")).getTime());
			}
			@Override
			public Date iDate() {
				return (Date) map.get("idate");
			}
			@Override
			public boolean required() {
				return Boolean.getBoolean(String.valueOf(map.get("required")));
			}

			@Override
			public int sortOrder() {
				return (Integer) map.get("sort_order");

			}

			@Override
			public boolean indexed() {
				return Boolean.parseBoolean(String.valueOf(map.get("indexed")));
			}

			@Override
			public boolean listed() {
				return Boolean.parseBoolean(String.valueOf(map.get("listed")));
			}

			@Override
			public boolean fixed() {
				return Boolean.parseBoolean(String.valueOf(map.get("fixed")));
			}

			@Override
			public boolean readOnly() {
				return Boolean.parseBoolean(String.valueOf(map.get("read_only")));
			}

			@Override
			public boolean searchable() {
				return Boolean.parseBoolean(String.valueOf(map.get("searchable")));
			}

			@Override
			public boolean unique() {
				return Boolean.parseBoolean(String.valueOf(map.get("unique_")));
			}

			@Override
			public List<FieldDecorator> fieldDecorators() {

				return super.fieldDecorators();
			}

			@Override
			public List<DataTypes> acceptedDataTypes() {
				return ImmutableList.of();
			}

		};
		
		
		return transformToSubclass(field);
		
		
	}
		
		
	public static Field transformToSubclass(Field field) throws DotDataException{
		FieldTypes TYPE = field.fieldType();

		switch (TYPE) {
			case BINARY:
				return ImmutableBinaryField.builder().from(field).build();
			case BUTTON:
				return ImmutableButtonField.builder().from(field).build();
			case CATEGORIES_TAB:
				return ImmutableCategoriesTabField.builder().from(field).build();
			case CATEGORY:
				return ImmutableCategoryField.builder().from(field).build();
			case CHECKBOX:
				return ImmutableCheckboxField.builder().from(field).build();
			case CONSTANT:
				return ImmutableConstantField.builder().from(field).build();
			case CUSTOM_FIELD:
				return ImmutableCustomField.builder().from(field).build();
			case DATE:
				return ImmutableDateField.builder().from(field).build();
			case DATE_TIME:
				return ImmutableDateTimeField.builder().from(field).build();
			case FILE:
				return ImmutableFileField.builder().from(field).build();
			case HIDDEN:
				return ImmutableHiddenField.builder().from(field).build();
			case HOST_OR_FOLDER:
				return ImmutableHostFolderField.builder().from(field).build();
			case IMAGE:
				return ImmutableImageField.builder().from(field).build();
			case KEY_VALUE:
				return ImmutableKeyValueField.builder().from(field).build();
			case LINE_DIVIDER:
				return ImmutableLineDividerField.builder().from(field).build();
			case MULTI_SELECT:
				return ImmutableMultiSelectField.builder().from(field).build();
			case PERMISSIONS_TAB:
				return ImmutablePermissionTabField.builder().from(field).build();
			case RADIO:
				return ImmutableRadioField.builder().from(field).build();
			case RELATIONSHIPS_TAB:
				return ImmutableRelationshipsTabField.builder().from(field).build();
			case SELECT:
				return ImmutableSelectField.builder().from(field).build();
			case TAB_DIVIDER:
				return ImmutableTabDividerField.builder().from(field).build();
			case TAG:
				return ImmutableTagField.builder().from(field).build();
			case TEXT:
				return ImmutableTextField.builder().from(field).build();
			case TEXT_AREA:
				return ImmutableTextAreaField.builder().from(field).build();
			case TIME:
				return ImmutableTimeField.builder().from(field).build();
			case WYSIWYG:
				return ImmutableWysiwygField.builder().from(field).build();

		}

		return field;
	}

	/**
	 * Fields in the db: inode owner idate type inode name description
	 * default_structure page_detail structuretype system fixed
	 * velocity_var_name url_map_pattern host folder expire_date_var
	 * publish_date_var mod_date
	 **/

	public static List<Field> transform(final List<Map<String, Object>> list) throws DotDataException {

		ImmutableList.Builder<Field> builder = ImmutableList.builder();
		for (Map<String, Object> map : list) {
			builder.add(transform(map));
		}
		return builder.build();
	}
}