package com.dotcms.notifications;

import com.dotcms.api.system.event.*;
import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.rest.api.v1.content.ContentTypeView;
import com.dotcms.rest.api.v1.system.websocket.SessionWrapper;
import com.dotcms.util.ContentTypeUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.structure.business.StructureAPI;
import com.dotmarketing.portlets.structure.model.Structure;
import com.liferay.portal.model.User;
import org.elasticsearch.common.annotations.VisibleForTesting;

import javax.servlet.http.HttpServletRequest;
import javax.websocket.Session;

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
        ContentTypeView contentTypeView = new ContentTypeView(structure, contentTypePayloadDataWrapper.getActionUrl());

        return new SystemEvent(event.getId(), event.getEventType(),
                new Payload(contentTypeView, payload.getVisibility(), payload.getVisibilityId()),
                event.getCreationDate());
    }
}
