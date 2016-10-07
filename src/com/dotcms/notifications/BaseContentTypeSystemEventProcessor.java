package com.dotcms.notifications;

import javax.websocket.Session;

import com.dotcms.api.system.event.ContentTypePayloadDataWrapper;
import com.dotcms.api.system.event.Payload;
import com.dotcms.api.system.event.SystemEvent;
import com.dotcms.api.system.event.SystemEventProcessor;
import com.dotcms.contenttype.transform.contenttype.StructureTransformer;
import com.dotcms.rest.api.v1.content.ContentTypeView;
import com.dotmarketing.portlets.structure.model.Structure;

/**
 * Decorates the {@link com.dotcms.api.system.event.SystemEventType#SAVE_BASE_CONTENT_TYPE},
 * {@link com.dotcms.api.system.event.SystemEventType#UPDATE_BASE_CONTENT_TYPE} and {@link com.dotcms.api.system.event.SystemEventType#DELETE_BASE_CONTENT_TYPE}
 * in order to convert the {@link com.dotmarketing.portlets.structure.model.Structure}
 * to {@link com.dotcms.rest.api.v1.content.ContentTypeView}.
 * @author jsanca
 */
public class BaseContentTypeSystemEventProcessor  implements SystemEventProcessor {

  @Override
    public SystemEvent process(SystemEvent event, Session session) {
        Payload payload = event.getPayload();
        ContentTypePayloadDataWrapper contentTypePayloadDataWrapper = (ContentTypePayloadDataWrapper) payload.getRawData();
        Structure structure = contentTypePayloadDataWrapper.getStructure();
        
        ContentTypeView contentTypeView = new ContentTypeView(new StructureTransformer(structure).from(), contentTypePayloadDataWrapper.getActionUrl());

        return new SystemEvent(event.getId(), event.getEventType(),
                new Payload(contentTypeView, payload.getVisibility(), payload.getVisibilityId()),
                event.getCreationDate());
    }
}
