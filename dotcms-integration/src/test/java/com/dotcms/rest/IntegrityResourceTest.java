package com.dotcms.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import com.dotcms.datagen.EnvironmentDataGen;
import com.dotcms.datagen.LayoutDataGen;
import com.dotcms.datagen.PushPublishingEndPointDataGen;
import com.dotcms.datagen.RoleDataGen;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.publisher.endpoint.bean.PublishingEndPoint;
import com.dotcms.publisher.environment.bean.Environment;
import com.dotcms.rest.exception.SecurityException;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Layout;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.PortletID;
import com.liferay.portal.ejb.UserTestUtil;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import java.io.File;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration tests for the session-authenticated integrity endpoints of {@link IntegrityResource}
 * ({@code checkintegrity}, {@code checkIntegrityProcessStatus}, {@code getIntegrityResult},
 * {@code discardconflicts} and {@code fixconflicts}), which back the integrity checker of the
 * Publishing Environments screen in the Configuration portlet.
 *
 * <p>Covers who may call them (a back-end user with the Configuration portlet, or a CMS admin),
 * and that the endpoint id in the request must belong to an existing endpoint.</p>
 */
public class IntegrityResourceTest {

    private static final String FOLDERS = "folders";

    private static IntegrityResource resource;
    private static RoleAPI roleAPI;
    private static Environment environment;
    private static PublishingEndPoint endpoint;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        resource = new IntegrityResource();
        roleAPI = APILocator.getRoleAPI();
        environment = new EnvironmentDataGen().nextPersisted();
        endpoint = new PushPublishingEndPointDataGen()
                .environment(environment)
                .address("127.0.0.1")
                .port("8080")
                .authKey(UUID.randomUUID().toString())
                .nextPersisted();
    }

    @AfterClass
    public static void cleanup() throws Exception {
        if (endpoint != null) {
            APILocator.getPublisherEndPointAPI().deleteEndPointById(endpoint.getId());
        }
        if (environment != null) {
            APILocator.getEnvironmentAPI().deleteEnvironment(environment.getId());
        }
    }

    private static HttpServletRequest baseRequest() {
        return new MockHeaderRequest(
                new MockSessionRequest(
                        new MockAttributeRequest(
                                new MockHttpRequestIntegrationTest("localhost", "/").request())
                                .request())
                        .request());
    }

    private static HttpServletRequest requestFor(final User user) {
        final HttpServletRequest request = baseRequest();
        request.getSession().setAttribute(WebKeys.USER_ID, user.getUserId());
        request.getSession().setAttribute(WebKeys.USER, user);
        return request;
    }

    private static HttpServletResponse response() {
        return new MockHttpResponse().response();
    }

    private static User backendUserWithLayout(final String... portletIds) throws Exception {
        final User user = UserTestUtil.getUser("integrity" + System.nanoTime(), false, true);
        roleAPI.addRoleToUser(roleAPI.loadBackEndUserRole(), user);
        if (portletIds.length > 0) {
            final Layout layout = new LayoutDataGen().portletIds(portletIds).nextPersisted();
            final Role role = new RoleDataGen().layout(layout).nextPersisted();
            roleAPI.addRoleToUser(role, user);
        }
        return user;
    }

    private static User frontendUser() throws Exception {
        final User user = UserTestUtil.getUser("integrityfe" + System.nanoTime(), false, true);
        roleAPI.addRoleToUser(roleAPI.loadFrontEndUserRole(), user);
        return user;
    }

    private static User configurationUser() throws Exception {
        return backendUserWithLayout(PortletID.CONFIGURATION.toString());
    }

    /** The five session-authenticated calls, all for the same endpoint id. */
    private static void assertEveryCallRejected(final User user, final String endpointId) throws Exception {
        assertRejected(() -> resource.checkIntegrity(requestFor(user), response(),
                "endpoint/" + endpointId));
        assertRejected(() -> resource.checkIntegrityProcessStatus(requestFor(user), response(),
                "endPoint/" + endpointId));
        assertRejected(() -> resource.getIntegrityResult(requestFor(user), response(),
                "endPoint/" + endpointId + "/type/" + FOLDERS));
        assertRejected(() -> resource.discardConflicts(requestFor(user), response(),
                "endPoint/" + endpointId + "/type/" + FOLDERS));
        assertRejected(() -> resource.fixConflicts(requestFor(user), response(),
                "endPoint/" + endpointId + "/type/" + FOLDERS + "/whereToFix/local"));
        assertRejected(() -> resource.fixConflicts(requestFor(user), response(),
                "endPoint/" + endpointId + "/type/" + FOLDERS + "/whereToFix/remote"));
    }

    private static void assertRejected(final Call call) throws Exception {
        try {
            final Response response = call.run();
            fail("the call must be rejected, got HTTP " + response.getStatus());
        } catch (final SecurityException expected) {
            assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(),
                    expected.getResponse().getStatus());
        }
    }

    private static File integrityFolder(final String endpointId) {
        return new File(ConfigUtils.getIntegrityPath() + File.separator + endpointId);
    }

    @FunctionalInterface
    private interface Call {
        Response run() throws Exception;
    }

    /**
     * Given : a back-end user with no portlets.
     * When  : any of the five integrity calls is made for an existing endpoint.
     * Then  : 401 for each call.
     */
    @Test
    public void backendUserWithoutConfigurationPortlet_isRejected() throws Exception {
        assertEveryCallRejected(backendUserWithLayout(), endpoint.getId());
    }

    /**
     * Given : a back-end user whose only portlet is not Configuration.
     * When  : any of the five integrity calls is made for an existing endpoint.
     * Then  : 401 for each call.
     */
    @Test
    public void backendUserWithOtherPortlet_isRejected() throws Exception {
        assertEveryCallRejected(backendUserWithLayout(PortletID.CONTENT.toString()), endpoint.getId());
    }

    /**
     * Given : a front-end user.
     * When  : any of the five integrity calls is made for an existing endpoint.
     * Then  : 401 for each call.
     */
    @Test
    public void frontendUser_isRejected() throws Exception {
        assertEveryCallRejected(frontendUser(), endpoint.getId());
    }

    /**
     * Given : a back-end user without the Configuration portlet and an endpoint id that does not
     *         exist.
     * When  : conflicts are sent to that remote endpoint.
     * Then  : 401, and no integrity folder is created for that id.
     */
    @Test
    public void rejectedUser_createsNoIntegrityFolder() throws Exception {
        final String unknownId = "integrity-" + UUID.randomUUID();
        final User user = backendUserWithLayout();

        assertRejected(() -> resource.fixConflicts(requestFor(user), response(),
                "endPoint/" + unknownId + "/type/" + FOLDERS + "/whereToFix/remote"));

        assertFalse(integrityFolder(unknownId).exists());
    }

    /**
     * Given : a back-end user whose only portlet is Configuration.
     * When  : the status, the result and the discard calls are made for an existing endpoint.
     * Then  : 200 for each call.
     */
    @Test
    public void configurationUser_isAllowed() throws Exception {
        final User user = configurationUser();

        assertEquals(200, resource.checkIntegrityProcessStatus(requestFor(user), response(),
                "endPoint/" + endpoint.getId()).getStatus());
        assertEquals(200, resource.getIntegrityResult(requestFor(user), response(),
                "endPoint/" + endpoint.getId() + "/type/" + FOLDERS).getStatus());
        assertEquals(200, resource.discardConflicts(requestFor(user), response(),
                "endPoint/" + endpoint.getId() + "/type/" + FOLDERS).getStatus());
        assertEquals(200, resource.fixConflicts(requestFor(user), response(),
                "endPoint/" + endpoint.getId() + "/type/" + FOLDERS + "/whereToFix/local").getStatus());
    }

    /**
     * Given : a CMS admin with no extra layout.
     * When  : the status call is made for an existing endpoint.
     * Then  : 200.
     */
    @Test
    public void admin_isAllowed() throws Exception {
        final User admin = APILocator.getUserAPI().loadByUserByEmail("admin@dotcms.com",
                APILocator.systemUser(), false);

        assertEquals(200, resource.checkIntegrityProcessStatus(requestFor(admin), response(),
                "endPoint/" + endpoint.getId()).getStatus());
    }

    /**
     * Given : a user with the Configuration portlet and an endpoint id that does not exist.
     * When  : each of the five integrity calls is made for that id.
     * Then  : 404 for each call, and no integrity folder is created for that id.
     */
    @Test
    public void unknownEndpoint_returnsNotFound() throws Exception {
        final String unknownId = "integrity-" + UUID.randomUUID();
        final User user = configurationUser();

        assertEquals(404, resource.checkIntegrity(requestFor(user), response(),
                "endpoint/" + unknownId).getStatus());
        assertEquals(404, resource.checkIntegrityProcessStatus(requestFor(user), response(),
                "endPoint/" + unknownId).getStatus());
        assertEquals(404, resource.getIntegrityResult(requestFor(user), response(),
                "endPoint/" + unknownId + "/type/" + FOLDERS).getStatus());
        assertEquals(404, resource.discardConflicts(requestFor(user), response(),
                "endPoint/" + unknownId + "/type/" + FOLDERS).getStatus());
        assertEquals(404, resource.fixConflicts(requestFor(user), response(),
                "endPoint/" + unknownId + "/type/" + FOLDERS + "/whereToFix/local").getStatus());
        assertEquals(404, resource.fixConflicts(requestFor(user), response(),
                "endPoint/" + unknownId + "/type/" + FOLDERS + "/whereToFix/remote").getStatus());

        assertFalse(integrityFolder(unknownId).exists());
    }

    /**
     * Given : a user with the Configuration portlet and an existing endpoint.
     * When  : the discard and fix calls receive a type that is not an integrity type.
     * Then  : 400 for each call.
     */
    @Test
    public void unknownType_returnsBadRequest() throws Exception {
        final User user = configurationUser();

        assertEquals(400, resource.discardConflicts(requestFor(user), response(),
                "endPoint/" + endpoint.getId() + "/type/notatype").getStatus());
        assertEquals(400, resource.fixConflicts(requestFor(user), response(),
                "endPoint/" + endpoint.getId() + "/type/notatype/whereToFix/local").getStatus());

        // only the plain ASCII case of a type name is accepted, so the same value can be sent
        // to the remote end-point
        final String dottedCapitalI = "fİleassets";
        assertEquals(400, resource.discardConflicts(requestFor(user), response(),
                "endPoint/" + endpoint.getId() + "/type/" + dottedCapitalI).getStatus());
        assertEquals(400, resource.fixConflicts(requestFor(user), response(),
                "endPoint/" + endpoint.getId() + "/type/" + dottedCapitalI + "/whereToFix/local").getStatus());
    }
}
