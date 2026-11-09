package com.dotcms.rest.api.v1.notification;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.UnitTestBase;
import com.dotcms.notifications.business.NotificationAPI;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.liferay.portal.model.User;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import org.junit.Before;
import org.junit.Test;

public class NotificationResourceTest extends UnitTestBase {

    private static final String USER_ID = "notification-user";
    private static final long OWN_COUNT = 3L;
    private static final long OWN_UNREAD_COUNT = 1L;
    private static final long ALL_COUNT = 99L;

    private NotificationAPI notificationAPI;
    private WebResource webResource;
    private InitDataObject initData;
    private User user;

    @Before
    public void setUp() throws Exception {
        notificationAPI = mock(NotificationAPI.class);
        webResource = mock(WebResource.class);
        initData = mock(InitDataObject.class);
        user = mock(User.class);

        when(user.getUserId()).thenReturn(USER_ID);
        when(initData.getUser()).thenReturn(user);
        when(initData.getParamsMap()).thenReturn(Map.of(NotificationResource.ALLUSERS, "true"));
        when(webResource.init(any(WebResource.InitBuilder.class))).thenReturn(initData);

        when(notificationAPI.getNotificationsCount()).thenReturn(ALL_COUNT);
        when(notificationAPI.getNotificationsCount(USER_ID)).thenReturn(OWN_COUNT);
        when(notificationAPI.getNewNotificationsCount(USER_ID)).thenReturn(OWN_UNREAD_COUNT);
        when(notificationAPI.getNotifications(anyLong(), anyLong())).thenReturn(List.of());
        when(notificationAPI.getNotifications(any(String.class), anyLong(), anyLong())).thenReturn(List.of());
    }

    /**
     * Method to test: {@link NotificationResource#getNotifications(HttpServletRequest, HttpServletResponse, String, String)}
     * Given Scenario: a user without the CMS Administrator role passes allusers/true
     * ExpectedResult: the total and the unread count are the user's own
     */
    @Test
    public void testGetNotifications_WhenNotAdminPassesAllUsers_ShouldReturnOwnTotals() throws Exception {
        when(user.isAdmin()).thenReturn(false);

        final Map<String, Object> entity = getNotificationsEntity();

        assertEquals(OWN_COUNT, entity.get("total"));
        assertEquals(OWN_UNREAD_COUNT, entity.get("totalUnreadNotifications"));
    }

    /**
     * Method to test: {@link NotificationResource#getNotifications(HttpServletRequest, HttpServletResponse, String, String)}
     * Given Scenario: a CMS Administrator passes allusers/true
     * ExpectedResult: the total covers every user
     */
    @Test
    public void testGetNotifications_WhenAdminPassesAllUsers_ShouldReturnAllTotal() throws Exception {
        when(user.isAdmin()).thenReturn(true);

        final Map<String, Object> entity = getNotificationsEntity();

        assertEquals(ALL_COUNT, entity.get("total"));
    }

    /**
     * Method to test: {@link NotificationResource#getNewNotificationsCount(HttpServletRequest, HttpServletResponse, String)}
     * Given Scenario: a user without the CMS Administrator role passes allusers/true
     * ExpectedResult: the count is the user's own unread count
     */
    @Test
    public void testGetNewNotificationsCount_WhenNotAdminPassesAllUsers_ShouldReturnOwnCount() throws Exception {
        when(user.isAdmin()).thenReturn(false);

        final Response response = new NotificationResource(notificationAPI, webResource)
                .getNewNotificationsCount(mock(HttpServletRequest.class), mock(HttpServletResponse.class),
                        "allusers/true");

        assertEquals(OWN_UNREAD_COUNT, ((ResponseEntityView<?>) response.getEntity()).getEntity());
    }

    /**
     * Method to test: {@link NotificationResource#getNewNotificationsCount(HttpServletRequest, HttpServletResponse, String)}
     * Given Scenario: a CMS Administrator passes allusers/true
     * ExpectedResult: the count covers every user
     */
    @Test
    public void testGetNewNotificationsCount_WhenAdminPassesAllUsers_ShouldReturnAllCount() throws Exception {
        when(user.isAdmin()).thenReturn(true);

        final Response response = new NotificationResource(notificationAPI, webResource)
                .getNewNotificationsCount(mock(HttpServletRequest.class), mock(HttpServletResponse.class),
                        "allusers/true");

        assertEquals(ALL_COUNT, ((ResponseEntityView<?>) response.getEntity()).getEntity());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getNotificationsEntity() throws Exception {
        final Response response = new NotificationResource(notificationAPI, webResource)
                .getNotifications(mock(HttpServletRequest.class), mock(HttpServletResponse.class),
                        "allusers/true/limit/10", null);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        return (Map<String, Object>) ((ResponseEntityView<?>) response.getEntity()).getEntity();
    }
}
