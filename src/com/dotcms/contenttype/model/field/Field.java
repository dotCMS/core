package com.dotcms.contenttype.model.field;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.elasticsearch.common.Nullable;
import org.immutables.value.Value;
import org.immutables.value.Value.Derived;

import com.dotcms.contenttype.model.decorator.FieldDecorator;
import com.dotcms.repackage.com.google.common.collect.ImmutableList;
import com.dotcms.repackage.org.apache.commons.lang.time.DateUtils;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.exception.DotDataException;


public abstract class Field implements FieldIf, Serializable {

	@Value.Default
	public  boolean searchable() {
		return false;
	}

	@Value.Default
	public  boolean unique() {
		return false;
	}
	
	@Value.Default
	public   boolean indexed() {
		return false;
	}

	@Value.Default
	public   boolean listed() {
		return false;
	}

	@Value.Default
	public   boolean readOnly() {
		return false;
	}
	
	@Value.Default
	public   boolean onePerContentType() {
		return false;
	}
	
	@Nullable
	public abstract   String owner();

	@Nullable
	public abstract   String inode();

	@Value.Default
	public   Date modDate() {
		return DateUtils.round(new Date(), Calendar.SECOND);
	}

	
	public abstract String name();

	@Derived
	public   String typeName() {
		return LegacyFieldTypes.getImplClass(this.getClass().getCanonicalName()).getCanonicalName();
	}

	@Derived
	public   Class<Field> type() {
		return LegacyFieldTypes.getImplClass(this.getClass().getCanonicalName());
	}

	@Nullable
	public   abstract String relationType();

	@Value.Default
	public   boolean required() {
		return false;
	}

	public abstract String variable();

	@Value.Default
	public   int sortOrder() {
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
	public boolean fixed() {
		return false;
	}
	
	@Value.Lazy
	public List<FieldVariable> fieldVariables(){
		try {
			return FactoryLocator.getFieldFactory2().loadVariables(this);
		} catch (DotDataException e) {
			throw new DotStateException("unable to load field variables:"  +e.getMessage(), e);
		}
	}

	@Value.Default
	public List<FieldDecorator> fieldDecorators() {
		return ImmutableList.of();
	}

	public abstract  List<DataTypes> acceptedDataTypes();

	public abstract  DataTypes dataType();

	@Nullable
	public abstract  String contentTypeId();

	@Nullable
	public  abstract String dbColumn();
	
	@Value.Default
	public  Date iDate() {
		return DateUtils.round(new Date(), Calendar.SECOND);

	}

}
