package com.dotcms.contenttype.transform;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.elasticsearch.common.Nullable;

import com.dotcms.contenttype.model.decorator.FieldDecorator;
import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.field.LegacyFieldTypes;
import com.dotcms.contenttype.util.FieldBuilderUtil;
import com.dotcms.repackage.com.google.common.collect.ImmutableList;
import com.dotmarketing.exception.DotDataException;

public class DbFieldTransformer {

	public static Field transform(final Map<String, Object> map) throws DotDataException {

		String fieldType = (String) map.get("field_type");

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
			@Nullable
			public String relationType() {
				return null;
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

			@Override
			public Class<? extends Field> type() {
				String typeName = (String) map.get("field_type");
				return LegacyFieldTypes.getImplClass(typeName);

			}

			@Override
			public String typeName() {
				// TODO Auto-generated method stub
				return null;
			}

		};

		return transformToImplclass(field);

	}

	public static Field transformToImplclass(Field field) throws DotDataException {

		try {
			FieldBuilder builder = FieldBuilderUtil.resolveBuilder(field);
			return builder.from(field).build();
		} catch (Exception e) {
			throw new DotDataException(e.getMessage(), e);
		}

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