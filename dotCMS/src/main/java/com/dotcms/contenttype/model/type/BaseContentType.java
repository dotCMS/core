package com.dotcms.contenttype.model.type;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Provides the different Content Types that can be used inside dotCMS. A
 * Content Type allows users to shape content in the application, i.e., the
 * fields used by authors to add content or information to their sites.
 * 
 * @author Will Ezell
 * @version 4.1.0
 * @since Oct 17, 2016
 *
 */
public enum BaseContentType {

	ANY(0, ContentType.class),
	CONTENT(1, SimpleContentType.class),
	WIDGET(2, WidgetContentType.class),
	FORM(3, FormContentType.class),
	FILEASSET(4, FileAssetContentType.class),
	HTMLPAGE(5, PageContentType.class),
	PERSONA(6, PersonaContentType.class),
	VANITY_URL(7, VanityUrlContentType.class),
	KEY_VALUE(8, KeyValueContentType.class);


	final int type;
	Class<? extends ContentType> immutableClass;

	/**
	 * Enum's constructor.
	 * 
	 * @param type
	 *            - The integer representation of the Content Type.
	 * @param clazz
	 *            - The class of the specific Content Type.
	 */
	BaseContentType(int type, Class<? extends ContentType> clazz) {
		this.type = type;
		this.immutableClass=clazz;
	}

	/**
	 * Gets the integer representation of this Content Type.
	 * 
	 * @return the integer representation
	 */
	@JsonValue
	public int getType() {
		return type;
	}

	/**
	 * Returns the immutable class associated to the specified Content Type.
	 * 
	 * @return The immutable class.
	 *
	 * @return
	 */
	public Class<? extends ContentType> immutableClass() {
		return immutableClass;
	}

	/**
	 * Returns the appropriate Content Type based on its integer representation.
	 * 
	 * @param value
	 *            - The numeric representation of a Content Type.
	 * @return The associated Content Type.
	 *
	 * @param value
	 * @return
	 */
	public static BaseContentType getBaseContentType (int value) {
		BaseContentType[] types = BaseContentType.values();
		for (BaseContentType type : types) {
			if (type.type==value){
				return type;
			}
		}
		return ANY;
	}

	/**
	 * Returns the appropriate immutable Content Type based on its integer
	 * representation.
	 * 
	 * @param value
	 *            - The numeric representation of an immutable Content Type.
	 * @return The associated immutable Content Type.
	 *
	 * @param value
	 * @return
	 */
	public static Class<? extends ContentType> getContentTypeClass (int value) {
		BaseContentType[] types = BaseContentType.values();
		for (BaseContentType type : types) {
			if (type.type==value){
				return type.immutableClass;
			}
		}
		return ANY.immutableClass;
	}

}
