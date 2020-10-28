package com.dotmarketing.beans;

/**
 * Encapsulates the icon names for diff types.
 * @author jsanca
 */
public enum IconType {

    FOLDER("folderIcon"),
    CONTENT("contentIcon"),
    WIDGET("gearIcon"),
    FORM("formIcon"),
    HTMLPAGE("pageIcon"),
    KEY_VALUE("keyValueIcon"),
    PERSONA("personaIcon"),
    VANITY_URL("vanityIcon"),
    UNKNOWN("uknIcon");

    private final String iconName;

    IconType(final String iconName) {
        this.iconName = iconName;
    }

    public String iconName () {
        return iconName;
    }
}
