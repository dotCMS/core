package com.dotcms.contenttype.transform.field;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.elasticsearch.common.Nullable;

import com.dotcms.contenttype.model.decorator.FieldDecorator;
import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.LegacyFieldTypes;
import com.dotcms.repackage.com.google.common.collect.ImmutableList;
import com.dotcms.repackage.org.apache.commons.lang.time.DateUtils;
import com.dotmarketing.business.DotStateException;

public class DbFieldTransformer implements ToFieldTransformer {

	final List<Map<String, Object>> results;

	public DbFieldTransformer(Map<String, Object> map) {
		this.results = ImmutableList.of(map);
	}

	public DbFieldTransformer(List<Map<String, Object>> results) {
		this.results = results;
	}

	@Override
	public Field from() throws DotStateException {
		if(results.size()==0) throw new DotStateException("0 results");
		return fromMap(results.get(0));

	}

	private Field fromMap(Map<String, Object> map) {
		final String fieldType = (String) map.get("field_type");

		@SuppressWarnings("serial")
		final Field field = new Field() {

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

				Date idate = (Date) map.get("idate");
				if (idate != null) {
					return DateUtils.round(new Date(idate.getTime()), Calendar.SECOND);

				}
				return null;
			}

			@Override
			public boolean required() {
				Object x  =map.get("required");
				return (Boolean) map.get("required");
			}

			@Override
			public int sortOrder() {
				return (Integer) map.get("sort_order");

			}

			@Override
			public boolean indexed() {
				return (Boolean) map.get("indexed");
			}

			@Override
			public boolean listed() {
				return (Boolean) map.get("listed");
			}

			@Override
			public boolean fixed() {
				return (Boolean) map.get("fixed");
			}

			@Override
			public boolean readOnly() {
				return (Boolean) map.get("read_only");

			}

			@Override
			public boolean searchable() {
				return (Boolean) map.get("searchable");

			}

			@Override
			public boolean unique() {
				return (Boolean) map.get("unique_");

			}

			@Override
			public List<FieldDecorator> fieldDecorators() {

				return ImmutableList.of();
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
				// TODO Auto-generated method stub
				return null;
			}

		};

		return new ImplClassFieldTransformer(field).from();

	}

	@Override
	public List<Field> asList() throws DotStateException {
		List<Field> list = new ArrayList<Field>();
		for (Map<String, Object> map : results) {
			list.add(fromMap(map));
		}

		return ImmutableList.copyOf(list);
	}
	
	
	

}