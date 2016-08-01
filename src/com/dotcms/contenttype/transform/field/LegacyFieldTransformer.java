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

public class LegacyFieldTransformer implements LegacyFieldTransformerIf {

	final List<com.dotmarketing.portlets.structure.model.Field> oldFields;
	final List<Field> newFields;

	public LegacyFieldTransformer(com.dotmarketing.portlets.structure.model.Field oldField) {
		this.oldFields = ImmutableList.of(oldField);
		this.newFields = ImmutableList.of(transformToNew(oldField));
	}

	public LegacyFieldTransformer(List<com.dotmarketing.portlets.structure.model.Field> oldFields) {
		this.oldFields = ImmutableList.copyOf(oldFields);
		List<Field> newList = new ArrayList<Field>();
		for (com.dotmarketing.portlets.structure.model.Field field : oldFields) {
			newList.add(transformToNew(field));
		}
		this.newFields = ImmutableList.copyOf(newList);
	}

	public LegacyFieldTransformer(Field newField) {
		this.oldFields = ImmutableList.of(transformToOld(newField));
		this.newFields = ImmutableList.of(newField);
	}

	public LegacyFieldTransformer(List<Field> newFields, boolean onlyNewFields) {
		this.newFields = ImmutableList.copyOf(newFields);
		List<com.dotmarketing.portlets.structure.model.Field> oldList = new ArrayList<com.dotmarketing.portlets.structure.model.Field>();
		for (Field field : newFields) {
			oldList.add(transformToOld(field));
		}
		this.oldFields = ImmutableList.copyOf(oldList);
	}

	public Field from() throws DotStateException {
		if (this.newFields.size() == 0)
			throw new DotStateException("0 results");

		return this.newFields.get(0);

	}

	@Override
	public List<Field> asList() throws DotStateException {

		return this.newFields;

	}

	private static com.dotmarketing.portlets.structure.model.Field transformToOld(Field field) {

		com.dotmarketing.portlets.structure.model.Field old = new com.dotmarketing.portlets.structure.model.Field();
		old.setDefaultValue(field.defaultValue());
		old.setFieldContentlet(field.dbColumn());
		old.setFieldName(field.name());
		old.setFieldRelationType(field.relationType());
		old.setFieldType(field.typeName());
		old.setFixed(field.fixed());
		old.setHint(field.hint());
		old.setiDate(field.iDate());
		old.setIdentifier(field.inode());
		old.setIndexed(field.indexed());
		old.setInode(field.inode());
		old.setListed(field.listed());
		old.setModDate(field.modDate());
		old.setOwner(field.owner());
		old.setReadOnly(field.readOnly());
		old.setRegexCheck(field.regexCheck());
		old.setRequired(field.required());
		old.setSearchable(field.searchable());
		old.setSortOrder(field.sortOrder());
		old.setStructureInode(field.contentTypeId());
		old.setUnique(field.unique());
		old.setValues(field.values());
		old.setVelocityVarName(field.variable());
		return old;

	}

	private static Field transformToNew(com.dotmarketing.portlets.structure.model.Field oldField) {
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
				if (oldField.getiDate() == null)
					return null;
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

	@Override
	public com.dotmarketing.portlets.structure.model.Field asOldField() throws DotStateException {
		return oldFields.get(0);
	}

	@Override
	public List<com.dotmarketing.portlets.structure.model.Field> asOldFieldList() throws DotStateException {
		return oldFields;
	}

}