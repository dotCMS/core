package com.dotmarketing.portlets.workflows.actionlet.event;

import com.dotmarketing.portlets.contentlet.model.Contentlet;

import java.io.Serializable;

public class CopyActionletEvent implements Serializable {

    private final Contentlet originalContentlet;
    private final Contentlet copyContentlet;

    public CopyActionletEvent(Contentlet originalContentlet, Contentlet copyContentlet) {
        this.originalContentlet = originalContentlet;
        this.copyContentlet = copyContentlet;
    }

    public Contentlet getOriginalContentlet() {
        return originalContentlet;
    }

    public Contentlet getCopyContentlet() {
        return copyContentlet;
    }
}
