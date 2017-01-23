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
import com.dotmarketing.db.DbConnectionFactory;
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
				Date modDate = (Date) map.get("mod_date");
				return modDate!=null?new Date(modDate.getTime()):super.modDate();
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
				return DbConnectionFactory.isDBTrue(map.get("required").toString());
			}

			@Override
			public int sortOrder() {
				return DbConnectionFactory.getInt(map.get("sort_order").toString());
			}

			@Override
			public boolean indexed() {
				return DbConnectionFactory.isDBTrue(map.get("indexed").toString());
			}

			@Override
			public boolean listed() {
                return DbConnectionFactory.isDBTrue(map.get("listed").toString());
			}

			@Override
			public boolean fixed() {
                return DbConnectionFactory.isDBTrue(map.get("fixed").toString());
			}

			@Override
			public boolean readOnly() {
                return DbConnectionFactory.isDBTrue(map.get("read_only").toString());
			}

			@Override
			public boolean searchable() {
                return DbConnectionFactory.isDBTrue(map.get("searchable").toString());
			}

			@Override
			public boolean unique() {
                return DbConnectionFactory.isDBTrue(map.get("unique_").toString());
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