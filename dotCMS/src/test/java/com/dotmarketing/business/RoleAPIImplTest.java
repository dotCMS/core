package com.dotmarketing.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.PortletID;
import com.dotmarketing.util.SecurityLogger;
import com.liferay.portal.model.User;
import java.util.ArrayList;
import java.util.List;
import org.mockito.MockedStatic;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;

/**
 * Covers {@link RoleAPIImpl#loadRoleByKeyOrId(String)}. Neither the key nor the id lookup caches a
 * miss, so the method must try the likelier form first and only then the other one.
 */
public class RoleAPIImplTest {

    private static final String ROLE_ID = "6b7a3c21-5d0e-4f8b-9a1c-2e3d4f5a6b7c";
    private static final String ROLE_KEY = Role.DOTCMS_BACK_END_USER;

    private RoleFactory roleFactory;
    private RoleAPIImpl roleAPI;
    private Role role;

    @Before
    public void setUp() {
        roleFactory = mock(RoleFactory.class);
        roleAPI = new RoleAPIImpl(roleFactory, mock(UserAPI.class));
        role = new Role();
        role.setId(ROLE_ID);
        role.setRoleKey(ROLE_KEY);
    }

    /**
     * Method to test: {@link RoleAPIImpl#loadRoleByKeyOrId(String)}
     * Given scenario: The value is shaped like a UUID and matches a role id.
     * Expected result: The id lookup runs first and the key lookup is never needed.
     */
    @Test
    public void testLoadRoleByKeyOrId_WhenValueIsAUuidMatchingAnId_ShouldTryTheIdFirst() throws Exception {
        when(roleFactory.getRoleById(ROLE_ID)).thenReturn(role);

        assertSame(role, roleAPI.loadRoleByKeyOrId(ROLE_ID));
        verify(roleFactory, never()).loadRoleByKey(anyString());
    }

    /**
     * Method to test: {@link RoleAPIImpl#loadRoleByKeyOrId(String)}
     * Given scenario: The value is shaped like a UUID but is actually a role key.
     * Expected result: After the id lookup misses, the key lookup finds the role.
     */
    @Test
    public void testLoadRoleByKeyOrId_WhenUuidShapedValueIsAKey_ShouldFallBackToTheKey() throws Exception {
        final Role uuidKeyedRole = new Role();
        uuidKeyedRole.setId("some-other-id");
        uuidKeyedRole.setRoleKey(ROLE_ID);
        when(roleFactory.getRoleById(ROLE_ID)).thenReturn(null);
        when(roleFactory.loadRoleByKey(ROLE_ID)).thenReturn(uuidKeyedRole);

        assertSame(uuidKeyedRole, roleAPI.loadRoleByKeyOrId(ROLE_ID));
        final InOrder order = inOrder(roleFactory);
        order.verify(roleFactory).getRoleById(ROLE_ID);
        order.verify(roleFactory).loadRoleByKey(ROLE_ID);
    }

    /**
     * Method to test: {@link RoleAPIImpl#loadRoleByKeyOrId(String)}
     * Given scenario: The value is a role key such as {@code DOTCMS_BACK_END_USER}.
     * Expected result: The key lookup runs first and the id lookup is never needed.
     */
    @Test
    public void testLoadRoleByKeyOrId_WhenValueIsAKey_ShouldTryTheKeyFirst() throws Exception {
        when(roleFactory.loadRoleByKey(ROLE_KEY)).thenReturn(role);

        assertSame(role, roleAPI.loadRoleByKeyOrId(ROLE_KEY));
        verify(roleFactory, never()).getRoleById(anyString());
    }

    /**
     * Method to test: {@link RoleAPIImpl#loadRoleByKeyOrId(String)}
     * Given scenario: The value is not shaped like a UUID and matches no key, but it is the id of
     * a role (ids are not always UUIDs).
     * Expected result: After the key lookup misses, the id lookup finds the role.
     */
    @Test
    public void testLoadRoleByKeyOrId_WhenNonUuidValueIsAnId_ShouldFallBackToTheId() throws Exception {
        final Role plainIdRole = new Role();
        plainIdRole.setId("cms_anon");
        when(roleFactory.loadRoleByKey("cms_anon")).thenReturn(null);
        when(roleFactory.getRoleById("cms_anon")).thenReturn(plainIdRole);

        assertSame(plainIdRole, roleAPI.loadRoleByKeyOrId("cms_anon"));
        final InOrder order = inOrder(roleFactory);
        order.verify(roleFactory).loadRoleByKey("cms_anon");
        order.verify(roleFactory).getRoleById("cms_anon");
    }

