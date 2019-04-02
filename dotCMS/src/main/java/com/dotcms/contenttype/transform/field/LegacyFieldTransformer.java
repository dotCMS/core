package com.dotcms.contenttype.transform.field;

import com.dotcms.contenttype.model.field.*;
import com.dotcms.repackage.com.google.common.collect.ImmutableList;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.util.StringUtils;
import com.dotmarketing.util.UtilMethods;
import org.elasticsearch.common.Nullable;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class LegacyFieldTransformer implements FieldTransformer {

	final List<com.dotmarketing.portlets.structure.model.Field> oldFields;
	final List<Field> newFields;

	public LegacyFieldTransformer(com.dotmarketing.portlets.structure.model.Field oldField) {
		this(ImmutableList.of(oldField));

	}

	public LegacyFieldTransformer(Field newField) {
		this(ImmutableList.of(newField));
	}

	public LegacyFieldTransformer(List<? extends FieldIf> newFields) {
		
		List<Field> news = new ArrayList<Field>();
		List<com.dotmarketing.portlets.structure.model.Field> olds = new ArrayList<com.dotmarketing.portlets.structure.model.Field>();
		
		for(FieldIf field : newFields){
			if(field instanceof Field){
				olds.add(transformToOld((Field) field));
				news.add((Field)field);
			}
			else{
				olds.add((com.dotmarketing.portlets.structure.model.Field) field);
				news.add(transformToNew((com.dotmarketing.portlets.structure.model.Field) field));
			}
		}
		
		this.newFields = ImmutableList.copyOf(news);
		this.oldFields = ImmutableList.copyOf(olds);
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
		old.setFieldContentlet(LegacyFieldTransformer.buildLegacyFieldContent(field));
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

	public static String buildLegacyFieldContent(Field field){
		String fieldContent = (field instanceof BinaryField)
		    ? fieldContent = "binary" + field.sortOrder() 
		    : (field.dbColumn() !=null)
		      ? field.dbColumn()
		          : " system_field";
		return fieldContent;
	}
	
    private static String buildNewFieldDbColumn(com.dotmarketing.portlets.structure.model.Field oldField){
      String fieldContent = (oldField.getFieldContentlet()!=null)
          ?  (oldField.getFieldContentlet().startsWith("binary"))
              ? "system_field"
              :  oldField.getFieldContentlet()
                : null;

      return fieldContent;
    }

    private static Field transformToNew(final com.dotmarketing.portlets.structure.model.Field oldField) {
		final String fieldType = oldField.getFieldType();

		@SuppressWarnings("serial")
		final Field field = new Field() {

			@Override
			public String variable() {
				return StringUtils.nullEmptyStr(oldField.getVelocityVarName());
			}

			@Override
			public String values() {
				return StringUtils.nullEmptyStr(oldField.getValues());
			}

			@Override
			@Nullable
			public String relationType() {
				return StringUtils.nullEmptyStr(oldField.getFieldRelationType());
			}

			@Override
			public String contentTypeId() {
				return StringUtils.nullEmptyStr(oldField.getStructureInode());
			}

			@Override
			public String regexCheck() {
				return StringUtils.nullEmptyStr(oldField.getRegexCheck());
			}

			@Override
			public String owner() {
				return StringUtils.nullEmptyStr(oldField.getOwner());
			}

			@Override
			public String name() {
				return StringUtils.nullEmptyStr(oldField.getFieldName());
			}

			@Override
			public String id() {
				return StringUtils.nullEmptyStr(oldField.getInode());
			}

			@Override
			public String hint() {
				return StringUtils.nullEmptyStr(oldField.getHint());
			}

			@Override
			public String defaultValue() {
				return StringUtils.nullEmptyStr(oldField.getDefaultValue());
			}

			@Override
			public DataTypes dataType() {
				String dbType = (oldField.getFieldContentlet()!=null) ? oldField.getFieldContentlet().replaceAll("[0-9]", "") : null;
				if(!UtilMethods.isSet(dbType)){
				   return FieldBuilder.instanceOf(LegacyFieldTypes.getImplClass(fieldType)).acceptedDataTypes().get(0);
				}
				return DataTypes.getDataType(dbType);
			}

			@Override
			public String dbColumn() {
				return StringUtils.nullEmptyStr(oldField.getFieldContentlet());

			}

			@Override
			public Date modDate() {
				return new Date(oldField.getModDate().getTime());
			}

			@Override
			public Date iDate() {
				if (oldField.getiDate() == null)
					return null;
				return new Date(oldField.getiDate().getTime());

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


	public com.dotmarketing.portlets.structure.model.Field asOldField() throws DotStateException {
		return oldFields.get(0);
	}


	public List<com.dotmarketing.portlets.structure.model.Field> asOldFieldList() throws DotStateException {
		return oldFields;
	}

}