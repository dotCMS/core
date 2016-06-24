package com.dotcms.contenttype.model;

import java.io.Serializable;
import java.util.Date;

import org.immutables.value.Value;

@Value.Immutable
public abstract class Field implements Serializable{


	private static final long serialVersionUID = 1L;

	public abstract String getType();

	public abstract String getOwner();

	public abstract String getInode();

	public abstract Date getIDate();

	public abstract String getIdentifier();

	public abstract String getStructureInode();

	public abstract String getFieldName();

	public abstract String getFieldType();

	public abstract String getFieldRelationType();

	public abstract String getFieldContentlet();

	public abstract boolean isRequired();

	public abstract String getVelocityVarName();

	public abstract int getSortOrder();

	public abstract String getValues();

	public abstract String getRegexCheck();

	public abstract String getHint();

	public abstract String getDefaultValue();

	public abstract boolean isIndexed();

	public abstract boolean isListed();

	public abstract boolean isFixed();

	public abstract boolean isReadOnly();

	public abstract boolean isSearchable();

	public abstract boolean isUnique();


}
