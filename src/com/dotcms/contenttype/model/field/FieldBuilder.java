package com.dotcms.contenttype.model.field;

import java.util.Date;

public interface FieldBuilder {

	// Generated builders will implement this method
	// It is compatible with signature of generated builder methods where
	// return type becomes a Field
	Field build();
	FieldBuilder from(Field field);
	FieldBuilder inode(String inode);
	FieldBuilder dbColumn(String dbColumn);
	FieldBuilder modDate(Date date);
	FieldBuilder name(String name);
	FieldBuilder variable(String variable);
	FieldBuilder contentTypeId(String contentTypeId);
	FieldBuilder hint(String hint);
	FieldBuilder dataType(DataTypes types);
	FieldBuilder required(boolean val);
	FieldBuilder listed(boolean val);
	FieldBuilder indexed(boolean val);
	FieldBuilder sortOrder(int val);
	FieldBuilder fixed(boolean val);
	FieldBuilder searchable(boolean val);
	FieldBuilder readOnly(boolean val);
	
}
