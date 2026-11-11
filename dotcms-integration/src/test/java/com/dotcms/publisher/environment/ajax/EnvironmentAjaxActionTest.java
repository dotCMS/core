package com.dotcms.publisher.environment.ajax;

import com.dotcms.IntegrationTestBase;
import com.dotcms.datagen.EnvironmentDataGen;
import com.dotcms.datagen.LayoutDataGen;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.publisher.environment.bean.Environment;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Layout;
import com.dotmarketing.util.PortletID;
import com.liferay.portal.model.User;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies that {@link EnvironmentAjaxAction} only lets users with administrative rights, or
 * access to the Configuration portlet, create or modify environments, in line with the REST
 * endpoint resource.
 */
public class EnvironmentAjaxActionTest extends IntegrationTestBase {

    private static User adminUser;
    private static User publishingUser;
    private static User configurationUser;
    private static Layout publishingLayout;
    private static Layout configurationLayout;

    private final List<String> createdNames = new ArrayList<>();

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();

        adminUser = TestUserUtils.getAdminUser();

        publishingLayout = new LayoutDataGen()
                .name("publishing-" + System.currentTimeMillis())
                .portletIds(PortletID.PUBLISHING_QUEUE.toString())
                .nextPersisted();
        publishingUser = backendUserWithLayout(publishingLayout);

        configurationLayout = new LayoutDataGen()
                .name("configuration-" + System.currentTimeMillis())
                .portletIds(PortletID.CONFIGURATION.toString())
                .nextPersisted();
        configurationUser = backendUserWithLayout(configurationLayout);
    }

    @AfterClass
    public static void cleanUpUsers() throws Exception {
        APILocator.getLayoutAPI().removeLayout(publishingLayout);
        APILocator.getLayoutAPI().removeLayout(configurationLayout);
    }

    @After
    public void cleanUp() throws Exception {
        for (final String name : createdNames) {
            final Environment environment = APILocator.getEnvironmentAPI()
                    .findEnvironmentByName(name);
            if (environment != null) {
                APILocator.getEnvironmentAPI().deleteEnvironment(environment.getId());
            }
        }
    }

    /**
     * Method to test: {@link EnvironmentAjaxAction#addEnvironment}
     * When: invoked by a backend user that can only use the publishing queue portlet
     * Should: not create the environment and answer 403.
     */
    @Test
    public void test_addEnvironment_byNonAdminUser_isRejected() throws Exception {
        final String name = uniqueName("ajax_env_add");

        final HttpServletResponse response = responseWithWriter();
        action(publishingUser).addEnvironment(environmentRequest(name, null), response);

        assertNull("environment must not be created by a non-admin user",
                APILocator.getEnvironmentAPI().findEnvironmentByName(name));
        verify(response).sendError(HttpServletResponse.SC_FORBIDDEN);
    }

    /**
     * Method to test: {@link EnvironmentAjaxAction#editEnvironment}
     * When: invoked by a backend user that can only use the publishing queue portlet
     * Should: leave the stored environment untouched and answer 403.
     */
    @Test
    public void test_editEnvironment_byNonAdminUser_isRejected() throws Exception {
        final Environment existing = existingEnvironment();

        final HttpServletResponse response = responseWithWriter();
        action(publishingUser).editEnvironment(
                environmentRequest(existing.getName() + "_renamed", existing.getId()), response);

        assertEquals(existing.getName(),
                APILocator.getEnvironmentAPI().findEnvironmentById(existing.getId()).getName());
        verify(response).sendError(HttpServletResponse.SC_FORBIDDEN);
    }

    /**
     * Method to test: {@link EnvironmentAjaxAction#addEnvironment}
     * When: invoked with no user
     * Should: not create the environment and answer 403.
     */
    @Test
    public void test_addEnvironment_withoutUser_isRejected() throws Exception {
        final String name = uniqueName("ajax_env_nouser");

        final HttpServletResponse response = responseWithWriter();
        action(null).addEnvironment(environmentRequest(name, null), response);

        assertNull(APILocator.getEnvironmentAPI().findEnvironmentByName(name));
        verify(response).sendError(HttpServletResponse.SC_FORBIDDEN);
    }

    /**
     * Method to test: {@link EnvironmentAjaxAction#addEnvironment}
     * When: invoked by an administrator
     * Should: create the environment.
     */
    @Test
    public void test_addEnvironment_byAdminUser_createsEnvironment() throws Exception {
        final String name = uniqueName("ajax_env_admin");

        action(adminUser).addEnvironment(environmentRequest(name, null), responseWithWriter());

        assertNotNull(APILocator.getEnvironmentAPI().findEnvironmentByName(name));
    }

    /**
     * Method to test: {@link EnvironmentAjaxAction#addEnvironment}
     * When: invoked by a non-administrator that has access to the Configuration portlet
     * Should: create the environment, as the REST endpoint resource does.
     */
    @Test
    public void test_addEnvironment_byConfigurationPortletUser_createsEnvironment() throws Exception {
        final String name = uniqueName("ajax_env_config");

        action(configurationUser).addEnvironment(environmentRequest(name, null), responseWithWriter());

        assertNotNull(APILocator.getEnvironmentAPI().findEnvironmentByName(name));
    }

    /**
     * Method to test: {@link EnvironmentAjaxAction#editEnvironment}
     * When: invoked by an administrator
     * Should: store the new name.
     */
    @Test
    public void test_editEnvironment_byAdminUser_updatesEnvironment() throws Exception {
        final Environment existing = existingEnvironment();
        final String newName = uniqueName(existing.getName() + "_renamed");

        action(adminUser).editEnvironment(environmentRequest(newName, existing.getId()),
                responseWithWriter());

        assertEquals(newName,
                APILocator.getEnvironmentAPI().findEnvironmentById(existing.getId()).getName());
    }

    private static User backendUserWithLayout(final Layout layout) throws Exception {
        final User user = new UserDataGen().nextPersisted();
        APILocator.getRoleAPI().addRoleToUser(APILocator.getRoleAPI().loadBackEndUserRole(), user);
        APILocator.getRoleAPI().addLayoutToRole(layout, APILocator.getRoleAPI().getUserRole(user));
        return user;
    }

    private EnvironmentAjaxAction action(final User user) {
        final EnvironmentAjaxAction action = new EnvironmentAjaxAction();
        action.setUser(user);
        return action;
    }

    private String uniqueName(final String prefix) {
        final String name = prefix + "_" + System.currentTimeMillis();
        createdNames.add(name);
        return name;
    }

    private Environment existingEnvironment() {
        final Environment existing = new EnvironmentDataGen().nextPersisted();
        createdNames.add(existing.getName());
        return existing;
    }

    private HttpServletRequest environmentRequest(final String name, final String identifier) {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameter("identifier")).thenReturn(identifier);
        when(request.getParameter("environmentName")).thenReturn(name);
        when(request.getParameter("whoCanUse")).thenReturn("");
        when(request.getParameter("pushType")).thenReturn("pushToAll");
        when(request.getSession()).thenReturn(mock(HttpSession.class));
        return request;
    }

    private HttpServletResponse responseWithWriter() throws Exception {
        final HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));
        return response;
    }
}
