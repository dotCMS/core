package com.dotcms.contenttype.model.field;

/**
 *
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

    private String name;

    ContentTypeFieldProperties(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
