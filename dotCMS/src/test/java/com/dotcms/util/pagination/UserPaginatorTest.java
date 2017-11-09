package com.dotcms.util.pagination;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.util.PaginatedArrayList;
import com.liferay.portal.model.User;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.dotcms.util.CollectionsUtils.list;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * test {@link UserPaginator}
 */
public class UserPaginatorTest {
    UserAPI userAPI;
    RoleAPI roleAPI;
    UserPaginator userPaginator;

    String loadCMSAdminRoleId = "2";
    String adminRoleId = "3";

    @Before
    public void init() throws DotDataException {
        userAPI = mock(UserAPI.class);

        Role role = mock(Role.class);
        when(role.getId()).thenReturn(loadCMSAdminRoleId);

        Role roleAdmin = mock(Role.class);
        when(roleAdmin.getId()).thenReturn(adminRoleId);

        roleAPI = mock(RoleAPI.class);
        when(roleAPI.loadCMSAdminRole()).thenReturn(roleAdmin);
        when(roleAPI.loadRoleByKey(Role.ADMINISTRATOR)).thenReturn(role);

        userPaginator = new UserPaginator( userAPI, roleAPI );
    }

    @Test
    public void testGetItems() throws DotDataException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        String filter = "filter";
        int limit = 5;
        int offset = 4;
        User user = new User();
        long totalRecords = 10;

        List<Map> usersMap = new ArrayList<>();
        usersMap.add( mock( Map.class ) );
        usersMap.add( mock( Map.class ) );
        usersMap.add( mock( Map.class ) );
        usersMap.add( mock( Map.class ) );
        usersMap.add( mock( Map.class ) );

        List<String> rolesId = list(adminRoleId, loadCMSAdminRoleId);
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

        when(userAPI.getUsersByName( filter, offset, limit, user, false ))
                .thenReturn( users );

        when(userAPI.getCountUsersByName( filter )).thenReturn( totalRecords );

        PaginatedArrayList<Map<String, Object>> items = userPaginator.getItems(user, filter, limit, offset);

        assertEquals(usersMap, items);
        assertEquals(items.getTotalResults(), totalRecords);
    }

    @Test
    public void testGetItemsException() throws DotDataException {
        String filter = "filter";
        boolean showArchived = false;
        int limit = 5;
        int offset = 4;
        User user = new User();

        when(userAPI.getUsersByName( filter, offset, limit, user, false ))
                .thenThrow(new DotDataException(""));

        try {
            userPaginator.getItems(user, filter, limit, offset);
            assertTrue(false);
        } catch (DotRuntimeException e) {
            assertTrue(true);
        }
    }
}
