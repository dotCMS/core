package com.dotcms.contenttype.model.event;

import com.dotcms.contenttype.model.type.ContentType;

public class ContentTypeDeletedEvent {

    private final ContentType contentType;

    public ContentTypeDeletedEvent(final ContentType contentType) {
        this.contentType = contentType;
    }

    public String getContentTypeVar() {
        return contentType.variable();
    }

    public ContentType getContentType() {
        return contentType;
    }
}
