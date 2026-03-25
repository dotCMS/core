package com.dotcms.rendering.velocity.viewtools;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.util.Config;
import com.liferay.portal.model.User;
import javax.servlet.http.HttpServletRequest;
import org.apache.velocity.context.Context;
import org.apache.velocity.tools.view.context.ViewContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

public class SecretToolTest {

    private SecretTool secretTool;
    private Context velocityContext;
    private HttpServletRequest request;
    private ViewContext viewContext;

    private MockedStatic<APILocator> mockedApiLocator;
    private MockedStatic<WebAPILocator> mockedWebApiLocator;
    private MockedStatic<Config> mockedConfig;

    private RoleAPI roleAPI;
    private UserWebAPI userWebAPI;

    @BeforeEach
    public void setUp() {
        secretTool = new SecretTool();
        velocityContext = mock(Context.class);
        request = mock(HttpServletRequest.class);
        viewContext = mock(ViewContext.class);

        when(viewContext.getVelocityContext()).thenReturn(velocityContext);
        when(viewContext.getRequest()).thenReturn(request);

        mockedApiLocator = mockStatic(APILocator.class);
        mockedWebApiLocator = mockStatic(WebAPILocator.class);
        mockedConfig = mockStatic(Config.class);

        roleAPI = mock(RoleAPI.class);
        userWebAPI = mock(UserWebAPI.class);

        mockedApiLocator.when(APILocator::getRoleAPI).thenReturn(roleAPI);
        mockedWebApiLocator.when(WebAPILocator::getUserWebAPI).thenReturn(userWebAPI);

        mockedConfig.when(() -> Config.getBooleanProperty(eq("secrets.scripting.enabled"), any(Boolean.class))).thenReturn(true); // Enabled by default

        secretTool.init(viewContext);
    }

    @AfterEach
    public void tearDown() {
        mockedApiLocator.close();
        mockedWebApiLocator.close();
        mockedConfig.close();
    }

    @Test
    public void canUserEvaluate_ScriptingDisabled_ThrowsException() {
        mockedConfig.when(() -> Config.getBooleanProperty(eq("secrets.scripting.enabled"), any(Boolean.class))).thenReturn(false);

        assertThrows(SecurityException.class, () -> secretTool.canUserEvaluate());
    }

    @Test
    public void canUserEvaluate_UserInRequestHasRole_Success() throws Exception {
        User user = mock(User.class);
        Role scriptingRole = mock(Role.class);

        when(userWebAPI.getUser(request)).thenReturn(user);
        when(user.isAnonymousUser()).thenReturn(false);
        when(roleAPI.loadRoleByKey(Role.SCRIPTING_DEVELOPER)).thenReturn(scriptingRole);
        when(roleAPI.doesUserHaveRole(user, scriptingRole)).thenReturn(true);

        assertDoesNotThrow(() -> secretTool.canUserEvaluate());
    }

    @Test
    public void canUserEvaluate_AnonymousInRequestButUserInContextHasRole_Success() throws Exception {
        User anonymousUser = mock(User.class);
        User contextUser = mock(User.class);
        Role scriptingRole = mock(Role.class);

        when(anonymousUser.isAnonymousUser()).thenReturn(true);
        when(userWebAPI.getUser(request)).thenReturn(anonymousUser);
        
        when(velocityContext.get("user")).thenReturn(contextUser);
        when(roleAPI.loadRoleByKey(Role.SCRIPTING_DEVELOPER)).thenReturn(scriptingRole);
        when(roleAPI.doesUserHaveRole(contextUser, scriptingRole)).thenReturn(true);

        assertDoesNotThrow(() -> secretTool.canUserEvaluate());
    }

    @Test
    public void canUserEvaluate_NoUserAnywhere_ThrowsException() throws Exception {
        when(userWebAPI.getUser(request)).thenReturn(null);
        when(velocityContext.get("user")).thenReturn(null);
        
        assertThrows(SecurityException.class, () -> secretTool.canUserEvaluate());
    }

    @Test
    public void canUserEvaluate_UserHasNoRole_ThrowsException() throws Exception {
        User user = mock(User.class);
        Role scriptingRole = mock(Role.class);

        when(userWebAPI.getUser(request)).thenReturn(user);
        when(user.isAnonymousUser()).thenReturn(false);
        when(roleAPI.loadRoleByKey(Role.SCRIPTING_DEVELOPER)).thenReturn(scriptingRole);
        when(roleAPI.doesUserHaveRole(user, scriptingRole)).thenReturn(false);

        assertThrows(SecurityException.class, () -> secretTool.canUserEvaluate());
    }
}
