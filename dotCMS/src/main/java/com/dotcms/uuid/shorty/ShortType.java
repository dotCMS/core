package com.dotcms.uuid.shorty;

public enum ShortType {

    CACHE_MISS("cache_miss"), 
    CONTENTLET("contentlet"), 
    CONTAINER("containers"), 
    FOLDER( "folder"), 
    HTMLPAGE( "htmlpage"), 
    LINKS("links"), 
    TEMPLATES("template"), 
    CATEGORY("category"),
    FILED("field"),
    RELATIONSHIP("relationship"),
    STRUCTURE("structure"),
    IDENTIFIER("identifier"),
    INODE("inode"),
    WORKFLOW_SCHEME("workflow_scheme"),
    WORKFLOW_STEP("workflow_step"),
    WORKFLOW_ACTION("workflow_action"),
    TEMP_FILE("temp_file");
    final String shortType;

    private ShortType(String type) {
        this.shortType = type;
    }


    public String getShortType() {
        return shortType;
    }

    public static ShortType fromString(String val) {
        if (val != null) {
            ShortType[] types = ShortType.values();
            for (ShortType type : types) {
                if (type.shortType.equals(val.toLowerCase())) {
                    return type;
                }
            }
        }
        return CACHE_MISS;
    }



}
