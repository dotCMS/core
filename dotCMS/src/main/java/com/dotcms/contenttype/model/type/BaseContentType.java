package com.dotcms.contenttype.model.type;

import com.dotcms.util.CollectionsUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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

	ANY(0,        ContentType.class),
	CONTENT(1,    SimpleContentType.class),
	WIDGET(2,     WidgetContentType.class),
	FORM(3,       FormContentType.class, "Form"),
	FILEASSET(4,  FileAssetContentType.class, "File"),
	HTMLPAGE(5,   PageContentType.class, "Page"),
	PERSONA(6,    PersonaContentType.class, "Persona"),
	VANITY_URL(7, VanityUrlContentType.class, "VanityURL"),
	KEY_VALUE(8,  KeyValueContentType.class, "KeyValue"),
	DOTASSET(9,   DotAssetContentType.class, "DotAsset");


	final int type;
	final String alternateName;
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
		this(type, clazz, null);
	}

	BaseContentType(final int type, final Class<? extends ContentType> clazz, final String alternateName) {
		this.type = type;
		this.immutableClass=clazz;
		this.alternateName = alternateName;
	}

	/**
	 * Gets the integer representation of this Content Type.
	 * 
	 * @return the integer representation
	 */

	public int getType() {
		return type;
	}

	/**
	 * Gets the alternate name
	 * @return String
	 */
	public String getAlternateName() {
		return alternateName != null ? alternateName : name();
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
	
    public static BaseContentType getBaseContentType (final String name) {
        BaseContentType[] types = BaseContentType.values();
        for (BaseContentType type : types) {
            if (type.name().equalsIgnoreCase(name) || isAnAlternateName(type, name)){
                return type;
            }
        }
        final String errorMsg = "BaseContentType " + name + " does not Exist";
		Logger.info(BaseContentType.class, errorMsg);
        throw new IllegalArgumentException(errorMsg);
    }

    private static boolean isAnAlternateName(final BaseContentType type, final String name) {
		return UtilMethods.isSet(type.alternateName) && type.alternateName.equalsIgnoreCase(name);
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

	public static List<BaseContentType> getEnterpriseBaseTypes() {
		return Arrays.stream(BaseContentType.values()).filter(baseType ->
				EnterpriseType.class.isAssignableFrom(baseType.immutableClass()))
				.collect(Collectors.toList());
	}

	public final static Map<BaseContentType, String> iconFallbackMap =
			CollectionsUtils.imap(
					BaseContentType.CONTENT,"event_note",
					BaseContentType.WIDGET,"settings",
					BaseContentType.FILEASSET,"insert_drive_file",
					BaseContentType.DOTASSET,"file_copy",
					BaseContentType.HTMLPAGE,"description",
					BaseContentType.PERSONA,"person",
					BaseContentType.FORM,"format_list_bulleted",
					BaseContentType.VANITY_URL,"format_strikethrough",
					BaseContentType.KEY_VALUE,"public"
			);


    public static Set<BaseContentType> fromNames(List<String> names) {
        if (names == null || names.isEmpty()) {
            return Set.of(BaseContentType.ANY);
        }

        return names.stream()
                .filter(name -> name != null && !name.isBlank())
                .map(name -> BaseContentType.getBaseContentType(name.trim()))
                .collect(Collectors.toSet());
    }

    public static List<BaseContentType> allBaseTypes() {
        return Arrays.asList(BaseContentType.values());
    }

}
