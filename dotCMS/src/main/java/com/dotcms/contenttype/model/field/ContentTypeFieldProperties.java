package com.dotcms.contenttype.model.field;

/**
 * Set of Properties allow for the fields
 *
 * @see Field#getFieldContentTypeProperties
 */
public enum ContentTypeFieldProperties {
    NAME("name"),
    VALUES("values"),
    CATEGORIES("categories"),
    REGEX_CHECK("regexCheck"),
    HINT("hint"),
    REQUIRED("required"),
    SEARCHABLE("searchable"),
    INDEXED("indexed"),
    LISTED("listed"),
    UNIQUE("unique"),
    DEFAULT_VALUE("defaultValue"),
    DATA_TYPE("dataType");

    private final String name;

    ContentTypeFieldProperties(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
