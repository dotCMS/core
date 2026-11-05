package com.dotcms.util.pagination;

import com.dotmarketing.business.LayoutAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.PaginatedArrayList;
import com.dotmarketing.util.PortletID;
import com.liferay.portal.model.User;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.dotcms.util.CollectionsUtils.list;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * test {@link UserPaginator}
 */
public class UserPaginatorTest {
    UserAPI userAPI;
    RoleAPI roleAPI;
    LayoutAPI layoutAPI;
    UserPaginator userPaginator;

    String loadCMSAdminRoleId = "2";
    String adminRoleId = "3";
    String backEndRoleId = "4";
    final List<Role> roles = new ArrayList<>(1);

    @Before
    public void init() throws DotDataException {
        userAPI = mock(UserAPI.class);

        final Role role = mock(Role.class);
        when(role.getId()).thenReturn(loadCMSAdminRoleId);

        final Role roleAdmin = mock(Role.class);
        when(roleAdmin.getId()).thenReturn(adminRoleId);

        final Role roleBackend = mock(Role.class);
        when(roleBackend.getId()).thenReturn(backEndRoleId);

        roleAPI = mock(RoleAPI.class);
        when(roleAPI.loadCMSAdminRole()).thenReturn(roleAdmin);
        when(roleAPI.loadRoleByKey(Role.ADMINISTRATOR)).thenReturn(role);
        when(roleAPI.loadBackEndUserRole()).thenReturn(roleBackend);

        roles.add(roleBackend);
        // every existing scenario lists as a caller who may read the full profile
        layoutAPI = mock(LayoutAPI.class);
        when(layoutAPI.doesUserHaveAccessToPortlet(eq(PortletID.USERS.toString()), any())).thenReturn(true);
        userPaginator = new UserPaginator( userAPI, roleAPI, () -> layoutAPI );
    }

    /**
     * <ul>
     *     <li><b>Method to test:</b> {@link UserPaginator#getItems(User, String, int, int, String, OrderDirection, Map)}</li>
     *     <li><b>Given Scenario:</b> Request the list of Users based on their name.</li>
     *     <li><b>Expected Result:</b> The pre-generated list of 5 Users is returned.</li>
     * </ul>
     */
    @Test
    public void testGetItems() throws DotDataException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        final String filter = "filter";
        final int limit = 5;
        final int offset = 4;
        final User user = mock(User.class);
        final long totalRecords = 10;

        List<Map> usersMap = new ArrayList<>();
        usersMap.add( new User().toMap() );
        usersMap.add( new User().toMap() );
        usersMap.add( new User().toMap() );
        usersMap.add( new User().toMap() );
        usersMap.add( new User().toMap() );

        List<String> rolesId = list(adminRoleId, loadCMSAdminRoleId, backEndRoleId);
        PaginatedArrayList<User> users = new PaginatedArrayList<>();
        users.setTotalResults(totalRecords);

        for (int i = 0; i < usersMap.size(); i++) {
            Map map = usersMap.get(i);
            String userId = String.valueOf(i);

            User userMock = mock(User.class);
            when(userMock.toMap()).thenReturn(map);
            when(userMock.getUserId()).thenReturn(userId);
            users.add( userMock );

            roleAPI.doesUserHaveRoles(userId, rolesId);
        }

        when(userAPI.getCountUsersByName( filter, null )).thenReturn( totalRecords );
        final List<User> userList = new ArrayList<>();
        userList.add(new User());
        userList.add(new User());
        userList.add(new User());
        userList.add(new User());
        userList.add(new User());
        final Map<String, Object> emptyParams = Map.of();
        when(userAPI.getUsersByName(anyString(), eq(null), anyInt(), anyInt(), any(UserAPI.FilteringParams.class))).thenReturn(userList);

        final PaginatedArrayList<Map<String, Object>> items = userPaginator.getItems(user, filter, limit, offset, null,
                null, emptyParams);

