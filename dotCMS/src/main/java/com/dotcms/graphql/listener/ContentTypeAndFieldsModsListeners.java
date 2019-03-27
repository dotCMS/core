package com.dotcms.graphql.listener;

import com.dotcms.contenttype.model.event.ContentTypeDeletedEvent;
import com.dotcms.contenttype.model.event.ContentTypeSavedEvent;
import com.dotcms.contenttype.model.field.event.FieldDeletedEvent;
import com.dotcms.contenttype.model.field.event.FieldSavedEvent;
import com.dotcms.graphql.business.GraphqlAPI;
import com.dotcms.system.event.local.model.Subscriber;
import com.dotmarketing.business.APILocator;

@SuppressWarnings("unused")
public class ContentTypeAndFieldsModsListeners {

    private final GraphqlAPI graphqlAPI = APILocator.getGraphqlAPI();

    @Subscriber
    public void onContentTypeSaved(final ContentTypeSavedEvent event) {
        graphqlAPI.invalidateSchema();
    }

    @Subscriber
    public void onContentTypeDeleted(final ContentTypeDeletedEvent event) {
        graphqlAPI.invalidateSchema();
    }

    @Subscriber
    public void onFieldCreated(final FieldSavedEvent event) {
        graphqlAPI.invalidateSchema();
    }

    @Subscriber
    public void onFieldDeleted(final FieldDeletedEvent event) {
        graphqlAPI.invalidateSchema();
    }

}
