package com.dotcms.contenttype.model.field;

import java.io.Serializable;

public interface FieldType extends Serializable{
	public Class<? extends Field> type();
	public String typeName();

}
