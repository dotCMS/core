package com.dotcms.api.system.event;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotmarketing.portlets.structure.model.Structure;

/**
 * It is a wrapper to store the Content Type's action url when a event is push
 */
public class ContentTypePayloadDataWrapper implements DataWrapper<ContentType> {
    private String actionUrl;
    private ContentType type;

    public ContentTypePayloadDataWrapper(String actionUrl, ContentType type) {
        this.actionUrl = actionUrl;
        this.type = type;
    }

    public String getActionUrl() {
        return actionUrl;
    }


    public ContentType getContentType() {
        return type;
    }

    @Override
    public ContentType getData() {
        return type;
    }
}
