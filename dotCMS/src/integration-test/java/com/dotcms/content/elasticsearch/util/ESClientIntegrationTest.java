package com.dotcms.content.elasticsearch.util;

import com.dotcms.cluster.bean.Server;
import com.dotcms.cluster.bean.ServerPort;
import com.dotcms.cluster.business.ClusterAPI;
import com.dotcms.cluster.business.ServerAPI;
import com.dotcms.util.ConfigTestHelper;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.UtilMethods;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;

import java.io.File;
import org.elasticsearch.common.settings.Settings;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;

import static com.dotcms.content.elasticsearch.util.ESClient.ES_NODE_DATA;
import static com.dotcms.content.elasticsearch.util.ESClient.ES_NODE_MASTER;
import static com.dotcms.content.elasticsearch.util.ESClient.ES_ZEN_UNICAST_HOSTS;
import static org.junit.Assert.assertEquals;

import static com.dotcms.content.elasticsearch.util.ESClient.ES_TRANSPORT_HOST;
import static org.junit.Assert.assertTrue;

@RunWith(DataProviderRunner.class)
public class ESClientIntegrationTest {

    private static final String OVERRIDE_YML_EXTERNAL_ES_PATH =
        "com/dotcms/content/elasticsearch/util/elasticsearch-override_external-es.yml";

    private static final String DEFAULT_YML_PATH =  "com/dotcms/content/elasticsearch/util/elasticsearch.yml";

    private static final String DEFAULT_YML_EXTERNAL_ES_PATH =
        "com/dotcms/content/elasticsearch/util/elasticsearch_external-es.yml";

    private static final String NON_EXISTING_PATH = "THIS-IS-A-NON-EXISTING-PATH";

