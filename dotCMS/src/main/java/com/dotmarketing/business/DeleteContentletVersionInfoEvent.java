package com.dotmarketing.business;

import com.dotmarketing.portlets.contentlet.model.Contentlet;

import java.io.Serializable;

/**
 * Trigger when a {@link com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo} is deleted
 */
public class DeleteContentletVersionInfoEvent implements Serializable {

    private final Contentlet contentlet;
    private boolean deleteAllVariant;

    public DeleteContentletVersionInfoEvent(final Contentlet contentlet) {
        this(contentlet, false);
    }

    public DeleteContentletVersionInfoEvent(final Contentlet contentlet, final boolean deleteAllVariant) {
        this.contentlet = contentlet;
        this.deleteAllVariant = deleteAllVariant;
    }

    public Contentlet getContentlet() {
        return contentlet;
    }

    public boolean isDeleteAllVariant() {
        return deleteAllVariant;
    }
}
