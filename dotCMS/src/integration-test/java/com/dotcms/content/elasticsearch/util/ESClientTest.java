package com.dotcms.content.elasticsearch.util;

import com.dotcms.cluster.bean.ServerPort;
import com.dotcms.util.ConfigTestHelper;
import com.dotcms.util.IntegrationTestInitService;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;

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

@RunWith(DataProviderRunner.class)
public class ESClientTest {

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

}
