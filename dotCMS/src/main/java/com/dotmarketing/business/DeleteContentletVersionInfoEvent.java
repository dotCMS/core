package com.dotmarketing.business;

import com.dotmarketing.portlets.contentlet.model.Contentlet;

import java.io.Serializable;

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
