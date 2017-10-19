package com.dotcms.contenttype.model.field;

import java.lang.reflect.Method;
import java.util.Date;

import com.dotmarketing.business.DotStateException;
import com.dotmarketing.util.Logger;

public interface FieldBuilder {

	// Generated builders will implement this method
	// It is compatible with signature of generated builder methods where
	// return type becomes a Field
	Field build();
	FieldBuilder from(Field field);
	FieldBuilder id(String inode);
	FieldBuilder dbColumn(String dbColumn);
	FieldBuilder modDate(Date date);
	FieldBuilder name(String name);
	FieldBuilder variable(String variable);
	FieldBuilder contentTypeId(String contentTypeId);
	FieldBuilder hint(String hint);
	FieldBuilder defaultValue(String defaultValue);
	FieldBuilder dataType(DataTypes types);
	FieldBuilder required(boolean val);
	FieldBuilder unique(boolean val);
	FieldBuilder searchable(boolean val);
	FieldBuilder sortOrder(int val);
	FieldBuilder fixed(boolean val);
	FieldBuilder readOnly(boolean val);
	FieldBuilder indexed(boolean val);
    FieldBuilder listed(boolean listed);
    FieldBuilder values(String values);
    
	public static FieldBuilder builder(Field field) throws DotStateException{
		return builder(field.type()).from(field);
	}
	
	
	public static FieldBuilder builder(final Class<?> clazz) throws DotStateException{
		try {
			String canon = clazz.getCanonicalName();
			Class<?> tryMe = clazz;
			
			if(!canon.contains(".Immutable")){
				String immutable = canon.substring(0,canon.lastIndexOf(".") ) +".Immutable" +canon.substring(canon.lastIndexOf(".")+1,canon.length() );
				try{
					tryMe = Class.forName(immutable);
				}
				catch(ClassNotFoundException cnfe){
					Logger.debug(FieldBuilder.class, "No immutable class found for field :" + clazz);
				}
			}
			
			
			Method method = tryMe.getMethod("builder");
			return (FieldBuilder) method.invoke(tryMe);
		} catch (Exception e) {
			throw new DotStateException(e.getMessage(),e);
		}
		
	}
	
	
	public static Field instanceOf(Class<?> clazz){
		FieldBuilder builder = builder(clazz);
		builder.name("INSTANCEFIELD");
		builder.variable("INSTANCEFIELD");
		builder.modDate(new Date(0));
		return builder.build();
	}
}
