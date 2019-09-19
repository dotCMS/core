package com.dotcms.util.pagination;

import static com.dotcms.util.CollectionsUtils.list;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.PaginatedArrayList;
import com.liferay.portal.model.User;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;

/**
 * test {@link UserPaginator}
 */
public class UserPaginatorTest {
    UserAPI userAPI;
    RoleAPI roleAPI;
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
        userPaginator = new UserPaginator( userAPI, roleAPI );
    }

    @Test
    public void testGetItems() throws DotDataException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        final String filter = "filter";
        final int limit = 5;
        final int offset = 4;
        final User user = new User();
        final long totalRecords = 10;

        List<Map> usersMap = new ArrayList<>();
        usersMap.add( mock( Map.class ) );
        usersMap.add( mock( Map.class ) );
        usersMap.add( mock( Map.class ) );
        usersMap.add( mock( Map.class ) );
        usersMap.add( mock( Map.class ) );

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

        when(userAPI.getUsersByName( filter, roles, offset, limit))
                .thenReturn( users );

        when(userAPI.getCountUsersByName( filter, roles )).thenReturn( totalRecords );

        PaginatedArrayList<Map<String, Object>> items = userPaginator.getItems(user, filter, limit, offset);

        assertEquals(usersMap, items);
        assertEquals(totalRecords,items.getTotalResults());
    }

    @Test
    public void testGetItemsException() throws DotDataException {
        final String filter = "filter";
        final int limit = 5;
        final int offset = 4;
        final User user = new User();

        when(userAPI.getUsersByName( filter, roles, offset, limit))
                .thenThrow(new DotDataException(""));

        try {
            userPaginator.getItems(user, filter, limit, offset);
            assertTrue(false);
        } catch (DotRuntimeException e) {
            assertTrue(true);
        }
    }
}
