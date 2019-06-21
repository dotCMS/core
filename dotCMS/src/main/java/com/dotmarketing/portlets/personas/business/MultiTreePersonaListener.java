package com.dotmarketing.portlets.personas.business;

import com.dotcms.content.elasticsearch.business.event.ContentletDeletedEvent;
import com.dotcms.content.elasticsearch.business.event.ContentletPublishEvent;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Versionable;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletListener;
import com.dotmarketing.portlets.personas.model.Persona;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

import java.util.Optional;

/**
 * This listener is to track the changes on personas in order to keep up to date changes on the
 * the personalization multitree
 * By default it is just cleaning up when the publish/unpublish or delete
 * @author jsanca
 */
public class MultiTreePersonaListener implements ContentletListener<Persona> {


    @Override
    public void onModified(final ContentletPublishEvent<Persona> contentletPublishEvent) {

        try {

            final Optional<Versionable> versionable = APILocator.getVersionableAPI()
                    .findPreviousVersion(contentletPublishEvent.getContentlet().getIdentifier());

            if (versionable.isPresent()) {

                final Contentlet previousPersonaContent = APILocator.getContentletAPI().find
                        (versionable.get().getInode(), APILocator.systemUser(), false);

                if (null != previousPersonaContent) {

                    final String currentKeyTag  = contentletPublishEvent.getContentlet().getKeyTag();
                    final String previousKeyTag = previousPersonaContent.getStringProperty(PersonaAPI.KEY_TAG_FIELD);

                    if (UtilMethods.isSet(currentKeyTag) && UtilMethods.isSet(previousKeyTag) &&
                            !currentKeyTag.equals(previousKeyTag)) { //  if the tag has changed.

                        // todo: update the previous multi tree personalization with persona tag with the new one
                    }
                }
            }

        } catch (Exception e) {
            Logger.error(this, e.getMessage(), e);
        }
    }

    @Override
    public void onDeleted(final ContentletDeletedEvent<Persona> contentletDeletedEvent) {

        Logger.info(this, "The Persona tag: " + contentletDeletedEvent.getContentlet().getKeyTag() +
                ", has been removed, triggering the DeleteMultiTreeUsedPersonaTagJob to remove the unused tags");

        DeleteMultiTreeUsedPersonaTagJob.triggerDeleteMultiTreeUsedPersonaTagJob(contentletDeletedEvent.getUser(), false);
    }
}
