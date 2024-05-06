package com.dotcms.util.marshal;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.text.ParseException;

import com.dotmarketing.business.APILocator;
import org.junit.BeforeClass;
import org.junit.Test;

import com.dotcms.api.system.event.Payload;
import com.dotcms.api.system.event.SystemEvent;
import com.dotcms.api.system.event.SystemEventType;
import com.dotcms.notifications.bean.Notification;
import com.dotcms.notifications.bean.NotificationAction;
import com.dotcms.notifications.bean.NotificationActionType;
import com.dotcms.notifications.bean.NotificationData;
import com.dotcms.notifications.bean.NotificationLevel;
import com.dotcms.notifications.bean.NotificationType;
import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.I18NMessage;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.util.json.JSONException;

/**
 * MarshalUtils
 * Test
 * @author jsanca
 * @version 3.7
 */

public class MarshalUtilsIntegrationTest {
	
	@BeforeClass
	public static void prepare() throws Exception{
		//Setting web app environment
        IntegrationTestInitService.getInstance().init();
	}

    @Test
    public void marshalSystemEvent() throws ParseException, JSONException {

        final MarshalFactory marshalFactory =
                MarshalFactory.getInstance();

        assertNotNull(marshalFactory);

        final MarshalUtils marshalUtils =
                marshalFactory.getMarshalUtils();

        assertNotNull(marshalUtils);

		final NotificationData notificationData = new NotificationData(new I18NMessage("Test Title"), new I18NMessage("Notification message"),
				CollectionsUtils.list(new NotificationAction(new I18NMessage("See More"), "#seeMore", NotificationActionType.LINK, null)));
		final SystemEvent systemEvent = new SystemEvent("123456", SystemEventType.NOTIFICATION, new Payload(
				new Notification("78910", NotificationType.GENERIC, NotificationLevel.INFO, "admin@dotcms.com", null,
						false, notificationData)), new java.util.Date(), "1234");

        String json = marshalUtils.marshal(systemEvent);

        System.out.println(json);

        assertNotNull(json);

        final SystemEvent systemEvent1 =
                marshalUtils.unmarshal(json, SystemEvent.class);

        assertTrue(systemEvent.equals(systemEvent1));
        assertTrue(systemEvent.getEventType() == systemEvent1.getEventType());

        System.out.println(systemEvent.getPayload().getData());
        System.out.println(systemEvent1.getPayload().getData());

        assertTrue(systemEvent.getPayload().getData().equals(systemEvent1.getPayload().getData()));
    }

}
