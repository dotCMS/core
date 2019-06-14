package com.dotmarketing.portlets.personas.business;

import com.dotcms.content.elasticsearch.business.event.ContentletArchiveEvent;
import com.dotcms.content.elasticsearch.business.event.ContentletCheckinEvent;
import com.dotcms.content.elasticsearch.business.event.ContentletPublishEvent;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Versionable;
import com.dotmarketing.business.VersionableAPI;
import com.dotmarketing.factories.MultiTreeAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletListener;
import com.dotmarketing.portlets.personas.model.Persona;
import com.liferay.util.StringPool;

import java.util.Optional;

/**
 * This listener is to track the changes on personas in order to keep up to date changes on the
 * the personalization multitree
 * @author jsanca
 */
public class MultiTreePersonaListener implements ContentletListener<Persona> {

    private final VersionableAPI versionableAPI = APILocator.getVersionableAPI();
    private final MultiTreeAPI   multiTreeAPI   = APILocator.getMultiTreeAPI();

    @Override
    public void onModified(ContentletCheckinEvent<Persona> contentletCheckinEvent) {

        if (null != contentletCheckinEvent.getContentlet() &&
            null != contentletCheckinEvent.getContentlet().getIdentifier() &&
            !contentletCheckinEvent.isNewVersionCreated()) {

            final Persona persona    = contentletCheckinEvent.getContentlet();
            final String  personaTag = persona.getKeyTag();
            final String identifier  = persona.getIdentifier();
            final Optional<Versionable> previousVersion =
                    this.versionableAPI.findPreviousVersion(identifier);

            // if there is a previous version, switch the old content associated to it to the new one.
            if (previousVersion.isPresent()) {

                final Versionable previousVersionable = previousVersion.get();
                if (previousVersionable instanceof Contentlet) {

                    final String previousPersonaTag = Contentlet.class.cast(previousVersionable)
                            .getStringProperty(PersonaAPI.KEY_TAG_FIELD);

                    this.multiTreeAPI.updateMultiTreeByPersonaTag (
                            Persona.DOT_PERSONA_PREFIX_SCHEME + StringPool.COLON + previousPersonaTag,
                            Persona.DOT_PERSONA_PREFIX_SCHEME + StringPool.COLON + personaTag);
                }
            }
        }
    }


    @Override
    public void onArchive(ContentletArchiveEvent<Persona> contentletArchiveEvent) {

    }
}
