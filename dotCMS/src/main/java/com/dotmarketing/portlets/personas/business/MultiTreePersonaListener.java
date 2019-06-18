package com.dotmarketing.portlets.personas.business;

import com.dotcms.content.elasticsearch.business.event.ContentletDeletedEvent;
import com.dotcms.content.elasticsearch.business.event.ContentletPublishEvent;
import com.dotmarketing.portlets.contentlet.model.ContentletListener;
import com.dotmarketing.portlets.personas.model.Persona;
import com.dotmarketing.util.Logger;

/**
 * This listener is to track the changes on personas in order to keep up to date changes on the
 * the personalization multitree
 * By default it is just cleaning up when the publish/unpublish or delete
 * @author jsanca
 */
public class MultiTreePersonaListener implements ContentletListener<Persona> {


    @Override
    public void onModified(final ContentletPublishEvent<Persona> contentletPublishEvent) {

        // Here want to check if the persona key tag has changed.
        DeleteMultiTreeUsedPersonaTagJob.triggerDeleteMultiTreeUsedPersonaTagJob(contentletPublishEvent.getUser(), false);
    }

    @Override
    public void onDeleted(final ContentletDeletedEvent<Persona> contentletDeletedEvent) {

        Logger.info(this, "The Persona tag: " + contentletDeletedEvent.getContentlet().getKeyTag() +
                ", has been removed, triggering the DeleteMultiTreeUsedPersonaTagJob to remove the unused tags");

        DeleteMultiTreeUsedPersonaTagJob.triggerDeleteMultiTreeUsedPersonaTagJob(contentletDeletedEvent.getUser(), false);
    }
}
