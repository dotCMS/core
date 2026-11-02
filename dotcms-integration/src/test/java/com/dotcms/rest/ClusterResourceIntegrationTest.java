package com.dotcms.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;

import com.dotcms.cluster.bean.Server;
import com.dotcms.cluster.business.ServerAPI;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.cache.transport.NullTransport;
import com.liferay.util.Base64;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.mockito.MockedStatic;
import org.junit.Test;

/**
 * Integration tests for the legacy {@code GET /api/cluster/getNodesStatus/}
 * ({@link ClusterResource#getNodesInfo}), which the old Configuration → Network tab reads.
 *
 * <p>With the {@code NullTransport} cache transport, {@code validateCacheInCluster} returns
 * {@code null}. The endpoint must treat that as "no server answered" instead of failing with a 500
 * (issue #37875), and otherwise keep its structure. The integration environment
 * itself runs an uninitialized {@code PubSubCacheTransport}, so the test hands the endpoint a
 * {@code NullTransport} through a spied cache administrator, for that one call only.</p>
 *
 * <p>This server's own {@code cluster_server} row, which a running dotCMS creates at startup, is
 * created the same way ({@link ServerAPI#getOrCreateMyServer()}) and removed afterwards if the
 * class created it; its heartbeat is refreshed so it is in the alive list.</p>
 *
 * @author hassandotcms
 */
public class ClusterResourceIntegrationTest {

    private static ClusterResource resource;
    private static Host testHost;
    private static boolean createdMyServer;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        resource = new ClusterResource();
        testHost = new SiteDataGen().nextPersisted();
        final ServerAPI serverAPI = APILocator.getServerAPI();
        createdMyServer = serverAPI.getServer(serverAPI.readServerId()) == null;
        serverAPI.getOrCreateMyServer();
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

    /**
     * Given : the NullTransport cache transport, which returns no replies at all (null).
     * When  : an admin calls the endpoint.
     * Then  : it answers 200 with its usual structure, and lists every alive server once as not
     *         responding (status red), instead of failing with a NullPointerException.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void nullTransport_listsEveryAliveServerAsNotResponding() throws Exception {
        LicenseUtil.updateLicenseHeartbeat();
        final Set<String> alive = APILocator.getServerAPI().getAliveServers().stream()
                .map(Server::getServerId).collect(Collectors.toSet());
        assertTrue("this server must be in the alive list", alive.contains(APILocator.getServerAPI().readServerId()));
        final DotCacheAdministrator nullTransportAdmin = spy(CacheLocator.getCacheAdministrator());
        doReturn(new NullTransport()).when(nullTransportAdmin).getTransport();

        final Response response;
        try (MockedStatic<CacheLocator> cacheLocator = mockStatic(CacheLocator.class, CALLS_REAL_METHODS)) {
            cacheLocator.when(CacheLocator::getCacheAdministrator).thenReturn(nullTransportAdmin);
            response = resource.getNodesInfo(adminRequest(), new MockHttpResponse().response(), "");
        }

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        final Map<String, Serializable> body = (Map<String, Serializable>) response.getEntity();
        assertEquals(APILocator.getServerAPI().readServerId(), body.get("myServerId"));
        assertEquals("red", body.get("clusterHealth"));
        final List<Map<String, Serializable>> serverInfo = (List<Map<String, Serializable>>) body.get("serverInfo");
        assertEquals(alive.size(), serverInfo.size());
        serverInfo.forEach(server -> {
            assertTrue("unexpected server " + server.get("serverId"), alive.contains(server.get("serverId")));
            assertEquals("red", server.get("status"));
        });
    }
}
