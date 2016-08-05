package com.dotcms.rest.api.v1.content;

import java.io.Serializable;

/**
 * Encapsulates what is needed for a content let type
 * @author jsanca
 */
public class ContentletTypeView implements Serializable {

    private final String type;
    private final String name;
    private final String inode;

    public ContentletTypeView(String type, String name, String inode) {
        this.type = type;
        this.name = name;
        this.inode = inode;
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getInode() {
        return inode;
    }

    @Override
    public String toString() {
        return "ContentletTypeView{" +
                "type='" + type + '\'' +
                ", name='" + name + '\'' +
                ", inode='" + inode + '\'' +
                '}';
    }
} // E:O:F:ContentletTypeView.
