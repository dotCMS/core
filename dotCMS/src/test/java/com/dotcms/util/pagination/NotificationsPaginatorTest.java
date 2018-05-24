package com.dotcms.util.pagination;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.notifications.NotificationConverter;
import com.dotcms.notifications.bean.Notification;
import com.dotcms.notifications.business.NotificationAPI;
import com.dotcms.notifications.view.NotificationView;
import com.dotcms.util.ConversionUtils;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.PaginatedArrayList;
import com.google.common.collect.ImmutableMap;
import com.liferay.portal.model.User;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;

public class NotificationsPaginatorTest {

    private NotificationAPI notificationAPI;
    private NotificationsPaginator notificationsPaginator;
    private ConversionUtils conversionUtils = ConversionUtils.INSTANCE;
    private NotificationConverter notificationConverter;

    @Before
    public void init() throws DotDataException {
        notificationAPI = mock(NotificationAPI.class);
        notificationConverter = mock(NotificationConverter.class);
        notificationsPaginator = new NotificationsPaginator(notificationAPI, conversionUtils, notificationConverter);
    }


    @Test
    public void test_Get_Notification_With_Offset_For_A_User() throws Exception{

        final int totalRecords = 50;

        PaginatedArrayList<Notification> notifications;
        notifications = new PaginatedArrayList<>();
        notifications.setTotalResults( totalRecords );
        notifications.add( mock( Notification.class ) );
        notifications.add( mock( Notification.class ) );
        notifications.add( mock( Notification.class ) );
        notifications.add( mock( Notification.class ) );
        notifications.add( mock( Notification.class ) );

        final String filter = null;
        final int limit = 5;
        final int offset = 4;
        final User user = mock(User.class);
        when(user.getUserId()).thenReturn("lol");

        when(notificationAPI.getNotifications(user.getUserId(), offset, limit)).thenReturn(notifications);
        when(notificationAPI.getNotificationsCount(user.getUserId())).thenReturn(Long.valueOf(totalRecords));

        when(notificationConverter.convert(Matchers.any())).thenReturn(
                mock(NotificationView.class)
        );

        final PaginatedArrayList<NotificationView> items = notificationsPaginator.getItems(user, filter, limit, offset, null, null);

        assertEquals(totalRecords, items.getTotalResults());

        assertEquals(notifications.size(), items.size());

    }


    @Test
    public void test_Get_Notification_For_All() throws Exception{

        final int totalRecords = 150;

        PaginatedArrayList<Notification> notifications;
        notifications = new PaginatedArrayList<>();
        notifications.setTotalResults( totalRecords );
        notifications.add( mock( Notification.class ) );
        notifications.add( mock( Notification.class ) );
        notifications.add( mock( Notification.class ) );
        notifications.add( mock( Notification.class ) );
        notifications.add( mock( Notification.class ) );

        final String filter = null;
        final int limit = 5;
        final int offset = 4;
        final User user = mock(User.class);
        when(user.getUserId()).thenReturn("lol");


        when(notificationAPI.getNotifications( offset, limit)).thenReturn(notifications);
        when(notificationAPI.getNotificationsCount()).thenReturn(Long.valueOf(totalRecords));

        when(notificationConverter.convert(Matchers.any())).thenReturn(
                mock(NotificationView.class)
        );

        final Map<String,Object> extraParams = ImmutableMap.of(NotificationsPaginator.ALL_USERS,true);

        final PaginatedArrayList<NotificationView> items = notificationsPaginator.getItems(user, filter, limit, offset, null, null, extraParams);

        assertEquals(totalRecords, items.getTotalResults());

        assertEquals(notifications.size(), items.size());

    }


}
