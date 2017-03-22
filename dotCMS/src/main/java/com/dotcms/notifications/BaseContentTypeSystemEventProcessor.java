package com.dotcms.notifications;

import com.dotcms.api.system.event.ContentTypePayloadDataWrapper;
import com.dotcms.api.system.event.Payload;
import com.dotcms.api.system.event.SystemEvent;
import com.dotcms.api.system.event.SystemEventProcessor;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.rest.api.v1.content.ContentTypeView;
import com.liferay.portal.model.User;

/**
 * Decorates the {@link com.dotcms.api.system.event.SystemEventType#SAVE_BASE_CONTENT_TYPE},
 * {@link com.dotcms.api.system.event.SystemEventType#UPDATE_BASE_CONTENT_TYPE} and {@link com.dotcms.api.system.event.SystemEventType#DELETE_BASE_CONTENT_TYPE}
 * in order to convert the {@link com.dotmarketing.portlets.structure.model.Structure}
 * to {@link com.dotcms.rest.api.v1.content.ContentTypeView}.
 * @author jsanca
 */
public class BaseContentTypeSystemEventProcessor  implements SystemEventProcessor {

    @Override
    public SystemEvent process(final SystemEvent event, final User sessionUser) {

        final Payload payload = event.getPayload();
        final ContentTypePayloadDataWrapper contentTypePayloadDataWrapper = (ContentTypePayloadDataWrapper) payload.getRawData();
        final ContentType type = contentTypePayloadDataWrapper.getContentType();
        final ContentTypeView contentTypeView = new ContentTypeView(type, contentTypePayloadDataWrapper.getActionUrl());

        return new SystemEvent(event.getId(), event.getEventType(),
                new Payload(contentTypeView, payload.getVisibility(), payload.getVisibilityValue()),
                event.getCreationDate());
    } // process.
} // E:O:F:BaseContentTypeSystemEventProcessor.
