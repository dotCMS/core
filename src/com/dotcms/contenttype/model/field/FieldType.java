package com.dotcms.contenttype.model.field;

public interface FieldType {
	public Class<? extends Field> type();
	public String typeName();

}
