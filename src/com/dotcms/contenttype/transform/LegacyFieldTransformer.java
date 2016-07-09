package com.dotcms.contenttype.transform;

import java.util.Date;
import java.util.List;

import org.elasticsearch.common.Nullable;

import com.dotcms.contenttype.model.decorator.FieldDecorator;
import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.field.LegacyFieldTypes;
import com.dotcms.repackage.com.google.common.collect.ImmutableList;
import com.dotmarketing.exception.DotDataException;

public class LegacyFieldTransformer {

	public static Field transform(final com.dotmarketing.portlets.structure.model.Field legacy) throws DotDataException {

		String fieldType = legacy.getType();

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
				return legacy.getVelocityVarName();
			}

			@Override
			public String values() {
				return legacy.getValues();
			}

			@Override
			@Nullable
			public String relationType() {
				return legacy.getFieldRelationType();
			}

			@Override
			public String contentTypeId() {
				return legacy.getStructureInode();
			}

			@Override
			public String regexCheck() {
				return legacy.getRegexCheck();
			}

			@Override
			public String owner() {
				return legacy.getOwner();
			}

			@Override
			public String name() {
				return legacy.getFieldName();
			}

			@Override
			public String inode() {
				return legacy.getInode();
			}

			@Override
			public String hint() {
				return legacy.getHint();
			}

			@Override
			public String defaultValue() {
				return legacy.getDefaultValue();
			}

			@Override
			public DataTypes dataType() {
				String dbType = legacy.getFieldContentlet().replaceAll("[0-9]", "");
				return DataTypes.getDataType(dbType);
			}

			@Override
			public String dbColumn() {
				return (String) legacy.getFieldContentlet();

			}

			@Override
			public Date modDate() {
				return legacy.getModDate();
			}

			@Override
			public Date iDate() {
				return legacy.getIDate();
			}

			@Override
			public boolean required() {
				return legacy.isRequired();
			}

			@Override
			public int sortOrder() {
				return legacy.getSortOrder();

			}

			@Override
			public boolean indexed() {
				return legacy.isIndexed();
			}

			@Override
			public boolean listed() {
				return legacy.isListed();
			}

			@Override
			public boolean fixed() {
				return legacy.isFixed();
			}

			@Override
			public boolean readOnly() {
				return legacy.isReadOnly();
			}

			@Override
			public boolean searchable() {
				return legacy.isSearchable();
			}

			@Override
			public boolean unique() {
				return legacy.isUnique();
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
			public Class type() {
				String typeName = fieldType;
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
			FieldBuilder builder = FieldBuilder.builder(field);
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

	public static List<Field> transform(final List<com.dotmarketing.portlets.structure.model.Field> list) throws DotDataException {

		ImmutableList.Builder<Field> builder = ImmutableList.builder();
		for (com.dotmarketing.portlets.structure.model.Field oldField : list) {
			builder.add(transform(oldField));
		}
		return builder.build();
	}
}