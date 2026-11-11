package com.dotcms.publisher.endpoint.ajax;

import com.dotcms.IntegrationTestBase;
import com.dotcms.datagen.EnvironmentDataGen;
import com.dotcms.datagen.LayoutDataGen;
import com.dotcms.datagen.PushPublishingEndPointDataGen;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.publisher.endpoint.bean.PublishingEndPoint;
import com.dotcms.publisher.endpoint.bean.impl.PushPublishingEndPoint;
import com.dotcms.publisher.environment.bean.Environment;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Layout;
import com.dotmarketing.cms.factories.PublicEncryptionFactory;
import com.dotmarketing.util.PortletID;
import com.liferay.portal.model.User;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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
 * Verifies that {@link PublishingEndpointAjaxAction} only lets users with administrative rights,
 * or access to the Configuration portlet, register or modify endpoints, in line with the REST
 * endpoint resource.
 */
public class PublishingEndpointAjaxActionTest extends IntegrationTestBase {

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
            final PublishingEndPoint endpoint = APILocator.getPublisherEndPointAPI()
                    .findEndPointByName(name);
            if (endpoint != null) {
                APILocator.getPublisherEndPointAPI().deleteEndPointById(endpoint.getId());
            }
        }
    }

    /**
     * Method to test: {@link PublishingEndpointAjaxAction#addEndpoint}
     * When: invoked by a backend user that can only use the publishing queue portlet
     * Should: not register the endpoint and answer 403.
     */
    @Test
    public void test_addEndpoint_byNonAdminUser_isRejected() throws Exception {
        final Environment environment = new EnvironmentDataGen().nextPersisted();
        final String name = uniqueName("ajax_add");

        final HttpServletResponse response = responseWithWriter();
        action(publishingUser).addEndpoint(
                endpointRequest(name, "secret-one", environment.getId(), null), response);

        assertNull("endpoint must not be registered by a non-admin user",
                APILocator.getPublisherEndPointAPI().findEndPointByName(name));
        verify(response).sendError(HttpServletResponse.SC_FORBIDDEN);
    }

    /**
     * Method to test: {@link PublishingEndpointAjaxAction#editEndpoint}
     * When: invoked by a backend user that can only use the publishing queue portlet
     * Should: leave the stored endpoint untouched and answer 403.
     */
    @Test
    public void test_editEndpoint_byNonAdminUser_isRejected() throws Exception {
        final PushPublishingEndPoint existing = existingEndpoint("original-secret");

        final HttpServletResponse response = responseWithWriter();
        action(publishingUser).editEndpoint(endpointRequest(
                existing.getServerName().toString(), "replaced-secret",
                existing.getGroupId(), existing.getId()), response);

        assertEquals("original-secret", storedKey(existing.getId()));
        verify(response).sendError(HttpServletResponse.SC_FORBIDDEN);
    }

    /**
     * Method to test: {@link PublishingEndpointAjaxAction#addEndpoint}
     * When: invoked with no user
     * Should: not register the endpoint and answer 403.
     */
    @Test
    public void test_addEndpoint_withoutUser_isRejected() throws Exception {
        final Environment environment = new EnvironmentDataGen().nextPersisted();
        final String name = uniqueName("ajax_nouser");

        final HttpServletResponse response = responseWithWriter();
        action(null).addEndpoint(
                endpointRequest(name, "secret-one", environment.getId(), null), response);

        assertNull(APILocator.getPublisherEndPointAPI().findEndPointByName(name));
        verify(response).sendError(HttpServletResponse.SC_FORBIDDEN);
    }

    /**
     * Method to test: {@link PublishingEndpointAjaxAction#addEndpoint}
     * When: invoked by an administrator
     * Should: register the endpoint.
     */
    @Test
    public void test_addEndpoint_byAdminUser_registersEndpoint() throws Exception {
        final Environment environment = new EnvironmentDataGen().nextPersisted();
        final String name = uniqueName("ajax_admin");

        action(adminUser).addEndpoint(
                endpointRequest(name, "secret-one", environment.getId(), null), responseWithWriter());

        assertNotNull(APILocator.getPublisherEndPointAPI().findEndPointByName(name));
    }

    /**
     * Method to test: {@link PublishingEndpointAjaxAction#addEndpoint}
     * When: invoked by a non-administrator that has access to the Configuration portlet
     * Should: register the endpoint, as the REST endpoint resource does.
     */
    @Test
    public void test_addEndpoint_byConfigurationPortletUser_registersEndpoint() throws Exception {
        final Environment environment = new EnvironmentDataGen().nextPersisted();
        final String name = uniqueName("ajax_config");

        action(configurationUser).addEndpoint(
                endpointRequest(name, "secret-one", environment.getId(), null), responseWithWriter());

        assertNotNull(APILocator.getPublisherEndPointAPI().findEndPointByName(name));
    }

    /**
     * Method to test: {@link PublishingEndpointAjaxAction#editEndpoint}
     * When: invoked by an administrator
     * Should: store the new key.
     */
    @Test
    public void test_editEndpoint_byAdminUser_updatesEndpoint() throws Exception {
        final PushPublishingEndPoint existing = existingEndpoint("original-secret");

        action(adminUser).editEndpoint(endpointRequest(
                existing.getServerName().toString(), "replaced-secret",
                existing.getGroupId(), existing.getId()), responseWithWriter());

        assertEquals("replaced-secret", storedKey(existing.getId()));
    }

    private static User backendUserWithLayout(final Layout layout) throws Exception {
        final User user = new UserDataGen().nextPersisted();
        APILocator.getRoleAPI().addRoleToUser(APILocator.getRoleAPI().loadBackEndUserRole(), user);
        APILocator.getRoleAPI().addLayoutToRole(layout, APILocator.getRoleAPI().getUserRole(user));
        return user;
    }

    private PublishingEndpointAjaxAction action(final User user) {
        final PublishingEndpointAjaxAction action = new PublishingEndpointAjaxAction();
        action.setUser(user);
        return action;
    }

    private String uniqueName(final String prefix) {
        final String name = prefix + "_" + System.currentTimeMillis();
        createdNames.add(name);
        return name;
    }

    private PushPublishingEndPoint existingEndpoint(final String authKey) {
        final Environment environment = new EnvironmentDataGen().nextPersisted();
        final PushPublishingEndPoint existing = new PushPublishingEndPointDataGen()
                .environment(environment)
                .address("10.20.30.41")
                .authKey(authKey)
                .sending(false)
                .nextPersisted();
        createdNames.add(existing.getServerName().toString());
        return existing;
    }

    private String storedKey(final String endpointId) throws Exception {
        final PublishingEndPoint stored = APILocator.getPublisherEndPointAPI()
                .findEndPointById(endpointId);
        return PublicEncryptionFactory.decryptString(stored.getAuthKey().toString());
    }

    private HttpServletRequest endpointRequest(final String name, final String authKey,
            final String environmentId, final String identifier) {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameter("identifier")).thenReturn(identifier);
        when(request.getParameter("serverName")).thenReturn(name);
        when(request.getParameter("protocol")).thenReturn("http");
        when(request.getParameter("address")).thenReturn("10.20.30.41");
        when(request.getParameter("port")).thenReturn("8080");
        when(request.getParameter("authKey")).thenReturn(authKey);
        when(request.getParameter("enabled")).thenReturn("true");
        when(request.getParameter("sending")).thenReturn("true");
        when(request.getParameter("environmentId")).thenReturn(environmentId);
        return request;
    }

    private HttpServletResponse responseWithWriter() throws Exception {
        final HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));
        return response;
    }
}
