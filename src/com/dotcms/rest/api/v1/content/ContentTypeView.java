package com.dotcms.rest.api.v1.content;

import com.dotmarketing.portlets.structure.model.Structure;

import java.io.Serializable;

/**
 * Encapsulates what is needed for a content let type
 * @author jsanca
 */
public class ContentTypeView implements Serializable {

    private final String type;
    private final String name;
    private final String inode;
    private final String action;

    public ContentTypeView(String type, String name, String inode, String action) {
        this.type   = type;
        this.name   = name;
        this.inode  = inode;
        this.action = action;
    }

    public ContentTypeView (Structure structure, String actionUrl){
        this(Structure.Type.getType(structure.getStructureType()).name(),
                structure.getName(), structure.getInode(), actionUrl);
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

    public String getAction() {
        return action;
    }

    @Override
    public String toString() {
        return "ContentTypeView{" +
                "type='" + type + '\'' +
                ", name='" + name + '\'' +
                ", inode='" + inode + '\'' +
                ", action='" + action + '\'' +
                '}';
    }
} // E:O:F:ContentTypeView.
