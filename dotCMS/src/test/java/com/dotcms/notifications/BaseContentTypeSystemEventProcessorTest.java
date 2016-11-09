package com.dotcms.notifications;


import com.dotcms.api.system.event.ContentTypePayloadDataWrapper;
import com.dotcms.api.system.event.Payload;
import com.dotcms.api.system.event.SystemEvent;
import com.dotcms.api.system.event.SystemEventType;
import com.dotcms.rest.api.v1.content.ContentTypeView;
import com.dotcms.rest.api.v1.system.websocket.SessionWrapper;
import com.dotmarketing.portlets.structure.model.Structure;
import com.liferay.portal.model.User;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BaseContentTypeSystemEventProcessorTest {

    @Test
    public void testProcess(){
        SystemEvent event = mock(SystemEvent.class);
        Structure structure = mock(Structure.class);
        Payload payload = mock(Payload.class);
        SessionWrapper session = mock(SessionWrapper.class);
        User user = new User();
        SystemEventType systemEventType = SystemEventType.SAVE_BASE_CONTENT_TYPE;

        ContentTypePayloadDataWrapper contentTypePayloadDataWrapper = new ContentTypePayloadDataWrapper("http://localhost:8080", structure);

        when(session.getUser()).thenReturn(user);
        when(event.getId()).thenReturn("1");
        when(event.getEventType()).thenReturn(systemEventType);
        when(event.getPayload()).thenReturn(payload);
        when(payload.getData()).thenReturn(structure);
        when(payload.getRawData()).thenReturn(contentTypePayloadDataWrapper);
        when(payload.getVisibilityId()).thenReturn("1");
        when(structure.getStructureType()).thenReturn(Structure.Type.CONTENT.getType());
        when(structure.getName()).thenReturn("test structure");
        when(structure.getInode()).thenReturn("3b276d59-46e3-4196-9169-639ddfe6677f");

        BaseContentTypeSystemEventProcessor baseContentTypeSystemEventProcessor = new BaseContentTypeSystemEventProcessor();
        SystemEvent result = baseContentTypeSystemEventProcessor.process(event, session);

        assertEquals(result.getEventType(), systemEventType);
        assertEquals(result.getId(), "1");
        ContentTypeView contentTypeView = ContentTypeView.class.cast(result.getPayload().getData());
        assertEquals(Structure.Type.CONTENT.toString(), contentTypeView.getType());
        assertEquals( "test structure", contentTypeView.getName());
        assertEquals("3b276d59-46e3-4196-9169-639ddfe6677f", contentTypeView.getInode());
        assertEquals("http://localhost:8080", contentTypeView.getAction());
    }
}
