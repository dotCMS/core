package com.dotcms.rest.api.v1.cluster;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import com.dotcms.cluster.business.ServerAPI;
import com.dotcms.datagen.LayoutDataGen;
import com.dotcms.datagen.RoleDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.rest.exception.SecurityException;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Layout;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.util.PortletID;
import com.liferay.portal.ejb.UserTestUtil;
import com.liferay.portal.model.User;
import com.liferay.util.Base64;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration tests for {@code GET /api/v1/cluster/nodes} ({@link ClusterNodesResource}), the
 * typed endpoint behind the Network (Beta) portlet (#37875).
 *
 * <p>No other server runs in the integration environment, and its cache transport is never
 * started, so no server answers the status request: this covers the access rule and the shape of
 * a server that didn't answer. The shape of an answering server is covered by the unit tests.</p>
 *
 * <p>This server's own {@code cluster_server} row, which a running dotCMS creates at startup, is
 * created the same way and removed afterwards if the class created it; its heartbeat is refreshed
 * so it is in the alive list.</p>
 *
 * @author hassandotcms
 */
public class ClusterNodesResourceIntegrationTest {

    private static ClusterNodesResource resource;
    private static Host testHost;
    private static RoleAPI roleAPI;
    private static boolean createdMyServer;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        resource = new ClusterNodesResource();
        testHost = new SiteDataGen().nextPersisted();
        roleAPI = APILocator.getRoleAPI();
        final ServerAPI serverAPI = APILocator.getServerAPI();
        createdMyServer = serverAPI.getServer(serverAPI.readServerId()) == null;
        serverAPI.getOrCreateMyServer();
        LicenseUtil.updateLicenseHeartbeat();
    }

    @AfterClass
    public static void cleanup() throws Exception {
        if (createdMyServer) {
            final ServerAPI serverAPI = APILocator.getServerAPI();
            serverAPI.removeServerFromClusterTable(serverAPI.readServerId());
        }
    }

    private static HttpServletRequest baseRequest() {
        return new MockHeaderRequest(
                new MockSessionRequest(
                        new MockAttributeRequest(
                                new MockHttpRequestIntegrationTest(testHost.getHostname(), "/").request())
                                .request())
                        .request());
    }

    private static HttpServletRequest adminRequest() {
        final MockHeaderRequest request = (MockHeaderRequest) baseRequest();
        request.setHeader("Authorization",
                "Basic " + new String(Base64.encode("admin@dotcms.com:admin".getBytes())));
        return request;
    }

    private static HttpServletRequest requestFor(final User user) {
        final HttpServletRequest request = baseRequest();
        request.getSession().setAttribute(com.liferay.portal.util.WebKeys.USER_ID, user.getUserId());
        request.getSession().setAttribute(com.liferay.portal.util.WebKeys.USER, user);
        return request;
    }

    private static User backendUserWithLayout(final String... portletIds) throws Exception {
        final User user = UserTestUtil.getUser("cn" + System.nanoTime(), false, true);
        roleAPI.addRoleToUser(roleAPI.loadBackEndUserRole(), user);
        if (portletIds.length > 0) {
            final Layout layout = new LayoutDataGen().portletIds(portletIds).nextPersisted();
            final Role role = new RoleDataGen().layout(layout).nextPersisted();
            roleAPI.addRoleToUser(role, user);
        }
        return user;
    }

    private static ClusterNodesView call(final HttpServletRequest request) throws Exception {
        final Response response = resource.getClusterNodes(request, new MockHttpResponse().response());
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        return ((ResponseEntityClusterNodesView) response.getEntity()).getEntity();
    }

    private static void assertRejected(final HttpServletRequest request) throws Exception {
        try {
            resource.getClusterNodes(request, new MockHttpResponse().response());
            fail("the call must be rejected");
        } catch (final SecurityException expected) {
            assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), expected.getResponse().getStatus());
        }
    }

    /**
     * Given : an admin.
     * When  : the nodes are listed, in an environment where no server answers.
     * Then  : 200; this server is listed with its database identity, as not responding, LAGGING
     *         (fresh heartbeat, no reply) and with no sections.
     */
    @Test
    public void admin_listsThisServerAsNotResponding() throws Exception {
        final ServerAPI serverAPI = APILocator.getServerAPI();
        final String myServerId = serverAPI.readServerId();

        final ClusterNodesView view = call(adminRequest());

        assertEquals(myServerId, view.currentServerId());
        final ClusterNodeView me = view.nodes().stream()
                .filter(node -> myServerId.equals(node.serverId())).findFirst()
                .orElseThrow(() -> new AssertionError("this server must be listed"));
        assertEquals(LicenseUtil.getDisplayServerId(myServerId), me.displayServerId());
        assertFalse(me.responded());
        assertEquals(NodeStatus.LAGGING, me.status());
        assertNull(me.cache());
        assertNull(me.search());
        assertNull(me.assets());
    }

    /**
     * Given : a back-end user whose only grant is the network-beta portlet.
     * When  : the nodes are listed.
     * Then  : 200.
     */
    @Test
    public void networkBetaUser_isAllowed() throws Exception {
        call(requestFor(backendUserWithLayout(PortletID.NETWORK_BETA.toString())));
    }

    /**
     * Given : a back-end user whose only grant is the configuration portlet, the one the old
     *         Network tab requires.
     * When  : the nodes are listed.
     * Then  : 200.
     */
    @Test
    public void configurationUser_isAllowed() throws Exception {
        call(requestFor(backendUserWithLayout(PortletID.CONFIGURATION.toString())));
    }

    /**
     * Given : a back-end user with neither the configuration nor the network-beta portlet, and an
     *         anonymous caller.
     * When  : they list the nodes.
     * Then  : both are rejected with 401.
     */
    @Test
    public void userWithoutPortlet_andAnonymous_areRejected() throws Exception {
        assertRejected(requestFor(backendUserWithLayout()));
        assertRejected(baseRequest());
    }
}
