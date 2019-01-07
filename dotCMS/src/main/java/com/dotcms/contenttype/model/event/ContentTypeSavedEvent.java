package com.dotcms.contenttype.model.event;

import com.dotcms.contenttype.model.type.ContentType;

public class ContentTypeSavedEvent {
    private ContentType contentType;

    public ContentTypeSavedEvent(final ContentType contentType) {
        this.contentType = contentType;
    }

    public ContentType getContentType() {
        return contentType;
    }
}
