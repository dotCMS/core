package com.dotcms.rest.api.v1.contenttype;

import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
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
    private String variable;

    public ContentTypeView(String type, String name, String inode, String action, String variable) {
        this.type   = type;
        this.name   = name;
        this.inode  = inode;
        this.action = action;
        this.variable = variable;
    }

    public ContentTypeView (ContentType type, String actionUrl){
        this(type.baseType().toString(),type.name(), type.id(), actionUrl, type.variable());
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

    public String getVariable() {
        return variable;
    }

    @Override
    public String toString() {
        return "ContentTypeView{" +
                "type='" + type + '\'' +
                ", name='" + name + '\'' +
                ", inode='" + inode + '\'' +
                ", action='" + action + '\'' +
                ", variable='" + variable + '\'' +
                '}';
    }
} // E:O:F:ContentTypeView.
