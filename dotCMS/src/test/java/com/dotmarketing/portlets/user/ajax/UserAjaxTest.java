package com.dotmarketing.portlets.user.ajax;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * Unit tests for {@link UserAjax#hasUserRoles}: the DWR method only delegates to
 * {@link RoleAPI#doesUserHaveRoles(String, List, User)} on behalf of the logged-in user, which owns
 * the rule of who may check whose roles.
 */
class UserAjaxTest {

    private static final String OTHER_USER_ID = "other-user-id";
    private static final String[] ROLE_IDS = {"role-a", "role-b"};

    private final User caller = mock(User.class);
    private final RoleAPI roleAPI = mock(RoleAPI.class);

    @Test
    void hasUserRoles_answersOnBehalfOfTheLoggedInUser() throws Exception {
        try (MockedStatic<DwrUtil> dwrUtil = mockStatic(DwrUtil.class);
                MockedStatic<APILocator> apiLocator = mockStatic(APILocator.class)) {
            // Given a logged-in user and a role API that answers for them
            dwrUtil.when(DwrUtil::getLoggedInUser).thenReturn(caller);
            apiLocator.when(APILocator::getRoleAPI).thenReturn(roleAPI);
            when(roleAPI.doesUserHaveRoles(eq(OTHER_USER_ID), anyList(), eq(caller))).thenReturn(true);

            // When a user's roles are checked
            // Then the answer of the role API is returned
            assertTrue(new UserAjax().hasUserRoles(OTHER_USER_ID, ROLE_IDS));
            verify(roleAPI).doesUserHaveRoles(OTHER_USER_ID, List.of("role-a", "role-b"), caller);
        }
    }

    @Test
    void hasUserRoles_refusedByTheRoleAPI_isRejected() throws Exception {
        try (MockedStatic<DwrUtil> dwrUtil = mockStatic(DwrUtil.class);
                MockedStatic<APILocator> apiLocator = mockStatic(APILocator.class)) {
            // Given a logged-in user the role API refuses
            dwrUtil.when(DwrUtil::getLoggedInUser).thenReturn(caller);
            apiLocator.when(APILocator::getRoleAPI).thenReturn(roleAPI);
            when(roleAPI.doesUserHaveRoles(eq(OTHER_USER_ID), anyList(), eq(caller)))
                    .thenThrow(new DotSecurityException("not allowed"));

            // When another user's roles are checked
            // Then the call is rejected
            assertThrows(DotSecurityException.class,
                    () -> new UserAjax().hasUserRoles(OTHER_USER_ID, ROLE_IDS));
        }
    }

    @Test
    void hasUserRoles_withoutLoggedInUser_isRejected() throws Exception {
        try (MockedStatic<DwrUtil> dwrUtil = mockStatic(DwrUtil.class);
                MockedStatic<APILocator> apiLocator = mockStatic(APILocator.class)) {
            // Given no logged-in DWR user
            dwrUtil.when(DwrUtil::getLoggedInUser).thenThrow(new DotSecurityException("not authorized"));
            apiLocator.when(APILocator::getRoleAPI).thenReturn(roleAPI);

            // When a user's roles are checked
            // Then the call is rejected and no role lookup runs
            assertThrows(DotSecurityException.class,
                    () -> new UserAjax().hasUserRoles(OTHER_USER_ID, ROLE_IDS));
            verify(roleAPI, never()).doesUserHaveRoles(anyString(), anyList(), any());
        }
    }
}
