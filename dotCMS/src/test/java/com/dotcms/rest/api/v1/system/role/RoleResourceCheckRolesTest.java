package com.dotcms.rest.api.v1.system.role;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.WebResource.InitBuilder;
import com.dotcms.rest.exception.ForbiddenException;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.LayoutAPI;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.util.SecurityLogger;
import com.liferay.portal.model.User;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * Unit tests for {@link RoleResource#checkRoles}: who may ask whether a user holds a set of roles.
 */
class RoleResourceCheckRolesTest {

    private static final String CALLER_ID = "caller-user-id";
    private static final String CALLER_EMAIL = "caller@example.com";
    private static final String OTHER_USER_ID = "other-user-id";
    private static final String ROLE_IDS = "role-a,role-b";

    private final HttpServletRequest request = mock(HttpServletRequest.class);
    private final HttpServletResponse response = mock(HttpServletResponse.class);
    private final RoleAPI roleAPI = mock(RoleAPI.class);
    private final LayoutAPI layoutAPI = mock(LayoutAPI.class);
    private final User caller = mock(User.class);
    private final List<String> securityLog = new ArrayList<>();

    @BeforeEach
    void setUp() {
        when(caller.getUserId()).thenReturn(CALLER_ID);
        when(caller.getEmailAddress()).thenReturn(CALLER_EMAIL);
        when(caller.isAdmin()).thenReturn(false);
        when(roleAPI.doesUserHaveRoles(anyString(), anyList())).thenReturn(true);
    }

    @Test
    void checkRoles_otherUserWithoutMembershipPrivilege_isForbidden() {
        // Given a back-end caller without the role membership privilege
        // When it checks the roles of another user
        // Then the request is forbidden and no role lookup runs
        assertThrows(ForbiddenException.class, () -> checkRoles(false, OTHER_USER_ID));
        verify(roleAPI, never()).doesUserHaveRoles(anyString(), anyList());
    }

    @Test
    void checkRoles_ownUser_isAllowedWithoutMembershipPrivilege() throws Exception {
        // Given a back-end caller without the role membership privilege
        // When it checks its own user id
        final Response result = checkRoles(false, CALLER_ID);

        // Then the response carries the result of the role lookup
        assertEquals(Boolean.TRUE, checkRolesValue(result));
        verify(roleAPI).doesUserHaveRoles(eq(CALLER_ID), anyList());
    }

    @Test
    void checkRoles_otherUserWithMembershipPrivilege_isAllowed() throws Exception {
        // Given a back-end caller with access to the Roles and Users portlets
        // When it checks the roles of another user
        final Response result = checkRoles(true, OTHER_USER_ID);

        // Then the response carries the result of the role lookup
        assertEquals(Boolean.TRUE, checkRolesValue(result));
        verify(roleAPI).doesUserHaveRoles(eq(OTHER_USER_ID), anyList());
    }

    @Test
    void checkRoles_ownEmailInsteadOfUserId_isTreatedAsAnotherUser() {
        // Given a back-end caller without the role membership privilege
        // When it sends its own email address instead of its user id
        // Then the request is forbidden, as for any other user
        assertThrows(ForbiddenException.class, () -> checkRoles(false, CALLER_EMAIL));
        verify(roleAPI, never()).doesUserHaveRoles(anyString(), anyList());
    }

    @Test
    void checkRoles_refusedRequest_logsTheCallerOnly() {
        // Given a back-end caller without the role membership privilege
        // When it checks the roles of another user
        assertThrows(ForbiddenException.class, () -> checkRoles(false, OTHER_USER_ID));

        // Then one security log line names the caller and not the requested user or roles
        assertEquals(1, securityLog.size());
        final String line = securityLog.get(0);
        assertTrue(line.contains(CALLER_ID));
        assertFalse(line.contains(OTHER_USER_ID));
        assertFalse(line.contains("role-a"));
    }

    /**
     * Calls {@link RoleResource#checkRoles} as {@link #caller}. The membership privilege is
     * granted or withheld through the caller's access to the Roles and Users portlets, and every
     * security log line is kept in {@link #securityLog}.
     */
    private Response checkRoles(final boolean membershipPrivilege, final String userId) throws Exception {

        try (MockedStatic<APILocator> apiLocator = mockStatic(APILocator.class);
                MockedStatic<SecurityLogger> securityLogger = mockStatic(SecurityLogger.class)) {
            securityLogger.when(() -> SecurityLogger.logInfo(any(), anyString()))
                    .thenAnswer(invocation -> securityLog.add(invocation.getArgument(1)));
            apiLocator.when(APILocator::getLayoutAPI).thenReturn(layoutAPI);
            when(layoutAPI.doesUserHaveAccessToPortlet(anyString(), eq(caller))).thenReturn(membershipPrivilege);
            when(layoutAPI.doesUserHaveAccessToUsersPortlet(caller)).thenReturn(membershipPrivilege);

            final InitDataObject initData = mock(InitDataObject.class);
            when(initData.getUser()).thenReturn(caller);
            final WebResource webResource = mock(WebResource.class);
            when(webResource.init(any(InitBuilder.class))).thenReturn(initData);

            return new RoleResource(webResource, roleAPI).checkRoles(request, response, userId, ROLE_IDS);
        }
    }

    @SuppressWarnings("unchecked")
    private static Object checkRolesValue(final Response result) {
        final Map<String, Object> entity =
                ((ResponseEntityRoleOperationView) result.getEntity()).getEntity();
        return entity.get("checkRoles");
    }
}
