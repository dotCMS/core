package com.dotcms.contenttype.transform.field;

import java.util.Date;
import java.util.List;

import org.elasticsearch.common.Nullable;

import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.LegacyFieldTypes;
import com.dotcms.repackage.com.google.common.collect.ImmutableList;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.portlets.structure.struts.FieldForm;
import com.dotmarketing.util.StringUtils;


public class StrustFieldFormTransformer implements FieldTransformer {

	final FieldForm form;
	final Field oldField;
	public StrustFieldFormTransformer(FieldForm form) {
		this(form, null);
	}

    public StrustFieldFormTransformer(FieldForm form, Field oldField) {
        this.form = form;
        this.oldField=oldField;
    }


	@Override
	public Field from() throws DotStateException {
		if(this.form ==null) throw new DotStateException("0 results");
		return fromForm();

	}

	private Field fromForm() {
	    
	    
		final String fieldType = form.getFieldType();

		@SuppressWarnings("serial")
		final Field field = new Field() {

			@Override
			public String variable() {
				return StringUtils.nullEmptyStr(form.getVelocityVarName());
			}

			@Override
			public String values() {
				return StringUtils.nullEmptyStr(form.getValues());
			}

			@Override
			@Nullable
			public String relationType() {
			    return StringUtils.nullEmptyStr(form.getFieldRelationType());
			}

			@Override
			public String contentTypeId() {
				return StringUtils.nullEmptyStr(form.getStructureInode());
			}

			@Override
			public String regexCheck() {
				return StringUtils.nullEmptyStr(form.getRegexCheck());
			}

			@Override
			public String owner() {
				return null;
			}

			@Override
			public String name() {
				return StringUtils.nullEmptyStr(form.getFieldName());
			}

			@Override
			public String id() {
				return StringUtils.nullEmptyStr(form.getInode());
			}

			@Override
			public String hint() {
				return StringUtils.nullEmptyStr(form.getHint());
			}

			@Override
			public String defaultValue() {
			    return StringUtils.nullEmptyStr(form.getDefaultValue());
		
			}

			@Override
			public DataTypes dataType() {
			    if(form.getFieldContentlet()!=null){
				String dbType = form.getFieldContentlet().toString().replaceAll("[0-9]", "");
				return DataTypes.getDataType(dbType);
			    }
			    return DataTypes.TEXT;
			}

			@Override
			public String dbColumn() {
				return StringUtils.nullEmptyStr(form.getFieldContentlet());

			}

			@Override
			public Date modDate() {
				return new Date();
			}
			@Override
			public void check() {
				//no checking for a generic type
			}
			
			@Override
			public Date iDate() {

				return null;
			}

			@Override
			public boolean required() {

				return (boolean) form.isRequired();
			}

			@Override
			public int sortOrder() {
				return (int)form.getSortOrder();

			}

			@Override
			public boolean indexed() {
				return (boolean)form.isIndexed();
			}

			@Override
			public boolean listed() {
				return (boolean) form.isListed();
			}

			@Override
			public boolean fixed() {
				return (boolean) form.isFixed();
			}

			@Override
			public boolean readOnly() {
				return (boolean) form.isReadOnly();

			}

			@Override
			public boolean searchable() {
				return (Boolean) form.isSearchable();

			}

			@Override
			public boolean unique() {
				return (Boolean) form.isUnique();

			}


			@Override
			public List<DataTypes> acceptedDataTypes() {
				return ImmutableList.of();
			}

			@Override
			public Class type() {
				return LegacyFieldTypes.getImplClass(fieldType);

			}

			@Override
			public String typeName() {
				return null;
			}

		};

		return new ImplClassFieldTransformer(field).from();

	}

	@Override
	public List<Field> asList() throws DotStateException {


		return ImmutableList.of(fromForm());
	}
	
	
	

}