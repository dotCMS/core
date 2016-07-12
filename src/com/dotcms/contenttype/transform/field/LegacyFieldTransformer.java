package com.dotcms.contenttype.transform.field;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.elasticsearch.common.Nullable;

import com.dotcms.contenttype.model.decorator.FieldDecorator;
import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.LegacyFieldTypes;
import com.dotcms.repackage.org.apache.commons.lang.time.DateUtils;
import com.dotmarketing.business.DotStateException;
import com.google.common.collect.ImmutableList;

public class LegacyFieldTransformer implements ToFieldTransformer {

	final List<com.dotmarketing.portlets.structure.model.Field> oldFields;

	public LegacyFieldTransformer(com.dotmarketing.portlets.structure.model.Field oldField) {
		this.oldFields = ImmutableList.of(oldField);
	}

	public LegacyFieldTransformer(List<com.dotmarketing.portlets.structure.model.Field> oldField) {
		this.oldFields = oldField;
	}

	public Field from() throws DotStateException {
		if (this.oldFields.size() == 0)
			throw new DotStateException("0 results");

		return fromLegacy(this.oldFields.get(0));

	}

	@Override
	public List<Field> asList() throws DotStateException {

		List<Field> list = new ArrayList<Field>();
		for (com.dotmarketing.portlets.structure.model.Field old : this.oldFields) {
			list.add(fromLegacy(old));
		}

		return ImmutableList.copyOf(list);

	}

	private static Field fromLegacy(com.dotmarketing.portlets.structure.model.Field oldField) {
		final String fieldType = oldField.getFieldType();

		@SuppressWarnings("serial")
		final Field field = new Field() {

			@Override
			public String variable() {
				return oldField.getVelocityVarName();
			}

			@Override
			public String values() {
				return oldField.getValues();
			}

			@Override
			@Nullable
			public String relationType() {
				return null;
			}

			@Override
			public String contentTypeId() {
				return oldField.getStructureInode();
			}

			@Override
			public String regexCheck() {
				return oldField.getRegexCheck();
			}

			@Override
			public String owner() {
				return oldField.getOwner();
			}

			@Override
			public String name() {
				return oldField.getFieldName();
			}

			@Override
			public String inode() {
				return oldField.getInode();
			}

			@Override
			public String hint() {
				return oldField.getHint();
			}

			@Override
			public String defaultValue() {
				return oldField.getDefaultValue();
			}

			@Override
			public DataTypes dataType() {
				String dbType = oldField.getFieldContentlet().replaceAll("[0-9]", "");
				return DataTypes.getDataType(dbType);
			}

			@Override
			public String dbColumn() {
				return oldField.getFieldContentlet();

			}

			@Override
			public Date modDate() {
				return DateUtils.round(new Date(oldField.getModDate().getTime()), Calendar.SECOND);
			}

			@Override
			public Date iDate() {
				if(oldField.getiDate()==null)return null;
				return DateUtils.round(new Date(oldField.getiDate().getTime()), Calendar.SECOND);
				
			}

			@Override
			public boolean required() {
				return oldField.isRequired();
			}

			@Override
			public int sortOrder() {
				return oldField.getSortOrder();

			}

			@Override
			public boolean indexed() {
				return oldField.isIndexed();
			}

			@Override
			public boolean listed() {
				return oldField.isListed();
			}

			@Override
			public boolean fixed() {
				return oldField.isFixed();
			}

			@Override
			public boolean readOnly() {
				return oldField.isReadOnly();
			}

			@Override
			public boolean searchable() {
				return oldField.isSearchable();
			}

			@Override
			public boolean unique() {
				return oldField.isUnique();
			}

			@Override
			public List<FieldDecorator> fieldDecorators() {

				return ImmutableList.of();
			}

			@Override
			public List<DataTypes> acceptedDataTypes() {
				return ImmutableList.of();
			}

			@SuppressWarnings({ "rawtypes", "unchecked" })
			@Override
			public Class type() {
				return LegacyFieldTypes.getImplClass(fieldType);

			}

			@Override
			public String typeName() {
				// TODO Auto-generated method stub
				return null;
			}

		};

		return new ImplClassFieldTransformer(field).from();

	}
}