    /**
     * Method to test: {@link RoleAPIImpl#loadRoleByKeyOrId(String)}
     * Given scenario: Neither lookup finds a role, or the factory hands back a role with no id.
     * Expected result: {@code null}.
     */
    @Test
    public void testLoadRoleByKeyOrId_WhenNothingMatches_ShouldReturnNull() throws Exception {
        when(roleFactory.loadRoleByKey("nope")).thenReturn(null);
        when(roleFactory.getRoleById("nope")).thenReturn(new Role());

        assertNull(roleAPI.loadRoleByKeyOrId("nope"));
    }

    /**
     * Method to test: {@link RoleAPIImpl#loadRoleByKeyOrId(String)}
     * Given scenario: The value is {@code null} or blank.
     * Expected result: {@code null}, without touching the factory.
     */
    @Test
    public void testLoadRoleByKeyOrId_WhenValueIsBlank_ShouldReturnNullWithoutLookups() throws Exception {
        assertNull(roleAPI.loadRoleByKeyOrId(null));
        assertNull(roleAPI.loadRoleByKeyOrId("  "));
        verify(roleFactory, never()).loadRoleByKey(anyString());
        verify(roleFactory, never()).getRoleById(anyString());
    }

    // ==================== role membership: who may read it ====================

    /**
     * Method to test: {@link RoleAPIImpl#canReadRoleMembership(User)}
     * Given scenario: The user is a CMS Administrator without any portlet.
     * Expected result: Role membership is readable.
     */
    @Test
    public void testCanReadRoleMembership_WhenAdministrator_ShouldBeTrue() throws Exception {
        assertTrue(canReadRoleMembership(true, false, false));
    }

    /**
     * Method to test: {@link RoleAPIImpl#canReadRoleMembership(User)}
     * Given scenario: The user is not an administrator but has both the Roles and the Users portlets.
     * Expected result: Role membership is readable.
     */
    @Test
    public void testCanReadRoleMembership_WhenRolesAndUsersPortlets_ShouldBeTrue() throws Exception {
        assertTrue(canReadRoleMembership(false, true, true));
    }

    /**
     * Method to test: {@link RoleAPIImpl#canReadRoleMembership(User)}
     * Given scenario: The user is not an administrator and has the Roles portlet only.
     * Expected result: Role membership is not readable; both portlets are required.
     */
    @Test
    public void testCanReadRoleMembership_WhenRolesPortletOnly_ShouldBeFalse() throws Exception {
        assertFalse(canReadRoleMembership(false, true, false));
    }

    /**
     * Method to test: {@link RoleAPIImpl#canReadRoleMembership(User)}
     * Given scenario: The user is not an administrator and has the Users portlet only.
     * Expected result: Role membership is not readable; both portlets are required.
     */
    @Test
    public void testCanReadRoleMembership_WhenUsersPortletOnly_ShouldBeFalse() throws Exception {
        assertFalse(canReadRoleMembership(false, false, true));
    }

    // ==================== role check on behalf of a requesting user ====================

    /**
     * Method to test: {@link RoleAPIImpl#doesUserHaveRoles(String, List, User)}
     * Given scenario: A requesting user without the role membership privilege checks its own roles.
     * Expected result: The answer of the role lookup.
     */
    @Test
    public void testDoesUserHaveRoles_WhenOwnUserWithoutMembershipPrivilege_ShouldAnswer() throws Exception {
        final RoleAPIImpl api = spyAnswering(true);

        assertTrue(checkAs(api, false, REQUESTER_ID));
        verify(api).doesUserHaveRoles(REQUESTER_ID, ROLE_IDS);
    }

    /**
     * Method to test: {@link RoleAPIImpl#doesUserHaveRoles(String, List, User)}
     * Given scenario: A requesting user without the role membership privilege checks another user.
     * Expected result: {@link DotSecurityException}, and no role lookup runs.
     */
    @Test
    public void testDoesUserHaveRoles_WhenOtherUserWithoutMembershipPrivilege_ShouldReject() throws Exception {
        final RoleAPIImpl api = spyAnswering(true);

        assertThrows(DotSecurityException.class, () -> checkAs(api, false, OTHER_USER_ID));
        verify(api, never()).doesUserHaveRoles(anyString(), anyList());
    }

    /**
     * Method to test: {@link RoleAPIImpl#doesUserHaveRoles(String, List, User)}
     * Given scenario: A requesting user without the role membership privilege sends its own email
     * address instead of its user id.
     * Expected result: {@link DotSecurityException}, as for any other user.
     */
    @Test
    public void testDoesUserHaveRoles_WhenOwnEmailInsteadOfUserId_ShouldReject() throws Exception {
        final RoleAPIImpl api = spyAnswering(true);

        assertThrows(DotSecurityException.class, () -> checkAs(api, false, REQUESTER_EMAIL));
        verify(api, never()).doesUserHaveRoles(anyString(), anyList());
    }

