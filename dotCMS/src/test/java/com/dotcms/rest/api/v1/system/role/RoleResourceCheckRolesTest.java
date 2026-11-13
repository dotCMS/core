package com.dotcms.rest.api.v1.system.role;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.WebResource.InitBuilder;
import com.dotcms.rest.exception.ForbiddenException;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.model.User;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * Unit tests for {@link RoleResource#checkRoles}: the endpoint asks
 * {@link RoleAPI#doesUserHaveRoles(String, List, User)} on behalf of the calling user, which owns
 * the rule of who may check whose roles.
 */
class RoleResourceCheckRolesTest {

    private static final String CALLER_ID = "caller-user-id";
    private static final String OTHER_USER_ID = "other-user-id";
    private static final String ROLE_IDS = "role-a,role-b";

    private final HttpServletRequest request = mock(HttpServletRequest.class);
    private final HttpServletResponse response = mock(HttpServletResponse.class);
    private final RoleAPI roleAPI = mock(RoleAPI.class);
    private final User caller = mock(User.class);

    @BeforeEach
    void setUp() {
        when(caller.getUserId()).thenReturn(CALLER_ID);
    }

    @Test
    void checkRoles_answersOnBehalfOfTheCaller() throws Exception {
        // Given the role API answers for the calling user
        when(roleAPI.doesUserHaveRoles(eq(OTHER_USER_ID), anyList(), eq(caller))).thenReturn(true);

        // When the caller checks a user's roles
        final Response result = checkRoles(OTHER_USER_ID);

        // Then the response carries that answer and the role ids were split from the path
        assertEquals(Boolean.TRUE, checkRolesValue(result));
        verify(roleAPI).doesUserHaveRoles(OTHER_USER_ID, List.of("role-a", "role-b"), caller);
    }

    @Test
    void checkRoles_refusedByTheRoleAPI_isForbidden() throws Exception {
        // Given the role API refuses the calling user
        when(roleAPI.doesUserHaveRoles(eq(OTHER_USER_ID), anyList(), eq(caller)))
                .thenThrow(new DotSecurityException("not allowed"));

        // When the caller checks another user's roles
        // Then the request is forbidden
        assertThrows(ForbiddenException.class, () -> checkRoles(OTHER_USER_ID));
    }

    /**
     * Calls {@link RoleResource#checkRoles} as {@link #caller}.
     */
    private Response checkRoles(final String userId) throws Exception {

        try (MockedStatic<APILocator> ignored = mockStatic(APILocator.class)) {
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
