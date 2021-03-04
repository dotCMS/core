package com.dotmarketing.business;

import com.dotmarketing.portlets.contentlet.model.Contentlet;

/**
 * Throw when try to publish a page with a content with a future publish date
 */
public class FutureContentletPublishStateException extends PublishStateException{

    private Contentlet contentlet;

    public FutureContentletPublishStateException(final Contentlet contentlet) {
        super("The content cannot be published because it is scheduled to be published on future date.");

        this.contentlet = contentlet;
    }

    public Contentlet getContentlet() {
        return contentlet;
    }
}
