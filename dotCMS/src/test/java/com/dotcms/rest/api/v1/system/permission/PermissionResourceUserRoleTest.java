package com.dotcms.rest.api.v1.system.permission;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.exception.NotFoundException;
import com.dotcms.util.pagination.UserPermissionsPaginator;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.util.PaginatedArrayList;
import com.liferay.portal.model.User;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Before;
import org.junit.Test;

/** Permission reads must not create personal roles or restore revoked role membership. */
public class PermissionResourceUserRoleTest {

    private final RoleAPI roleAPI = mock(RoleAPI.class);
    private final HttpServletRequest request = mock(HttpServletRequest.class);
    private final HttpServletResponse response = mock(HttpServletResponse.class);
    private PermissionResource resource;

    /** Authenticates a non-admin caller reading their own permissions without external services. */
    @Before
    public void setUp() throws Exception {
        final WebResource webResource = mock(WebResource.class);
        final UserAPI userAPI = mock(UserAPI.class);
        final User user = mock(User.class);
        final InitDataObject initData = new InitDataObject();
        initData.setUser(user);
        when(user.getUserId()).thenReturn("fixture-user");
        when(user.getFullName()).thenReturn("Fixture User");
        when(user.getEmailAddress()).thenReturn("fixture@example.com");
        when(webResource.init(any(WebResource.InitBuilder.class))).thenReturn(initData);
        when(userAPI.loadUserById("fixture-user")).thenReturn(user);
        when(request.getRequestURI()).thenReturn("/api/v1/permissions/user/fixture-user");
        final UserPermissionsPaginator paginator = mock(UserPermissionsPaginator.class);
        when(paginator.getItems(any(), anyInt(), anyInt(), anyMap()))
                .thenReturn(new PaginatedArrayList<>());
        resource = new PermissionResource(webResource, null, userAPI, roleAPI, null,
                paginator, null, null);
    }

    /** Uses the key lookup only, even if a personal role is no longer assigned to its user. */
    @Test
    public void test_getUserPermissions_doesNotRestorePersonalRoleMembership() throws Exception {
        final Role role = new Role();
        role.setId("personal-role");
        when(roleAPI.loadRoleByKey("fixture-user")).thenReturn(role);

        final UserPermissionsView permissions = (UserPermissionsView) resource.getUserPermissions(
                request, response, "fixture-user", 1, 40).getEntity();

        assertEquals("personal-role", permissions.roleId());
        verify(roleAPI).loadRoleByKey("fixture-user");
        verifyNoMoreInteractions(roleAPI);
    }

    /** A missing personal role is a 404, not an invitation to create and assign a role. */
    @Test
    public void test_getUserPermissions_missingPersonalRoleIsNotCreated() throws Exception {
        final NotFoundException error = assertThrows(NotFoundException.class,
                () -> resource.getUserPermissions(request, response, "fixture-user", 1, 40));

        assertEquals(404, error.getResponse().getStatus());
        verify(roleAPI).loadRoleByKey("fixture-user");
        verifyNoMoreInteractions(roleAPI);
    }
}
