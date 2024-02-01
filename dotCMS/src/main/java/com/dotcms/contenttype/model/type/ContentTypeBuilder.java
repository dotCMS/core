package com.dotcms.contenttype.model.type;

import com.dotmarketing.business.DotStateException;
import com.dotmarketing.util.Logger;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.Map;

/**
 * This Builder interface allows dotCMS to create immutable Content Type objects.
 *
 * @author Will Ezell
 * @since Jul 7th, 2016
 */
public interface ContentTypeBuilder {

	// Generated builders will implement this method
	// It is compatible with signature of generated builder methods where
	// return type becomes a Field
	ContentType build();

	ContentTypeBuilder from(ContentType field);

	ContentTypeBuilder id(String inode);

	ContentTypeBuilder modDate(Date date);

	ContentTypeBuilder name(String name);

	ContentTypeBuilder description(String variable);

	ContentTypeBuilder defaultType(boolean variable);

	ContentTypeBuilder detailPage(String variable);

	ContentTypeBuilder fixed(boolean variable);

	ContentTypeBuilder iDate(Date date);

	ContentTypeBuilder system(boolean variable);

	ContentTypeBuilder versionable(boolean variable);

	ContentTypeBuilder multilingualable(boolean variable);

	ContentTypeBuilder variable(String variable);

	ContentTypeBuilder urlMapPattern(String variable);

	ContentTypeBuilder publishDateVar(String variable);

	ContentTypeBuilder expireDateVar(String variable);

	ContentTypeBuilder owner(String variable);

	ContentTypeBuilder host(String hostIdentifier);

	ContentTypeBuilder siteName(String siteName);

	ContentTypeBuilder folder(String variable);

	ContentTypeBuilder folderPath(String path);

	ContentTypeBuilder icon(String variable);

	ContentTypeBuilder sortOrder(int variable);

	/**
	 * if used the content type will be marked for deletion and will be deleted
	 * Using this only makes sense when creating a mock content type that will be used to temporarily hold contentlets that are about to be deleted
	 * @param variable
	 * @return
	 */
	ContentTypeBuilder markedForDeletion(boolean variable);

	/**
	 * Allows a Content Type to store additional or temporary information associated to a given
	 * Content Type. Developers can save any attributes they deem necessary.
	 *
	 * @param metadata A Map of key/value pairs to be stored as metadata
	 *
	 * @return The current {@link ContentTypeBuilder} instance.
	 */
	ContentTypeBuilder metadata(final Map<String, ? extends Object> metadata);

	static ContentTypeBuilder builder(ContentType type) throws DotStateException {
		return builder(type.getClass()).from(type);
	}

	static ContentTypeBuilder builder(final Class clazz) throws DotStateException {
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

			Method method = tryMe.getMethod("builder");
			return (ContentTypeBuilder) method.invoke(tryMe);
		} catch (Exception e) {
			throw new DotStateException(e.getMessage(), e);
		}

	}

	static ContentType instanceOf(Class clazz) {
		ContentTypeBuilder builder = builder(clazz);
		builder.name("INSTANCETYPE");
		builder.variable("INSTANCETYPE");
		builder.modDate(new Date(0));
		builder.iDate(new Date(0));
		return builder.build();
	}

}