    /**
     * Method to test: {@link RoleAPIImpl#doesUserHaveRoles(String, List, User)}
     * Given scenario: A requesting user with the Roles and Users portlets checks another user.
     * Expected result: The answer of the role lookup for that user.
     */
    @Test
    public void testDoesUserHaveRoles_WhenOtherUserWithMembershipPrivilege_ShouldAnswer() throws Exception {
        final RoleAPIImpl api = spyAnswering(true);

        assertTrue(checkAs(api, true, OTHER_USER_ID));
        verify(api).doesUserHaveRoles(OTHER_USER_ID, ROLE_IDS);
    }

    /**
     * Method to test: {@link RoleAPIImpl#doesUserHaveRoles(String, List, User)}
     * Given scenario: A requesting user without the role membership privilege checks another user.
     * Expected result: One security log line names the requesting user and not the requested user
     * or roles.
     */
    @Test
    public void testDoesUserHaveRoles_WhenRejected_ShouldLogTheRequesterOnly() throws Exception {
        final RoleAPIImpl api = spyAnswering(true);

        assertThrows(DotSecurityException.class, () -> checkAs(api, false, OTHER_USER_ID));

        assertEquals(1, securityLog.size());
        final String line = securityLog.get(0);
        assertTrue(line.contains(REQUESTER_ID));
        assertFalse(line.contains(OTHER_USER_ID));
        assertFalse(line.contains(ROLE_IDS.get(0)));
    }

    private static final String REQUESTER_ID = "requester-user-id";
    private static final String REQUESTER_EMAIL = "requester@example.com";
    private static final String OTHER_USER_ID = "other-user-id";
    private static final List<String> ROLE_IDS = List.of("role-a", "role-b");

    private final List<String> securityLog = new ArrayList<>();

    /**
     * Builds a {@link User} with the given administrator flag and asks
     * {@link RoleAPIImpl#canReadRoleMembership(User)} about it, with the Roles and Users portlet
     * access given.
     */
    private boolean canReadRoleMembership(final boolean admin, final boolean rolesPortlet,
            final boolean usersPortlet) throws Exception {
        final User user = requester(admin);
        final LayoutAPI layoutAPI = mock(LayoutAPI.class);
        when(layoutAPI.doesUserHaveAccessToPortlet(PortletID.ROLES.toString(), user)).thenReturn(rolesPortlet);
        when(layoutAPI.doesUserHaveAccessToUsersPortlet(user)).thenReturn(usersPortlet);
        try (MockedStatic<APILocator> apiLocator = mockStatic(APILocator.class)) {
            apiLocator.when(APILocator::getLayoutAPI).thenReturn(layoutAPI);
            return roleAPI.canReadRoleMembership(user);
        }
    }

    /**
     * A spy of the API under test whose plain role lookup ({@code doesUserHaveRoles(String, List)})
     * answers the given value, so the tests observe whether and for whom it runs.
     */
    private RoleAPIImpl spyAnswering(final boolean answer) {
        final RoleAPIImpl api = spy(new RoleAPIImpl(roleFactory, mock(UserAPI.class)));
        doReturn(answer).when(api).doesUserHaveRoles(anyString(), anyList());
        return api;
    }

    /**
     * Calls {@link RoleAPIImpl#doesUserHaveRoles(String, List, User)} for the given user id on behalf
     * of a non-administrator requester whose membership privilege is granted or withheld, keeping
     * every security log line in {@link #securityLog}.
     */
    private boolean checkAs(final RoleAPIImpl api, final boolean membershipPrivilege, final String userId)
            throws Exception {
        final User requester = requester(false);
        doReturn(membershipPrivilege).when(api).canReadRoleMembership(requester);
        try (MockedStatic<SecurityLogger> securityLogger = mockStatic(SecurityLogger.class)) {
            securityLogger.when(() -> SecurityLogger.logInfo(any(), anyString()))
                    .thenAnswer(invocation -> securityLog.add(invocation.getArgument(1)));
            return api.doesUserHaveRoles(userId, ROLE_IDS, requester);
        }
    }

    private static User requester(final boolean admin) {
        final User user = mock(User.class);
        when(user.getUserId()).thenReturn(REQUESTER_ID);
        when(user.getEmailAddress()).thenReturn(REQUESTER_EMAIL);
        when(user.isAdmin()).thenReturn(admin);
        return user;
    }
}
