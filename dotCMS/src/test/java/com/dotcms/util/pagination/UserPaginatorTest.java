package com.dotcms.util.pagination;

import com.dotcms.UnitTestBase;
import com.dotcms.rest.api.v1.system.role.SmallRoleView;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.PaginatedArrayList;
import com.liferay.portal.model.User;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.dotcms.util.CollectionsUtils.list;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * test {@link UserPaginator}
 * <p>Extends {@link UnitTestBase} because {@link UserPaginator}'s constructor reaches {@code APILocator} through
 * {@code UserResourceHelper.getInstance()}; without the base class bootstrap the test only survives when another
 * {@code UnitTestBase} subclass happened to run first in the same (reused) fork.</p>
 */
public class UserPaginatorTest extends UnitTestBase {
    UserAPI userAPI;
    RoleAPI roleAPI;
    UserPaginator userPaginator;

    String loadCMSAdminRoleId = "2";
    String adminRoleId = "3";
    String backEndRoleId = "4";
    static final String USERS_ROOT_ID = "users-root-id";
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

        final Role usersRoot = mock(Role.class);
        when(usersRoot.getId()).thenReturn(USERS_ROOT_ID);

        roleAPI = mock(RoleAPI.class);
        when(roleAPI.loadCMSAdminRole()).thenReturn(roleAdmin);
        when(roleAPI.loadRoleByKey(Role.ADMINISTRATOR)).thenReturn(role);
        when(roleAPI.loadRoleByKey(RoleAPI.USERS_ROOT_ROLE_KEY)).thenReturn(usersRoot);
        when(roleAPI.loadBackEndUserRole()).thenReturn(roleBackend);

        roles.add(roleBackend);
        userPaginator = new UserPaginator( userAPI, roleAPI );
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

