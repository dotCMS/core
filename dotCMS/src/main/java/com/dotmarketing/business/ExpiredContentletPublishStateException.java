package com.dotmarketing.business;

import com.dotmarketing.portlets.contentlet.model.Contentlet;

public class ExpiredContentletPublishStateException extends PublishStateException {
    private Contentlet contentlet;

    public ExpiredContentletPublishStateException(final Contentlet contentlet) {
        super("\"The content cannot be published because the expire date has already passed.\"");

        this.contentlet = contentlet;
    }

    public Contentlet getContentlet() {
        return contentlet;
    }
}
