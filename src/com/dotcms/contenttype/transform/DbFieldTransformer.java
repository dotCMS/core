package com.dotcms.contenttype.transform;

import java.util.Date;
import java.util.List;
import java.util.Map;

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
import com.dotmarketing.exception.DotDataException;

public class DbFieldTransformer {

	public static Field transform(final Map<String, Object> map) throws DotDataException {

		String fieldType = (String) map.get("field_type");
		final FieldTypes TYPE = FieldTypes.valueOf(fieldType);
		final Field field = new Field() {
			/**
			 * structure_inode field_type field_relation_type field_contentlet
			 * required indexed listed sort_order default_value fixed read_only
			 * searchable unique_ inode idate type
			 */
			@Override
			public String variable() {
				return map.get("velocity_var_name").toString();
			}

			@Override
			public String values() {
				return map.get("field_values").toString();
			}

			@Override
			protected String type() {
				return "field";
			}

			@Override
			public String relationType() {
				return map.get("field_relation_type").toString();
			}

			@Override
			public String regexCheck() {
				return map.get("regex_check").toString();
			}

			@Override
			public String owner() {
				return map.get("owner").toString();
			}

			@Override
			public String name() {
				return map.get("field_name").toString();
			}

			@Override
			public String inode() {
				return map.get("inode").toString();
			}

			@Override
			public String hint() {
				return map.get("hint").toString();
			}

			@Override
			public String defaultValue() {
				return map.get("default_value").toString();
			}

			@Override
			public DataTypes dataType() {
				String dbType = map.get("field_contentlet").toString().replaceAll("[0-9]", "");
				return DataTypes.valueOf(dbType);
			}

			@Override
			public Date modDate() {
				return (Date) map.get("mod_date");
			}

			@Override
			public boolean required() {
				return Boolean.getBoolean(String.valueOf(map.get("required")));
			}

			@Override
			public int sortOrder() {
				return Integer.getInteger(String.valueOf(map.get("sort_order")));
			}

			@Override
			public boolean indexed() {
				return Boolean.getBoolean(String.valueOf(map.get("indexed")));
			}

			@Override
			public boolean listed() {
				return Boolean.getBoolean(String.valueOf(map.get("listed")));
			}

			@Override
			public boolean fixed() {
				return Boolean.getBoolean(String.valueOf(map.get("fixed")));
			}

			@Override
			public boolean readOnly() {
				return Boolean.getBoolean(String.valueOf(map.get("read_only")));
			}

			@Override
			public boolean searchable() {
				return Boolean.getBoolean(String.valueOf(map.get("searchable")));
			}

			@Override
			public boolean unique() {
				return Boolean.getBoolean(String.valueOf(map.get("unique_")));
			}

			@Override
			public List<FieldDecorator> fieldDecorators() {
				// TODO Auto-generated method stub
				return super.fieldDecorators();
			}

			@Override
			public List<DataTypes> acceptedDataTypes() {
				// TODO Auto-generated method stub
				return super.acceptedDataTypes();
			}

		};

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

		throw new DotDataException("invalid content type");
	}
}

/**
 * Fields in the db inode owner idate type inode name description
 * default_structure page_detail structuretype system fixed velocity_var_name
 * url_map_pattern host folder expire_date_var publish_date_var mod_date
 **/
