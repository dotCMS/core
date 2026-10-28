package com.dotmarketing.portlets.user.ajax;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.business.ajax.DwrUtil;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * Unit tests for {@link UserAjax#hasUserRoles}: who may ask whether a user holds a set of roles.
 */
class UserAjaxTest {

    private static final String CALLER_ID = "caller-user-id";
    private static final String CALLER_EMAIL = "caller@example.com";
    private static final String OTHER_USER_ID = "other-user-id";
    private static final String[] ROLE_IDS = {"role-a", "role-b"};

    private final User caller = mock(User.class);
    private final RoleAPI roleAPI = mock(RoleAPI.class);

    @BeforeEach
    void setUp() {
        when(caller.getUserId()).thenReturn(CALLER_ID);
        when(caller.getEmailAddress()).thenReturn(CALLER_EMAIL);
        when(roleAPI.doesUserHaveRoles(anyString(), anyList())).thenReturn(true);
    }

    @Test
    void hasUserRoles_otherUserWithoutMembershipPrivilege_isRejected() {
        try (MockedStatic<DwrUtil> dwrUtil = mockStatic(DwrUtil.class);
                MockedStatic<APILocator> apiLocator = mockStatic(APILocator.class)) {
            // Given a logged-in back-end user without the role membership privilege
            givenCaller(dwrUtil, apiLocator, false);

            // When it checks the roles of another user
            // Then the call is rejected and no role lookup runs
            assertThrows(DotSecurityException.class,
                    () -> new UserAjax().hasUserRoles(OTHER_USER_ID, ROLE_IDS));
            verify(roleAPI, never()).doesUserHaveRoles(anyString(), anyList());
        }
    }

    @Test
    void hasUserRoles_ownUser_isAllowedWithoutMembershipPrivilege() throws Exception {
        try (MockedStatic<DwrUtil> dwrUtil = mockStatic(DwrUtil.class);
                MockedStatic<APILocator> apiLocator = mockStatic(APILocator.class)) {
            // Given a logged-in back-end user without the role membership privilege
            givenCaller(dwrUtil, apiLocator, false);

            // When it checks its own user id
            // Then the result of the role lookup is returned
            assertTrue(new UserAjax().hasUserRoles(CALLER_ID, ROLE_IDS));
            verify(roleAPI).doesUserHaveRoles(eq(CALLER_ID), anyList());
        }
    }

    @Test
    void hasUserRoles_ownEmailInsteadOfUserId_isTreatedAsAnotherUser() {
        try (MockedStatic<DwrUtil> dwrUtil = mockStatic(DwrUtil.class);
                MockedStatic<APILocator> apiLocator = mockStatic(APILocator.class)) {
            // Given a logged-in back-end user without the role membership privilege
            givenCaller(dwrUtil, apiLocator, false);

            // When it sends its own email address instead of its user id
            // Then the call is rejected, as for any other user, and no role lookup runs
            assertThrows(DotSecurityException.class,
                    () -> new UserAjax().hasUserRoles(CALLER_EMAIL, ROLE_IDS));
            verify(roleAPI, never()).doesUserHaveRoles(anyString(), anyList());
        }
    }

    @Test
    void hasUserRoles_otherUserWithMembershipPrivilege_isAllowed() throws Exception {
        try (MockedStatic<DwrUtil> dwrUtil = mockStatic(DwrUtil.class);
                MockedStatic<APILocator> apiLocator = mockStatic(APILocator.class)) {
            // Given a logged-in back-end user with access to the Roles and Users portlets
            givenCaller(dwrUtil, apiLocator, true);

            // When it checks the roles of another user
            // Then the result of the role lookup is returned
            assertTrue(new UserAjax().hasUserRoles(OTHER_USER_ID, ROLE_IDS));
            verify(roleAPI).doesUserHaveRoles(eq(OTHER_USER_ID), anyList());
        }
    }

    /**
     * Makes {@link #caller} the logged-in DWR user. Without the membership privilege, the shared
     * membership guard rejects the caller, as {@link DwrUtil#validateRoleMembershipPermissions} does.
     */
    private void givenCaller(final MockedStatic<DwrUtil> dwrUtil, final MockedStatic<APILocator> apiLocator,
            final boolean membershipPrivilege) {

        dwrUtil.when(DwrUtil::getLoggedInUser).thenReturn(caller);
        if (!membershipPrivilege) {
            dwrUtil.when(() -> DwrUtil.validateRoleMembershipPermissions(caller))
                    .thenThrow(new DotSecurityException("not authorized"));
        }
        apiLocator.when(APILocator::getRoleAPI).thenReturn(roleAPI);
    }
}