    @BeforeClass
    public static void prepare() throws Exception{
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    @DataProvider
    public static Object[] testCases() throws URISyntaxException {
        return new ESClientTestCase[] {
            new ESClientTestCase.Builder()
                .overrideFilePath(Paths.get(ConfigTestHelper.getUrlToTestResource(OVERRIDE_YML_EXTERNAL_ES_PATH).toURI()))
                .community(false)
                .expectedESNodeData(Boolean.FALSE.toString())
                .expectedESNodeMaster(Boolean.FALSE.toString())
                .expectedTransportHost("172.20.0.4")
                .expectedZenUniCastHosts("[172.20.0.2:9300, 172.20.0.3:9300]")
                .expectedTCPPort("9310")
                .build(),

            new ESClientTestCase.Builder()
                .overrideFilePath(Paths.get(ConfigTestHelper.getUrlToTestResource(OVERRIDE_YML_EXTERNAL_ES_PATH).toURI()))
                .community(true)
                .expectedESNodeData(Boolean.TRUE.toString())
                .expectedESNodeMaster(Boolean.TRUE.toString())
                .expectedTransportHost("localhost")
                .expectedZenUniCastHosts("localhost:9310")
                .expectedTCPPort("9310")
                .build(),

            new ESClientTestCase.Builder()
                .overrideFilePath(Paths.get(NON_EXISTING_PATH))
                .defaultFilePath(Paths.get(ConfigTestHelper.getUrlToTestResource(DEFAULT_YML_PATH).toURI()))
                .community(true)
                .expectedESNodeData(Boolean.TRUE.toString())
                .expectedESNodeMaster(Boolean.TRUE.toString())
                .expectedTransportHost("localhost")
                .expectedZenUniCastHosts("localhost:9400")
                .expectedTCPPort("9400")
                .build(),

            new ESClientTestCase.Builder()
                .overrideFilePath(Paths.get(NON_EXISTING_PATH))
                .defaultFilePath(Paths.get(NON_EXISTING_PATH))
                .community(true)
                .expectedESNodeData(Boolean.TRUE.toString())
                .expectedESNodeMaster(Boolean.TRUE.toString())
                .expectedTransportHost("localhost")
                .expectedZenUniCastHosts("localhost:"+ ServerPort.ES_TRANSPORT_TCP_PORT.getDefaultValue())
                .expectedTCPPort(ServerPort.ES_TRANSPORT_TCP_PORT.getDefaultValue())
                .build(),

            new ESClientTestCase.Builder()
                .overrideFilePath(Paths.get(NON_EXISTING_PATH))
                .defaultFilePath(Paths.get(ConfigTestHelper.getUrlToTestResource(DEFAULT_YML_EXTERNAL_ES_PATH).toURI()))
                .community(true)
                .expectedESNodeData(Boolean.TRUE.toString())
                .expectedESNodeMaster(Boolean.TRUE.toString())
                .expectedTransportHost("localhost")
                .expectedZenUniCastHosts("localhost:9500")
                .expectedTCPPort("9500")
                .build()
        };
    }

    @DataProvider
    public static Object[] dataProviderTestSetTransportConfToSettings() throws URISyntaxException {
        final String SOME_EXTERNAL_ADDRESS = "192.168.1.2";
        final String SOME_EXTERNAL_PORT = "9310";

        final Server serverWithAddressAndPort = Server.builder()
                .withIpAddress("127.0.0.1")
                .withEsTransportTcpPort(9300)
                .build();

        return new ESClientSetTransportConfTestCase[]{
                new ESClientSetTransportConfTestCase.Builder()
                        .withTransportHostFromExtSettings(SOME_EXTERNAL_ADDRESS)
                        .withTransportTCPPortFromExtSettings(SOME_EXTERNAL_PORT)
                        .withCurrentServer(serverWithAddressAndPort)
                        .withExpectedTransportHost(SOME_EXTERNAL_ADDRESS)
                        .withExpectedTransportTCPPort(SOME_EXTERNAL_PORT)
                .build()
        };
    }

    @Test
    @UseDataProvider("testCases")
    public void testGetExtSettingsBuilder(final ESClientTestCase testCase) throws IOException {
        final ESClient esClient = Mockito.spy(new ESClient());
        Mockito.doReturn(testCase.getOverrideFilePath()).when(esClient).getOverrideYamlPath();
        Mockito.doReturn(testCase.getDefaultFilePath()).when(esClient).getDefaultYaml();
        Mockito.doReturn(testCase.isCommunity()).when(esClient).isCommunityOrStandard();
        final Settings.Builder builder = esClient.getExtSettingsBuilder();
        assertEquals(testCase.getExpectedTransportHost(), builder.get(ES_TRANSPORT_HOST));
        assertEquals(testCase.getExpectedZenUniCastHosts(), builder.get(ES_ZEN_UNICAST_HOSTS));
        assertEquals(testCase.getExpectedESNodeData(), builder.get(ES_NODE_DATA));
        assertEquals(testCase.getExpectedESNodeMaster(), builder.get(ES_NODE_MASTER));
    }

    @Test
    @UseDataProvider("dataProviderTestSetTransportConfToSettings")
    public void testSetTransportConfToSettings(final ESClientSetTransportConfTestCase testCase) throws DotDataException {
        final Settings.Builder settings = Settings.builder();
        settings.put(ES_TRANSPORT_HOST, testCase.getTransportHostFromExtSettings());
        settings.put(ServerPort.ES_TRANSPORT_TCP_PORT.getPropertyName(), testCase.getExpectedTransportTCPPort());

        final ClusterAPI clusterAPI = Mockito.mock(ClusterAPI.class);
        Mockito.when(clusterAPI.isESAutoWire()).thenReturn(true);

        final ServerAPI serverAPI = Mockito.spy(APILocator.getServerAPI());
        Mockito.doReturn(testCase.getCurrentServer()).when(serverAPI).getCurrentServer();

        final ESClient esClient = Mockito.spy(new ESClient(serverAPI, clusterAPI));
        Mockito.doReturn(testCase.getTransportHostFromExtSettings()).when(esClient)
                .validateAddress(testCase.getTransportHostFromExtSettings());

        esClient.setTransportConfToSettings(settings);

        assertEquals(testCase.getExpectedTransportHost(), settings.get(ES_TRANSPORT_HOST));
        assertEquals(testCase.getExpectedTransportTCPPort(), settings.get(ServerPort.ES_TRANSPORT_TCP_PORT.getPropertyName()));
    }

    @Test
    public void testSetHttpConfToSettings() throws DotDataException {
        final String EXTERNAL_HTTP_PORT = "9210";
        final Settings.Builder settings = Settings.builder();
        settings.put("http.enabled", "true");
        settings.put(ServerPort.ES_HTTP_PORT.getPropertyName(), EXTERNAL_HTTP_PORT);

        final ClusterAPI clusterAPI = Mockito.mock(ClusterAPI.class);
        Mockito.when(clusterAPI.isESAutoWire()).thenReturn(true);

        final Server serverWithHttpPort = Server.builder()
                .withEsHttpPort(9200)
                .build();

        final ServerAPI serverAPI = Mockito.spy(APILocator.getServerAPI());
        Mockito.doReturn(serverWithHttpPort).when(serverAPI).getCurrentServer();

        final ESClient esClient = new ESClient(serverAPI, clusterAPI);

        esClient.setHttpConfToSettings(settings);

        assertEquals(EXTERNAL_HTTP_PORT, settings.get(ServerPort.ES_HTTP_PORT.getPropertyName()));
    }

    @Test
    @UseDataProvider("testCases")
    public void testLoadNodeSettings(final ESClientTestCase testCase) throws IOException {
        final ESClient esClient = Mockito.spy(new ESClient());

        Mockito.doReturn(testCase.getOverrideFilePath()).when(esClient).getOverrideYamlPath();
        if (UtilMethods.isSet(testCase.getDefaultFilePath()) && !testCase.getDefaultFilePath()
                .equals(Paths.get(NON_EXISTING_PATH))) {
            Mockito.doReturn(testCase.getDefaultFilePath()).when(esClient).getDefaultYaml();
        }
        Mockito.doReturn(testCase.isCommunity()).when(esClient).isCommunityOrStandard();
        final Settings builder = esClient
                .loadNodeSettings(esClient.getExtSettingsBuilder());

        if (testCase.isCommunity()) {
            assertTrue(builder.keySet().stream().filter(key -> key.startsWith("discovery.") && !key
                    .equals(ES_ZEN_UNICAST_HOSTS)).count() == 0);
        } else {
            assertTrue(builder.keySet().stream().filter(key -> key.startsWith("discovery.") && !key
                    .equals(ES_ZEN_UNICAST_HOSTS)).count() > 0);
        }

        if (UtilMethods.isSet(builder.get("path.data"))){
            assertTrue(new File(builder.get("path.data")).isAbsolute());
        }
        if (UtilMethods.isSet(builder.get("path.repo"))) {
            assertTrue(new File(builder.get("path.repo")).isAbsolute());
        }

        if (UtilMethods.isSet(builder.get("path.logs"))) {
            assertTrue(new File(builder.get("path.logs")).isAbsolute());
        }
    }

}
