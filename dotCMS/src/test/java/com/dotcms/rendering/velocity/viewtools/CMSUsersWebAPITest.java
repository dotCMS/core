package com.dotcms.rendering.velocity.viewtools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.business.UserAPI;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.TimeZone;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * The users handed out by the {@code $cmsuser} view tool are template-facing copies: they keep
 * the profile data and hide the stored password.
 */
public class CMSUsersWebAPITest {

    private MockedStatic<APILocator> mockedApiLocator;
    private UserAPI userAPI;
    private RoleAPI roleAPI;
    private User systemUser;
    private User storedUser;

    @BeforeEach
    public void setUp() throws Exception {
        userAPI = mock(UserAPI.class);
        roleAPI = mock(RoleAPI.class);
        systemUser = new User("system");
        systemUser.setCompanyId("dotcms.org");

        storedUser = new User("admin-id");
        storedUser.setCompanyId("dotcms.org");
        storedUser.setEmailAddress("admin@example.com");
        storedUser.setFirstName("Admin");
        storedUser.setLastName("User");
        storedUser.setPassword("stored-password-hash");
        storedUser.setRememberMeToken("remember-me-token");

        mockedApiLocator = mockStatic(APILocator.class);
        mockedApiLocator.when(APILocator::getUserAPI).thenReturn(userAPI);
        mockedApiLocator.when(APILocator::getRoleAPI).thenReturn(roleAPI);
        mockedApiLocator.when(APILocator::systemTimeZone).thenReturn(TimeZone.getTimeZone("UTC"));
        when(userAPI.getSystemUser()).thenReturn(systemUser);
        when(userAPI.loadByUserByEmail(eq("admin@example.com"), any(User.class), anyBoolean())).thenReturn(storedUser);
        when(userAPI.loadUserById(eq("admin-id"), any(User.class), anyBoolean())).thenReturn(storedUser);
    }

    @AfterEach
    public void tearDown() {
        mockedApiLocator.close();
    }

    /**
     * Method to test: {@link CMSUsersWebAPI#getUserByEmail(String)}
     * Given scenario: the API returns a user with a stored password
     * Expected result: the template gets a copy with the profile and no password or token
     */
    @Test
    public void getUserByEmailHidesCredentials() {
        final User result = new CMSUsersWebAPI().getUserByEmail("admin@example.com");

        assertTrue(result instanceof TemplateUser);
        assertEquals("admin@example.com", result.getEmailAddress());
        assertEquals("Admin User", result.getFullName());
        assertNull(result.getPassword());
        assertNull(result.getRememberMeToken());
        assertEquals("stored-password-hash", storedUser.getPassword());
    }

    /**
     * Method to test: {@link CMSUsersWebAPI#getUserByUserId(String)}
     * Given scenario: the API returns a user with a stored password
     * Expected result: the template gets a copy with no password or token
     */
    @Test
    public void getUserByUserIdHidesCredentials() {
        final User result = new CMSUsersWebAPI().getUserByUserId("admin-id");

        assertTrue(result instanceof TemplateUser);
        assertEquals("admin-id", result.getUserId());
        assertNull(result.getPassword());
        assertNull(result.getRememberMeToken());
    }

    /**
     * Method to test: {@link CMSUsersWebAPI#getAllUsersInGroup(String)}
     * Given scenario: the role resolves to one user with a stored password
     * Expected result: every user in the list is a template copy with no password
     */
    @Test
    public void getAllUsersInGroupHidesCredentials() throws Exception {
        final Role role = new Role();
        role.setId("role-id");
        when(roleAPI.loadRoleById("role-id")).thenReturn(role);
        when(roleAPI.findUserIdsForRole(role)).thenReturn(List.of("admin-id"));

        final List<User> result = new CMSUsersWebAPI().getAllUsersInGroup("role-id");

        assertEquals(1, result.size());
        assertTrue(result.get(0) instanceof TemplateUser);
        assertNull(result.get(0).getPassword());
    }

    /**
     * Method to test: {@link CMSUsersWebAPI#getLoggedInUser(HttpServletRequest)}
     * Given scenario: the request carries the logged-in user as a request attribute
     * Expected result: the template gets a copy with no password or token
     */
    @Test
    public void getLoggedInUserHidesCredentials() {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute(WebKeys.USER)).thenReturn(storedUser);

        final User result = new CMSUsersWebAPI().getLoggedInUser(request);

        assertTrue(result instanceof TemplateUser);
        assertEquals("admin-id", result.getUserId());
        assertNull(result.getPassword());
        assertNull(result.getRememberMeToken());
    }

    /**
     * Method to test: {@link CMSUsersWebAPI#getUserByEmail(String)}
     * Given scenario: the email does not match any user
     * Expected result: null, as before
     */
    @Test
    public void unknownEmailStillReturnsNull() {
        assertNull(new CMSUsersWebAPI().getUserByEmail("nobody@example.com"));
    }

}
