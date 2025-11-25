package com.dotcms.graphql.listener;

import com.dotcms.contenttype.model.event.ContentTypeDeletedEvent;
import com.dotcms.contenttype.model.event.ContentTypeSavedEvent;
import com.dotcms.contenttype.model.field.event.FieldDeletedEvent;
import com.dotcms.contenttype.model.field.event.FieldSavedEvent;
import com.dotcms.graphql.business.GraphqlAPI;
import com.dotcms.system.event.local.model.Subscriber;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Logger;

@SuppressWarnings("unused")
public class ContentTypeAndFieldsModsListeners {

    @Subscriber
    public void onContentTypeSaved(final ContentTypeSavedEvent event) {
        Logger.debug(this, ()->"onContentTypeSaved, Invalidating GraphQL schema");
        APILocator.getGraphqlAPI().invalidateSchema();
    }

    @Subscriber
    public void onContentTypeDeleted(final ContentTypeDeletedEvent event) {
        Logger.debug(this, ()->"onContentTypeDeleted, Invalidating GraphQL schema");
        APILocator.getGraphqlAPI().invalidateSchema();
    }

    @Subscriber
    public void onFieldCreated(final FieldSavedEvent event) {
        Logger.debug(this, ()->"onFieldCreated, Invalidating GraphQL schema");
        APILocator.getGraphqlAPI().invalidateSchema();
    }

    @Subscriber
    public void onFieldDeleted(final FieldDeletedEvent event) {
        Logger.debug(this, ()->"onFieldDeleted, Invalidating GraphQL schema");
        APILocator.getGraphqlAPI().invalidateSchema();
    }

}
