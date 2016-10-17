package com.dotcms.contenttype.model.relationship;

import java.io.Serializable;

import org.immutables.value.Value;

@Value.Immutable
public abstract class Relationship implements Serializable {


	private static final long serialVersionUID = 1L;

	public abstract String getParentStructureInode();

	public abstract String getChildStructureInode();

	public abstract String getParentRelationName();

	public abstract String getChildRelationName();

	public abstract String getRelationTypeValue();

	@Value.Default
	String getType() {
		return "relationship";
	};

	public abstract int getCardinality();

	public abstract boolean isParentRequired();

	public abstract boolean isChildRequired();

	public abstract boolean isFixed();

}
