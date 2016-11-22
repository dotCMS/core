package com.dotcms.contenttype.transform.field;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.elasticsearch.common.Nullable;

import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.LegacyFieldTypes;
import com.dotcms.repackage.com.google.common.collect.ImmutableList;
import com.dotcms.repackage.org.apache.commons.lang.time.DateUtils;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.util.StringUtils;


public class DbFieldTransformer implements FieldTransformer {

	final List<Map<String, Object>> results;

	public DbFieldTransformer(Map<String, Object> map) {
		this.results = ImmutableList.of(map);
	}

	public DbFieldTransformer(List<Map<String, Object>> results) {
		this.results = results;
	}

	@Override
	public Field from() throws DotStateException {
		if(this.results.size()==0) throw new DotStateException("0 results");
		return fromMap(results.get(0));

	}

	private static Field fromMap(Map<String, Object> map) {
		final String fieldType = (String) map.get("field_type");

		@SuppressWarnings("serial")
		final Field field = new Field() {

			@Override
			public String variable() {
				return StringUtils.nullEmptyStr((String) map.get("velocity_var_name"));
			}

			@Override
			public String values() {
				return StringUtils.nullEmptyStr((String) map.get("field_values"));
			}

			@Override
			@Nullable
			public String relationType() {
				return null;
			}

			@Override
			public String contentTypeId() {
				return StringUtils.nullEmptyStr((String) map.get("structure_inode"));
			}

			@Override
			public String regexCheck() {
				return StringUtils.nullEmptyStr((String) map.get("regex_check"));
			}

			@Override
			public String owner() {
				return StringUtils.nullEmptyStr((String) map.get("owner"));
			}

			@Override
			public String name() {
				return StringUtils.nullEmptyStr((String) map.get("field_name"));
			}

			@Override
			public String id() {
				return StringUtils.nullEmptyStr((String) map.get("inode"));
			}

			@Override
			public String hint() {
				return StringUtils.nullEmptyStr((String) map.get("hint"));
			}

			@Override
			public String defaultValue() {
			    return StringUtils.nullEmptyStr((String) map.get("default_value"));
		
			}

			@Override
			public DataTypes dataType() {
				String dbType = map.get("field_contentlet").toString().replaceAll("[0-9]", "");
				return DataTypes.getDataType(dbType);
			}

			@Override
			public String dbColumn() {
				return StringUtils.nullEmptyStr((String) map.get("field_contentlet"));

			}

			@Override
			public Date modDate() {
				return new Date(((Date) map.get("mod_date")).getTime());
			}
			@Override
			public void check() {
				//no checking for a generic type
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
		List<Field> list = new ArrayList<Field>();
		for (Map<String, Object> map : results) {
		    Field f  = fromMap(map);
			list.add(fromMap(map));
		}

		return ImmutableList.copyOf(list);
	}
	
	
	

}