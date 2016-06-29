package com.dotcms.contenttype.model.field;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import org.elasticsearch.common.Nullable;
import org.immutables.value.Value;

import com.dotcms.contenttype.model.decorator.FieldDecorator;
import com.dotcms.repackage.com.google.common.collect.ImmutableList;

public abstract class Field implements Serializable {

	private static final long serialVersionUID = 1L;

	protected abstract String type();

	public abstract String owner();

	@Nullable
	abstract public String inode();

	@Value.Default
	public Date modDate() {
		return new Date();
	}

	public abstract String name();

	@Nullable
	public abstract String relationType();

	public boolean required() {
		return false;
	}

	public abstract String variable();

	@Value.Default
	public int sortOrder() {

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
	public boolean indexed() {
		return false;
	}

	@Value.Default
	public boolean listed() {
		return false;
	}

	@Value.Default
	public boolean fixed() {
		return false;
	}

	@Value.Default
	public boolean readOnly() {
		return false;
	}

	@Value.Default
	public boolean searchable() {
		return false;
	}

	@Value.Default
	public boolean unique() {
		return false;
	}
	@Value.Default
	public List<FieldDecorator> fieldDecorators() {
		return ImmutableList.of();
	}
	
	
	@Value.Default
	public List<DataTypes> acceptedDataTypes(){
		return ImmutableList.of();
	}

	public abstract DataTypes dataType();

}
