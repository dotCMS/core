package com.dotcms.rest.api.v1.index;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.content.index.MigrationIndexVisibility;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.liferay.portal.model.User;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * Unit tests for the role gate of {@link MigrationReadinessResource#isMigrationSupportUser(User)} —
 * the readiness endpoint is restricted to CMS administrators and members of the migration support
 * role, and fails closed otherwise (issue #36360). All access APIs are mocked; no container needed.
 */
public class MigrationReadinessResourceTest {

    /** No authenticated user → denied. */
    @Test
    public void nullUser_denied() {
        assertFalse(MigrationReadinessResource.isMigrationSupportUser(null));
    }

    /** A CMS administrator is allowed without any role lookup. */
    @Test
    public void cmsAdmin_allowed() throws DotDataException {
        final User user = mock(User.class);
        final UserAPI userAPI = mock(UserAPI.class);
        when(userAPI.isCMSAdmin(user)).thenReturn(true);

        try (MockedStatic<APILocator> api = Mockito.mockStatic(APILocator.class)) {
            api.when(APILocator::getUserAPI).thenReturn(userAPI);
            assertTrue(MigrationReadinessResource.isMigrationSupportUser(user));
        }
    }

    /** A non-admin who holds the configured support role is allowed. */
    @Test
    public void roleMember_allowed() throws DotDataException {
        final User user = mock(User.class);
        final UserAPI userAPI = mock(UserAPI.class);
        when(userAPI.isCMSAdmin(user)).thenReturn(false);
        final Role role = mock(Role.class);
        final RoleAPI roleAPI = mock(RoleAPI.class);
        when(roleAPI.loadRoleByKey(MigrationIndexVisibility.DEFAULT_VISIBILITY_ROLE_KEY))
                .thenReturn(role);
        when(roleAPI.doesUserHaveRole(user, role)).thenReturn(true);

        try (MockedStatic<APILocator> api = Mockito.mockStatic(APILocator.class)) {
            api.when(APILocator::getUserAPI).thenReturn(userAPI);
            api.when(APILocator::getRoleAPI).thenReturn(roleAPI);
            assertTrue(MigrationReadinessResource.isMigrationSupportUser(user));
        }
    }

    /** A non-admin without the role is denied. */
    @Test
    public void nonAdminWithoutRole_denied() throws DotDataException {
        final User user = mock(User.class);
        final UserAPI userAPI = mock(UserAPI.class);
        when(userAPI.isCMSAdmin(user)).thenReturn(false);
        final RoleAPI roleAPI = mock(RoleAPI.class);
        when(roleAPI.loadRoleByKey(MigrationIndexVisibility.DEFAULT_VISIBILITY_ROLE_KEY))
                .thenReturn(mock(Role.class));
        when(roleAPI.doesUserHaveRole(Mockito.eq(user), Mockito.any(Role.class))).thenReturn(false);

        try (MockedStatic<APILocator> api = Mockito.mockStatic(APILocator.class)) {
            api.when(APILocator::getUserAPI).thenReturn(userAPI);
            api.when(APILocator::getRoleAPI).thenReturn(roleAPI);
            assertFalse(MigrationReadinessResource.isMigrationSupportUser(user));
        }
    }

    /** An access-layer failure fails closed (denied), never open. */
    @Test
    public void accessLookupThrows_failsClosed() throws DotDataException {
        final User user = mock(User.class);
        final UserAPI userAPI = mock(UserAPI.class);
        when(userAPI.isCMSAdmin(user)).thenThrow(new DotDataException("boom"));

        try (MockedStatic<APILocator> api = Mockito.mockStatic(APILocator.class)) {
            api.when(APILocator::getUserAPI).thenReturn(userAPI);
            assertFalse(MigrationReadinessResource.isMigrationSupportUser(user));
        }
    }
}
