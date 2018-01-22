package com.dotcms.system.event.local.type.content;

import com.dotmarketing.portlets.contentlet.model.Contentlet;

/**
 * Object used to represent an event to be triggered when a Commit Listener is executed
 *
 * @author Jonathan Gamba 7/28/17
 */
public class CommitListenerEvent {

    private final Contentlet contentlet;

    public CommitListenerEvent(Contentlet contentlet) {
        this.contentlet = contentlet;
    }

    public Contentlet getContentlet() {
        return contentlet;
    }

}