package com.dotcms.rest;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.exception.DotDataException;
import com.liferay.portal.model.User;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;

/**
 * Covers {@link WebResource#hasAnyRole(RoleAPI, User, Set)}, the membership check behind
 * {@link WebResource.InitBuilder#requiredBackendUser(boolean)} and friends. The required roles are
 * collected as role keys, so the check must resolve them through the API rather than treat them as
 * ids, which only worked while the role cache happened to hold the role.
 */
public class WebResourceRoleCheckTest {

    private static final String BACKEND_ROLE_ID = "b1a2c3d4-0000-0000-0000-000000000001";

    private RoleAPI roleAPI;
    private User user;
    private Role backendRole;

    @Before
    public void setUp() throws Exception {
        roleAPI = mock(RoleAPI.class);
        user = new User("dotcms.org.1");
        backendRole = new Role();
        backendRole.setId(BACKEND_ROLE_ID);
        backendRole.setRoleKey(Role.DOTCMS_BACK_END_USER);
    }

    /**
     * Method to test: {@link WebResource#hasAnyRole(RoleAPI, User, Set)}
     * Given scenario: The required role key resolves through {@code loadRoleByKey} and the user
     * holds that role.
     * Expected result: Access is granted, and the key is never handed to the id-based overload.
     */
    @Test
    public void testHasAnyRole_WhenRoleResolvesByKeyAndUserHoldsIt_ShouldReturnTrue() throws Exception {
        when(roleAPI.loadRoleByKey(Role.DOTCMS_BACK_END_USER)).thenReturn(backendRole);
        when(roleAPI.doesUserHaveRole(user, backendRole)).thenReturn(true);

        assertTrue(WebResource.hasAnyRole(roleAPI, user, Set.of(Role.DOTCMS_BACK_END_USER)));
        verify(roleAPI, never()).doesUserHaveRole(any(User.class), anyString());
    }

    /**
     * Method to test: {@link WebResource#hasAnyRole(RoleAPI, User, Set)}
     * Given scenario: The role resolves by key, but the user does not hold it.
     * Expected result: Access is denied.
     */
    @Test
    public void testHasAnyRole_WhenUserLacksTheRole_ShouldReturnFalse() throws Exception {
        when(roleAPI.loadRoleByKey(Role.DOTCMS_BACK_END_USER)).thenReturn(backendRole);
        when(roleAPI.doesUserHaveRole(user, backendRole)).thenReturn(false);

        assertFalse(WebResource.hasAnyRole(roleAPI, user, Set.of(Role.DOTCMS_BACK_END_USER)));
    }

    /**
     * Method to test: {@link WebResource#hasAnyRole(RoleAPI, User, Set)}
     * Given scenario: The value is not a role key but a role id, as a caller of
     * {@code requiredRoles(...)} might pass.
     * Expected result: The lookup falls back to {@code loadRoleById} and access is granted.
     */
    @Test
    public void testHasAnyRole_WhenValueIsARoleId_ShouldFallBackToLookupById() throws Exception {
        when(roleAPI.loadRoleByKey(BACKEND_ROLE_ID)).thenReturn(null);
        when(roleAPI.loadRoleById(BACKEND_ROLE_ID)).thenReturn(backendRole);
        when(roleAPI.doesUserHaveRole(user, backendRole)).thenReturn(true);

        assertTrue(WebResource.hasAnyRole(roleAPI, user, Set.of(BACKEND_ROLE_ID)));
    }

    /**
     * Method to test: {@link WebResource#hasAnyRole(RoleAPI, User, Set)}
     * Given scenario: Neither lookup finds a role for the value.
     * Expected result: Access is denied without asking whether the user holds a {@code null} role.
     */
    @Test
    public void testHasAnyRole_WhenRoleIsUnknown_ShouldReturnFalseWithoutMembershipCheck() throws Exception {
        when(roleAPI.loadRoleByKey("no-such-role")).thenReturn(null);
        when(roleAPI.loadRoleById("no-such-role")).thenReturn(null);

        assertFalse(WebResource.hasAnyRole(roleAPI, user, Set.of("no-such-role")));
        verify(roleAPI, never()).doesUserHaveRole(eq(user), any(Role.class));
    }

    /**
     * Method to test: {@link WebResource#hasAnyRole(RoleAPI, User, Set)}
     * Given scenario: Several roles are required and the user holds only the second one.
     * Expected result: Holding any one of them is enough.
     */
    @Test
    public void testHasAnyRole_WhenUserHoldsOneOfSeveral_ShouldReturnTrue() throws Exception {
        final Role frontendRole = new Role();
        frontendRole.setId("f1a2c3d4-0000-0000-0000-000000000002");
        frontendRole.setRoleKey(Role.DOTCMS_FRONT_END_USER);
        when(roleAPI.loadRoleByKey(Role.DOTCMS_BACK_END_USER)).thenReturn(backendRole);
        when(roleAPI.loadRoleByKey(Role.DOTCMS_FRONT_END_USER)).thenReturn(frontendRole);
        when(roleAPI.doesUserHaveRole(user, backendRole)).thenReturn(false);
        when(roleAPI.doesUserHaveRole(user, frontendRole)).thenReturn(true);

        assertTrue(WebResource.hasAnyRole(roleAPI, user,
                Set.of(Role.DOTCMS_BACK_END_USER, Role.DOTCMS_FRONT_END_USER)));
    }

    /**
     * Method to test: {@link WebResource#hasAnyRole(RoleAPI, User, Set)}
     * Given scenario: There is no user for the request.
     * Expected result: Access is denied without touching the API.
     */
    @Test
    public void testHasAnyRole_WhenUserIsNull_ShouldReturnFalse() throws Exception {
        assertFalse(WebResource.hasAnyRole(roleAPI, null, Set.of(Role.DOTCMS_BACK_END_USER)));
        verify(roleAPI, never()).loadRoleByKey(anyString());
    }

    /**
     * Method to test: {@link WebResource#hasAnyRole(RoleAPI, User, Set)}
     * Given scenario: The role lookup throws.
     * Expected result: The failure is treated as "no role", not propagated.
     */
    @Test
    public void testHasAnyRole_WhenLookupFails_ShouldReturnFalse() throws Exception {
        when(roleAPI.loadRoleByKey(Role.DOTCMS_BACK_END_USER)).thenThrow(new DotDataException("boom"));

        assertFalse(WebResource.hasAnyRole(roleAPI, user, Set.of(Role.DOTCMS_BACK_END_USER)));
    }

}
