package com.dotcms.contenttype.model.field;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.elasticsearch.common.Nullable;
import org.immutables.value.Value;
import org.immutables.value.Value.Derived;

import com.dotcms.contenttype.model.decorator.FieldDecorator;
import com.dotcms.repackage.com.google.common.collect.ImmutableList;
import com.dotcms.repackage.org.apache.commons.lang.time.DateUtils;


public interface Field extends FieldType {

	@Value.Default
	default boolean searchable() {
		return false;
	}

	@Value.Default
	default boolean unique() {
		return false;
	}
	
	@Value.Default
	default boolean indexed() {
		return false;
	}

	@Value.Default
	default boolean listed() {
		return false;
	}

	@Value.Default
	default boolean readOnly() {
		return false;
	}
	
	@Nullable
	public  String owner();

	@Nullable
	public  String inode();

	@Value.Default
	default Date modDate() {
		return new Date();
	}

	
	public abstract String name();

	@Derived
	default String typeName() {
		return LegacyFieldTypes.getImplClass(this.getClass().getCanonicalName()).getCanonicalName();
	}

	@Derived
	default Class<Field> type() {
		return LegacyFieldTypes.getImplClass(this.getClass().getCanonicalName());
	}

	@Nullable
	public abstract String relationType();

	@Value.Default
	default boolean required() {
		return false;
	}

	public abstract String variable();

	@Value.Default
	default int sortOrder() {
		return (int) (System.currentTimeMillis() / 1000);
	}

	@Nullable
	public abstract String values();

	@Nullable
	public abstract String regexCheck();

	@Nullable
	public abstract String hint();

	@Nullable
	public abstract String defaultValue();


	@Value.Default
	default boolean fixed() {
		return false;
	}



	@Value.Default
	default List<FieldDecorator> fieldDecorators() {
		return ImmutableList.of();
	}

	public List<DataTypes> acceptedDataTypes();

	public DataTypes dataType();

	@Nullable
	public String contentTypeId();

	@Nullable
	public abstract String dbColumn();
	
	@Value.Default
	default Date iDate() {
		return DateUtils.round(new Date(), Calendar.SECOND);

	}

}
