package com.dotmarketing.portlets.templates.design.bean;

/**
 * Possible values for a {@link Sidebar}
 */
public enum SidebarWidthValue {
    NONE(0),
    SMALL(20),
    MEDIUM(30),
    LARGE(40);

    private int widthPercent;

    SidebarWidthValue(int widthPercent) {
        this.widthPercent = widthPercent;
    }

    public int getWidthPercent() {
        return widthPercent;
    }
    
    static public SidebarWidthValue fromString(final String in) {
        for(SidebarWidthValue val : SidebarWidthValue.values()) {
            if(val.name().equalsIgnoreCase(in)) {
                return val;
            }
        }
        return NONE;
    }


}