        assertEquals(usersMap, items);
        assertEquals(totalRecords,items.getTotalResults());
    }

    /**
     * <ul>
     *     <li><b>Method to test:</b> {@link UserPaginator#getItems(User, String, int, int)}</li>
     *     <li><b>Given Scenario:</b> Request the list of Users based on their name, and generate an exception when
     *     doing it.</li>
     *     <li><b>Expected Result:</b> A {@link DotRuntimeException} is generated by the PaginationUtil class.</li>
     * </ul>
     */
    @Test
    public void testGetItemsException() throws DotDataException {
        final String filter = "filter";
        final int limit = 5;
        final int offset = 4;
        final User user = new User();

        when(userAPI.getUsersByName(anyString(), eq(null), anyInt(), anyInt(), any(UserAPI.FilteringParams.class)))
                .thenThrow(new DotDataException(""));

        try {
            userPaginator.getItems(user, filter, limit, offset);
            assertTrue(false);
        } catch (DotRuntimeException e) {
            assertTrue(true);
        }
    }

    /**
     * <ul>
     *     <li><b>Method to test:</b> {@link UserPaginator#getItems(User, String, int, int, String, OrderDirection, Map)}</li>
     *     <li><b>Given Scenario:</b> A caller that is neither a CMS Administrator nor has access to the Users portlet
     *     lists users.</li>
     *     <li><b>Expected Result:</b> Only the {@link UserPaginator#DIRECTORY_KEYS} are returned for each user.</li>
     * </ul>
     */
    @Test
    public void testGetItemsWithoutUsersPortletReturnsOnlyDirectoryKeys() throws Exception {
        when(layoutAPI.doesUserHaveAccessToPortlet(eq(PortletID.USERS.toString()), any())).thenReturn(false);
        final User caller = mock(User.class);
        when(caller.isAdmin()).thenReturn(false);

        final Map<String, Object> full = new User().toMap();
        full.put("userId", "u1");
        full.put("lastLoginDate", "2020-01-01");
        full.put("failedLoginAttempts", 3);
        final User listed = mock(User.class);
        when(listed.toMap()).thenReturn(new java.util.HashMap<>(full));
        final List<User> users = new ArrayList<>();
        users.add(listed);
        when(userAPI.getUsersByName(any(), any(), anyInt(), anyInt(), any())).thenReturn(users);

        final PaginatedArrayList<Map<String, Object>> items = userPaginator.getItems(caller, "f", 5, 0, null,
                OrderDirection.ASC, new java.util.HashMap<>());

        assertEquals(1, items.size());
        assertTrue(UserPaginator.DIRECTORY_KEYS.containsAll(items.get(0).keySet()));
        assertTrue(items.get(0).containsKey("userId"));
    }

    /**
     * <ul>
     *     <li><b>Method to test:</b> {@link UserPaginator#getItems(User, String, int, int, String, OrderDirection, Map)}</li>
     *     <li><b>Given Scenario:</b> A CMS Administrator lists users.</li>
     *     <li><b>Expected Result:</b> The full data map is returned and the {@link LayoutAPI} is never consulted.</li>
     * </ul>
     */
    @Test
    public void testGetItemsAsAdminReturnsFullProfile() throws Exception {
        final User caller = mock(User.class);
        when(caller.isAdmin()).thenReturn(true);

        final Map<String, Object> full = new User().toMap();
        full.put("lastLoginDate", "2020-01-01");
        final User listed = mock(User.class);
        when(listed.toMap()).thenReturn(new java.util.HashMap<>(full));
        final List<User> users = new ArrayList<>();
        users.add(listed);
        when(userAPI.getUsersByName(any(), any(), anyInt(), anyInt(), any())).thenReturn(users);

        final PaginatedArrayList<Map<String, Object>> items = userPaginator.getItems(caller, "f", 5, 0, null,
                OrderDirection.ASC, new java.util.HashMap<>());

        assertTrue(items.get(0).containsKey("lastLoginDate"));
        org.mockito.Mockito.verifyNoInteractions(layoutAPI);
    }
}
