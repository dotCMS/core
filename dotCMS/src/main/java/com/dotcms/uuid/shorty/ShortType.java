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
    USER_PROXY("user_proxy"),
    VIRTUAL_LINK("virtual_link"),
    IDENTIFIER("identifier"),
    INODE("inode");
    
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
