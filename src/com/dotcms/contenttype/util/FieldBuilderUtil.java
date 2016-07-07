package com.dotcms.contenttype.util;

import java.lang.reflect.Method;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotmarketing.business.DotStateException;

public class FieldBuilderUtil {
	
	public static FieldBuilder resolveBuilder(Field field) throws DotStateException{
		return resolveBuilder(field.type());
	}
	
	
	public static FieldBuilder resolveBuilder(Class clazz) throws DotStateException{
		try {
			String canon = clazz.getCanonicalName();
			
			
			
			
			String immutable = canon.substring(0,canon.lastIndexOf(".") ) +".Immutable" +canon.substring(canon.lastIndexOf(".")+1,canon.length() );

			clazz = Class.forName(immutable);
			Method method = clazz.getMethod("builder", null);

			return (FieldBuilder) method.invoke(null, new Object[0]);

		} catch (Exception e) {
			throw new DotStateException(e.getMessage(),e);
		}
		
	}
	
	
	public static Field instanceOf(Class clazz){
		FieldBuilder builder = resolveBuilder(clazz);
		return builder.build();
		
		
	}
	
}
