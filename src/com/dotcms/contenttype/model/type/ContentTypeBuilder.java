package com.dotcms.contenttype.model.type;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.List;

import com.dotcms.contenttype.model.field.Field;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.util.Logger;

public interface ContentTypeBuilder {

	// Generated builders will implement this method
	// It is compatible with signature of generated builder methods where
	// return type becomes a Field
	ContentType build();

	ContentTypeBuilder from(ContentType field);

	ContentTypeBuilder inode(String inode);

	ContentTypeBuilder modDate(Date date);

	ContentTypeBuilder name(String name);

	ContentTypeBuilder description(String variable);

	ContentTypeBuilder defaultStructure(boolean variable);

	ContentTypeBuilder storageType(StorageType variable);

	ContentTypeBuilder pagedetail(String variable);

	ContentTypeBuilder fixed(boolean variable);

	ContentTypeBuilder iDate(Date date);

	ContentTypeBuilder system(boolean variable);

	ContentTypeBuilder versionable(boolean variable);

	ContentTypeBuilder multilingualable(boolean variable);

	ContentTypeBuilder velocityVarName(String variable);

	ContentTypeBuilder urlMapPattern(String variable);

	ContentTypeBuilder publishDateVar(String variable);

	ContentTypeBuilder expireDateVar(String variable);

	ContentTypeBuilder owner(String variable);

	ContentTypeBuilder host(String variable);

	ContentTypeBuilder folder(String variable);
	
	
	public static ContentTypeBuilder builder(ContentType type) throws DotStateException {
		return builder(type.getClass()).from(type);
	}

	public static ContentTypeBuilder builder(final Class clazz) throws DotStateException {
		try {
			String canon = clazz.getCanonicalName();
			Class tryMe = clazz;

			if (!canon.contains(".Immutable")) {
				String immutable = canon.substring(0, canon.lastIndexOf(".")) + ".Immutable"
						+ canon.substring(canon.lastIndexOf(".") + 1, canon.length());
				try {
					tryMe = Class.forName(immutable);
				} catch (ClassNotFoundException cnfe) {
					Logger.debug(ContentTypeBuilder.class, "No immutable class found for field :" + clazz);
				}
			}

			Method method = tryMe.getMethod("builder", null);
			return (ContentTypeBuilder) method.invoke(null, new Object[0]);
		} catch (Exception e) {
			throw new DotStateException(e.getMessage(), e);
		}

	}

	public static ContentType instanceOf(Class clazz) {
		ContentTypeBuilder builder = builder(clazz);
		builder.name("INSTANCETYPE");
		builder.velocityVarName("INSTANCETYPE");
		builder.modDate(new Date(0));
		builder.iDate(new Date(0));
		return builder.build();
	}

}