        when(userAPI.getCountUsersByName(eq(filter), eq(null), any(UserAPI.FilteringParams.class)))
                .thenReturn(totalRecords);
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
     *     <li><b>Method to test:</b> {@link UserPaginator#getItems(User, String, int, int, String, OrderDirection, Map)}</li>
     *     <li><b>Given Scenario:</b> Request the list of Users passing a Role list under the
     *     {@link UserPaginator#ROLES_PARAM} extra parameter.</li>
     *     <li><b>Expected Result:</b> Both the item query and the total-records count receive the same Role
     *     list, so the page and its {@code totalEntries} stay consistent.</li>
     * </ul>
     */
    @Test
    public void testGetItemsPassesRolesToQueryAndCount() throws Exception {
        final String filter = "filter";
        final long totalRecords = 3;
        final User user = mock(User.class);

        final List<User> userList = new ArrayList<>();
        for (int i = 0; i < totalRecords; i++) {
            final User userMock = mock(User.class);
            when(userMock.toMap()).thenReturn(new User().toMap());
            userList.add(userMock);
        }
        when(userAPI.getUsersByName(anyString(), eq(roles), anyInt(), anyInt(),
                any(UserAPI.FilteringParams.class))).thenReturn(userList);
        when(userAPI.getCountUsersByName(eq(filter), eq(roles), any(UserAPI.FilteringParams.class)))
                .thenReturn(totalRecords);

        final Map<String, Object> extraParams = Map.of(UserPaginator.ROLES_PARAM, roles);
        final PaginatedArrayList<Map<String, Object>> items = userPaginator.getItems(user, filter, 5, 0,
                null, null, extraParams);

        assertEquals(totalRecords, items.getTotalResults());
        assertEquals(userList.size(), items.size());
        verify(userAPI).getUsersByName(eq(filter), eq(roles), anyInt(), anyInt(),
                any(UserAPI.FilteringParams.class));
        verify(userAPI).getCountUsersByName(eq(filter), eq(roles), any(UserAPI.FilteringParams.class));
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
     *     <li><b>Given Scenario:</b> {@link UserPaginator#INCLUDE_ROLES_PARAM} is not set.</li>
     *     <li><b>Expected Result:</b> Items carry no {@code roles} key and the direct-role lookup is never
     *     performed — the opt-out path has zero extra cost.</li>
     * </ul>
     */
    @Test
    public void testGetItemsWithoutIncludeRolesAddsNothing() throws Exception {
        final User userMock = mock(User.class);
        when(userMock.getUserId()).thenReturn("u1");
        // stubbed map: the real User#toMap() reaches APILocator/DB, which a unit test must not do
        when(userMock.toMap()).thenReturn(new HashMap<>(Map.of("userId", "u1")));
        when(userAPI.getUsersByName(anyString(), eq(null), anyInt(), anyInt(), any(UserAPI.FilteringParams.class)))
                .thenReturn(list(userMock));

        final PaginatedArrayList<Map<String, Object>> items = userPaginator.getItems(mock(User.class), "filter", 5, 0,
                null, null, Map.of());

        assertFalse(items.get(0).containsKey(UserPaginator.ROLES_PARAM));
        verify(roleAPI, never()).loadRolesForUser(anyString(), any(Boolean.class));
    }

    /**
     * <ul>
     *     <li><b>Method to test:</b> {@link UserPaginator#getItems(User, String, int, int, String, OrderDirection, Map)}</li>
     *     <li><b>Given Scenario:</b> {@link UserPaginator#INCLUDE_ROLES_PARAM} is {@code true}; the user directly
     *     holds a keyed role, a keyless role, an ordinary root role whose NAME starts with "User" (so
     *     {@link Role#isUser()} would wrongly flag it) and their personal role (DBFQN under the cms_users root).</li>
     *     <li><b>Expected Result:</b> Direct roles are requested ({@code includeImplicitRoles=false}) and exposed
     *     as {@link SmallRoleView}s carrying id, name and (possibly null) roleKey; the "User Managers" role is kept
     *     and only the personal role is dropped.</li>
     * </ul>
     */
    @Test
    public void testGetItemsWithIncludeRolesAddsDirectRoleViews() throws Exception {
        final User userMock = mock(User.class);
        when(userMock.getUserId()).thenReturn("u1");
        // stubbed map: the real User#toMap() reaches APILocator/DB, which a unit test must not do
        when(userMock.toMap()).thenReturn(new HashMap<>(Map.of("userId", "u1")));
        when(userAPI.getUsersByName(anyString(), eq(null), anyInt(), anyInt(), any(UserAPI.FilteringParams.class)))
                .thenReturn(list(userMock));

        final Role keyed = mock(Role.class);
        when(keyed.getId()).thenReturn("r-keyed");
        when(keyed.getName()).thenReturn("Keyed");
        when(keyed.getRoleKey()).thenReturn("keyed");
        when(keyed.getDBFQN()).thenReturn("r-keyed");
        final Role keyless = mock(Role.class);
        when(keyless.getId()).thenReturn("r-keyless");
        when(keyless.getName()).thenReturn("Keyless");
        when(keyless.getDBFQN()).thenReturn("some-parent --> r-keyless");
        final Role userManagers = mock(Role.class);
        when(userManagers.getId()).thenReturn("r-user-managers");
        when(userManagers.getName()).thenReturn("User Managers");
        when(userManagers.getDBFQN()).thenReturn("r-user-managers");
        when(userManagers.isUser()).thenReturn(true); // what the name-based FQN check would say
        final Role personal = mock(Role.class);
        when(personal.getId()).thenReturn("r-personal");
        when(personal.getDBFQN()).thenReturn(USERS_ROOT_ID + " --> r-personal");
        when(roleAPI.loadRolesForUser("u1", false)).thenReturn(list(keyed, keyless, userManagers, personal));

        final PaginatedArrayList<Map<String, Object>> items = userPaginator.getItems(mock(User.class), "filter", 5, 0,
                null, null, Map.of(UserPaginator.INCLUDE_ROLES_PARAM, true));

        @SuppressWarnings("unchecked")
        final List<SmallRoleView> roles = (List<SmallRoleView>) items.get(0).get(UserPaginator.ROLES_PARAM);
        assertEquals(3, roles.size());
        assertEquals("r-keyed", roles.get(0).getId());
        assertEquals("Keyed", roles.get(0).getName());
        assertEquals("keyed", roles.get(0).getRoleKey());
        assertEquals("r-keyless", roles.get(1).getId());
        assertNull(roles.get(1).getRoleKey());
        assertEquals("a root role merely NAMED 'User...' must be kept", "r-user-managers", roles.get(2).getId());
        verify(roleAPI, never()).loadRolesForUser("u1", true);
    }

}
