package com.dotcms.notifications;


import com.dotcms.UnitTestBase;
import com.dotcms.api.system.event.ContentTypePayloadDataWrapper;
import com.dotcms.api.system.event.Payload;
import com.dotcms.api.system.event.SystemEvent;
import com.dotcms.api.system.event.SystemEventType;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.model.type.SimpleContentType;
import com.dotcms.rest.api.v1.content.ContentTypeView;
import com.liferay.portal.model.User;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BaseContentTypeSystemEventProcessorTest extends UnitTestBase {

    @Test
    public void testProcess(){
        SystemEvent event = mock(SystemEvent.class);
        ContentType type = ContentTypeBuilder.builder(SimpleContentType.class)
            .id("3b276d59-46e3-4196-9169-639ddfe6677f")
            .name("test structure")
            .variable("testtestingStructure").build();
        Payload payload = mock(Payload.class);
        User user = new User();
        SystemEventType systemEventType = SystemEventType.SAVE_BASE_CONTENT_TYPE;

        ContentTypePayloadDataWrapper contentTypePayloadDataWrapper = new ContentTypePayloadDataWrapper("http://localhost:8080", type);

        when(event.getId()).thenReturn("1");
        when(event.getEventType()).thenReturn(systemEventType);
        when(event.getPayload()).thenReturn(payload);
        when(payload.getData()).thenReturn(type);
        when(payload.getRawData()).thenReturn(contentTypePayloadDataWrapper);

        when(payload.getVisibilityValue()).thenReturn("1");


        BaseContentTypeSystemEventProcessor baseContentTypeSystemEventProcessor = new BaseContentTypeSystemEventProcessor();
        SystemEvent result = baseContentTypeSystemEventProcessor.process(event, user);

        assertEquals(result.getEventType(), systemEventType);
        assertEquals(result.getId(), "1");
        ContentTypeView contentTypeView = ContentTypeView.class.cast(result.getPayload().getData());
        assertEquals(BaseContentType.CONTENT.toString(), contentTypeView.getType());
        assertEquals( "test structure", contentTypeView.getName());
        assertEquals("3b276d59-46e3-4196-9169-639ddfe6677f", contentTypeView.getInode());
        assertEquals("http://localhost:8080", contentTypeView.getAction());
    }
}
